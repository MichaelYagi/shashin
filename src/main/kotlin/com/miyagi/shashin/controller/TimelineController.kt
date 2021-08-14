package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.*


@Controller
class TimelineController {

    @Autowired
    private val metadataRepository: MetadataRepository? = null
    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null
    @Value("\${app.photoDir}")
    lateinit var rootPhotoDir: String

    @GetMapping("/timeline")
    fun getTimeline(model: Model): String {
        val module = "timeline"

        model["metadataList"] = ""
        val sort = Sort.by(
            Sort.Order.desc("year"),
            Sort.Order.desc("month"),
            Sort.Order.desc("day")
        )
        val metadataList = metadataRepository?.findAll(sort)
        if (metadataList != null) {
            model["metadataList"] = metadataList
        }

        model["data"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/timeline/update/{metadataId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateMetadata(@RequestBody metadataFormUpdateData: Metadata, @PathVariable metadataId: String): String? {
        val metadataObj: Optional<Metadata?>? = metadataRepository?.findById(metadataId)
        if (metadataObj != null) {
            metadataObj.get().setYear(metadataFormUpdateData.getYear())
            metadataObj.get().setMonth(metadataFormUpdateData.getMonth())
            metadataObj.get().setDay(metadataFormUpdateData.getDay())

            // Update DB
            metadataRepository?.save(metadataObj.get())
            // Update MD file
            val rootPath = FileSystemResource("").file.absolutePath
            val sidecarDir = rootPath + relativeSidecarDir
            val metadataDirectory = sidecarDir.dropLast(1) + "/metadata"
            val rootDirFile = File(rootPhotoDir)
            val photoFile = File(metadataObj.get().getPath())
            var fileRootDir: String = (photoFile.parent).lowercase().replace((rootDirFile.canonicalPath).lowercase(), "")
            fileRootDir = fileRootDir.replace('\\', '/')
            val metadataFileStr = metadataDirectory + fileRootDir + "/" + photoFile.name + ".yaml"
            val mdFile = File(metadataFileStr)
            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build()
            val om = ObjectMapper(yamlFactory)
            om.writeValue(mdFile, metadataObj.get());
            return "{\"status\":\"success\",\"msg\":\"Saved\"}"
        }
        return "{\"status\":\"fail\",\"msg\":\"Not Saved\"}"
    }
}