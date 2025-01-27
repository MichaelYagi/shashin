$(window).bind("load", function () {
    $("header .placeholder").removeClass("placeholder");
});

async function setVarsTopnav(darkMode, placeNames, timezone, notificationAlerts, searchHistoryLimit, queryLimit, accessTimelineView, hasMediaUploadDirectory, autoplayVideo) {
    shashin.darkMode = darkMode;
    shashin.showPlacename = placeNames;
    shashin.autoplayVideo = autoplayVideo;

    const activePage = $("#activePage").val();

    if (activePage !== "notifications") {
        await Util.getNotifications(notificationAlerts, timezone);
    }

    if (notificationAlerts === true) {
        setTimeout(function () {
            const http = new Http("check compreface status");
            http.ajax("get", "/status/compreface").then(function (data) {
                if (data.hasOwnProperty("status") && data["status"] === false) {
                    shashin.showToastMessage("CompreFace server check failed", "Check CompreFace server connection.", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        autohide: false,
                        target: shashin.toast.target.three,
                        borderColor: "danger"
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

    // Focus and select search input
    $("#appSearchInput").focus(function () {
        $(this).select();
    });

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

            shashin.createAutocomplete("#appSearchInput", searchHistoryData, false, searchHistoryLimit, function () {
                $("#appSearchSubmit").click();
            });
        }
    }

    $("#darkModeSwitch").change(async function () {
        let darkMode = this.checked;

        const http = new Http("dark mode");
        const json = {darkMode: darkMode};
        const data = await http.ajax("post", "/users/darkmode", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                shashin.darkMode = darkMode;

                let html = $("html");
                html.hide();
                Util.darkModeToggle(darkMode);
                html.show();
            } else {
                darkMode = darkMode !== true;
                $("#darkModeSwitch").prop("checked", darkMode);
                shashin.darkMode = darkMode;

                shashin.showToastMessage("Setting failed", "Dark mode setting failed. "+data["msg"], {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            darkMode = darkMode !== true;
            $("#darkModeSwitch").prop("checked", darkMode);
            shashin.darkMode = darkMode;

            shashin.showToastMessage("Setting failed", "Dark mode setting failed. "+data["msg"], {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });

    $("#showNotificationAlertsSwitch").change(async function () {
        const notificationAlerts = this.checked;

        const http = new Http("show notificationAlerts");
        const json = {notificationAlerts: notificationAlerts};
        const data = await http.ajax("post", "/users/shownotificationalerts", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                shashin.printMessageToConsole("Successfully set notification alerts.");
            } else {
                shashin.showToastMessage("Setting failed", "Notification setting failed. "+data["msg"], {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            shashin.showToastMessage("Setting failed", "Notification setting failed. "+data["msg"], {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });

    $("#showPlacenameSwitch").change(async function () {
        let showPlacename = this.checked;

        const http = new Http("show placename");
        const json = {showPlacename: showPlacename};
        const data = await http.ajax("post", "/users/showplacename", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                shashin.showPlacename = showPlacename;
                shashin.printMessageToConsole("Successfully set show place names.");
            } else {
                showPlacename = showPlacename !== true;
                $("#showPlacenameSwitch").prop("checked", showPlacename);
                shashin.showPlacename = showPlacename;

                shashin.showToastMessage("Setting failed", "Show place names setting failed. "+data["msg"], {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            showPlacename = showPlacename !== true;
            $("#showPlacenameSwitch").prop("checked", showPlacename);
            shashin.showPlacename = showPlacename;

            shashin.showToastMessage("Setting failed", "Show place names setting failed. "+data["msg"], {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });

    $("#autoplayVideoSwitch").change(async function () {
        let autoplayVideo = this.checked;
        const http = new Http("autoplay video");
        const json = {autoplayVideo: autoplayVideo};
        const data = await http.ajax("post", "/users/autoplayvideo", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                shashin.autoplayVideo = autoplayVideo;
                shashin.printMessageToConsole("Successfully set autoplay.");
            } else {
                autoplayVideo = autoplayVideo !== true;
                $("#autoplayVideoSwitch").prop("checked", autoplayVideo);
                shashin.autoplayVideo = autoplayVideo;

                shashin.showToastMessage("Setting failed", "Autoplay videos setting failed. "+data["msg"], {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            autoplayVideo = autoplayVideo !== true;
            $("#autoplayVideoSwitch").attr("checked", autoplayVideo);
            shashin.autoplayVideo = autoplayVideo;

            shashin.showToastMessage("Setting failed", "Autoplay videos setting failed.", {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });

    toastClosureListeners();

    initializeSlideshow(accessTimelineView, queryLimit);

    captureMessages(activePage, notificationAlerts, timezone);

    if (hasMediaUploadDirectory === true && (activePage === "recent" || activePage === "modified" || activePage === "taken" || activePage === "accessed" || activePage === "archived" || activePage === "folders" || activePage === "folder" || activePage === "album")) {
        initializeUploads(activePage);
    }
}

function toastClosureListeners() {
    // Clear live toast message on close
    $('#' + shashin.toast.target.default).on('hidden.bs.toast', function () {
        $("#toastIcon").removeClass();
        $("#toastIcon").removeAttr('style');
        $("#toastTitle").text("");
        $("#toastMessage").text("");
    });

    $('#' + shashin.toast.target.one).on('hidden.bs.toast', function () {
        $("#toastIcon1").removeClass();
        $("#toastIcon1").removeAttr('style');
        $("#toastTitle1").text("");
        $("#toastMessage1").text("");
    });

    $('#' + shashin.toast.target.two).on('hidden.bs.toast', function () {
        $("#toastIcon2").removeClass();
        $("#toastIcon2").removeAttr('style');
        $("#toastTitle2").text("");
        $("#toastMessage2").text("");
    });

    $('#' + shashin.toast.target.three).on('hidden.bs.toast', function () {
        $("#toastIcon3").removeClass();
        $("#toastIcon3").removeAttr('style');
        $("#toastTitle3").text("");
        $("#toastMessage3").text("");
    });

    $('#' + shashin.toast.target.four).on('hidden.bs.toast', function () {
        $("#toastIcon4").removeClass();
        $("#toastIcon4").removeAttr('style');
        $("#toastTitle4").text("");
        $("#toastMessage4").text("");
    });
}