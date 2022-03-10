package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank


@Entity
@Table(name = "albumphoto", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("albumId", "metadataId"))])
class AlbumPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["metadataId"] = this.metadataId
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