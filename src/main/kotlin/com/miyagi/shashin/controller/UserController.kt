package com.miyagi.shashin.controller

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Level
import java.util.logging.Logger
import javax.validation.Valid


@RestController
class UserController {

    enum class Status {
        SUCCESS, USER_ALREADY_EXISTS, FAILURE
    }

    private var logger: Logger = Logger.getLogger(UserController::class.simpleName)
    @Autowired
    var userRepository: UserRepository? = null
    @PostMapping("/users/register")
    fun registerUser(@RequestBody newUser: @Valid User?): Status {
        val users: List<User?> = userRepository?.findAll() as List<User?>
        logger.log(Level.INFO, "New user: " + newUser.toString())

        for (user in users) {
            logger.log(Level.INFO, "Registered user: " + newUser.toString())
            if (user != null && newUser != null) {
                if (user.sameAs(newUser)) {
                    return Status.USER_ALREADY_EXISTS
                }
            }
        }
        userRepository!!.save(newUser!!)
        return Status.SUCCESS
    }

    @PostMapping("/users/login")
    fun loginUser(@RequestBody user: @Valid User?): Status {
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