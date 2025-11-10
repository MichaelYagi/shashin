package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComments
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
    @Query("SELECT * FROM duplicates ORDER BY image_id1", nativeQuery = true)
    fun findDuplicates(): MutableIterable<Duplicates>

    @Query("SELECT COUNT(*) FROM duplicates WHERE (image_id1 = :metadataId1 AND image_id2 = :metadataId2) OR (image_id1 = :metadataId2 AND image_id2 = :metadataId1)", nativeQuery = true)
    fun findDuplicateMetadataId(@Param("metadataId1") metadataId1: String, @Param("metadataId2") metadataId2: String): Int

    @Query("SELECT *\n" +
            "FROM metadata m\n" +
            "WHERE hidden = 0 AND duplicate_hash IS NOT NULL AND type LIKE \"%image%\" " +
//            "AND type NOT LIKE \"%gif%\" " +
            "AND NOT EXISTS (\n" +
            "    SELECT 1\n" +
            "    FROM duplicates d\n" +
            "    WHERE d.image_id1 = m.id OR d.image_id2 = m.id\n" +
            ")", nativeQuery = true)
    fun findDuplicateImageHash(): MutableList<Metadata>?

    @Query("SELECT DISTINCT sub.*\n" +
            "FROM (\n" +
            "         SELECT m.*\n" +
            "         FROM duplicates d\n" +
            "                  JOIN metadata m ON m.id = d.image_id1\n" +
            "         UNION ALL\n" +
            "         SELECT m.*\n" +
            "         FROM duplicates d\n" +
            "                  JOIN metadata m ON m.id = d.image_id2\n" +
            "     ) AS sub\n" +
            "ORDER BY sub.duplicate_hash", nativeQuery = true)
    fun findAllMetadataIds(): MutableList<Metadata>?
}