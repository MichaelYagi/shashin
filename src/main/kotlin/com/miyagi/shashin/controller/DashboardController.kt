package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.atomic.AtomicLong


@Controller
@Secured("ROLE_ADMIN")
class DashboardController {
    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    @Autowired
    private lateinit var metadataRepository: MetadataRepository

    @Autowired
    private lateinit var mediaDirRepository: MediaDirectoryRepository

    @Autowired
    private lateinit var albumRepository: AlbumRepository

    @Autowired
    private lateinit var albumPhotoRepository: AlbumPhotoRepository

    @Autowired
    private lateinit var albumPhotoCommentRepository: AlbumPhotoCommentRepository

    @Autowired
    private lateinit var favoriteRepository: FavoriteRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var recognitionLabelRepository: RecognitionLabelRepository

    @Autowired
    private lateinit var recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @RequestMapping(value = ["/dashboard"], method = [RequestMethod.GET])
    fun getDashboard(model: Model): String {
        val module = "dashboard"
        val response = buildDashboardData(model)

        for ((k, v) in response) {
            model[k] = v!!
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun buildDashboardData(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        // Site stats
        val photosWithPeopleTaggedCount = recognitionLabelPhotoRepository.countDistinctMetadataId()
        val favoritesCount = favoriteRepository.count()
        val commentsCount = commentRepository.count()
        val albumCount = albumRepository.count()
        response["photosWithPeopleTaggedCount"] = photosWithPeopleTaggedCount
        response["favoritesCount"] = favoritesCount
        response["commentsCount"] = commentsCount
        response["albumCount"] = albumCount

        // Files stats
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
        val sidecarSize = Files.walk(Paths.get(sidecarDir)).mapToLong { p -> p.toFile().length() }.sum()
        response["sidecarSizeMB"] = sidecarSize/(1024 * 1024)

        // User stats
        val allowedUserCount = userRepository.countAllByIsAllowedIsTrueAndAuthorityEquals(userRole!!)
        val notAllowedUserCount = userRepository.countAllByIsAllowedIsFalseAndAuthorityEquals(userRole!!)
        val allowedAdminCount = userRepository.countAllByIsAllowedIsTrueAndAuthorityEquals(adminRole!!)
        val notAllowedAdminCount = userRepository.countAllByIsAllowedIsFalseAndAuthorityEquals(adminRole!!)
        val loggedInCount = userRepository.countAllByLoggedInIsTrue()
        response["allowedUserCount"] = allowedUserCount
        response["notAllowedUserCount"] = notAllowedUserCount
        response["allowedAdminCount"] = allowedAdminCount
        response["notAllowedAdminCount"] = notAllowedAdminCount
        response["loggedInCount"] = loggedInCount

        // Media stats
        val photoCount = metadataRepository.countAllByTypeContains("image")
        val videoCount = metadataRepository.countAllByTypeContains("video")
        val locatedCount = metadataRepository.countAllByLatIsNotNullAndLngIsNotNull()
        val notLocatedCount = metadataRepository.countAllByLatIsNullAndLngIsNull()
        response["photoCount"] = photoCount
        response["videoCount"] = videoCount
        response["locatedCount"] = locatedCount
        response["notLocatedCount"] = notLocatedCount
        val cameraCounts = metadataRepository.countByCameraType()
        val cameraCountList = ArrayList<HashMap<String, Any>>()
        for (cameraCount in cameraCounts) {
            val cameraCountMap = HashMap<String, Any>()
            var cameraName = cameraCount.getCamera().toString()
            if (cameraCount.getCamera() == null) {
                cameraName = "Unknown"
            }
            cameraCountMap["x"] = cameraName
            cameraCountMap["y"] = cameraCount.getCount().toString().toInt()
            cameraCountList.add(cameraCountMap)
        }
        response["cameraCountJson"] = mapper.writeValueAsString(cameraCountList)
        response["cameraTotalCount"] = cameraCountList.count()

        response["message"] = ""

        return response
    }
}