package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "keywordphoto", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("keywordId", "metadataId"))])
class KeywordPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0

    @NotBlank
    private var metadataId: String? = null

    @NotBlank
    private var keywordId: Int? = null

    private var createdAt: String? = null

    private var modifiedAt: String? = null

    fun KeywordPhoto() {}

    fun getId(): Int {
        return this.id
    }

    fun getMetadataId(): String? {
        return this.metadataId
    }

    fun setMetadataId(metadataId: String?) {
        this.metadataId = metadataId
    }

    fun getKeywordId(): Int? {
        return this.keywordId
    }

    fun setKeywordId(keywordId: Int?) {
        this.keywordId = keywordId
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
        map["metadataId"] = this.metadataId
        map["keywordId"] = this.keywordId
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