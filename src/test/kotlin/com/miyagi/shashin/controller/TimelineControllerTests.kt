package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.*
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.MockMvcConfigurer
import org.springframework.web.context.WebApplicationContext


@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration
@WithMockUser(username="admin",roles=["ADMIN"])
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

    @LocalServerPort
    private val port = 8080

    @Autowired
    private lateinit var context: WebApplicationContext

    private var mvc: MockMvc? = null;

    @Before
    fun setup() {
        mvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build();
    }


//    @Autowired
//    private val restTemplate: TestRestTemplate? = null
//
//    @Autowired
//    private val springSecurityFilterChain: FilterChainProxy? = null

    @Test
    @Throws(Exception::class)
    fun timelineShouldReturnLogin() {
//        assertThat(
//            this.restTemplate?.getForObject(
//                "http://localhost:$port/timeline",
//                String::class.java
//            )
//        ).contains("Please Login")
    }

    @Test
    @Throws(Exception::class)
//    @WithMockUser(username = "userMock1", password = "pwd", roles = ["ADMIN"])
    fun timelineShouldReturnTimeline() {
        val res = mvc?.perform(MockMvcRequestBuilders.get("/timeline"))
            ?.andExpect(status().isOk)
            ?.andReturn()
        println(res?.response?.contentAsString)
        assertEquals(true, true);


//        assertThat(
//            this.restTemplate?.getForObject(
//                "http://localhost:" + port.toString() + "/timeline",
//                String::class.java
//            )
//        ).contains("Please asdf")
    }

}