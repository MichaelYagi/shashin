package com.miyagi.shashin.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "useralbum", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("albumId", "userId"))])
class UserAlbum {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private var id: Int = 0

    @NotBlank
    private var userId: Int? = null

    @NotBlank
    private var albumId: Int? = null

    private var createdAt: String? = null

    private var modifiedAt: String? = null

    fun UserAlbum() {}

    fun getId(): Int {
        return this.id
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
        return "UserAlbum{" +
                "id=" + this.id +
                ", userId='" + this.userId + '\'' +
                ", albumId=" + this.albumId +
                '}'
    }
}