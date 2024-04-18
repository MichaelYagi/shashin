package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
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
    private val jdbcTemplate: JdbcTemplate? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    private var shouldStop = AtomicBoolean(false)

    private val threadExtensionName: String = "facescan_shashinscan"

    @Value("\${app.sidecar.path}")
    private val relativeSidecarDir: String? = null

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["somevalue"] = "This is a test"

        val userAgent = request.getHeader("User-Agent")


        val url = URL("https://www.pexels.com/")
        val conn = url.openConnection()
        conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        val `is` = conn.getInputStream()
        val br = BufferedReader(InputStreamReader(`is`))
        var line: String? = null
        val sb = StringBuffer()
        while ((br.readLine().also { line = it }) != null) {
            sb.append(line)
        }
        val htmlContent = sb.toString()
        println("testzzz")
        println(htmlContent)



        val doc = Jsoup.connect("https://www.pexels.com/")
            .method(Connection.Method.GET)
            .userAgent(userAgent)
            .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
            .header("Accept-Language","en-CA,en;q=0.9,ja-JP;q=0.8,ja;q=0.7,en-GB;q=0.6,en-US;q=0.5")
            .header("Cache-Control","no-cache")
            .header("Cookie","ab.storage.deviceId.5791d6db-4410-4ace-8814-12c903a548ba=g%3A79befdc3-189c-4679-8468-67ca1d388303%7Ce%3Aundefined%7Cc%3A1708142471100%7Cl%3A1708142471100; ab.storage.sessionId.5791d6db-4410-4ace-8814-12c903a548ba=g%3A6d8be8ab-20f6-0c65-c5b6-37115dd55f71%7Ce%3A1708144271105%7Cc%3A1708142471097%7Cl%3A1708142471105; country-code-v2=CA; OptanonConsent=isGpcEnabled=0&datestamp=Tue+Apr+16+2024+18%3A27%3A59+GMT-0700+(Pacific+Daylight+Time)&version=202301.1.0&isIABGlobal=false&hosts=&landingPath=NotLandingPage&groups=C0001%3A1%2CC0002%3A0%2CC0003%3A0%2CC0004%3A0&AwaitingReconsent=false; _sp_id.9ec1=fea7ac91-e04a-4135-9279-8c298d5de7bc.1708142471.3.1713317280.1713313573.91eb0838-50ba-4311-aab6-b99644156a98.c5641ced-d938-4a64-872d-2ae6d0ce3e3f.57ec68e6-be62-4ae6-975f-804f40cf8baf.1713317279930.1; cf_clearance=G7v2936XTGZY4yaucm.RhJEEm8yf19kwAt_PZp4Jz2s-1713317280-1.0.1.1-l8lvMn9oKiJ6jwwnwAZ4Ngox4i3QmQD1uS58grZidkuef6lfiL5hR5_4wj5iRipQuK5jlFOQhJL_E9we.I9RFw")
            .header("Pragma","no-cache")
            .header("Sec-Ch-Ua","\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"")
            .header("Sec-Ch-Ua-Mobile","\"?0")
            .header("Sec-Ch-Ua-Platform","Windows")
            .header("Sec-Fetch-Dest","document")
            .header("Sec-Fetch-Mode","navigate")
            .header("Sec-Fetch-Site","none")
            .header("Sec-Fetch-User","\"?1")
            .header("Upgrade-Insecure-Requests","1")
            .get()

        val imgTags = doc.getElementsByTag("img")
        println("imgTags")
        println(imgTags)

//        val settings = model.getAttribute("settings") as Settings
//
//        val tempDir = System.getProperty("java.io.tmpdir")
//
//        if (!FileUtils.checkThreadFileAlive(threadExtensionName)) {
//            // Clean up any existing thread files
//            FileUtils.deleteThreadFiles(threadExtensionName)
//
//            Thread {
//                val threadFile = FileUtils.createFile(
//                    tempDir,
//                    tempDir + "/" + Thread.currentThread().name + "." + threadExtensionName,
//                    "Thread"
//                )
//
//                // Object and person recognition
//                if (threadFile != null) {
//
//                    val testImages = metadataRepository.findNonMatched(settings.getMatchScanLimit()!!)
//                    val trainingData = metadataRepository.findTrainingData(
//                        settings.getRecognitionConfidenceThreshold()!!,
//                        settings.getTrainingDataLimit()!!
//                    )
//                    val distinctLabelRecords = recognitionLabelPhotoRepository.findGroupByRecognitionLabelId()
//
//                    if (distinctLabelRecords.count() > 0) {
//                        println("running recognizer")
//                        val faceRecognizer = DjlFaceRecognizer(
//                            testImages,
//                            trainingData,
//                            recognitionLabelPhotoRepository,
//                            recognitionLabelRepository,
//                            settings,
//                            relativeSidecarDir!!,
//                            threadFile
//                        )
////                        val facesRecognized = faceRecognizer.startPredict()
////                        println(facesRecognized)
//
//                        //            faceRecognizer.test()
//                    } else {
//                        println("no labels found. start tagging people")
//                    }
//                }
//            }.start()
//        }

//
//        // Start matching in a separate thread
//        if (distinctLabelRecords.count() > 0) {
//            val faceRecognizer = FaceRecognizer(
//                testImages,
//                trainingData,
//                recognitionLabelPhotoRepository
//                //,
////                recognitionLabelRepository,
////                notificationRepository,
////                userRepository,
////                adminRole,
////                settings.getRecognitionConfidenceThreshold()!!.toDouble()
//            )
//            faceRecognizer.runRecognizer()
//            shouldStop.set(false)
//        }




//        val metricsUtil = MetricsUtil()
//        metricsUtil.start("query test 1")
//
//        val metadataList1 = metadataRepository.findAllByYearAndMonthAndDayAndHiddenEqualsOrderByYearDescMonthDescDayDescTimeDesc(
//            2018, 12, 26, hidden = false
//        )
//
//        println(metadataList1)
//
//
//        metricsUtil.end()

//        val metadataList2 = metadataRepository.findAllByTypeAndYearAndMonthAndDayFocused(
//            "image",
//            2018, 12, 26
//        ).toMutableList()
//
//        println(metadataList2)

//        metricsUtil.start("query test 2")


//        try {
//        val metadataList =
//            metadataRepository.findTimelineDateFocused(
//                2018, 12, 26
//            )
//
//        for (metadata in metadataList) {
//            println(metadata.getThumbnailUrlExtraSmall())
//        }

//        } catch(e: Exception) {
//            println("Error")
//            println(e.stackTraceToString())
//        }

//        metricsUtil.end()



//        val persistentLoginsDetails = persistentLoginsRepository.findAllPersistentLoginsDetails()
//        model["persistentLoginsDetails"] = persistentLoginsDetails as Any

        // http://127.0.0.1:6624/image/68bcd16b-b362-304c-b521-f21bb6ee23d3/viewer
//        val metadataId = "42722f17-65de-3d6c-aa6f-dbe6dbb257f9"
//        val metadataObj = metadataRepository.findById(metadataId).get()


//        val file = File(metadataObj.getPath()!!)

//        val img = ImageFactory.getInstance().fromFile(Path(metadataObj.getPath()!!))

//        for (modelObj in ModelZoo.listModels()) {
//            println(modelObj)
//        }

//        val criteria: Criteria<Image, DetectedObjects> = Criteria.builder()
//            .optApplication(Application.CV.OBJECT_DETECTION)
//            .setTypes(Image::class.java, DetectedObjects::class.java)
//            .optEngine(Engine.getDefaultEngineName())
//            .optFilter("backbone", "resnet50")
//            .optProgress(ProgressBar())
//            .build()
//
//        ModelZoo.loadModel(criteria).use { objmodel ->
//            objmodel.newPredictor().use { predictor ->
//                try {
//                    val detection = predictor.predict(img)
//                    println("testzzzz12")
//                    println(detection.numberOfObjects)
//                    for (i in 0..detection.numberOfObjects) {
//                        println("testzzz")
//                        println(detection.item<Classifications.Classification?>(i).probability)
//                        println(detection.item<Classifications.Classification?>(i).className)
//                    }
//                    //println(detection)
//
//                } catch (e: Exception) {
//                    println(e.message)
//                }
//            }
//        }

        return "test"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/testvideo"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun getTestVideo(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testVideo/PXL_20210725_213342002.mp4";
        return FileSystemResource(path)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/testimage"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    fun getTestImage(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testData/anotherDir/DSCF1061.JPG";
        return FileSystemResource(path)
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @RequestMapping(value = ["/testaudio"], method = [RequestMethod.GET], produces = ["audio/3gpp","audio/aac","audio/flac","audio/mpeg","audio/mp3","audio/mp4","audio/ogg","audio/wav","audio/webm"])
    @ResponseBody
    fun getTestAudio(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/some/audio.mp3";
        return FileSystemResource(path)
    }
}