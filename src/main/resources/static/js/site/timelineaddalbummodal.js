(function( timelineAddAlbumModal, $, undefined ) {
    timelineAddAlbumModal.toggleBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('toggle');
    }

    timelineAddAlbumModal.closeBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('hide');
    }

    timelineAddAlbumModal.populateBatchAlbum = function() {
        const checkedBoxes = $('input[name="albums[]"]:checked');
        let albumsString = "";
        checkedBoxes.each(function() {
            albumsString += $(this).val().replace(/ +(?= )/g,'').trim() + ",";
        });
        if (albumsString.length > 0) {
            albumsString = albumsString.slice(0,-1)
        }
        $("#albumNameInput").val(shashin.decodeHtml(albumsString));
    }
}( window.timelineAddAlbumModal = window.timelineAddAlbumModal || {}, jQuery ));

$('#propAddAlbum').on('hide.bs.modal', function () {
    $("#albumNameInput").val("");
    $("#albumResponseMsg").html("");
})

$("#saveAlbum").click(function (e) {
    e.preventDefault();

    $("#albumResponseMsg").html("");

    let albumMetadataIds = $.trim($("#albumMetadataIds").val());
    let albumNameInputs = $.trim($("#albumNameInput").val());

    if (albumMetadataIds !== "" && albumNameInputs !== "") {
        let albumMetadataIdArray = $.parseJSON(albumMetadataIds);
        let albumNameArray = albumNameInputs.split(",");

        const posting = $.post({
            url: "/albums/add",
            data: JSON.stringify({albumNames:albumNameArray,albumMetadataIds:albumMetadataIdArray}),
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