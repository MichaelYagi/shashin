package com.miyagi.shashin.model

interface AlbumComments {
    fun getComment(): String?
    fun getCommentId(): Int?
    fun getAlbumId(): Int?
    fun getUserId(): Int?
    fun getUsername(): String?
    fun getCreatedAt(): String?
}