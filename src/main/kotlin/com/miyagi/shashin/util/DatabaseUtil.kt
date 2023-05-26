package com.miyagi.shashin.util

import org.springframework.core.io.FileSystemResource
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object DatabaseUtil {
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

                val names = dir.list { dir, name ->
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