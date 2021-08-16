package com.miyagi.shashin.model

import javax.persistence.*
import javax.validation.constraints.NotBlank;
import java.util.Objects;

@Entity
@Table(name = "user")
class User {
    @Id
    @GeneratedValue
    private var id: Int = 0
    @NotBlank
    private var username: String? = null
    @NotBlank
    private var password: String? = null
    @NotBlank
    private var loggedIn = false
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

    fun isLoggedIn(): Boolean {
        return loggedIn
    }

    fun setLoggedIn(loggedIn: Boolean) {
        this.loggedIn = loggedIn
    }

    fun sameAs(o: Any): Boolean {
        if (this === o) return true
        if (o !is User) return false
        val user: User = o
        return Objects.equals(username, user.username) &&
                Objects.equals(password, user.password)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id, username, password,
            loggedIn
        )
    }

    override fun toString(): String {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", loggedIn=" + loggedIn +
                '}'
    }
}