package com.miyagi.shashin.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.ToolsControllerTestConfig
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.ApiResponse
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
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
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
@Import(ToolsControllerTestConfig::class)
@TestInstance(Lifecycle.PER_CLASS)
class APITests {

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

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Autowired
    private val mediaDirRepository: MediaDirectoryRepository? = null

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

    @Autowired
    private val notificationRepository: NotificationRepository? = null

    @Autowired
    private val recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private val recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private val settingsRepository: SettingsRepository? = null

    @Autowired
    private val keywordPhotoRepository: KeywordPhotoRepository? = null

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Autowired
    private val searchHistoryRepository: SearchHistoryRepository? = null

    @Autowired
    private val searchRepository: SearchRepository? = null

    @Autowired
    private val useragentRepository: UseragentRepository? = null

    private var bcrypt = BCryptPasswordEncoder()

    @BeforeAll
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

    @AfterAll
    fun tearDown() {
        userRepository?.deleteAll()
        metadataRepository?.deleteAll()
        mediaDirRepository?.deleteAll()
        userAlbumRepository?.deleteAll()
        favoriteRepository?.deleteAll()
        commentRepository?.deleteAll()
        albumPhotoCommentRepository?.deleteAll()
        albumCommentRepository?.deleteAll()
        albumRepository?.deleteAll()
        albumPhotoRepository?.deleteAll()
        notificationRepository?.deleteAll()
        recognitionLabelRepository?.deleteAll()
        recognitionLabelPhotoRepository?.deleteAll()
        settingsRepository?.deleteAll()
        keywordPhotoRepository?.deleteAll()
        keywordRepository?.deleteAll()
        searchHistoryRepository?.deleteAll()
        searchRepository?.deleteAll()
        useragentRepository?.deleteAll()

        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = File("$rootPath/sidecar_test")
        if (sidecarDir.exists()) {
            //sidecarDir.deleteRecursively()
            purgeDirectory(sidecarDir)
        }
    }

    private fun purgeDirectory(dir: File) {
        for (file in dir.listFiles()!!) {
            if (file.isDirectory) {
                purgeDirectory(file)
            }
            file.delete()
        }
    }

    @Test
    fun shouldReturn403ForRecentRequest() {
        val response = mockMvc!!.perform(
            get("/api/v1/recent/0")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", userKey)
        )

        response
            .andExpect(status().is4xxClientError)

        // Forbidden
        Assertions.assertTrue(response.andReturn().response.status == 403)
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturn403WhenSendingRequestToTimelineApiWithInvalidAuthority() {
        val userObj = User()
        userObj.setUsername("testuser")
        val encodedPassword = bcrypt.encode("testuser")
        userObj.setPassword(encodedPassword)
        userObj.setAuthority("ROLE_USER")
        userObj.setIsAuthorized(true)
        userKey = "00000000-00000000-00000000-00000002"
        userObj.setApikey(userKey)
        userRepository?.save(userObj)
        userId = userObj.getId()

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

        var assertTriggered = false
        for (jsonObj in jsonNode) {
            val authorizedRoleList = jsonObj.get("authorizedRoles").asIterable().toList()
            if (authorizedRoleList.size == 1 && authorizedRoleList[0].textValue() != "public") {
                assertTriggered = true
                Assertions.assertTrue(authorizedRoleList[0].textValue() == "super")
                break
            }
        }
        Assertions.assertTrue(assertTriggered)
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnProperEndpointsForAdmin() {
        val adminObj = User()
        adminObj.setUsername("testadmin")
        val encodedPassword = bcrypt.encode("testadmin")
        adminObj.setPassword(encodedPassword)
        adminObj.setAuthority("ROLE_ADMIN")
        adminObj.setIsAuthorized(true)
        adminKey = "00000000-00000000-00000000-00000001"
        adminObj.setApikey(adminKey)
        userRepository?.save(adminObj)
        adminId = adminObj.getId()

        val response = mockMvc!!.perform(
            get("/api/v1/endpoints")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", adminKey)
        )
//        println(response.andReturn().response.contentAsString)
        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("admin")))

        val jsonStr = response.andReturn().response.contentAsString
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(jsonStr)

        var assertTriggered = false
        for (jsonObj in jsonNode) {
            val authorizedRoleList = jsonObj.get("authorizedRoles").asIterable().toList()
            if (authorizedRoleList.size == 2) {
                assertTriggered = true
                Assertions.assertTrue(authorizedRoleList[0].textValue() == "admin")
                Assertions.assertTrue(authorizedRoleList[1].textValue() == "super")
                break
            }
        }
        Assertions.assertTrue(assertTriggered)
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnProperEndpointsForUser() {
        val userObj: User? = userRepository?.findByApikey(userKey)
        userObj?.setIsAuthorized(true)
        userRepository?.save(userObj!!)

        val response = mockMvc!!.perform(
            get("/api/v1/endpoints")
                .header("Content-Type", "application/json")
                .header("X-Api-Key", userKey)
        )

        response
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("user")))

        val jsonStr = response.andReturn().response.contentAsString
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(jsonStr)

        for (jsonObj in jsonNode) {
            val authorizedRoleList = jsonObj.get("authorizedRoles").asIterable().toList()
            Assertions.assertTrue(authorizedRoleList.size == 1 || authorizedRoleList.size == 3)
            if (authorizedRoleList.size == 1) {
                Assertions.assertTrue(authorizedRoleList[0].textValue() == "public")
            } else {
                Assertions.assertTrue(authorizedRoleList[0].textValue() == "super")
                Assertions.assertTrue(authorizedRoleList[1].textValue() == "admin")
                Assertions.assertTrue(authorizedRoleList[2].textValue() == "user")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldUpdateAuthorized() {
        var userUser = userRepository?.findById(userId!!.toInt())
        var userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertTrue(userUser?.get()?.getIsAuthorized()!!)
        Assertions.assertTrue(userAdmin?.get()?.getIsAuthorized()!!)

        val payload: Any = object : Any() {
            val userIds: MutableList<Int> = mutableListOf(adminId!!.toInt(), userId!!.toInt())
            val authorized = false
            override fun toString() = "$authorized ${userIds.joinToString(",")}"
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
    @Throws(Exception::class)
    fun shouldUpdateAuthority() {
        var userUser = userRepository?.findById(userId!!.toInt())
        var userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertTrue(userUser?.get()?.getAuthority() == "ROLE_USER")
        Assertions.assertTrue(userAdmin?.get()?.getAuthority() == "ROLE_ADMIN")

        val payload: Any = object : Any() {
            val userIds: MutableList<Int> = mutableListOf(adminId!!.toInt(), userId!!.toInt())
            val role = "ROLE_SUPER"
            override fun toString() = "$role ${userIds.joinToString(",")}"
        }

        val objectMapper = ObjectMapper()
        val json = objectMapper.writeValueAsString(payload)

        val response = mockMvc!!.perform(
            post("/api/v1/users/update/role")
                .header("CONTENT-TYPE", "application/json")
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
    @Throws(Exception::class)
    fun shouldDeleteUsers() {
        val userUser = userRepository?.findById(userId!!.toInt())
        val userAdmin = userRepository?.findById(adminId!!.toInt())

        Assertions.assertTrue(userUser?.get()?.getAuthority() == "ROLE_USER")
        Assertions.assertTrue(userAdmin?.get()?.getAuthority() == "ROLE_ADMIN")

        val payload: Any = object : Any() {
            val userIds: MutableList<Int> = mutableListOf(adminId!!.toInt(), userId!!.toInt())
            override fun toString() = userIds.joinToString(",")
        }

        val objectMapper = ObjectMapper()
        val json = objectMapper.writeValueAsString(payload)

        val response = mockMvc!!.perform(
            post("/api/v1/users/delete")
                .header("content-type", "application/json")
                .header("x-api-key", superKey)
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

    @Test
    @Throws(Exception::class)
    fun shouldDisplayHeathStatus() {
        var response = mockMvc!!.perform(
            get("/api/v1/health")
                .header("Content-Type", "application/json")
                .header("X-API-KEY", userKey)
        )

//        println(response.andReturn().response.contentAsString)

        var jsonString = response.andReturn().response.contentAsString
        var jsonNode: JsonNode?
        val mapper = ObjectMapper()

        var status = ""
        var os = ""
        if (jsonString.isNotBlank()) {
            jsonNode = mapper.readTree(jsonString)
            status = jsonNode.get("serverStatus").textValue()
            os = jsonNode.get("system").get("os").textValue()
        }

        Assertions.assertTrue(status == "OK")
        Assertions.assertTrue(os.isNotEmpty())

        response = mockMvc!!.perform(
            get("/api/v1/status")
                .header("Content-Type", "application/json")
                .header("X-API-Key", adminKey)
        )

        jsonString = response.andReturn().response.contentAsString

        var singleStatus = ""
        if (jsonString.isNotBlank()) {
            jsonNode = mapper.readTree(jsonString)
            singleStatus = jsonNode.get("serverStatus").textValue()
        }

        Assertions.assertTrue(singleStatus == "OK")
    }
}