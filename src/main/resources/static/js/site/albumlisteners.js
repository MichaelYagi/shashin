$(document).ready(function () {
    const albumId = $("#albumId").val();

    $('#saveAlbumModal').on("click", async function (e) {
        e.preventDefault();
        $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
        $("#albumsModalStatus").visible();
        $("#albumsModalStatus").attr("title", "");
        $("#albumsModalCancel").prop('disabled', true);

        $('#albumModalMsg').html("");
        const metadataId = $("#metadataId").val();

        let requestJson = {
            setCoverAlbum: $('#setCoverAlbum').val() === "yes",
            metadataId: metadataId,
            albumId: albumId
        };

        const http = new Http("updating album");
        const data = await http.ajax("post", "/album/update", JSON.stringify(requestJson), function () {
            $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
            $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#albumsModalCancel").prop('disabled', false);
        });

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === "redirect") {
                // window.location.replace(data.msg);
                window.top.location = window.top.location;
                $("#albumMessage").html('<div class="alert alert-success" role="alert">' + data.msg + '</div>');
            } else {
                if (data.status === shashin.apiResponse.SUCCESS) {
                    $("#albumsModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                    $("#albumsModalCancel").prop('disabled', false);

                    shashin.showToastMessage(shashin.getTranslatedValue("main.sidebar.album"), shashin.getTranslatedValue("main.pages.albums.updated"), {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        tag: "albummodal",
                        borderColor:"success"
                    });

                    $("#propAlbumModal").modal('hide');
                } else {
                    $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                    $("#albumsModalCancel").prop('disabled', false);
                }
            }
        } else {
            $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#albumsModalCancel").prop('disabled', false);
        }

        return false;
    });

    $('#propAlbumModal').on('hide.bs.modal', function () {
        $("#albumsModalStatus").attr("class","spinner-grow me-auto");
        $("#albumsModalStatus").invisible();
    });
});