package com.miyagi.shashin.service

import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.MetricsUtil
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import java.io.File
import java.text.DecimalFormat
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.collections.set
import kotlin.math.log10
import kotlin.math.pow

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

    @Cacheable("getMetadataFileStats")
    fun getMetadataFileStats(metadata: com.miyagi.shashin.model.Metadata): MutableMap<String, Any?> {
        val metricsUtil = MetricsUtil()
        metricsUtil.start("metadatafilestats")
        val response = mutableMapOf<String, Any?>()

        response["fileSizeBytes"] = null
        response["fileSize"] = null
        response["sidecarSize"] = null

        if (metadata.getPath() != null) {
            val imageFile = File(metadata.getPath()!!)
            if (imageFile.exists()) {
                var fileSize = imageFile.length()
                response["fileSizeBytes"] = fileSize
                response["fileSize"] = formatFileSize(fileSize)
            }

            var thumbnailSmallFileSize = 0L
            var thumbnailCenteredFileSize = 0L
            var thumbnailMapMarkerFileSize = 0L
            var thumbnailLargeFileSize = 0L

            val basePath = metadata.getThumbnailPathSmall()?.substringBefore("_225.")
            val extension = metadata.getThumbnailPathSmall()?.substringAfterLast('.') // eg png

            // 225
            val thumbnailSmallPath = metadata.getThumbnailPathSmall()
            if (thumbnailSmallPath != null) {
                val thumbnailSmallFile = File(thumbnailSmallPath)
                if (thumbnailSmallFile.exists()) {
                    thumbnailSmallFileSize = thumbnailSmallFile.length()
                }
            }

            // centered
            val centeredFile = File(basePath+"_centered."+extension)
            if (centeredFile.exists()) {
                thumbnailCenteredFileSize = centeredFile.length()
            }

            // mapmarker
            val mapmarkerFile = File(basePath+"_mapmarker."+extension)
            if (mapmarkerFile.exists()) {
                thumbnailMapMarkerFileSize = mapmarkerFile.length()
            }

            // original
            val originalFile = File(basePath+"_original."+extension)
            if (originalFile.exists()) {
                thumbnailLargeFileSize = originalFile.length()
            }

            var totalMetadataSidecarSize = thumbnailSmallFileSize + thumbnailCenteredFileSize + thumbnailMapMarkerFileSize + thumbnailLargeFileSize
            response["sidecarSize"] = formatFileSize(totalMetadataSidecarSize)
        }

        metricsUtil.end()

        logger.log(Level.INFO, "getMetadataFileStats elapsed time:" + metricsUtil.getTotalElapsedTime())

        return response
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val size = bytes / 1024.0.pow(digitGroups.toDouble())

        val df = DecimalFormat("#,##0.00")
        return "${df.format(size)} ${units[digitGroups]}"
    }
}