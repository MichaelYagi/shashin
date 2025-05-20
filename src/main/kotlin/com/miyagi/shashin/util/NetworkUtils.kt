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

        fun checkCompreFaceConnection(compreFaceServer: String?, compreFaceKey: String?): Boolean {
            val timingStart = Date()
            var available = false

            if (!compreFaceKey.isNullOrBlank() && !compreFaceServer.isNullOrBlank()) {
                val requestProperties: Map<String, String> = mapOf("x-api-key" to compreFaceKey)
                available = pingURL(compreFaceServer + "api/v1/recognition/faces?page=0&size=1&subject=0", requestProperties, 1000)
            }

            val timingEnd = Date()
            val diff: Long = timingEnd.time - timingStart.time

            val processingTime = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
            logger.log(Level.INFO, "checkCompreFaceConnection - processing time: $processingTime")

            return available
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