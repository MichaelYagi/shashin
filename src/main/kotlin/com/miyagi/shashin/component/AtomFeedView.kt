package com.miyagi.shashin.component

import com.miyagi.shashin.repository.AlbumPhotoRepository
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.UserRepository
import com.rometools.rome.feed.atom.Content
import com.rometools.rome.feed.atom.Entry
import com.rometools.rome.feed.atom.Feed
import com.rometools.rome.feed.atom.Link
import com.rometools.rome.feed.rss.Enclosure
import com.rometools.rome.feed.rss.Guid
import com.rometools.rome.feed.synd.SyndPerson
import com.rometools.rome.feed.synd.SyndPersonImpl
import org.jdom2.filter.Filters.document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.web.servlet.view.feed.AbstractAtomFeedView
import java.nio.file.Files
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
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

        feed.alternateLinks = listOf(link);
        feed.updated = Date()
    }

    override fun buildFeedEntries(
        model: MutableMap<String, Any>,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): List<Entry> {

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
                    for (randomAlbum in randomAlbums) {
                        if (randomAlbum.getIsShared() == 1) {
                            val albumPhotos = albumPhotoRepository?.findImagesByAlbumId(randomAlbum.getAlbumId()!!, 100)
                            if (albumPhotos != null) {
                                for (albumPhoto in albumPhotos) {
                                    val metadata = metadataRepository?.findByMetadataId(albumPhoto?.getMetadataId()!!)
                                    if (metadata != null) {
                                        val album = albumRepository?.findAlbumById(albumPhoto?.getAlbumId())

                                        val entry = Entry()
                                        entry.id = metadata.getId()
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
                                        content.value = descVal.dropLast(3)
                                        entry.summary = content

                                        val link = Link()
                                        link.href = "$baseUrl/api/v1/image/${metadata.getId()}"
                                        entry.alternateLinks = listOf(link)

                                        atomList.add(entry)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return atomList
    }

}