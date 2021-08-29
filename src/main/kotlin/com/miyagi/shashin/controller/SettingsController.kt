package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScanMessage
import com.miyagi.shashin.model.MediaDirectory
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.ImageProcessingUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Sort
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpSession
import javax.transaction.Transactional


@Controller
class SettingsController {

    @Value("\${app.api.version}")
    private var apiVersion: String? = null

    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String? = null

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Autowired
    private val mediaDirRepository: MediaDirectoryRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val userAlbumRepository: UserAlbumRepository? = null

    @Autowired
    private val favoriteRepository: FavoriteRepository? = null

    @Autowired
    private val commentRepository: CommentRepository? = null

    @Autowired
    private val albumPhotoCommentRepository: AlbumPhotoCommentRepository? = null

    @Autowired
    private val albumRepository: AlbumRepository? = null

    @Autowired
    private val albumPhotoRepository: AlbumPhotoRepository? = null

    @Autowired
    private val messagingTemplate: SimpMessagingTemplate? = null

    private var logger: Logger = Logger.getLogger(SettingsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @MessageMapping("/scanmessage")
    @SendTo("/topic/messages")
    @Throws(java.lang.Exception::class)
    fun sendScanMessage(message: ScanMessage): Message? {
        //println("message:${message.getMessage()}")

        var msg = "Start Scan";

        if (!checkThreadFileAlive()) {
            msg = "Scan Complete"
        }

        val threadFileContent = readThreadFile()
        if (threadFileContent != null) {
            msg = "Scan in progress: " + threadFileContent.replace("\\","/")
        }

        val messageObj = Message()
        messageObj.setContent(msg)

        return messageObj
    }

    @SubscribeMapping("/topic/messages")
    fun subscribe(
        session: HttpSession,
        @PathVariable pipelineId: String,
        @PathVariable topic: String
    ) {
        //println("subscribe")
        //println(session.id)
//        messagingTemplate?.convertAndSend("/app/scanmessage", "testingzzz");

    }

    @EventListener
    fun onApplicationEvent(event: SessionConnectEvent) {
//        println("SessionConnectEvent")
//        println(event.source)

//        messagingTemplate?.convertAndSend("/topic/messages", "testingzzz");
    }

    @EventListener
    fun onApplicationEvent(event: SessionDisconnectEvent) {
//        println("SessionDisconnectEvent")
//        println(event.sessionId)
    }

    @EventListener
    fun handleSubscribeEvent(event: SessionSubscribeEvent) {
//        println("SessionSubscribeEvent")
//        println(event.message)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings")
    fun getSettings(model: Model): String {
        val mediaDirectories = mediaDirRepository?.findAll()

        val module = "settings"
        model["data"] = ""
        model["mediaDirList"] = ""
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && mediaDirectories != null) {
            model["mediaDirList"] = mediaDirectories.joinToString { "${it?.getDirectory()}" }
        }
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["message"] = ""
        model["alertClass"] = ""
        return module
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings"], method = [RequestMethod.POST])
    fun postSettings(model: Model, redirectAttributes: RedirectAttributes, @RequestParam("mediaDirList") mediaDirList: String): String {
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && mediaDirList.isNotBlank()) {
            val mediaDirs = mediaDirList.trim().split(",").map { it.trim() }
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            val mediaDirArrayList: ArrayList<MediaDirectory> = ArrayList()

            if (mediaDirs.isNotEmpty()) {
                for (mediaDir in mediaDirs) {
                    var mediaDirObj = mediaDirRepository?.findByDirectory(mediaDir)
                    if (mediaDirObj == null) {
                        mediaDirObj = MediaDirectory()
                        mediaDirObj.setDirectory(mediaDir)
                    }
                    mediaDirObj.setCreatedAt(dtf.format(now))
                    mediaDirObj.setModifiedAt(dtf.format(now))
                    mediaDirArrayList.add(mediaDirObj)
                }
                mediaDirRepository?.saveAll(mediaDirArrayList)
            }
        }

        val module = "settings"
        model["data"] = ""
        model["mediaDirList"] = ""
        model["mediaDirList"] = mediaDirList.trim()
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["message"] = "Success"
        model["alertClass"] = "alert-success"
        return module
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/users")
    fun getUsers(model: Model): String {
        val module = "users"
        model["users"] = ""

        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
            val sort = Sort.by(
                Sort.Order.asc("username")
            )
            val users = userRepository?.findAll(sort)
            if (users != null) {
                model["users"] = users
            }
        }

        model["data"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/user/delete/{userId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteUser(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userDeleteMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && userDeleteMap.containsKey("userId") && userDeleteMap.containsKey("delete") && model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
            val userIdRequest = userDeleteMap["userId"].toString().toInt()
            val deleteFlag = userDeleteMap["delete"].toString().toBoolean()

            if (deleteFlag && userId == userIdRequest) {
                userRepository?.deleteById(userId)
                userAlbumRepository?.deleteByUserId(userId)
                favoriteRepository?.deleteByUserId(userId)
                commentRepository?.deleteByUserId(userId)
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
    @RequestMapping(value = ["/settings/user/role/{userId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun changeUserRole(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userRoleChangeMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && userRoleChangeMap.containsKey("userId") && userRoleChangeMap.containsKey("changeTo") && model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
            val userIdRequest = userRoleChangeMap["userId"].toString().toInt()
            val changeRoleTo = userRoleChangeMap["changeTo"].toString()

            if (changeRoleTo == model.getAttribute("userRole")) {
                favoriteRepository?.deleteByUserId(userIdRequest)
            }
            if (userId == userIdRequest) {
                val userObj = userRepository?.findById(userId)?.get()
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now()
                if (userObj != null) {
                    userObj.setModifiedAt(dtf.format(now))
                    userObj.setAuthority(changeRoleTo)
                    userRepository?.save(userObj)
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
    @GetMapping("/settings/scan")
    fun getScan(model: Model): String {
        return if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
            val module = "scan"
            model["data"] = "Click scan to scan photo directories"
            model["activePage"] = module
            model["activeSidebar"] = module
            model["titleDescriptor"] = TextUtils.capitalized(module)
            module
        } else {
            "albums"
        }
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/scan"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postScan(model: Model, @RequestParam submit: String, @RequestParam isCheckOnly: Boolean): String {
        resp["msg"] = "Nothing to see here"

        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
            // Check for deleted original files
            val metadataList = metadataRepository?.findAll()
            if (metadataList != null) {
                for (metadata in metadataList) {
                    if (metadata != null) {
                        if (!metadata.getPath().isNullOrBlank()) {
                            val checkFile = File(metadata.getPath()!!)
                            if (!checkFile.exists()) {
                                // Delete side car and metadata files
                                if (!metadata.getThumbnailPathCentered().isNullOrBlank()) {
                                    val fileObj = File(metadata.getThumbnailPathCentered()!!)
                                    if (fileObj.delete()) {
                                        logger.log(Level.INFO, "Deleted thumbnail centered file: " + fileObj.name)
                                    } else {
                                        logger.log(Level.WARNING, "Failed to delete thumbnail centered file: " + fileObj.name)
                                    }
                                }
                                if (!metadata.getThumbnailPathSmall().isNullOrBlank()) {
                                    val fileObj = File(metadata.getThumbnailPathSmall()!!)
                                    if (fileObj.delete()) {
                                        logger.log(Level.INFO, "Deleted thumbnail small file: " + fileObj.name)
                                    } else {
                                        logger.log(Level.WARNING, "Failed to delete thumbnail small file: " + fileObj.name)
                                    }
                                }
                                if (!metadata.getThumbnailPathOriginal().isNullOrBlank()) {
                                    val fileObj = File(metadata.getThumbnailPathOriginal()!!)
                                    if (fileObj.delete()) {
                                        logger.log(Level.INFO, "Deleted thumbnail original file: " + fileObj.name)
                                    } else {
                                        logger.log(Level.WARNING, "Failed to delete thumbnail original file: " + fileObj.name)
                                    }
                                }
                                if (!metadata.getMapMarkerPath().isNullOrBlank()) {
                                    val fileObj = File(metadata.getMapMarkerPath()!!)
                                    if (fileObj.delete()) {
                                        logger.log(Level.INFO, "Deleted map marker file: " + fileObj.name)
                                    } else {
                                        logger.log(Level.WARNING, "Failed to delete map marker file: " + fileObj.name)
                                    }
                                }

                                val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                                val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
                                val metadataDir = sidecarDir + "metadata/"
                                val thumbnailDir = sidecarDir.replace('\\', '/')+"thumbnails/"
                                var relativePath: String = metadata.getThumbnailPathOriginal()!!.replace('\\', '/').lowercase().replace(thumbnailDir.lowercase(), "")
                                relativePath = relativePath.replace("_original.jpg","")
                                val metadataYamlFile = "$metadataDir$relativePath.yaml"
                                var fileObj = File(metadataYamlFile)
                                if (fileObj.delete()) {
                                    logger.log(Level.INFO, "Deleted yaml file: " + fileObj.name)
                                } else {
                                    logger.log(Level.WARNING, "Failed to delete yaml file: " + fileObj.name)
                                }
                                val metadataExifFile = "$metadataDir$relativePath.exif.yaml"
                                fileObj = File(metadataExifFile)
                                if (fileObj.delete()) {
                                    logger.log(Level.INFO, "Deleted EXIF file: " + fileObj.name)
                                } else {
                                    logger.log(Level.WARNING, "Failed to delete EXIF file: " + fileObj.name)
                                }

                                // Delete comments
                                logger.log(Level.FINE, "File "+metadata.getPath()+" no longer exists. Deleting metadata: " + metadata.getId())
                                val albumPhotoCommentList = albumPhotoCommentRepository?.findByMetadataId(metadata.getId())
                                if (albumPhotoCommentList != null) {
                                    for (albumPhotoComment in albumPhotoCommentList) {
                                        if (albumPhotoComment != null) {
                                            commentRepository?.deleteById(albumPhotoComment.getId())
                                        }
                                    }
                                }
                                albumPhotoCommentRepository?.deleteByMetadataId(metadata.getId())
                                logger.log(Level.INFO, "Removed comment records for: " + metadata.getId())

                                // Delete from favorites
                                favoriteRepository?.deleteByMetadataId(metadata.getId())
                                logger.log(Level.INFO, "Removed favorite records for: " + metadata.getId())

                                // Delete from album
                                albumPhotoRepository?.deleteByMetadataId(metadata.getId())
                                val albumPhotoCounts = albumRepository?.countNumberOfPhotosInAlbums()
                                if (albumPhotoCounts != null) {
                                    for (albumPhotoCount in albumPhotoCounts) {
                                        if (albumPhotoCount != null) {
                                            if (albumPhotoCount.getPhotoCount() == 0) {
                                                // Delete the album
                                                albumRepository?.deleteById(albumPhotoCount.getAlbumId()!!)
                                                userAlbumRepository?.deleteByAlbumId(albumPhotoCount.getAlbumId()!!)
                                            }
                                        }
                                    }
                                }
                                logger.log(Level.INFO, "Removed album records for: " + metadata.getId())

                                // Delete from Metadata
                                metadataRepository?.deleteById(metadata.getId())
                                logger.log(Level.INFO, "Removed metadata records for: " + metadata.getId())
                            }
                        }
                    }
                }
            }

            // Scan files
            if (isCheckOnly) {
                if (!checkThreadFileAlive()) {
                    resp["msg"] = "Scan complete"
                    return mapper.writeValueAsString(resp)
                }

                val threadFileContent = readThreadFile()
                if (threadFileContent != null) {
                    resp["msg"] = "Scanning " + threadFileContent.replace("\\","\\\\")
                    return mapper.writeValueAsString(resp)
                }
                resp["msg"] = "Start scanning"
                return mapper.writeValueAsString(resp)
            } else if (submit == "Scan") {
                val mediaDirs = mediaDirRepository?.findAll()

                if (mediaDirs != null) {
                    if (mediaDirs.count() > 0) {
                        if (!checkThreadFileAlive()) {
                            // Clean up any existing thread files
                            deleteThreadFiles()

                            val rootPath = FileSystemResource("").file.absolutePath
                            val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")

                            // Iterate through directory in another thread
                            Thread {
                                //Create file with thread name and write file name iterated
                                val tempDir = System.getProperty("java.io.tmpdir")
                                val threadFile = FileUtils.createFile(tempDir, tempDir + "/" + Thread.currentThread().name + ".shashinscan", "Thread")
                                if (threadFile != null) {
                                    // Scan for new files
                                    for (mediaDir in mediaDirs) {
                                        if (mediaDir != null) {
                                            getFile(mediaDir.getDirectory().toString(), threadFile, sidecarDir, mediaDir.getDirectory().toString())
                                        }
                                    }

                                    logger.log(Level.INFO, "Scan completed")

                                    // Delete thread file
                                    if (threadFile.delete()) {
                                        logger.log(Level.FINE, "Thread file deleted: " + threadFile.name)
                                    } else {
                                        logger.log(Level.SEVERE, "Could not delete thread file: " + threadFile.name)
                                    }
                                }
                            }.start()

                            resp["msg"] = "Scan started"
                            return mapper.writeValueAsString(resp)
                        }

                        val threadFileContent = readThreadFile()
                        if (threadFileContent != null) {
                            resp["msg"] = "Scanning " + threadFileContent.replace("\\","\\\\")
                            return mapper.writeValueAsString(resp)
                        } else {
                            resp["msg"] = "Scan in progress"
                            return mapper.writeValueAsString(resp)
                        }
                    }
                    resp["msg"] = "No directories configured"
                }
                resp["msg"] = "No directories configured"
            }
        }

        return mapper.writeValueAsString(resp)
    }

    private fun threadIsAlive(threadName: String): Boolean {
        for (t in Thread.getAllStackTraces().keys) {
            if (t.name == threadName) {
                return t.isAlive
            }
        }
        return false
    }

    private fun readThreadFile(): String? {
        val tempDir = System.getProperty("java.io.tmpdir")
        val f = File(tempDir)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {
                val file: File = files[i]

                if (file.isFile &&
                    file.extension.lowercase() == "shashinscan" &&
                    threadIsAlive(file.nameWithoutExtension)
                ) {
                    return Files.readString(file.toPath())
                }
            }
        }

        return null
    }

    private fun checkThreadFileAlive(): Boolean {
        val tempDir = System.getProperty("java.io.tmpdir")
        val f = File(tempDir)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {
                val file: File = files[i]

                if (file.isFile &&
                    file.extension.lowercase() == "shashinscan" &&
                    threadIsAlive(file.nameWithoutExtension)
                ) {
                    return true
                }
            }
        }

        return false
    }

    private fun deleteThreadFiles() {
        val tempDir = System.getProperty("java.io.tmpdir")
        val f = File(tempDir)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {
                val file: File = files[i]

                if (file.isFile &&
                    file.extension.lowercase() == "shashinscan"
                ) {
                    file.delete()
                }
            }
        }
    }

    private fun getFile(dirPath: String, threadFile: File, sidecarDir: String, rootDir: String) {
        val f = File(dirPath)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {

                val file: File = files[i]

                if (file.isFile) {
                    val supportedFormats = FileUtils.allowableMediaFiles()
                    if (supportedFormats.contains(file.extension.lowercase())) {

                        try {
                            val writer = BufferedWriter(FileWriter(threadFile))
                            writer.write(file.path)
                            writer.close()
                        } catch(e: Exception) {
                            logger.log(Level.WARNING, "Could not write to thread file: " + threadFile.name)

                        }

                        // TODO: Check if mapped sidecar file exists, if it does, skip creating them

                        if (FileUtils.allowableMediaFiles().contains(file.extension.lowercase())) {
                            val imageProcessingUtils = ImageProcessingUtils(apiVersion,geocodeUrl)
                            var metadataObj: Metadata? = Metadata()
                            metadataObj =
                                imageProcessingUtils.createThumbnails(file, sidecarDir, rootDir, metadataObj)
                            metadataObj =
                                imageProcessingUtils.populateMetadata(file, sidecarDir, rootDir, metadataObj)
                            if (metadataObj != null) {
                                metadataRepository?.save(metadataObj)
                            }
                        }
                    }
                }

                if (file.isDirectory) {
                    getFile(file.absolutePath, threadFile, sidecarDir, rootDir)
                }
            }
        }
    }
}