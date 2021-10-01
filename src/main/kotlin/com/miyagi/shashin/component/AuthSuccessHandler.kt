package com.miyagi.shashin.component

import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.stereotype.Component
import org.springframework.ui.Model
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


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
                    val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val now = LocalDateTime.now()
                    if (currentAuthority == userRole && user.getIsAllowed() == false) {
                        user.setModifiedAt(dtf.format(now))
                        user.setLoggedIn(false)
                        userRepository?.save(user)
                        SecurityContextLogoutHandler().logout(request, response, authentication)
                        SecurityContextHolder.getContext().authentication = null
                        redirectStrategy.sendRedirect(request, response, "/users/login?msg=loginfail")
                        isAllowed = false
                    } else {
                        user.setModifiedAt(dtf.format(now))
                        user.setLoggedIn(true)
                        userRepository?.save(user)
                    }
                }

                if (isAllowed) {
                    notifyLogin(user,currentAuthority)
                    if (currentAuthority == adminRole) {
                        redirectStrategy.sendRedirect(request, response, "/timeline/mediatype/all")
                    } else {
                        redirectStrategy.sendRedirect(request, response, "/albums")
                    }
                }
            }
        }
    }

    private fun notifyLogin(currentUserObj: User?, authority: String) {
        val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val now = LocalDateTime.now()

        if (admins != null && currentUserObj != null) {
            val notificationObjList = mutableListOf<Notification>()
            for (admin in admins) {
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(dtf.format(now))
                notificationObj.setModifiedAt(dtf.format(now))
                notificationObj.setRead(false)
                var identity = currentUserObj.getUsername()
                if (admin.getId() == currentUserObj.getId()) {
                    identity = "You "
                }
                notificationObj.setMessage("<a href='/settings/users' target='_blank'>$identity</a> logged in at "+dtf.format(now)+".")
                notificationObjList.add(notificationObj)
            }
            if (notificationObjList.isNotEmpty()) {
                notificationRepository?.saveAll(notificationObjList)
            }
        }
    }
}