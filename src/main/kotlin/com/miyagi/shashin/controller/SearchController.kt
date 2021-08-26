package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.SearchRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import javax.servlet.http.HttpServletRequest


@Controller
@Secured("ROLE_ADMIN","ROLE_USER")
class SearchController {

    @Autowired
    private val searchRepository: SearchRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @GetMapping("/search")
    fun getSearch(model: Model, request: HttpServletRequest): String {
        model["searchTerm"] = ""
        model["metadataSearchList"] = ""

        val searchTerm = request.getParameter("searchTerm")
        if (!searchTerm.isNullOrBlank()) {
            model["searchTerm"] = searchTerm
            if (model.getAttribute("authority").toString() == model.getAttribute("adminRole")) {
                val metadataList = searchRepository?.findMetadataBySearchTerm(searchTerm)
                model["metadataSearchList"] = metadataList as MutableIterable<Metadata>
            } else if (model.getAttribute("authority").toString() == model.getAttribute("userRole")) {
                val currentUserObj = model.getAttribute("currentUser") as User?
                if (currentUserObj != null) {
                    val metadataList = searchRepository?.findMetadataBySearchTermAndUserId(searchTerm, currentUserObj.getId())
                    model["metadataSearchList"] = metadataList as MutableIterable<Metadata>
                }
            }
        }

        val module = "search"
        model["data"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/search"], method = [RequestMethod.POST], consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun postSearch(model: Model, redirectAttributes: RedirectAttributes, @RequestBody formData: MultiValueMap<String, String>): String {
        model["searchTerm"] = ""
        if (formData.containsKey("appSearchInput")) {
            val searchTerm: String = java.lang.String.valueOf(formData.getFirst("appSearchInput"))
            redirectAttributes.addAttribute("searchTerm", searchTerm);
        }

        val module = "search"
        model["data"] = ""
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return "redirect:/"+module
    }
}