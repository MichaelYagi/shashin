(function( albumsModalSettings, $, undefined ) {
    albumsModalSettings.updateShareLink = async function (baseUrl, albumId, action) {
        $("#generateLink").prop('disabled', true);
        $("#cancelUserShare").prop('disabled', true);
        $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
        $("#albumsModalStatus").visible();
        $("#albumsModalStatus").attr("title", "");
        $("#share"+albumId+" span").removeClass('bi-share-fill').addClass('bi-share');
        $("#share"+albumId+" span").attr("title", "Share with other people");
        $("#msg").html("");
        let relativeShareLink = "";

        if (action === "generate") {
            relativeShareLink = await albumsModalSettings.makeShareLinkId(albumId);
            $("#shareLink").val(relativeShareLink);
            $("#share"+albumId+" span").removeClass('bi-share').addClass('bi-share-fill');
            $("#share"+albumId+" span").attr("title", "Shared with other people");
        }

        if ($("#shareLink").val() !== "" && action !== "clear") {
            $("#clearLink").prop('disabled', false);
            $("#copyLink").prop('disabled', false);
        } else {
            $("#clearLink").prop('disabled', true);
            $("#copyLink").prop('disabled', true);
        }

        const http = new Http("save sharelink");
        let json = {albumId: albumId, relativeShareUrl: relativeShareLink};
        const data = await http.ajax("post", "/share/album/save", JSON.stringify(json), function () {
            $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
            $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#cancelUserShare").prop('disabled', false);
        });

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("relativeShareUrl")) {
            let relativeShareUrlData = data.relativeShareUrl === null ? "" : data.relativeShareUrl;
            $("#shareLink").val(relativeShareUrlData);
            $("#fullShareLinkContainer").css("display", "none");
            $("#fullShareLink").text("");

            if (relativeShareUrlData !== "") {
                const fullShareLink = baseUrl + "/share/" + relativeShareUrlData + "/album/"+albumId;
                $("#fullShareLinkContainer").css("display", "block");
                $("#fullShareLink").html("<a target='_blank' href='" + fullShareLink + "'>" + fullShareLink + "</a>");
                $("#copyLink").attr("data-clipboard-text", fullShareLink);
            }

            if (data.status === shashin.apiResponse.SUCCESS && $("#shareLink").val() === relativeShareUrlData) {
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
    };

    albumsModalSettings.makeShareLinkId = async function (albumId) {
        const http = new Http("create rand link");
        const data = await http.ajax("get", "/share/album/" + albumId + "/create");
        let result = "";
        if (data.hasOwnProperty("relativeShareLink") && data.relativeShareLink !== "") {
            result = data.relativeShareLink;
        } else {
            shashin.showToastMessage("Something went wrong!", "Could not generate share link", {
                icon:"bi-exclamation-triangle",
                iconColor:"#FF0000",
                borderColor:"danger"
            });
        }

        return result;
    };
}( window.albumsModalSettings = window.albumsModalSettings || {}, jQuery ));