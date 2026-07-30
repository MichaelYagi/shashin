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
    // selectedPillIndex: null = text input active, 0..N-1 = which pill is highlighted
    let selectedPillIndex = null;

    function setSelectedPill(index) {
        $("#appSearchPillBox .search-mention-pill").removeClass("pill-selected");
        if (index === null || currentPills.size === 0) {
            selectedPillIndex = null;
            return;
        }
        selectedPillIndex = Math.max(0, Math.min(index, currentPills.size - 1));
        $("#appSearchPillBox .search-mention-pill").eq(selectedPillIndex).addClass("pill-selected");
    }

    // Remove pill at DOM/Set index — caller updates selectedPillIndex afterward
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
            setSelectedPill(null);
            $("#appSearchTextInput").focus();
        });
        pill.append(btn);
        $("#appSearchTextInput").before(pill);
        setSelectedPill(null);
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
        document.getElementById("appSearch").submit();
    }

    $("#appSearch").on("submit", function(e) {
        e.preventDefault();
        buildAndSubmit();
    });

    // Clicking the pill box outside a pill returns focus to text input
    $("#appSearchPillBox").on("click", function(e) {
        if (!$(e.target).closest(".search-mention-pill").length) {
            setSelectedPill(null);
            $("#appSearchTextInput").focus();
        }
    });

    // Keyboard navigation among pills
    $("#appSearchTextInput").on("keydown", function(e) {
        const n = currentPills.size;
        if (e.key === "ArrowLeft") {
            if (selectedPillIndex === null) {
                if (this.selectionStart === 0 && n > 0) {
                    setSelectedPill(n - 1);
                    e.preventDefault();
                }
            } else {
                setSelectedPill(selectedPillIndex - 1);
                e.preventDefault();
            }
        } else if (e.key === "ArrowRight") {
            if (selectedPillIndex !== null) {
                if (selectedPillIndex >= n - 1) {
                    setSelectedPill(null);
                } else {
                    setSelectedPill(selectedPillIndex + 1);
                }
                e.preventDefault();
            }
        } else if (e.key === "Backspace" || e.key === "Delete") {
            if (selectedPillIndex !== null) {
                const idx = selectedPillIndex;
                deletePillAt(idx);
                setSelectedPill(currentPills.size > 0 ? Math.min(idx, currentPills.size - 1) : null);
                e.preventDefault();
            } else if (e.key === "Backspace" && $(this).val() === "" && n > 0) {
                deletePillAt(n - 1);
                setSelectedPill(null);
                e.preventDefault();
            }
        } else if (e.key === "Escape") {
            if (selectedPillIndex !== null) {
                setSelectedPill(null);
                e.preventDefault();
            }
        } else if (selectedPillIndex !== null && !e.metaKey && !e.ctrlKey && !e.altKey && e.key.length === 1) {
            setSelectedPill(null);
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