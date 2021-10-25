function populateBatchAlbumInput(e,albumId,albumName) {
    e.preventDefault();
    $("#albumNameInput").val(shashin.decodeHtml(albumName));
    $("#albumIdInput").val(albumId);
}

$('#propAddAlbum').on('hide.bs.modal', function () {
    $("#albumNameInput").val("");
    $("#albumResponseMsg").html("");
})

$("#saveAlbum").click(function (e) {
    e.preventDefault();

    $("#albumResponseMsg").html("");

    let albumMetadataIds = $.trim($("#albumMetadataIds").val());
    let albumNameInput = $.trim($("#albumNameInput").val());

    if (albumMetadataIds !== "" && albumNameInput !== "") {
        let albumIdInput = $.trim($("#albumIdInput").val())
        let albumMetadataIdArray = $.parseJSON(albumMetadataIds);

        if (albumIdInput === "" && $('#albumNameList').length > 0) {
            // Iterate through list and find id by name
            $('#albumNameList').children('a').each(function() {
                if (albumNameInput.toLowerCase() === $(this).text().toLowerCase()) {
                    albumIdInput = $(this).attr('id');
                    return false;
                }
            });
        }

        const posting = $.post({
            url: "/albums/add",
            data: JSON.stringify({albumId:albumIdInput,albumName:albumNameInput,albumMetadataIds:albumMetadataIdArray}),
            contentType: 'application/json; charset=utf-8'
        });

        posting.done(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    //window.top.location = window.top.location
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                }
                $("#albumResponseMsg").html(message);
                shashin.clearTimelineSelection();
                shashin.removeAllMetadataIdList();
                shashin.removeAllMetadataFilenamesList();
                shashin.removeAllMetadataThumbnailsList();
            }
        });
    } else {
        $("#albumResponseMsg").html("<div class=\"alert alert-danger\" role=\"alert\">Must enter or select album</div>");
    }

    return false;
});

// Clear message on modal close
$('#propAddAlbum').on('hide.bs.modal', function () {
    $("#albumResponseMsg").html("");
});

// Clear message on input editing
$('#propAddAlbum').bind('keypress', function() {
    $("#albumResponseMsg").html("");
});