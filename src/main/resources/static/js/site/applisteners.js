$("#appToolsBatchEdit").click(function(e) {
    e.preventDefault();

    let thumbnailList = "";
    let metadataIdArray = shashin.getMetdataIdList();
    let metadataFilenamesArray = shashin.getMetadataFilenamesList();
    let metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
    for (let index in metadataThumbnailsArray) {
        const metadataThumnailUrl = metadataThumbnailsArray[index];
        thumbnailList += '<img src="'+metadataThumnailUrl+'" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="'+metadataFilenamesArray[index]+'" onError="shashin.errorImg(this,\''+metadataFilenamesArray[index]+'\',75)">';
    }

    $("#batchMetadataIds").val(JSON.stringify(metadataIdArray));
    $("#batchisobject")[0].checked = false;
    $("#batchhidden")[0].checked = false;
    if (thumbnailList !== "") {
        $("#editPhotosNamesModalLabel").html(thumbnailList);
    }
    $("#propBatchMetadata").modal('show');
});

$("#appToolsDeselectAll").click(function(e) {
    e.preventDefault();

    shashin.clearTimelineSelection();
});

$("#albumAppToolsDeselectAll").click(function(e) {
    e.preventDefault();

    shashin.clearTimelineSelection();
});

$("#appToolsAddAlbum").click(function(e) {
    e.preventDefault();

    let thumbnailList = "";
    let metadataFilenamesArray = shashin.getMetadataFilenamesList();
    let metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
    for (let index in metadataThumbnailsArray) {
        const metadataThumnailUrl = metadataThumbnailsArray[index];
        thumbnailList += '<img src="'+metadataThumnailUrl+'" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="'+metadataFilenamesArray[index]+'" onError="shashin.errorImg(this,\''+metadataFilenamesArray[index]+'\',75)">';
    }

    if (thumbnailList !== "") {
        $("#editAlbumPhotosNamesModalLabel").html(thumbnailList);
    }
    let metadataIdList = shashin.getMetdataIdList();
    $("#albumMetadataIds").val(JSON.stringify(metadataIdList));
    $("#propAddAlbum").modal('show');
});

$("#albumAppToolsRemoveAlbum").click(function(e) {
    e.preventDefault();

    let metadataIdList = [];
    $('.bi-circle-fill').each(function(i, obj) {
        metadataIdList.push(obj.id.substring(6, obj.id.length));
    });

    let albumId = $('#albumId').text();
    if (metadataIdList.length > 0 && albumId.length > 0) {
        let json = {metadataIdList: metadataIdList, albumId: parseInt(albumId)}
        const ajaxParams = {
            type: "post",
            url: "/album/delete/batch",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error removing from album. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
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
                        window.top.location = window.top.location
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    }
                    $("#albumMessage").html(message);
                }
            }
        });
    }
});

$("#albumAppToolsRemoveFavorites").click(function(e) {
    e.preventDefault();

    let metadataIdList = [];
    $('.bi-circle-fill').each(function(i, obj) {
        metadataIdList.push(obj.id.substring(6, obj.id.length));
    });

    if (metadataIdList.length > 0) {
        let json = {metadataIdList: metadataIdList}
        const ajaxParams = {
            type: "post",
            url: "/favorites/delete",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error removing favories. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        $.ajax(ajaxParams)
        .fail(onFail).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    location.reload();
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                }
                $("#favoritesMessage").html(message);
            }
        });
    }

    return false;
});

$("#albumAppToolsUntrash").click(function(e) {
    e.preventDefault();

    let metadataIdList = [];
    $('.bi-circle-fill').each(function(i, obj) {
        metadataIdList.push(obj.id.substring(6, obj.id.length));
    });

    if (metadataIdList.length > 0) {
        let json = {metadataIdList: metadataIdList}
        const ajaxParams = {
            type: "post",
            url: "/trash/unhide",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error untrashing. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        $.ajax(ajaxParams)
        .fail(onFail).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    window.top.location = window.top.location
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                }
                $("#trashMessage").html(message);
            }
        });
    }

    return false;
});