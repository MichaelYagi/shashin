package com.miyagi.shashin.unit

import org.springframework.test.context.ActiveProfiles
import com.miyagi.shashin.util.MetricsUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

@ActiveProfiles("test")
class MetricsUtilTest {
    @Test
    fun timingTestsTest() {
        val metricsUtil = MetricsUtil()

        val times = intArrayOf(50, 150, 200, 100)

        metricsUtil.start("test1")
        sleep(times[0].toLong())
        metricsUtil.end()

        metricsUtil.start("test2")
        sleep(times[1].toLong())
        metricsUtil.end()

        metricsUtil.start("test3")
        sleep(times[2].toLong())
        metricsUtil.end()

        metricsUtil.start("test4")
        sleep(times[3].toLong())
        metricsUtil.end()

        Assertions.assertTrue(metricsUtil.getAverageTime() > metricsUtil.getMinTime() && metricsUtil.getAverageTime() < metricsUtil.getMaxTime())
        Assertions.assertTrue(metricsUtil.getAllTimings().size == times.size)
//        Assertions.assertTrue(times[0] < metricsUtil.getMinTime())
        Assertions.assertTrue(metricsUtil.getMinTimeModule() == "test1")
//        Assertions.assertTrue(metricsUtil.getMaxTime() > times[2] &&  metricsUtil.getMinTime() < metricsUtil.getMaxTime())
        Assertions.assertTrue(metricsUtil.getMaxTimeModule() == "test3")
        Assertions.assertTrue(metricsUtil.getTotalElapsedTime() > times.sum())
        Assertions.assertTrue(metricsUtil.getMaxTime() == metricsUtil.getMetricsList()[2]["elapsedTime"])
        Assertions.assertTrue(metricsUtil.getMaxTimeModule() == metricsUtil.getMetricsList()[2]["module"])
        Assertions.assertTrue(metricsUtil.getMinTime() == metricsUtil.getMetricsList()[0]["elapsedTime"])
        Assertions.assertTrue(metricsUtil.getMinTimeModule() == metricsUtil.getMetricsList()[0]["module"])
    }
}
