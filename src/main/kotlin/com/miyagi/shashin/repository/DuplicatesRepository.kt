package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Duplicates
import com.miyagi.shashin.model.Metadata
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Transactional
@Repository
interface DuplicatesRepository : CrudRepository<Duplicates?, Int?> {
    @Query("SELECT * FROM duplicates ORDER BY image_id_one", nativeQuery = true)
    fun findDuplicates(): MutableIterable<Duplicates>

    @Query("SELECT COUNT(*) FROM duplicates WHERE (image_id_one = :metadataId1 AND image_id_two = :metadataId2) OR (image_id_one = :metadataId2 AND image_id_two = :metadataId1)", nativeQuery = true)
    fun findDuplicateMetadataId(@Param("metadataId1") metadataId1: String, @Param("metadataId2") metadataId2: String): Int

    fun deleteByImageIdOneOrImageIdTwo(@Param("metadataId1") metadataId1: String, @Param("metadataId2") metadataId2: String): Long

    @Query("SELECT *\n" +
            "FROM metadata m\n" +
            "WHERE hidden = 0 AND duplicate_hash IS NOT NULL AND type LIKE \"%image%\" " +
            "LIMIT :offset, :limit", nativeQuery = true)
    fun findDuplicateImageHash(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableList<Metadata>?

    @Query("WITH counts AS (\n" +
            "  SELECT id, COUNT(*) AS c\n" +
            "  FROM (\n" +
            "    SELECT image_id_one AS id FROM duplicates\n" +
            "    UNION ALL\n" +
            "    SELECT image_id_two FROM duplicates\n" +
            "  )\n" +
            "  GROUP BY id\n" +
            "  HAVING COUNT(*) > 1\n" +
            "),\n" +
            "edges AS (\n" +
            "  SELECT image_id_one AS a, image_id_two AS b FROM duplicates\n" +
            "  UNION\n" +
            "  SELECT image_id_two AS a, image_id_one AS b FROM duplicates\n" +
            "),\n" +
            "seeds AS (\n" +
            "  SELECT id AS root, id AS node FROM counts\n" +
            "),\n" +
            "reach(root, node) AS (\n" +
            "  SELECT root, node FROM seeds\n" +
            "  UNION\n" +
            "  SELECT r.root, e.b\n" +
            "  FROM reach r\n" +
            "  JOIN edges e ON e.a = r.node\n" +
            "),\n" +
            "min_root AS (\n" +
            "  SELECT node, MIN(root) AS component_id\n" +
            "  FROM reach\n" +
            "  GROUP BY node\n" +
            ")\n" +
            "SELECT m.*, mr.component_id\n" +
            "FROM min_root mr\n" +
            "JOIN metadata m ON m.id = mr.node\n" +
            "WHERE m.hidden = 0\n" +
            "ORDER BY mr.component_id, m.id\n" +
            "LIMIT :offset, :limit;", nativeQuery = true)
    fun findAllMetadataIds(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableList<Metadata>?

    @Query("SELECT COUNT(DISTINCT m.id) AS counted\n" +
            "FROM duplicates d\n" +
            "         JOIN metadata m\n" +
            "              ON m.id IN (d.image_id_one, d.image_id_two)\n" +
            "WHERE m.hidden = 0"
        , nativeQuery = true)
    fun countAllMetadataIds(): Int?
}