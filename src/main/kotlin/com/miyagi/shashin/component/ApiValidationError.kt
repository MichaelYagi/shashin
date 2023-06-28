package com.miyagi.shashin.component


internal abstract class ApiSubError

internal class ApiValidationError(private val `object`: String, private val message: String) : ApiSubError() {
    private val field: String? = null
    private val rejectedValue: Any? = null
}