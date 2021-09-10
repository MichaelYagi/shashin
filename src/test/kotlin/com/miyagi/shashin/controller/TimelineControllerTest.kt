package com.miyagi.shashin.controller

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext


@RunWith(SpringRunner::class)
@SpringBootTest
@Transactional
class TimelineControllerTest {

    private var mockMvc: MockMvc? = null

    @Autowired
    private val context: WebApplicationContext? = null

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Autowired
    private val mediaDirRepository: MediaDirectoryRepository? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val userAlbumRepository: UserAlbumRepository? = null

    @Autowired
    private val favoriteRepository: FavoriteRepository? = null

    @Autowired
    private val commentRepository: CommentRepository? = null

    @Autowired
    private val albumPhotoCommentRepository: AlbumPhotoCommentRepository? = null

    @Autowired
    private val albumCommentRepository: AlbumCommentRepository? = null

    @Autowired
    private val albumRepository: AlbumRepository? = null

    @Autowired
    private val albumPhotoRepository: AlbumPhotoRepository? = null

    private var tempUserId: Int? = null

    @Before
    fun setup() {
        val userObj = User()
        userObj.setUsername("test")
        userObj.setPassword("test")
        userObj.setAuthority("test")
        userRepository?.save(userObj)

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @Test
    @WithMockUser(username = "invaliduser", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturn302WhenSendingRequestToControllerWithInvalidRoleUser() {
        mockMvc!!.perform(get("/timeline/mediatype/all"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    @WithMockUser(username = "test", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturn200WhenSendingRequestToControllerWithRoleUser() {
        val response = mockMvc!!.perform(get("/timeline/mediatype/all"))
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Timeline")))
        //println(response.andReturn().response.contentAsString)
    }
}