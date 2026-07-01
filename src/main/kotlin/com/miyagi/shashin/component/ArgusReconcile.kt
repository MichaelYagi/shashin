package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.RecognitionLabel
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.RecognitionLabelPhotoRepository
import com.miyagi.shashin.repository.RecognitionLabelRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.logging.Level
import java.util.logging.Logger

@Component
class ArgusReconcile(
    private var recognitionLabelRepository: RecognitionLabelRepository? = null,
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null,
    private var metadataRepository: MetadataRepository? = null
) {
    private val logger: Logger = Logger.getLogger(ArgusReconcile::class.simpleName)
    private val mapper = ObjectMapper()

    fun run(settings: Settings) {
        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return

        try {
            val webClient = WebClient.create(settings.getArgusServer()!!)
            val argusServer = settings.getArgusServer()!!.trimEnd('/')

            val summaryJson = webClient.get()
                .uri("api/identities/summary?type=face")
                .header("X-API-Key", settings.getArgusKey())
                .retrieve().bodyToMono(String::class.java).block()

            val identities = mapper.readTree(summaryJson ?: "{}")
            val argusIdentityIds = mutableSetOf<Int>()

            for (identity in identities["items"] ?: emptyList()) {
                val argusIdentityId = identity["id"]?.asInt() ?: continue
                val argusName = identity["label"]?.asText() ?: continue
                argusIdentityIds.add(argusIdentityId)

                var person = recognitionLabelRepository?.findByNameIgnoreCase(argusName)
                if (person == null) {
                    val newLabel = RecognitionLabel()
                    newLabel.setName(argusName)
                    newLabel.setArgusIdentityId(argusIdentityId)
                    person = recognitionLabelRepository?.save(newLabel)
                }
                if (person == null) continue

                if (person.getArgusIdentityId() != argusIdentityId) {
                    person.setArgusIdentityId(argusIdentityId)
                    recognitionLabelRepository?.save(person)
                }

                val galleryJson = webClient.get()
                    .uri("api/identities/$argusIdentityId/gallery?limit=9999")
                    .header("X-API-Key", settings.getArgusKey())
                    .retrieve().bodyToMono(String::class.java).block() ?: continue

                val galleryObj = mapper.readTree(galleryJson)
                val items = galleryObj["items"] ?: continue
                val argusDetectionIds = items.mapNotNull { it["detection_id"]?.asInt()?.toString() }.toSet()

                for (item in items) {
                    val detectionId = item["detection_id"]?.asInt()?.toString() ?: continue
                    val enrolled = item["enrolled"]?.asBoolean() ?: false

                    val record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId) ?: continue

                    var changed = false
                    if (record.getRecognitionLabelId() != person.getId()) {
                        record.setRecognitionLabelId(person.getId())
                        changed = true
                    }
                    val expectedAutoTagged = !enrolled
                    if (record.getAutoTagged() != expectedAutoTagged) {
                        record.setAutoTagged(expectedAutoTagged)
                        changed = true
                    }
                    if (changed) {
                        try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                    }
                }

                // Remove tags for detections no longer in this identity's Argus gallery
                val shashinPhotos = recognitionLabelPhotoRepository?.findAllByRecognitionLabelId(person.getId()) ?: emptyList()
                for (photo in shashinPhotos) {
                    if (photo != null && photo.getArgusDetectionId() != null && photo.getArgusDetectionId() !in argusDetectionIds) {
                        recognitionLabelPhotoRepository?.delete(photo)
                    }
                }

                if (person.getCoverUrl() == null) {
                    val firstMatch = recognitionLabelPhotoRepository?.findFirstByRecognitionLabelId(person.getId())
                    val metadataCover = if (firstMatch?.getMetadataId() != null)
                        metadataRepository?.findByMetadataId(firstMatch.getMetadataId()!!)?.getThumbnailUrlCentered()
                    else null

                    val cover = metadataCover
                        ?: items.firstOrNull { it["crop_url"] != null && !it["crop_url"].isNull }
                            ?.get("crop_url")?.textValue()?.let { argusServer + it }

                    if (cover != null) {
                        person.setCoverUrl(cover)
                        recognitionLabelRepository?.save(person)
                    }
                }
            }

            // Delete Shashin identities whose Argus identity no longer exists
            val allLabels = recognitionLabelRepository?.findAll() ?: emptyList()
            for (label in allLabels) {
                if (label != null && label.getArgusIdentityId() != null && label.getArgusIdentityId() !in argusIdentityIds) {
                    val photos = recognitionLabelPhotoRepository?.findAllByRecognitionLabelId(label.getId())
                    if (!photos.isNullOrEmpty()) recognitionLabelPhotoRepository?.deleteAll(photos)
                    recognitionLabelRepository?.delete(label)
                }
            }

        } catch (e: Exception) {
            logger.log(Level.WARNING, "Argus reconciliation error: ${e.localizedMessage}")
        }
    }
}
