package com.miyagi.shashin.util

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.miyagi.shashin.model.Metadata
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger
import javax.imageio.ImageIO


class ImageProcessingUtils {

    companion object {
        private var logger: Logger = Logger.getLogger(ImageProcessingUtils::class.simpleName)

        fun createMetadata(file: File, sidecarDir: String, rootDir: String, thumbnailFile: File?): Metadata? {
            val metadataDirectory = sidecarDir.dropLast(1) + "/metadata"

            val rootDirFile = File(rootDir)
            var fileRootDir: String = (file.parent).lowercase().replace((rootDirFile.canonicalPath).lowercase(), "")
            fileRootDir = fileRootDir.replace('\\', '/')

            val metadataFileStr = metadataDirectory + fileRootDir + "/" + file.name + ".yaml"

            var metadataObj = Metadata()
            metadataObj = populateMetadataObject(file, metadataObj)
            if (thumbnailFile != null) {
                metadataObj.setThumbnailPath(thumbnailFile.path)
            }

            val mdFile = FileUtils.createFile(metadataDirectory + fileRootDir, metadataFileStr, "YAML metadata")
            if (mdFile != null) {
                val om = ObjectMapper(YAMLFactory())
                om.writeValue(mdFile, metadataObj);
                return metadataObj
            }

            return null
        }

        private fun populateMetadataObject(file: File, metadataObj: Metadata): Metadata {
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

            // Get image data
            val metadata = ImageMetadataReader.readMetadata(file)
            for (directory in metadata.directories) {
                var cameraMake: String? = null
                var cameraModel: String? = null
                var lensMake: String? = null
                var lensModel: String? = null
                for (tag in directory.tags) {
                    // TODO: Get this info into exif file
                    when (tag.tagName) {
                        "Date/Time" -> {
                            val takenFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
                            date = takenFormat.parse(tag.description)
                            metadataObj.setTakenAt(destFormat.format(date))

                            val dateArray = tag.description.split(" ")
                            val takenDateArray = dateArray[0].split(":")
                            metadataObj.setYear(takenDateArray[0].toInt())
                            metadataObj.setMonth(takenDateArray[1].toInt())
                            metadataObj.setDay(takenDateArray[2].toInt())
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
                        "GPS Latitude" -> {
                            val latArray = tag.description.split(" ")
                            val latDegree = latArray[0].dropLast(1).toDouble()
                            val latMinute = latArray[1].dropLast(1).toDouble()
                            val latSeconds = latArray[2].dropLast(1).toDouble()
                            val latTotalSeconds = (((latMinute*60) + latSeconds)/3600)
                            val latDecimal = latDegree.toString().dropLast(1) + latTotalSeconds.toString().drop(2)
                            metadataObj.setLat(latDecimal)
                        }
                        "GPS Longitude" -> {
                            val lngArray = tag.description.split(" ")
                            val lngDegree = lngArray[0].dropLast(1).toDouble()
                            val lngMinute = lngArray[1].dropLast(1).toDouble()
                            val lngSeconds = lngArray[2].dropLast(1).toDouble()
                            val lngTotalSeconds = (((lngMinute*60) + lngSeconds)/3600)
                            val lngDecimal = lngDegree.toString().dropLast(1) + lngTotalSeconds.toString().drop(2)
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
                            metadataObj.setFocalLength(flArray[0].toInt())
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

            metadataObj.setId(
                TextUtils.generateUUID(
                    metadataObj.getPath(),
                    metadataObj.getTakenAt(),
                    metadataObj.getLat(),
                    metadataObj.getLng()
                ).toString()
            )

            return metadataObj
        }

        fun createThumbnails(file: File, sidecarDir: String, rootDir: String): File? {
            val thumbnailDirectory = sidecarDir.dropLast(1) + "/thumbnails"

            // Scale to different sizes and save
            val img: BufferedImage = ImageIO.read(file)
            val scaled: BufferedImage = scaleImageByHeight(img, 224)

            // Map path to sidecar file
            val rootDirFile = File(rootDir)
            var fileRootDir: String = (file.parent).lowercase().replace((rootDirFile.canonicalPath).lowercase(), "")
            fileRootDir = fileRootDir.replace('\\', '/')
            val thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + ".jpg"

            val tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
            if (tnFile != null) {
                ImageIO.write(scaled, "jpg", tnFile)
            }

            return tnFile
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
    }
}