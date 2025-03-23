package com.miyagi.shashin.component

import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.logging.Level
import java.util.logging.Logger
import java.time.ZoneId
import java.util.*
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import nl.basjes.parse.useragent.UserAgentAnalyzer


@Component
class AuthFailureHandler : SimpleUrlAuthenticationFailureHandler() {
    private val redirectStrategy = DefaultRedirectStrategy()

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var notificationRepository: NotificationRepository? = null

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?
    ) {
        val logger: Logger = Logger.getLogger(AuthFailureHandler::class.simpleName)

        val lastUserName: String = request?.getParameter("username") ?: ""

        val admins = userRepository?.findAllAdmins()
        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())

        var lastUser: User? = null
        try {
            lastUser = userRepository?.findByUsername(lastUserName)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "userRepository?.findByUsername error: ${e.message}")
        }

        var message = "Unknown user '$lastUserName' attempted login"
        if (lastUser != null) {
            message = "User '$lastUserName' failed login"
        }

        // Capture UA data
        val userAgent = request?.getHeader("User-Agent")
        val uaa = UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withCache(10000)
            .build()
        val agentObj = uaa.parse(userAgent)

        // eg. mobile
        val osClass = if (agentObj.getValue("OperatingSystemClass") == "??") "" else TextUtils.capitalized(agentObj.getValue("OperatingSystemClass").lowercase()) + " "
        // eg. android
        val osName = if (agentObj.getValue("OperatingSystemName") == "??") "" else TextUtils.capitalized(agentObj.getValue("OperatingSystemName").lowercase()) + " "
        // eg. 13
        val osVersion = if (agentObj.getValue("OperatingSystemVersion") == "??") "" else TextUtils.capitalized(agentObj.getValue("OperatingSystemVersion").lowercase()) + " "
        // eg. chrome
        val agentName = if (agentObj.getValue("AgentName") == "??") "" else TextUtils.capitalized(agentObj.getValue("AgentName").lowercase()) + " "
        // eg. 114
        val agentVersion = if (agentObj.getValue("AgentVersion") == "??") "" else TextUtils.capitalized(agentObj.getValue("AgentVersion").lowercase()) + " "

        val clientIP = TextUtils.getClientIp(request)

        var ipString = "from IP $clientIP "
        if (!TextUtils.isLocalIp(clientIP)) {
            ipString = "from IP <a href='https://ipgeolocation.io/ip-location/$clientIP' target='_blank'>$clientIP</a> "
        }

        message += " ${ipString}using device $osName$osVersion$osClass and browser $agentName$agentVersion at ${sdtf.format(Date())}"

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