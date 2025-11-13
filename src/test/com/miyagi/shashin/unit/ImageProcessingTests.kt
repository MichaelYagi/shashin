package com.miyagi.shashin.unit

import com.miyagi.shashin.ToolsControllerTestConfig
import com.miyagi.shashin.service.DuplicateImageChecker
import com.miyagi.shashin.service.ImageProcessing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(ToolsControllerTestConfig::class)
@ActiveProfiles("test")
class ImageProcessingTests {
    @Test
    fun isDupeImageTest() {
        val classLoader = javaClass.classLoader

        var testImageOne = classLoader.getResource("testscreen.jpg")!!.path
        var testImageTwo = classLoader.getResource("testscreen.jpg")!!.path
        var isDupe = DuplicateImageChecker.isDuplicate(testImageOne, testImageTwo)
        Assertions.assertTrue(isDupe)

        testImageOne = classLoader.getResource("subdir/tablecup.jpg")!!.path
        testImageTwo = classLoader.getResource("subdir/tablecup_bw.jpg")!!.path
        isDupe = DuplicateImageChecker.isDuplicate(testImageOne, testImageTwo)
        Assertions.assertTrue(isDupe)

        testImageOne = classLoader.getResource("subdir/SoSSpcl_Cvr_Main_JorgeJimenezJpg")!!.path
        testImageTwo = classLoader.getResource("subdir/people.jpg")!!.path
        isDupe = DuplicateImageChecker.isDuplicate(testImageOne, testImageTwo)
        Assertions.assertFalse(isDupe)
    }
}