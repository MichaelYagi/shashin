package com.miyagi.shashin.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
    @WithMockUser(username = "testuser", roles = ["USER"])
    @Throws(Exception::class)
    fun shouldReturn403WhenSendingRequestToTimelineApiWithInvalidAuthority() {
        val response = mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", userKey)
        )

//        println(response.andReturn().response.contentAsString)
//        println(response.andReturn().response.status)

        response
            .andExpect(status().is4xxClientError)

        // Forbidden
        Assertions.assertTrue(response.andReturn().response.status == 403)
    }

    @Test
    @WithMockUser(username = "testadmin", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun shouldReturn415WhenSendingRequestToTimelineApiWithMissingContentType() {
        val response = mockMvc!!.perform(
            get("/api/v1/timeline/0")
                .header("X-Api-Key", adminKey)
        )

//        println(response.andReturn().response.contentAsString)
//        println(response.andReturn().response.status)

        response
            .andExpect(status().is4xxClientError)

        // Unsupported Media Type
        Assertions.assertTrue(response.andReturn().response.status == 415)
    }

    @Test
    @WithMockUser(username = "testsuper", roles = ["SUPER"])
    @Throws(Exception::class)
    fun shouldReturnSuccessWhenSendingRequestToTimelineApiWithRoleSuper() {
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

    @Test
    @WithMockUser(username = "testsuper", roles = ["SUPER"])
    @Throws(Exception::class)
    fun shouldUpdateAuthorized() {
        var userUser = userRepository?.findById(userId!!.toInt())
        var userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertTrue(userUser?.get()?.getIsAuthorized()!!)
        Assertions.assertTrue(userAdmin?.get()?.getIsAuthorized()!!)

        val payload: Any = object : Any() {
            val userIds: MutableList<Int> = mutableListOf(adminId!!.toInt(), userId!!.toInt())
            val authorized = false
        }

        val objectMapper = ObjectMapper()
        val json = objectMapper.writeValueAsString(payload)

        val response = mockMvc!!.perform(
            post("/api/v1/users/update/authorized")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", superKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
//println(response.andReturn().response.contentAsString)
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString(userId!!.toInt().toString())))

        userUser = userRepository?.findById(userId!!.toInt())
        userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertFalse(userUser?.get()?.getIsAuthorized()!!)
        Assertions.assertFalse(userAdmin?.get()?.getIsAuthorized()!!)
    }

    @Test
    @WithMockUser(username = "testsuper", roles = ["SUPER"])
    @Throws(Exception::class)
    fun shouldUpdateAuthority() {
        var userUser = userRepository?.findById(userId!!.toInt())
        var userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertTrue(userUser?.get()?.getAuthority() == "ROLE_USER")
        Assertions.assertTrue(userAdmin?.get()?.getAuthority() == "ROLE_ADMIN")

        val payload: Any = object : Any() {
            val userIds: MutableList<Int> = mutableListOf(adminId!!.toInt(), userId!!.toInt())
            val role = "ROLE_SUPER"
        }

        val objectMapper = ObjectMapper()
        val json = objectMapper.writeValueAsString(payload)

        val response = mockMvc!!.perform(
            post("/api/v1/users/update/role")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", superKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
//println(response.andReturn().response.contentAsString)
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString(userId!!.toInt().toString())))

        userUser = userRepository?.findById(userId!!.toInt())
        userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertTrue(userUser?.get()?.getAuthority() == "ROLE_SUPER")
        Assertions.assertTrue(userAdmin?.get()?.getAuthority() == "ROLE_SUPER")
    }

    @Test
    @WithMockUser(username = "testsuper", roles = ["SUPER"])
    @Throws(Exception::class)
    fun shouldDeleteUsers() {
        val userUser = userRepository?.findById(userId!!.toInt())
        val userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertTrue(userUser?.get()?.getAuthority() == "ROLE_USER")
        Assertions.assertTrue(userAdmin?.get()?.getAuthority() == "ROLE_ADMIN")

        val payload: Any = object : Any() {
            val userIds: MutableList<Int> = mutableListOf(adminId!!.toInt(), userId!!.toInt())
        }

        val objectMapper = ObjectMapper()
        val json = objectMapper.writeValueAsString(payload)

        val response = mockMvc!!.perform(
            post("/api/v1/users/delete")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", superKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
//println(response.andReturn().response.contentAsString)
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString(userId!!.toInt().toString())))

        val users = userRepository?.findAll()

        Assertions.assertTrue(users!!.count() == 1)
        Assertions.assertTrue(users.first()!!.getId() == superId)
    }
}