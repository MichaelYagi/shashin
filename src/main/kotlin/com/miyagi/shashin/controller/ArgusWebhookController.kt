package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.RecognitionLabel
import com.miyagi.shashin.repository.RecognitionLabelPhotoRepository
import com.miyagi.shashin.repository.RecognitionLabelRepository
import com.miyagi.shashin.repository.SettingsRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.reactive.function.client.WebClient
import java.util.logging.Level
import java.util.logging.Logger

@Controller
class ArgusWebhookController(
    private var recognitionLabelRepository: RecognitionLabelRepository? = null,
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null,
    private var settingsRepository: SettingsRepository? = null
) {
    private val logger = Logger.getLogger(ArgusWebhookController::class.simpleName)
    private val mapper = ObjectMapper()

    @PostMapping("/api/argus/webhook")
    @ResponseBody
    fun receive(@RequestBody body: String): ResponseEntity<Void> {
        try {
            val root = mapper.readTree(body)
            val event = root["event"]?.asText() ?: return ResponseEntity.ok().build()
            val data = root["data"] ?: return ResponseEntity.ok().build()
            when (event) {
                "identity.created"  -> handleIdentityCreated(data)
                "identity.updated"  -> handleIdentityUpdated(data)
                "identity.merged"   -> handleIdentityMerged(data)
                "identity.deleted"  -> handleIdentityDeleted(data)
                "detection.labeled" -> handleDetectionLabeled(data)
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Argus webhook error: ${e.localizedMessage}")
        }
        return ResponseEntity.ok().build()
    }

    private fun handleIdentityCreated(data: JsonNode) {
        if (data["type"]?.asText() != "face") return
        val argusId = data["identity_id"]?.asInt() ?: return
        val label = data["label"]?.asText()?.takeIf { it.isNotBlank() } ?: return

        // If external_ref is already a valid Shashin person ID, just ensure the link is correct
        val externalRefId = data["external_ref"]?.takeUnless { it.isNull }?.asText()?.toIntOrNull()
        if (externalRefId != null) {
            val found = recognitionLabelRepository?.findById(externalRefId)?.takeIf { it.isPresent }?.get()
            if (found != null) {
                if (found.getArgusIdentityId() != argusId) {
                    found.setArgusIdentityId(argusId)
                    recognitionLabelRepository?.save(found)
                }
                return
            }
        }

        // Already linked
        if (recognitionLabelRepository?.findByArgusIdentityId(argusId) != null) return

        // Name match — only if not already linked to a different Argus identity
        val nameMatch = recognitionLabelRepository?.findByNameIgnoreCase(label)
        val person: RecognitionLabel?
        if (nameMatch != null && (nameMatch.getArgusIdentityId() == null || nameMatch.getArgusIdentityId() == argusId)) {
            if (nameMatch.getArgusIdentityId() != argusId) {
                nameMatch.setArgusIdentityId(argusId)
                recognitionLabelRepository?.save(nameMatch)
            }
            person = nameMatch
        } else {
            val newLabel = RecognitionLabel()
            newLabel.setName(label)
            newLabel.setArgusIdentityId(argusId)
            person = recognitionLabelRepository?.save(newLabel) ?: return
        }

        stampExternalRef(argusId, person.getId())
    }

    private fun handleIdentityUpdated(data: JsonNode) {
        val argusId = data["identity_id"]?.asInt() ?: return
        when (data["action"]?.asText()) {
            "renamed" -> {
                val person = recognitionLabelRepository?.findByArgusIdentityId(argusId) ?: return
                val newLabel = data["label"]?.asText()?.takeIf { it.isNotBlank() } ?: return
                if (!newLabel.equals(person.getName(), ignoreCase = true)) {
                    person.setName(newLabel)
                    recognitionLabelRepository?.save(person)
                }
            }
            "thumbnail_updated" -> {
                val person = recognitionLabelRepository?.findByArgusIdentityId(argusId) ?: return
                val thumbUrl = data["thumbnail_url"]?.takeUnless { it.isNull }?.asText() ?: return
                if (person.getArgusThumbnailPath() != thumbUrl) {
                    person.setArgusThumbnailPath(thumbUrl)
                    recognitionLabelRepository?.save(person)
                }
            }
            "embedding_added" -> {
                val detectionId = data["detection_id"]?.takeUnless { it.isNull }?.asInt()?.toString() ?: return
                val record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId) ?: return
                if (record.getAutoTagged() != false) {
                    record.setAutoTagged(false)
                    try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                }
            }
            "embedding_removed" -> {
                val detectionId = data["detection_id"]?.takeUnless { it.isNull }?.asInt()?.toString() ?: return
                val record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId) ?: return
                if (record.getAutoTagged() == false) {
                    record.setAutoTagged(true)
                    try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun handleIdentityMerged(data: JsonNode) {
        val sourceArgusId = data["identity_id"]?.asInt() ?: return
        val targetArgusId = data["merged_into"]?.asInt() ?: return
        val source = recognitionLabelRepository?.findByArgusIdentityId(sourceArgusId) ?: return
        val target = recognitionLabelRepository?.findByArgusIdentityId(targetArgusId) ?: return
        val photos = recognitionLabelPhotoRepository?.findAllByRecognitionLabelId(source.getId())
        if (!photos.isNullOrEmpty()) {
            for (photo in photos) {
                photo ?: continue
                photo.setRecognitionLabelId(target.getId())
                try { recognitionLabelPhotoRepository?.save(photo) } catch (_: Exception) {}
            }
        }
        recognitionLabelRepository?.delete(source)
    }

    private fun handleIdentityDeleted(data: JsonNode) {
        val argusId = data["identity_id"]?.asInt() ?: return
        val person = recognitionLabelRepository?.findByArgusIdentityId(argusId) ?: return
        val photos = recognitionLabelPhotoRepository?.findAllByRecognitionLabelId(person.getId())
        if (!photos.isNullOrEmpty()) recognitionLabelPhotoRepository?.deleteAll(photos)
        recognitionLabelRepository?.delete(person)
    }

    private fun handleDetectionLabeled(data: JsonNode) {
        if (data["type"]?.asText() != "face") return
        val detectionId = data["detection_id"]?.asInt()?.toString() ?: return
        val identityId = data["identity_id"]?.takeUnless { it.isNull }?.asInt() ?: return
        val record = recognitionLabelPhotoRepository?.findByArgusDetectionId(detectionId) ?: return
        val person = recognitionLabelRepository?.findByArgusIdentityId(identityId) ?: return
        if (record.getRecognitionLabelId() != person.getId()) {
            record.setRecognitionLabelId(person.getId())
            try { recognitionLabelPhotoRepository?.save(record) } catch (_: Exception) {}
        }
    }

    private fun stampExternalRef(argusId: Int, shashinId: Int?) {
        val settings = settingsRepository?.findFirstByOrderByIdAsc() ?: return
        if (settings.getArgusServer().isNullOrBlank() || settings.getArgusKey().isNullOrBlank()) return
        try {
            val body = mapper.writeValueAsString(mapOf("external_ref" to shashinId.toString()))
            WebClient.create(settings.getArgusServer()!!.trimEnd('/') + "/")
                .put()
                .uri("api/identities/$argusId/external_ref")
                .header("X-API-Key", settings.getArgusKey())
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve().bodyToMono(String::class.java).block()
        } catch (_: Exception) {}
    }
}
