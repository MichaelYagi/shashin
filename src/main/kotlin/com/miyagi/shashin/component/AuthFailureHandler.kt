package com.miyagi.shashin.component

import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.logging.Level
import java.util.logging.Logger
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse


@Component
class AuthFailureHandler : SimpleUrlAuthenticationFailureHandler() {
    private val redirectStrategy = DefaultRedirectStrategy()

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var notificationRepository: NotificationRepository? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?
    ) {
        val lastUserName: String = request?.getParameter("username") ?: ""

        val admins = userRepository?.findAllAdmins()
        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())

        var lastUser: User? = null
        try {
            lastUser = userRepository?.findByUsername(lastUserName)
        } catch (_: Exception) {}

        var message = "Unknown user '$lastUserName' attempted login at "+ sdtf.format(Date())+"."
        if (lastUser != null) {
            message = "User '$lastUserName' failed login at " + sdtf.format(Date()) + "."
        }

        val logger: Logger = Logger.getLogger(AuthFailureHandler::class.simpleName)
        logger.log(Level.WARNING, message)

        if (admins != null) {
            val notificationObjList = mutableListOf<Notification>()
            for (admin in admins) {
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(getCurrentTimestamp())
                notificationObj.setModifiedAt(getCurrentTimestamp())
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