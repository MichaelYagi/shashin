package com.miyagi.shashin.e2e

import com.miyagi.shashin.repository.*
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import java.io.File
import java.util.logging.Logger
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.Modifying
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
abstract class BaseSeleniumTests {
    protected var driver: WebDriver? = null
    protected var logger: Logger = Logger.getLogger(BaseSeleniumTests::class.simpleName)
    protected var elementScanTimeoutMillis = 5000
    protected var elementWaitSeconds = 60L
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
    private val folderDataRepository: FolderDataRepository? = null

    @Autowired
    private val keywordPhotoRepository: KeywordPhotoRepository? = null

    @Autowired
    private val keywordRepository: KeywordRepository? = null

    @Autowired
    private val searchHistoryRepository: SearchHistoryRepository? = null

    @Autowired
    private val searchRepository: SearchRepository? = null

    @Autowired
    private val useragentRepository: UseragentRepository? = null

    @Transactional
    @BeforeEach
    open fun setUp() {
        deleteRecords()

//        println(os)
        WebDriverManager.chromedriver().browserVersion("128.0.6613.137").setup()
//        val capabilities = DesiredCapabilities.chrome()
        val options = ChromeOptions()
        options.addArguments("--no-sandbox") // Bypass OS security model, MUST BE THE VERY FIRST OPTION
        options.addArguments("--disable-setuid-sandbox")
        options.addArguments("--headless")
//        options.setExperimentalOption("useAutomationExtension", false)
        options.addArguments("start-maximized") // open Browser in maximized mode
        options.addArguments("--window-size=1400,600")
        options.addArguments("disable-infobars") // disabling infobars
        options.addArguments("--disable-extensions") // disabling extensions
        options.addArguments("--disable-gpu") // applicable to windows os only
        options.addArguments("--disable-dev-shm-usage") // overcome limited resource problems
        options.addArguments("--remote-allow-origins=*")
        options.addArguments("--ignore-ssl-errors=yes")
        options.addArguments("--ignore-certificate-errors")
//        options.merge(capabilities)

        if (os.contains("linux", ignoreCase = true)) {
            // https://storage.googleapis.com/chrome-for-testing-public/127.0.6533.119/linux64/chromedriver-linux64.zip
            // https://storage.googleapis.com/chrome-for-testing-public/127.0.6533.119/linux64/chrome-linux64.zip
//            options.setBinary("/mnt/c/Users/Michael/Downloads/chrome-linux64/chrome")
//            System.setProperty("webdriver.chrome.driver", "/mnt/c/Users/Michael/Downloads/chromedriver-linux64/chromedriver")
            System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
        } else if (os.contains("windows", ignoreCase = true)) {
            // Drivers to run locally
            // https://www.ubuntuupdates.org/package/google_chrome/stable/main/base/google-chrome-stable?id=202706&page=1
            // https://storage.googleapis.com/chrome-for-testing-public/128.0.6613.137/win64/chrome-win64.zip
            // https://storage.googleapis.com/chrome-for-testing-public/128.0.6613.137/win64/chromedriver-win64.zip
            // CircleCI downloads from here:
            // https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_130.0.6723.58-1_amd64.deb

            options.setBinary("C:\\Users\\Michael\\Downloads\\chrome-win64\\chrome-win64\\chrome.exe")
            System.setProperty("webdriver.chrome.driver", "C:\\Users\\Michael\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe")
        }

        driver = ChromeDriver(options)
    }

    @Transactional
    @AfterEach
    open fun tearDown() {
        deleteRecords()

        if (driver != null) {
            driver!!.quit()
        }
    }

    @Modifying
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
        folderDataRepository?.deleteAll()
        keywordPhotoRepository?.deleteAll()
        keywordRepository?.deleteAll()
        searchHistoryRepository?.deleteAll()
        searchRepository?.deleteAll()
        useragentRepository?.deleteAll()

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