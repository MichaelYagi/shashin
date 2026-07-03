package com.miyagi.shashin.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class NetworkUtils {


    companion object {
        private var logger: Logger = Logger.getLogger(NetworkUtils::class.simpleName)

        private fun pingURL(url: String, requestProperties: Map<String,String>? = null, timeoutInMS: Int = 0, requestMethod: String = "HEAD", jsonInputString: String = "", getResponse: Boolean = false): Boolean {
            var urlcopy = url
            urlcopy = urlcopy.replaceFirst(
                "^https".toRegex(),
                "http"
            ) // Otherwise an exception may be thrown on invalid SSL certificates.
            return try {
                val connection: HttpURLConnection = URL(urlcopy).openConnection() as HttpURLConnection
                connection.connectTimeout = timeoutInMS
                connection.readTimeout = timeoutInMS
                connection.requestMethod = requestMethod

                if (requestProperties != null) {
                    for (requestProperty in requestProperties) {
                        connection.setRequestProperty(requestProperty.key, requestProperty.value)
                    }
                }

                if (jsonInputString.isNotEmpty()) {
                    connection.setDoOutput(true)
                    val os: OutputStream = connection.outputStream
                    os.write(jsonInputString.toByteArray())
                    os.flush()
                    os.close()
                }

                if (getResponse) {
                    var response = ""
                    BufferedReader(
                        InputStreamReader(connection.inputStream, "utf-8")
                    ).use { br ->
                        val responseBuilder = StringBuilder()
                        var responseLine: String?
                        while (br.readLine().also { responseLine = it } != null) {
                            responseBuilder.append(responseLine!!.trim { it <= ' ' })
                        }
                        response = responseBuilder.toString()
                    }
                    logger.log(Level.INFO, "pingURL - response: $response")
                }

                val responseCode: Int = connection.responseCode

                responseCode in 200..399
            } catch (exception: IOException) {
                logger.log(Level.WARNING, "pingURL - exception for $urlcopy: ${exception.message}")
                false
            }
        }

        fun checkArgusConnection(argusServer: String?, argusKey: String?): Boolean {
            val timingStart = Date()
            var available = false

            if (!argusKey.isNullOrBlank() && !argusServer.isNullOrBlank()) {
                val requestProperties: Map<String, String> = mapOf("X-API-Key" to argusKey)
                // Hit an authenticated endpoint (GET, not HEAD) so an invalid key fails the check —
                // /api/health is unauthenticated and would pass with any key. A bad key returns
                // 401/403, which falls outside the 200..399 success range.
                available = pingURL(argusServer.trimEnd('/') + "/api/review/count", requestProperties, 5000, "GET")
            }

            val timingEnd = Date()
            val diff: Long = timingEnd.time - timingStart.time

            val processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
            logger.log(Level.INFO, "checkArgusConnection - processing time: $processingTime")

            return available
        }

        // Returns connectivity + which Argus models are active.
        // Map keys: "connected" (server reachable AND key valid), "face"/"object" (models active).
        // NOTE: /api/health is unauthenticated, so it returns 200 for any key (even a fake one).
        // To actually validate the key we first hit an authenticated endpoint (/api/review/count);
        // a bad key returns 401/403 there and throws, leaving "connected" false.
        fun checkArgusModels(argusServer: String?, argusKey: String?): Map<String, Boolean> {
            val result = mutableMapOf("connected" to false, "face" to false, "object" to false)
            if (argusKey.isNullOrBlank() || argusServer.isNullOrBlank()) return result

            try {
                val webClient = WebClient.create(argusServer.trimEnd('/') + "/")

                // Validate the key against an authenticated endpoint (throws on 401/403/bad key)
                webClient.get()
                    .uri("api/review/count")
                    .header("X-API-Key", argusKey)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()

                // Key is valid + server reachable; read models from the (public) health endpoint
                val response = webClient.get()
                    .uri("api/health")
                    .header("X-API-Key", argusKey)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()

                result["connected"] = true
                if (!response.isNullOrBlank()) {
                    val json = ObjectMapper().readTree(response)
                    result["face"] = json.has("face_model") && !json["face_model"].isNull
                    result["object"] = json.has("object_model") && !json["object_model"].isNull
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "checkArgusModels - exception: ${e.message}")
            }

            return result
        }

        fun checkCircleCiStatus(apiKey: String?): Boolean {
            var passing = false

            if (!apiKey.isNullOrBlank()) {
                val response: ResponseEntity<String>?
                try {
                    val webClient =
                        WebClient.create("https://circleci.com/api/v1.1/project/github/MichaelYagi/shashin?limit=1&offset=0&filter=completed&circle-token=$apiKey")
                    response = webClient.get()
                        .retrieve()
                        .toEntity(String::class.java)
                        .block()

                    if (response != null) {
                        val jsonResult = response.body
                        val mapper = ObjectMapper()
                        val jsonObj = mapper.readTree(jsonResult)
                        val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Array<Map<String, Any>>>() {})
                        // :retried, :canceled, :infrastructure_fail, :timedout, :not_run, :running, :failed, :queued, :not_running, :no_tests, :fixed, :success
                        if (resultMap[0].containsKey("status") && resultMap[0]["status"] == "success") {
                            passing = true
                        } else {
                            logger.log(Level.WARNING, "CircleCI failed: $jsonResult")
                        }
                    }
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Error checking CircleCI: ${e.message}")
                    passing = false
                }
            }

            return passing
        }

        fun checkNominatimConnection(nominatimUrl: String?): Boolean {
            var available = false
            if (!nominatimUrl.isNullOrBlank()) {
                available = pingURL(nominatimUrl, null, 1000)
//
//                val response: ResponseEntity<String>?
//                try {
//                    val webClient = WebClient.create(nominatimUrl)
//                    response = webClient.get()
//                        .retrieve()
//                        .toEntity(String::class.java)
//                        .block()
//
//                    if (response != null) {
//                        val jsonResult = response.body
//                        val mapper = ObjectMapper()
//                        val jsonObj = mapper.readTree(jsonResult)
//                        val resultMap = mapper.convertValue(jsonObj, object : TypeReference<Map<String, Any>>() {})
//                        if (resultMap.containsKey("status") && resultMap["status"] == 0) {
//                            available = true
//                        }
//                    }
//                } catch (e: Exception) {
//                    available = false
//                }
            }

            return available
        }
    }
}