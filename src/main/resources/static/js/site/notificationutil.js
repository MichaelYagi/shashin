// https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API/Using_the_Notifications_API
class NotificationUtil {
    // notification.createNotification("Test", {
    //     body: "I'm the body",
    //     icon: "/images/android/mipmap-xhdpi/ic_launcher.png",
    //     image: "/api/v1/image/56a91c60-1c9a-38c6-8abc-c86f7879046c"
    // });

    constructor() {
        // Check if the browser supports notifications
        if (!("Notification" in window)) {
            shashin.printMessageToConsole("Notification API not available.", {
                consoleType: shashin.consoleTypes.warn,
                tag: "notifications"
            });
            this.available = false;
            return false;
        }

        this.available = true;
        // granted, denied, default (The user decision is unknown; in this case the application will act as if permission was denied.)
        this.permission = Notification.permission;
        this.notification = null;
    }

    isAvailable() {
        return this.available;
    }

    async requestNotificationPermission() {
        if (Notification.permission !== "granted") {
            this.permission = await Notification.requestPermission();
        }
    }

    getNotificationPermission() {
        this.permission = Notification.permission;
        return this.permission;
    }

    createNotification(title, options) {
        if (options === undefined) {
            options = {};
        }

        if (this.permission === "granted") {
            // Query notification
            this.notification = new Notification(title, options);
        } else {
            if (shashin) {
                shashin.printMessageToConsole("Notification permissions not granted.", {
                    consoleType: shashin.consoleTypes.warn,
                    tag: "notifications"
                });
            }
        }
    }

    closeNotification() {
        if (this.notification !== null) {
            this.notification.close();
        }
    }

    static markNotificationRead() {
        const http = new Http("marking notifications read");
        http.ajax("get", "/notifications/markallread/notification").then(function () {
            NotificationUtil.toggleNotificationBadge();
        });
    }

    static toggleNotificationBadge() {
        // Check all rows and remove badge if all read
        if ($(".notification").length === 0) {
            if ($("#topNavAlertBadge").length > 0) {
                $("#topNavAlertBadge").remove();
            }
            if ($("#sideBarAlertBadge").length > 0) {
                $("#sideBarAlertBadge").remove();
            }
        }
    }
}