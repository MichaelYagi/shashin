package com.miyagi.shashin.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "mediadir")
class MediaDirectory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    @NotBlank
    private var directory: String? = null
    @NotBlank
    private var exclude: Boolean? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun MediaDirectory() {}

    fun getId(): Int {
        return this.id
    }

    fun getDirectory(): String? {
        return this.directory
    }

    fun setDirectory(directory: String?) {
        this.directory = directory
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

    fun getExclude(): Boolean? {
        return this.exclude
    }

    fun setExclude(exclude: Boolean?) {
        this.exclude = exclude
    }
}