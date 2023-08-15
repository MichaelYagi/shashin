package com.miyagi.shashin.model

interface MapData {
    fun getId(): String?
    fun getType(): String?
    fun getLat(): String?
    fun getLng(): String?
    fun getThumbnailUrlOriginal(): String?
    fun getVideoUrl(): String?
    fun getMapMarkerUrl(): String?
    fun getYear(): Int?
    fun getMonth(): Int?
    fun getDay(): Int?
}