package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "settings")
class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    @NotBlank
    private var recognitionConfidenceThreshold: String? = null
    @NotBlank
    private var objectRecognitionConfidenceThreshold: String? = null
    @NotBlank
    private var compreFaceServer: String? = null
    @NotBlank
    private var compreFaceKey: String? = null
    @NotBlank
    private var queryLimit: Int? = null
    @NotBlank
    private var matchScanLimit: Int? = null
    @NotBlank
    private var trainingDataLimit: Int? = null
    @NotBlank
    private var notificationLimit: Int? = null
    @NotBlank
    private var searchHistoryLimit: Int? = null
    @NotBlank
    private var port: String? = null
    @NotBlank
    private var scanAutomatically: Boolean? = null
    @NotBlank
    private var objectDetection: Boolean? = null
    @NotBlank
    private var facialDetection: Boolean? = null
    @NotBlank
    private var scheduledMatching: Boolean? = null
    @NotBlank
    private var scheduledTime: String? = null
    @NotBlank
    private var uploadMediaDirectory: String? = null
    private var sidecarSizeK: Long? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun Settings() {}

    fun getId(): Int {
        return this.id
    }

    fun getScheduledTime(): String? {
        return this.scheduledTime
    }

    fun setScheduledTime(scheduledTime: String?) {
        this.scheduledTime = scheduledTime
    }

    fun getRecognitionConfidenceThreshold(): String? {
        return this.recognitionConfidenceThreshold
    }

    fun setRecognitionConfidenceThreshold(recognitionConfidenceThreshold: String?) {
        this.recognitionConfidenceThreshold = recognitionConfidenceThreshold
    }

    fun getObjectRecognitionConfidenceThreshold(): String? {
        return this.objectRecognitionConfidenceThreshold
    }

    fun setObjectRecognitionConfidenceThreshold(objectRecognitionConfidenceThreshold: String?) {
        this.objectRecognitionConfidenceThreshold = objectRecognitionConfidenceThreshold
    }

    fun getCompreFaceKey(): String? {
        return this.compreFaceKey
    }

    fun setCompreFaceKey(compreFaceKey: String?) {
        this.compreFaceKey = compreFaceKey
    }

    fun getCompreFaceServer(): String? {
        return this.compreFaceServer
    }

    fun setCompreFaceServer(compreFaceServer: String?) {
        this.compreFaceServer = compreFaceServer
    }

    fun getMatchScanLimit(): Int? {
        return this.matchScanLimit
    }

    fun setMatchScanLimit(matchScanLimit: Int?) {
        this.matchScanLimit = matchScanLimit
    }

    fun getSidecarSizeK(): Long? {
        return this.sidecarSizeK
    }

    fun setSidecarSizeK(sidecarSizeK: Long?) {
        this.sidecarSizeK = sidecarSizeK
    }

    fun getTrainingDataLimit(): Int? {
        return this.trainingDataLimit
    }

    fun setTrainingDataLimit(trainingDataLimit: Int?) {
        this.trainingDataLimit = trainingDataLimit
    }

    fun getNotificationLimit(): Int? {
        return this.notificationLimit
    }

    fun setNotificationLimit(notificationLimit: Int?) {
        this.notificationLimit = notificationLimit
    }

    fun getSearchHistoryLimit(): Int? {
        return this.searchHistoryLimit
    }

    fun setSearchHistoryLimit(searchHistoryLimit: Int?) {
        this.searchHistoryLimit = searchHistoryLimit
    }

    fun getQueryLimit(): Int? {
        return this.queryLimit
    }

    fun setQueryLimit(queryLimit: Int?) {
        this.queryLimit = queryLimit
    }

    fun getPort(): String? {
        return this.port
    }

    fun setPort(port: String?) {
        this.port = port
    }

    fun getScanAutomatically(): Boolean? {
        return this.scanAutomatically
    }

    fun setScanAutomatically(scanAutomatically: Boolean?) {
        this.scanAutomatically = scanAutomatically
    }

    fun getObjectDetection(): Boolean? {
        return this.objectDetection
    }

    fun setObjectDetection(objectDetection: Boolean?) {
        this.objectDetection = objectDetection
    }

    fun getFacialDetection(): Boolean? {
        return this.facialDetection
    }

    fun setFacialDetection(facialDetection: Boolean?) {
        this.facialDetection = facialDetection
    }

    fun getScheduledMatching(): Boolean? {
        return this.scheduledMatching
    }

    fun setScheduledMatching(scheduledMatching: Boolean?) {
        this.scheduledMatching = scheduledMatching
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

    fun getUploadMediaDirectory(): String? {
        return this.uploadMediaDirectory
    }

    fun setUploadMediaDirectory(uploadMediaDirectory: String?) {
        this.uploadMediaDirectory = uploadMediaDirectory
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["port"] = this.port
        map["compreFaceServer"] = this.compreFaceServer
        map["compreFaceKey"] = this.compreFaceKey
        map["objectDetection"] = this.objectDetection
        map["facialDetection"] = this.facialDetection
        map["recognitionConfidenceThreshold"] = this.recognitionConfidenceThreshold
        map["objectRecognitionConfidenceThreshold"] = this.objectRecognitionConfidenceThreshold
        map["queryLimit"] = this.queryLimit
        map["matchScanLimit"] = this.matchScanLimit
        map["trainingDataLimit"] = this.trainingDataLimit
        map["notificationLimit"] = this.notificationLimit
        map["searchHistoryLimit"] = this.searchHistoryLimit
        map["scanAutomatically"] = this.scanAutomatically
        map["scheduledMatching"] = this.scheduledMatching
        map["scheduledTime"] = this.scheduledTime
        map["uploadMediaDirectory"] = this.uploadMediaDirectory
        map["sidecarSizeK"] = this.sidecarSizeK
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