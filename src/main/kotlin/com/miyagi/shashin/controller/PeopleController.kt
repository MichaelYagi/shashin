package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.RecognitionLabelPhotoRepository
import com.miyagi.shashin.repository.RecognitionLabelRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.query.Param
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.ArrayList
import java.util.HashMap

@Controller
class PeopleController {

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    @Autowired
    private var albumRepository: AlbumRepository? = null

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/person/matches/{personId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPredictions(model: Model, @PathVariable personId: Int): String {
        val module = "matches"
        model["data"] = ""
        model["lowMatchResult"] = ""

        val settings = model.getAttribute("settings") as Settings
        // Get records of photos that haven't been confirmed - Threshold not 100.0 and greater than threshold configured
        val lowMatchResults = metadataRepository?.findLowMatchesByPerson(personId,settings.getRecognitionConfidenceThreshold()!!,settings.getMatchScanLimit()!!)
        if (lowMatchResults != null && lowMatchResults.count() > 0) {
            model["lowMatchResult"] = lowMatchResults
        } else {
            // Scan records of photos that haven't been scanned in a separate thread
            val testImages = metadataRepository?.findNonMatched(settings.getMatchScanLimit()!!)
            val trainingData = metadataRepository?.findTrainingData(settings.getRecognitionConfidenceThreshold()!!, settings.getTrainingDataLimit()!!)

            if (trainingData != null) {
                for (trainingObj in trainingData) {
                    println(trainingObj.getRecognitionLabelName())
                }
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/people")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getPeople(model: Model): String {
        val module = "people"
        model["data"] = "There are no people tagged."
        model["peopleList"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var peopleList: MutableIterable<MetadataPeople>? = null
            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                peopleList = metadataRepository?.findAlbumPhotoByPeople()
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                peopleList = metadataRepository?.findMetadataByPeople()
            }
            if (peopleList != null) {
                model["peopleList"] = peopleList
                model["data"] = ""
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/person/{personId}"], method = [RequestMethod.GET])
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    fun getPagedTimeline(model: Model, @PathVariable personId: Int): String {
        val module = "person"
        val page = 0
        val response = buildPersonAlbum(model,personId,page)
        model["data"] = response["data"]!!
        model["metadataList"] = response["metadataList"]!!
        model["recognitionLabels"] = response["recognitionLabels"]!!
        model["labelPhotoMap"] = response["labelPhotoMap"]!!
        model["personInfo"] = response["personInfo"]!!
        model["parameter"] = response["parameter"]!!

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildPersonAlbum(model: Model,personId: Int,page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["data"] = "There are no photos.."
        response["metadataList"] = ""
        response["recognitionLabels"] = ""
        response["labelPhotoMap"] = ""
        response["personInfo"] = ""
        response["parameter"] = personId

        response["msg"] = "Could not get results"
        response["status"] = "fail"

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            val recognitionLabel = recognitionLabelRepository?.findById(personId)
            if (recognitionLabel != null) {
                response["personInfo"] = recognitionLabel.get()
            }

            var metadataList: MutableIterable<Metadata>? = null
            if (currentUserObj!!.getAuthority() == model.getAttribute("userRole")) {
                metadataList = metadataRepository?.findAlbumPhotoByPerson(personId,currentUserObj.getId(),page,2000)
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                val recognitionLabels = recognitionLabelRepository?.findAll()
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    response["recognitionLabels"] = recognitionLabels
                }
                metadataList = metadataRepository?.findMetadataByPerson(personId,page,2000)
            }

            response["metadataList"] = metadataList
            if (metadataList != null && metadataList.count() > 0) {
                response["data"] = ""

                val labelPhotoMap = mutableMapOf<String, String>()
                for (metadata in metadataList) {
                    val recognitionLabelPhotos = recognitionLabelPhotoRepository?.findByMetadataId(metadata.getId())
                    var labelString = ""
                    if (recognitionLabelPhotos != null) {
                        for (recognitionLabelPhoto in recognitionLabelPhotos) {
                            val recognitionLabelObj = recognitionLabelRepository?.findById(recognitionLabelPhoto.getRecognitionLabelId()!!)
                            if (recognitionLabelObj != null) {
                                labelString += recognitionLabelObj.get().getName() + ","
                            }
                        }
                    }
                    if (labelString.isNotBlank()) {
                        labelString = labelString.dropLast(1)
                    }
                    labelPhotoMap[metadata.getId()] = labelString
                }
                response["labelPhotoMap"] = labelPhotoMap
            }

            response["metadataList"] = metadataList
            response["msg"] = "Results"
            response["status"] = "success"
        }

        return response
    }
}