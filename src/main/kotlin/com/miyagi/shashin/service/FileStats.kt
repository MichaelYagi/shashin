package com.miyagi.shashin.service

import com.miyagi.shashin.util.ImageProcessing
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import java.io.File
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Level
import java.util.logging.Logger

@Service
@CacheConfig(cacheNames=["filestats"])
class FileStats {
    private var logger: Logger = Logger.getLogger(FileStats::class.simpleName)

    @Cacheable("getFileStats")
    fun getFileStats(model: Model): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        // Files stats
        val kilo = 1024
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
        var sidecarSize = 0.toLong()
        try {
            sidecarSize = if (Files.isSymbolicLink(Paths.get(sidecarDir))) {
                Files.walk(Paths.get(sidecarDir), FileVisitOption.FOLLOW_LINKS)
                    .mapToLong { p -> p.toFile().length() }.sum()
            } else {
                Files.walk(Paths.get(sidecarDir)).mapToLong { p -> p.toFile().length() }.sum()
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error calculating sidecar size:" + e.message)
        }
        var sidecarSizeProcessed = sidecarSize.toDouble() / (kilo * kilo)
        var sidecarSizeNotation = "MB"
        if (sidecarSizeProcessed > kilo) {
            sidecarSizeProcessed /= kilo
            sidecarSizeNotation = "GB"
        }
        if (sidecarSizeProcessed > kilo) {
            sidecarSizeProcessed /= kilo
            sidecarSizeNotation = "TB"
        }
        response["sidecarUsedSizeText"] = "${String.format("%.2f", sidecarSizeProcessed)} $sidecarSizeNotation"
        response["sidecarUsedSizeB"] = sidecarSize.toDouble()

        var rawSidecarUsabe: Double
        var sidecarUsabe: Double
        try {
            if (File(sidecarDir).exists()) {
                var dir = Paths.get(sidecarDir)
                dir = dir.toRealPath()
                val fs = Files.getFileStore(dir)
                sidecarUsabe = fs.usableSpace.toDouble() / (kilo * kilo).toDouble()
                rawSidecarUsabe = fs.usableSpace.toDouble()
            } else {
                var dir = Paths.get(rootPath)
                dir = dir.toRealPath()
                val fs = Files.getFileStore(dir)
                sidecarUsabe = fs.usableSpace.toDouble() / (kilo * kilo).toDouble()
                rawSidecarUsabe = fs.usableSpace.toDouble()
            }
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "Error reading sidecar directory:" + exception.message)
            sidecarUsabe = 0.0
            rawSidecarUsabe = 0.0
        }
        var sidecarUsabeNotation = "MB"
        if (sidecarUsabe > kilo) {
            sidecarUsabe /= kilo
            sidecarUsabeNotation = "GB"
        }
        if (sidecarUsabe > kilo) {
            sidecarUsabe /= kilo
            sidecarUsabeNotation = "TB"
        }
        response["sidecarUsableSizeText"] = "${String.format("%.2f", sidecarUsabe)} $sidecarUsabeNotation"
        response["sidecarUsableSizeB"] = rawSidecarUsabe.toDouble()

        var sidecarTotalSizeB = rawSidecarUsabe.toDouble() + sidecarSize.toDouble()
        var sidecarSpaceTotalSizeProcessed = sidecarTotalSizeB.toDouble() / (kilo * kilo)
        sidecarSizeNotation = "MB"
        if (sidecarSpaceTotalSizeProcessed > kilo) {
            sidecarSpaceTotalSizeProcessed /= kilo
            sidecarSizeNotation = "GB"
        }
        if (sidecarSpaceTotalSizeProcessed > kilo) {
            sidecarSpaceTotalSizeProcessed /= kilo
            sidecarSizeNotation = "TB"
        }
        response["sidecarTotalSizeText"] = "${String.format("%.2f", sidecarSpaceTotalSizeProcessed)} $sidecarSizeNotation"
        response["sidecarTotalSizeB"] = sidecarTotalSizeB

        return response
    }
}