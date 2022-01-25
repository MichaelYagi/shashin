package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.transaction.Transactional
import kotlin.collections.HashMap

@Suppress("UNCHECKED_CAST")
@Controller
class AlbumsController {

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var albumPhotoRepository: AlbumPhotoRepository

    @Autowired
    private lateinit var userAlbumRepository: UserAlbumRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var albumCommentRepository: AlbumCommentRepository

    @Autowired
    private lateinit var albumPhotoCommentRepository: AlbumPhotoCommentRepository

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/albums")
    fun getAlbums(model: Model): String {
        val response = buildAlbums(model)
        for ((k, v) in response) {
            model[k] = v!!
        }
        return model.getAttribute("activePage").toString()
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @RequestMapping(value = ["/api/v1/albums"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAlbumsApi(model: Model): String {
        return mapper.writeValueAsString(buildAlbums(model))
    }

    private fun buildAlbums(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val module = "albums"
        response["message"] = "There are no albums."
        response["albumsList"] =  mutableListOf<Album>()
        response["userAlbums"] = mutableListOf<UserAlbum>()
        response["userCount"] = 0
        response["albumsCommentsMap"] = mutableMapOf<Int, ArrayList<HashMap<String, Any>>>()
        response["notificationMap"] = mutableMapOf<Int, Boolean>()

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val userAlbums = userAlbumRepository.findAllByUserId(currentUserObj.getId())

            if (userAlbums != null && userAlbums.count() > 0) {
                val albumsCommentsMap = HashMap<Int, ArrayList<HashMap<String, Any>>>()

                val notificationMap = HashMap<Int, Boolean>()
                val albums = ArrayList<HashMap<String, Any>>()
                var albumCount: Int
                for (userAlbum in userAlbums) {

                    val albumCommentsList = ArrayList<HashMap<String, Any>>()
                    if (userAlbum?.getAlbumId() != null) {
                        albumCount = 0
                        val albumMap = HashMap<String, Any>()
                        val albumObj = albumRepository.findById(userAlbum.getAlbumId()!!)
                        val albumPhotoCount = albumPhotoRepository.countByAlbumId(userAlbum.getAlbumId()!!)
                        if (albumPhotoCount != null) {
                            albumCount = albumPhotoCount
                        }
                        albumMap["id"] = albumObj.get().getId()
                        albumMap["name"] = if (albumObj.get().getName() == null) "" else albumObj.get().getName()!!
                        albumMap["coverUrl"] = if (albumObj.get().getCoverUrl() == null) "" else albumObj.get().getCoverUrl()!!
                        albumMap["shareUrl"] = if (albumObj.get().getShareUrl() == null) "" else albumObj.get().getShareUrl()!!
                        albumMap["albumCount"] = albumCount
                        albums.add(albumMap)

                        val notificationCount = notificationRepository.countAllByAlbumIdAndUserIdAndMetadataIdIsNullAndReadIsFalse(userAlbum.getAlbumId()!!,currentUserObj.getId())
                        notificationMap[userAlbum.getAlbumId()!!] = notificationCount > 0

                        // Get comments for this album
                        val albumComments = commentRepository.findCommentsByAlbumId(albumObj.get().getId())
                        for (albumComment in albumComments) {
                            val albumCommentMap = HashMap<String, Any>()
                            albumCommentMap["comment"] = albumComment.getComment().toString()
                            albumCommentMap["commentId"] = albumComment.getCommentId().toString().toInt()
                            albumCommentMap["albumId"] = albumComment.getAlbumId().toString().toInt()
                            albumCommentMap["userId"] = albumComment.getUserId().toString().toInt()
                            albumCommentMap["username"] = albumComment.getUsername().toString()
                            albumCommentMap["createdAt"] = TextUtils.formatToLongDateWithTime(albumComment.getCreatedAt().toString())
                            albumCommentsList.add(albumCommentMap)
                        }
                        if (albumCommentsList.isNotEmpty()) {
                            albumsCommentsMap[albumObj.get().getId()] = albumCommentsList
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    response["albumsList"] = albums
                    response["albumsCommentsMap"] = albumsCommentsMap
                    response["notificationMap"] = notificationMap

                    val userCount = userRepository.count()
                    if (userCount > 1) {
                        response["userAlbums"] = userAlbumRepository.findAllByOrderByUserIdAsc()!!
                        response["userCount"] = userCount
                        val sharedAlbumsList = ArrayList<HashMap<String, Any>>()
                        val sharedAlbums = userRepository.findUserBySharedAlbum(currentUserObj.getId())
                        for (sharedAlbum in sharedAlbums) {
                            val sharedAlbumsMap = HashMap<String, Any>()
                            sharedAlbumsMap["userId"] = sharedAlbum.getUserId().toString().toInt()
                            sharedAlbumsMap["albumId"] = sharedAlbum.getAlbumId().toString().toInt()
                            sharedAlbumsMap["username"] = sharedAlbum.getUsername().toString()
                            sharedAlbumsMap["isShared"] = sharedAlbum.getIsShared().toString().toInt()
                            sharedAlbumsList.add(sharedAlbumsMap)
                        }
                        response["sharedAlbums"] = sharedAlbumsList
                    }
                    response["message"] = ""
                }
            }
        }

        response["msg"] = "Success!"
        response["status"] = "success"
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)

        return response
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/album/delete/{albumId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteAlbumPhotos(@RequestBody requestBody: JsonNode, @PathVariable albumId: Int): String? {
        val albumDeleteMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (albumDeleteMap.containsKey("albumId") && albumDeleteMap.containsKey("delete")) {
            val albumIdRequest = albumDeleteMap["albumId"].toString().toInt()
            val deleteFlag = albumDeleteMap["delete"].toString().toBoolean()

            if (deleteFlag && albumId == albumIdRequest) {
                userAlbumRepository.deleteByAlbumId(albumId)
                albumPhotoRepository.deleteByAlbumId(albumId)
                albumRepository.deleteById(albumId)
                // Delete comments
                val albumComments = albumCommentRepository.findAllByAlbumId(albumId)
                if (albumComments != null) {
                    val commentIdList = ArrayList<Int>()
                    for (albumComment in albumComments) {
                        if (albumComment != null && albumComment.getCommentId() !in commentIdList) {
                            commentIdList.add(albumComment.getCommentId()!!)
                        }
                    }

                    if (commentIdList.count() > 0) {
                        commentRepository.deleteAllById(commentIdList)
                        albumCommentRepository.deleteByAlbumId(albumId)
                        albumPhotoCommentRepository.deleteByAlbumId(albumId)
                    }
                }
            }

            resp["msg"] = "Success!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/album/delete/batch"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteAlbumPhotos(@RequestBody requestBody: JsonNode): String? {
        val batchMetadataMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (batchMetadataMap.containsKey("metadataIdList") && batchMetadataMap.containsKey("albumId")) {
            val idArray = batchMetadataMap["metadataIdList"] as ArrayList<String>
            val albumId = batchMetadataMap["albumId"].toString().toInt()

            for (metadataId in idArray) {
                albumPhotoRepository.deleteByMetadataIdAndAlbumId(metadataId, albumId)
                val count = albumPhotoRepository.countByAlbumId(albumId)
                if (count != null && count.toInt() > 0) {
                    var metadataObj = metadataRepository.findById(metadataId)
                    val coverAlbumUrl = metadataObj.get().getThumbnailUrlCentered()
                    val album = albumRepository.findById(albumId)
                    if (album.get().getCoverUrl() == coverAlbumUrl) {
                        // Use the first photo in album
                        val albumPhoto = albumPhotoRepository.findFirstByOrderByIdAsc()
                        if (albumPhoto != null) {
                            metadataObj = metadataRepository.findById(albumPhoto.getMetadataId().toString())
                            album.get().setCoverUrl(metadataObj.get().getThumbnailUrlCentered())
                            albumRepository.save(album.get())
                        }
                    }
                }
            }

            val count = albumPhotoRepository.countByAlbumId(albumId)
            if (count != null && count.toInt() == 0) {
                userAlbumRepository.deleteByAlbumId(albumId)
                albumRepository.deleteById(albumId)
                // Delete comments
                val albumComments = albumCommentRepository.findAllByAlbumId(albumId)
                if (albumComments != null) {
                    val commentIdList = ArrayList<Int>()
                    for (albumComment in albumComments) {
                        if (albumComment != null && albumComment.getCommentId() !in commentIdList) {
                            commentIdList.add(albumComment.getCommentId()!!)
                        }
                    }

                    if (commentIdList.count() > 0) {
                        commentRepository.deleteAllById(commentIdList)
                        albumCommentRepository.deleteByAlbumId(albumId)
                        albumPhotoCommentRepository.deleteByAlbumId(albumId)
                    }
                }

                resp["msg"] = "/albums"
                resp["status"] = "redirect"
                return mapper.writeValueAsString(resp)
            }

            resp["msg"] = "Saved!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/album/update"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun updateAlbum(@RequestBody requestBody: JsonNode): String? {
        val albumOptionsMapper = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (albumOptionsMapper.containsKey("removeFromAlbum") &&
            albumOptionsMapper.containsKey("setCoverAlbum") &&
            albumOptionsMapper.containsKey("metadataId") &&
            albumOptionsMapper.containsKey("albumId")
        ) {
            val albumId = albumOptionsMapper["albumId"].toString().toInt()
            val metadataId = albumOptionsMapper["metadataId"].toString()
            val removeFromAlbum = albumOptionsMapper["removeFromAlbum"].toString().toBoolean()
            val setCoverAlbum = albumOptionsMapper["setCoverAlbum"].toString().toBoolean()

            if (removeFromAlbum) {
                albumPhotoRepository.deleteByMetadataIdAndAlbumId(metadataId, albumId)
                val count = albumPhotoRepository.countByAlbumId(albumId)

                if (count != null) {
                    if (count.toInt() > 0) {
                        var metadataObj = metadataRepository.findById(metadataId)
                        val coverAlbumUrl = metadataObj.get().getThumbnailUrlCentered()
                        val album = albumRepository.findById(albumId)
                        if (album.get().getCoverUrl() == coverAlbumUrl) {
                            // Use the first photo in album
                            val albumPhoto = albumPhotoRepository.findFirstByOrderByIdAsc()
                            if (albumPhoto != null) {
                                metadataObj = metadataRepository.findById(albumPhoto.getMetadataId().toString())
                                album.get().setCoverUrl(metadataObj.get().getThumbnailUrlCentered())
                                albumRepository.save(album.get())
                            }
                        }

                    } else {
                        userAlbumRepository.deleteByAlbumId(albumId)
                        albumRepository.deleteById(albumId)
                        // Delete comments
                        val albumComments = albumCommentRepository.findAllByAlbumId(albumId)
                        if (albumComments != null) {
                            val commentIdList = ArrayList<Int>()
                            for (albumComment in albumComments) {
                                if (albumComment != null && albumComment.getCommentId() !in commentIdList) {
                                    commentIdList.add(albumComment.getCommentId()!!)
                                }
                            }

                            if (commentIdList.count() > 0) {
                                commentRepository.deleteAllById(commentIdList)
                                albumCommentRepository.deleteByAlbumId(albumId)
                                albumPhotoCommentRepository.deleteByAlbumId(albumId)
                            }

                        }

                        resp["msg"] = "/albums"
                        resp["status"] = "redirect"
                        return mapper.writeValueAsString(resp)
                    }
                }
            } else if (setCoverAlbum) {
                val metadataObj = metadataRepository.findById(metadataId)
                val coverAlbumUrl = metadataObj.get().getThumbnailUrlCentered()
                val album = albumRepository.findById(albumId)
                album.get().setCoverUrl(coverAlbumUrl)
                album.get().setModifiedAt(getCurrentTimestamp())
                albumRepository.save(album.get())
            }

            resp["msg"] = "Saved!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/album/{albumId}/save/sharelink"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postAnonymousShareAlbum(@RequestBody requestBody: JsonNode, @PathVariable albumId: Int): String? {
        val albumShareInfo = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (albumShareInfo.containsKey("albumId") && albumShareInfo.containsKey("relativeShareUrl")) {
            val albumIdRequest = albumShareInfo["albumId"].toString().toInt()
            var relativeShareUrl: String? = albumShareInfo["relativeShareUrl"].toString().trim()

            if (albumId == albumIdRequest && albumId > 0) {
                val albumObj = albumRepository.findById(albumId)
                if (albumObj.get().getId() == albumIdRequest) {
                    resp["msg"] = "Share link generated"
                    if (relativeShareUrl != null && relativeShareUrl.isEmpty()) {
                        relativeShareUrl = null
                        resp["msg"] = "Share link cleared"
                    }
                    albumObj.get().setShareUrl(relativeShareUrl)
                    albumRepository.save(albumObj.get())

                    resp["relativeShareUrl"] = relativeShareUrl
                    resp["status"] = "success"
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["relativeShareLink"] = ""
        resp["msg"] = "Could not generate link"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/share/{shareLink}/album/{albumId}"], method = [RequestMethod.GET])
    fun getAnonymousShareAlbum(model: Model, @PathVariable shareLink: String, @PathVariable albumId: Int): String? {
        val module = "share"

        val queryLimit = model.getAttribute("queryLimit").toString().toInt()
        val response = buildShareData(albumId,shareLink, queryLimit, 0)

        model["album"] = response["album"]!!
        model["albumMetadataList"] = response["albumMetadataList"]!!
        model["shareLink"] = response["shareLink"]!!
        model["message"] = response["message"]!!
        model["msg"] = response["msg"]!!
        model["status"] = response["status"]!!

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/share/{shareLink}/album/{albumId}/{page}","/api/v1/share/{shareLink}/album/{albumId}/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedAnonymousShareAlbum(model: Model, @PathVariable shareLink: String, @PathVariable albumId: Int, @PathVariable page: Int): String? {
        val queryLimit = model.getAttribute("queryLimit").toString().toInt()
        val response = buildShareData(albumId,shareLink, queryLimit, page)
        return mapper.writeValueAsString(response)
    }

    private fun buildShareData(albumId: Int,shareLink: String, queryLimit: Int, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()
        response["message"] = "Nothing to see here."
        val tempAlbum = Album()
        tempAlbum.setId(0)
        response["album"] = tempAlbum
        response["albumMetadataList"] = mutableListOf<Metadata>()
        response["shareLink"] = ""
        response["msg"] = "No results"
        response["status"] = "fail"

        val photoObj = albumRepository.findById(albumId)
        if (photoObj.get().getShareUrl() == shareLink) {
            val resultPage = page*queryLimit
            val albumPhotos = albumPhotoRepository.findAllByAlbumIdAndOffsetAndLimit(albumId,resultPage,queryLimit)
            val albumMetadataList = ArrayList<Metadata>()
            if (albumPhotos != null) {
                for (albumPhoto in albumPhotos) {
                    if (albumPhoto != null) {
                        val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                        albumMetadataList.add(metadata.get())
                    }
                }

                if (albumMetadataList.isNotEmpty()) {
                    val album = albumRepository.findById(albumId)
                    response["message"] = ""
                    response["album"] = album.get()
                    response["albumMetadataList"] = albumMetadataList
                    response["shareLink"] = shareLink
                    response["msg"] = "Results"
                    response["status"] = "success"
                }
            }
        }

        return response
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/album/share/{albumId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun shareAlbum(@RequestBody requestBody: JsonNode, @PathVariable albumId: Int): String? {
        val shareAlbum = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (shareAlbum.containsKey("albumId") && shareAlbum.containsKey("userShareMap")) {
            val userMapObj = mapper.readTree(shareAlbum["userShareMap"].toString())
            val userMap = mapper.convertValue(userMapObj, object : TypeReference<Map<String, Boolean>>() {})
            val shareAlbumId = shareAlbum["albumId"].toString().toInt()
            val userAlbumList = mutableListOf<UserAlbum>()
            val deleteUserAlbumList = mutableListOf<UserAlbum>()

            for ((userId, share) in userMap) {
                if (share) {
                    val countUserAlbum = userAlbumRepository.countByUserIdAndAlbumId(userId.toInt(), albumId)
                    if (countUserAlbum == 0) {
                        val userAlbumObj = UserAlbum()
                        userAlbumObj.setUserId(userId.toInt())
                        userAlbumObj.setAlbumId(shareAlbumId)
                        userAlbumObj.setCreatedAt(getCurrentTimestamp())
                        userAlbumObj.setModifiedAt(getCurrentTimestamp())
                        userAlbumList.add(userAlbumObj)
                    }
                } else {
                    val userAlbumObj = userAlbumRepository.findByUserIdAndAlbumId(userId.toInt(),shareAlbumId)
                    if (userAlbumObj != null) {
                        deleteUserAlbumList.add(userAlbumObj)
                    }
                }
            }

            if (userAlbumList.count() > 0) {
                userAlbumRepository.saveAll(userAlbumList)
            }
            if (deleteUserAlbumList.count() > 0) {
                userAlbumRepository.deleteAll(deleteUserAlbumList)
            }

            resp["msg"] = "Shared!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @RequestMapping(value = ["/album/{albumId}"], method = [RequestMethod.GET])
    fun getAlbum(model: Model, @PathVariable albumId: Int): String {
        val response = buildAlbum(model,albumId,0)
        for ((k, v) in response) {
            model[k] = v!!
        }
        model["currentUser"] = model.getAttribute("currentUser") as User
        return model.getAttribute("activePage").toString()
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @RequestMapping(value = ["/album/{albumId}/page/{page}","/api/v1/album/{albumId}/page/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getPagedAlbum(model: Model, @PathVariable albumId: Int, @PathVariable page: Int): String {
        return mapper.writeValueAsString(buildAlbum(model,albumId,page))
    }

    private fun buildAlbum(model: Model, albumId: Int, page: Int): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val module = "album"
        response["message"] = "Nothing to see here."
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)

        response["album"] = Album()
        response["albumId"] = 0
        response["albumMetadataList"] = mutableListOf<Metadata>()
        response["albumPhotoCommentsMap"] = mutableMapOf<String, ArrayList<HashMap<String, Any>>>()
        response["userMap"] = mutableMapOf<String, Any>()
        response["notificationMap"] = mutableMapOf<String, Boolean>()
        response["favorites"] = mutableMapOf<String, String>()
        response["msg"] = "No results"
        response["status"] = "noop"
        response["keywordMap"] = mutableMapOf<String, String>()
        response["status"] = "noop"

        val favoritesMap = HashMap<String, HashMap<String, Any>>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && albumId > 0) {
            val userAlbums = userAlbumRepository.findByUserIdAndAlbumId(currentUserObj.getId(), albumId)
            if (userAlbums != null) {
                // Get album photos
                val albumPhotos = albumPhotoRepository.findAllByAlbumIdAndOffsetAndLimit(albumId,page*model.getAttribute("queryLimit").toString().toInt(),model.getAttribute("queryLimit").toString().toInt())
                val albumMetadataList = ArrayList<Metadata>()
                if (albumPhotos != null) {
                    val albumPhotosCommentsMap = HashMap<String, ArrayList<HashMap<String, Any>>>()
                    val notificationMap = HashMap<String, Boolean>()

                    for (albumPhoto in albumPhotos) {
                        val albumPhotoCommentsList = ArrayList<HashMap<String, Any>>()
                        if (albumPhoto != null) {
                            val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                            albumMetadataList.add(metadata.get())

                            val notificationCount = notificationRepository.countAllByMetadataIdAndUserIdAndReadIsFalse(albumPhoto.getMetadataId()!!,currentUserObj.getId())
                            notificationMap[albumPhoto.getMetadataId()!!] = notificationCount > 0

                            val favorites = favoriteRepository.findAllByMetadataId(albumPhoto.getMetadataId())
                            if (favorites != null) {
                                for (favorite in favorites) {
                                    if (favorite != null) {
                                        favoritesMap[albumPhoto.getMetadataId().toString()] = hashMapOf(
                                            "favorite" to (favorite.getUserId() == currentUserObj.getId()),
                                            "count" to favoriteRepository.countAllByMetadataId(albumPhoto.getMetadataId().toString())
                                        )

                                        if (favorite.getUserId() == currentUserObj.getId()) {
                                            break
                                        }
                                    }
                                }
                            }

                            // Get comments for this photo
                            val albumPhotoComments = commentRepository.findCommentsByAlbumIdAndMetadataId(albumId,albumPhoto.getMetadataId()!!)
                            for (albumPhotoComment in albumPhotoComments) {
                                val albumPhotoCommentMap = HashMap<String, Any>()
                                albumPhotoCommentMap["comment"] = albumPhotoComment.getComment().toString()
                                albumPhotoCommentMap["commentId"] = albumPhotoComment.getCommentId().toString().toInt()
                                albumPhotoCommentMap["metadataId"] = albumPhotoComment.getMetadataId().toString()
                                albumPhotoCommentMap["albumId"] = albumPhotoComment.getAlbumId().toString().toInt()
                                albumPhotoCommentMap["userId"] = albumPhotoComment.getUserId().toString().toInt()
                                albumPhotoCommentMap["username"] = albumPhotoComment.getUsername().toString()
                                albumPhotoCommentMap["createdAt"] = TextUtils.formatToLongDateWithTime(albumPhotoComment.getCreatedAt().toString())
                                albumPhotoCommentsList.add(albumPhotoCommentMap)
                            }
                            if (albumPhotoCommentsList.isNotEmpty()) {
                                albumPhotosCommentsMap[metadata.get().getId()] = albumPhotoCommentsList
                            }
                        }
                    }
                    if (albumMetadataList.count() > 0) {
                        val album = albumRepository.findById(albumId)
                        response["favorites"] = favoritesMap
                        response["notificationMap"] = notificationMap
                        response["albumPhotoCommentsMap"] = albumPhotosCommentsMap
                        response["album"] = album.get()
                        response["albumId"] = album.get().getId()
                        val keywordList = keywordRepository!!.findAllKeywordsGroupedByMetadataId()
                        val keywordMap = mutableMapOf<String, String>()
                        for (keywordGroup in keywordList) {
                            keywordMap[keywordGroup.getMetadataId()!!] = keywordGroup.getKeywords()!!
                        }
                        response["keywordMap"] = keywordMap
                        val userMap = HashMap<String, Any>()
                        userMap["id"] = currentUserObj.getId()
                        userMap["username"] = if (currentUserObj.getUsername() == null) "" else currentUserObj.getUsername()!!
                        var showControls = false
                        if (currentUserObj.getAuthority() != null && currentUserObj.getAuthority()!! == "ROLE_ADMIN") {
                            showControls = true
                        }
                        userMap["showControls"] = showControls
                        response["userMap"] = userMap
                        response["albumMetadataList"] = albumMetadataList
                        response["msg"] = "Results retrieved"
                        response["status"] = "success"
                        response["message"] = ""
                    }
                }
            }
        }

        return response
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/album/updatename/{albumId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun updateAlbumName(@RequestBody requestBody: JsonNode, @PathVariable albumId: Int): String? {
        val albumPayload = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (albumPayload.containsKey("albumId") && albumPayload.containsKey("albumName")) {
            val postAlbumId = albumPayload["albumId"].toString().toInt()
            val albumName = albumPayload["albumName"].toString()

            if (postAlbumId == albumId && albumName.isNotEmpty()) {
                val albumObj = albumRepository.findById(albumId).get()
                albumObj.setName(albumName)
                albumRepository.save(albumObj)

                resp["msg"] = "Saved"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }
}