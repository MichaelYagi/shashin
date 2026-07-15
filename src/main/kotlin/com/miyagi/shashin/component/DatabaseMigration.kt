package com.miyagi.shashin.component

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.sql.Connection
import javax.sql.DataSource
import java.util.logging.Level
import java.util.logging.Logger

@Component
@Order(1)
class DatabaseMigration(private val dataSource: DataSource) : ApplicationRunner {

    private val logger = Logger.getLogger(DatabaseMigration::class.java.name)

    private val indexes = listOf(
        "CREATE INDEX IF NOT EXISTS `idx_metadata_hidden_ymd` ON metadata (`hidden`, `year` DESC, `month` DESC, `day` DESC)",
        "CREATE INDEX IF NOT EXISTS `idx_metadata_hidden_taken_at` ON metadata (`hidden`, `takenAt`)",
        "CREATE INDEX IF NOT EXISTS `idx_metadata_hidden_added_at` ON metadata (`hidden`, `addedAt` DESC)",
        "CREATE INDEX IF NOT EXISTS `idx_metadata_hidden_modified_at` ON metadata (`hidden`, `modifiedAt` DESC)",
        "CREATE INDEX IF NOT EXISTS `idx_metadata_hidden_accessed_at` ON metadata (`hidden`, `lastAccessedAt` DESC)",
        "CREATE INDEX IF NOT EXISTS `idx_metadata_hidden_folder` ON metadata (`hidden`, `folder`)",
        "CREATE INDEX IF NOT EXISTS `idx_rlp_metadata_id` ON recognitionlabelphoto (`metadata_id`)",
        "CREATE INDEX IF NOT EXISTS `idx_ap_album_id` ON albumphoto (`albumId`)",
        "CREATE INDEX IF NOT EXISTS `idx_kp_keyword_id` ON keywordphoto (`keywordId`)",
        "CREATE INDEX IF NOT EXISTS `idx_ua_album_id` ON useralbum (`albumId`)"
    )

    override fun run(args: ApplicationArguments) {
        dataSource.connection.use { conn: Connection ->
            conn.autoCommit = false
            try {
                for (sql in indexes) {
                    conn.createStatement().use { it.execute(sql) }
                }
                conn.commit()
                logger.log(Level.INFO, "Database index migration complete")
            } catch (e: Exception) {
                conn.rollback()
                logger.log(Level.WARNING, "Database index migration failed: ${e.message}")
            }
        }
    }
}
