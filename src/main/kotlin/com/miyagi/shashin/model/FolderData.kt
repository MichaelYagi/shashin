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
    private var mid: String? = null

    fun FolderData() {}

    fun getFolder(): String? {
        return this.folder
    }

    fun setFolder(folder: String) {
        this.folder = folder
    }

    fun getMid(): String? {
        return this.mid
    }

    fun setMid(mid: String) {
        this.mid = mid
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["userId"] = this.folder
        map["mid"] = this.mid
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