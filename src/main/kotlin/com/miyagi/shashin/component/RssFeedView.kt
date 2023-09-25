package com.miyagi.shashin.component

import com.miyagi.shashin.repository.AlbumPhotoRepository
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.UserRepository
import com.rometools.rome.feed.rss.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.servlet.view.feed.AbstractRssFeedView
import java.awt.image.BufferedImage
import java.net.URL
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.io.path.Path


@Component
class RssFeedView : AbstractRssFeedView() {

    @Value("\${app.build.properties.name}")
    private val appName: String? = null

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var albumRepository: AlbumRepository? = null

    @Autowired
    var albumPhotoRepository: AlbumPhotoRepository? = null

    @Autowired
    var metadataRepository: MetadataRepository? = null

    override fun buildFeedMetadata(model: MutableMap<String, Any>, feed: Channel, request: HttpServletRequest) {
        feed.title = "$appName RSS Feed"
        feed.description = "$appName images"
        feed.feedType = "rss_2.0"

        if (model.containsKey("apiKey")) {
            val apiKey = model["apiKey"] as String?
            val currentUser = userRepository?.findByApikey(apiKey)
            if (currentUser != null) {
                feed.description = "${currentUser.getUsername()} $appName images"
            }
        }

        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        var apiKey = ""
        if (model.containsKey("apiKey")) {
            apiKey = model["apiKey"] as String
        }
        feed.link = "${baseUrlBuilder.build().toUriString()}/$apiKey/rss"
    }

    override fun buildFeedItems(
        model: Map<String, Any>,
        request: HttpServletRequest, response: HttpServletResponse
    ): List<Item> {

        val rssList = mutableListOf<Item>()
        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        val baseUrl = baseUrlBuilder.build().toUriString()

        if (model.containsKey("apiKey")) {
            val apiKey = model["apiKey"] as String?
            val currentUser = userRepository?.findByApikey(apiKey)
            if (currentUser != null) {
                val randomAlbums = albumRepository?.findRandomAlbumsByUser(currentUser.getId())
                if (randomAlbums != null && randomAlbums.count() > 0) {
                    for (randomAlbum in randomAlbums) {
//                        if (randomAlbum.getIsShared() == 1) {
                            val albumPhotos = albumPhotoRepository?.findImagesByAlbumId(randomAlbum.getAlbumId()!!, 100)
                            if (albumPhotos != null) {
                                for (albumPhoto in albumPhotos) {
                                    val metadata = metadataRepository?.findByMetadataId(albumPhoto?.getMetadataId()!!)
                                    if (metadata != null) {
                                        val entry = Item()
                                        entry.title = metadata.getTitle()
                                        val description = Description()
                                        var place = ""
                                        var metadataDescription = ""
                                        if (metadata.getPlaceName() != null && metadata.getPlaceName() != "") {
                                            val placeArray = metadata.getPlaceName()!!.split(";")
                                            place = placeArray[0]
                                        }
                                        if (metadata.getDescription() != null && metadata.getDescription() != "") {
                                            metadataDescription = metadata.getDescription()!!
                                        }
                                        val descVal = "$metadataDescription $place"
                                        description.value = descVal.trim()
                                        entry.description = description
                                        entry.link = "$baseUrl/api/v1/image/${metadata.getId()}"
                                        entry.uri = "$baseUrl/api/v1/image/${metadata.getId()}"
                                        val guid = Guid()
                                        guid.value = "$baseUrl/api/v1/image/${metadata.getId()}"
                                        entry.guid = guid
                                        val enc = Enclosure()
                                        enc.url = "$baseUrl/api/v1/image/${metadata.getId()}"
                                        enc.type = metadata.getType()
                                        enc.length = Files.size(Path(metadata.getPath()!!))
                                        entry.enclosures = mutableListOf(enc)
                                        rssList.add(entry)
                                    }
                                }
                            }
//                        }
                    }
                }
            }
        }

        return rssList.toList()
    }
}