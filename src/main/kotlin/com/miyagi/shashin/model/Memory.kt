package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*

@Entity
@Table(name = "memory")
class Memory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    private var title: String? = null
    private var caption: String? = null
    private var generatedDate: String? = null
    private var strategyType: String? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun Memory() {}

    fun getId(): Int = this.id
    fun setId(id: Int) { this.id = id }
    fun getTitle(): String? = this.title
    fun setTitle(title: String?) { this.title = title }
    fun getCaption(): String? = this.caption
    fun setCaption(caption: String?) { this.caption = caption }
    fun getGeneratedDate(): String? = this.generatedDate
    fun setGeneratedDate(generatedDate: String?) { this.generatedDate = generatedDate }
    fun getStrategyType(): String? = this.strategyType
    fun setStrategyType(strategyType: String?) { this.strategyType = strategyType }
    fun getCreatedAt(): String? = this.createdAt
    fun setCreatedAt(createdAt: String?) { this.createdAt = createdAt }
    fun getModifiedAt(): String? = this.modifiedAt
    fun setModifiedAt(modifiedAt: String?) { this.modifiedAt = modifiedAt }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["title"] = this.title
        map["caption"] = this.caption
        map["strategyType"] = this.strategyType
        map["generatedDate"] = this.generatedDate
        val mapper = ObjectMapper()
        var mapJson: String? = "{}"
        try { mapJson = mapper.writeValueAsString(map) } catch (e: IOException) { e.printStackTrace() }
        return mapJson.toString()
    }
}
