package com.miyagi.shashin.component

import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component

@Component
class DataLoader @Autowired constructor(private val settingsRepository: SettingsRepository) : ApplicationRunner {

    @Value("\${app.config.default.querylimit}")
    private var queryLimitProperty: Int = 30

    @Value("\${app.config.default.matchscanlimit}")
    private var matchScanLimitProperty: Int = 50

    @Value("\${app.config.default.trainingdatalimit}")
    private var trainingDataLimitProperty: Int = 100

    @Value("\${app.config.default.notificationlimit}")
    private var notificationLimitProperty: Int = 20

    @Value("\${app.config.default.searchhistorylimit}")
    private var searchHistoryLimitProperty: Int = 10

    @Value("\${app.config.default.recognitionConfidenceThreshold}")
    private lateinit var recognitionConfidenceThresholdProperty: String

    @Value("\${app.config.default.objectRecognitionConfidenceThreshold}")
    private lateinit var objectRecognitionConfidenceThresholdProperty: String

    @Value("\${server.port}")
    private lateinit var portProperty: String

    @Value("\${app.config.default.scheduledTime}")
    private lateinit var scheduledTime: String

    @CacheEvict(value = ["firstSettingQuery"], allEntries = true)
    override fun run(args: ApplicationArguments) {
        val settings = settingsRepository.findFirstByOrderByIdAsc()
        if (settings == null) {
            val settingsObj = Settings()
            settingsObj.setQueryLimit(queryLimitProperty)
            settingsObj.setMatchScanLimit(matchScanLimitProperty)
            settingsObj.setTrainingDataLimit(trainingDataLimitProperty)
            settingsObj.setNotificationLimit(notificationLimitProperty)
            settingsObj.setSearchHistoryLimit(searchHistoryLimitProperty)
//            settingsObj.setCompreFaceServer(comprefaceServer)
            settingsObj.setPort(portProperty)
            settingsObj.setScanAutomatically(false)
            settingsObj.setObjectDetection(false)
            settingsObj.setFacialDetection(false)
            settingsObj.setSidecarSizeK(0)
            settingsObj.setScheduledMatching(false)
            settingsObj.setScheduledTime(scheduledTime)
            settingsObj.setRecognitionConfidenceThreshold(recognitionConfidenceThresholdProperty)
            settingsObj.setObjectRecognitionConfidenceThreshold(objectRecognitionConfidenceThresholdProperty)
            settingsObj.setCreatedAt(TextUtils.getCurrentTimestamp())
            settingsObj.setModifiedAt(TextUtils.getCurrentTimestamp())
            settingsRepository.save(settingsObj)
        }
    }
}