package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ImageProcessing
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class TestController {

    @Autowired
    private lateinit var persistentLoginsRepository: PersistentLoginsRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository

    @Autowired
    private lateinit var recognitionLabelRepository: RecognitionLabelRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testRepository: TestRepository

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    private var shouldStop = AtomicBoolean(false)

    private val threadExtensionName: String = "facescan_shashinscan"

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null

    @Secured("ROLE_SUPER")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["somevalue"] = "This is a test"

        return "test"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/creategif"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun createGif(response: HttpServletResponse?): String? {
        Thread {
            // Retroactively create gif
            val metadataList = metadataRepository.findAllByMediaType("video")

            if (metadataList != null) {
                var numProcessed = 0
                val metadataCount = metadataList.count()
                for ((index, metadata) in metadataList.withIndex()) {
                    println("iteration ${index+1} out of $metadataCount")
                    if (metadata.getThumbnailPathSmall() !== null) {
                        val jpgVersion = metadata.getThumbnailPathSmall()
                        val gifVersion = jpgVersion?.replace("_225.jpg", "_225.gif")
                        println("processing $gifVersion")

                        val gifFile = File(gifVersion!!)
                        if (!gifFile.exists()) {
                            println("gif doesn't exist")

                            ImageProcessing.createVideoGif(metadata.getId(), metadataRepository)
                            numProcessed++
                            println("processed $gifVersion")
                        } else {
                            println("already exists $gifVersion")
                        }

                        println("-------------")
                    }
                }
                println("Number gifs processed: $numProcessed")
            }
        }.start()

        return "test"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/createxsmall"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun createXSImages(response: HttpServletResponse?): String? {
        Thread {
            // Retroactively create xsmall images
            val allMetadataList = metadataRepository.findAll()

            val metadataCount = allMetadataList.count()
            var numProcessed = 0
            val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
            val sidecarDir = rootPath + relativeSidecarDir

            for ((index, metadata) in allMetadataList.withIndex()) {
                println("iteration ${index + 1} out of $metadataCount")
                if (metadata != null) {
                    if (metadata.getThumbnailPathExtraSmall() == null || !File(metadata.getThumbnailPathExtraSmall()!!).exists()) {
                        val imageProcessing = ImageProcessing("v1", File(metadata.getPath()!!), sidecarDir, metadata)
                        val metadataObj = imageProcessing.createThumbnails()
                        if (metadataObj != null) {
                            metadataRepository.save(metadataObj)
                            println("processed thumbnail ${metadataObj.getThumbnailPathExtraSmall()}")
                            numProcessed++
                        }
                    }
                }
            }
            println("Number xs thumbnails processed: $numProcessed")
        }.start()

        return "test"
    }

    private fun getFSR(path: String, mediaType: String, mediaSubtype: String): ResponseEntity<FileSystemResource> {
        if (File(path).exists()) {
            val resource = FileSystemResource(path)
            val headers = HttpHeaders()
            headers.contentLength = resource.contentLength()
            headers.contentType = MediaType(mediaType, mediaSubtype)
            headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS))
            return ResponseEntity<FileSystemResource>(resource, headers, HttpStatus.OK)
        } else {
            return ResponseEntity<FileSystemResource>(null, null, HttpStatus.NOT_FOUND)
        }
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/testvideo"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun getTestVideo(response: HttpServletResponse?): ResponseEntity<FileSystemResource> {
        val path = "c:/Users/Michael/Downloads/testpics/PXL_20240505_214316983.TS.mp4"
        return getFSR(path, "video", "mp4")
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/testimage"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    fun getTestImage(response: HttpServletResponse?): ResponseEntity<FileSystemResource> {
        val path = "c:/Users/Michael/Downloads/testpics/PXL_20240316_005235232.jpg"
        return getFSR(path, "image", "jpeg")
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/testaudio"], method = [RequestMethod.GET], produces = ["audio/3gpp","audio/aac","audio/flac","audio/mpeg","audio/mp3","audio/mp4","audio/ogg","audio/wav","audio/webm"])
    @ResponseBody
    fun getTestAudio(response: HttpServletResponse?): ResponseEntity<FileSystemResource> {
        val path = "c:/Users/Michael/Downloads/testpics/Ray Bull - The New Thing Dies.mp3"
        return getFSR(path, "audio", "mpeg")
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/fixtakendates")
    fun reconcileTakenDates(response: HttpServletResponse): String {
        Thread {
            val metadataRecords = metadataRepository.findAll()
            val metadataRecordsList = mutableListOf<Metadata>()

            var index = 0;
            for (metadata in metadataRecords) {
                if (metadata != null) {
                    val adjustedTakenDateProp = metadata.getTakenAt()
                    val adjustedTakenDateArray = adjustedTakenDateProp?.split(" ")?.toTypedArray()
                    if (adjustedTakenDateArray != null && adjustedTakenDateArray.size == 2) {
                        val adjustedTakenTime = adjustedTakenDateArray[1]

                        val takenDateYear = metadata.getYear()
                        val takenDateMonth = metadata.getMonth()
                        val takenDateDay = metadata.getDay()
                        val month = if (takenDateMonth!!.toInt() > 9) takenDateMonth else "0$takenDateMonth"
                        val day = if (takenDateDay!!.toInt() > 9) takenDateDay else "0$takenDateDay"

                        val fullDate = takenDateYear.toString() + "-" + month + "-" + day + " " + adjustedTakenTime

                        if (fullDate != adjustedTakenDateProp) {
                            println("iteration ${index + 1} out of ${metadataRecords.count()}")
                            index++
                            println("TakenDates don't match, fixing...")
                            println("metadata ID: ${metadata.getId()}")
                            println("originalTaken: $fullDate")
                            println("adjustedTaken: $adjustedTakenDateProp")
                            println("------------")
                            metadata.setTakenAt(fullDate)
                            metadataRecordsList.add(metadata)
                        }
                    }
                }
            }

            if (metadataRecordsList.size > 0) {
                metadataRepository.saveAll(metadataRecordsList)
            }
            println("# of records not matching: $index")
        }.start()

        return "test"
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/fixnullplacenames")
    fun fixNullPlaceNames(response: HttpServletResponse): String {
        Thread {
            val mids = testRepository.findLocationsWithNullPlace()
            val metadataRecordsList = mutableListOf<Metadata>()

            var index = 0;
            if (mids != null) {
                for (metadataId in mids) {
                    val metadata = metadataRepository.findByMetadataId(metadataId)

                    if (metadata?.getLat() != null && metadata.getLat() != null && metadata.getPlaceName() == null) {
                        println(metadata.toString())
                        val coordinateMap =
                            TextUtils.processCoordinates(geocodeUrl!!, metadata.getLat() + "," + metadata.getLng())
                        if (coordinateMap["place"] != null) {
                            println("iteration ${index + 1} out of ${mids.count()}")
                            index++
                            println("Location not found, fixing...")
                            println("metadata ID: ${metadata.getId()}")
                            println("location: ${coordinateMap["place"]}")

                            println("------------")
                            metadata.setPlaceName(coordinateMap["place"])
                            metadataRecordsList.add(metadata)
                        }
                    }
                }
            }

            if (metadataRecordsList.size > 0) {
                metadataRepository.saveAll(metadataRecordsList)
            }
            println("# of records missing location data: $index")
        }.start()

        return "test"
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/rescansearchedplacenames")
    fun rescanSearchedPlaceNames(response: HttpServletResponse, @RequestParam query: Optional<String>): String {
        val search = query.orElse("")

        if (search.isNotEmpty() && search.isNotBlank()) {
            Thread {
                val mids = testRepository.findByPlaceName(query.get())
                val metadataRecordsList = mutableListOf<Metadata>()

                println("query: ${query.get()}")

                var index = 0
                val x = 100
                if (mids != null) {
                    for (metadata in mids) {
                        if (metadata.getLat() != null && metadata.getLat() != null) {
                            println(metadata.toString())
                            val coordinateMap =
                                TextUtils.processCoordinates(geocodeUrl!!, metadata.getLat() + "," + metadata.getLng())
                            if (coordinateMap["place"] != null) {
                                println("iteration ${index + 1} out of ${mids.count()}")
                                index++
                                println("metadata ID: ${metadata.getId()}")
                                println("location: ${coordinateMap["place"]}")

                                metadata.setPlaceName(coordinateMap["place"])
                                metadataRecordsList.add(metadata)

                                // Save every 100 records
                                if (metadataRecordsList.size % x == 0) {
                                    println("Batch saving $x records")
                                    metadataRepository.saveAll(metadataRecordsList)
                                    metadataRecordsList.clear()
                                }

                                println("------------")
                            }
                        }
                    }
                }

                if (metadataRecordsList.size > 0) {
                    metadataRepository.saveAll(metadataRecordsList)
                }
                println("# of records with rescanned location data: $index")
            }.start()
        } else {
            println("Proceed flag is false")
        }

        return "test"
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/rescanplacenames")
    fun rescanAllPlaceNames(response: HttpServletResponse, @RequestParam proceed: Optional<Boolean>): String {
        val proceedRescan = proceed.orElse(false)

        if (proceedRescan) {
            Thread {
                val mids = metadataRepository.findAll()
                val metadataRecordsList = mutableListOf<Metadata>()

                var index = 0
                val x = 100
                for (metadata in mids) {
                    if (metadata?.getLat() != null && metadata.getLat() != null) {
                        println(metadata.toString())
                        val coordinateMap =
                            TextUtils.processCoordinates(geocodeUrl!!, metadata.getLat() + "," + metadata.getLng())
                        if (coordinateMap["place"] != null) {
                            println("iteration ${index + 1} out of ${mids.count()}")
                            index++
                            println("metadata ID: ${metadata.getId()}")
                            println("location: ${coordinateMap["place"]}")

                            metadata.setPlaceName(coordinateMap["place"])
                            metadataRecordsList.add(metadata)

                            // Save every 100 records
                            if (metadataRecordsList.size % x == 0) {
                                println("Batch saving $x records")
                                metadataRepository.saveAll(metadataRecordsList)
                                metadataRecordsList.clear()
                            }

                            println("------------")
                        }
                    }
                }

                if (metadataRecordsList.size > 0) {
                    metadataRepository.saveAll(metadataRecordsList)
                }
                println("# of records with rescanned location data: $index")
            }.start()
        } else {
            println("Proceed flag is false")
        }

        return "test"
    }
}