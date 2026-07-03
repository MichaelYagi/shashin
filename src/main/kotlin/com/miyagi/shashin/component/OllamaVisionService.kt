package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Keyword
import com.miyagi.shashin.model.KeywordPhoto
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.KeywordPhotoRepository
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.util.NetworkUtils
import com.miyagi.shashin.util.TextUtils
import net.coobird.thumbnailator.Thumbnails
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

@Component
class OllamaVisionService(
    private val metadataRepository: MetadataRepository,
    private val keywordRepository: KeywordRepository,
    private val keywordPhotoRepository: KeywordPhotoRepository
) {
    private val logger = Logger.getLogger(OllamaVisionService::class.simpleName)
    private val mapper = ObjectMapper()

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

        val place = metadata.getPlaceName()
        val locationHint = if (!place.isNullOrBlank())
            "The photo's metadata indicates it was taken at: $place. Include this naturally if relevant.\n\n"
        else ""

        val prompt = "${locationHint}Describe this photo briefly and naturally in under 500 characters. " +
            "Don't start with 'This is a photo of' or similar.\n\n" +
            "Then on a new line:\nKEYWORDS: word1,word2,word3"

        val payload = mapper.writeValueAsString(mapOf(
            "model" to settings.getOllamaVisionModel()!!,
            "messages" to listOf(mapOf(
                "role" to "user",
                "content" to "/no_think $prompt",
                "images" to listOf(imageB64)
            )),
            "stream" to false
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

            val (description, keywords) = parseResponse(content)

            if (description.isNotBlank() && (rescan || !hasDescription)) {
                metadata.setDescription(description)
                metadata.setModifiedAt(TextUtils.getCurrentTimestamp())
                metadataRepository.save(metadata)
            }

            if (keywords.isNotEmpty() && (rescan || !hasKeywords)) {
                if (rescan) keywordPhotoRepository.deleteAllByMetadataId(metadata.getId())
                saveKeywords(keywords, metadata.getId())
            }

            logger.log(Level.INFO, "Ollama: ${metadata.getId()} → \"${description.take(60)}…\" | ${keywords.joinToString()}")
            true
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Ollama error for ${metadata.getId()}: ${e.localizedMessage}")
            false
        }
    }

    private fun encodeForOllama(file: File): String {
        val buf = ByteArrayOutputStream()
        Thumbnails.of(file)
            .size(1024, 1024)
            .keepAspectRatio(true)
            .outputFormat("jpg")
            .toOutputStream(buf)
        return Base64.getEncoder().encodeToString(buf.toByteArray())
    }

    private fun parseResponse(text: String): Pair<String, List<String>> {
        val parts = text.split(Regex("\\nKEYWORDS\\s*:\\s*", RegexOption.IGNORE_CASE), limit = 2)
        val description = parts[0].trim().take(500)
        val keywords = if (parts.size >= 2)
            parts[1].split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.take(10)
        else emptyList()
        return Pair(description, keywords)
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
                        if (item == null || shouldStop?.get() == true) break
                        if (processMedia(item, settings, false, ollamaReady, argusReady)) processed++
                    }
                    if (processed == 0) break
                }
            }
        }
    }
}
