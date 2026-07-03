package com.miyagi.shashin.component

import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.controller.TimelineController
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.util.NetworkUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.time.*
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


@Component
class CronProperties(
    private var settingsRepository: SettingsRepository? = null,
    @Value("\${app.config.default.scheduledTime}")
    private var scheduledTime: String? = null,
    var messageSource: MessageSource? = null
) {
    private var logger: Logger = Logger.getLogger(TimelineController::class.simpleName)

    fun expression(): String {
        val settings = settingsRepository?.findFirstByOrderByIdAsc()

        var hourSetting = "9"
        var hourProperty = "9"

        if (settings != null) {
            val scheduledTimeSetting = settings.getScheduledTime()
            val scheduledTimeSettingGmt = TextUtils.doUtcTimeZoneConversion(scheduledTimeSetting!!, true)
            val scheduledTimeSettingArray = scheduledTimeSettingGmt.split(":")
            hourSetting = scheduledTimeSettingArray[0]
            if (hourSetting.first() == '0') {
                hourSetting = hourSetting.drop(1)
            }

            if (scheduledTime != null) {
                val scheduledTimePropertyGmt = TextUtils.doUtcTimeZoneConversion(scheduledTime, true)
                val scheduledTimePropArray = scheduledTimePropertyGmt.split(":")
                hourProperty = scheduledTimePropArray[0]
                if (hourProperty.first() == '0') {
                    hourProperty = hourProperty.drop(1)
                }
            }
        }

        val cronSettings = "0 0 $hourSetting * * *"
        val cronProperty = "0 0 $hourProperty * * *"

        if (settings != null) {
            logger.log(
                Level.INFO,
                "Cron from settings used: '$cronSettings' GMT"
            )
        } else {
            logger.log(
                Level.INFO,
                "Cron from property used: '$cronProperty' GMT"
            )
        }

        return if (settings != null) cronSettings else cronProperty
    }
}

@Component
class ScheduledTasks(
    private var settingsRepository: SettingsRepository? = null,
    private var metadataRepository: MetadataRepository? = null,
    private var recognitionLabelRepository: RecognitionLabelRepository? = null,
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null,
    private var keywordRepository: KeywordRepository? = null,
    private var keywordPhotoRepository: KeywordPhotoRepository? = null,
    private var notificationRepository: NotificationRepository? = null,
    private var userRepository: UserRepository? = null,
    @Value("\${app.role.super}")
    private var superRole: String? = null,
    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null,
    var messageSource: MessageSource? = null,
    private var argusReconcile: ArgusReconcile? = null,
    private var ollamaVisionService: OllamaVisionService? = null
) {

    private var logger: Logger = Logger.getLogger(TimelineController::class.simpleName)

    // Check Compreface connection every 2 days at midnight
    @Scheduled(cron = "0 0 12 */2 * *", zone="GMT")
    fun updateNotificationsForFaceRecogAvailability() {
        val settings = settingsRepository?.findFirstByOrderByIdAsc()
        val superAdmins = userRepository?.findAllByAuthorityEquals(superRole!!)

        if (settings != null && superAdmins != null && settings.getFacialDetection() == true &&
            !settings.getArgusServer().isNullOrBlank() &&
            !settings.getArgusKey().isNullOrBlank()
        ) {
            val faceRecogServicesAvailable = NetworkUtils.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())
            if (!faceRecogServicesAvailable) {
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (admin in superAdmins) {
                    var language = admin.getLanguage()
                    if (language == null) {
                        language = "en"
                    }

                    val locale = Locale(language)

                    val notificationObj = Notification()
                    notificationObj.setUserId(admin.getId())
                    notificationObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setRead(false)
                    notificationObj.setMessage(messageSource?.getMessage("main.notification.compreface.notconnected", null, locale))
                    notificationObjList.add(notificationObj)
                }
                if (notificationObjList.isNotEmpty()) {
                    notificationRepository?.saveAll(notificationObjList)
                }
            }
        }
    }

    // Configured hour to scan faces
    @Scheduled(cron = "#{cronProperties.expression()}", zone="GMT")
    fun scanSubjectsAndObjectsJob() {
        val settings = settingsRepository?.findFirstByOrderByIdAsc()

        if (settings != null && settings.getScheduledMatching()!!) {
            scheduledScan(settings)
        }
    }

    fun scheduledScan(settings: Settings) {
        Thread {
            logger.log(
                Level.INFO,
                "Scheduled scanning started at " + TextUtils.getCurrentTimestamp()
            )

            val superAdmins = userRepository?.findAllByAuthorityEquals(superRole!!)

            // Object and person recognition

            // Start subject matching
            logger.log(
                Level.INFO,
                "Scheduled scanning for facial recognition started at " + TextUtils.getCurrentTimestamp()
            )

            if (!NetworkUtils.checkArgusConnection(settings.getArgusServer(), settings.getArgusKey())) {
                if (superAdmins != null) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in superAdmins) {
                        var language = admin.getLanguage()
                        if (language == null) {
                            language = "en"
                        }

                        var locale = Locale(language)

                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                        notificationObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage(messageSource?.getMessage("main.notification.people.missing", null, locale))
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository?.saveAll(notificationObjList)
                    }
                }
            }

            val (recognitionCount, scannedIds) = ImageProcessing.scanAll(
                metadataRepository,
                recognitionLabelRepository,
                recognitionLabelPhotoRepository,
                keywordRepository,
                keywordPhotoRepository,
                settings,
                null,
                null,
                messageSource
            )
            if (scannedIds.isNotEmpty()) {
                ollamaVisionService?.processItems(scannedIds, settings)
            }
            if (superAdmins != null && recognitionCount > 0) {
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (admin in superAdmins) {
                    var language = admin.getLanguage()
                    if (language == null) {
                        language = "en"
                    }

                    var locale = Locale(language)
                    val notificationObj = Notification()
                    notificationObj.setUserId(admin.getId())
                    notificationObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setRead(false)
                    notificationObj.setMessage(messageSource?.getMessage("main.notification.setting.scheduled.scan.faces", arrayOf(recognitionCount), locale)+"- ${sdtf.format(Date())}.")
                    notificationObjList.add(notificationObj)
                }
                if (notificationObjList.isNotEmpty()) {
                    notificationRepository?.saveAll(notificationObjList)
                }
            }


            argusReconcile?.run(settings)

            logger.log(
                Level.INFO,
                "Scheduled scanning completed at " + TextUtils.getCurrentTimestamp()
            )
        }.start()
    }
}