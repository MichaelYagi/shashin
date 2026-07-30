package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.Keyword
import com.miyagi.shashin.model.RecognitionLabel
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set

@Controller
class BaseController(
    private var recognitionLabelRepository: RecognitionLabelRepository? = null,
    private var albumRepository: AlbumRepository? = null,
    private var keywordRepository: KeywordRepository? = null,
    private var metadataRepository: MetadataRepository? = null
) {
    fun getAllAttributeData(model: Model): MutableMap<String, Any> {
        val response = mutableMapOf<String,Any>()

        model["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining(TextUtils.getObjectName())
        if (recognitionLabels != null && recognitionLabels.count() > 0) {
            model["recognitionLabels"] = recognitionLabels
        }

        model["allAlbumList"] = mutableListOf<Album>()
        val allAlbumList = albumRepository?.findAllOrderByAlbumName()
        if (allAlbumList != null && allAlbumList.count() > 0) {
            model["allAlbumList"] = allAlbumList
        }

        model["timeOffsets"] = TextUtils.timeOffsets()
        var keywords = ""
        val keywordList: MutableIterable<Keyword>? = keywordRepository?.findAllDistinctOrderByKeyword()
        if (keywordList != null && keywordList.count() > 0) {
            keywords = keywordList.map { it.getKeyword() }.joinToString(",")
        }
        model["keywords"] = keywords

        model["cameras"] = ""
        val cameraList = metadataRepository?.findByCameraTypeAlphabetical()
        if (cameraList != null && cameraList.count() > 0) {
            model["cameras"] = cameraList.joinToString()
        }

        model["lenses"] = ""
        val lensList = metadataRepository?.findByLensTypeAlphabetical()
        if (lensList != null && lensList.count() > 0) {
            model["lenses"] = lensList.joinToString()
        }

        response["recognitionLabels"] = model.getAttribute("recognitionLabels") as Any
        response["allAlbumList"] = model.getAttribute("allAlbumList") as Any
        response["timeOffsets"] = model.getAttribute("timeOffsets") as Any
        response["keywords"] = model.getAttribute("keywords") as Any
        response["cameras"] = model.getAttribute("cameras") as Any
        response["lenses"] = model.getAttribute("lenses") as Any

        return response
    }
}