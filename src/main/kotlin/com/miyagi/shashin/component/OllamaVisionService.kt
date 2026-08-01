package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Keyword
import com.miyagi.shashin.model.KeywordPhoto
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.OllamaContext
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.KeywordPhotoRepository
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.OllamaContextRepository
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.util.NetworkUtils
import com.miyagi.shashin.util.TextUtils
import net.coobird.thumbnailator.Thumbnails
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.client.WebClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sqrt
import java.util.logging.Level
import java.util.logging.Logger

@Component
class OllamaVisionService(
    private val metadataRepository: MetadataRepository,
    private val keywordRepository: KeywordRepository,
    private val keywordPhotoRepository: KeywordPhotoRepository,
    private val jdbcTemplate: JdbcTemplate,
    transactionManager: PlatformTransactionManager,
    private val embeddingStore: EmbeddingStore? = null
) {
    private val txTemplate = TransactionTemplate(transactionManager)
    private val logger = Logger.getLogger(OllamaVisionService::class.simpleName)
    private val mapper = ObjectMapper()

    val embeddingRunning = AtomicBoolean(false)
    val embeddingProcessed = AtomicInteger(0)
    val embeddingTotal = AtomicInteger(0)

    private val imageCache = ConcurrentHashMap<String, String>(64)

    fun getVisionModels(ollamaUrl: String): List<String> {
        return try {
            val client = WebClient.create(ollamaUrl.trimEnd('/'))
            val resp = client.get().uri("/api/tags")
                .retrieve().bodyToMono(String::class.java)
                .block(Duration.ofSeconds(5)) ?: return emptyList()
            val json = mapper.readTree(resp)
            json["models"]?.filter { model ->
                model["capabilities"]?.any { it.asText() == "vision" } == true
            }?.map { it["name"].asText() } ?: emptyList()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not fetch Ollama models from $ollamaUrl: ${e.localizedMessage}")
            emptyList()
        }
    }

    fun hasEmbedModel(ollamaUrl: String): Boolean {
        return try {
            val client = WebClient.create(ollamaUrl.trimEnd('/'))
            val resp = client.get().uri("/api/tags")
                .retrieve().bodyToMono(String::class.java)
                .block(Duration.ofSeconds(5)) ?: return false
            val json = mapper.readTree(resp)
            json["models"]?.any { it["name"]?.asText()?.startsWith("nomic-embed-text") == true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun isOllamaAvailable(settings: Settings): Boolean {
        val url = settings.getOllamaUrl()?.trim() ?: return false
        if (url.isBlank()) return false
        val model = settings.getOllamaVisionModel()?.trim() ?: return false
        if (model.isBlank()) return false
        return getVisionModels(url).contains(model)
    }

    fun processMedia(metadata: Metadata, settings: Settings, rescan: Boolean = false,
                     ollamaReady: Boolean? = null, argusReady: Boolean? = null): Boolean {
        val hasDescription = !metadata.getDescription().isNullOrBlank()
        val hasKeywords = keywordPhotoRepository.countByMetadataId(metadata.getId()) > 0

        if (!rescan && hasDescription && hasKeywords) return false

        val useOllama = ollamaReady ?: isOllamaAvailable(settings)
        if (useOllama) {
            return processWithOllama(metadata, settings, rescan, hasDescription, hasKeywords)
        }

        // Argus fallback: keywords only, leave description alone
        val useArgus = argusReady ?: (settings.getObjectDetection() == true &&
            NetworkUtils.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey()))
        if (useArgus) {
            if (!rescan && hasKeywords) return false
            if (rescan) keywordPhotoRepository.deleteAllByMetadataId(metadata.getId())
            return ImageProcessing.Companion.detectAndStoreObjects(
                metadata, settings, keywordRepository, keywordPhotoRepository, metadataRepository,
                replace = rescan
            )
        }

        return false
    }

    private fun processWithOllama(
        metadata: Metadata,
        settings: Settings,
        rescan: Boolean,
        hasDescription: Boolean,
        hasKeywords: Boolean
    ): Boolean {
        val imagePath = ImageProcessing.Companion.argusImagePath(metadata) ?: return false
        val imageFile = File(imagePath)
        if (!imageFile.exists()) return false

        val imageB64 = try {
            encodeForOllama(imageFile)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Image encode failed for ${metadata.getId()}: ${e.localizedMessage}")
            return false
        }

        val prompt = "Describe this photo as an outside observer. " +
            "NEVER use I, me, my, mine, we, our, us — you are not in this photo and have no memories of it. " +
            "Do not invent dialogue, quotes, or personal memories. " +
            "Describe the people, place, activity, and mood warmly and vividly. " +
            "STRICT LIMIT: your description must be under 500 characters total. Aim for 400-480 characters. Stop writing when you approach 480 characters. " +
            "Don't start with 'This is a photo of'.\n\n" +
            "Then on a new line:\nKEYWORDS: word1,word2,word3"

        val messages = mutableListOf<Map<String, Any>>()
        buildSystemContext(metadata, false)?.let { messages.add(mapOf("role" to "system", "content" to it)) }
        messages.add(mapOf("role" to "user", "content" to "/no_think $prompt", "images" to listOf(imageB64)))

        val payload = mapper.writeValueAsString(mapOf(
            "model" to settings.getOllamaVisionModel()!!,
            "messages" to messages,
            "stream" to false,
            "keep_alive" to -1,
            "options" to mapOf("num_predict" to 180, "temperature" to 0.7)
        ))

        return try {
            val client = WebClient.builder()
                .baseUrl(settings.getOllamaUrl()!!.trimEnd('/'))
                .codecs { it.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) }
                .build()
            val response = client.post()
                .uri("/api/chat")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofSeconds(300)) ?: return false

            var content = mapper.readTree(response)["message"]?.get("content")?.asText() ?: return false
            // Strip Qwen3 thinking blocks
            content = content.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()

            val (rawDescription, keywords) = parseResponse(content)
            val description = if (rawDescription.length > 500) {
                val cut = rawDescription.take(500)
                val lastSentence = maxOf(cut.lastIndexOf('.'), cut.lastIndexOf('!'), cut.lastIndexOf('?'))
                if (lastSentence > 350) cut.take(lastSentence + 1)
                else cut.substringBeforeLast(' ').trimEnd()
            } else rawDescription

            var metadataDirty = false
            var embeddingDirty = false

            if (description.isNotBlank() && (rescan || !hasDescription)) {
                metadata.setDescription(description)
                metadataDirty = true
                val vec = embed(description, settings)
                if (vec != null) {
                    metadata.setEmbedding(mapper.writeValueAsString(vec.toList()))
                    embeddingDirty = true
                }
            }

            if (keywords.isNotEmpty() && (rescan || !hasKeywords)) {
                if (rescan) keywordPhotoRepository.deleteAllByMetadataId(metadata.getId())
                saveKeywords(keywords, metadata.getId())
                metadataDirty = true
            }

            if (metadataDirty) {
                val ts = TextUtils.getCurrentTimestamp()
                if (embeddingDirty) {
                    val desc = metadata.getDescription(); val emb = metadata.getEmbedding(); val id = metadata.getId()
                    txTemplate.execute { jdbcTemplate.update("UPDATE metadata SET description = ?, embedding = ?, modified_at = ? WHERE id = ?", desc, emb, ts, id) }
                    if (emb != null) embeddingStore?.update(id, emb)
                } else {
                    val desc = metadata.getDescription(); val id = metadata.getId()
                    txTemplate.execute { jdbcTemplate.update("UPDATE metadata SET description = ?, modified_at = ? WHERE id = ?", desc, ts, id) }
                }
            }

            logger.log(Level.INFO, "Ollama: ${metadata.getId()} → \"${description.take(60)}…\" | ${keywords.joinToString()} [saved]")
            true
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama error for ${metadata.getId()}: ${e.localizedMessage}")
            false
        }
    }

    private fun encodeForOllama(file: File): String {
        val buf = ByteArrayOutputStream()
        Thumbnails.of(file)
            .size(512, 512)
            .keepAspectRatio(true)
            .outputFormat("jpg")
            .toOutputStream(buf)
        return Base64.getEncoder().encodeToString(buf.toByteArray())
    }

    private fun parseResponse(text: String): Pair<String, List<String>> {
        val parts = text.split(Regex("\\nKEYWORDS\\s*:\\s*", RegexOption.IGNORE_CASE), limit = 2)
        val description = parts[0].trim()
        val keywords = if (parts.size >= 2)
            parts[1].split(",").map { it.trim().lowercase().replace("_", " ") }.filter { it.isNotBlank() }.take(10)
        else emptyList()
        return Pair(description, keywords)
    }

    fun isOllamaConfigured(settings: Settings): Boolean =
        !settings.getOllamaUrl().isNullOrBlank() && !settings.getOllamaVisionModel().isNullOrBlank()

    fun isEmbedConfigured(settings: Settings): Boolean =
        !settings.getOllamaUrl().isNullOrBlank()

    fun embed(text: String, settings: Settings): FloatArray? {
        val model = settings.getOllamaEmbedModel()?.takeIf { it.isNotBlank() } ?: "nomic-embed-text"
        val url = settings.getOllamaUrl()?.takeIf { it.isNotBlank() } ?: return null
        val payload = mapper.writeValueAsString(mapOf("model" to model, "input" to text, "keep_alive" to -1))
        return try {
            val client = WebClient.builder()
                .baseUrl(url.trimEnd('/'))
                .codecs { it.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
                .build()
            val response = client.post()
                .uri("/api/embed")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofSeconds(30)) ?: return null
            val node = mapper.readTree(response)
            val arr = node["embeddings"]?.get(0) ?: node["embedding"] ?: return null
            FloatArray(arr.size()) { arr[it].floatValue() }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama embed error: ${e.localizedMessage}")
            null
        }
    }

    fun embedImage(imageFile: File, settings: Settings): FloatArray? {
        val model = settings.getOllamaVisionModel()?.takeIf { it.isNotBlank() } ?: return null
        val url = settings.getOllamaUrl()?.takeIf { it.isNotBlank() } ?: return null
        val b64 = try { encodeForOllama(imageFile) } catch (e: Exception) { return null }
        val payload = mapper.writeValueAsString(mapOf("model" to model, "input" to "", "images" to listOf(b64), "keep_alive" to -1))
        return try {
            val client = WebClient.builder()
                .baseUrl(url.trimEnd('/'))
                .codecs { it.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) }
                .build()
            val response = client.post()
                .uri("/api/embed")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofSeconds(60)) ?: return null
            val node = mapper.readTree(response)
            val arr = node["embeddings"]?.get(0) ?: node["embedding"] ?: return null
            FloatArray(arr.size()) { arr[it].floatValue() }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama image embed error: ${e.localizedMessage}")
            null
        }
    }

    fun generateEmbeddingsBatch(settings: Settings, shouldStop: AtomicBoolean? = null) {
        if (!isEmbedConfigured(settings)) return
        if (embeddingRunning.get()) return
        embeddingRunning.set(true)
        embeddingProcessed.set(0)
        embeddingTotal.set(0)
        val ollamaReady = isOllamaAvailable(settings)
        val failed = mutableSetOf<String>()
        val done = mutableSetOf<String>()
        val seen = mutableSetOf<String>()
        try {
            val batchSize = 50
            while (true) {
                if (shouldStop?.get() == true) return
                val skip = failed.size + done.size
                val batch = metadataRepository.findAllWithNoEmbedding(batchSize + skip)
                    .filter { it.getId() !in failed && it.getId() !in done }
                    .take(batchSize)
                batch.forEach { m -> m.getId()?.let { if (seen.add(it)) embeddingTotal.incrementAndGet() } }
                if (batch.isEmpty()) break
                for (metadata in batch) {
                    if (shouldStop?.get() == true) return
                    val id = metadata.getId() ?: continue
                    val succeeded: Boolean
                    if (metadata.getDescription().isNullOrBlank()) {
                        if (!ollamaReady) { failed.add(id); continue }
                        val saved = processMedia(metadata, settings, rescan = false, ollamaReady = true, argusReady = false)
                        succeeded = saved && !metadata.getEmbedding().isNullOrBlank()
                    } else {
                        val vec = embed(metadata.getDescription()!!, settings)
                        if (vec != null) {
                            val emb = mapper.writeValueAsString(vec.toList()); val ts = TextUtils.getCurrentTimestamp()
                            txTemplate.execute { jdbcTemplate.update("UPDATE metadata SET embedding = ?, modified_at = ? WHERE id = ?", emb, ts, id) }
                            embeddingStore?.update(id, emb)
                            succeeded = true
                        } else {
                            succeeded = false
                        }
                    }
                    if (succeeded) {
                        val n = embeddingProcessed.incrementAndGet()
                        logger.log(Level.INFO, "[$n / ${embeddingTotal.get()}] ${metadata.getId()}")
                        done.add(id)
                    } else failed.add(id)
                }
            }
            logger.log(Level.INFO, "Embedding generation complete")
        } finally {
            embeddingRunning.set(false)
        }
    }

    fun ask(metadata: Metadata, question: String, settings: Settings, contextRepository: OllamaContextRepository?): String? {
        val metadataId = metadata.getId() ?: return null
        val imagePath = ImageProcessing.Companion.argusImagePath(metadata) ?: return null
        val imageFile = File(imagePath)
        if (!imageFile.exists()) return null

        // Option 2: use cached base64, encode from disk only on first access
        val imageB64 = imageCache.getOrPut(metadataId) {
            try { encodeForOllama(imageFile) } catch (e: Exception) { return null }
        }.also { if (imageCache.size > 50) imageCache.keys.take(imageCache.size - 50).forEach(imageCache::remove) }

        // load stored context tokens; discard if model changed
        val storedCtx = contextRepository?.findByMetadataId(metadataId)
        val ctxValid = storedCtx != null && storedCtx.getModel() == settings.getOllamaVisionModel()

        // Option 1: image always in the current prompt only, not in history
        val payload = mutableMapOf<String, Any>(
            "model" to settings.getOllamaVisionModel()!!,
            "prompt" to "/no_think $question",
            "images" to listOf(imageB64),
            "stream" to false,
            "keep_alive" to -1,
            "options" to mapOf("num_predict" to 400)
        )
        if (ctxValid) {
            payload["context"] = mapper.readTree(storedCtx!!.getContext())
        } else {
            buildSystemContext(metadata, true)?.let { payload["system"] = it }
        }

        return try {
            val client = WebClient.builder()
                .baseUrl(settings.getOllamaUrl()!!.trimEnd('/'))
                .codecs { it.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) }
                .build()
            val response = client.post()
                .uri("/api/generate")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(mapper.writeValueAsString(payload))
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofSeconds(120)) ?: return null

            val responseNode = mapper.readTree(response)
            var content = responseNode["response"]?.asText() ?: return null
            content = content.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()

            // Save updated context tokens back to DB
            val newContextNode = responseNode["context"]
            if (newContextNode != null && contextRepository != null) {
                val ctxObj = (if (ctxValid) storedCtx else null) ?: OllamaContext().also { it.setMetadataId(metadataId) }
                ctxObj.setModel(settings.getOllamaVisionModel())
                ctxObj.setContext(mapper.writeValueAsString(newContextNode))
                ctxObj.setUpdatedAt(TextUtils.getCurrentTimestamp())
                contextRepository.save(ctxObj)
            }

            content
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama ask error for $metadataId: ${e.localizedMessage}")
            null
        }
    }

    fun askStream(
        metadata: Metadata,
        question: String,
        settings: Settings,
        contextRepository: OllamaContextRepository?,
        onToken: (String) -> Unit
    ): Boolean {
        val metadataId = metadata.getId() ?: return false
        val ollamaUrl = settings.getOllamaUrl()?.trimEnd('/') ?: return false
        val imagePath = ImageProcessing.Companion.argusImagePath(metadata) ?: return false
        val imageFile = File(imagePath)
        if (!imageFile.exists()) return false

        val imageB64 = imageCache.getOrPut(metadataId) {
            try { encodeForOllama(imageFile) } catch (e: Exception) { return false }
        }.also { if (imageCache.size > 50) imageCache.keys.take(imageCache.size - 50).forEach(imageCache::remove) }

        val storedCtx = contextRepository?.findByMetadataId(metadataId)
        val ctxValid = storedCtx != null && storedCtx.getModel() == settings.getOllamaVisionModel()

        val payload = mutableMapOf<String, Any>(
            "model" to settings.getOllamaVisionModel()!!,
            "prompt" to "/no_think $question",
            "images" to listOf(imageB64),
            "stream" to true,
            "keep_alive" to -1,
            "options" to mapOf("num_predict" to 400)
        )
        if (ctxValid) {
            payload["context"] = mapper.readTree(storedCtx!!.getContext())
        } else {
            buildSystemContext(metadata, true)?.let { payload["system"] = it }
        }

        return try {
            val conn = java.net.URL("$ollamaUrl/api/generate").openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
            conn.outputStream.use { it.write(mapper.writeValueAsBytes(payload)) }

            conn.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    try {
                        val node = mapper.readTree(line)
                        val token = node["response"]?.asText() ?: ""
                        if (token.isNotEmpty()) onToken(token)
                        if (node["done"]?.asBoolean() == true) {
                            val ctxNode = node["context"]
                            if (ctxNode != null && contextRepository != null) {
                                val ctxObj = (if (ctxValid) storedCtx else null)
                                    ?: OllamaContext().also { it.setMetadataId(metadataId) }
                                ctxObj.setModel(settings.getOllamaVisionModel())
                                ctxObj.setContext(mapper.writeValueAsString(ctxNode))
                                ctxObj.setUpdatedAt(TextUtils.getCurrentTimestamp())
                                contextRepository.save(ctxObj)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            true
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama ask stream error for $metadataId: ${e.localizedMessage}")
            false
        }
    }

    private fun buildSystemContext(metadata: Metadata, includeDescriptionAndKeywords: Boolean): String? {
        val parts = mutableListOf<String>()
        val place = metadata.getPlaceName()?.takeIf { it.isNotBlank() }
        val lat = metadata.getLat()?.takeIf { it.isNotBlank() }
        val lng = metadata.getLng()?.takeIf { it.isNotBlank() }
        if (place != null) {
            val coords = if (lat != null && lng != null) " ($lat, $lng)" else ""
            parts.add("Location: $place$coords")
        } else if (lat != null && lng != null) {
            parts.add("Coordinates: $lat, $lng")
        }
        val albums = jdbcTemplate.queryForList(
            "SELECT a.name FROM album a JOIN albumphoto ap ON a.id = ap.album_id WHERE ap.metadata_id = ?",
            String::class.java, metadata.getId()
        )
        if (albums.isNotEmpty()) parts.add("Albums: ${albums.joinToString(", ")}")
        if (includeDescriptionAndKeywords) {
            metadata.getDescription()?.takeIf { it.isNotBlank() }?.let { parts.add("Description: $it") }
            val keywords = jdbcTemplate.queryForList(
                "SELECT k.keyword FROM keyword k JOIN keywordphoto kp ON k.id = kp.keyword_id WHERE kp.metadata_id = ?",
                String::class.java, metadata.getId()
            )
            if (keywords.isNotEmpty()) parts.add("Keywords: ${keywords.joinToString(", ")}")
        }
        return if (parts.isNotEmpty()) parts.joinToString("\n") else null
    }

    private fun saveKeywords(keywords: List<String>, metadataId: String) {
        for (term in keywords) {
            if (term.isBlank()) continue
            var kwObj = keywordRepository.findByKeywordIgnoreCase(term)
            if (kwObj == null) {
                kwObj = Keyword()
                kwObj.setKeyword(term)
                kwObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                kwObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                keywordRepository.save(kwObj)
            }
            if (keywordPhotoRepository.countByKeywordIdAndMetadataId(kwObj.getId(), metadataId) == 0) {
                val kp = KeywordPhoto()
                kp.setKeywordId(kwObj.getId())
                kp.setMetadataId(metadataId)
                kp.setCreatedAt(TextUtils.getCurrentTimestamp())
                kp.setModifiedAt(TextUtils.getCurrentTimestamp())
                keywordPhotoRepository.save(kp)
            }
        }
    }

    private val genericPersonLabels = setOf(
        "boy", "girl", "man", "woman", "child", "children", "kid", "kids",
        "baby", "babies", "person", "people", "toddler", "teen", "teenager", "infant"
    )

    private fun containsGenericLabel(text: String): Boolean {
        val lower = text.lowercase()
        return genericPersonLabels.any { label -> Regex("\\b${Regex.escape(label)}('s)?\\b").containsMatchIn(lower) }
    }

    private fun chatForCluster(client: WebClient, model: String, messages: List<Map<String, Any>>): String? {
        val payload = mapper.writeValueAsString(mapOf(
            "model" to model,
            "messages" to messages,
            "stream" to false,
            "format" to "json",
            "keep_alive" to -1,
            "options" to mapOf("num_predict" to 120, "temperature" to 0.7, "num_ctx" to 8192)
        ))
        val response = client.post()
            .uri("/api/chat")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String::class.java)
            .block(Duration.ofSeconds(120)) ?: return null
        var content = mapper.readTree(response)["message"]?.get("content")?.asText() ?: return null
        return content.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()
    }

    fun describeCluster(sampleIds: List<String>, hint: String, settings: Settings, avoidTitles: List<String> = emptyList()): Pair<String, String>? {
        val model = settings.getOllamaVisionModel() ?: return null
        val ollamaUrl = settings.getOllamaUrl()?.trimEnd('/') ?: return null

        val images = sampleIds.mapNotNull { id ->
            val metadata = metadataRepository.findById(id).orElse(null) ?: return@mapNotNull null
            val imagePath = ImageProcessing.Companion.argusImagePath(metadata) ?: return@mapNotNull null
            val imageFile = File(imagePath)
            if (!imageFile.exists()) return@mapNotNull null
            try { encodeForOllama(imageFile) } catch (e: Exception) { null }
        }
        if (images.isEmpty()) return null

        val systemPrompt = "You are labeling a personal photo memory made up of multiple photos. " +
            "Write a short, direct title (2-5 words) and one plain sentence as a caption. " +
            "The title and caption must describe what the photos share in common — the overall occasion, setting, or people — not any single photo. " +
            "Use the date, place, or person name in the title when it fits. " +
            "IMPORTANT: Never use generic labels for people. Forbidden words: boy, girl, man, woman, child, children, kid, kids, baby, babies, person, people, toddler, teen, teenager, infant. " +
            "Only refer to people by their actual name if a name is provided in the context. " +
            "If no name is given, describe the place, occasion, or activity instead. " +
            "Do not use flowery, poetic, or metaphorical language. " +
            "Bad titles: \"Tiny Explorer's Day\", \"Boy Playing Outside\", \"Baby's First Christmas\", \"Baby at Home\". " +
            "Good titles: \"Trip to Cultus Lake\", \"Christmas 2019\", \"Noah at the Park\", \"Wedding Day\". " +
            "Bad captions: \"A young boy in pajamas plays on the deck.\" (uses a generic label). " +
            "Good captions: \"Photos from Christmas morning at home.\" (no generic labels, describes the whole set). " +
            "Respond ONLY with JSON: {\"title\": \"...\", \"caption\": \"...\"}"

        val userContent = buildString {
            append("/no_think Context: $hint.")
            if (avoidTitles.isNotEmpty()) append(" Avoid titles similar to: ${avoidTitles.joinToString(", ")}.")
            append(" Describe what all these photos have in common, not any single one.")
        }

        val messages = mutableListOf<Map<String, Any>>(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to userContent, "images" to images)
        )

        return try {
            val client = WebClient.builder()
                .baseUrl(ollamaUrl)
                .codecs { it.defaultCodecs().maxInMemorySize(20 * 1024 * 1024) }
                .build()

            var content = chatForCluster(client, model, messages) ?: return null
            var node = mapper.readTree(content)
            var title = node["title"]?.asText()?.trim()?.takeIf { it.isNotBlank() } ?: return null
            var caption = node["caption"]?.asText()?.trim() ?: ""

            if (containsGenericLabel(title) || containsGenericLabel(caption)) {
                logger.log(Level.INFO, "Retrying cluster description — generic label detected in \"$title\"")
                val retryMessages = messages + listOf(
                    mapOf("role" to "assistant", "content" to content),
                    mapOf("role" to "user", "content" to
                        "Your title \"$title\" uses a forbidden generic person label (boy, girl, baby, child, kid, man, woman, etc.). " +
                        "Rewrite the title and caption. Do NOT use any of these words. " +
                        "Focus on the place, date, or activity instead of who the people are. " +
                        "Respond ONLY with JSON: {\"title\": \"...\", \"caption\": \"...\"}")
                )
                val retryContent = chatForCluster(client, model, retryMessages)
                if (retryContent != null) {
                    try {
                        val retryNode = mapper.readTree(retryContent)
                        val retryTitle = retryNode["title"]?.asText()?.trim()?.takeIf { it.isNotBlank() }
                        val retryCaption = retryNode["caption"]?.asText()?.trim() ?: ""
                        if (retryTitle != null) { title = retryTitle; caption = retryCaption }
                    } catch (_: Exception) {}
                }
            }

            Pair(title, caption)
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.log(Level.WARNING, "Ollama describeCluster error for \"$hint\": ${e.localizedMessage} — body: ${e.responseBodyAsString}")
            null
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama describeCluster error for \"$hint\": ${e.localizedMessage}")
            null
        }
    }

    fun chatText(systemPrompt: String, userPrompt: String, settings: Settings): String? {
        val ollamaUrl = settings.getOllamaUrl()?.trimEnd('/') ?: return null
        val model = settings.getOllamaVisionModel() ?: return null
        if (ollamaUrl.isBlank() || model.isBlank()) return null

        val messages = listOf(
            mapOf("role" to "system", "content" to systemPrompt),
            mapOf("role" to "user", "content" to "/no_think $userPrompt")
        )
        val payload = mapper.writeValueAsString(mapOf(
            "model" to model,
            "messages" to messages,
            "stream" to false,
            "format" to "json",
            "keep_alive" to -1,
            "options" to mapOf("num_predict" to 2048, "temperature" to 0.8)
        ))

        return try {
            val client = WebClient.builder()
                .baseUrl(ollamaUrl)
                .codecs { it.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
                .build()
            val response = client.post()
                .uri("/api/chat")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofSeconds(120)) ?: return null
            var content = mapper.readTree(response)["message"]?.get("content")?.asText() ?: return null
            content = content.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()
            content
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama chatText error: ${e.localizedMessage}")
            null
        }
    }

    fun processItems(metadataIds: List<String>, settings: Settings) {
        val ollamaReady = isOllamaAvailable(settings)
        val argusReady = settings.getObjectDetection() == true &&
            NetworkUtils.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())
        if (!ollamaReady && !argusReady) return

        for (id in metadataIds) {
            val metadata = metadataRepository.findById(id).orElse(null) ?: continue
            processMedia(metadata, settings, false, ollamaReady, argusReady)
        }
    }

    fun processMissingBatch(settings: Settings, shouldStop: AtomicBoolean? = null) {
        val ollamaReady = isOllamaAvailable(settings)
        val argusReady = settings.getObjectDetection() == true &&
            NetworkUtils.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())
        if (!ollamaReady && !argusReady) return

        for (mediaType in listOf("image", "video")) {
            for (query in listOf(
                { metadataRepository.findAllMissingDescriptionByTypeOffsetAndLimit(mediaType, 0, 50).toList() },
                { metadataRepository.findAllMissingKeywordsByTypeOffsetAndLimit(mediaType, 0, 50).toList() }
            )) {
                while (true) {
                    if (shouldStop?.get() == true) return
                    val items = query()
                    if (items.isEmpty()) break
                    var processed = 0
                    for (item in items) {
                        if (shouldStop?.get() == true) break
                        if (processMedia(item, settings, false, ollamaReady, argusReady)) processed++
                    }
                    if (processed == 0) break
                }
            }
        }
    }
}
