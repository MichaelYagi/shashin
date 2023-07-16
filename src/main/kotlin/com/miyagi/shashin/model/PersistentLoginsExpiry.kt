package com.miyagi.shashin.model

import javax.persistence.*
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "persistent_logins_expiry")
class PersistentLoginsExpiry {
    @Id
    @NotBlank
    private var series: String? = null
    @NotBlank
    private var expiry: String? = null
    private var host: String? = null

    fun getSeries(): String? {
        return this.series
    }

    fun setSeries(series: String?) {
        this.series = series
    }

    fun getExpiry(): String? {
        return this.expiry
    }

    fun setExpiry(expiry: String?) {
        this.expiry = expiry
    }

    fun getHost(): String? {
        return this.host
    }

    fun setHost(host: String?) {
        this.host = host
    }
}