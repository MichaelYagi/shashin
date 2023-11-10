package com.miyagi.shashin.unit

import com.miyagi.shashin.util.FileUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class FileUtilsTest {
    @Test
    fun parseBase64Test() {
        var byteArray = FileUtils.parseBase64("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII")
        Assertions.assertTrue(byteArray!!.isNotEmpty())

        byteArray = FileUtils.parseBase64("nonbase64")
        Assertions.assertNull(byteArray)

        byteArray = FileUtils.parseBase64("")
        Assertions.assertNull(byteArray)
    }
}