package com.miyagi.shashin.model

interface UserRoleCount {
    fun getAuthority(): String
    fun getAllowedCount(): Int
    fun getNotAllowedCount(): Int
}
