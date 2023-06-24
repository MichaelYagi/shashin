class Http {
    constructor(action) {
        this.action = action;
    }

    async ajax(type,url,data,failFunction) {

        const ajaxParams = {
            type: type,
            url: url,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        if (shashin.apikey !== "") {
            ajaxParams.headers = {"X-API-KEY": shashin.apikey};
        }

        if (type === "post" && typeof data !== "undefined") {
            ajaxParams.data = data;
        }

        if (type === "get") {
            ajaxParams.cache = true;
        }

        return await $.ajax(ajaxParams).fail(function(xhr, textStatus) {
            const message = " executing " + (this.action && this.action.length > 0 ? this.action : "unknown.");
            shashin.onFail(xhr, textStatus, ajaxParams, message, failFunction);
        }).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                return data;
            } else {
                return {"message": "Error: No status or msg keys."}
            }
        }.bind(this));
    }
}