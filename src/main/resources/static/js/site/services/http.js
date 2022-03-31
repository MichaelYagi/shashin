class Http {
    constructor(module) {
        this.module = module;
    }

    async ajaxGet(url) {

        const ajaxParams = {
            type: 'get',
            url: url,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        return await $.ajax(ajaxParams).fail(function(xhr, textStatus) {
            const message = " updating" + (this.module && this.module.length > 0 ? " " + this.module : "");
            shashin.onFail(xhr, textStatus, ajaxParams, message);
        }).then(function (data) {
            // console.log(data)
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                return data;
            }

            return null;
        }.bind(this));
    }
}