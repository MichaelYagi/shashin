package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.configuration.MultiSecurityConfig
import com.miyagi.shashin.model.*
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.*
import com.sun.management.OperatingSystemMXBean
import org.springdoc.core.annotations.RouterOperation
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
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.management.ManagementFactory
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import jakarta.servlet.http.HttpServletRequest


@Controller
class ToolsController {

    @Autowired
    private lateinit var metaRepository: MetadataRepository

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Value("\${app.circleci.key}")
    private lateinit var circleCiKey: String

    @Autowired
    private var healthEndpoint: HealthEndpoint? = null

    private var logger: Logger = Logger.getLogger(ToolsController::class.simpleName)

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

    @GetMapping("/health")
    fun getHealth(model: Model): String {

        for ((k, v) in buildHealthData(model)) {
            model[k] = v!!
        }

        model["localServerTime"] = TextUtils.getCurrentTimestampTZ()

        return "health"
    }

    @RequestMapping(value = ["/status/compreface"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
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

    @RequestMapping(value = ["/api/v1/status"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getStatusApi(model: Model): String {
        val healthData = buildHealthData(model)
        return "{\"status\":\""+healthData["status"]+"\"}"
    }

    @RequestMapping(value = ["/api/v1/health"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getHealthApi(model: Model): String {
        return mapper.writeValueAsString(buildHealthData(model))
    }

    private fun buildHealthData(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val requestTimingStart = Date()

        var status = "OK"

        response["status"] = status

        response["utcTimestampMS"] = System.currentTimeMillis()

        val health: HealthComponent? = healthEndpoint!!.health()
        if (health == null || health.status.code != "UP") {
            status = "FAIL"
        }

        response["uptime"] = TextUtils.getServerUptime()

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

        val dbTimingStart = Date()
        var sqlLiteQueryCount = 0

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

        val dbTimingDiff: Long = dbTimingEnd.time - dbTimingStart.time
//        response["sqlLiteQueryTiming"] = SimpleDateFormat("mm:ss.SSS").format(Date(dbTimingDiff))
        logger.log(Level.INFO, "HealthEP - SQLite query time for $sqlLiteQueryCount records: ${SimpleDateFormat("mm:ss.SSS").format(Date(dbTimingDiff))}")

        response["buildVersion"] = if (buildProperties != null) buildProperties?.version.toString() else "Missing"

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
        logger.log(Level.INFO, "HealthEP - CircleCI connection time: ${SimpleDateFormat("mm:ss.SSS").format(Date(circleciTimingDiff))}")

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
        response["system"] = systemMap

        val requestTimingEnd = Date()

        val requestTimingDiff: Long = requestTimingEnd.time - requestTimingStart.time

        response["responseTime"] = SimpleDateFormat("mm:ss:SSS").format(Date(requestTimingDiff))
        logger.log(Level.INFO, "HealthEP - Total request time: ${SimpleDateFormat("mm:ss:SSS").format(Date(requestTimingDiff))}")

        response["status"] = status

        return response
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    @RequestMapping(value = ["/console/log"], method = [RequestMethod.POST], consumes = ["application/json"])
    @ResponseBody
    fun writeConsoleToLog(model: Model, @RequestBody requestBody: JsonNode): String {
        val consoleLogMapper = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (consoleLogMapper.containsKey("type") && consoleLogMapper.containsKey("log")) {
            // error: 0, info: 1, log: 2, warn: 3
            val type = consoleLogMapper["type"].toString().toInt()
            val log = consoleLogMapper["log"].toString()

            when (type) {
                0 -> {
                    logger.log(Level.SEVERE, log)
                }
                1 -> {
                    logger.log(Level.INFO, log)
                }
                2 -> {
                    logger.log(Level.INFO, log)
                }
                3 -> {
                    logger.log(Level.WARNING, log)
                }
                else -> {
                    logger.log(Level.INFO, log)
                }
            }

            return "{\"status\": \"success\",\"msg\":\"\"}"
        }

        return "{\"status\": \"fail\",\"msg\":\"\"}"
    }

    @Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
    @RequestMapping(value = ["/api/v1/endpoints"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Suppress("UNCHECKED_CAST")
    fun getApiEndpoints(model: Model, request: HttpServletRequest): String {

        val currentUserObj = model.getAttribute("currentUser") as User?
        var response = listOf(mutableMapOf<String, Any>())

        if (currentUserObj?.getAuthority() != null &&
                (currentUserObj.getAuthority()!! == "ROLE_SUPER" || currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_USER")) {

            val applicationContext =
                WebApplicationContextUtils.getRequiredWebApplicationContext(request.session.servletContext)

            val requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
            val map = requestMappingHandlerMapping.handlerMethods

            val apiMapList = mutableListOf<MutableMap<String, Any>>()

            // Based on WebSecurityConfig
            val superEndpoints = MultiSecurityConfig.superList
            superEndpoints.forEachIndexed { i, _ ->
                if (superEndpoints[i].contains("**")) {
                    superEndpoints[i] = superEndpoints[i].replace("**", "(.*)")
                }
            }
            val adminEndpoints = MultiSecurityConfig.adminList
            adminEndpoints.forEachIndexed { i, _ ->
                if (adminEndpoints[i].contains("**")) {
                    adminEndpoints[i] = adminEndpoints[i].replace("**", "(.*)")
                }
            }
            val allRoleEndpoints = MultiSecurityConfig.allRoleList
            allRoleEndpoints.forEachIndexed { i, _ ->
                if (allRoleEndpoints[i].contains("**")) {
                    allRoleEndpoints[i] = allRoleEndpoints[i].replace("**", "(.*)")
                }
            }
//        println(allRoleEndpoints.contentToString())

            var roleController = mutableMapOf<String, Any>()

            map.forEach { (key, value) ->
//            println(key.toString())
//            println(value.getMethodAnnotation(RouterOperation::class.java)?.operation?.description)
                if (key.toString().contains("/api/v1/", ignoreCase = true) && !key.toString()
                        .contains("/docs/", ignoreCase = true)
                ) {
                    roleController["requestType"] = ""
                    roleController["endpoints"] = arrayOf<String>()
                    roleController["produces"] = ""
                    roleController["summary"] = ""
                    roleController["authorizedRoles"] = arrayOf("public")

                    // Order is important! Highest to lowest roles
                    for (superEndpoint in superEndpoints) {
                        val matcher = superEndpoint.toRegex()
                        if (matcher.findAll(key.toString()).count() > 0 && currentUserObj.getAuthority()!! == "ROLE_SUPER") {
                            roleController["authorizedRoles"] = arrayOf("super")
                            break
                        }
                    }

                    for (adminEndpoint in adminEndpoints) {
                        val matcher = adminEndpoint.toRegex()
                        if (matcher.findAll(key.toString()).count() > 0 &&
                            (currentUserObj.getAuthority()!! == "ROLE_SUPER" || currentUserObj.getAuthority()!! == "ROLE_ADMIN")
                        ) {
                            roleController["authorizedRoles"] = arrayOf("admin", "super")
                            break
                        }
                    }

                    if ((roleController["authorizedRoles"]!! as Array<String>).isNotEmpty()) {
                        for (allRoleEndpoint in allRoleEndpoints) {
                            val matcher = allRoleEndpoint.toRegex()
                            if (matcher.findAll(key.toString()).count() > 0 &&
                                (currentUserObj.getAuthority()!! == "ROLE_SUPER" || currentUserObj.getAuthority()!! == "ROLE_ADMIN" || currentUserObj.getAuthority()!! == "ROLE_USER")
                            ) {
                                roleController["authorizedRoles"] = arrayOf("super", "admin", "user")
                                break
                            }
                        }
                    }

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
                                roleController["requestType"] = requestType

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
                                    roleController["endpoints"] = pathArray
                                }
                            }

                            // Consumes
                            if (endpointParts.contains("consumes")) {
                                val consumesStr = endpointParts.drop(11).dropLast(1)
                                var consumesArray = consumesStr.split("||")
                                consumesArray = consumesArray.map{it.trim()}

                                roleController["consumes"] = consumesArray
                            }

                            // Produces
                            if (endpointParts.contains("produces")) {
                                val producesStr = endpointParts.drop(11).dropLast(2)
                                var producesArray = producesStr.split("||")
                                producesArray = producesArray.map{it.trim()}

                                roleController["produces"] = producesArray
                            }
                        }

                        // Description
                        if (value.getMethodAnnotation(RouterOperation::class.java)?.operation?.summary != null) {
                            roleController["summary"] =
                                value.getMethodAnnotation(RouterOperation::class.java)?.operation?.summary.toString()
                        }
                    }

                    apiMapList.add(roleController)

                    roleController = mutableMapOf()
                }
            }

            response = apiMapList.sortedBy { it["order"].toString() }
        }

        return mapper.writeValueAsString(response)
    }

    private fun roundOffDecimal(number: Double): Any {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }
}