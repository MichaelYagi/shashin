package com.miyagi.shashin.repository

import com.miyagi.shashin.model.PersistentLogins
import com.miyagi.shashin.model.RecognitionLabelPhoto
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface PersistentLoginsRepository : CrudRepository<PersistentLogins?, String?> {
    fun countPersistentLoginsBySeries(series: String): Int
    fun deleteBySeries(series: String): Int
}