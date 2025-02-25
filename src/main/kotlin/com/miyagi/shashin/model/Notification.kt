package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "notification")
class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0

    @NotBlank
    private var userId: Int? = null

    private var type: String? = null

    private var identifier: String? = null

    private var imageUrl: String? = null

    @NotBlank
    private var message: String? = null

    private var createdAt: String? = null

    private var modifiedAt: String? = null

    @NotBlank
    private var read: Boolean? = null

    fun Notification() {}

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

    fun getType(): String? {
        return this.type
    }

    fun setType(type: String?) {
        this.type = type
    }

    fun getIdentifier(): String? {
        return this.identifier
    }

    fun setIdentifier(identifier: String?) {
        this.identifier = identifier
    }

    fun getImageUrl(): String? {
        return this.imageUrl
    }

    fun setImageUrl(imageUrl: String?) {
        this.imageUrl = imageUrl
    }

    fun getMessage(): String? {
        return this.message
    }

    fun setMessage(message: String?) {
        this.message = message
    }

    fun getRead(): Boolean? {
        return this.read
    }

    fun setRead(read: Boolean?) {
        this.read = read
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
        map["type"] = this.type
        map["identifier"] = this.identifier
        map["imageUrl"] = this.imageUrl
        map["message"] = this.message
        map["read"] = this.read
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