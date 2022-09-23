package com.miyagi.shashin.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


@Component
class TextUtils {
    companion object {

        private var logger: Logger = Logger.getLogger(TextUtils::class.simpleName)

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
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val newSdf = SimpleDateFormat("EEE, MMM d, yyyy")
            val temp = sdf.parse(oldDate)
            return newSdf.format(temp)
        }

        fun formatToLongDateWithTime(oldDate: String): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
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
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val now = LocalDateTime.now()
            return dtf.format(now)
        }

        fun generateUUID(
            filePath:String?,
            createdAt:String?,
            type: String?,
            fStopNumber: Double?,
            iso: Int?,
            exposure: String?): UUID {
            val uuidInput = "$filePath-$createdAt-$type-$fStopNumber-$iso-$exposure"
            val uuid = UUID.nameUUIDFromBytes(uuidInput.toByteArray())
            logger.log(Level.INFO, "UUID $uuid generated from input $uuidInput")
            return uuid
        }

        fun getGeoData(geocodeUrl: String,lat: String, lng: String): String? {
            val geoLookupUrl: String = geocodeUrl+"reverse?format=json&lat="+lat+"&lon="+lng
            return readUrl(geoLookupUrl)
        }

        fun getPlaceNameFromJson(geoDataJsonString: String?): String {
            var buildPlace = ""
            if (geoDataJsonString != null) {
                val mapper = ObjectMapper()
                val addressObj = mapper.readTree(geoDataJsonString)

                if (!addressObj.isNull) {
                    if (addressObj.get("address").get("road") != null) {
                        buildPlace += addressObj.get("address").get("road").textValue() + ", "
                    }
                    if (addressObj.get("address").get("city") != null) {
                        buildPlace += addressObj.get("address").get("city").textValue() + ", "
                    }
                    if (addressObj.get("address").get("state") != null) {
                        buildPlace += addressObj.get("address").get("state").textValue() + " "
                    }
                    if (addressObj.get("address").get("country") != null) {
                        buildPlace += addressObj.get("address").get("country").textValue()
                    }
                    if (buildPlace.isNotBlank()) {
                        buildPlace = buildPlace.trim()
                    }
                }
            }

            if (buildPlace.isBlank()) {
                buildPlace = "Unknown location name"
            }

            return buildPlace
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

        private fun readUrl(urlString: String): String? {
            var place: String? = null
            var reader: BufferedReader? = null
            try {
                val url = URL(urlString)
                reader = BufferedReader(InputStreamReader(url.openStream()))
                val buffer = StringBuffer()
                var read: Int
                val chars = CharArray(1024)
                while (reader.read(chars).also { read = it } != -1) buffer.append(chars, 0, read)
                place = buffer.toString()
            } catch(e: Exception) {
                logger.log(Level.WARNING, "Could not read URL: " + e.message)
            } finally {
                reader?.close()
            }

            return place
        }
    }
}