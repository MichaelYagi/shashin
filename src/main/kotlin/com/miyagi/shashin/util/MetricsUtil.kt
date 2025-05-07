package com.miyagi.shashin.util

import org.springframework.beans.factory.annotation.Value
import java.util.logging.Level
import java.util.logging.Logger

class MetricsUtil {
    private var counter: Int = 0

    private var elapsedTime: Long = 0

    private var totalElapsedTime: Long = 0

    private var startTime: Long? = null

    private var endTime: Long? = null

    private var module: String = ""

    private var logger: Logger = Logger.getLogger(MetricsUtil::class.simpleName)

    @Value("\${app.config.default.slaMS}")
    private var slaMS: Long? = null

    fun getTotalElapsedTime(): Long {
        return totalElapsedTime
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