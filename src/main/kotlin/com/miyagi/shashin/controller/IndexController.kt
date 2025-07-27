package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.annotation.Secured
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.support.RequestContextUtils

@Controller
class IndexController {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

    @GetMapping("/")
    fun getIndex(model: Model, request: HttpServletRequest): String {
        val module = "index"

        model["loggedIn"] = false
        val currentUserObj = model.getAttribute("currentUser") as User?
        val sessionUser = request.session.getAttribute("CurrentUser") as User?
        val cookieAuthority = TextUtils.checkValidRememberMeToken(request.getHeader("Cookie"), rememberMeKey.toString(), userRepository)

        if ((currentUserObj != null && currentUserObj.getIsAuthorized() == true) || (sessionUser != null && sessionUser.getIsAuthorized() == true || cookieAuthority != null)) {
            if (cookieAuthority != null) {
                model["authority"] = cookieAuthority
            }
            model["loggedIn"] = true
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }

    @GetMapping("/features")
    fun getFeatures(model: Model, request: HttpServletRequest): String {
        val module = "features"

        model["loggedIn"] = false
        val currentUserObj = model.getAttribute("currentUser") as User?
        val sessionUser = request.session.getAttribute("CurrentUser") as User?
        val cookieAuthority = TextUtils.checkValidRememberMeToken(request.getHeader("Cookie"), rememberMeKey.toString(), userRepository)

        if ((currentUserObj != null && currentUserObj.getIsAuthorized() == true) || (sessionUser != null && sessionUser.getIsAuthorized() == true || cookieAuthority != null)) {
            if (cookieAuthority != null) {
                model["authority"] = cookieAuthority
            }
            model["loggedIn"] = true
        }

        model["supportedImageTypes"] = FileUtils.allowableImageFiles().joinToString(", ", transform = { it.uppercase() })
        model["supportedRawTypes"] = FileUtils.allowableRawImageFiles().joinToString(", ", transform = { it.uppercase() })
        model["supportedVideoTypes"] = FileUtils.allowableVideoFiles().joinToString(", ", transform = { it.uppercase() })

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)

        return module
    }
}