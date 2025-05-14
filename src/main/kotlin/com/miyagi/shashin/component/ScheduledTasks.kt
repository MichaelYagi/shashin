package com.miyagi.shashin.component

import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.controller.TimelineController
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ImageProcessing.Companion.subjectRecognizer
import com.miyagi.shashin.util.ImageProcessing
import com.miyagi.shashin.util.ImageProcessing.Companion.buildObjectRecognitionCriteria
import com.miyagi.shashin.util.NetworkUtils
import com.miyagi.shashin.util.TextUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.time.*
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


@Component
class CronProperties {
    private var logger: Logger = Logger.getLogger(TimelineController::class.simpleName)

    @Autowired
    private var settingsRepository: SettingsRepository? = null

    @Value("\${app.config.default.scheduledTime}")
    private var scheduledTime: String? = null

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

    @Autowired
    private var notificationRepository: NotificationRepository? = null

    @Autowired
    private var userRepository: UserRepository? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null

    // Check Compreface connection every 2 days at midnight
    @Scheduled(cron = "0 0 12 */2 * *", zone="GMT")
    fun updateNotificationsForFaceRecogAvailability() {
        val settings = settingsRepository?.findFirstByOrderByIdAsc()
        val superAdmins = userRepository?.findAllByAuthorityEquals(superRole!!)

        if (settings != null && superAdmins != null && !settings.getCompreFaceServer().isNullOrBlank() && !settings.getCompreFaceKey().isNullOrBlank()) {
            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(settings.getCompreFaceServer(), settings.getCompreFaceKey())
            if (!faceRecogServicesAvailable) {
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (admin in superAdmins) {
                    val notificationObj = Notification()
                    notificationObj.setUserId(admin.getId())
                    notificationObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setRead(false)
                    notificationObj.setMessage("Not connected to Compreface server. Check <a href='/settings' target='_blank'>configuration</a>.")
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

    fun scheduledScan(settings: Settings) = runBlocking {
        launch {
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

            val classLoader: ClassLoader = ShashinApplication::class.java.classLoader
            val vggfaceFileExists = classLoader.getResource("lib/vggface2.pt") != null
            val retinafaceFileExists = classLoader.getResource("lib/retinaface.pt") != null

            if ((!vggfaceFileExists || !retinafaceFileExists) && !NetworkUtils.checkCompreFaceConnection(
                    settings.getCompreFaceServer(),
                    settings.getCompreFaceKey()
                )) {
                if (superAdmins != null) {
                    val notificationObjList = mutableListOf<Notification>()
                    val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                    sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                    for (admin in superAdmins) {
                        val notificationObj = Notification()
                        notificationObj.setUserId(admin.getId())
                        notificationObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                        notificationObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                        notificationObj.setRead(false)
                        notificationObj.setMessage("Missing lib files for DJL face scan")
                        notificationObjList.add(notificationObj)
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository?.saveAll(notificationObjList)
                    }
                }
            }

            var recognitionCount = 0
            if (settings.getFacialDetection() == true) {
                recognitionCount = subjectRecognizer(
                    metadataRepository,
                    recognitionLabelRepository,
                    recognitionLabelPhotoRepository,
                    relativeSidecarDir!!,
                    settings,
                    null,
                    null
                )
            }
            if (superAdmins != null && recognitionCount > 0) {
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (admin in superAdmins) {
                    val notificationObj = Notification()
                    notificationObj.setUserId(admin.getId())
                    notificationObj.setCreatedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setModifiedAt(TextUtils.getCurrentTimestamp())
                    notificationObj.setRead(false)
                    notificationObj.setMessage("$recognitionCount faces recognized during scheduled scanning at ${sdtf.format(Date())}.")
                    notificationObjList.add(notificationObj)
                }
                if (notificationObjList.isNotEmpty()) {
                    notificationRepository?.saveAll(notificationObjList)
                }
            }

            // Start object recognition
            if (settings.getObjectDetection() == true) {
                logger.log(
                    Level.INFO,
                    "Scheduled scanning for object recognition started at " + TextUtils.getCurrentTimestamp()
                )

                val threshold = settings.getObjectRecognitionConfidenceThreshold()
                val withoutKeywords = metadataRepository?.findWithoutKeywords(settings.getMatchScanLimit()!!)

                if (withoutKeywords != null) {
                    val criteria = buildObjectRecognitionCriteria()

                    if (criteria != null) {
                        for (withoutKeyword in withoutKeywords) {
                            val metadataWithoutKeywordsObj =
                                metadataRepository?.findById(withoutKeyword.getId())?.get()

                            val keywordMap = ImageProcessing.objectRecognizer(
                                metadataWithoutKeywordsObj!!,
                                criteria,
                                threshold.toString().toDouble()
                            )

                            ImageProcessing.processObjects(keywordMap.keys.toTypedArray().toList(), metadataWithoutKeywordsObj, keywordRepository!!, keywordPhotoRepository!!, metadataRepository!!)
                        }
                    }
                }
            }

            logger.log(
                Level.INFO,
                "Scheduled scanning completed at " + TextUtils.getCurrentTimestamp()
            )
        }
    }
}