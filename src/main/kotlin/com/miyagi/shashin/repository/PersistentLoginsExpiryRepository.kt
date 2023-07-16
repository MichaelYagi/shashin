package com.miyagi.shashin.repository

import com.miyagi.shashin.model.PersistentLogins
import com.miyagi.shashin.model.PersistentLoginsExpiry
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface PersistentLoginsExpiryRepository : CrudRepository<PersistentLoginsExpiry?, String?> {
    fun deleteBySeries(series: String): Long
}