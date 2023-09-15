package com.miyagi.shashin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.util.UrlPathHelper




@SpringBootApplication
@EnableCaching
@EnableScheduling
class ShashinApplication

fun main(args: Array<String>) {
	System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
	runApplication<ShashinApplication>(*args)
}
