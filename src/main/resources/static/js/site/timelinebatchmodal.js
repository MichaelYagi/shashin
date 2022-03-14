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

$("#saveBatchMetadata").click(function (e) {
    e.preventDefault();

    $("#timelineBatchModalCancel").prop("disabled", true);
    $("#msgBatchMetadata").html("");
    $("#timelineBatchModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#timelineBatchModalStatus").css("visibility","visible");
    $("#timelineBatchModalStatus").attr("title", "");
    timelineBatchModal.closeBatchTagPeopleDropdown();
    timelineBatchModal.closeBatchTagAlbumDropdown();
    const activePage = $("#activePage").val();

    const metadataIds = JSON.parse($("#batchMetadataIds").val());

    const metadataChangeMap = {};
    if ($("#yearTakenBatchData").val().trim() !== "" || $("#monthTakenBatchData").val().trim() !== ""|| $("#dayTakenBatchData").val().trim() !== "") {
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
        if($("#batchisobject").is(':checked')) {
            $("#batchisobject").val("on");
        }
        if($("#batchhidden").is(':checked')) {
            $("#batchhidden").val("on");
        }

        let ajaxParams = {};

        const batchObj = Util.serializeObject($('#saveBatchData'));

        if ($("#batchhidden").is(':checked')) {
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
        .fail(function(xhr, textStatus) {
            $("#timelineBatchModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
            $("#timelineBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#timelineBatchModalCancel").prop("disabled", false);
            shashin.onFail(xhr, textStatus, ajaxParams, " updating batch timeline modal");
        }).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("keywords") && data["keywords"] !== "") {
                        $("#keywordsString").val(data["keywords"]);
                        $("#keywordsBatchString").val(data["keywords"]);
                    }

                    if (data.hasOwnProperty("cameras") && data["cameras"] !== "") {
                        $("#camerasString").val(data["cameras"]);
                        $("#camerasBatchString").val(data["cameras"]);
                    }

                    if (data.hasOwnProperty("allAlbumList") && data["allAlbumList"].length > 0) {
                        let renderAlbumList = false;
                        const albumList = data["allAlbumList"];

                        let batchHtml =
                            '<input type="text" class="form-control" aria-label="Albums Name" id="albumNameInput" name="albumNameInput" value="">\n' +
                            '<div class="input-group-append dropdown">\n' +
                            '   <button class="btn btn-outline-secondary dropdown-toggle" onClick="return timelineBatchModal.toggleBatchTagAlbumDropdown();" id="tagalbumdropdown" type="button" aria-haspopup="true" aria-expanded="false">Albums</button>\n' +
                            '   <div class="dropdown-menu" id="albumNameList">\n';

                        for (let index in albumList) {
                            const album = albumList[index];

                            if ($("#"+album.id).length === 0) {
                                renderAlbumList = true;
                            }

                            batchHtml +=
                                '<button class="dropdown-item" type="button">\n' +
                                '    <input type="checkbox" onclick="return timelineBatchModal.populateBatchAlbum();" id="'+album.id+'" value="'+album.name+'" name="albums[]">\n' +
                                '    <label for="'+album.id+'">'+album.name+'</label>\n' +
                                '</button>\n';
                        }

                        batchHtml +=
                            '   </div>\n' +
                            '</div>\n';

                        if (true === renderAlbumList) {
                            $("#albumListForModal").html(batchHtml);
                        }
                    }

                    if (data.hasOwnProperty("recognitionLabels") && data["recognitionLabels"].length > 0) {
                        let renderRecognitionLabels = false;
                        const recognitionLabels = data["recognitionLabels"];

                        let batchHtml =
                            '       <input type="text" class="form-control" onfocus="return timelineBatchModal.closeBatchTagPeopleDropdown();" aria-label="Tag People" id="tagBatchDataInput" name="tagBatchDataInput" value="">\n' +
                            '       <div class="input-group-append">\n' +
                            '           <button class="btn btn-outline-secondary dropdown-toggle" onclick="return timelineBatchModal.toggleBatchTagPeopleDropdown();" id="tagpeopledropdown" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                            '           <div class="dropdown-menu" id="peopleNameList">';

                        for (let index in recognitionLabels) {
                            const recognitionLabel = recognitionLabels[index];

                            if ($("#"+recognitionLabel.id).length === 0) {
                                renderRecognitionLabels = true;
                            }

                            batchHtml +=
                                '           <button class="dropdown-item" type="button">\n' +
                                '               <input type="checkbox" onclick="return timelineBatchModal.populateBatchLabel();" id="'+recognitionLabel.id+'" value="'+recognitionLabel.name+'" name="recognitionLabel[]">\n' +
                                '               <label for="'+recognitionLabel.id+'">'+recognitionLabel.name+'</label>\n' +
                                '           </button>'
                        }
                        batchHtml +=
                            '   </div>\n' +
                            '</div>\n';

                        if (true === renderRecognitionLabels) {
                            $("#batchLabelIds").html(batchHtml);
                        }
                    }

                    let dateGalleryRemoved = false;
                    for (const index in metadataIds) {
                        const metadataId = metadataIds[index];

                        if ($("#image" + metadataId).length > 0) {
                            $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil").addClass("bi-pencil-square");

                            shashin.getMetadata(metadataId).then(function (metadataObj) {
                                if ($("#cameraBatchData").val().trim() !== "") {
                                    metadataObj.camera = $("#cameraBatchData").val()
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
                                    Util.populateDetailsInfo(metadataObj, "propTimelineModal")
                                    $("#timelineModalEdit" + metadataId).attr("tag", metadataId)
                                    $("#mediaLink" + metadataId).attr("tag", metadataId)

                                    if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlngBatchData").val().trim !== "") {
                                        $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil-square").addClass("bi-pencil");
                                    }

                                    if (metadataChangeMap.hasOwnProperty(metadataId) && metadataChangeMap[metadataId] === true && activePage === "timeline") {
                                        dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                                    }
                                } else if (activePage === "timeline") {
                                    dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                                }

                                if (parseInt(index) === (metadataIds.length-1)) {
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
                                        if (index === metadataIds.length-1) {
                                            Util.setMetadataLocalStorage();
                                        }
                                    }
                                }
                            });
                        }
                    }

                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    $("#timelineBatchModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                    $("#timelineBatchModalCancel").prop("disabled", false);
                    // window.top.location = window.top.location
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#timelineBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#timelineBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                    $("#timelineBatchModalCancel").prop("disabled", false);
                }
            } else {
                $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#timelineBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineBatchModalCancel").prop("disabled", false);
            }
        });
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

$('body').click(function(event) {
    if (!$(event.target).closest("#tagalbumdropdown").length && !$(event.target).closest("#albumNameList").length && $("#tagalbumdropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagAlbumDropdown();
    }

    if (!$(event.target).closest("#tagpeopledropdown").length && !$(event.target).closest("#peopleNameList").length && $("#tagpeopledropdown").hasClass("show")) {
        timelineBatchModal.toggleBatchTagPeopleDropdown();
    }
});