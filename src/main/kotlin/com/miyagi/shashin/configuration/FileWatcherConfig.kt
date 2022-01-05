package com.miyagi.shashin.configuration

import com.miyagi.shashin.component.ShashinFileChangeListener
import com.miyagi.shashin.controller.SettingsController
import com.miyagi.shashin.repository.MediaDirectoryRepository
import com.miyagi.shashin.repository.SettingsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.devtools.filewatch.FileSystemWatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PreDestroy

@Configuration
class FileWatcherConfig {
    @Autowired
    private lateinit var mediaDirRepository: MediaDirectoryRepository

    @Autowired
    private lateinit var settingsRepository: SettingsRepository

    @Autowired
    private lateinit var settingsController: SettingsController

    private var allowAutomaticScan = false

    @Bean
    fun fileSystemWatcher(): FileSystemWatcher {

        val settingsCount = settingsRepository.count()
        if (settingsCount > 0) {
            val settings = settingsRepository.findFirstByOrderByIdAsc()
            if (settings != null && settings.getScanAutomatically() == true) {
                allowAutomaticScan = true
            }
        }

        val mediaDirectories = mediaDirRepository.findAll()
        val fileSystemWatcher = FileSystemWatcher()
        if (allowAutomaticScan) {
            for (mediaDir in mediaDirectories) {
                if (mediaDir != null) {
                    val path: Path = Paths.get(mediaDir.getDirectory()!!)
                    if (Files.exists(path)) {
                        fileSystemWatcher.addSourceDirectory(File(mediaDir.getDirectory()!!))
                    }
                }
            }
            fileSystemWatcher.addListener(ShashinFileChangeListener(settingsController))
            fileSystemWatcher.start()
        }
        return fileSystemWatcher
    }

    @PreDestroy
    @Throws(Exception::class)
    fun onDestroy() {
        fileSystemWatcher().stop()
    }
}