package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.ImageProcessingUtils
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
    private lateinit var relativeSidecarDir: String
    @Value("\${app.api.version}")
    private var apiVersion: String? = null
    @Value("\${app.mediaDir}")
    lateinit var rootMediaDir: String

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
            metadataObj.get().setLat(metadataFormUpdateData.getLat())
            metadataObj.get().setLng(metadataFormUpdateData.getLng())

            // Update DB
            metadataRepository?.save(metadataObj.get())
            // Update MD file
            val imageProcessingUtils = ImageProcessingUtils(apiVersion)
            imageProcessingUtils.saveMetadata(metadataObj.get(), relativeSidecarDir, rootMediaDir)
            return "{\"status\":\"success\",\"msg\":\"Saved\"}"
        }
        return "{\"status\":\"fail\",\"msg\":\"Not Saved\"}"
    }

    @RequestMapping(value = ["/timeline/update/batch"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateBatchMetadata(@RequestBody requestBody: JsonNode): String? {
        val mapper = ObjectMapper()
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        var idArray: Array<String>? = null
        var dayTaken: Int? = null
        var monthTaken: Int? = null
        var yearTaken: Int? = null
        var lat: String? = null
        var lng: String? = null

        for ((k, v) in batchMetadataMap) {
            if (v != "") {

                when (k) {
                    "batchMetadataIds" -> {
                        idArray = mapper.readValue(v.toString(), Array<String>::class.java)
                    }
                    "dayTakenBatchData" -> {
                        dayTaken = v.toString().toInt()
                    }
                    "monthTakenBatchData" -> {
                        monthTaken = v.toString().toInt()
                    }
                    "yearTakenBatchData" -> {
                        yearTaken = v.toString().toInt()
                    }
                    "latBatchData" -> {
                        lat = v.toString()
                    }
                    "lngBatchData" -> {
                        lng = v.toString()
                    }
                }
            }
        }

        if (!idArray.isNullOrEmpty()) {
            val metadataList: ArrayList<Metadata> = ArrayList()

            for (id in idArray) {
                val metadataObj: Optional<Metadata?>? = metadataRepository?.findById(id)

                if (metadataObj != null) {
                    val metadata = metadataObj.get()

                    if (dayTaken != null) {
                        metadata.setDay(dayTaken)
                    }
                    if (monthTaken != null) {
                        metadata.setMonth(monthTaken)
                    }
                    if (yearTaken != null) {
                        metadata.setYear(yearTaken)
                    }
                    if (lat != null) {
                        metadata.setLat(lat)
                    }
                    if (lng != null) {
                        metadata.setLng(lng)
                    }

                    metadataList.add(metadata)
                }
            }

            if (metadataList.isNotEmpty()) {
                // Update DB
                metadataRepository?.saveAll(metadataList)

                // Update MD file
                val imageProcessingUtils = ImageProcessingUtils(apiVersion)
                for (metadata in metadataList) {
                    imageProcessingUtils.saveMetadata(metadata, relativeSidecarDir, rootMediaDir)
                }
                return "{\"status\":\"success\",\"msg\":\"Saved\"}"
            }
        }

        return "{\"status\":\"fail\",\"msg\":\"Not Saved\"}"
    }
}