package com.miyagi.shashin.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.repository.MetadataRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpServletResponse


@Component
class TextUtils {
    companion object {

        private var logger: Logger = Logger.getLogger(TextUtils::class.simpleName)

        fun parseRememberMeCookie(cookie: String): HashMap<String,String> {
            val seriesExpiryMap = HashMap<String,String>()
            seriesExpiryMap["series"] = ""
            seriesExpiryMap["expiry"] = ""

            val cookieArray = cookie.split("; ")
            for (keyValue in cookieArray) {
                val keyValueArray = keyValue.split("=")
                val key = keyValueArray[0]
                var value = ""

                if (keyValueArray.size > 1) {
                    value = keyValueArray[1]
                }

                if (key.lowercase() == "remember-me" && value.isNotEmpty()) {
                    seriesExpiryMap["series"] = decodePersistenceToken(value)
                }

                if (key.lowercase() == "expires" && value.isNotEmpty()) {
                    val datetime = SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z").parse(value);
                    seriesExpiryMap["expires"] = datetime.time.toString()
                }
            }

            return seriesExpiryMap
        }

        fun decodePersistenceToken(token: String): String {
            if (token.isNotBlank()) {
                var decodedSeriesToken = String(Base64.getDecoder().decode(token))
                decodedSeriesToken = URLDecoder.decode(decodedSeriesToken, StandardCharsets.UTF_8.toString())
                val decodedSeriesTokenArray = decodedSeriesToken.split(":")
                return decodedSeriesTokenArray[0]
            }

            return ""
        }

        fun decodePersistenceSeries(token: String): String {
            if (token.isNotBlank()) {
                var decodedSeriesToken = String(Base64.getDecoder().decode(token))
                decodedSeriesToken = URLDecoder.decode(decodedSeriesToken, StandardCharsets.UTF_8.toString())
                val decodedSeriesTokenArray = decodedSeriesToken.split(":")
                return decodedSeriesTokenArray[1]
            }

            return ""
        }

        fun getCommonDateFormat(): String {
            return "yyyy-MM-dd HH:mm:ss"
        }

        fun isNumber(input: String): Boolean {
            val integerChars = '0'..'9'
            var dotOccurred = 0
            return input.all { it in integerChars || it == '.' && dotOccurred++ < 1 }
        }

        fun isInteger(input: String) = input.all {
            val integerChars = '0'..'9'
            it in integerChars
        }

        fun formatToLongDate(oldDate: String): String {
            val sdf = SimpleDateFormat(getCommonDateFormat())
            val newSdf = SimpleDateFormat("EEE, MMM d, yyyy")
            val temp = sdf.parse(oldDate)
            return newSdf.format(temp)
        }

        fun formatToLongDateWithTime(oldDate: String): String {
            val sdf = SimpleDateFormat(getCommonDateFormat())
            val newSdf = SimpleDateFormat("EEE, MMM d, yyyy  'at' h:mm aa")
            val temp = sdf.parse(oldDate)
            return newSdf.format(temp)
        }

        fun capitalized(str: String): String {
            return str.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase(Locale.getDefault())
                else it.toString()
            }
        }

        fun convertDecimalToFraction(x: Double): String {
            if (x < 0) {
                return "-" + convertDecimalToFraction(-x)
            }
            val tolerance = 1.0E-6
            var h1 = 1.0
            var h2 = 0.0
            var k1 = 0.0
            var k2 = 1.0
            var b = x
            do {
                val a = Math.floor(b)
                var aux = h1
                h1 = a * h1 + h2
                h2 = aux
                aux = k1
                k1 = a * k1 + k2
                k2 = aux
                b = 1 / (b - a)
            } while (Math.abs(x - h1 / k1) > x * tolerance)
            return "${h1.toInt()}/${k1.toInt()}"
        }

        fun getCurrentTimestamp(): String {
            val dtf = DateTimeFormatter.ofPattern(getCommonDateFormat())
            val now = LocalDateTime.now()
            return dtf.format(now)
        }

        fun generateUUID(
            inputString:String?,
            inputStringTwo:String? = null,
            inputStringThree: String? = null,
            someDouble: Double? = null,
            someInt: Int? = null,
            inputStringFour: String? = null,
            location: String = ""): UUID {
            val uuidInput = "$inputString-$inputStringTwo-$inputStringThree-$someDouble-$someInt-$inputStringFour"
            val uuid = UUID.nameUUIDFromBytes(uuidInput.toByteArray())

            var logString = "UUID $uuid generated from input $uuidInput"
            if (location.isNotEmpty()) {
                logString += " from $location"
            }
            logger.log(Level.INFO, logString)
            return uuid
        }

        fun getGeoData(geocodeUrl: String,lat: String, lng: String): String? {
            val geoLookupUrl: String = geocodeUrl+"reverse?format=json&lat="+lat+"&lon="+lng+"&extratags=1&namedetails=1"
            return readUrl(geoLookupUrl)
        }

        fun getPlaceNamesForDate(year: Int, month: Int, day: Int, metadataRepository:MetadataRepository, type: String = "all"): MutableList<String> {
            val placeList = if (type == "all") {
                metadataRepository.findTimelinePlaceByDate(year, month, day)
            } else {
                metadataRepository.findTimelinePlaceByDateAndType(year, month, day, type)
            }
            var placeNameHeaders = mutableListOf<String>()
            val processedPlaceNameArray = mutableListOf<String>()
            if (placeList != null) {
                for (placeDescription in placeList) {
                    if (placeDescription != null) {
                        val placeDescriptionArray = placeDescription.split(";")
                        if (placeDescriptionArray.size > 1) {
                            val placeName = placeDescriptionArray[0]
                            val placeNameArray = placeName.split(",")
                            if (placeNameArray.size > 2) {
                                val processedPlaceName = placeNameArray[placeNameArray.size - 3].trim() + ", " + placeNameArray[placeNameArray.size - 2].trim() + ", " + placeNameArray[placeNameArray.size - 1].trim()
                                processedPlaceNameArray.add(processedPlaceName)
                            }
                        } else {
                            val placeNameArray = placeDescription.split(",")
                            if (placeNameArray.size > 2) {
                                val processedPlaceName =
                                    placeNameArray[placeNameArray.size - 3].trim() + ", " + placeNameArray[placeNameArray.size - 2].trim() + ", " + placeNameArray[placeNameArray.size - 1].trim()
                                processedPlaceNameArray.add(processedPlaceName)
                            }
                        }
                    }
                }
            }
            val sortedPlaces = processedPlaceNameArray
                .groupingBy{ it }
                .eachCount()
                .toList()
                .sortedByDescending{ it.second }.map{it.first}

            if (sortedPlaces.isNotEmpty()) {
                placeNameHeaders = sortedPlaces as MutableList<String>
            } else {
                placeNameHeaders.add("")
            }

            return placeNameHeaders
        }

        fun getPlaceNameFromJson(geoDataJsonString: String?): String {
            var buildPlace = ""
            if (geoDataJsonString != null) {
                val mapper = ObjectMapper()
                val addressObj = mapper.readTree(geoDataJsonString)

                if (!addressObj.isNull) {
                    if (addressObj.has("name") && addressObj.get("name") != null &&
                        addressObj.get("name").textValue() != "") {
                        if (addressObj.has("address") && addressObj.get("address").has("road") && addressObj.get("address").get("road") != null &&
                            addressObj.get("address").get("road").textValue() != "" && addressObj.get("address").get("road").textValue() != addressObj.get("name").textValue()) {
                            buildPlace += addressObj.get("name").textValue() + ", "
                        }
                    }

                    if (addressObj.has("address")) {
                        if (addressObj.get("address").has("house_number") && addressObj.get("address").get("house_number") != null && addressObj.get("address").get("house_number").textValue() != "") {
                            buildPlace += addressObj.get("address").get("house_number").textValue() + ", "
                        }
                        if (addressObj.get("address").has("road") && addressObj.get("address").get("road") != null && addressObj.get("address").get("road").textValue() != "") {
                            buildPlace += addressObj.get("address").get("road").textValue() + ", "
                        }
                        if (addressObj.get("address").has("suburb") && addressObj.get("address")
                                .get("suburb") != null && addressObj.get("address").get("suburb").textValue() != ""
                        ) {
                            buildPlace += addressObj.get("address").get("suburb").textValue() + ", "
                        }
                        if (addressObj.get("address").has("city") && addressObj.get("address").get("city") != null && addressObj.get("address").get("city").textValue() != "") {
                            buildPlace += addressObj.get("address").get("city").textValue() + ", "
                        }
                        if (addressObj.get("address").has("state") && addressObj.get("address").get("state") != null && addressObj.get("address").get("state").textValue() != "") {
                            buildPlace += addressObj.get("address").get("state").textValue() + ", "
                        }
                        if (addressObj.get("address").has("country") && addressObj.get("address")
                                .get("country") != null && addressObj.get("address").get("country").textValue() != ""
                        ) {
                            buildPlace += addressObj.get("address").get("country").textValue()  + ", "
                        }

                        if (buildPlace.trim().isNotBlank()) {
                            buildPlace = buildPlace.replace(", $".toRegex(), "").trim()
                            buildPlace += "; "
                        }
                    }

                    if (buildPlace == "" && addressObj.has("display_name") && addressObj.get("display_name") != null && addressObj.get("display_name").textValue() != "") {
                        buildPlace = addressObj.get("display_name").textValue() + ", "
                    }

                    if (addressObj.has("class") && addressObj.get("class") != null && addressObj.get("class").textValue() != "") {
                        buildPlace += addressObj.get("class").textValue().replace("_", " ")  + ", "
                    }

                    if (addressObj.has("type") && addressObj.get("type") != null && addressObj.get("type").textValue() != "" &&
                        addressObj.get("type").textValue() != "yes" && addressObj.get("type").textValue() != "no") {
                        buildPlace += addressObj.get("type").textValue().replace("_", " ")  + ", "
                    }

                    if (buildPlace.trim().isNotBlank()) {
                        buildPlace = buildPlace.replace(", $".toRegex(), "").trim()
                    }

                    buildPlace = buildPlace.trim()
                }
            }

            if (buildPlace.isBlank()) {
                buildPlace = "Unknown location name"
            }

            logger.log(Level.INFO, "Place string built: $buildPlace")

            return buildPlace
        }

        fun timeSchedules(): List<String> {
            return listOf(
                "0:00",
                "1:00",
                "2:00",
                "3:00",
                "4:00",
                "5:00",
                "6:00",
                "7:00",
                "8:00",
                "9:00",
                "10:00",
                "11:00",
                "12:00",
                "13:00",
                "14:00",
                "15:00",
                "16:00",
                "17:00",
                "18:00",
                "19:00",
                "20:00",
                "21:00",
                "22:00",
                "23:00"
            )
        }

        fun timeOffsets(): List<String> {

            return listOf(
                "-12:00",
                "-11:00",
                "-10:00",
                "-09:30",
                "-09:00",
                "-08:00",
                "-07:00",
                "-06:00",
                "-05:00",
                "-04:30",
                "-04:00",
                "-03:30",
                "-03:00",
                "-02:00",
                "-01:00",
                "±00:00",
                "+01:00",
                "+02:00",
                "+03:00",
                "+03:30",
                "+04:00",
                "+04:30",
                "+05:00",
                "+05:30",
                "+05:45",
                "+06:00",
                "+06:30",
                "+07:00",
                "+08:00",
                "+08:45",
                "+09:00",
                "+09:30",
                "+10:00",
                "+11:00",
                "+11:30",
                "+12:00",
                "+13:00",
                "+14:00"
            )
        }

        fun doTimeConversion(time: String?, type: Boolean): String {
            if (time != null) {
                val timeArray = time.split(':')
                var hour: String
                if (timeArray.size > 1) {
                    hour = timeArray[0]
                    if (hour.count() == 1) {
                        hour = "0$hour"
                    }
                    val reformattedTime = "$hour:${timeArray[1]}"

                    val localZone = ZoneId.systemDefault()
                    val lt = LocalTime.parse(reformattedTime)
                    val ldt = LocalDate.now(localZone).atTime(lt)
                    val resultTime: ZonedDateTime = if (type) {
                        ldt.atZone(localZone).withZoneSameInstant(ZoneOffset.UTC)
                    } else {
                        ldt.atOffset(ZoneOffset.UTC).atZoneSameInstant(localZone)
                    }
                    val newTime = resultTime.toLocalTime()
                    return newTime.toString()
                }
            }
            return ""
        }

        fun escape(raw: String): String {
            var escaped = raw
            escaped = escaped.replace("\\", "\\\\")
            escaped = escaped.replace("\"", "\\\"")
            escaped = escaped.replace("\b", "\\b")
            escaped = escaped.replace("\u000c", "\\u000c")
            escaped = escaped.replace("\n", "\\n")
            escaped = escaped.replace("\r", "\\r")
            escaped = escaped.replace("\t", "\\t")
            // TODO: escape other non-printing characters using uXXXX notation
            return escaped
        }

        fun metadataInputValidation(day: Int?, month: Int?, year: Int?, time: String?, offset: String?, duration: String?): Boolean {
            val timeValidate = "(\\d{2}:\\d{2}:\\d{2})".toRegex()

            if (day != null && !(day in 1..31)) {
                return false
            }

            if (month != null && !(month in 1..12)) {
                return false
            }

            if (year != null && !(year in 1826..Calendar.getInstance().get(Calendar.YEAR))) {
                return false
            }

            if (time != null && time != "" && !time.matches(timeValidate)) {
                return false
            }

            if (offset != null && offset != "" && !(offset in timeOffsets())) {
                return false
            }

            if (duration != null && duration != "") {
                val isValidWithoutHour = "([1-5]?[0-9])(:[0-5][0-9])?".toRegex().matches(duration);
                val isValidWithHour = "([0-1]?[0-9]|2[0-4]):([0-5][0-9])(:[0-5][0-9])?".toRegex().matches(duration);

                if (!isValidWithoutHour && !isValidWithHour) {
                    return false
                }
            }

            return true
        }

        fun returnForbiddenError(response: HttpServletResponse): String {
            val jsonResponseMap = mutableMapOf<String, Any>()
            jsonResponseMap["msg"] = "Access is denied"
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern(TextUtils.getCommonDateFormat())
            jsonResponseMap["timestamp"] = now.format(formatter);
            jsonResponseMap["status"] = HttpStatus.FORBIDDEN
            val mapper = ObjectMapper()

            response.contentType = "application/json"
            response.status = HttpStatus.FORBIDDEN.value()
            return mapper.writeValueAsString(jsonResponseMap)
        }

        private fun readUrl(urlString: String): String? {
            var place: String? = null
//            var reader: BufferedReader? = null
            try {
//                val url = URL(urlString)
//                reader = BufferedReader(InputStreamReader(url.openStream()))
//                val buffer = StringBuffer()
//                var read: Int
//                val chars = CharArray(1024)
//                while (reader.read(chars).also { read = it } != -1) buffer.append(chars, 0, read)
                place = URL(urlString).readText()
            } catch(e: Exception) {
                logger.log(Level.WARNING, "Could not read URL: " + e.message)
            }
//            finally {
//                reader?.close()
//            }

            return place
        }
    }
}