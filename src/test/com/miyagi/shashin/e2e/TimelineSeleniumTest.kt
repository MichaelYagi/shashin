package com.miyagi.shashin.e2e

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.logging.Level
import javax.imageio.ImageIO

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TimelineSeleniumTest: BaseSeleniumTests() {

    private var metadataId: String? = null
    private var adminId: Int? = null
    private var userId: Int? = null
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

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()

        this.driver?.get("http://localhost:$port/users/login")
        //println(this.driver?.pageSource)
        val username = this.driver!!.findElement(By.id("username"))
        val password = this.driver!!.findElement(By.id("password"))
        val rememberMe = this.driver!!.findElement(By.id("remember-me"))
        val login = this.driver!!.findElement(By.id("submit-loginreg"))
        rememberMe.click()
        username.sendKeys("testadmin")
        password.sendKeys("testadmin")
        login.click()

        Thread.sleep(this.elementScanTimeoutMillis.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun shouldUploadPhotoAndViewInTimeline() {
        Assertions.assertEquals("http://localhost:$port/timeline", this.driver!!.currentUrl)

        this.driver!!.get("http://localhost:$port/settings")

        // Get test image data and populate in settings
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("testscreen.jpg")!!
        val testImageFile = File(testImageUrl.file)
        val mediaDirTextArea = this.driver!!.findElement(By.id("mediaDirTextArea"))
        mediaDirTextArea.sendKeys(testImageFile.parent)
        val mediaExcludeDirTextArea = this.driver!!.findElement(By.id("mediaExcludeDirTextArea"))
        mediaExcludeDirTextArea.sendKeys(testImageFile.parent+"/subdir")

        val saveSettings = this.driver!!.findElement(By.id("saveSettings"))
        saveSettings.click()
//         println(this.driver?.pageSource)
//        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Settings saved')]")))
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        // Scan new image
        this.driver!!.get("http://localhost:$port/settings/scan")
        val scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        val scanPhotos = this.driver!!.findElement(By.id("scanPhotos"))
        scanPhotos.click()

        // Indicates scanning something
        var scanBeforeAfter: WebElement? = null
        val startTime = System.currentTimeMillis()
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())
        this.logger.log(Level.INFO, "TimelineSeleniumTest - Photos scanned.")

        // Check if UUID present
        this.driver!!.get("http://localhost:$port/timeline")
        //println(this.driver?.pageSource)
        val scrollContainer = this.driver!!.findElement(By.id("infinite-scroll-gallery"))
        val spanContainerEl = scrollContainer.findElement(By.xpath("./span[1]"))
        // Get the date id
        var dateId = ""
        if (spanContainerEl !== null && spanContainerEl.getAttribute("id") !== null) {
            if (spanContainerEl.getAttribute("id")?.contains("container") == true) {
                dateId = spanContainerEl.getAttribute("id")?.substringAfter("container_").toString()
            } else if (spanContainerEl.getAttribute("id")?.contains("tail") == true) {
                dateId = spanContainerEl.getAttribute("id")?.substringAfter("tail_").toString()
            } else if (spanContainerEl.getAttribute("id")?.contains("amp") == true) {
                dateId = spanContainerEl.getAttribute("id")?.substringAfter("amp_").toString()
            }
        }
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("row$dateId")))
        val parentEl = this.driver!!.findElement(By.id("row$dateId"))
        val childEl = parentEl.findElement(By.xpath("./div[1]"))
        val imageId = childEl.getAttribute("id")
        metadataId = imageId?.substringAfter("photoThumbnailContainer")

        Assertions.assertTrue(isUUID(metadataId!!))

        // Check image src
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("img[src^='/api/v1/thumbnails']")))
//        println(this.driver?.pageSource)
        val imageEl = this.driver!!.findElement(By.id("image$metadataId"))
        val imageSrc = imageEl.getAttribute("src")
        val imgSrcArray = imageSrc?.split("/")
        val length = imgSrcArray?.size

        var metadataId = ""
        if (length != null && length > 0) {
            val metadata = imgSrcArray[length-1]
            val metadataArray = metadata.split("?")
            metadataId = metadataArray[0]
        }
        Assertions.assertTrue(metadataId != "")
        var image: BufferedImage? = null
        try {
            val url = URL("http://localhost:$port/api/v1/image/$metadataId")
            image = ImageIO.read(url)
        } catch (_: IOException) {}
        Assertions.assertTrue(image?.type == BufferedImage.TYPE_3BYTE_BGR)
    }

    private fun isUUID(someUUID: String): Boolean {
        val isUuid = try {
            UUID.fromString(someUUID)
            true
        } catch (exception: IllegalArgumentException) {
            //handle the case where string is not valid UUID
            false
        }

        return isUuid
    }
}