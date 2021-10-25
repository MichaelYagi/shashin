(function( albumsModalSettings, $, undefined ) {
    albumsModalSettings.updateShareLink = function (baseUrl,albumId,action) {
        $("#generateLink"+albumId).prop('disabled', true);
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
        const posting = $.post({
            url: "/album/"+albumId+"/save/sharelink",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8'
        });

        posting.done(function (data) {
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
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                }
                $("#generateLink"+albumId).prop('disabled', false);
                $("#msg"+albumId).html(message);
            } else {
                $("#generateLink"+albumId).prop('disabled', false);
                $("#msg"+albumId).html("<div class=\"alert alert-danger\" role=\"alert\">Generated link not saved</div>");
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