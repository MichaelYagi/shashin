package com.miyagi.shashin.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "persistent_logins")
class PersistentLogins {
    @NotBlank
    private var username: String? = null
    @Id
    @NotBlank
    private var series: String? = null
    @NotBlank
    private var token: String? = null
    @NotBlank
    private var lastUsed: String? = null

    fun getUsername(): String? {
        return this.username
    }

    fun setUsername(username: String?) {
        this.username = username
    }

    fun getSeries(): String? {
        return this.series
    }

    fun setSeries(series: String?) {
        this.series = series
    }

    fun getToken(): String? {
        return this.token
    }

    fun setToken(token: String?) {
        this.token = token
    }

    fun getLastUsed(): String? {
        return this.lastUsed
    }

    fun setLastUsed(lastUsed: String?) {
        this.lastUsed = lastUsed
    }
}