package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*

@Entity
@Table(name = "memoryphoto", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("memory_id", "metadata_id"))])
class MemoryPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    private var memoryId: Int? = null
    private var metadataId: String? = null
    private var displayOrder: Int? = 0
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun MemoryPhoto() {}

    fun getId(): Int = this.id
    fun getMemoryId(): Int? = this.memoryId
    fun setMemoryId(memoryId: Int?) { this.memoryId = memoryId }
    fun getMetadataId(): String? = this.metadataId
    fun setMetadataId(metadataId: String?) { this.metadataId = metadataId }
    fun getDisplayOrder(): Int? = this.displayOrder
    fun setDisplayOrder(displayOrder: Int?) { this.displayOrder = displayOrder }
    fun getCreatedAt(): String? = this.createdAt
    fun setCreatedAt(createdAt: String?) { this.createdAt = createdAt }
    fun getModifiedAt(): String? = this.modifiedAt
    fun setModifiedAt(modifiedAt: String?) { this.modifiedAt = modifiedAt }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["memoryId"] = this.memoryId
        map["metadataId"] = this.metadataId
        map["displayOrder"] = this.displayOrder
        val mapper = ObjectMapper()
        var mapJson: String? = "{}"
        try { mapJson = mapper.writeValueAsString(map) } catch (e: IOException) { e.printStackTrace() }
        return mapJson.toString()
    }
}
