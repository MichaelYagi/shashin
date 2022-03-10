package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "favorite", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("metadataId", "userId"))])
class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0

    @NotBlank
    private var userId: Int? = null

    @NotBlank
    private var metadataId: String? = null

    private var createdAt: String? = null

    private var modifiedAt: String? = null

    fun Favorite() {}

    fun getId(): Int {
        return this.id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getUserId(): Int? {
        return this.userId
    }

    fun setUserId(userId: Int?) {
        this.userId = userId
    }

    fun getMetadataId(): String? {
        return this.metadataId
    }

    fun setMetadataId(metadataId: String?) {
        this.metadataId = metadataId
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
        map["userId"] = this.userId
        map["metadataId"] = this.metadataId
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