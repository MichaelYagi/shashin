package com.miyagi.shashin.model

import jakarta.persistence.*

@Entity
@Table(name = "ollama_conversation")
class OllamaConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    private var metadataId: String? = null
    private var role: String? = null
    @Column(columnDefinition = "TEXT")
    private var content: String? = null
    private var createdAt: String? = null

    fun getId(): Int = id
    fun getMetadataId(): String? = metadataId
    fun setMetadataId(v: String?) { metadataId = v }
    fun getRole(): String? = role
    fun setRole(v: String?) { role = v }
    fun getContent(): String? = content
    fun setContent(v: String?) { content = v }
    fun getCreatedAt(): String? = createdAt
    fun setCreatedAt(v: String?) { createdAt = v }
}
