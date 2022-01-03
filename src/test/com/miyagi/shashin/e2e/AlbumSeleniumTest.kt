package com.miyagi.shashin.e2e

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Level


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AlbumSeleniumTest: BaseSeleniumTests() {

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
        adminObj.setAuthority("ROLE_ADMIN")
        adminObj.setIsAllowed(true)
        userRepository?.save(adminObj)
        adminId = adminObj.getId()

        val userObj = User()
        userObj.setUsername("testuser")
        encodedPassword = bcrypt.encode("testuser")
        userObj.setPassword(encodedPassword)
        userObj.setAuthority("ROLE_USER")
        userObj.setIsAllowed(true)
        userRepository?.save(userObj)
        userId = userObj.getId()

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()

        this.driver?.get("http://localhost:$port/users/login")
        //println(this.driver?.pageSource)
        var username = this.driver!!.findElement(By.id("username"))
        var password = this.driver!!.findElement(By.id("password"))
        var login = this.driver!!.findElement(By.tagName("button"))
        username.sendKeys("testadmin")
        password.sendKeys("testadmin")
        login.click()

        this.driver!!.get("http://localhost:$port/settings")

        // Get test image data and populate in settings
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("testscreen.jpg")
        val testImageFile = File(testImageUrl.file)
        val mediaDirTextArea = this.driver!!.findElement(By.id("mediaDirTextArea"))
        mediaDirTextArea.sendKeys(testImageFile.parent)
        this.logger.log(Level.INFO, "AlbumSeleniumTest - Media Directory ${testImageFile.parent} saved.")

        val saveSettings = this.driver!!.findElement(By.id("saveSettings"))
        saveSettings.click()
        WebDriverWait(this.driver, 30).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Settings saved')]")))

        // Scan new image
        this.driver!!.get("http://localhost:$port/settings/scan")
        val scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        val scanPhotos = this.driver!!.findElement(By.id("scanPhotos"))
        scanPhotos.click()

        // Indicates scanning something
        var scanBeforeAfter: WebElement? = null
        var startTime = System.currentTimeMillis()
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<30000) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        this.logger.log(Level.INFO, "AlbumSeleniumTest - Photos scanned.")

        // Check if UUID present
        this.driver!!.get("http://localhost:$port/timeline")
        this.logger.log(Level.INFO, "AlbumSeleniumTest - Redirected to timeline.")
        val scrollContainer = this.driver!!.findElement(By.id("infinite-scroll-gallery"))
        val spanContainerEl = scrollContainer.findElement(By.xpath("./span[1]"))
        val dateId = spanContainerEl.getAttribute("id").substringAfter("container_")
        val parentEl = this.driver!!.findElement(By.id("row$dateId"))
        val childEl = parentEl.findElement(By.xpath("./div[1]"))
        val imageId = childEl.getAttribute("id")
        metadataId = imageId.substringAfter("photoThumbnailContainer")

        // Check image src
        WebDriverWait(this.driver, 30).until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("img[src^='/api/v1/thumbnails']")))
//        println(this.driver?.pageSource)
        val imageEl = this.driver!!.findElement(By.id("image$metadataId"))

        //Creating object of an Actions class
        val action = Actions(this.driver)
        //Performing the mouse hover action on the target element.
        action.moveToElement(imageEl).perform()
        val timelineModalEdit = this.driver!!.findElement(By.id("timelineModalEdit$metadataId"))
        timelineModalEdit.click()

        WebDriverWait(this.driver, 30).until(ExpectedConditions.visibilityOfElementLocated(By.id("timelineModalTitle")))

        //Save album in timeline
        val albumNamesInput = this.driver!!.findElement(By.id("albumnames"))
        albumNamesInput.sendKeys("testalbum")
        val saveMetadataButton = this.driver!!.findElement(By.id("saveMetadata"))
        saveMetadataButton.click()

        startTime = System.currentTimeMillis()
        var elementHasClass = elementHasClass(this.driver!!.findElement(By.id("timelineModalStatus")),"bi-check-circle")
        while (!elementHasClass || (System.currentTimeMillis()-startTime)<30000) {
            elementHasClass = elementHasClass(this.driver!!.findElement(By.id("timelineModalStatus")),"bi-check-circle")
        }

        this.driver!!.get("http://localhost:$port/albums")

        //Share album with testuser
        WebDriverWait(this.driver, 30).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class=\"card\"][1]")))

        val albumCard = this.driver!!.findElement(By.xpath("//div[@class=\"card\"][1]"))
        val albumLink = albumCard.findElement(By.xpath("./a[1]"))
        val albumIdentifier = albumLink.getAttribute("id")
        albumId = albumIdentifier.substringAfter("album").toInt()

        val shareLink = this.driver!!.findElement(By.id("share$albumId"))
        shareLink.click()

        WebDriverWait(this.driver, 30).until(ExpectedConditions.visibilityOfElementLocated(By.id("id-$userId-$albumId")))
        val userShareCheckbox = this.driver!!.findElement(By.id("id-$userId-$albumId"))
        userShareCheckbox.click()

        val saveUserShare = this.driver!!.findElement(By.id("saveUserShare$albumId"))
        saveUserShare.click()

        WebDriverWait(this.driver, 30).until(ExpectedConditions.visibilityOfElementLocated(By.id("albumsModalStatus$albumId")))

        //Login as testuser
        this.driver!!.get("http://localhost:$port/users/logout")
        this.driver?.get("http://localhost:$port/users/login")
        username = this.driver!!.findElement(By.id("username"))
        password = this.driver!!.findElement(By.id("password"))
        login = this.driver!!.findElement(By.tagName("button"))
        username.sendKeys("testuser")
        password.sendKeys("testuser")
        login.click()
    }

    @Test
    @Throws(Exception::class)
    fun shouldViewInAlbumAsUser() {
        Assertions.assertEquals("http://localhost:$port/albums", this.driver!!.currentUrl)

        val albumLink = this.driver!!.findElement(By.id("album$albumId"))
        albumLink.click()

        val albumNameHeading = this.driver!!.findElement(By.id("albumNameHeading"))

        Assertions.assertEquals("testalbum", albumNameHeading.text)
    }

    private fun elementHasClass(element: WebElement, active: String?): Boolean {
        return element.getAttribute("class").contains(active!!)
    }
}