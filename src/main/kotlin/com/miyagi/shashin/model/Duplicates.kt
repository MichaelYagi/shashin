package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import java.io.IOException

@Entity
@Table(name = "duplicates")
class Duplicates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0

    @NotBlank
    private var imageId1: String? = null

    @NotBlank
    private var imageId2: String? = null

    private var distance: Int? = null

    private var createdAt: String? = null

    fun Comment() {}

    fun getId(): Int {
        return this.id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getImageId1(): String? {
        return this.imageId1
    }

    fun setImageId1(imageId1: String?) {
        this.imageId1 = imageId1
    }

    fun getImageId2(): String? {
        return this.imageId2
    }

    fun setImageId2(imageId2: String?) {
        this.imageId2 = imageId2
    }

    fun getDistance(): Int? {
        return this.distance
    }

    fun setDistance(distance: Int?) {
        this.distance = distance
    }

    fun getCreatedAt(): String? {
        return this.createdAt
    }

    fun setCreatedAt(createdAt: String?) {
        this.createdAt = createdAt
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["imageId1"] = this.imageId1
        map["imageId2"] = this.imageId2
        map["distance"] = this.distance
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