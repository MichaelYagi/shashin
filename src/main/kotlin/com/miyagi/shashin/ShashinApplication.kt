package com.miyagi.shashin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Instant


@SpringBootApplication
@EnableCaching
@EnableScheduling
class ShashinApplication

@Autowired
var jdbcTemplate: JdbcTemplate? = null

fun main(args: Array<String>) {
	System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true")
	System.setProperty("shashinServerStartUnixMS", (System.currentTimeMillis()).toString())
	jdbcTemplate?.execute("PRAGMA journal_mode = WAL")
	jdbcTemplate?.execute("PRAGMA synchronous = NORMAL")
	runApplication<ShashinApplication>(*args)
}
