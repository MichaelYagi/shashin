class Http {
    constructor(action) {
        this.action = action;
    }

    async ajax(type,url,data,subroutine) {

        const ajaxParams = {
            type: type,
            url: url,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        if (type === "post" && typeof data !== "undefined") {
            ajaxParams.data = data;
        }

        return await $.ajax(ajaxParams).fail(function(xhr, textStatus) {
            const message = " executing " + (this.action && this.action.length > 0 ? this.action : "unknown.");
            shashin.onFail(xhr, textStatus, ajaxParams, message, subroutine);
        }).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                return data;
            }

            return null;
        }.bind(this));
    }
}