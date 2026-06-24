package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "recognitionlabelphoto")
class RecognitionLabelPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    @NotBlank
    private var recognitionLabelId: Int? = null
    @NotBlank
    private var metadataId: String? = null
    @NotBlank
    @Column(name = "compreFaceImageId")
    private var argusDetectionId: String? = null
    @NotBlank
    private var confidence: String? = null
    @NotBlank
    private var autoTagged: Boolean? = false

    fun RecognitionLabelPhoto() {}

    fun getId(): Int {
        return this.id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getRecognitionLabelId(): Int? {
        return this.recognitionLabelId
    }

    fun setRecognitionLabelId(recognitionLabelId: Int?) {
        this.recognitionLabelId = recognitionLabelId
    }

    fun getMetadataId(): String? {
        return this.metadataId
    }

    fun setArgusDetectionId(argusDetectionId: String?) {
        this.argusDetectionId = argusDetectionId
    }

    fun getArgusDetectionId(): String? {
        return this.argusDetectionId
    }

    fun setMetadataId(metadataId: String?) {
        this.metadataId = metadataId
    }

    fun getConfidence(): String? {
        return this.confidence
    }

    fun setConfidence(confidence: String?) {
        this.confidence = confidence
    }

    fun getAutoTagged(): Boolean? {
        return this.autoTagged
    }

    fun setAutoTagged(autoTagged: Boolean?) {
        this.autoTagged = autoTagged
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["metadataId"] = this.metadataId
        map["recognitionLabelId"] = this.recognitionLabelId
        map["confidence"] = this.confidence
        map["autoTagged"] = this.autoTagged
        val mapper = ObjectMapper()
        var mapJson: String? = "{}"
        try {
            mapJson = mapper.writeValueAsString(map)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return mapJson.toString()
    }
}