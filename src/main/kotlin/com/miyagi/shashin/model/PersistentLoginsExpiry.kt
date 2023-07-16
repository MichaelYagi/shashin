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
    private var expiry: Long? = null
    private var host: String? = null

    fun getSeries(): String? {
        return this.series
    }

    fun setSeries(series: String?) {
        this.series = series
    }

    fun getExpiry(): Long? {
        return this.expiry
    }

    fun setExpiry(expiry: Long?) {
        this.expiry = expiry
    }

    fun getHost(): String? {
        return this.host
    }

    fun setHost(host: String?) {
        this.host = host
    }
}