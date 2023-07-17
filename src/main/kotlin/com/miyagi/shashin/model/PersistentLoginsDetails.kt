package com.miyagi.shashin.model

interface PersistentLoginsDetails {
    fun getUsername(): String?
    fun getSeries(): String?
    fun getToken(): String?
    fun getExpiry(): Long?
    fun getHost(): String?
    fun getUseragent(): String?
}