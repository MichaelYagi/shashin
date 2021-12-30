package com.miyagi.shashin

//import BaseSeleniumTests
//import com.miyagi.shashin.model.User
//import com.miyagi.shashin.repository.UserRepository
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.openqa.selenium.By
//import org.openqa.selenium.WebElement
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.web.server.LocalServerPort
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
//import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
//import org.springframework.test.web.servlet.setup.MockMvcBuilders
//import org.springframework.web.context.WebApplicationContext
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class TimelineSeleniumTest: BaseSeleniumTests() {
//
//    private var adminId: Int? = null
//    private var userId: Int? = null
//    private var mockMvc: MockMvc? = null
//
//    @LocalServerPort
//    private val port = 0
//
//    @Autowired
//    private val context: WebApplicationContext? = null
//
//    @Autowired
//    private val userRepository: UserRepository? = null
//
//    private var bcrypt = BCryptPasswordEncoder()
//
//    @BeforeEach
//    fun setup() {
//        val adminObj = User()
//        adminObj.setUsername("testadmin")
//        var encodedPassword: String = bcrypt.encode("testadmin")
//        adminObj.setPassword(encodedPassword)
//        adminObj.setAuthority("ROLE_ADMIN")
//        adminObj.setIsAllowed(true)
//        userRepository?.save(adminObj)
//        adminId = adminObj.getId()
//
//        val userObj = User()
//        userObj.setUsername("testuser")
//        encodedPassword = bcrypt.encode("testuser")
//        userObj.setPassword(encodedPassword)
//        userObj.setAuthority("ROLE_USER")
//        userObj.setIsAllowed(true)
//        userRepository?.save(userObj)
//        userId = userObj.getId()
//
//        mockMvc = MockMvcBuilders
//            .webAppContextSetup(context!!)
//            .apply<DefaultMockMvcBuilder>(springSecurity())
//            .build()
//    }
//
//    @AfterEach
//    fun teardown() {
//        adminId?.let { userRepository?.deleteById(it) }
//        userId?.let { userRepository?.deleteById(it) }
//    }
//
//    @Test
//    fun getIndexPage() {
//        this.driver?.get("http://localhost:$port")
//        val element: WebElement? = this.driver?.findElement(By.id("copyrightYear"))
//        Assertions.assertNotNull(element)
//    }
//}