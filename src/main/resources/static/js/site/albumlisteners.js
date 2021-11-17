$(document).ready(function () {
    const metadataId = $("#metadataId").val();
    const albumId = $("#albumId").val();
    const albumName = $("#albumName").val();
    const authority = $("#authority").val();

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
        $("#albumsModalStatus").css("visibility","visible");



        $('#albumModalMsg').html("");

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

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error saving album. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        $.ajax(ajaxParams)
            .fail(onFail).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === "redirect") {
                    window.location.replace(data["msg"]);
                } else {
                    let message = "Error";
                    if (data["status"] === "success") {
                        message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        // window.top.location = window.top.location;
                        $("#albumsModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                    }
                    //$('#albumModalMsg').html(message);
                }
            }
        });
        return false;
    });

    $('#propAlbumModal').on('hide.bs.modal', function () {
        $("#albumsModalStatus").attr("class","spinner-grow me-auto");
        $("#albumsModalStatus").css("visibility","hidden");
    });

    if (authority === 'ROLE_ADMIN') {
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
            if (val.length > 0) {
                let json = {albumId: albumId,albumName: val}
                const ajaxParams = {
                    type: "post",
                    url: "/album/updatename/"+albumId,
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8',
                    retries: shashin.ajaxRetries
                }

                function onFail(xhr, textStatus) {
                    shashin.printMessageToConsole("AJAX error updating album name. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
                    if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                        $.ajax(ajaxParams).fail(onFail);
                    }
                }

                $.ajax(ajaxParams)
                    .fail(onFail).then(function (data) {
                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data["status"] === "success") {
                        $('#albumNameHeader').html('<h1 id="albumNameHeading">'+val+'</h1>');
                    } else {
                        $('#albumNameHeader').html('<h1 id="albumNameHeading">'+albumName+'</h1>');
                    }
                });
            }
        }
    }
});