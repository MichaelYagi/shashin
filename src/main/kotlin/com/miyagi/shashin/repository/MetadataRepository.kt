package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Metadata
import org.springframework.data.domain.Sort
import org.springframework.data.repository.CrudRepository

interface MetadataRepository : CrudRepository<Metadata?, String?> {
   fun findAll(sort: Sort?): MutableIterable<Metadata?>?
}