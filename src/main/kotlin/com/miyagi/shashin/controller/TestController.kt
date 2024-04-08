package com.miyagi.shashin.controller

import ai.djl.modality.cv.Image
import ai.djl.repository.zoo.Criteria
import com.miyagi.shashin.ShashinApplication
import com.miyagi.shashin.component.DjlFaceRecognizer
import com.miyagi.shashin.model.MetadataFocused
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.*
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
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.persistence.EntityManager
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


    @Secured("ROLE_SUPER","ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["somevalue"] = "This is a test"

        val settings = model.getAttribute("settings") as Settings


        val testImages = metadataRepository.findNonMatched(settings.getMatchScanLimit()!!)
        val trainingData = metadataRepository.findTrainingData(settings.getRecognitionConfidenceThreshold()!!, settings.getTrainingDataLimit()!!)
        val distinctLabelRecords = recognitionLabelPhotoRepository.findGroupByRecognitionLabelId()

        if (distinctLabelRecords.count() > 0) {
            println("running recognizer")
            val faceRecognizer = DjlFaceRecognizer(testImages, trainingData, recognitionLabelPhotoRepository, settings, model)
            faceRecognizer.startPredict()
        } else {
            println("no labels found. start tagging people")
        }




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