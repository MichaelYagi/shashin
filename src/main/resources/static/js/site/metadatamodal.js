(function( metadataModal, $, undefined ) {
    function getUpdateText() {
        return "Updating location ...";
    }

    metadataModal.toggleTagPeopleDropdown = function (metadataId) {
        $("#tagpeopledropdown"+metadataId).dropdown('toggle');
    };

    metadataModal.closeTagPeopleDropdown = function (metadataId) {
        $("#tagpeopledropdown"+metadataId).dropdown('hide');
    };

    metadataModal.toggleAlbumDropdown = function (metadataId) {
        $("#albumdropdown"+metadataId).dropdown('toggle');
    };

    metadataModal.closeAlbumDropdown = function (metadataId) {
        $("#albumdropdown"+metadataId).dropdown('hide');
    };

    metadataModal.populateLabel = function (metadataId) {
        const checkedBoxes = $('input[name="recognitionLabel' + metadataId + '[]"]:checked');
        let labelString = "";

        checkedBoxes.each(function() {
            labelString += $(this).val() + ",";
        });

        if (labelString.length > 0) {
            labelString = labelString.slice(0,-1);
        }

        $("#tagpeople").val(labelString);

        if (labelString !== "") {
            $("#isobject").prop("checked", false);
        }
    };

    metadataModal.populateAlbum = function (metadataId) {
        const checkedBoxes = $('input[name="album' + metadataId + '[]"]:checked');
        let albumString = "";

        checkedBoxes.each(function() {
            albumString += $(this).val() + ",";
        });

        if (albumString.length > 0) {
            albumString = albumString.slice(0,-1);
        }

        $("#albumnames").val(albumString);
    };

}( window.metadataModal = window.metadataModal || {}, jQuery ));

function getMetadataModalUpdateText() {
    return "Updating location ...";
}

$("#confirmRescanMetadata").on("click", function (e) {
    e.preventDefault();

    const metadataId = $("#metadataId").val();
    const metadataIdArray = [];
    metadataIdArray.push(metadataId);

    Util.rescanMetadata(metadataIdArray,"propMetadata");
});

$("#rescan").on("click", async function (e) {
    if ($("#rescan").prop("checked")) {
        $("#hidden").prop("checked", false);
        $("#saveTimelineModalForm :input").prop("disabled", true);
        $("#rescan").prop("disabled", false);
        $("#metadataId").prop("disabled", false);
    } else {
        $("#saveTimelineModalForm :input").prop("disabled", false);
    }
});

$("#hidden").on("click", async function (e) {
    if ($("#hidden").prop("checked")) {
        $("#rescan").prop("checked", false);
        $("#saveTimelineModalForm :input").prop("disabled", true);
        $("#hidden").prop("disabled", false);
        $("#metadataId").prop("disabled", false);
    } else {
        $("#saveTimelineModalForm :input").prop("disabled", false);
    }
});

$("#saveMetadata").on("click", async function (e) {
    await saveMetadata(e);
});

$("#propMetadata").on("keydown", async function (e) {
    if (e.key === "Enter" || e.code === "Enter" || e.which === 13 || e.keyCode === 13) {
        await saveMetadata(e);
    }
});

async function saveMetadata(e) {
    e.preventDefault();

    $("#metadataModalMsg").html("");
    $("#metadataModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#metadataModalStatus").visible();
    $("#metadataModalStatus").attr("title", "");
    $("#metadataModalCancel").prop('disabled', true);
    $("#placeName").attr("placeholder", "");

    const propMetadataModal = bootstrap.Modal.getInstance(document.getElementById('propMetadata'));
    propMetadataModal._config.backdrop = 'static';
    propMetadataModal._config.keyboard = false;

    const metadataId = $("#metadataId").val();
    let prevPeople = $("#peopleList").val();
    let prevAlbums = $("#albumList").val();
    const activePage = $("#activePage").val();

    metadataModal.closeTagPeopleDropdown(metadataId);

    let prevPeopleArray = [];
    let prevAlbumsArray = [];
    let takenDateUpdated = false;
    let captionUpdated = false;
    let prevLat = "";
    let prevLng = "";
    let prevPlaceName = "";
    let type = "";

    shashin.getMetadata(metadataId).then(function (metadataObj) {
        if (parseInt(metadataObj.year) !== parseInt($("#yearTaken").val()) ||
            parseInt(metadataObj.month) !== parseInt($("#monthTaken").val()) ||
            parseInt(metadataObj.day) !== parseInt($("#dayTaken").val())) {
            takenDateUpdated = true;
        }

        if ($("#description").val() !== metadataObj.description) {
            captionUpdated = true;
        }

        prevPeopleArray = prevPeople.split(",").map(function (item) {
            return item.trim();
        });

        prevAlbumsArray = prevAlbums.split(",").map(function (item) {
            return item.trim();
        });

        type = metadataObj.type;

        prevLat = metadataObj.lat;
        prevLng = metadataObj.lng;
        prevPlaceName = metadataObj.placeName;
    });

    if ($("#rescan").prop("checked")) {
        $("#rescanMetadataConfirmation").modal('show');
    } else if (Util.validateMetadataInputs(
        $("#dayTaken").val(),
        $("#monthTaken").val(),
        $("#yearTaken").val(),
        $("#timeTaken").val(),
        $("#offsetTaken").val(),
        $("#latlng").val(),
        $("#duration").val()
    ) === true) {
        const people = $("#tagpeople").val();
        const albums = $("#albumnames").val();

        const json = {
            id: metadataId,
            title: $("#title").val().trim(),
            description: $("#description").val().trim(),
            camera: Util.decodeHtml($("#camera").val().trim()),
            lens: Util.decodeHtml($("#lens").val().trim()),
            duration: Util.decodeHtml($("#duration").val().trim()),
            year: $("#yearTaken").val(),
            month: $("#monthTaken").val(),
            day: $("#dayTaken").val(),
            time: $("#timeTaken").val(),
            offset: $("#offsetTaken").val() === null ? "" : $("#offsetTaken").val(),
            latlng: Util.decodeHtml($("#latlng").val()),
            keywords: $("#keywords").val(),
            tagpeople: people,
            albumnames: albums,
            hidden: $("#hidden").prop("checked"),
            isObject: $("#isobject").prop("checked")
        };

        let compreFaceImageId = "";
        const peopleArray = people.split(",").map(function (item) {
            return item.trim();
        });

        const albumsArray = albums.split(",").map(function (item) {
            return item.trim();
        });

        if (Util.arraysEqual(prevPeopleArray, peopleArray) === false) {
            $.each(peopleArray, async function (index, person) {
                person = person.trim();

                if (person !== '' && $.inArray(person, prevPeopleArray) === -1) {
                    const personJson = {
                        personName: person,
                        metadataId: metadataId
                    };
                    const http = new Http("upload faces");
                    let persondata = await http.ajax("post", "/person/recognition/faces", JSON.stringify(personJson));

                    if (persondata.hasOwnProperty("responseDataUpload") && persondata.responseDataUpload.hasOwnProperty("image_id")) {
                        compreFaceImageId = persondata.responseDataUpload.image_id;
                    }
                }
            });
        }

        const http = new Http("save timeline");
        let data;

        if ($("#hidden").is(':checked')) {
            data = await http.ajax("post", "/metadata/remove/" + metadataId, JSON.stringify(json), function () {
                $("#metadataModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#metadataModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#metadataModalCancel").prop('disabled', false);
            });
        } else {
            data = await http.ajax("put", "/metadata/update/" + metadataId, JSON.stringify(json), function () {
                $("#metadataModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#metadataModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#metadataModalCancel").prop('disabled', false);
            });
        }

        if (activePage === "timeline" && timelineSettings.db !== null) {
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

                // Update tag
                const metadataObj = {};
                let dateGalleryRemoved = false;

                let mediaContentList = [];
                if (captionUpdated === true) {
                    const updatedDescription = $("#description").val();
                    $("#mediaLink" + metadataId).attr("data-sub-html", updatedDescription);
                    if (shashin.getLightGallery() !== null) {
                        mediaContentList = shashin.getLightGallery().galleryItems;
                        if (mediaContentList.length > 0) {
                            for (let i = 0; i <= mediaContentList.length; i++) {
                                let mediaContent = mediaContentList[i];
                                if (mediaContent && mediaContent.hasOwnProperty("args") && mediaContent.args === metadataId) {
                                    mediaContent.subHtml = updatedDescription;
                                    if (updatedDescription.trim().length === 0) {
                                        delete mediaContent.subHtml;
                                    }
                                    mediaContentList[i] = mediaContent;
                                    break;
                                }
                            }
                        }
                    }
                }

                const latlngArray = $("#latlng").val().split(",");
                metadataObj.lat = $.trim(latlngArray[0]);
                metadataObj.lng = $.trim(latlngArray[1]);

                metadataObj.hidden = $("#hidden").prop("checked");

                if (metadataObj.hidden === false) {
                    $("#metadataModalEdit" + metadataId).attr("tag", metadataId);
                    $("#mediaLink" + metadataId).attr("tag", metadataId);

                    if ($("#keywords").val() === "" && data.hasOwnProperty("keywordsIdentified") && data.keywordsIdentified !== "") {
                        $("#keywords").val(data.keywordsIdentified);
                    }

                    if (type.indexOf("video") >= 0) {
                        let duration = $("#duration").val().trim();
                        if (duration === "" || duration === null) {
                            duration = "0:00";
                        }
                        $("#duration" + metadataId).text(Util.decodeHtml(duration));
                        $("#duration").val(duration);
                    }

                    $("#metadataModalEdit" + metadataId + " span").removeClass("bi-info-circle").addClass("bi-info-square");
                    if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlng").val() !== "") {
                        $("#metadataModalEdit" + metadataId + " span").removeClass("bi-info-square").addClass("bi-info-circle");
                        shashin.map = null;
                        $("#mapTabNav").show();
                    } else if ($("#generalTabNav").length === 0) {
                        $("#mapTabNav").hide();
                    }

                    $("#infoModalEdit" + metadataId + " span").removeClass("bi-info-circle").addClass("bi-info-square");
                    if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlng").val() !== "") {
                        $("#infoModalEdit" + metadataId + " span").removeClass("bi-info-square").addClass("bi-info-circle");
                        shashin.map = null;
                        $("#mapTabNav").show();
                    } else if ($("#generalTabNav").length === 0) {
                        $("#mapTabNav").hide();
                    }

                    if (prevLat !== metadataObj.lat || prevLng !== metadataObj.lng) {
                        // Reload in map view if latlng changed
                        if (activePage === "map") {
                            let year = $("#yearTaken").val();
                            let month = $("#monthTaken").val();
                            let day = $("#dayTaken").val();

                            let queryParamDates = "";
                            if (year !== null && year !== "" && month !== null && month !== "" && day !== null && day !== "") {
                                if (month < 10) {
                                    month = '0' + month;
                                }
                                let lastDay = day;
                                if (lastDay < 29) {
                                    lastDay = 28;
                                }
                                queryParamDates = '&sd=' + year + '-' + month + '-01&ed=' + year + '-' + month + '-' + lastDay;
                            }

                            Util.setMetadataLocalStorage();
                            window.location.replace("/map?latlng=" + $("#latlng").val() + queryParamDates);
                        }
                    }

                    if (takenDateUpdated === true && ($("#activePage").length > 0 && $("#activePage").val() !== "recent" && $("#activePage").val() !== "modified" && $("#activePage").val() !== "taken" && $("#activePage").val() !== "folder") || $("#activePage").length === 0) {
                        dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                    }
                } else if (activePage === "map" && metadataObj.hidden === true) {
                    $("#metadataModalEdit" + metadataId + " span").removeClass("bi-info-circle").addClass("bi-info-square");
                    if (metadataObj.lat !== null && metadataObj.lng !== null && prevLng !== metadataObj.lng && $("#latlng").val() !== "") {
                        // Reload in map view if removed
                        Util.setMetadataLocalStorage();
                        window.location.replace("/map");
                    }

                    $("#infoModalEdit" + metadataId + " span").removeClass("bi-info-circle").addClass("bi-info-square");
                    if (metadataObj.lat !== null && metadataObj.lng !== null && $("#latlng").val() !== "") {
                        // Reload in map view if removed
                        Util.setMetadataLocalStorage();
                        window.location.replace("/map");
                    }
                } else if (activePage === "timeline" || $("#activePage").length === 0) {
                    dateGalleryRemoved = shashin.removeThumbnail(metadataId);
                }

                if (activePage === "timeline" && (takenDateUpdated === true || metadataObj.hidden === true)) {
                    timelineSettings.refreshTimeline($("#mediaTypeFilter").val()).then(function (data) {
                        // If a date section was removed refresh the timeline
                        if (dateGalleryRemoved === true) {
                            const elements = Util.elementsInViewport($(".scrollspy"));
                            let firstElementId = $(elements[0]).attr("id");
                            let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
                            timelineSettings.jumpFromTimelineToc(event, firstVisibleId, $("#mediaTypeFilter").val());
                        }
                    });
                }

                if (typeof Util !== "undefined" && dateGalleryRemoved === false && captionUpdated === true) {
                    let refreshContent = true;
                    if (activePage === "timeline") {
                        refreshContent = false;
                    }
                    // Refresh gallery if caption updated
                    const options = {
                        mediaContentList: mediaContentList,
                        refreshContent: refreshContent
                    };
                    // Util.reinitLightGalleryInstance(options);
                }

                // refresh version
                Util.setMetadataLocalStorage();

                // Reload page
                if (
                    (activePage !== "timeline" && activePage !== "recent" && activePage !== "modified" && activePage !== "folder" && activePage !== "taken" && takenDateUpdated === true) ||
                    (metadataObj.hidden === true && activePage !== "timeline") ||
                    activePage === "matches" ||
                    (Util.arraysEqual(prevPeopleArray, peopleArray) === false && activePage === "person") ||
                    ((activePage === "map" || activePage === "album") && Util.arraysEqual(prevAlbumsArray, albumsArray) === false)
                ) {
                    window.location.reload();
                }

                $("#metadataModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                $("#saveMetadata").prop('disabled', true);
                $('#metadataModalStatus').fadeOut(5000, function () {
                    $(this).removeClass('bi-check-circle').removeClass('spinner-grow');
                    $(this).css("display", "block");
                    $("#saveMetadata").prop('disabled', false);
                });
                $("#metadataModalCancel").prop('disabled', false);

                $("#propMetadata").modal('hide');

                shashin.showToastMessage("Metadata", "Metadata Saved", {
                    icon: "bi-info-circle",
                    iconColor: "#777777",
                    tag: "metadatamodal",
                    borderColor:"success"
                });
            } else {
                $("#metadataModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#metadataModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#metadataModalCancel").prop('disabled', false);

                shashin.showToastMessage("Metadata", "Could not save metadata", {
                    icon:"bi-exclamation-triangle",
                    iconColor:"#FF0000",
                    tag: "metadatamodal",
                    borderColor:"danger"
                });
            }
        } else {
            $("#metadataModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            $("#metadataModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#metadataModalCancel").prop('disabled', false);

            shashin.showToastMessage("Metadata", "Could not save metadata", {
                icon:"bi-exclamation-triangle",
                iconColor:"#FF0000",
                tag: "metadatamodal",
                borderColor:"danger"
            });
        }
        propMetadataModal._config.backdrop = true;
        propMetadataModal._config.keyboard = true;

        $("#propMetadata").modal('hide');
    } else {
        $("#metadataModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
        propMetadataModal._config.backdrop = true;
        propMetadataModal._config.keyboard = true;

        shashin.showToastMessage("Metadata", "Could not save metadata", {
            icon:"bi-exclamation-triangle",
            iconColor:"#FF0000",
            tag: "metadatamodal",
            borderColor:"danger"
        });

        $("#propMetadata").modal('hide');
    }
}

// Clear message on modal close
$('#propMetadata').on('hide.bs.modal', function () {
    if (shashin.map !== null) {
        // Set map target to null and reset
        shashin.map.setTarget(null);
        shashin.map = null;
    }

    $("#collapseMetadata").collapse("hide");
    $("#metadataModalStatus").attr("class","spinner-grow me-auto");
    $("#metadataModalStatus").invisible();
    $("#metadataModalMsg").html("");
    $("#generalTabLink").prop('disabled', false);
    $("#detailsTabLink").prop('disabled', false);
    $("#exifTabLink").prop('disabled', false);
    $("#mapTabLink").prop('disabled', false);
    $("#placeName").attr("placeholder", "");
    $("#metadataModalCancel").text("Cancel");
    $("#saveTimelineModalForm :input").prop("disabled", false);
    $("#shortLocationLabel").html("");
    $("#shortLocationLabel").attr("title", "");
    $("#mapTabMessage").text("Save coordinates in the context menu");

    if ($("#generalTabLink").length > 0) {
        const tab = new bootstrap.Tab($("#generalTabLink"));
        tab.show();
    } else if ($("#detailsTabLink").length > 0) {
        const tab = new bootstrap.Tab($("#detailsTabLink"));
        tab.show();
    }
});

$('#rescanMetadataConfirmation').on('hide.bs.modal', function () {
    $("#metadataModalCancel").prop('disabled', false);
    $("#metadataModalStatus").removeClass('bi-x-circle').removeClass('bi-check-circle').removeClass('spinner-grow');
});

// Clear message on input editing
$('#propMetadata').find(':input').bind('keypress', function() {
    $("#metadataModalStatus").attr("class","spinner-grow me-auto");
    $("#metadataModalStatus").invisible();
    $("#metadataModalMsg").html("");
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
        $("#timeTaken").val(originalTakenAtDateArray[1]);
    }
});

$("#detailsTabLink").on("click", function (e) {
    e.preventDefault();

    $("#yearTaken").attr("max", new Date().getFullYear());
    const propMetadataModal = document.getElementById('propMetadata');
    const modal = bootstrap.Modal.getInstance(propMetadataModal);
    modal.handleUpdate();
    $("#metadataModalMsg").html("");

    const metadataId = $("#metadataId").val();
    shashin.getMetadata(metadataId).then(function (metadataObj) {
        Util.populateDetailsInfo(metadataObj);
    });
});

$("#mapTabLink").on("click", function (e) {
    e.preventDefault();

    const propMetadataModal = document.getElementById('propMetadata');
    const modal = bootstrap.Modal.getInstance(propMetadataModal);
    modal.handleUpdate();
    $("#metadataModalMsg").html("");

    const metadataId = $("#metadataId").val();

    if (shashin.map === null && metadataId.length > 0) {
        shashin.getMetadata(metadataId).then(function (metadataObj) {
            shashin.openMap(metadataObj);
        });
    }
});

$("#exifTabLink").on("click", async function (e) {
    e.preventDefault();

    const propMetadataModal = document.getElementById('propMetadata');
    const modal = bootstrap.Modal.getInstance(propMetadataModal);
    modal.handleUpdate();
    $("#exifInfo").val("");

    // Get exif yaml data and display
    const metadataId = $("#metadataId").val();
    const http = new Http("get exif");
    const data = await http.ajax("get", "/exif/metadata/" + metadataId);

    let exif = "";
    if (data.hasOwnProperty("exif")) {
        exif = JSON.stringify(data.exif, null, 2);
    }
    $("#exifInfo").val(exif);

    if (exif.trim() === "") {
        $("#exifInfo").val("EXIF data not available");
    }
});

$("#generalTabLink").on("click", function (e) {
    e.preventDefault();

    const propMetadataModal = document.getElementById('propMetadata');
    const modal = bootstrap.Modal.getInstance(propMetadataModal);
    modal.handleUpdate();
});

$("#isobject").on("click", function (e) {
    const metadataId = $("#metadataId").val();

    metadataModal.closeTagPeopleDropdown(metadataId);
    if ($(this).prop("checked") === true) {
        $("#tagpeople").val("");
    }
});

$("#tagpeople").on("focus", function (e) {
    e.preventDefault();

    const metadataId = $("#metadataId").val();
    metadataModal.closeTagPeopleDropdown(metadataId);
});

$("#collapseMetadata").on("shown.bs.collapse", function (e) {
    $("#propMetadataBody").animate({
        scrollTop: $('#propMetadataBody')[0].scrollHeight - $('#propMetadataBody')[0].clientHeight
    }, 1000);
});

$('body').on("click", function(event) {
    const metadataId = $("#metadataId").val();

    if (!$(event.target).closest("#albumdropdown"+metadataId).length && !$(event.target).closest("#albumsList").length && $("#albumdropdown"+metadataId).hasClass("show")) {
        metadataModal.toggleAlbumDropdown(metadataId);
    }

    if (!$(event.target).closest("#tagpeopledropdown"+metadataId).length && !$(event.target).closest("#recognitionLabelsList").length && $("#tagpeopledropdown"+metadataId).hasClass("show")) {
        metadataModal.toggleTagPeopleDropdown(metadataId);
    }
});