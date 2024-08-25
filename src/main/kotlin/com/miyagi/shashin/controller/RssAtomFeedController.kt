package com.miyagi.shashin.controller

import com.miyagi.shashin.component.AtomFeedView
import com.miyagi.shashin.component.RssFeedView
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.View


@Controller
class RssAtomFeedController {

    @Autowired
    private val rssview: RssFeedView? = null

    @Autowired
    private val atomview: AtomFeedView? = null

    @Autowired
    var userRepository: UserRepository? = null

    @GetMapping("/{apiKey}/rss")
    fun getRssFeed(@PathVariable apiKey: String): View? {
        return rssview
    }

    @GetMapping("/{apiKey}/atom")
    fun getAtomFeed(@PathVariable apiKey: String): View? {
        return atomview
    }
}