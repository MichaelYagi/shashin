package com.miyagi.shashin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.OllamaVisionService
import com.miyagi.shashin.model.Memory
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.MemoryRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

data class MemoryView(
    val id: Int,
    val title: String,
    val caption: String,
    val strategyType: String,
    val photoIds: List<String>
)

private data class MemoryConcept(
    val type: String,
    val title: String,
    val caption: String,
    val personNames: List<String> = emptyList(),
    val year: Int? = null,
    val month: Int? = null,
    val keywords: List<String> = emptyList(),
    val placeName: String? = null
)

@Service
class MemoriesService(
    private val jdbcTemplate: JdbcTemplate,
    private val transactionManager: PlatformTransactionManager,
    private val memoryRepository: MemoryRepository,
    private val ollamaVisionService: OllamaVisionService
) {
    private val txTemplate = TransactionTemplate(transactionManager)
    private val mapper = ObjectMapper()
    private val logger = Logger.getLogger(MemoriesService::class.simpleName)

    val generationRunning = AtomicBoolean(false)

    fun hasGeneratedMemories(): Boolean = memoryRepository.count() > 0

    fun deleteAllMemories() {
        txTemplate.execute {
            jdbcTemplate.update("DELETE FROM memoryphoto")
            jdbcTemplate.update("DELETE FROM memory")
        }
        logger.log(Level.INFO, "All memories deleted")
    }

    fun getMemoryViews(): List<MemoryView> {
        val memories = memoryRepository.findAllOrderById()
        return memories.mapNotNull { memory ->
            val id = memory.getId()
            val photoIds = jdbcTemplate.queryForList(
                "SELECT metadata_id FROM memoryphoto WHERE memory_id = ? ORDER BY display_order ASC",
                String::class.java, id
            )
            if (photoIds.isEmpty()) null
            else MemoryView(
                id = id,
                title = memory.getTitle() ?: "",
                caption = memory.getCaption() ?: "",
                strategyType = memory.getStrategyType() ?: "",
                photoIds = photoIds
            )
        }
    }

    fun generateMemories(settings: Settings) {
        if (settings.getShowMemories() != true) return
        if (!ollamaVisionService.isOllamaConfigured(settings)) return
        if (!generationRunning.compareAndSet(false, true)) return

        try {
            logger.log(Level.INFO, "Memories generation started")

            val summary = buildLibrarySummary()
            val responseJson = ollamaVisionService.chatText(
                buildSystemPrompt(),
                buildUserPrompt(summary),
                settings
            ) ?: run {
                logger.log(Level.WARNING, "Ollama returned null for memories generation — keeping existing memories")
                return
            }

            val concepts = parseMemoryConcepts(responseJson)
            if (concepts.isEmpty()) {
                logger.log(Level.WARNING, "No valid memory concepts parsed from Ollama response")
                return
            }

            val usedMetadataIds = mutableSetOf<String>()
            val generated = mutableListOf<Pair<MemoryConcept, List<String>>>()
            val typeCounts = mutableMapOf("person" to 0, "event" to 0, "location" to 0, "theme" to 0)
            val typeLimits = mapOf("person" to 6, "event" to 4, "location" to 3, "theme" to 2)

            for (concept in concepts) {
                val typeLimit = typeLimits[concept.type] ?: 0
                if ((typeCounts[concept.type] ?: 0) >= typeLimit) continue

                val candidates = fetchCandidates(concept, usedMetadataIds)
                if (candidates.size < 5) continue

                val photos = candidates.take(15)
                usedMetadataIds.addAll(photos)
                typeCounts[concept.type] = (typeCounts[concept.type] ?: 0) + 1
                generated.add(Pair(concept, photos))
                logger.log(Level.INFO, "Memory \"${concept.title}\" (${concept.type}): ${photos.size} photos")
            }

            if (generated.isEmpty()) {
                logger.log(Level.WARNING, "No memories generated (all concepts below minimum photo threshold)")
                return
            }

            swapMemories(generated)
            logger.log(Level.INFO, "Memories generation complete — ${generated.size} memories stored")
        } finally {
            generationRunning.set(false)
        }
    }

    private fun buildLibrarySummary(): Map<String, Any> {
        val total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM metadata WHERE hidden = 0", Long::class.java
        ) ?: 0L

        val yearRows = jdbcTemplate.queryForList(
            "SELECT MIN(year) as minYear, MAX(year) as maxYear FROM metadata WHERE year IS NOT NULL AND year > 1970 AND hidden = 0"
        )
        val minYear = (yearRows.firstOrNull()?.get("minYear") as? Long)?.toInt() ?: 0
        val maxYear = (yearRows.firstOrNull()?.get("maxYear") as? Long)?.toInt() ?: 0

        val people = jdbcTemplate.queryForList(
            "SELECT rl.name, COUNT(*) as cnt FROM recognitionlabel rl JOIN recognitionlabelphoto rlp ON rl.id = rlp.recognition_label_id GROUP BY rl.id ORDER BY cnt DESC LIMIT 30"
        ).map { "${it["name"]} (${it["cnt"]} photos)" }

        val places = jdbcTemplate.queryForList(
            "SELECT place_name as pname, COUNT(*) as cnt FROM metadata WHERE place_name IS NOT NULL AND place_name != '' AND hidden = 0 GROUP BY place_name ORDER BY cnt DESC LIMIT 20"
        ).map { "${it["pname"]} (${it["cnt"]} photos)" }

        val keywords = jdbcTemplate.queryForList(
            "SELECT k.keyword, COUNT(*) as cnt FROM keyword k JOIN keywordphoto kp ON k.id = kp.keyword_id GROUP BY k.id ORDER BY cnt DESC LIMIT 30"
        ).map { "${it["keyword"]} (${it["cnt"]} photos)" }

        return mapOf(
            "total" to total,
            "minYear" to minYear,
            "maxYear" to maxYear,
            "people" to people,
            "places" to places,
            "keywords" to keywords
        )
    }

    private fun buildSystemPrompt(): String = """
        You are a thoughtful curator of personal photo memories. You analyze library summaries and propose meaningful, evocative memory collections.
        You MUST respond with a valid JSON array ONLY. No preamble, no explanation, no markdown fences.
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    private fun buildUserPrompt(summary: Map<String, Any>): String {
        val total = summary["total"]
        val minYear = summary["minYear"]
        val maxYear = summary["maxYear"]
        val people = (summary["people"] as? List<String>)?.joinToString(", ") ?: "none"
        val places = (summary["places"] as? List<String>)?.joinToString(", ") ?: "none"
        val keywords = (summary["keywords"] as? List<String>)?.joinToString(", ") ?: "none"

        return """
Photo library summary:
- Total photos: $total
- Years: $minYear–$maxYear
- People (named): $people
- Places: $places
- Keywords: $keywords

Propose up to 15 diverse memory concepts as a JSON array. Target mix: up to 6 person-focused, up to 4 event/time-based, up to 3 location-based, up to 2 theme-based. Only propose concepts that match available data above.

For each memory output an object with:
- "type": one of "person", "event", "location", "theme"
- "title": short evocative title (2–6 words)
- "caption": one warm sentence
- "person" type: add "person_names" array with exact names from the People list above
- "event" type: add "year" (integer), optionally "month" (1–12), and optionally "keywords" array
- "location" type: add "place_name" using an exact place name from the Places list above
- "theme" type: add "keywords" array using terms from the Keywords list above

Example format:
[{"type":"person","title":"Noah's Early Years","caption":"A look back at Noah growing up.","person_names":["Noah"]},{"type":"event","title":"Christmas 2022","caption":"A festive family celebration.","year":2022,"month":12,"keywords":["christmas"]},{"type":"location","title":"Tokyo Adventures","caption":"Exploring the city's many faces.","place_name":"Tokyo"},{"type":"theme","title":"Adventures in Nature","caption":"Hikes, trails, and open skies.","keywords":["hiking","nature"]}]
        """.trimIndent()
    }

    private fun parseMemoryConcepts(json: String): List<MemoryConcept> {
        return try {
            val cleaned = json.trim()
                .replace(Regex("^```[a-zA-Z]*\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("```\\s*$", RegexOption.MULTILINE), "")
                .trim()
            logger.log(Level.INFO, "Ollama memories response (first 500 chars): ${cleaned.take(500)}")
            val node = mapper.readTree(cleaned)
            val arr = when {
                node.isArray -> node
                node.isObject -> node["memories"] ?: node["concepts"] ?: return emptyList()
                else -> return emptyList()
            }
            arr.mapNotNull { obj ->
                val type = obj["type"]?.asText()?.lowercase() ?: return@mapNotNull null
                if (type !in setOf("person", "event", "location", "theme")) return@mapNotNull null
                val title = obj["title"]?.asText()?.trim() ?: return@mapNotNull null
                val caption = obj["caption"]?.asText()?.trim() ?: ""
                val personNames = obj["person_names"]?.map { it.asText() } ?: emptyList()
                val year = obj["year"]?.asInt()
                val month = obj["month"]?.asInt()
                val keywords = obj["keywords"]?.map { it.asText().lowercase() } ?: emptyList()
                val placeName = obj["place_name"]?.asText()
                MemoryConcept(type, title, caption, personNames, year, month, keywords, placeName)
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to parse memory concepts JSON: ${e.localizedMessage}. Raw (first 500): ${json.take(500)}")
            emptyList()
        }
    }

    private fun fetchCandidates(concept: MemoryConcept, usedIds: Set<String>): List<String> {
        val rows: List<Map<String, Any>> = when (concept.type) {
            "person" -> fetchPersonCandidates(concept)
            "event" -> fetchEventCandidates(concept)
            "location" -> fetchLocationCandidates(concept)
            "theme" -> fetchThemeCandidates(concept)
            else -> emptyList()
        }

        return deduplicateByDay(rows)
            .filter { it !in usedIds }
    }

    private fun fetchPersonCandidates(concept: MemoryConcept): List<Map<String, Any>> {
        if (concept.personNames.isEmpty()) return emptyList()
        val placeholders = concept.personNames.joinToString(",") { "?" }
        val params = concept.personNames.toTypedArray<Any>()
        return jdbcTemplate.queryForList(
            """SELECT DISTINCT m.id, m.year || '-' || m.month || '-' || m.day as dayKey
               FROM metadata m
               JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id
               JOIN recognitionlabel rl ON rlp.recognition_label_id = rl.id
               WHERE rl.name IN ($placeholders)
                 AND m.description IS NOT NULL AND m.description != ''
                 AND m.hidden = 0
                 AND CAST(rlp.confidence AS REAL) >= 70.0
               ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC""",
            *params
        )
    }

    private fun fetchEventCandidates(concept: MemoryConcept): List<Map<String, Any>> {
        if (concept.year == null) return emptyList()
        val conditions = StringBuilder("m.year = ? AND m.description IS NOT NULL AND m.description != '' AND m.hidden = 0")
        val params = mutableListOf<Any>(concept.year)
        if (concept.month != null) {
            conditions.append(" AND m.month = ?")
            params.add(concept.month)
        }
        if (concept.keywords.isNotEmpty()) {
            val kw = concept.keywords.joinToString(",") { "?" }
            return jdbcTemplate.queryForList(
                """SELECT DISTINCT m.id, m.year || '-' || m.month || '-' || m.day as dayKey
                   FROM metadata m
                   JOIN keywordphoto kp ON m.id = kp.metadata_id
                   JOIN keyword k ON kp.keyword_id = k.id
                   WHERE $conditions AND k.keyword IN ($kw)
                   ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC""",
                *(params + concept.keywords).toTypedArray()
            )
        }
        return jdbcTemplate.queryForList(
            """SELECT DISTINCT m.id, m.year || '-' || m.month || '-' || m.day as dayKey
               FROM metadata m WHERE $conditions
               ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC""",
            *params.toTypedArray()
        )
    }

    private fun fetchLocationCandidates(concept: MemoryConcept): List<Map<String, Any>> {
        val place = concept.placeName ?: return emptyList()
        return jdbcTemplate.queryForList(
            """SELECT DISTINCT m.id, m.year || '-' || m.month || '-' || m.day as dayKey
               FROM metadata m
               WHERE m.place_name LIKE ? AND m.description IS NOT NULL AND m.description != '' AND m.hidden = 0
               ORDER BY m.year DESC, m.month DESC, m.day DESC""",
            "%$place%"
        )
    }

    private fun fetchThemeCandidates(concept: MemoryConcept): List<Map<String, Any>> {
        if (concept.keywords.isEmpty()) return emptyList()
        val kw = concept.keywords.joinToString(",") { "?" }
        return jdbcTemplate.queryForList(
            """SELECT DISTINCT m.id, m.year || '-' || m.month || '-' || m.day as dayKey
               FROM metadata m
               JOIN keywordphoto kp ON m.id = kp.metadata_id
               JOIN keyword k ON kp.keyword_id = k.id
               WHERE k.keyword IN ($kw) AND m.description IS NOT NULL AND m.description != '' AND m.hidden = 0
               ORDER BY m.year DESC, m.month DESC, m.day DESC""",
            *concept.keywords.toTypedArray()
        )
    }

    private fun deduplicateByDay(rows: List<Map<String, Any>>): List<String> {
        val seenDays = mutableSetOf<String>()
        return rows.mapNotNull { row ->
            val id = row["id"]?.toString() ?: return@mapNotNull null
            val day = row["dayKey"]?.toString() ?: id
            if (seenDays.add(day)) id else null
        }
    }

    private fun swapMemories(memories: List<Pair<MemoryConcept, List<String>>>) {
        val ts = TextUtils.getCurrentTimestamp()
        txTemplate.execute {
            jdbcTemplate.update("DELETE FROM memoryphoto")
            jdbcTemplate.update("DELETE FROM memory")
            for ((concept, photoIds) in memories) {
                jdbcTemplate.update(
                    "INSERT INTO memory (title, caption, strategy_type, generated_date, createdAt, modifiedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    concept.title, concept.caption, concept.type, ts, ts, ts
                )
                val memoryId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long::class.java)!!
                photoIds.forEachIndexed { idx, metadataId ->
                    jdbcTemplate.update(
                        "INSERT INTO memoryphoto (memory_id, metadata_id, display_order, createdAt, modifiedAt) VALUES (?, ?, ?, ?, ?)",
                        memoryId, metadataId, idx, ts, ts
                    )
                }
            }
        }
    }
}
