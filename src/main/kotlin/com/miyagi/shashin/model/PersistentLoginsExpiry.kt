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
}