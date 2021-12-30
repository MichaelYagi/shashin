package com.miyagi.shashin

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import java.io.File
import java.net.URL


abstract class BaseSeleniumTests {
    protected var driver: WebDriver? = null
    private val os = System.getProperty("os.name")

    @BeforeEach
    open fun setUp() {
        val driverFileStr: String = findFile()!!
//        println(os)
//        println(driverFileStr)
        val capabilities = DesiredCapabilities.chrome()
        val options = ChromeOptions()
        options.addArguments("--no-sandbox") // Bypass OS security model, MUST BE THE VERY FIRST OPTION
        options.addArguments("--headless")
//        options.setExperimentalOption("useAutomationExtension", false)
        options.addArguments("start-maximized") // open Browser in maximized mode
        options.addArguments("disable-infobars") // disabling infobars
        options.addArguments("--disable-extensions") // disabling extensions
        options.addArguments("--disable-gpu") // applicable to windows os only
        options.addArguments("--disable-dev-shm-usage") // overcome limited resource problems
        options.merge(capabilities)

        val driverFile = File(driverFileStr)
        if (!driverFile.canExecute()) {
            driverFile.setExecutable(true)
        }
        when {
            os.contains("windows", ignoreCase = true) -> {
                val service = ChromeDriverService.Builder()
                    .usingDriverExecutable(driverFile)
                    .build()
                driver = ChromeDriver(service, options)
            }
            os.contains("mac", ignoreCase = true) -> {
                System.setProperty("webdriver.chrome.driver", driverFile.absolutePath);
                driver = ChromeDriver(options)
            }
            os.contains("linux", ignoreCase = true) -> {
                System.setProperty("webdriver.chrome.driver", driverFile.absolutePath);
                driver = ChromeDriver(options)
            }
            else -> {
                System.setProperty("webdriver.chrome.driver", driverFile.absolutePath);
                driver = ChromeDriver(options)
            }
        }
    }

    private fun findFile(): String? {
        val classLoader = javaClass.classLoader

        val chromeDriver = when {
            os.contains("windows", ignoreCase = true) -> "chromedriver_windows.exe"
            os.contains("mac", ignoreCase = true) -> "chromedriver_mac64"
            os.contains("linux", ignoreCase = true) -> "chromedriver_linux"
            else -> "chromedriver_linux"
        }
        val url: URL = classLoader.getResource(chromeDriver)
        return url.file
    }

    @AfterEach
    open fun tearDown() {
        if (driver != null) {
            driver!!.quit()
        }
    }
}