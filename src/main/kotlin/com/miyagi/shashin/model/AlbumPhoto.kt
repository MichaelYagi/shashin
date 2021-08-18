package com.miyagi.shashin.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "albumphoto", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("albumId", "metadataId"))])
class AlbumPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private var id: Int = 0

    @NotBlank
    private var metadataId: String? = null

    @NotBlank
    private var albumId: Int? = null

    private var createdAt: String? = null

    private var modifiedAt: String? = null

    fun AlbumPhoto() {}

    fun getId(): Int {
        return this.id
    }

    fun getMetadataId(): String? {
        return this.metadataId
    }

    fun setMetadataId(metadataId: String?) {
        this.metadataId = metadataId
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
        return "AlbumPhoto{" +
                "id=" + this.id +
                ", metadataId='" + this.metadataId + '\'' +
                ", albumId=" + this.albumId +
                '}'
    }
}