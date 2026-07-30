package com.miyagi.shashin.util

import com.miyagi.shashin.model.Metadata
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

object SearchQueryBuilder {

    private val metadataRowMapper = RowMapper { rs: ResultSet, _ ->
        Metadata().apply {
            setId(rs.getString("id"))
            setPath(rs.getString("path"))
            setTitle(rs.getString("title"))
            setDescription(rs.getString("description"))
            setFileName(rs.getString("file_name"))
            setExpectedExtension(rs.getString("expected_extension"))
            setTimeZone(rs.getString("time_zone"))
            setType(rs.getString("type"))
            setLat(rs.getString("lat"))
            setLng(rs.getString("lng"))
            setPlaceName(rs.getString("place_name"))
            setCamera(rs.getString("camera"))
            setLens(rs.getString("lens"))
            setQuality(rs.getString("quality"))
            setCompressionType(rs.getString("compression_type"))
            setIso(rs.getObject("iso") as Int?)
            setExposure(rs.getString("exposure"))
            setFstopNumber(rs.getObject("fstop_number") as Double?)
            setFocalLength(rs.getObject("focal_length") as Double?)
            setYear(rs.getObject("year") as Int?)
            setMonth(rs.getObject("month") as Int?)
            setDay(rs.getObject("day") as Int?)
            setTime(rs.getString("time"))
            setThumbnailPathExtraSmall(rs.getString("thumbnail_path_extra_small"))
            setThumbnailUrlExtraSmall(rs.getString("thumbnail_url_extra_small"))
            setThumbnailPathSmall(rs.getString("thumbnail_path_small"))
            setThumbnailUrlSmall(rs.getString("thumbnail_url_small"))
            setThumbnailSmallWidth(rs.getObject("thumbnail_small_width") as Int?)
            setThumbnailSmallHeight(rs.getObject("thumbnail_small_height") as Int?)
            setThumbnailPathCentered(rs.getString("thumbnail_path_centered"))
            setThumbnailUrlCentered(rs.getString("thumbnail_url_centered"))
            setThumbnailUrlOriginal(rs.getString("thumbnail_url_original"))
            setOriginalImageWidth(rs.getObject("original_image_width") as Int?)
            setOriginalImageHeight(rs.getObject("original_image_height") as Int?)
            setMapMarkerPath(rs.getString("map_marker_path"))
            setMapMarkerUrl(rs.getString("map_marker_url"))
            setFolder(rs.getString("folder"))
            setVideoUrl(rs.getString("video_url"))
            setDuration(rs.getString("duration"))
            setHidden((rs.getObject("hidden") as? Int)?.let { it != 0 })
            setAddedAt(rs.getString("added_at"))
            setTakenAt(rs.getString("taken_at"))
            setCreatedAt(rs.getString("created_at"))
            setModifiedAt(rs.getString("modified_at"))
            setLastAccessedAt(rs.getString("last_accessed_at"))
            setLastAccessedBy(rs.getObject("last_accessed_by") as Int?)
            setUploadedBy(rs.getObject("uploaded_by") as Int?)
            setFreeFormString(rs.getString("free_form_string"))
            setBrightness(rs.getString("brightness"))
            setContrast(rs.getString("contrast"))
            setSaturation(rs.getString("saturation"))
            setSharpness(rs.getString("sharpness"))
            setFlipHorizontally((rs.getObject("flip_horizontally") as? Int)?.let { it != 0 })
            setFlipVertically((rs.getObject("flip_vertically") as? Int)?.let { it != 0 })
            setRotation(rs.getObject("rotation") as Int?)
            setDuplicateHash(rs.getString("duplicate_hash"))
        }
    }

    private val MENTION_REGEX = Regex("""@"([^"]+)"""")

    // Extract @"Name" tokens and the remaining free text from a search term
    fun parseMentions(term: String): Pair<List<String>, String> {
        val people = MENTION_REGEX.findAll(term).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
        val remaining = MENTION_REGEX.replace(term, "").trim()
        return Pair(people, remaining)
    }

    // EXISTS subquery ensuring a specific person is tagged in the photo (one ? param per call)
    private fun buildPersonExistsClause(): String =
        "EXISTS (SELECT 1 FROM recognitionlabelphoto rlp_p JOIN recognitionlabel rl_p ON rl_p.id = rlp_p.recognition_label_id WHERE rlp_p.metadata_id = m.id AND lower(rl_p.name) = lower(?))"

    // WHERE filter: token must match somewhere (permissive substring match)
    private fun buildTokenClause(token: String): String = """
        (k.keyword LIKE lower('%' || ? || '%')
        OR m.id LIKE lower('%' || ? || '%')
        OR m.path LIKE lower('%' || ? || '%')
        OR m.title LIKE lower('%' || ? || '%')
        OR m.description LIKE lower('%' || ? || '%')
        OR m.place_name LIKE lower('%' || ? || '%')
        OR rl.name LIKE lower('%' || ? || '%')
        OR m.camera LIKE lower('%' || ? || '%')
        OR m.lens LIKE lower('%' || ? || '%')
        OR m.taken_at LIKE lower('%' || ? || '%')
        OR m.type LIKE lower('%' || ? || '%')
        OR m.original_image_height = ?
        OR m.original_image_width = ?
        OR m.original_image_width || 'x' || m.original_image_height = ?)
    """.trimIndent()

    private val FIELDS_PER_TOKEN = 14

    private fun tokenParams(tokens: List<String>): List<Any> =
        tokens.flatMap { token -> List(FIELDS_PER_TOKEN) { token } }

    /**
     * Relevance score for a single token using the already-joined k and rl tables.
     * MAX() collapses the multiple rows produced by the join back to one per image.
     *
     * Score 2 = word-boundary or exact match on keyword or face-recognition label
     * Score 1 = word-boundary or exact match on description or place name
     * Score 0 = substring-only match
     *
     * Token is interpolated (not a ? param) because it appears in ORDER BY.
     * Single quotes are escaped to prevent injection.
     */
    private fun buildRelevanceClause(token: String): String {
        val t = token.trim().lowercase().replace("'", "''")
        return """
            MAX(CASE
                WHEN lower(k.keyword) = '$t'
                    OR lower(k.keyword) LIKE '$t %'
                    OR lower(k.keyword) LIKE '% $t'
                    OR lower(k.keyword) LIKE '% $t %'
                    OR lower(rl.name) = '$t'
                    OR lower(rl.name) LIKE '$t %'
                    OR lower(rl.name) LIKE '% $t'
                    OR lower(rl.name) LIKE '% $t %'
                    THEN 2
                WHEN lower(m.description) = '$t'
                    OR lower(m.description) LIKE '$t %'
                    OR lower(m.description) LIKE '% $t'
                    OR lower(m.description) LIKE '% $t %'
                    OR lower(m.place_name) = '$t'
                    OR lower(m.place_name) LIKE '$t %'
                    OR lower(m.place_name) LIKE '% $t'
                    OR lower(m.place_name) LIKE '% $t %'
                    THEN 1
                ELSE 0
            END)
        """.trimIndent()
    }

    /**
     * Multi-token search for admin/super roles.
     * Supports @"Name" mentions for per-person EXISTS filtering.
     */
    fun findByTerm(
        jdbcTemplate: JdbcTemplate,
        term: String,
        offset: Int,
        limit: Int
    ): List<Metadata> {
        val (people, remaining) = parseMentions(term)
        val tokens = remaining.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val hasTokens = tokens.isNotEmpty()
        if (!hasTokens && people.isEmpty()) return emptyList()

        val tokenWhere = if (hasTokens) "AND (${tokens.joinToString(" AND ") { buildTokenClause(it) }})" else ""
        val peopleWhere = people.joinToString("\n") { "AND ${buildPersonExistsClause()}" }
        val relevanceScore = if (hasTokens) tokens.joinToString(" + ") { buildRelevanceClause(it) } else "0"
        val sql = """
            SELECT m.*
            FROM metadata m
            LEFT JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id
            LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
            LEFT JOIN keywordphoto kp ON kp.metadata_id = m.id
            LEFT JOIN keyword k ON kp.keyword_id = k.id
            WHERE m.hidden = false
            $tokenWhere
            $peopleWhere
            GROUP BY m.id
            ORDER BY ($relevanceScore) DESC, m.year DESC, m.month DESC, m.day DESC, m.time DESC, m.id DESC
            LIMIT $limit OFFSET $offset
        """.trimIndent()
        val params = (if (hasTokens) tokenParams(tokens) else emptyList()) + people
        return jdbcTemplate.query(sql, metadataRowMapper, *params.toTypedArray())
    }

    fun countByTerm(
        jdbcTemplate: JdbcTemplate,
        term: String
    ): Int {
        val (people, remaining) = parseMentions(term)
        val tokens = remaining.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val hasTokens = tokens.isNotEmpty()
        if (!hasTokens && people.isEmpty()) return 0

        val tokenWhere = if (hasTokens) "AND (${tokens.joinToString(" AND ") { buildTokenClause(it) }})" else ""
        val peopleWhere = people.joinToString("\n") { "AND ${buildPersonExistsClause()}" }
        val sql = """
            SELECT COUNT(DISTINCT m.id)
            FROM metadata m
            LEFT JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id
            LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
            LEFT JOIN keywordphoto kp ON kp.metadata_id = m.id
            LEFT JOIN keyword k ON kp.keyword_id = k.id
            WHERE m.hidden = false
            $tokenWhere
            $peopleWhere
        """.trimIndent()
        val params = (if (hasTokens) tokenParams(tokens) else emptyList()) + people
        return jdbcTemplate.queryForObject(sql, Int::class.java, *params.toTypedArray()) ?: 0
    }

    /**
     * Multi-token search scoped to a specific user.
     * Supports @"Name" mentions for per-person EXISTS filtering.
     */
    fun findByTermAndUserId(
        jdbcTemplate: JdbcTemplate,
        term: String,
        userId: Int,
        offset: Int,
        limit: Int
    ): List<Metadata> {
        val (people, remaining) = parseMentions(term)
        val tokens = remaining.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val hasTokens = tokens.isNotEmpty()
        if (!hasTokens && people.isEmpty()) return emptyList()

        val tokenWhere = if (hasTokens) "AND (${tokens.joinToString(" AND ") { buildTokenClause(it) }})" else ""
        val peopleWhere = people.joinToString("\n") { "AND ${buildPersonExistsClause()}" }
        val relevanceScore = if (hasTokens) tokens.joinToString(" + ") { buildRelevanceClause(it) } else "0"
        val sql = """
            SELECT m.*
            FROM metadata m
            LEFT JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id
            LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
            LEFT JOIN keywordphoto kp ON kp.metadata_id = m.id
            LEFT JOIN keyword k ON kp.keyword_id = k.id
            LEFT JOIN albumphoto a ON m.id = a.metadata_id
            LEFT JOIN useralbum ua ON ua.album_id = a.album_id
            WHERE m.hidden = false
            $tokenWhere
            $peopleWhere
            AND ua.user_id = ?
            GROUP BY m.id
            ORDER BY ($relevanceScore) DESC, m.year DESC, m.month DESC, m.day DESC, m.time DESC, m.id DESC
            LIMIT $limit OFFSET $offset
        """.trimIndent()
        val params = (if (hasTokens) tokenParams(tokens) else emptyList()) + people + userId
        return jdbcTemplate.query(sql, metadataRowMapper, *params.toTypedArray())
    }

    fun countByTermAndUserId(
        jdbcTemplate: JdbcTemplate,
        term: String,
        userId: Int
    ): Int {
        val (people, remaining) = parseMentions(term)
        val tokens = remaining.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val hasTokens = tokens.isNotEmpty()
        if (!hasTokens && people.isEmpty()) return 0

        val tokenWhere = if (hasTokens) "AND (${tokens.joinToString(" AND ") { buildTokenClause(it) }})" else ""
        val peopleWhere = people.joinToString("\n") { "AND ${buildPersonExistsClause()}" }
        val sql = """
            SELECT COUNT(DISTINCT m.id)
            FROM metadata m
            LEFT JOIN recognitionlabelphoto rlp ON m.id = rlp.metadata_id
            LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
            LEFT JOIN keywordphoto kp ON kp.metadata_id = m.id
            LEFT JOIN keyword k ON kp.keyword_id = k.id
            LEFT JOIN albumphoto a ON m.id = a.metadata_id
            LEFT JOIN useralbum ua ON ua.album_id = a.album_id
            WHERE m.hidden = false
            $tokenWhere
            $peopleWhere
            AND ua.user_id = ?
        """.trimIndent()
        val params = (if (hasTokens) tokenParams(tokens) else emptyList()) + people + userId
        return jdbcTemplate.queryForObject(sql, Int::class.java, *params.toTypedArray()) ?: 0
    }
}