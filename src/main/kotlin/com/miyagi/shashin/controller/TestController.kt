package com.miyagi.shashin.controller

import ai.djl.Application
import ai.djl.engine.Engine
import ai.djl.modality.Classifications
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ModelZoo
import ai.djl.training.util.ProgressBar
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.io.path.Path


@Controller
class TestController {

    @Autowired
    private lateinit var persistentLoginsRepository: PersistentLoginsRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Secured("ROLE_ADMIN")
    @GetMapping("/test")
    fun test(model: Model, request: HttpServletRequest, response: HttpServletResponse): String {
        model["somevalue"] = "This is a test"

        val persistentLoginsDetails = persistentLoginsRepository.findAllPersistentLoginsDetails()
        model["persistentLoginsDetails"] = persistentLoginsDetails as Any

        // http://127.0.0.1:6624/image/68bcd16b-b362-304c-b521-f21bb6ee23d3/viewer
        val metadataId = "42722f17-65de-3d6c-aa6f-dbe6dbb257f9"
        val metadataObj = metadataRepository.findById(metadataId).get()


//        val file = File(metadataObj.getPath()!!)

        val img = ImageFactory.getInstance().fromFile(Path(metadataObj.getPath()!!))

//        for (modelObj in ModelZoo.listModels()) {
//            println(modelObj)
//        }
//        CV.IMAGE_CLASSIFICATION=[ai.djl.pytorch/resnet/0.0.1/traced_resnet50 {"layers":"50","dataset":"imagenet"}, ai.djl.pytorch/resnet/0.0.1/traced_resnet18 {"layers":"18","dataset":"imagenet"}, ai.djl.pytorch/resnet/0.0.1/resnet101_v1 {"layers":"101","dataset":"imagenet"}, ai.djl.pytorch/resnet18_embedding/0.0.1/resnet18_embedding {}]
//        CV.IMAGE_GENERATION=[ai.djl.pytorch/cyclegan/0.0.1/style_cezanne {"artist":"cezanne"}, ai.djl.pytorch/cyclegan/0.0.1/style_monet {"artist":"monet"}, ai.djl.pytorch/cyclegan/0.0.1/style_ukiyoe {"artist":"ukiyoe"}, ai.djl.pytorch/cyclegan/0.0.1/style_vangogh {"artist":"vangogh"}, ai.djl.pytorch/biggan-deep/0.0.1/biggan-deep-128 {"layers":"12","size":"128","dataset":"imagenet"}, ai.djl.pytorch/biggan-deep/0.0.1/biggan-deep-256 {"layers":"24","size":"256","dataset":"imagenet"}, ai.djl.pytorch/biggan-deep/0.0.1/biggan-deep-512 {"layers":"12","size":"512","dataset":"imagenet"}]
//        CV.OBJECT_DETECTION=[ai.djl.pytorch/ssd/0.0.1/ssd_300_resnet50 {"size":"300","backbone":"resnet50","dataset":"coco"}]
//        CV.SEMANTIC_SEGMENTATION=[ai.djl.pytorch/deeplabv3/0.0.1/deeplabv3 {"backbone":"resnet50","flavor":"v1b","dataset":"coco"}]
//        NLP.QUESTION_ANSWER=[ai.djl.pytorch/bertqa/0.0.1/distilbert-base-uncased-distilled-squad {"modelType":"distilbert","size":"base","cased":"false","dataset":"SQuAD"}, ai.djl.pytorch/bertqa/0.0.1/distilbert-base-cased-distilled-squad {"modelType":"distilbert","size":"base","cased":"true","dataset":"SQuAD"}, ai.djl.pytorch/bertqa/0.0.1/trace_bertqa {"backbone":"bert","cased":"false","dataset":"SQuAD"}, ai.djl.pytorch/bertqa/0.0.1/trace_cased_bertqa {"backbone":"bert","cased":"true","dataset":"SQuAD"}, ai.djl.pytorch/bertqa/0.0.1/trace_distilbertqa {"backbone":"distilbert","cased":"true","dataset":"SQuAD"}]
//        NLP.SENTIMENT_ANALYSIS=[ai.djl.pytorch/distilbert/0.0.1/traced_distilbert_sst_english {"backbone":"distilbert","dataset":"sst"}]
//        TIMESERIES.FORECASTING=[ai.djl.pytorch/deepar/0.0.1/m5forecast {"dataset":"m5forecast"}]
        val criteria: Criteria<Image, DetectedObjects> = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image::class.java, DetectedObjects::class.java)
            .optEngine(Engine.getDefaultEngineName())
            .optFilter("backbone", "resnet50")
            .optProgress(ProgressBar())
            .build()

        ModelZoo.loadModel(criteria).use { objmodel ->
            objmodel.newPredictor().use { predictor ->
                try {
                    val detection = predictor.predict(img)
                    println("testzzzz12")
                    println(detection.numberOfObjects)
                    for (i in 0..detection.numberOfObjects) {
                        println("testzzz")
                        println(detection.item<Classifications.Classification?>(i).probability)
                        println(detection.item<Classifications.Classification?>(i).className)
                    }
                    //println(detection)

                } catch (e: Exception) {
                    println(e.message)
                }
            }
        }

        println(request.session.getAttribute("ComprefaceConnection"))


        return "test"
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/testvideo"], method = [RequestMethod.GET], produces = ["video/mp4","video/3gpp","video/mpeg","video/ogg","video/quicktime","video/webm"])
    @ResponseBody
    fun getTestVideo(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testVideo/PXL_20210725_213342002.mp4";
        return FileSystemResource(path)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/testimage"], method = [RequestMethod.GET], produces = ["image/apng","image/avif","image/gif","image/jpeg","image/png","image/svg+xml","image/svg+xml","image/webp"])
    @ResponseBody
    fun getTestImage(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/Users/micha/Downloads/testData/anotherDir/DSCF1061.JPG";
        return FileSystemResource(path)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/testaudio"], method = [RequestMethod.GET], produces = ["audio/3gpp","audio/aac","audio/flac","audio/mpeg","audio/mp3","audio/mp4","audio/ogg","audio/wav","audio/webm"])
    @ResponseBody
    fun getTestAudio(response: HttpServletResponse?): FileSystemResource? {
        val path = "c:/some/audio.mp3";
        return FileSystemResource(path)
    }
}