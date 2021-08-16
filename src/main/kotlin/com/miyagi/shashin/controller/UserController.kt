package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Authority
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.AuthorityRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger
import javax.validation.Valid


@Controller
class UserController {

    enum class Status {
        SUCCESS, USER_ALREADY_EXISTS, FAILURE
    }

    private var logger: Logger = Logger.getLogger(UserController::class.simpleName)
    @Autowired
    var userRepository: UserRepository? = null
    @Autowired
    var authorityRepository: AuthorityRepository? = null
    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

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

    @RequestMapping(value = ["/users/register"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun registerUser(@ModelAttribute newUser: @Valid User?): String? {
        val userCount = userRepository?.count()

        val users: List<User?> = userRepository?.findAll() as List<User?>

        logger.log(Level.INFO, "New user: " + newUser.toString())

        for (user in users) {
            logger.log(Level.INFO, "Already registered user: " + newUser.toString())
            if (user != null && newUser != null) {
                if (user.sameAs(newUser)) {
                    resp["msg"] = "User already exists"
                    resp["status"] = "fail"
                    return mapper.writeValueAsString(resp)
                }
            }
        }

        if (newUser != null) {
            val encodedPassword: String = BCryptPasswordEncoder().encode(newUser.getPassword())
            newUser.setPassword(encodedPassword)
            val authority = Authority()
            if ((userCount != null) && (userCount.toInt() == 0)) {
                authority.setAuthority("ADMIN")
            } else {
                authority.setAuthority("USER")
            }

            authority.setUserId(newUser.getId())
            newUser.setAuthority(authority)
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            newUser.setCreatedAt(dtf.format(now))
            newUser.setModifiedAt(dtf.format(now))

//            authorityRepository?.save(authority)
//            userRepository?.save(newUser)

            resp["msg"] = "Registered!"
            resp["status"] = "success"
            return mapper.writeValueAsString(resp)
        }

        resp["msg"] = "Something went wrong"
        resp["status"] = "fail"
        return mapper.writeValueAsString(resp)

    }

    @GetMapping("/users/login")
    fun getLoginUser(model: Model): String {
        val module = "login"
        model["user"] = User()
        model["data"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @PostMapping("/users/login")
    fun loginUser(@ModelAttribute user: @Valid User?): Status {
        val users: List<User?> = userRepository?.findAll() as List<User?>
        for (other in users) {
            if (other != null && user != null) {
                if (other.sameAs(user)) {
                    user.setLoggedIn(true)
                    userRepository!!.save(user!!)
                    return Status.SUCCESS
                }
            }
        }
        return Status.FAILURE
    }

    @PostMapping("/users/logout")
    fun logUserOut(@RequestBody user: @Valid User?): Status {
        val users: List<User?> = userRepository?.findAll() as List<User?>
        for (other in users) {
            if (other != null && user != null) {
                if (other.sameAs(user)) {
                    user.setLoggedIn(false)
                    userRepository!!.save(user!!)
                    return Status.SUCCESS
                }
            }
        }
        return Status.FAILURE
    }

    @DeleteMapping("/users/delete")
    fun deleteUser(@RequestBody user: @Valid User?): Status {
        val users: List<User?> = userRepository?.findAll() as List<User?>
        for (other in users) {
            if (other != null && user != null) {
                if (other.sameAs(user)) {
                    user.setLoggedIn(false)
                    userRepository!!.delete(user)
                    return Status.SUCCESS
                }
            }
        }
        return Status.FAILURE
    }

    @DeleteMapping("/users/deleteall")
    fun deleteUsers(): Status {
        userRepository!!.deleteAll()
        return Status.SUCCESS
    }
}