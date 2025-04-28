package com.miyagi.shashin.component

import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.AlbumPhotoRepository
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import com.rometools.rome.feed.atom.*
import com.rometools.rome.feed.synd.SyndPerson
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.servlet.view.feed.AbstractAtomFeedView
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path


@Component
class AtomFeedView : AbstractAtomFeedView() {

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

    override fun buildFeedMetadata(model: MutableMap<String, Any>, feed: Feed, request: HttpServletRequest) {
        val content = Content()
        content.value = "$appName images"

        feed.title = "$appName ATOM Feed"
        feed.feedType = "atom_1.0"

        if (model.containsKey("apiKey")) {
            val apiKey = model["apiKey"] as String?
            val currentUser = userRepository?.findByApikey(apiKey)
            if (currentUser != null) {
                content.value = "${currentUser.getUsername()} $appName images"
            }
        }

        feed.subtitle = content

        var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
        if (request.scheme == "https") {
            baseUrlBuilder = baseUrlBuilder.scheme("https")
        }
        var apiKey = ""
        if (model.containsKey("apiKey")) {
            apiKey = model["apiKey"] as String
        }

        val link = Link()
        link.href = "${baseUrlBuilder.build().toUriString()}/$apiKey/atom"
        link.rel = "self"
        link.title = "$appName ATOM feed URL"

        feed.alternateLinks = listOf(link)
        feed.id = "${baseUrlBuilder.build().toUriString()}/$apiKey/atom"
        feed.updated = Date()
    }

    override fun buildFeedEntries(
        model: MutableMap<String, Any>,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): List<Entry> {
        val logger: Logger = Logger.getLogger(AtomFeedView::class.simpleName)

        val atomList = mutableListOf<Entry>()
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
                    var queryLimit = 20
                    if (model.containsKey("queryLimit")) {
                        queryLimit = model["queryLimit"] as Int
                    }

                    for (randomAlbum in randomAlbums) {
                        var albumPhotos: MutableIterable<AlbumPhoto?>? = null
                        try {
                            albumPhotos = albumPhotoRepository?.findRandomImagesByAlbumIdAndLimit(randomAlbum.getAlbumId()!!, queryLimit)
                        } catch (e: Exception) {
                            logger.log(Level.WARNING, "albumPhotoRepository?.findRandomImagesByAlbumIdAndLimit error: ${e.message}")
                        }

                        if (albumPhotos != null) {
                            for (albumPhoto in albumPhotos) {

                                var metadataOpt: Metadata? = null

                                if (metadataRepository != null && metadataRepository!!.count() > 0) {
                                    try {
                                        metadataOpt = metadataRepository?.findByMetadataId(albumPhoto?.getMetadataId()!!)
                                    } catch (e: Exception) {
                                        logger.log(Level.WARNING, "metadataRepository?.findByMetadataId error: ${e.message}")
                                    }

                                    if (metadataOpt != null) {
                                        val metadata = metadataOpt
                                        val album = albumRepository?.findAlbumById(albumPhoto?.getAlbumId())

                                        val entry = Entry()
                                        entry.id = "$baseUrl/api/v1/image/${metadata.getId()}"
                                        entry.title = metadata.getTitle()

                                        var place = ""
                                        var metadataDescription = ""
                                        var albumName = ""
                                        if (metadata.getPlaceName() != null && metadata.getPlaceName() != "") {
                                            val placeArray = metadata.getPlaceName()!!.split(";")
                                            place = placeArray[0].trim() + " - "
                                        }
                                        if (metadata.getDescription() != null && metadata.getDescription() != "") {
                                            metadataDescription = metadata.getDescription()!!.trim() + " - "
                                        }
                                        if (album?.getName() != null && album.getName() != "") {
                                            albumName = album.getName()!!.trim() + " - "
                                        }
                                        val descVal = "$albumName$metadataDescription$place"

                                        val content = Content()
                                        content.type = "text/html"
                                        content.value = "<img src='$baseUrl/api/v1/thumbnails/225/${metadata.getId()}'><br>${descVal.dropLast(3)}"
                                        entry.contents = listOf(content)
                                        val summaryContent = Content()
                                        summaryContent.type = "text/html"
                                        summaryContent.value = descVal.dropLast(3)
                                        entry.summary = summaryContent

                                        val link = Link()
                                        link.href = "$baseUrl/api/v1/image/${metadata.getId()}"
                                        link.rel = "enclosure"
                                        link.type = metadata.getType()
                                        link.length = Files.size(Path(metadata.getPath()!!))
                                        entry.alternateLinks = listOf(link)
                                        val author: SyndPerson = Person()
                                        author.name = metadata.getId()
                                        entry.authors = listOf(author)
                                        entry.alternateLinks = listOf(link)
                                        val pattern = "EEE, MMM d, yyyy 'at' h:mm a"
                                        val simpleDateFormat = SimpleDateFormat(pattern)
                                        entry.updated =
                                            simpleDateFormat.parse(TextUtils.formatToLongDateWithTime((metadata.getCreatedAt()!!)))

                                        atomList.add(entry)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return atomList.shuffled()
    }

}