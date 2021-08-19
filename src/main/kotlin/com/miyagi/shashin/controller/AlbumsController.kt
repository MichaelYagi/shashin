package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.UserAlbum
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.transaction.Transactional


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

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/albums")
    fun getAlbums(model: Model): String {
        val module = "albums"
        model["data"] = "There are no albums."
        model["albumsList"] = ""
        model["albumsCount"] = ""
        model["users"] = ""
        model["currentUser"] = ""
        model["userAlbums"] = ""
        model["userCount"] = ""

        val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
        if (currentUserObj != null) {
            val userAlbums = userAlbumRepository.findAllByUserId(currentUserObj.getId())

            if (userAlbums != null) {
                if (userAlbums.count() > 0) {
                    val albums = ArrayList<Album>()
                    val albumsCount = ArrayList<Int>()
                    var albumCount = 0
                    for (userAlbum in userAlbums) {
                        if (userAlbum?.getAlbumId() != null) {
                            albumCount = 0
                            val albumObj = albumRepository.findById(userAlbum.getAlbumId()!!)
                            val albumPhotoCount = albumPhotoRepository.countByAlbumId(userAlbum.getAlbumId()!!)
                            if (albumPhotoCount != null) {
                                albumCount = albumPhotoCount
                            }
                            albumsCount.add(albumCount)
                            albums.add(albumObj.get())
                        }
                    }

                    if (albums.count() > 0) {
                        model["albumsList"] = albums
                        model["albumsCount"] = albumsCount
                        val userCount = userRepository.count()
                        if (userCount > 1) {
                            model["users"] = userRepository.findAll()
                            model["currentUser"] = currentUserObj
                            model["userAlbums"] = userAlbumRepository.findAllByOrderByUserIdAsc()!!
                            model["userCount"] = userCount
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
                            model["sharedAlbums"] = sharedAlbumsList
                        }
                        model["data"] = ""
                    }
                }
            }
        }
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

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
            }

            resp["msg"] = "Success!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

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
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now()
                album.get().setModifiedAt(dtf.format(now))
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

    @RequestMapping(value = ["/album/share/{albumId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun shareAlbum(@RequestBody requestBody: JsonNode, @PathVariable albumId: Int): String? {
        val shareAlbum = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (shareAlbum.containsKey("albumId") && shareAlbum.containsKey("userShareMap")) {
            val userMapObj = mapper.readTree(shareAlbum["userShareMap"].toString())
            val userMap = mapper.convertValue(userMapObj, object : TypeReference<Map<String, Boolean>>() {})
            val shareAlbumId = shareAlbum["albumId"].toString().toInt();
            for ((userId, share) in userMap) {
                if (share) {
                    val countUserAlbum = userAlbumRepository.countByUserIdAndAlbumId(userId.toInt(), albumId)
                    if (countUserAlbum == 0) {
                        val userAlbumObj = UserAlbum()
                        userAlbumObj.setUserId(userId.toInt())
                        userAlbumObj.setAlbumId(shareAlbumId)
                        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val now = LocalDateTime.now()
                        userAlbumObj.setCreatedAt(dtf.format(now))
                        userAlbumObj.setModifiedAt(dtf.format(now))
                        userAlbumRepository.save(userAlbumObj)
                    }
                } else {
                    userAlbumRepository.deleteByUserIdAndAlbumId(userId.toInt(),shareAlbumId)
                }
            }

            resp["msg"] = "Shared!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Could not save"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/album/{albumId}"], method = [RequestMethod.GET])
    fun getAlbum(model: Model, @PathVariable albumId: Int): String {
        val module = "album"
        model["data"] = "Oops, something went wrong!"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        model["album"] = ""
        model["albumMetadataList"] = ""

        val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
        if (currentUserObj != null && albumId > 0) {
            val userAlbums = userAlbumRepository.findByUserIdAndAlbumId(currentUserObj.getId(), albumId)
            if (userAlbums != null) {
                // Get album photos
                val albumPhotos = albumPhotoRepository.findAllByAlbumId(albumId)
                val albumMetadataList = ArrayList<Metadata>()
                if (albumPhotos != null) {
                    for (albumPhoto in albumPhotos) {
                        if (albumPhoto != null) {
                            val metadata = metadataRepository.findById(albumPhoto.getMetadataId()!!)
                            albumMetadataList.add(metadata.get())
                        }
                    }
                    if (albumMetadataList.count() > 0) {
                        val album = albumRepository.findById(albumId)
                        model["album"] = album.get()
                        model["albumMetadataList"] = albumMetadataList
                        model["data"] = ""

                    }
                }
            }
        }

        return module
    }

    @RequestMapping(value = ["/albums/add"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postAddAlbum(model: Model, @RequestBody requestBody: JsonNode): String? {
        val albumData = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        var albumId: Int? = null
        val albumIdString = albumData["albumId"].toString()
        val albumName = albumData["albumName"].toString()
        val albumMetadataIdList = mapper.convertValue(albumData["albumMetadataIds"], object : TypeReference<Array<String>>() {})

        var albumPhotoObj = AlbumPhoto()
        var albumObj = Album()

        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val now = LocalDateTime.now()

        if (albumIdString.isNullOrBlank()) {
            if (albumMetadataIdList.count() > 0) {
                // Get the first one and set as album cover
                val metadataObj = metadataRepository.findById(albumMetadataIdList[0])
                albumObj.setCoverUrl(metadataObj.get().getThumbnailUrlCentered())
            }
            albumObj.setName(albumName)
            albumObj.setCreatedAt(dtf.format(now))
            albumObj.setModifiedAt(dtf.format(now))
            albumObj = albumRepository.save(albumObj)
            albumId = albumObj.getId()
        } else {
            albumId = albumIdString.toInt()
        }

        val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
        if (currentUserObj != null) {
            val userAlbumCount = userAlbumRepository.countByUserIdAndAlbumId(currentUserObj.getId(), albumId)
            if (userAlbumCount == 0) {
                val userAlbumObj = UserAlbum()
                userAlbumObj.setAlbumId(albumId)
                userAlbumObj.setUserId(currentUserObj.getId())
                userAlbumObj.setCreatedAt(dtf.format(now))
                userAlbumObj.setModifiedAt(dtf.format(now))
                userAlbumRepository.save(userAlbumObj)
            }
        }


        var albumPhotoCount = 0
        for (metadataId in albumMetadataIdList) {
            albumPhotoCount = albumPhotoRepository.countByMetadataIdAndAlbumId(metadataId, albumId)!!
            if (albumPhotoCount == 0) {
                albumPhotoObj = AlbumPhoto()
                albumPhotoObj.setMetadataId(metadataId)
                albumPhotoObj.setAlbumId(albumId)
                albumPhotoObj.setCreatedAt(dtf.format(now))
                albumPhotoObj.setModifiedAt(dtf.format(now))
                albumPhotoRepository.save(albumPhotoObj)
            }
        }

        resp["msg"] = "Saved!"
        resp["status"] = "success"

        return mapper.writeValueAsString(resp)
    }
}