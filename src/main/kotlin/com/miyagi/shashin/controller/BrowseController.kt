package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest

@Controller
@Secured("ROLE_ADMIN")
class BrowseController: BaseController() {

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

        buildInitialPage(module,model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/recent/{page}","/api/v1/recent/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedRecent(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildBrowseRecord("recent",model,page))
    }

    @RequestMapping(value = ["/modified"], method = [RequestMethod.GET])
    fun getModified(model: Model): String {
        val module = "modified"

        buildInitialPage(module,model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/modified/{page}","/api/v1/modified/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedModified(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildBrowseRecord("modified",model,page))
    }

    private fun buildBrowseRecord(module: String, model: Model, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "There are no photos. Please setup directories to scan in Settings and index media in Media Indexing."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            var metadataList = mutableListOf<Metadata>()
            if (module == "recent") {
                metadataList = metadataRepository.findRecentByOffsetAndLimit(
                    pageValue,
                    model.getAttribute("queryLimit").toString().toInt()
                ).toMutableList()
            } else if (module == "modified") {
                metadataList = metadataRepository.findModifiedByOffsetAndLimit(
                    pageValue,
                    model.getAttribute("queryLimit").toString().toInt()
                ).toMutableList()
            }

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
                        labelPhotoMap[metadata.getId()] = labelString
                    }

                    val albumPhotos = albumPhotoRepository.findAlbumPhotoByMetadataId(metadata.getId())
                    if (albumPhotos != null) {
                        var albumMetadataList = ""
                        for (albumPhoto in albumPhotos) {
                            val album = albumRepository.findById(albumPhoto!!.getAlbumId()!!)
                            albumMetadataList += album.get().getName()+","
                        }
                        if (albumMetadataList.isNotEmpty()) {
                            albumMetadataList = albumMetadataList.dropLast(1)
                            albumMap[metadata.getId()] = albumMetadataList
                        }
                    }

                    val keywords = keywordRepository.findKeywordsByMetadataId(metadata.getId())
                    var keywordMetadataList = ""
                    for (keyword in keywords) {
                        keywordMetadataList += keyword.getKeyword()+","
                    }
                    if (keywordMetadataList.isNotEmpty()) {
                        keywordMetadataList = keywordMetadataList.dropLast(1)
                        keywordMap[metadata.getId()] = keywordMetadataList
                    }
                }
                response["labelPhotoMap"] = labelPhotoMap
                response["albumMap"] = albumMap
                response["keywordMap"] = keywordMap

                val albumList = albumRepository.findAllOrderByAlbumName()
                if (albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }

    private fun buildInitialFoldersPage(model: Model): Model {
        val page = 0
        val response = buildPagedFolders(model,page)
        for ((k, v) in response) {
            model[k] = v!!
        }

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttribueData(model)

        return model
    }

    private fun buildInitialPage(module: String, model: Model): Model {
        val page = 0
        val response = buildBrowseRecord(module,model,page)
        for ((k, v) in response) {
            model[k] = v!!
        }

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttribueData(model)

        return model
    }

    @GetMapping("/folders")
    fun getFolders(model: Model): String {
        val module = "folders"
        buildInitialFoldersPage(model)

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["api/v1/folders"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getFoldersApi(model: Model): String {
        return mapper.writeValueAsString(buildAllFolders(model))
    }

    private fun buildAllFolders(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val module = "folders"
        response["message"] = "There are no folders."
        response["foldersList"] = mutableListOf<Folder>()

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

    private fun buildPagedFolders(model: Model, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        response["status"] = ApiResponse.FAIL.status

        val module = "folders"
        response["msg"] = "There are no folders."
        response["message"] = "There are no folders."
        response["foldersList"] = mutableListOf<Folder>()

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit
            val folderObj = metadataRepository.findFoldersOffsetAndLimit(pageValue, queryLimit)

            if (folderObj != null && folderObj.count() > 0) {
                response["foldersList"] = folderObj
                response["status"] = ApiResponse.SUCCESS.status
                response["message"] = ""
            }
        }

        response["msg"] = ""
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)

        return response
    }

    @RequestMapping(value = ["/folders/{page}","/api/v1/folders/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedFolders(model: Model, request: HttpServletRequest, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildPagedFolders(model,page))
    }

    @RequestMapping(value = ["/folder/{folder}"], method = [RequestMethod.GET])
    fun getRecentlyAdded(model: Model, @PathVariable folder: String): String {
        val module = "folder"
        val page = 0
        val response = buildFolder(model,URLDecoder.decode(folder, StandardCharsets.UTF_8.toString()),page)

        for ((k, v) in response) {
            model[k] = v!!
        }

        getAllAttribueData(model)

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
        response["message"] = "There are no photos. Please setup directories to scan in Settings and index media in Media Indexing."
        response["metadataList"] = mutableListOf<Metadata>()
        response["favorites"] = mutableMapOf<String, Any>()
        response["albumList"] = mutableListOf<Album>()
        response["recognitionLabels"] = mutableListOf<RecognitionLabel>()
        response["labelPhotoMap"] = mutableMapOf<String, Any>()
        response["mediaTypeFilter"] = "all"
        response["albumMap"] = mutableMapOf<String, Any>()
        response["keywordMap"] = mutableMapOf<String, Any>()
        response["folder"] = folder

        response["msg"] = "Could not get results"
        response["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("currentUser") != "") {
            val currentUserObj = model.getAttribute("currentUser") as User?
            val queryLimit = model.getAttribute("queryLimit").toString().toInt()
            val pageValue = page*queryLimit

            val favoritesMap = HashMap<String, HashMap<String, Any>>()

            val metadataList: MutableList<Metadata> = metadataRepository.findAllByFolderOffsetAndLimit(
                folder,
                pageValue,
                queryLimit
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
                        labelPhotoMap[metadata.getId()] = labelString
                    }

                    val albumPhotos = albumPhotoRepository.findAlbumPhotoByMetadataId(metadata.getId())
                    if (albumPhotos != null) {
                        var albumMetadataList = ""
                        for (albumPhoto in albumPhotos) {
                            val album = albumRepository.findById(albumPhoto!!.getAlbumId()!!)
                            albumMetadataList += album.get().getName()+","
                        }
                        if (albumMetadataList.isNotEmpty()) {
                            albumMetadataList = albumMetadataList.dropLast(1)
                            albumMap[metadata.getId()] = albumMetadataList
                        }
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

                val albumList = albumRepository.findAllOrderByAlbumName()
                if (albumList.count() > 0) {
                    response["albumList"] = albumList
                }

                response["favorites"] = favoritesMap
            }

            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status
        }

        return response
    }
}