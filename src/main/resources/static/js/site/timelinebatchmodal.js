(function( timelineBatchModal, $, undefined ) {

    timelineBatchModal.toggleBatchTagPeopleDropdown = function() {
        $("#tagpeopledropdown").dropdown('toggle');
    }

    timelineBatchModal.closeBatchTagPeopleDropdown = function() {
        $("#tagpeopledropdown").dropdown('hide');
    }

    timelineBatchModal.populateBatchLabel = function() {
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

    timelineBatchModal.toggleBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('toggle');
    }

    timelineBatchModal.closeBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('hide');
    }

    timelineBatchModal.populateBatchAlbum = function() {
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

}( window.timelineBatchModal = window.timelineBatchModal || {}, jQuery ));

$("#saveBatchMetadata").on("click", async function (e) {
    e.preventDefault();

    $("#timelineBatchModalCancel").prop("disabled", true);
    $("#msgBatchMetadata").html("");
    $("#timelineBatchModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#timelineBatchModalStatus").css("visibility", "visible");
    $("#timelineBatchModalStatus").attr("title", "");
    timelineBatchModal.closeBatchTagPeopleDropdown();
    timelineBatchModal.closeBatchTagAlbumDropdown();
    const activePage = $("#activePage").val();

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
            data = await http.ajax("post", "/timeline/remove/batch", JSON.stringify(batchObj), function () {
                $("#timelineBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#timelineBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineBatchModalCancel").prop("disabled", false);
            });
        } else {
            data = await http.ajax("post", "/timeline/update/batch", JSON.stringify(Util.getBatchData(batchObj)), function () {
                $("#timelineBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#timelineBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineBatchModalCancel").prop("disabled", false);
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

                shashin.processAlbumList(data, true);

                shashin.processPeopleList(data, true);

                let dateGalleryRemoved = false;
                for (const index in metadataIds) {
                    const metadataId = metadataIds[index];

                    if ($("#image" + metadataId).length > 0) {
                        $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil").addClass("bi-pencil-square");

                        const metadataObj = {};

                        if ($("#latlngBatchData").val().trim() !== "") {
                            const latlngArray = $("#latlngBatchData").val().split(",");
                            metadataObj.lat = $.trim(latlngArray[0])
                            metadataObj.lng = $.trim(latlngArray[1])
                            if (metadataObj.lat !== null && metadataObj.lng !== null && metadataObj.lat !== "" && metadataObj.lng !== "") {
                                $("#latlng").val(metadataObj.lat + "," + metadataObj.lng)
                            }
                        }

                        metadataObj.hidden = $("#batchhidden").prop("checked")

                        if (metadataObj.hidden === false) {
                            $("#timelineModalEdit" + metadataId).attr("tag", metadataId);
                            $("#mediaLink" + metadataId).attr("tag", metadataId);

                            if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlngBatchData").val().trim !== "") {
                                $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil-square").addClass("bi-pencil");
                            }

                            if (metadataChangeMap.hasOwnProperty(metadataId) && metadataChangeMap[metadataId] === true && activePage === "timeline") {
                                dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                            }
                        } else if (activePage === "timeline") {
                            dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                        }

                        if (parseInt(index) === (metadataIds.length - 1)) {
                            if ($("#offcanvasToc").length > 0 && (($("#yearTakenBatchData").val().trim() !== "" || $("#monthTakenBatchData").val().trim() !== "" || $("#dayTakenBatchData").val().trim() !== "") || metadataObj.hidden === true)) {
                                shashin.refreshTimeline($("#mediaTypeFilter").val()).then(function () {
                                    // If a date section was removed refresh the timeline
                                    if (dateGalleryRemoved === true) {
                                        const elements = $(".scrollspy").withinviewport()
                                        let firstElementId = $(elements[0]).attr("id");
                                        let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
                                        timelineSettings.jumpFromTimelineToc(null, firstVisibleId, $("#mediaTypeFilter").val());
                                    }
                                });
                                if (index === metadataIds.length - 1) {
                                    Util.setMetadataLocalStorage();
                                }
                            }
                        }
                    }
                }

                $("#timelineBatchModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                $("#timelineBatchModalCancel").prop("disabled", false);
            } else {
                $("#timelineBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#timelineBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineBatchModalCancel").prop("disabled", false);
            }
        } else {
            $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            $("#timelineBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#timelineBatchModalCancel").prop("disabled", false);
        }
    } else {
        $("#timelineBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
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
    timelineBatchModal.closeBatchTagPeopleDropdown();
    if($(this).prop("checked") === true) {
        $("#tagBatchDataInput").val("");
    }
});

// Clear message on modal close
$('#propBatchMetadata').on('hide.bs.modal', function () {
    $("#timelineBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#timelineBatchModalStatus").css("visibility","hidden");
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
    $('#offsetTakenBatchData').val('');
    timelineBatchModal.closeBatchTagPeopleDropdown();
    timelineBatchModal.closeBatchTagAlbumDropdown();
    shashin.clearTimelineSelection();
});

// Clear message on input editing
$('#propBatchMetadata').bind('keypress', function() {
    $("#timelineBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#timelineBatchModalStatus").css("visibility","hidden");
    $("#msgBatchMetadata").html("");
});

$('body').on("click", function(event) {
    if (!$(event.target).closest("#tagalbumdropdown").length && !$(event.target).closest("#albumNameList").length && $("#tagalbumdropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagAlbumDropdown();
    }

    if (!$(event.target).closest("#tagpeopledropdown").length && !$(event.target).closest("#peopleNameList").length && $("#tagpeopledropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagPeopleDropdown();
    }
});