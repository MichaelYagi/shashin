package com.miyagi.shashin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class ShashinApplication

fun main(args: Array<String>) {
	runApplication<ShashinApplication>(*args)
}
