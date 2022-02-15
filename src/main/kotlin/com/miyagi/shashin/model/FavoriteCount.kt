package com.miyagi.shashin.model

interface FavoriteCount {
    fun getId(): Int?
    fun getMetadataId(): String?
    fun getUserId(): Int?
    fun getCreatedAt(): String?
    fun getModifiedAt(): String?
    fun getCount(): Int?
}