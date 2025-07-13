$("#appToolsBatchEdit,#matchToolsBatchEdit").on("click", function(e) {
    e.preventDefault();

    let thumbnailList = "";
    let metadataIdArray = shashin.getMetadataIdList();
    let metadataFilenamesArray = shashin.getMetadataFilenamesList();
    let metadataThumbnailsArray = shashin.getMetadataThumbnailsList();

    if (Util.isMobile() && metadataThumbnailsArray.length > 3) {
        thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[0] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[0] + '" draggable="false">';
        thumbnailList += '<span class="bi-arrow-left ms-1 me-1 display-6 align-middle"></span><span class="display-6 align-middle">'+(metadataIdArray.length-2).toString()+'</span><span class="bi-arrow-right ms-1 me-1 display-6 align-middle"></span>';
        thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[metadataThumbnailsArray.length-1] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[metadataFilenamesArray.length-1] + '" draggable="false">';
    } else if (Util.isMobile() === false && metadataThumbnailsArray.length > 5) {
        thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[0] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[0] + '" draggable="false">';
        thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[1] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[1] + '" draggable="false">';
        thumbnailList += '<span class="bi-arrow-left ms-1 me-1 display-6 align-middle"></span><span class="display-6 align-middle">'+(metadataIdArray.length-4).toString()+'</span><span class="bi-arrow-right ms-1 me-1 display-6 align-middle"></span>';
        thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[metadataThumbnailsArray.length-2] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[metadataFilenamesArray.length-2] + '" draggable="false">';
        thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[metadataThumbnailsArray.length-1] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[metadataFilenamesArray.length-1] + '" draggable="false">';
    } else {
        for (let index in metadataThumbnailsArray) {
            const metadataThumnailUrl = metadataThumbnailsArray[index];
            thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumnailUrl + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[index] + '" draggable="false">';
        }
    }

    $("#yearTakenBatchData ").attr("max", new Date().getFullYear());
    $("#batchMetadataIds").val(JSON.stringify(metadataIdArray));
    $("#batchisobject")[0].checked = false;
    $("#batchhidden")[0].checked = false;
    if (thumbnailList !== "") {
        $("#editPhotosNamesModalLabel").html(thumbnailList);
    }

    const keywordAvailableList = $("#keywordsBatchString").val().split(",");
    shashin.createAutocomplete("#keywordsBatchData", keywordAvailableList, true, 10);

    const cameraList = $("#camerasBatchString").val().split(",");
    shashin.createAutocomplete("#cameraBatchData", cameraList, false);

    const lensList = $("#lensesBatchString").val().split(",");
    shashin.createAutocomplete("#lensBatchData", lensList, false);

    const http = new Http("batch save timeline");
    http.ajax("get", "/metadata/attributes").then(function (data) {
        shashin.processBatchAlbumList(data);
        shashin.processBatchPeopleList(data);

        const albumcheckedBoxes = $('input[name="albums[]');
        const albumNames = [];
        albumcheckedBoxes.each(function() {
            albumNames.push($(this).val().replace(/ +(?= )/g,'').trim());
        });
        if (albumcheckedBoxes.length > 0) {
            $("#albumBatchNameData").css("display", "block");

            $("#albumBatchNameData").on("click", function (e) {
                e.preventDefault();
                shashin.createBatchModalMultiselect("album");
            });
        }
        shashin.createAutocomplete("#albumNameInput", albumNames, false);
        shashin.syncCheckboxInputs("#albumNameInput", "albums");

        const peoplecheckedBoxes = $('input[name="recognitionLabel[]');
        const peopleNames = [];
        peoplecheckedBoxes.each(function() {
            peopleNames.push($(this).val().replace(/ +(?= )/g,'').trim());
        });
        if (peoplecheckedBoxes.length > 0) {
            $("#peopleBatchNameData").css("display", "block");

            $("#peopleBatchNameData").on("click", function (e) {
                e.preventDefault();
                shashin.createBatchModalMultiselect("people");
            });
        }
        shashin.createAutocomplete("#tagBatchDataInput", peopleNames, false);
        shashin.syncCheckboxInputs("#tagBatchDataInput", "recognitionLabel");
    });

    $("#propBatchMetadata").modal('show');
});

$("#appToolsBatchDownload").on("click", function(e) {
    e.preventDefault();

    // Download items
    shashin.downloadSelected("appToolsBatchDownload");
});

$("#matchToolsBatchDownload").on("click", function(e) {
    e.preventDefault();

    // Download items
    shashin.downloadSelected("matchToolsBatchDownload");
});

$("#albumAppToolsBatchDownload").on("click", function(e) {
    e.preventDefault();

    // Download items
    shashin.downloadSelected("albumAppToolsBatchDownload");
});

$("#appToolsDeselectAll").on("click", function(e) {
    e.preventDefault();

    shashin.clearTimelineSelection();
});

$("#albumAppToolsDeselectAll").on("click", function(e) {
    e.preventDefault();

    shashin.clearAlbumSelection();
});

$("#comprefaceDeselectAll").on("click", function(e) {
    e.preventDefault();

    shashin.clearTimelineSelection();
});

$("#appToolsAddAlbum").on("click", function(e) {
    e.preventDefault();

    let thumbnailList = "";
    let metadataFilenamesArray = shashin.getMetadataFilenamesList();
    let metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
    for (let index in metadataThumbnailsArray) {
        const metadataThumnailUrl = metadataThumbnailsArray[index];
        thumbnailList += '<img loading="lazy" src="'+metadataThumnailUrl+'" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="'+metadataFilenamesArray[index]+'" draggable="false">';
    }

    if (thumbnailList !== "") {
        $("#editAlbumPhotosNamesModalLabel").html(thumbnailList);
    }
    let metadataIdList = shashin.getMetadataIdList();
    $("#albumMetadataIds").val(JSON.stringify(metadataIdList));
    $("#propAddAlbum").modal('show');
});

$("#albumAppToolsRemoveAlbum").on("click", async function (e) {
    e.preventDefault();

    let metadataIdList = [];
    $(".thumbnail-tl .bi-circle-fill").each(function (i, obj) {
        metadataIdList.push(obj.id.substring(6, obj.id.length));
    });

    let albumId = $('#albumId').val();
    if (metadataIdList.length > 0 && albumId.length > 0) {
        const http = new Http("album batch delete");
        let json = {metadataIdList: metadataIdList, albumId: parseInt(albumId)}
        const data = await http.ajax("delete", "/album/media/delete/batch", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === "redirect") {
                window.location.replace(data.msg);
            } else {
                let message = "Error";
                if (data.status === shashin.apiResponse.SUCCESS) {
                    message = '<div class="alert alert-success" role="alert">' + data.msg + '</div>';
                    window.top.location = window.top.location
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
                }
                $("#albumMessage").html(message);
            }
        }
    }
});

$("#albumAppToolsRemoveFavorites").on("click", async function (e) {
    e.preventDefault();

    let metadataIdList = [];
    $('.bi-circle-fill').each(function (i, obj) {
        metadataIdList.push(obj.id.substring(6, obj.id.length));
    });

    if (metadataIdList.length > 0) {
        const http = new Http("album remove favorites");
        let json = {metadataIdList: metadataIdList}
        const data = await http.ajax("post", "/favorites/delete", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data.status === shashin.apiResponse.SUCCESS) {
                message = '<div class="alert alert-success" role="alert">' + data.msg + '</div>';
                location.reload();
            } else {
                message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
            }
            $("#favoritesMessage").html(message);
        }
    }

    return false;
});

$("#albumAppToolsRestore").on("click", async function (e) {
    e.preventDefault();

    $("#albumAppToolsRestoreIcon").removeClass("bi-arrow-repeat").addClass("spinner-border spinner-border-sm");
    $("#albumAppToolsRestoreIcon").css({
        "width": "1.5rem",
        "height": "1.5rem",
        "font-size": ""
    });

    let metadataIdList = [];
    $('.bi-circle-fill').each(function (i, obj) {
        metadataIdList.push(obj.id.substring(6, obj.id.length));
    });

    if (metadataIdList.length > 0) {
        const http = new Http("archive restore");
        let json = {metadataIdList: metadataIdList}
        const data = await http.ajax("post", "/unarchive", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data.status === shashin.apiResponse.SUCCESS) {
                shashin.showToastMessage("Success!", "Media restored", {
                    icon: "bi-info-circle",
                    iconColor: "#777777",
                    tag: "restoreArchive",
                    borderColor:"success"
                });

                const el = shashin.getToastElement(shashin.toast.placement.bottom.center, "restoreArchive");
                if (el !== null) {
                    $('#' + $(el).attr("id")).on('hidden.bs.toast', function () {
                        $("#albumAppToolsRestoreIcon").removeClass("spinner-border spinner-border-sm").addClass("bi-arrow-repeat");
                        $("#albumAppToolsRestoreIcon").css({
                            "width": "",
                            "height": "",
                            "font-size": "1.5rem"
                        });
                        location.replace(location.href.split('#')[0]);
                    });
                }
            } else {
                shashin.showToastMessage("Could not restore media", data.msg, {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor:"danger"});

                $("#albumAppToolsRestoreIcon").removeClass("spinner-border spinner-border-sm").addClass("bi-arrow-repeat");
                $("#albumAppToolsRestoreIcon").css({
                    "width": "",
                    "height": "",
                    "font-size": "1.5rem"
                });
            }
        }
    }

    return false;
});

let albumVideoRegex = new RegExp("\\/album\\/(\\d+)\\/video$");
let albumRegex = new RegExp("\\/album\\/(\\d+)$");

if (window.location.href.indexOf("/timeline/video") > -1) {
    $("#timelineMediaTypeToggleIcon").removeClass("bi-camera-video").addClass("bi-camera-video-fill");
} else if (window.location.href.indexOf("/timeline") > -1) {
    $("#timelineMediaTypeToggleIcon").removeClass("bi-camera-video-fill").addClass("bi-camera-video");
} else if (albumVideoRegex.test(window.location.href)) {
    $("#albumMediaTypeToggleIcon").removeClass("bi-camera-video").addClass("bi-camera-video-fill");
} else if (albumRegex.test(window.location.href)) {
    $("#albumMediaTypeToggleIcon").removeClass("bi-camera-video-fill").addClass("bi-camera-video");
}

$("#timelineMediaTypeToggle").on("click", function(e) {
    e.preventDefault();

    if (window.location.href.indexOf("/timeline/video") > -1) {
        window.location.replace("/timeline");
    } else {
        window.location.replace("/timeline/video");
    }
});

$("#albumMediaTypeToggleIcon").on("click", function(e) {
    e.preventDefault();

    const updatedStr = window.location.href.replace("\/video","")
    if (albumVideoRegex.test(window.location.href)) {
        window.location.replace(updatedStr);
    } else {
        window.location.replace(updatedStr + "/video");
    }
});