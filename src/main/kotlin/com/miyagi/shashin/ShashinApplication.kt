package com.miyagi.shashin

import jakarta.activation.URLDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationStartingEvent
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ApplicationListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.Taskbar
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit
import javax.swing.*


@SpringBootApplication
@EnableCaching
@EnableScheduling
class ShashinApplication {

	companion object {

		@Autowired
		var jdbcTemplate: JdbcTemplate? = null

		@JvmStatic
		fun main(args: Array<String>) {
			System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true")
			System.setProperty("com.miyagi.shashin.serverStartUnixMS", (System.currentTimeMillis()).toString())
			System.setProperty("java.awt.headless", "false")
			jdbcTemplate?.execute("PRAGMA journal_mode = WAL")
			jdbcTemplate?.execute("PRAGMA synchronous = NORMAL")

			val app = SpringApplication(ShashinApplication::class.java)
			val os = System.getProperty("os.name")

			if (os.lowercase().contains("windows") || os.lowercase().contains("mac")) {
				// Create simple GUI
				val frame = JFrame("Shashin")
				frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
				frame.size = Dimension(400, 100)

				val panel = JPanel(BorderLayout())
				panel.isOpaque = true
				panel.setBounds(30, 30, 100, 100)
				panel.requestFocus()

				val text = JTextArea("Loading Shashin")
				text.isOpaque = false
				text.isEditable = false
				text.margin = Insets(10, 10, 10, 10)

				try {
					val iconimg = ImageIcon(this.javaClass.getResource("/static/images/favicon-32x32.png"))
					frame.iconImage = iconimg.image
					var taskbarimg = ImageIcon(this.javaClass.getResource("/static/images/favicon-256x256.png"))
					val taskbar = Taskbar.getTaskbar()
					// set icon for mac os (and other systems which do support this method)
					taskbar.iconImage = taskbarimg.image
				} catch (e: UnsupportedOperationException) {
					println("The os does not support: 'taskbar.setIconImage'." + e.message)
				} catch (e: SecurityException) {
					println("There was a security exception for: 'taskbar.setIconImage'." + e.message)
				}

				frame.setResizable(false)
				frame.layout = FlowLayout(FlowLayout.LEADING, 3, 3)
				panel.add(text, BorderLayout.CENTER)

				try {
					app.addListeners(ApplicationStartingListener(frame, panel))
					app.run(*args)
				} catch (e: IOException) {
					text.text = "Error: ${e.message}"
				}

				val startPage = "http://127.0.0.1:6624"
				text.text = "Visit $startPage.\r\nClose this window to quit Shashin."
			} else {
				app.run(*args)
			}
		}
	}
}

class ApplicationStartingListener(jFrame: JFrame, jPanel: JPanel) : ApplicationListener<ApplicationStartingEvent> {
	private var jFrame: JFrame? = jFrame
	private var jPanel: JPanel? = jPanel

	override fun onApplicationEvent(event: ApplicationStartingEvent) {
		if (this.jFrame != null) {
			this.jFrame!!.contentPane = this.jPanel
			this.jFrame!!.isVisible = true
		}
	}
}