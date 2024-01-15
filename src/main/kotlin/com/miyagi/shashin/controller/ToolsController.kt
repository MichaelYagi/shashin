package com.miyagi.shashin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import com.miyagi.shashin.util.NetworkUtils
import com.sun.management.OperatingSystemMXBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.lang.management.ManagementFactory
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class ToolsController {

    @Autowired
    private lateinit var metaRepository: MetadataRepository

    @Autowired
    private var buildProperties: BuildProperties? = null

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    @Autowired
    private lateinit var persistentLoginsRepository: PersistentLoginsRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, Any?>()

//    @RequestMapping(value = ["tools/minifyassets"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
//    @ResponseBody
//    fun getMinifyAssets(model: Model): String {
//        val response = mutableMapOf<String, Any?>()
//
//        var input = "static/js/site/app.js"
//        var ouput = "static/js/site/app.min.js"
//        var resource = ClassPathResource("static/js/site/app.js")
//        var fis = FileInputStream(resource.file)
//        var fileContents = getFileContent(fis,"UTF-8")
//        var compiled = compile(fileContents)
//
//        println(compiled)
//
//        response["msg"] = ApiResponse.SUCCESS.status
//        response["message"] = "Success"
//
//        return mapper.writeValueAsString(response)
//    }

//    private fun compile(code: String?): String? {
//        val compiler = Compiler()
//        val options = CompilerOptions()
//        // Advanced mode is used here, but additional options could be set, too.
//        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(
//            options
//        )
//
//        // To get the complete set of externs, the logic in
//        // CompilerRunner.getDefaultExterns() should be used here.
//        val extern = SourceFile.fromCode(
//            "externs.js",
//            "function alert(x) {}"
//        )
//
//        // The dummy input name "input.js" is used here so that any warnings or
//        // errors will cite line numbers in terms of input.js.
//        val input = SourceFile.fromCode("input.js", code)
//
//        // compile() returns a Result, but it is not needed here.
//        compiler.compile(extern, input, options)
//
//        // The compiler is responsible for generating the compiled code; it is not
//        // accessible via the Result.
//        return compiler.toSource()
//    }
//
//    @Throws(IOException::class)
//    private fun getFileContent(
//        fis: FileInputStream,
//        encoding: String
//    ): String {
//        BufferedReader(InputStreamReader(fis, encoding)).use { br ->
//            val sb = StringBuilder()
//            var line: String?
//            while (br.readLine().also { line = it } != null) {
//                sb.append(line)
//                sb.append('\n')
//            }
//            return sb.toString()
//        }
//    }

    @Secured("ROLE_ADMIN")
    @RequestMapping(value = ["/tools/tokens"], method = [RequestMethod.GET])
    fun getPersistentTokens(model: Model, request: HttpServletRequest, response: HttpServletResponse): String? {
        var currentRememberMeToken = ""
        for (cookie in request.cookies!!) {
            if (cookie.name.contains("remember-me")) {
                currentRememberMeToken = cookie.value

                break
            }
        }

//        val persistentLoginsDetails = persistentLoginsRepository.findAllPersistentLoginsDetails()
//        model["persistentLoginsDetails"] = persistentLoginsDetails as Any
        model["currentTimeMS"] = System.currentTimeMillis()
        model["currentRememberMeToken"] = currentRememberMeToken

        return "tokens"
    }

    @GetMapping("/health")
    fun getHealth(model: Model): String {

        var status = "OK"

        model["dbCount"] = 0
        val timingOne = Date()
        val allMetadata = metaRepository.findAll()
        val timingTwo = Date()
        if (allMetadata.count() >= 0) {
            model["dbConnect"] = "OK"
            model["dbCount"] = allMetadata.count()
        } else {
            model["dbConnect"] = "FAIL"
            status = "FAIL"
        }

        val diff: Long = timingTwo.time - timingOne.time

        if (diff >= 0) {
            model["dbTiming"] = SimpleDateFormat("mm:ss:SSS").format(Date(diff))
        } else {
            model["dbTiming"] = "FAIL"
            status = "FAIL"
        }

        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        model["initialMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.init.toDouble() / 1073741824)
        model["usedHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.used.toDouble() / 1073741824)
        model["maxHeapMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.max.toDouble() / 1073741824)
        model["committedMemoryGB"] = roundOffDecimal(memoryMXBean.heapMemoryUsage.committed.toDouble() / 1073741824)
//        println("Used Heap Memory GB:"+metricsMap["usedHeapMemoryGB"])
//        println("Max Heap Memory GB:"+metricsMap["maxHeapMemoryGB"])

        val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean

        val cores = Runtime.getRuntime().availableProcessors()
        if (cores < 1) {
            status = "FAIL"
        }
        model["availableCores"] = cores

//        println("Process CPU load:"+(osMXBean.processCpuLoad * 100).toInt())
//        println("System CPU load:"+(osMXBean.cpuLoad * 100).toInt())
        model["processCpuLoadPercentDouble"] = (osMXBean.processCpuLoad * 100).toInt()
        @Suppress("DEPRECATION")
        model["systemCpuLoadPercentDouble"] = (osMXBean.systemCpuLoad * 100).toInt()
        model["os"] = System.getProperty("os.name") + " v" + System.getProperty("os.version") + " " + System.getProperty("os.arch")
        val reachable: Boolean = NetworkUtils.checkNominatimConnection(geocodeUrl+"status.php?format=json")
        if (reachable) {
            model["geocoderServicesAvailable"] = "OK"
        } else {
            model["geocoderServicesAvailable"] = "FAIL"
            status = "FAIL"
        }

        // If enabled - status fail if not available
        val settings = model.getAttribute("settings") as Settings?
        if (settings != null && settings.getCompreFaceKey() != "" && settings.getCompreFaceServer() != "") {
            val faceRecogServicesAvailable = NetworkUtils.checkCompreFaceConnection(
                settings.getCompreFaceServer(),
                settings.getCompreFaceKey()
            )
            if (faceRecogServicesAvailable) {
                model["faceRecogAvailable"] = "OK"
            } else {
                model["faceRecogAvailable"] = "FAIL"
                status = "FAIL"
            }
        } else {
            model["faceRecogAvailable"] = "N/A"
        }

        model["buildVersion"] = if (buildProperties != null) buildProperties?.version.toString() else "Missing"

        model["status"] = status

        return "health"
    }

    private fun roundOffDecimal(number: Double): Any {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }
}