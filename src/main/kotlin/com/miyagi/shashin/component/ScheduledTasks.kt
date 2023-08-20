package com.miyagi.shashin.component

import com.miyagi.shashin.controller.TimelineController
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.FileUtils.Companion.subjectRecognizer
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.logging.Level
import java.util.logging.Logger


@Component
class ScheduledTasks {

    private var logger: Logger = Logger.getLogger(TimelineController::class.simpleName)

    @Autowired
    private var settingsRepository: SettingsRepository? = null

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private var keywordRepository: KeywordRepository? = null

    @Autowired
    private var keywordPhotoRepository: KeywordPhotoRepository? = null

    // Run everyday at 2am
    @Scheduled(cron = "0 0 2 * * *")
    fun scanSubjectsAndObjectsJob() {
        val settings = settingsRepository?.findFirstByOrderByIdAsc()

        if (settings != null && settings.getScheduledMatching()!!) {

            Thread {

                // Object and person recognition

                // Start subject matching
                if (FileUtils.checkCompreFaceConnection(
                        settings.getCompreFaceServer(),
                        settings.getCompreFaceKey()
                    )
                ) {
                    subjectRecognizer(metadataRepository, recognitionLabelRepository, recognitionLabelPhotoRepository, settings, null, null)
                }

                // Start object recognition
                if (settings.getObjectDetection() == true) {
                    val withoutKeywords = metadataRepository?.findWithoutKeywords(settings.getMatchScanLimit()!!)
                    if (withoutKeywords != null) {
                        for (withoutKeyword in withoutKeywords) {
                            val metadataWithoutKeywordsObj = metadataRepository?.findById(withoutKeyword.getId())?.get()

                            FileUtils.objectRecognizer(
                                keywordRepository!!,
                                keywordPhotoRepository!!,
                                metadataRepository!!,
                                metadataWithoutKeywordsObj!!,
                                settings,
                                null,
                                null
                            )
                        }
                    }
                }

                logger.log(
                    Level.INFO,
                    "Scheduled scanning completed at " + TextUtils.getCurrentTimestamp()
                )
            }.start()
        }
    }
}