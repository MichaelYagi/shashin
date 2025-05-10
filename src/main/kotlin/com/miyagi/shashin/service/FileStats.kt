package com.miyagi.shashin.service

import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.repository.SettingsRepository
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.ImageProcessing
import com.miyagi.shashin.util.MetricsUtil
import jakarta.servlet.http.HttpSession
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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
import kotlin.io.path.Path

@Service
@CacheConfig(cacheNames=["filestats"])
class FileStats {
    private var logger: Logger = Logger.getLogger(FileStats::class.simpleName)

    @Cacheable("getFileStats")
    fun getFileStats(model: Model, settings: Settings?): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val metricsUtil = MetricsUtil()
        metricsUtil.start("filestats")

        // Files stats
        val kilo = 1000 //1024
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
        var sidecarSizeNotation = ""

        var rawSidecarUsabe: Double = 0.0
        var sidecarUsabe: Double = 0.0
        var rawSidecarTotal: Double = 0.0
        var sidecarSize: Double = 0.0
        if (settings != null && settings.getSidecarSizeK() != null) {
            sidecarSize = settings.getSidecarSizeK()!!.toDouble()
        }

        val diskUsageMap = FileUtils.sidecarDiskUsage(sidecarDir)
        if (diskUsageMap["rawSidecarUsabe"] != null && diskUsageMap["rawSidecarTotal"] != null) {
            rawSidecarUsabe = diskUsageMap["rawSidecarUsabe"]!!
            rawSidecarTotal = diskUsageMap["rawSidecarTotal"]!!
            sidecarUsabe = rawSidecarUsabe / (kilo * kilo).toDouble()
        }

        logger.log(Level.INFO, "Sidecar used: $sidecarSize")
        logger.log(Level.INFO, "Sidecar usable: $rawSidecarUsabe")
        logger.log(Level.INFO, "Sidecar Total: $rawSidecarTotal")

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

        var sidecarTotalSizeB = rawSidecarTotal.toDouble()
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

        var sidecarSizeProcessed = sidecarSize.toDouble() / (kilo * kilo)
        sidecarSizeNotation = "MB"
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

        metricsUtil.end()

        logger.log(Level.INFO, "Sidecar file stats elapsed time:" + metricsUtil.getTotalElapsedTime())

        return response
    }
}