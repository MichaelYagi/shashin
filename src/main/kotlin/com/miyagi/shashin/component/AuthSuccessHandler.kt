package com.miyagi.shashin.component

import com.miyagi.shashin.controller.SettingsController
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils.Companion.getModifiedCreateTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.logging.Level
import java.util.logging.Logger

@Component
class AuthSuccessHandler : SimpleUrlAuthenticationSuccessHandler() {
    private val redirectStrategy = DefaultRedirectStrategy()

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var notificationRepository: NotificationRepository? = null

    @Throws(IOException::class)
    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?, authentication: Authentication?) {
        val logger: Logger = Logger.getLogger(AuthSuccessHandler::class.simpleName)

        if (authentication != null) {
            var currentAuthority = ""
            for (authority in authentication.authorities) {
                if (authority.authority == adminRole) {
                    currentAuthority = adminRole!!
                    break
                } else if (authority.authority == userRole) {
                    currentAuthority = userRole!!
                    break
                }
            }

            if (currentAuthority != "") {
                var isAllowed = true
                val user = userRepository?.findByUsername(authentication.name)
                if (user != null) {
                    if (currentAuthority == userRole && user.getIsAllowed() == false) {
                        user.setModifiedAt(getModifiedCreateTimestamp())
                        user.setLoggedIn(false)
                        userRepository?.save(user)
                        SecurityContextLogoutHandler().logout(request, response, authentication)
                        SecurityContextHolder.getContext().authentication = null
                        redirectStrategy.sendRedirect(request, response, "/users/login?msg=loginfail")
                        isAllowed = false
                    } else {
                        user.setModifiedAt(getModifiedCreateTimestamp())
                        user.setLoggedIn(true)
                        try {
                            userRepository?.save(user)
                        } catch(e: Exception) {
                            logger.log(Level.SEVERE, "Could not save status for user: " + e.message)
                        }
                    }
                }

                if (isAllowed) {
                    notifyLogin(user,currentAuthority)
                    if (currentAuthority == adminRole) {
                        redirectStrategy.sendRedirect(request, response, "/timeline")
                    } else {
                        redirectStrategy.sendRedirect(request, response, "/albums")
                    }
                }
            }
        }
    }

    private fun notifyLogin(currentUserObj: User?, authority: String) {
        val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
        val sdtf = DateTimeFormatter
            .ofLocalizedTime(FormatStyle.LONG)
            .withZone(ZoneId.systemDefault())
        val now = LocalDateTime.now()

        if (admins != null && currentUserObj != null) {
            val notificationObjList = mutableListOf<Notification>()
            for (admin in admins) {
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(getModifiedCreateTimestamp())
                notificationObj.setModifiedAt(getModifiedCreateTimestamp())
                var identity = "<a href='/settings/users' target='_blank'>"+currentUserObj.getUsername()+"</a>"
                if (admin.getId() == currentUserObj.getId()) {
                    notificationObj.setRead(true)
                    identity = "You "
                } else {
                    notificationObj.setRead(false)
                }
                notificationObj.setMessage("$identity logged in at "+sdtf.format(now)+".")
                notificationObjList.add(notificationObj)
            }
            if (notificationObjList.isNotEmpty()) {
                notificationRepository?.saveAll(notificationObjList)
            }
        }
    }
}