package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.MediaDirectory
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.ImageProcessingUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Sort
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger
import javax.transaction.Transactional

@Controller
@Secured("ROLE_ADMIN")
class SettingsController {

    @Value("\${app.api.version}")
    private var apiVersion: String? = null

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

    private var logger: Logger = Logger.getLogger(SettingsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/settings")
    fun getSettings(model: Model): String {
        val mediaDirectories = mediaDirRepository?.findAll()

        val module = "settings"
        model["data"] = ""
        model["mediaDirList"] = ""
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && mediaDirectories != null) {
            model["mediaDirList"] = mediaDirectories.joinToString { it -> "${it?.getDirectory()}" }
        }
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        model["message"] = ""
        model["alertClass"] = ""
        return module
    }

    @RequestMapping(value = ["/settings"], method = [RequestMethod.POST])
    fun postSettings(model: Model, redirectAttributes: RedirectAttributes, @RequestParam("mediaDirList") mediaDirList: String): String {
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole") && !mediaDirList.isNullOrBlank()) {
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

    @GetMapping("/settings/scan")
    fun getScan(model: Model): String {
        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
            val module = "scan"
            model["data"] = "Scan photos"
            model["activePage"] = module
            model["activeSidebar"] = module
            model["titleDescriptor"] = TextUtils.capitalized(module)
            return module
        } else {
            return "albums"
        }
    }

    @RequestMapping(value = ["/settings/scan"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun postScan(model: Model, @RequestParam submit: String, @RequestParam isCheckOnly: Boolean): String {
        resp["msg"] = "Nothing to see here"

        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
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
                                    if (mediaDirs != null) {
                                        for (mediaDir in mediaDirs) {
                                            if (mediaDir != null) {
                                                getFile(mediaDir.getDirectory().toString(), threadFile, sidecarDir, mediaDir.getDirectory().toString())
                                            }
                                        }
                                    }

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

                // TODO: Remove this test
                //Thread.sleep(4000);

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

                        // TODO: If RAW then convert to jpeg
//                        if (FileUtils.isRaw(file.extension.lowercase())) {
//
//                        } else {
                            // TODO: Check if mapped sidecar file exists, if it does, skip creating them
                            if (false) {

                            } else {
                                if (FileUtils.allowableMediaFiles().contains(file.extension.lowercase())) {
                                    val imageProcessingUtils = ImageProcessingUtils(apiVersion)
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
//                        }
                    }
                }

                if (file.isDirectory) {
                    getFile(file.absolutePath, threadFile, sidecarDir, rootDir)
                }
            }
        }
    }
}