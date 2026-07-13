package com.miyagi.shashin.model

import jakarta.persistence.*

@Entity
@Table(name = "ollama_context")
class OllamaContext {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    private var metadataId: String? = null
    private var model: String? = null
    @Column(columnDefinition = "TEXT")
    private var context: String? = null
    private var updatedAt: String? = null

    fun getId(): Int = id
    fun getMetadataId(): String? = metadataId
    fun setMetadataId(v: String?) { metadataId = v }
    fun getModel(): String? = model
    fun setModel(v: String?) { model = v }
    fun getContext(): String? = context
    fun setContext(v: String?) { context = v }
    fun getUpdatedAt(): String? = updatedAt
    fun setUpdatedAt(v: String?) { updatedAt = v }
}
