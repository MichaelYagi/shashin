package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Settings
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.CrudRepository
import javax.transaction.Transactional

@Transactional
interface SettingsRepository : CrudRepository<Settings?, Int?> {
    @Cacheable("firstSettingQuery")
    fun findFirstByOrderByIdAsc(): Settings?
}