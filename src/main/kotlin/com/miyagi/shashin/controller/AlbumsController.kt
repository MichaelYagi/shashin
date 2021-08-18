package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.AlbumPhoto
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

        val currentUserObj = userRepository.findByUsername(model.getAttribute("username").toString())
        if (currentUserObj != null) {
            val userAlbums = userAlbumRepository.findAllByUserId(currentUserObj.getId())
            if (userAlbums != null) {
                if (userAlbums.count() > 0) {
                    model["data"] = ""
                    val albums = HashMap<String, Album>()
                    for (userAlbum in userAlbums) {
                        if (userAlbum?.getAlbumId() != null) {
                            val albumObj = albumRepository.findById(userAlbum.getAlbumId()!!)
                        }
                    }
                    model["albumsList"] = albums
                }
            }
        }
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
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