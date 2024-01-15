package com.miyagi.shashin.util

import org.springframework.core.io.FileSystemResource
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object DatabaseUtils {
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
        var backupDbFile: File?
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
    private fun copyInputStreamToFile(inputStream: InputStream, file: File?) {

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