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

    private fun buildTokenClause(token: String): String = """
        (k.keyword LIKE lower('%' || ? || '%')
        OR m2.id LIKE lower('%' || ? || '%')
        OR m2.path LIKE lower('%' || ? || '%')
        OR m2.title LIKE lower('%' || ? || '%')
        OR m2.description LIKE lower('%' || ? || '%')
        OR m2.placeName LIKE lower('%' || ? || '%')
        OR rl.name LIKE lower('%' || ? || '%')
        OR m2.camera LIKE lower('%' || ? || '%')
        OR m2.lens LIKE lower('%' || ? || '%')
        OR m2.takenAt LIKE lower('%' || ? || '%')
        OR m2.type LIKE lower('%' || ? || '%')
        OR m2.originalImageHeight = ?
        OR m2.originalImageWidth = ?
        OR m2.originalImageWidth || 'x' || m2.originalImageHeight = ?)
    """.trimIndent()

    private val FIELDS_PER_TOKEN = 14 // number of ? placeholders per token

    private fun tokenParams(tokens: List<String>): List<Any> =
        tokens.flatMap { token -> List(FIELDS_PER_TOKEN) { token } }

    /**
     * Multi-token search for admin/super roles.
     * Single token behaves identically to the existing @Query.
     */
    fun findByTerm(
        jdbcTemplate: JdbcTemplate,
        term: String,
        offset: Int,
        limit: Int
    ): List<Metadata> {
        val tokens = term.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val whereClauses = tokens.joinToString(" AND ") { buildTokenClause(it) }
        val sql = """
            SELECT * FROM metadata m
            WHERE m.hidden = false
            AND m.id IN (
                SELECT DISTINCT m2.id FROM metadata m2
                LEFT JOIN recognitionlabelphoto rlp ON m2.id = rlp.metadata_id
                LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
                LEFT JOIN keywordphoto kp ON kp.metadata_id = m2.id
                LEFT JOIN keyword k ON kp.keyword_id = k.id
                WHERE $whereClauses
            )
            ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC
            LIMIT $limit OFFSET $offset
        """.trimIndent()
        return jdbcTemplate.query(sql, metadataRowMapper, *tokenParams(tokens).toTypedArray())
    }

    fun countByTerm(
        jdbcTemplate: JdbcTemplate,
        term: String
    ): Int {
        val tokens = term.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val whereClauses = tokens.joinToString(" AND ") { buildTokenClause(it) }
        val sql = """
            SELECT COUNT(*) FROM metadata m
            WHERE m.hidden = false
            AND m.id IN (
                SELECT DISTINCT m2.id FROM metadata m2
                LEFT JOIN recognitionlabelphoto rlp ON m2.id = rlp.metadata_id
                LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
                LEFT JOIN keywordphoto kp ON kp.metadata_id = m2.id
                LEFT JOIN keyword k ON kp.keyword_id = k.id
                WHERE $whereClauses
            )
        """.trimIndent()
        return jdbcTemplate.queryForObject(sql, Int::class.java, *tokenParams(tokens).toTypedArray()) ?: 0
    }

    /**
     * Multi-token search scoped to a specific user.
     */
    fun findByTermAndUserId(
        jdbcTemplate: JdbcTemplate,
        term: String,
        userId: Int,
        offset: Int,
        limit: Int
    ): List<Metadata> {
        val tokens = term.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val whereClauses = tokens.joinToString(" AND ") { buildTokenClause(it) }
        val sql = """
            SELECT * FROM metadata m
            WHERE m.hidden = false
            AND m.id IN (
                SELECT DISTINCT m2.id FROM metadata m2
                LEFT JOIN recognitionlabelphoto rlp ON m2.id = rlp.metadata_id
                LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
                LEFT JOIN albumphoto a ON m2.id = a.metadata_id
                LEFT JOIN useralbum ua ON ua.album_id = a.album_id
                LEFT JOIN keywordphoto kp ON kp.metadata_id = m2.id
                LEFT JOIN keyword k ON kp.keyword_id = k.id
                WHERE $whereClauses
                AND ua.user_id = ?
            )
            ORDER BY m.year DESC, m.month DESC, m.day DESC, m.time DESC
            LIMIT $limit OFFSET $offset
        """.trimIndent()
        val params = tokenParams(tokens) + userId
        return jdbcTemplate.query(sql, metadataRowMapper, *params.toTypedArray())
    }

    fun countByTermAndUserId(
        jdbcTemplate: JdbcTemplate,
        term: String,
        userId: Int
    ): Int {
        val tokens = term.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val whereClauses = tokens.joinToString(" AND ") { buildTokenClause(it) }
        val sql = """
            SELECT COUNT(*) FROM metadata m
            WHERE m.hidden = false
            AND m.id IN (
                SELECT DISTINCT m2.id FROM metadata m2
                LEFT JOIN recognitionlabelphoto rlp ON m2.id = rlp.metadata_id
                LEFT JOIN recognitionlabel rl ON rl.id = rlp.recognition_label_id
                LEFT JOIN albumphoto a ON m2.id = a.metadata_id
                LEFT JOIN useralbum ua ON ua.album_id = a.album_id
                LEFT JOIN keywordphoto kp ON kp.metadata_id = m2.id
                LEFT JOIN keyword k ON kp.keyword_id = k.id
                WHERE $whereClauses
                AND ua.user_id = ?
            )
        """.trimIndent()
        val params = tokenParams(tokens) + userId
        return jdbcTemplate.queryForObject(sql, Int::class.java, *params.toTypedArray()) ?: 0
    }
}