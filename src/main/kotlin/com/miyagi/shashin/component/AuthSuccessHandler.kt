package com.miyagi.shashin.component

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.configuration.MultiSecurityConfig
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.PersistentLoginsExpiry
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.repository.PersistentLoginsExpiryRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.service.CustomUserDetailsService
import com.miyagi.shashin.util.DatabaseUtil
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.sql.DataSource
import kotlin.collections.HashMap


@Component
class AuthSuccessHandler : SimpleUrlAuthenticationSuccessHandler() {

    private val redirectStrategy = DefaultRedirectStrategy()

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    @Value("\${app.build.properties.version}")
    private val appVersion: String? = null

    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

    @Value("\${app.rememberme.expiration.seconds}")
    private var expirationSeconds: Int? = null

    @Autowired
    private val dataSource: DataSource? = null

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    var notificationRepository: NotificationRepository? = null

    @Autowired
    var customUserDetailsService: CustomUserDetailsService? = null

    @Autowired
    var persistentLoginsRepository: PersistentLoginsRepository? = null

    @Autowired
    var persistentLoginsExpiryRepository: PersistentLoginsExpiryRepository? = null

    private var persistentTokenRepository: PersistentTokenRepository? = null

    fun setPersistentTokenRepository(persistentTokenRepository: PersistentTokenRepository?): AuthSuccessHandler {
        this.persistentTokenRepository = persistentTokenRepository

        return this
    }

    @Throws(IOException::class)
    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?, authentication: Authentication?) {
        val logger: Logger = Logger.getLogger(AuthSuccessHandler::class.simpleName)

        var uriPath = request!!.session.getAttribute("ShashinReferer")
        if (uriPath == null || !validSubPaths(uriPath.toString())) {
            uriPath = ""
        } else {
            uriPath = uriPath.toString()
        }
        request.session.removeAttribute("ShashinReferer")

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
                var isAuthorized = true
                val user = userRepository?.findByUsername(authentication.name)
                if (user != null && user.getId() > 0) {
                    if (user.getIsAuthorized() == false) {
                        user.setModifiedAt(getCurrentTimestamp())
                        userRepository?.save(user)
                        SecurityContextLogoutHandler().logout(request, response, authentication)
                        SecurityContextHolder.getContext().authentication = null
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
                        notifyLogin(user)
//                        checkLatestAppVersion(user)
                    }

                    if (this.persistentTokenRepository != null) {
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

                    if (response != null) {
                        var series = ""
                        var expiry = ""

                        for (cookie in response.getHeaders("Set-Cookie")) {
                            if (cookie.contains("remember-me")) {
                                val seriesExpiryMap = TextUtils.parseRememberMeCookie(cookie)
                                series = seriesExpiryMap["series"].toString()
                                expiry = seriesExpiryMap["expiry"].toString()

                                break
                            }
                        }

                        if (series.isNotEmpty() && expiry.isNotEmpty()) {
                            val persistentLoginsExpiry = PersistentLoginsExpiry()
                            persistentLoginsExpiry.setSeries(series)
                            persistentLoginsExpiry.setExpiry((System.currentTimeMillis()+(expiry.toLong()*1000)).toString())
                            persistentLoginsExpiryRepository?.save(persistentLoginsExpiry)
                        }

                        // Cleanup tasks
                        DatabaseUtil.cleanupPersistence(persistentLoginsExpiryRepository, persistentLoginsRepository)
                    }

                    if (uriPath.isNotEmpty()) {
                        redirectStrategy.sendRedirect(request, response, uriPath)
                    } else if (currentAuthority == adminRole) {
                        redirectStrategy.sendRedirect(request, response, "/timeline")
                    } else {
                        redirectStrategy.sendRedirect(request, response, "/albums")
                    }
                }
            }
        }
    }

    private fun validSubPaths(pathToCompare: String): Boolean {
        val validWebSubPaths = MultiSecurityConfig.validWebSubPaths
        for (path in validWebSubPaths) {
            if (pathToCompare.lowercase().contains(path)) {
                return true
            }
        }

        return false
    }

    private fun checkLatestAppVersion(user: User) {
        // Check app version
        val logger: Logger = Logger.getLogger(AuthSuccessHandler::class.simpleName)
        val client = HttpClient.newBuilder().build()
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://shashin.jfrog.io/artifactory/api/search/aql"))
            .POST(HttpRequest.BodyPublishers.ofString("items.find({\"repo\":\"shashin\"})"))
            .header("Content-Type", "text/plain")
            .header("X-JFrog-Art-Api", "AKCp8kq2kFaHn5BovzP5cJFrb3Ny8kSVSdDW778KeLk3645jyVSSVrdcjgds6R8qK6SJV65ct")
            .build()

        val httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        val jsonResult = httpResponse.body()
        val mapper = ObjectMapper()
        try {
            val jsonObj = mapper.readTree(jsonResult)
            val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, ArrayList<Map<String, Any>>>>() {})
            val resultList = resultMap["results"] as ArrayList<Map<String, Any>>
    
            var lastMinVersion = DefaultArtifactVersion(appVersion)
            var latestVersion = appVersion
            for (result in resultList) {
                val propName = result["name"].toString()
                if (propName.startsWith("shashin-") && propName.endsWith(".tar")) {
                    var parsedVersion = propName.substringAfter("shashin-")
                    parsedVersion = parsedVersion.substringBefore(".tar")
                    val checkedVersion = DefaultArtifactVersion(parsedVersion)
                    if (checkedVersion > lastMinVersion) {
                        lastMinVersion = checkedVersion
                        latestVersion = parsedVersion
                    }
                }
            }
            
            if (latestVersion!!.isNotBlank() && appVersion!!.isNotBlank() && latestVersion != appVersion) {
                notifyLatestVersion(user, latestVersion)
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Could not read latest version: " + e.message)
        }
    }

    private fun notifyLogin(currentUserObj: User?) {
        val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
        val sdtf = SimpleDateFormat("yyyy/MM/dd h:mm:ss aa z")
        sdtf.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())

        if (admins != null && currentUserObj != null) {
            val notificationObjList = mutableListOf<Notification>()
            for (admin in admins) {
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(getCurrentTimestamp())
                notificationObj.setModifiedAt(getCurrentTimestamp())
                var identity = "<a href='/settings/users' target='_blank'>"+currentUserObj.getUsername()+"</a>"
                if (admin.getId() == currentUserObj.getId()) {
                    notificationObj.setRead(true)
                    identity = "You "
                } else {
                    notificationObj.setRead(false)
                }
                notificationObj.setMessage("$identity logged in at "+sdtf.format(Date())+".")
                notificationObjList.add(notificationObj)
            }
            if (notificationObjList.isNotEmpty()) {
                notificationRepository?.saveAll(notificationObjList)
            }
        }
    }

    private fun notifyLatestVersion(currentUserObj: User?, version: String) {
        val admins = userRepository?.findAllByAuthorityEquals(adminRole!!)
        if (admins != null && currentUserObj != null) {
            val notificationObjList = mutableListOf<Notification>()
            for (admin in admins) {
                val notificationObj = Notification()
                notificationObj.setUserId(admin.getId())
                notificationObj.setCreatedAt(getCurrentTimestamp())
                notificationObj.setModifiedAt(getCurrentTimestamp())
                notificationObj.setRead(false)
                notificationObj.setMessage("Server update for Shashin version $version is available.")
                notificationObjList.add(notificationObj)
            }
            if (notificationObjList.isNotEmpty()) {
                notificationRepository?.saveAll(notificationObjList)
            }
        }
    }
}