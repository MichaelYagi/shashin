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

$("#saveMetadata").on("click", async function (e) {
    e.preventDefault();
    $("#timelineModalMsg").html("");
    $("#timelineModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#timelineModalStatus").css("visibility", "visible");
    $("#timelineModalStatus").attr("title", "");
    $("#timelineModalCancel").prop('disabled', true);
    const metadataId = $("#metadataId").val();
    let timeTakenPrev = $("#timeTaken").val();

    timelineModal.closeTagPeopleDropdown(metadataId);

    let takenDateUpdated = false;
    let captionUpdated = false;
    shashin.getMetadata(metadataId).then(function (metadataObj) {
        if (parseInt(metadataObj.year) !== parseInt($("#yearTaken").val()) ||
            parseInt(metadataObj.month) !== parseInt($("#monthTaken").val()) ||
            parseInt(metadataObj.day) !== parseInt($("#dayTaken").val())) {
            takenDateUpdated = true;
        }

        if ($("#description").val() !== metadataObj.description) {
            captionUpdated = true;
        }

        timeTakenPrev = metadataObj.time;
    });

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
            id: metadataId,
            title: Util.decodeHtml($("#title").val().trim()),
            description: Util.decodeHtml($("#description").val().trim()),
            camera: Util.decodeHtml($("#camera").val().trim()),
            year: $("#yearTaken").val(),
            month: $("#monthTaken").val(),
            day: $("#dayTaken").val(),
            time: $("#timeTaken").val(),
            offset: $("#offsetTaken").val() === null ? "" : $("#offsetTaken").val(),
            latlng: Util.decodeHtml($("#latlng").val()),
            keywords: Util.decodeHtml($("#keywords").val()),
            tagpeople: Util.decodeHtml($("#tagpeople").val()),
            albumnames: Util.decodeHtml($("#albumnames").val()),
            hidden: $("#hidden").prop("checked"),
            isObject: $("#isobject").prop("checked")
        }

        const http = new Http("save timeline");
        let data;

        if ($("#hidden").is(':checked')) {
            data = await http.ajax("post", "/timeline/remove/" + metadataId, JSON.stringify(json), function () {
                $("#timelineModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#timelineModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineModalCancel").prop('disabled', false);
            });
        } else {
            data = await http.ajax("post", "/timeline/update/" + metadataId, JSON.stringify(json), function () {
                $("#timelineModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#timelineModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineModalCancel").prop('disabled', false);
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

                shashin.processAlbumList(data);

                shashin.processPeopleList(data);

                // Update tag
                const metadataObj = {};
                let dateGalleryRemoved = false;

                if (captionUpdated === true) {
                    $("#mediaLink" + metadataId).attr("data-sub-html", $("#description").val());
                }

                const latlngArray = $("#latlng").val().split(",");
                metadataObj.lat = $.trim(latlngArray[0])
                metadataObj.lng = $.trim(latlngArray[1])

                metadataObj.hidden = $("#hidden").prop("checked")

                if (metadataObj.hidden === false) {
                    $("#timelineModalEdit" + metadataId).attr("tag", metadataId);
                    $("#mediaLink" + metadataId).attr("tag", metadataId);

                    $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil").addClass("bi-pencil-square");
                    if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlng").val() !== "") {
                        $("#timelineModalEdit" + metadataId + " span").removeClass("bi-pencil-square").addClass("bi-pencil");
                    }

                    if (takenDateUpdated === true && ($("#activePage").length > 0 && $("#activePage").val() !== "recent" && $("#activePage").val() !== "folder") || $("#activePage").length === 0) {
                        dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                    }
                } else if (($("#activePage").length > 0 && $("#activePage").val() !== "recent" && $("#activePage").val() !== "folders") || $("#activePage").length === 0) {
                    dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                }

                if ($("#offcanvasToc").length > 0 && (takenDateUpdated === true || metadataObj.hidden === true)) {
                    timelineSettings.refreshTimeline($("#mediaTypeFilter").val()).then(function (data) {
                        // If a date section was removed refresh the timeline
                        if (dateGalleryRemoved === true) {
                            const elements = Util.elementsInViewport($(".scrollspy"));
                            let firstElementId = $(elements[0]).attr("id");
                            let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
                            timelineSettings.jumpFromTimelineToc(e, firstVisibleId, $("#mediaTypeFilter").val());
                        }
                    });
                }

                if ($("#offcanvasToc").length > 0 && (takenDateUpdated === true || metadataObj.hidden === true || metadataObj.time !== timeTakenPrev)) {
                    Util.setMetadataLocalStorage();
                }

                if (typeof timelineSettings !== "undefined" && dateGalleryRemoved === false && captionUpdated === true) {
                    // Refresh gallery if caption updated
                    timelineSettings.reinitLightGalleryInstance();
                }

                $("#timelineModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                $("#timelineModalCancel").prop('disabled', false);
            } else {
                $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#timelineModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineModalCancel").prop('disabled', false);
            }
        } else {
            $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            $("#timelineModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#timelineModalCancel").prop('disabled', false);
        }
    } else {
        $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
    }
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

$("#refreshTakenDate").on("click", function (e) {
    e.preventDefault();

    const originalTakenAtDate = $(".takenAtDetails").first().text();
    const originalTakenAtDateArray = originalTakenAtDate.split(" ");
    const takenAtParts = originalTakenAtDateArray[0].split("-");

    if (takenAtParts.length === 3) {
        $("#yearTaken").val(parseInt(takenAtParts[0]));
        $("#monthTaken").val(parseInt(takenAtParts[1]));
        $("#dayTaken").val(parseInt(takenAtParts[2]));
        $("#timeTaken").val(originalTakenAtDateArray[1])
    }
});

$("#detailsTabLink").on("click", function (e) {
    e.preventDefault();

    const propTimelineModal = document.getElementById('propTimelineModal');
    const modal = bootstrap.Modal.getInstance(propTimelineModal);
    modal.handleUpdate();
    $("#timelineModalMsg").html("");
    $("#saveMetadata").prop('disabled', true);

    const metadataId = $("#metadataId").val();
    shashin.getMetadata(metadataId).then(function (metadataObj) {
        Util.populateDetailsInfo(metadataObj, "propTimelineModal");
    });
});

$("#mapTabLink").on("click", function (e) {
    e.preventDefault();

    const propTimelineModal = document.getElementById('propTimelineModal');
    const modal = bootstrap.Modal.getInstance(propTimelineModal);
    modal.handleUpdate();
    $("#timelineModalMsg").html("");
    $("#saveMetadata").prop('disabled', true);

    const metadataId = $("#metadataId").val();
    shashin.getMetadata(metadataId).then(function (metadataObj) {
        shashin.openMap(metadataObj);
    });
});

$("#exifTabLink").on("click", async function (e) {
    e.preventDefault();

    const propTimelineModal = document.getElementById('propTimelineModal');
    const modal = bootstrap.Modal.getInstance(propTimelineModal);
    modal.handleUpdate();
    $("#exifInfo").val("");
    $("#saveMetadata").prop('disabled', true);

    // Get exif yaml data and display
    const metadataId = $("#metadataId").val();
    const http = new Http("get exif");
    const data = await http.ajax("get", "/api/v1/exif/metadata/" + metadataId);

    let exif = "";
    if (data.hasOwnProperty("exif")) {
        exif = JSON.stringify(data["exif"], null, 2);
    }
    $("#exifInfo").val(exif);
});

$("#generalTabLink").on("click", function (e) {
    e.preventDefault();

    const propTimelineModal = document.getElementById('propTimelineModal');
    const modal = bootstrap.Modal.getInstance(propTimelineModal);
    modal.handleUpdate();
    $("#saveMetadata").prop('disabled', false);
});

$("#isobject").on("click", function (e) {
    const metadataId = $("#metadataId").val();

    timelineModal.closeTagPeopleDropdown(metadataId);
    if ($(this).prop("checked") === true) {
        $("#tagpeople").val("");
    }
});

$("#tagpeople").on("focus", function (e) {
    e.preventDefault();

    const metadataId = $("#metadataId").val();
    timelineModal.closeTagPeopleDropdown(metadataId);
});

$('body').on("click", function(event) {
    const metadataId = $("#metadataId").val();

    if (!$(event.target).closest("#albumdropdown"+metadataId).length && !$(event.target).closest("#albumsList").length && $("#albumdropdown"+metadataId).hasClass("show")) {
        timelineModal.toggleAlbumDropdown(metadataId);
    }

    if (!$(event.target).closest("#tagpeopledropdown"+metadataId).length && !$(event.target).closest("#recognitionLabelsList").length && $("#tagpeopledropdown"+metadataId).hasClass("show")) {
        timelineModal.toggleTagPeopleDropdown(metadataId);
    }
});