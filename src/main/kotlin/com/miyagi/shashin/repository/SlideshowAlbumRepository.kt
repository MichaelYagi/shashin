package com.miyagi.shashin.repository

import com.miyagi.shashin.model.SlideshowAlbum
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import jakarta.transaction.Transactional
import org.springframework.data.repository.query.Param

@Transactional
@Repository
interface SlideshowAlbumRepository : CrudRepository<SlideshowAlbum?, Int?> {
    fun countAllByUserId(userId: Int): Int?
    fun findFirstByUserId(@Param("userId") userId: Int): SlideshowAlbum?
    fun deleteByUserId(userId: Int?): Long
}