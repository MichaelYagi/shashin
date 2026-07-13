package com.miyagi.shashin.repository

import com.miyagi.shashin.model.OllamaContext
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface OllamaContextRepository : JpaRepository<OllamaContext, Int> {
    fun findByMetadataId(metadataId: String): OllamaContext?

    @Transactional
    fun deleteByMetadataId(metadataId: String)
}
