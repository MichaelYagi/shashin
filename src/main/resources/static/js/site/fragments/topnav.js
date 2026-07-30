async function setGlobalListeners(darkMode, placeNames, timezone, notificationAlerts, searchHistoryLimit, hasMediaUploadDirectory, autoplayVideo, locale) {
    shashin.darkMode = darkMode;
    shashin.showPlacename = placeNames;
    shashin.autoplayVideo = autoplayVideo;
    // Above used elsewhere - do not add to list

    // Avoids aria-hidden warnings
    document.addEventListener('hide.bs.modal', function (event) {
        if (document.activeElement) {
            document.activeElement.blur();
        }
    });

    const activePage = $("#activePage").val();

    if (activePage !== "notifications") {
        await Util.getNotifications(notificationAlerts, timezone, locale);
    }

    // Disable draggable on html
    if (activePage === "album" || activePage === "recent" || activePage === "modified" || activePage === "taken" || activePage === "accessed") {
        $('body').on('dragstart', function(e) {
            e.preventDefault();
        });
    }

    setTimeout(function () {
        const http = new Http("check compreface status");
        http.ajax("get", "/status/argus").then(function (data) {
            if (data.hasOwnProperty("argusStatus") && data.argusStatus === false) {
                shashin.showToastMessage(shashin.getTranslatedValue("main.pages.dashboard.compreface"), data.message, {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    autohide: false,
                    borderColor: "danger"
                });
            }
        });
    }, 0);

    $("#appSearch").on("submit", function (e) {
        const searchTerm = $("#appSearchInput").val().trim();
        if (searchTerm === "") {
            e.preventDefault();
        } else {
            Util.showSearchSpinner(true);
        }
    });

    // Focus and select search input
    $("#appSearchInput").focus(function () {
        $(this).select();
    });

    let peopleNames = [];
    const peopleHttp = new Http("people names");
    peopleHttp.ajax("get", "/search/people/names").then(function(pData) {
        if (pData.status === "success" && Array.isArray(pData.names)) {
            peopleNames = pData.names;
        }
    });

    // Returns the active @mention prefix if the caret is inside one, null otherwise.
    // An active mention is the last @ not already closed as @"Name".
    function getActiveMentionPrefix(value) {
        const re = /@(?!"[^"]*")/g;
        let lastMatch = null, m;
        while ((m = re.exec(value)) !== null) { lastMatch = m; }
        if (!lastMatch) return null;
        const after = value.slice(lastMatch.index + 1);
        // Strip leading " so @"Al and @Al both match the same names
        const raw = after.startsWith('"') ? after.slice(1) : after;
        if (/\s/.test(raw)) return null;
        return { start: lastMatch.index, prefix: raw };
    }

    const http = new Http("search history");
    const data = await http.ajax("get", "/search/history");

    let searchHistoryData = [];
    if (data.hasOwnProperty("status") && data.status === "success" && data.hasOwnProperty("searchHistoryList")) {
        for (const index in data.searchHistoryList) {
            searchHistoryData.push(data.searchHistoryList[index].term);
        }
    }

    $("#appSearchInput").autocomplete({
        minLength: 1,
        source: function(request, response) {
            const val = request.term;
            const mention = getActiveMentionPrefix(val);
            if (mention !== null) {
                const prefix = mention.prefix.toLowerCase();
                const starts = peopleNames.filter(n => n.toLowerCase().startsWith(prefix));
                const contains = peopleNames.filter(n => !n.toLowerCase().startsWith(prefix) && n.toLowerCase().includes(prefix));
                response(starts.concat(contains).slice(0, searchHistoryLimit));
            } else {
                const filtered = searchHistoryData.filter(t => t.toLowerCase().includes(val.toLowerCase()));
                response(shashin.prefixFirstSort(filtered, val).slice(0, searchHistoryLimit));
            }
        },
        select: function(event, ui) {
            const val = $(this).val();
            const mention = getActiveMentionPrefix(val);
            if (mention !== null) {
                $(this).val(val.slice(0, mention.start) + '@"' + ui.item.value + '" ');
            } else {
                $(this).val(ui.item.value);
                $("#appSearchSubmit").click();
            }
            return false;
        },
        focus: function() { return false; }
    });

    $("#darkModeSwitch").change(async function () {
        let darkMode = this.checked;

        const http = new Http("dark mode");
        const json = {darkMode: darkMode};
        const data = await http.ajax("post", "/users/darkmode", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === "success") {
                shashin.darkMode = darkMode;

                let html = $("html");
                html.hide();
                Util.darkModeToggle(darkMode);
                html.show();
            } else {
                darkMode = darkMode !== true;
                $("#darkModeSwitch").prop("checked", darkMode);
                shashin.darkMode = darkMode;

                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.body") + ": " + data.msg, {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            darkMode = darkMode !== true;
            $("#darkModeSwitch").prop("checked", darkMode);
            shashin.darkMode = darkMode;

            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.body") + ": " + data.msg, {
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
            if (data.status === "success") {
                shashin.printMessageToConsole("Successfully set notification alerts.",{tag:"notifications"});
            } else {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.notifalert.fail.body") + ": " + data.msg, {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.notifalert.fail.body") + ": " + data.msg, {
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
            if (data.status === "success") {
                shashin.showPlacename = showPlacename;
                shashin.printMessageToConsole("Successfully set show place names.");
            } else {
                showPlacename = showPlacename !== true;
                $("#showPlacenameSwitch").prop("checked", showPlacename);
                shashin.showPlacename = showPlacename;

                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.placename.fail.body") + ": " + data.msg, {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            showPlacename = showPlacename !== true;
            $("#showPlacenameSwitch").prop("checked", showPlacename);
            shashin.showPlacename = showPlacename;

            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.placename.fail.body") + ": " + data.msg, {
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
            if (data.status === "success") {
                shashin.autoplayVideo = autoplayVideo;
                shashin.printMessageToConsole("Successfully set autoplay.");
            } else {
                autoplayVideo = autoplayVideo !== true;
                $("#autoplayVideoSwitch").prop("checked", autoplayVideo);
                shashin.autoplayVideo = autoplayVideo;

                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.autoplay.fail.body") + ": " + data.msg, {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            autoplayVideo = autoplayVideo !== true;
            $("#autoplayVideoSwitch").attr("checked", autoplayVideo);
            shashin.autoplayVideo = autoplayVideo;

            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.topnav.darkmode.fail.title"), shashin.getTranslatedValue("main.toast.topnav.autoplay.fail.body") + ": " + data.msg, {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });

    if (activePage === "settings") {
        $('body').off("click").on("click", function () {
            shashin.closeToastMessages();
        });
    }

    if (activePage !== "timeline" && activePage !== "wake") {
        captureMessages(activePage, notificationAlerts, timezone, locale);
    }

    if (hasMediaUploadDirectory === true && $("#dummyframe").length > 0) {
        initializeUploads(activePage);
    }
}