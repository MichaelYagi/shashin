package com.miyagi.shashin.model

interface MapData {
    fun getId(): String?
    fun getType(): String?
    fun getLat(): String?
    fun getLng(): String?
    fun getThumbnailUrlSmall(): String?
    fun getThumbnailUrlOriginal(): String?
    fun getOriginalImageWidth(): Int?
    fun getOriginalImageHeight(): Int?
    fun getVideoUrl(): String?
    fun getMapMarkerUrl(): String?
    fun getPlaceName(): String?
    fun getYear(): Int?
    fun getMonth(): Int?
    fun getDay(): Int?
}