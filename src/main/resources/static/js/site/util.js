class Util {

    static konamiCodeListener(callback) {
        let konamiCodePosition = 0;
        const konamiCode = ['up', 'up', 'down', 'down', 'left', 'right', 'left', 'right', 'b', 'a'];
        const konamiAllowedKeys = {
            37: 'left',
            38: 'up',
            39: 'right',
            40: 'down',
            65: 'a',
            66: 'b',
            'ArrowLeft': 'left',
            'ArrowUp': 'up',
            'ArrowRight': 'right',
            'ArrowDown': 'down',
            'KeyA': 'a',
            'KeyB': 'b',
            'a': 'a',
            'b': 'b'
        };

        // add keydown event listener
        document.addEventListener('keydown', function(e) {
            // get the value of the key code from the key map
            const key = konamiAllowedKeys[e.key || e.code || e.which || e.keyCode];
            // get the value of the required key from the konami code
            const requiredKey = konamiCode[konamiCodePosition];
            // compare the key with the required key
            if (key === requiredKey) {

                // move to the next key in the konami code sequence
                konamiCodePosition++;

                // if the last key is reached, activate cheats
                if (konamiCodePosition === konamiCode.length) {
                    konamiCodePosition = 0;
                    callback(true);
                } else {
                    callback(false);
                }
            } else {
                konamiCodePosition = 0;
                callback(false);
            }
        });
    }

    static uglify(activePage) {
        $("body").addClass("uglify");
        $("strong").addClass("blinkify");
        $("h1, h2, h3, h4, h4, h5").addClass("marquee");
        $("input").css("background-color","#9AF444");
        $("textarea").css("background-color","#EB449A");
        $("img").css({"background-color": "#4A412A", "opacity": "0.3"});
        $(".offcanvas," +
            ".modal," +
            ".dropdown-menu," +
            ".card"
        ).css("background-color","#4A412A");

        if (activePage === "dashboard") {
            Util.updateDashboardColors(true);
        }
    }

    static deuglify(activePage) {
        $("body").removeClass("uglify");
        $("strong").removeClass("blinkify");
        $("h1, h2, h3, h4, h4, h5").removeClass("marquee");
        $("input").css("background-color","");
        $("textarea").css("background-color","");
        $("img").css({"background-color": "", "opacity": ""});
        $(".offcanvas," +
            ".modal," +
            ".dropdown-menu," +
            ".card"
        ).css("background-color","");

        if (activePage === "dashboard") {
            Util.updateDashboardColors(false);
        }
    }

    static updateDashboardColors(uglify) {
        let bgColor = [
            'rgba(54, 162, 235, 0.2)',
            'rgba(255, 206, 86, 0.2)',
            'rgba(153, 102, 255, 0.2)',
            'rgba(255, 159, 64, 0.2)'
        ];

        let bColor = [
            'rgba(54, 162, 235, 1)',
            'rgba(255, 206, 86, 1)',
            'rgba(153, 102, 255, 1)',
            'rgba(255, 159, 64, 1)'
        ];

        if (uglify === true) {
            bgColor = [
                'rgba(93, 73, 9, 0.2)',
                'rgba(90, 74, 19, 0.2)',
                'rgba(53, 39, 4, 0.2)',
                'rgba(112, 101, 57, 0.2)'
            ];

            bColor = [
                'rgba(93, 73, 9, 1)',
                'rgba(90, 74, 19, 1)',
                'rgba(53, 39, 4, 1)',
                'rgba(112, 101, 57, 1)'
            ];
        }

        const siteStatChart = Chart.getChart('siteStatChart');
        siteStatChart.data.datasets[0].backgroundColor = bgColor;
        siteStatChart.data.datasets[0].borderColor = bColor;
        siteStatChart.update();

        const mediaChart = Chart.getChart('mediaChart');
        mediaChart.data.datasets[0].backgroundColor = bgColor;
        mediaChart.data.datasets[0].borderColor = bColor;
        mediaChart.update();

        const cameraChart = Chart.getChart('cameraChart');
        cameraChart.data.datasets[0].backgroundColor = (uglify === true ? ['rgb(93, 73, 9)'] : ['rgba(54, 162, 235, 1)']);
        cameraChart.update();

        const userChart = Chart.getChart('userChart');
        let userColors = [];
        let userBorders = [];
        const colorSelection = [
            ['rgba(90, 74, 19,0.8)','rgba(90, 74, 19,1)'],
            ['rgba(53, 39, 4,0.8)','rgba(53, 39, 4,1)'],
            ['rgba(112, 101, 57,0.8)','rgba(112, 101, 57,1)'],
            ['rgba(93, 73, 9,0.8)','rgba(93, 73, 9,1)'],
            ['rgba(92, 96, 37,0.8)','rgba(92, 96, 37,1)'],
            ['rgba(98, 70, 5, 0.8)','rgba(98, 70, 5, 1)'],
            ['rgba(149, 116, 9,0.8)','rgba(149, 116, 9,1)'],
            ['rgba(102,112,0,0.8)','rgba(102,112,0,1)'],
            ['rgba(113,97,14,0.8)','rgba(113,97,14,1)'],
            ['rgba(137,112,112,0.8)','rgba(137,112,112,1)'],
            ['rgba(95,76,35,0.8)','rgba(95,76,35,1)'],
            ['rgba(152,127,36,0.8)','rgba(152,127,36,1)']
        ];
        for (let i = 0; i < userChart.data.datasets[0].data.length; i++) {
            let colors = Dashboard.randomPastelRgba();
            if (uglify === true) {
                colors = colorSelection[Dashboard.getRandomInt(0,colorSelection.length-1)];
            }
            userColors.push(colors[0]);
            userBorders.push(colors[1]);
        }
        userChart.data.datasets[0].backgroundColor = userColors;
        userChart.data.datasets[0].borderColor = userBorders;
        userChart.update();

        const browserChart = Chart.getChart('browserChart');
        userColors = [];
        userBorders = [];
        for (let i = 0; i < browserChart.data.datasets[0].data.length; i++) {
            let colors = Dashboard.randomPastelRgba();
            if (uglify === true) {
                colors = colorSelection[Dashboard.getRandomInt(0,colorSelection.length-1)];
            }
            userColors.push(colors[0]);
            userBorders.push(colors[1]);
        }
        browserChart.data.datasets[0].backgroundColor = userColors;
        browserChart.data.datasets[0].borderColor = userBorders;
        browserChart.update();

        const osChart = Chart.getChart('osChart');
        userColors = [];
        userBorders = [];
        for (let i = 0; i < osChart.data.datasets[0].data.length; i++) {
            let colors = Dashboard.randomPastelRgba();
            if (uglify === true) {
                colors = colorSelection[Dashboard.getRandomInt(0,colorSelection.length-1)];
            }
            userColors.push(colors[0]);
            userBorders.push(colors[1]);
        }
        osChart.data.datasets[0].backgroundColor = userColors;
        osChart.data.datasets[0].borderColor = userBorders;
        osChart.update();

        const keywordChart = Chart.getChart('keywordChart');
        keywordChart.data.datasets[0].backgroundColor = (uglify === true ? ['rgb(93, 73, 9)'] : ['rgba(54, 162, 235, 1)']);
        keywordChart.update();

        const cpuChart = Chart.getChart('cpuChart');
        cpuChart.data.datasets[0].borderColor = (uglify === true ? ['rgb(93, 73, 9)'] : ['rgb(54, 162, 235)']);
        cpuChart.data.datasets[1].borderColor = (uglify === true ? ['rgb(53, 39, 4)'] : ['rgb(255, 206, 86)']);
        cpuChart.update();

        const memoryChart = Chart.getChart('memoryChart');
        memoryChart.data.datasets[0].borderColor = (uglify === true ? ['rgb(93, 73, 9)'] : ['rgba(54, 162, 235, 1)']);
        memoryChart.update();
    }

    static getYearCreated() {
        return 2021;
    }

    static getEpochCreated() {
        return 1628560260000;
    }

    static lgApiKey() {
        return "A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3";
    }

    static downloadLatestVersion(currVersion, latestVersion) {
        // If has whitespace
        if (/\s/g.test(currVersion) === true || /\s/g.test(latestVersion) === true) {
            return false;
        }

        if (currVersion.charAt(0) === 'v') {
            currVersion = currVersion.substring(1);
        }

        if (latestVersion.charAt(0) === 'v') {
            latestVersion = latestVersion.substring(1);
        }

        const currVersionArray = currVersion.split(".");
        const latestVersionArray = latestVersion.split(".");

        if (currVersionArray.length === 3 && latestVersionArray.length === 3 &&
            currVersionArray[0].trim() !== "" && currVersionArray[1].trim() !== "" && currVersionArray[2].trim() !== "" &&
            latestVersionArray[0].trim() !== "" && latestVersionArray[1].trim() !== "" && latestVersionArray[2].trim() !== "" &&
            isNaN(currVersionArray[0]) === false && isNaN(currVersionArray[1]) === false && isNaN(currVersionArray[2]) === false &&
            isNaN(latestVersionArray[0]) === false && isNaN(latestVersionArray[1]) === false && isNaN(latestVersionArray[2]) === false)
        {
            if (parseInt(latestVersionArray[0]) === parseInt(currVersionArray[0]) &&
                parseInt(latestVersionArray[1]) === parseInt(currVersionArray[1]) &&
                parseInt(latestVersionArray[2]) === parseInt(currVersionArray[2])
            ) {
                return false;
            }

            // Compare major, minor, patch
            if (parseInt(latestVersionArray[0]) > parseInt(currVersionArray[0])) {
                return true;
            } else if (parseInt(latestVersionArray[0]) < parseInt(currVersionArray[0])) {
                return false;
            }

            if (parseInt(latestVersionArray[1]) > parseInt(currVersionArray[1])) {
                return true;
            } else if (parseInt(latestVersionArray[1]) < parseInt(currVersionArray[1])) {
                return false;
            }

            if (parseInt(latestVersionArray[2]) > parseInt(currVersionArray[2])) {
                return true;
            } else if (parseInt(latestVersionArray[2]) < parseInt(currVersionArray[2])) {
                return false;
            }
        }

        return false;
    }

    static detectSwipe(element, callback) {
        function handleTouchStart(evt) {
            const firstTouch = getTouches(evt)[0];
            shashin.slideshowXDown = firstTouch.clientX;
            shashin.slideshowYDown = firstTouch.clientY;
        }

        function getTouches(evt) {
            return evt.touches ||          // browser API
                evt.originalEvent.touches; // jQuery
        }

        $(element).on('touchstart', e => {
            handleTouchStart(e);
        });

        $(element).on('touchmove', e => {
            handleTouchMove(e, callback);
        });

        function handleTouchMove(evt, callback) {
            if (!shashin.slideshowXDown || !shashin.slideshowYDown) {
                if (callback !== undefined && typeof callback === 'function') {
                    callback(null);
                }
                return;
            }

            const xUp = evt.touches[0].clientX;
            const yUp = evt.touches[0].clientY;

            const xDiff = shashin.slideshowXDown - xUp;
            const yDiff = shashin.slideshowYDown - yUp;

            if (Math.abs(xDiff) > Math.abs(yDiff)) {
                if (xDiff > 0) {
                    if (callback !== undefined && typeof callback === 'function') {
                        callback("left");
                    }
                } else {
                    if (callback !== undefined && typeof callback === 'function') {
                        callback("right");
                    }
                }
            } else {
                if (yDiff > 0) {
                    if (callback !== undefined && typeof callback === 'function') {
                        callback("up");
                    }
                } else {
                    if (callback !== undefined && typeof callback === 'function') {
                        callback("down");
                    }
                }
            }

            // Reset values
            shashin.slideshowXDown = null;
            shashin.slideshowYDown = null;
        }
    }

    static scrollToTopListener(elToScroll) {
        const scrollToTopButton = $("#btn-back-to-top");
        const showScrollToTop = function (scrollEl) {
            if (scrollToTopButton.length > 0) {
                if (scrollEl.scrollTop() > 0) {
                    scrollToTopButton.css("display", "block");
                } else {
                    scrollToTopButton.css("display", "none");
                }
            }
        };

        $(elToScroll).on('scroll', function () {
            showScrollToTop($(elToScroll));
        });

        if (scrollToTopButton.length > 0) {
            scrollToTopButton.on("click", function () {
                $(elToScroll)[0].scrollTo({top: 0, behavior: 'smooth'});
            });
        }
    }

    static isInViewport(element) {
        if (element.length > 0) {
            const header = $('header');
            const elementTop = element.offset().top;
            const elementBottom = elementTop + element.outerHeight();
            const viewportTop = header.outerHeight()-$(window).scrollTop();
            const viewportBottom = viewportTop + ($(window).height()-header.outerHeight());

            return elementBottom > viewportTop && elementTop < viewportBottom;
        }

        return false;
    }

    static copyToClipboard(textToCopy, callback) {
        if (!navigator.clipboard) {
            shashin.printMessageToConsole("copyToClipboard using execCommand",{tag:"clipboard"});
            const textArea = document.createElement("textarea");
            textArea.value = textToCopy;
            textArea.innerText = textToCopy;

            // Set non-editable to avoid focus and move outside of view
            textArea.setAttribute('readonly', '');
            textArea.style = {position: 'absolute', left: '-9999px'};

            document.body.appendChild(textArea);
            textArea.select();
            textArea.focus();

            try {
                const successful = document.execCommand('copy');
                textArea.remove();

                let status = false;

                if (successful === false) {
                    shashin.showToastMessage("Could not copy", textToCopy + " could not be copied", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor:"danger",
                        tag: "clipboard"
                    });
                } else {
                    status = true;

                    shashin.showToastMessage("Copied to clipboard", textToCopy + " copied to clipboard", {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        tag: "clipboard"
                    });
                }

                if (callback !== undefined) {
                    callback(status);
                }
            } catch (err) {
                shashin.showToastMessage("Could not copy", textToCopy + " could not be copied: " + err, {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });

                if (callback !== undefined) {
                    callback(false);
                }
            }
        } else {
            shashin.printMessageToConsole("copyToClipboard using navigator.clipboard",{tag:"clipboard"});
            navigator.clipboard.writeText(textToCopy).then(function () {
                shashin.showToastMessage("Copied to clipboard", textToCopy + " copied to clipboard", {
                    icon: "bi-info-circle",
                    iconColor: "#777777"
                });

                if (callback !== undefined) {
                    callback(true);
                }
            }).catch(function (err) {
                shashin.showToastMessage("Could not copy", textToCopy + " could not be copied: " + err, {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });

                if (callback !== undefined) {
                    callback(false);
                }
            });
        }
    }

    static elementsInViewport(element) {
        const elementsArray = [];
        if (element.length > 0) {
            element.each(function () {
                if (Util.isInViewport($(this))) {
                    elementsArray.push(this);
                }
            });
        }

        return $(elementsArray);
    }

    static elementsNotInViewport(element) {
        const elementsArray = [];
        if (element.length > 0) {
            element.each(function () {
                if (!Util.isInViewport($(this))) {
                    elementsArray.push(this);
                }
            });
        }

        return $(elementsArray);
    }

    static isOverlap(div1, div2) {
        if (div1.length > 0 && div2.length > 0) {
            const x1 = div1.offset().left;
            const y1 = div1.offset().top;
            const h1 = div1.outerHeight(true);
            const w1 = div1.outerWidth(true);
            const b1 = y1 + h1;
            const r1 = x1 + w1;
            const x2 = div2.offset().left;
            const y2 = div2.offset().top;
            const h2 = div2.outerHeight(true);
            const w2 = div2.outerWidth(true);
            const b2 = y2 + h2;
            const r2 = x2 + w2;

            return !(b1 < y2 || y1 > b2 || r1 < x2 || x1 > r2);

        } else {
            return false;
        }
    }

    static genericFunction(path) {
        return [window].concat(path.split('.')).reduce(function(prev, curr) {
            return prev[curr];
        });
    }

    static escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    static htmlDecode(input){
        const e = document.createElement('textarea');
        e.innerHTML = input;
        // handle case of empty input
        return e.childNodes.length === 0 ? "" : e.childNodes[0].nodeValue;
    }

    static thumbnailHeight() {
        return 225;
    }

    static setMetadataLocalStorage(date) {
        if (Util.localStorageAvailable() === true) {
            if (typeof date !== "undefined") {
                let json = {};
                if (localStorage.getItem("metadataDateVersion") !== null && localStorage.getItem("metadataDateVersion").length > 0) {
                    json = JSON.parse(localStorage.getItem("metadataDateVersion"));
                    if (typeof date === "undefined") {
                        localStorage.removeItem("metadataDateVersion");
                    } else {
                        json[date] = uuidv4();
                    }

                    if (Object.keys(json).length > 0) {
                        localStorage.setItem("metadataDateVersion", JSON.stringify(json));
                    }
                } else {
                    let json = {};

                    if (typeof date !== "undefined") {
                        json[date] = uuidv4();
                    }

                    localStorage.setItem("metadataDateVersion", JSON.stringify(json));
                }
            } else {
                localStorage.setItem("metadataVersion", uuidv4());
            }
        }
    }

    static removeMetadataLocalStorage(date) {
        if (Util.localStorageAvailable() === true) {
            if (typeof date !== "undefined") {
                let json = {};
                if (localStorage.getItem("metadataDateVersion") !== null && localStorage.getItem("metadataDateVersion").length > 0) {
                    json = JSON.parse(localStorage.getItem("metadataDateVersion"));
                    if (json.hasOwnProperty(date)) {
                        delete json[date];
                    }
                    localStorage.setItem("metadataDateVersion", JSON.stringify(json));
                }
            } else {
                localStorage.removeItem("metadataVersion");
            }
        }
    }

    static clearMetadataLocalStorage() {
        if (Util.localStorageAvailable() === true) {
            if (localStorage.getItem("metadataDateVersion") !== null && localStorage.getItem("metadataDateVersion").length > 0) {
                localStorage.removeItem("metadataDateVersion");
                localStorage.removeItem("metadataVersion");
            }
        }
    }

    static rescanMetadata(metadataIdArray, modalId) {
        const activePage = $("#activePage").val();
        const http = new Http("rescan batch metadata");
        const version = Util.getMetadataLocalStorage();

        let originalMetadata = {};
        if (metadataIdArray.length === 1) {
            const metadataId = metadataIdArray[0];
            shashin.getMetadata(metadataId).then(function (metadataObj) {
                originalMetadata = metadataObj;
            });
        }

        const json = {
            metadataIdList: metadataIdArray
        };

        let propMetadataModal = null;
        if (modalId === "propMetadata") {
            $("#metadataModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#metadataModalStatus").visible();
            $("#metadataModalStatus").attr("title", "");
            $("#metadataModalCancel").prop('disabled', true);
            $("#saveMetadata").prop("disabled", true);

            // Specify static for a backdrop which doesn’t close the modal when clicked
            propMetadataModal = bootstrap.Modal.getInstance(document.getElementById('propMetadata'));
            propMetadataModal._config.backdrop = 'static';
            propMetadataModal._config.keyboard = false;
        } else if (modalId === "propBatchMetadata") {
            $("#metadataBatchModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#metadataBatchModalStatus").visible();
            $("#metadataBatchModalStatus").attr("title", "");
            $("#metadataBatchModalCancel").prop("disabled", true);
            $("#saveBatchMetadata").prop("disabled", true);

            propMetadataModal = bootstrap.Modal.getInstance(document.getElementById('propBatchMetadata'));
            propMetadataModal._config.backdrop = 'static';
            propMetadataModal._config.keyboard = false;
        }

        http.ajax("post", "/rescan/metadata" + (version === "" ? "" : "?v=" + version), JSON.stringify(json)).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                Util.setMetadataLocalStorage();

                let metadataMap = {};
                let metadataKeys = [];
                if (data.hasOwnProperty("metadataMap")) {
                    metadataMap = data.metadataMap;
                    metadataKeys = Array.from(Object.keys(metadataMap));
                }

                if (metadataIdArray.length === 1 && metadataKeys.length === 1) {
                    // If single value, update fields in timeline view
                    const rescannedMetadata = metadataMap[metadataKeys[0]];
                    let takenDateUpdated = false;

                    if (rescannedMetadata.id !== metadataIdArray[0]) {
                        takenDateUpdated = true;
                        Util.setMetadataLocalStorage();
                    }

                    $("#title").val("");
                    if (rescannedMetadata.title !== null) {
                        $("#title").val(rescannedMetadata.title);
                    }
                    $("#camera").val("");
                    if (rescannedMetadata.camera !== null) {
                        $("#camera").val(rescannedMetadata.camera);
                    }
                    $("#lens").val("");
                    if (rescannedMetadata.lens !== null) {
                        $("#lens").val(rescannedMetadata.lens);
                    }
                    $("#description").val("");
                    if (rescannedMetadata.description !== null) {
                        $("#description").val(rescannedMetadata.description);
                    }
                    $("#duration").val("");
                    if (rescannedMetadata.duration !== null) {
                        $("#duration").val(rescannedMetadata.duration);
                    }
                    $("#yearTaken").val("");
                    if (rescannedMetadata.year !== null) {
                        $("#yearTaken").val(rescannedMetadata.year);
                        Util.setMetadataLocalStorage();
                    }
                    $("#monthTaken").val("");
                    if (rescannedMetadata.month !== null) {
                        $("#monthTaken").val(rescannedMetadata.month);
                        Util.setMetadataLocalStorage();
                    }
                    $("#dayTaken").val("");
                    if (rescannedMetadata.day !== null) {
                        $("#dayTaken").val(rescannedMetadata.day);
                        Util.setMetadataLocalStorage();
                    }
                    if ($.isEmptyObject(originalMetadata) === false &&
                        (parseInt(originalMetadata.year) !== parseInt(rescannedMetadata.year) ||
                        parseInt(originalMetadata.month) !== parseInt(rescannedMetadata.month) ||
                        parseInt(originalMetadata.day) !== parseInt(rescannedMetadata.day)))
                    {
                        takenDateUpdated = true;
                    }
                    $("#timeTaken").val("");
                    if (rescannedMetadata.time !== null) {
                        $("#timeTaken").val(rescannedMetadata.time);
                    }
                    $("#offsetTaken").val("");
                    if (rescannedMetadata.timeZone !== null) {
                        $("#offsetTaken").val(rescannedMetadata.timeZone);
                    }
                    $("#placeName").val("");
                    if (rescannedMetadata.placeName !== null) {
                        $("#placeName").val(rescannedMetadata.placeName);
                    }
                    $("#latlng").val("");
                    if (rescannedMetadata.lat !== null && rescannedMetadata.lng !== null) {
                        $("#latlng").val(rescannedMetadata.lat+","+rescannedMetadata.lng);
                        $("#mapTabNav").show();
                        Util.setMetadataLocalStorage();
                    } else if ($("#generalTabNav").length === 0) {
                        $("#mapTabNav").hide();
                    }
                    if (takenDateUpdated === true) {
                        const dateGalleryRemoved = shashin.removeThumbnail((rescannedMetadata.id !== metadataIdArray[0]) ? metadataIdArray[0] : rescannedMetadata.id);
                        if (activePage === "timeline") {
                            timelineSettings.refreshTimeline($("#mediaTypeFilter").val()).then(function (data) {
                                // If a date section was removed refresh the timeline
                                if (dateGalleryRemoved === true) {
                                    Util.setMetadataLocalStorage();
                                    const elements = Util.elementsInViewport($(".scrollspy"));
                                    let firstElementId = $(elements[0]).attr("id");
                                    let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
                                    timelineSettings.jumpFromTimelineToc(event, firstVisibleId, $("#mediaTypeFilter").val());
                                }
                            });
                        }
                    }
                }

                if (data.status === shashin.apiResponse.SUCCESS) {
                    shashin.showToastMessage("Metadata Rescanned", "Metadata successfully rescanned!", {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        tag: "metadatamodal",
                        borderColor:"success"
                    });
                } else {
                    shashin.showToastMessage("Error Rescanning Metadata", "There was an error rescanning metadata!", {
                        icon:"bi-exclamation-triangle",
                        iconColor:"#FF0000",
                        tag: "metadatamodal",
                        borderColor:"danger"
                    });
                }
            } else {
                shashin.showToastMessage("Error Rescanning Metadata", "There was an error rescanning metadata!", {
                    icon:"bi-exclamation-triangle",
                    iconColor:"#FF0000",
                    tag: "metadatamodal",
                    borderColor:"danger"
                });
            }
            if (propMetadataModal !== null) {
                // Default states
                propMetadataModal._config.backdrop = true;
                propMetadataModal._config.keyboard = true;
            }

            if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
                shashin.getLightGallery().refresh();
            }

            if (modalId === "propMetadata") {
                $("#metadataModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').removeClass('spinner-grow');
                $("#metadataModalStatus").visible();
                $("#metadataModalStatus").attr("title", "");
                $("#metadataModalCancel").prop('disabled', false);
                $("#saveMetadata").prop("disabled", false);

                $("#propMetadata").modal('hide');
            } else if (modalId === "propBatchMetadata") {
                $("#metadataBatchModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').removeClass('spinner-grow');
                $("#metadataBatchModalStatus").visible();
                $("#metadataBatchModalStatus").attr("title", "");
                $("#metadataBatchModalCancel").prop("disabled", false);
                $("#saveBatchMetadata").prop("disabled", false);

                $("#propBatchMetadata").modal('hide');
            }
        });
    }

    static convertMSToDayTime(ms) {
        const dt = new Date(ms);
        const units = {
            'years': (dt.getUTCFullYear() - 1970),
            'months': dt.getUTCMonth(),
            'days': dt.getUTCDate() - 1,
            'hours': dt.getUTCHours(),
            'minutes': dt.getUTCMinutes(),
            'seconds': dt.getUTCSeconds()
        };

        let ymd = ((units.years > 0) ? " " + units.years + " year" + (units.years === 1 ? "" : "s") : "") +
            ((units.months > 0) ? " " + units.months + " month" + (units.months === 1 ? "" : "s") : "") +
            ((units.days > 0) ? " " + units.days + " day" + (units.days === 1 ? "" : "s") : "");
        ymd = ymd.trim();

        const hms = ((units.hours < 10) ? "0" : "") + units.hours + ":" +
            ((units.minutes < 10) ? "0" : "") + units.minutes + ":" +
            ((units.seconds < 10) ? "0" : "") + units.seconds;

        return ((ymd.length === 0) ? "" : ymd + " ") + hms;
    }

    static getMetadataLocalStorage(date) {
        let version = "";
        if (typeof date !== "undefined") {
            if (Util.localStorageAvailable() === true && "metadataDateVersion" in localStorage && localStorage.getItem("metadataDateVersion").length > 0) {
                const json = JSON.parse(localStorage.getItem("metadataDateVersion"));
                if (json.hasOwnProperty(date) && json[date].length > 0) {
                    version = json[date];
                }
            }
        } else {
            if (Util.localStorageAvailable() === true && "metadataVersion" in localStorage && localStorage.getItem("metadataVersion").length > 0) {
                version = localStorage.getItem("metadataVersion");
            } else {
                Util.setMetadataLocalStorage();
                version = localStorage.getItem("metadataVersion");
            }
        }

        return version;
    }

    static localStorageAvailable() {
        if (typeof localStorage !== 'undefined') {
            try {
                localStorage.setItem('feature_test', 'yes');
                if (localStorage.getItem('feature_test') === 'yes') {
                    localStorage.removeItem('feature_test');
                    // localStorage is enabled
                    return true;
                } else {
                    // localStorage is disabled
                    return false;
                }
            } catch(e) {
                // localStorage is disabled
                return false;
            }
        } else {
            // localStorage is not available
            return false;
        }
    }

    static isLocalNetwork(hostname = window.location.hostname) {
        return (
            (['localhost', '127.0.0.1', '', '::1'].includes(hostname)) || 
            (hostname.startsWith('192.168.')) ||
            (hostname.startsWith('10.')) ||
            (hostname.endsWith('.local'))
        );
    }

    static arraysEqual(a, b) {
        if (a === b) return true;
        if (a == null || b == null) return false;
        if (a.length !== b.length) return false;

        // If you don't care about the order of the elements inside
        // the array, you should sort both arrays here.
        // Please note that calling sort on an array will modify that array.
        // you might want to clone your array first.

        for (let i = 0; i < a.length; ++i) {
            if (a[i] !== b[i]) return false;
        }
        return true;
    }

    static isChrome() {
        const isChromium = window.chrome;
        const winNav = window.navigator;
        const vendorName = winNav.vendor;
        const isOpera = typeof window.opr !== "undefined";
        const isIEedge = winNav.userAgent.indexOf("Edg") > -1;
        const isIOSChrome = winNav.userAgent.match("CriOS");

        let isChrome = false;
        if (isIOSChrome ||
            (isChromium !== null &&
                typeof isChromium !== "undefined" &&
                vendorName === "Google Inc." &&
                isOpera === false &&
                isIEedge === false)
        ) {
            isChrome = true;
        }

        return isChrome;
    }

    static getOS() {
        let userAgent = window.navigator.userAgent,
            platform = (window.navigator.hasOwnProperty("userAgentData") && window.navigator.userAgentData.platform) || window.navigator.platform,
            macosPlatforms = ["macintosh", "macintel", "macppc", "mac68k"],
            windowsPlatforms = ["win32", "win64", "windows", "wince"],
            iosPlatforms = ["iphone", "ipad", "ipod"],
            os = "";

        if (platform !== undefined && platform !== null && macosPlatforms.indexOf(platform.toLowerCase()) !== -1) {
            os = "MacOS";
        } else if (platform !== undefined && platform !== null && iosPlatforms.indexOf(platform.toLowerCase()) !== -1) {
            os = "iOS";
        } else if (platform !== undefined && platform !== null && windowsPlatforms.indexOf(platform.toLowerCase()) !== -1) {
            os = "Windows";
        } else if (platform !== undefined && platform !== null && /linux/.test(platform.toLowerCase())) {
            os = "Linux";
        } else if (userAgent !== undefined && userAgent !== null && /android/.test(userAgent.toLowerCase())) {
            os = "Android";
        }

        return os;
    }

    static isSafari() {
        return navigator.vendor && navigator.vendor.indexOf('Apple') > -1 &&
            navigator.userAgent &&
            navigator.userAgent.indexOf('CriOS') == -1 &&
            navigator.userAgent.indexOf('FxiOS') == -1;
    }

    static isFirefox() {
        return navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
    }

    static serializeObject(formElement) {
        const o = {};
        const a = formElement.serializeArray();
        $.each(a, function() {
            if (o[this.name]) {
                if (!o[this.name].push) {
                    o[this.name] = [o[this.name]];
                }
                o[this.name].push(this.value || '');
            } else {
                o[this.name] = this.value || '';
            }
        });
        return o;
    }

    static getShortDay(index) {
        const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        return days[index];
    }

    static getShortMonths(index) {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return months[index];
    }

    static validateMetadataInputs(day, month, year, time, offset, latlng, duration, test=false) {
        if (offset === null ) {
            offset = "";
        }

        const timeValidate = "^(\\d{2}:\\d{2}:\\d{2})$";
        const offsetValidate = "^([+-±](?:2[0-3]|[01][0-9]):[0-5][0-9])$";

        let msg = "";
        if (day !== "" && !(parseInt(day) >= 1 && parseInt(day) <= 31)) {
            msg = "Enter Valid Day. Format: 1-31";
        }

        if (month !== "" && !(parseInt(month) >= 1 && parseInt(month) <= 12)) {
            msg = "Enter Valid Month. Format: 1-12";
        }

        if (year !== "" && !(+year >= 1826 && +year <= new Date().getFullYear())) {
            msg = "Enter Valid Year. Format: 1826-" + (new Date().getFullYear());
        }

        if (time !== "" && !time.match(timeValidate)) {
            msg = "Enter Valid Time. Format: HH:MM:SS";
        }

        if (offset !== "" && !offset.match(offsetValidate)) {
            msg = "Enter Valid Offset";
        }

        if (latlng !== "") {
            latlng = $.trim(latlng);
            const latlngArr = latlng.split(",");

            if (latlngArr.length !== 2 || latlng.split(".").length !== 3 || !Util.isNumericString(latlngArr[0]) || !Util.isNumericString(latlngArr[1])) {
                msg = "Enter Valid Latitude/Longitude. Format: lat,lng";
            }
        }

        if (duration !== "") {
            const isValidWithoutHour = /^([1-5]?[0-9])(:[0-5][0-9])?$/.test(duration);
            const isValidWithHour = /^([0-1]?[0-9]|2[0-4]):([0-5][0-9])(:[0-5][0-9])?$/.test(duration);

            if (isValidWithoutHour === false && isValidWithHour === false) {
                msg = "Enter Valid Duration. Format: 0:00/00:00:00";
            }
        }

        if (msg !== "") {
            if (test === false) {
                // $("#"+msgId).html('<div class="alert alert-danger" role="alert">'+msg+'</div>');
                shashin.showToastMessage("Input errors", msg, {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor:"danger"});
            }
            return false;
        } else {
            return true;
        }

    }

    static getParameterByName(name, url = window.location.href) {
        name = name.replace(/[\[\]]/g, '\\$&');
        const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    static atEndOfPage(element) {
        return (((window.innerHeight + element.scrollTop) * 1.05) >= element.scrollHeight); // compare with scroll position + some give (*1.5)
    }

    static hasScrollBar(containerElement) {
        return containerElement.get(0).scrollHeight > containerElement.get(0).clientHeight;
    }

    static getDateString(year,month,day) {
        if (year !== null && year !== "" &&
            month !== null && month !== "" &&
            day !== null && day !== ""
        ) {
            let date = new Date(month+"/"+day+"/"+year);
            if (date.toString() !== "Invalid Date") {
                let shortMonth = Util.getShortMonths(date.getMonth());
                let adjustedDay = date.getDate();
                let dayOfWeek = Util.getShortDay(date.getDay());
                return dayOfWeek + ", " + shortMonth + " " + adjustedDay + ", " + year;
            }
        }
        return "";
    }

    static getKeyByValue(object, value) {
        return Object.keys(object).find(key => object[key] === value);
    }

    static getDateObject(dateString) {
        if (dateString.indexOf("tail_") >= 0) {
            const idParts = dateString.split("tail_");
            dateString = idParts[1];
        }
        if (typeof dateString !== "undefined" && dateString !== null) {
            const dateStringParts = dateString.split("-");
            if (dateStringParts.length === 3) {
                const year = dateStringParts[0];
                const month = dateStringParts[1];
                const day = dateStringParts[2];

                if (year !== null && year !== "" &&
                    month !== null && month !== "" &&
                    day !== null && day !== ""
                ) {
                    return new Date(month + "/" + day + "/" + year);
                }
            }
        }
        return null;
    }

    static isNumericString(str) {
        if (typeof str != "string") {return false;} // we only process strings!
        return !isNaN(str) && // use type coercion to parse the _entirety_ of the string (`parseFloat` alone does not do this)...
            !isNaN(parseFloat(str)); // ...and ensure strings of whitespace fail
    }

    static decodeHtml(html) {
        const txt = document.createElement("textarea");
        txt.innerHTML = html;
        return txt.value;
    }

    static encodeHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    static stringEscape(s) {
        return s ? s.replace(/\\/g,'\\\\').replace(/\n/g,'\\n').replace(/\t/g,'\\t').replace(/\v/g,'\\v').replace(/'/g,"\\'").replace(/"/g,'\\"').replace(/[\x00-\x1F\x80-\x9F]/g,hex) : s;
        function hex(c) {
            const v = "0" + c.charCodeAt(0).toString(16);
            return "\\x" + v.substring(v.length - 2); }
    }
    
    static checkErrorImage(element) {
        if (element === undefined) {
            element = "img";
        }
        $(element).on('error', function() {
            const width = $(this).attr('width');
            const height = $(this).attr('width');
            let dimensions = "/" + Util.thumbnailHeight();

            if (typeof width !== 'undefined' && width !== false && typeof height !== 'undefined' && height !== false) {
                dimensions = "/"+width+"x"+height;
            }
            const imagePlaceholder = "https://via.placeholder.com"+dimensions+"?text="+encodeURI($(this).attr('src')).replace(";", "%3B");
            $(this).attr("src", imagePlaceholder);
        });
    }

    static activateMetadataListeners(metadataId) {
        if (metadataId.length > 0) {
            $("#image" + metadataId).on('load', function () {
                $(this).css("background-color", "transparent");
            });
        }
    }

    static addKeywordToMetadata(metadata, keywords) {
        if (metadata && metadata.hasOwnProperty("id")) {
            metadata.keywords = keywords;
        }
        return metadata;
    }

    static removeDateGallery(id) {
        $("#br"+id).remove();
        $("#row"+id).remove();
        $("#amp_"+id).remove();
        $("#tail_"+id).remove();
        $("#"+id).remove();
        $("#container_"+id).remove();
    }

    /*
    Options:
    useDestroyMethod - bool default true
    timeoutValue - int default 0
    mediaContentList - list of media content
    selector - default .mediaLink
    refreshContent - default false
     */
    static reinitLightGalleryInstance(options) {
        let timeoutValue = 0;
        if (options === undefined || options === null) {
            options = {};
        }

        if (options.hasOwnProperty("timeoutValue")) {
            timeoutValue = options.timeoutValue;
        }

        if (shashin && shashin.getLightGallery() !== null) {
            let mediaContentList = [];
            if (options.hasOwnProperty("mediaContentList")) {
                mediaContentList = options.mediaContentList;
            } else {
                mediaContentList = shashin.getLightGallery().galleryItems;
            }

            const closeTimeout = shashin.getLightGallery().closeGallery(true);
            setTimeout(() => {
                if (shashin.getLightGallery() !== null) {
                    if (options.hasOwnProperty("useDestroyMethod") && (options.useDestroyMethod === false || options.useDestroyMethod === true)) {
                        if (options.useDestroyMethod === false) {
                            shashin.getLightGallery().destroyModules(true);
                            shashin.getLightGallery().invalidateItems();
                            $(window).off(`.lg.global${shashin.getLightGallery().lgId}`);
                            shashin.getLightGallery().LGel.off('.lg');
                            // https://github.com/sachinchoolur/lightGallery/blob/383d51852657ab44bb8697748c570cf110723f97/src/lightgallery.ts#L2396
                            // Hack because lg.destroy() errors out
                            // when photos appear slower than destroy called, then there's an error
                            try {
                                shashin.getLightGallery().$container.remove();
                            } catch (e) {
                                shashin.printMessageToConsole("Error removing lightGallery instance: " + e.message, {
                                    consoleType: shashin.consoleTypes.error,
                                    tag: "lightgallery"
                                });
                            }
                        } else {
                            shashin.getLightGallery().destroy();
                        }
                    } else {
                        // Default
                        shashin.getLightGallery().destroy();
                    }

                    if (shashin) {
                        shashin.closeToastMessages({tags:["subhtml"]});
                    }

                    shashin.lg = null;

                    setTimeout(() => {
                        let selector = ".mediaLink";
                        if (options.hasOwnProperty("selector") && options.selector !== "") {
                            selector = options.selector;
                        }

                        const lgConfig = {
                            "selector":selector,
                            plugins:[]
                        };
                        if (typeof lgMetadataDetail !== "undefined") {
                            lgConfig.plugins.push(lgMetadataDetail);
                            lgConfig.metadataDetail = true;
                            lgConfig.metadataDetailFun = shashin.openEditMetadataModal;
                        }
                        if (typeof lgVideoThumbnail !== "undefined") {
                            lgConfig.plugins.push(lgVideoThumbnail);
                            lgConfig.videoThumbnail = true;
                            lgConfig.videoThumbnailFun = shashin.processVideoThumbnail;
                        }

                        shashin.setLightGallery(lgConfig);

                        if (options.hasOwnProperty("refreshContent") && options.refreshContent === true && mediaContentList.length > 0 && shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
                            shashin.getLightGallery().refresh(mediaContentList);
                        }

                        $(".bi-play-btn").visible();
                        $(".bi-play-circle").visible();
                        $(".mediaLink").bind('click');
                    }, timeoutValue);
                }
            }, closeTimeout);
        }
    }

    static activateDarkModeListener() {
        let useMatchMedia = false;

        if (window.matchMedia) {
            const query = window.matchMedia('prefers-color-scheme: dark');
            if (query.hasOwnProperty("matches")) {
                useMatchMedia = true;
                Util.darkModeToggle(query.matches === true);
            }
        }

        if (useMatchMedia === false) {
            const dmCookieName = "shashindmcookie";

            $("#darkmodeSwitchContainer").css("display", "block");

            const darkModeCookie = Util.getCookie(dmCookieName);

            Util.darkModeToggle(darkModeCookie !== null && darkModeCookie === "on");

            const mode = Util.getParameterByName("m");

            if (mode === "dark" || (darkModeCookie !== "" && darkModeCookie === "on")) {
                $("#darkmodeSwitch").prop('checked', true);
            }

            $('#darkmodeSwitch').on("click", function () {
                let html = $("html");
                html.hide();

                Util.deleteCookie(dmCookieName, "/");
                if ($("#darkmodeSwitch").is(':checked')) {
                    // (name, value, path, domain, days)
                    Util.setCookie(dmCookieName, "on", "/", null, 365);
                }
                Util.darkModeToggle($("#darkmodeSwitch").is(':checked'));
                html.show();
            });
        }
    }

    // Set things live when toggling dark mode
    static darkModeToggle(darkmodeEnabled) {
        if (darkmodeEnabled === true) {
            $("LINK[href='/css/bootstrap.min.css']").attr("href", "/css/bootstrap-night.min.css");
            $(".lightmodeIcons").addClass("darkmodeIcons").removeClass("lightmodeIcons");
            $(".link-button-lightmode").addClass("link-button-darkmode").removeClass("link-button-lightmode");
            $("#aboutIcon").attr("src", "/images/kamon_transparent_inverted.png");
        } else {
            $("LINK[href='/css/bootstrap-night.min.css']").attr("href", "/css/bootstrap.min.css");
            $(".darkmodeIcons").addClass("lightmodeIcons").removeClass("darkmodeIcons");
            $(".link-button-darkmode").addClass("link-button-lightmode").removeClass("link-button-darkmode");
            $("#aboutIcon").attr("src", "/images/kamon_transparent.png");
        }
    }

    static getCookie(name) {
        const parts = document.cookie.split(name + "=");

        if (parts.length === 2) {
            return parts.pop().split(";").shift();
        }

        return "";
    }

    static setCookie(name, value, path, domain, days) {
        const d = new Date();
        let time = 3600*1000;
        if (days) {
            time = days*24*60*60*1000;
        }
        d.setTime(d.getTime() + (time));
        document.cookie = name + "=" +
            ((value !== "") ? value : "") +
            ((path) ? ";path=" + path : "") +
            ((domain) ? ";domain=" + domain : "") +
            ";expires=" + d.toUTCString();
    }

    static deleteCookie( name, path, domain ) {
        if (Util.getCookie(name)) {
            document.cookie = name + "=" +
                ((path) ? ";path=" + path : "") +
                ((domain) ? ";domain=" + domain : "") +
                ";expires=Thu, 01 Jan 1970 00:00:01 GMT";
        }
    }

    static formatBytes(bytes, decimals = 2) {
        if (+bytes === false) {
            return '0 Bytes';
        }

        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];

        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
    }

    static msToTimeSegments(duration) {
        let milliseconds = Math.floor((duration % 1000) / 100),
            seconds = Math.floor((duration / 1000) % 60),
            minutes = Math.floor((duration / (1000 * 60)) % 60),
            hours = Math.floor((duration / (1000 * 60 * 60)) % 24),
            days = Math.floor(duration / (1000 * 24 * 60 * 60)),
            months = Math.floor(duration / (1000 * 60 * 60 * 24 * 30)),
            years = Math.floor(duration / (1000 * 60 * 60 * 24 * 365));

        return {
            years: years,
            months: months,
            days: days,
            hours: hours,
            minutes: minutes,
            seconds: seconds,
            milliseconds: milliseconds
        };
    }

    static getMessageSubText(createdAt, timezone) {
        const createdAtDate = new Date(new Date(createdAt).toLocaleString("en-US", {timeZone: timezone}));
        const nowDate = new Date(new Date().toLocaleString("en-US", {timeZone: timezone}));
        const elapsedTime = Util.msToTimeSegments(nowDate - createdAtDate);
        let timeLapsed = "0s";
        if (elapsedTime.years > 0) {
            timeLapsed = elapsedTime.years + " year" + (elapsedTime.years === 1 ? "": "s");
        } else if (elapsedTime.months > 0) {
            timeLapsed = elapsedTime.months + " month" + (elapsedTime.months === 1 ? "": "s");
        } else if (elapsedTime.days > 0) {
            timeLapsed = elapsedTime.days + " day" + (elapsedTime.days === 1 ? "": "s");
        } else if (elapsedTime.hours > 0) {
            timeLapsed = elapsedTime.hours + " hour" + (elapsedTime.hours === 1 ? "": "s");
        } else if (elapsedTime.minutes > 0) {
            timeLapsed = elapsedTime.minutes + " minute" + (elapsedTime.minutes === 1 ? "": "s");
        } else if (elapsedTime.seconds > 0) {
            timeLapsed = elapsedTime.seconds + " second" + (elapsedTime.seconds === 1 ? "": "s");
        }
        return "<small class='text-muted'>"+timeLapsed+" ago</small>";
    }

    static getDateGalleryHeight(id) {
        if ($("#br" + id).length === 0 && $("#row" + id).length === 0 && $("#amp_" + id).length === 0 && $("#tail_" + id).length === 0 && $("#" + id).length === 0) {
            return 0;
        }

        // console.log("$(\"#br\" + id).outerHeight(true):" + $("#br" + id).outerHeight(true))
        // console.log("$(\"#row\" + id).outerHeight(true):"+$("#row" + id).outerHeight(true))
        // console.log("$(\"#amp_\" + id).outerHeight(true):"+$("#amp_" + id).outerHeight(true))
        // console.log("$(\"#container_\" + id).outerHeight(true):"+$("#amp_" + id).outerHeight(true))
        // console.log("$(\"#tail_\" + id).outerHeight(true):"+$("#tail_" + id).outerHeight(true))
        // console.log("$(\"#\" + id).outerHeight(true):"+$("#" + id).outerHeight(true))


        return $("#br" + id).outerHeight(true) +
        $("#row" + id).outerHeight(true) +
        $("#" + id).outerHeight(true);

        // $("#amp_" + id).outerHeight(true) +
        // $("#container_" + id).outerHeight(true);
    }

    static getLgFunction(that, functionName) {
        let args = null;
        let fn = null;

        if (that.hasOwnProperty("settings") && that.hasOwnProperty("core")) {
            // Get current slide
            let currentDynamicEl = (that.settings.dynamicEl[that.core.index] && that.settings.dynamicEl[that.core.index].hasOwnProperty(functionName)) ?
                that.settings.dynamicEl[that.core.index] : that.core.galleryItems[that.core.index];

            // Optionally get a function and args and execute when creating lg items dynamically
            if (currentDynamicEl.hasOwnProperty(functionName) && currentDynamicEl.hasOwnProperty("args")) {
                args = currentDynamicEl.args;
                fn = currentDynamicEl[functionName];
            } else if (currentDynamicEl.hasOwnProperty("metadataId") && that.settings.hasOwnProperty(functionName)) { // Get the metadataId and set the function variable
                args = currentDynamicEl.metadataId;
                fn = that.settings[functionName];
            }
        }

        if (typeof fn === 'function' && args !== null && (args.length > 0 || Object.keys(args).length > 0)) {
            return {fn: fn, args: args};
        } else {
            return {fn: null, args: null};
        }
    }

    static updateProgressBar(value, options) {
        let timeout = 0;
        let autoVisibility = true;

        if (options !== undefined) {
            if (options.hasOwnProperty("timeout")) {
                timeout = options.timeout;
            }

            if (options.hasOwnProperty("autoVisibility")) {
                autoVisibility = options.autoVisibility;
            }
        }

        if (autoVisibility === true) {
            if (value === 0 || value === 100) {
                $("#progressBarWrapper").invisible();
            } else {
                $("#progressBarWrapper").visible();
            }
        }

        setTimeout(() => {
            $("#progressBarWrapper").attr("aria-valuenow",value.toString());
            $("#progressBar").css("width",value.toString()+"%");
        }, timeout);
    }

    // Formats to yyyy-mm-dd if input is yyyy-m-d for eg.
    static formatDate(dateString) {
        const dateStringArrayDash = dateString.toString().split("-");
        const dateStringArraySlash = dateString.toString().split("/");
        if (dateStringArrayDash.length === 3) {
            let month = dateStringArrayDash[1];
            if (month.charAt(0) !== "0" && Number.isInteger(parseInt(month)) && month <= 9) {
                month = "0" + month;
            }

            let day = dateStringArrayDash[2];
            if (day.charAt(0) !== "0" && Number.isInteger(parseInt(day)) && day <= 9) {
                day = "0" + day;
            }

            dateString = dateStringArrayDash[0] + "-" + month + "-" + day;
        } else if (dateStringArraySlash.length === 3) {
            let month = dateStringArraySlash[0];
            if (month.charAt(0) !== "0" && Number.isInteger(parseInt(month)) && month <= 9) {
                month = "0" + month;
            }

            let day = dateStringArraySlash[1];
            if (day.charAt(0) !== "0" && Number.isInteger(parseInt(day)) && day <= 9) {
                day = "0" + day;
            }

            dateString = dateStringArraySlash[2] + "-" + month + "-" + day;
        } else {
            return null;
        }

        return dateString;
    }

    static getPlaceholderBackground() {
        return "data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs=";
    }

    // Validates yyy-mm-dd format
    static isValidDate(dateString) {
        if (dateString === "" || dateString === null) {
            return true;
        }

        const regEx = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateString.match(regEx)) {
            return false;
        }
        // Invalid format
        const d = new Date(dateString);
        const dNum = d.getTime();
        if (!dNum && dNum !== 0) {
            return false;
        }
        // NaN value, Invalid date
        return d.toISOString().slice(0, 10) === dateString;
    }

    static isValidLatLon(lat, lon) {
        const regexLat = /^(-?[1-8]?\d(?:\.\d{1,18})?|90(?:\.0{1,18})?)$/;
        const regexLon = /^(-?(?:1[0-7]|[1-9])?\d(?:\.\d{1,18})?|180(?:\.0{1,18})?)$/;

        let validLat = regexLat.test(lat);
        let validLon = regexLon.test(lon);
        return validLat && validLon;
    }

    static formatDateTime(d) {
        if (Object.prototype.toString.call(d) === "[object Date]") {
            // it is a date
            if (isNaN(d)) { // d.getTime() or d.valueOf() will also work
                // date object is not valid
                return null;
            } else {
                // date object is valid
                return new Date(d.getTime() - d.getTimezoneOffset() * -60000);
            }
        } else {
            // not a date object
            return null;
        }
    }

    static getBatchData(batchObj) {
        const jsonData = {};
        jsonData.batchMetadataIds = batchObj.hasOwnProperty("batchMetadataIds") ? JSON.parse(batchObj.batchMetadataIds) : null;
        jsonData.dayTakenBatchData = batchObj.hasOwnProperty("dayTakenBatchData") ? batchObj.dayTakenBatchData : null;
        jsonData.monthTakenBatchData = batchObj.hasOwnProperty("monthTakenBatchData") ? batchObj.monthTakenBatchData : null;
        jsonData.yearTakenBatchData = batchObj.hasOwnProperty("yearTakenBatchData") ? batchObj.yearTakenBatchData : null;
        jsonData.latlngBatchData = batchObj.hasOwnProperty("latlngBatchData") ? Util.decodeHtml(batchObj.latlngBatchData) : null;
        jsonData.keywordsBatchData = batchObj.hasOwnProperty("keywordsBatchData") ? batchObj.keywordsBatchData : null;
        jsonData.cameraBatchData = batchObj.hasOwnProperty("cameraBatchData") ? Util.decodeHtml(batchObj.cameraBatchData) : null;
        jsonData.lensBatchData = batchObj.hasOwnProperty("lensBatchData") ? Util.decodeHtml(batchObj.lensBatchData) : null;
        jsonData.offsetTakenBatchData = batchObj.hasOwnProperty("offsetTakenBatchData") ? batchObj.offsetTakenBatchData : null;
        jsonData.tagBatchDataInput = batchObj.hasOwnProperty("tagBatchDataInput") ? batchObj.tagBatchDataInput : null;
        jsonData.albumNameInput = batchObj.hasOwnProperty("albumNameInput") ? batchObj.albumNameInput : null;
        jsonData.batchisobject = batchObj.hasOwnProperty("batchisobject") ? batchObj.batchisobject : null;
        jsonData.batchhidden = batchObj.hasOwnProperty("batchhidden") ? batchObj.batchhidden : null;
        jsonData.addtoexistingalbums = batchObj.hasOwnProperty("addtoexistingalbums") ? batchObj.addtoexistingalbums : null;
        jsonData.addtoexistingpeople = batchObj.hasOwnProperty("addtoexistingpeople") ? batchObj.addtoexistingpeople : null;
        jsonData.addtoexistingkeywords = batchObj.hasOwnProperty("addtoexistingkeywords") ? batchObj.addtoexistingkeywords : null;

        return jsonData;
    }

    static getNotifications(notificationAlerts, timezone, pollMinutes) {
        setTimeout(() => {
            let http = new Http("check notifications");
            http.ajax("get", "/notifications/check").then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("hasNotifications")) {
                    if (data.facialDetection === true && data.uncheckedPersonMatches > 0) {
                        $("#sidebarMenuDrawer").html('<span id="topNavAlertBadge" class="position-absolute top-0 start-50 p-1 bg-danger border border-light rounded-circle"><span class="visually-hidden">New alerts</span></span>');
                        $("#sidebarPeople").html('<span id="sideBarAlertPersonBadge" class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle"><span class="visually-hidden">New alerts</span></span>');
                    }

                    if (data.hasNotifications === true) {
                        $("#sidebarMenuDrawer").html('<span id="topNavAlertBadge" class="position-absolute top-0 start-50 p-1 bg-danger border border-light rounded-circle"><span class="visually-hidden">New alerts</span></span>');
                        $("#sidebarNotification").html('<span id="sideBarAlertBadge" class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle"><span class="visually-hidden">New alerts</span></span>');

                        if (notificationAlerts === true) {
                            setTimeout(() => {
                                const unreadNotifications = data.unreadNotifications;
                                const notificationCount = unreadNotifications.length;
                                const firstNotification = data.unreadNotifications[0];
                                const createdAtDate = firstNotification.createdAt;
                                const title = notificationCount + " new notification" + (notificationCount === 1 ? "" : "s");
                                let message = '<div class="container"><div class="row">' + ((firstNotification.imageUrl !== null && firstNotification.imageUrl !== "") ? '<div class="col-4"><img src="' + firstNotification.imageUrl + '" width="100"></div>' : '') + ((firstNotification.imageUrl !== null && firstNotification.imageUrl !== "") ? '<div class="col-8">' : '<div class="col">') + ((notificationCount > 1) ? 'Latest - ' : '') + firstNotification.message + '</div></div>';
                                message = message + "<div class='row'><hr class='mb-1 mt-3'><div class='col'><a href='#' class='gotoNotifications' target='_blank'>Read notifications</a></div><div class='col'><a href='#' class='markNotifications'>Mark "+((notificationCount === 1) ? 'read' : 'all read')+"</a></div></div></div>";

                                // if (notificationCount === 1) {
                                //     NotificationUtil.markNotificationRead();
                                // } else {
                                if (notificationCount > 1) {
                                    shashin.showToastMessage(title, message, {
                                        icon: "bi-bell",
                                        iconColor: "#FF8C00",
                                        headerSubtext: Util.getMessageSubText(createdAtDate, timezone),
                                        autohide: false,
                                        tag: "notifications",
                                        borderColor: "warning"
                                    });

                                    $(".gotoNotifications").on("click", function (e) {
                                        e.preventDefault();

                                        window.open("/notifications", "_blank");

                                        setTimeout(() => {
                                            NotificationUtil.markNotificationRead();
                                            shashin.closeToastMessages({
                                                tag: "notifications"
                                            });
                                        }, 500);
                                    });

                                    $(".markNotifications").on("click", function (e) {
                                        e.preventDefault();

                                        NotificationUtil.markNotificationRead();
                                        shashin.closeToastMessages({
                                            tag: "notifications"
                                        });
                                    });
                                }
                                // }
                            }, 0);

                            // Poll every x minutes
                            if (pollMinutes !== undefined) {
                                setInterval(Util.getNotifications, (pollMinutes * 60 * 1000));
                            }
                        }
                    }

                    if (data.hasNotifications === false && data.uncheckedPersonMatches === 0) {
                        $("#sidebarMenuDrawer").html('');
                        $("#sidebarNotification").html('');
                        $("#sidebarPeople").html('');

                        shashin.closeToastMessages({
                            tag: "notifications"
                        });

                        if (notificationAlerts === true && pollMinutes !== undefined) {
                            // Poll every x minutes
                            setInterval(Util.getNotifications, (pollMinutes * 60 * 1000));
                        }
                    }
                }
            });
        }, 0);
    }

    static populateDetailsInfo(metadata) {
        // Clear data
        $(".descriptionDetails").text("");
        $(".pathDetails").text("");
        $(".typeDetails").text("");
        $(".isoDetails").text("");
        $(".compressionDetails").text("");
        $(".exposureDetails").text("");
        $(".fNumberDetails").text("");
        $(".focalLengthDetails").text("");
        $(".cameraDetails").text("");
        $(".lensDetails").text("");
        $(".qualityDetails").text("");
        $(".addedAtDetails").text("");
        $(".createdAtDetails").text("");
        $(".modifiedAtDetails").text("");
        $(".takenAtDetails").text("");
        $(".manualTakenAtDetails").text("");
        $(".timeZoneDetails").text("");
        $(".keywordsDetails").html("");
        $(".resolutionDetails").text("");
        $(".shareUrlDetails").html("");
        $(".shareUrlDetailsA").html("");
        $(".coordinatesDetails").text("");
        $(".locationDetails").html("");
        $(".locationTypeDetails").html("");
        $(".accessedAtDetails").html("");
        $(".lastViewedByDetails").html("");
        $(".albumsDetails").html("");
        $(".uploadedByDetails").html("");

        $(".pathLabel").hide();
        $(".descriptionLabel").hide();
        $(".timelineLink").hide();
        $(".typeLabel").hide();
        $(".isoLabel").hide();
        $(".compressionLabel").hide();
        $(".exposureLabel").hide();
        $(".fNumberLabel").hide();
        $(".focalLengthLabel").hide();
        $(".cameraLabel").hide();
        $(".lensLabel").hide();
        $(".qualityLabel").hide();
        $(".addedAtLabel").hide();
        $(".createdAtLabel").hide();
        $(".modifiedAtLabel").hide();
        $(".takenAtLabel").hide();
        $(".manualTakenAtLabel").hide();
        $(".accessedAtLabel").hide();
        $(".lastViewedByLabel").hide();
        $(".timeZoneLabel").hide();
        $(".keywordsLabel").hide();
        $(".resolutionLabel").hide();
        $(".shareUrlLabel").hide();
        $(".coordinatesLabel").hide();
        $(".locationLabel").hide();
        $(".locationTypeLabel").hide();
        $(".metadataIdLabel").hide();
        $(".albumsLabel").hide();
        $(".uploadedByLabel").hide();

        $(".linkCopyStatus").invisible();

        const activePage = $("#activePage").val();

        // Fill in details tab data
        if (metadata.lat != null && metadata.lng != null && metadata.lat !== "" && metadata.lng !== "") {
            $(".coordinatesLabel").show();
            $(".coordinatesDetails").text(metadata.lat + ", " + metadata.lng);

            // if (metadata.placeName != null) {
                $(".locationLabel").show();
                let locationType = "";
                let placeNameDisplayName = (metadata.placeName === null) ? 'Unknown location' : metadata.placeName;
                let placeNameDisplayNameArray = placeNameDisplayName.split(";");
                if (placeNameDisplayNameArray.length > 1) {
                    placeNameDisplayName = placeNameDisplayNameArray[0];
                    locationType = placeNameDisplayNameArray[1];
                }
                shashin.printMessageToConsole("Populating detail info - original placename: " + metadata.placeName + " - Display placename: " + placeNameDisplayName,{tag:"metadata"});
                let queryParamDates = "";
                if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
                    let month = metadata.month;
                    if (month < 10) {
                        month = '0'+month;
                    }
                    let lastDay = metadata.day;
                    if (lastDay < 29) {
                        lastDay = 28;
                    }
                    queryParamDates = '&sd='+metadata.year+'-'+metadata.month+'-'+metadata.day+'&ed='+metadata.year+'-'+metadata.month+'-'+metadata.day;
                }
                const linkHtml = "<a href='/map?lat=" + metadata.lat + "&lng=" + metadata.lng + queryParamDates + "' target='_blank'><span class='bi-geo-alt-fill' style='font-size:1.0rem;'></span>" + placeNameDisplayName + "</a>" +
                "&nbsp;<a href='https://www.google.com/maps/search/?api=1&query="+metadata.lat+"%2C"+metadata.lng+"' target='_blank' class='bi-google'></a>";
                $(".locationDetails").html(linkHtml);
                if (locationType.length > 0) {
                    $(".locationTypeLabel").show();
                    let locationTypeHtml = "";
                    const locationTypeArray = locationType.split(",");
                    if (locationTypeArray.length > 1) {
                        for (let index in locationTypeArray) {
                            const locationTypeSingle = locationTypeArray[index].trim();
                            locationTypeHtml += "<a href='/search?term="+locationTypeSingle+"' target='_blank'>"+locationTypeSingle+"</a>, ";
                        }
                    } else {
                        locationTypeHtml = "<a href='/search?term="+locationTypeArray[0]+"' target='_blank'>"+locationTypeArray[0]+"</a>";
                    }

                    locationTypeHtml = locationTypeHtml.replace(/,\s*$/, "");
                    $(".locationTypeDetails").html(locationTypeHtml);
                }
            // }
        }
        if (metadata.path != null) {
            $(".pathLabel").show();
            $(".pathDetails").text(metadata.path);
        }
        if (metadata.description != null) {
            $(".descriptionLabel").show();
            $(".descriptionDetails").text(metadata.description);
        }
        if (metadata.hasOwnProperty("albumMap") && Object.keys(metadata.albumMap).length > 0) {
            $(".albumsLabel").show();
            let albumHtml = "";
            const albumMap = metadata.albumMap;
            $.each(albumMap , function( key, value ) {
                albumHtml += "<a href='/album/"+key+"' target='_blank'>"+value+"</a>, ";
            });
            if (albumHtml.length > 0) {
                albumHtml = albumHtml.slice(0,-2);
            }
            $(".albumsDetails").html(albumHtml);
        }
        if (metadata.hasOwnProperty("keywords") && metadata.keywords != null && metadata.keywords.length > 0) {
            let keywordHtml = "";
            const keywordArray = metadata.keywords;
            if (keywordArray.length > 1) {
                for (let index in keywordArray) {
                    const keyword = keywordArray[index].trim();
                    if (keyword !== "unidentified objects") {
                        $(".keywordsLabel").show();
                        keywordHtml += "<a href='/search?term=" + keyword + "' target='_blank'>" + keyword + "</a>, ";
                    }
                }
            } else if (keywordArray[0] !== "unidentified objects") {
                $(".keywordsLabel").show();
                keywordHtml = "<a href='/search?term="+keywordArray[0]+"' target='_blank'>"+keywordArray[0]+"</a>";
            }

            keywordHtml = keywordHtml.replace(/,\s*$/, "");
            $(".keywordsDetails").html(keywordHtml);
        }
        if (metadata.type != null) {
            $(".typeLabel").show();
            $(".typeDetails").text(metadata.type);
        }
        if (metadata.iso != null) {
            $(".isoLabel").show();
            $(".isoDetails").text(metadata.iso);
        }
        if (metadata.compressionType != null) {
            $(".compressionLabel").show();
            $(".compressionDetails").text(metadata.compressionType);
        }
        if (metadata.exposure != null) {
            $(".exposureLabel").show();
            $(".exposureDetails").text(metadata.exposure + " s");
        }
        if (metadata.fstopNumber != null) {
            $(".fNumberLabel").show();
            $(".fNumberDetails").text("f/"+metadata.fstopNumber);
        }
        if (metadata.focalLength != null) {
            $(".focalLengthLabel").show();
            $(".focalLengthDetails").text(metadata.focalLength + " mm");
        }
        if (metadata.camera != null) {
            $(".cameraLabel").show();
            $(".cameraDetails").text(metadata.camera);
        }
        if (metadata.lens != null) {
            $(".lensLabel").show();
            $(".lensDetails").text(metadata.lens);
        }
        if (metadata.quality != null) {
            $(".qualityLabel").show();
            $(".qualityDetails").text(metadata.quality);
        }
        if (metadata.addedAt != null) {
            $(".addedAtLabel").show();
            $(".addedAtDetails").text(metadata.addedAt);
        }
        if (metadata.createdAt != null) {
            $(".createdAtLabel").show();
            $(".createdAtDetails").text(metadata.createdAt);
        }
        if (metadata.modifiedAt != null) {
            $(".modifiedAtLabel").show();
            $(".modifiedAtDetails").text(metadata.modifiedAt);
        }
        if (metadata.lastAccessedAt != null) {
            $(".accessedAtLabel").show();
            $(".accessedAtDetails").text(metadata.lastAccessedAt);
        }
        if (metadata.lastAccessedByDetails != null) {
            $(".lastViewedByLabel").show();
            $(".lastViewedByDetails").text(metadata.lastAccessedByDetails);
        }
        if (metadata.uploadedByDetails != null) {
            $(".uploadedByLabel").show();
            $(".uploadedByDetails").text(metadata.uploadedByDetails);
        }
        if (metadata.takenAt != null) {
            $(".takenAtLabel").show();
            $(".takenAtDetails").text(metadata.takenAt);
        }
        if (metadata.originalImageWidth != null && metadata.originalImageHeight != null) {
            $(".resolutionLabel").show();
            $(".resolutionDetails").text(metadata.originalImageWidth+"x"+metadata.originalImageHeight);
        }
        let timeLinkHtml = "";
        if (metadata.year !== undefined && metadata.month !== undefined && metadata.day !== undefined &&
            metadata.year !== null && metadata.month !== null && metadata.day !== null) {
            let month = (metadata.month.toString().length === 1 && metadata.month < 10) ? "0"+metadata.month : metadata.month;
            let day = (metadata.day.toString().length === 1 && metadata.day < 10) ? "0"+metadata.day : metadata.day;
            let takenDate = metadata.year + '-' + month + '-' + day;
            let takenDetails = takenDate;
            if (metadata.time !== null && metadata.time !== "") {
                takenDetails += ' ' + metadata.time;
            }
            $(".manualTakenAtLabel").show();
            $(".manualTakenAtDetails").text(takenDetails);

            if (Util.isSafari() === false) {
                timeLinkHtml = "&nbsp;&nbsp;&nbsp;<a class='bi-calendar4' style='font-size: 1rem;' href='/timeline#" + metadata.year + '-' + metadata.month + '-' + metadata.day + "' title='"+(activePage === "archived" ? "Search for date in " : "View in ")+"timeline' target='_blank'></a>";
            }
        }
        // if (metadata.takenAt !== null) {
        //     $(".manualTakenAtLabel").show();
        //     $(".manualTakenAtDetails").text(metadata.takenAt);
        //
        //     if (Util.isSafari() === false && metadata.year !== undefined && metadata.month !== undefined && metadata.day !== undefined &&
        //         metadata.year !== null && metadata.month !== null && metadata.day !== null)
        //     {
        //         timeLinkHtml = "&nbsp;&nbsp;&nbsp;<a class='bi-calendar4' style='font-size: 1rem;' href='/timeline#" + metadata.year + '-' + metadata.month + '-' + metadata.day + "' title='"+(activePage === "archived" ? "Search for date in " : "View in ")+"timeline' target='_blank'></a>";
        //     }
        // }
        if (metadata.timeZone != null) {
            $(".timeZoneLabel").show();
            $(".timeZoneDetails").text(metadata.timeZone);
        }
        if (metadata.id != null) {
            $(".metadataIdLabel").show();
            $(".metadataIdDetails").html(metadata.id + "&nbsp;<a href='#' class='copyLink bi-clipboard-plus sharecopy copyMetadataId' data-clipboard-text='" + metadata.id + "' title='Copy metadata ID' style='font-size: 1rem;'></a>");

            $(".copyMetadataId").on("click", function () {

                Util.copyToClipboard(metadata.id, function (successfullyCopied) {
                    if (successfullyCopied) {
                        $(".copyMetadataId").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-x").addClass("bi-clipboard-check");
                        $('.copyMetadataId').fadeOut(5000, function () {
                            $(this).removeClass("bi-clipboard-check").removeClass("bi-clipboard-x").addClass("bi-clipboard-plus");
                        }).fadeIn(400);
                    } else {
                        $(".copyMetadataId").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-check").addClass("bi-clipboard-x");
                    }
                });
            });
        }
        if (metadata.thumbnailUrlOriginal != null || metadata.videoUrl != null) {
            let relativeShareLink = "/api/v1/image/" + metadata.id;
            if (metadata.videoUrl != null) {
                relativeShareLink = metadata.videoUrl;
            }
            const getUrl = window.location;
            const baseUrl = getUrl.protocol + "//" + getUrl.host;
            // const shareUrl = baseUrl + relativeShareLink.replace('/api/v1','');
            $(".shareUrlLabel").show();

            let page = "/viewer";
            if (metadata.videoUrl != null) {
                page = "/player";
            }

            let shareDetailsHtml = "<a class='bi-download' href='" + relativeShareLink + "/download' title='Download photo' style='font-size: 1rem;'></a>&nbsp;&nbsp;&nbsp;" +
                "<a class='" + ((metadata.videoUrl != null) ? "bi-camera-video" : "bi-file-image") + "' style='font-size: 1rem;' href='" + relativeShareLink.replace('/api/v1', '') + page + "' title='View " + ((metadata.videoUrl != null) ? "video" : "image") + "' target='_blank'></a>";

            if (metadata.videoUrl === null && Util.isLocalNetwork() === false) {
                shareDetailsHtml += "&nbsp;&nbsp;&nbsp;<a class='bi-camera' style='font-size: 1rem;' href='https://lens.google.com/uploadbyurl?url=" + baseUrl + relativeShareLink + "' title='Search Google Lens' target='_blank'></a>";
            }

            // shareDetailsHtml += "&nbsp;&nbsp;&nbsp;<a class='bi-pencil editDetails' tag='"+metadata.id+"' style='font-size: 1rem;' href='#' title='Edit Metadata'></a>";

            if ($(".shareUrlDetailsA").length > 0) {
                shareDetailsHtml += timeLinkHtml;
                $(".shareUrlDetailsA").html(shareDetailsHtml);
            } else {
                $(".shareUrlDetails").html(shareDetailsHtml);
            }

            $(".editDetails").on("click", function (e) {
                e.preventDefault();

                const bsOffcanvasEl = document.getElementById('propInfoSidebar');
                const bsOffcanvas = bootstrap.Offcanvas.getInstance(bsOffcanvasEl);
                if (bsOffcanvas !== null) {
                    bsOffcanvas.hide();
                }

                shashin.openEditMetadataModal(metadata.id);
            });

            $(".sharecopy").on("click", function (e) {
                e.preventDefault();
            });
        }
    }
}

// Convenience functions
(function ($) {
    $.fn.invisible = function () {
        return this.css('visibility', 'hidden');
    };
    $.fn.visible = function() {
        return this.css('visibility', 'visible');
    };
    $.fn.hasScrollBar = function() {
        return this.get(0).scrollHeight > this.get(0).clientHeight;
    };
}(jQuery));

if (typeof module !== 'undefined') {
    module.exports = Util;
}