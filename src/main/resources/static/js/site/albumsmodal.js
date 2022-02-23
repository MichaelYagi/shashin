(function( albumsModalSettings, $, undefined ) {
    albumsModalSettings.updateShareLink = function (baseUrl,albumId,action) {
        $("#generateLink"+albumId).prop('disabled', true);
        $("#cancelUserShare"+albumId).prop('disabled', true);
        $("#albumsModalStatus"+albumId).removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
        $("#albumsModalStatus"+albumId).css("visibility","visible");
        $("#albumsModalStatus"+albumId).attr("title","");
        $("#share"+albumId+" span").removeClass('bi-share-fill').addClass('bi-share');
        $("#msg"+albumId).html("");
        let relativeShareLink = "";

        if (action === "generate") {
            relativeShareLink = albumsModalSettings.makeShareLinkId(8, 11);
            $("#shareLink"+albumId).val(relativeShareLink);
            $("#share"+albumId+" span").removeClass('bi-share').addClass('bi-share-fill');
        }

        if ($("#shareLink"+albumId).val() !== "" && action !== "clear") {
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

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {
            $("#albumsModalStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
            $("#albumsModalStatus"+albumId).attr("title", shashin.modalStatusFailMessage());
            shashin.onFail(xhr, textStatus, ajaxParams, " updating album share link");
        }).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("relativeShareUrl")) {
                let message = "Generated link not saved";
                let relativeShareUrlData = data["relativeShareUrl"] === null ? "" : data["relativeShareUrl"];
                $("#shareLink"+albumId).val(relativeShareUrlData);
                $("#fullShareLinkContainer"+albumId).css("display","none");
                $("#fullShareLink"+albumId).text("");

                if (relativeShareUrlData !== "") {
                    const fullShareLink = baseUrl+ "share/" + relativeShareUrlData + "/album/"+albumId;
                    $("#fullShareLinkContainer"+albumId).css("display","block");
                    $("#fullShareLink"+albumId).html("<a target='_blank' href='"+fullShareLink+"'>"+fullShareLink+"</a>");
                    $("#copyLink"+albumId).attr("data-clipboard-text",fullShareLink);
                }

                if (data["status"] === "success" && $("#shareLink"+albumId).val() === relativeShareUrlData) {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    $("#albumsModalStatus"+albumId).addClass('bi-check-circle').removeClass('spinner-grow');
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#albumsModalStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#albumsModalStatus"+albumId).attr("title", shashin.modalStatusFailMessage());
                }

                $("#generateLink"+albumId).prop('disabled', false);
                $("#cancelUserShare"+albumId).prop('disabled', false);
                //$("#msg"+albumId).html(message);
            } else {
                $("#generateLink"+albumId).prop('disabled', false);
                //$("#msg"+albumId).html("<div class=\"alert alert-danger\" role=\"alert\">Generated link not saved</div>");
                $("#albumsModalStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                $("#albumsModalStatus"+albumId).attr("title", shashin.modalStatusFailMessage());
                $("#cancelUserShare"+albumId).prop('disabled', false);
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