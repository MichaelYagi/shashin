package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "recognitionlabel")
class RecognitionLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    @NotBlank
    private var name: String? = null
    private var coverUrl: String? = null
    private var argusIdentityId: Int? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun RecognitionLabel() {}

    fun getId(): Int {
        return this.id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getName(): String? {
        return this.name
    }

    fun setName(name: String?) {
        this.name = name
    }

    fun getCoverUrl(): String? {
        return this.coverUrl
    }

    fun setCoverUrl(coverUrl: String?) {
        this.coverUrl = coverUrl
    }

    fun getArgusIdentityId(): Int? {
        return this.argusIdentityId
    }

    fun setArgusIdentityId(argusIdentityId: Int?) {
        this.argusIdentityId = argusIdentityId
    }

    fun getCreatedAt(): String? {
        return this.createdAt
    }

    fun setCreatedAt(createdAt: String?) {
        this.createdAt = createdAt
    }

    fun getModifiedAt(): String? {
        return this.modifiedAt
    }

    fun setModifiedAt(modifiedAt: String?) {
        this.modifiedAt = modifiedAt
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["name"] = this.name
        map["coverUrl"] = this.coverUrl
        val mapper = ObjectMapper()
        var mapJson: String? = "{}"
        try {
            mapJson = mapper.writeValueAsString(map)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return mapJson.toString()
    }
}