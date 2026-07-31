package com.miyagi.shashin.repository

import com.miyagi.shashin.model.MemoryPhoto
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemoryPhotoRepository : CrudRepository<MemoryPhoto?, Int?> {
    @Query("SELECT * FROM memoryphoto WHERE memory_id = :memoryId ORDER BY display_order ASC", nativeQuery = true)
    fun findByMemoryIdOrderByDisplayOrder(@Param("memoryId") memoryId: Int): MutableList<MemoryPhoto>
}
