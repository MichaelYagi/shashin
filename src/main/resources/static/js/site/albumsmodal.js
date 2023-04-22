(function( albumsModalSettings, $, undefined ) {
    albumsModalSettings.updateShareLink = async function (baseUrl, albumId, action) {
        $("#generateLink").prop('disabled', true);
        $("#cancelUserShare").prop('disabled', true);
        $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
        $("#albumsModalStatus").css("visibility", "visible");
        $("#albumsModalStatus").attr("title", "");
        $("#share"+albumId+" span").removeClass('bi-share-fill').addClass('bi-share');
        $("#msg").html("");
        let relativeShareLink = "";

        if (action === "generate") {
            relativeShareLink = albumsModalSettings.makeShareLinkId(8, 11);
            $("#shareLink").val(relativeShareLink);
            $("#share"+albumId+" span").removeClass('bi-share').addClass('bi-share-fill');
        }

        if ($("#shareLink").val() !== "" && action !== "clear") {
            $("#copyLink").prop('disabled', false);
        } else {
            $("#copyLink").prop('disabled', true);
        }

        const http = new Http("save sharelink");
        let json = {albumId: albumId, relativeShareUrl: relativeShareLink}
        const data = await http.ajax("post", "/album/"+albumId+"/save/sharelink", JSON.stringify(json), function () {
            $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
            $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#cancelUserShare").prop('disabled', false);
        });

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("relativeShareUrl")) {
            let relativeShareUrlData = data["relativeShareUrl"] === null ? "" : data["relativeShareUrl"];
            $("#shareLink").val(relativeShareUrlData);
            $("#fullShareLinkContainer").css("display", "none");
            $("#fullShareLink").text("");

            if (relativeShareUrlData !== "") {
                const fullShareLink = baseUrl + "share/" + relativeShareUrlData + "/album/"+albumId;
                $("#fullShareLinkContainer").css("display", "block");
                $("#fullShareLink").html("<a target='_blank' href='" + fullShareLink + "'>" + fullShareLink + "</a>");
                $("#copyLink").attr("data-clipboard-text", fullShareLink);
            }

            if (data["status"] === "success" && $("#shareLink").val() === relativeShareUrlData) {
                $("#albumsModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
            } else {
                $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
            }

            $("#generateLink").prop('disabled', false);
            $("#cancelUserShare").prop('disabled', false);
        } else {
            $("#generateLink").prop('disabled', false);
            $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#cancelUserShare").prop('disabled', false);
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