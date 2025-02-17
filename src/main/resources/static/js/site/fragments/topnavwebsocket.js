function captureMessages(activePage, notificationAlerts, timezone) {
    // Get updates from media scans
    let stompClient = null;
    let scanInProgress = false;
    let mediaScanCounterSP = 0;

    connectSP();

    $("#scanPhotos").on("click", function (e) {
        e.preventDefault();
        postToScan(false);
    });

    $("#deleteThread").on("click", function (e) {
        e.preventDefault();
        let posting = $.post(
            "/settings/scan",
            {"submit": "", "deleteThread": true, "stopScan": false, "reindexFiles": false}
        );

        posting.done(function (data) {
            if (data.hasOwnProperty("msg")) {
                showMessageSP(data.msg);
            }
        });
    });

    $("#stopScanPhotos").on("click", function (e) {
        e.preventDefault();
        let posting = $.post(
            "/settings/scan",
            {"submit": "", "deleteThread": false, "stopScan": true, "reindexFiles": false}
        );

        posting.done(function (data) {
            if (data.hasOwnProperty("msg")) {
                showMessageSP(data.msg);
                window.top.location = window.top.location;
            }
        });
    });

    function postToScan(deleteThread) {
        let posting = $.post(
            "/settings/scan",
            {
                "submit": "Scan",
                "deleteThread": deleteThread,
                "stopScan": false,
                "reindexFiles": $('#reindexFiles').prop("checked")
            }
        );

        posting.done(function (data) {

            showMessageSP(data.hasOwnProperty("msg") ? data.msg : "");
            if (scanInProgress === false) {
                if ($("#msg").text() === "Start Scan") {
                    connectSP();
                } else {
                    scanRefresh();
                }
            }
        });

        return false;
    }

    function scanRefresh() {
        sendMessageSP();
        let msgVal = $("#scanPhotoMsg").val();
        if (activePage === "scan") {
            msgVal = $("#msg").text();
        }

        if (msgVal === "Scan Stopped" || msgVal === "No directories configured" || msgVal === "Scan Complete") {
            scanInProgress = false;
        } else {
            scanInProgress = true;
            setTimeout(scanRefresh, 1000);
        }
    }

    function connectSP() {
        const socket = new SockJS('/websocket-endpoint');
        stompClient = Stomp.over(socket);
        if (shashin.showDebug === false) {
            stompClient.debug = null;
        }

        shashin.printMessageToConsole("Socket Connecting");

        let counterMessage = 0;
        stompClient.connect({}, function () {
            if (scanInProgress === false) {
                scanRefresh();
            }
            shashin.printMessageToConsole("Connected STOMP client");
            this.subscribe("/topic/messages", function (message) {
                let respMessageJsonString = JSON.parse(message.body).content;
                const messageMap = JSON.parse(respMessageJsonString);
                let respMessage = messageMap.hasOwnProperty("message") ? messageMap.message : "";
                let currentMediaCount = messageMap.hasOwnProperty("currentMediaCount") ? parseInt(messageMap.currentMediaCount) : 0;
                let totalMediaCount = messageMap.hasOwnProperty("totalMediaCount") ? parseInt(messageMap.totalMediaCount) : 0;

                if (activePage !== "map" && totalMediaCount > 0 && currentMediaCount > 0) {
                    $("#progressBarWrapper").visible();
                    let completedPercent = (currentMediaCount / totalMediaCount) * 100;
                    Util.updateProgressBar(completedPercent);
                }

                shashin.printMessageToConsole("message: " + message);
                if (respMessage === "Scan Stopped" || respMessage === "No directories configured" || respMessage === "Scan Complete") {
                    $("#mediaScanSpinner").css("display", "none");
                    $("#progressBarWrapper").invisible();
                    $("#profileImage").css("opacity", 1.0);
                    $("#profileImagePlaceholder").css("opacity", 1.0);
                    Util.updateProgressBar(0);
                    if (counterMessage === 1) {
                        Util.getNotifications(notificationAlerts, timezone);
                    }
                } else {
                    $("#mediaScanSpinner").css("display", "block");
                    $("#profileImage").css("opacity", 0.5);
                    $("#profileImagePlaceholder").css("opacity", 0.5);
                    counterMessage = 0;
                }
                showMessageSP(respMessage);
                counterMessage++;
            });
        }, function (e) {
            shashin.printMessageToConsole("Socket connection error in connectSP(): " + e.toString());

            if (mediaScanCounterSP > 10) {
                showMessageSP("Oops, something went wrong! " + e.toString() + ". Probably already scanning.");
                if (activePage === "scan") {
                    window.top.location = window.top.location;
                }
                mediaScanCounterSP = 0;
            } else {
                showMessageSP("Oops, something went wrong! " + e.toString() + ". Click the Scan button once, to proceed with indexing.");
                disconnectSP();
                connectSP();
            }

            scanInProgress = false;
            mediaScanCounterSP++;
        });
    }

    function disconnectSP() {
        if (stompClient !== null) {
            stompClient.disconnect();
        }
        shashin.printMessageToConsole("Disconnected");
    }

    function sendMessageSP() {
        if (stompClient !== null) {
            stompClient.send("/app/scanmessage", {}, JSON.stringify({'message': "getScanMessage"}));
        } else {
            showMessageSP("Trying to send message but STOMP client is null");
            scanInProgress = false;
        }
    }

    function showMessageSP(message) {
        $("#scanPhotoMsg").val(message);
        if (activePage === "scan") {
            $("#msg").text(message);
        }
    }
}