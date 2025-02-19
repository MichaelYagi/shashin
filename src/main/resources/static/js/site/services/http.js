class Http {
    constructor(action) {
        this.action = action;
        this.additionalParameters = {};
        this.additionalHeaders = {};
    }

    async setAdditionalParameters(params) {
        $.extend(this.additionalParameters, params);
    }

    async setAdditionalHeaders(params) {
        $.extend(this.additionalHeaders, params);
    }

    async ajax(type,url,data,failFunction) {

        function isJSON(something) {
            if (typeof something != 'string')
                something = JSON.stringify(something);

            try {
                JSON.parse(something);
                return true;
            } catch (e) {
                return false;
            }
        }

        const ajaxParams = {
            type: type,
            url: url,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        };

        $.extend(ajaxParams, this.additionalParameters);

        $.extend(ajaxParams.headers, this.additionalHeaders);

        if (data === "undefined" || data === null) {
            data = "";
        }

        if (type === "post" || type === "put" || type === "delete") {
            ajaxParams.data = data;
        }

        if (type === "get") {
            ajaxParams.cache = true;
        }

        return await $.ajax(ajaxParams).fail(function(xhr, textStatus) {
            const message = " executing " + (this.action && this.action.length > 0 ? this.action : "unknown.");
            shashin.onFail(xhr, textStatus, ajaxParams, message, failFunction);
        }).then(function (data, statusText, xhr) {
            shashin.printMessageToConsole("http response",{tag:"http"});
            shashin.printMessageToConsole(JSON.stringify(data),{tag:"http"});
            if (isJSON(data) && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                return data;
            } else if (isJSON(data) === false) {
                if (data.indexOf("Login - Shashin") >= 0) {
                    $(location).prop('href', '/users/login');
                }
            } else {
                return {"message": "Error: No status or msg keys."};
            }
        }.bind(this));
    }
}