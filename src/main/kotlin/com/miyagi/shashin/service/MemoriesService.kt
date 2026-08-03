package com.miyagi.shashin.service

import com.miyagi.shashin.component.EmbeddingStore
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
    private val ollamaVisionService: OllamaVisionService,
    private val embeddingStore: EmbeddingStore
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
        }.sortedBy { if (it.strategyType == "occasion") 1 else 0 }
    }

    fun generateMemories(settings: Settings) {
        if (settings.getShowMemories() != true) return
        if (!ollamaVisionService.isOllamaConfigured(settings)) return
        if (!ollamaVisionService.isOllamaReachable(settings)) {
            logger.log(Level.WARNING, "Memories generation skipped — Ollama is not reachable")
            return
        }
        if (!generationRunning.compareAndSet(false, true)) return

        try {
            logger.log(Level.INFO, "Memories generation started")

            val allClusters = buildClusters()
            logger.log(Level.INFO, "Found ${allClusters.size} candidate clusters")

            if (allClusters.isEmpty()) {
                logger.log(Level.WARNING, "No clusters found — keeping existing memories")
                return
            }

            val totalTarget = 16

            // Exclude photos already shown so each regeneration surfaces fresh content.
            // Fall back to empty exclusion if not enough fresh clusters exist.
            val shownIds = jdbcTemplate.queryForList(
                "SELECT metadata_id FROM memoryphoto", String::class.java
            ).toMutableSet()
            var usedMetadataIds = shownIds.toMutableSet()
            val qualifyingCount = allClusters.count { c ->
                c.photoIds.count { it !in usedMetadataIds } >= 5
            }
            if (qualifyingCount < totalTarget) {
                logger.log(Level.INFO, "Not enough fresh clusters ($qualifyingCount), resetting exclusion list")
                usedMetadataIds = mutableSetOf()
            }

            val selected = mutableListOf<PhotoCluster>()

            for (cluster in allClusters) {
                if (selected.size >= totalTarget) break
                val availableIds = cluster.photoIds.filter { it !in usedMetadataIds }
                if (availableIds.size < 5) continue
                usedMetadataIds.addAll(availableIds.take(12))
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
                val memoryPhotos = sampleIds(cluster.photoIds, (6..12).random())
                val ollamaPhotos = sampleIds(memoryPhotos, 6)
                val description = ollamaVisionService.describeCluster(
                    ollamaPhotos,
                    cluster.hint,
                    settings,
                    usedTitles.toList()
                )
                if (description == null) {
                    logger.log(Level.WARNING, "Ollama returned null for cluster \"${cluster.hint}\" — skipping")
                    continue
                }
                logger.log(Level.INFO, "Memory \"${description.first}\" (${cluster.type}): ${memoryPhotos.size} photos")
                usedTitles.add(description.first)
                generated.add(GeneratedMemory(cluster, memoryPhotos, description.first, description.second))
                try { Thread.sleep(1000) } catch (_: InterruptedException) {}
            }

            if (generated.isEmpty()) {
                logger.log(Level.WARNING, "No memories generated")
                return
            }

            val deduplicated = generated.distinctBy { it.title.trim().lowercase() }
            swapMemories(deduplicated)
            logger.log(Level.INFO, "Memories generation complete — ${deduplicated.size} memories stored")
        } finally {
            generationRunning.set(false)
        }
    }

    private fun buildClusters(): List<PhotoCluster> {
        val occasion = buildOccasionClusters().map { it.copy(photoIds = it.photoIds.shuffled()) }.shuffled().toMutableList()
        val longitudinal = (buildSoloLongitudinalClusters() + buildPairLongitudinalClusters()).shuffled().toMutableList()
        // Interleave 1:1 so person/pair memories always compete on equal footing with occasion.
        // Longitudinal is naturally capped by how many qualify; occasion fills the rest.
        val result = mutableListOf<PhotoCluster>()
        while (occasion.isNotEmpty() || longitudinal.isNotEmpty()) {
            if (longitudinal.isNotEmpty()) result.add(longitudinal.removeFirst())
            if (occasion.isNotEmpty()) result.add(occasion.removeFirst())
        }
        return result
    }

    // DBSCAN on per-day embedding groups. Each cluster = a coherent occasion within a day.
    private fun buildOccasionClusters(): List<PhotoCluster> {
        val days = jdbcTemplate.queryForList(
            """SELECT year, month, day, GROUP_CONCAT(id) as ids
               FROM metadata
               WHERE hidden = 0 AND year IS NOT NULL AND year > 1970
                 AND month IS NOT NULL AND day IS NOT NULL
                 AND embedding IS NOT NULL AND embedding != ''
               GROUP BY year, month, day
               HAVING COUNT(*) >= 5
               ORDER BY RANDOM()
               LIMIT 120"""
        )

        val result = mutableListOf<PhotoCluster>()
        for (row in days) {
            if (result.size >= 25) break
            val year = row["year"]?.toString() ?: continue
            val month = row["month"]?.toString()?.toIntOrNull() ?: continue
            val day = row["day"]?.toString() ?: continue
            val ids = row["ids"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: continue

            val vecs = embeddingStore.getAll(ids)
            if (vecs.size < 5) continue

            val idsWithVecs = ids.filter { it in vecs }.let {
                if (it.size > 40) it.shuffled().take(40) else it
            }
            try { Thread.sleep(100) } catch (_: InterruptedException) {}
            val clusters = dbscan(idsWithVecs, vecs, epsilon = 0.35f, minPts = 3)

            for (cluster in clusters) {
                if (cluster.size < 5) continue
                val coherent = filterByCoherence(cluster, vecs)
                if (coherent.size < 5) continue
                val kws = topKeywords(coherent)
                val place = topPlaceName(coherent)
                val hint = buildString {
                    append("Photos from ${monthName(month)} $year")
                    if (place != null) append(" at $place")
                    if (kws.isNotBlank()) append(" ($kws)")
                }
                result.add(PhotoCluster("occasion", hint, coherent))
            }
        }
        return result
    }

    // One recognized person appearing across ≥ 2 years.
    private fun buildSoloLongitudinalClusters(): List<PhotoCluster> {
        val rows = jdbcTemplate.queryForList(
            """SELECT rl.id as label_id, rl.name,
                 COUNT(DISTINCT rlp.metadata_id) as cnt,
                 COUNT(DISTINCT m.year) as year_span
               FROM recognitionlabel rl
               JOIN recognitionlabelphoto rlp ON rl.id = rlp.recognition_label_id
               JOIN metadata m ON rlp.metadata_id = m.id
               WHERE m.hidden = 0
               GROUP BY rl.id
               HAVING cnt >= 10 AND year_span >= 2
               ORDER BY RANDOM()
               LIMIT 40"""
        )
        return rows.mapNotNull { row ->
            val name = row["name"]?.toString() ?: return@mapNotNull null
            val labelId = row["label_id"]?.toString() ?: return@mapNotNull null
            val ids = jdbcTemplate.queryForList(
                """SELECT rlp.metadata_id FROM recognitionlabelphoto rlp
                   JOIN metadata m ON rlp.metadata_id = m.id
                   WHERE rlp.recognition_label_id = ?
                     AND m.hidden = 0
                   ORDER BY m.year, m.month, m.day""",
                String::class.java, labelId
            )
            if (ids.size < 10) return@mapNotNull null
            PhotoCluster("person", "$name over the years", ids)
        }
    }

    // Two recognized people co-occurring across ≥ 2 years.
    private fun buildPairLongitudinalClusters(): List<PhotoCluster> {
        val rows = jdbcTemplate.queryForList(
            """SELECT rl1.id as id1, rl1.name as name1, rl2.id as id2, rl2.name as name2,
                 COUNT(DISTINCT rlp1.metadata_id) as cnt,
                 COUNT(DISTINCT m.year) as year_span
               FROM recognitionlabel rl1
               JOIN recognitionlabelphoto rlp1 ON rl1.id = rlp1.recognition_label_id
               JOIN recognitionlabelphoto rlp2 ON rlp1.metadata_id = rlp2.metadata_id
               JOIN recognitionlabel rl2 ON rlp2.recognition_label_id = rl2.id
               JOIN metadata m ON rlp1.metadata_id = m.id
               WHERE rl1.id < rl2.id
                 AND m.hidden = 0
               GROUP BY rl1.id, rl2.id
               HAVING cnt >= 8 AND year_span >= 2
               ORDER BY RANDOM()
               LIMIT 40"""
        )
        return rows.mapNotNull { row ->
            val name1 = row["name1"]?.toString() ?: return@mapNotNull null
            val name2 = row["name2"]?.toString() ?: return@mapNotNull null
            val id1 = row["id1"]?.toString() ?: return@mapNotNull null
            val id2 = row["id2"]?.toString() ?: return@mapNotNull null
            val ids = jdbcTemplate.queryForList(
                """SELECT DISTINCT rlp1.metadata_id FROM recognitionlabelphoto rlp1
                   JOIN recognitionlabelphoto rlp2 ON rlp1.metadata_id = rlp2.metadata_id
                   JOIN metadata m ON rlp1.metadata_id = m.id
                   WHERE rlp1.recognition_label_id = ? AND rlp2.recognition_label_id = ?
                     AND m.hidden = 0
                   ORDER BY m.year, m.month, m.day""",
                String::class.java, id1, id2
            )
            if (ids.size < 8) return@mapNotNull null
            PhotoCluster("pair", "$name1 and $name2 over the years", ids)
        }
    }

    private fun dbscan(ids: List<String>, vecs: Map<String, FloatArray>, epsilon: Float, minPts: Int): List<List<String>> {
        val n = ids.size
        val labels = IntArray(n) { UNVISITED }
        var clusterId = 0

        fun neighbors(i: Int): List<Int> {
            val vi = vecs[ids[i]] ?: return emptyList()
            return (0 until n).filter { j ->
                if (j == i) return@filter false
                val vj = vecs[ids[j]] ?: return@filter false
                cosineDist(vi, vj) <= epsilon
            }
        }

        for (i in 0 until n) {
            if (labels[i] != UNVISITED) continue
            val nbrs = neighbors(i)
            if (nbrs.size < minPts - 1) { labels[i] = NOISE; continue }
            labels[i] = clusterId
            val seeds = nbrs.toMutableList()
            var s = 0
            while (s < seeds.size) {
                val q = seeds[s++]
                if (labels[q] == NOISE) labels[q] = clusterId
                if (labels[q] != UNVISITED) continue
                labels[q] = clusterId
                val qNbrs = neighbors(q)
                if (qNbrs.size >= minPts - 1) seeds.addAll(qNbrs.filterNot { it in seeds })
            }
            clusterId++
        }

        return (0 until clusterId).map { cid -> ids.filterIndexed { i, _ -> labels[i] == cid } }
    }

    private fun cosineDist(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        val len = minOf(a.size, b.size)
        for (i in 0 until len) dot += a[i] * b[i]
        return 1f - dot  // vectors are pre-normalized in EmbeddingStore
    }

    // Remove photos that are outliers relative to the cluster centroid.
    // DBSCAN chaining can pull in bridging photos that connect two unrelated groups;
    // filtering by centroid distance catches those after the fact.
    private fun filterByCoherence(ids: List<String>, vecs: Map<String, FloatArray>, threshold: Float = 0.35f): List<String> {
        if (ids.size < 2) return ids
        val dim = vecs.values.first().size
        val centroid = FloatArray(dim)
        var count = 0
        for (id in ids) {
            val v = vecs[id] ?: continue
            for (i in 0 until minOf(dim, v.size)) centroid[i] += v[i]
            count++
        }
        if (count == 0) return ids
        for (i in 0 until dim) centroid[i] /= count
        var norm = 0f
        for (x in centroid) norm += x * x
        norm = kotlin.math.sqrt(norm)
        if (norm == 0f) return ids
        val normCentroid = FloatArray(dim) { centroid[it] / norm }
        return ids.filter { id ->
            val v = vecs[id] ?: return@filter false
            cosineDist(normCentroid, v) <= threshold
        }
    }

    private fun topPlaceName(ids: List<String>): String? {
        if (ids.isEmpty()) return null
        val sample = ids.take(50)
        val placeholders = sample.joinToString(",") { "?" }
        return try {
            jdbcTemplate.queryForList(
                "SELECT place_name FROM metadata WHERE id IN ($placeholders) AND place_name IS NOT NULL AND place_name != '' GROUP BY place_name ORDER BY COUNT(*) DESC LIMIT 1",
                String::class.java, *sample.toTypedArray()
            ).firstOrNull()
        } catch (_: Exception) { null }
    }

    private fun topKeywords(ids: List<String>, limit: Int = 3): String {
        if (ids.isEmpty()) return ""
        val sample = ids.take(50)
        val placeholders = sample.joinToString(",") { "?" }
        return try {
            jdbcTemplate.queryForList(
                "SELECT k.keyword FROM keyword k JOIN keywordphoto kp ON k.id = kp.keywordId WHERE kp.metadataId IN ($placeholders) GROUP BY k.id ORDER BY COUNT(*) DESC LIMIT $limit",
                String::class.java, *sample.toTypedArray()
            ).joinToString(", ")
        } catch (_: Exception) { "" }
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

    companion object {
        private const val UNVISITED = Int.MIN_VALUE
        private const val NOISE = -1
    }
}
