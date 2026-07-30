async function setGlobalListeners(darkMode, placeNames, timezone, notificationAlerts, searchHistoryLimit, hasMediaUploadDirectory, autoplayVideo, locale, peopleNames) {
    peopleNames = peopleNames || [];
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

    // Pill state — original-cased names of confirmed @mentions
    const currentPills = new Set();
    // cursorPos: null = inside text input; integer 0..N = virtual cursor among N pills
    //   0 = before all pills, N = after last pill (one step before text input)
    let cursorPos = null;

    function updateCursorIndicator() {
        $("#searchCursorIndicator").remove();
        if (cursorPos === null) {
            $("#appSearchTextInput").css("caret-color", "");
            return;
        }
        $("#appSearchTextInput").css("caret-color", "transparent");
        const pillEls = $("#appSearchPillBox .search-mention-pill");
        if (!pillEls.length) { cursorPos = null; return; }
        const indicator = $('<span id="searchCursorIndicator" aria-hidden="true"></span>');
        if (cursorPos === 0) {
            pillEls.first().before(indicator);
        } else if (cursorPos >= pillEls.length) {
            $("#appSearchTextInput").before(indicator);
        } else {
            pillEls.eq(cursorPos - 1).after(indicator);
        }
    }

    // Remove pill at DOM/Set index without touching cursorPos — caller adjusts
    function deletePillAt(index) {
        const pillEls = $("#appSearchPillBox .search-mention-pill");
        if (index < 0 || index >= pillEls.length) return;
        const name = [...currentPills][index];
        if (name) { currentPills.delete(name); pillEls.eq(index).remove(); }
    }

    function addPill(name) {
        if ([...currentPills].some(n => n.toLowerCase() === name.toLowerCase())) return;
        currentPills.add(name);
        const pill = $('<span class="search-mention-pill"></span>');
        pill.append(document.createTextNode(name));
        const btn = $('<button type="button" class="pill-remove" aria-label="Remove">×</button>');
        btn.on("click", function() {
            currentPills.delete([...currentPills].find(n => n.toLowerCase() === name.toLowerCase()) || name);
            pill.remove();
            cursorPos = null;
            updateCursorIndicator();
            $("#appSearchTextInput").focus();
        });
        pill.append(btn);
        $("#appSearchTextInput").before(pill);
        cursorPos = null;
        updateCursorIndicator();
        $("#appSearchTextInput").focus();
    }

    // Reconstruct full query from pills + free text, set on hidden input, submit
    function buildAndSubmit() {
        const pillPart = [...currentPills].map(n => `@"${n}"`).join(" ");
        const textPart = $("#appSearchTextInput").val().trim();
        const combined = [pillPart, textPart].filter(Boolean).join(" ");
        if (!combined) return;
        $("#appSearchInput").val(combined);
        Util.showSearchSpinner(true);
        $("#appSearchSubmit").click();
    }

    $("#appSearch").on("submit", function(e) {
        e.preventDefault();
        buildAndSubmit();
    });

    // Clicking the pill box outside a pill returns cursor to text input
    $("#appSearchPillBox").on("click", function(e) {
        if (!$(e.target).closest(".search-mention-pill").length) {
            cursorPos = null;
            updateCursorIndicator();
            $("#appSearchTextInput").focus();
        }
    });

    // Keyboard navigation among pills
    $("#appSearchTextInput").on("keydown", function(e) {
        const n = currentPills.size;
        if (e.key === "ArrowLeft") {
            if (cursorPos === null) {
                if (this.selectionStart === 0 && n > 0) {
                    cursorPos = n;
                    updateCursorIndicator();
                    e.preventDefault();
                }
            } else {
                cursorPos = Math.max(0, cursorPos - 1);
                updateCursorIndicator();
                e.preventDefault();
            }
        } else if (e.key === "ArrowRight") {
            if (cursorPos !== null) {
                cursorPos++;
                if (cursorPos > n) cursorPos = n;
                if (cursorPos === n) { cursorPos = null; }
                updateCursorIndicator();
                e.preventDefault();
            }
        } else if (e.key === "Backspace") {
            if (cursorPos === null) {
                if ($(this).val() === "" && n > 0) {
                    deletePillAt(n - 1);
                    updateCursorIndicator();
                    e.preventDefault();
                }
            } else if (cursorPos > 0) {
                deletePillAt(cursorPos - 1);
                cursorPos = Math.min(cursorPos - 1, currentPills.size);
                updateCursorIndicator();
                e.preventDefault();
            } else {
                e.preventDefault();
            }
        } else if (e.key === "Delete") {
            if (cursorPos !== null) {
                if (cursorPos < n) {
                    deletePillAt(cursorPos);
                    cursorPos = Math.min(cursorPos, currentPills.size);
                    updateCursorIndicator();
                }
                e.preventDefault();
            }
        } else if (e.key === "Escape") {
            if (cursorPos !== null) {
                cursorPos = null;
                updateCursorIndicator();
                e.preventDefault();
            }
        } else if (cursorPos !== null && !e.metaKey && !e.ctrlKey && !e.altKey && e.key.length === 1) {
            cursorPos = null;
            updateCursorIndicator();
        }
    });

    // Parse initial term (from server-side ${term}) into pills + free text on page load
    const initialTerm = $("#appSearchInput").val();
    if (initialTerm) {
        [...initialTerm.matchAll(/@"([^"]+)"/g)].forEach(m => addPill(m[1]));
        $("#appSearchTextInput").val(initialTerm.replace(/@"[^"]*"\s*/g, "").trim());
        $("#appSearchInput").val("");
    }

    // Active @mention detection — simplified since text input never contains completed @"Name" tokens
    function getActiveMentionPrefix(value) {
        const m = value.match(/@([^@\s]*)$/);
        if (!m) return null;
        const raw = m[1].startsWith('"') ? m[1].slice(1) : m[1];
        return { start: m.index, prefix: raw };
    }

    const http = new Http("search history");
    const data = await http.ajax("get", "/search/history");

    let searchHistoryData = [];
    if (data.hasOwnProperty("status") && data.status === "success" && data.hasOwnProperty("searchHistoryList")) {
        for (const index in data.searchHistoryList) {
            searchHistoryData.push(data.searchHistoryList[index].term);
        }
    }

    $("#appSearchTextInput").autocomplete({
        minLength: 0,
        source: function(request, response) {
            const val = request.term;
            const mention = getActiveMentionPrefix(val);
            if (mention !== null) {
                const available = peopleNames.filter(n => ![...currentPills].some(p => p.toLowerCase() === n.toLowerCase()));
                const matches = $.ui.autocomplete.filter(available, mention.prefix);
                response(shashin.prefixFirstSort(matches, mention.prefix).slice(0, searchHistoryLimit));
            } else if (val.length > 0) {
                const matches = $.ui.autocomplete.filter(searchHistoryData, val);
                response(shashin.prefixFirstSort(matches, val).slice(0, searchHistoryLimit));
            } else {
                response([]);
            }
        },
        select: function(event, ui) {
            const val = $(this).val();
            const mention = getActiveMentionPrefix(val);
            if (mention !== null) {
                $(this).val(val.slice(0, mention.start).trimEnd());
                addPill(ui.item.value);
            } else {
                $(this).val(ui.item.value);
                buildAndSubmit();
            }
            return false;
        },
        focus: function() { return false; }
    }).focus(function() {
        $(this).autocomplete("search");
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