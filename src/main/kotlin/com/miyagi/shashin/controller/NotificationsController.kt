package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.util.TextUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.ArrayList
import javax.transaction.Transactional

@Controller
@Secured("ROLE_ADMIN","ROLE_USER")
class NotificationsController {
    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/notifications")
    @Transactional
    fun getNotifications(model: Model): String {
        val module = "notifications"
        model["message"] = "There are no notifications."
        model["notificationList"] = mutableListOf<Notification>()

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

    @GetMapping("/notifications/markread/album/{albumId}", produces = ["application/json"])
    @ResponseBody
    fun markNotificationsReadByAlbum(model: Model,@PathVariable albumId: Int): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && albumId > 0) {
            val notificationList = notificationRepository.findAllByAlbumIdAndUserIdAndMetadataIdIsNullOrderByCreatedAtDesc(albumId,currentUserObj.getId())
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

    @GetMapping("/notifications/markread/metadata/{metadataId}", produces = ["application/json"])
    @ResponseBody
    fun markNotificationsReadByMetadata(model: Model,@PathVariable metadataId: String): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && metadataId.length > 0) {
            val notificationList = notificationRepository.findAllByMetadataIdAndUserIdOrderByCreatedAtDesc(metadataId,currentUserObj.getId())
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

    @GetMapping("/notifications/markread/favorites", produces = ["application/json"])
    @ResponseBody
    fun markNotificationsReadByFavorites(model: Model): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val notificationList = notificationRepository.findAllByUserIdAndFavoriteIdIsNotNull(currentUserObj.getId())
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

    @GetMapping("/notifications/markread/users", produces = ["application/json"])
    @ResponseBody
    fun markNotificationsReadByUsers(model: Model): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val notificationList = notificationRepository.findAllByUserIdAndAlbumIdIsNullAndCommentIdIsNullAndMetadataIdIsNullAndFavoriteIdIsNull(currentUserObj.getId())
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

    @GetMapping("/notifications/check/{userId}", produces = ["application/json"])
    @ResponseBody
    fun checkHasNotifications(@PathVariable userId: Int): String {
        val response = mutableMapOf<String, Any?>()

        response["msg"] = "No results"
        response["status"] = "fail"
        response["hasNotifications"] = false

        if (userId > 0) {
            response["msg"] = "Results"
            response["status"] = "success"

            val notificationCount = notificationRepository.countAllByUserIdAndReadIsFalse(userId)
            response["hasNotifications"] = notificationCount > 0
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/notifications/markread/notification"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun markNotificationsReadByUsers(model: Model, @RequestBody requestBody: JsonNode): String? {
        val notificationIdList = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        if (notificationIdList.containsKey("notificationIds")) {
            val notificationIds = notificationIdList["notificationIds"] as ArrayList<*>

            val notificationObjList = mutableListOf<Notification>()
            for (notificationId in notificationIds) {
                val currentUserObj = model.getAttribute("currentUser") as User?
                if (currentUserObj != null && notificationId != null && notificationId as Int > 0) {
                    val notificationObj = notificationRepository.findById(notificationId)
                    notificationObj.get().setRead(true)
                    notificationObjList.add(notificationObj.get())
                }
            }
            if (notificationObjList.isNotEmpty()) {
                notificationRepository.saveAll(notificationObjList)
            }
        }
        return "{}"
    }
}