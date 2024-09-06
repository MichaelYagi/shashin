package com.miyagi.shashin.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
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
class APITest {

    private var superId: Int? = null
    private var adminId: Int? = null
    private var userId: Int? = null
    private var superKey: String? = null
    private var adminKey: String? = null
    private var userKey: String? = null
    private var mockMvc: MockMvc? = null

    @Autowired
    private val context: WebApplicationContext? = null

    @Autowired
    private val userRepository: UserRepository? = null

    private var bcrypt = BCryptPasswordEncoder()

    @BeforeEach
    fun setup() {
        val superObj = User()
        superObj.setUsername("testsuper")
        var encodedPassword: String = bcrypt.encode("testsuper")
        superObj.setPassword(encodedPassword)
        superObj.setAuthority("ROLE_SUPER")
        superObj.setIsAuthorized(true)
        superKey = "00000000-00000000-00000000-00000000"
        superObj.setApikey(superKey)
        userRepository?.save(superObj)
        superId = superObj.getId()

        val adminObj = User()
        adminObj.setUsername("testadmin")
        encodedPassword = bcrypt.encode("testadmin")
        adminObj.setPassword(encodedPassword)
        adminObj.setAuthority("ROLE_ADMIN")
        adminObj.setIsAuthorized(true)
        adminKey = "00000000-00000000-00000000-00000001"
        adminObj.setApikey(adminKey)
        userRepository?.save(adminObj)
        adminId = adminObj.getId()

        val userObj = User()
        userObj.setUsername("testuser")
        encodedPassword = bcrypt.encode("testuser")
        userObj.setPassword(encodedPassword)
        userObj.setAuthority("ROLE_USER")
        userObj.setIsAuthorized(true)
        userKey = "00000000-00000000-00000000-00000002"
        userObj.setApikey(userKey)
        userRepository?.save(userObj)
        userId = userObj.getId()

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @AfterEach
    fun tearDown() {
        superId?.let { userRepository?.deleteById(it) }
        adminId?.let { userRepository?.deleteById(it) }
        userId?.let { userRepository?.deleteById(it) }
    }

    @Test
    @WithMockUser(username = "invaliduser", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturn401WhenSendingRequestToTimelineApiWithRoleUser() {
        mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("X-Api-Key", superKey)
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    @Throws(Exception::class)
    fun shouldReturn4xxWhenSendingRequestToTimelineApiWithRoleUser() {
        mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", userKey)
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    @WithMockUser(username = "testsuper", roles = ["SUPER"])
    @Throws(Exception::class)
    fun shouldReturnSuccessWhenSendingRequestToTimelineApiWithRoleAdmin() {
        val response = mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", superKey)
        )
//        println(response.andReturn().response.contentAsString)
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString(ApiResponse.SUCCESS.status)))
    }

    @Test
    @WithMockUser(username = "testsuper", roles = ["SUPER"])
    @Throws(Exception::class)
    fun shouldReturnProperEndpointsForSuper() {
        val response = mockMvc!!.perform(
            get("/api/v1/endpoints")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", superKey)
        )
//        println(response.andReturn().response.contentAsString)
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("super")))

        val jsonStr = response.andReturn().response.contentAsString
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(jsonStr)

        for (jsonObj in jsonNode) {
            Assertions.assertTrue(jsonObj.get("authorizedRoles").toString().contains("super") || jsonObj.get("authorizedRoles").toString().contains("public"))
        }
    }
}