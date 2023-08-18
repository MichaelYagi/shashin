package com.miyagi.shashin.util

import com.miyagi.shashin.repository.PersistentLoginsExpiryRepository
import com.miyagi.shashin.repository.PersistentLoginsRepository
import org.springframework.core.io.FileSystemResource
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object DatabaseUtil {
    fun cleanupPersistence(persistentLoginsExpiryRepository: PersistentLoginsExpiryRepository?, persistentLoginsRepository: PersistentLoginsRepository?) {
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
                if (persistentLoginsExpiryObj != null && System.currentTimeMillis() > persistentLoginsExpiryObj.getExpiry()!!) {
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
    fun backup(fullDbName: String?): File? {
        var backupDbFile: File? = null
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
                backupDbFile = File(rootPath + "/" + dbName + "." + dtf.format(now) + ".bak")
                val dbFile = File("$rootPath/$dbName")
                if (dbFile.exists()) {
                    dbFile.copyTo(backupDbFile)
                }
            }
        }

        return backupDbFile
    }

    fun import(fullDbName: String?, inputStream: InputStream?): Boolean {
        var success = false
        var backupDbFile: File? = null
        val fullDbNameArray = fullDbName?.split(":")
        if (!fullDbNameArray.isNullOrEmpty() && fullDbNameArray.size > 2) {
            val dbNameArray = fullDbNameArray[2].split("?")
            if (dbNameArray.isNotEmpty()) {
                val dbName = dbNameArray[0]
                val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')

                val dir = File(rootPath)

                val names = dir.list { _, name ->
                    name == dbName
                }

                for (name in names) {
                    val fileToDelete = File("$rootPath/$name")
                    if (fileToDelete.exists()) {
                        fileToDelete.delete()
                    }
                }

                backupDbFile = File("$rootPath/$dbName")
                if (inputStream != null) {
                    copyInputStreamToFile(inputStream, backupDbFile)
                    success = true
                }
            }
        }

        return success
    }

    @Throws(IOException::class)
    fun copyInputStreamToFile(inputStream: InputStream, file: File?) {

        // append = false
        FileOutputStream(file, false).use { outputStream ->
            var read: Int
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (inputStream.read(bytes).also { read = it } != -1) {
                outputStream.write(bytes, 0, read)
            }
        }
    }
}