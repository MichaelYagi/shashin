package com.miyagi.shashin

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.miyagi.shashin.model.Metadata
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.logging.Logger
import javax.imageio.ImageIO


class ImageProcessingUtils {

    companion object {
        private var logger: Logger = Logger.getLogger(ImageProcessingUtils::class.simpleName)

        fun createSidecarData(file: File, sidecarDir: String, rootDir: String, thumbnailFile: File?): Metadata {
            val metadataDirectory = sidecarDir.dropLast(1) + "/metadata"
            val exifDirectory = sidecarDir.dropLast(1) + "/exif"

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
            }

            return metadataObj
        }

        private fun populateMetadataObject(file: File, metadataObj: Metadata): Metadata {
            metadataObj.setPath(file.path)

            // Get file data
            val attr: BasicFileAttributes = Files.readAttributes(
                file.toPath(),
                BasicFileAttributes::class.java
            )
            metadataObj.setCreatedAt(attr.creationTime().toString())
            metadataObj.setModifiedAt(attr.lastModifiedTime().toString())
            metadataObj.setLastAccessedAt(attr.lastAccessTime().toString())

            // Get image data
            val metadata = ImageMetadataReader.readMetadata(file)
            for (directory in metadata.directories) {
                var year: String? = null
                var month: String? = null
                var day: String? = null
                var cameraMake: String? = null
                var cameraModel: String? = null
                var lensMake: String? = null
                var lensModel: String? = null
                for (tag in directory.tags) {
                    // TODO: Get this info into exif file
                    when (tag.tagName) {
                        "Date/Time" -> {
                            val dateArray = tag.description.split(" ")
                            val takenDateArray = dateArray[0].split(":")
                            metadataObj.setTakenAt(tag.description)
                            metadataObj.setYear(takenDateArray[0])
                            metadataObj.setMonth(takenDateArray[1])
                            metadataObj.setDay(takenDateArray[2])
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

            metadataObj.setUID(TextUtils.generateUUID(metadataObj.getPath(),metadataObj.getTakenAt(),metadataObj.getLat(),metadataObj.getLng()))

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