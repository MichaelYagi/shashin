package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.*
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimelineControllerTests {
    @MockBean
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var metadataRepository: MetadataRepository

    @MockBean
    private lateinit var mediaDirRepository: MediaDirectoryRepository

    @MockBean
    private lateinit var albumRepository: AlbumRepository

    @MockBean
    private lateinit var favoriteRepository: FavoriteRepository

    private var mockMvc: MockMvc? = null;

    @Autowired
    private lateinit var context: WebApplicationContext

    @LocalServerPort
    private val port = 0

    @Autowired
    private val restTemplate: TestRestTemplate? = null

    @MockBean
    private val request: MockHttpServletRequest? = null

    @MockBean
    private val springSecurityFilterChain: FilterChainProxy? = null

    @Before
    fun setup() {
//        mockMvc = MockMvcBuilders
//            .webAppContextSetup(context)
//            .apply<DefaultMockMvcBuilder>(springSecurity())
//            .build();

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters<DefaultMockMvcBuilder>(springSecurityFilterChain)
            .build();

    }

    @Test
    @Throws(Exception::class)
    fun timelineShouldReturnLogin() {
        Assertions.assertThat(
            this.restTemplate?.getForObject(
                "http://localhost:$port/timeline",
                String::class.java
            )
        ).contains("Please Login")
    }

    @Test
    @WithMockUser(username = "username", password = "pwd", roles = ["ADMIN"])
    @Throws(Exception::class)
    fun timelineShouldReturnTimeline() {
        val session = mockMvc!!.perform(
            get("/users/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "username")
                .param("password", "pwd")
        )
        .andExpect(status().isOk) //.andExpect(redirectedUrl("/user/home"))
        .andReturn()
        .request
        .session

        request?.setSession(session!!)

        val securityContext =
            session!!.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) as SecurityContext

        SecurityContextHolder.setContext(securityContext)
    }

}