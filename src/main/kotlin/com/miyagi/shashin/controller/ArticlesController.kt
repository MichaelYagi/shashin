package com.miyagi.shashin.controller

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import jakarta.servlet.http.HttpServletRequest

@Controller
class ArticlesController(
    var userRepository: UserRepository
) {
    @RequestMapping(value = ["/articles/quickstart"], method = [RequestMethod.GET])
    fun getQuickstart(model: Model, request: HttpServletRequest): String {
        val module = "articles/quickstart"

        model["loggedIn"] = false
        val currentUserObj = model.getAttribute("currentUser") as User?
        val sessionUser = request.session.getAttribute("CurrentUser") as User?

        if ((currentUserObj != null && currentUserObj.getIsAuthorized() == true) || (sessionUser != null && sessionUser.getIsAuthorized() == true)) {
            model["loggedIn"] = true
        }

        val moduleArray = module.split("/")
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(moduleArray[moduleArray.size-1])

        return module
    }

    @RequestMapping(value = ["/articles","/articles/tips"], method = [RequestMethod.GET])
    fun getTips(model: Model, request: HttpServletRequest): String {
        val module = "articles/tips"

        model["loggedIn"] = false
        val currentUserObj = model.getAttribute("currentUser") as User?
        val sessionUser = request.session.getAttribute("CurrentUser") as User?

        if ((currentUserObj != null && currentUserObj.getIsAuthorized() == true) || (sessionUser != null && sessionUser.getIsAuthorized() == true)) {
            model["loggedIn"] = true
        }

        val moduleArray = module.split("/")
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(moduleArray[moduleArray.size-1])

        return module
    }

//    @Secured("ROLE_SUPER", "ROLE_ADMIN")
    @RequestMapping(value = ["/articles/devnotes"], method = [RequestMethod.GET])
    fun getDevnotes(model: Model, request: HttpServletRequest): String {
        val module = "articles/devnotes"

        model["loggedIn"] = false
        val currentUserObj = model.getAttribute("currentUser") as User?
        val sessionUser = request.session.getAttribute("CurrentUser") as User?

        if ((currentUserObj != null && currentUserObj.getIsAuthorized() == true) || (sessionUser != null && sessionUser.getIsAuthorized() == true)) {
            model["loggedIn"] = true
        }

        val moduleArray = module.split("/")
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(moduleArray[moduleArray.size-1])

        return module
    }

//    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["articles/endpoints"], method = [RequestMethod.GET])
    fun getEndpoints(model: Model, request: HttpServletRequest): String {
        val module = "articles/endpoints"

        model["loggedIn"] = false

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj?.getAuthority() != null && currentUserObj.getIsAuthorized() == true &&
            (currentUserObj.getAuthority()!! == "ROLE_SUPER" || currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_USER")) {

            model["apiEndpointsMapList"] = TextUtils.getEndpointData(currentUserObj.getAuthority()!!,request)
            model["loggedIn"] = true
        }

        val moduleArray = module.split("/")
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(moduleArray[moduleArray.size-1])

        return module
    }
}