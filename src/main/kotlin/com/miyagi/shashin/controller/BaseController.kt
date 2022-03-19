package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.RecognitionLabel
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set

@Controller
class BaseController {

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var albumRepository: AlbumRepository? = null

    @Autowired
    private var keywordRepository: KeywordRepository? = null

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    protected fun getAllAttribueData(model: Model): MutableMap<String, Any> {
        val response = mutableMapOf<String,Any>()

        model["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
        if (recognitionLabels != null && recognitionLabels.count() > 0) {
            model["recognitionLabels"] = recognitionLabels
        }

        model["allAlbumList"] = mutableListOf<Album>()
        val allAlbumList = albumRepository?.findAllOrderByAlbumName()
        if (allAlbumList != null && allAlbumList.count() > 0) {
            model["allAlbumList"] = allAlbumList
        }

        model["timeOffsets"] = TextUtils.timeOffsets()

        val keywordList = keywordRepository?.findAllDistinctOrderByKeyword()
        var keywords = ""
        if (keywordList != null && keywordList.count() > 0) {
            keywords = keywordList.map { it.getKeyword() }.joinToString(",")
        }
        model["keywords"] = keywords

        model["cameras"] = ""
        val cameraList = metadataRepository?.findByCameraTypeAlphabetical()
        if (cameraList != null && cameraList.count() > 0) {
            model["cameras"] = cameraList.joinToString()
        }

        response["recognitionLabels"] = model.getAttribute("recognitionLabels") as Any
        response["allAlbumList"] = model.getAttribute("allAlbumList") as Any
        response["timeOffsets"] = model.getAttribute("timeOffsets") as Any
        response["keywords"] = model.getAttribute("keywords") as Any
        response["cameras"] = model.getAttribute("cameras") as Any

        return response
    }
}