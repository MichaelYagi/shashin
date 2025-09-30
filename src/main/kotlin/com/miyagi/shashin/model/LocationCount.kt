package com.miyagi.shashin.model

interface LocationCount {
    fun getCountry(): String?
    fun getProvince(): String?
    fun getCity(): String?
    fun getCount(): Int?
}