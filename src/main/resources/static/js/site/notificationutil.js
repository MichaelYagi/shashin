// https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API/Using_the_Notifications_API
class NotificationUtil {
    constructor() {
        // Check if the browser supports notifications
        if (!("Notification" in window)) {
            return;
        }

        this.permission = "default";
        this.notification = null;
    }

    askNotificationPermission() {
        Notification.requestPermission().then((permission) => {
            this.permission = permission;
        });
    }

    createNotification(title, options) {
        let settings = {};

        if (options === undefined) {
            options = {};
        }

        if (options.hasOwnProperty("message")) {
            settings["body"] = options["message"];
        }

        if (options.hasOwnProperty("imageUrl")) {
            settings["icon"] = options["imageUrl"];
        }

        if (options.hasOwnProperty("id")) {
            settings["tag"] = options["id"];
        }

        if (this.permission === "granted") {
            // Query notification
            this.notification = new Notification(title, settings);

            // Get unread

            // Loop through messages
        } else {
            if (shashin) {
                shashin.printMessageToConsole("Notification permissions not granted.");
            }
        }
    }

    closeNotification() {
        if (this.notification !== null) {
            this.notification.close();
        }
    }
}