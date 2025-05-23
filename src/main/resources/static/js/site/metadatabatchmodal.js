(function( metadataBatchModal, $, undefined ) {

    metadataBatchModal.toggleBatchTagPeopleDropdown = function() {
        $("#tagbatchpeopledropdown").dropdown('toggle');
    };

    metadataBatchModal.closeBatchTagPeopleDropdown = function() {
        $("#tagbatchpeopledropdown").dropdown('hide');
    };

    metadataBatchModal.populateBatchLabel = function() {
        const checkedBoxes = $('#peopleBatchSelectionList :checked');
        let labelString = "";
        checkedBoxes.each(function() {
            labelString += $(this).val() + ",";
        });
        if (labelString.length > 0) {
            labelString = labelString.slice(0,-1);
        }
        $("#tagBatchDataInput").val(labelString);

        if (labelString !== "") {
            $("#batchisobject").prop("checked", false);
        }
    };

    metadataBatchModal.toggleBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('toggle');
    };

    metadataBatchModal.closeBatchTagAlbumDropdown = function() {
        $("#tagalbumdropdown").dropdown('hide');
    };

    metadataBatchModal.populateBatchAlbum = function() {
        const checkedBoxes = $('#albumBatchSelectionList :checked');
        let albumsString = "";
        checkedBoxes.each(function() {
            albumsString += $(this).val().replace(/ +(?= )/g,'').trim() + ",";
        });
        if (albumsString.length > 0) {
            albumsString = albumsString.slice(0,-1);
        }
        $("#albumNameInput").val(albumsString);
    };
}( window.metadataBatchModal = window.metadataBatchModal || {}, jQuery ));

$("#batchMapTabLink").on("click", function (e) {
    e.preventDefault();

    const propBatchMetadataModal = document.getElementById('propBatchMetadata');
    const modal = bootstrap.Modal.getInstance(propBatchMetadataModal);
    modal.handleUpdate();

    const metadataIds = shashin.getMetadataIdList();

    if (shashin.map === null && metadataIds.length > 0) {
        shashin.openMap();
    }
});

$("#batchrescan").on("click", async function (e) {
    if ($("#batchrescan").prop("checked")) {
        $("#batchhidden").prop("checked", false);
        $("#saveBatchData :input").prop("disabled", true);
        $("#batchrescan").prop("disabled", false);
        $("#batchMetadataIds").prop("disabled", false);
    } else {
        $("#saveBatchData :input").prop("disabled", false);
    }
});

$("#batchhidden").on("click", async function (e) {
    if ($("#batchhidden").prop("checked")) {
        $("#batchrescan").prop("checked", false);
        $("#saveBatchData :input").prop("disabled", true);
        $("#batchhidden").prop("disabled", false);
        $("#batchMetadataIds").prop("disabled", false);
    } else {
        $("#saveBatchData :input").prop("disabled", false);
    }
});

$("#confirmRescanBatchMetadata").on("click", function (e) {
    e.preventDefault();

    const metadataIds = JSON.parse($("#batchMetadataIds").val());

    Util.rescanMetadata(metadataIds, "propBatchMetadata");
});

$("#saveBatchMetadata").on("click", async function (e) {
    await saveBatchMetadata(e);
});

$("#propBatchMetadata").on("keydown", async function (e) {
    if (e.key === "Enter" || e.code === "Enter" || e.which === 13 || e.keyCode === 13) {
        await saveBatchMetadata(e);
    }
});

async function saveBatchMetadata(e) {
    e.preventDefault();

    if ($("#cameraBatchData").val() === "" && $("#lensBatchData").val() === "" &&
        $("#yearTakenBatchData").val() === "" && $("#monthTakenBatchData").val() === "" && $("#dayTakenBatchData").val() === "" && $("#offsetTakenBatchData").val() === "" &&
        $("#latlngBatchData").val() === "" && $("#keywordsBatchData").val() === "" && $("#albumNameInput").val() === "" && $("#tagBatchDataInput").val() === "" &&
        $("#batchhidden").is(':checked') === false && $("#batchisobject").is(':checked') === false && $("#batchrescan").is(':checked') === false
    ) {
        shashin.showToastMessage("Metadata", "Did not save, nothing entered.", {
            icon: "bi-info-circle",
            iconColor: "#777777",
            tag: "metadatabatchmodal",
            borderColor:"warning"
        });

        $("#propBatchMetadata").modal('hide');

        return true;
    }

    $("#metadataBatchModalCancel").prop("disabled", true);
    $("saveBatchMetadata").prop("disabled", true);
    $("#msgBatchMetadata").html("");
    $("#metadataBatchModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#metadataBatchModalStatus").visible();
    $("#metadataBatchModalStatus").attr("title", "");

    const propBatchMetadataModal = bootstrap.Modal.getInstance(document.getElementById('propBatchMetadata'));
    propBatchMetadataModal._config.backdrop = 'static';
    propBatchMetadataModal._config.keyboard = false;

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

    if ($("#batchrescan").prop("checked")) {
        $("#rescanBatchMetadataConfirmation").modal('show');
    } else if (Util.validateMetadataInputs(
        $("#dayTakenBatchData").val(),
        $("#monthTakenBatchData").val(),
        $("#yearTakenBatchData").val(),
        "",
        $("#offsetTakenBatchData").val(),
        $("#latlngBatchData").val(),
        ""
    ) === true) {
        if ($("#batchisobject").is(':checked')) {
            $("#batchisobject").val("on");
        }
        if ($("#batchhidden").is(':checked')) {
            $("#batchhidden").val("on");
        }
        if ($("#addtoexistingalbums").is(':checked')) {
            $("#addtoexistingalbums").val("on");
        }
        if ($("#addtoexistingpeople").is(':checked')) {
            $("#addtoexistingpeople").val("on");
        }
        if ($("#addtoexistingkeywords").is(':checked')) {
            $("#addtoexistingkeywords").val("on");
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

        if (activePage === "timeline") {
            setTimeout(function () {
                const http = new Http("timeline");
                http.ajax("get", "/timeline/yearmonthcounts/all").then(function (data) {
                    if (data.hasOwnProperty("metadataDates") && data.hasOwnProperty("metadataYearMonthCount")) {
                        timelineSettings.timelineDates = data.metadataDates;
                        timelineSettings.metadataYearMonthCount = data.metadataYearMonthCount;
                        timelineSettings.timelineDatesHash = data.metadataDatesHash;
                    }
                });
            }, 0);

            if (timelineSettings.db !== null) {
                setTimeout(function () {
                    const http = new Http("indexeddb test");
                    http.ajax("get", "/timeline/all/dates").then(async function (data) {
                        if (data.hasOwnProperty("allMetadata") && data.hasOwnProperty("favorites") && data.hasOwnProperty("placeNameHeaders")) {
                            const metadataList = data.allMetadata;
                            timelineSettings.favoritesMap = data.favorites;
                            timelineSettings.placeNameHeaders = data.placeNameHeaders;

                            timelineSettings.db = new Dexie("MetadataDatabase");
                            timelineSettings.db.version(1).stores({
                                metadataList: `id, [year+month+day]`
                            });

                            timelineSettings.db.metadataList.bulkPut(metadataList);
                        }
                    });
                }, 0);
            }
        }

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === shashin.apiResponse.SUCCESS) {
                if (data.hasOwnProperty("keywords") && data.keywords !== "") {
                    $("#keywordsString").val(data.keywords);
                    $("#keywordsBatchString").val(data.keywords);
                }

                if (data.hasOwnProperty("cameras") && data.cameras !== "") {
                    $("#camerasString").val(data.cameras);
                    $("#camerasBatchString").val(data.cameras);
                }

                if (data.hasOwnProperty("lenses") && data.lenses !== "") {
                    $("#lensesString").val(data.lenses);
                    $("#lensesBatchString").val(data.lenses);
                }

                shashin.processBatchAlbumList(data, albumInputVal);

                shashin.processBatchPeopleList(data, subjectInputVal);

                let dateGalleryRemoved = false;
                for (const index in metadataIds) {
                    const metadataId = metadataIds[index];

                    if ($("#image" + metadataId).length > 0) {
                        $("#metadataModalEdit" + metadataId + " span").removeClass("bi-info-circle").addClass("bi-info-square");

                        const metadataObj = {};

                        if ($("#latlngBatchData").val().trim() !== "") {
                            const latlngArray = $("#latlngBatchData").val().split(",");
                            metadataObj.lat = $.trim(latlngArray[0]);
                            metadataObj.lng = $.trim(latlngArray[1]);
                            if (metadataObj.lat !== null && metadataObj.lng !== null && metadataObj.lat !== "" && metadataObj.lng !== "") {
                                $("#latlng").val(metadataObj.lat + "," + metadataObj.lng);
                            }
                        }

                        metadataObj.hidden = markedHidden;

                        if (metadataObj.hidden === false) {
                            $("#metadataModalEdit" + metadataId).attr("tag", metadataId);
                            $("#mediaLink" + metadataId).attr("tag", metadataId);

                            if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlngBatchData").val().trim !== "") {
                                $("#metadataModalEdit" + metadataId + " span").removeClass("bi-info-square").addClass("bi-info-circle");
                            }

                            if (metadataChangeMap.hasOwnProperty(metadataId) && metadataChangeMap[metadataId] === true && activePage === "timeline") {
                                dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                            }
                        } else if (activePage === "timeline") {
                            dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                        }

                        if (parseInt(index) === (metadataIds.length - 1)) {
                            if (activePage === "timeline" && (($("#yearTakenBatchData").val().trim() !== "" || $("#monthTakenBatchData").val().trim() !== "" || $("#dayTakenBatchData").val().trim() !== "") || metadataObj.hidden === true)) {
                                timelineSettings.refreshTimeline($("#mediaTypeFilter").val()).then(function () {
                                    // If a date section was removed refresh the timeline
                                    if (dateGalleryRemoved === true) {
                                        const elements = Util.elementsInViewport($(".scrollspy"));
                                        let firstElementId = $(elements[0]).attr("id");
                                        let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
                                        timelineSettings.jumpFromTimelineToc(null, firstVisibleId, $("#mediaTypeFilter").val());
                                    }
                                });
                                // if (index === metadataIds.length - 1) {
                                Util.setMetadataLocalStorage();
                                // }
                            }

                            if ($("#latlngBatchData").val().trim() !== "") {
                                Util.setMetadataLocalStorage();
                            }
                        }
                    }
                }

                $("#metadataBatchModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                $("#saveBatchMetadata").prop('disabled', true);
                $('#metadataBatchModalStatus').fadeOut(5000, function () {
                    $(this).removeClass('bi-check-circle').removeClass('spinner-grow');
                    $(this).css("display", "block");
                    $("#saveBatchMetadata").prop('disabled', false);
                });
                $("#metadataBatchModalCancel").prop("disabled", false);

                if (activePage !== "timeline") {
                    if (markedHidden === true ||
                        (activePage !== "recent" && activePage !== "modified" && activePage !== "accessed" && activePage !== "folder" && activePage !== "taken" &&
                            ($("#yearTakenBatchData").val() !== "" ||
                                $("#monthTakenBatchData").val() !== "" ||
                                $("#dayTakenBatchData").val() !== "")
                        )
                    ) {
                        window.location.reload();
                    }

                    if (activePage === "matches" || (activePage === "album" && albumInputVal !== "")) {
                        window.location.reload();
                    }
                }

                shashin.showToastMessage("Metadata", "Metadata Saved", {
                    icon: "bi-info-circle",
                    iconColor: "#777777",
                    tag: "metadatabatchmodal",
                    borderColor:"success"
                });

                $("#propBatchMetadata").modal('hide');
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
        $("saveBatchMetadata").prop("disabled", false);
        propBatchMetadataModal._config.backdrop = true;
        propBatchMetadataModal._config.keyboard = true;
    } else {
        $("#metadataBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
        $("saveBatchMetadata").prop("disabled", false);
        propBatchMetadataModal._config.backdrop = true;
        propBatchMetadataModal._config.keyboard = true;
    }

    return false;
}

$('#rescanBatchMetadataConfirmation').on('hide.bs.modal', function () {
    $("#metadataBatchModalCancel").prop("disabled", false);
    $("#saveBatchMetadata").prop("disabled", false);
    $("#metadataBatchModalStatus").removeClass('bi-x-circle').removeClass('bi-check-circle').removeClass('spinner-grow');
});

$('#propAddAlbum').on('hide.bs.modal', function () {
    $("#albumNameInput").val("");
    $("#albumResponseMsg").html("");
});

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
    if (shashin.map !== null) {
        // Set map target to null and reset
        shashin.map.setTarget(null);
        shashin.map = null;
    }
    $("#metadataBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#metadataBatchModalStatus").invisible();
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
    $("#saveBatchData :input").prop("disabled", false);
    $("#batchrescan")[0].checked = false;
    $("#collapseBatchMetadata").collapse("hide");
    metadataBatchModal.closeBatchTagPeopleDropdown();
    metadataBatchModal.closeBatchTagAlbumDropdown();
    if ($("#batchGeneralTabLink").length > 0) {
        const tab = new bootstrap.Tab($("#batchGeneralTabLink"));
        tab.show();
    }
    shashin.clearTimelineSelection();
});

// Clear message on input editing
$('#propBatchMetadata').bind('keypress', function() {
    $("#metadataBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#metadataBatchModalStatus").invisible();
    $("#msgBatchMetadata").html("");
});

$("#collapseBatchMetadata").on("shown.bs.collapse", function (e) {
    $("#propBatchMetadataBody").animate({
        scrollTop: $('#propBatchMetadataBody')[0].scrollHeight - $('#propBatchMetadataBody')[0].clientHeight
    }, 1000);
});

$('body').on("click", function(event) {
    if (!$(event.target).closest("#tagalbumdropdown").length && !$(event.target).closest("#albumNameList").length && $("#tagalbumdropdown").hasClass("show")) {
        metadataBatchModal.toggleBatchTagAlbumDropdown();
    }

    if (!$(event.target).closest("#tagbatchpeopledropdown").length && !$(event.target).closest("#peopleNameList").length && $("#tagbatchpeopledropdown").hasClass("show")) {
        metadataBatchModal.toggleBatchTagPeopleDropdown();
    }
});