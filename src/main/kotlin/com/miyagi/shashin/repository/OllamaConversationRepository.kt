package com.miyagi.shashin.repository

import com.miyagi.shashin.model.OllamaConversation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface OllamaConversationRepository : JpaRepository<OllamaConversation, Int> {
    fun findAllByMetadataIdOrderByIdAsc(metadataId: String): List<OllamaConversation>

    @Transactional
    fun deleteAllByMetadataId(metadataId: String)
}
