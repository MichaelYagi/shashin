package com.miyagi.shashin.model

interface MetadataMinimal {
    fun getId(): String?
    fun getType(): String?
    fun getLat(): String?
    fun getLng(): String?
    fun getThumbnailPathSmall(): String?
    fun getThumbnailUrlSmall(): String?
    fun getThumbnailPathExtraSmall(): String?
    fun getThumbnailUrlExtraSmall(): String?
    fun getThumbnailPathCentered(): String?
    fun getThumbnailUrlCentered(): String?
    fun getThumbnailUrlOriginal(): String?
    fun getThumbnailSmallWidth(): Int?
    fun getThumbnailSmallHeight(): Int?
    fun getOriginalImageWidth(): Int?
    fun getOriginalImageHeight(): Int?
    fun getVideoUrl(): String?
    fun getMapMarkerUrl(): String?
    fun getPlaceName(): String?
    fun getYear(): Int?
    fun getMonth(): Int?
    fun getDay(): Int?
}