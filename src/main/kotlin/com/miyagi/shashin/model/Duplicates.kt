package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
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
    private var imageIdOne: String? = null

    @NotBlank
    private var imageIdTwo: String? = null

    private var distance: Int? = null

    private var createdAt: String? = null

    fun getId(): Int {
        return this.id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getImageIdOne(): String? {
        return this.imageIdOne
    }

    fun setImageIdOne(imageIdOne: String?) {
        this.imageIdOne = imageIdOne
    }

    fun getImageIdTwo(): String? {
        return this.imageIdTwo
    }

    fun setImageIdTwo(imageIdTwo: String?) {
        this.imageIdTwo = imageIdTwo
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
        map["imageIdOne"] = this.imageIdOne
        map["imageIdTwo"] = this.imageIdTwo
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