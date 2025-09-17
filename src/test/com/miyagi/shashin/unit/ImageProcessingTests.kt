package com.miyagi.shashin.unit

import com.miyagi.shashin.util.ImageProcessing
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ImageProcessingTests {
    @Test
    fun isDupeImageTest() {
        val classLoader = javaClass.classLoader

        var testImageOne = classLoader.getResource("testscreen.jpg")!!.path
        var testImageTwo = classLoader.getResource("testscreen.jpg")!!.path
        var isDupe = ImageProcessing.isDuplicate(testImageOne, testImageTwo)
        Assertions.assertTrue(isDupe)

        testImageOne = classLoader.getResource("subdir/tablecup.jpg")!!.path
        testImageTwo = classLoader.getResource("subdir/tablecup_bw.jpg")!!.path
        isDupe = ImageProcessing.isDuplicate(testImageOne, testImageTwo)
        Assertions.assertTrue(isDupe)

        testImageOne = classLoader.getResource("subdir/SoSSpcl_Cvr_Main_JorgeJimenezJpg")!!.path
        testImageTwo = classLoader.getResource("subdir/people.jpg")!!.path
        isDupe = ImageProcessing.isDuplicate(testImageOne, testImageTwo)
        Assertions.assertFalse(isDupe)
    }
}