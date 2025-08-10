package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.util.Objects

@Entity
@Table(name = "user")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0
    @NotBlank
    private var username: String? = null
    @NotBlank
    private var password: String? = null
    @NotBlank
    private var isAuthorized: Boolean? = false
    @NotBlank
    private var apikey: String? = null
    @NotBlank
    private var darkMode: Boolean? = false
    @NotBlank
    private var autoplayVideo: Boolean? = false
    @NotBlank
    private var showPlacename: Boolean? = false
    @NotBlank
    private var notificationAlerts: Boolean? = true
    @NotBlank
    private var slideshowProgress: Boolean? = false
    @NotBlank
    private var slideshowOrientation: Int? = 0
    @NotBlank
    private var slideshowInterval: Int? = 10
    @NotBlank
    private var language: String? = "en"
    @NotBlank
    private var slideshowFillScreen: Boolean? = false
    private var profile: String? = null
    private var authority: String? = null
    private var createdAt: String? = null
    private var modifiedAt: String? = null

    fun User() {}

    fun User(
        @NotBlank username: String?,
        @NotBlank password: String?
    ) {
        this.username = username
        this.password = password
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getId(): Int {
        return this.id
    }

    fun getUsername(): String? {
        return this.username
    }

    fun setUsername(username: String?) {
        this.username = username
    }

    fun getPassword(): String? {
        return this.password
    }

    fun setPassword(password: String?) {
        this.password = password
    }

    fun getLanguage(): String? {
        return this.language
    }

    fun setLanguage(language: String?) {
        this.language = language
    }

    fun getSlideshowInterval(): Int? {
        return this.slideshowInterval
    }

    fun setSlideshowInterval(slideshowInterval: Int?) {
        this.slideshowInterval = slideshowInterval
    }

    fun getApikey(): String? {
        return this.apikey
    }

    fun setApikey(apikey: String?) {
        this.apikey = apikey
    }

    fun getCreatedAt(): String? {
        return this.createdAt
    }

    fun setCreatedAt(createdAt: String?) {
        this.createdAt = createdAt
    }

    fun getModifiedAt(): String? {
        return this.modifiedAt
    }

    fun setModifiedAt(modifiedAt: String?) {
        this.modifiedAt = modifiedAt
    }

    fun getAuthority(): String? {
        return this.authority
    }

    fun setAuthority(authority: String?) {
        this.authority = authority
    }

    fun getSlideshowProgress(): Boolean? {
        return this.slideshowProgress
    }

    fun setSlideshowProgress(slideshowProgress: Boolean?) {
        this.slideshowProgress = slideshowProgress
    }

    fun getSlideshowFillScreen(): Boolean? {
        return this.slideshowFillScreen
    }

    fun setSlideshowFillScreen(slideshowFillScreen: Boolean?) {
        this.slideshowFillScreen = slideshowFillScreen
    }

    fun getSlideshowOrientation(): Int? {
        return this.slideshowOrientation
    }

    fun setSlideshowOrientation(slideshowOrientation: Int?) {
        this.slideshowOrientation = slideshowOrientation
    }

    fun getIsAuthorized(): Boolean? {
        return this.isAuthorized
    }

    fun setIsAuthorized(isAuthorized: Boolean?) {
        this.isAuthorized = isAuthorized
    }

    fun getProfile(): String? {
        return this.profile
    }

    fun setProfile(profile: String?) {
        this.profile = profile
    }

    fun getDarkMode(): Boolean? {
        return this.darkMode
    }

    fun setDarkMode(darkMode: Boolean?) {
        this.darkMode = darkMode
    }

    fun getAutoplayVideo(): Boolean? {
        return this.autoplayVideo
    }

    fun setAutoplayVideo(autoplayVideo: Boolean?) {
        this.autoplayVideo = autoplayVideo
    }

    fun getNotificationAlerts(): Boolean? {
        return this.notificationAlerts
    }

    fun setNotificationAlerts(notificationAlerts: Boolean?) {
        this.notificationAlerts = notificationAlerts
    }

    fun getShowPlacename(): Boolean? {
        return this.showPlacename
    }

    fun setShowPlacename(showPlacename: Boolean?) {
        this.showPlacename = showPlacename
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        val user: User = other
        return Objects.equals(username, user.username)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id, username
        )
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["username"] = this.username
        map["authority"] = this.authority
        map["apikey"] = this.apikey
        map["isAuthorized"] = this.isAuthorized
        map["darkMode"] = this.darkMode
        map["autoplayVideo"] = this.autoplayVideo
        map["showPlacename"] = this.showPlacename
        map["notificationAlerts"] = this.notificationAlerts
        map["createdAt"] = this.createdAt
        map["modifiedAt"] = this.modifiedAt
        map["slideshowInterval"] = this.slideshowInterval
        map["language"] = this.language
        map["slideshowProgress"] = this.slideshowProgress
        map["slideshowOrientation"] = this.slideshowOrientation
        map["slideshowFillScreen"] = this.slideshowFillScreen
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