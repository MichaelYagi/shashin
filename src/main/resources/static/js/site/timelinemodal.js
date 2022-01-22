(function( timelineModal, $, undefined ) {
    timelineModal.toggleTagPeopleDropdown = function (metadataId) {
        $("#tagpeopledropdown"+metadataId).dropdown('toggle')
    }

    timelineModal.closeTagPeopleDropdown = function (metadataId) {
        $("#tagpeopledropdown"+metadataId).dropdown('hide')
    }

    timelineModal.toggleAlbumDropdown = function (metadataId) {
        $("#albumdropdown"+metadataId).dropdown('toggle')
    }

    timelineModal.closeAlbumDropdown = function (metadataId) {
        $("#albumdropdown"+metadataId).dropdown('hide')
    }

    timelineModal.populateLabel = function (metadataId) {
        const checkedBoxes = $('input[name="recognitionLabel' + metadataId + '[]"]:checked');
        let labelString = "";

        checkedBoxes.each(function() {
            labelString += $(this).val() + ",";
        });

        if (labelString.length > 0) {
            labelString = labelString.slice(0,-1)
        }

        $("#tagpeople").val(Util.decodeHtml(labelString));
    }

    timelineModal.populateAlbum = function (metadataId) {
        const checkedBoxes = $('input[name="album' + metadataId + '[]"]:checked');
        let albumString = "";

        checkedBoxes.each(function() {
            albumString += $(this).val() + ",";
        });

        if (albumString.length > 0) {
            albumString = albumString.slice(0,-1)
        }

        $("#albumnames").val(Util.decodeHtml(albumString));
    }

}( window.timelineModal = window.timelineModal || {}, jQuery ));

$("#saveMetadata").click(function (e) {
    e.preventDefault();
    $("#timelineModalMsg").html("");
    $("#timelineModalStatus").css("visibility","visible");
    const metadataId = $("#metadataId").val();

    timelineModal.closeTagPeopleDropdown(metadataId);

    if (Util.validateMetadataInputs(
        $("#dayTaken").val(),
        $("#monthTaken").val(),
        $("#yearTaken").val(),
        $("#timeTaken").val(),
        $("#offsetTaken").val(),
        $("#latlng").val(),
        "timelineModalMsg"
    ) === true) {
        const json = {
            id:metadataId,
            title:$("#title").val().trim(),
            year:$("#yearTaken").val(),
            month:$("#monthTaken").val(),
            day:$("#dayTaken").val(),
            time:$("#timeTaken").val(),
            offset:$("#offsetTaken").val() === null ? "" : $("#offsetTaken").val(),
            latlng:$("#latlng").val(),
            keywords:$("#keywords").val(),
            tagpeople:$("#tagpeople").val(),
            albumnames:$("#albumnames").val(),
            hidden:$("#hidden").prop("checked"),
            isObject:$("#isobject").prop("checked")
        }

        let ajaxParams = {}

        if($("#hidden").is(':checked')) {
            ajaxParams = {
                type: "post",
                url: "/timeline/remove/" + metadataId,
                data: JSON.stringify(json),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }
        } else {
            ajaxParams = {
                type: "post",
                url: "/timeline/update/" + metadataId,
                data: JSON.stringify(json),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }
        }

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating timeline data")}).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("keywords") && data["keywords"] !== "") {
                        $("#keywordsString").val(data["keywords"]);
                    }
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    // window.top.location = window.top.location

                    // Update tag
                    let takenDateUpdated = false;
                    const metadataObj = shashin.checkMetadata(metadataId);
                    if (parseInt(metadataObj.year) !== parseInt($("#yearTaken").val()) ||
                        parseInt(metadataObj.month) !== parseInt($("#monthTaken").val()) ||
                        parseInt(metadataObj.day) !== parseInt($("#dayTaken").val()))
                    {
                        takenDateUpdated = true;
                    }
                    metadataObj.title = $("#title").val().trim() === "" ? $("#currentfilename").val() : $("#title").val().trim()
                    metadataObj.year = $("#yearTaken").val()
                    metadataObj.month = $("#monthTaken").val()
                    metadataObj.day = $("#dayTaken").val()
                    metadataObj.time = $("#timeTaken").val()
                    metadataObj.timeZone = $("#offsetTaken").val()
                    metadataObj.keywords = $("#keywords").val()
                    const latlngArray = $("#latlng").val().split(",");
                    metadataObj.lat = $.trim(latlngArray[0])
                    metadataObj.lng = $.trim(latlngArray[1])
                    if (metadataObj.lat !== null && metadataObj.lng !== null && metadataObj.lat !== "" && metadataObj.lng !== "") {
                        $("#latlng").val(metadataObj.lat+","+metadataObj.lng)
                    }
                    metadataObj.tagpeople = $("#tagpeople").val()
                    metadataObj.albumlist = $("#albumnames").val()
                    metadataObj.hidden = $("#hidden").prop("checked")

                    if ($("#offcanvasToc").length > 0) {
                        const offCanvasId = (metadataObj.year == null || metadataObj.month == null || metadataObj.day == null) ?
                            "offcanvas_undated" : "offcanvas_" + metadataObj.year + '-' + metadataObj.month + '-' + metadataObj.day;
                        shashin.refreshTimeline($("#mediaTypeFilter").val(), offCanvasId);
                    }

                    if (metadataObj.hidden === false) {
                        Util.populateDetailsInfo(metadataObj,"propTimelineModal")
                        $("#timelineModalEdit" + metadataId).attr("tag", JSON.stringify(metadataObj))

                        $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil").addClass("bi-pencil-square");
                        if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlng").val() !== "") {
                            $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil-square").addClass("bi-pencil");
                        }

                        if (takenDateUpdated === true) {
                            removeThumbnail(metadataId);
                        }
                    } else {
                        removeThumbnail(metadataId);
                    }

                    $("#timelineModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                }
                //$("#timelineModalMsg").html(message);
            }
            //$("#timelineModalStatus").css("visibility","hidden");
        });
    }

    function removeThumbnail(metadataId) {
        const targetElement = $("#photoThumbnailContainer" + metadataId);
        const rowId = $("#photoThumbnailContainer" + metadataId).parent().attr("id");
        const headingId = rowId.replace("row", "");

        // Count children
        const currentNumChildren = targetElement.siblings("div").length;

        // Remove metadata
        targetElement.remove();

        if (currentNumChildren === 0) {
            Util.removeDateGallery(headingId);
        }
    }
});

$("#refreshTakenDate").click(function (e) {
    e.preventDefault();

    const metadataId = $("#metadataId").val();

    const json = {
        id: metadataId
    };
    const ajaxParams = {
        type: "post",
        url: "/timeline/sync/"+metadataId,
        data: JSON.stringify(json),
        contentType: 'application/json; charset=utf-8',
        retries: shashin.ajaxRetries
    }

    $.ajax(ajaxParams)
    .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " refreshing taken date")}).then(function (data) {
        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === "success") {
                message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                if (data["year"] !== "" && data["month"] !== "" && data["day"] !== "" && data["time"] !== "") {
                    $("#yearTaken").val(data["year"]);
                    $("#monthTaken").val(data["month"]);
                    $("#dayTaken").val(data["day"]);
                    $("#timeTaken").val(data["time"]);
                }
                // window.top.location = window.top.location
            } else {
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
            }
            // $("#timelineModalMsg").html(message);
        }
    });

    return false;
});

// Clear message on modal close
$('#propTimelineModal').on('hide.bs.modal', function () {
    $("#timelineModalStatus").attr("class","spinner-grow me-auto");
    $("#timelineModalStatus").css("visibility","hidden");
    $("#timelineModalMsg").html("");
    $("#saveMetadata").prop('disabled', false);
    const tab = new bootstrap.Tab($("#generalTabLink"));
    tab.show();
});

// Clear message on input editing
$('#propTimelineModal').find(':input').bind('keypress', function() {
    $("#timelineModalStatus").attr("class","spinner-grow me-auto");
    $("#timelineModalStatus").css("visibility","hidden");
    $("#timelineModalMsg").html("");
});

$("#refreshTakenDate").click(function (e) {
    e.preventDefault();

    const originalTakenAtDate = $("#takenAtDetails").text();
    const originalTakenAtDateArray = originalTakenAtDate.split(" ");
    const takenAtParts = originalTakenAtDateArray[0].split("-");

    if (takenAtParts.length > 3) {
        $("#yearTaken").val(takenAtParts[0]);
        $("#monthTaken").val(takenAtParts[1]);
        $("#dayTaken").val(takenAtParts[2]);
        $("#timeTaken").val(originalTakenAtDateArray[1])
    }
});

$("#detailsTabLink").click(function (e) {
    e.preventDefault();
    $("#timelineModalMsg").html("");
    $("#saveMetadata").prop('disabled', true);
});

$("#mapTabLink").click(function (e) {
    e.preventDefault();
    $("#timelineModalMsg").html("");
    $("#saveMetadata").prop('disabled', true);

    const metadataId = $("#metadataId").val();
    const metadata = shashin.checkMetadata(metadataId);

    shashin.openMap(metadata)
});

$("#generalTabLink").click(function (e) {
    e.preventDefault();
    $("#saveMetadata").prop('disabled', false);
});

$("#isobject").click(function (e) {
    const metadataId = $("#metadataId").val();

    timelineModal.closeTagPeopleDropdown(metadataId);
    if ($(this).prop("checked") === true) {
        $("#tagpeople").val("");
    }
});

$("#tagpeople").focus(function (e) {
    e.preventDefault();

    const metadataId = $("#metadataId").val();
    timelineModal.closeTagPeopleDropdown(metadataId);
});

$('body').click(function(event) {
    const metadataId = $("#metadataId").val();

    if (!$(event.target).closest("#albumdropdown"+metadataId).length && !$(event.target).closest("#albumsList").length && $("#albumdropdown"+metadataId).hasClass("show")) {
        timelineModal.toggleAlbumDropdown(metadataId);
    }

    if (!$(event.target).closest("#tagpeopledropdown"+metadataId).length && !$(event.target).closest("#recognitionLabelsList").length && $("#tagpeopledropdown"+metadataId).hasClass("show")) {
        timelineModal.toggleTagPeopleDropdown(metadataId);
    }
});