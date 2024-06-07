package com.miyagi.shashin.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.controller.ToolsController
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.regex.Pattern


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class APITests: BaseSeleniumTests() {

    @Autowired
    private lateinit var toolsController: ToolsController
    private var metadataId: String? = null
    private var adminId: Int? = null
    private var userId: Int? = null
    private var albumId: Int? = null
    private var mockMvc: MockMvc? = null

    @LocalServerPort
    private val port = 0

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
        adminObj.setAuthority("ROLE_SUPER")
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
    }

    @Test
    @Throws(Exception::class)
    fun toolsControllerAPITests() {
        val webClient = WebClient.create("http://localhost:$port/")

        var jsonString: String? = null
        var jsonNode: JsonNode? = null
        val mapper = ObjectMapper()

        try {
            val response = webClient.get()
                .uri("api/v1/health")
                .header("x-api-key", "00000000-00000000-00000000-00000000")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            jsonString = response
        } catch (_: Exception) {}

        var status = ""
        var os = ""
        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
            status = jsonNode.get("status").textValue()
            os = jsonNode.get("system").get("os").textValue()
        }

        Assertions.assertTrue(status == "OK")
        Assertions.assertTrue(os.isNotEmpty())

        jsonString = ""
        try {
            val response = webClient.get()
                .uri("api/v1/status")
                .header("x-api-key", "00000000-00000000-00000000-00000001")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            jsonString = response
        } catch (_: Exception) {}

        var singleStatus = ""
        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
            singleStatus = jsonNode.get("status").textValue()
        }

        Assertions.assertTrue(singleStatus == "OK")
    }
}