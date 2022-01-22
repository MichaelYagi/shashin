$("#appToolsBatchEdit").click(function(e) {
    e.preventDefault();

    let thumbnailList = "";
    let metadataIdArray = shashin.getMetdataIdList();
    let metadataFilenamesArray = shashin.getMetadataFilenamesList();
    let metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
    for (let index in metadataThumbnailsArray) {
        const metadataThumnailUrl = metadataThumbnailsArray[index];
        thumbnailList += '<img loading="lazy" src="'+metadataThumnailUrl+'" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="'+metadataFilenamesArray[index]+'" onError="Util.errorImg(this,\''+metadataFilenamesArray[index]+'\',75)">';
    }

    $("#batchMetadataIds").val(JSON.stringify(metadataIdArray));
    $("#batchisobject")[0].checked = false;
    $("#batchhidden")[0].checked = false;
    if (thumbnailList !== "") {
        $("#editPhotosNamesModalLabel").html(thumbnailList);
    }
    $("#keywordsBatchData").autocomplete({
        minLength: 0,
        source: function (request, response) {
            // delegate back to autocomplete, but extract the last term
            response($.ui.autocomplete.filter($("#keywordsBatchString").val().split(","), shashin.autocompleteExtractLast(request.term)));
        },
        focus: function () {
            // prevent value inserted on focus
            return false;
        },
        select: function (event, ui) {
            const terms = shashin.autocompleteSplit(this.value);
            // remove the current input
            terms.pop();
            // add the selected item
            terms.push(ui.item.value);
            // add placeholder to get the comma-and-space at the end
            terms.push("");
            this.value = terms.join(",");
            this.value = this.value.replace(/,\s*$/, "");
            return false;
        }
    });
    $("#keywordsBatchData").autocomplete( "option", "appendTo", "#saveBatchData" );
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
        thumbnailList += '<img loading="lazy" src="'+metadataThumnailUrl+'" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="'+metadataFilenamesArray[index]+'" onError="Util.errorImg(this,\''+metadataFilenamesArray[index]+'\',75)">';
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

    let albumId = $('#albumId').val();
    if (metadataIdList.length > 0 && albumId.length > 0) {
        let json = {metadataIdList: metadataIdList, albumId: parseInt(albumId)}
        const ajaxParams = {
            type: "post",
            url: "/album/delete/batch",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " removing from album")}).then(function (data) {
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

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " removing favorites")}).then(function (data) {
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

$("#albumAppToolsRestore").click(function(e) {
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

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " restore")}).then(function (data) {
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