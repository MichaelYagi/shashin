package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.sqrt

@Component
class EmbeddingStore(private val jdbcTemplate: JdbcTemplate) {
    private val store = ConcurrentHashMap<String, FloatArray>()
    private val mapper = ObjectMapper()
    private val logger = Logger.getLogger(EmbeddingStore::class.simpleName)

    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        Thread {
            try {
                val rows = jdbcTemplate.queryForList(
                    "SELECT id, embedding FROM metadata WHERE embedding IS NOT NULL AND embedding != ''"
                )
                var count = 0
                for (row in rows) {
                    val id = row["id"] as? String ?: continue
                    val emb = row["embedding"] as? String ?: continue
                    parseAndNormalize(emb)?.let { store[id] = it; count++ }
                }
                logger.log(Level.INFO, "EmbeddingStore: loaded $count embeddings")
            } catch (e: Exception) {
                logger.log(Level.WARNING, "EmbeddingStore load error: ${e.localizedMessage}")
            }
        }.also { it.isDaemon = true; it.name = "embedding-store-init" }.start()
    }

    fun update(id: String, embeddingJson: String) {
        parseAndNormalize(embeddingJson)?.let { store[id] = it }
    }

    fun remove(id: String) {
        store.remove(id)
    }

    fun size(): Int = store.size

    fun getAll(ids: Collection<String>): Map<String, FloatArray> =
        ids.mapNotNull { id -> store[id]?.let { id to it } }.toMap()

    fun search(queryVec: FloatArray, size: Int): List<Pair<String, Float>> {
        val normQ = normalize(queryVec) ?: return emptyList()
        return store.entries
            .map { (id, vec) -> Pair(id, dot(normQ, vec)) }
            .sortedByDescending { it.second }
            .take(size)
    }

    private fun parseAndNormalize(json: String): FloatArray? {
        return try {
            val node = mapper.readTree(json)
            normalize(FloatArray(node.size()) { node[it].floatValue() })
        } catch (_: Exception) { null }
    }

    private fun normalize(v: FloatArray): FloatArray? {
        var norm = 0f
        for (x in v) norm += x * x
        norm = sqrt(norm)
        if (norm == 0f) return null
        return FloatArray(v.size) { v[it] / norm }
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        val len = minOf(a.size, b.size)
        for (i in 0 until len) sum += a[i] * b[i]
        return sum
    }
}
