package com.miyagi.shashin.component

import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component

@Component
class DataLoader @Autowired constructor(private val settingsRepository: SettingsRepository) : ApplicationRunner {
    
    @CacheEvict(value = ["firstSettingQuery"], allEntries = true)
    override fun run(args: ApplicationArguments) {
        val settings = settingsRepository.findFirstByOrderByIdAsc()
        if (settings == null) {
            val settingsObj = Settings()
            settingsObj.setQueryLimit(20)
            settingsObj.setMatchScanLimit(50)
            settingsObj.setTrainingDataLimit(100)
            settingsObj.setNotificationLimit(20)
            settingsObj.setSearchHistoryLimit(15)
            settingsObj.setPort("6624")
            settingsObj.setScanAutomatically(false)
            settingsObj.setRecognitionConfidenceThreshold("0.6")
            settingsObj.setCreatedAt(TextUtils.getCurrentTimestamp())
            settingsObj.setModifiedAt(TextUtils.getCurrentTimestamp())
            settingsRepository.save(settingsObj)
        }
    }
}