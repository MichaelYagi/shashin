package com.miyagi.shashin.repository

import com.miyagi.shashin.model.MediaDirectory
import com.miyagi.shashin.model.User
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface MediaDirectoryRepository : CrudRepository<MediaDirectory?, Int?> {
    fun findByExclude(exclude: Boolean): MutableIterable<MediaDirectory?>

    @Query("SELECT * FROM mediadir WHERE exclude = :exclude AND directory= :directory", nativeQuery = true)
    fun findByExcludeIsAndDirectory(exclude: Boolean, directory: String?): MediaDirectory?

    @Transactional
    fun deleteByExclude(exclude: Boolean): Long?
}