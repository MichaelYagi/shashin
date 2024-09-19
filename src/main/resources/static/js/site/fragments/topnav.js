$(window).bind("load", function () {
    $("header .placeholder").removeClass("placeholder");
});

let activePageVar = '';
let timezoneVar = "America/Los_Angeles";
let notificationAlertsVar = false;
let searchHistoryLimitVar = 10;
let queryLimitVar = 30;
let accessTimelineViewVar = false;

function setVarsTopnav(darkMode, placeNames, activePage, timezone, notificationAlerts, searchHistoryLimit, queryLimit, accessTimelineView) {
    shashin.darkMode = darkMode;
    shashin.showPlacename = placeNames;
    activePageVar = activePage;
    timezoneVar = timezone;
    notificationAlertsVar = notificationAlerts;
    searchHistoryLimitVar = searchHistoryLimit;
    queryLimitVar = queryLimit;
    accessTimelineViewVar = accessTimelineView;
}

$(document).ready(async function () {
    if (activePageVar !== "notifications") {
        await Util.getNotifications(notificationAlertsVar, timezoneVar);
    }

    if (notificationAlertsVar === true) {
        setTimeout(function () {
            const http = new Http("check compreface status");
            http.ajax("get", "/status/compreface").then(function (data) {
                if (data.hasOwnProperty("status") && data["status"] === false) {
                    shashin.showToastMessage("CompreFace server check failed", "Check CompreFace server connection.", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        autohide: false,
                        target: shashin.toast.target.three,
                        borderColor:"danger"
                    });
                }
            })
        }, 0);
    }

    $("#appSearch").on("submit", function (e) {
        const searchTerm = $("#appSearchInput").val().trim();
        if (searchTerm === "") {
            e.preventDefault();
        }
    })

    const http = new Http("search history");
    const data = await http.ajax("get", "/search/history");

    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data["status"] === "success" && data.hasOwnProperty("searchHistoryList")) {
        const searchHistoryList = data["searchHistoryList"];

        if (searchHistoryList.length > 0) {
            let searchHistoryData = [];
            for (const index in searchHistoryList) {
                const searchHistoryObj = searchHistoryList[index];
                searchHistoryData.push(searchHistoryObj.term)
            }

            /*<![CDATA[*/
            shashin.createAutocomplete("#appSearchInput", searchHistoryData, false, searchHistoryLimitVar, function () {
                $("#appSearchSubmit").click();
            });
            /*]]>*/
        }
    }

    $("#darkModeSwitch").change(async function () {
        const darkMode = this.checked;
        shashin.darkMode = darkMode;
        const http = new Http("dark mode");
        const json = {darkMode: darkMode};
        const data = await http.ajax("post", "/users/darkmode", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                let html = $("html");

                html.hide();
                Util.darkModeToggle(darkMode);
                html.show();
            }
        }
    });

    $("#showNotificationAlertsSwitch").change(async function () {
        const notificationAlertsVar = this.checked;
        const http = new Http("show notificationAlertsVar");
        const json = {notificationAlertsVar: notificationAlertsVar};
        const data = await http.ajax("post", "/users/shownotificationalerts", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                window.location.reload();
            }
        }
    });

    $("#showPlacenameSwitch").change(async function () {
        const activePageVar = $("#activePageVar").val();
        const showPlacename = this.checked;
        shashin.showPlacename = showPlacename;
        const http = new Http("show placename");
        const json = {showPlacename: showPlacename};
        const data = await http.ajax("post", "/users/showplacename", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                if (activePageVar === "timeline") {
                    Util.reinitLightGalleryInstance();
                } else {
                    window.location.reload();
                }
            }
        }
    });

    $("#autoplayVideoSwitch").change(async function () {
        const activePageVar = $("#activePageVar").val();
        const autoplayVideo = this.checked;
        shashin.autoplayVideo = autoplayVideo;
        const http = new Http("autoplay video");
        const json = {autoplayVideo: autoplayVideo};
        const data = await http.ajax("post", "/users/autoplayvideo", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                if (activePageVar === "timeline") {
                    Util.reinitLightGalleryInstance();
                } else {
                    window.location.reload();
                }
            }
        }
    });

    let slideshowIntervalId;
    let slideshowStarted = false;
    let slideshowIsPaused = false;
    let slideshowIsElapsed = 30; // Seconds
    let slideshowCurrentIndex = 0;
    let slideshowMetadataIds = [];
    let slideshowMouseTimer = null;
    let slideshowCursorVisible = true;
    function getSlideshowImage(callback) {
        const http = new Http("show slideshow");

        shashin.printMessageToConsole("slideshowCurrentIndex: " + slideshowCurrentIndex, {tag:"slideshow"});
        shashin.printMessageToConsole("slideshowMetadataIds:", {tag:"slideshow"});
        shashin.printMessageToConsole(slideshowMetadataIds, {tag:"slideshow"});

        if (slideshowMetadataIds.length > 0 && slideshowCurrentIndex >= 0 && slideshowCurrentIndex <= slideshowMetadataIds.length-1) {
            shashin.printMessageToConsole("Looking up " + slideshowMetadataIds[slideshowCurrentIndex], {tag:"slideshow"});
            http.ajax("get", "/media/metadata/"+slideshowMetadataIds[slideshowCurrentIndex]).then(function (data) {
                processSlideData(data, "existing", callback);
            })
        } else {
            shashin.printMessageToConsole("New random image", {tag:"slideshow"});
            http.ajax("get", "/random/image").then(function (data) {
                processSlideData(data, "new", callback);
            })
        }
    }

    function processSlideData(data, type, callback) {
        if (data.hasOwnProperty("metadata") === true &&
            data["metadata"].hasOwnProperty("thumbnailUrlOriginal") === true &&
            data["metadata"]["thumbnailUrlOriginal"] != "" &&
            data.hasOwnProperty("baseUrl") === true &&
            data["baseUrl"] !== ""
        ) {
            $("#mediaSrc").css("display", "block");
            const photoUrl = data["baseUrl"] + data["metadata"]["thumbnailUrlOriginal"];

            const tempImage = new Image();

            tempImage.onload = function() {
                $("#mediaSrc").fadeOut((slideshowStarted === false) ? 0 : 300, function () {
                    $("#mediaSrc").attr("src", photoUrl).fadeIn((slideshowStarted === false) ? 0 : 600);

                    $("#playPause").css({
                        "font-size": "10rem",
                        "color": "#FFFFFF",
                        "z-index": 99999,
                        "max-width": $(window).innerWidth() + 1,
                        "height": "auto",
                        "max-height": $(window).innerHeight() + 1,
                        "position": "absolute",
                        "top": "50%",
                        "left": "50%",
                        "transform": "translate(-50%, -50%)"
                    });

                    $(".centerFit").css({
                        "max-width": $(window).innerWidth() + 1,
                        "height": "auto",
                        "max-height": $(window).innerHeight() + 1,
                        "position": "absolute",
                        "top": "50%",
                        "left": "50%",
                        "transform": "translate(-50%, -50%)"
                    });

                    if (Util.isMobile() === false) {
                        $("#mediaInfo").css({
                            "max-width": ($(window).width() + 1),
                            "font-size": "1.7rem"
                        });
                    } else {
                        $("#mediaInfo").css({
                            "font-size": "1rem"
                        });
                    }

                    if (type === "new") {
                        slideshowMetadataIds.push(data["metadata"]["id"]);
                    }
                    if (slideshowMetadataIds.length > queryLimitVar) {
                        slideshowMetadataIds.splice(0, 1); // At position 0, remove 1
                        slideshowCurrentIndex--;
                    }

                    const takenDateString = data["metadata"]["year"] + "-" + data["metadata"]["month"] + "-" + data["metadata"]["day"];
                    const takenDate = new Date(takenDateString);
                    const options = {weekday: 'long', year: 'numeric', month: 'short', day: 'numeric'};
                    let description = takenDate.toLocaleDateString('en-us', options)

                    if (accessTimelineViewVar === false && data.hasOwnProperty("albumIds") === true && data["albumIds"].hasOwnProperty(0) === true) {
                        description = "<a style='color:#DBE9F4;text-decoration:none;' href='/album/" + data["albumIds"][0] + "' target='_blank'>" + takenDate.toLocaleDateString('en-us', options) + "</a>"
                    } else if (accessTimelineViewVar === true) {
                        description = "<a style='color:#DBE9F4;text-decoration:none;' href='/timeline#" + takenDateString + "' target='_blank'>" + takenDate.toLocaleDateString('en-us', options) + "</a>"
                    }

                    if (data["shortPlaceName"] !== "") {
                        description += " • " + data["shortPlaceName"];
                    }
                    $("#mediaInfo").html(description);

                    slideshowStarted = true;

                    if (callback !== undefined && typeof callback === 'function') {
                        callback(true);
                    }
                });
            }

            tempImage.src = photoUrl;
        }

        if (callback !== undefined && typeof callback === 'function') {
            callback(false);
        }
    }

    function exitSlideshowGallery() {
        if (document.documentElement.exitFullscreen) {
            document.documentElement.exitFullscreen();
        }

        document.body.style.overflow = 'visible';

        $("#slideshowGallery").css({
            "display": "none"
        });

        slideshowCurrentIndex = 0;
        slideshowMetadataIds = [];

        if (slideshowIntervalId) {
            clearInterval(slideshowIntervalId);
            slideshowIntervalId = 0;
        }

        if (slideshowMouseTimer) {
            clearTimeout(slideshowMouseTimer);
        }

        document.body.style.cursor = "default";
        slideshowCursorVisible = true;
        slideshowStarted = false;

        $("#mediaInfo").html("");

        $("#mediaSrc").attr("src", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII");
    }

    $("body").on("dblclick", function( e ) {
        if (Util.isMobile() === true && $("#slideshowGallery").css("display") === "block") {
            if (document.fullscreenElement !== null && document.exitFullscreen) {
                document.exitFullscreen();
            } else {
                exitSlideshowGallery();
            }
        }
    })

    $("body").on("keyup", function( e ) {
        if ($("#slideshowGallery").css("display") === "block") {
            if (e.code === "Escape" || e.keyCode === 27) {
                if (document.fullscreenElement !== null && document.documentElement.exitFullscreen) {
                    document.documentElement.exitFullscreen();
                } else {
                    exitSlideshowGallery();
                }
            }

            if (e.key === " " || e.code === "Space" || e.keyCode === 32) {
                slideshowGalleryPlayPause();
            }

            if (e.keyCode === 39 || e.keyCode === 37) {
                if (slideshowCurrentIndex > 0 && e.keyCode === 37) {
                    slideshowCurrentIndex--;
                } else if (slideshowCurrentIndex <= slideshowMetadataIds.length-1 && e.keyCode === 39) {
                    slideshowCurrentIndex++;
                }

                getSlideshowImage();
                if (slideshowIsPaused === false) {
                    clearInterval(slideshowIntervalId);
                    slideshowIntervalId = window.setInterval(function () {
                        if (slideshowIsPaused === false) {
                            slideshowCurrentIndex++;
                            getSlideshowImage();
                        }
                    }, (slideshowIsElapsed*1000));
                }
            }

            if ((e.code === "f" || e.keyCode === 70) && !document.fullscreenElement) {
                document.documentElement.requestFullscreen();
            }
        }
    });

    Util.detectSwipe("#slideshowGallery", function (direction) {
        if (direction === "up" || direction === "down") {
            if (document.fullscreenElement !== null && document.exitFullscreen) {
                document.exitFullscreen();
            } else {
                exitSlideshowGallery();
            }
        } else if (direction === "left" || direction === "right") {
            if (slideshowCurrentIndex > 0 && direction === "right") {
                slideshowCurrentIndex--;
            } else if (slideshowCurrentIndex <= slideshowMetadataIds.length-1 && direction === "left") {
                slideshowCurrentIndex++;
            }

            getSlideshowImage();
            if (slideshowIsPaused === false) {
                clearInterval(slideshowIntervalId);
                slideshowIntervalId = window.setInterval(function () {
                    if (slideshowIsPaused === false) {
                        slideshowCurrentIndex++;
                        getSlideshowImage();
                    }
                }, (slideshowIsElapsed*1000));
            }
        }
    });

    function disappearCursor() {
        slideshowMouseTimer = null;
        document.body.style.cursor = "none";
        slideshowCursorVisible = false;
    }

    $("body").on("mousemove", function() {
        if (Util.isMobile() === false && $("#slideshowGallery").css("display") === "block") {
            if (slideshowMouseTimer) {
                clearTimeout(slideshowMouseTimer);
            }

            if (slideshowCursorVisible === false) {
                document.body.style.cursor = "default";
                slideshowCursorVisible = true;
            }

            slideshowMouseTimer = setTimeout(disappearCursor, 5000);
        }
    });

    $("#mediaSrc").on("click", function( e ) {
        if ($("#slideshowGallery").css("display") === "block") {
            slideshowGalleryPlayPause();
        }
    });

    function slideshowGalleryPlayPause() {
        $("#playPause").stop(true, true);
        if (slideshowIsPaused === false) {
            $("#playPause").removeClass("bi-play-circle").addClass("bi-pause-circle");
            slideshowIsPaused = true;
            $("#mediaInfo").css({
                "display": "block"
            });
        } else {
            $("#playPause").removeClass("bi-pause-circle").addClass("bi-play-circle");
            slideshowIsPaused = false;
            $("#mediaInfo").css("display", "none");
        }

        $("#playPause").show();
        $("#playPause").fadeOut(3000);
    }

    $("#viewAllSlideshow").on("click", function (e) {
        e.preventDefault();

        document.body.style.overflow = 'hidden';

        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
        }

        slideshowIntervalId = window.setInterval(function() {
            if (slideshowIsPaused === false) {
                slideshowCurrentIndex++;
                getSlideshowImage();
            }
        }, (slideshowIsElapsed*1000));

        getSlideshowImage(function (loaded) {
            if (loaded === true) {
                $("#playPause").show();
                $("#playPause").fadeOut(3000);

                $("#slideshowGallery").css({
                    "width": "101%",
                    "height": "101%",
                    "display": "block",
                    "z-index": 9999,
                    "background-color": "#000000",
                    "line-height": 1,
                    "overflow": "hidden"
                });

                if (Util.isMobile() === false) {
                    $("#mediaInfo").css({
                        "max-width": ($(window).width() + 1),
                        "white-space": "nowrap"
                    });
                }

                $("#playPause").css("display", "block");
            }
        });
    })

    // Clear live toast message on close
    $('#'+shashin.toast.target.default).on('hidden.bs.toast', function () {
        $("#toastIcon").removeClass();
        $("#toastIcon").removeAttr('style');
        $("#toastTitle").text("");
        $("#toastMessage").text("");
    });

    $('#'+shashin.toast.target.one).on('hidden.bs.toast', function () {
        $("#toastIcon1").removeClass();
        $("#toastIcon1").removeAttr('style');
        $("#toastTitle1").text("");
        $("#toastMessage1").text("");
    });

    $('#'+shashin.toast.target.two).on('hidden.bs.toast', function () {
        $("#toastIcon2").removeClass();
        $("#toastIcon2").removeAttr('style');
        $("#toastTitle2").text("");
        $("#toastMessage2").text("");
    });

    $('#'+shashin.toast.target.three).on('hidden.bs.toast', function () {
        $("#toastIcon3").removeClass();
        $("#toastIcon3").removeAttr('style');
        $("#toastTitle3").text("");
        $("#toastMessage3").text("");
    });

    $('#'+shashin.toast.target.four).on('hidden.bs.toast', function () {
        $("#toastIcon4").removeClass();
        $("#toastIcon4").removeAttr('style');
        $("#toastTitle4").text("");
        $("#toastMessage4").text("");
    });

    // Focus and select search input
    $("#appSearchInput").focus(function() { $(this).select(); } );

    // Get updates from media scans
    let stompClient = null;
    let scanInProgress = false;

    if (activePageVar !== "timeline") {
        connectSP();
    }

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
                showMessageSP(data["msg"]);
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
                showMessageSP(data["msg"]);
                window.top.location = window.top.location
            }
        });
    });

    function postToScan(deleteThread) {
        let posting = $.post(
            "/settings/scan",
            {"submit": "Scan", "deleteThread": deleteThread, "stopScan": false, "reindexFiles": $('#reindexFiles').prop("checked")}
        );

        posting.done(function (data) {

            showMessageSP(data.hasOwnProperty("msg") ? data["msg"] : "")
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
        if (activePageVar === "scan") {
            msgVal = $("#msg").text();
        }

        if (msgVal === "Scan Stopped" || msgVal === "No directories configured" || msgVal === "Scan Complete") {
            scanInProgress = false;
        } else {
            scanInProgress = true;
            setTimeout(scanRefresh, 1000);
        }
    }

    let counterSP = 0;
    function connectSP() {
        const socket = new SockJS('/websocket-endpoint');
        stompClient = Stomp.over(socket);
        if (shashin.showDebug === false) {
            stompClient.debug = null
        }

        shashin.printMessageToConsole("Socket Connecting");

        let counterMessage = 0;
        stompClient.connect({}, function() {
            if (scanInProgress === false) {
                scanRefresh();
            }
            shashin.printMessageToConsole( "Connected STOMP client");
            this.subscribe("/topic/messages", function (message) {
                let respMessageJsonString = JSON.parse(message.body).content;
                const messageMap = JSON.parse(respMessageJsonString);
                let respMessage = messageMap.hasOwnProperty("message") ? messageMap["message"] : "";
                let currentMediaCount = messageMap.hasOwnProperty("currentMediaCount") ? parseInt(messageMap["currentMediaCount"]) : 0;
                let totalMediaCount = messageMap.hasOwnProperty("totalMediaCount") ? parseInt(messageMap["totalMediaCount"]) : 0;

                if (activePageVar !== "map" && totalMediaCount > 0 && currentMediaCount > 0) {
                    $("#progressBarWrapper").visible()
                    let completedPercent = (currentMediaCount/totalMediaCount)*100;
                    Util.updateProgressBar(completedPercent);
                }

                shashin.printMessageToConsole("message: "+message);
                if (respMessage === "Scan Stopped" || respMessage === "No directories configured" || respMessage === "Scan Complete") {
                    $("#mediaScanSpinner").css("display", "none");
                    $("#progressBarWrapper").invisible();
                    $("#profileImage").css("opacity", 1.0);
                    $("#profileImagePlaceholder").css("opacity", 1.0);
                    Util.updateProgressBar(0);
                    if (counterMessage === 1) {
                        Util.getNotifications(notificationAlertsVar, timezoneVar);
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
        }, function(e) {
            shashin.printMessageToConsole("Socket connection error in connectSP(): " + e.toString())

            if (counterSP > 10) {
                showMessageSP("Oops, something went wrong! " + e.toString() + ". Probably already scanning.");
                if (activePageVar === "scan") {
                    window.top.location = window.top.location
                }
                counterSP = 0;
            } else {
                showMessageSP("Oops, something went wrong! " + e.toString() + ". Click the Scan button once, to proceed with indexing.");
                disconnectSP();
                connectSP();
            }

            scanInProgress = false;
            counterSP++;
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
            showMessageSP("Trying to send message but STOMP client is null")
            scanInProgress = false;
        }
    }

    function showMessageSP(message) {
        $("#scanPhotoMsg").val(message);
        if (activePageVar === "scan") {
            $("#msg").text(message);
        }
    }
});