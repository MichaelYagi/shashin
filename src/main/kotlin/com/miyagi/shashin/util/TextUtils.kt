package com.miyagi.shashin.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Component
class TextUtils {
    companion object {
        fun formatToLongDate(oldDate: String): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val newSdf = SimpleDateFormat("EEE, MMM dd, yyyy")
            val temp = sdf.parse(oldDate)
            return newSdf.format(temp)
        }

        fun formatToLongDateWithTime(oldDate: String): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val newSdf = SimpleDateFormat("EEE, MMM dd, yyyy  'at' h:mm aa")
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

        fun generateUUID(
            takenAt:String?,
            createdAt:String?,
            type: String?,
            fNumber: Double?,
            iso: Int?,
            exposure: String?,
            lat:String?,
            lng:String?): UUID {
            val uuidInput = takenAt+createdAt+type+fNumber+iso+exposure+lat+lng
            return UUID.nameUUIDFromBytes(uuidInput.toByteArray())
        }

        fun getPlaceNameFromCoordinates(geocodeUrl: String,lat: String, lng: String): String {
            val geoLookupUrl: String = geocodeUrl+"reverse?format=json&lat="+lat+"&lon="+lng
            val response: String? = readUrl(geoLookupUrl)
            val mapper = ObjectMapper()
            val addressObj = mapper.readTree(response)
            var buildPlace = ""

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
                if (!buildPlace.isNullOrBlank()) {
                    buildPlace = buildPlace.trim()
                }
            }

            return buildPlace
        }

        @Throws(java.lang.Exception::class)
        private fun readUrl(urlString: String): String? {
            var reader: BufferedReader? = null
            return try {
                val url = URL(urlString)
                reader = BufferedReader(InputStreamReader(url.openStream()))
                val buffer = StringBuffer()
                var read: Int
                val chars = CharArray(1024)
                while (reader.read(chars).also { read = it } != -1) buffer.append(chars, 0, read)
                buffer.toString()
            } finally {
                reader?.close()
            }
        }
    }
}