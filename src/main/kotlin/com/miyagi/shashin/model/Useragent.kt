package com.miyagi.shashin.model

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.io.IOException

@Entity
@Table(name = "useragent")
class Useragent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Int = 0

    @NotBlank
    private var userId: Int? = null
    private var deviceClass: String? = null
    private var osClass: String? = null
    private var osName: String? = null
    private var osVersion: String? = null
    private var agentName: String? = null
    private var agentVersion: String? = null
    private var createdAt: String? = null

    fun setId(id: Int) {
        this.id = id
    }

    fun getId(): Int {
        return this.id
    }

    fun getUserId(): Int? {
        return this.userId
    }

    fun setUserId(userId: Int?) {
        this.userId = userId
    }

    fun getDeviceClass(): String? {
        return this.deviceClass
    }

    fun setDeviceClass(deviceClass: String?) {
        this.deviceClass = deviceClass
    }

    fun getOsClass(): String? {
        return this.osClass
    }

    fun setOsClass(osClass: String?) {
        this.osClass = osClass
    }

    fun getOsName(): String? {
        return this.osName
    }

    fun setOsName(osName: String?) {
        this.osName = osName
    }

    fun getOsVersion(): String? {
        return this.osVersion
    }

    fun setOsVersion(osVersion: String?) {
        this.osVersion = osVersion
    }

    fun getAgentName(): String? {
        return this.agentName
    }

    fun setAgentName(agentName: String?) {
        this.agentName = agentName
    }

    fun getAgentVersion(): String? {
        return this.agentVersion
    }

    fun setAgentVersion(agentVersion: String?) {
        this.agentVersion = agentVersion
    }

    fun getCreatedAt(): String? {
        return this.createdAt
    }

    fun setCreatedAt(createdAt: String?) {
        this.createdAt = createdAt
    }

    override fun toString(): String {
        val map = mutableMapOf<String, Any?>()
        map["id"] = this.id
        map["userId"] = this.userId
        map["deviceClass"] = this.deviceClass
        map["osClass"] = this.osClass
        map["osName"] = this.osName
        map["osVersion"] = this.osVersion
        map["agentName"] = this.agentName
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