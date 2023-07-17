package com.miyagi.shashin.repository

import com.miyagi.shashin.model.PersistentLogins
import com.miyagi.shashin.model.PersistentLoginsDetails
import com.miyagi.shashin.model.RecognitionLabelPhoto
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface PersistentLoginsRepository : CrudRepository<PersistentLogins?, String?> {
    fun countPersistentLoginsBySeries(series: String): Int
    fun deleteBySeries(series: String): Int

    @Query("SELECT pl.username, pl.series, pl.token, ple.expiry, ple.host, ple.useragent FROM persistent_logins pl INNER JOIN persistent_logins_expiry ple on pl.series = ple.series WHERE pl.series = :series", nativeQuery = true)
    fun findPersistentLoginsDetails(series: String): PersistentLoginsDetails

    @Query("SELECT pl.username, pl.series, pl.token, ple.expiry, ple.host, ple.useragent FROM persistent_logins pl INNER JOIN persistent_logins_expiry ple on pl.series = ple.series", nativeQuery = true)
    fun findAllPersistentLoginsDetails(): MutableIterable<PersistentLoginsDetails?>?

}