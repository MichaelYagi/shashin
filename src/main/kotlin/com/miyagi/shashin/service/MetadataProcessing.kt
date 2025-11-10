package com.miyagi.shashin.service

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


class MetadataProcessing() {

    private var logger: Logger = Logger.getLogger(MetadataProcessing::class.simpleName)
    private lateinit var apiVersion: String
    private lateinit var file: File
    private lateinit var sidecarDir: String
    private lateinit var metadataObj: Metadata
    private lateinit var geocodeUrl: String

    constructor(apiVersion: String, file: File, sidecarDir: String, metadataObj: Metadata, geocodeUrl: String) : this() {
        this.apiVersion = apiVersion
        this.file = file
        this.sidecarDir = sidecarDir
        this.metadataObj = metadataObj
        this.geocodeUrl = geocodeUrl
    }

    fun populateMetadata(): Metadata {
        this.metadataObj.setPath(file.path)
        this.metadataObj.setFileName(file.name)
        this.metadataObj.setTitle(file.name)

        // Get file data
        val attr: BasicFileAttributes = Files.readAttributes(
            file.toPath(),
            BasicFileAttributes::class.java
        )

        val datePattern = TextUtils.Companion.getCommonDateFormat()
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
        this.metadataObj.setCreatedAt(destFormat.format(date))

        val modifiedTime = attr.lastModifiedTime().toString()
        date = if (modifiedTime.contains(".")) {
            sourceFormatMS.parse(modifiedTime)
        } else {
            sourceFormat.parse(modifiedTime)
        }
        this.metadataObj.setModifiedAt(destFormat.format(date))

        if (this.metadataObj.getCreatedAt() != null || this.metadataObj.getModifiedAt() != null) {
            if (this.metadataObj.getModifiedAt() != null) {
                this.metadataObj.setTakenAt(this.metadataObj.getModifiedAt())
            }
            if (this.metadataObj.getCreatedAt() != null) {
                this.metadataObj.setTakenAt(this.metadataObj.getCreatedAt())
            }

            val dateArray = this.metadataObj.getTakenAt().toString().split(" ")
            val takenDateArray = dateArray[0].split("-")

            this.metadataObj.setYear(takenDateArray[0].toInt())
            this.metadataObj.setMonth(takenDateArray[1].toInt())
            this.metadataObj.setDay(takenDateArray[2].toInt())
            this.metadataObj.setTime(dateArray[1])
        }

        this.metadataObj.setLastAccessedAt(getCurrentTimestamp())
        val exifMap = hashMapOf<String, HashMap<String, String>>()

//        println("=================")

        // Get image data
        try {
            val metadata = ImageMetadataReader.readMetadata(file)
            var cameraMake: String? = null
            var cameraModel: String? = null
            var lensMake: String? = null
            var lensModel: String? = null
//            var lat: String? = null
//            var lng: String? = null
            var rotation = 0
            var originalPixelWidth: Int? = null
            var originalPixelHeight: Int? = null
            var jpegImageWidth = false
            var jpegImageHeight = false
            var mp4VideoCreationTime = false
            var fileModificationTime = false

            var takenTagged = false
            for (directory in metadata.directories) {
                val subExifMap = hashMapOf<String, String>()
                val directoryName = directory.name

                for (tag in directory.tags) {
                    if (tag.description != null) {
                        subExifMap[tag.tagName] = tag.description

                        when (tag.tagName) {
                            "Orientation", "Rotation" -> {
                                if ((tag.description.contains("Rotate") && ((!jpegImageHeight && !jpegImageWidth) || directory.name == "Exif IFD0")) || directory.name == "MP4" || directory.name == "QuickTime") {
                                    val digit = tag.description.filter { it.isDigit() }

                                    if (TextUtils.Companion.isInteger(digit)) {
                                        rotation = digit.toInt()
                                    }
                                }
                            }
                            "Creation Date", "Date/Time", "Creation Time", "Date/Time Digitized", "Date/Time Original" -> {
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
                                } catch (_: Exception) {
                                    try {
                                        // Sun Jul 25 14:34:09 PDT 2021
                                        val sourceDateFormat =
                                            SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                                        date = sourceDateFormat.parse(tag.description)

                                    } catch (_: Exception) {
                                        try {
                                            // Sun Jul 25 14:34:09 -07:00 2021
                                            val sourceDateFormat =
                                                SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                            date = sourceDateFormat.parse(tag.description)
                                        } catch (_: Exception) {
                                            try {
                                                // Sun. Jul. 25 14:34:09 -07:00 2021
                                                val sourceDateFormat =
                                                    SimpleDateFormat("EEE. MMM. dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                                date = sourceDateFormat.parse(tag.description)
                                            } catch (_: Exception) {
                                                try {
                                                    // Sun. Jul. 25 14:34:09 -07:00 2021
                                                    val sourceDateFormat =
                                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
                                                    date = sourceDateFormat.parse(tag.description)
                                                } catch (_: Exception) {
                                                    // Do nothing
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!takenTagged && date != null) {
                                    var formattedDate = destFormat.format(date)

                                    // When a file or folder doesn't have a timestamp, it defaults to the epoch date
                                    val gmtPattern = TextUtils.Companion.getCommonDateFormat()
                                    val gmtFormat = SimpleDateFormat(gmtPattern, Locale.ENGLISH)
                                    gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
                                    if (gmtFormat.format(date) == TextUtils.Companion.getEpochDateTime() || gmtFormat.format(date) == TextUtils.Companion.getExifDateTimeDefault()) {
                                        formattedDate = getCurrentTimestamp()
                                        logger.log(
                                            Level.INFO,
                                            "Epoch time detected for " + file.name + ". Changing CreatedAt/TakenAt datetime to the current datetime"
                                        )
                                    }
                                    this.metadataObj.setTakenAt(formattedDate)
                                    this.metadataObj.setCreatedAt(formattedDate)

                                    val dateArray = formattedDate.toString().split(" ")
                                    val takenDateArray = dateArray[0].split("-")

                                    this.metadataObj.setYear(takenDateArray[0].toInt())
                                    this.metadataObj.setMonth(takenDateArray[1].toInt())
                                    this.metadataObj.setDay(takenDateArray[2].toInt())
                                    this.metadataObj.setTime(dateArray[1])

                                    if (tag.tagName == "Date/Time Original") {
                                        takenTagged = true
                                    }

                                    logger.log(
                                        Level.INFO,
                                        "Dates set for " + file.name
                                    )
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
                                } catch (_: Exception) {
                                    try {
                                        // Sun Jul 25 14:34:09 PDT 2021
                                        val sourceDateFormat =
                                            SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
                                        date = sourceDateFormat.parse(tag.description)
                                    } catch (_: Exception) {
                                        try {
                                            // Sun Jul 25 14:34:09 -07:00 2021
                                            val sourceDateFormat =
                                                SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                            date = sourceDateFormat.parse(tag.description)
                                        } catch (_: Exception) {
                                            try {
                                                // Sun. Jul. 25 14:34:09 -07:00 2021
                                                val sourceDateFormat =
                                                    SimpleDateFormat("EEE. MMM. dd HH:mm:ss XXX yyyy", Locale.ENGLISH)
                                                date = sourceDateFormat.parse(tag.description)
                                            } catch (_: Exception) {
                                                // Do nothing
                                            }
                                        }
                                    }
                                }

                                if (date != null) {
                                    this.metadataObj.setModifiedAt(destFormat.format(date))
                                    logger.log(
                                        Level.INFO,
                                        "Modification dates set for " + file.name
                                    )
                                }
                            }
                            "Detected MIME Type" -> {
                                this.metadataObj.setType(tag.description)
                                logger.log(
                                    Level.INFO,
                                    "Type set for " + file.name
                                )
                            }
                            "Expected File Name Extension" -> {
                                this.metadataObj.setExpectedExtension(tag.description)
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
                                    var denominator = 3600
                                    if (latMinute == 0.0) {
                                        denominator = 36
                                    }
                                    val latTotalSeconds = (((latMinute * 60) + latSeconds) / denominator)
                                    latDecimal = latDegree.toString().dropLast(1) + latTotalSeconds.toString().drop(2)
                                }
//                                lat = latDecimal
                                if (latDecimal != "0.0") {
                                    this.metadataObj.setLat(latDecimal)
                                    logger.log(
                                        Level.INFO,
                                        "Lat set for " + file.name
                                    )
                                }
//                                else {
//                                    lat = null
//                                }
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
                                    var denominator = 3600
                                    if (lngMinute == 0.0) {
                                        denominator = 36
                                    }
                                    val lngTotalSeconds = (((lngMinute * 60) + lngSeconds) / denominator)
                                    lngDecimal = lngDegree.toString().dropLast(1) + lngTotalSeconds.toString().drop(2)
                                }
//                                lng = lngDecimal
                                if (lngDecimal != "0.0") {
                                    this.metadataObj.setLng(lngDecimal)
                                    logger.log(
                                        Level.INFO,
                                        "Lng set for " + file.name
                                    )
                                }
//                                else {
//                                    lng = null
//                                }
                            }
                            "ISO Speed Ratings" -> {
                                this.metadataObj.setIso(tag.description.toInt())
                                logger.log(
                                    Level.INFO,
                                    "ISO set for " + file.name
                                )
                            }
                            "Compressor Name", "Compression Type" -> {
                                this.metadataObj.setCompressionType(tag.description)
                                logger.log(
                                    Level.INFO,
                                    "Compression Type set for " + file.name
                                )
                            }
                            "Exposure Time" -> {
                                val exposureArray = tag.description.split(" ")
                                var fraction = exposureArray[0]
                                if (fraction.contains(".")) {
                                    val exposureTime = exposureArray[0].toDouble()
                                    fraction = TextUtils.Companion.convertDecimalToFraction(exposureTime)
                                }
                                this.metadataObj.setExposure(fraction)
                                logger.log(
                                    Level.INFO,
                                    "Exposure Time set for " + file.name
                                )
                            }
                            "F-Number" -> {
                                val regex = "\\d+(\\.\\d+)?".toRegex()
                                val match = regex.find(tag.description)!!
                                val matchValue = match.value

                                if (matchValue.isNotBlank()) {
                                    this.metadataObj.setFstopNumber(matchValue.toDouble())
                                    logger.log(
                                        Level.INFO,
                                        "F-Number set for " + file.name
                                    )
                                }
                            }
                            "Focal Length" -> {
                                val fLengthValue = stringToFloatUsingRegex(tag.description)
                                val df = DecimalFormat("#.#")
                                df.roundingMode = RoundingMode.HALF_EVEN
                                this.metadataObj.setFocalLength(df.format(fLengthValue).toDouble())
                                logger.log(
                                    Level.INFO,
                                    "Focal Length set for " + file.name
                                )
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
                                this.metadataObj.setQuality(tag.description.trim())
                            }
                            "Duration in Seconds" -> {
                                val durationParts = tag.description.split(":")
                                val hours = durationParts[0].toInt()
                                val minutes = durationParts[1].toInt()
                                val seconds = StringUtils.substring(durationParts[2], 0, 2).toInt()

                                this.metadataObj.setDuration(formatDuration(hours, minutes, seconds))
                            }
                            "Duration" -> {
                                val milliseconds = tag.description.toInt()
                                val hours = (milliseconds / (1000 * 60 * 60) % 24)
                                val minutes = (milliseconds / (1000 * 60) % 60)
                                val seconds = (milliseconds / 1000) % 60

                                this.metadataObj.setDuration(formatDuration(hours, minutes, seconds))
                            }
                        }

                        exifMap["$directoryName"] = subExifMap
                    } else {
                        logger.log(
                            Level.WARNING,
                            "Tag description not available for " + file.name + " for tag " + tag.tagName
                        )
                    }
                }
            }

            if (this.metadataObj.getType().isNullOrBlank() && !this.metadataObj.getExpectedExtension().isNullOrBlank()) {
                this.metadataObj.setType("image/${this.metadataObj.getExpectedExtension()}")
            }

            if (this.metadataObj.getCamera().isNullOrBlank() && (!cameraMake.isNullOrBlank() || !cameraModel.isNullOrBlank())) {
                var camera = ""
                if (!cameraMake.isNullOrBlank()) {
                    camera += cameraMake
                }
                if (!cameraModel.isNullOrBlank()) {
                    camera += if (camera.isNotBlank()) " $cameraModel" else cameraModel
                }
                if (camera.isNotBlank()) {
                    this.metadataObj.setCamera(camera.trim())
                    logger.log(
                        Level.INFO,
                        "Camera set for " + file.name
                    )
                }
            }
            if (this.metadataObj.getLens().isNullOrBlank() && (!lensMake.isNullOrBlank() || !lensModel.isNullOrBlank())) {
                var lens = ""
                if (!lensMake.isNullOrBlank()) {
                    lens += lensMake
                }
                if (!lensModel.isNullOrBlank()) {
                    lens += if (lens.isNotBlank()) " $lensModel" else lensModel
                }
                if (lens.isNotBlank()) {
                    this.metadataObj.setLens(lens.trim())
                    logger.log(
                        Level.INFO,
                        "Lens set for " + file.name
                    )
                }
            }

            if (originalPixelHeight != null && originalPixelWidth != null) {
                if (rotation == 90 || rotation == 270 || rotation == -90 || rotation == -270) {
                    this.metadataObj.setOriginalImageWidth(originalPixelHeight)
                    this.metadataObj.setOriginalImageHeight(originalPixelWidth)
                } else {
                    this.metadataObj.setOriginalImageWidth(originalPixelWidth)
                    this.metadataObj.setOriginalImageHeight(originalPixelHeight)
                }
                logger.log(
                    Level.INFO,
                    "Width/height set for " + file.name
                )
            }
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not read metadata for " + file.name + ": " + e.message
            )
        }

        saveExifdata(exifMap, sidecarDir, file.path)

        // Generate a hash for comparing potential duplicates
        if (!this.metadataObj.getType().isNullOrBlank() && this.metadataObj.getType()?.contains("image")!! && !this.metadataObj.getType()?.contains("gif")!!) {
            val dupeImageChecker = DuplicateImageChecker()
            dupeImageChecker.setAlgorithm("dhash")
            var hash = dupeImageChecker.computeHashValue(this.file)
            this.metadataObj.setDuplicateHash(hash)
        }

        this.metadataObj.setId(
            TextUtils.Companion.generateUUID(
                file.path,
                this.metadataObj.getCreatedAt(),
                this.metadataObj.getType(),
                this.metadataObj.getFstopNumber(),
                this.metadataObj.getIso(),
                this.metadataObj.getExposure()
            ).toString()
        )

        val supportedImageFormats = FileUtils.Companion.allowableImageFiles()
        val supportedVideoFormats = FileUtils.Companion.allowableVideoFiles()
        val mediaExtension = FileUtils.Companion.probeFileExtension(file)

        if (supportedImageFormats.contains(mediaExtension)) {
            this.metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/image/${this.metadataObj.getId()}")
        } else if (supportedVideoFormats.contains(mediaExtension)) {
            this.metadataObj.setVideoUrl("/api/$apiVersion/video/${this.metadataObj.getId()}")
        }

        this.metadataObj.setAddedAt(getCurrentTimestamp())

        return metadataObj
    }
    private fun saveExifdata(exifMap: HashMap<String, HashMap<String, String>>, _sidecarDir: String, path: String) {
        if (exifMap.isNotEmpty()) {
            // Update Exif file
            val metadataDirectory = _sidecarDir.dropLast(1) + "/metadata"
            val photoFile = File(path)
            val fileRootDir: String = FileUtils.Companion.getRootDir(photoFile)
            val exifFile = FileUtils.Companion.createFile(
                "$metadataDirectory/$fileRootDir/" + photoFile.name + ".exif.yaml"
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

    private fun formatDuration(hours: Int, minutes: Int, seconds: Int): String {
        val f = DecimalFormat("00")
        var duration = "$minutes:${f.format(seconds)}"
        if (hours > 0) {
            duration = "$hours:$minutes:${f.format(seconds)}"
        }

        return duration
    }

    companion object {
        private var logger: Logger = Logger.getLogger(MetadataProcessing::class.simpleName)

        fun deleteAlbumPhoto(metadataRepository: MetadataRepository, albumRepository: AlbumRepository, albumPhotoRepository: AlbumPhotoRepository, metadataId: String, albumId: Int): Int? {
            albumPhotoRepository.deleteByMetadataIdAndAlbumId(metadataId, albumId)
            val count = albumPhotoRepository.countByAlbumId(albumId)
            if (count != null && count.toInt() > 0) {
                var metadataObj = metadataRepository.findById(metadataId)
                println("metadataId:"+metadataId)
                val coverAlbumUrl = metadataObj.get().getThumbnailUrlCentered()
                val album = albumRepository.findById(albumId)
                if (album.isPresent && album.get().getCoverUrl() == coverAlbumUrl) {
                    // Use the first photo in album
                    val albumPhoto = albumPhotoRepository.findFirstByAlbumId(albumId)
                    if (albumPhoto != null) {
                        metadataObj = metadataRepository.findById(albumPhoto.getMetadataId().toString())
                        album.get().setCoverUrl(metadataObj.get().getThumbnailUrlCentered())
                        albumRepository.save(album.get())
                        logger.log(
                            Level.INFO,
                            "Set the album cover when deleting album photo"
                        )
                    }
                }
            }

            return count
        }

        fun deleteAlbum(albumRepository: AlbumRepository, albumPhotoRepository: AlbumPhotoRepository, userAlbumRepository: UserAlbumRepository, commentRepository: CommentRepository, albumPhotoCommentRepository: AlbumPhotoCommentRepository, albumCommentRepository: AlbumCommentRepository, albumId: Int): Int? {
            val count = albumPhotoRepository.countByAlbumId(albumId)
            if (count != null && count.toInt() == 0) {
                userAlbumRepository.deleteByAlbumId(albumId)
                albumRepository.deleteById(albumId)
                // Delete comments
                val albumComments = albumCommentRepository.findAllByAlbumId(albumId)
                if (albumComments != null) {
                    val commentIdList = ArrayList<Int>()
                    for (albumComment in albumComments) {
                        if (albumComment != null && albumComment.getCommentId() !in commentIdList) {
                            commentIdList.add(albumComment.getCommentId()!!)
                        }
                    }

                    if (commentIdList.isNotEmpty()) {
                        commentRepository.deleteAllById(commentIdList)
                        albumCommentRepository.deleteByAlbumId(albumId)
                        albumPhotoCommentRepository.deleteByAlbumId(albumId)
                    }
                }
            }

            return count
        }

        fun stringToFloatUsingRegex(input: String): Float? {
            val regex = Regex("[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)")
            val match = regex.find(input)
            var result: Float? = null
            if (match != null && match.value.isNotEmpty()) {
                result = match.value.toFloatOrNull()
            }

            return result
        }
    }
}