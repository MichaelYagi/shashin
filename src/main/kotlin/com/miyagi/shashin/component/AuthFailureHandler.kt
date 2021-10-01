package com.miyagi.shashin.component

import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class AuthFailureHandler : SimpleUrlAuthenticationFailureHandler() {
    private val redirectStrategy = DefaultRedirectStrategy()

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var notificationRepository: NotificationRepository? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?
    ) {
        val lastUserName: String = request?.getParameter("username") ?: ""

        val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
        val dtf = DateTimeFormatter
            .ofLocalizedTime(FormatStyle.LONG)
            .withZone(ZoneId.systemDefault())

        val now = LocalDateTime.now()

        val message = "$lastUserName login failed at "+ dtf.format(now)+"."
        val logger: Logger = Logger.getLogger(AuthFailureHandler::class.simpleName)
        logger.log(Level.WARNING, message)

        if (admins != null) {
            val notificationObjList = mutableListOf<Notification>()
            for (admin in admins) {
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(dtf.format(now))
                notificationObj.setModifiedAt(dtf.format(now))
                notificationObj.setRead(false)
                notificationObj.setMessage(message)
                notificationObjList.add(notificationObj)
            }
            if (notificationObjList.isNotEmpty()) {
                notificationRepository?.saveAll(notificationObjList)
            }
        }

        redirectStrategy.sendRedirect(request, response, "/users/login?error=401")
    }
}