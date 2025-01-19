package com.miyagi.shashin.model

interface Folder {
    fun getMetadataId(): String?
    fun getFolder(): String?
    fun getThumbnailUrlCentered(): String?
    fun getCount(): Int?
}