package com.miyagi.shashin.service

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
    fun getFileStats(model: Model, session: HttpSession): MutableMap<String, Any?> {
        val response = mutableMapOf<String, Any?>()

        val metricsUtil = MetricsUtil()
        metricsUtil.start("filestats")

        // Files stats
        val kilo = 1000 //1024
        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = rootPath + model.getAttribute("relativeSidecarDir")
        var sidecarSizeNotation = ""

        var rawSidecarUsabe: Double
        var sidecarUsabe: Double
        var rawSidecarTotal: Double = 0.0
        var sidecarSize: Long = 0
        try {
            if (File(sidecarDir).exists()) {
                var dir = Paths.get(sidecarDir)
                dir = dir.toRealPath()
                val fs = Files.getFileStore(dir)
                sidecarUsabe = fs.usableSpace.toDouble() / (kilo * kilo).toDouble()
                rawSidecarUsabe = fs.usableSpace.toDouble()
                rawSidecarTotal = fs.totalSpace.toDouble()
                sidecarSize = org.apache.commons.io.FileUtils.sizeOfDirectory(File(sidecarDir))
            } else {
                var dir = Paths.get(rootPath)
                dir = dir.toRealPath()
                val fs = Files.getFileStore(dir)
                sidecarUsabe = fs.usableSpace.toDouble() / (kilo * kilo).toDouble()
                rawSidecarUsabe = fs.usableSpace.toDouble()
                rawSidecarTotal = fs.totalSpace.toDouble()
                sidecarSize = org.apache.commons.io.FileUtils.sizeOfDirectory(File(rootPath))
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

//        var sidecarSize = 0.toLong()
//        if (session.getAttribute("sidecarSize") == null) {
//            logger.log(Level.WARNING, "Setting sidecarSize session attribute")
//            try {
//                val files = if (Files.isSymbolicLink(Paths.get(sidecarDir))) {
//                    val path = Path(sidecarDir)
//                    val realPath = path.toRealPath()
//                    val directory = realPath.toFile()
//                    directory.walk().filter { it.isFile }.toList()
//                } else {
//                    val directory = File(sidecarDir)
//                    directory.walk().filter { it.isFile }.toList()
//                }
//
//                files.map { file ->
//                    sidecarSize += file.length()
//                }
//                session.setAttribute("sidecarSize", sidecarSize)
//            } catch (e: Exception) {
//                logger.log(Level.WARNING, "Error calculating sidecar size:" + e.message)
//            }
//        } else {
//            logger.log(Level.WARNING, "Using sidecarSize session attribute")
//            sidecarSize = session.getAttribute("sidecarSize").toString().toLong()
//        }

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

        logger.log(Level.WARNING, "Sidecar file stats elapsed time:" + metricsUtil.getTotalElapsedTime())

        return response
    }
}