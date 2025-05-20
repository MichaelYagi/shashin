package com.miyagi.shashin.util

import org.springframework.beans.factory.annotation.Value
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.collections.MutableMap

class MetricsUtil {
    private var counter: Int = 0

    private var timings: MutableList<Long> = mutableListOf()

    private var metricsList: MutableList<MutableMap<String, Any>> = mutableListOf()

    private var elapsedTime: Long = 0L

    private var minTime: Long = 0L

    private var minTimeModule: String = ""

    private var maxTime: Long = 0L

    private var maxTimeModule: String = ""

    private var totalElapsedTime: Long = 0L

    private var startTime: Long? = null

    private var endTime: Long? = null

    private var module: String = ""

    private var logger: Logger = Logger.getLogger(MetricsUtil::class.simpleName)

    @Value("\${app.config.default.slaMS}")
    private var slaMS: Long? = null

    fun getMetricsList(): MutableList<MutableMap<String, Any>> {
        return metricsList
    }

    fun getAllTimings(): List<Long> {
        return timings
    }

    fun getAverageTime(): Double {
        return timings.average()
    }

    fun getTotalElapsedTime(): Long {
        return totalElapsedTime
    }

    fun getMaxTime(): Long {
        return maxTime
    }

    fun getMaxTimeModule(): String {
        return maxTimeModule
    }

    fun getMinTime(): Long {
        return minTime
    }

    fun getMinTimeModule(): String {
        return minTimeModule
    }

    fun start(lmodule: String = "") {
        if (lmodule != "") {
            module = lmodule
        }
        var logMessage = " "
        if (module != "") {
            logMessage = " for $module "
        }
        if (counter == 0) {
            logger.log(Level.INFO, "START METRICS recording$logMessage.")
        }
        startTime = System.currentTimeMillis()
        counter++
    }

    fun end() {
        if (slaMS == null) {
            slaMS = 300
        }

        endTime = System.currentTimeMillis()

        var logMessage = " "
        if (module != "") {
            logMessage = " for $module "
        }

        if (startTime != null) {
            elapsedTime = endTime!! - startTime!!
            totalElapsedTime += elapsedTime

            timings.add(elapsedTime)
            metricsList.add(mutableMapOf("module" to module, "elapsedTime" to elapsedTime))

            if (elapsedTime > maxTime || maxTime == 0L) {
                maxTime = elapsedTime
                maxTimeModule = module
            }

            if (elapsedTime < minTime || minTime == 0L) {
                minTime = elapsedTime
                minTimeModule = module
            }

            if (counter > 1) {
                logger.log(Level.INFO, "$counter calls for this instance elapsed time: $totalElapsedTime ms.")
            }

            if (elapsedTime > slaMS!!) {
                logger.log(Level.WARNING, "Elapsed time${logMessage}was $elapsedTime ms.")
            } else {
                logger.log(Level.INFO, "Elapsed time${logMessage}was $elapsedTime ms.")
            }

            if (elapsedTime > slaMS!!) {
                logger.log(Level.WARNING, "The elapsed time ${elapsedTime}${logMessage}is over the SLA threshold of $slaMS ms.")
            }

            if (counter > 1 && totalElapsedTime > slaMS!!) {
                logger.log(Level.WARNING, "The total elapsed time ${totalElapsedTime}${logMessage}is over the SLA threshold of $slaMS ms.")
            }

        } else {
            logger.log(Level.WARNING, "Start time${logMessage}not defined.")
        }

        startTime = null
        endTime = null
    }
}