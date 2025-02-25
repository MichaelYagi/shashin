package com.miyagi.shashin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.miyagi.shashin.model.Notification
import com.miyagi.shashin.model.Settings
import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.MediaDirectoryRepository
import com.miyagi.shashin.repository.MetadataRepository
import com.miyagi.shashin.repository.NotificationRepository
import com.miyagi.shashin.util.ApiResponse
import com.miyagi.shashin.util.FileUtils
import com.miyagi.shashin.util.TextUtils
import com.miyagi.shashin.util.TextUtils.Companion.getCurrentTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.*
import jakarta.transaction.Transactional
import org.apache.commons.text.StringEscapeUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Controller
@Secured("ROLE_SUPER","ROLE_ADMIN","ROLE_USER")
class NotificationsController {
    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private var metadataRepository: MetadataRepository? = null

    val mapper = ObjectMapper()
    val resp = mutableMapOf<String, String?>()

    @GetMapping("/notifications")
    @Transactional
    fun getNotifications(model: Model): String {
        val module = "notifications"
        model["message"] = "Nothing to see here."
        model["notificationList"] = mutableListOf<Notification>()
        model["notificationLimit"] = 0

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val allNotificationList = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserObj.getId())
            if (allNotificationList != null && allNotificationList.count() > 0) {
                model["message"] = ""
                val settings = model.getAttribute("settings") as Settings
                val notificationLimit = settings.getNotificationLimit()
                model["notificationLimit"] = notificationLimit!!

                var notificationList = mutableListOf<Notification?>()
                if (allNotificationList.count() > notificationLimit) {
                    for ((index, notification) in allNotificationList.withIndex()) {
                        if (index > (notificationLimit-1) && notification != null) {
                            notificationRepository.deleteById(notification.getId())
                        } else {
                            notificationList.add(notification!!)
                        }
                    }
                } else {
                    notificationList = allNotificationList.toMutableList()
                }
                model["notificationList"] = notificationList
            }
        }

        model["activePage"] = module
        model["activeSidebar"] = module
        model["titleDescriptor"] = TextUtils.capitalized(module)
        return module
    }

    @RequestMapping(value = ["/notifications/create"], method = [RequestMethod.POST], produces = ["application/json"])
    @ResponseBody
    fun createNotification(model: Model, @RequestBody requestBody: JsonNode): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        val paramMap = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})

        val response = mutableMapOf<String, Any?>()

        response["msg"] = ""
        response["status"] = ApiResponse.FAIL.status

        if (currentUserObj != null && paramMap.containsKey("message") && paramMap.containsKey("type") && paramMap.containsKey("identifier")) {
            val message = paramMap["message"] as String?
            val type = paramMap["type"] as String?
            val identifier = paramMap["identifier"] as String?

            val notification = Notification()
            notification.setType(type)
            notification.setIdentifier(identifier)
            notification.setRead(false)
            notification.setMessage(message)
            notification.setUserId(currentUserObj.getId())
            notification.setCreatedAt(getCurrentTimestamp())
            notification.setModifiedAt(getCurrentTimestamp())
            notificationRepository.save(notification)

            response["msg"] = ""
            response["status"] = ApiResponse.SUCCESS.status
        } else {
            response["msg"] = ""
            response["status"] = ApiResponse.FAIL.status
        }

        return mapper.writeValueAsString(response)
    }

    @GetMapping("/notifications/markread", produces = ["application/json"])
    @ResponseBody
    fun markNotificationsRead(model: Model): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            Thread {
                val notificationList =
                    notificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserObj.getId())
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
            }.start()
        } else {
            return "{\"msg\":\"\",\"status\":\"fail\"}"
        }

        return "{\"msg\":\"\",\"status\":\"success\"}"
    }

    @GetMapping("/notifications/mediascan", produces = ["application/json"])
    @ResponseBody
    fun getMediaScanStatus(model: Model): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            var msg = "complete"

            if (FileUtils.checkThreadFileAlive("shashinscan")) {
                val threadFileContent = FileUtils.readThreadFile("shashinscan")
                if (threadFileContent != null) {
                    msg = "processing"
                }
            }

            return "{\"msg\":\"$msg\",\"status\":\"success\"}"
        }

        return "{\"msg\":\"\",\"status\":\"fail\"}"
    }

    @GetMapping("/notifications/exists/{type}/{identifier}", produces = ["application/json"])
    @ResponseBody
    fun existsNotificationsReadByTypeAndIdentifier(model: Model,@PathVariable type: String,@PathVariable identifier: String): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        var exists = false

        if (currentUserObj != null && identifier.isNotEmpty()) {
            var notificationList: MutableIterable<Notification?>? = notificationRepository.findAllByTypeAndId(
                type,
                identifier,
                currentUserObj.getId()
            )

            if (notificationList != null) {
                if (notificationList.count() > 0) {
                    exists = true
                }
            } else {
                return "{\"msg\":\"\",\"status\":\"fail\"}"
            }
        } else {
            return "{\"msg\":\"\",\"status\":\"fail\"}"
        }

        return "{\"msg\":\"\",\"status\":\"success\",\"exists\":$exists}"
    }

    @GetMapping("/notifications/check/{type}/{identifier}", produces = ["application/json"])
    @ResponseBody
    fun checkNotificationsReadByTypeAndIdentifier(model: Model,@PathVariable type: String,@PathVariable identifier: String): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        var allChecked = true

        if (currentUserObj != null && identifier.isNotEmpty()) {
            var notificationList: MutableIterable<Notification?>? = notificationRepository.findAllByTypeAndId(
                type,
                identifier,
                currentUserObj.getId()
            )

            if (notificationList != null) {
                if (notificationList.count() > 0) {
                    for (notification in notificationList) {
                        if (notification != null) {
                            if (!notification.getRead()!!) {
                                allChecked = false
                                break
                            }
                        }
                    }
                }
            } else {
                return "{\"msg\":\"\",\"status\":\"fail\"}"
            }
        } else {
            return "{\"msg\":\"\",\"status\":\"fail\"}"
        }

        return "{\"msg\":\"\",\"status\":\"success\",\"allChecked\":$allChecked}"
    }

    @GetMapping("/notifications/markread/{type}/{identifier}", produces = ["application/json"])
    @ResponseBody
    fun markNotificationsReadByTypeAndIdentifier(model: Model,@PathVariable type: String,@PathVariable identifier: String): String {
        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null && identifier.isNotEmpty()) {
            Thread {
                var notificationList: MutableIterable<Notification?>? = null

                if (type == "metadata") {
                    notificationList = notificationRepository.findAllByTypeAndId(
                        type,
                        identifier,
                        currentUserObj.getId()
                    )
                }

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
            }.start()
        } else {
            return "{\"msg\":\"\",\"status\":\"fail\"}"
        }

        return "{\"msg\":\"\",\"status\":\"success\"}"
    }

    @GetMapping("/notifications/check", produces = ["application/json"])
    @ResponseBody
    fun checkHasNotifications(model: Model): String {
        val response = mutableMapOf<String, Any?>()

        response["msg"] = "No results"
        response["status"] = ApiResponse.FAIL.status
        response["hasNotifications"] = false
        response["unreadNotifications"] = mutableListOf<Notification>()
        response["uncheckedPersonMatches"] = 0
        response["facialDetection"] = false

        val currentUserObj = model.getAttribute("currentUser") as User?
        if (currentUserObj != null) {
            val settings = model.getAttribute("settings") as Settings
            response["msg"] = "Results"
            response["status"] = ApiResponse.SUCCESS.status

            val unreadNotifications = notificationRepository.findAllByUserIdAndReadIsFalse(currentUserObj.getId())
            response["unreadNotifications"] = unreadNotifications
            var count = 0
            if (unreadNotifications != null) {
                count = unreadNotifications.count()
            }
            response["hasNotifications"] = count > 0
            response["uncheckedPersonMatches"] = metadataRepository?.findAllLowMatches(
                settings.getRecognitionConfidenceThreshold()!!
            )
            response["facialDetection"] = settings.getFacialDetection()
        }

        return mapper.writeValueAsString(response)
    }

    @RequestMapping(value = ["/notifications/markread/notification"], method = [RequestMethod.POST], consumes = ["application/json"], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun markNotificationsReadByUsers(model: Model, @RequestBody requestBody: JsonNode): String? {
        val notificationIdList = mapper.convertValue(requestBody, object : TypeReference<Map<String, Any>>() {})
        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null && notificationIdList.containsKey("notificationIds")) {
            Thread {
                val notificationIds = notificationIdList["notificationIds"] as ArrayList<*>
                val notificationObjList = mutableListOf<Notification>()

                for (notificationId in notificationIds) {
                    if (notificationId != null && notificationId as Int > 0) {
                        val notificationObj = notificationRepository.findById(notificationId)
                        notificationObj.get().setRead(true)
                        notificationObjList.add(notificationObj.get())
                    }
                }
                if (notificationObjList.isNotEmpty()) {
                    notificationRepository.saveAll(notificationObjList)
                }
            }.start()
        } else {
            return "{\"msg\":\"\",\"status\":\"fail\"}"
        }
        return "{\"msg\":\"\",\"status\":\"success\"}"
    }

    @RequestMapping(value = ["/notifications/markallread/notification"], method = [RequestMethod.GET], produces = ["application/json"])
    @ResponseBody
    @Transactional
    fun markAllNotificationsReadByUsers(model: Model): String? {
        val currentUserObj = model.getAttribute("currentUser") as User?

        if (currentUserObj != null) {
            val notificationList = notificationRepository.findAllByUserIdAndReadIsFalse(currentUserObj.getId())

            if (notificationList != null && notificationList.count() > 0) {
                Thread {
                    val notificationObjList = mutableListOf<Notification>()
                    for (notificationObj in notificationList) {
                        if (notificationObj != null) {
                            notificationObj.setRead(true)
                            notificationObjList.add(notificationObj)
                        }
                    }
                    if (notificationObjList.isNotEmpty()) {
                        notificationRepository.saveAll(notificationObjList)
                    }
                }.start()
            }
        } else {
            return "{\"msg\":\"\",\"status\":\"fail\"}"
        }

        return "{\"msg\":\"\",\"status\":\"success\"}"
    }
}