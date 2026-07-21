package com.miyagi.shashin.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.RecognitionLabel
import com.miyagi.shashin.model.RecognitionLabelPhoto
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
            val apiKey = settings.getArgusKey()!!
            val argusServer = settings.getArgusServer()!!.trimEnd('/')
            val argusIdentityIds = mutableSetOf<Int>()

            // Fetch summary (paginated) to get thumbnail_url per identity (used for People page covers)
            val summaryThumbnailPaths = mutableMapOf<Int, String>()
            try {
                var summaryCursor: String? = null
                do {
                    val summaryUri = buildString {
                        append("api/identities/summary")
                        if (summaryCursor != null) append("?cursor=$summaryCursor")
                    }
                    val summaryJson = webClient.get()
                        .uri(summaryUri)
                        .header("X-API-Key", apiKey)
                        .retrieve().bodyToMono(String::class.java).block()
                    if (summaryJson.isNullOrBlank()) break
                    val summaryNode = mapper.readTree(summaryJson)
                    val summaryItems = when {
                        summaryNode.isArray -> summaryNode.toList()
                        summaryNode.has("items") -> summaryNode["items"].toList()
                        else -> emptyList()
                    }
                    for (item in summaryItems) {
                        val id = item["id"]?.asInt() ?: continue
                        val thumbUrl = item["thumbnail_url"]?.takeUnless { it.isNull }?.asText() ?: continue
                        summaryThumbnailPaths[id] = thumbUrl
                    }
                    val summaryHasMore = summaryNode["has_more"]?.asBoolean() ?: false
                    summaryCursor = if (summaryHasMore) summaryNode["next_cursor"]?.textValue() else null
                } while (summaryCursor != null)
            } catch (_: Exception) {}

            // Paginate through all Argus identities.
            // GET /api/identities includes external_ref and supports cursor pagination.
            // GET /api/identities/summary does not include external_ref and has no cursor pagination.
            var cursor: String? = null
            do {
                val uri = buildString {
                    append("api/identities?type=face&limit=200")
                    if (cursor != null) append("&cursor=$cursor")
                }
                val identitiesJson = webClient.get()
                    .uri(uri)
                    .header("X-API-Key", apiKey)
                    .retrieve().bodyToMono(String::class.java).block() ?: break

                val identitiesObj = mapper.readTree(identitiesJson)
                val hasMore = identitiesObj["has_more"]?.asBoolean() ?: false
                cursor = if (hasMore) identitiesObj["next_cursor"]?.textValue() else null
                val identities = identitiesObj["items"]?.toList() ?: emptyList()

                for (identity in identities) {
                    val argusId = identity["id"]?.asInt() ?: continue
                    val argusName = identity["label"]?.asText() ?: continue
                    val argusExternalRef = identity["external_ref"]?.takeUnless { it.isNull }?.asText()
                    val identityThumbUrl = identity["thumbnail_url"]?.takeUnless { it.isNull }?.asText()
                    argusIdentityIds.add(argusId)

                    // Find existing Shashin person in priority order:
                    // 1. external_ref — stable ID link written by resync/confirm
                    // 2. argusIdentityId stored in Shashin — covers pre-external_ref records
                    // 3. name match — last resort for legacy unlinked records
                    // 4. create new
                    var person: RecognitionLabel? = null
                    var matchSource = "none"

                    val externalRefId = argusExternalRef?.toIntOrNull()
                    if (externalRefId != null) {
                        val found = recognitionLabelRepository?.findById(externalRefId)?.takeIf { it.isPresent }?.get()
                        if (found != null) { person = found; matchSource = "external_ref($externalRefId)" }
                    }
                    if (person == null) {
                        val found = recognitionLabelRepository?.findByArgusIdentityId(argusId)
                        if (found != null) { person = found; matchSource = "argusIdentityId($argusId)" }
                    }
                    if (person == null) {
                        val nameMatch = recognitionLabelRepository?.findByNameIgnoreCase(argusName)
                        // Only take name match if that Shashin person isn't already linked to a different Argus identity
                        if (nameMatch != null && (nameMatch.getArgusIdentityId() == null || nameMatch.getArgusIdentityId() == argusId)) {
                            person = nameMatch; matchSource = "name($argusName)"
                        }
                    }
                    if (person == null) {
                        val newLabel = RecognitionLabel()
                        newLabel.setName(argusName)
                        newLabel.setArgusIdentityId(argusId)
                        person = recognitionLabelRepository?.save(newLabel)
                        matchSource = "created"
                    }
                    if (person == null) continue

                    logger.log(Level.INFO, "Argus identity $argusId ($argusName) → Shashin #${person.getId()} (${person.getName()}) via $matchSource")

                    var changed = false

                    if (person.getArgusIdentityId() != argusId) {
                        person.setArgusIdentityId(argusId)
                        changed = true
                    }

                    // Sync name Argus → Shashin if it drifted
                    if (argusName.isNotBlank() && !argusName.equals(person.getName(), ignoreCase = true)) {
                        person.setName(argusName)
                        changed = true
                    }

                    // Sync Argus cover crop path — prefer summary lookup, fall back to per-identity field
                    val thumbPath = summaryThumbnailPaths[argusId] ?: identityThumbUrl
                    if (thumbPath != null && person.getArgusThumbnailPath() != thumbPath) {
                        person.setArgusThumbnailPath(thumbPath)
                        changed = true
                    }

                    if (changed) recognitionLabelRepository?.save(person)

                    // Stamp Shashin person ID into Argus external_ref if not already set
                    if (argusExternalRef != person.getId().toString()) {
                        try {
                            val extRefBody = mapper.writeValueAsString(mapOf("external_ref" to person.getId().toString()))
                            webClient.put()
                                .uri("api/identities/$argusId/external_ref")
                                .header("X-API-Key", apiKey)
                                .header("Content-Type", "application/json")
                                .bodyValue(extRefBody)
                                .retrieve().bodyToMono(String::class.java).block()
                        } catch (_: Exception) {}
                    }

                    // Gallery sync: reconcile enrolled/pending status for each detection
                    try {
                        var galleryCursor: String? = null
                        var firstCropUrl: String? = null
                        do {
                            val galleryUri = buildString {
                                append("api/identities/$argusId/gallery?limit=200")
                                if (galleryCursor != null) append("&cursor=$galleryCursor")
                            }
                            val galleryJson = webClient.get()
                                .uri(galleryUri)
                                .header("X-API-Key", apiKey)
                                .retrieve().bodyToMono(String::class.java).block() ?: break

                            val galleryObj = mapper.readTree(galleryJson)
                            val galleryHasMore = galleryObj["has_more"]?.asBoolean() ?: false
                            galleryCursor = if (galleryHasMore) galleryObj["next_cursor"]?.textValue() else null
                            val items = galleryObj["items"] ?: break

                            for (item in items) {
                                val detectionId = item["detection_id"]?.asInt()?.toString() ?: continue
                                val enrolled = item["enrolled"]?.asBoolean() ?: false

                                var record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId)
                                if (record == null) {
                                    // Gallery item has no Shashin record — create one if source_external_ref
                                    // (the Shashin metadata ID stored by Argus at detection time) is available
                                    val sourceExternalRef = item["source_external_ref"]
                                        ?.takeUnless { it.isNull }?.asText()?.takeIf { it.isNotBlank() }
                                    if (sourceExternalRef != null) {
                                        val metadata = metadataRepository?.findById(sourceExternalRef)
                                            ?.takeIf { it.isPresent }?.get()
                                        if (metadata != null) {
                                            val stub = RecognitionLabelPhoto()
                                            stub.setMetadataId(metadata.getId())
                                            stub.setArgusDetectionId(detectionId)
                                            stub.setConfidence("0.0")
                                            try { record = recognitionLabelPhotoRepository?.save(stub) } catch (_: Exception) {}
                                        }
                                    }
                                    if (record == null) continue
                                }

                                var recordChanged = false
                                if (record.getRecognitionLabelId() != person.getId()) {
                                    record.setRecognitionLabelId(person.getId())
                                    recordChanged = true
                                }
                                // enrolled=true → confirm as manually-tagged (auto_tagged=false)
                                // enrolled=false → only mark auto-tagged if not already manually confirmed;
                                //   never downgrade a manual tag (auto_tagged=false) to auto-tagged
                                if (enrolled && record.getAutoTagged() != false) {
                                    record.setAutoTagged(false)
                                    recordChanged = true
                                } else if (!enrolled && record.getAutoTagged() == null) {
                                    record.setAutoTagged(true)
                                    recordChanged = true
                                }
                                if (recordChanged) {
                                    try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                                }

                                if (firstCropUrl == null) {
                                    firstCropUrl = item["crop_url"]?.takeUnless { it.isNull }?.asText()
                                        ?.let { argusServer + it }
                                }
                            }
                        } while (galleryCursor != null)

                        if (person.getCoverUrl() == null) {
                            val firstMatch = recognitionLabelPhotoRepository?.findFirstByRecognitionLabelId(person.getId())
                            val metadataCover = if (firstMatch?.getMetadataId() != null)
                                metadataRepository?.findByMetadataId(firstMatch.getMetadataId()!!)?.getThumbnailUrlCentered()
                            else null

                            val cover = metadataCover ?: firstCropUrl
                            if (cover != null) {
                                person.setCoverUrl(cover)
                                recognitionLabelRepository?.save(person)
                            }
                        }
                    } catch (e: Exception) {
                        logger.log(Level.WARNING, "Gallery sync failed for identity $argusId (${person.getName()}): ${e.localizedMessage}")
                    }

                    Thread.sleep(100)
                }
            } while (cursor != null)

            // Remove Shashin persons whose linked Argus identity no longer exists.
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
