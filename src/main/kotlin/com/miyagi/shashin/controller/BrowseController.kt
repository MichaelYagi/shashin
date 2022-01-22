package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import javax.servlet.http.HttpServletRequest

@Controller
@Secured("ROLE_ADMIN")
class BrowseController {

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var albumPhotoRepository: AlbumPhotoRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Autowired
    private var recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @RequestMapping(value = ["/recent"], method = [RequestMethod.GET])
    fun getRecentlyAdded(model: Model): String {
        val module = "recent"
        val page = 0
        val response = buildRecentlyAdded(model,page)
        for ((k, v) in response) {
            model[k] = v!!
        }

        for ((k, v) in response) {
            model[k] = v!!
        }

        model["timeOffsets"] = TextUtils.timeOffsets()
        val keywordList = keywordRepository.findAll().map { it?.getKeyword() }
        val keywords = keywordList.joinToString(",")
        model["keywords"] = keywords
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/recent/{page}","/api/v1/recent/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedRecent(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildRecentlyAdded(model,page))
    }

    private fun buildRecentlyAdded(model: Model, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "There are no photos. Please setup directories in Settings and scan ."
        response["metadataList"] = ""
        response["favorites"] = ""
        response["albumList"] = ""
        response["recognitionLabels"] = ""
        response["labelPhotoMap"] = mutableMapOf<String, String>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, String>()
        response["keywordMap"] = mutableMapOf<String, String>()

        response["msg"] = "Could not get results"
        response["status"] = "fail"

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            val metadataList: MutableList<Metadata> = metadataRepository.findRecentByOffsetAndLimit(
                pageValue,
                model.getAttribute("queryLimit").toString().toInt()
            ).toMutableList()

            if (metadataList.isNotEmpty()) {
                response["metadataList"] = metadataList
                response["message"] = ""
                response["favorites"] = favoritesMap

                val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    response["recognitionLabels"] = recognitionLabels
                }

                val labelPhotoMap = mutableMapOf<String, String>()
                val albumMap = mutableMapOf<String, String>()
                val keywordMap = mutableMapOf<String, String>()

                for (metadata in metadataList) {
                    val favorites = favoriteRepository.findAllByMetadataId(metadata.getId())
                    if (favorites != null) {
                        for (favorite in favorites) {
                            if (favorite != null) {
                                favoritesMap[metadata.getId()] = hashMapOf(
                                    "favorite" to (favorite.getUserId() == currentUserObj?.getId()),
                                    "count" to favoriteRepository.countAllByMetadataId(metadata.getId())
                                )

                                if ((favorite.getUserId() == currentUserObj?.getId())) {
                                    break
                                }
                            }
                        }
                    }
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

                    val albumPhotos = albumPhotoRepository.findAlbumPhotoByMetadataId(metadata.getId())
                    if (albumPhotos != null) {
                        var albumMetadataList = ""
                        for (albumPhoto in albumPhotos) {
                            val album = albumRepository.findById(albumPhoto!!.getAlbumId()!!)
                            albumMetadataList += album.get().getName()+","
                        }
                        if (albumMetadataList.isNotEmpty()) {
                            albumMetadataList = albumMetadataList.dropLast(1)
                        }
                        albumMap[metadata.getId()] = albumMetadataList
                    }

                    val keywords = keywordRepository.findKeywordsByMetadataId(metadata.getId())
                    var keywordMetadataList = ""
                    for (keyword in keywords) {
                        keywordMetadataList += keyword.getKeyword()+","
                    }
                    if (keywordMetadataList.isNotEmpty()) {
                        keywordMetadataList = keywordMetadataList.dropLast(1)
                    }
                    keywordMap[metadata.getId()] = keywordMetadataList
                }
                response["labelPhotoMap"] = labelPhotoMap
                response["albumMap"] = albumMap
                response["keywordMap"] = keywordMap

                val albumList = albumRepository.findAll()
                if (albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = "Results"
            response["status"] = "success"
        }

        return response
    }

    @GetMapping("/folders")
    fun getFolders(model: Model): String {
        val response = buildFolders(model)
        for ((k, v) in response) {
            model[k] = v!!
        }
        return model.getAttribute("activePage").toString()
    }

    @RequestMapping(value = ["api/v1/folders"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getFoldersApi(model: Model): String {
        return mapper.writeValueAsString(buildFolders(model))
    }

    private fun buildFolders(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val module = "folders"
        response["message"] = "There are no folders."
        response["foldersList"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val folderObj = metadataRepository.findFolders()

            if (folderObj != null && folderObj.count() > 0) {
                response["foldersList"] = folderObj
                response["message"] = ""
            }
        }

        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)

        return response
    }

    @RequestMapping(value = ["/folder/{folder}"], method = [RequestMethod.GET])
    fun getRecentlyAdded(model: Model, @PathVariable folder: String): String {
        val module = "folder"
        val page = 0
        val response = buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        val keywordList = keywordRepository.findAll().map { it?.getKeyword() }
        val keywords = keywordList.joinToString(",")
        model["keywords"] = keywords
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/folder/{page}/{folder}","/api/v1/folder/{page}/{folder}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolder(model: Model, request: HttpServletRequest, @PathVariable page: Int, @PathVariable folder: String): String {
        return mapper.writeValueAsString(buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page))
    }

    private fun buildFolder(model: Model, folder: String, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "There are no photos.."
        response["metadataList"] = ""
        response["favorites"] = ""
        response["albumList"] = ""
        response["recognitionLabels"] = ""
        response["labelPhotoMap"] = mutableMapOf<String, String>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, String>()
        response["keywordMap"] = mutableMapOf<String, String>()
        response["folder"] = folder

        response["msg"] = "Could not get results"
        response["status"] = "fail"

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            val metadataList: MutableList<Metadata> = metadataRepository.findAllByFolderOffsetAndLimit(
                folder,
                pageValue,
                model.getAttribute("queryLimit").toString().toInt()
            ).toMutableList()

            if (metadataList.isNotEmpty()) {
                response["metadataList"] = metadataList
                response["message"] = ""
                response["favorites"] = favoritesMap

                val recognitionLabels = recognitionLabelRepository?.findAllByNameNotContaining("object")
                if (recognitionLabels != null && recognitionLabels.count() > 0) {
                    response["recognitionLabels"] = recognitionLabels
                }

                val labelPhotoMap = mutableMapOf<String, String>()
                val albumMap = mutableMapOf<String, String>()
                val keywordMap = mutableMapOf<String, String>()

                for (metadata in metadataList) {
                    val favorites = favoriteRepository.findAllByMetadataId(metadata.getId())
                    if (favorites != null) {
                        for (favorite in favorites) {
                            if (favorite != null) {
                                favoritesMap[metadata.getId()] = hashMapOf(
                                    "favorite" to (favorite.getUserId() == currentUserObj?.getId()),
                                    "count" to favoriteRepository.countAllByMetadataId(metadata.getId())
                                )

                                if ((favorite.getUserId() == currentUserObj?.getId())) {
                                    break
                                }
                            }
                        }
                    }
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

                    val albumPhotos = albumPhotoRepository.findAlbumPhotoByMetadataId(metadata.getId())
                    if (albumPhotos != null) {
                        var albumMetadataList = ""
                        for (albumPhoto in albumPhotos) {
                            val album = albumRepository.findById(albumPhoto!!.getAlbumId()!!)
                            albumMetadataList += album.get().getName()+","
                        }
                        if (albumMetadataList.isNotEmpty()) {
                            albumMetadataList = albumMetadataList.dropLast(1)
                        }
                        albumMap[metadata.getId()] = albumMetadataList
                    }

                    val keywords = keywordRepository.findKeywordsByMetadataId(metadata.getId())
                    var keywordMetadataList = ""
                    for (keyword in keywords) {
                        keywordMetadataList += keyword.getKeyword()+","
                    }
                    if (keywordMetadataList.isNotEmpty()) {
                        keywordMetadataList = keywordMetadataList.dropLast(1)
                    }
                    keywordMap[metadata.getId()] = keywordMetadataList
                }
                response["labelPhotoMap"] = labelPhotoMap
                response["albumMap"] = albumMap
                response["keywordMap"] = keywordMap

                val albumList = albumRepository.findAll()
                if (albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = "Results"
            response["status"] = "success"
        }

        return response
    }
}