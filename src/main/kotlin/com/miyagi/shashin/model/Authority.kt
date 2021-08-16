package com.miyagi.shashin.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotBlank

@Entity
@Table(name = "authority")
class Authority {
    @Id
    @GeneratedValue
    private var id: Int = 0
    @NotBlank
    private var userId: Int? = 0
    @NotBlank
    private var authority: String? = null

    fun Authority() {}

    fun Authority(
        @NotBlank userId: Int?,
        @NotBlank authority: String?
    ) {
        this.userId = userId
        this.authority = authority
    }

    fun getId(): Int {
        return this.id
    }

    fun getUserId(): Int? {
        return this.userId
    }

    fun setUserId(userId: Int?) {
        this.userId = userId
    }

    fun getAuthority(): String? {
        return this.authority
    }

    fun setAuthority(authority: String?) {
        this.authority = authority
    }
}