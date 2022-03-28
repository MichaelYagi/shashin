package com.miyagi.shashin.util

import com.drew.imaging.ImageMetadataReader
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.miyagi.shashin.model.Metadata
import net.iakovlev.timeshape.TimeZoneEngine
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

        val accessTime = attr.lastAccessTime().toString()
        date = if (accessTime.contains(".")) {
            sourceFormatMS.parse(accessTime)
        } else {
            sourceFormat.parse(accessTime)
        }
        this.metadataObj.setLastAccessedAt(destFormat.format(date))
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

            var takenTagged = false
            for (directory in metadata.directories) {
                for (tag in directory.tags) {
                    if (tag.description != null) {
                        val tagName = tag.tagName.replace(" ", "").replace("/", "")
                        val directoryName = directory.name.replace(" ", "").replace("/", "")
                        if ("unknowntag" !in tagName.lowercase()) {
                            exifMap["$directoryName-$tagName"] = tag.description
                        }

//                        if (file.name == "DSC00115.JPG") {
//                            println(directory.name)
//                            println(directory.tagCount)
//                            println(file.path)
//                            println(tag.tagName)
//                            println(tag.description)
//                            println()
//                        }

                        when (tag.tagName) {
                            "Orientation", "Rotation" -> {
                                if ((tag.description.contains("Rotate") && ((!jpegImageHeight && !jpegImageWidth) || directory.name == "Exif IFD0")) || directory.name == "MP4") {
                                    val digit = tag.description.filter { it.isDigit() }

                                    if (TextUtils.isInteger(digit)) {
                                        rotation = digit.toInt()
                                    }
                                }
                            }
                            "Date/Time", "Creation Time", "Date/Time Digitized", "Date/Time Original" -> {
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

                                if (date != null && !takenTagged) {
                                    this.metadataObj.setTakenAt(destFormat.format(date))
                                    this.metadataObj.setCreatedAt(destFormat.format(date))

                                    val dateArray = destFormat.format(date).toString().split(" ")
                                    val takenDateArray = dateArray[0].split("-")

                                    this.metadataObj.setYear(takenDateArray[0].toInt())
                                    this.metadataObj.setMonth(takenDateArray[1].toInt())
                                    this.metadataObj.setDay(takenDateArray[2].toInt())
                                    this.metadataObj.setTime(dateArray[1])

                                    if (tag.tagName == "Date/Time Original") {
                                        takenTagged = true
                                    }
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
                                    this.metadataObj.setModifiedAt(destFormat.format(date))
                                }
                            }
                            "Detected MIME Type" -> {
                                this.metadataObj.setType(tag.description)
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
                                lat = latDecimal
                                if (latDecimal != "0.0") {
                                    this.metadataObj.setLat(latDecimal)
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
                                    var denominator = 3600
                                    if (lngMinute == 0.0) {
                                        denominator = 36
                                    }
                                    val lngTotalSeconds = (((lngMinute * 60) + lngSeconds) / denominator)
                                    lngDecimal = lngDegree.toString().dropLast(1) + lngTotalSeconds.toString().drop(2)
                                }
                                lng = lngDecimal
                                if (lngDecimal != "0.0") {
                                    this.metadataObj.setLng(lngDecimal)
                                } else {
                                    lng = null
                                }
                            }
                            "ISO Speed Ratings" -> {
                                this.metadataObj.setIso(tag.description.toInt())
                            }
                            "Compression Type" -> {
                                this.metadataObj.setCompressionType(tag.description)
                            }
                            "Exposure Time" -> {
                                val exposureArray = tag.description.split(" ")
                                var fraction = exposureArray[0]
                                if (fraction.contains(".")) {
                                    val exposureTime = exposureArray[0].toDouble()
                                    fraction = TextUtils.convertDecimalToFraction(exposureTime)
                                }
                                this.metadataObj.setExposure(fraction)
                            }
                            "F-Number" -> {
                                val regex = "\\d+(\\.\\d+)?".toRegex()
                                var matchValue = ""
                                try {
                                    val match = regex.find(tag.description)!!
                                    matchValue=match.value
                                } catch (_: Exception) {}

                                if (matchValue.isNotBlank()) {
                                    this.metadataObj.setFstopNumber(matchValue.toDouble())
                                }
                            }
                            "Focal Length" -> {
                                val flengthValue = tag.description.filter { it.isDigit() }
                                if (flengthValue.isNotBlank()) {
                                    this.metadataObj.setFocalLength(flengthValue.toDouble())
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
                                this.metadataObj.setQuality(tag.description.trim())
                            }
                            "Duration in Seconds" -> {
                                val durationParts = tag.description.split(":")
                                val hours = durationParts[0].toInt()
                                val minutes = durationParts[1].toInt()
                                val seconds = durationParts[2]

                                val hoursToMinutes = hours * 60
                                val totalMinutes = minutes + hoursToMinutes
                                val duration = "$totalMinutes:$seconds"

                                this.metadataObj.setDuration(duration)
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
                }
            }

            if (!lat.isNullOrBlank() && !lng.isNullOrBlank()) {
                val geoDataJson = TextUtils.getGeoData(geocodeUrl, lat, lng)

                val buildPlace = TextUtils.getPlaceNameFromJson(geoDataJson)
                if (buildPlace.isNotBlank()) {
                    this.metadataObj.setPlaceName(buildPlace)

                    val engine = TimeZoneEngine.initialize()
                    val maybeZoneId: Optional<ZoneId> =
                        engine.query(lat.toString().toDouble(), lng.toString().toDouble())
                    val zone = ZoneId.of(maybeZoneId.get().id)
                    val dt = LocalDateTime.now()
                    val zdt: ZonedDateTime = dt.atZone(zone)
                    val offset = zdt.offset
                    this.metadataObj.setTimeZone(offset.toString())
                }
            }

            if (originalPixelHeight != null && originalPixelWidth != null) {
                if (rotation == 90 || rotation == 270) {
                    this.metadataObj.setOriginalImageWidth(originalPixelHeight)
                    this.metadataObj.setOriginalImageHeight(originalPixelWidth)
                } else {
                    this.metadataObj.setOriginalImageWidth(originalPixelWidth)
                    this.metadataObj.setOriginalImageHeight(originalPixelHeight)
                }
            }
        } catch (e: Exception) {
            logger.log(
                Level.WARNING,
                "Could not read metadata for " + file.name + ": " + e.message
            )
        }

        saveExifdata(exifMap, sidecarDir, file.path)

        this.metadataObj.setAddedAt(TextUtils.getCurrentTimestamp())

        this.metadataObj.setId(
            TextUtils.generateUUID(
                file.path,
                this.metadataObj.getCreatedAt(),
                this.metadataObj.getType(),
                this.metadataObj.getFstopNumber(),
                this.metadataObj.getIso(),
                this.metadataObj.getExposure()
            ).toString()
        )

        val supportedImageFormats = FileUtils.allowableImageFiles()
        val supportedVideoFormats = FileUtils.allowableVideoFiles()
//        val supportedAudioFormats = FileUtils.allowableAudioFiles()

        if (supportedImageFormats.contains(file.extension.lowercase())) {
            this.metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/image/${this.metadataObj.getId()}")
        } else if (supportedVideoFormats.contains(file.extension.lowercase())) {
            this.metadataObj.setVideoUrl("/api/$apiVersion/video/${this.metadataObj.getId()}")
        }
//        else if (supportedAudioFormats.contains(file.extension.lowercase())) {
//            metadataObj.setVideoUrl("/api/$apiVersion/audio/${metadataObj.getId()}")
//        }

        return metadataObj
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
}