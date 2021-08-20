package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "comment")
class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private var id: Int = 0

    @NotBlank
    private var comment: String? = null

    private var createdAt: String? = null

    private var modifiedAt: String? = null

    fun Comment() {}

    fun getId(): Int {
        return this.id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getComment(): String? {
        return this.comment
    }

    fun setComment(comment: String?) {
        this.comment = comment
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
        map["comment"] = this.comment
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