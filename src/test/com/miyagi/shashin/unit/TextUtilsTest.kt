package com.miyagi.shashin.unit

import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.sortPlaceNames
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class TextUtilsTest {
    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

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
        Assertions.assertTrue(cookie["token"] == "")
        Assertions.assertTrue(cookie["series"] == "")
        Assertions.assertTrue(cookie["expires"] == "")

        cookie = TextUtils.parseRememberMeCookie("remember-me=YXNkZjpxd2Vy; expires=Mon, 30-May-2016 05:06:07 +0100")
        Assertions.assertTrue(cookie["token"] == "asdf")
        Assertions.assertTrue(cookie["series"] == "qwer")
        Assertions.assertTrue(cookie["expires"] == "1464581167000")

        var token = TextUtils.decodePersistenceToken("YXNkZjpxd2Vy")
        Assertions.assertTrue(token == "asdf")

        var series = TextUtils.decodePersistenceSeries("YXNkZjpxd2Vy")
        Assertions.assertTrue(series == "qwer")

        var tokenSeries = TextUtils.verifyPersistenceToken("super","1771171640177", "\$2a\$10\$d4J5iPrj38rerwEwP/qJkO94.Lr/um9nj41YWM0m9gKTT7Ng/6vem", rememberMeKey.toString())
        Assertions.assertTrue(tokenSeries == "c3VwZXI6MTc3MTE3MTY0MDE3NzpTSEEyNTY6NGYyNmIxYWQ2N2YzOWRkNmU5NmViMTg4N2RhYzQzODdiYTEyZDJlYWM2ZTcxMjNkYTk2NDhmMzQ0NjBkYzgwZg")

        token = TextUtils.decodePersistenceToken(tokenSeries)
        Assertions.assertTrue(token == "super")

        series = TextUtils.decodePersistenceSeries(tokenSeries)
        Assertions.assertTrue(series == "1771171640177")

        val test = TextUtils.parseRememberMeCookie("remember-me=c3VwZXI6MTc3MTE3MTY0MDE3NzpTSEEyNTY6NGYyNmIxYWQ2N2YzOWRkNmU5NmViMTg4N2RhYzQzODdiYTEyZDJlYWM2ZTcxMjNkYTk2NDhmMzQ0NjBkYzgwZg")
        Assertions.assertTrue(test["token"] == "super")
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

        isNumber = TextUtils.isNumber("-12.3")
        Assertions.assertTrue(isNumber)

        var isInteger = TextUtils.isInteger("3")
        Assertions.assertTrue(isInteger)

        isInteger = TextUtils.isInteger("3.1")
        Assertions.assertFalse(isInteger)

        isInteger = TextUtils.isInteger("-3")
        Assertions.assertTrue(isInteger)
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
    fun getPlaceNameFromJson() {
        val jsonObj = mutableMapOf<String, Any?>()
        val addressObj = mutableMapOf<String, Any?>()

        var test = TextUtils.getPlaceNameFromJson(null)
        Assertions.assertEquals("Unknown location", test)

        test = TextUtils.getPlaceNameFromJson("{}")
        Assertions.assertEquals("Unknown location", test)

        test = TextUtils.getPlaceNameFromJson("Weird Thing that's not JSON}")
        Assertions.assertEquals("Unknown location", test)

        test = TextUtils.getPlaceNameFromJson("{\"place_id\":393686709,\"licence\":\"Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright\",\"osm_type\":\"way\",\"osm_id\":1293634847,\"lat\":\"35.7198906\",\"lon\":\"139.863119935947\",\"class\":\"amenity\",\"type\":\"bicycle_parking\",\"place_rank\":30,\"importance\":8.593499656667457e-05,\"addresstype\":\"amenity\",\"name\":\"新小岩東駐車場\",\"display_name\":\"新小岩東駐車場, 新小岩停車場線, Higashi-Shinkoiwa 1-chome, Katsushika, Tokyo, 124-0024, Japan\",\"address\":{\"amenity\":\"新小岩東駐車場\",\"road\":\"新小岩停車場線\",\"neighbourhood\":\"Higashi-Shinkoiwa 1-chome\",\"city\":\"Katsushika\",\"ISO3166-2-lvl4\":\"JP-13\",\"postcode\":\"124-0024\",\"country\":\"Japan\",\"country_code\":\"jp\"},\"extratags\":{\"fee\": \"yes\", \"covered\": \"no\", \"website\": \"https://www.city.katsushika.lg.jp/planning/1030243/1003619/1020233.html\", \"operator\": \"葛飾区\", \"operator:type\": \"public\"},\"namedetails\":{\"name\": \"新小岩東駐車場\"},\"boundingbox\":[\"35.7188020\",\"35.7208012\",\"139.8614596\",\"139.8646542\"]}")
        Assertions.assertEquals("新小岩東駐車場 • 新小岩停車場線, Katsushika, Japan; amenity, bicycle parking", test)

        test = TextUtils.getPlaceNameFromJson("{\"place_id\":260246161,\"licence\":\"Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright\",\"osm_type\":\"node\",\"osm_id\":4987573157,\"lat\":\"35.7105768\",\"lon\":\"139.7731305\",\"class\":\"amenity\",\"type\":\"cafe\",\"place_rank\":30,\"importance\":9.99999999995449e-06,\"addresstype\":\"amenity\",\"name\":\"Renoir\",\"display_name\":\"Renoir, 31, Ueno 2-chome, Taito, Tokyo, 110-0005, Japan\",\"address\":{\"amenity\":\"Renoir\",\"house_number\":\"31\",\"neighbourhood\":\"Ueno 2-chome\",\"city\":\"Taito\",\"ISO3166-2-lvl4\":\"JP-13\",\"postcode\":\"110-0005\",\"country\":\"Japan\",\"country_code\":\"jp\"},\"extratags\":{\"level\": \"1\", \"cuisine\": \"coffee_shop\", \"brand:en\": \"Renoir\", \"brand:ja\": \"ルノアール\", \"takeaway\": \"yes\", \"brand:wikidata\": \"Q11649991\", \"brand:wikipedia\": \"ja:銀座ルノアール\"},\"namedetails\":{\"name\": \"ルノアール\", \"brand\": \"ルノアール\", \"name:en\": \"Renoir\", \"name:ja\": \"ルノアール\", \"official_name\": \"喫茶室ルノアール\", \"official_name:en\": \"Ginza Renoir\", \"official_name:ja\": \"喫茶室ルノアール\"},\"boundingbox\":[\"35.7105268\",\"35.7106268\",\"139.7730805\",\"139.7731805\"]}")
        Assertions.assertEquals("Renoir • 31 Ueno 2-chome, Taito, Japan; amenity, cafe", test)

        jsonObj.clear()
        addressObj.clear()
        jsonObj["name"] = "Mike's Tool Shop"
        jsonObj["class"] = "amenity"
        jsonObj["type"] = "garage"
        jsonObj["display_name"] = "Mike's Tool Shop, 1234 Bright St, Surrey, BC, Canada"
        addressObj["house_number"] = "1234"
        addressObj["road"] = "101 Avenue"
        addressObj["residential"] = "Cobblefield Lane"
        addressObj["suburb"] = "Guildford"
        addressObj["neighbourhood"] = "Mr. Rogers"
        addressObj["city"] = "Surrey"
        addressObj["county"] = "Metro Vancouver Regional District"
        addressObj["state"] = "British Columbia"
        addressObj["ISO3166-2-lvl4"] = "CA-BC"
        addressObj["postcode"] = "V3R 4J6"
        addressObj["country"] = "Canada"
        addressObj["country_code"] = "ca"
        jsonObj["address"] = addressObj
        test = TextUtils.getPlaceNameFromJson(JSONObject(jsonObj).toString())
        Assertions.assertEquals("Mike's Tool Shop • 1234 101 Avenue, Guildford, Surrey, British Columbia, Canada; amenity, garage", test)

        jsonObj.clear()
        addressObj.clear()
        jsonObj["name"] = "Mike's Tool Shop"
        jsonObj["class"] = "amenity"
        jsonObj["type"] = "garage"
        jsonObj["display_name"] = "Mike's Tool Shop, 1234 Bright St, Surrey, BC, Canada"
        test = TextUtils.getPlaceNameFromJson(JSONObject(jsonObj).toString())
        Assertions.assertEquals("Mike's Tool Shop, 1234 Bright St, Surrey, BC, Canada; amenity, garage", test)

        jsonObj.clear()
        addressObj.clear()
        jsonObj["name"] = "Mike's Tool Shop"
        jsonObj["class"] = "amenity"
        jsonObj["type"] = "garage"
        jsonObj["display_name"] = "Mike's Tool Shop, 1234 Bright St, Surrey, BC, Canada"
        addressObj["house_number"] = "1234"
        addressObj["residential"] = "Cobblefield Lane"
        addressObj["suburb"] = "Guildford"
        addressObj["neighbourhood"] = "Mr. Rogers"
        addressObj["city"] = "Surrey"
        addressObj["county"] = "Metro Vancouver Regional District"
        addressObj["state"] = "British Columbia"
        addressObj["ISO3166-2-lvl4"] = "CA-BC"
        addressObj["postcode"] = "V3R 4J6"
        addressObj["country"] = "Canada"
        addressObj["country_code"] = "ca"
        jsonObj["address"] = addressObj
        test = TextUtils.getPlaceNameFromJson(JSONObject(jsonObj).toString())
        Assertions.assertEquals("Mike's Tool Shop • 1234 Mr. Rogers, Guildford, Surrey, British Columbia, Canada; amenity, garage", test)

        jsonObj.clear()
        addressObj.clear()
        jsonObj["name"] = "Mike's Tool Shop"
        jsonObj["class"] = "amenity"
        jsonObj["type"] = "garage"
        jsonObj["display_name"] = "Mike's Tool Shop, 1234 Bright St, Surrey, BC, Canada"
        addressObj["road"] = "Hillcrest Road"
        addressObj["suburb"] = "Gidea Park"
        addressObj["city"] = "London Borough of Havering"
        addressObj["state_district"] = "Grand Londres"
        addressObj["state"] = "Angleterre"
        addressObj["postcode"] = "RM11 1EA"
        addressObj["country"] = "Royaume-Uni"
        addressObj["country_code"] = "gb"
        jsonObj["address"] = addressObj
        test = TextUtils.getPlaceNameFromJson(JSONObject(jsonObj).toString())
        Assertions.assertEquals("Mike's Tool Shop • Hillcrest Road, Gidea Park, London Borough of Havering, Angleterre, Royaume-Uni; amenity, garage", test)
    }

    @Test
    fun getGenerateUUID() {
        var test = TextUtils.generateUUID("asdf",
        "1234",
        "5678",
        1.2,
        6,
        "1357",
        "asdf")

        Assertions.assertEquals("285803e1-9082-39b4-a819-f318746cf54a", test.toString())

        test = TextUtils.generateUUID(
            "2",
            null,
            null,
            null,
            null,
            null,
            "random string generated from tests")

        Assertions.assertEquals("e5ae7a0f-0969-3378-83cf-a710c3b707f1", test.toString())
    }

    @Test
    fun getProcessedPlaceName() {
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
    fun getCacheControl() {
        var test = TextUtils.getCacheControl("23h")
        Assertions.assertEquals("CacheControl [max-age=82800]", test.toString())

        test = TextUtils.getCacheControl("1s")
        Assertions.assertEquals("CacheControl [max-age=1]", test.toString())

        test = TextUtils.getCacheControl("1d")
        Assertions.assertEquals("CacheControl [max-age=86400]", test.toString())

        test = TextUtils.getCacheControl("24h")
        Assertions.assertEquals("CacheControl [max-age=86400]", test.toString())

        test = TextUtils.getCacheControl("60m")
        Assertions.assertEquals("CacheControl [max-age=3600]", test.toString())

        test = TextUtils.getCacheControl("1h")
        Assertions.assertEquals("CacheControl [max-age=3600]", test.toString())

        test = TextUtils.getCacheControl("0202S")
        Assertions.assertEquals("CacheControl [max-age=202]", test.toString())

        test = TextUtils.getCacheControl("1l")
        Assertions.assertEquals("CacheControl [no-cache]", test.toString())

        test = TextUtils.getCacheControl("none")
        Assertions.assertEquals("CacheControl [no-cache]", test.toString())

        test = TextUtils.getCacheControl("asdh")
        Assertions.assertEquals("CacheControl [no-cache]", test.toString())
    }

    @Test
    fun getSortedPlacenames() {
        var data = arrayOf(
            "11 Ameyoko Street, Taito, Japan; building, retail",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Kasuga-dori Avenue, Taito, Japan; highway, secondary",
            "Kasuga-dori Avenue, Taito, Japan; highway, secondary",
            "摩利支天横町, Taito, Japan; highway, unclassified",
            "Ueno 4-chome, Taito, Japan; highway, unclassified",
            "TSC TOWER • Ueno 4-chome, Taito, Japan; building",
            "MAGAZINES • Ueno 4-chome, Taito, Japan; shop, clothes",
            "MAGAZINES • Ueno 4-chome, Taito, Japan; shop, clothes",
            "Ueno 6-chome, Taito, Japan; highway, unclassified",
            "11 Ameyoko Street, Taito, Japan; building, retail",
            "11 Ameyoko Street, Taito, Japan; building, retail",
            "Tsukishima Monja Moheji • Ameyoko Street, Taito, Japan; amenity, restaurant",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "Monthly Sweets • Kasuga-dori Avenue, Taito, Japan; shop, confectionery",
            "コウベタンタンメン エニシスタンド • 12 Taito, Japan; amenity, restaurant",
            "Ueno 3-chome, Taito, Japan; highway, unclassified",
            "久世福商店 • Chuo-dori Avenue, 外神田, Taito, Japan; shop, supermarket",
            "Hotel Kangetsuso • 28 Taito, Japan; tourism, hotel",
            "10 Okachimachi Panda Square, Taito, Japan; building",
            "Emerald Avenue, Taito, Japan; amenity, photo booth",
            "NewDays • Okachimachi Panda Square, Taito, Japan; shop, convenience",
            "10 Okachimachi Panda Square, Taito, Japan; building",
            "Ueno 3-chome, Taito, Japan; highway, unclassified",
            "CAFFÈ VELOCE • 7 Chuo-dori Avenue, 外神田, Taito, Japan; amenity, cafe",
            "Ueno 3-chome, Taito, Japan; highway, unclassified",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Tosei Hotel Cocone • 9 Taito, Japan; tourism, hotel",
            "Higashiueno 4-chome, 東上野, Taito, Japan; highway, unclassified",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "過門香 • 52 Taito, Japan; amenity, restaurant",
            "Uenokoen, Taito, Japan; highway, unclassified",
            "Starbucks • 22 Taito, Japan; amenity, cafe",
            "Ueno Zoological Gardens (East Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens (West Garden) • Taito, Japan; tourism, zoo",
            "Ueno Zoological Gardens • Taito, Japan; tourism, zoo",
            "Dobutsuen Dori, Taito, Japan; highway, tertiary",
            "コマツオトメ原木 • Dobutsuen Dori, Taito, Japan; natural, tree",
            "コマツオトメ原木 • Dobutsuen Dori, Taito, Japan; natural, tree",
            "パンダ橋, Taito, Japan; tourism, information",
            "Tokyo Bunka Kaikan • Taito, Japan; building",
            "タイムズ • Panda Bridge, Taito, Japan; amenity, parking",
            "JR Transferting, Taito, Japan; highway, footway",
            "Sugar Butter no Ki • Atré, Taito, Japan; shop, confectionery",
            "JR Transferting, Taito, Japan; highway, footway",
            "Ueno Station, 東上野, Taito, Japan; highway, footway",
            "ユーハイム • Ueno Station, 東上野, Taito, Japan; shop, confectionery",
            "Sugar Butter no Ki • Atré, Taito, Japan; shop, confectionery",
            "ユーハイム • Ueno Station, 東上野, Taito, Japan; shop, confectionery",
            "Andersen • Ueno Station, Taito, Japan; shop, bakery",
            "Atré • 1 Taito, Japan; shop, mall",
            "Atré • 1 Taito, Japan; shop, mall",
            "7;8 • Marunouchi-Muromachi Line, Chiyoda, Japan; railway, platform",
            "Chigasaki • 茅ヶ崎停車場茅ヶ崎線(雄三通り), Chigasaki, Kanagawa Prefecture, Japan; railway, stop",
            "日本教育書道藝術院西校舎 • 2 Taito, Japan; amenity, school",
            "Sango Street, Taito, Japan; highway, unclassified"
        )

        var metadataList = mutableListOf(
            com.miyagi.shashin.model.Metadata()
        )

        for (placeName in data) {
            val metadata = com.miyagi.shashin.model.Metadata()
            metadata.setPlaceName(placeName)
            metadataList.add(metadata)
        }

        var sortedPlaceNames = sortPlaceNames(metadataList)
//        for (placeName in sortedPlaceNames) {
//            println(placeName)
//        }

        var sortedArray = arrayOf(
            "Marunouchi-Muromachi Line, Chiyoda, Japan",
            "Chigasaki, Kanagawa Prefecture, Japan",
            "Ameyoko Street, Taito, Japan",
            "Atré, Taito, Japan",
            "Dobutsuen Dori, Taito, Japan",
            "Emerald Avenue, Taito, Japan",
            "JR Transferting, Taito, Japan",
            "Kasuga-dori Avenue, Taito, Japan",
            "Okachimachi Panda Square, Taito, Japan",
            "Panda Bridge, Taito, Japan",
            "Sango Street, Taito, Japan",
            "Ueno 3-chome, Taito, Japan",
            "Ueno 4-chome, Taito, Japan",
            "Ueno 6-chome, Taito, Japan",
            "Ueno Station, Taito, Japan",
            "Uenokoen, Taito, Japan",
            "パンダ橋, Taito, Japan",
            "外神田, Taito, Japan",
            "摩利支天横町, Taito, Japan",
            "東上野, Taito, Japan"
        )

        Assertions.assertTrue(sortedPlaceNames.toTypedArray() contentEquals sortedArray)

        // Test 2
        data = arrayOf(
            "横浜駅 • Nishi Ward, Yokohama, Kanagawa Prefecture, Japan",
            "Nakakaigan 2-chome, Chigasaki, Kanagawa Prefecture, Japan",
            "Yokohama Chinatown • Yamashitacho, Naka Ward, Yokohama, Kanagawa Prefecture, Japan"
        )

        metadataList = mutableListOf(
            com.miyagi.shashin.model.Metadata()
        )

        for (placeName in data) {
            val metadata = com.miyagi.shashin.model.Metadata()
            metadata.setPlaceName(placeName)
            metadataList.add(metadata)
        }

        sortedPlaceNames = sortPlaceNames(metadataList)
//        for (placeName in sortedPlaceNames) {
//            println(placeName)
//        }

        sortedArray = arrayOf(
            "Chigasaki, Kanagawa Prefecture, Japan",
            "Yokohama, Kanagawa Prefecture, Japan"
        )

        Assertions.assertTrue(sortedPlaceNames.toTypedArray() contentEquals sortedArray)
    }

    @Test
    fun isMobileTest() {
        // Android
        var isMobile = TextUtils.isMobile("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
        Assertions.assertTrue(isMobile)

        isMobile = TextUtils.isMobile("Mozilla/5.0 (Linux; Android 12; SM-X906C Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/80.0.3987.119 Mobile Safari/537.36")
        Assertions.assertTrue(isMobile)

        //iPad
        isMobile = TextUtils.isMobile("Mozilla/5.0 (iPad; CPU OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1")
        Assertions.assertTrue(isMobile)

        //iPhone
        isMobile = TextUtils.isMobile("Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1")
        Assertions.assertTrue(isMobile)

        // Desktop
        isMobile = TextUtils.isMobile("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9")
        Assertions.assertFalse(isMobile)

        isMobile = TextUtils.isMobile("802s asdf")
        Assertions.assertTrue(isMobile)
    }
}