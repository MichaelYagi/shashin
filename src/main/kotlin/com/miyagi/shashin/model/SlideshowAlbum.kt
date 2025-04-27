package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "slideshowalbum")
class SlideshowAlbum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    @NotBlank
    private var userId: Int? = null
    @NotBlank
    private var albums: String? = null

    fun getId(): Int {
        return this.id
    }

    fun getUserId(): Int? {
        return this.userId
    }

    fun setUserId(userId: Int?) {
        this.userId = userId
    }

    fun getAlbums(): String? {
        return this.albums
    }

    fun setAlbums(albums: String?) {
        this.albums = albums
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["userId"] = this.userId
        map["albums"] = this.albums

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