package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import javax.persistence.*
import javax.validation.constraints.NotBlank;
import java.util.Objects;

@Entity
@Table(name = "user")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    @NotBlank
    private var username: String? = null
    @NotBlank
    private var password: String? = null
    @NotBlank
    private var loggedIn: Boolean? = false
    @NotBlank
    private var isAllowed: Boolean? = false
    @NotBlank
    private var darkMode: Boolean? = false
    private var authority: String? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun User() {}

    fun User(
        @NotBlank username: String?,
        @NotBlank password: String?
    ) {
        this.username = username
        this.password = password
        this.loggedIn = false
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getId(): Int {
        return this.id
    }

    fun getUsername(): String? {
        return this.username
    }

    fun setUsername(username: String?) {
        this.username = username
    }

    fun getPassword(): String? {
        return this.password
    }

    fun setPassword(password: String?) {
        this.password = password
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

    fun getAuthority(): String? {
        return this.authority
    }

    fun setAuthority(authority: String?) {
        this.authority = authority
    }

    fun getLoggedIn(): Boolean? {
        return this.loggedIn
    }

    fun setLoggedIn(loggedIn: Boolean?) {
        this.loggedIn = loggedIn
    }

    fun getIsAllowed(): Boolean? {
        return this.isAllowed
    }

    fun setIsAllowed(isAllowed: Boolean?) {
        this.isAllowed = isAllowed
    }

    fun getDarkMode(): Boolean? {
        return this.darkMode
    }

    fun setDarkMode(darkMode: Boolean?) {
        this.darkMode = darkMode
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        val user: User = other
        return Objects.equals(username, user.username)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id, username, password, authority, loggedIn
        )
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["username"] = this.username
        map["authority"] = this.authority
        map["loggedIn"] = this.loggedIn
        map["isAllowed"] = this.isAllowed
        map["darkMode"] = this.darkMode
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