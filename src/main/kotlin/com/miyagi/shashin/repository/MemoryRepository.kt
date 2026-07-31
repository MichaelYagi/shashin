package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Memory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MemoryRepository : CrudRepository<Memory?, Int?> {
    @Query("SELECT * FROM memory ORDER BY id ASC", nativeQuery = true)
    fun findAllOrderById(): MutableList<Memory>
}
