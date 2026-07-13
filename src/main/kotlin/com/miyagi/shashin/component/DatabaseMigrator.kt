package com.miyagi.shashin.component

import jakarta.annotation.PostConstruct
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.util.logging.Level
import java.util.logging.Logger

@Component
class DatabaseMigrator(private val jdbcTemplate: JdbcTemplate) {

    private val logger = Logger.getLogger(DatabaseMigrator::class.simpleName)

    @PostConstruct
    fun migrate() {
        addColumnIfMissing("metadata", "embedding", "TEXT DEFAULT NULL")
        addColumnIfMissing("settings", "ollamaEmbedModel", "VARCHAR(255) DEFAULT NULL")
    }

    private fun addColumnIfMissing(table: String, column: String, definition: String) {
        try {
            jdbcTemplate.execute("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            logger.log(Level.INFO, "Migration: added $column to $table")
        } catch (e: Exception) {
            // Column already exists — safe to ignore
        }
    }
}
