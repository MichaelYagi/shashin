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

            if (latlngArr.length !== 2 || latlng.split(".").length !== 3 || !shashin.isNumeric(latlngArr[0]) || !shashin.isNumeric(latlngArr[1])) {
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
}