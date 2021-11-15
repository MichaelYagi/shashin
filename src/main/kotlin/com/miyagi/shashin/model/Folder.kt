package com.miyagi.shashin.model

interface Folder {
    fun getFolder(): String?
    fun getThumbnailUrlCentered(): String?
    fun getCount(): Int?
}