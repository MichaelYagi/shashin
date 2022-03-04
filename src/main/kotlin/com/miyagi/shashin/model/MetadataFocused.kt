package com.miyagi.shashin.model

interface MetadataFocused {
    fun getId(): String?
    fun getYear(): Int?
    fun getMonth(): Int?
    fun getDay(): Int?
    fun getType(): String?
    fun getFileName(): String?
    fun getThumbnailUrlSmall(): String?
    fun getThumbnailUrlCentered(): String?
    fun getThumbnailSmallWidth(): Int?
    fun getThumbnailSmallHeight(): Int?
}