package com.miyagi.shashin.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "album")
class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private var id: Int = 0
    @NotBlank
    private var name: String? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun Album() {}

    fun getId(): Int {
        return this.id
    }

    fun getName(): String? {
        return this.name
    }

    fun setName(name: String?) {
        this.name = name
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
        return "Album{" +
                "id=" + this.id +
                ", name='" + this.name + '\'' +
                '}'
    }
}