package com.miyagi.shashin.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.miyagi.shashin.configuration.MultiSecurityConfig
import com.miyagi.shashin.model.FreeFormText
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.iakovlev.timeshape.TimeZoneEngine
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.ui.Model
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.net.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableIterable
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.count
import kotlin.collections.distinct
import kotlin.collections.dropLastWhile
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.joinToString
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.sortedBy
import kotlin.collections.toMutableList
import kotlin.collections.toTypedArray
import kotlin.collections.withIndex
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random


@Component
class TextUtils {
    companion object {

        private var logger: Logger = Logger.getLogger(TextUtils::class.simpleName)

        fun getRandomWithExclusion(start: Int, end: Int, exclude: List<Int> = listOf()): Int {
            var random = start + Random.nextInt(end - start + 1 - exclude.size)
            for (ex in exclude) {
                if (random < ex) {
                    break
                }
                random++
            }
            return random
        }

        fun isMobile(userAgent: String?): Boolean {
            var isMobile = false; //initiate as false

            val regex1 = "(android|bb\\d+|meego).+mobile|avantgo|bada/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|ipad|iris|kindle|Android|Silk|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino"
            val regex2 = "1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw-(n|u)|c55/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(-|_)|g1 u|g560|gene|gf-5|g-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd-(m|p|t)|hei-|hi(pt|ta)|hp( i|ip)|hs-c|ht(c(-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i-(20|go|ma)|i230|iac( |-|/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |/)|klon|kpt |kwc-|kyo(c|k)|le(no|xi)|lg( g|/(k|l|u)|50|54|-[a-w])|libw|lynx|m1-w|m3ga|m50/|ma(te|ui|xo)|mc(01|21|ca)|m-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|-([1-8]|c))|phil|pire|pl(ay|uc)|pn-2|po(ck|rt|se)|prox|psio|pt-g|qa-a|qc(07|12|21|32|60|-[2-7]|i-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55/|sa(ge|ma|mm|ms|ny|va)|sc(01|h-|oo|p-)|sdk/|se(c(-|0|1)|47|mc|nd|ri)|sgh-|shar|sie(-|m)|sk-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h-|v-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl-|tdg-|tel(i|m)|tim-|t-mo|to(pl|sh)|ts(70|m-|m3|m5)|tx-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas-|your|zeto|zte-"

            if(userAgent != null && userAgent != "" && (regex1.toRegex().containsMatchIn(userAgent.lowercase()) || regex2.toRegex().containsMatchIn(userAgent.substring(0,4).lowercase()))) {
                isMobile = true;
            }

            return isMobile
        }

        fun getObjectName(): String {
            return "shashinobject"
        }

        fun getMetadataFreeformString(model: Model): String {
            return model.getAttribute("clientIP").toString()+"|"+model.getAttribute("agentName").toString()+"|"+model.getAttribute("requestResourceType").toString()+"|"+model.getAttribute("agentOS").toString()
        }

        fun parseMetadataFreeformString(freeformString: String?): FreeFormText? {
            val infoArray = freeformString?.split("|")
            var freeFormText: FreeFormText? = null

            if (infoArray != null && infoArray.size > 1) {
                val freeForm = FreeFormText()
                freeForm.setClientIP(infoArray[0])
                freeForm.setBrowser(infoArray[1])
                freeForm.setRequestResourceType(infoArray[2])
                freeForm.setOperatingSystem(infoArray[3])
                freeFormText = freeForm
            }

            return freeFormText
        }

        fun isLocalIp(testAddress: String?): Boolean {
            if (testAddress.isNullOrBlank()) {
                logger.log(
                    Level.INFO,
                    "Testing IP: null or blank",
                )
                return false
            }

            logger.log(
                Level.INFO,
                "Testing IP: $testAddress"
            )

            val address: InetAddress

            try {
                address = InetAddress.getByName(testAddress)
            } catch (exception: UnknownHostException) {
                logger.log(
                    Level.WARNING,
                    "Testing unknown IP $testAddress: ${exception.localizedMessage}"
                )
                return false
            }

            return address.isSiteLocalAddress || address.isLoopbackAddress
        }

        fun parseRememberMeCookie(cookie: String): HashMap<String,String> {
            val seriesExpiryMap = HashMap<String,String>()
            seriesExpiryMap["token"] = ""
            seriesExpiryMap["series"] = ""
            seriesExpiryMap["expires"] = ""
            seriesExpiryMap["cookieValue"] = ""

            val cookieArray = cookie.split("; ")
            for (keyValue in cookieArray) {
                val keyValueArray = keyValue.split("=")
                val key = keyValueArray[0]
                var value = ""

                if (keyValueArray.size > 1) {
                    value = keyValueArray[1]
                }

                if (key.lowercase() == "remember-me" && value.isNotEmpty()) {
                    seriesExpiryMap["cookieValue"] = value
                    seriesExpiryMap["token"] = decodePersistenceToken(value)
                    seriesExpiryMap["series"] = decodePersistenceSeries(value)
                }

                if (key.lowercase() == "expires" && value.isNotEmpty()) {
                    val datetime = SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z").parse(value)
                    seriesExpiryMap["expires"] = datetime.time.toString()
                }
            }

            return seriesExpiryMap
        }

        fun checkValidRememberMeToken(requestCookie: String?, rememberMeKey: String, userRepository: UserRepository?): User? {
            if (requestCookie != null) {
                val seriesExpiryMap = parseRememberMeCookie(requestCookie)
                var cookieValue = seriesExpiryMap["cookieValue"].toString()
                var username = seriesExpiryMap["token"]
                val series = seriesExpiryMap["series"]
                var timeStamp = if (series != null && series != "") series.toLong() else 0L
                var verifiedCookieValue = ""
                var user: User? = null

                if (cookieValue != "" && username != "") {
                    user = userRepository?.findByUsername(username)
                    if (user != null && user.getId() > 0 && user.getIsAuthorized() == true) {
                        verifiedCookieValue = verifyPersistenceToken(username.toString(), timeStamp.toString(), user.getPassword().toString(), rememberMeKey.toString()).toString()
                    }
                }

                val now = Instant.now().toEpochMilli()

                fun removeTrailingEquals(value: String): String {
                    verifiedCookieValue = value
                    var cookieEqualCount = 0
                    for (i in verifiedCookieValue.length - 1 downTo 0) {
                        var char = verifiedCookieValue[i]
                        if (char == '=') {
                            cookieEqualCount++
                        } else {
                            break
                        }
                    }

                    if (cookieEqualCount > 0) {
                        verifiedCookieValue = verifiedCookieValue.dropLast(cookieEqualCount)
                    }

                    return verifiedCookieValue
                }

                verifiedCookieValue = removeTrailingEquals(verifiedCookieValue)

                cookieValue = removeTrailingEquals(cookieValue)

                if (user != null && verifiedCookieValue != "" && verifiedCookieValue == cookieValue && timeStamp != 0L && timeStamp > now) {
                    return user
                }
            }

            return null
        }

        // See https://docs.spring.io/spring-security/reference/servlet/authentication/rememberme.html
        fun verifyPersistenceToken(token: String,series: String,hashedPassword: String,rememberKey: String): String {
            if (token.isNotBlank() && series.isNotBlank() && hashedPassword.isNotBlank() && rememberKey.isNotBlank()) {
                val tokenSignature = "$token:$series:$hashedPassword:$rememberKey"
                val digest = MessageDigest.getInstance("SHA-256")

                // Perform the hash computation
                val encodedHash = digest.digest(tokenSignature.toByteArray())

                // Convert byte array into a hexadecimal string
                val hexString = StringBuilder()
                for (b in encodedHash) {
                    val hex = String.format("%02x", b)
                    hexString.append(hex)
                }
                val tokenSeries = "$token:$series:SHA256:$hexString"
                var encodedSeries = String(Base64.getEncoder().encode(tokenSeries.toByteArray()))
                if (encodedSeries.substring(encodedSeries.length - 2) == "==") {
                    encodedSeries = encodedSeries.substring(0, encodedSeries.length - 2)
                }
                return encodedSeries
            }

            return ""
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

        fun getUnformattedDateFormat(): String {
            return "yyyyMMddHHmmss"
        }

        fun getCommonDateFormat(): String {
            return "yyyy-MM-dd HH:mm:ss"
        }

        fun getEpochDateTime() : String {
            return "1970-01-01 00:00:00"
        }

        fun isNumber(input: String): Boolean {
            return input.toDoubleOrNull() != null
        }

        fun isInteger(input: String): Boolean {
            return input.toIntOrNull() != null
        }

        fun formatToLongDate(oldDate: String): String {
            var formattedDate = ""
            try {
                val sdf = SimpleDateFormat(getCommonDateFormat())
                val newSdf = SimpleDateFormat("EEE, MMM d, yyyy")
                val temp = sdf.parse(oldDate)
                formattedDate = newSdf.format(temp)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not format date $oldDate. ${e.message}")
            }
            return formattedDate
        }

        fun formatToAbbrDate(oldDate: String): String {
            var formattedDate = ""
            try {
                val sdf = SimpleDateFormat(getCommonDateFormat())
                val newSdf = SimpleDateFormat("MMM d, yyyy")
                val temp = sdf.parse(oldDate)
                formattedDate = newSdf.format(temp)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not format date $oldDate. ${e.message}")
            }
            return formattedDate
        }

        fun formatToLongDateWithTime(oldDate: String): String {
            var formattedDate = ""
            try {
                val sdf = SimpleDateFormat(getCommonDateFormat())
                val newSdf = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm aa")
                val temp = sdf.parse(oldDate)
                formattedDate = newSdf.format(temp)
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not format date $oldDate. ${e.message}")
            }
            return formattedDate
        }

        fun capitalized(str: String?): String {
            if (str == null) {
                return ""
            }

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
                val a = floor(b)
                var aux = h1
                h1 = a * h1 + h2
                h2 = aux
                aux = k1
                k1 = a * k1 + k2
                k2 = aux
                b = 1 / (b - a)
            } while (abs(x - h1 / k1) > x * tolerance)
            return "${h1.toInt()}/${k1.toInt()}"
        }

        fun getUnformattedCurrentTimestamp(): String {
            val dtf = DateTimeFormatter.ofPattern(getUnformattedDateFormat())
            val now = LocalDateTime.now()
            return dtf.format(now)
        }

        fun getCurrentTimestamp(): String {
            val dtf = DateTimeFormatter.ofPattern(getCommonDateFormat())
            val now = LocalDateTime.now()
            return dtf.format(now)
        }

        fun getCurrentTimestampTZ(): String {
            return getCurrentTimestamp() + " " + ZoneId.systemDefault()
        }

        fun generateUUID(
            inputString:String?,
            inputStringTwo:String? = null,
            inputStringThree: String? = null,
            someDouble: Double? = null,
            someInt: Int? = null,
            inputStringFour: String? = null,
            infoOrigin: String = ""): UUID {
            val uuidInput = "$inputString-$inputStringTwo-$inputStringThree-$someDouble-$someInt-$inputStringFour"
            val uuid = UUID.nameUUIDFromBytes(uuidInput.toByteArray())

            if (infoOrigin.isEmpty() || (infoOrigin.isNotEmpty() && !infoOrigin.contains("AttributeController"))) {
                var logString = "UUID $uuid generated from input $uuidInput"
                if (infoOrigin.isNotEmpty()) {
                    logString += " from $infoOrigin"
                }

                logger.log(Level.INFO, logString)
            }
            return uuid
        }

        fun getGeoData(geocodeUrl: String,lat: String, lng: String): String? {
            val geoLookupUrl: String = geocodeUrl+"reverse?format=json&lat="+lat+"&lon="+lng+"&extratags=1&namedetails=1&accept-language=en&email=myagi.developer@gmail.com"
            return readUrl(geoLookupUrl)
        }

        // Sort by province/state then by city
        fun sortPlaceNames(metadataList: MutableIterable<Metadata>): MutableList<String> {
            val placeNamesSplitArray: MutableList<MutableList<String>> = ArrayList()

            for (metadata in metadataList) {
                val placeNameHeader = formatPlaceNameForHeader(metadata.getPlaceName()).trim()
                if (placeNameHeader.isNotEmpty()) {
                    val placeNameHeaderArray = placeNameHeader.split(", ")
                    if (placeNameHeaderArray.size > 2) {
                        placeNamesSplitArray.add(placeNameHeaderArray.toMutableList())
                    }
                }
            }

            var sortedPlaces: MutableList<String> = ArrayList()

            if (placeNamesSplitArray.size > 0) {
                val sortedArrayProvinceState = placeNamesSplitArray.sortedBy { it[0] }
                val sortedArrayCity = sortedArrayProvinceState.sortedBy { it[1] }
                sortedPlaces = sortedArrayCity.map { it.joinToString(", ") }.toMutableList()
            }

            if (sortedPlaces.isEmpty()) {
                sortedPlaces.add("")
            }

            return sortedPlaces.distinct().toMutableList()
        }

        fun getPlaceNamesForDateHeader(year: Int, month: Int, day: Int, metadataRepository:MetadataRepository, type: String = "all"): MutableList<String> {
            val metadataList = if (type == "all") {
                metadataRepository.findTimelinePlaceByDate(year, month, day) as MutableList<Metadata>
            } else {
                metadataRepository.findTimelinePlaceByDateAndType(year, month, day, type) as MutableList<Metadata>
            }

            return sortPlaceNames(metadataList)
        }

        fun processCoordinates(geocodeUrl: String, latlngStr: String?): Map<String, String?> {
            val coordinateMap = mutableMapOf<String, String?>()
            coordinateMap["lat"] = null
            coordinateMap["lng"] = null
            coordinateMap["place"] = null
            coordinateMap["timezone"] = null

            var lat: String? = null
            var lng: String? = null
            var place: String? = null
            var timezone: String? = null

            if (latlngStr != null && latlngStr.trim().isNotBlank()) {
                val latlng = latlngStr.replace("\\s".toRegex(), "")
                val latlngArr = latlng.split(",")
                val latlngRegex = "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)\\s*,\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$".toRegex()
                if (latlngRegex.matches(latlng) && latlngArr.count() == 2) {
                    lat = latlngArr[0]
                    lng = latlngArr[1]

                    val buildPlace = getPlaceNameFromJson(getGeoData(geocodeUrl,lat, lng))
                    if (buildPlace.isNotBlank()) {
                        place = buildPlace

                        val engine = TimeZoneEngine.initialize()
                        val maybeZoneId: Optional<ZoneId> = engine.query(lat.toDouble(), lng.toDouble())
                        val zone = ZoneId.of(maybeZoneId.get().id)
                        val dt = LocalDateTime.now()
                        val zdt: ZonedDateTime = dt.atZone(zone)
                        val zoneOffset = zdt.offset
                        timezone = zoneOffset.toString()
                    }
                }
            }

            if (lat != null && lng != null) {
                coordinateMap["lat"] = lat
                coordinateMap["lng"] = lng
            }
            if (place != null) {
                coordinateMap["place"] = place
            }
            if (timezone != null) {
                coordinateMap["timezone"] = timezone
            }

            return coordinateMap
        }

        fun getPlaceNameFromJson(geoDataJsonString: String?): String {
            var buildPlace = ""
            if (geoDataJsonString != null) {
                val mapper = ObjectMapper()
                try {
                    val addressObj = mapper.readTree(geoDataJsonString)

                    logger.log(Level.INFO, "Creating place name from JSON: $geoDataJsonString")

                    if (!addressObj.isNull) {
                        var name = ""
                        if (addressObj.has("name") && addressObj.get("name") != null &&
                            addressObj.get("name").textValue() != ""
                        ) {
                            buildPlace += addressObj.get("name").textValue() + " • "
                            name = addressObj.get("name").textValue()
                            logger.log(Level.INFO, "name found: ${addressObj.get("name").textValue()}")
                        }

                        if (addressObj.has("address")) {
                            if (addressObj.get("address").has("house_number") && addressObj.get("address")
                                    .get("house_number") != null && addressObj.get("address").get("house_number")
                                    .textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("house_number").textValue() + " "
                                logger.log(
                                    Level.INFO,
                                    "house_number found: ${addressObj.get("address").get("house_number").textValue()}"
                                )
                            }

                            if (addressObj.get("address").has("road") && addressObj.get("address")
                                    .get("road") != null && addressObj.get("address").get("road").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("road").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "road found: ${addressObj.get("address").get("road").textValue()}"
                                )
                            } else if (addressObj.get("address").has("neighbourhood") && addressObj.get("address")
                                    .get("neighbourhood") != null && addressObj.get("address").get("neighbourhood")
                                    .textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("neighbourhood").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "neighbourhood found: ${addressObj.get("address").get("neighbourhood").textValue()}"
                                )
                            }

                            if (addressObj.get("address").has("suburb") && addressObj.get("address")
                                    .get("suburb") != null && addressObj.get("address").get("suburb").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("suburb").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "suburb found: ${addressObj.get("address").get("suburb").textValue()}"
                                )
                            }

                            if (addressObj.get("address").has("hamlet") && addressObj.get("address")
                                    .get("hamlet") != null && addressObj.get("address").get("hamlet")
                                    .textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("hamlet").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "hamlet found: ${addressObj.get("address").get("hamlet").textValue()}"
                                )
                            } else if (addressObj.get("address").has("village") && addressObj.get("address")
                                    .get("village") != null && addressObj.get("address").get("village")
                                    .textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("village").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "village found: ${addressObj.get("address").get("village").textValue()}"
                                )
                            } else if (addressObj.get("address").has("town") && addressObj.get("address")
                                    .get("town") != null && addressObj.get("address").get("town").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("town").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "town found: ${addressObj.get("address").get("town").textValue()}"
                                )
                            } else if (addressObj.get("address").has("city") && addressObj.get("address")
                                    .get("city") != null && addressObj.get("address").get("city").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("city").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "city found: ${addressObj.get("address").get("city").textValue()}"
                                )
                            } else if (addressObj.get("address").has("municipality") && addressObj.get("address")
                                    .get("municipality") != null && addressObj.get("address").get("municipality").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("municipality").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "municipality found: ${addressObj.get("address").get("municipality").textValue()}"
                                )
                            } else if (addressObj.get("address").has("county") && addressObj.get("address")
                                    .get("county") != null && addressObj.get("address").get("county").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("county").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "county found: ${addressObj.get("address").get("county").textValue()}"
                                )
                            } else if (addressObj.get("address").has("state_district") && addressObj.get("address")
                                    .get("state_district") != null && addressObj.get("address").get("state_district").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("state_district").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "state_district found: ${addressObj.get("address").get("state_district").textValue()}"
                                )
                            } else if (addressObj.get("address").has("region") && addressObj.get("address")
                                    .get("region") != null && addressObj.get("address").get("region").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("region").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "region found: ${addressObj.get("address").get("region").textValue()}"
                                )
                            }

                            if (addressObj.get("address").has("province") && addressObj.get("address")
                                    .get("province") != null && addressObj.get("address").get("province")
                                    .textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("province").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "province found: ${addressObj.get("address").get("province").textValue()}"
                                )
                            } else if (addressObj.get("address").has("state") && addressObj.get("address")
                                    .get("state") != null && addressObj.get("address").get("state").textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("state").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "state found: ${addressObj.get("address").get("state").textValue()}"
                                )
                            }

                            if (addressObj.get("address").has("country") && addressObj.get("address")
                                    .get("country") != null && addressObj.get("address").get("country")
                                    .textValue() != ""
                            ) {
                                buildPlace += addressObj.get("address").get("country").textValue() + ", "
                                logger.log(
                                    Level.INFO,
                                    "country found: ${addressObj.get("address").get("country").textValue()}"
                                )
                            }

                            // Check if the name is repeated in some other area of the string, remove the name
                            val buildPlaceArr = buildPlace.split(" • ")
                            if (name !== "" && buildPlaceArr.size == 2 && buildPlaceArr[1].contains(buildPlaceArr[0])) {
                                buildPlace = buildPlaceArr[1]
                                logger.log(
                                    Level.INFO,
                                    "redundant name found, removing from string: ${buildPlaceArr[0]}"
                                )
                            }

                            if (buildPlace.trim().isNotBlank()) {
                                buildPlace = buildPlace.replace(", $".toRegex(), "").trim()
                                buildPlace += "; "
                            }
                        }

                        if ((buildPlace == "" || !addressObj.has("address")) && addressObj.has("display_name") && addressObj.get("display_name") != null && addressObj.get(
                                "display_name"
                            ).textValue() != ""
                        ) {
                            buildPlace = addressObj.get("display_name").textValue() + "; "
                            logger.log(Level.INFO, "display_name found: ${addressObj.get("display_name").textValue()}")
                        }

                        if (addressObj.has("class") && addressObj.get("class") != null && addressObj.get("class")
                                .textValue() != ""
                        ) {
                            buildPlace += addressObj.get("class").textValue().replace("_", " ") + ", "
                        }

                        if (addressObj.has("type") && addressObj.get("type") != null && addressObj.get("type")
                                .textValue() != "" &&
                            addressObj.get("type").textValue() != "yes" && addressObj.get("type").textValue() != "no"
                        ) {
                            buildPlace += addressObj.get("type").textValue().replace("_", " ") + ", "
                        }

                        if (buildPlace.trim().isNotBlank()) {
                            buildPlace = buildPlace.replace(", $".toRegex(), "").trim()
                        }

                        buildPlace = buildPlace.trim()
                    }
                } catch (exception: Exception) {
                    logger.log(Level.WARNING, "Could not parse place name. " + exception.localizedMessage)
                    buildPlace = ""
                }
            }

            if (buildPlace.isBlank()) {
                buildPlace = "Unknown location"
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

        fun doUtcTimeZoneConversion(time: String?, type: Boolean): String {
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

            if (day != null && day !in 1..31) {
                logger.log(Level.WARNING, "Invalid day")
                return false
            }

            if (month != null && month !in 1..12) {
                logger.log(Level.WARNING, "Invalid month")
                return false
            }

            if (year != null && year !in 1826..Calendar.getInstance().get(Calendar.YEAR)) {
                logger.log(Level.WARNING, "Invalid year")
                return false
            }

            if (time != null && time != "" && !time.matches(timeValidate)) {
                logger.log(Level.WARNING, "Invalid time")
                return false
            }

            if (offset != null && offset != "" && !(offset in timeOffsets())) {
                logger.log(Level.WARNING, "Invalid offset")
                return false
            }

            if (duration != null && duration != "") {
                val isValidWithoutHour = "([1-5]?[0-9])(:[0-5][0-9])?".toRegex().matches(duration)
                val isValidWithHour = "([0-1]?[0-9]|2[0-4]):([0-5][0-9])(:[0-5][0-9])?".toRegex().matches(duration)

                if (!isValidWithoutHour && !isValidWithHour) {
                    logger.log(Level.WARNING, "Invalid duration")
                    return false
                }
            }

            return true
        }

        fun convertDateToYMD(inDate: String): String? {
            var isShort = false
            var date: Date?

            try {
                val sourceDateFormat =
                    SimpleDateFormat("yy/MM/dd", Locale.ENGLISH)
                date = sourceDateFormat.parse(inDate)
            } catch (_: Exception) {
                try {
                    val sourceDateFormat =
                        SimpleDateFormat("yy-M-d", Locale.ENGLISH)
                    date = sourceDateFormat.parse(inDate)
                } catch (_: Exception) {
                    try {
                        val sourceDateFormat =
                            SimpleDateFormat("yy/M/d", Locale.ENGLISH)
                        date = sourceDateFormat.parse(inDate)
                    } catch (_: Exception) {
                        try {
                            val sourceDateFormat =
                                SimpleDateFormat("yy/M", Locale.ENGLISH)
                            date = sourceDateFormat.parse(inDate)
                            isShort = true
                        } catch (_: Exception) {
                            try {
                                val sourceDateFormat =
                                    SimpleDateFormat("yy-M", Locale.ENGLISH)
                                date = sourceDateFormat.parse(inDate)
                                isShort = true
                            } catch (_: Exception) {
                                try {
                                    val sourceDateFormat =
                                        SimpleDateFormat("yy/MM", Locale.ENGLISH)
                                    date = sourceDateFormat.parse(inDate)
                                    isShort = true
                                } catch (_: Exception) {
                                    try {
                                        val sourceDateFormat =
                                            SimpleDateFormat("yy-MM", Locale.ENGLISH)
                                        date = sourceDateFormat.parse(inDate)
                                        isShort = true
                                    } catch (_: Exception) {
                                        try {
                                            val sourceDateFormat =
                                                SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
                                            date = sourceDateFormat.parse(inDate)
                                        } catch (_: Exception) {
                                            try {
                                                val sourceDateFormat =
                                                    SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                                                date = sourceDateFormat.parse(inDate)
                                            } catch (_: Exception) {
                                                try {
                                                    val sourceDateFormat =
                                                        SimpleDateFormat("yyyy-M-d", Locale.ENGLISH)
                                                    date = sourceDateFormat.parse(inDate)
                                                } catch (_: Exception) {
                                                    try {
                                                        val sourceDateFormat =
                                                            SimpleDateFormat("yyyy/M/d", Locale.ENGLISH)
                                                        date = sourceDateFormat.parse(inDate)
                                                    } catch (_: Exception) {
                                                        try {
                                                            val sourceDateFormat =
                                                                SimpleDateFormat("yyyy/M", Locale.ENGLISH)
                                                            date = sourceDateFormat.parse(inDate)
                                                            isShort = true
                                                        } catch (_: Exception) {
                                                            try {
                                                                val sourceDateFormat =
                                                                    SimpleDateFormat("yyyy-M", Locale.ENGLISH)
                                                                date = sourceDateFormat.parse(inDate)
                                                                isShort = true
                                                            } catch (_: Exception) {
                                                                try {
                                                                    val sourceDateFormat =
                                                                        SimpleDateFormat("yyyy/MM", Locale.ENGLISH)
                                                                    date = sourceDateFormat.parse(inDate)
                                                                    isShort = true
                                                                } catch (_: Exception) {
                                                                    try {
                                                                        val sourceDateFormat =
                                                                            SimpleDateFormat(
                                                                                "yyyy-MM",
                                                                                Locale.ENGLISH
                                                                            )
                                                                        date = sourceDateFormat.parse(inDate)
                                                                        isShort = true
                                                                    } catch (_: Exception) {
                                                                        return null
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


            var destFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            if (isShort) {
                destFormat = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
            }

            return destFormat.format(date)

        }

        fun returnForbiddenError(response: HttpServletResponse): String {
            val jsonResponseMap = mutableMapOf<String, Any>()
            jsonResponseMap["msg"] = "Access is denied"
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern(getCommonDateFormat())
            jsonResponseMap["timestamp"] = now.format(formatter)
            jsonResponseMap["status"] = HttpStatus.FORBIDDEN
            val mapper = ObjectMapper()

            response.contentType = "application/json"
            response.status = HttpStatus.FORBIDDEN.value()
            return mapper.writeValueAsString(jsonResponseMap)
        }

        private val IP_HEADER_CANDIDATES = arrayOf(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        )

        fun getClientIp(request: HttpServletRequest?): String? {
            if (request == null) {
                return "0.0.0.0"
            }

            try {
                for (header in IP_HEADER_CANDIDATES) {
                    val ipList = request.getHeader(header)
                    if (ipList != null && ipList.isNotEmpty() && !"unknown".equals(ipList, ignoreCase = true)) {
                        val ip = ipList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                        return ip
                    }
                }

                return request.remoteAddr
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not get IP: " + e.message)
                return "0.0.0.0"
            }
        }

        fun getServerUptime(): String {
            val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
            val seconds: Long = runtimeMXBean.uptime / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            return (if (days > 0) (days.toString() + " day"+(if (days.toInt() == 1) "" else "s")+ " ") else "") + (if ((hours % 24) < 10) "0" else "") + (hours % 24) + ":" + (if ((minutes % 60) < 10) "0" else "") + (minutes % 60) + ":" + (if ((seconds % 60) < 10) "0" else "") + (seconds % 60)
        }

        fun getEndpointData(role: String, request: HttpServletRequest): MutableList<MutableMap<String, Any>> {
            val requestURL = StringBuilder(request.requestURL.toString())
            var requestOrigin = "web"
            if (requestURL.contains("api/v1")) {
                requestOrigin = "api"
            }

            val apiMapList = mutableListOf<MutableMap<String, Any>>()
            var data = mutableMapOf<String, Any>()

            val applicationContext =
                WebApplicationContextUtils.getRequiredWebApplicationContext(request.session.servletContext)
            val requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
            val requestMap = requestMappingHandlerMapping.handlerMethods

            // Get lists from WebSecurityConfig
            val superEndpoints = MultiSecurityConfig.superList
            superEndpoints.forEachIndexed { i, _ ->
                if(superEndpoints[i].contains("**")) {
                    superEndpoints[i] = superEndpoints[i].replace("**", "(.*)")
                }
            }
            val adminEndpoints = MultiSecurityConfig.adminList
            adminEndpoints.forEachIndexed { i, _ ->
                if(adminEndpoints[i].contains("**")) {
                    adminEndpoints[i] = adminEndpoints[i].replace("**", "(.*)")
                }
            }
            val allRoleEndpoints = MultiSecurityConfig.allRoleList
            allRoleEndpoints.forEachIndexed { i, _ ->
                if(allRoleEndpoints[i].contains("**")) {
                    allRoleEndpoints[i] = allRoleEndpoints[i].replace("**", "(.*)")
                }
            }

            requestMap.forEach { (key, value) ->

                if (key.toString().contains("/api/v1/", ignoreCase = true) && !key.toString().contains("/docs/", ignoreCase = true)) {
                    if (requestOrigin == "web") {
                        data["role"] = "Public"
                    } else {
                        data["authorizedRoles"] = arrayOf("public")
                    }


                    // Order is important! Highest to lowest roles
                    for (superEndpoint in superEndpoints) {
                        val matcher = superEndpoint.toRegex()
                        if (matcher.findAll(key.toString()).count() > 0) {
                            if (requestOrigin == "web") {
                                data["role"] = "Super"
                            } else {
                                data["authorizedRoles"] = arrayOf("super")
                            }
                            break
                        }
                    }

                    for (adminEndpoint in adminEndpoints) {
                        val matcher = adminEndpoint.toRegex()
                        if (matcher.findAll(key.toString()).count() > 0) {
                            if (requestOrigin == "web") {
                                data["role"] = "Admin and Super"
                            } else {
                                data["authorizedRoles"] = arrayOf("admin", "super")
                            }
                            break
                        }
                    }

                    if ((data["role"].toString()).isNotBlank() || (data["authorizedRoles"]!! as Array<*>).isNotEmpty()) {
                        for (allRoleEndpoint in allRoleEndpoints) {
                            val matcher = allRoleEndpoint.toRegex()
                            if (matcher.findAll(key.toString()).count() > 0) {
                                if (requestOrigin == "web") {
                                    data["role"] = "Super, Admin and User"
                                } else {
                                    data["authorizedRoles"] = arrayOf("super", "admin", "user")
                                }
                                break
                            }
                        }
                    }

                    if (requestOrigin == "web") {
                        data["produces"] = ""
                        data["description"] = ""

                        data["roleAnchor"] = data["role"].toString().lowercase().replace("\\s".toRegex(), "")

                        data["rolePath"] =
                            generateUUID(key.toString(), "", "", 0.0, 0, "", "endpoint article creation")
                                .toString()
                        data["controller"] = value.toString()
                        val apiRegex = "/api/v1/.*]".toRegex()
                        var apiMatchResult = apiRegex.find(key.toString())
                        data["order"] = data["role"].toString() + apiMatchResult!!.value

                        val endpointArray = key.toString().split(",")
                        if (endpointArray.size > 0) {
                            if (endpointArray.size == 2) {
                                data["produces"] = endpointArray[1].drop(11).dropLast(2).replace(" || ", ", ")
                            }

                            apiMatchResult = apiRegex.find(endpointArray[0])
                            val apiCall = apiMatchResult?.value?.dropLast(1)
                            val apiCalls = apiCall.toString().split("||")
                            val pathArray = mutableListOf<String>()
                            for (path in apiCalls) {
                                if (path.trim().startsWith("/api/v1/")) {
                                    pathArray.add(path.trim())
                                }
                            }

                            if (pathArray.size > 0) {
                                data["apiCall"] = pathArray.joinToString(separator = ", ")
                            }

                            if (!value.getMethodAnnotation(RouterOperation::class.java)?.operation?.description.isNullOrBlank()) {
                                data["description"] =
                                    value.getMethodAnnotation(RouterOperation::class.java)?.operation?.description.toString()
                            } else if (!value.getMethodAnnotation(RouterOperation::class.java)?.operation?.summary.isNullOrBlank()) {
                                data["description"] =
                                    value.getMethodAnnotation(RouterOperation::class.java)?.operation?.summary.toString()
                            }

                            val apiCallArray = endpointArray[0].split(" ")
                            if (apiCallArray.size > 0) {
                                val requestType = apiCallArray[0].drop(1).trim()
                                data["requestType"] = requestType

                                var badgeStyle = "badge bg-success"
                                if (requestType.lowercase() == "get") {
                                    badgeStyle = "badge bg-primary"
                                } else if (requestType.lowercase() == "post") {
                                    badgeStyle = "badge bg-success"
                                } else if (requestType.lowercase() == "put") {
                                    badgeStyle = "badge bg-warning"
                                } else if (requestType.lowercase() == "delete") {
                                    badgeStyle = "badge bg-danger"
                                }

                                data["badgeStyle"] = badgeStyle
                            }
                        }
                    } else {
                        data["requestType"] = ""
                        data["endpoints"] = ""
                        data["consumes"] = ""
                        data["produces"] = ""
                        data["summary"] = ""

                        val apiRegex = "/api/v1/.*]".toRegex()

                        val endpointArray = key.toString().split(",")
                        if (endpointArray.isNotEmpty()) {
                            for (endpointParts in endpointArray) {

                                // Request Type and API calls
                                if (endpointParts.contains("GET") ||
                                    endpointParts.contains("DELETE") ||
                                    endpointParts.contains("POST") ||
                                    endpointParts.contains("PUT") ||
                                    endpointParts.contains("PATCH") ||
                                    endpointParts.contains("HEAD"))
                                {
                                    val requestTypeArray = endpointParts.drop(1).trim().split(" ")
                                    val requestType = requestTypeArray[0]
                                    data["requestType"] = requestType

                                    val apiMatchResult = apiRegex.find(endpointParts)
                                    val apiCall = apiMatchResult?.value?.dropLast(1)
                                    val apiCalls = apiCall.toString().split("||")
                                    val pathArray = mutableListOf<String>()

                                    for (path in apiCalls) {
                                        if (path.trim().startsWith("/api/v1/")) {
                                            pathArray.add(path.trim())
                                        }
                                    }

                                    if (pathArray.size > 0) {
                                        data["endpoints"] = pathArray
                                    }
                                }

                                // Consumes
                                if (endpointParts.contains("consumes")) {
                                    val consumesStr = endpointParts.drop(11).dropLast(1)
                                    var consumesArray = consumesStr.split("||")
                                    consumesArray = consumesArray.map{it.trim()}

                                    data["consumes"] = consumesArray
                                }

                                // Produces
                                if (endpointParts.contains("produces")) {
                                    val producesStr = endpointParts.drop(11).dropLast(2)
                                    var producesArray = producesStr.split("||")
                                    producesArray = producesArray.map{it.trim()}

                                    data["produces"] = producesArray
                                }
                            }

                            // Description
                            if (value.getMethodAnnotation(RouterOperation::class.java)?.operation?.summary != null) {
                                data["summary"] =
                                    value.getMethodAnnotation(RouterOperation::class.java)?.operation?.summary.toString()
                            }
                        }
                    }

                    if ((requestOrigin == "web" && data["role"].toString().lowercase().contains("public")) ||
                        (data["authorizedRoles"] != null && (data["authorizedRoles"] as Array<*>).contains("public"))
                    ) {
                        apiMapList.add(data)
                    }

                    if (role == "ROLE_SUPER" &&
                        ((requestOrigin == "web" && data["role"].toString().lowercase().contains("super")) ||
                        (data["authorizedRoles"] != null && (data["authorizedRoles"] as Array<*>).contains("super")))
                    ) {
                        apiMapList.add(data)
                    }

                    if (role == "ROLE_ADMIN" &&
                        ((requestOrigin == "web" && data["role"].toString().lowercase().contains("admin")) ||
                        (data["authorizedRoles"] != null && (data["authorizedRoles"] as Array<*>).contains("admin")))
                    ) {
                        apiMapList.add(data)
                    }

                    if (role == "ROLE_USER" &&
                        ((requestOrigin == "web" && data["role"].toString().lowercase().contains("user")) ||
                        (data["authorizedRoles"] != null && (data["authorizedRoles"] as Array<*>).contains("user")))
                    ) {
                        apiMapList.add(data)
                    }

                    data = mutableMapOf()
                }
            }

            return apiMapList.sortedBy { it["order"].toString() } as MutableList<MutableMap<String, Any>>
        }

        private fun readUrl(urlString: String): String? {
            var result: String? = null

            try {
                result = URL(urlString).readText()
            } catch(e: Exception) {
                logger.log(Level.WARNING, "Could not read URL: " + e.message)
            }

            return result
        }

        // 1l - 1ms
        fun getCacheControl(ttl: String): CacheControl {
            var cache = CacheControl.noCache()

            if (ttl != "none" && ttl.length > 1) {
                var measure = ttl.last().lowercase()
                var unit = ttl.dropLast(1).toLongOrNull()

                var timeUnit: TimeUnit? = null
                if (measure == "d") {
                    timeUnit = TimeUnit.DAYS
                } else if (measure == "h") {
                    timeUnit = TimeUnit.HOURS
                } else if (measure == "m") {
                    timeUnit = TimeUnit.MINUTES
                } else if (measure == "s") {
                    timeUnit = TimeUnit.SECONDS
                }

                if (timeUnit != null && unit != null) {
                    cache = CacheControl.maxAge(unit, timeUnit)
                }
            }

            return cache
        }

        // https://docs.github.com/en/rest/repos/repos?apiVersion=2022-11-28#list-repository-tags
        fun getReleases(githubKey: String): MutableList<Map<String, Any>>? {
            var url = "https://api.github.com/repos/michaelyagi/shashin/releases"
            var array: MutableList<Map<String, Any>>? = mutableListOf<Map<String, Any>>()

            try {
                val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                connection.requestMethod = "GET"

                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("Authorization", "Bearer $githubKey")

                BufferedReader(
                    InputStreamReader(connection.inputStream, "utf-8")
                ).use { br ->
                    val responseBuilder = StringBuilder()
                    var responseLine: String?
                    while (br.readLine().also { responseLine = it } != null) {
                        responseBuilder.append(responseLine!!.trim { it <= ' ' })
                    }

                    val jsonString = responseBuilder.toString()
                    array = Gson().fromJson(jsonString, array?.javaClass)
                }

                val responseCode: Int = connection.responseCode

                if (responseCode != 200) {
                    logger.log(Level.WARNING, "Could not process GitHub request.")
                }
            } catch (e: IOException) {
                logger.log(Level.WARNING, "Could not process GitHub request: " + e.message)
            }

            return array
        }

        fun formatPlaceNameForHeader(placeDescription: String?): String {
            var placeNameHeader = ""

            if (!placeDescription.isNullOrBlank()) {
                val placeDescriptionArray = placeDescription.split(";")

                val placeName = if (placeDescriptionArray.size > 1) {
                    placeDescriptionArray[placeDescriptionArray.size-2]
                } else {
                    placeDescription
                }

                val placeNameArray = placeName.split(",")

                if (placeNameArray.size >= 2) {
                    var processedPlaceName = placeNameArray[placeNameArray.size - 2].trim() + ", " + placeNameArray[placeNameArray.size - 1].trim()
                    if (placeNameArray.size > 2) {
                        processedPlaceName = placeNameArray[placeNameArray.size - 3].trim() + ", " + placeNameArray[placeNameArray.size - 2].trim() + ", " + placeNameArray[placeNameArray.size - 1].trim()
                    }

                    val processedPlaceNameArr = processedPlaceName.split(" • ")
                    placeNameHeader = if (processedPlaceNameArr.size > 1) {
                        processedPlaceNameArr[1]
                    } else {
                        processedPlaceName
                    }

                    // Process city, remove numbers at start of string
                    val placeNameProcessedArray = placeNameHeader.split(",")

                    val city = placeNameProcessedArray[0].trim()

                    if (city.isNotEmpty()) {
                        val cityArray = city.split(" ")
                        if (cityArray.size > 1) {
                            var cityString = ""
                            for ((index, citySubString) in cityArray.withIndex()) {
                                if (citySubString.contains("[0-9]".toRegex()) && index != cityArray.size - 1) {
                                    cityString = ""
                                } else {
                                    cityString += "$citySubString "
                                }
                            }
                            cityString = cityString.trim()

                            placeNameHeader = if (cityString.isNotEmpty()) {
                                if (placeNameArray.size == 2) {
                                    cityString + ", " + placeNameProcessedArray[1].trim()
                                } else {
                                    cityString + ", " + placeNameProcessedArray[1].trim() + ", " + placeNameProcessedArray[2].trim()
                                }
                            } else {
                                if (placeNameArray.size == 2) {
                                    placeNameProcessedArray[1].trim()
                                } else {
                                    placeNameProcessedArray[1].trim() + ", " + placeNameProcessedArray[2].trim()
                                }
                            }
                        }
                    }
                } else {
                    val processedPlaceNameArr = placeName.split(" • ")
                    placeNameHeader = if (processedPlaceNameArr.size > 1) {
                        processedPlaceNameArr[1].trim()
                    } else {
                        placeName.trim()
                    }
                }
            }

//            if (!placeNameHeader.contains(",")) {
//                placeNameHeader = ""
//            }

            return placeNameHeader
        }
    }
}