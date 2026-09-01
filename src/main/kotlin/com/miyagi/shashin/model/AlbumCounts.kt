package com.miyagi.shashin.model

interface AlbumCounts {
    fun getAlbumId(): Int
    fun getTotalCount(): Int
    fun getPhotoCount(): Int
    fun getVideoCount(): Int
}
