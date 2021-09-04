package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.MetadataPeople
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import java.util.ArrayList
import java.util.HashMap

@Controller
class PeopleController {

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    @Autowired
    private var albumRepository: AlbumRepository? = null

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/people")
    fun getPeople(model: Model): String {
        val module = "people"
        model["data"] = "There are no people tagged."
        model["peopleList"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var peopleList: MutableIterable<MetadataPeople>? = null
            if (currentUserObj.getAuthority() == model.getAttribute("userRole")) {
                peopleList = albumRepository?.findAlbumPhotoByPeople()
            } else if (currentUserObj.getAuthority() == model.getAttribute("adminRole")) {
                peopleList = metadataRepository?.findMetadataByPeople()
            }
            if (peopleList != null) {
                for (person in peopleList) {
                    println(person.getThumbnailUrlCentered())
                }
                model["peopleList"] = peopleList
                model["data"] = ""
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }
}