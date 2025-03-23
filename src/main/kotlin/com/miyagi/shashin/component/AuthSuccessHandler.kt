package com.miyagi.shashin.component

import com.miyagi.shashin.configuration.MultiSecurityConfig
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.User
import com.miyagi.shashin.model.Useragent
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.service.CustomUserDetailsService
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.RedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


@Component
class AuthSuccessHandler : SimpleUrlAuthenticationSuccessHandler() {

    private val redirectStrategy = DefaultRedirectStrategy()

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    @Value("\${app.sidecar.path}")
    private var relativeSidecarDir: String? = null

    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var notificationRepository: NotificationRepository? = null

    @Autowired
    var customUserDetailsService: CustomUserDetailsService? = null

    @Autowired
    var useragentRepository: UseragentRepository? = null

    private var persistentTokenRepository: PersistentTokenRepository? = null

    private var profile: String? = null

    fun setProfile(profile: String?): AuthSuccessHandler {
        this.profile = profile

        return this
    }

    @Throws(IOException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        authentication: Authentication?
    ) {
        handle(request, response, authentication)
        clearAuthenticationAttributes(request)
    }

    @Throws(IOException::class)
    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?, authentication: Authentication?) {
        val logger: Logger = Logger.getLogger(AuthSuccessHandler::class.simpleName)

        var userId = 0

        var uriPath = request!!.session.getAttribute("ShashinReferer")
        uriPath = if (uriPath == null || !validSubPaths(uriPath.toString())) {
            ""
        } else {
            uriPath.toString()
        }
        request.session.removeAttribute("ShashinReferer")

        if (authentication != null) {
            var currentAuthority = ""
            for (authority in authentication.authorities) {
                if (authority.authority == adminRole) {
                    currentAuthority = adminRole!!
                    break
                } else if (authority.authority == superRole) {
                    currentAuthority = superRole!!
                    break
                } else if (authority.authority == userRole) {
                    currentAuthority = userRole!!
                    break
                }
            }

            if (currentAuthority != "") {
                var isAuthorized = true
                val user = userRepository?.findByUsername(authentication.name)
                if (user != null && user.getId() > 0) {
                    request.session.setAttribute("CurrentUser",user)

                    userId = user.getId()
                    if (user.getIsAuthorized() == false) {
                        user.setModifiedAt(getCurrentTimestamp())
                        userRepository?.save(user)
                        SecurityContextLogoutHandler().logout(request, response, authentication)
                        SecurityContextHolder.getContext().authentication = null

                        val admins = userRepository?.findAllAdmins()
                        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
                        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
                        val message = "User '${user.getUsername()}' failed login at "+ sdtf.format(Date())+"."
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

                        redirectStrategy.sendRedirect(request, response, "/users/login?msg=loginfail")
                        isAuthorized = false
                    } else {
                        user.setModifiedAt(getCurrentTimestamp())
                        try {
                            userRepository?.save(user)
                        } catch(e: Exception) {
                            logger.log(Level.SEVERE, "Could not save status for user: " + e.message)
                        }
                    }
                }

                if (isAuthorized) {
                    if (user != null && user.getId() > 0) {
                        if (this.profile != "test" && this.persistentTokenRepository != null) {
                            val rememberMeServices =
                                PersistentTokenBasedRememberMeServices(
                                    rememberMeKey,
                                    customUserDetailsService,
                                    this.persistentTokenRepository
                                )

                            if (request.getParameter(rememberMeServices.parameter) != "on") {
                                rememberMeServices.setAlwaysRemember(true)
                                rememberMeServices.setCookieName("remember-me")
                                // 1 Hour
                                rememberMeServices.setTokenValiditySeconds(3600)
                                rememberMeServices.loginSuccess(request, response, authentication)
                            }
                        }

                        // Capture UA data
                        val userAgent = request.getHeader("User-Agent")
                        val uaa = UserAgentAnalyzer
                            .newBuilder()
                            .hideMatcherLoadStats()
                            .withCache(10000)
                            .build()
                        val agentObj = uaa.parse(userAgent)

                        // eg. phone
                        val deviceClass = if (agentObj.getValue("DeviceClass") == "??") null else agentObj.getValue("DeviceClass").lowercase()
                        // eg. mobile
                        val osClass = if (agentObj.getValue("OperatingSystemClass") == "??") null else agentObj.getValue("OperatingSystemClass").lowercase()
                        // eg. android
                        val osName = if (agentObj.getValue("OperatingSystemName") == "??") null else agentObj.getValue("OperatingSystemName").lowercase()
                        // eg. 13
                        val osVersion = if (agentObj.getValue("OperatingSystemVersion") == "??") null else agentObj.getValue("OperatingSystemVersion").lowercase()
                        // eg. chrome
                        val agentName = if (agentObj.getValue("AgentName") == "??") null else agentObj.getValue("AgentName").lowercase()
                        // eg. 114
                        val agentVersion = if (agentObj.getValue("AgentVersion") == "??") null else agentObj.getValue("AgentVersion").lowercase()

                        val useragentObj = Useragent()
                        useragentObj.setDeviceClass(deviceClass)
                        useragentObj.setOsClass(osClass)
                        useragentObj.setOsName(osName)
                        useragentObj.setOsVersion(osVersion)
                        useragentObj.setAgentName(agentName)
                        useragentObj.setAgentVersion(agentVersion)
                        useragentObj.setUserId(userId)
                        useragentObj.setCreatedAt(getCurrentTimestamp())
                        useragentRepository?.save(useragentObj)

                        val clientIp = TextUtils.getClientIp(request)

                        notifyLogin(user, useragentObj, clientIp)

                        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
                        val sidecarDir = rootPath + relativeSidecarDir
                        val uuidFromUsername = TextUtils.generateUUID(user.getUsername(),null,null,null,null,null,"profile creation")
                        val profileDirectory = sidecarDir.dropLast(1) + "/profile"
                        val profileFileStr = "$profileDirectory/$uuidFromUsername.png"
                        // If image doesn't exist, delete profile entry
                        if (!user.getProfile().isNullOrEmpty() && !File(profileFileStr).exists()) {
                            logger.log(Level.WARNING, "Removing profile entry. Profile entry exists but image missing.")
                            user.setProfile(null)
                            user.setModifiedAt(getCurrentTimestamp())
                            userRepository?.save(user)
                        }

                        if (uriPath.isNotEmpty()) {
                            redirectStrategy.sendRedirect(request, response, uriPath)
                        } else if ((currentAuthority == adminRole || currentAuthority == superRole) && agentName != "safari") {
                            redirectStrategy.sendRedirect(request, response, "/timeline")
                        } else if (currentAuthority == adminRole || currentAuthority == superRole) {
                            redirectStrategy.sendRedirect(request, response, "/taken")
                        } else {
                            redirectStrategy.sendRedirect(request, response, "/albums")
                        }
                    }
                }
            }
        }
    }

    private fun validSubPaths(pathToCompare: String): Boolean {
        val validWebPaths = MultiSecurityConfig.validWebPaths
        for (path in validWebPaths) {
            if (path.toRegex().matches(pathToCompare.drop(1))) {
                return true
            }
        }

        return false
    }

    private fun notifyLogin(currentUserObj: User?, userAgent: Useragent, clientIP: String?) {
        val admins = userRepository?.findAllAdmins()

        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())

        if (admins != null && currentUserObj != null) {
            // eg. mobile
            val osClass = if (userAgent.getOsClass() == null) "" else userAgent.getOsClass() + " "
            // eg. android
            val osName = if (userAgent.getOsName() == null) "" else userAgent.getOsName() + " "
            // eg. 13
            val osVersion = if (userAgent.getOsVersion() == null) "" else userAgent.getOsVersion() + " "
            // eg. chrome
            val agentName = if (userAgent.getAgentName() == null) "" else userAgent.getAgentName() + " "
            // eg. 114
            val agentVersion = if (userAgent.getAgentVersion() == null) "" else userAgent.getAgentVersion() + " "


            val notificationObjList = mutableListOf<Notification>()
            for (admin in admins) {
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(getCurrentTimestamp())
                notificationObj.setModifiedAt(getCurrentTimestamp())
                var identity = currentUserObj.getUsername()
                if (admin.getAuthority() == superRole) {
                    identity = "<a href='/settings/users' target='_blank'>"+currentUserObj.getUsername()+"</a>"
                }

                if (admin.getId() == currentUserObj.getId()) {
                    notificationObj.setRead(true)
                    identity = "You "
                } else {
                    notificationObj.setRead(false)
                }
                notificationObj.setMessage("$identity logged in from IP <a href='https://ipgeolocation.io/ip-location/$clientIP' target='_blank'>$clientIP</a> using device $osClass$osName$osVersion and browser $agentName$agentVersion at ${sdtf.format(Date())}.")
                notificationObjList.add(notificationObj)
            }
            if (notificationObjList.isNotEmpty()) {
                notificationRepository?.saveAll(notificationObjList)
            }
        }
    }

    override fun getRedirectStrategy(): RedirectStrategy {
        return redirectStrategy
    }
}