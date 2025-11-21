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

    @Query("WITH dup_hashes AS (\n" +
            "    SELECT duplicate_hash\n" +
            "    FROM metadata\n" +
            "    WHERE hidden = 0\n" +
            "    GROUP BY duplicate_hash\n" +
            "    HAVING COUNT(*) > 1\n" +
            ")\n" +
            "SELECT DISTINCT m.*\n" +
            "FROM metadata m\n" +
            "         JOIN duplicates d ON m.id = d.image_id_one\n" +
            "WHERE m.hidden = 0\n" +
            "  AND m.duplicate_hash IN (SELECT duplicate_hash FROM dup_hashes)\n" +
            "UNION\n" +
            "SELECT DISTINCT m.*\n" +
            "FROM metadata m\n" +
            "         JOIN duplicates d ON m.id = d.image_id_two\n" +
            "WHERE m.hidden = 0\n" +
            "  AND m.duplicate_hash IN (SELECT duplicate_hash FROM dup_hashes)\n" +
            "ORDER BY duplicate_hash \n" +
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