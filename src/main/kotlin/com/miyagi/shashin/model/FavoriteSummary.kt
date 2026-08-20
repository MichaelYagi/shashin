package com.miyagi.shashin.model

interface FavoriteSummary {
    fun getMetadataId(): String?
    fun getCount(): Int?
    fun getUserFavorited(): Int?
}
