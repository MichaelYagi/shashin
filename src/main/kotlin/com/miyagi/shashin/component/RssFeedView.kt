package com.miyagi.shashin.component

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.SlideshowAlbum
import com.miyagi.shashin.repository.AlbumPhotoRepository
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.SlideshowAlbumRepository
import com.miyagi.shashin.repository.UserAlbumRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import com.rometools.rome.feed.rss.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.servlet.view.feed.AbstractRssFeedView
import java.nio.file.Files
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlin.collections.MutableList
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

    @Autowired
    var slideshowAlbumRepository: SlideshowAlbumRepository? = null

    @Autowired
    var userAlbumRepository: UserAlbumRepository? = null

    override fun buildFeedMetadata(model: MutableMap<String, Any>, feed: Channel, request: HttpServletRequest) {
        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }

        feed.title = "$appName RSS Feed"
        feed.description = "$appName images - Invalid key"
        feed.feedType = "rss_2.0"

        if (model.containsKey("apiKey")) {
            val apiKey = model["apiKey"] as String?
            val currentUser = userRepository?.findByApikey(apiKey)
            if (currentUser != null) {
                feed.description = "${currentUser.getUsername()} $appName images"
            }
        }

        var apiKey = ""
        if (model.containsKey("apiKey")) {
            apiKey = model["apiKey"] as String
        }

        val image = Image()
        image.link = "${baseUrlBuilder.build().toUriString()}/$apiKey/rss"
        image.url = "${baseUrlBuilder.build().toUriString()}/images/favicon-256x256.png"
        image.title = "$appName RSS Feed"
        image.width = 56
        image.height = 56
        feed.image = image
        feed.link = "${baseUrlBuilder.build().toUriString()}/$apiKey/rss"
        feed.lastBuildDate = Date()
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
            if (currentUser != null && currentUser.getIsAuthorized() == true) {
                var queryLimit = 20
                if (model.containsKey("queryLimit")) {
                    queryLimit = model["queryLimit"] as Int
                }

                var slideshowAlbum = slideshowAlbumRepository?.findFirstByUserId(currentUser.getId())
                if (slideshowAlbum == null) {
                    slideshowAlbum = SlideshowAlbum()
                    slideshowAlbum.setUserId(currentUser.getId())
                    slideshowAlbum.setAlbums("all")
                }

                val albumsString = slideshowAlbum.getAlbums()
                var albumsArray = albumsString?.split(",")?.map { it -> it.trim() }
                var randomMetadata = mutableListOf<Metadata>()

                if (albumsArray != null && albumsArray.contains("all") && (currentUser.getAuthority() == "ROLE_ADMIN" || currentUser.getAuthority() == "ROLE_SUPER")) {
                    randomMetadata = metadataRepository?.findRandomMetadatasMedia("image", queryLimit) as MutableList<Metadata>
                } else {
                    val albumsIntArray = if (albumsArray != null && albumsArray.contains("all")) {
                        userAlbumRepository?.findAlbumIdsByUserId(currentUser.getId())
                    } else {
                        albumsString?.split(",")?.map { it -> it.trim().toInt() } as MutableList<Int>?
                    }

                    if (albumsArray != null) {
                        randomMetadata = albumPhotoRepository?.findRandomImagesByAlbumIdsAndLimit(
                            albumsIntArray!!,
                            queryLimit
                        ) as MutableList<Metadata>
                    }
                }

                for (metadata in randomMetadata) {
                    val entry = Item()
                    entry.title = metadata.getTitle()
                    val description = Description()
                    var place = ""
                    var metadataDescription = ""
                    var taken = ""

                    if (metadata.getYear() != null && metadata.getYear() != 0 &&
                        metadata.getMonth() != null && metadata.getMonth() != 0 &&
                        metadata.getDay() != null && metadata.getDay() != 0)
                    {
                        taken = TextUtils.formatToLongDate("${metadata.getYear()}-${metadata.getMonth()}-${metadata.getDay()}")
                    }
                    if (metadata.getPlaceName() != null && metadata.getPlaceName() != "") {
                        val placeArray = metadata.getPlaceName()!!.split(";")
                        place = (if (taken != "") "<br>" else "") + placeArray[0].trim()
                    }
                    if (metadata.getDescription() != null && metadata.getDescription() != "") {
                        metadataDescription = (if (place != "") "<br>" else "") + metadata.getDescription()!!.trim()
                    }
                    val descVal = "$taken$place$metadataDescription"

                    description.value = "<img src='$baseUrl/api/v1/thumbnails/225/${metadata.getId()}'><br>${descVal}"
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

        return rssList.shuffled()
    }
}