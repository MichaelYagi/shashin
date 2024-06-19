package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.MapData
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.AlbumRepository
import com.miyagi.shashin.repository.KeywordRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils
import io.swagger.v3.oas.annotations.Operation
import org.springdoc.core.annotations.RouterOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

@Controller
class MapController: BaseController() {

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Autowired
    private val albumRepository: AlbumRepository? = null

    @Autowired
    private lateinit var keywordRepository: KeywordRepository

    @Value("\${app.endpoint.url.geocode}")
    private lateinit var geocodeUrl: String

    val mapper = ObjectMapper()

    @GetMapping("/map")
    fun getMap(model: Model): String {
        val module = "map"
        model["message"] = ""
        model["showControls"] = false
        model["albums"] = mutableListOf<Album>()

        val currentUserObj = model.getAttribute("currentUser") as User?

        var albums: MutableIterable<Album>? = null

        // If ROLE_ADMIN get lat lng for timeline
        if (currentUserObj != null) {
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                model["showControls"] = true
                albums = albumRepository?.findAllWithLocationOrderByAlbumName()
            } else {
                albums = albumRepository?.findAllWithLocationOrderByAlbumNameAndUserId(currentUserObj.getId())
            }
        }

        getAllAttributeData(model)

        if (albums != null) {
            model["albums"] = albums
        }
        model["msg"] = ""
        model["status"] = ApiResponse.SUCCESS.status
        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RouterOperation(
        operation =
        Operation(
            operationId = "getMapData",
            description = "<strong>Get results used for map data.</strong>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/mapdata\" \\\n" +
                    "-H \"Content-Type: application/json\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>Content-Type</td><td>header</td><td>string</td><td>required</td><td>application/json</td></tr>" +
                    "<tr><td>x-api-key</td><td>header</td><td>string</td><td>required</td><td>API key of the Shashin service</td></tr>" +
                    "</tbody></table><br>" +
                    "Response body on success:<br>" +
                    "<code><pre>{\n" +
                    "    \"msg\": \"\",\n" +
                    "    \"status\": \"success\",\n" +
                    "    \"mapdata\": [\n" +
                    "        {\n" +
                    "           &lt;metadata&gt;\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Type</th><th>Description</th></tr>" +
                    "</thead><tbody>" +
                    "<tr><td>mapdata[].metadata</td><td>object</td><td>A <a href=\"#\" data-bs-toggle=\"modal\" data-bs-target=\"#propMetadataDocs\">Metadata</a> object</td></tr>" +
                    "</tbody></table>"
        )
    )
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/mapdata", "/mapdata"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getMapData(model: Model): ResponseEntity<String> {
        val response = mutableMapOf<String, Any?>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        response["mapdata"] = mutableListOf<MapData>()
        response["msg"] = "Not logged in"
        response["status"] = ApiResponse.SUCCESS.status

        // If ROLE_ADMIN get lat lng for timeline
        if (currentUserObj != null) {
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                response["mapdata"] = metadataRepository!!.findTimelineAllForMap()
            } else {
                response["mapdata"] = metadataRepository!!.findByAlbumMetadataByUserIdForMap(currentUserObj.getId())
            }

            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status
        }

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/mapdata/keywords/{offset}/{limit}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getMapDataWithKeywords(model: Model, @PathVariable offset: Int, @PathVariable limit: Int): ResponseEntity<String> {
        val response = mutableMapOf<String, Any?>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        response["mapdata"] = mutableListOf<MapData>()
        response["keywordMap"] = mutableMapOf<String, Any?>()
        response["msg"] = "Not logged in"
        response["status"] = ApiResponse.SUCCESS.status

        if (currentUserObj != null) {
            val mapdata = if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                metadataRepository!!.findTimelineForMap(offset, limit) as MutableList<MapData>
            } else {
                metadataRepository!!.findByAlbumMetadataByUserIdForMapWithLimit(currentUserObj.getId(), offset, limit) as MutableList<MapData>
            }

            response["mapdata"] = mapdata

            val keywordMap = mutableMapOf<String, Any?>()
            for (data in mapdata) {
                val metadataId = data.getId()

                val keywordArray = mutableListOf<String>()
                val keywords = keywordRepository.findKeywordsByMetadataId(metadataId!!)
                for (keyword in keywords) {
                    keywordArray.add(keyword.getKeyword()!!)
                }
                keywordMap[metadataId] = keywordArray
            }
            response["keywordMap"] = keywordMap

            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status
        }

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/mapdata/{id}"], method = [RequestMethod.GET], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getAlbumMapData(model: Model, @PathVariable(required = true) id: Int): ResponseEntity<String> {
        val response = mutableMapOf<String, Any?>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        response["albummapdata"] = mutableListOf<String>()
        response["msg"] = "Not logged in"
        response["status"] = ApiResponse.SUCCESS.status

        // If ROLE_ADMIN get lat lng for timeline
        if (currentUserObj != null) {
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                response["albummapdata"] = albumRepository?.findMetadataIdsByAlbumId(id)
            } else {
                response["albummapdata"] = albumRepository?.findMetadataIdsByAlbumIdAndUserId(id, currentUserObj.getId())
            }

            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status
        }

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/placedata", "/placedata"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getPlaceData(model: Model, @RequestBody requestBody: JsonNode): String {
        val response = mutableMapOf<String, Any?>()
        val currentUserObj = model.getAttribute("currentUser") as User?
        val coordinateMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, String>>() {})
        response["msg"] = ""
        response["status"] = ApiResponse.FAIL.status
        response["placedata"] = "{}"

        if (currentUserObj != null && coordinateMap.containsKey("lat") && coordinateMap.containsKey("lng") && currentUserObj.getShowPlacename()!!) {
            val lat = coordinateMap["lat"].toString()
            val lng = coordinateMap["lng"].toString()
            val geoDataJson = TextUtils.getGeoData(geocodeUrl, lat, lng)

            if (geoDataJson != null && geoDataJson != "") {
                response["placedata"] = geoDataJson
                response["msg"] = "Success"
                response["status"] = ApiResponse.SUCCESS.status
            }
        }

        return mapper.writeValueAsString(response)
    }
}