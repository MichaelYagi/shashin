package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.UserAlbum
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.service.MemoriesService
import com.miyagi.shashin.service.MemoryView
import com.miyagi.shashin.util.TextUtils
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.logging.Level
import java.util.logging.Logger

@Controller
class MemoriesController(
    private val memoriesService: MemoriesService,
    private val memoryRepository: MemoryRepository,
    private val memoryPhotoRepository: MemoryPhotoRepository,
    private val albumRepository: AlbumRepository,
    private val albumPhotoRepository: AlbumPhotoRepository,
    private val userAlbumRepository: UserAlbumRepository,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val keywordRepository: KeywordRepository? = null,
    private val recognitionLabelRepository: RecognitionLabelRepository? = null,
    private val metadataRepository: MetadataRepository? = null
): BaseController(
    recognitionLabelRepository = recognitionLabelRepository,
    albumRepository = albumRepository,
    keywordRepository = keywordRepository,
    metadataRepository = metadataRepository
) {
    private val logger = Logger.getLogger(MemoriesController::class.simpleName)
    private val mapper = ObjectMapper()

    @Secured("ROLE_ADMIN", "ROLE_SUPER")
    @GetMapping("/memories")
    fun getMemories(model: Model): String {
        val settings = settingsRepository.findFirstByOrderByIdAsc()
        val module = "memories"

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = "Memories"

        if (settings?.getShowMemories() != true) {
            getAllAttributeData(model)
            model["memoriesList"] = emptyList<MemoryView>()
            model["memoriesJson"] = "[]"
            model["memoriesDisabled"] = true
            return "memories"
        }

        getAllAttributeData(model)
        model["memoriesDisabled"] = false
        val views = memoriesService.getMemoryViews().map { view ->
            val album = albumRepository.findAlbumByNameIgnoreCase(view.title)
            if (album != null) view.copy(savedAlbumId = album.getId()) else view
        }
        model["memoriesList"] = views
        model["memoriesJson"] = mapper.writeValueAsString(
            views.map { mapOf("id" to it.id, "photoIds" to it.photoIds) }
        )
        return "memories"
    }

    @Secured("ROLE_ADMIN", "ROLE_SUPER")
    @PostMapping("/memories/{id}/save")
    @ResponseBody
    fun saveMemoryAsAlbum(
        @PathVariable id: Int,
        principal: Principal
    ): Map<String, Any> {
        val memory = memoryRepository.findById(id).orElse(null)
            ?: return mapOf("status" to "error", "msg" to "Memory not found")

        val photoIds = memoryPhotoRepository.findByMemoryIdOrderByDisplayOrder(id)
            .mapNotNull { it.getMetadataId() }

        if (photoIds.isEmpty()) return mapOf("status" to "error", "msg" to "Memory has no photos")

        val title = memory.getTitle() ?: "Memory"
        val existingAlbum = albumRepository.findAlbumByNameIgnoreCase(title)
        if (existingAlbum != null) {
            return mapOf("status" to "success", "msg" to "", "albumId" to (existingAlbum.getId() ?: 0), "albumName" to (existingAlbum.getName() ?: title))
        }
        val existingName = findUniqueName(title)

        val album = Album()
        album.setName(existingName)
        album.setCoverUrl("/api/v1/thumbnails/centered/${photoIds.first()}")
        album.setCreatedAt(TextUtils.getCurrentTimestamp())
        album.setModifiedAt(TextUtils.getCurrentTimestamp())
        val savedAlbum = albumRepository.save(album)
        val albumId = savedAlbum?.getId() ?: return mapOf("status" to "error", "msg" to "Could not create album")

        val user = userRepository.findByUsername(principal.name)
            ?: return mapOf("status" to "error", "msg" to "User not found")

        val userAlbum = UserAlbum()
        userAlbum.setUserId(user.getId())
        userAlbum.setAlbumId(albumId)
        userAlbum.setCreatedAt(TextUtils.getCurrentTimestamp())
        userAlbum.setModifiedAt(TextUtils.getCurrentTimestamp())
        userAlbumRepository.save(userAlbum)

        photoIds.forEachIndexed { idx, metadataId ->
            val ap = AlbumPhoto()
            ap.setAlbumId(albumId)
            ap.setMetadataId(metadataId)
            ap.setCreatedAt(TextUtils.getCurrentTimestamp())
            ap.setModifiedAt(TextUtils.getCurrentTimestamp())
            try { albumPhotoRepository.save(ap) } catch (e: Exception) {
                logger.log(Level.WARNING, "Skipped duplicate albumphoto for $metadataId: ${e.localizedMessage}")
            }
        }

        logger.log(Level.INFO, "Memory \"$title\" saved as album #$albumId with ${photoIds.size} photos")
        return mapOf("status" to "success", "msg" to "", "albumId" to albumId, "albumName" to existingName)
    }

    @Secured("ROLE_ADMIN", "ROLE_SUPER")
    @PostMapping("/memories/generate")
    @ResponseBody
    fun triggerGenerate(): Map<String, Any> {
        if (memoriesService.generationRunning.get()) {
            return mapOf("status" to "already_running", "msg" to "")
        }
        val settings = settingsRepository.findFirstByOrderByIdAsc()
            ?: return mapOf("status" to "error", "msg" to "Settings not found")
        if (settings.getShowMemories() != true) {
            return mapOf("status" to "error", "msg" to "Memories is disabled")
        }
        Thread { memoriesService.generateMemories(settings) }.start()
        return mapOf("status" to "started", "msg" to "")
    }

    @Secured("ROLE_ADMIN", "ROLE_SUPER")
    @GetMapping("/memories/status")
    @ResponseBody
    fun getStatus(): Map<String, Any> {
        return mapOf(
            "running" to memoriesService.generationRunning.get(),
            "count" to memoryRepository.count()
        )
    }

    private fun findUniqueName(base: String): String {
        if (albumRepository.findAlbumByNameIgnoreCase(base) == null) return base
        var i = 2
        while (albumRepository.findAlbumByNameIgnoreCase("$base ($i)") != null) i++
        return "$base ($i)"
    }
}
