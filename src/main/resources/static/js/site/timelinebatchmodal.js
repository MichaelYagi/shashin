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

        let ajaxParams = {};

        const batchObj = Util.serializeObject($('#saveBatchData'));

        if($("#batchhidden").is(':checked')) {
            ajaxParams = {
                type: "post",
                url: "/timeline/remove/batch",
                data: JSON.stringify(batchObj),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }
        } else {
            ajaxParams = {
                type: "post",
                url: "/timeline/update/batch",
                data: JSON.stringify(Util.getBatchData(batchObj)),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }
        }

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating batch timeline modal")}).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("keywords") && data["keywords"] !== "") {
                        $("#keywordsString").val(data["keywords"]);
                        $("#keywordsBatchString").val(data["keywords"]);
                    }
                    const metadataIds = JSON.parse($("#batchMetadataIds").val());
                    for (const index in metadataIds) {
                        const metadataId = metadataIds[index];

                        if ($("#image" + metadataId).length > 0) {
                            $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil").addClass("bi-pencil-square");

                            // Update tag
                            let takenDateUpdated = false;
                            const metadataObj = shashin.checkMetadata(metadataId);

                            if (($("#yearTakenBatchData").val().trim() !== "" && parseInt(metadataObj.year) !== parseInt($("#yearTakenBatchData").val())) ||
                                ($("#monthTakenBatchData").val().trim() !== "" && parseInt(metadataObj.month) !== parseInt($("#monthTakenBatchData").val())) ||
                                ($("#dayTakenBatchData").val().trim() !== "" && parseInt(metadataObj.day) !== parseInt($("#dayTakenBatchData").val())))
                            {
                                takenDateUpdated = true;
                                if ($("#yearTakenBatchData").val().trim() !== "") {
                                    metadataObj.year = $("#yearTakenBatchData").val()
                                }
                                if ($("#monthTakenBatchData").val().trim() !== "") {
                                    metadataObj.month = $("#monthTakenBatchData").val()
                                }
                                if ($("#dayTakenBatchData").val().trim() !== "") {
                                    metadataObj.day = $("#dayTakenBatchData").val()
                                }
                            }

                            if ($("#keywordsBatchData").val().trim() !== "") {
                                metadataObj.keywords = $("#keywordsBatchData").val()
                            }

                            if ($("#latlngBatchData").val().trim() !== "") {
                                const latlngArray = $("#latlngBatchData").val().split(",");
                                metadataObj.lat = $.trim(latlngArray[0])
                                metadataObj.lng = $.trim(latlngArray[1])
                                if (metadataObj.lat !== null && metadataObj.lng !== null && metadataObj.lat !== "" && metadataObj.lng !== "") {
                                    $("#latlng").val(metadataObj.lat + "," + metadataObj.lng)
                                }
                            }
                            if ($("#tagBatchDataInput").val().trim() !== "") {
                                metadataObj.tagpeople = $("#tagBatchDataInput").val()
                            }
                            if ($("#albumNameInput").val().trim() !== "") {
                                metadataObj.albumlist = $("#albumNameInput").val()
                            }
                            metadataObj.hidden = $("#batchhidden").prop("checked")

                            if (metadataObj.hidden === false) {
                                Util.populateDetailsInfo(metadataObj,"propTimelineModal")
                                $("#timelineModalEdit" + metadataId).attr("tag", JSON.stringify(metadataObj))
                                $("#mediaLink" + metadataId).attr("tag", JSON.stringify(metadataObj))

                                if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlngBatchData").val().trim !== "") {
                                    $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil-square").addClass("bi-pencil");
                                }

                                if (takenDateUpdated === true) {
                                    shashin.removeThumbnail(metadataId);
                                }
                            } else {
                                shashin.removeThumbnail(metadataId);
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

                // shashin.clearTimelineSelection();
                // $("#batchMetadataIds").val("");
                // $("#batchFilenames").val("");
                // $("#dayTakenBatchData").val("");
                // $("#monthTakenBatchData").val("");
                // $("#yearTakenBatchData").val("");
                // $("#latlngBatchData").val("");
                // $("#keywordsBatchData").val("");
                // $("#albumNameInput").val("");
                // $("#tagBatchDataInput").val("");
                // $("#batchisobject")[0].checked = false;
                // $("#batchhidden")[0].checked = false;
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
    $('#albumNameInput').val('');
    $('#keywordsBatchData').val('');
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

$('body').click(function(event) {
    if (!$(event.target).closest("#tagalbumdropdown").length && !$(event.target).closest("#albumNameList").length && $("#tagalbumdropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagAlbumDropdown();
    }

    if (!$(event.target).closest("#tagpeopledropdown").length && !$(event.target).closest("#peopleNameList").length && $("#tagpeopledropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagPeopleDropdown();
    }
});