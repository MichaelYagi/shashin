package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumPhoto
import org.springframework.data.repository.CrudRepository

interface AlbumPhotoRepository : CrudRepository<AlbumPhoto?, Int?> {
    fun countByMetadataIdAndAlbumId(metdataId: String?, albumId: Int?): Int?
}