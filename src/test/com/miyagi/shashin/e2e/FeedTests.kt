package com.miyagi.shashin.e2e

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.logging.Level
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.Int

// API tests that require image scans
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FeedTests: BaseSeleniumTests() {
    private var superId: Int? = null
    private var mockMvc: MockMvc? = null

    @LocalServerPort
    private val port = 0

    @Autowired
    private val context: WebApplicationContext? = null

    @Autowired
    private val userRepository: UserRepository? = null

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
        login.click()

        this.driver!!.get("http://localhost:$port/settings")

        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        // Get test image data and populate in settings
        val classLoader = javaClass.classLoader
        val testImageUrl: URL = classLoader.getResource("testscreen.jpg")!!
        val testImageFile = File(testImageUrl.file)

        val mediaDirTextAreaText = testImageFile.parent+"/subdir"
        var mediaDirTextArea = this.driver!!.findElement(By.id("mediaDirTextArea"))
        mediaDirTextArea.click()
        mediaDirTextArea.sendKeys(mediaDirTextAreaText)

//        println("mediaDirTextArea textarea text: $mediaDirTextAreaText")
//        println("mediaDirTextArea value: ${mediaDirTextArea.getDomAttribute("value")}")

        val mediaExcludeDirTextAreaText = testImageFile.parent+"/subdir/dice.mp4"
        var mediaExcludeDirTextArea = this.driver!!.findElement(By.id("mediaExcludeDirTextArea"))
        mediaExcludeDirTextArea.click()
        mediaExcludeDirTextArea.sendKeys(mediaExcludeDirTextAreaText)

//        println("mediaExcludeDirTextArea textarea text: $mediaExcludeDirTextAreaText")
//        println("mediaExcludeDirTextArea value: ${mediaExcludeDirTextArea.getDomAttribute("value")}")

        var objectDetectionCheck = this.driver!!.findElement(By.id("objectDetection"))
        objectDetectionCheck.sendKeys(Keys.SPACE)
        if (!objectDetectionCheck.isSelected) {
            objectDetectionCheck.click()
        }

        var saveSettings = this.driver!!.findElement(By.id("saveSettings"))
        saveSettings.submit()
//        println(this.driver?.pageSource)

        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("topCenter_ToastMessage_2")))

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
        this.logger.log(Level.INFO, "FeedTests - Photos scanned.")
    }

    @Test
    @Throws(Exception::class)
    fun viewFeedAsSuperTest() {
        val adminRssUrlString = "http://localhost:$port/00000000-00000000-00000000-00000000/rss"
        val adminRssUrl = URL(adminRssUrlString)

        // Parse XML from URL stream
        var factory = DocumentBuilderFactory.newInstance()
        var builder = factory.newDocumentBuilder()
        var doc: Document = builder.parse(adminRssUrl.openStream())

        doc.documentElement.normalize()

        val channelList: NodeList = doc.getElementsByTagName("channel")
        for (i in 0 until channelList.length) {
            val channelNode: Node = channelList.item(i)

            if (channelNode.nodeType == Node.ELEMENT_NODE) {
                val channelElement = channelNode as Element

                val title = channelElement.getElementsByTagName("title").item(0).textContent
                Assertions.assertEquals("Shashin RSS Feed", title)
            }
        }

        val itemList: NodeList = doc.getElementsByTagName("item")
        var itemFound = false
        for (i in 0 until itemList.length) {
            val item: Element = itemList.item(i) as Element

            val itemTitle = item.getElementsByTagName("title").item(0).textContent
            if (itemTitle == "tablecup.jpg") {
                itemFound = true
                break
            }
        }

        Assertions.assertTrue(itemFound)

        val adminAtomUrlString = "http://localhost:$port/00000000-00000000-00000000-00000000/atom"
        val adminAtomUrl = URL(adminAtomUrlString)

        // Parse XML from URL stream
        factory = DocumentBuilderFactory.newInstance()
        builder = factory.newDocumentBuilder()
        doc = builder.parse(adminAtomUrl.openStream())

        doc.documentElement.normalize()

        val feedList: NodeList = doc.getElementsByTagName("feed")
        for (i in 0 until feedList.length) {
            val feedNode: Node = feedList.item(i)

            if (feedNode.nodeType == Node.ELEMENT_NODE) {
                val feedElement = feedNode as Element

                val title = feedElement.getElementsByTagName("title").item(0).textContent
                Assertions.assertEquals("Shashin ATOM Feed", title)
            }
        }

        val entryList: NodeList = doc.getElementsByTagName("entry")
        itemFound = false
        for (i in 0 until entryList.length) {
            val item: Element = entryList.item(i) as Element

            val itemTitle = item.getElementsByTagName("title").item(0).textContent
            if (itemTitle == "tablecup.jpg") {
                itemFound = true
                break
            }
        }

        Assertions.assertTrue(itemFound)
    }
}