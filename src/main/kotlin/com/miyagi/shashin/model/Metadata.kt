package com.miyagi.shashin.model

class Metadata {
    fun Metadata(
        path: String?,
        thumbnailPath: String?,
        takenAt: String?,
        uid: String?,
        type: String?,
        fileName: String?,
        timeZone: String?,
        lat: String?,
        lng: String?,
        year: String?,
        month: String?,
        day: String?,
        iso: Int?,
        exposure: String?,
        fNumber: Double?,
        focalLength: Int?,
        camera: String?,
        lens: String?,
        quality: String?,
        keywords: String?,
        createdAt: String?,
        modifiedAt: String?,
        lastAccessedAt: String?
    ) {
        this.Path = path
        this.ThumbnailPath = thumbnailPath
        this.TakenAt = takenAt
        this.UID = uid
        this.Type = type
        this.FileName = fileName
        this.TimeZone = timeZone
        this.Lat = lat
        this.Lng = lng
        this.Year = year
        this.Month = month
        this.Day = day
        this.ISO = iso
        this.Exposure = exposure
        this.FNumber = fNumber
        this.FocalLength = focalLength
        this.Camera = camera
        this.Lens = lens
        this.Quality = quality
        this.Keywords = keywords
        this.CreatedAt = createdAt
        this.ModifiedAt = modifiedAt
        this.LastAccessedAt = lastAccessedAt
    }

    // Without a default constructor, Jackson will throw an exception
    fun Metadata() {}

    private var Path: String? = null
    private var ThumbnailPath: String? = null
    private var TakenAt: String? = null
    private var UID: String? = null
    private var Type: String? = null
    private var FileName: String? = null
    private var TimeZone: String? = null
    private var Lat: String? = null
    private var Lng: String? = null
    private var Year: String? = null
    private var Month: String? = null
    private var Day: String? = null
    private var ISO: Int? = 0
    private var Exposure: String? = null
    private var FNumber: Double? = 0.0
    private var FocalLength: Int? = 0
    private var Camera: String? = null
    private var Lens: String? = null
    private var Quality: String? = null
    private var Keywords: String? = null
    private var CreatedAt: String? = null
    private var ModifiedAt: String? = null
    private var LastAccessedAt: String? = null

    fun setPath(path: String?) {
        this.Path = path
    }
    fun setThumbnailPath(thumbnailPath: String?) {
        this.ThumbnailPath = thumbnailPath
    }
    fun setTakenAt(takenAt: String?) {
        this.TakenAt = takenAt
    }
    fun setUID(uid: String?) {
        this.UID = uid
    }
    fun setType(type: String?) {
        this.Type = type
    }
    fun setFileName(fileName: String?) {
        this.FileName = fileName
    }
    fun setTimeZone(timeZone: String?) {
        this.TimeZone = timeZone
    }
    fun setLat(lat: String?) {
        this.Lat = lat
    }
    fun setLng(lng: String?) {
        this.Lng = lng
    }
    fun setYear(year: String?) {
        this.Year = year
    }
    fun setMonth(month: String?) {
        this.Month = month
    }
    fun setDay(day: String?) {
        this.Day = day
    }
    fun setISO(iso: Int?) {
        this.ISO = iso
    }
    fun setExposure(exposure: String?) {
        this.Exposure = exposure
    }
    fun setFNumber(fNumber: Double?) {
        this.FNumber = fNumber
    }
    fun setFocalLength(focalLength: Int?) {
        this.FocalLength = focalLength
    }
    fun setCamera(camera: String?) {
        this.Camera = camera
    }
    fun setLens(lens: String?) {
        this.Lens = lens
    }
    fun setQuality(quality: String?) {
        this.Quality = quality
    }
    fun setKeywords(keywords: String?) {
        this.Keywords = keywords
    }
    fun setCreatedAt(createdAt: String?) {
        this.CreatedAt = createdAt
    }
    fun setModifiedAt(modifiedAt: String?) {
        this.ModifiedAt = modifiedAt
    }
    fun setLastAccessedAt(lastAccessedAt: String?) {
        this.LastAccessedAt = lastAccessedAt
    }

    fun getPath(): String? {
        return this.Path
    }
    fun getThumbnailPath(): String? {
        return this.ThumbnailPath
    }
    fun getTakenAt(): String? {
        return this.TakenAt
    }
    fun getUID(): String? {
        return this.UID
    }
    fun getType(): String? {
        return this.Type
    }
    fun getFileName(): String? {
        return this.FileName
    }
    fun getTimeZone(): String? {
        return this.TimeZone
    }
    fun getLat(): String? {
        return this.Lat
    }
    fun getLng(): String? {
        return this.Lng
    }
    fun getYear(): String? {
        return this.Year
    }
    fun getMonth(): String? {
        return this.Month
    }
    fun getDay(): String? {
        return this.Day
    }
    fun getISO(): Int? {
        return this.ISO
    }
    fun getExposure(): String? {
        return this.Exposure
    }
    fun getFNumber(): Double? {
        return this.FNumber
    }
    fun getFocalLength(): Int? {
        return this.FocalLength
    }
    fun getCamera(): String? {
        return this.Camera
    }
    fun getLens(): String? {
        return this.Lens
    }
    fun getQuality(): String? {
        return this.Quality
    }
    fun getKeywords(): String? {
        return this.Keywords
    }
    fun getCreatedAt(): String? {
        return this.CreatedAt
    }
    fun getModifiedAt(): String? {
        return this.ModifiedAt
    }
    fun getLastAccessedAt(): String? {
        return return this.LastAccessedAt
    }
}