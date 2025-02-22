package com.miyagi.shashin.repository

import com.miyagi.shashin.model.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface FolderDataRepository : CrudRepository<FolderData?, Int?> {
    @Query("SELECT COUNT(*) FROM folderdata WHERE folder = :folderName", nativeQuery = true)
    fun countByFolder(folderName: String): Int

    @Query("SELECT * FROM folderdata WHERE folder = :folderName", nativeQuery = true)
    fun findByFolder(folderName: String): FolderData
}