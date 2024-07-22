package com.miyagi.shashin.unit

import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.sortPlaceNames
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

    @Test
    fun getProcessedPlacename() {
        var test = TextUtils.formatPlaceNameForHeader("Japan; amenity, cafe")
        Assertions.assertEquals("Japan", test)

        test = TextUtils.formatPlaceNameForHeader("Kohikan • Japan; amenity, cafe")
        Assertions.assertEquals("Japan", test)

        test = TextUtils.formatPlaceNameForHeader("Kohikan • 9 Taito, Japan; amenity, cafe")
        Assertions.assertEquals("Taito, Japan", test)

        test = TextUtils.formatPlaceNameForHeader("Atré • 1 Taito, Japan; shop, mall")
        Assertions.assertEquals("Taito, Japan", test)

        test = TextUtils.formatPlaceNameForHeader("Atré • 1 Taito, asdf, Japan; shop, mall")
        Assertions.assertEquals("Taito, asdf, Japan", test)

        test = TextUtils.formatPlaceNameForHeader("101A Avenue, Guildford, Surrey, British Columbia, Canada")
        Assertions.assertEquals("Surrey, British Columbia, Canada", test)

        test = TextUtils.formatPlaceNameForHeader("Tendon Kohaku • 190 Smithe Street, Downtown, Vancouver, British Columbia, Canada")
        Assertions.assertEquals("Vancouver, British Columbia, Canada", test)

        test = TextUtils.formatPlaceNameForHeader("Atré asdf asdf • 12-3 ,1 Taito, asdf, Japan; shop, mall")
        Assertions.assertEquals("Taito, asdf, Japan", test)
    }

    @Test
    fun getSortedPlacenames() {
        val data = arrayOf("11 Ameyoko Street, Taito, Japan; building, retail",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Kasuga-dori Avenue, Taito, Japan; highway, secondary",
            "Kasuga-dori Avenue, Taito, Japan; highway, secondary",
            "摩利支天横町, Taito, Japan; highway, unclassified",
            "摩利支天横町, Taito, Japan; highway, unclassified",
            "摩利支天横町, Taito, Japan; highway, unclassified",
            "Ueno 4-chome, Taito, Japan; highway, unclassified",
            "Ueno 4-chome, Taito, Japan; highway, unclassified",
            "TSC TOWER • Ueno 4-chome, Taito, Japan; building",
            "TSC TOWER • Ueno 4-chome, Taito, Japan; building",
            "TSC TOWER • Ueno 4-chome, Taito, Japan; building",
            "MAGAZINES • Ueno 4-chome, Taito, Japan; shop, clothes",
            "MAGAZINES • Ueno 4-chome, Taito, Japan; shop, clothes",
            "MAGAZINES • Ueno 4-chome, Taito, Japan; shop, clothes",
            "Ueno 6-chome, Taito, Japan; highway, unclassified",
            "11 Ameyoko Street, Taito, Japan; building, retail",
            "11 Ameyoko Street, Taito, Japan; building, retail",
            "Tsukishima Monja Moheji • Ameyoko Street, Taito, Japan; amenity, restaurant",
            "Tsukishima Monja Moheji • Ameyoko Street, Taito, Japan; amenity, restaurant",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "コウベタンタンメン エニシスタンド • 12 Taito, Japan; amenity, restaurant",
            "Ueno 3-chome, Taito, Japan; highway, unclassified",
            "久世福商店 • Chuo-dori Avenue, 外神田, Taito, Japan; shop, supermarket",
            "久世福商店 • Chuo-dori Avenue, 外神田, Taito, Japan; shop, supermarket",
            "Hotel Kangetsuso • 28 Taito, Japan; tourism, hotel",
            "10 Okachimachi Panda Square, Taito, Japan; building",
            "Emerald Avenue, Taito, Japan; amenity, photo booth",
            "NewDays • Okachimachi Panda Square, Taito, Japan; shop, convenience",
            "NewDays • Okachimachi Panda Square, Taito, Japan; shop, convenience",
            "10 Okachimachi Panda Square, Taito, Japan; building",
            "Ueno 3-chome, Taito, Japan; highway, unclassified",
            "CAFFÈ VELOCE • 7 Chuo-dori Avenue, 外神田, Taito, Japan; amenity, cafe",
            "CAFFÈ VELOCE • 7 Chuo-dori Avenue, 外神田, Taito, Japan; amenity, cafe",
            "Ueno 3-chome, Taito, Japan; highway, unclassified",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Higashiueno 4-chome, 東上野, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "過門香 • 52 Taito, Japan; amenity, restaurant",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Starbucks • 22 Taito, Japan; amenity, cafe",
            "Ueno Zoological Gardens (East Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (West Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (West Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (West Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (West Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Dobutsuen Dori, Taito, Japan; highway, tertiary",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (East Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (East Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (East Garden) • Taito, Japan; tourism, zoo",
            "コマツオトメ原木 • Dobutsuen Dori, Taito, Japan; natural, tree",
            "コマツオトメ原木 • Dobutsuen Dori, Taito, Japan; natural, tree",
            "パンダ橋, Taito, Japan; tourism, information",
            "Tokyo Bunka Kaikan • Taito, Japan; building",
            "タイムズ • Panda Bridge, Taito, Japan; amenity, parking",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "JR Transferting, Taito, Japan; highway, footway",
            "JR Transferting, Taito, Japan; highway, footway",
            "Sugar Butter no Ki • Atré, Taito, Japan; shop, confectionery",
            "JR Transferting, Taito, Japan; highway, footway",
            "Ueno Station, 東上野, Taito, Japan; highway, footway",
            "ユーハイム • Ueno Station, 東上野, Taito, Japan; shop, confectionery",
            "ユーハイム • Ueno Station, 東上野, Taito, Japan; shop, confectionery",
            "Sugar Butter no Ki • Atré, Taito, Japan; shop, confectionery",
            "ユーハイム • Ueno Station, 東上野, Taito, Japan; shop, confectionery",
            "Andersen • Ueno Station, Taito, Japan; shop, bakery",
            "Andersen • Ueno Station, Taito, Japan; shop, bakery",
            "Atré • 1 Taito, Japan; shop, mall",
            "Atré • 1 Taito, Japan; shop, mall",
            "7;8 • Marunouchi-Muromachi Line, Chiyoda, Japan; railway, platform",
            "Chigasaki • 茅ヶ崎停車場茅ヶ崎線(雄三通り), Chigasaki, Kanagawa Prefecture, Japan; railway, stop",
            "日本教育書道藝術院西校舎 • 2 Taito, Japan; amenity, school",
            "Sango Street, Taito, Japan; highway, unclassified")

        val metadataList = mutableListOf(
            com.miyagi.shashin.model.Metadata()
        )

        for (placeName in data) {
            val metadata = com.miyagi.shashin.model.Metadata()
            metadata.setPlaceName(placeName)
            metadataList.add(metadata)
        }

        val sortedPlaceNames = sortPlaceNames(metadataList)
        println(sortedPlaceNames)
        Assertions.assertEquals(true, true)
    }
}