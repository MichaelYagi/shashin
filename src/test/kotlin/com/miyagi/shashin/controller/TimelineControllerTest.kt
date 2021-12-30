package com.miyagi.shashin.controller

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext


@SpringBootTest
@Transactional
class TimelineControllerTest {

    private var userId: Int? = null

    private var mockMvc: MockMvc? = null

    @Autowired
    private val context: WebApplicationContext? = null

    @Autowired
    private val userRepository: UserRepository? = null

    private var bcrypt = BCryptPasswordEncoder()

    @BeforeEach
    fun setup() {
        val userObj = User()
        userObj.setUsername("test")
        val encodedPassword: String = bcrypt.encode("test")
        userObj.setPassword(encodedPassword)
        userObj.setAuthority("ROLE_ADMIN")
        userObj.setIsAllowed(true)
        userRepository?.save(userObj)
        userId = userObj.getId()

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @AfterEach
    fun tearDown() {
        userId?.let { userRepository?.deleteById(it) }
    }

    @Test
    @WithMockUser(username = "invaliduser", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturn302WhenSendingRequestToControllerWithInvalidRoleUser() {
        mockMvc!!.perform(get("/timeline"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    @WithMockUser(username = "test", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturn200WhenSendingRequestToControllerWithRoleUser() {
        val response = mockMvc!!.perform(get("/timeline"))
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Timeline")))
        //println(response.andReturn().response.contentAsString)
    }
}