package com.miyagi.shashin.model

interface MediaStats {
    fun getPhotoCount(): Int?
    fun getVideoCount(): Int?
    fun getNotLocatedCount(): Int?
    fun getHiddenCount(): Int?
}
