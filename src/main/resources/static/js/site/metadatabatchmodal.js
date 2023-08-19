(function( metadataBatchModal, $, undefined ) {

    metadataBatchModal.toggleBatchTagPeopleDropdown = function() {
        $("#tagbatchpeopledropdown").dropdown('toggle');
    }

    metadataBatchModal.closeBatchTagPeopleDropdown = function() {
        $("#tagbatchpeopledropdown").dropdown('hide');
    }

    metadataBatchModal.populateBatchLabel = function() {
        const checkedBoxes = $('input[name="recognitionLabel[]"]:checked');
        let labelString = "";
        checkedBoxes.each(function() {
            labelString += $(this).val() + ",";
        });
        if (labelString.length > 0) {
            labelString = labelString.slice(0,-1)
        }
        $("#tagBatchDataInput").val(labelString);
    }

    metadataBatchModal.toggleBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('toggle');
    }

    metadataBatchModal.closeBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('hide');
    }

    metadataBatchModal.populateBatchAlbum = function() {
        const checkedBoxes = $('input[name="albums[]"]:checked');
        let albumsString = "";
        checkedBoxes.each(function() {
            albumsString += $(this).val().replace(/ +(?= )/g,'').trim() + ",";
        });
        if (albumsString.length > 0) {
            albumsString = albumsString.slice(0,-1)
        }
        $("#albumNameInput").val(albumsString);
    }

}( window.metadataBatchModal = window.metadataBatchModal || {}, jQuery ));

$("#saveBatchMetadata").on("click", async function (e) {
    e.preventDefault();

    $("#metadataBatchModalCancel").prop("disabled", true);
    $("#msgBatchMetadata").html("");
    $("#metadataBatchModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#metadataBatchModalStatus").css("visibility", "visible");
    $("#metadataBatchModalStatus").attr("title", "");
    metadataBatchModal.closeBatchTagPeopleDropdown();
    metadataBatchModal.closeBatchTagAlbumDropdown();
    const activePage = $("#activePage").val();
    const markedHidden = $("#batchhidden").prop("checked");
    const albumInputVal = $("#albumNameInput").val();
    const subjectInputVal = $("#tagBatchDataInput").val();
    const metadataIds = JSON.parse($("#batchMetadataIds").val());

    const metadataChangeMap = {};
    if ($("#yearTakenBatchData").val().trim() !== "" || $("#monthTakenBatchData").val().trim() !== "" || $("#dayTakenBatchData").val().trim() !== "") {
        for (const index in metadataIds) {
            const metadataId = metadataIds[index];

            shashin.getMetadata(metadataId).then(function (metadataObj) {
                metadataChangeMap[metadataId] = parseInt(metadataObj.year) !== parseInt($("#yearTakenBatchData").val()) ||
                    parseInt(metadataObj.month) !== parseInt($("#monthTakenBatchData").val()) ||
                    parseInt(metadataObj.day) !== parseInt($("#dayTakenBatchData").val());
            });
        }
    }

    if (Util.validateMetadataInputs(
        $("#dayTakenBatchData").val(),
        $("#monthTakenBatchData").val(),
        $("#yearTakenBatchData").val(),
        "",
        $("#offsetTakenBatchData").val(),
        $("#latlngBatchData").val(),
        "msgBatchMetadata"
    ) === true) {
        if ($("#batchisobject").is(':checked')) {
            $("#batchisobject").val("on");
        }
        if ($("#batchhidden").is(':checked')) {
            $("#batchhidden").val("on");
        }

        const http = new Http("batch save timeline");
        const batchObj = Util.serializeObject($('#saveBatchData'));
        let data;

        if ($("#batchhidden").is(':checked')) {
            data = await http.ajax("post", "/metadata/remove/batch", JSON.stringify(batchObj), function () {
                $("#metadataBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#metadataBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#metadataBatchModalCancel").prop("disabled", false);
            });
        } else {
            data = await http.ajax("put", "/metadata/update/batch", JSON.stringify(Util.getBatchData(batchObj)), function () {
                $("#metadataBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#metadataBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#metadataBatchModalCancel").prop("disabled", false);
            });
        }

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data["status"] === "success") {
                if (data.hasOwnProperty("keywords") && data["keywords"] !== "") {
                    $("#keywordsString").val(data["keywords"]);
                    $("#keywordsBatchString").val(data["keywords"]);
                }

                if (data.hasOwnProperty("cameras") && data["cameras"] !== "") {
                    $("#camerasString").val(data["cameras"]);
                    $("#camerasBatchString").val(data["cameras"]);
                }

                if (data.hasOwnProperty("lenses") && data["lenses"] !== "") {
                    $("#lensesString").val(data["lenses"]);
                    $("#lensesBatchString").val(data["lenses"]);
                }

                shashin.processBatchAlbumList(data, albumInputVal);

                shashin.processBatchPeopleList(data, subjectInputVal);

                let dateGalleryRemoved = false;
                for (const index in metadataIds) {
                    const metadataId = metadataIds[index];

                    if ($("#image" + metadataId).length > 0) {
                        $("#metadataModalEdit" + metadataId + " span").removeClass("bi-pencil").addClass("bi-pencil-square");

                        const metadataObj = {};

                        if ($("#latlngBatchData").val().trim() !== "") {
                            const latlngArray = $("#latlngBatchData").val().split(",");
                            metadataObj.lat = $.trim(latlngArray[0])
                            metadataObj.lng = $.trim(latlngArray[1])
                            if (metadataObj.lat !== null && metadataObj.lng !== null && metadataObj.lat !== "" && metadataObj.lng !== "") {
                                $("#latlng").val(metadataObj.lat + "," + metadataObj.lng)
                            }
                        }

                        metadataObj.hidden = markedHidden;

                        if (metadataObj.hidden === false) {
                            $("#metadataModalEdit" + metadataId).attr("tag", metadataId);
                            $("#mediaLink" + metadataId).attr("tag", metadataId);

                            if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlngBatchData").val().trim !== "") {
                                $("#metadataModalEdit" + metadataId + " span").removeClass("bi-pencil-square").addClass("bi-pencil");
                            }

                            if (metadataChangeMap.hasOwnProperty(metadataId) && metadataChangeMap[metadataId] === true && activePage === "timeline") {
                                dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                            }
                        } else if (activePage === "timeline") {
                            dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                        }

                        if (parseInt(index) === (metadataIds.length - 1)) {
                            if ($("#offcanvasToc").length > 0 && (($("#yearTakenBatchData").val().trim() !== "" || $("#monthTakenBatchData").val().trim() !== "" || $("#dayTakenBatchData").val().trim() !== "") || metadataObj.hidden === true)) {
                                timelineSettings.refreshTimeline($("#mediaTypeFilter").val()).then(function () {
                                    // If a date section was removed refresh the timeline
                                    if (dateGalleryRemoved === true) {
                                        const elements = Util.elementsInViewport($(".scrollspy"));
                                        let firstElementId = $(elements[0]).attr("id");
                                        let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
                                        timelineSettings.jumpFromTimelineToc(null, firstVisibleId, $("#mediaTypeFilter").val());
                                    }
                                });
                                if (index === metadataIds.length - 1) {
                                    Util.setMetadataLocalStorage();
                                }
                            }

                            if ($("#latlngBatchData").val().trim() !== "") {
                                Util.setMetadataLocalStorage();
                            }
                        }
                    }
                }

                $("#metadataBatchModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                $("#metadataBatchModalCancel").prop("disabled", false);

                if (activePage !== "timeline") {
                    if (markedHidden === true ||
                        (activePage !== "recent" && activePage !== "modified" && activePage !== "folder" &&
                            ($("#yearTakenBatchData").val() !== "" ||
                            $("#monthTakenBatchData").val() !== "" ||
                            $("#dayTakenBatchData").val() !== "")
                        )
                    ) {
                        window.location.reload();
                    }

                    if (activePage === "album" && albumInputVal !== "") {
                        window.location.reload();
                    }
                }
            } else {
                $("#metadataBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#metadataBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#metadataBatchModalCancel").prop("disabled", false);
            }
        } else {
            $("#metadataModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            $("#metadataBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#metadataBatchModalCancel").prop("disabled", false);
        }
    } else {
        $("#metadataBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
    }

    return false;
});

$('#propAddAlbum').on('hide.bs.modal', function () {
    $("#albumNameInput").val("");
    $("#albumResponseMsg").html("");
})

// Clear message on input editing
$('#propAddAlbum').bind('keypress', function() {
    $("#albumResponseMsg").html("");
});

$("#batchisobject").on("click", function (e) {
    metadataBatchModal.closeBatchTagPeopleDropdown();
    if($(this).prop("checked") === true) {
        $("#tagBatchDataInput").val("");
    }
});

// Clear message on modal close
$('#propBatchMetadata').on('hide.bs.modal', function () {
    $("#metadataBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#metadataBatchModalStatus").css("visibility","hidden");
    $("#msgBatchMetadata").html("");
    $("#msgBatchMetadata").html("");
    $('input:checkbox').prop('checked', false);
    $('#batchMetadataIds').val('');
    $('#batchFilenames').val('');
    $('#dayTakenBatchData').val('');
    $('#monthTakenBatchData').val('');
    $('#yearTakenBatchData').val('');
    $('#latlngBatchData').val('');
    $('#tagBatchDataInput').val('');
    $('#albumNameInput').val('');
    $('#keywordsBatchData').val('');
    $('#cameraBatchData').val('');
    $('#lensBatchData').val('');
    $('#offsetTakenBatchData').val('');
    metadataBatchModal.closeBatchTagPeopleDropdown();
    metadataBatchModal.closeBatchTagAlbumDropdown();
    shashin.clearTimelineSelection();
});

// Clear message on input editing
$('#propBatchMetadata').bind('keypress', function() {
    $("#metadataBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#metadataBatchModalStatus").css("visibility","hidden");
    $("#msgBatchMetadata").html("");
});

$('body').on("click", function(event) {
    if (!$(event.target).closest("#tagalbumdropdown").length && !$(event.target).closest("#albumNameList").length && $("#tagalbumdropdown").hasClass("show")) {
        metadataBatchModal.toggleBatchTagAlbumDropdown();
    }

    if (!$(event.target).closest("#tagbatchpeopledropdown").length && !$(event.target).closest("#peopleNameList").length && $("#tagbatchpeopledropdown").hasClass("show")) {
        metadataBatchModal.toggleBatchTagPeopleDropdown();
    }
});