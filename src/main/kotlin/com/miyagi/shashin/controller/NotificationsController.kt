package com.miyagi.shashin.controller

import com.miyagi.shashin.model.Metadata
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.FavoriteRepository
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.ArrayList
import javax.transaction.Transactional

@Controller
class NotificationsController {
    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @GetMapping("/notifications")
    @Transactional
    fun getNotifications(model: Model): String {
        val module = "notifications"
        model["message"] = "There are no notifications."
        model["notificationList"] = ""

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val notificationList = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserObj.getId())
            if (notificationList != null && notificationList.count() > 0) {
                model["message"] = ""
                val settings = model.getAttribute("settings") as Settings
                val notificationLimit = settings.getNotificationLimit()

                if (notificationList.count() > notificationLimit!!) {
                    // Delete last entry
                    val lastEntry = notificationList.last()
                    if (lastEntry != null) {
                        notificationRepository.deleteById(lastEntry.getId())
                    }
                }
                model["notificationList"] = notificationList
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @GetMapping("/notifications/markread", produces = ["application/json"])
    @ResponseBody
    fun markNotificationsRead(model: Model): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val notificationList = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserObj.getId())
            if (notificationList != null && notificationList.count() > 0) {
                val notifications = mutableListOf<Notification>()
                for (notification in notificationList) {
                    if (notification != null) {
                        if (!notification.getRead()!!) {
                            notification.setRead(true)
                            notifications.add(notification)
                        }
                    }
                }
                if (notifications.isNotEmpty()) {
                    notificationRepository.saveAll(notifications)
                }
            }
        }

        return "{}"
    }
}