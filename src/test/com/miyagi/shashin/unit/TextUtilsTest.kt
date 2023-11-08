package com.miyagi.shashin.unit

import com.miyagi.shashin.util.TextUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class TextUtilsTest {

    @Test
    fun metadataInputValidationTest() {
        var valid = TextUtils.metadataInputValidation(1, 1, 2000, "01:00:00", "-07:00", "0:01")
        Assertions.assertTrue(valid)

        valid = TextUtils.metadataInputValidation(null, null, null, null, null, null)
        Assertions.assertTrue(valid)

        valid = TextUtils.metadataInputValidation(1, null, 2000, null, "-07:00", null)
        Assertions.assertTrue(valid)

        valid = TextUtils.metadataInputValidation(32, 1, 2000, "01:00:00", "-07:00", "0:01")
        Assertions.assertFalse(valid)

        valid = TextUtils.metadataInputValidation(1, 13, 2000, "01:00:00", "-07:00", "0:01")
        Assertions.assertFalse(valid)

        valid = TextUtils.metadataInputValidation(1, 1, 1800, "01:00:00", "-07:00", "0:01")
        Assertions.assertFalse(valid)

        valid = TextUtils.metadataInputValidation(1, 1, 2000, "1:00:00", "-07:00", "0:01")
        Assertions.assertFalse(valid)

        valid = TextUtils.metadataInputValidation(1, 1, 2000, "01:00:00", "-7:00", "0:01")
        Assertions.assertFalse(valid)

        valid = TextUtils.metadataInputValidation(1, 1, 2000, "01:00:00", "-07:00", "0:90")
        Assertions.assertFalse(valid)
    }
}