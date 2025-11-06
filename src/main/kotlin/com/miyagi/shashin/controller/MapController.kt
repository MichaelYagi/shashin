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
import com.miyagi.shashin.repository.RecognitionLabelRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.TextUtils
import io.swagger.v3.oas.annotations.Operation
import org.springdoc.core.annotations.RouterOperation
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
class MapController(
    private val metadataRepository: MetadataRepository,
    private val albumRepository: AlbumRepository,
    private var keywordRepository: KeywordRepository,
    recognitionLabelRepository: RecognitionLabelRepository,
    @Value("\${app.endpoint.url.geocode}")
    private var geocodeUrl: String
): BaseController(
    recognitionLabelRepository = recognitionLabelRepository,
    albumRepository = albumRepository,
    keywordRepository = keywordRepository,
    metadataRepository = metadataRepository
) {
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
            if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute(
                    "superRole"
                )
            ) {
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
            summary = "Get results used for map data.",
            description = "<strong>Get results used for map data.</strong>" +
                    "<pre><code>" +
                    "curl -X GET \"http://127.0.0.1:6624/api/v1/mapdata\" \\\n" +
                    "-H \"x-api-key: &lt;service_api_key&gt;\"" +
                    "</code></pre>" +
                    "<table class=\"table table-bordered\"><thead>" +
                    "<tr><th>Element</th><th>Description</th><th>Type</th><th>Required</th><th>Notes</th></tr>" +
                    "</thead><tbody>" +
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
    @RequestMapping(value = ["/api/v1/mapdata", "/mapdata"], method = [RequestMethod.GET], produces = ["application/json"])
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

    @RouterOperation(
        operation =
        Operation(
            operationId = "getAllMapDataWithKeywords",
            summary = "Get results used for map data with keywords."
        )
    )
    @Suppress("UNCHECKED_CAST")
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/mapdata/keywords", "mapdata/keywords"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    fun getAllMapDataWithKeywords(model: Model): ResponseEntity<String> {
        val response = mutableMapOf<String, Any?>()
        response["mapdata"] = mutableListOf<MapData>()
        response["keywordMap"] = mutableMapOf<String, String>()
        response["msg"] = ""
        response["status"] = ApiResponse.FAIL.status

        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null) {

            val mapdata: MutableIterable<MapData> = if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute("superRole")) {
                metadataRepository!!.findTimelineAllForMap()
            } else {
                metadataRepository!!.findByAlbumMetadataByUserIdForMap(currentUserObj.getId())
            }

            response["mapdata"] = mapdata

            var keywordMap = mutableMapOf<String, String>()
            if (mapdata.count() > 0) {
                val metadataIds = mapdata.map { it.getId()!! }.toMutableList()
                val keywordList = keywordRepository.findAllKeywordsGroupedByMetadataIds(metadataIds)
                if (keywordList.count() > 0) {
                    keywordMap =
                        keywordList.associate { it.getMetadataId() to it.getKeywords() } as MutableMap<String, String>
                }

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

    @RouterOperation(
        operation =
        Operation(
            operationId = "getMapDataWithKeywords",
            summary = "Get paged results used for map data with keywords."
        )
    )
    @Suppress("UNCHECKED_CAST")
    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/api/v1/mapdata/keywords"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    fun getMapDataWithKeywords(model: Model, @RequestBody requestBody: JsonNode): ResponseEntity<String> {
        val mapKeywordsMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val response = mutableMapOf<String, Any?>()

        if (mapKeywordsMap.containsKey("page") && mapKeywordsMap.containsKey("size") && mapKeywordsMap.containsKey("startDate") && mapKeywordsMap.containsKey("endDate")) {
            val page = mapKeywordsMap["page"].toString().toInt()
            val size = mapKeywordsMap["size"].toString().toInt()
            val startDate = mapKeywordsMap["startDate"].toString()
            val endDate = mapKeywordsMap["endDate"].toString()

            val currentUserObj = model.getAttribute("currentUser") as User?
            response["mapdata"] = mutableListOf<MapData>()
            response["keywordMap"] = mutableMapOf<String, String>()
            response["msg"] = "Not logged in"
            response["status"] = ApiResponse.SUCCESS.status

            if (currentUserObj != null) {
                val mapdata: MutableList<MapData>

                if (startDate != "" && endDate != "") {
                    mapdata = if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute(
                            "superRole"
                        )
                    ) {
                        metadataRepository!!.findTimelineDatesForMap((page * size), size, startDate, endDate) as MutableList<MapData>
                    } else {
                        metadataRepository!!.findByAlbumMetadataByUserIdDatesForMapWithLimit(
                            currentUserObj.getId(),
                            (page * size), size,
                            startDate,
                            endDate
                        ) as MutableList<MapData>
                    }
                } else {
                    mapdata = if (currentUserObj.getAuthority() == model.getAttribute("adminRole") || currentUserObj.getAuthority() == model.getAttribute(
                            "superRole"
                        )
                    ) {
                        metadataRepository!!.findTimelineForMap((page * size), size) as MutableList<MapData>
                    } else {
                        metadataRepository!!.findByAlbumMetadataByUserIdForMapWithLimit(
                            currentUserObj.getId(),
                            (page * size), size
                        ) as MutableList<MapData>
                    }
                }

                response["mapdata"] = mapdata

                var keywordMap = mutableMapOf<String, String>()
                if (mapdata.count() > 0) {
                    val metadataIds = mapdata.map { it.getId()!! }.toMutableList()
                    val keywordList = keywordRepository.findAllKeywordsGroupedByMetadataIds(metadataIds)
                    if (keywordList.count() > 0) {
                        keywordMap =
                            keywordList.associate { it.getMetadataId() to it.getKeywords() } as MutableMap<String, String>
                    }

                }
                response["keywordMap"] = keywordMap

                response["msg"] = ""
                response["status"] = ApiResponse.SUCCESS.status
            }
        }

        val json = mapper.writeValueAsString(response)
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
            .body(json)
    }

    @Secured("ROLE_SUPER", "ROLE_ADMIN", "ROLE_USER")
    @RequestMapping(value = ["/album/mapdata/{id}"], method = [RequestMethod.GET], produces = ["application/json"])
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
        response["placename"] = ""

        if (currentUserObj != null && coordinateMap.containsKey("lat") && coordinateMap.containsKey("lng") && currentUserObj.getShowPlacename()!!) {
            val lat = coordinateMap["lat"].toString()
            val lng = coordinateMap["lng"].toString()
            val geoDataJson = TextUtils.getGeoData(geocodeUrl, lat, lng)

            if (geoDataJson != null && geoDataJson != "") {
                val buildPlace = TextUtils.getPlaceNameFromJson(geoDataJson)
                if (buildPlace.isNotBlank()) {
                    val buildPlaceArr = buildPlace.split(";")
                    val buildPlaceStr = buildPlaceArr[0].trim()
                    response["placename"] = buildPlaceStr
                }
                response["placedata"] = geoDataJson
                response["msg"] = "Success"
                response["status"] = ApiResponse.SUCCESS.status
            }
        }

        return mapper.writeValueAsString(response)
    }
}