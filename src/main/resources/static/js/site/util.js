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

    static addKeywordToMetadata(metadata, keywords) {
        if (metadata && metadata.hasOwnProperty("id")) {
            metadata["keywords"] = keywords;
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

    static getBatchData(batchObj) {
        const jsonData = {};
        jsonData.batchMetadataIds = batchObj.hasOwnProperty("batchMetadataIds") ? JSON.parse(batchObj["batchMetadataIds"]) : null;
        jsonData.dayTakenBatchData = batchObj.hasOwnProperty("dayTakenBatchData") ? batchObj["dayTakenBatchData"] : null;
        jsonData.monthTakenBatchData = batchObj.hasOwnProperty("monthTakenBatchData") ? batchObj["monthTakenBatchData"] : null;
        jsonData.yearTakenBatchData = batchObj.hasOwnProperty("yearTakenBatchData") ? batchObj["yearTakenBatchData"] : null;
        jsonData.latlngBatchData = batchObj.hasOwnProperty("latlngBatchData") ? batchObj["latlngBatchData"] : null;
        jsonData.keywordsBatchData = batchObj.hasOwnProperty("keywordsBatchData") ? batchObj["keywordsBatchData"] : null;
        jsonData.tagBatchDataInput = batchObj.hasOwnProperty("tagBatchDataInput") ? batchObj["tagBatchDataInput"] : null;
        jsonData.albumNameInput = batchObj.hasOwnProperty("albumNameInput") ? batchObj["albumNameInput"] : null;
        jsonData.batchisobject = batchObj.hasOwnProperty("batchisobject") ? batchObj["batchisobject"] : null;
        jsonData.batchhidden = batchObj.hasOwnProperty("batchhidden") ? batchObj["batchhidden"] : null;

        return jsonData;
    }

    static populateDetailsInfo(metadata,containerModalId) {
        if (typeof containerModalId === "undefined" || containerModalId.length === 0) {
            if ($("#propInfoModal").length > 0) {
                containerModalId = "propInfoModal";
            } else if ($("#propTimelineModal").length > 0) {
                containerModalId = "propTimelineModal";
            } else if ($("#propInfoSidebar").length > 0) {
                containerModalId = "propInfoSidebar";
            }
        }

        // Clear data
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
        $(".keywordsDetails").text("");
        $(".resolutionDetails").text("");
        $(".shareUrlDetails").html("");
        $(".coordinatesDetails").text("");
        $(".locationDetails").html("");

        $(".pathLabel").hide();
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
        $(".timeZoneLabel").hide();
        $(".keywordsLabel").hide();
        $(".resolutionLabel").hide();
        $(".shareUrlLabel").hide();
        $(".coordinatesLabel").hide();
        $(".locationLabel").hide();

        $(".linkCopyStatus").css("visibility","hidden");

        // Fill in details tab data
        if (metadata.lat != null && metadata.lng != null && metadata.lat !== "" && metadata.lng !== "") {
            $(".coordinatesLabel").show();
            $(".coordinatesDetails").text(metadata.lat + ", " + metadata.lng);

            if (metadata.placeName != null) {
                $(".locationLabel").show();
                $(".locationDetails").html("<a href='/map?lat=" + metadata.lat + "&lng=" + metadata.lng + "' target='_blank'>" + metadata.placeName + "</a>");
            }
        }
        if (metadata.path != null) {
            $(".pathLabel").show();
            $(".pathDetails").text(metadata.path);
        }
        if (metadata.keywords != null && metadata.keywords !== '') {
            $(".keywordsLabel").show();
            $(".keywordsDetails").text(metadata.keywords);
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
        if (metadata.takenAt != null) {
            $(".takenAtLabel").show();
            $(".takenAtDetails").text(metadata.takenAt);
        }
        if (metadata.originalImageWidth != null && metadata.originalImageHeight != null) {
            $(".resolutionLabel").show();
            $(".resolutionDetails").text(metadata.originalImageWidth+"x"+metadata.originalImageHeight);
        }
        if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
            let takenDetails = metadata.year + '-' + metadata.month + '-' + metadata.day;
            if (metadata.time !== null && metadata.time !== "") {
                takenDetails += ' ' + metadata.time;
            }
            $(".manualTakenAtLabel").show();
            $(".manualTakenAtDetails").text(takenDetails);
        }
        if (metadata.timeZone != null) {
            $(".timeZoneLabel").show();
            $(".timeZoneDetails").text(metadata.timeZone);
        }
        if (metadata.thumbnailUrlOriginal != null || metadata.videoUrl != null) {
            let relativeShareLink = metadata.thumbnailUrlOriginal;
            if (metadata.videoUrl != null) {
                relativeShareLink = metadata.videoUrl;
            }
            const getUrl = window.location;
            const baseUrl = getUrl.protocol + "//" + getUrl.host;
            const shareUrl = baseUrl + relativeShareLink;
            $(".shareUrlLabel").show();
            $(".shareUrlDetails").html("<a href='"+relativeShareLink+"' target='_blank'>Share Link</a>&nbsp;<span class='copyLink bi-clipboard-plus' data-clipboard-text='"+shareUrl+"'></span>&nbsp;<span class='linkCopyStatus bi-check-circle' style='visibility: hidden;color:green;'></span>");

            const clipboard = new ClipboardJS(".copyLink.bi-clipboard-plus",{container: document.getElementById(containerModalId)});
            clipboard.on('success', function (e) {
                $(".linkCopyStatus").addClass('bi-check-circle').removeClass('bi-x-circle');
                $('.linkCopyStatus').css({'visibility':'visible', 'color':'green'}).hide().fadeIn("slow");
            });

            clipboard.on('error', function (e) {
                $(".linkCopyStatus").addClass('bi-x-circle').removeClass('bi-check-circle');
                $('.linkCopyStatus').css({'visibility':'visible', 'color':'red'}).hide().fadeIn("slow");
            });
        }
    }
}

if (typeof module !== 'undefined') {
    module.exports = Util;
}