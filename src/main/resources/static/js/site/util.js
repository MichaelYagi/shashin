class Util {

    static lgApiKey() {
        return "A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3";
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
    };

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
    };

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

    static isMobile() {
        let isMobile = false; //initiate as false
        // device detection
        if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|ipad|iris|kindle|Android|Silk|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(navigator.userAgent)
            || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(navigator.userAgent.substr(0,4))) {
            isMobile = true;
        }

        return isMobile;
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
          platform =
            window.navigator?.userAgentData?.platform ||
            window.navigator.platform,
          macosPlatforms = ["Macintosh", "MacIntel", "MacPPC", "Mac68K"],
          windowsPlatforms = ["Win32", "Win64", "Windows", "WinCE"],
          iosPlatforms = ["iPhone", "iPad", "iPod"],
          os = "";

        if (macosPlatforms.indexOf(platform) !== -1) {
          os = "MacOS";
        } else if (iosPlatforms.indexOf(platform) !== -1) {
          os = "iOS";
        } else if (windowsPlatforms.indexOf(platform) !== -1) {
          os = "Windows";
        } else if (/Android/.test(userAgent)) {
          os = "Android";
        } else if (/Linux/.test(platform)) {
          os = "Linux";
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

        if (year !== "" && !(+year >= 1826 && +year <= new Date().getFullYear())) {
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
        return (((window.innerHeight + element.scrollTop) * 1.05) >= element.scrollHeight) // compare with scroll position + some give (*1.5)
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

    static stringEscape(s) {
        return s ? s.replace(/\\/g,'\\\\').replace(/\n/g,'\\n').replace(/\t/g,'\\t').replace(/\v/g,'\\v').replace(/'/g,"\\'").replace(/"/g,'\\"').replace(/[\x00-\x1F\x80-\x9F]/g,hex) : s;
        function hex(c) {
            const v = "0" + c.charCodeAt(0).toString(16);
            return "\\x" + v.substr(v.length - 2); }
    }
    
    static checkErrorImage() {
        $("img").on('error', function() {
            const width = $(this).attr('width');
            const height = $(this).attr('width');
            let dimensions = "/" + Util.thumbnailHeight();

            if (typeof width !== 'undefined' && width !== false && typeof height !== 'undefined' && height !== false) {
                dimensions = "/"+width+"x"+height;
            }
            const imagePlaceholder = "https://via.placeholder.com"+dimensions+"?text="+encodeURI($(this).attr('src'));
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

    static getCookie(name) {
        const parts = document.cookie.split(name + "=");

        if (parts.length === 2) {
            return parts.pop().split(";").shift();
        }

        return "";
    }

    static setCookie(name, value, path, domain) {
        const d = new Date();
        d.setTime(d.getTime() + (3600*1000));
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
        if (!+bytes) return '0 Bytes'

        const k = 1024
        const dm = decimals < 0 ? 0 : decimals
        const sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB']

        const i = Math.floor(Math.log(bytes) / Math.log(k))

        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
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

    static getBatchData(batchObj) {
        const jsonData = {};
        jsonData.batchMetadataIds = batchObj.hasOwnProperty("batchMetadataIds") ? JSON.parse(batchObj["batchMetadataIds"]) : null;
        jsonData.dayTakenBatchData = batchObj.hasOwnProperty("dayTakenBatchData") ? batchObj["dayTakenBatchData"] : null;
        jsonData.monthTakenBatchData = batchObj.hasOwnProperty("monthTakenBatchData") ? batchObj["monthTakenBatchData"] : null;
        jsonData.yearTakenBatchData = batchObj.hasOwnProperty("yearTakenBatchData") ? batchObj["yearTakenBatchData"] : null;
        jsonData.latlngBatchData = batchObj.hasOwnProperty("latlngBatchData") ? Util.decodeHtml(batchObj["latlngBatchData"]) : null;
        jsonData.keywordsBatchData = batchObj.hasOwnProperty("keywordsBatchData") ? Util.decodeHtml(batchObj["keywordsBatchData"]) : null;
        jsonData.cameraBatchData = batchObj.hasOwnProperty("cameraBatchData") ? Util.decodeHtml(batchObj["cameraBatchData"]) : null;
        jsonData.lensBatchData = batchObj.hasOwnProperty("lensBatchData") ? Util.decodeHtml(batchObj["lensBatchData"]) : null;
        jsonData.offsetTakenBatchData = batchObj.hasOwnProperty("offsetTakenBatchData") ? batchObj["offsetTakenBatchData"] : null;
        jsonData.tagBatchDataInput = batchObj.hasOwnProperty("tagBatchDataInput") ? Util.decodeHtml(batchObj["tagBatchDataInput"]) : null;
        jsonData.albumNameInput = batchObj.hasOwnProperty("albumNameInput") ? Util.decodeHtml(batchObj["albumNameInput"]) : null;
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
            } else if ($("#propMetadataLocation").length > 0) {
                containerModalId = "propMetadataLocation";
            }
        }

        // Clear data
        $(".descriptionDetails").text("");
        $(".pathDetails").text("");
        $(".timelineLink").html("");
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
                const linkHtml = "<a href='/map?lat=" + metadata.lat + "&lng=" + metadata.lng + "' target='_blank'>" + metadata.placeName + "</a>" +
                "&nbsp;<a href='https://www.google.com/maps/search/?api=1&query="+metadata.lat+"%2C"+metadata.lng+"' target='_blank' class='bi-google'></a>";
                $(".locationDetails").html(linkHtml);
            }
        }
        if (metadata.path != null) {
            $(".pathLabel").show();
            $(".pathDetails").text(metadata.path);
        }
        if (metadata.description != null) {
            $(".descriptionLabel").show();
            $(".descriptionDetails").text(metadata.description);
        }
        if (metadata.keywords != null && metadata.keywords.length > 0) {
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
            let takenDate = metadata.year + '-' + metadata.month + '-' + metadata.day;
            let takenDetails = takenDate;
            if (metadata.time !== null && metadata.time !== "") {
                takenDetails += ' ' + metadata.time;
            }
            $(".manualTakenAtLabel").show();
            $(".manualTakenAtDetails").text(takenDetails);

            $(".timelineLink").show();
            $(".timelineLink").html("<a href='/timeline#" + takenDate + "' target='_blank'>View date in timeline</a>");
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

            let page = "/viewer"
            if (metadata.videoUrl != null) {
                page = "/player"
            }
            $(".shareUrlDetails").html("<a class='bi-download' href='" + relativeShareLink + "/download'></a>&nbsp;<a href='" + relativeShareLink + page + "' target='_blank'>Share Link</a>&nbsp;<span class='copyLink bi-clipboard-plus' data-clipboard-text='" + shareUrl + page + "'></span>&nbsp;<span class='linkCopyStatus bi-check-circle' style='visibility: hidden;color:green;'></span>");

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