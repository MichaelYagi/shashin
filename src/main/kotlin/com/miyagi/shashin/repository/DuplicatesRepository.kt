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
            ") LIMIT :offset, :limit", nativeQuery = true)
    fun findDuplicateImageHash(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableList<Metadata>?

    @Query("SELECT DISTINCT sub.*\n" +
            "FROM (\n" +
            "         SELECT m.*, m.duplicate_hash\n" +
            "         FROM duplicates d\n" +
            "                  JOIN metadata m ON m.id = d.image_id1\n" +
            "         WHERE m.hidden = 0\n" +
            "         UNION ALL\n" +
            "         SELECT m.*, m.duplicate_hash\n" +
            "         FROM duplicates d\n" +
            "                  JOIN metadata m ON m.id = d.image_id2\n" +
            "         WHERE m.hidden = 0\n" +
            "     ) AS sub\n" +
            "WHERE sub.duplicate_hash IN (\n" +
            "    SELECT duplicate_hash\n" +
            "    FROM metadata\n" +
            "    WHERE hidden = 0\n" +
            "    GROUP BY duplicate_hash\n" +
            "    HAVING COUNT(*) > 1\n" +
            ")\n" +
            "ORDER BY sub.duplicate_hash " +
            "LIMIT :offset, :limit", nativeQuery = true)
    fun findAllMetadataIds(@Param("offset") offset: Int, @Param("limit") limit: Int): MutableList<Metadata>?
}