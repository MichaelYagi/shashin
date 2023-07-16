package com.miyagi.shashin.util

import com.miyagi.shashin.repository.PersistentLoginsExpiryRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import org.springframework.core.io.FileSystemResource
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object DatabaseUtil {
    fun cleanupPersistence(persistentLoginsExpiryRepository: PersistentLoginsExpiryRepository?, persistentLoginsRepository: PersistentLoginsRepository?) {
        // Cleanup tasks
        val persistentLoginsExpiryList = persistentLoginsExpiryRepository?.findAll()
        if (persistentLoginsExpiryList != null && persistentLoginsExpiryList.count() > 0) {
            for (persistentLoginsExpiryObj in persistentLoginsExpiryList) {
                val series = persistentLoginsExpiryObj?.getSeries()
                if (series != null) {
                    val persistentLoginsCount = persistentLoginsRepository?.countPersistentLoginsBySeries(series)
                    if (persistentLoginsCount != null && persistentLoginsCount == 0) {
                        persistentLoginsExpiryRepository.deleteBySeries(series)
                    }
                }

                var seriesDeleted = false
                if (persistentLoginsExpiryObj != null && System.currentTimeMillis() > persistentLoginsExpiryObj.getExpiry()!!.toLong()) {
                    // delete entry in repos
                    persistentLoginsRepository?.deleteBySeries(series.toString())
                    persistentLoginsExpiryRepository.deleteBySeries(series.toString())
                    seriesDeleted = true
                }

                if (!seriesDeleted) {
                    val persistentLoginsCount = persistentLoginsRepository?.countPersistentLoginsBySeries(series.toString())
                    if (persistentLoginsCount != null && persistentLoginsCount == 0) {
                        persistentLoginsExpiryRepository.deleteBySeries(series.toString())
                    }
                }
            }
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun backup(fullDbName: String?): String {
        var backupName = ""
        val fullDbNameArray = fullDbName?.split(":")
        if (!fullDbNameArray.isNullOrEmpty() && fullDbNameArray.size > 2) {
            val dbNameArray = fullDbNameArray[2].split("?")
            if (dbNameArray.isNotEmpty()) {
                val dbName = dbNameArray[0]
                val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')

                val dir = File(rootPath)

                val names = dir.list { _, name ->
                    name.endsWith(".bak")
                }

                for (name in names) {
                    val fileToDelete = File("$rootPath/$name")
                    if (fileToDelete.exists()) {
                        fileToDelete.delete()
                    }
                }

                val dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                val now = LocalDateTime.now()
                val backupDbFile = File(rootPath + "/" + dbName + "." + dtf.format(now) + ".bak")
                val dbFile = File("$rootPath/$dbName")
                if (dbFile.exists()) {
                    dbFile.copyTo(backupDbFile)
                    backupName = dbName + "." + dtf.format(now) + ".bak"
                }
            }
        }

        return backupName
    }
}