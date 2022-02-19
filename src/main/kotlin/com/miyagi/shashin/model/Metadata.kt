package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.compress.archivers.dump.DumpArchiveConstants
import java.io.IOException
import javax.persistence.*

@Entity
@Table(name = "metadata")
class Metadata {
    @Id
    private lateinit var id: String
    private var path: String? = null
    private var title: String? = null
    private var description: String? = null
    private var thumbnailPathSmall: String? = null
    private var thumbnailUrlSmall: String? = null
    private var thumbnailSmallWidth: Int? = null
    private var thumbnailSmallHeight: Int? = null
    private var originalImageWidth: Int? = null
    private var originalImageHeight: Int? = null
    private var thumbnailPathCentered: String? = null
    private var thumbnailUrlCentered: String? = null
    private var thumbnailUrlOriginal: String? = null
    private var mapMarkerPath: String? = null
    private var mapMarkerUrl: String? = null
    private var folder: String? = null
    private var videoUrl: String? = null
    private var duration: String? = null
    private var type: String? = null
    private var fileName: String? = null
    private var expectedExtension: String? = null
    private var timeZone: String? = null
    private var lat: String? = null
    private var lng: String? = null
    private var placeName: String? = null
    private var year: Int? = null
    private var month: Int? = null
    private var day: Int? = null
    private var time: String? = null
    private var iso: Int? = null
    private var exposure: String? = null
    private var fstopNumber: Double? = null
    private var focalLength: Double? = null
    private var camera: String? = null
    private var lens: String? = null
    private var quality: String? = null
    private var compressionType: String? = null
    private var hidden: Boolean? = null
    private var addedAt: String? = null
    private var takenAt: String? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null
    private var lastAccessedAt: String? = null

    // Without a default constructor, Jackson will throw an exception
    fun Metadata() {}

    fun setPath(path: String?) {
        this.path = path
    }
    fun setTitle(title: String?) {
        this.title = title
    }
    fun setDescription(description: String?) {
        this.description = description
    }
    fun setThumbnailPathSmall(thumbnailPathSmall: String?) {
        this.thumbnailPathSmall = thumbnailPathSmall
    }
    fun setThumbnailUrlSmall(thumbnailUrlSmall: String?) {
        this.thumbnailUrlSmall = thumbnailUrlSmall
    }
    fun setThumbnailSmallWidth(thumbnailSmallWidth: Int?) {
        this.thumbnailSmallWidth = thumbnailSmallWidth
    }
    fun setThumbnailSmallHeight(thumbnailSmallHeight: Int?) {
        this.thumbnailSmallHeight = thumbnailSmallHeight
    }
    fun setOriginalImageWidth(originalImageWidth: Int?) {
        this.originalImageWidth = originalImageWidth
    }
    fun setOriginalImageHeight(originalImageHeight: Int?) {
        this.originalImageHeight = originalImageHeight
    }
    fun setThumbnailPathCentered(thumbnailPathCentered: String?) {
        this.thumbnailPathCentered = thumbnailPathCentered
    }
    fun setThumbnailUrlCentered(thumbnailUrlCentered: String?) {
        this.thumbnailUrlCentered = thumbnailUrlCentered
    }
    fun setThumbnailUrlOriginal(thumbnailUrlOriginal: String?) {
        this.thumbnailUrlOriginal = thumbnailUrlOriginal
    }
    fun setMapMarkerPath(mapMarkerPath: String?) {
        this.mapMarkerPath = mapMarkerPath
    }
    fun setMapMarkerUrl(mapMarkerUrl: String?) {
        this.mapMarkerUrl = mapMarkerUrl
    }
    fun setFolder(folder: String?) {
        this.folder = folder
    }
    fun setVideoUrl(videoUrl: String?) {
        this.videoUrl = videoUrl
    }
    fun setDuration(duration: String?) {
        this.duration = duration
    }
    fun setTakenAt(takenAt: String?) {
        this.takenAt = takenAt
    }
    fun setAddedAt(addedAt: String?) {
        this.addedAt = addedAt
    }
    fun setId(id: String) {
        this.id = id
    }
    fun setType(type: String?) {
        this.type = type
    }
    fun setFileName(fileName: String?) {
        this.fileName = fileName
    }
    fun setExpectedExtension(expectedExtension: String?) {
        this.expectedExtension = expectedExtension
    }
    fun setTimeZone(timeZone: String?) {
        this.timeZone = timeZone
    }
    fun setLat(lat: String?) {
        this.lat = lat
    }
    fun setLng(lng: String?) {
        this.lng = lng
    }
    fun setPlaceName(placeName: String?) {
        this.placeName = placeName
    }
    fun setYear(year: Int?) {
        this.year = year
    }
    fun setMonth(month: Int?) {
        this.month = month
    }
    fun setDay(day: Int?) {
        this.day = day
    }
    fun setTime(time: String?) {
        this.time = time
    }
    fun setIso(iso: Int?) {
        this.iso = iso
    }
    fun setExposure(exposure: String?) {
        this.exposure = exposure
    }
    fun setFstopNumber(fstopNumber: Double?) {
        this.fstopNumber = fstopNumber
    }
    fun setFocalLength(focalLength: Double?) {
        this.focalLength = focalLength
    }
    fun setCamera(camera: String?) {
        this.camera = camera
    }
    fun setLens(lens: String?) {
        this.lens = lens
    }
    fun setQuality(quality: String?) {
        this.quality = quality
    }
    fun setCompressionType(compressionType: String?) {
        this.compressionType = compressionType
    }
    fun setHidden(hidden: Boolean?) {
        this.hidden = hidden
    }
    fun setCreatedAt(createdAt: String?) {
        this.createdAt = createdAt
    }
    fun setModifiedAt(modifiedAt: String?) {
        this.modifiedAt = modifiedAt
    }
    fun setLastAccessedAt(lastAccessedAt: String?) {
        this.lastAccessedAt = lastAccessedAt
    }

    fun getPath(): String? {
        return this.path
    }
    fun getTitle(): String? {
        return this.title
    }
    fun getDescription(): String? {
        return this.description
    }
    fun getThumbnailPathSmall(): String? {
        return this.thumbnailPathSmall
    }
    fun getThumbnailUrlSmall(): String? {
        return this.thumbnailUrlSmall
    }
    fun getThumbnailPathCentered(): String? {
        return this.thumbnailPathCentered
    }
    fun getThumbnailUrlCentered(): String? {
        return this.thumbnailUrlCentered
    }
    fun getThumbnailUrlOriginal(): String? {
        return this.thumbnailUrlOriginal
    }
    fun getThumbnailSmallWidth(): Int? {
        return this.thumbnailSmallWidth
    }
    fun getThumbnailSmallHeight(): Int? {
        return this.thumbnailSmallHeight
    }
    fun getOriginalImageWidth(): Int? {
        return this.originalImageWidth
    }
    fun getOriginalImageHeight(): Int? {
        return this.originalImageHeight
    }
    fun getMapMarkerPath(): String? {
        return this.mapMarkerPath
    }
    fun getMapMarkerUrl(): String? {
        return this.mapMarkerUrl
    }
    fun getFolder(): String? {
        return this.folder
    }
    fun getVideoUrl(): String? {
        return this.videoUrl
    }
    fun getDuration(): String? {
        return this.duration
    }
    fun getTakenAt(): String? {
        return this.takenAt
    }
    fun getAddedAt(): String? {
        return this.addedAt
    }
    fun getId(): String {
        return this.id
    }
    fun getType(): String? {
        return this.type
    }
    fun getFileName(): String? {
        return this.fileName
    }
    fun getExpectedExtension(): String? {
        return this.expectedExtension
    }
    fun getTimeZone(): String? {
        return this.timeZone
    }
    fun getLat(): String? {
        return this.lat
    }
    fun getLng(): String? {
        return this.lng
    }
    fun getPlaceName(): String? {
        return this.placeName
    }
    fun getYear(): Int? {
        return this.year
    }
    fun getMonth(): Int? {
        return this.month
    }
    fun getDay(): Int? {
        return this.day
    }
    fun getTime(): String? {
        return this.time
    }
    fun getIso(): Int? {
        return this.iso
    }
    fun getExposure(): String? {
        return this.exposure
    }
    fun getFstopNumber(): Double? {
        return this.fstopNumber
    }
    fun getFocalLength(): Double? {
        return this.focalLength
    }
    fun getCamera(): String? {
        return this.camera
    }
    fun getLens(): String? {
        return this.lens
    }
    fun getQuality(): String? {
        return this.quality
    }
    fun getCompressionType(): String? {
        return this.compressionType
    }
    fun getHidden(): Boolean? {
        return this.hidden
    }
    fun getCreatedAt(): String? {
        return this.createdAt
    }
    fun getModifiedAt(): String? {
        return this.modifiedAt
    }
    fun getLastAccessedAt(): String? {
        return this.lastAccessedAt
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["thumbnailUrlSmall"] = this.thumbnailUrlSmall
        map["thumbnailUrlCentered"] = this.thumbnailUrlCentered
        map["thumbnailUrlOriginal"] = this.thumbnailUrlOriginal
        map["thumbnailSmallWidth"] = this.thumbnailSmallWidth
        map["thumbnailSmallHeight"] = this.thumbnailSmallHeight
        map["originalImageWidth"] = this.originalImageWidth
        map["originalImageHeight"] = this.originalImageHeight
        map["mapMarkerUrl"] = this.mapMarkerUrl
        map["folder"] = this.folder
        map["videoUrl"] = this.videoUrl
        map["duration"] = this.duration
        map["type"] = this.type
        map["fileName"] = this.fileName
        map["expectedExtension"] = this.expectedExtension
        map["title"] = this.title
        map["description"] = this.description
        map["timeZone"] = this.timeZone
        map["lat"] = this.lat
        map["lng"] = this.lng
        map["placeName"] = this.placeName
        map["year"] = this.year
        map["month"] = this.month
        map["day"] = this.day
        map["time"] = this.time
        map["iso"] = this.iso
        map["exposure"] = this.exposure
        map["fstopNumber"] = this.fstopNumber
        map["focalLength"] = this.focalLength
        map["camera"] = this.camera
        map["lens"] = this.lens
        map["quality"] = this.quality
        map["compressionType"] = this.compressionType
        map["hidden"] = this.hidden
        map["takenAt"] = this.takenAt
        map["addedAt"] = this.addedAt
        map["createdAt"] = this.createdAt
        map["modifiedAt"] = this.modifiedAt
        map["lastAccessedAt"] = this.lastAccessedAt

        val mapper = ObjectMapper()
        var mapJson: String? = "{}"
        try {
            mapJson = mapper.writeValueAsString(map)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return mapJson.toString()
    }
}