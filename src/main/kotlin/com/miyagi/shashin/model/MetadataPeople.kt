package com.miyagi.shashin.model

interface MetadataPeople {
    fun getId(): Int?
    fun getName(): String?
    fun getCoverUrl(): String?
    fun getTagCount(): Int?
    fun getMetadataId(): String?
    fun getThumbnailUrlCentered(): String?
}