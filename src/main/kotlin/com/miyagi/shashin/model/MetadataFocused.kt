package com.miyagi.shashin.model

import jakarta.persistence.Column

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
    @Column(columnDefinition = "varchar")
    fun getThumbnailUrlExtraSmall(): String?
    fun getThumbnailUrlCentered(): String?
}