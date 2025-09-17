package com.miyagi.shashin.integration

import com.miyagi.shashin.e2e.BaseSeleniumTests
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.MessageSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.io.IOException
import java.util.Locale

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TextTests: BaseSeleniumTests() {

    private var adminId: Int? = null
    private var mockMvc: MockMvc? = null

    @LocalServerPort
    private val port = 0

    @Autowired
    private val context: WebApplicationContext? = null

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    var messageSource: MessageSource? = null

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

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context!!)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()

        this.driver?.get("http://localhost:$port/users/login")
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
    fun jsAndKotlinTranslationsMatch() {
        val enProperties = java.util.Properties()
        try {
            java.io.FileInputStream("src/main/resources/messages.properties").use { fis ->
                enProperties.load(fis)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var locale = Locale("en")

        this.driver!!.get("http://localhost:$port/test")
        val js: JavascriptExecutor = this.driver as JavascriptExecutor

        val keyValTranslation: Map<String, String> = js.executeScript("return shashini18n;") as Map<String, String>
        Assertions.assertEquals(keyValTranslation.size, enProperties.stringPropertyNames().size)
        // Keys not present in either map
        Assertions.assertEquals((keyValTranslation.keys - enProperties.stringPropertyNames()).size, 0)
        Assertions.assertEquals((enProperties.stringPropertyNames() - keyValTranslation.keys).size, 0)

        // English
        var index = 0
        for (key in enProperties.stringPropertyNames()) {
            index++
//            var value = properties.getProperty(key)
//            println("Key: $key, Value: $value")

            var val1a = "Bob"
            var val1b = 5
            var val2 = "Alice"
            var val3 = "Max"
            var val4 = "Linda"
            var val5 = "Joe"
            var val6 = "Matthew"
            var val7 = "Jacob"
            var val8 = "Mike"
            var val9 = "Irene"

            var useInt = false

            if (key == "main.pages.albums.photo" ||
                key == "main.pages.people.items" ||
                key == "main.pages.albums.video" ||
                key == "main.pages.map.modal.result"
            ) {
                useInt = true
            }

            var jsTranslation = if (key == "main.pages.scan.training.similarity" || key == "main.pages.scan.training.similarity.label") {
                js.executeScript("const translatedValue = shashin.getTranslatedValue('" + key + "', '" + val1a + "', '" + val2 + "', '" + val3 + "', '" + val4 + "', '" + val5 + "', '" + val6 + "', '" + val7 + "', '" + val8 + "', '" + val9 + "'); return translatedValue;")
            } else if (key == "main.toast.topnav.selected" || key == "main.pages.match.info") {
                js.executeScript("const translatedValue = shashin.getTranslatedValue('" + key + "', " + val1b + "); return translatedValue;")
            } else if (key == "main.notification.comments.photo.commented") {
                js.executeScript("const translatedValue = shashin.getTranslatedValue('" + key + "', '" + val1a + "', '" + val2 + "', '" + val3 + "', '" + val4 + "'); return translatedValue;")
            } else if (key == "main.notification.comments.commented") {
                js.executeScript("const translatedValue = shashin.getTranslatedValue('"+key+"', '"+val1b+"', '"+val2+"', '"+val3+"'); return translatedValue;")
            } else if (key == "main.pages.matching.processing" || key == "main.notification.album.shared.access") {
                js.executeScript("const translatedValue = shashin.getTranslatedValue('"+key+"', '"+val1a+"', '"+val2+"', "+val1b+"); return translatedValue;")
            } else if (key == "main.pages.albums.photo" || key == "main.pages.people.items" || key == "main.pages.albums.video" || key == "main.pages.map.modal.result") {
                js.executeScript("const translatedValue = shashin.getTranslatedValue('"+key+"', '"+val1b+"', '"+val2+"'); return translatedValue;")
            } else {
                js.executeScript("const translatedValue = shashin.getTranslatedValue('"+key+"', '"+val1a+"', '"+val2+"'); return translatedValue;")
            }

//            println("JS translation:"+jsTranslation)

            var kotlinTranslation = if (key == "main.pages.scan.training.similarity" || key == "main.pages.scan.training.similarity.label") {
                messageSource?.getMessage(key, arrayOf(val1a, val2, val3, val4, val5, val6, val7, val8, val9), locale)
            } else if (key == "main.toast.topnav.selected" || key == "main.pages.match.info") {
                messageSource?.getMessage(key, arrayOf(val1b), locale)
            } else if (key == "main.notification.comments.photo.commented") {
                messageSource?.getMessage(key, arrayOf(val1a, val2, val3, val4), locale)
            } else if (key == "main.notification.comments.commented") {
                messageSource?.getMessage(key, arrayOf(val1b, val2, val3), locale)
            } else if (key == "main.pages.matching.processing" || key == "main.notification.album.shared.access") {
                messageSource?.getMessage(key, arrayOf(val1a, val2, val1b), locale)
            } else if (key == "main.pages.albums.photo" || key == "main.pages.people.items" || key == "main.pages.albums.video" || key == "main.pages.map.modal.result") {
                val mixedArray: Array<Any> = arrayOf(val1b, val2)
                messageSource?.getMessage(key, mixedArray, locale)
            } else {
                messageSource?.getMessage(key, arrayOf(val1a, val2), locale)
            }
//            println("kotlinTranslation:"+kotlinTranslation)

//            if (jsTranslation != kotlinTranslation) {
//                println("translations do not match")
//            }

            Assertions.assertEquals(jsTranslation, kotlinTranslation)

//            println("--------------")
        }

        Assertions.assertTrue(index > 0 && index == enProperties.stringPropertyNames().size)

        // German
        val deProperties = java.util.Properties()
        try {
            java.io.FileInputStream("src/main/resources/messages_de.properties").use { fis ->
                deProperties.load(fis)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Assertions.assertEquals(keyValTranslation.size, deProperties.stringPropertyNames().size)
        Assertions.assertEquals((keyValTranslation.keys - deProperties.stringPropertyNames()).size, 0)
        Assertions.assertEquals((deProperties.stringPropertyNames() - keyValTranslation.keys).size, 0)

        for (key in deProperties.stringPropertyNames()) {
            val regex = Regex("""\{(\d+)}""")
            var deValue = deProperties.getProperty(key)
            var enValue = enProperties.getProperty(key)
            var matches = regex.findAll(deValue)
            val deValueCount = matches.count()
            matches = regex.findAll(enValue)
            val enValueCount = matches.count()

            Assertions.assertEquals(enValueCount, deValueCount)
            Assertions.assertTrue(enProperties.stringPropertyNames().contains(key))
        }

        // Spanish
        val esProperties = java.util.Properties()
        try {
            java.io.FileInputStream("src/main/resources/messages_es.properties").use { fis ->
                esProperties.load(fis)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Assertions.assertEquals(keyValTranslation.size, esProperties.stringPropertyNames().size)
        Assertions.assertEquals((keyValTranslation.keys - esProperties.stringPropertyNames()).size, 0)
        Assertions.assertEquals((esProperties.stringPropertyNames() - keyValTranslation.keys).size, 0)

        for (key in esProperties.stringPropertyNames()) {
            val regex = Regex("""\{(\d+)}""")
            var esValue = esProperties.getProperty(key)
            var enValue = enProperties.getProperty(key)
            var matches = regex.findAll(esValue)
            val esCount = matches.count()
            matches = regex.findAll(enValue)
            val enCount = matches.count()

            Assertions.assertEquals(enCount, esCount)
            Assertions.assertTrue(enProperties.stringPropertyNames().contains(key))
        }

        // French
        val frProperties = java.util.Properties()
        try {
            java.io.FileInputStream("src/main/resources/messages_fr.properties").use { fis ->
                frProperties.load(fis)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Assertions.assertEquals(keyValTranslation.size, frProperties.stringPropertyNames().size)
        Assertions.assertEquals((keyValTranslation.keys - frProperties.stringPropertyNames()).size, 0)
        Assertions.assertEquals((frProperties.stringPropertyNames() - keyValTranslation.keys).size, 0)

        for (key in frProperties.stringPropertyNames()) {
            val regex = Regex("""\{(\d+)}""")
            var frValue = frProperties.getProperty(key)
            var enValue = enProperties.getProperty(key)
            var matches = regex.findAll(frValue)
            val frValueCount = matches.count()
            matches = regex.findAll(enValue)
            val enValueCount = matches.count()

            Assertions.assertEquals(enValueCount, frValueCount)
            Assertions.assertTrue(enProperties.stringPropertyNames().contains(key))
        }

        // Japanese
        val jaProperties = java.util.Properties()
        try {
            java.io.FileInputStream("src/main/resources/messages_ja.properties").use { fis ->
                jaProperties.load(fis)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Assertions.assertEquals(keyValTranslation.size, jaProperties.stringPropertyNames().size)
        Assertions.assertEquals((keyValTranslation.keys - jaProperties.stringPropertyNames()).size, 0)
        Assertions.assertEquals((jaProperties.stringPropertyNames() - keyValTranslation.keys).size, 0)

        for (key in jaProperties.stringPropertyNames()) {
            val regex = Regex("""\{(\d+)}""")
            var jpValue = jaProperties.getProperty(key)
            var enValue = enProperties.getProperty(key)
            var matches = regex.findAll(jpValue)
            val jpValueCount = matches.count()
            matches = regex.findAll(enValue)
            val enValueCount = matches.count()

            // Japanese singular only
            if (key == "main.pages.albums.photo" ||
                key == "main.pages.people.items" ||
                key == "main.pages.albums.video" ||
                key == "main.pages.map.modal.result"
            ) {
                Assertions.assertEquals(1, jpValueCount)
            } else {
                Assertions.assertEquals(enValueCount, jpValueCount)
            }

            Assertions.assertTrue(enProperties.stringPropertyNames().contains(key))
        }

        // Portuguese
        val ptProperties = java.util.Properties()
        try {
            java.io.FileInputStream("src/main/resources/messages_pt.properties").use { fis ->
                ptProperties.load(fis)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Assertions.assertEquals(keyValTranslation.size, ptProperties.stringPropertyNames().size)
        Assertions.assertEquals((keyValTranslation.keys - ptProperties.stringPropertyNames()).size, 0)
        Assertions.assertEquals((ptProperties.stringPropertyNames() - keyValTranslation.keys).size, 0)

        for (key in ptProperties.stringPropertyNames()) {
            val regex = Regex("""\{(\d+)}""")
            var ptValue = ptProperties.getProperty(key)
            var enValue = enProperties.getProperty(key)
            var matches = regex.findAll(ptValue)
            val ptValueCount = matches.count()
            matches = regex.findAll(enValue)
            val enValueCount = matches.count()

            Assertions.assertEquals(enValueCount, ptValueCount)
            Assertions.assertTrue(enProperties.stringPropertyNames().contains(key))
        }
    }
}