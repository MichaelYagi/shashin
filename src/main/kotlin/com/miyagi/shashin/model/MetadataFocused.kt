package com.miyagi.shashin.model

interface MetadataFocused {
    fun getId(): String?
    fun getYear(): Int?
    fun getMonth(): Int?
    fun getDay(): Int?
    fun getType(): String?
    fun getFileName(): String?
    fun getThumbnailSmallWidth(): Int?
    fun getThumbnailSmallHeight(): Int?
    fun getThumbnailUrlSmall(): String?
    fun getThumbnailUrlExtraSmall(): String?
    fun getThumbnailUrlCentered(): String?
}