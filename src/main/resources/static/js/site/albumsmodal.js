(function( albumsModalSettings, $, undefined ) {
    albumsModalSettings.updateShareLink = function (baseUrl,albumId,action) {
        $("#generateLink"+albumId).prop('disabled', true);
        $("#albumsModalStatus"+albumId).css("visibility","visible");
        $("#msg"+albumId).html("");
        let relativeShareLink = "";
        if (action === "generate") {
            relativeShareLink = albumsModalSettings.makeShareLinkId(8, 11);
            $("#shareLink"+albumId).val(relativeShareLink);
        }
        if ($("#shareLink"+albumId).val() !== "") {
            $("#copyLink"+albumId).prop('disabled', false);
        } else {
            $("#copyLink"+albumId).prop('disabled', true);
        }

        let json = {albumId: albumId,relativeShareUrl: relativeShareLink}
        const ajaxParams = {
            type: "post",
            url: "/album/"+albumId+"/save/sharelink",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating album share link. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        $.ajax(ajaxParams)
        .fail(onFail).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("relativeShareUrl")) {
                let message = "Generated link not saved";
                let relativeShareUrlData = data["relativeShareUrl"] === null ? "" : data["relativeShareUrl"];
                $("#shareLink"+albumId).val(relativeShareUrlData);
                $("#fullShareLinkContainer"+albumId).css("display","none");
                $("#fullShareLink"+albumId).text("");
                if (relativeShareUrlData !== "") {
                    $("#fullShareLinkContainer"+albumId).css("display","block");
                    $("#fullShareLink"+albumId).html("<a target='_blank' href='"+baseUrl+"share/"+relativeShareUrlData+"/album/"+albumId+"'>"+baseUrl+"share/"+relativeShareUrlData+"/album/"+albumId+"</a>");
                }
                if (data["status"] === "success" && $("#shareLink"+albumId).val() === relativeShareUrlData) {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    $("#albumsModalStatus"+albumId).addClass('bi-check-circle').removeClass('spinner-grow');
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#albumsModalStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                }
                $("#generateLink"+albumId).prop('disabled', false);
                //$("#msg"+albumId).html(message);
            } else {
                $("#generateLink"+albumId).prop('disabled', false);
                //$("#msg"+albumId).html("<div class=\"alert alert-danger\" role=\"alert\">Generated link not saved</div>");
                $("#albumsModalStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
            }
        });
    }

    albumsModalSettings.makeShareLinkId = function (minLength, maxLength) {
        const length = Math.floor(
            Math.random() * (Math.ceil(maxLength) - Math.floor(minLength) + 1) + minLength
        );
        let result = '';
        const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
        const charactersLength = characters.length;
        for (let i = 0; i < length; i++ ) {
            result += characters.charAt(Math.floor(Math.random() *
                charactersLength));
        }
        return result;
    }
}( window.albumsModalSettings = window.albumsModalSettings || {}, jQuery ));