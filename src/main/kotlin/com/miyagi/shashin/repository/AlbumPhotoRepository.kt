package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhoto
import com.miyagi.shashin.model.UserAlbum
import org.springframework.data.repository.CrudRepository

interface AlbumPhotoRepository : CrudRepository<AlbumPhoto?, Int?> {
    fun countByMetadataIdAndAlbumId(metdataId: String?, albumId: Int?): Int?
    fun countByAlbumId(albumId: Int?): Int?
    fun findAllByAlbumId(albumId: Int?): MutableIterable<AlbumPhoto?>?
    fun deleteByMetadataIdAndAlbumId(metadataId: String?, albumId: Int?): Long?
    fun deleteByAlbumId(albumId: Int?): Long?
    fun findFirstByOrderByIdAsc(): AlbumPhoto?
}