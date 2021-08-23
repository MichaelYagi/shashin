package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Favorite
import com.miyagi.shashin.model.Metadata
import org.springframework.data.repository.CrudRepository

interface FavoriteRepository : CrudRepository<Favorite?, Int?> {
    fun findByMetadataIdAndUserId(metdataId: String?, userId: Int?): Favorite?
    fun findAllByUserId(userId: Int?): MutableIterable<Favorite?>?
    fun deleteByMetadataIdAndUserId(metdataId: String?, userId: Int?): Long
    fun deleteByUserId(userId: Int?): Long
}