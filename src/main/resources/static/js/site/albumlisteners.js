$(document).ready(function () {
    const albumId = $("#albumId").val();
    let albumName = $("#albumName").val();
    const canEdit = $("#canEdit").val();

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

    if (canEdit === "true") {
        // Album header to input
        $("#albumNameHeader").click(function () {
            if ($("#albumNameHeading").length > 0) {
                const inputText = $("#albumNameHeader").text();
                $(this).html('<h1><input id="albumNameHeaderEdit" type="text" style="width: 100%" value=""></h1>');
                $("#albumNameHeaderEdit").focus().val(inputText);
            }
        });

        // On blur, save the album title
        $('body').click(function(event) {
            if (!$(event.target).closest("#albumNameHeaderEdit").length && $("#albumNameHeaderEdit").length > 0 && $("#albumNameHeaderEdit").is(":focus") === false) {
                saveHeader($("#albumNameHeaderEdit").val());
            }
        });

        // On enter, save the album title
        $('#albumNameHeader').keypress(function (e) {
            const key = e.which;
            if (key === 13) {
                const val = $("#albumNameHeaderEdit").val();
                saveHeader(val);
                return false;
            }
        });

        function saveHeader(val) {
            val = Util.escapeHtml(val);

            if (val.length > 0) {
                let json = {albumId: albumId,albumName: val}
                const ajaxParams = {
                    type: "post",
                    url: "/album/updatename/"+albumId,
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8',
                    retries: shashin.ajaxRetries
                }

                $.ajax(ajaxParams)
                    .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating album name")}).then(function (data) {

                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data["status"] === "success") {
                        $('#albumNameHeader').html('<h1 id="albumNameHeading">'+val+'</h1>');
                    } else {
                        $('#albumNameHeader').html('<h1 id="albumNameHeading">'+val+'</h1>');
                    }
                });
            }
        }
    }
});