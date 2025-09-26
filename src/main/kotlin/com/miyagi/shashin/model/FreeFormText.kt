package com.miyagi.shashin.model

class FreeFormText {
    private var clientIP: String? = null
    private var browser: String? = null
    private var requestResourceType: String? = null
    private var operatingSystem: String? = null
    private var viewPage: String? = null

    fun setClientIP(clientIP: String?) {
        this.clientIP = clientIP
    }
    fun setBrowser(browser: String?) {
        this.browser = browser
    }
    fun setRequestResourceType(requestResourceType: String?) {
        this.requestResourceType = requestResourceType
    }
    fun setOperatingSystem(operatingSystem: String?) {
        this.operatingSystem = operatingSystem
    }
    fun setViewPage(viewPage: String?) {
        this.viewPage = viewPage
    }
    fun getClientIP(): String? {
        return this.clientIP
    }
    fun getBrowser(): String? {
        return this.browser
    }
    fun getRequestResourceType(): String? {
        return this.requestResourceType
    }
    fun getOperatingSystem(): String? {
        return this.operatingSystem
    }
    fun getViewPage(): String? {
        return this.viewPage
    }
}