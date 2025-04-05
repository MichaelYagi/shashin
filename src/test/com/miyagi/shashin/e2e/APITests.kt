package com.miyagi.shashin.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.repository.UserRepository
import com.miyagi.shashin.util.TextUtils
import jakarta.transaction.Transactional
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.logging.Level

// API tests that require image scans
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class APITests: BaseSeleniumTests() {

    private var superId: Int? = null
    private var userId: Int? = null
    private var mockMvc: MockMvc? = null

    @LocalServerPort
    private val port = 0

    @Autowired
    private val context: WebApplicationContext? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    private var bcrypt = BCryptPasswordEncoder()

    @BeforeEach
    @Transactional
    fun setup() {
        val adminObj = User()
        adminObj.setUsername("testsuper")
        var encodedPassword: String = bcrypt.encode("testsuper")
        adminObj.setPassword(encodedPassword)
        adminObj.setAuthority("ROLE_SUPER")
        adminObj.setIsAuthorized(true)
        adminObj.setApikey("00000000-00000000-00000000-00000000")
        userRepository?.save(adminObj)
        superId = adminObj.getId()

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
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()

        this.driver?.get("http://localhost:$port/users/login")
        //println(this.driver?.pageSource)
        val username = this.driver!!.findElement(By.id("username"))
        val password = this.driver!!.findElement(By.id("password"))
        val rememberMe = this.driver!!.findElement(By.id("remember-me"))
        val login = this.driver!!.findElement(By.id("submit-loginreg"))
        rememberMe.click()
        username.sendKeys("testsuper")
        password.sendKeys("testsuper")

        println("username value: ${username.getDomProperty("value")}")

        login.click()

        this.driver!!.get("http://localhost:$port/settings")

        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        // Get test image data and populate in settings
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("testscreen.jpg")!!
        val testImageFile = File(testImageUrl.file)

        var mediaDirTextArea = this.driver!!.findElement(By.id("mediaDirTextArea"))
        mediaDirTextArea.click()
        mediaDirTextArea.sendKeys(testImageFile.parent+"/subdir")

        println("mediaDirTextArea textarea text: ${testImageFile.parent+"/subdir"}")
        println("mediaDirTextArea value: ${mediaDirTextArea.getDomProperty("value")}")

        var mediaExcludeDirTextArea = this.driver!!.findElement(By.id("mediaExcludeDirTextArea"))
        mediaExcludeDirTextArea.click()
        mediaExcludeDirTextArea.sendKeys("${testImageFile.parent}/subdir/dice.mp4")

        println("mediaExcludeDirTextArea textarea text: ${testImageFile.parent+"/subdir/dice.mp4"}")
        println("mediaExcludeDirTextArea value: ${mediaExcludeDirTextArea.getDomProperty("value")}")

        val objectDetectionCheck = this.driver!!.findElement(By.id("objectDetection"))
        objectDetectionCheck.sendKeys(Keys.SPACE)
        if (!objectDetectionCheck.isSelected) {
            objectDetectionCheck.click()
        }

        val saveSettings = this.driver!!.findElement(By.id("saveSettings"))
        saveSettings.submit()
//        println(this.driver?.pageSource)

        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Settings saved')]")))

        // Scan new image
        this.driver!!.get("http://localhost:$port/settings/scan")
        val scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        val scanPhotos = this.driver!!.findElement(By.id("scanPhotos"))
        scanPhotos.click()

        // Indicates scanning something
        var scanBeforeAfter: WebElement? = null
        var startTime = System.currentTimeMillis()
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }

        Thread.sleep(this.elementScanTimeoutMillis.toLong())
        this.logger.log(Level.INFO, "APITests - Photos scanned.")
    }

    @Test
    @Throws(Exception::class)
    fun browseControllerAPITest() {
        val webClient = WebClient.create("http://localhost:$port/")

        var jsonString: String? = null
        var jsonNode: JsonNode? = null
        val mapper = ObjectMapper()

        val response = webClient.get()
            .uri("/api/v1/recent/0")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        Assertions.assertTrue(jsonNode!!.has("metadataList"))
        Assertions.assertTrue(jsonNode.get("metadataList").get(0).get("id").textValue() != "")
    }

    @Test
    @Throws(Exception::class)
    fun recentImageAPITest() {
        val webClient = WebClient.create("http://localhost:$port/")

        var jsonString: String? = null
        var jsonNode: JsonNode? = null
        val mapper = ObjectMapper()

        // Get metadata
        val response = webClient.get()
            .uri("/api/v1/recent/0")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        // Get image metadata ID
        val metadataList = jsonNode!!.get("metadataList").toList()
        var metadataId = ""
        for (metadata in metadataList) {
            if (metadata.get("type").textValue().contains("image/")) {
                metadataId = metadata.get("id").textValue()
                break
            }
        }

        Assertions.assertTrue(metadataId != "")

        val imageResponse = mockMvc!!.perform(
            get("/api/v1/image/$metadataId")
        )

        Assertions.assertTrue(imageResponse.andReturn().response.contentType!!.contains("image/"))
    }

    @Test
    @Throws(Exception::class)
    fun metadataKeywordAPITest() {
        val webClient = WebClient.create("http://localhost:$port/")

        var jsonString: String?
        var jsonNode: JsonNode? = null
        val mapper = ObjectMapper()

        var response = webClient.get()
            .uri("/api/v1/recent/0")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        Assertions.assertTrue(jsonNode!!.has("metadataList"))
        Assertions.assertTrue(jsonNode.get("metadataList").get(0).get("id").textValue() != "")
        val metadataId = jsonNode.get("metadataList").get(0).get("id").textValue()
        val metadataObj = metadataRepository?.findById(metadataId)
        var metadata: Metadata = Metadata()
        if (metadataObj != null && metadataObj.isPresent) {
            metadata = metadataObj.get()
            metadata.setLat("1.1111")
            metadata.setLng("1.1111")
            metadataRepository?.save(metadata)
        }

        // Get metadata
        response = webClient.get()
            .uri("/api/v1/mapdata/keywords")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        Assertions.assertTrue(jsonNode?.get("keywordMap")?.get(metadataId)?.textValue() != "")
    }

    @Test
    @Throws(Exception::class)
    fun rescanAPITest() {
        val webClient = WebClient.create("http://localhost:$port/")

        var jsonString: String? = null
        var jsonNode: JsonNode? = null
        val mapper = ObjectMapper()

        var response = webClient.get()
            .uri("/api/v1/recent/0")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        Assertions.assertTrue(jsonNode!!.has("metadataList"))

        val metadataList = jsonNode.get("metadataList").toList()
        var metadataId = ""
        var metadata: Metadata? = null
        var gson = Gson()
        for (metadataJson in metadataList) {
            if (metadataJson.get("type").textValue().contains("image/")) {
                val metadataJsonObj = ObjectMapper().writeValueAsString(metadataJson)
                metadata = gson.fromJson(metadataJsonObj, Metadata::class.java)
                metadataId = metadata.getId()

                break
            }
        }

        Assertions.assertTrue(metadataId != "")
        Assertions.assertTrue(metadata != null)

        var map = mutableMapOf<String, Any>()
        // Capture original dates
        val originalYear = metadata!!.getYear()
        val originalMonth = metadata.getMonth()
        val originalDay = metadata.getDay()

        // Update metadata with different dates
        map["id"] = metadataId
        map["year"] = "2001"
        map["month"] = "1"
        map["day"] = "2"
        map["time"] = "23:01:02"
        map["offset"] = "+02:00"
        map["keywords"] = ""
        map["latlng"] = ""
        map["title"] = if (metadata.getTitle() == null) "" else metadata.getTitle().toString()
        map["description"] = if (metadata.getDescription() == null) "" else metadata.getDescription().toString()
        map["albumnames"] = ""
        map["tagpeople"] = ""
        map["hidden"] = "0"
        map["isObject"] = "0"
        map["camera"] = if (metadata.getCamera() == null) "" else metadata.getCamera().toString()
        map["lens"] = if (metadata.getLens() == null) "" else metadata.getLens().toString()
        map["duration"] = if (metadata.getDuration() == null) "" else metadata.getDuration().toString()

        response = webClient.put()
            .uri("/api/v1/update/metadata/${metadataId}")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .bodyValue(gson.toJson(map).toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        // Check if dates differ from original
        response = webClient.get()
            .uri("/api/v1/metadata/${metadataId}")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        Assertions.assertTrue(jsonNode!!.has("metadata"))
        Assertions.assertTrue(jsonNode.get("metadata").get("year").toString().toInt() != originalYear)
        Assertions.assertTrue(jsonNode.get("metadata").get("month").toString().toInt() != originalMonth)
        Assertions.assertTrue(jsonNode.get("metadata").get("day").toString().toInt() != originalDay)

        // Rescan
        map = mutableMapOf<String, Any>()
        map["metadataIdList"] = arrayOf(metadataId)

        response = webClient.post()
            .uri("/api/v1/rescan/metadata")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .bodyValue(gson.toJson(map).toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        // Check if dates are the same as original
        response = webClient.get()
            .uri("/api/v1/metadata/${metadataId}")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        Assertions.assertTrue(jsonNode!!.has("metadata"))
        Assertions.assertTrue(jsonNode.get("metadata").get("year").toString().toInt() == originalYear)
        Assertions.assertTrue(jsonNode.get("metadata").get("month").toString().toInt() == originalMonth)
        Assertions.assertTrue(jsonNode.get("metadata").get("day").toString().toInt() == originalDay)
    }

    @Test
    @Throws(Exception::class)
    fun corsAPITest() {
        val webClient = WebClient.create("http://localhost:$port/")

        var jsonString: String? = null
        var jsonNode: JsonNode? = null
        val mapper = ObjectMapper()

        // Get metadata
        val response = webClient.get()
            .uri("/api/v1/recent/0")
            .header("x-api-key", "00000000-00000000-00000000-00000000")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
            .retrieve()
            .bodyToMono(String::class.java)
            .block()

        jsonString = response

        if (!jsonString.isNullOrBlank()) {
            jsonNode = mapper.readTree(jsonString)
        }

        // Get image metadata ID
        val metadataList = jsonNode!!.get("metadataList").toList()
        var metadataId = ""
        for (metadata in metadataList) {
            if (metadata.get("type").textValue().contains("image/")) {
                metadataId = metadata.get("id").textValue()
                break
            }
        }

        Assertions.assertTrue(metadataId != "")

        // Perform options call to test CORS response
        mockMvc!!.perform(
            options("/api/v1/user/self")
            .header("Content-Type", "application/json")
            .header("X-Api-Key", "00000000-00000000-00000000-00000001")
            .header("Origin", "http://111.111.0.111:9999")
            .header("Access-Control-Request-Method", "GET")
        )
        .andExpect(status().isOk)
        .andExpect(MockMvcResultMatchers.header().stringValues("Access-Control-Allow-Origin", "*"))
        .andExpect(MockMvcResultMatchers.header().stringValues("Access-Control-Allow-Methods", "GET"))

        val selfObjJson = mockMvc!!.perform(
            get("/api/v1/user/self")
            .header("Content-Type", "application/json")
            .header("X-Api-Key", "00000000-00000000-00000000-00000001")
            .header("Origin", "http://111.111.0.111:9999")
            .header("Access-Control-Request-Method", "GET")
        )
        val selfObj = selfObjJson.andReturn().response.contentAsString
        var gson = Gson()
        val userObj = gson.fromJson(selfObj, User::class.java)
        val username = userObj.getUsername()
        Assertions.assertEquals(username, "testuser")

        mockMvc!!.perform(
            options("/api/v1/image/$metadataId")
            .header("Content-Type", "application/json")
            .header("X-Api-Key", "00000000-00000000-00000000-00000001")
            .header("Origin", "http://111.111.0.111:9999")
            .header("Access-Control-Request-Method", "GET")
        )
        .andExpect(status().isOk)
        .andExpect(MockMvcResultMatchers.header().stringValues("Access-Control-Allow-Origin", "*"))
        .andExpect(MockMvcResultMatchers.header().stringValues("Access-Control-Allow-Methods", "GET"))
    }

    companion object {

        @Autowired
        private val settingsRepository: SettingsRepository? = null

        private fun saveSettings() {
            var settings = settingsRepository?.findFirstByOrderByIdAsc()
            if (settings == null) {
                settings = Settings()
            }
            settings.setSearchHistoryLimit(10)
            settings.setQueryLimit(30)
            settings.setObjectRecognitionConfidenceThreshold(0.20.toString())
            settings.setRecognitionConfidenceThreshold(0.20.toString())
            settings.setObjectDetection(true)
            settings.setTrainingDataLimit(10)
            settings.setScheduledTime("2:00")
            settings.setCompreFaceServer("")
            settings.setCompreFaceKey("")
            settings.setMatchScanLimit(10)
            settings.setNotificationLimit(10)
            settings.setPort(6624.toString())
            settings.setScanAutomatically(false)
            settings.setScheduledMatching(false)
            settings.setCreatedAt(TextUtils.getCurrentTimestamp())
            settings.setModifiedAt(TextUtils.getCurrentTimestamp())
            settingsRepository?.save(settings)
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            saveSettings()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            settingsRepository?.deleteAll()
        }
    }
}