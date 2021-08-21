package com.miyagi.shashin.model

interface AlbumPhotoComments {
    fun getComment(): String?
    fun getCommentId(): Int?
    fun getMetadataId(): String?
    fun getAlbumId(): Int?
    fun getUserId(): Int?
    fun getUsername(): String?
    fun getCreatedAt(): String?
}