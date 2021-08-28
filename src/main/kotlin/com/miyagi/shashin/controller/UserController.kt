package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.SessionStatus
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpSession
import javax.validation.Valid

@Suppress("UNCHECKED_CAST")
@Controller
class UserController {

    private var logger: Logger = Logger.getLogger(UserController::class.simpleName)
    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()
    var bcrypt = BCryptPasswordEncoder()

    @Autowired
    var userRepository: UserRepository? = null

    @GetMapping("/users/update")
    fun getUpdateUser(model: Model): String {
        model["data"] = ""
        model["user"] = ""
        model["message"] = ""
        model["alertClass"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            model["user"] = currentUserObj
        }

        val module = "update"
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
            val oldPassword = java.lang.String.valueOf(formData.getFirst("oldpassword"))
            val newPassword = java.lang.String.valueOf(formData.getFirst("newpassword"))
            val newPasswordConfirm = java.lang.String.valueOf(formData.getFirst("newpasswordconfirm"))

            val currentUserObj = model.getAttribute("currentUser") as User?
            if (currentUserObj != null) {
                if (newPassword == newPasswordConfirm) {
                    if (bcrypt.matches(oldPassword, currentUserObj.getPassword())) {
                        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val now = LocalDateTime.now()
                        currentUserObj.setModifiedAt(dtf.format(now))
                        currentUserObj.setPassword(bcrypt.encode(newPassword))
                        userRepository?.save(currentUserObj)
                        model["message"] = "Success"
                        model["alertClass"] = "alert-success"
                    }
                }
            }
        }

        val module = "update"
        model["data"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/users/register")
    fun getRegisterUser(model: Model): String {
        val userCount = userRepository?.count()

        model["user"] = User()
        model["data"] = ""
        if ((userCount != null) && (userCount.toInt() == 0)) {
            model["data"] = "Register as an admin"
        }

        val module = "register"
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
        model["data"] = ""

        val module = "register"
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        for (user in users) {
            if (user != null && newUser != null) {
                if (user.getUsername() == newUser.getUsername()) {
                    logger.log(Level.INFO, "Already registered user: $newUser")
                    model["data"] = "User already exists"
                    return module
                }
            }
        }

        if (newUser != null) {
            val encodedPassword: String = bcrypt.encode(newUser.getPassword())
            newUser.setPassword(encodedPassword)
            if ((userCount != null) && (userCount.toInt() == 0)) {
                newUser.setAuthority("ROLE_ADMIN")
            } else {
                newUser.setAuthority("ROLE_USER")
            }

            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            newUser.setCreatedAt(dtf.format(now))
            newUser.setModifiedAt(dtf.format(now))
            newUser.setLoggedIn(true)

            userRepository?.save(newUser)

            return "redirect:/users/login?msg=regsuccess"
        }

        model["data"] = "Something went wrong"
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
            model["data"] = ""
            if (error == "401") {
                model["data"] = "Login failed"
            } else if (message == "regsuccess") {
                model["data"] = "Registration succeful. Please login."
            }
            model["activePage"] = module
            model["activeSidebar"] = module
            model["titleDescriptor"] = TextUtils.capitalized(module)
            return module
        }
    }

    @RequestMapping(value = ["/users/login"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun loginUser(@ModelAttribute user: @Valid User?, @RequestParam(name="msg",required=false) message: String?): String? {
        val users: List<User?> = userRepository?.findAll() as List<User?>
        for (other in users) {
            if (other != null && user != null) {
                if (other.equals(user) && bcrypt.matches(user.getPassword(), other.getPassword())) {
                    user.setLoggedIn(true)
                    userRepository?.save(user)

                    resp["msg"] = "Logged in!"
                    resp["status"] = "success"
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        resp["msg"] = "Could not login"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)
    }

    @GetMapping("/users/logout")
    fun logUserOut(httpsession: HttpSession, status: SessionStatus): String {
        val authentication = SecurityContextHolder.getContext().authentication

        if (!authentication.name.isNullOrBlank()) {
            val user = userRepository?.findByUsername(authentication.name)

            if (user != null) {
                val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now()
                user.setLoggedIn(false)
                user.setModifiedAt(dtf.format(now))
                userRepository?.save(user)
            }

            status.setComplete()
            httpsession.invalidate()
            SecurityContextHolder.clearContext()
            return "redirect:/users/login"
        }
        status.setComplete()
        httpsession.invalidate()
        SecurityContextHolder.clearContext()
        return "redirect:/users/login"
    }

    @RequestMapping(value = ["/users/delete"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Secured("ROLE_ADMIN")
    fun deleteUser(model: Model, @ModelAttribute user: @Valid User?): String? {
        resp["status"] = "fail"

        if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {

            val users: List<User?> = userRepository?.findAll() as List<User?>
            for (other in users) {
                if (other != null && user != null) {
                    if (other.equals(user)) {
                        user.setLoggedIn(false)
                        userRepository?.delete(user)

                        resp["msg"] = "Deleted user " + user.getUsername()
                        resp["status"] = "success"
                        return mapper.writeValueAsString(resp)
                    }
                }
            }

            resp["msg"] = "Could not delete user"
            if (user != null) {
                resp["msg"] = "Could not delete user " + user.getUsername()
            }
            resp["status"] = "fail"
        }
        return mapper.writeValueAsString(resp)
    }
}