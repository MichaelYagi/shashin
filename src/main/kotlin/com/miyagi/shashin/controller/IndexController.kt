package com.miyagi.shashin.controller

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.service.CustomUserDetailsService
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

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

        if ((currentUserObj != null && currentUserObj.getIsAuthorized() == true) ||
            (sessionUser != null && sessionUser.getIsAuthorized() == true) ||
            TextUtils.checkValidRememberMeToken(request.getHeader("Cookie"), rememberMeKey.toString(), userRepository)
        ) {
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

        if ((currentUserObj != null && currentUserObj.getIsAuthorized() == true) || (sessionUser != null && sessionUser.getIsAuthorized() == true) || TextUtils.checkValidRememberMeToken(request.getHeader("Cookie"), rememberMeKey.toString(), userRepository)) {
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