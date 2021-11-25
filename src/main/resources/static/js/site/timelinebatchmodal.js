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
        $("#tagBatchDataInput").val(Util.decodeHtml(labelString));
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
        $("#albumNameInput").val(Util.decodeHtml(albumsString));
    }

}( window.timelineBatchModal = window.timelineBatchModal || {}, jQuery ));

$("#saveBatchMetadata").click(function (e) {
    e.preventDefault();
    $("#msgBatchMetadata").html("");
    $("#timelineBatchModalStatus").css("visibility","visible");
    timelineBatchModal.closeBatchTagPeopleDropdown();
    timelineBatchModal.closeBatchTagAlbumDropdown();

    if (Util.validateMetadataInputs(
        $("#dayTakenBatchData").val(),
        $("#monthTakenBatchData").val(),
        $("#yearTakenBatchData").val(),
        "",
        "",
        $("#latlngBatchData").val(),
        "msgBatchMetadata"
    ) === true) {
        if($("#batchisobject").is(':checked')) {
            $("#batchisobject").val("on");
        }
        if($("#batchhidden").is(':checked')) {
            $("#batchhidden").val("on");
        }

        let ajaxParams = {}

        if($("#batchhidden").is(':checked')) {
            ajaxParams = {
                type: "post",
                url: "/timeline/remove/batch",
                data: JSON.stringify(Util.serializeObject($('#saveBatchData'))),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }
        } else {
            ajaxParams = {
                type: "post",
                url: "/timeline/update/batch",
                data: JSON.stringify(Util.serializeObject($('#saveBatchData'))),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }
        }

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating batch timeline modal")}).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {

                    const metadataIds = JSON.parse($("#batchMetadataIds").val());
                    for (const index in metadataIds) {
                        const metadataId = metadataIds[index];

                        if ($("#photoThumbnailContainer" + metadataId).length > 0) {
                            $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil").addClass("bi-pencil-square");

                            if ($("#batchhidden").is(':checked')) {
                                // Get parent Id
                                const rowId = $("#photoThumbnailContainer" + metadataId).parent().attr("id");
                                const headingId = rowId.replace("row", "");
                                const elToRemove = $("#photoThumbnailContainer" + metadataId);

                                // Count children
                                const currentNumChildren = elToRemove.siblings("div").length;

                                // Remove metadata
                                elToRemove.remove();

                                if (currentNumChildren === 0) {
                                    // Remove header
                                    $("#br" + headingId).remove();
                                    $("#" + headingId).remove();
                                    $("#row" + headingId).remove();
                                }
                            } else if ($("#latlngBatchData").val() !== "") {
                                const latlngArray = $("#latlngBatchData").val().split(",");
                                const metadataTaggedObj = JSON.parse($("#timelineModalEdit" + metadataId).attr("tag"));
                                metadataTaggedObj["lat"] = $.trim(latlngArray[0]);
                                metadataTaggedObj["lng"] = $.trim(latlngArray[1]);
                                $("#timelineModalEdit" + metadataId).attr("tag", JSON.stringify(metadataTaggedObj));
                                $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil-square").addClass("bi-pencil");
                            }
                        }
                    }

                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    $("#timelineBatchModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                    // window.top.location = window.top.location
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#timelineBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                }

                if ($("#offcanvasToc").length > 0) {
                    const offCanvasId = ($("#dayTakenBatchData").val() == null || $("#dayTakenBatchData").val() === "" || $("#monthTakenBatchData").val() == null || $("#monthTakenBatchData").val() === "" || $("#yearTakenBatchData").val() == null || $("#yearTakenBatchData").val() == "") ?
                        "offcanvas_undated" : "offcanvas_" + $("#yearTakenBatchData").val() + '-' + $("#monthTakenBatchData").val() + '-' + $("#dayTakenBatchData").val();
                    shashin.refreshTimeline($("#mediaTypeFilter").val(), offCanvasId);
                }

                //$("#msgBatchMetadata").html(message);
                //$("#timelineBatchModalStatus").css("visibility","hidden");

                shashin.clearTimelineSelection();
                shashin.removeAllMetadataIdList();
                shashin.removeAllMetadataFilenamesList();
                shashin.removeAllMetadataThumbnailsList();
                $("#batchMetadataIds").val("");
                $("#batchFilenames").val("");
                $("#dayTakenBatchData").val("");
                $("#monthTakenBatchData").val("");
                $("#yearTakenBatchData").val("");
                $("#latlngBatchData").val("");
                $("#keywordsBatchData").val("");
                $("#albumNameInput").val("");
                $("#tagBatchDataInput").val("");
                $("#batchisobject")[0].checked = false;
                $("#batchhidden")[0].checked = false;
            }
        });
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

$("#batchisobject").click(function (e) {
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
    $('#keywordsBatchData').val('');
    timelineBatchModal.closeBatchTagPeopleDropdown();
    timelineBatchModal.closeBatchTagAlbumDropdown();
});

// Clear message on input editing
$('#propBatchMetadata').bind('keypress', function() {
    $("#timelineBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#timelineBatchModalStatus").css("visibility","hidden");
    $("#msgBatchMetadata").html("");
});

$('body').click(function(event) {
    if (!$(event.target).closest("#tagalbumdropdown").length && !$(event.target).closest("#albumNameList").length && $("#tagalbumdropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagAlbumDropdown();
    }

    if (!$(event.target).closest("#tagpeopledropdown").length && !$(event.target).closest("#peopleNameList").length && $("#tagpeopledropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagPeopleDropdown();
    }
});