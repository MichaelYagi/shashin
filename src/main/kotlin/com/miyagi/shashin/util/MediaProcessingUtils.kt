package com.miyagi.shashin.util

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import net.iakovlev.timeshape.TimeZoneEngine
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.io.FileSystemResource
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.lang.Double.parseDouble
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

@ComponentScan
class MediaProcessingUtils(private var apiVersion: String?, private var geocodeUrl: String?) {

    private var logger: Logger = Logger.getLogger(MediaProcessingUtils::class.simpleName)

    fun populateMetadata(file: File, sidecarDir: String, _metadataObj: Metadata?): Metadata? {
        return _metadataObj?.let { getAndSetMetadataExifData(file, sidecarDir, it) }
    }

    private fun getAndSetMetadataExifData(file: File, sidecarDir: String, metadataObj: Metadata): Metadata {
        metadataObj.setPath(file.path)
        metadataObj.setFileName(file.name)
        metadataObj.setTitle(file.name)

        // Get file data
        val attr: BasicFileAttributes = Files.readAttributes(
            file.toPath(),
            BasicFileAttributes::class.java
        )

        val datePattern = "yyyy-MM-dd HH:mm:ss"
        val sourceFormatMS = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        val sourceFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val destFormat = SimpleDateFormat(datePattern, Locale.ENGLISH)
        var date: Date?

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

        if (metadataObj.getCreatedAt() != null || metadataObj.getModifiedAt() != null) {
            if (metadataObj.getModifiedAt() != null) {
                metadataObj.setTakenAt(metadataObj.getModifiedAt())
            }
            if (metadataObj.getCreatedAt() != null) {
                metadataObj.setTakenAt(metadataObj.getCreatedAt())
            }

            val dateArray = metadataObj.getTakenAt().toString().split(" ")
            val takenDateArray = dateArray[0].split("-")

            metadataObj.setYear(takenDateArray[0].toInt())
            metadataObj.setMonth(takenDateArray[1].toInt())
            metadataObj.setDay(takenDateArray[2].toInt())
            metadataObj.setTime(dateArray[1])
        }

        val accessTime = attr.lastAccessTime().toString()
        date = if (accessTime.contains(".")) {
            sourceFormatMS.parse(accessTime)
        } else {
            sourceFormat.parse(accessTime)
        }
        metadataObj.setLastAccessedAt(destFormat.format(date))
        val exifMap = hashMapOf<String, String>()

//        println("=================")

        // Get image data
        try {
            val metadata = ImageMetadataReader.readMetadata(file)
            var cameraMake: String? = null
            var cameraModel: String? = null
            var lensMake: String? = null
            var lensModel: String? = null
            var lat: String? = null
            var lng: String? = null
            var rotation = 0
            var originalPixelWidth: Int? = null
            var originalPixelHeight: Int? = null
            var jpegImageWidth = false
            var jpegImageHeight = false
            var mp4VideoCreationTime = false
            var fileModificationTime = false

            for (directory in metadata.directories) {
                for (tag in directory.tags) {
                    if (tag.description != null) {
                        val tagName = tag.tagName.replace(" ", "").replace("/", "")
                        val directoryName = directory.name.replace(" ", "").replace("/", "")
                        if ("unknowntag" !in tagName.lowercase()) {
                            exifMap["$directoryName-$tagName"] = tag.description
                        }
//                println(directory.name)
//                println(directory.tagCount)
//                println(file.path)
//                println(tag.tagName)
//                println(tag.description)
//                println()

                        when (tag.tagName) {
                            "Orientation" -> {
                                if (tag.description.contains("Rotate") && ((!jpegImageHeight && !jpegImageWidth) || directory.name == "Exif IFD0")) {
                                    val digit = tag.description.filter { it.isDigit() }
                                    var numeric = true

                                    try {
                                        parseDouble(digit)
                                    } catch (e: NumberFormatException) {
                                        numeric = false
                                    }
                                    if (numeric) {
                                        rotation = digit.toInt()
                                    }
                                }
                            }
                            "Date/Time", "Creation Time", "Date/Time Original" -> {
                                // Sometimes created time is incorrect for mp4 files
                                if (mp4VideoCreationTime) {
                                    continue
                                }

                                if (directory.name == "MP4" && tag.tagName == "Creation Time") {
                                    mp4VideoCreationTime = true
                                }

                                val takenFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ENGLISH)
                                date = null

                                try {
                                    date = takenFormat.parse(tag.description)
                                } catch (e: Exception) {
                                    try {
                                        // Sun Jul 25 14:34:09 PDT 2021
                                        val sourceDateFormat =
                                            SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                                        date = sourceDateFormat.parse(tag.description)

                                    } catch (e: Exception) {
                                        try {
                                            // Sun Jul 25 14:34:09 -07:00 2021
                                            val sourceDateFormat =
                                                SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                            date = sourceDateFormat.parse(tag.description)
                                        } catch (e: Exception) {
                                            try {
                                                // Sun. Jul. 25 14:34:09 -07:00 2021
                                                val sourceDateFormat =
                                                    SimpleDateFormat("EEE. MMM. dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                                date = sourceDateFormat.parse(tag.description)
                                            } catch (e: Exception) {
                                                // Do nothing
                                            }
                                        }
                                    }
                                }

                                if (date != null) {
                                    metadataObj.setTakenAt(destFormat.format(date))
                                    metadataObj.setCreatedAt(destFormat.format(date))

                                    val dateArray = destFormat.format(date).toString().split(" ")
                                    val takenDateArray = dateArray[0].split("-")

                                    metadataObj.setYear(takenDateArray[0].toInt())
                                    metadataObj.setMonth(takenDateArray[1].toInt())
                                    metadataObj.setDay(takenDateArray[2].toInt())
                                    metadataObj.setTime(dateArray[1])
                                }
                            }
                            "Modification Time", "File Modified Date" -> {
                                // Sometimes modified time is incorrect for mp4 files
                                if (fileModificationTime) {
                                    continue
                                }

                                if (tag.tagName == "File Modified Date") {
                                    fileModificationTime = true
                                }

                                val modificationFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ENGLISH)
                                date = null

                                try {
                                    date = modificationFormat.parse(tag.description)
                                } catch (e: Exception) {
                                    try {
                                        // Sun Jul 25 14:34:09 PDT 2021
                                        val sourceDateFormat =
                                            SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                                        date = sourceDateFormat.parse(tag.description)
                                    } catch (e: Exception) {
                                        try {
                                            // Sun Jul 25 14:34:09 -07:00 2021
                                            val sourceDateFormat =
                                                SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                            date = sourceDateFormat.parse(tag.description)
                                        } catch (e: Exception) {
                                            try {
                                                // Sun. Jul. 25 14:34:09 -07:00 2021
                                                val sourceDateFormat =
                                                    SimpleDateFormat("EEE. MMM. dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                                date = sourceDateFormat.parse(tag.description)
                                            } catch (e: Exception) {
                                                // Do nothing
                                            }
                                        }
                                    }
                                }

                                if (date != null) {
                                    metadataObj.setModifiedAt(destFormat.format(date))
                                }
                            }
                            "Detected MIME Type" -> {
                                metadataObj.setType(tag.description)
                            }
//                    "File Name" -> {
//                        metadataObj.setFileName(tag.description)
//                        metadataObj.setTitle(tag.description)
//                    }
                            // XXX pixels
                            "Exif Image Width", "Width", "Image Width" -> {
                                val widthValue = tag.description.filter { it.isDigit() }

                                if (jpegImageWidth) {
                                    continue
                                }

                                if (directory.name == "JPEG" && tag.tagName == "Image Width") {
                                    jpegImageWidth = true
                                }

                                if ((originalPixelWidth == null && widthValue != "") || (originalPixelWidth != null && widthValue.toInt() > originalPixelWidth)) {
                                    originalPixelWidth = widthValue.toInt()
                                }
                            }
                            "Exif Image Height", "Height", "Image Height" -> {
                                val heightValue = tag.description.filter { it.isDigit() }

                                if (jpegImageHeight) {
                                    continue
                                }

                                if (directory.name == "JPEG" && tag.tagName == "Image Height") {
                                    jpegImageHeight = true
                                }

                                if ((originalPixelHeight == null && heightValue != "")  || (originalPixelHeight != null && heightValue.toInt() > originalPixelHeight)) {
                                    originalPixelHeight = heightValue.toInt()
                                }
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
                                lat = latDecimal
                                if (latDecimal != "0.0") {
                                    metadataObj.setLat(latDecimal)
                                } else {
                                    lat = null
                                }
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
                                lng = lngDecimal
                                if (lngDecimal != "0.0") {
                                    metadataObj.setLng(lngDecimal)
                                } else {
                                    lng = null
                                }
                            }
                            "ISO Speed Ratings" -> {
                                metadataObj.setIso(tag.description.toInt())
                            }
                            "Compression Type" -> {
                                metadataObj.setCompressionType(tag.description)
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
                                val regex = "\\d+(\\.\\d+)?".toRegex()
                                var matchValue = ""
                                try {
                                    val match = regex.find(tag.description)!!
                                    matchValue=match.value
                                } catch (e: Exception) {}

                                if (matchValue.isNotBlank()) {
                                    metadataObj.setFstopNumber(matchValue.toDouble())
                                }
                            }
                            "Focal Length" -> {
                                val flengthValue = tag.description.filter { it.isDigit() }
                                if (flengthValue.isNotBlank()) {
                                    metadataObj.setFocalLength(flengthValue.toDouble())
                                }
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
                            "Duration in Seconds" -> {
                                val durationParts = tag.description.split(":")
                                val hours = durationParts[0].toInt()
                                val minutes = durationParts[1].toInt()
                                val seconds = durationParts[2]

                                val hoursToMinutes = hours * 60
                                val totalMinutes = minutes + hoursToMinutes
                                val duration = "$totalMinutes:$seconds"

                                metadataObj.setDuration(duration)
                            }
                        }
                    } else {
                        logger.log(
                            Level.WARNING,
                            "Tag description not available for " + file.name + " for tag " + tag.tagName
                        )
                    }
                }
            }

            if (metadataObj.getCamera().isNullOrBlank() && (!cameraMake.isNullOrBlank() || !cameraModel.isNullOrBlank())) {
                var camera = ""
                if (!cameraMake.isNullOrBlank()) {
                    camera += cameraMake
                }
                if (!cameraModel.isNullOrBlank()) {
                    camera += if (camera.isNotBlank()) " $cameraModel" else cameraModel
                }
                if (camera.isNotBlank()) {
                    metadataObj.setCamera(camera.trim())
                }
            }
            if (metadataObj.getLens().isNullOrBlank() && (!lensMake.isNullOrBlank() || !lensModel.isNullOrBlank())) {
                var lens = ""
                if (!lensMake.isNullOrBlank()) {
                    lens += lensMake
                }
                if (!lensModel.isNullOrBlank()) {
                    lens += if (lens.isNotBlank()) " $lensModel" else lensModel
                }
                if (lens.isNotBlank()) {
                    metadataObj.setLens(lens.trim())
                }
            }

            if (!lat.isNullOrBlank() && !lng.isNullOrBlank()) {
                val geoDataJson = TextUtils.getGeoData(geocodeUrl!!, lat, lng)

                val buildPlace = TextUtils.getPlaceNameFromJson(geoDataJson)
                if (buildPlace.isNotBlank()) {
                    metadataObj.setPlaceName(buildPlace)

                    val engine = TimeZoneEngine.initialize()
                    val maybeZoneId: Optional<ZoneId> =
                        engine.query(lat.toString().toDouble(), lng.toString().toDouble())
                    val zone = ZoneId.of(maybeZoneId.get().id)
                    val dt = LocalDateTime.now()
                    val zdt: ZonedDateTime = dt.atZone(zone)
                    val offset = zdt.offset
                    metadataObj.setTimeZone(offset.toString())
                }
            }

            if (originalPixelHeight != null && originalPixelWidth != null) {
                if (rotation == 90 || rotation == 270) {
                    metadataObj.setOriginalImageWidth(originalPixelHeight)
                    metadataObj.setOriginalImageHeight(originalPixelWidth)
                } else {
                    metadataObj.setOriginalImageWidth(originalPixelWidth)
                    metadataObj.setOriginalImageHeight(originalPixelHeight)
                }
            }
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not read metadata for " + file.name + ": " + e.message
            )
        }

        saveExifdata(exifMap, sidecarDir, file.path)

        metadataObj.setAddedAt(getCurrentTimestamp())

        metadataObj.setId(
            TextUtils.generateUUID(
                file.path,
                metadataObj.getCreatedAt(),
                metadataObj.getType(),
                metadataObj.getCamera(),
                metadataObj.getFstopNumber(),
                metadataObj.getIso(),
                metadataObj.getExposure()
            ).toString()
        )

        val supportedImageFormats = FileUtils.allowableImageFiles()
        val supportedVideoFormats = FileUtils.allowableVideoFiles()
//        val supportedAudioFormats = FileUtils.allowableAudioFiles()

        if (supportedImageFormats.contains(file.extension.lowercase())) {
            metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/image/${metadataObj.getId()}")
        } else if (supportedVideoFormats.contains(file.extension.lowercase())) {
            metadataObj.setVideoUrl("/api/$apiVersion/video/${metadataObj.getId()}")
        }
//        else if (supportedAudioFormats.contains(file.extension.lowercase())) {
//            metadataObj.setVideoUrl("/api/$apiVersion/audio/${metadataObj.getId()}")
//        }

        return metadataObj
    }

    fun createThumbnails(file: File, sidecarDir: String, metadataObj: Metadata?): Metadata? {
        var _metadataObj = metadataObj

        // Check rotation
        var rotation = 0
        try {
            val fileMetadata = ImageMetadataReader.readMetadata(file)
            var jpegImageWidth = false
            var jpegImageHeight = false
            for (directory in fileMetadata.directories) {
                for (tag in directory.tags) {
                    when (tag.tagName) {
                        "Orientation" -> {
                            if (tag.description.contains("Rotate") && ((!jpegImageHeight && !jpegImageWidth) || directory.name == "Exif IFD0")) {
                                val digit = tag.description.filter { it.isDigit() }
                                var numeric = true

                                try {
                                    parseDouble(digit)
                                } catch (e: NumberFormatException) {
                                    numeric = false
                                }
                                if (numeric) {
                                    rotation = digit.toInt()
                                }
                            }
                        }
                        "Exif Image Height", "Height", "Image Height" -> {
                            if (jpegImageHeight) {
                                continue
                            }

                            if (directory.name == "JPEG" && tag.tagName == "Image Height") {
                                jpegImageHeight = true
                            }
                        }
                        "Exif Image Width", "Width", "Image Width" -> {
                            if (jpegImageWidth) {
                                continue
                            }

                            if (directory.name == "JPEG" && tag.tagName == "Image Width") {
                                jpegImageWidth = true
                            }
                        }
                    }
                }
            }
//        println("rotation:$rotation")
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not get rotation when reading metadata for " + file.name + ": " + e.message
            )
        }

        val thumbnailDirectory = sidecarDir.dropLast(1) + "/thumbnails"

        // Map path to sidecar file
        val fileRootDir: String = FileUtils.getRootDir(file)
        val supportedImageFormats = FileUtils.allowableImageFiles()
        val supportedVideoFormats = FileUtils.allowableVideoFiles()
        var extension = "jpg"

        var img: BufferedImage? = null
        if (FileUtils.isRaw(file.extension.lowercase())) {
            try {
                img = ImageIO.read(file)
                if (rotation > 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedImageFormats.contains(file.extension.lowercase())) {
            extension = file.extension.lowercase()
            try {
                img = ImageIO.read(file)
                if (rotation > 0) {
                    img = rotateImage(img, rotation.toDouble())
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "Could not read file: " + file.path)
            }
        } else if (supportedVideoFormats.contains(file.extension.lowercase())) {
            // Grab screen shot
            img = grabScreenshot(file)
            if (img != null && _metadataObj?.getOriginalImageWidth() == null && _metadataObj?.getOriginalImageHeight() == null) {
                _metadataObj?.setOriginalImageWidth(img.width)
                _metadataObj?.setOriginalImageHeight(img.height)
            }
//            _metadataObj?.setVideoUrl("/api/$apiVersion/original/video$fileRootDir/" + file.name)
        }

        // Create thumbnails
        if (img != null) {
            _metadataObj?.setFolder(fileRootDir)

            // Raw file to image conversion
            var thumbnailFileStr: String
            var tnFile: File?
            if (FileUtils.isRaw(file.extension.lowercase())) {
                thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_original." + extension
                tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
                if (tnFile != null) {
                    ImageIO.write(img, extension, tnFile)
                }
                _metadataObj?.setThumbnailUrlOriginal("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_original." + extension)
            }

            // Gallery thumbnails
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_209." + extension
            tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")

            var scaled209: BufferedImage = if (file.extension.lowercase() == "gif") {
                Thumbnails.of(img)
                    .height(209)
                    .imageType(BufferedImage.TYPE_INT_ARGB)
                    .outputQuality(1.0)
                    .asBufferedImage()
            } else {
                Thumbnails.of(img)
                    .height(209)
                    .outputQuality(1.0)
                    .asBufferedImage()
            }

            if (scaled209.width > scaled209.height * 2) {
                scaled209 = if (file.extension.lowercase() == "gif") {
                    Thumbnails.of(img)
                        .height(209)
                        .imageType(BufferedImage.TYPE_INT_ARGB)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                } else {
                    Thumbnails.of(scaled209)
                        .height(209)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                }
            }
            if (tnFile != null) {
                ImageIO.write(scaled209, extension, tnFile)
            }
            _metadataObj?.setThumbnailSmallHeight(scaled209.height)
            _metadataObj?.setThumbnailSmallWidth(scaled209.width)
            _metadataObj?.setThumbnailPathSmall(thumbnailFileStr)
            _metadataObj?.setThumbnailUrlSmall("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_209." + extension)

            // Square image thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_centered." + extension
            tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
            if (tnFile != null) {
                val square: BufferedImage
                if (img.height > img.width) {
//                    scaled = scaleImageByWidth(img, 209)
                    val temp = Thumbnails.of(img)
                        .width(209)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    square = Thumbnails.of(temp)
                        .width(209)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                } else {
//                    scaled = scaleImageByHeight(img, 209)
                    val temp = Thumbnails.of(img)
                        .height(209)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    square = Thumbnails.of(temp)
                        .height(209)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 209, 209)
                        .asBufferedImage()
                }

//                val square: BufferedImage = getSquareThumbnail(scaled)

                ImageIO.write(square, extension, tnFile)
            }
            _metadataObj?.setThumbnailPathCentered(thumbnailFileStr)
            _metadataObj?.setThumbnailUrlCentered("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_centered." + extension)

            // Map marker thumbnail
            thumbnailFileStr = thumbnailDirectory + fileRootDir + "/" + file.name + "_mapmarker." + extension
            tnFile = FileUtils.createFile(thumbnailDirectory + fileRootDir, thumbnailFileStr, "Thumbnail")
            if (tnFile != null) {
                val mapMarker: BufferedImage
                if (img.height > img.width) {
                    val temp = Thumbnails.of(img)
                        .width(45)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    mapMarker = Thumbnails.of(temp)
                        .width(45)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 45, 45)
                        .asBufferedImage()
                } else {
                    val temp = Thumbnails.of(img)
                        .height(45)
                        .outputQuality(1.0)
                        .asBufferedImage()
                    mapMarker = Thumbnails.of(temp)
                        .height(45)
                        .outputQuality(1.0)
                        .sourceRegion(Positions.CENTER, 45, 45)
                        .asBufferedImage()
                }

//                    val scaled: BufferedImage
//                    if (img.height > img.width) {
//                        scaled = scaleImageByWidth(img, 45)
//                    } else {
//                        scaled = scaleImageByHeight(img, 45)
//                    }
//                    val mapMarker: BufferedImage = getSquareThumbnail(scaled)

                ImageIO.write(mapMarker, extension, tnFile)
            }
            _metadataObj?.setMapMarkerPath(thumbnailFileStr)
            _metadataObj?.setMapMarkerUrl("/api/$apiVersion/thumbnails$fileRootDir/" + file.name + "_mapmarker." + extension)
        } else {
            logger.log(Level.WARNING, "File not supported: " + file.name)
            _metadataObj = null
        }

        // Save serialized metadata obj
        val metadataDirectory = sidecarDir.dropLast(1) + "/metadata/"
        val metadataFileStr = metadataDirectory + fileRootDir + "/" + file.name + ".yaml"
        val mdFile = FileUtils.createFile(metadataDirectory + fileRootDir, metadataFileStr, "YAML metadata")
        if (mdFile != null) {
            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build()
            val om = ObjectMapper(yamlFactory)
            om.writeValue(mdFile, _metadataObj)
        }

        return _metadataObj
    }

    private fun grabScreenshot(file: File): BufferedImage? {
        try {
            val frameGrabber = FFmpegFrameGrabber(file.path)
//            frameGrabber.format = file.extension.lowercase()
            frameGrabber.start()

            val aa = Java2DFrameConverter()

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
            if (!rotationStr.isNullOrBlank() && bi != null) {
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

    private fun rotateImage(buffImage: BufferedImage, angle: Double): BufferedImage {
        val radian = Math.toRadians(angle)
        val sin = Math.abs(Math.sin(radian))
        val cos = Math.abs(Math.cos(radian))
        val width = buffImage.width
        val height = buffImage.height
        val nWidth = Math.floor(width.toDouble() * cos + height.toDouble() * sin).toInt()
        val nHeight = Math.floor(height.toDouble() * cos + width.toDouble() * sin).toInt()
        val rotatedImage = BufferedImage(
            nWidth, nHeight, BufferedImage.TYPE_INT_RGB
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val xScale = w.toDouble() / source.width
        val yScale = h.toDouble() / source.height
        val at = AffineTransform.getScaleInstance(xScale, yScale)
        g2d.drawRenderedImage(source, at)
        g2d.dispose()
        return bi
    }

    private fun getSquareThumbnail(source: BufferedImage): BufferedImage {
        // Get a square thumbnail
        val side = source.width.coerceAtMost(source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return source.getSubimage(x, y, side, side)
    }

    private fun getCompatibleImage(w: Int, h: Int): BufferedImage {
        return BufferedImage(
            w, h,
            BufferedImage.TYPE_INT_RGB
        )
    }

    private fun saveExifdata(exifMap: HashMap<String, String>, _sidecarDir: String, path: String) {
        if (exifMap.isNotEmpty()) {
            // Update Exif file
            val metadataDirectory = _sidecarDir.dropLast(1) + "/metadata"
            val photoFile = File(path)
            val fileRootDir: String = FileUtils.getRootDir(photoFile)
            val exifFile = FileUtils.createFile(
                "$metadataDirectory/$fileRootDir",
                "$metadataDirectory/$fileRootDir/" + photoFile.name + ".exif.yaml",
                "Exif"
            )
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

    fun saveMetadata(metadataObj: Metadata?, _sidecarDir: String) {
        if (metadataObj != null) {
            // Update MD file
            val rootPath = FileSystemResource("").file.absolutePath
            val sidecarDir = rootPath + _sidecarDir
            val metadataDirectory = sidecarDir.dropLast(1) + "/metadata"
            val photoFile = File(metadataObj.getPath()!!)
            val fileRootDir = FileUtils.getRootDir(photoFile)
            val metadataFileStr = metadataDirectory + fileRootDir + "/" + photoFile.name + ".yaml"
            val mdFile = File(metadataFileStr)
            val yamlFactory: YAMLFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build()
            val om = ObjectMapper(yamlFactory)
            om.writeValue(mdFile, metadataObj)
        }
    }
}