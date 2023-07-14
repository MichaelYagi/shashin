package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import io.swagger.v3.oas.annotations.Operation
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.text.StringEscapeUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.SessionStatus
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.ByteArrayInputStream
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.annotation.Resource
import javax.imageio.ImageIO
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.validation.Valid


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

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var persistentLoginsRepository: PersistentLoginsRepository? = null

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Resource(name = "authenticationManager")
    private val authManager: AuthenticationManager? = null

    @GetMapping("/users/update")
    fun getUpdateUser(model: Model): String {
        model["message"] = ""
        model["user"] = User()
        model["alertClass"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            model["user"] = currentUserObj
        }

        val module = "update"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/users/update"], method = [RequestMethod.POST], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun postUpdateUser(model: Model, redirectAttributes: RedirectAttributes, @RequestBody formData: MultiValueMap<String, String>): String {
        model["message"] = "Could not save password"
        model["alertClass"] = "alert-danger"
        if (formData.containsKey("oldpassword") && formData.containsKey("newpassword") && formData.containsKey("newpasswordconfirm")) {
            val oldPassword = StringEscapeUtils.escapeHtml4(java.lang.String.valueOf(formData.getFirst("oldpassword")))
            val newPassword = StringEscapeUtils.escapeHtml4(java.lang.String.valueOf(formData.getFirst("newpassword")))
            val newPasswordConfirm = StringEscapeUtils.escapeHtml4(java.lang.String.valueOf(formData.getFirst("newpasswordconfirm")))

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                if (newPassword == newPasswordConfirm) {
                    if (bcrypt.matches(oldPassword, currentUserObj.getPassword())) {
                        currentUserObj.setModifiedAt(getCurrentTimestamp())
                        currentUserObj.setPassword(bcrypt.encode(newPassword))
                        userRepository?.save(currentUserObj)
                        model["message"] = "Success"
                        model["alertClass"] = "alert-success"
                    }
                }
            }
        }

        val module = "update"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["message"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/users/profile")
    fun getProfile(model: Model): String {
        model["message"] = ""
        model["user"] = User()
        model["alertClass"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            model["user"] = currentUserObj
        }

        val module = "profile"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/users/profile"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_ADMIN","ROLE_USER")
    fun postUpdateProfile(model: Model, redirectAttributes: RedirectAttributes, @RequestBody requestBody: JsonNode): String {
        val base64Map = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        val response = mutableMapOf<String, Any?>()
        response["randomString"] = TextUtils.generateUUID(getCurrentTimestamp())

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

                    val uuidFromUsername = TextUtils.generateUUID(currentUserObj.getUsername())

                    val profileDirectory = sidecarDir.dropLast(1) + "/profile"
                    val profileFileStr = "$profileDirectory/$uuidFromUsername.$extension"
                    if (File(profileFileStr).exists()) {
                        File(profileFileStr).delete()
                    }
                    val profileFile = FileUtils.createFile(profileDirectory, profileFileStr, "Profile")
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

    @GetMapping("/users/apikey")
    fun getApiKey(model: Model): String {
        model["message"] = ""
        model["user"] = User()
        model["alertClass"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            model["user"] = currentUserObj
        }

        val module = "apikey"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/users/apikey/update"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_ADMIN","ROLE_USER")
    fun postWebUpdateApikey(model: Model, redirectAttributes: RedirectAttributes, @RequestBody requestBody: JsonNode): String {
        val apikeyMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        val response = mutableMapOf<String, Any?>()
        response["msg"] = "Could not update API key"
        response["message"] = "Could not update API key"
        response["status"] = ApiResponse.FAIL.status
        response["updatedApikey"] = ""

        if (apikeyMap.containsKey("currentApikey")) {
            val currentApikey = apikeyMap["currentApikey"]

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null && currentUserObj.getApikey() == currentApikey) {
                // Generate a new key
                val updatedUserObj = userRepository?.findById(currentUserObj.getId())
                if (updatedUserObj != null && updatedUserObj.isPresent) {
                    val updatedApikey = TextUtils.generateUUID(currentUserObj.getUsername(),System.currentTimeMillis().toString(),"",0.0,0,"").toString()
                    updatedUserObj.get().setApikey(updatedApikey)
                    userRepository?.save(updatedUserObj.get())

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

    @GetMapping("/users/register")
    fun getRegisterUser(model: Model): String {
        val userCount = userRepository?.count()

        model["user"] = User()
        model["message"] = ""
        if ((userCount != null) && (userCount.toInt() == 0)) {
            model["message"] = "Register as an admin"
        }

        val module = "register"
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/users/register"], method = [RequestMethod.POST])
    fun registerUser(model: Model, @ModelAttribute newUser: @Valid User?): String {
        val userCount = userRepository?.count()

        val users: List<User?> = userRepository?.findAll() as List<User?>

        logger.log(Level.INFO, "New user: " + newUser.toString())

        model["user"] = User()
        model["message"] = ""

        val module = "register"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        for (user in users) {
            if (user != null && newUser != null) {
                if (user.getUsername() == newUser.getUsername()) {
                    logger.log(Level.INFO, "Already registered user: $newUser")
                    model["message"] = "User already exists"
                    return module
                }
            }
        }

        if (newUser != null) {
            val encodedPassword: String = bcrypt.encode(newUser.getPassword())
            newUser.setPassword(encodedPassword)
            newUser.setCreatedAt(getCurrentTimestamp())
            newUser.setModifiedAt(getCurrentTimestamp())
            newUser.setApikey(TextUtils.generateUUID(newUser.getUsername(),System.currentTimeMillis().toString(),"",0.0,0,"").toString())

            if ((userCount != null) && (userCount.toInt() == 0)) {
                newUser.setAuthority("ROLE_ADMIN")
                newUser.setIsAuthorized(true)
                userRepository?.save(newUser)
                return "redirect:/users/login?msg=regsuccess"
            } else {
                newUser.setAuthority("ROLE_USER")
                userRepository?.save(newUser)

                val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
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
                        notificationObj.setMessage("<a href='/settings/users' target='_blank'>"+newUser.getUsername()+"</a> registered at "+sdtf.format(Date())+" and is pending approval.")
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



    @GetMapping("/users/login")
    fun getLoginUser(model: Model, @RequestParam(name="error",required=false) error: String?, @RequestParam(name="msg",required=false) message: String?): String {
        val module = "login"

        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
            return "redirect:/timeline"
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

    @GetMapping("/users/logout")
    fun logUserOut(httpsession: HttpSession, status: SessionStatus, response: HttpServletResponse): String {
        logoutProcedure(httpsession, status, response)
        return "redirect:/users/login"
    }

    private fun logoutProcedure(httpsession: HttpSession, status: SessionStatus, response: HttpServletResponse) {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.name.isNullOrBlank()) {
            val user = userRepository?.findByUsername(authentication.name)

            if (user != null) {
                user.setModifiedAt(getCurrentTimestamp())
                userRepository?.save(user)
                persistentLoginsRepository?.deleteByUsername(user.getUsername()!!)
            }
        }

        status.setComplete()
        httpsession.invalidate()
        SecurityContextHolder.clearContext()

        val cookie = Cookie("remember-me", null) // Not necessary, but saves bandwidth.
        cookie.path = "/"
        cookie.isHttpOnly = true
        cookie.maxAge = 0
        response.addCookie(cookie)
    }

    @RequestMapping(value = ["/users/darkmode"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_ADMIN","ROLE_USER")
    fun toggleDarkmode(model: Model, @RequestBody requestBody: JsonNode): String? {
        val userMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})

        resp["status"] = ApiResponse.FAIL.status
        resp["msg"] = "Darkmode not toggled"

        if (userMap.containsKey("darkMode")) {
            val darkMode = userMap["darkMode"].toBoolean()

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                currentUserObj.setDarkMode(darkMode)
                userRepository?.save(currentUserObj)
                resp["status"] = ApiResponse.SUCCESS.status
                resp["msg"] = "Darkmode toggled"
            }
        }

        return mapper.writeValueAsString(resp)
    }

    @RequestMapping(value = ["/users/delete"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_ADMIN")
    fun deleteUser(model: Model, @ModelAttribute user: @Valid User?): String? {
        resp["status"] = ApiResponse.FAIL.status

        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {

            val users: List<User?> = userRepository?.findAll() as List<User?>
            for (other in users) {
                if (other != null && user != null) {
                    if (other.equals(user)) {
                        userRepository?.delete(user)

                        resp["msg"] = "Deleted user " + user.getUsername()
                        resp["status"] = ApiResponse.SUCCESS.status
                        return mapper.writeValueAsString(resp)
                    }
                }
            }

            resp["msg"] = "Could not delete user"
            if (user != null) {
                resp["msg"] = "Could not delete user " + user.getUsername()
            }
            resp["status"] = ApiResponse.FAIL.status
        }
        return mapper.writeValueAsString(resp)
    }

    private fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getMyUserInfo",
            description = "<strong>See your user information including your user ID.</strong><br>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/users/self\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"message\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"user\": {\n" +
                    "        \"id\": &lt;user_id&gt;,\n" +
                    "        \"username\": \"&lt;username&gt;\",\n" +
                    "        \"authority\": \"&lt;role&gt;\",\n" +
                    "        \"apikey\": \"&lt;api_key&gt;\",\n" +
                    "        \"isAuthorized\": &lt;is_authorized&gt;,\n" +
                    "        \"darkMode\": &lt;dark_mode&gt;\n" +
                    "    }\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>user.id</td><td>int</td><td>Your user ID</td></tr>" +
                    "<tr><td>user.username</td><td>string</td><td>Your username</td></tr>" +
                    "<tr><td>user.authority</td><td>string</td><td>One of \"ROLE_ADMIN\" or \"ROLE_USER\"</td></tr>" +
                    "<tr><td>user.apikey</td><td>string</td><td>Your service API key</td></tr>" +
                    "<tr><td>user.isAuthorized</td><td>boolean</td><td>Authorized to access Shashin flag</td></tr>" +
                    "<tr><td>user.darkMode</td><td>boolean</td><td>Flag of whether you have dark enabled or not</td></tr>" +
                    "</tbody></table>"
        )
    )
    @RequestMapping(value = ["/api/v1/users/self"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_ADMIN","ROLE_USER")
    fun getMyUserInfo(model: Model): String {
        val response = mutableMapOf<String, Any?>()
        response["msg"] = "Could not get user info"
        response["message"] = "Could not get user info"
        response["status"] = ApiResponse.FAIL.status
        response["user"] = User()

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            response["user"] = mapper.readTree(currentUserObj.toString())
            response["msg"] = ""
            response["message"] = ""
            response["status"] = ApiResponse.SUCCESS.status
        }

        return mapper.writeValueAsString(response)
    }
}