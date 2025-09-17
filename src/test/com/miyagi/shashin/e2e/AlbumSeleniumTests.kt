package com.miyagi.shashin.e2e

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
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
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.logging.Level

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AlbumSeleniumTests: BaseSeleniumTests() {

    private var metadataId: String? = null
    private var superId: Int? = null
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
        val superObj = User()
        superObj.setUsername("testsuper")
        var encodedPassword: String = bcrypt.encode("testsuper")
        superObj.setPassword(encodedPassword)
        superObj.setAuthority("ROLE_SUPER")
        superObj.setIsAuthorized(true)
        superObj.setApikey("00000000-00000000-00000000-00000000")
        userRepository?.save(superObj)
        superId = superObj.getId()

        val userObj = User()
        userObj.setUsername("testuser")
        encodedPassword = bcrypt.encode("testuser")
        userObj.setPassword(encodedPassword)
        userObj.setAuthority("ROLE_USER")
        userObj.setIsAuthorized(true)
        userObj.setApikey("00000000-00000000-00000000-00000001")
        userRepository?.save(userObj)
        userId = userObj.getId()

        val adminObj = User()
        adminObj.setUsername("testadmin")
        encodedPassword = bcrypt.encode("testadmin")
        adminObj.setPassword(encodedPassword)
        adminObj.setAuthority("ROLE_ADMIN")
        adminObj.setIsAuthorized(true)
        adminObj.setApikey("00000000-00000000-00000000-00000002")
        userRepository?.save(adminObj)
        adminId = adminObj.getId()

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()

        this.driver?.get("http://localhost:$port/users/login")
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
        this.logger.log(Level.INFO, "AlbumSeleniumTest - Photos scanned.")

        // Check if UUID present
        this.driver!!.get("http://localhost:$port/timeline")
        Thread.sleep(this.elementScanTimeoutMillis.toLong())
        this.logger.log(Level.INFO, "AlbumSeleniumTest - Redirected to timeline.")
        val scrollContainer = this.driver!!.findElement(By.id("infinite-scroll-gallery"))
        val spanContainerEl = scrollContainer.findElement(By.xpath("./span[1]"))
        // Get the date id
        var dateId = ""
        if (spanContainerEl !== null && spanContainerEl.getDomAttribute("id") !== null) {
            if (spanContainerEl.getDomAttribute("id")?.contains("container") == true) {
                dateId = spanContainerEl.getDomAttribute("id")?.substringAfter("container_").toString()
            } else if (spanContainerEl.getDomAttribute("id")?.contains("tail") == true) {
                dateId = spanContainerEl.getDomAttribute("id")?.substringAfter("tail_").toString()
            } else if (spanContainerEl.getDomAttribute("id")?.contains("amp") == true) {
                dateId = spanContainerEl.getDomAttribute("id")?.substringAfter("amp_").toString()
            }
        }
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("row$dateId")))
        val parentEl = this.driver!!.findElement(By.id("row$dateId"))
        val childEl = parentEl.findElement(By.xpath("./div[1]"))
        val imageId = childEl.getDomAttribute("id")
        metadataId = imageId?.substringAfter("photoThumbnailContainer")

        // Check image src
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("img[src^='/api/v1/thumbnails']")))
        val imageEl = this.driver!!.findElement(By.id("image$metadataId"))

        // TODO: don't like sleep
        this.logger.log(Level.INFO, "3 sec sleep.")
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val timelineBottomLeft = "tnbl$metadataId"
        val timelineBottomLeftEl = this.driver!!.findElement(By.id(timelineBottomLeft))
        val metadataModalEdit = this.driver!!.findElement(By.id("metadataModalEdit$metadataId"))

        //Creating object of an Actions class
        val action = Actions(this.driver!!)

        //Performing the mouse hover action on the target element.
        action.moveToElement(imageEl).perform()
        action.moveToElement(timelineBottomLeftEl).perform()
        action.moveToElement(metadataModalEdit).perform()
        metadataModalEdit.click()

        this.logger.log(Level.INFO, "Timeline edit button clicked.")
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("metadataModalTitle")))

        //Save album in timeline
        val albumNamesInput = this.driver!!.findElement(By.id("albumnames"))
        albumNamesInput.sendKeys("testalbum")
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.elementToBeClickable(By.id("saveMetadata"))).click()

//        println(this.driver?.pageSource)

        Thread.sleep(this.elementScanTimeoutMillis.toLong())
        this.logger.log(Level.INFO, "Relocating to albums view.")
        this.driver!!.get("http://localhost:$port/albums")

        //Share album with testuser
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class=\"card\"][1]")))

        val albumCard = this.driver!!.findElement(By.xpath("//div[@class=\"card\"][1]"))
        val albumLink = albumCard.findElement(By.xpath("./a[1]"))
        val albumIdentifier = albumLink.getDomAttribute("id")
        albumId = albumIdentifier?.substringAfter("album")?.toInt()

        val shareLink = this.driver!!.findElement(By.id("share$albumId"))
        shareLink.click()

        this.logger.log(Level.INFO, "Share link created.")

        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("id-$userId-$albumId")))
        val userShareCheckbox = this.driver!!.findElement(By.id("id-$userId-$albumId"))
        userShareCheckbox.click()

        val saveUserShare = this.driver!!.findElement(By.id("saveUserShare"))
        // Not working in CI after making changes to size of modal, using enter key instead
        // https://github.com/MichaelYagi/shashin/commit/d5079f8e169b653127dd707e180af2d298d6d5e4
//        saveUserShare.click()
        saveUserShare.sendKeys(Keys.RETURN)

        this.logger.log(Level.INFO, "Share link saved.")

        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("albumsModalStatus")))
    }

    @Test
    @Throws(Exception::class)
    fun shouldViewInAlbumAsSuper() {
        this.driver!!.get("http://localhost:$port/albums")

        var isPresent = this.driver!!.findElements(By.id("share$albumId")).isNotEmpty()
        Assertions.assertTrue(isPresent)

        isPresent = this.driver!!.findElements(By.id("trash$albumId")).isNotEmpty()
        Assertions.assertTrue(isPresent)

        isPresent = this.driver!!.findElements(By.id("edit$albumId")).isNotEmpty()
        Assertions.assertTrue(isPresent)

        isPresent = this.driver!!.findElements(By.id("comment$albumId")).isNotEmpty()
        Assertions.assertTrue(isPresent)

        // Test change album name
        val editAlbumEl = this.driver!!.findElement(By.id("edit$albumId"))
        var scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        editAlbumEl.click()
        var startTime = System.currentTimeMillis()
        var scanBeforeAfter: WebElement? = null
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val albumEditName = this.driver!!.findElement(By.id("albumEditName"))

        albumEditName.clear()
        albumEditName.sendKeys("Album name update")
        startTime = System.currentTimeMillis()
        var elementHasClass = elementHasClass(this.driver!!.findElement(By.id("editAlbumNameStatus")),"bi-check-circle")
        val albumEditNameButton = this.driver!!.findElement(By.id("editAlbum"))

        // Same problem as above
//        albumEditNameButton.click()
        albumEditNameButton.sendKeys(Keys.RETURN)

        this.logger.log(Level.INFO, "Changed album name as admin.")
        while (!elementHasClass || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            elementHasClass = elementHasClass(this.driver!!.findElement(By.id("editAlbumNameStatus")),"bi-check-circle")
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())
        this.driver?.get("http://localhost:$port/albums")
        val albumNameEl = this.driver!!.findElement(By.id("albumName$albumId"))
        Assertions.assertEquals("Album name update",albumNameEl.text)

        // Test share album
        val shareAlbumEl = this.driver!!.findElement(By.id("share$albumId"))
        scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        shareAlbumEl.click()
        startTime = System.currentTimeMillis()
        scanBeforeAfter = null
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val generateShareAlbumEl = this.driver!!.findElement(By.id("generateLink"))
        startTime = System.currentTimeMillis()
        elementHasClass = elementHasClass(this.driver!!.findElement(By.id("albumsModalStatus")),"bi-check-circle")
        generateShareAlbumEl.click()
        this.logger.log(Level.INFO, "Shared album as admin.")
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("shareConfirmationModalTitle")))

        val shareConfirmation = this.driver!!.findElement(By.id("shareConfirmation"))
        shareConfirmation.click()

        while (!elementHasClass || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            elementHasClass = elementHasClass(this.driver!!.findElement(By.id("albumsModalStatus")),"bi-check-circle")
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val fullShareLink = this.driver!!.findElement(By.id("fullShareLink"))
        val linkEl = fullShareLink.findElement(By.xpath("./a[1]"))
        this.driver?.get(linkEl.text)

        val titleHeader = this.driver!!.findElement(By.id("albumNameTitle"))
        Assertions.assertEquals("Album name update",titleHeader.text)

        this.driver?.get("http://localhost:$port/notifications")

        // Test delete album
        this.driver?.get("http://localhost:$port/albums")

        val deleteAlbumEl = this.driver!!.findElement(By.id("trash$albumId"))
        deleteAlbumEl.click()

        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("trashModalLabel")))

        val deleteAlbumButton = this.driver!!.findElement(By.id("deleteAlbum"))
        deleteAlbumButton.click()
        this.logger.log(Level.INFO, "Deleted album as admin.")
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val msgEl = this.driver!!.findElement(By.id("msg"))
        Assertions.assertEquals("Nothing to see here",msgEl.text)
    }

    @Test
    @Throws(Exception::class)
    fun shouldViewInAlbumAndCommentAsUser() {
        //Login as testuser
        this.driver!!.get("http://localhost:$port/users/logout")
        // Logging out redirects to login page
        //println(this.driver?.pageSource)
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("remember-me")))
        val username = this.driver!!.findElement(By.id("username"))
        val password = this.driver!!.findElement(By.id("password"))
        val rememberMe = this.driver!!.findElement(By.id("remember-me"))
        val login = this.driver!!.findElement(By.id("submit-loginreg"))
        rememberMe.click()
        username.sendKeys("testuser")
        password.sendKeys("testuser")
        login.click()

        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        Assertions.assertEquals("http://localhost:$port/albums", this.driver!!.currentUrl)

        var isPresent = this.driver!!.findElements(By.id("share$albumId")).isNotEmpty()
        Assertions.assertFalse(isPresent)

        isPresent = this.driver!!.findElements(By.id("trash$albumId")).isNotEmpty()
        Assertions.assertFalse(isPresent)

        isPresent = this.driver!!.findElements(By.id("edit$albumId")).isNotEmpty()
        Assertions.assertFalse(isPresent)

        val postCommentElement = this.driver!!.findElements(By.id("comment$albumId"))
        isPresent = postCommentElement.isNotEmpty()
        Assertions.assertTrue(isPresent)

        // Post comment on albums view
        postCommentElement[0].click()
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("commentModalLabel")))
        var scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        val commentTextArea = this.driver!!.findElement(By.id("commentText"))
        commentTextArea.click()
        commentTextArea.sendKeys("Test comment")

        val saveComment = this.driver!!.findElement(By.id("saveCommentAlbum"))
        saveComment.click()

        this.logger.log(Level.INFO, "Shared comment as user.")

        var scanBeforeAfter: WebElement? = null
        var startTime = System.currentTimeMillis()
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        this.driver?.get("http://localhost:$port/albums")
        val commentCountEl = this.driver!!.findElement(By.id("commentcount$albumId"))
        val commentCountStr = commentCountEl.text
        Assertions.assertTrue(commentCountStr.toInt() > 0)

        val postCommentEl = this.driver!!.findElement(By.id("comment$albumId"))
        postCommentEl.click()
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("commentModalLabel")))

        // Check comment
        val commentList = this.driver!!.findElement(By.id("commentList"))
        val commentEl = commentList.findElement(By.xpath("./li[1]"))
        val commentId = commentEl.getDomAttribute("id")?.substringAfter("comment")

        Assertions.assertTrue(this.driver!!.findElement(By.id("commentcontent$commentId")).text.contains("Test comment"))

        // Update comment
        val editCommentEl = this.driver!!.findElement(By.id("editcomment$commentId"))
        scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        editCommentEl.click()
        this.logger.log(Level.INFO, "Shared comment edited as user.")
        startTime = System.currentTimeMillis()
        scanBeforeAfter = null
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val editCommentTextArea = this.driver!!.findElement(By.id("commenttext$commentId"))
        editCommentTextArea.click()
        editCommentTextArea.sendKeys("Test update")

        val updateComment = this.driver!!.findElement(By.id("updateCommentAlbum"))
        scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        updateComment.click()

        scanBeforeAfter = null
        startTime = System.currentTimeMillis()
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        Assertions.assertTrue(this.driver!!.findElement(By.id("commentcontent$commentId")).text.contains("Test update"))

        // Delete comment
        val deleteCommentEl = this.driver!!.findElement(By.id("deletecomment$commentId"))
        scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        deleteCommentEl.click()
        this.logger.log(Level.INFO, "Deleted comment as user.")
        startTime = System.currentTimeMillis()
        scanBeforeAfter = null
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        Assertions.assertTrue(this.driver!!.findElements(By.id("commentcontent$commentId")).isNotEmpty())

        this.driver?.get("http://localhost:$port/albums")
        val albumLink = this.driver!!.findElement(By.id("album$albumId"))
        albumLink.click()

        Assertions.assertEquals("testalbum - Shashin", this.driver!!.title)
    }

    @Test
    @Throws(Exception::class)
    fun shouldViewInAlbumAsAdmin() {
        //Login as testadmin
        this.driver!!.get("http://localhost:$port/users/logout")
        // Logging out redirects to login page
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("remember-me")))
        val username = this.driver!!.findElement(By.id("username"))
        val password = this.driver!!.findElement(By.id("password"))
        val rememberMe = this.driver!!.findElement(By.id("remember-me"))
        val login = this.driver!!.findElement(By.id("submit-loginreg"))
        rememberMe.click()
        username.sendKeys("testadmin")
        password.sendKeys("testadmin")
        login.click()

        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        Assertions.assertEquals("http://localhost:$port/timeline", this.driver!!.currentUrl)
        Thread.sleep(this.elementScanTimeoutMillis.toLong())
        this.driver?.get("http://localhost:$port/albums")
        Assertions.assertEquals("http://localhost:$port/albums", this.driver!!.currentUrl)

        var isPresent = this.driver!!.findElements(By.id("share$albumId")).isNotEmpty()
        Assertions.assertTrue(isPresent)

        isPresent = this.driver!!.findElements(By.id("trash$albumId")).isNotEmpty()
        Assertions.assertTrue(isPresent)

        isPresent = this.driver!!.findElements(By.id("edit$albumId")).isNotEmpty()
        Assertions.assertTrue(isPresent)

        val postCommentElement = this.driver!!.findElements(By.id("comment$albumId"))
        isPresent = postCommentElement.isNotEmpty()
        Assertions.assertTrue(isPresent)

        // Post comment on albums view
        postCommentElement[0].click()
        WebDriverWait(this.driver, Duration.ofSeconds(this.elementWaitSeconds)).until(ExpectedConditions.visibilityOfElementLocated(By.id("commentModalLabel")))
        var scanBeforeBody = this.driver!!.findElement(By.tagName("body"))
        val commentTextArea = this.driver!!.findElement(By.id("commentText"))
        commentTextArea.click()
        commentTextArea.sendKeys("Test admin comment")

        val saveComment = this.driver!!.findElement(By.id("saveCommentAlbum"))
        saveComment.click()

        this.logger.log(Level.INFO, "Shared comment as admin.")

        var scanBeforeAfter: WebElement? = null
        var startTime = System.currentTimeMillis()
        while (scanBeforeBody != scanBeforeAfter || (System.currentTimeMillis()-startTime)<this.elementScanTimeoutMillis) {
            scanBeforeAfter = this.driver!!.findElement(By.tagName("body"))
        }
        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        this.driver?.get("http://localhost:$port/albums")
        val commentCountEl = this.driver!!.findElement(By.id("commentcount$albumId"))
        val commentCountStr = commentCountEl.text
        Assertions.assertTrue(commentCountStr.toInt() > 0)

        //Delete album as admin
        val trashEl = this.driver!!.findElement(By.id("trash$albumId"))
        trashEl.click()

        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val confirmButton = this.driver!!.findElement(By.id("deleteAlbum"))
        confirmButton.sendKeys(Keys.RETURN)

        Thread.sleep(this.elementScanTimeoutMillis.toLong())

        val msgEl = this.driver!!.findElement(By.id("msg"))
        Assertions.assertEquals("Nothing to see here",msgEl.text)
    }

    private fun elementHasClass(element: WebElement, active: String?): Boolean {
        return element.getDomAttribute("class")!!.contains(active!!)
    }
}