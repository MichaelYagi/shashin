package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "settings")
class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private var id: Int = 0
    @NotBlank
    private var recognitionConfidenceThreshold: String? = null
    @NotBlank
    private var queryLimit: Int? = null
    @NotBlank
    private var matchScanLimit: Int? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun Settings() {}

    fun getId(): Int {
        return this.id
    }

    fun getRecognitionConfidenceThreshold(): String? {
        return this.recognitionConfidenceThreshold
    }

    fun setRecognitionConfidenceThreshold(recognitionConfidenceThreshold: String?) {
        this.recognitionConfidenceThreshold = recognitionConfidenceThreshold
    }

    fun getMatchScanLimit(): Int? {
        return this.matchScanLimit
    }

    fun setMatchScanLimit(matchScanLimit: Int?) {
        this.matchScanLimit = matchScanLimit
    }

    fun getQueryLimit(): Int? {
        return this.queryLimit
    }

    fun setQueryLimit(queryLimit: Int?) {
        this.queryLimit = queryLimit
    }

    fun getCreatedAt(): String? {
        return this.createdAt
    }

    fun setCreatedAt(createdAt: String?) {
        this.createdAt = createdAt
    }

    fun getModifiedAt(): String? {
        return this.modifiedAt
    }

    fun setModifiedAt(modifiedAt: String?) {
        this.modifiedAt = modifiedAt
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["recognitionConfidenceThreshold"] = this.recognitionConfidenceThreshold
        map["queryLimit"] = this.queryLimit
        map["matchScanLimit"] = this.matchScanLimit
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