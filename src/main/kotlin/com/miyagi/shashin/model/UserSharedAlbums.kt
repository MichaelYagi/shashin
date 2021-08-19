package com.miyagi.shashin.model

interface UserSharedAlbums {
    fun getUserId(): Int?
    fun getUsername(): String?
    fun getAlbumId(): Int?
    fun getIsShared(): Int?

//    private var userId: Int?
//    private var username: String? = null
//    private var albumId: Int? = null
//    private var isShared: Int? = null
//
//    constructor(userId: Int?,username: String?,albumId: Int?,isShared: Int?) {
//        this.userId = userId
//        this.username = username
//        this.albumId = albumId
//        this.isShared = isShared
//    }
//
//    fun getUserId(): Int? {
//        return this.userId
//    }
//    fun setUserId(userId: Int?) {
//        this.userId = userId
//    }
//    fun getUsername(): String? {
//        return this.username
//    }
//    fun setUsername(username: String?) {
//        this.username = username
//    }
//    fun getAlbumId(): Int? {
//        return this.albumId
//    }
//    fun setAlbumId(albumId: Int?) {
//        this.albumId = userId
//    }
//    fun getIsShared(): Int? {
//        return this.isShared
//    }
//    fun setIsShared(isShared: Int?) {
//        this.isShared = isShared
//    }
}