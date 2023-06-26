package com.miyagi.shashin.controller

import com.miyagi.shashin.configuration.MultiSecurityConfig
import com.miyagi.shashin.util.TextUtils
import org.springdoc.core.annotations.RouterOperation
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.http.HttpServletRequest

@Suppress("UNCHECKED_CAST")
@Controller
class ArticlesController {
    @RequestMapping(value = ["/articles","/articles/quickstart"], method = [RequestMethod.GET])
    fun getQuickstart(model: Model, request: HttpServletRequest): String {
        val module = "articles/quickstart"

        val moduleArray = module.split("/")
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(moduleArray[moduleArray.size-1])

        return module
    }

    @RequestMapping(value = ["articles/endpoints"], method = [RequestMethod.GET])
    fun getEndpoints(model: Model, request: HttpServletRequest): String {
        val module = "articles/endpoints"

        val applicationContext =
            WebApplicationContextUtils.getRequiredWebApplicationContext(request.session.servletContext)

        val requestMappingHandlerMapping = applicationContext
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
        val map = requestMappingHandlerMapping.handlerMethods
        model["apiEndpointsMap"] = mutableMapOf<String, String>()
        val apiMap = mutableMapOf<String, MutableMap<String, String>>()

        // Based on WebSecurityConfig
        val webSecurityConfig = MultiSecurityConfig.WebSecurityConfig()
        val adminEndpoints = webSecurityConfig.adminList
        adminEndpoints.forEachIndexed { i, _ ->
            if(adminEndpoints[i].contains("**")) {
                adminEndpoints[i] = adminEndpoints[i].replace("**", "(.*)")
            }
        }
        val allRoleEndpoints = webSecurityConfig.allRoleList
        allRoleEndpoints.forEachIndexed { i, _ ->
            if(allRoleEndpoints[i].contains("**")) {
                allRoleEndpoints[i] = allRoleEndpoints[i].replace("**", "(.*)")
            }
        }
//        println(allRoleEndpoints.contentToString())

        var roleController = mutableMapOf<String, String>()

        map.forEach { (key, value) ->
//            println(key.toString())
//            println(value.getMethodAnnotation(RouterOperation::class.java)?.operation?.description)
            if (key.toString().contains("/api/v1/", ignoreCase = true) && !key.toString().contains("/docs/", ignoreCase = true)) {
                roleController["requestType"] = ""
                roleController["apiCall"] = ""
                roleController["rolePath"] = ""
                roleController["produces"] = ""
                roleController["description"] = ""
                roleController["role"] = "Public"

                for (adminEndpoint in adminEndpoints) {
                    val matcher = adminEndpoint.toRegex()
                    if (matcher.findAll(key.toString()).count() > 0) {
                        roleController["role"] = "Admin"
                        break
                    }
                }

                if (roleController["role"]!!.isNotBlank()) {
                    for (allRoleEndpoint in allRoleEndpoints) {
                        val matcher = allRoleEndpoint.toRegex()
                        if (matcher.findAll(key.toString()).count() > 0) {
                            roleController["role"] = "Admin and User"
                            break
                        }
                    }
                }

                roleController["rolePath"] = TextUtils.generateUUID(key.toString(),"","",0.0,0,"").toString()
                roleController["controller"] = value.toString()
                apiMap[key.toString()] = roleController

                val endpointArray = key.toString().split(",")
                if (endpointArray.size > 0) {
                    if (endpointArray.size == 2) {
//                        println(endpointArray[1].drop(11).dropLast(2).replace(" || ",","))
                        roleController["produces"] = endpointArray[1].drop(11).dropLast(2).replace(" || ",", ")
                    }
//                println(endpointArray[0])
//                println("request type: "+endpointArray[0].substring(1,5).trim())
                    val regex = "\\/api\\/v1\\/.*\\]".toRegex()
//                println(regex)
                    val matchResult = regex.find(endpointArray[0])
//                println("matchResult: ${matchResult?.value?.dropLast(1)}")

                    val requestType = endpointArray[0].substring(1, 5).trim()
                    val apiCall = matchResult?.value?.dropLast(1)
                    roleController["requestType"] = requestType
                    roleController["apiCall"] = apiCall.toString().replace(" || ",", ")
                    if (value.getMethodAnnotation(RouterOperation::class.java)?.operation?.description != null) {
                        roleController["description"] =
                            value.getMethodAnnotation(RouterOperation::class.java)?.operation?.description.toString()
                    }
                }

                roleController = mutableMapOf()
            }
        }

        model["apiEndpointsMap"] = apiMap

        val moduleArray = module.split("/")
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(moduleArray[moduleArray.size-1])

        return module
    }
}