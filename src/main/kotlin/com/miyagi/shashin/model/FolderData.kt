package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "folderdata")
class FolderData() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0

    @NotBlank
    private var folder: String? = null

    @NotBlank
    private var coverUrl: String? = null

    fun FolderData() {}

    fun getFolder(): String? {
        return this.folder
    }

    fun setFolder(folder: String) {
        this.folder = folder
    }

    fun getCoverUrl(): String? {
        return this.coverUrl
    }

    fun setCoverUrl(coverUrl: String) {
        this.coverUrl = coverUrl
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["userId"] = this.folder
        map["metadataId"] = this.coverUrl
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