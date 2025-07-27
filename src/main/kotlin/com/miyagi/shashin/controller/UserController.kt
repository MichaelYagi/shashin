package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.CommentRepository
import com.miyagi.shashin.repository.FavoriteRepository
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.PersistentLoginsExpiryRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.repository.SlideshowAlbumRepository
import com.miyagi.shashin.repository.UserAlbumRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import com.miyagi.shashin.util.TextUtils.Companion.parseRememberMeCookie
import com.miyagi.shashin.util.TextUtils.Companion.verifyPersistenceToken
import io.swagger.v3.oas.annotations.Operation
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Sort
import org.springframework.security.access.annotation.Secured
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ResponseCookie
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import kotlin.collections.set
import kotlin.io.path.Path
import kotlin.text.toLong


@Suppress("UNCHECKED_CAST")
@Controller
class UserController {

    private var logger: Logger = Logger.getLogger(UserController::class.simpleName)
    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()
    var bcrypt = BCryptPasswordEncoder()

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    @Value("\${app.api.version}")
    private var apiVersion: String? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

    @Value("\${app.rememberme.expiration.seconds}")
    private var expirationSeconds: Int? = null

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var slideshowAlbumRepository: SlideshowAlbumRepository? = null

    @Autowired
    private val userAlbumRepository: UserAlbumRepository? = null

    @Autowired
    private val settingsRepository: SettingsRepository? = null

    @Autowired
    private val favoriteRepository: FavoriteRepository? = null

    @Autowired
    private val commentRepository: CommentRepository? = null

    @Autowired
    var persistentLoginsRepository: PersistentLoginsRepository? = null

    @Autowired
    var messageSource: MessageSource? = null

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var persistentLoginsExpiryRepository: PersistentLoginsExpiryRepository

    @GetMapping("/users/account")
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun getAccount(model: Model, request: HttpServletRequest, locale: Locale): String {
        model["message"] = ""
        model["user"] = User()
        model["alertClass"] = ""
        model["roleDescription"] = ""
        model["dateJoined"] = ""
        model["status"] = ""
        model["toastTitle"] = ""
        model["toastBody"] = ""
        model["canDeleteAccount"] = false

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && currentUserObj.getAuthority() != "ROLE_SUPER") {
            model["canDeleteAccount"] = true
        }
        val model = getUserInfo(model, currentUserObj, request, locale)

        val module = "account"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @PostMapping("/users/account")
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun postAccount(model: Model, request: HttpServletRequest, response: HttpServletResponse, locale: Locale): String {
        model["message"] = ""
        model["user"] = User()
        model["alertClass"] = ""
        model["roleDescription"] = ""
        model["dateJoined"] = ""
        model["status"] = ""
        model["toastTitle"] = ""
        model["toastBody"] = ""
        model["canDeleteAccount"] = false

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && currentUserObj.getAuthority() != "ROLE_SUPER") {
            model["canDeleteAccount"] = true
        }
        val model = getUserInfo(model, currentUserObj, request, locale)

        if (request.getParameter("oldpassword") != null && request.getParameter("newpassword") != null && request.getParameter("newpasswordconfirm") != null) {
            val oldPassword = java.lang.String.valueOf(request.getParameter("oldpassword")).trim()
            val newPassword = java.lang.String.valueOf(request.getParameter("newpassword")).trim()
            val newPasswordConfirm = java.lang.String.valueOf(request.getParameter("newpasswordconfirm")).trim()

            if (currentUserObj != null) {
                if (newPassword.isNotEmpty() && newPassword == newPasswordConfirm && bcrypt.matches(oldPassword, currentUserObj.getPassword())) {
                    val updatedPassword = bcrypt.encode(newPassword)
                    currentUserObj.setModifiedAt(getCurrentTimestamp())
                    currentUserObj.setPassword(updatedPassword)
                    userRepository?.save(currentUserObj)

                    // Update remember-me cookie
                    val cookies = request.cookies
                    for (cookie in cookies) {
                        if (cookie.name == "remember-me") {
                            val seriesExpiryMap = parseRememberMeCookie(cookie.name+"="+cookie.value)
                            var cookieValue = seriesExpiryMap["cookieValue"]
                            var username = seriesExpiryMap["token"]
                            val series = seriesExpiryMap["series"]
                            var timeStamp = if (series != null && series != "") series.toLong() else 0L

                            if (cookieValue != "" && username != "") {
                                val user = userRepository?.findByUsername(username)
                                if (user != null && user.getId() > 0) {
                                    val now = Instant.now().toEpochMilli()
                                    val updatedCookieValue = verifyPersistenceToken(username.toString(), timeStamp.toString(), user.getPassword().toString(), rememberMeKey.toString())
                                    val resCookie = ResponseCookie.from("remember-me", updatedCookieValue)
                                        .path("/")
                                        .httpOnly(true)
                                        .maxAge((timeStamp.toLong()-now)/1000)
                                        .build()
                                    response.addHeader("Set-Cookie", resCookie.toString())
                                }
                            }
                            break
                        }
                    }

                    model["msg"] = "Success"
                    model["status"] = ApiResponse.SUCCESS.status
                    model["toastTitle"] = messageSource?.getMessage("main.toast.account.password.updated.success.title", null, locale)
                    model["toastBody"] = messageSource?.getMessage("main.toast.account.password.updated.success.body", null, locale)
                } else {
                    model["message"] = ""
                    model["msg"] = ""
                    model["status"] = ApiResponse.FAIL.status
                    model["toastTitle"] = messageSource?.getMessage("main.toast.account.password.updated.fail.title", null, locale)
                    model["toastBody"] = messageSource?.getMessage("main.toast.account.password.updated.fail.body", null, locale)
                }
            }
        }

        val module = "account"
        model["msg"] = ""
        model["status"] = ApiResponse.FAIL.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    private fun getUserInfo(model: Model, currentUserObj: User?, request: HttpServletRequest, locale: Locale): Model {
        if (currentUserObj != null) {
            model["user"] = currentUserObj

            var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
            if (request.scheme == "https") {
                baseUrlBuilder = baseUrlBuilder.scheme("https")
            }
            val baseUrl = baseUrlBuilder.build().toUriString()
            model["rssFeedLink"] = "$baseUrl/${currentUserObj.getApikey()}/rss"
            model["atomFeedLink"] = "$baseUrl/${currentUserObj.getApikey()}/atom"

            if (currentUserObj.getAuthority() == "ROLE_SUPER") {
                model["roleDescription"] = messageSource?.getMessage("main.pages.account.role.description.super", null, locale)
            } else if (currentUserObj.getAuthority() == "ROLE_ADMIN") {
                model["roleDescription"] = messageSource?.getMessage("main.pages.account.role.description.admin", null, locale)
            } else {
                model["roleDescription"] = messageSource?.getMessage("main.pages.account.role.description.user", null, locale)
            }

            model["dateJoined"] = TextUtils.formatToAbbrDate(currentUserObj.getCreatedAt().toString(), model.getAttribute("locale").toString())
        }

        return model
    }

    @RequestMapping(value = ["/users/profile"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun postUpdateProfile(model: Model, @RequestBody requestBody: JsonNode): String {
        val base64Map = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        val response = mutableMapOf<String, Any?>()
        response["randomString"] = TextUtils.generateUUID(getCurrentTimestamp(),null,null,null,null,null,"random string generated from UserController")

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {

            if (base64Map.containsKey("base64")) {
                val base64Image = base64Map["base64"].toString()

                val imageBytes = FileUtils.parseBase64(base64Image)
                if (imageBytes != null) {
                    // save file
                    val img = ImageIO.read(ByteArrayInputStream(imageBytes))
                    val extension = "png"

                    val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                    val sidecarDir = rootPath + relativeSidecarDir

                    val uuidFromUsername = TextUtils.generateUUID(currentUserObj.getUsername(),null,null,null,null,null,"user UUID generated from UserController")

                    val profileDirectory = sidecarDir.dropLast(1) + "/profile"
                    val profileFileStr = "$profileDirectory/$uuidFromUsername.$extension"
                    if (File(profileFileStr).exists()) {
                        File(profileFileStr).delete()
                    }
                    val profileFile = FileUtils.createFile(profileFileStr)
                    if (profileFile != null) {
                        val tempFile = File(System.getProperty("java.io.tmpdir") + ".$extension")
                        ImageIO.write(img, extension, profileFile)
                        tempFile.delete()
                    }

                    // save as url
                    val profileUrl = "/api/$apiVersion/profile/$uuidFromUsername.$extension"
                    currentUserObj.setProfile(profileUrl)
                    userRepository?.save(currentUserObj)

                    response["msg"] = "Updated profile picture"
                    response["message"] = "Updated profile picture"
                    response["status"] = ApiResponse.SUCCESS.status
                    response["imageUrl"] = profileUrl

                    return mapper.writeValueAsString(response)
                }
            }
        }

        response["msg"] = "Could not update profile picture"
        response["message"] = "Could not update profile picture"
        response["status"] = ApiResponse.FAIL.status
        response["imageUrl"] = ""

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/users/delete/profile"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun postDeleteProfile(model: Model, @RequestBody requestBody: JsonNode): String {
        val requestMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        val response = mutableMapOf<String, Any?>()
        response["randomString"] = TextUtils.generateUUID(getCurrentTimestamp(),null,null,null,null,null,"random string generated from UserController")

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {

            if (requestMap.containsKey("userId")) {
                val userId = requestMap["userId"]?.toInt()

                if (userId != null && userId > 0 && currentUserObj.getId() == userId) {
                    val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                    val sidecarDir = rootPath + relativeSidecarDir

                    val uuidFromUsername = TextUtils.generateUUID(currentUserObj.getUsername(),null,null,null,null,null,"user UUID generated from UserController")

                    val extension = "png"
                    val profileDirectory = sidecarDir.dropLast(1) + "/profile"
                    val profileFileStr = "$profileDirectory/$uuidFromUsername.$extension"
                    if (File(profileFileStr).exists()) {
                        File(profileFileStr).delete()
                        currentUserObj.setProfile(null)
                        userRepository?.save(currentUserObj)

                        response["msg"] = "Profile picture deleted"
                        response["message"] = "Profile picture deleted"
                        response["status"] = ApiResponse.SUCCESS.status
                        return mapper.writeValueAsString(response)
                    }
                }
            }
        }

        response["msg"] = "Could not delete profile picture"
        response["message"] = "Could not delete profile picture"
        response["status"] = ApiResponse.FAIL.status

        return mapper.writeValueAsString(response)
    }

    @GetMapping("/users/apikey")
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun getApiKey(model: Model, request: HttpServletRequest): String {

        model["message"] = ""
        model["user"] = User()
        model["alertClass"] = ""
        model["rssFeedLink"] = ""
        model["atomFeedLink"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            model["user"] = currentUserObj

            var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
            if (request.scheme == "https") {
                baseUrlBuilder = baseUrlBuilder.scheme("https")
            }
            val baseUrl = baseUrlBuilder.build().toUriString()
            model["rssFeedLink"] = "$baseUrl/${currentUserObj.getApikey()}/rss"
            model["atomFeedLink"] = "$baseUrl/${currentUserObj.getApikey()}/atom"
        }


        val module = "apikey"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = "API Key"
        return module
    }

    @RequestMapping(value = ["/users/update/apikey"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun postWebUpdateApikey(model: Model, request: HttpServletRequest, @RequestBody requestBody: JsonNode): String {
        val apikeyMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        val response = mutableMapOf<String, Any?>()
        response["msg"] = "Could not update API key"
        response["message"] = "Could not update API key"
        response["status"] = ApiResponse.FAIL.status
        response["updatedApikey"] = ""
        response["rssFeedLink"] = ""
        response["atomFeedLink"] = ""

        if (apikeyMap.containsKey("currentApikey")) {
            val currentApikey = apikeyMap["currentApikey"]

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null && currentUserObj.getApikey() == currentApikey) {
                // Generate a new key
                val updatedUserObj = userRepository?.findById(currentUserObj.getId())
                if (updatedUserObj != null && updatedUserObj.isPresent) {
                    var updatedApikey = TextUtils.generateUUID(currentUserObj.getUsername(),System.currentTimeMillis().toString(),"",0.0,0,"","API key generated from UserController").toString()
                    var foundDuplicate = true

                    // Ensure no dupes
                    while (foundDuplicate) {
                        val apiKeyCheck = userRepository?.findByApikey(updatedApikey)
                        if (updatedApikey == currentApikey || (apiKeyCheck != null && apiKeyCheck.getId() > 0)) {
                            updatedApikey = TextUtils.generateUUID(currentUserObj.getUsername(),System.currentTimeMillis().toString(),"",0.0,0,"","API key generated from UserController").toString()
                        } else {
                            foundDuplicate = false
                        }
                    }

                    updatedUserObj.get().setApikey(updatedApikey)
                    userRepository?.save(updatedUserObj.get())

                    request.session.setAttribute("CurrentUser", updatedUserObj.get())
                    model["currentUser"] = updatedUserObj.get()

                    var baseUrlBuilder = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
                    if (request.scheme == "https") {
                        baseUrlBuilder = baseUrlBuilder.scheme("https")
                    }
                    val baseUrl = baseUrlBuilder.build().toUriString()
                    response["rssFeedLink"] = "$baseUrl/${updatedUserObj.get().getApikey()}/rss"
                    response["atomFeedLink"] = "$baseUrl/${updatedUserObj.get().getApikey()}/atom"

                    response["updatedApikey"] = updatedApikey
                    response["msg"] = ""
                    response["status"] = ApiResponse.SUCCESS.status
                    response["message"] = ""
                }
            }
        }

        val module = "apikey"
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/users/update/language"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun postWebUpdateLanguage(model: Model, request: HttpServletRequest, @RequestBody requestBody: JsonNode): String {
        val languageMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        val response = mutableMapOf<String, Any?>()
        response["msg"] = "Could not update language"
        response["message"] = "Could not update language"
        response["status"] = ApiResponse.FAIL.status
        response["updatedLanguage"] = ""

        if (languageMap.containsKey("language")) {
            val language = languageMap["language"]

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null && (language == "en" || language == "ja")) {
                currentUserObj.setLanguage(language)
                userRepository?.save(currentUserObj)
                response["msg"] = "Updated language"
                response["message"] = "Updated language"
                response["status"] = ApiResponse.SUCCESS.status
                response["updatedLanguage"] = language
            }
        }

        val module = "language"
        response["activePage"] = module
        response["activeSidebar"] = module
        response["titleDescriptor"] = TextUtils.capitalized(module)

        return mapper.writeValueAsString(response)
    }

    @GetMapping("/users/register")
    fun getRegisterUser(model: Model, request: HttpServletRequest): String {
        val module = "register"

        if ((model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) && model.getAttribute("agentName") != "Safari") {
            return "redirect:/timeline"
        } else if ((model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) && model.getAttribute("agentName") == "Safari") {
            return "redirect:/recent"
        } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
            return "redirect:/albums"
        } else if (TextUtils.checkValidRememberMeToken(request.getHeader("Cookie"), rememberMeKey.toString(), userRepository) != null) {
            return "redirect:/users/login"
        } else {
            model["message"] = ""

            val userCount = userRepository?.count()
            if ((userCount != null) && (userCount.toInt() == 0)) {
                model["message"] = "Register as a super admin"
            }
            model["msg"] = ""
            model["status"] = ApiResponse.SUCCESS.status
            model["activePage"] = module
            model["activeSidebar"] = module
            model["titleDescriptor"] = TextUtils.capitalized(module)
            return module
        }
    }

    @RequestMapping(value = ["/users/register"], method = [RequestMethod.POST])
    fun registerUser(model: Model, request: HttpServletRequest): String {
        if ((model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) && model.getAttribute("agentName") != "Safari") {
            return "redirect:/timeline"
        } else if ((model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) && model.getAttribute("agentName") == "Safari") {
            return "redirect:/taken"
        } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
            return "redirect:/albums"
        } else {
            var newUser: User? = null
            var userCount: Long? = null

            model["user"] = User()
            model["message"] = ""

            val module = "register"
            model["activePage"] = module
            model["activeSidebar"] = module
            model["titleDescriptor"] = TextUtils.capitalized(module)

            if (request.getParameter("username") != null && request.getParameter("password") != null) {
                val userName = request.getParameter("username").toString()
                val passWord = request.getParameter("password").toString()

                if (userName.length < 4) {
                    model["message"] = "Username must be at least 4 characters"
                    return module
                }

                if (passWord.length < 6) {
                    model["message"] = "Password must be at least 6 characters"
                    return module
                }

                newUser = User()
                newUser.setUsername(userName)
                newUser.setPassword(passWord)

                userCount = userRepository?.count()

                val users: List<User?> = userRepository?.findAll() as List<User?>

                logger.log(Level.INFO, "New user: $newUser")

                for (user in users) {
                    if (user != null) {
                        if (user.getUsername()?.lowercase() == newUser.getUsername()?.lowercase()) {
                            logger.log(Level.INFO, "Already registered user: $newUser")
                            model["message"] = "Could not register user"
                            return module
                        }
                    }
                }
            }

            if (newUser != null) {
                val encodedPassword: String = bcrypt.encode(newUser.getPassword())
                newUser.setPassword(encodedPassword)
                newUser.setCreatedAt(getCurrentTimestamp())
                newUser.setModifiedAt(getCurrentTimestamp())
                newUser.setApikey(
                    TextUtils.generateUUID(
                        newUser.getUsername(),
                        System.currentTimeMillis().toString(),
                        "",
                        0.0,
                        0,
                        "",
                        "API key generated for new user from UserController"
                    ).toString()
                )

                // Create sideshow entry
                val slideshowAlbum = SlideshowAlbum()
                slideshowAlbum.setUserId(newUser.getId())
                slideshowAlbum.setAlbums("all")
                slideshowAlbumRepository?.save(slideshowAlbum)

                if ((userCount != null) && (userCount.toInt() == 0)) {
                    newUser.setAuthority("ROLE_SUPER")
                    newUser.setIsAuthorized(true)
                    userRepository?.save(newUser)
                    return "redirect:/users/login?msg=regsuccess"
                } else {
                    newUser.setAuthority("ROLE_USER")
                    userRepository?.save(newUser)

                    val admins = userRepository?.findAllAdmins()

                    if (admins != null) {
                        val notificationObjList = mutableListOf<Notification>()
                        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                        for (admin in admins) {
                            val notificationObj = Notification()
                            notificationObj.setUserId(admin.getId())
                            notificationObj.setCreatedAt(getCurrentTimestamp())
                            notificationObj.setModifiedAt(getCurrentTimestamp())
                            notificationObj.setRead(false)
                            var message =
                                newUser.getUsername() + " registered at " + sdtf.format(Date()) + " and is pending approval."
                            if (admin.getAuthority() == superRole) {
                                message =
                                    "<a href='/settings/users' target='_blank'>" + newUser.getUsername() + "</a> registered at " + sdtf.format(
                                        Date()
                                    ) + " and is pending approval."
                            }
                            notificationObj.setMessage(message)
                            notificationObjList.add(notificationObj)
                        }
                        if (notificationObjList.isNotEmpty()) {
                            notificationRepository.saveAll(notificationObjList)
                        }
                    }

                    return "redirect:/users/login?msg=regpending"
                }
            }

            model["msg"] = ""
            model["status"] = ApiResponse.FAIL.status
            model["message"] = "Something went wrong"
            return module
        }
    }

    @GetMapping("/users/login")
    fun getLoginUser(model: Model, session: HttpSession, @RequestParam(name="error",required=false) error: String?, @RequestParam(name="msg",required=false) message: String?): String {
        val module = "login"

        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + relativeSidecarDir

        var sidecarSize = FileUtils.sidecarDiskUsed(sidecarDir)
        val sidecarSizeUpdate = settingsRepository?.findFirstByOrderByIdAsc()
        sidecarSizeUpdate?.setSidecarSizeK(sidecarSize)
        settingsRepository?.save(sidecarSizeUpdate!!)

        val userCount = userRepository?.count()
        if ((userCount != null) && (userCount.toInt() == 0)) {
            return "redirect:/users/register"
        } else if ((model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) && model.getAttribute("agentName") != "Safari") {
            return "redirect:/timeline"
        } else if ((model.getAttribute("authority").toString() == model.getAttribute("adminRole") || model.getAttribute("authority").toString() == model.getAttribute("superRole")) && model.getAttribute("agentName") == "Safari") {
            return "redirect:/taken"
        } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
            return "redirect:/albums"
        } else {
            model["user"] = User()
            model["message"] = ""
            if (error == "401") {
                model["message"] = "Login failed"
            } else if (message == "regsuccess") {
                model["message"] = "Registration successful. Please login."
            } else if (message == "regpending") {
                model["message"] = "Registration pending."
            } else if (message == "loginfail") {
                model["message"] = "Login failed"
            }
            model["msg"] = ""
            model["status"] = ApiResponse.FAIL.status
            model["activePage"] = module
            model["activeSidebar"] = module
            model["titleDescriptor"] = TextUtils.capitalized(module)
            return module
        }
    }

    @RequestMapping(value = ["/users/darkmode"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun toggleDarkmode(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String? {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})

        resp["status"] = ApiResponse.FAIL.status
        resp["msg"] = messageSource?.getMessage("main.toast.topnav.darkmode.message.nottoggled", null, locale)

        if (userMap.containsKey("darkMode")) {
            val darkMode = userMap["darkMode"].toBoolean()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                currentUserObj.setDarkMode(darkMode)
                userRepository?.save(currentUserObj)
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = messageSource?.getMessage("main.toast.topnav.darkmode.message.toggled", null, locale)
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/users/slideshowinterval"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun setSlideshowInterval(model: Model, @RequestBody requestBody: JsonNode): String? {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})

        resp["status"] = ApiResponse.FAIL.status
        resp["msg"] = ""

        if (userMap.containsKey("slideshowInterval") && userMap["slideshowInterval"] != null && userMap["slideshowInterval"]!!.toInt() > 0) {
            val slideshowInterval = userMap["slideshowInterval"]!!.toInt()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                currentUserObj.setSlideshowInterval(slideshowInterval)
                userRepository?.save(currentUserObj)
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = ""
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/users/autoplayvideo"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun toggleAutoplayVideo(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String? {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})

        resp["status"] = ApiResponse.FAIL.status
        resp["msg"] = messageSource?.getMessage("main.toast.topnav.autoplay.message.nottoggled", null, locale)

        if (userMap.containsKey("autoplayVideo")) {
            val autoplayVideo = userMap["autoplayVideo"].toBoolean()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                currentUserObj.setAutoplayVideo(autoplayVideo)
                userRepository?.save(currentUserObj)
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = messageSource?.getMessage("main.toast.topnav.autoplay.message.toggled", null, locale)
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/users/showplacename"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun toggleShowPlacename(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String? {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})

        resp["status"] = ApiResponse.FAIL.status
        resp["msg"] = messageSource?.getMessage("main.toast.topnav.placename.message.nottoggled", null, locale)

        if (userMap.containsKey("showPlacename")) {
            val showPlacename = userMap["showPlacename"].toBoolean()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                currentUserObj.setShowPlacename(showPlacename)
                userRepository?.save(currentUserObj)
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = messageSource?.getMessage("main.toast.topnav.placename.message.toggled", null, locale)
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/users/shownotificationalerts"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun toggleShowNotificationAlerts(model: Model, @RequestBody requestBody: JsonNode, locale: Locale): String? {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})

        resp["status"] = ApiResponse.FAIL.status
        resp["msg"] = messageSource?.getMessage("main.toast.topnav.notifalert.message.nottoggled", null, locale)

        if (userMap.containsKey("notificationAlerts")) {
            val notificationAlerts = userMap["notificationAlerts"].toBoolean()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                currentUserObj.setNotificationAlerts(notificationAlerts)
                userRepository?.save(currentUserObj)
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = messageSource?.getMessage("main.toast.topnav.notifalert.message.toggled", null, locale)
            }
        }

        return mapper.writeValueAsString(resp)
    }

    private fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "resetPasswordUser",
            summary = "Update users password with a chosen or generated password from a valid user ID's.",
        )
    )
    @Secured("ROLE_SUPER")
    @RequestMapping(value = ["/user/update/password", "/api/v1/user/update/password"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun resetPasswordUser(@RequestBody requestBody: JsonNode): String? {
        val response = mutableMapOf<String, Any?>()
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (userMap.containsKey("userId")) {
            val userIdRequest = userMap["userId"].toString().toInt()

            val userObjOpt = userRepository?.findById(userIdRequest)
            if (userObjOpt != null) {
                val userObj = userObjOpt.get()
                userObj.setModifiedAt(getCurrentTimestamp())
                if (userMap.containsKey("password") && userMap["password"].toString().isNotBlank()) {
                    userObj.setPassword(bcrypt.encode(userMap["password"].toString()))
                } else {
                    val generatedPassword = getRandomString(8)
                    userObj.setPassword(bcrypt.encode(generatedPassword))
                    response["password"] = generatedPassword
                }
                userObj.setModifiedAt(getCurrentTimestamp())

                userRepository?.save(userObj)
                response["msg"] = "Success!"
                response["status"] = ApiResponse.SUCCESS.status
                return mapper.writeValueAsString(response)
            }
        }

        response["msg"] = "Could not save"
        response["status"] = ApiResponse.FAIL.status
        return mapper.writeValueAsString(response)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "postUpdateUsersAuthorized",
            summary = "Update users authorized status from a list of valid user ID's.",
        )
    )
    @RequestMapping(value = ["/api/v1/users/update/authorized", "/users/update/authorized"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER")
    fun postUpdateUsersAuthorized(@RequestBody requestBody: JsonNode): String {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val response = mutableMapOf<String, Any?>()
        response["userIds"] = mutableListOf<String>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""
        val usersUpdated = mutableListOf<Int>()

        if (userMap.containsKey("userIds") && userMap.containsKey("authorized")) {
            val userIds: Array<String>? = mapper.readValue(userMap["userIds"].toString(), Array<String>::class.java)
            val authorized: Boolean = userMap["authorized"].toString().toBoolean()
            val usersArray = mutableListOf<User>()

            for (userIdStr in userIds!!) {
                val userId = userIdStr.toInt()

                // Delete profile picture
                val user = userRepository?.findById(userId)
                if (user != null && user.isPresent) {
                    user.get().setModifiedAt(getCurrentTimestamp())
                    user.get().setIsAuthorized(authorized)
                    usersArray.add(user.get())
                    usersUpdated.add(userId)
                }
            }

            if (usersArray.isNotEmpty()) {
                userRepository?.saveAll(usersArray)

                response["userIds"] = usersUpdated

                response["status"] = ApiResponse.SUCCESS.status
                response["msg"] = "Successfully updated authorized"
                logger.log(Level.INFO, "Updated authorized for user's ${usersUpdated.joinToString(", ")}")
            } else {
                response["status"] = ApiResponse.FAIL.status
                response["msg"] = "Failed updating authorized"
            }
        } else {
            response["msg"] = "No user IDs detected"
        }

        return mapper.writeValueAsString(response)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "postUpdateUsersRole",
            summary = "Update users roles from a list of valid user ID's.",
        )
    )
    @RequestMapping(value = ["/api/v1/users/update/role", "/users/update/role"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER")
    fun postUpdateUsersRole(@RequestBody requestBody: JsonNode): String {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val response = mutableMapOf<String, Any?>()
        response["userIds"] = mutableListOf<String>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""
        val usersUpdated = mutableListOf<Int>()

        if (userMap.containsKey("userIds") && userMap.containsKey("role") &&
            (userMap["role"].toString().lowercase().contains("super") || userMap["role"].toString().lowercase().contains("admin") || userMap["role"].toString().lowercase().contains("user"))) {
            val userIds: Array<String>? = mapper.readValue(userMap["userIds"].toString(), Array<String>::class.java)
            var role = "ROLE_${userMap["role"].toString().uppercase()}"
            if (userMap["role"].toString().startsWith("ROLE_")) {
                role = userMap["role"].toString().uppercase()
            }
            val usersArray = mutableListOf<User>()

            for (userIdStr in userIds!!) {
                val userId = userIdStr.toInt()

                // Delete profile picture
                val user = userRepository?.findById(userId)
                if (user != null && user.isPresent) {
                    user.get().setAuthority(role)
                    user.get().setModifiedAt(getCurrentTimestamp())
                    usersArray.add(user.get())
                    usersUpdated.add(userId)
                }
            }

            if (usersArray.isNotEmpty()) {
                userRepository?.saveAll(usersArray)

                response["userIds"] = usersUpdated

                response["status"] = ApiResponse.SUCCESS.status
                response["msg"] = "Successfully updated roles"
                logger.log(Level.INFO, "Updated roles for user's ${usersUpdated.joinToString(", ")}")
            } else {
                response["status"] = ApiResponse.FAIL.status
                response["msg"] = "Failed updating roles"
            }
        } else {
            response["msg"] = "No user IDs detected"
        }

        return mapper.writeValueAsString(response)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "postDeleteUsers",
            summary = "Delete users from a list of valid user ID's.",
        )
    )
    @RequestMapping(value = ["/api/v1/users/delete", "/users/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @Secured("ROLE_SUPER")
    fun postDeleteUsers(@RequestBody requestBody: JsonNode): String {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val response = mutableMapOf<String, Any?>()
        response["userIds"] = mutableListOf<String>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""
        val usersDeleted = mutableListOf<Int>()

        if (userMap.containsKey("userIds")) {
            val userIds: Array<String>? = mapper.readValue(userMap["userIds"].toString(), Array<String>::class.java)
            for (userIdStr in userIds!!) {
                val userId = userIdStr.toInt()

                // Delete profile picture
                val user = userRepository?.findById(userId)
                if (user != null && user.isPresent) {
                    val profileImage =
                        if (user.get().getProfile() == null) "" else user.get().getProfile()!!.replace("/api/v1/profile/", "")
                    if (profileImage.isNotEmpty()) {
                        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                        val sidecarDir = rootPath + relativeSidecarDir
                        val profileDirectory = sidecarDir.dropLast(1) + "/profile"
                        val profileFileStr = "$profileDirectory/$profileImage"
                        if (File(profileFileStr).exists()) {
                            File(profileFileStr).delete()
                        }
                    }

                    usersDeleted.add(userId)
                }

                userRepository?.deleteById(userId)
                userAlbumRepository?.deleteByUserId(userId)
                favoriteRepository?.deleteByUserId(userId)
                commentRepository?.deleteByUserId(userId)
                slideshowAlbumRepository?.deleteByUserId(userId)
            }

            response["userIds"] = usersDeleted

            if (usersDeleted.count() > 0) {
                response["status"] = ApiResponse.SUCCESS.status
                response["msg"] = "Successfully removed user ID's"
                logger.log(Level.INFO, "Deleted user's ${usersDeleted.joinToString(", ")}")
            } else {
                response["status"] = ApiResponse.FAIL.status
                response["msg"] = "Failed removing users"
            }
        } else {
            response["msg"] = "No user IDs detected"
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/api/v1/users/account/delete", "/users/account/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun postDeleteUser(model: Model, @RequestBody requestBody: JsonNode): String {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val response = mutableMapOf<String, Any?>()
        response["userId"] = mutableListOf<String>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""
        val currentUserObj = model.getAttribute("currentUser") as User?

        if (userMap.containsKey("userId") && currentUserObj != null && currentUserObj.getId() == userMap["userId"].toString().toInt()) {
            val userId = currentUserObj.getId()

            // Delete profile picture
            val user = userRepository?.findById(userId)
            var username = ""
            if (user != null && user.isPresent) {
                username = user.get().getUsername().toString()
                val profileImage =
                    if (user.get().getProfile() == null) "" else user.get().getProfile()!!.replace("/api/v1/profile/", "")
                if (profileImage.isNotEmpty()) {
                    val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                    val sidecarDir = rootPath + relativeSidecarDir
                    val profileDirectory = sidecarDir.dropLast(1) + "/profile"
                    val profileFileStr = "$profileDirectory/$profileImage"
                    if (File(profileFileStr).exists()) {
                        File(profileFileStr).delete()
                    }
                }
            }

            userRepository?.deleteById(userId)
            userAlbumRepository?.deleteByUserId(userId)
            favoriteRepository?.deleteByUserId(userId)
            commentRepository?.deleteByUserId(userId)

            val superAdmins = userRepository?.findAllByAuthorityEquals(superRole.toString())

            if (superAdmins != null) {
                val notificationObjList = mutableListOf<Notification>()
                val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                for (superAdmin in superAdmins) {
                    val notificationObj = Notification()
                    notificationObj.setUserId(superAdmin.getId())
                    notificationObj.setCreatedAt(getCurrentTimestamp())
                    notificationObj.setModifiedAt(getCurrentTimestamp())
                    notificationObj.setRead(false)
                    notificationObj.setMessage("$username deleted their account at "+sdtf.format(Date())+".")
                    notificationObjList.add(notificationObj)
                }
                if (notificationObjList.isNotEmpty()) {
                    notificationRepository.saveAll(notificationObjList)
                }
            }

            response["status"] = ApiResponse.SUCCESS.status
            response["msg"] = "Successfully deleted account"
            logger.log(Level.INFO, "User ${currentUserObj.getId()} - ${currentUserObj.getUsername()} deleted")
        } else {
            response["msg"] = "No user ID detected"
        }

        return mapper.writeValueAsString(response)
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getMyUserInfo",
            summary = "See your user information including your user ID.",
            description = "<strong>See your user information including your user ID.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/user/self\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"id\": &lt;user_id&gt;,\n" +
                    "    \"username\": \"&lt;username&gt;\",\n" +
                    "    \"authority\": \"&lt;role&gt;\",\n" +
                    "    \"apikey\": \"&lt;api_key&gt;\",\n" +
                    "    \"isAuthorized\": &lt;is_authorized&gt;,\n" +
                    "    \"darkMode\": &lt;dark_mode&gt;\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>user.id</td><td>int</td><td>Your user ID</td></tr>" +
                    "<tr><td>user.username</td><td>string</td><td>Your username</td></tr>" +
                    "<tr><td>user.authority</td><td>string</td><td>One of ROLE_SUPER, ROLE_ADMIN or ROLE_USER</td></tr>" +
                    "<tr><td>user.apikey</td><td>string</td><td>Your service API key</td></tr>" +
                    "<tr><td>user.isAuthorized</td><td>boolean</td><td>Authorized to access Shashin flag</td></tr>" +
                    "<tr><td>user.darkMode</td><td>boolean</td><td>Flag of whether you have dark enabled or not</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/api/v1/user/self"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    fun getMyUserInfo(model: Model): String {
        val response: JsonNode?

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response = mapper.readTree(currentUserObj.toString())

            (response as ObjectNode).put("status", ApiResponse.SUCCESS.status)
            response.put("msg", "")
        } else {
            response = mapper.readTree(User().toString())

            (response as ObjectNode).put("status", ApiResponse.FAIL.status)
            response.put("msg", "")
            logger.log(Level.INFO, "Could not access user info")
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/api/v1/user/info/{userId}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER")
    fun getUserInfoById(model: Model, @PathVariable userId: Int): String {
        val response: JsonNode?

        val currentUserObj = model.getAttribute("currentUser") as User?
        val userObj = userRepository?.findById(userId)?.orElse(null)

        if (currentUserObj != null && userObj != null) {
            response = mapper.readTree(userObj.toString())
        } else {
            response = mapper.readTree(User().toString())
            logger.log(Level.INFO, "Could not access user info")
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/api/v1/users/info/authorized/{authorized}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER")
    fun getUsersInfoByAuthorized(model: Model, @PathVariable authorized: Boolean): String {
        val response: JsonNode?

        val usersObj: MutableIterable<User?>? = userRepository?.findAllByAuthorized(authorized)

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && usersObj != null) {
            response = mapper.readTree(usersObj.toString())
        } else {
            response = mapper.readTree(mutableListOf<User>().toString())
            logger.log(Level.INFO, "Could not access users info")
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/api/v1/users/info/authority/{authority}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER")
    fun getUsersInfoByAuthority(model: Model, @PathVariable authority: String): String {
        val response: JsonNode?

        var authorityStr = "user"
        if (authority == "user") {
            authorityStr = "ROLE_USER"
        } else if (authority == "admin") {
            authorityStr = "ROLE_ADMIN"
        } else if (authority == "super") {
            authorityStr = "ROLE_SUPER"
        }

        val usersObj: MutableIterable<User?>? = userRepository?.findAllByAuthorityEquals(authorityStr)

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && usersObj != null) {
            response = mapper.readTree(usersObj.toString())
        } else {
            response = mapper.readTree(mutableListOf<User>().toString())
            logger.log(Level.INFO, "Could not access users info")
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/api/v1/users/info"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER")
    fun getUsersInfo(model: Model): String {
        val response: JsonNode?

        val usersObj: MutableIterable<User?>? = userRepository?.findAll(Sort.by(Sort.Direction.DESC, "id"))

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && usersObj != null) {
            response = mapper.readTree(usersObj.toString())
        } else {
            response = mapper.readTree(mutableListOf<User>().toString())
            logger.log(Level.INFO, "Could not access users info")
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/api/v1/users/info/{page}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_SUPER")
    fun getUsersInfoPaged(model: Model, @PathVariable page: Int): String {
        val response = mutableMapOf<String, Any?>()
        val usersObj: MutableIterable<User?>?

        val size: Int = model.getAttribute("queryLimit").toString().toInt()
        val pageValue = page * size

        response["page"] = page
        response["size"] = size

        usersObj = userRepository?.findAllByOffsetAndLimit(pageValue, size)

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && usersObj != null) {
            // toString avoids printing password
            response["userList"] = mapper.readTree(usersObj.toString())
        } else {
            response["userList"] = mapper.readTree(mutableListOf<User>().toString())
            logger.log(Level.INFO, "Could not access users info")
        }

        return mapper.writeValueAsString(response)
    }
}