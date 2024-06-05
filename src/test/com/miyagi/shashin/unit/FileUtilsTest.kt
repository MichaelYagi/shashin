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

    @Test
    fun convertYamlToJsonTest() {
        var json = FileUtils.convertYamlToJson("french-hens: 3")
        Assertions.assertEquals(json, "{\"french-hens\":3}")

        json = FileUtils.convertYamlToJson(
                " calling-birds:\n   - huey\n   - dewey")
        Assertions.assertEquals(json, "{\"calling-birds\":[\"huey\",\"dewey\"]}")

        json = FileUtils.convertYamlToJson("ray: \"a drop of golden sun\"\npi: 3.14159")
        Assertions.assertEquals(json, "{\"ray\":\"a drop of golden sun\",\"pi\":3.14159}")

        json = FileUtils.convertYamlToJson("french-hens: 3\ncalling-birds:\n   - huey\n   - dewey")
        Assertions.assertEquals(json, "{\"french-hens\":3,\"calling-birds\":[\"huey\",\"dewey\"]}")

        json = FileUtils.convertYamlToJson("xmas-fifth-day:\n   golden-rings: 5\n   partridges:\n     count: 1")
        Assertions.assertEquals(json, "{\"xmas-fifth-day\":{\"golden-rings\":5,\"partridges\":{\"count\":1}}}")

        json = FileUtils.convertYamlToJson("french-hens:3")
        Assertions.assertEquals(json, "")

        json = FileUtils.convertYamlToJson(" calling-birds:\n  - huey\n   - dewey")
        Assertions.assertEquals(json, "{\"calling-birds\":[\"huey - dewey\"]}")

        json = FileUtils.convertYamlToJson("---\nQuickTimeSound-SampleRate: 48000")
        Assertions.assertEquals(json, "{\"QuickTimeSound-SampleRate\":48000}")
    }
}