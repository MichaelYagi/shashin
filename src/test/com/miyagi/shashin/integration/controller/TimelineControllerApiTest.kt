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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@ActiveProfiles("test")
class TimelineControllerApiTest {

    private var adminId: Int? = null
    private var userId: Int? = null
    private var mockMvc: MockMvc? = null

    @Autowired
    private val context: WebApplicationContext? = null

    @Autowired
    private val userRepository: UserRepository? = null

    private var bcrypt = BCryptPasswordEncoder()

    @BeforeEach
    fun setup() {
        val adminObj = User()
        adminObj.setUsername("testadmin")
        var encodedPassword: String = bcrypt.encode("testadmin")
        adminObj.setPassword(encodedPassword)
        adminObj.setAuthority("ROLE_ADMIN")
        adminObj.setIsAuthorized(true)
        adminObj.setApikey("00000000-00000000-00000000-00000000")
        userRepository?.save(adminObj)
        adminId = adminObj.getId()

        val userObj = User()
        userObj.setUsername("testuser")
        encodedPassword = bcrypt.encode("testuser")
        userObj.setPassword(encodedPassword)
        userObj.setAuthority("ROLE_USER")
        userObj.setIsAuthorized(true)
        userObj.setApikey("00000000-00000000-00000000-00000001")
        userRepository?.save(userObj)
        userId = userObj.getId()

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @AfterEach
    fun tearDown() {
        adminId?.let { userRepository?.deleteById(it) }
        userId?.let { userRepository?.deleteById(it) }
    }

    @Test
    @WithMockUser(username = "invaliduser", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturn401WhenSendingRequestToTimelineApiWithRoleUser() {
        mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("X-Api-Key", "00000000-00000000-00000000-00000000")
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @Throws(Exception::class)
    fun shouldReturn403WhenSendingRequestToTimelineApiWithRoleUser() {
        mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", "00000000-00000000-00000000-00000003")
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @WithMockUser(username = "testadmin", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturnSuccessWhenSendingRequestToTimelineApiWithRoleAdmin() {
        val response = mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", "00000000-00000000-00000000-00000000")
        )
        //println(response.andReturn().response.contentAsString)
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("success")))
    }
}