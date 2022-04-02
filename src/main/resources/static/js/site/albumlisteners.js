$(document).ready(function () {
    const albumId = $("#albumId").val();

    $("#removeFromAlbum").change(function() {
        if(this.checked) {
            $('#setCoverAlbum').prop('checked', false);
        }
    });
    $("#setCoverAlbum").change(function() {
        if(this.checked) {
            $('#removeFromAlbum').prop('checked', false);
        }
    });

    $('#saveAlbumModal').on("click", async function (e) {
        e.preventDefault();
        $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
        $("#albumsModalStatus").css("visibility", "visible");
        $("#albumsModalStatus").attr("title", "");
        $("#albumsModalCancel").prop('disabled', true);

        $('#albumModalMsg').html("");
        const metadataId = $("#metadataId").val();

        let requestJson = {
            removeFromAlbum: $('#removeFromAlbum').prop("checked"),
            setCoverAlbum: $('#setCoverAlbum').prop("checked"),
            metadataId: metadataId,
            albumId: albumId
        }

        const http = new Http("updating album");
        const data = await http.ajax("post", "/album/update", JSON.stringify(requestJson), function () {
            $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
            $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#albumsModalCancel").prop('disabled', false);
        });

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "redirect") {
                window.location.replace(data["msg"]);
            } else {
                if (data["status"] === "success") {
                    $("#albumsModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                    $("#albumsModalCancel").prop('disabled', false);
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
        $("#albumsModalStatus").css("visibility","hidden");
    });
});