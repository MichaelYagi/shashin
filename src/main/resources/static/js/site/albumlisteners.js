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

    $('#saveAlbumModal').click(function (e) {
        // $('#saveAlbum'+metadata.id).click(function (e) {
        e.preventDefault();
        $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
        $("#albumsModalStatus").css("visibility","visible");
        $("#albumsModalStatus").attr("title", "");
        $("#albumsModalCancel").prop('disabled', true);

        $('#albumModalMsg').html("");
        const metadataId = $("#metadataId").val();

        let requestJson = {
            removeFromAlbum:$('#removeFromAlbum').prop("checked"),
            setCoverAlbum:$('#setCoverAlbum').prop("checked"),
            metadataId:metadataId,
            albumId:albumId
        }

        const ajaxParams = {
            type: "post",
            url: "/album/update",
            data: JSON.stringify(requestJson),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        $.ajax(ajaxParams)
            .fail(function(xhr, textStatus) {
                $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#albumsModalCancel").prop('disabled', false);
                shashin.onFail(xhr, textStatus, ajaxParams, " saving album");
            }).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === "redirect") {
                    window.location.replace(data["msg"]);
                } else {
                    let message = "Error";
                    if (data["status"] === "success") {
                        message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        // window.top.location = window.top.location;
                        $("#albumsModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                        $("#albumsModalCancel").prop('disabled', false);
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                        $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                        $("#albumsModalCancel").prop('disabled', false);
                    }
                    //$('#albumModalMsg').html(message);
                }
            } else {
                $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#albumsModalCancel").prop('disabled', false);
            }
        });
        return false;
    });

    $('#propAlbumModal').on('hide.bs.modal', function () {
        $("#albumsModalStatus").attr("class","spinner-grow me-auto");
        $("#albumsModalStatus").css("visibility","hidden");
    });
});