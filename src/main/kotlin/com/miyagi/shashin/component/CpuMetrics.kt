package com.miyagi.shashin.component

import com.google.javascript.jscomp.jarjar.javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import java.lang.management.ManagementFactory
import java.util.logging.Level
import java.util.logging.Logger
import javax.management.Attribute
import javax.management.AttributeList
import javax.management.MBeanServer
import javax.management.ObjectName

@Component
class CpuMetrics {

    companion object {
        private const val PROCESS_METRICS_NAME = "process.cpu.load"
        private const val SYSTEM_METRICS_NAME = "system.cpu.load"
    }

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private var logger: Logger = Logger.getLogger(CpuMetrics::class.simpleName)

    @PostConstruct
    fun init() {
        Gauge.builder(PROCESS_METRICS_NAME, this) { getProcessCpuLoad()?.toDouble()!! }
            .baseUnit("%")
            .description("Process CPU Load")
            .register(meterRegistry)

        Gauge.builder(SYSTEM_METRICS_NAME, this) { getSystemCpuLoad()?.toDouble()!! }
            .baseUnit("%")
            .description("System CPU Load")
            .register(meterRegistry)
    }

    fun getProcessCpuLoad(): Double? {
        return try {
            val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
            val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
            val list: AttributeList = mbs.getAttributes(name, arrayOf("ProcessCpuLoad"))

            list.takeIf { it.isNotEmpty() }
                ?.let { it[0] as Attribute }
                ?.value as? Double
        } catch (ex: Exception) {
            logger.log(Level.WARNING, "Process CPU metrics error: ${ex.localizedMessage}")
            null
        }
    }

    fun getSystemCpuLoad(): Double? {
        return try {
            val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
            val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
            val list: AttributeList = mbs.getAttributes(name, arrayOf("SystemCpuLoad"))

            list.takeIf { it.isNotEmpty() }
                ?.let { it[0] as Attribute }
                ?.value as? Double
        } catch (ex: Exception) {
            logger.log(Level.WARNING, "System CPU metrics error: ${ex.localizedMessage}")
            null
        }
    }
}