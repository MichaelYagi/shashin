package com.miyagi.shashin.service

import com.miyagi.shashin.component.OllamaVisionService
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
    val photoIds: List<String>,
    val savedAlbumId: Int? = null
)

private data class PhotoCluster(
    val type: String,
    val hint: String,
    val photoIds: List<String>
)

private data class GeneratedMemory(
    val cluster: PhotoCluster,
    val photoIds: List<String>,
    val title: String,
    val caption: String
)

@Service
class MemoriesService(
    private val jdbcTemplate: JdbcTemplate,
    private val transactionManager: PlatformTransactionManager,
    private val memoryRepository: MemoryRepository,
    private val ollamaVisionService: OllamaVisionService
) {
    private val txTemplate = TransactionTemplate(transactionManager)
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

            val allClusters = buildClusters()
            logger.log(Level.INFO, "Found ${allClusters.size} candidate clusters")

            if (allClusters.isEmpty()) {
                logger.log(Level.WARNING, "No clusters found — keeping existing memories")
                return
            }

            val typeWeights = mapOf("person" to 6, "event" to 4, "location" to 3)
            val totalTarget = 15
            val totalWeight = typeWeights.values.sum().toDouble()
            val typeTargets = typeWeights.mapValues { (_, w) -> kotlin.math.round((w / totalWeight) * totalTarget).toInt() }
            val typeCounts = mutableMapOf("person" to 0, "event" to 0, "location" to 0)
            val usedMetadataIds = mutableSetOf<String>()
            val overflowClusters = mutableListOf<PhotoCluster>()
            val selected = mutableListOf<PhotoCluster>()

            // Pass 1: fill to proportional targets
            for (cluster in allClusters) {
                if (selected.size >= totalTarget) break
                val availableIds = cluster.photoIds.filter { it !in usedMetadataIds }
                if (availableIds.size < 5) continue
                val target = typeTargets[cluster.type] ?: 0
                if ((typeCounts[cluster.type] ?: 0) >= target) {
                    overflowClusters.add(cluster.copy(photoIds = availableIds))
                    continue
                }
                usedMetadataIds.addAll(availableIds.take(20))
                typeCounts[cluster.type] = (typeCounts[cluster.type] ?: 0) + 1
                selected.add(cluster.copy(photoIds = availableIds))
            }

            // Pass 2: fill remaining slots from overflow
            for (cluster in overflowClusters) {
                if (selected.size >= totalTarget) break
                val availableIds = cluster.photoIds.filter { it !in usedMetadataIds }
                if (availableIds.size < 5) continue
                usedMetadataIds.addAll(availableIds.take(20))
                selected.add(cluster.copy(photoIds = availableIds))
            }

            if (selected.isEmpty()) {
                logger.log(Level.WARNING, "No clusters had enough photos")
                return
            }

            // Ask Ollama vision to describe each cluster
            val generated = mutableListOf<GeneratedMemory>()
            val usedTitles = mutableListOf<String>()
            for (cluster in selected) {
                val description = ollamaVisionService.describeCluster(
                    sampleIds(cluster.photoIds.take(20), 4),
                    cluster.hint,
                    settings,
                    usedTitles.toList()
                )
                if (description == null) {
                    logger.log(Level.WARNING, "Ollama returned null for cluster \"${cluster.hint}\" — skipping")
                    continue
                }
                logger.log(Level.INFO, "Memory \"${description.first}\" (${cluster.type}): ${cluster.photoIds.size} photos")
                usedTitles.add(description.first)
                generated.add(GeneratedMemory(cluster, cluster.photoIds, description.first, description.second))
            }

            if (generated.isEmpty()) {
                logger.log(Level.WARNING, "No memories generated")
                return
            }

            swapMemories(generated)
            logger.log(Level.INFO, "Memories generation complete — ${generated.size} memories stored")
        } finally {
            generationRunning.set(false)
        }
    }

    private fun buildClusters(): List<PhotoCluster> {
        val clusters = mutableListOf<PhotoCluster>()
        clusters.addAll(buildPeopleClusters())
        clusters.addAll(buildDateClusters())
        clusters.addAll(buildPlaceClusters())
        return clusters
    }

    private fun buildPeopleClusters(): List<PhotoCluster> {
        val rows = jdbcTemplate.queryForList(
            """SELECT rl.name, GROUP_CONCAT(DISTINCT rlp.metadata_id) as ids, COUNT(DISTINCT rlp.metadata_id) as cnt
               FROM recognitionlabel rl
               JOIN recognitionlabelphoto rlp ON rl.id = rlp.recognition_label_id
               JOIN metadata m ON rlp.metadata_id = m.id
               WHERE CAST(rlp.confidence AS REAL) >= 70 AND m.hidden = 0
               GROUP BY rl.id
               HAVING cnt >= 5
               ORDER BY cnt DESC
               LIMIT 20"""
        )
        return rows.mapNotNull { row ->
            val name = row["name"]?.toString() ?: return@mapNotNull null
            val ids = row["ids"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: return@mapNotNull null
            if (ids.size < 5) return@mapNotNull null
            PhotoCluster("person", "Photos of $name", ids)
        }
    }

    private fun buildDateClusters(): List<PhotoCluster> {
        val rows = jdbcTemplate.queryForList(
            """SELECT year, month, day, GROUP_CONCAT(id) as ids, COUNT(*) as cnt
               FROM metadata
               WHERE hidden = 0 AND year IS NOT NULL AND year > 1970
                 AND month IS NOT NULL AND day IS NOT NULL
               GROUP BY year, month, day
               HAVING cnt >= 5
               ORDER BY cnt DESC
               LIMIT 40"""
        )
        return rows.mapNotNull { row ->
            val year = row["year"]?.toString() ?: return@mapNotNull null
            val month = row["month"]?.toString()?.toIntOrNull() ?: return@mapNotNull null
            val day = row["day"]?.toString() ?: return@mapNotNull null
            val ids = row["ids"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: return@mapNotNull null
            if (ids.size < 5) return@mapNotNull null
            PhotoCluster("event", "Photos from ${monthName(month)} $day, $year", ids)
        }
    }

    private fun buildPlaceClusters(): List<PhotoCluster> {
        val rows = jdbcTemplate.queryForList(
            """SELECT place_name, GROUP_CONCAT(id) as ids, COUNT(*) as cnt
               FROM metadata
               WHERE hidden = 0 AND place_name IS NOT NULL AND place_name != ''
               GROUP BY place_name
               HAVING cnt >= 5
               ORDER BY cnt DESC
               LIMIT 20"""
        )
        return rows.mapNotNull { row ->
            val place = row["place_name"]?.toString() ?: return@mapNotNull null
            val ids = row["ids"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: return@mapNotNull null
            if (ids.size < 5) return@mapNotNull null
            PhotoCluster("location", "Photos from $place", ids)
        }
    }

    private fun sampleIds(ids: List<String>, n: Int): List<String> {
        if (ids.size <= n) return ids
        if (n <= 1) return listOf(ids[0])
        return (0 until n).map { i -> ids[(i * (ids.size - 1)) / (n - 1)] }.distinct()
    }

    private fun monthName(month: Int) = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ).getOrElse(month - 1) { "$month" }

    private fun swapMemories(memories: List<GeneratedMemory>) {
        val ts = TextUtils.getCurrentTimestamp()
        txTemplate.execute {
            jdbcTemplate.update("DELETE FROM memoryphoto")
            jdbcTemplate.update("DELETE FROM memory")
            for (memory in memories) {
                jdbcTemplate.update(
                    "INSERT INTO memory (title, caption, strategy_type, generated_date, created_at, modified_at) VALUES (?, ?, ?, ?, ?, ?)",
                    memory.title, memory.caption, memory.cluster.type, ts, ts, ts
                )
                val memoryId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long::class.java)!!
                memory.photoIds.forEachIndexed { idx, metadataId ->
                    jdbcTemplate.update(
                        "INSERT INTO memoryphoto (memory_id, metadata_id, display_order, created_at, modified_at) VALUES (?, ?, ?, ?, ?)",
                        memoryId, metadataId, idx, ts, ts
                    )
                }
            }
        }
    }
}
