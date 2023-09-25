package com.miyagi.shashin.controller

import com.miyagi.shashin.component.RssFeedView
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.View


@Controller
class RssFeedController {

    @Autowired
    private val view: RssFeedView? = null

    @Autowired
    var userRepository: UserRepository? = null

    @GetMapping("/{apiKey}/rss")
    fun getFeed(@PathVariable apiKey: String): View? {
        return view
    }
}