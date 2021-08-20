package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "albumcomment", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("commentId", "albumId"))])
class AlbumComment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private var id: Int = 0

    @NotBlank
    private var commentId: Int? = null

    @NotBlank
    private var albumId: Int? = null

    private var createdAt: String? = null

    private var modifiedAt: String? = null

    fun AlbumComment() {}

    fun getId(): Int {
        return this.id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getAlbumId(): Int? {
        return this.albumId
    }

    fun setAlbumId(albumId: Int?) {
        this.albumId = albumId
    }

    fun getCommentId(): Int? {
        return this.commentId
    }

    fun setCommentId(commentId: Int?) {
        this.commentId = commentId
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
        map["commentId"] = this.commentId
        map["albumId"] = this.albumId
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