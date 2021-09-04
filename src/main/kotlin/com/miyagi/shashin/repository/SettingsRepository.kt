package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Settings
import org.springframework.data.repository.CrudRepository

interface SettingsRepository : CrudRepository<Settings?, Int?> {
    fun findFirstByOrderByIdAsc(): Settings?
}