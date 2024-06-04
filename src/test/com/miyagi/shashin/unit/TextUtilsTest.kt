package com.miyagi.shashin.unit

import com.miyagi.shashin.util.TextUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.time.LocalTime

@ActiveProfiles("test")
class TextUtilsTest {

    @Test
    fun isLocalIpTest() {
        var isLocal = TextUtils.isLocalIp("127.0.0.1")
        Assertions.assertTrue(isLocal)

        isLocal = TextUtils.isLocalIp("localhost")
        Assertions.assertTrue(isLocal)

        isLocal = TextUtils.isLocalIp("192.168.0.1")
        Assertions.assertTrue(isLocal)

        isLocal = TextUtils.isLocalIp("0.0.0.0")
        Assertions.assertFalse(isLocal)

        isLocal = TextUtils.isLocalIp("https://www.google.com/")
        Assertions.assertFalse(isLocal)

        isLocal = TextUtils.isLocalIp("garbage")
        Assertions.assertFalse(isLocal)

        isLocal = TextUtils.isLocalIp("")
        Assertions.assertFalse(isLocal)

        isLocal = TextUtils.isLocalIp(null)
        Assertions.assertFalse(isLocal)
    }

    @Test
    fun parseRememberMeCookieTest() {
        var cookie = TextUtils.parseRememberMeCookie("JSESSIONID=AAE3E4E1396B3B2FCD6E519CCC9EF5BC; Path=/; HttpOnly")
        Assertions.assertTrue(cookie["series"] == "")
        Assertions.assertTrue(cookie["expires"] == "")

        cookie = TextUtils.parseRememberMeCookie("remember-me=YXNkZjpxd2Vy; expires=Mon, 30-May-2016 05:06:07 +0100")
        Assertions.assertTrue(cookie["series"] == "asdf")
        Assertions.assertTrue(cookie["expires"] == "1464581167000")

        val token = TextUtils.decodePersistenceToken("YXNkZjpxd2Vy")
        Assertions.assertTrue(token == "asdf")

        val series = TextUtils.decodePersistenceSeries("YXNkZjpxd2Vy")
        Assertions.assertTrue(series == "qwer")
    }

    @Test
    fun isNumberTest() {
        var isNumber = TextUtils.isNumber("123")
        Assertions.assertTrue(isNumber)

        isNumber = TextUtils.isNumber("09")
        Assertions.assertTrue(isNumber)

        isNumber = TextUtils.isNumber("1.23")
        Assertions.assertTrue(isNumber)

        isNumber = TextUtils.isNumber("1.2.3")
        Assertions.assertFalse(isNumber)

        isNumber = TextUtils.isNumber("a")
        Assertions.assertFalse(isNumber)

        isNumber = TextUtils.isNumber("a.3")
        Assertions.assertFalse(isNumber)

        isNumber = TextUtils.isNumber("3.a")
        Assertions.assertFalse(isNumber)

        var isInteger = TextUtils.isInteger("3")
        Assertions.assertTrue(isInteger)

        isInteger = TextUtils.isInteger("3.1")
        Assertions.assertFalse(isInteger)
    }

    @Test
    fun metadataInputValidationTest() {
        var valid = TextUtils.metadataInputValidation(1, 1, 2000, "01:00:00", "-07:00", "0:01")
        Assertions.assertTrue(valid)

        valid = TextUtils.metadataInputValidation(null, null, null, null, null, null)
        Assertions.assertTrue(valid)

        valid = TextUtils.metadataInputValidation(null, null, null, "", "", "")
        Assertions.assertTrue(valid)

        valid = TextUtils.metadataInputValidation(1, null, 2000, null, "-07:00", null)
        Assertions.assertTrue(valid)

        valid = TextUtils.metadataInputValidation(1, null, 2000, "", "-07:00", "")
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

        valid = TextUtils.metadataInputValidation(32, 1, 2000, "mumbo", "jumbo", "tothemax")
        Assertions.assertFalse(valid)
    }

    @Test
    fun formatToLongDateTest() {
        var longDate = TextUtils.formatToLongDate("2023-11-9 01:00:00")
        Assertions.assertEquals("Thu, Nov 9, 2023", longDate)

        longDate = TextUtils.formatToLongDate("2023-11-9")
        Assertions.assertEquals("", longDate)
    }

    @Test
    fun formatToLongDateWithTimeTest() {
        var longDateWithTime = TextUtils.formatToLongDateWithTime("2023-11-9 01:00:00")
        Assertions.assertEquals("Thu, Nov 9, 2023 at 1:00 AM", longDateWithTime)

        longDateWithTime = TextUtils.formatToLongDate("2023-11-9")
        Assertions.assertEquals("", longDateWithTime)
    }

    @Test
    fun capitalizedTest() {
        var str = TextUtils.capitalized("asdf")
        Assertions.assertEquals("Asdf", str)

        str = TextUtils.capitalized("")
        Assertions.assertEquals("", str)

        str = TextUtils.capitalized(null)
        Assertions.assertEquals("", str)
    }

    @Test
    fun convertDecimalToFractionTest() {
        var fraction = TextUtils.convertDecimalToFraction(0.9)
        Assertions.assertEquals("9/10", fraction)

        fraction = TextUtils.convertDecimalToFraction(0.3333333)
        Assertions.assertEquals("1/3", fraction)

        fraction = TextUtils.convertDecimalToFraction(0.5)
        Assertions.assertEquals("1/2", fraction)

        fraction = TextUtils.convertDecimalToFraction(0.0)
        Assertions.assertEquals("0/1", fraction)
    }

    @Test
    fun getCurrentTimestampTest() {
        val ts = TextUtils.getCurrentTimestamp()
        // yyyy-MM-dd HH:mm:ss
        Assertions.assertTrue(ts.matches("^\\d{4}\\-(0?[1-9]|1[012])\\-(0?[1-9]|[12][0-9]|3[01]) (?:(?:([01]?\\d|2[0-3]):)?([0-5]?\\d):)?([0-5]?\\d)\$".toRegex()))
    }

    @Test
    fun convertDateToYMDTest() {
        var possibleDate = TextUtils.convertDateToYMD("2024/01")
        Assertions.assertEquals("2024-01", possibleDate)

        possibleDate = TextUtils.convertDateToYMD("24/01/20")
        Assertions.assertEquals("2024-01-20", possibleDate)

        possibleDate = TextUtils.convertDateToYMD("yada/yada/yada")
        Assertions.assertEquals(null, possibleDate)
    }

    @Test
    fun getConvertDateToYMDTest() {
        var convertedString = TextUtils.convertDateToYMD("")
        Assertions.assertEquals(null, convertedString)

        convertedString = TextUtils.convertDateToYMD("asdf")
        Assertions.assertEquals(null, convertedString)

        convertedString = TextUtils.convertDateToYMD("2024/03/25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("2024-03-25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("24/03/25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("24-03-25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("2024/3/25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("2024-3-25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("24/3/25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("24-3-25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("24/3")
        Assertions.assertEquals("2024-03", convertedString)

        convertedString = TextUtils.convertDateToYMD("24-3")
        Assertions.assertEquals("2024-03", convertedString)

        convertedString = TextUtils.convertDateToYMD("24/3/25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("24-3-25")
        Assertions.assertEquals("2024-03-25", convertedString)

        convertedString = TextUtils.convertDateToYMD("03/3")
        Assertions.assertEquals("2003-03", convertedString)

        convertedString = TextUtils.convertDateToYMD("03-3")
        Assertions.assertEquals("2003-03", convertedString)
    }
}