package com.miyagi.shashin.e2e

import com.miyagi.shashin.repository.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import java.io.File
import java.net.URL
import java.util.logging.Logger
import javax.transaction.Transactional

abstract class BaseSeleniumTests {
    protected var driver: WebDriver? = null
    protected var logger: Logger = Logger.getLogger(BaseSeleniumTests::class.simpleName)
    protected var elementScanTimeoutMillis = 3000
    private val os = System.getProperty("os.name")

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val metadataRepository: MetadataRepository? = null

    @Autowired
    private val mediaDirRepository: MediaDirectoryRepository? = null

    @Autowired
    private val userAlbumRepository: UserAlbumRepository? = null

    @Autowired
    private val favoriteRepository: FavoriteRepository? = null

    @Autowired
    private val commentRepository: CommentRepository? = null

    @Autowired
    private val albumPhotoCommentRepository: AlbumPhotoCommentRepository? = null

    @Autowired
    private val albumCommentRepository: AlbumCommentRepository? = null

    @Autowired
    private val albumRepository: AlbumRepository? = null

    @Autowired
    private val albumPhotoRepository: AlbumPhotoRepository? = null

    @Autowired
    private val notificationRepository: NotificationRepository? = null

    @Autowired
    private val recognitionLabelRepository: RecognitionLabelRepository? = null

    @Autowired
    private val recognitionLabelPhotoRepository: RecognitionLabelPhotoRepository? = null

    @Autowired
    private val settingsRepository: SettingsRepository? = null


    @Transactional
    @BeforeEach
    open fun setUp() {
        deleteRecords()

        val driverFileStr: String = findFile()!!
//        println(os)
//        println(driverFileStr)
        val capabilities = DesiredCapabilities.chrome()
        val options = ChromeOptions()
        options.addArguments("--no-sandbox") // Bypass OS security model, MUST BE THE VERY FIRST OPTION
        options.addArguments("--disable-setuid-sandbox")
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
                // CircleCI default location
                System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
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
            os.contains("windows", ignoreCase = true) -> "cdwindows/chromedriver.exe"
            os.contains("mac", ignoreCase = true) -> "cdmac64/chromedriver"
            os.contains("linux", ignoreCase = true) -> "cdlinux/chromedriver"
            else -> "chromedriver_linux"
        }
        val url: URL = classLoader.getResource(chromeDriver)
        return url.file
    }


    @AfterEach
    open fun tearDown() {
        deleteRecords()

        if (driver != null) {
            driver!!.quit()
        }
    }

    @Transactional
    open fun deleteRecords() {
        userRepository?.deleteAll()
        metadataRepository?.deleteAll()
        mediaDirRepository?.deleteAll()
        userAlbumRepository?.deleteAll()
        favoriteRepository?.deleteAll()
        commentRepository?.deleteAll()
        albumPhotoCommentRepository?.deleteAll()
        albumCommentRepository?.deleteAll()
        albumRepository?.deleteAll()
        albumPhotoRepository?.deleteAll()
        notificationRepository?.deleteAll()
        recognitionLabelRepository?.deleteAll()
        recognitionLabelPhotoRepository?.deleteAll()
        settingsRepository?.deleteAll()

        val rootPath = FileSystemResource("").file.absolutePath.replace('\\', '/')
        val sidecarDir = File("$rootPath/sidecar_test")
        if (sidecarDir.exists()) {
            //sidecarDir.deleteRecursively()
            purgeDirectory(sidecarDir)
        }
    }

    private fun purgeDirectory(dir: File) {
        for (file in dir.listFiles()!!) {
            if (file.isDirectory) {
                purgeDirectory(file)
            }
            file.delete()
        }
    }
}