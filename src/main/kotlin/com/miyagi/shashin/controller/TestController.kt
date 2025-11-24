package com.miyagi.shashin.controller

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.service.DuplicateImageDetection
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.MetadataDate
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.service.ImageProcessing
import com.miyagi.shashin.service.MetadataProcessing
import com.miyagi.shashin.util.MetricsUtil
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.ZoneId
import javax.imageio.ImageIO
import kotlin.String
import kotlin.collections.mutableMapOf
import kotlin.collections.set

// Used for prototyping
@Controller
class TestController(
    // Avoid field injection as much as possible (AutoWiring)
    private val metadataRepository: MetadataRepository,
    private val keywordRepository: KeywordRepository,
    private val keywordPhotoRepository: KeywordPhotoRepository,
    private val duplicatesRepository: DuplicatesRepository,
    private val testRepository: TestRepository,
    private val recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository?, // nullable bean
    @Value("\${app.endpoint.url.geocode}")
    private val geocodeUrl: String?, // injected config
    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? // injected config
) {

    private var currentIndex = 0
    private var totalIndex = 0
    private var startTime = System.currentTimeMillis()
    private var etr: Long = 0
    private var activeLink = ""
    private val mapper = ObjectMapper()

    @GetMapping("/testgrounds")
    fun testPage(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["activePage"] = "testgrounds"

        return "testgrounds"
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["activePage"] = "test"
        model["mike"] = "Mike"
        model["noah"] = "Noah"

        println("hash_experiment")
//        var fileOne = File("C:\\Users\\Michael\\Downloads\\testpics\\shashin_download_20251122_094127\\IMG_20181215_161431_j1DHSrnNruAqFAC9iw07g.jpg")
        var fileOne = File("C:\\Users\\Michael\\Downloads\\testpics\\IMG_2409.HEIC")
//        var fileTwo = File("C:\\Users\\Michael\\Downloads\\testpics\\shashin_download_20251122_094127\\IMG_20181215_161431-edited_iZUJjIQ6PheCZaziOvljCA.jpg")
//        var fileTwo = File("C:\\Users\\Michael\\Downloads\\testpics\\dupetest3\\tablecup.jpg")
//        var fileTwo = File("C:\\Users\\Michael\\Downloads\\testpics\\005_Aug_22__1984.jpg")
//        var fileTwo = fileOne
//
        var resolution = 64
//
        var imageOne = ImageIO.read(fileOne)
//        var imageTwo = ImageIO.read(fileTwo)
//
        var dcheck = DuplicateImageDetection()
        var d1 = dcheck.dhash(fileOne,resolution)
//        var d2 = dcheck.dhash(fileTwo,resolution)
//
//        println("purekotlin")
        println(d1.toString())
//        println(d2.toString())
////        println(dcheck.hammingDistance(BigInteger("3176040817552762079483"), BigInteger("4356632438270173382911")))
////        println(dcheck.similarityPercentage(BigInteger("3176040817552762079483"), BigInteger("4356632438270173382911"), resolution))
//        println(DuplicateImageChecker.hammingDistance(d1, d2))
//        println(DuplicateImageChecker.similarityPercentage(d1, d2, resolution))
//
//        //-----------
//
//        println("jimagehash")
//        val dupeImageDetection = DuplicateImageDetection()
//        dupeImageDetection.setAlgorithm("dhash", resolution)
//        var hashOne = dupeImageChecker.computeHash(fileOne)
//        var hashTwo = dupeImageChecker.computeHash(fileTwo)
//        println(hashOne?.hashValue)
//        println(hashTwo?.hashValue)
//        println(hashOne?.hammingDistance(hashTwo))
//        println(dupeImageChecker.similarityScore(hashOne!!, hashTwo!!))

        model["currentTimestamp"] = TextUtils.getCurrentTimestamp()
        model["defaultTZ"] = ZoneId.systemDefault()

        return "test"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/test"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun posttest(model: Model, request: HttpServletRequest, response: HttpServletResponse, @RequestBody requestBody: JsonNode): String {
        val payloadMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        val response = mutableMapOf<String, Any?>()

        response["activePage"] = "test"
        response["text"] = "Enter fields"
        response["setone"] = ""
        response["settwo"] = ""
        response["base64_1"] = ""
        response["msg"] = ""
        response["status"] = ApiResponse.FAIL.status

        // C:\Users\Michael\Downloads\PXL_20230721_142144451.MP.jpg
        // C:\Users\Michael\Downloads\PXL_20210930_164602780.jpg
        // C:\Users\Michael\Downloads\PXL_20210930_164602780_resize.jpg

//        val test = DuplicateImageChecker()
//        var cHash1 = test.computeHashFromString("14404428052212531424")
//        var cHash2 = test.computeHashFromString("14408931651839910112")
//        val isDupe = test.isDuplicate(cHash1, cHash2)
//        val similarityScore = test.similarityScore(cHash1, cHash2)
//        println(isDupe)
//        println(similarityScore)

        if (payloadMap.containsKey("setone") && payloadMap.containsKey("settwo") && payloadMap.containsKey("resolution") && payloadMap.containsKey("threshold")) {
            var setOneFilename = payloadMap["setone"].toString()
            var setTwoFilename = payloadMap["settwo"].toString()
            var threshold = payloadMap["threshold"].toString().toInt()
            var resolution = payloadMap["resolution"].toString().toInt()

            response["setone"] = setOneFilename
            response["settwo"] = setTwoFilename

            val metricsUtil = MetricsUtil()
            metricsUtil.start("Start dupe detection")

            val file1 = File(setOneFilename)
            val file2 = File(setTwoFilename)

            val i = DuplicateImageDetection()

            var hash1 = i.dhash(file1, resolution)
            var hash2 = i.dhash(file2, resolution)

            metricsUtil.end()

            metricsUtil.start("dupe check")
            val isDuplicate = DuplicateImageDetection.isDuplicate(hash1!!, hash2!!, threshold)
            metricsUtil.end()

            response["text"] = "Algorithm: dhash"+
                    "<br>Resolution: "+i.getResolution()+
                    "<br>hash 1: " + hash1 +
                    "<br>hash 2: " + hash2 +
                    "<br>threshold: " + threshold +
                    "<br>Is duplicate: " + isDuplicate +
                    "<br>Similarity: " + DuplicateImageDetection.similarityPercentage(hash1, hash2) +
                    "<br>Timings: " + metricsUtil.getMetricsList().toString() +
                    "<br>Elapsed Time: " + metricsUtil.getTotalElapsedTime() + "ms"

            response["status"] = ApiResponse.SUCCESS.status
        }

        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/duplicatestest")
    fun dupetest(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["metadataList"] = mutableListOf<Metadata>()

        val size = 1000

        // Find all duplicates - put entries in duplicates table
        DuplicateImageDetection.findAndStoreDuplicates(duplicatesRepository, 5, 0, size)

        // Display and group by duplicate images
        model["metadataList"] = duplicatesRepository.findAllMetadataIds(0, size)

        model["page"] = 1
        model["size"] = 1000

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/duplicatestest/{page}/{size}")
    fun dupetest(model: Model, @PathVariable page: Int, @PathVariable size: Int, request: HttpServletRequest, response: HttpServletResponse): String {
        model["metadataList"] = mutableListOf<Metadata>()
        model["page"] = page+1
        model["size"] = size

        val pageValue = page*size
        // Find all duplicates
//        DuplicateImageChecker.findAndStoreDuplicates(duplicatesRepository, 5, pageValue, size)

        // Display and group by duplicate images
        model["metadataList"] = duplicatesRepository.findAllMetadataIds(pageValue, size)

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/test/{page}")
    fun testPaged(model: Model, @PathVariable(required = true) page: Int?, request: HttpServletRequest, response: HttpServletResponse): String {
        model["activePage"] = "test"
        model["currentPage"] = page

        model["base64_1"] = ""
        model["base64_2"] = ""

        return "test"
    }

    @GetMapping("/sandbox")
    fun sandbox(model: Model, request: HttpServletRequest, response: HttpServletResponse, @RequestParam size: Optional<Int>): String {
        model["activePage"] = "sandbox"

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/sandbox/data"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun sandboxAPI(model: Model, request: HttpServletRequest, response: HttpServletResponse, @RequestParam size: Optional<Int>): String {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status
        response["activePage"] = "sandbox"
        response["size"] = size.orElse(500)
        response["metadataList"] = mutableListOf<Metadata>()
        response["metadataDates"] = mutableListOf<MetadataDate>()
        response["metadataDatesHash"] = mutableMapOf<String, Int>()

        val sizeValue = size.orElse(500)
        val page = 0

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response["metadataList"] = metadataRepository.findAllByOffsetAndLimit((page * sizeValue), sizeValue)

            val metadataDateHash = mutableMapOf<String, Int>()
            response["metadataDatesHash"] = metadataDateHash

            val metadataDates = metadataRepository.findAllYearMonthDay()

            if (metadataDates != null) {
                response["metadataDates"] = metadataDates

                val dates = metadataDates.toMutableList()
                for ((index, metadataDate) in dates.withIndex()) {
                    metadataDateHash[metadataDate.getYear().toString() + "-" + metadataDate.getMonth()
                        .toString() + "-" + metadataDate.getDay().toString()] = index
                }
                response["metadataDatesHash"] = metadataDateHash
            }
        }

        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/sandbox/data/{year}/{month}/{day}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun sandboxAPIDate(model: Model, request: HttpServletRequest, response: HttpServletResponse, @PathVariable(required = true) year: Int?, @PathVariable(required = true) month: Int?, @PathVariable(required = true) day: Int?): String {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = ""
        response["status"] = ApiResponse.SUCCESS.status
        response["activePage"] = "sandbox"
        response["metadataList"] = mutableListOf<Metadata>()

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response["metadataList"] = metadataRepository.findTimelineByDate(year, month, day)
        }

        return mapper.writeValueAsString(response)
    }

    @Secured("ROLE_SUPER")
    @GetMapping("/wake")
    fun wake(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["activePage"] = "wake"

        return "wake"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/deletethread"], method = [RequestMethod.GET])
    @ResponseBody
    fun deleteThread(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        FileUtils.deleteThreadFiles("repairscripts")

        return "sandbox"
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
    @RequestMapping(value = ["/insertdupehash"], method = [RequestMethod.GET])
    @ResponseBody
    fun getDupeHash(response: HttpServletResponse?): String {
        Thread {
            val allMetadataList = testRepository.findImagePaths()
            val count = testRepository.countImagePaths()
            val metadataList = mutableListOf<Metadata>()
            val x = 100

            for ((index, metadata) in allMetadataList?.withIndex()!!) {
                val file = File(metadata.getPath().toString())

                if (metadata.getDuplicateHash() == null && file.exists() && file.length() > 0) {
                    val dupeImageDetection = DuplicateImageDetection()
                    val hash = dupeImageDetection.dhash(File(metadata.getPath()!!))
                    metadata.setDuplicateHash(hash.toString())
                    metadataList.add(metadata)
                    println("Saving metadata ${metadata.getPath()} at ${index+1}/$count")

                    if (metadataList.size % x == 0) {
                        println("Batch saving $x records")
                        metadataRepository.saveAll(metadataList)
                        metadataList.clear()
                    }
                }
            }

            if (metadataList.size > 0) {
                metadataRepository.saveAll(metadataList)
            }
        }.start()

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/fixexif"], method = [RequestMethod.GET])
    @ResponseBody
    fun fixExif(response: HttpServletResponse?): String? {
        if (activeLink.isBlank()) {
            activeLink = "fixExif"
        }

        FileUtils.deleteThreadFiles("repairscripts")

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {
            Thread {
                // Retroactively create gif

                val metadataList = metadataRepository.findAll()

                var localIndex = 0
                currentIndex = 0
                totalIndex = metadataList.count()
                startTime = System.currentTimeMillis()
                val timesArray = mutableListOf<Long>()

                for ((index, metadata) in metadataList.withIndex()) {
                    val elapsedStartTime = System.currentTimeMillis()
                    println("iteration ${index + 1} out of $totalIndex")

                    if (metadata?.getThumbnailPathSmall() !== null) {
                        val thumbnailPathSmall = metadata.getThumbnailPathSmall()
                        var exifFilePath = thumbnailPathSmall?.replace("_225.*".toRegex(), ".exif.yaml")
                        exifFilePath = exifFilePath?.replace("/thumbnails/", "/metadata/")
                        println("processing $exifFilePath")

                        val filePath = metadata.getPath()
                        val file = File(filePath!!)

                        val exifFile = File(exifFilePath!!)
                        if (!exifFile.exists() && !file.exists()) {
                            println("exif $exifFilePath doesn't exist")
                            println("file $file doesn't exist")
                            continue
                        } else {
                            val metadata = ImageMetadataReader.readMetadata(file)
                            val exifMap = hashMapOf<String, HashMap<String, String>>()

                            for (directory in metadata.directories) {
                                val subExifMap = hashMapOf<String, String>()
                                val directoryName = directory.name

                                for (tag in directory.tags) {
                                    if (tag.description != null) {
                                        val tagName = tag.tagName

                                        if ("unknowntag" !in tagName.lowercase()) {
                                            subExifMap["$tagName"] = tag.description
                                        }
                                    }

                                    exifMap["$directoryName"] = subExifMap
                                }
                            }

                            if (exifMap.isNotEmpty()) {
                                val yamlFactory: YAMLFactory = YAMLFactory.builder()
                                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                                    .disable(YAMLGenerator.Feature.SPLIT_LINES)
                                    .build()
                                val om = ObjectMapper(yamlFactory)
                                om.writeValue(exifFile, exifMap)
                            }


                            localIndex++
                            println("processed $exifFile")
                        }

                        println("-------------")
                    }

                    val endTime = System.currentTimeMillis()
                    timesArray.add((endTime-elapsedStartTime))
                    etr = progress(currentIndex, totalIndex, timesArray)
                    currentIndex++
                }
                println("Number exifs processed: $localIndex")

                activeLink = ""
                FileUtils.deleteThreadFiles("fixExif")
            }.start()
        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/creategif"], method = [RequestMethod.GET])
    @ResponseBody
    fun createGif(response: HttpServletResponse?): String? {
        if (activeLink.isBlank()) {
            activeLink = "creategif"
        }

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {
            Thread {
                // Retroactively create gif

                val metadataList = metadataRepository.findAllByMediaType("video")

                if (metadataList != null) {
                    var localIndex = 0
                    currentIndex = 0
                    totalIndex = metadataList.count()
                    startTime = System.currentTimeMillis()
                    val timesArray = mutableListOf<Long>()
                    for ((index, metadata) in metadataList.withIndex()) {
                        val elapsedStartTime = System.currentTimeMillis()
                        println("iteration ${index + 1} out of $totalIndex")
                        if (metadata.getThumbnailPathSmall() !== null) {
                            val jpgVersion = metadata.getThumbnailPathSmall()
                            val gifVersion = jpgVersion?.replace("_225.jpg", "_225.gif")
                            println("processing $gifVersion")

                            val gifFile = File(gifVersion!!)
                            if (!gifFile.exists()) {
                                println("gif doesn't exist")

                                ImageProcessing.createVideoGif(metadata.getId(), metadataRepository)
                                localIndex++
                                println("processed $gifVersion")
                            } else {
                                println("already exists $gifVersion")
                            }

                            println("-------------")
                        }
                        val endTime = System.currentTimeMillis()
                        timesArray.add((endTime-elapsedStartTime))
                        etr = progress(currentIndex, totalIndex, timesArray)
                        currentIndex++
                    }
                    println("Number gifs processed: $localIndex")
                }
                activeLink = ""
                FileUtils.deleteThreadFiles("repairscripts")
            }.start()
        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/createxsmall"], method = [RequestMethod.GET])
    @ResponseBody
    fun createXSImages(response: HttpServletResponse?): String? {
        activeLink = "createxsmall"

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {

            Thread {
                // Retroactively create xsmall images
                val allMetadataList = metadataRepository.findAll()
                val metadataRecordsList = mutableListOf<Metadata>()

                totalIndex = allMetadataList.count()
                currentIndex = 0
                startTime = System.currentTimeMillis()
                var localIndex = 0
                val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                val sidecarDir = rootPath + relativeSidecarDir
                val timesArray = mutableListOf<Long>()
                val x = 100
                for ((index, metadata) in allMetadataList.withIndex()) {
                    val elapsedStartTime = System.currentTimeMillis()
                    println("iteration ${index + 1} out of $totalIndex")
                    if (metadata != null) {
                        if (metadata.getThumbnailPathExtraSmall() == null || !File(metadata.getThumbnailPathExtraSmall()!!).exists()) {
                            val imageProcessing = ImageProcessing("v1", File(metadata.getPath()!!), sidecarDir, metadata)
                            val metadataObj = imageProcessing.createThumbnails()
                            if (metadataObj != null) {
                                metadataRecordsList.add(metadataObj)
                                println("processed thumbnail ${metadataObj.getThumbnailPathExtraSmall()}")

                                // Save every 100 records
                                if (metadataRecordsList.size % x == 0) {
                                    println("Batch saving $x records")
                                    metadataRepository.saveAll(metadataRecordsList)
                                    metadataRecordsList.clear()
                                }
                                localIndex++
                            }
                        }
                    }
                    val endTime = System.currentTimeMillis()
                    timesArray.add((endTime-elapsedStartTime))
                    etr = progress(currentIndex, totalIndex, timesArray)
                    currentIndex++
                }

                if (metadataRecordsList.size > 0) {
                    metadataRepository.saveAll(metadataRecordsList)
                }

                println("Number xs thumbnails processed: $localIndex")
                activeLink = ""
                FileUtils.deleteThreadFiles("repairscripts")
            }.start()
        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/fixtakendates"], method = [RequestMethod.GET])
    @ResponseBody
    fun reconcileTakenDates(response: HttpServletResponse): String {
        if (activeLink.isBlank()) {
            activeLink = "fixtakendates"
        }

        println("repairscripts thread exists: "+FileUtils.checkThreadFileAlive("repairscripts"))

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {

            Thread {
                val metadataRecords = metadataRepository.findAll()
                val metadataRecordsList = mutableListOf<Metadata>()

                var localIndex = 0
                currentIndex = 0
                startTime = System.currentTimeMillis()
                totalIndex = metadataRecords.count()
                val timesArray = mutableListOf<Long>()
                val x = 100
                for (metadata in metadataRecords) {
                    val elapsedStartTime = System.currentTimeMillis()
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
                                println("iteration ${localIndex + 1} out of ${metadataRecords.count()}")
                                localIndex++
                                println("TakenDates don't match, fixing...")
                                println("metadata ID: ${metadata.getId()}")
                                println("originalTaken: $fullDate")
                                println("adjustedTaken: $adjustedTakenDateProp")
                                println("------------")
                                metadata.setTakenAt(fullDate)
                                metadataRecordsList.add(metadata)

                                // Save every 100 records
                                if (metadataRecordsList.size % x == 0) {
                                    println("Batch saving $x records")
                                    metadataRepository.saveAll(metadataRecordsList)
                                    metadataRecordsList.clear()
                                }
                            }
                        }
                    }
                    val endTime = System.currentTimeMillis()
                    timesArray.add((endTime-elapsedStartTime))
                    etr = progress(currentIndex, totalIndex, timesArray)
                    currentIndex++
                }

                if (metadataRecordsList.size > 0) {
                    metadataRepository.saveAll(metadataRecordsList)
                }
                println("# of records not matching: $localIndex")
                activeLink = ""
                FileUtils.deleteThreadFiles("repairscripts")
            }.start()
        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/fixnullplacenames"], method = [RequestMethod.GET])
    @ResponseBody
    fun fixNullPlaceNames(response: HttpServletResponse): String {
        if (activeLink.isBlank()) {
            activeLink = "fixnullplacenames"
        }

        println("repairscripts thread exists: "+FileUtils.checkThreadFileAlive("repairscripts"))

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {
            Thread {
                val mids = testRepository.findLocationsWithNullPlace()
                val metadataRecordsList = mutableListOf<Metadata>()

                var localIndex = 0
                currentIndex = 0
                startTime = System.currentTimeMillis()
                if (mids != null) {
                    totalIndex = mids.count()
                    val timesArray = mutableListOf<Long>()
                    val x = 100
                    for (metadataId in mids) {
                        val elapsedStartTime = System.currentTimeMillis()

                        if (metadataRepository.count() > 0) {

                            val metadata = metadataRepository.findByMetadataId(metadataId)

                            if (metadata != null) {
                                if (metadata.getLat() != null && metadata.getLat() != null && metadata.getPlaceName() == null) {
                                    println(metadata.toString())
                                    val coordinateMap =
                                        TextUtils.processCoordinates(
                                            geocodeUrl!!,
                                            metadata.getLat() + "," + metadata.getLng()
                                        )
                                    if (coordinateMap["place"] != null) {
                                        println("iteration ${localIndex + 1} out of ${mids.count()}")
                                        localIndex++
                                        println("Location not found, fixing...")
                                        println("metadata ID: ${metadata.getId()}")
                                        println("location: ${coordinateMap["place"]}")

                                        println("------------")
                                        metadata.setPlaceName(coordinateMap["place"])
                                        metadataRecordsList.add(metadata)

                                        // Save every 100 records
                                        if (metadataRecordsList.size % x == 0) {
                                            println("Batch saving $x records")
                                            metadataRepository.saveAll(metadataRecordsList)
                                            metadataRecordsList.clear()
                                        }
                                    }
                                }
                            }
                        }

                        val endTime = System.currentTimeMillis()
                        timesArray.add((endTime-elapsedStartTime))
                        etr = progress(currentIndex, totalIndex, timesArray)
                        currentIndex++
                    }
                }

                if (metadataRecordsList.size > 0) {
                    metadataRepository.saveAll(metadataRecordsList)
                }
                println("# of records missing location data: $localIndex")
                activeLink = ""
                FileUtils.deleteThreadFiles("repairscripts")
            }.start()
        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/rescansearchedplacenames"], method = [RequestMethod.GET])
    @ResponseBody
    fun rescanSearchedPlaceNames(response: HttpServletResponse, @RequestParam query: Optional<String>): String {
        if (activeLink.isBlank()) {
            activeLink = "rescansearchedplacenames"
        }

        println("repairscripts thread exists: "+FileUtils.checkThreadFileAlive("repairscripts"))

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {

            val search = query.orElse("")

            if (search.isNotEmpty() && search.isNotBlank()) {
                Thread {
                    val mids = testRepository.findByPlaceName(query.get())
                    val metadataRecordsList = mutableListOf<Metadata>()

                    println("query: ${query.get()}")

                    var localIndex = 0
                    currentIndex = 0
                    startTime = System.currentTimeMillis()
                    val x = 100
                    if (mids != null) {
                        totalIndex = mids.count()
                        val timesArray = mutableListOf<Long>()
                        for (metadata in mids) {
                            val elapsedStartTime = System.currentTimeMillis()
                            if (metadata.getLat() != null && metadata.getLat() != null) {
                                println(metadata.toString())
                                val coordinateMap =
                                    TextUtils.processCoordinates(geocodeUrl!!, metadata.getLat() + "," + metadata.getLng())
                                if (coordinateMap["place"] != null) {
                                    println("iteration ${localIndex + 1} out of ${mids.count()}")
                                    localIndex++
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
                            val endTime = System.currentTimeMillis()
                            timesArray.add((endTime-elapsedStartTime))
                            etr = progress(currentIndex, totalIndex, timesArray)
                            currentIndex++
                        }
                    }

                    if (metadataRecordsList.size > 0) {
                        metadataRepository.saveAll(metadataRecordsList)
                    }
                    println("# of records with rescanned location data: $localIndex")
                    activeLink = ""
                    FileUtils.deleteThreadFiles("repairscripts")
                }.start()
            } else {
                FileUtils.deleteThreadFiles("repairscripts")
                println("Proceed flag is false")
                activeLink = ""
            }
        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/rescanplacenames"], method = [RequestMethod.GET])
    @ResponseBody
    fun rescanAllPlaceNames(response: HttpServletResponse, @RequestParam proceed: Optional<Boolean>): String {
        if (activeLink.isBlank()) {
            activeLink = "rescanplacenames"
        }

        println("repairscripts thread exists: "+FileUtils.checkThreadFileAlive("repairscripts"))

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {
            val proceedRescan = proceed.orElse(false)

            if (proceedRescan) {
                Thread {
                    val mids = metadataRepository.findAll()
                    val metadataRecordsList = mutableListOf<Metadata>()

                    var localIndex = 0
                    currentIndex = 0
                    totalIndex = mids.count()
                    startTime = System.currentTimeMillis()
                    val x = 100
                    val timesArray = mutableListOf<Long>()
                    for (metadata in mids) {
                        val elapsedStartTime = System.currentTimeMillis()
                        if (metadata?.getLat() != null && metadata.getLat() != null) {
                            println(metadata.toString())
                            val coordinateMap =
                                TextUtils.processCoordinates(geocodeUrl!!, metadata.getLat() + "," + metadata.getLng())

                            if (coordinateMap["place"] != null) {
                                println("iteration ${localIndex + 1} out of ${mids.count()}")
                                localIndex++
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
                        val endTime = System.currentTimeMillis()
                        timesArray.add((endTime-elapsedStartTime))
                        etr = progress(currentIndex, totalIndex, timesArray)
                        currentIndex++
                    }

                    if (metadataRecordsList.size > 0) {
                        metadataRepository.saveAll(metadataRecordsList)
                    }
                    println("# of records with rescanned location data: $localIndex")
                    activeLink = ""
                    FileUtils.deleteThreadFiles("repairscripts")
                }.start()
            } else {
                FileUtils.deleteThreadFiles("repairscripts")
                println("Proceed flag is false")
                activeLink = ""
            }
        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/rescanflengths"], method = [RequestMethod.GET])
    @ResponseBody
    fun rescanFLengths(response: HttpServletResponse): String {
        if (activeLink.isBlank()) {
            activeLink = "rescanflengths"
        }

        println("repairscripts thread exists: "+FileUtils.checkThreadFileAlive("repairscripts"))

        if (!FileUtils.checkThreadFileAlive("repairscripts") && FileUtils.createThreadFile("repairscripts") != null) {
            Thread {
                val mids = testRepository.findAllFocalLengths()
                val metadataRecordsList = mutableListOf<Metadata>()

                if (!mids.isNullOrEmpty()) {
                    var localIndex = 0
                    currentIndex = 0
                    totalIndex = mids.count()
                    startTime = System.currentTimeMillis()
                    val x = 100
                    val timesArray = mutableListOf<Long>()
                    val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                    val sidecarDir = rootPath + relativeSidecarDir
                    var rescannedMetadata: Metadata
                    var prevFocalLength: Double

                    for (metadata in mids) {
                        val elapsedStartTime = System.currentTimeMillis()
                        if (metadata.getFocalLength() != null) {
                            prevFocalLength = metadata.getFocalLength()!!

                            val metadataProcessing = MetadataProcessing(
                                "v1",
                                File(metadata.getPath()!!),
                                sidecarDir,
                                metadata,
                                geocodeUrl!!
                            )
                            rescannedMetadata = metadataProcessing.populateMetadata()

                            if (rescannedMetadata.getFocalLength() != prevFocalLength) {
                                println("iteration ${localIndex + 1} out of ${mids.count()}")
                                localIndex++
                                println("metadata ID: ${metadata.getId()}")
                                println("Old fLength: $prevFocalLength")
                                println("New fLength: ${rescannedMetadata.getFocalLength()}")

                                metadataRecordsList.add(rescannedMetadata)

                                // Save every 100 records
                                if (metadataRecordsList.size % x == 0) {
                                    println("Batch saving $x records")
                                    metadataRepository.saveAll(metadataRecordsList)
                                    metadataRecordsList.clear()
                                }

                                println("------------")
                            }
                        }
                        val endTime = System.currentTimeMillis()
                        timesArray.add((endTime-elapsedStartTime))
                        etr = progress(currentIndex, totalIndex, timesArray)
                        currentIndex++
                    }

                    if (metadataRecordsList.size > 0) {
                        metadataRepository.saveAll(metadataRecordsList)
                    }

                    println("# of records with rescanned focal length data: $localIndex")
                }
                activeLink = ""
                FileUtils.deleteThreadFiles("repairscripts")
            }.start()

        } else {
            println("$activeLink already running")
        }

        return "sandbox"
    }

    private fun progress(position: Int, total: Int, times: MutableList<Long>): Long {
//        val elapsedTimeFromStart = System.currentTimeMillis() - startTime
        var estimatedRemaining: Long = 0

        println("Progress: start time is $startTime")

        if (position > 0 && times.isNotEmpty()) {
//            estimatedRemaining = (elapsedTimeFromStart / position) * (total - position)

            val sum = times.sum()
            val average = sum / times.size
            estimatedRemaining = (total*average)-((position+1)*average)

        }

        val averageTime = times.sum() / times.size
        println("Average time for each iteration $averageTime ms")
        println("ETR: $estimatedRemaining")

        return estimatedRemaining
    }
}