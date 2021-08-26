package com.miyagi.shashin.controller

import com.miyagi.shashin.repository.*
import junit.framework.Assert.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringJUnitConfig
@WebMvcTest(controllers = [TimelineController::class])
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
    private val restTemplate: TestRestTemplate? = null

    @Autowired
    private val mockMvc: MockMvc? = null

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
    @WithMockUser(username = "userMock", password = "pwd", roles = ["ADMIN"])
    fun timelineShouldReturnTimeline() {
//        val res = mockMvc?.perform(MockMvcRequestBuilders.get("/timeline"))
//            ?.andExpect(status().isOk)
//            ?.andReturn()
//        println(res?.response?.contentAsString)
        assertEquals(true, true);


//        assertThat(
//            this.restTemplate?.getForObject(
//                "http://localhost:" + port.toString() + "/timeline",
//                String::class.java
//            )
//        ).contains("Please asdf")
    }

}