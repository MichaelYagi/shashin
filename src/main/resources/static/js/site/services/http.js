class Http {
    constructor(module) {
        this.module = module;
    }

    async ajax(type,url,data) {

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
            const message = " updating" + (this.module && this.module.length > 0 ? " " + this.module : "");
            shashin.onFail(xhr, textStatus, ajaxParams, message);
        }).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                return data;
            }

            return null;
        }.bind(this));
    }
}