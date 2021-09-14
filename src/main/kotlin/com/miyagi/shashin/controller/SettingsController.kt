package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.component.Message
import com.miyagi.shashin.component.ScanMessage
import com.miyagi.shashin.model.MediaDirectory
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.RecognitionLabel
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.MediaProcessingUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Sort
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
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
import java.nio.file.Path
import java.nio.file.Paths
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
    private val albumCommentRepository: AlbumCommentRepository? = null

    @Autowired
    private val albumRepository: AlbumRepository? = null

    @Autowired
    private val albumPhotoRepository: AlbumPhotoRepository? = null

    @Autowired
    private val recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private val recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private val settingsRepository: SettingsRepository? = null

    private var logger: Logger = Logger.getLogger(SettingsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @MessageMapping("/scanmessage")
    @SendTo("/topic/messages")
    @Throws(java.lang.Exception::class)
    fun sendScanMessage(message: ScanMessage): Message? {
        //println("message:${message.getMessage()}")
        var msg = "Start Scan";

        val mediaDirs = mediaDirRepository?.findAll()
        if (mediaDirs != null && mediaDirs.count() > 0) {
            if (!FileUtils.checkThreadFileAlive("shashinscan")) {
                msg = "Scan Complete"
            }

            val threadFileContent = FileUtils.readThreadFile("shashinscan")
            if (threadFileContent != null) {
                msg = "Scan in progress: " + threadFileContent.replace("\\", "/")
            }
        } else {
            msg = "No directories configured"
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
        model["message"] = ""
        model["mediaDirList"] = ""
        model["settings"] = ""
        model["alertClass"] = ""

        var dirDneString = ""
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && mediaDirectories != null) {
            model["mediaDirList"] = mediaDirectories.joinToString { "${it?.getDirectory()}" }

            for (mediaDir in mediaDirectories) {
                if (mediaDir != null) {
                    val path: Path = Paths.get(mediaDir.getDirectory()!!)
                    if (!Files.exists(path)) {
                        dirDneString += "${mediaDir.getDirectory()},"
                    }
                }
            }
            if (dirDneString.isNotBlank()) {
                dirDneString = "Cannot find "+dirDneString.dropLast(1)
                model["alertClass"] = "alert-warning"
            }
            val settings = settingsRepository?.findFirstByOrderByIdAsc()
            model["settings"] = settings as Settings
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["message"] = dirDneString

        return module
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings"], method = [RequestMethod.POST])
    fun postSettings(model: Model, redirectAttributes: RedirectAttributes,
        @RequestParam("mediaDirList") mediaDirList: String,
        @RequestParam("recognitionConfidenceThreshold") recognitionConfidenceThreshold: String,
        @RequestParam("queryLimit") queryLimit: Int,
        @RequestParam("matchScanLimit") matchScanLimit: Int,
        @RequestParam("trainingDataLimit") trainingDataLimit: Int
    ): String {
        var mediaDirs: List<String>? = null
        val mediaDirArrayList: ArrayList<MediaDirectory> = ArrayList()
        if  (mediaDirList.isNotBlank()) {
            mediaDirs = mediaDirList.trim().split(",").map { it.trim() }
        }

        model["alertClass"] = "alert-success"
        var message = ""

        var dirDneString = ""
        if (mediaDirs != null && mediaDirs.isNotEmpty()) {
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()

            mediaDirRepository?.deleteAll()
            for (mediaDir in mediaDirs) {
                var mediaDirObj = mediaDirRepository?.findByDirectory(mediaDir)
                if (mediaDirObj == null) {
                    mediaDirObj = MediaDirectory()
                    mediaDirObj.setDirectory(mediaDir)
                }
                mediaDirObj.setCreatedAt(dtf.format(now))
                mediaDirObj.setModifiedAt(dtf.format(now))
                mediaDirArrayList.add(mediaDirObj)

                val path: Path = Paths.get(mediaDir)
                if (!Files.exists(path)) {
                    dirDneString += "$mediaDir,"
                }
            }
            if (dirDneString.isNotBlank()) {
                message = "Cannot find "+dirDneString.dropLast(1)
                model["alertClass"] = "alert-warning"
            }
            mediaDirRepository?.saveAll(mediaDirArrayList)
        } else {
            mediaDirRepository?.deleteAll()
        }

        val settings = settingsRepository?.findFirstByOrderByIdAsc()
        if (recognitionConfidenceThreshold.isNotEmpty()) {
            settings?.setRecognitionConfidenceThreshold(recognitionConfidenceThreshold)
        }
        if (queryLimit > 0) {
            settings?.setQueryLimit(queryLimit)
        }
        if (matchScanLimit > 0) {
            settings?.setMatchScanLimit(matchScanLimit)
        }
        if (trainingDataLimit > 0) {
            settings?.setTrainingDataLimit(trainingDataLimit)
        }
        if (settings != null) {
            settingsRepository?.save(settings)
            model["settings"] = settings
        }

        if (message.isBlank() && model.getAttribute("alertClass") == "alert-success") {
            message = "Settings saved"
        }

        val module = "settings"
        model["message"] = ""
        model["mediaDirList"] = ""
        model["mediaDirList"] = mediaDirList.trim()
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["message"] = message
        return module
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/users")
    fun getUsers(model: Model): String {
        val module = "users"
        model["users"] = ""

        val sort = Sort.by(
            Sort.Order.asc("username")
        )
        val users = userRepository?.findAll(sort)
        if (users != null) {
            model["users"] = users
        }

        model["message"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/content/delete"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteContent(model: Model, @RequestBody requestBody: JsonNode): String? {
        val userDeleteMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userDeleteMap.containsKey("deleteContent")) {
            val deleteContent = userDeleteMap["deleteContent"].toString().toBoolean()

            if (deleteContent) {
                metadataRepository?.deleteAll()
                albumRepository?.deleteAll()
                userAlbumRepository?.deleteAll()
                albumPhotoRepository?.deleteAll()
                favoriteRepository?.deleteAll()
                commentRepository?.deleteAll()
                albumCommentRepository?.deleteAll()
                albumPhotoCommentRepository?.deleteAll()
                recognitionLabelRepository?.deleteAll()
                recognitionLabelPhotoRepository?.deleteAll()

                // Clean up thread files
                FileUtils.deleteThreadFiles("shashinscan")
                FileUtils.deleteThreadFiles("facescan_shashinscan")

                val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
                val sidecarDirFile = File(sidecarDir)
                if (sidecarDirFile.exists()) {

                    // Delete it
                    val dirDeleteSuccess = sidecarDirFile.deleteRecursively()

                    if (dirDeleteSuccess) {
                        resp["msg"] = "Success!"
                    } else {
                        resp["msg"] = "Success, but could not delete sidecar files."
                    }
                    resp["status"] = "success"
                    return mapper.writeValueAsString(resp)
                }

                resp["msg"] = "Success!"
                resp["status"] = "success"
                return mapper.writeValueAsString(resp)
            }

            resp["msg"] = "Something went wrong."
            resp["status"] = "fail"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Something went wrong."
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/user/delete/{userId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun deleteUser(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userDeleteMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userDeleteMap.containsKey("userId") && userDeleteMap.containsKey("delete")) {
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
        if (userRoleChangeMap.containsKey("userId") && userRoleChangeMap.containsKey("changeTo")) {
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
    @RequestMapping(value = ["/settings/user/permission/{userId}"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun changeUserLoginPermission(model: Model, @RequestBody requestBody: JsonNode, @PathVariable userId: Int): String? {
        val userRoleChangeMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userRoleChangeMap.containsKey("userId") && userRoleChangeMap.containsKey("changeTo")) {
            val userIdRequest = userRoleChangeMap["userId"].toString().toInt()
            val changePermissionTo = userRoleChangeMap["changeTo"].toString().toBoolean()

            if (userId == userIdRequest) {
                val userObj = userRepository?.findById(userId)?.get()
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now()
                if (userObj != null) {
                    userObj.setModifiedAt(dtf.format(now))
                    userObj.setIsAllowed(changePermissionTo)
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
        val module = "scan"
        model["message"] = "Click scan to scan photo directories"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/settings/match")
    fun getMatchScan(model: Model): String {
        val module = "match"
        model["message"] = "Click scan to start finding people"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/settings/scan"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun postScan(model: Model, @RequestParam submit: String, @RequestParam deleteThread: Boolean): String {
        resp["msg"] = "Nothing to see here"

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
                            val thumbnailDir = sidecarDir.replace('\\', '/')+"thumbnails"
                            var relativePath: String = metadata.getThumbnailPathCentered()!!.replace('\\', '/').lowercase().replace(thumbnailDir.lowercase(), "")
                            relativePath = relativePath.replace("_centered.jpg","")
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
//                            if (metadata.getRecognitionLabelId() != null) {
//                                if (metadataRepository?.countByRecognitionLabelId(metadata.getRecognitionLabelId()!!) == 0) {
//                                    // Delete the label
//                                    recognitionLabelRepository?.deleteById(metadata.getRecognitionLabelId()!!)
//                                }
//                            }
                            metadataRepository?.deleteById(metadata.getId())
                            logger.log(Level.INFO, "Removed metadata records for: " + metadata.getId())
                        }
                    }
                }
            }
        }

        if (deleteThread) {
            FileUtils.deleteThreadFiles("shashinscan")
            logger.log(Level.INFO, "Thread file manually deleted")
            resp["msg"] = "Thread file manually deleted"
        } else if (submit == "Scan") {
            val mediaDirs = mediaDirRepository?.findAll()

            if (mediaDirs != null && mediaDirs.count() > 0) {
                if (!FileUtils.checkThreadFileAlive("shashinscan")) {
                    // Clean up any existing thread files
                    FileUtils.deleteThreadFiles("shashinscan")

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

                            logger.log(Level.INFO, "Scan Complete")

                            // Delete thread file
                            if (threadFile.delete()) {
                                logger.log(Level.FINE, "Thread file deleted: " + threadFile.name)
                            } else {
                                logger.log(Level.SEVERE, "Could not delete thread file: " + threadFile.name)
                            }
                        }
                    }.start()

                    resp["msg"] = "Start Scan"
                    return mapper.writeValueAsString(resp)
                }

                val threadFileContent = FileUtils.readThreadFile("shashinscan")
                if (threadFileContent != null) {
                    resp["msg"] = "Scan in progress: " + threadFileContent.replace("\\", "/")
                    return mapper.writeValueAsString(resp)
                } else {
                    resp["msg"] = "Scan in progress"
                    return mapper.writeValueAsString(resp)
                }
            } else {
                resp["msg"] = "No directories configured"
            }
            resp["msg"] = "Start Scan"
        }

        return mapper.writeValueAsString(resp)
    }

    private fun getFile(dirPath: String, threadFile: File, sidecarDir: String, rootDir: String) {
        val f = File(dirPath)
        val files = f.listFiles()
        if (files != null) {
            for (i in files.indices) {

                val file: File = files[i]

                if (file.isFile) {
                    if (FileUtils.allowableMediaFiles().contains(file.extension.lowercase())) {

                        val mediaProcessingUtils = MediaProcessingUtils(apiVersion,geocodeUrl)
                        var metadataObj: Metadata? = Metadata()

                        var threadText = file.path + " ALREADY SCANNED"

                        if (FileUtils.allowableMediaFiles().contains(file.extension.lowercase())) {
                            metadataObj =
                                mediaProcessingUtils.createThumbnails(file, sidecarDir, rootDir, metadataObj)
                            if (metadataObj != null) {
                                metadataObj =
                                    mediaProcessingUtils.populateMetadata(file, sidecarDir, rootDir, metadataObj)
                            }
                            if (metadataObj != null) {
                                try {
                                    metadataRepository?.save(metadataObj)
                                    threadText = file.path + " indexed"
                                } catch(e: Exception) {
                                    logger.log(Level.SEVERE, "Could not save file "+metadataObj.getPath()+": " + e.localizedMessage)
                                    threadText = file.path + " exception: " + e.localizedMessage
                                }
                            }
                        } else {
                            threadText = file.path + " NOT SUPPORTED"
                            logger.log(Level.WARNING, "File not supported: " + threadFile.name)
                        }

                        try {
                            val writer = BufferedWriter(FileWriter(threadFile))
                            writer.write(threadText)
                            writer.close()
                        } catch(e: Exception) {
                            logger.log(Level.WARNING, "Could not write to thread file: " + threadFile.name)
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