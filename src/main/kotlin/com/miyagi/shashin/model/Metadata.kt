package com.miyagi.shashin.model

import javax.persistence.*

@Entity
@Table(name = "metadata")
class Metadata {
    @Id
    private lateinit var id: String
    private var path: String? = null
    private var thumbnailPathSmall: String? = null
    private var thumbnailUrlSmall: String? = null
    private var thumbnailPathCentered: String? = null
    private var thumbnailUrlCentered: String? = null
    private var thumbnailPathOriginal: String? = null
    private var thumbnailUrlOriginal: String? = null
    private var videoUrl: String? = null
    private var takenAt: String? = null
    private var type: String? = null
    private var fileName: String? = null
    private var timeZone: String? = null
    private var lat: String? = null
    private var lng: String? = null
    private var year: Int? = null
    private var month: Int? = null
    private var day: Int? = null
    private var iso: Int? = null
    private var exposure: String? = null
    private var fNumber: Double? = null
    private var focalLength: Double? = null
    private var camera: String? = null
    private var lens: String? = null
    private var quality: String? = null
    private var keywords: String? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null
    private var lastAccessedAt: String? = null

    // Without a default constructor, Jackson will throw an exception
    fun Metadata() {}

    fun setPath(path: String?) {
        this.path = path
    }
    fun setThumbnailPathSmall(thumbnailPathSmall: String?) {
        this.thumbnailPathSmall = thumbnailPathSmall
    }
    fun setThumbnailUrlSmall(thumbnailUrlSmall: String?) {
        this.thumbnailUrlSmall = thumbnailUrlSmall
    }
    fun setThumbnailPathCentered(thumbnailPathCentered: String?) {
        this.thumbnailPathCentered = thumbnailPathCentered
    }
    fun setThumbnailUrlCentered(thumbnailUrlCentered: String?) {
        this.thumbnailUrlCentered = thumbnailUrlCentered
    }
    fun setThumbnailPathOriginal(thumbnailPathOriginal: String?) {
        this.thumbnailPathOriginal = thumbnailPathOriginal
    }
    fun setThumbnailUrlOriginal(thumbnailUrlOriginal: String?) {
        this.thumbnailUrlOriginal = thumbnailUrlOriginal
    }
    fun setVideoUrl(videoUrl: String?) {
        this.videoUrl = videoUrl
    }
    fun setTakenAt(takenAt: String?) {
        this.takenAt = takenAt
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
    fun setTimeZone(timeZone: String?) {
        this.timeZone = timeZone
    }
    fun setLat(lat: String?) {
        this.lat = lat
    }
    fun setLng(lng: String?) {
        this.lng = lng
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
    fun setISO(iso: Int?) {
        this.iso = iso
    }
    fun setExposure(exposure: String?) {
        this.exposure = exposure
    }
    fun setFNumber(fNumber: Double?) {
        this.fNumber = fNumber
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
    fun setKeywords(keywords: String?) {
        this.keywords = keywords
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
        return this.thumbnailPathCentered
    }
    fun getThumbnailPathOriginal(): String? {
        return this.thumbnailPathOriginal
    }
    fun getThumbnailUrlOriginal(): String? {
        return this.thumbnailUrlOriginal
    }
    fun getVideoUrl(): String? {
        return this.videoUrl
    }
    fun getTakenAt(): String? {
        return this.takenAt
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
    fun getTimeZone(): String? {
        return this.timeZone
    }
    fun getLat(): String? {
        return this.lat
    }
    fun getLng(): String? {
        return this.lng
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
    fun getISO(): Int? {
        return this.iso
    }
    fun getExposure(): String? {
        return this.exposure
    }
    fun getFNumber(): Double? {
        return this.fNumber
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
    fun getKeywords(): String? {
        return this.keywords
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
}