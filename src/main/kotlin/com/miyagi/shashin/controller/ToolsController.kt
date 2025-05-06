package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.*
import com.sun.management.OperatingSystemMXBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.HealthComponent
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.info.BuildProperties
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.lang.management.ManagementFactory
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.concurrent.TimeUnit
import kotlin.collections.iterator
import kotlin.collections.set


@Controller
class ToolsController {

    @Autowired
    private lateinit var metaRepository: MetadataRepository

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Value("\${app.circleci.key}")
    private var circleCiKey: String? = null

    @Value("\${app.github.key}")
    private var githubKey: String? = null

    @Autowired
    private var healthEndpoint: HealthEndpoint? = null

    private var logger: Logger = Logger.getLogger(ToolsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @RequestMapping(value = ["/releases", "/api/v1/releases"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getReleases(model: Model): ResponseEntity<String> {
        var response = mutableMapOf<String, Any?>()
        response["releases"] = mutableListOf<Map<String, Any>>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && githubKey != null && githubKey != "" && currentUserObj.getAuthority()!! == "ROLE_SUPER") {
            val array = TextUtils.getReleases(githubKey!!)
            if (array != null && array.isNotEmpty()) {
                response["releases"] = array
                response["status"] = ApiResponse.SUCCESS.status
            }
        }

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
//            .cacheControl(CacheControl.maxAge(1, TimeUnit.SECONDS))
//            .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
            .body(json)
    }

    @CrossOrigin(origins = ["*"], originPatterns = [], allowedHeaders = ["*"], methods = [RequestMethod.GET], maxAge = 3600)
    @RequestMapping(value = ["/api/v1/tags"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getTags(): ResponseEntity<String> {
        var response = mutableMapOf<String, Any?>()
        response["tags"] = mutableListOf<Map<String, Any>>()
        response["status"] = ApiResponse.FAIL.status
        response["msg"] = ""

        if (githubKey != null && githubKey != "") {
            val array = TextUtils.getReleases(githubKey!!)
            if (array != null && array.isNotEmpty()) {
                val tags = mutableListOf<Map<String, Any>>()
                for (item in array) {
                    if (item.containsKey("tag_name") && item.containsKey("body")) {
                        val tag = mutableMapOf<String, Any>()
                        tag["tag_name"] = item.getValue("tag_name")
                        tag["body"] = item.getValue("body")
                        tags.add(tag)
                    }
                }
                response["tags"] = tags
                response["status"] = ApiResponse.SUCCESS.status
            }
        } else {
            response["msg"] = "Could not complete request"
        }

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
//            .cacheControl(CacheControl.maxAge(1, TimeUnit.SECONDS))
            .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
            .body(json)
    }

    @RequestMapping(value = ["/bcrypt/{strtobcrypt}"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getBcryptVal(@PathVariable(required = true) strtobcrypt: String): String {

        val response = mutableMapOf<String, Any?>()

        var encodedStr = ""

        if (strtobcrypt.isNotEmpty()) {

            var bcrypt = BCryptPasswordEncoder()

            encodedStr = bcrypt.encode(strtobcrypt)
        }

        response["msg"] = "Success"
        response["status"] = ApiResponse.SUCCESS.status
        response["bcryptValue"] = encodedStr

        return mapper.writeValueAsString(response)
    }

    @GetMapping("/bcrypt")
    fun getBcrypt(model: Model): String {

        model["activePage"] = "bcrypt"
        model["titleDescriptor"] = "bcrypt"

        return "bcrypt"
    }

    @RequestMapping(value = ["/status/compreface"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun checkComprefacestatus(model: Model): String {
        val settings = model.getAttribute("settings") as Settings?
        val currentUserObj = model.getAttribute("currentUser") as User?

        var status = true

        if (settings != null && currentUserObj != null && currentUserObj.getAuthority() != null && (currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_SUPER")) {
            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )

            status = if ((settings.getCompreFaceKey() == "" || settings.getCompreFaceKey() == null) && (settings.getCompreFaceServer() == "" || settings.getCompreFaceServer() == null)) {
                true
            } else if ((settings.getCompreFaceKey() == "" || settings.getCompreFaceKey() == null) && settings.getCompreFaceServer() != "") {
                false
            } else if (settings.getCompreFaceKey() != "" && (settings.getCompreFaceServer() == "" || settings.getCompreFaceServer() == null)) {
                false
            } else {
                faceRecogServicesAvailable
            }
        }

        return "{\"status\":$status,\"msg\":\"\"}"
    }

    @RequestMapping(value = ["/api/v1/status"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getStatusApi(model: Model,@RequestParam ignoreBuildCheck: Optional<Boolean>): String {
        val ignoreBuild = ignoreBuildCheck.orElse(false)
        val healthData = buildHealthData(model,ignoreBuild)
        return "{\"status\":\""+healthData["status"]+"\"}"
    }

    @GetMapping("/health")
    fun getHealth(model: Model,@RequestParam ignoreBuildCheck: Optional<Boolean>): String {
        val ignoreBuild = ignoreBuildCheck.orElse(false)
        for ((k, v) in buildHealthData(model,ignoreBuild)) {
            model[k] = v!!
        }

        model["localServerTime"] = TextUtils.getCurrentTimestampTZ()

        return "health"
    }

    @RequestMapping(value = ["/api/v1/health"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getHealthApi(model: Model): String {
        return mapper.writeValueAsString(buildHealthData(model))
    }

    private fun buildHealthData(model: Model, ignoreBuildCheck: Boolean = false): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val serverTimingStart = Date()

        var status = "OK"

        response["status"] = status

        response["serverTiming"] = "00:00:000"

        val metricsUtil = MetricsUtil()
        metricsUtil.start("health endpoint")
        val health: HealthComponent? = healthEndpoint!!.health()
        if (health == null || health.status.code != "UP") {
            status = "FAIL"
        }
        metricsUtil.end()

        response["uptimeText"] = TextUtils.getServerUptimeFormatted()

        metricsUtil.start("nominatim endpoint")
        val nominatimTimingStart = Date()
        val reachable: Boolean = NetworkUtils.checkNominatimConnection(geocodeUrl+"status.php?format=json")
        if (reachable) {
            response["nominatimAvailable"] = "OK"
        } else {
            response["nominatimAvailable"] = "FAIL"
            status = "FAIL"
        }
        val nominatimTimingEnd = Date()
        val nominatimTimingDiff: Long = nominatimTimingEnd.time - nominatimTimingStart.time
//        response["nominatimTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(nominatimTimingDiff))
        logger.log(Level.INFO, "HealthEP - Nominatim connection time: ${SimpleDateFormat("mm:ss.SSS").format(Date(nominatimTimingDiff))}")
        metricsUtil.end()

        metricsUtil.start("compreface endpoint")
        // If enabled - status fail if not available
        val settings = model.getAttribute("settings") as Settings?
        if (settings?.getCompreFaceKey() != null &&
            settings.getCompreFaceKey() != "" &&
            settings.getCompreFaceServer() != null &&
            settings.getCompreFaceServer() != "")
        {
            val compreFaceTimingStart = Date()
            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )
            if (faceRecogServicesAvailable) {
                response["compreFaceAvailable"] = "OK"
            } else {
                response["compreFaceAvailable"] = "FAIL"
                status = "FAIL"
            }
            val compreFaceTimingEnd = Date()
            val compreFaceTimingDiff: Long = compreFaceTimingEnd.time - compreFaceTimingStart.time
//            response["compreFaceTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(compreFaceTimingDiff))
            logger.log(Level.INFO, "HealthEP - CompreFace connection time: ${SimpleDateFormat("mm:ss.SSS").format(Date(compreFaceTimingDiff))}")
        }
        metricsUtil.end()

        val dbTimingStart = Date()
        var sqlLiteQueryCount = 0

        metricsUtil.start("SQL check")
        try {
            val metadataResult = metaRepository.findAllByOffsetAndLimit(0, 500)
            sqlLiteQueryCount = metadataResult.count()
            response["sqlLiteAvailable"] = "OK"
            response["sqlLiteQueryCount"] = sqlLiteQueryCount
        } catch (e: Exception) {
            response["sqlLiteAvailable"] = "FAIL"
            response["sqlLiteQueryCount"] = sqlLiteQueryCount
            status = "FAIL"
            logger.log(Level.WARNING, "HealthEP - Error querying SQLLite: ${e.message}")
        }
        val dbTimingEnd = Date()
        metricsUtil.end()

        val dbTimingDiff: Long = dbTimingEnd.time - dbTimingStart.time
//        response["sqlLiteQueryTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(dbTimingDiff))
        logger.log(Level.INFO, "HealthEP - SQLite query time for $sqlLiteQueryCount records: ${SimpleDateFormat("mm:ss.SSS").format(Date(dbTimingDiff))}")

        response["buildVersion"] = if (buildProperties != null) buildProperties?.version.toString() else "Missing"

        if (ignoreBuildCheck == false) {
            metricsUtil.start("circleci endpoint")
            val circleciTimingStart = Date()
            val passing: Boolean = NetworkUtils.checkCircleCiStatus(circleCiKey)
            if (passing) {
                response["circleCIBuild"] = "OK"
            } else {
                response["circleCIBuild"] = "FAIL"
                // Don't include as part of status, credits might run out resulting
                // status = "FAIL"
            }
            val circleciTimingEnd = Date()
            val circleciTimingDiff: Long = circleciTimingEnd.time - circleciTimingStart.time
//        response["circleCIBuildTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(circleciTimingDiff))
            logger.log(
                Level.INFO,
                "HealthEP - CircleCI connection time: ${SimpleDateFormat("mm:ss.SSS").format(Date(circleciTimingDiff))}"
            )
            metricsUtil.end()
        }

        metricsUtil.start("system check")
        val systemMap = mutableMapOf<String, Any?>()
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        systemMap["initialMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.init.toDouble() / 1073741824)
        systemMap["usedHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.used.toDouble() / 1073741824)
        systemMap["maxHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.max.toDouble() / 1073741824)
        systemMap["committedMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.committed.toDouble() / 1073741824)

        val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

        val cores = Runtime.getRuntime().availableProcessors()
        if (cores < 1) {
            status = "FAIL"
        }
        systemMap["availableCores"] = cores

//        println("Process CPU load:"+(osMXBean.processCpuLoad * 100).toInt())
//        println("System CPU load:"+(osMXBean.cpuLoad * 100).toInt())
        systemMap["processCpuLoadPercent"] = (osMXBean.processCpuLoad * 100).toInt()
        @Suppress("DEPRECATION")
        systemMap["systemCpuLoadPercent"] = (osMXBean.systemCpuLoad * 100).toInt()
        systemMap["os"] = System.getProperty("os.name") + " v" + System.getProperty("os.version") + " " + System.getProperty("os.arch")
        systemMap["utcTimestampMS"] = System.currentTimeMillis()
        response["system"] = systemMap

        val serverTimingEnd = Date()

        val serverTimingDiff: Long = serverTimingEnd.time - serverTimingStart.time

        response["serverTiming"] = SimpleDateFormat("mm:ss:SSS").format(Date(serverTimingDiff))
        logger.log(Level.INFO, "HealthEP - Total request time: ${SimpleDateFormat("mm:ss:SSS").format(Date(serverTimingDiff))}")
        metricsUtil.end()

        response["status"] = status

        return response
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    @RequestMapping(value = ["/console/log"], method = [RequestMethod.POST], consumes = ["application/json"])
    @ResponseBody
    fun writeConsoleToLog(@RequestBody requestBody: JsonNode): String {
        val consoleLogMapper = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (consoleLogMapper.containsKey("consoleType") && consoleLogMapper.containsKey("log") && consoleLogMapper.containsKey("tag")) {
            // error: 0, info: 1, log: 2, warn: 3
            val type = consoleLogMapper["consoleType"].toString().toInt()
            val log = consoleLogMapper["log"].toString()
            val tag = consoleLogMapper["tag"].toString()

            when (type) {
                0 -> {
                    logger.log(Level.SEVERE, "$log. Tag: $tag")
                }
                1 -> {
                    logger.log(Level.INFO, "$log. Tag: $tag")
                }
                2 -> {
                    logger.log(Level.INFO, "$log. Tag: $tag")
                }
                3 -> {
                    logger.log(Level.WARNING, "$log. Tag: $tag")
                }
                else -> {
                    logger.log(Level.INFO, "$log. Tag: $tag")
                }
            }

            return "{\"status\": \"success\",\"msg\":\"\"}"
        }

        return "{\"status\": \"fail\",\"msg\":\"\"}"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    @RequestMapping(value = ["/api/v1/endpoints"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getApiEndpoints(model: Model, request: HttpServletRequest): String {

        val currentUserObj = model.getAttribute("currentUser") as User?
        var apiMapList = mutableListOf<MutableMap<String, Any>>()

        if (currentUserObj?.getAuthority() != null &&
            (currentUserObj.getAuthority()!! == "ROLE_SUPER" || currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_USER")) {

            apiMapList = TextUtils.getEndpointData(currentUserObj.getAuthority()!!,request)
        }

        return mapper.writeValueAsString(apiMapList)
    }

    private fun roundOffDecimal(number: Double): Any {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }
}