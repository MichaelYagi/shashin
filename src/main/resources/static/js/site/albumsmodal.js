(function( albumsModalSettings, $, undefined ) {
    albumsModalSettings.updateShareLink = async function (baseUrl, albumId, action) {
        $("#generateLink" + albumId).prop('disabled', true);
        $("#cancelUserShare" + albumId).prop('disabled', true);
        $("#albumsModalStatus" + albumId).removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
        $("#albumsModalStatus" + albumId).css("visibility", "visible");
        $("#albumsModalStatus" + albumId).attr("title", "");
        $("#share" + albumId + " span").removeClass('bi-share-fill').addClass('bi-share');
        $("#msg" + albumId).html("");
        let relativeShareLink = "";

        if (action === "generate") {
            relativeShareLink = albumsModalSettings.makeShareLinkId(8, 11);
            $("#shareLink" + albumId).val(relativeShareLink);
            $("#share" + albumId + " span").removeClass('bi-share').addClass('bi-share-fill');
        }

        if ($("#shareLink" + albumId).val() !== "" && action !== "clear") {
            $("#copyLink" + albumId).prop('disabled', false);
        } else {
            $("#copyLink" + albumId).prop('disabled', true);
        }

        const http = new Http("save sharelink");
        let json = {albumId: albumId, relativeShareUrl: relativeShareLink}
        const data = await http.ajax("post", "/album/" + albumId + "/save/sharelink", JSON.stringify(json), function () {
            $("#albumsModalStatus" + albumId).removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
            $("#albumsModalStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
            $("#cancelUserShare" + albumId).prop('disabled', false);
        });

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("relativeShareUrl")) {
            let relativeShareUrlData = data["relativeShareUrl"] === null ? "" : data["relativeShareUrl"];
            $("#shareLink" + albumId).val(relativeShareUrlData);
            $("#fullShareLinkContainer" + albumId).css("display", "none");
            $("#fullShareLink" + albumId).text("");

            if (relativeShareUrlData !== "") {
                const fullShareLink = baseUrl + "share/" + relativeShareUrlData + "/album/" + albumId;
                $("#fullShareLinkContainer" + albumId).css("display", "block");
                $("#fullShareLink" + albumId).html("<a target='_blank' href='" + fullShareLink + "'>" + fullShareLink + "</a>");
                $("#copyLink" + albumId).attr("data-clipboard-text", fullShareLink);
            }

            if (data["status"] === "success" && $("#shareLink" + albumId).val() === relativeShareUrlData) {
                $("#albumsModalStatus" + albumId).addClass('bi-check-circle').removeClass('spinner-grow');
            } else {
                $("#albumsModalStatus" + albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                $("#albumsModalStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
            }

            $("#generateLink" + albumId).prop('disabled', false);
            $("#cancelUserShare" + albumId).prop('disabled', false);
        } else {
            $("#generateLink" + albumId).prop('disabled', false);
            $("#albumsModalStatus" + albumId).addClass('bi-x-circle').removeClass('spinner-grow');
            $("#albumsModalStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
            $("#cancelUserShare" + albumId).prop('disabled', false);
        }
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