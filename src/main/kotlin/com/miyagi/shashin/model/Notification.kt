package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "notification")
class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private var id: Int = 0

    @NotBlank
    private var userId: Int? = null

    private var albumId: Int? = null

    private var metadataId: String? = null

    private var commentId: Int? = null

    private var favoriteId: Int? = null

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

    fun getAlbumId(): Int? {
        return this.albumId
    }

    fun setAlbumId(albumId: Int?) {
        this.albumId = albumId
    }

    fun getFavoriteId(): Int? {
        return this.favoriteId
    }

    fun setFavoriteId(favoriteId: Int?) {
        this.favoriteId = favoriteId
    }

    fun getCommentId(): Int? {
        return this.commentId
    }

    fun setCommentId(commentId: Int?) {
        this.commentId = commentId
    }

    fun getMetadataId(): String? {
        return this.metadataId
    }

    fun setMetadataId(metadataId: String?) {
        this.metadataId = metadataId
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
        map["albumId"] = this.albumId
        map["commentId"] = this.commentId
        map["favoriteId"] = this.favoriteId
        map["metadataId"] = this.metadataId
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