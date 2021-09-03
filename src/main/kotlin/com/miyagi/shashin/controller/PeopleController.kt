package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.User
import com.miyagi.shashin.util.TextUtils
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import java.util.ArrayList
import java.util.HashMap

@Controller
class PeopleController {
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/people")
    fun getPeople(model: Model): String {
        val module = "people"
        model["data"] = "There are no people tagged."
        model["peopleList"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {

        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }
}