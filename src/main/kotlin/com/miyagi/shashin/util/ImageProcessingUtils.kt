package com.miyagi.shashin.util

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.model.Metadata
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.io.FileSystemResource
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO


@ComponentScan
class ImageProcessingUtils(private var apiVersion: String?) {

    private var logger: Logger = Logger.getLogger(ImageProcessingUtils::class.simpleName)

    fun populateMetadata(file: File, sidecarDir: String, rootDir: String, _metadataObj: Metadata?): Metadata? {
        val metadataDirectory = sidecarDir.dropLast(1) + "/metadata"

        val rootDirFile = File(rootDir)
        var fileRootDir: String = file.parent.replace('\\', '/').lowercase().replace(rootDirFile.parent.replace('\\', '/').lowercase(), "")
        fileRootDir = fileRootDir.replace('\\', '/')

        val metadataFileStr = metadataDirectory + fileRootDir + "/" + file.name + ".yaml"
        val metadataObj = _metadataObj?.let { extractExifData(file, sidecarDir, rootDir, it) }

        val mdFile = FileUtils.createFile(metadataDirectory + fileRootDir, metadataFileStr, "YAML metadata")
        if (mdFile != null) {
            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build()
            val om = ObjectMapper(yamlFactory)
            om.writeValue(mdFile, metadataObj);
            return metadataObj
        }

        return null
    }

    private fun extractExifData(file: File, sidecarDir: String, rootDir: String, metadataObj: Metadata): Metadata {
        metadataObj.setPath(file.path)

        // Get file data
        val attr: BasicFileAttributes = Files.readAttributes(
            file.toPath(),
            BasicFileAttributes::class.java
        )

        val datePattern = "yyyy-MM-dd HH:mm:ss"
        val sourceFormatMS = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val sourceFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val destFormat = SimpleDateFormat(datePattern)
        var date: Date? = null

        val creationTime = attr.creationTime().toString()
        date = if (creationTime.contains(".")) {
            sourceFormatMS.parse(creationTime)
        } else {
            sourceFormat.parse(creationTime)
        }
        metadataObj.setCreatedAt(destFormat.format(date))

        val modifiedTime = attr.lastModifiedTime().toString()
        date = if (modifiedTime.contains(".")) {
            sourceFormatMS.parse(modifiedTime)
        } else {
            sourceFormat.parse(modifiedTime)
        }
        metadataObj.setModifiedAt(destFormat.format(date))

        val accessTime = attr.lastAccessTime().toString()
        date = if (accessTime.contains(".")) {
            sourceFormatMS.parse(accessTime)
        } else {
            sourceFormat.parse(accessTime)
        }
        metadataObj.setLastAccessedAt(destFormat.format(date))
        val exifMap = hashMapOf<String,String>()

        // Get image data
        val metadata = ImageMetadataReader.readMetadata(file)
        for (directory in metadata.directories) {
            var cameraMake: String? = null
            var cameraModel: String? = null
            var lensMake: String? = null
            var lensModel: String? = null

            for (tag in directory.tags) {
                val tagName = tag.tagName.replace(" ", "").replace("/", "")
                if ("unknowntag" !in tagName.lowercase()) {
                    exifMap[tagName] = tag.description
                }
//                println(tag.tagName)
//                println(tag.description)
//                println()

                // TODO: Get this info into exif file


                when (tag.tagName) {
                    "Date/Time", "Creation Time" -> {
                        val takenFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
                        date = null

                        try {
                            date = takenFormat.parse(tag.description)
                        } catch(e: Exception) {
                            try {
                                // Sun Jul 25 14:34:09 PDT 2021
                                val sourceFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
                                date = sourceFormat.parse(tag.description)
                            } catch(e: Exception) {
                                try {
                                    // Sun Jul 25 14:34:09 -07:00 2021
                                    val sourceFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy")
                                    date = sourceFormat.parse(tag.description)
                                } catch(e: Exception) {
                                    // Do nothing
                                }
                            }
                        }

                        metadataObj.setTakenAt(destFormat.format(date))
                        metadataObj.setCreatedAt(destFormat.format(date))

                        val dateArray = destFormat.format(date).toString().split(" ")
                        val takenDateArray = dateArray[0].split("-")
                        metadataObj.setYear(takenDateArray[0].toInt())
                        metadataObj.setMonth(takenDateArray[1].toInt())
                        metadataObj.setDay(takenDateArray[2].toInt())
                    }
                    "Modification Time" -> {
                        val modificationFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
                        date = null

                        try {
                            date = modificationFormat.parse(tag.description)
                        } catch(e: Exception) {
                            try {
                                // Sun Jul 25 14:34:09 PDT 2021
                                val sourceFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
                                date = sourceFormat.parse(tag.description)
                            } catch(e: Exception) {
                                try {
                                    // Sun Jul 25 14:34:09 -07:00 2021
                                    val sourceFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy")
                                    date = sourceFormat.parse(tag.description)
                                } catch(e: Exception) {
                                    // Do nothing
                                }
                            }
                        }
                        metadataObj.setModifiedAt(destFormat.format(date))
                    }
                    "File Modified Date" -> {
                        val dateArray = (tag.description).split(" ")
                        val timeZone = dateArray[dateArray.count()-2]
                        metadataObj.setTimeZone(timeZone)
                    }
                    "Detected MIME Type" -> {
                        metadataObj.setType(tag.description)
                    }
                    "File Name" -> {
                        metadataObj.setFileName(tag.description)
                    }
                    "GPS Latitude", "Latitude" -> {
                        val latitudeValue = tag.description
                        var latDecimal = latitudeValue

                        val numeric = latitudeValue.matches("-?\\d+(\\.\\d+)?".toRegex())
                        if (!numeric) {
                            val latArray = tag.description.split(" ")
                            val latDegree = latArray[0].dropLast(1).toDouble()
                            val latMinute = latArray[1].dropLast(1).toDouble()
                            val latSeconds = latArray[2].dropLast(1).toDouble()
                            val latTotalSeconds = (((latMinute * 60) + latSeconds) / 3600)
                            latDecimal = latDegree.toString().dropLast(1) + latTotalSeconds.toString().drop(2)
                        }
                        metadataObj.setLat(latDecimal)
                    }
                    "GPS Longitude", "Longitude" -> {
                        val longitudeValue = tag.description
                        var lngDecimal = longitudeValue

                        // Check if decimal
                        val numeric = longitudeValue.matches("-?\\d+(\\.\\d+)?".toRegex())
                        if (!numeric) {
                            val lngArray = tag.description.split(" ")
                            val lngDegree = lngArray[0].dropLast(1).toDouble()
                            val lngMinute = lngArray[1].dropLast(1).toDouble()
                            val lngSeconds = lngArray[2].dropLast(1).toDouble()
                            val lngTotalSeconds = (((lngMinute * 60) + lngSeconds) / 3600)
                            lngDecimal = lngDegree.toString().dropLast(1) + lngTotalSeconds.toString().drop(2)
                        }
                        metadataObj.setLng(lngDecimal)
                    }
                    "ISO Speed Ratings" -> {
                        metadataObj.setISO(tag.description.toInt())
                    }
                    "Exposure Time" -> {
                        val exposureArray = tag.description.split(" ")
                        var fraction = exposureArray[0]
                        if (fraction.contains(".")) {
                            val exposureTime = exposureArray[0].toDouble()
                            fraction = TextUtils.convertDecimalToFraction(exposureTime)
                        }
                        metadataObj.setExposure(fraction)
                    }
                    "F-Number" -> {
                        metadataObj.setFNumber(tag.description.drop(2).toDouble())
                    }
                    "Focal Length" -> {
                        val flArray = tag.description.split(" ")
                        metadataObj.setFocalLength(flArray[0].toDouble())
                    }
                    "Make" -> {
                        cameraMake = tag.description
                    }
                    "Model" -> {
                        cameraModel = tag.description
                    }
                    "Lens Make" -> {
                        lensMake = tag.description
                    }
                    "Lens Model" -> {
                        lensModel = tag.description
                    }
                    "Quality" -> {
                        metadataObj.setQuality(tag.description.trim())
                    }
                }

                if (!cameraMake.isNullOrBlank() && !cameraModel.isNullOrBlank()) {
                    val camera = "$cameraMake $cameraModel"
                    metadataObj.setCamera(camera.trim())
                }
                if (!lensMake.isNullOrBlank() && !lensModel.isNullOrBlank()) {
                    val lens = "$lensMake $lensModel"
                    metadataObj.setLens(lens.trim())
                }
            }
        }

        saveExifdata(exifMap, sidecarDir, rootDir, file.path)

        metadataObj.setId(
            TextUtils.generateUUID(
                metadataObj.getTakenAt(),
                metadataObj.getCreatedAt(),
                metadataObj.getType(),
                metadataObj.getFNumber(),
                metadataObj.getISO(),
                metadataObj.getExposure(),
                metadataObj.getLat(),
                metadataObj.getLng()
            ).toString()
        )

        return metadataObj
    }

    fun createThumbnails(file: File, sidecarDir: String, rootDir: String, metadataObj: Metadata?): Metadata? {
        val thumbnailDirectory = sidecarDir.dropLast(1) + "/thumbnails"

        // Map path to sidecar file
        val rootDirFile = File(rootDir)

        var fileRootDir: String = file.parent.replace('\\', '/').lowercase().replace(rootDirFile.parent.replace('\\', '/').lowercase(), "")
        fileRootDir = fileRootDir.replace('\\', '/')

        val supportedImageFormats = FileUtils.allowableImageFiles()
        val supportedVideoFormats = FileUtils.allowableVideoFiles()

        var img: BufferedImage? = null
        if (supportedImageFormats.contains(file.extension.lowercase())) {
            img = ImageIO.read(file)
        } else if (supportedVideoFormats.contains(file.extension.lowercase())) {
            // Grab screen shot
            img = grabScreenshot(file)
            metadataObj?.setVideoUrl("/api/$apiVersion/original/video$fileRootDir/" + file.name)
        }

        if (img != null) {
            // Scale to different sizes and save
            var thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_209.jpg"
            var tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
            if (tnFile != null) {
                val scaled209: BufferedImage = scaleImageByHeight(img, 209)
                ImageIO.write(scaled209, "jpg", tnFile)
                metadataObj?.setThumbnailPathSmall(tnFile.path)
                metadataObj?.setThumbnailUrlSmall("/api/$apiVersion/thumbnails$fileRootDir/" + tnFile.name)
            }

            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_original.jpg"
            tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
            if (tnFile != null) {
                val scaled: BufferedImage = scaleImageByRatio(img, 1.0)
                ImageIO.write(scaled, "jpg", tnFile)
                metadataObj?.setThumbnailPathOriginal(tnFile.path)
                metadataObj?.setThumbnailUrlOriginal("/api/$apiVersion/thumbnails$fileRootDir/" + tnFile.name)
            }
        } else {
            logger.log(Level.WARNING, "File not supported: " + file.name)
        }

        return metadataObj
    }

    private fun grabScreenshot(file: File): BufferedImage? {
        val frameGrabber = FFmpegFrameGrabber(file.path)
        frameGrabber.start()

        val aa = Java2DFrameConverter()

        try {
            var f = frameGrabber.grabKeyFrame()
            var bi = aa.convert(f)

            val limit = 1000
            var count = 0
            while (bi != null) {
                if (limit > count) {
                    break
                }
                f = frameGrabber.grabKeyFrame()
                bi = aa.convert(f)
                count++
            }

            val rotationStr = frameGrabber.getVideoMetadata("rotate")
            if (!rotationStr.isNullOrBlank()) {
                val rotation = rotationStr.toDouble()
                if (rotation > 0) {
                    bi = rotateImage(bi, rotation)
                }
            }

            frameGrabber.stop()

            return bi
        } catch (e: IOException) {
            logger.log(Level.WARNING, "Could not convert video " + file.name + ": " + e.message)
            return null
        }
    }

    private fun rotateImage(buffImage: BufferedImage, angle: Double): BufferedImage? {
        val radian = Math.toRadians(angle)
        val sin = Math.abs(Math.sin(radian))
        val cos = Math.abs(Math.cos(radian))
        val width = buffImage.width
        val height = buffImage.height
        val nWidth = Math.floor(width.toDouble() * cos + height.toDouble() * sin).toInt()
        val nHeight = Math.floor(height.toDouble() * cos + width.toDouble() * sin).toInt()
        val rotatedImage = BufferedImage(
            nWidth, nHeight, BufferedImage.TYPE_INT_ARGB
        )
        val graphics = rotatedImage.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
        graphics.translate((nWidth - width) / 2, (nHeight - height) / 2)
        // rotation around the center point
        graphics.rotate(radian, (width / 2).toDouble(), (height / 2).toDouble())
        graphics.drawImage(buffImage, 0, 0, null)
        graphics.dispose()
        return rotatedImage
    }

    private fun scaleImageByRatio(source: BufferedImage, ratio: Double): BufferedImage {
        val w = (source.width * ratio).toInt()
        val h = (source.height * ratio).toInt()
        return scaleImage(source, w, h)
    }

    private fun scaleImageByWidth(source: BufferedImage, w: Int): BufferedImage {
        val wScale: Float = (w.toFloat() / source.width.toFloat())
        val h = source.height.toFloat() * wScale //3705*(50/6000)
        return scaleImage(source, w, h.toInt())
    }

    private fun scaleImageByHeight(source: BufferedImage, h: Int): BufferedImage {
        val hScale: Float = (h.toFloat() / source.height.toFloat())
        val w = source.width.toFloat() * hScale
        return scaleImage(source, w.toInt(), h)
    }

    private fun scaleImage(source: BufferedImage, w: Int, h: Int): BufferedImage {
        val bi = getCompatibleImage(w, h)
        val g2d = bi.createGraphics()
        val xScale = w.toDouble() / source.width
        val yScale = h.toDouble() / source.height
        val at = AffineTransform.getScaleInstance(xScale, yScale)
        g2d.drawRenderedImage(source, at)
        g2d.dispose()
        return bi
    }

    private fun getCompatibleImage(w: Int, h: Int): BufferedImage {
        return BufferedImage(
            w, h,
            BufferedImage.TYPE_INT_RGB
        )
    }

   private fun saveExifdata(exifMap: HashMap<String, String>, _sidecarDir: String, rootDir: String, path: String) {
        if (exifMap.isNotEmpty()) {
            // Update Exif file
            val metadataDirectory = _sidecarDir.dropLast(1) + "/metadata"
            val photoFile = File(path)
            val photoFileParent = photoFile.parent.replace('\\', '/')
            val rootDirFile = File(rootDir)
            var fileRootDir: String =
                photoFileParent.replace('\\', '/').lowercase().replace(rootDirFile.parent.replace('\\', '/').lowercase(), "")
            fileRootDir = fileRootDir.replace('\\', '/')
            val exifFile = FileUtils.createFile("$metadataDirectory/$fileRootDir", "$metadataDirectory/$fileRootDir/" + photoFile.name + ".exif.yaml", "Exif")
            if (exifFile != null) {
                val yamlFactory: YAMLFactory = YAMLFactory.builder()
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                    .disable(YAMLGenerator.Feature.SPLIT_LINES)
                    .build()
                val om = ObjectMapper(yamlFactory)
                om.writeValue(exifFile, exifMap)
            }
        }
    }

    fun saveMetadata(metadataObj: Metadata?, _sidecarDir: String, rootDir: String) {
        if (metadataObj != null) {
            // Update MD file
            val rootPath = FileSystemResource("").file.absolutePath
            val sidecarDir = rootPath + _sidecarDir
            val metadataDirectory = sidecarDir.dropLast(1) + "/metadata"
            val rootDirFile = File(rootDir)
            val photoFile = File(metadataObj.getPath())
            var fileRootDir: String =
                photoFile.parent.replace('\\', '/').lowercase().replace(rootDirFile.parent.replace('\\', '/').lowercase(), "")
            fileRootDir = fileRootDir.replace('\\', '/')
            val metadataFileStr = metadataDirectory + fileRootDir + "/" + photoFile.name + ".yaml"
            val mdFile = File(metadataFileStr)
            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build()
            val om = ObjectMapper(yamlFactory)
            om.writeValue(mdFile, metadataObj);
        }
    }
}