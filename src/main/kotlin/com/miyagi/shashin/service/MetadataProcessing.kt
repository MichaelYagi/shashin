package com.miyagi.shashin.service

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.repository.*
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


class MetadataProcessing() {

    private var logger: Logger = Logger.getLogger(MetadataProcessing::class.simpleName)
    private lateinit var apiVersion: String
    private var file: File? = null
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
        this.metadataObj.setPath(file!!.path)
        this.metadataObj.setFileName(file!!.name)
        this.metadataObj.setTitle(file!!.name)
        this.metadataObj.setBrightness("1.0")
        this.metadataObj.setContrast("1.0")
        this.metadataObj.setSaturation("1.0")
        this.metadataObj.setSharpness("1.0")
        this.metadataObj.setFlipVertically(false)
        this.metadataObj.setFlipHorizontally(false)
        this.metadataObj.setRotation(0)

        // Get file data
        val attr: BasicFileAttributes = Files.readAttributes(
            file!!.toPath(),
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
        // File modification time in the server's time zone
        if (file!!.lastModified() > 0) {
            this.metadataObj.setModifiedAt(destFormat.format(Date(file!!.lastModified())))
        }

        // Read photo, RAW, video and audio metadata with Kiroku. Only valid values are set; missing values stay null.
        val result = KirokuMetadata.read(file!!)
        if (result != null) {
            KirokuMetadata.mimeType(result)?.let { this.metadataObj.setType(it) }
            KirokuMetadata.expectedExtension(result)?.let { this.metadataObj.setExpectedExtension(it) }

            // Capture time. Without one, the file times set above are kept.
            val takenAt = KirokuMetadata.capturedAtLocal(result)
            if (takenAt != null) {
                val formattedDate = takenAt.format(DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH))
                this.metadataObj.setTakenAt(formattedDate)
                this.metadataObj.setCreatedAt(formattedDate)

                val dateArray = formattedDate.split(" ")
                val takenDateArray = dateArray[0].split("-")

                this.metadataObj.setYear(takenDateArray[0].toInt())
                this.metadataObj.setMonth(takenDateArray[1].toInt())
                this.metadataObj.setDay(takenDateArray[2].toInt())
                this.metadataObj.setTime(dateArray[1])
            }

            KirokuMetadata.iso(result)?.let { this.metadataObj.setIso(it) }
            KirokuMetadata.exposure(result)?.let { this.metadataObj.setExposure(it) }
            KirokuMetadata.fNumber(result)?.let { this.metadataObj.setFstopNumber(it) }
            KirokuMetadata.focalLength(result)?.let { this.metadataObj.setFocalLength(it) }
            KirokuMetadata.quality(result)?.let { this.metadataObj.setQuality(it) }
            KirokuMetadata.compressionType(result)?.let { this.metadataObj.setCompressionType(it) }
            KirokuMetadata.duration(result)?.let { this.metadataObj.setDuration(it) }

            // 0.0 has always been treated as "no coordinate".
            KirokuMetadata.latitude(result)?.takeIf { it != 0.0 }?.let { this.metadataObj.setLat(it.toString()) }
            KirokuMetadata.longitude(result)?.takeIf { it != 0.0 }?.let { this.metadataObj.setLng(it.toString()) }

            if (this.metadataObj.getCamera().isNullOrBlank()) {
                KirokuMetadata.camera(result)?.let { this.metadataObj.setCamera(it) }
            }
            if (this.metadataObj.getLens().isNullOrBlank()) {
                KirokuMetadata.lens(result)?.let { this.metadataObj.setLens(it) }
            }

            // Pixel size as displayed (width and height swapped for 90/270 degree orientations).
            KirokuMetadata.orientedSize(result)?.let { (width, height) ->
                this.metadataObj.setOriginalImageWidth(width)
                this.metadataObj.setOriginalImageHeight(height)
            }

            logger.log(Level.INFO, "Metadata read for " + file!!.name + " (" + result.status + ")")
        }

        if (this.metadataObj.getType().isNullOrBlank() && !this.metadataObj.getExpectedExtension().isNullOrBlank()) {
            this.metadataObj.setType("image/${this.metadataObj.getExpectedExtension()}")
        }

        KirokuMetadata.writeExifSidecar(KirokuMetadata.exifMap(result), sidecarDir, file!!)

        // Generate a hash for comparing potential duplicates
        if (!this.metadataObj.getType().isNullOrBlank() && this.metadataObj.getType()?.contains("image")!! && file!!.exists()) {
            if (file != null) {
                val dupeImageDetection = DuplicateImageDetection()
                val hash = dupeImageDetection.dhash(file)
                if (hash != null) {
                    this.metadataObj.setDuplicateHash(hash.toString())
                }
            } else {
                logger.log(
                    Level.WARNING,
                    "Could not read metadata for DuplicateImageDetection " + file!!.name
                )
            }
        }

        this.metadataObj.setId(
            TextUtils.generateUUID(
                file!!.path,
                this.metadataObj.getCreatedAt(),
                this.metadataObj.getType(),
                this.metadataObj.getFstopNumber(),
                this.metadataObj.getIso(),
                this.metadataObj.getExposure()
            ).toString()
        )

        val supportedImageFormats = FileUtils.Companion.allowableImageFiles()
        val supportedVideoFormats = FileUtils.Companion.allowableVideoFiles()
        val mediaExtension = FileUtils.Companion.probeFileExtension(file!!)

        if (supportedImageFormats.contains(mediaExtension)) {
            this.metadataObj.setThumbnailUrlOriginal("/api/$apiVersion/image/${this.metadataObj.getId()}")
        } else if (supportedVideoFormats.contains(mediaExtension)) {
            this.metadataObj.setVideoUrl("/api/$apiVersion/video/${this.metadataObj.getId()}")
        }

        this.metadataObj.setAddedAt(getCurrentTimestamp())

        return metadataObj
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