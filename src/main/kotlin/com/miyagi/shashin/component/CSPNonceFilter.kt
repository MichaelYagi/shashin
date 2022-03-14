package com.miyagi.shashin.component

import org.apache.commons.lang3.StringUtils
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import java.security.SecureRandom
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper


class CSPNonceFilter : GenericFilterBean() {
    private val secureRandom: SecureRandom = SecureRandom()

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(req: ServletRequest, res: ServletResponse?, chain: FilterChain) {
        val request = req as HttpServletRequest
        val response = res as HttpServletResponse?
        val nonceArray = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonceArray)
        var nonce: String = Base64.getEncoder().encodeToString(nonceArray)
        if (request.requestURI == "/") {
            nonce = "0"
        }
        request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce)
        chain.doFilter(request, CSPNonceResponseWrapper(response, nonce))
    }

    /**
     * Wrapper to fill the nonce value
     */
    class CSPNonceResponseWrapper(response: HttpServletResponse?, private val nonce: String) :
        HttpServletResponseWrapper(response) {
        override fun setHeader(name: String, value: String) {
            if (name == "Content-Security-Policy" && StringUtils.isNotBlank(value)) {
                super.setHeader(name, value.replace("{nonce}", nonce))
            } else {
                super.setHeader(name, value)
            }
        }

        override fun addHeader(name: String, value: String) {
            if (name == "Content-Security-Policy" && StringUtils.isNotBlank(value)) {
                super.addHeader(name, value.replace("{nonce}", nonce))
            } else {
                super.addHeader(name, value)
            }
        }
    }

    companion object {
        private const val NONCE_SIZE = 32 //recommended is at least 128 bits/16 bytes
        private const val CSP_NONCE_ATTRIBUTE = "cspNonce"
    }
}