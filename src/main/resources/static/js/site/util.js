class Util {
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

    static isSafari() {
        const isSafari = navigator.vendor && navigator.vendor.indexOf('Apple') > -1 &&
            navigator.userAgent &&
            navigator.userAgent.indexOf('CriOS') == -1 &&
            navigator.userAgent.indexOf('FxiOS') == -1;

        return isSafari;
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
    };

    static getShortDay(index) {
        const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        return days[index];
    }

    static getShortMonths(index) {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return months[index];
    }

    static validateMetadataInputs(day, month, year, time, offset, latlng, msgId) {
        if (offset === null ) {
            offset = "";
        }
        const dayValidate = "([1-9]|[12]\d|3[01])";
        const monthValidate = "^(0?[1-9]|1[012])$";
        const timeValidate = "^(\\d{2}:\\d{2}:\\d{2})$";
        const offsetValidate = "^([+-±](?:2[0-3]|[01][0-9]):[0-5][0-9])$";

        let msg = "";
        if (day !== "" && !day.match(dayValidate)) {
            msg = "Enter Valid Day";
        }

        if (month !== "" && !month.match(monthValidate)) {
            msg = "Enter Valid Month";
        }

        if (year !== "" && !(+year >= 1888 && +year <= new Date().getFullYear())) {
            msg = "Enter Valid Year";
        }

        if (time !== "" && !time.match(timeValidate)) {
            msg = "Enter Valid Time";
        }

        if (offset !== "" && !offset.match(offsetValidate)) {
            msg = "Enter Valid Offset";
        }

        if (latlng !== "") {
            latlng = $.trim(latlng);
            const latlngArr = latlng.split(",");

            if (latlngArr.length !== 2 || latlng.split(".").length !== 3 || !Util.isNumericString(latlngArr[0]) || !Util.isNumericString(latlngArr[1])) {
                msg = "Enter Valid Latitude/Longitude";
            }
        }

        if (msg !== "") {
            $("#"+msgId).html('<div class="alert alert-danger" role="alert">'+msg+'</div>');
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
        return ((window.innerHeight + element.scrollTop)  >= element.scrollHeight) // compare with scroll position + some give (*1.5)
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
        if (typeof str != "string") return false // we only process strings!
        return !isNaN(str) && // use type coercion to parse the _entirety_ of the string (`parseFloat` alone does not do this)...
            !isNaN(parseFloat(str)) // ...and ensure strings of whitespace fail
    }

    static decodeHtml(html) {
        const txt = document.createElement("textarea");
        txt.innerHTML = html;
        return txt.value;
    }

    static encodeHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    static errorImg(_this,text,defaulWidthtHeight) {
        let dimensions = "/209";
        if (defaulWidthtHeight != null) {
            dimensions = "/"+defaulWidthtHeight;
        }
        if (_this.width != null && _this.width > 0 && _this.height != null && _this.height > 0) {
            dimensions = "/"+_this.width+"x"+_this.height;
        }
        _this.src = "https://via.placeholder.com"+dimensions+"?text="+encodeURI(text);
    }

    static activateMetadataListeners(metadata) {
        if (metadata && metadata.hasOwnProperty("id")) {
            $("#image" + metadata.id).on('load', function () {
                $(this).css("background-color", "transparent");
            });
        }
    }

    static removeDateGallery(id) {
        $("#br"+id).remove();
        $("#row"+id).remove();
        $("#amp_"+id).remove();
        $("#tail_"+id).remove();
        $("#"+id).remove();
        $("#container_"+id).remove();
    }

    static getDateGalleryHeight(id) {
        if ($("#br" + id).length === 0 && $("#br" + id).length === 0 && $("#br" + id).length === 0 && $("#br" + id).length === 0 && $("#br" + id).length === 0) {
            return 0;
        }

        return $("#br" + id).outerHeight(true) +
            $("#row" + id).outerHeight(true) +
            $("#amp_" + id).outerHeight(true) +
            $("#tail_" + id).outerHeight(true) +
            $("#" + id).outerHeight(true);
    }

    static populateDetailsTab(metadata) {
        // Clear data
        $("#pathDetails").text("");
        $("#typeDetails").text("");
        $("#isoDetails").text("");
        $("#compressionDetails").text("");
        $("#exposureDetails").text("");
        $("#fNumberDetails").text("");
        $("#focalLengthDetails").text("");
        $("#cameraDetails").text("");
        $("#lensDetails").text("");
        $("#qualityDetails").text("");
        $("#addedAtDetails").text("");
        $("#createdAtDetails").text("");
        $("#modifiedAtDetails").text("");
        $("#takenAtDetails").text("");
        $("#manualTakenAtDetails").text("");
        $("#timeZoneDetails").text("");
        $("#keywordsDetails").text("");
        $("#resolutionDetails").text("");
        $("#shareUrlDetails").html("");
        $("#msgcopyLink").html("");

        // Fill in details tab data
        if (metadata.path != null) {
            $("#pathDetails").text(metadata.path);
        }
        if (metadata.keywords != null) {
            $("#keywordsDetails").text(metadata.keywords);
        }
        if (metadata.type != null) {
            $("#typeDetails").text(metadata.type);
        }
        if (metadata.iso != null) {
            $("#isoDetails").text(metadata.iso);
        }
        if (metadata.compressionType != null) {
            $("#compressionDetails").text(metadata.compressionType);
        }
        if (metadata.exposure != null) {
            $("#exposureDetails").text(metadata.exposure + " s");
        }
        if (metadata.fstopNumber != null) {
            $("#fNumberDetails").text("f/"+metadata.fstopNumber);
        }
        if (metadata.focalLength != null) {
            $("#focalLengthDetails").text(metadata.focalLength + " mm");
        }
        if (metadata.camera != null) {
            $("#cameraDetails").text(metadata.camera);
        }
        if (metadata.lens != null) {
            $("#lensDetails").text(metadata.lens);
        }
        if (metadata.quality != null) {
            $("#qualityDetails").text(metadata.quality);
        }
        if (metadata.addedAt != null) {
            $("#addedAtDetails").text(metadata.addedAt);
        }
        if (metadata.createdAt != null) {
            $("#createdAtDetails").text(metadata.createdAt);
        }
        if (metadata.modifiedAt != null) {
            $("#modifiedAtDetails").text(metadata.modifiedAt);
        }
        if (metadata.takenAt != null) {
            $("#takenAtDetails").text(metadata.takenAt);
        }
        if (metadata.originalImageWidth != null && metadata.originalImageHeight != null) {
            $("#resolutionDetails").text(metadata.originalImageWidth+"x"+metadata.originalImageHeight);
        }
        if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
            let takenDetails = metadata.year + '-' + metadata.month + '-' + metadata.day;
            if (metadata.time !== null && metadata.time !== "") {
                takenDetails += ' ' + metadata.time;
            }
            $("#manualTakenAtDetails").text(takenDetails);
        }
        if (metadata.timeZone != null) {
            $("#timeZoneDetails").text(metadata.timeZone);
        }
        if (metadata.thumbnailUrlOriginal != null || metadata.videoUrl != null) {
            let relativeShareLink = metadata.thumbnailUrlOriginal;
            if (metadata.videoUrl != null) {
                relativeShareLink = metadata.videoUrl;
            }
            const getUrl = window.location;
            const baseUrl = getUrl.protocol + "//" + getUrl.host;
            const shareUrl = baseUrl + relativeShareLink;
            $("#shareUrlDetails").html("<a href='"+relativeShareLink+"' target='_blank'>Share Link</a>&nbsp;<span id='copyLink' class='bi-clipboard-plus' data-clipboard-text='"+shareUrl+"'></span>");
            const clipboard = new ClipboardJS(document.getElementById("copyLink"));
            clipboard.on('success', function (e) {
                $("#copyLinkMessage").html("<div class=\"alert alert-success\" role=\"alert\">Link copied to clipboard!</div>");
            });

            clipboard.on('error', function (e) {
                $("#copyLinkMessage").html("<div class=\"alert alert-warning\" role=\"alert\">Could not copy text</div>");
            });
        }
    }
}

if (typeof module !== 'undefined') {
    module.exports = Util;
}