(function( metadataModal, $, undefined ) {
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

$("#description").on("input paste", function () {
    $("#descriptionCharacterCount").text(500-$(this).val().length);
});

$("#confirmRescanMetadata").off("click").on("click", function (e) {
    e.preventDefault();

    const metadataId = $("#metadataId").val();
    const metadataIdArray = [];
    metadataIdArray.push(metadataId);

    Util.rescanMetadata(metadataIdArray,"propMetadata");
});

$("#rescan").off("click").on("click", async function (e) {
    if ($("#rescan").prop("checked")) {
        $("#hidden").prop("checked", false);
        $("#saveTimelineModalForm :input").prop("disabled", true);
        $("#rescan").prop("disabled", false);
        $("#metadataId").prop("disabled", false);
    } else {
        $("#saveTimelineModalForm :input").prop("disabled", false);
    }
});

$("#hidden").off("click").on("click", async function (e) {
    if ($("#hidden").prop("checked")) {
        $("#rescan").prop("checked", false);
        $("#saveTimelineModalForm :input").prop("disabled", true);
        $("#hidden").prop("disabled", false);
        $("#metadataId").prop("disabled", false);
    } else {
        $("#saveTimelineModalForm :input").prop("disabled", false);
    }
});

$("#saveMetadata").off("click").on("click", async function (e) {
    await saveMetadata(e);
});

$("#propMetadata")
.off("keydown")
.on("keydown", async function (e) {
    const isEnter =
        e.key === "Enter" ||
        e.code === "Enter" ||
        e.which === 13 ||
        e.keyCode === 13;

    if (isEnter && e.target.id !== "description" && e.target.id !== "askInput") {
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
    let ignoreDuplicateUpdated = false;
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

        if (($("#ignoreduplicates").prop("checked") && metadataObj.duplicateHash !== null) ||
            ($("#ignoreduplicates").prop("checked") === false && metadataObj.duplicateHash === null)
        ) {
            ignoreDuplicateUpdated = true;
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
            isObject: $("#isobject").prop("checked"),
            removeDuplicateHash: $("#ignoreduplicates").prop("checked")
        };

        const peopleArray = people.split(",").map(function (item) {
            return item.trim();
        });

        const albumsArray = albums.split(",").map(function (item) {
            return item.trim();
        });

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

        if (activePage === "timeline") {
            setTimeout(function () {
                const http = new Http("timeline");
                http.ajax("get", "/timeline/yearmonthcounts/all").then(function (data) {
                    if (data.hasOwnProperty("metadataDates") && data.hasOwnProperty("metadataYearMonthCount") && data.hasOwnProperty("metadataDatesHash")) {
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
                            const jumpFromTimelineToc = timelineSettings.once(timelineSettings.jumpFromTimelineToc);
                            jumpFromTimelineToc(event, firstVisibleId, $("#mediaTypeFilter").val());
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

                if (activePage === "duplicates" && ignoreDuplicateUpdated === true) {
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

                shashin.showToastMessage(shashin.getTranslatedValue('main.toast.metadata.title'), shashin.getTranslatedValue('main.toast.metadata.message.success'), {
                    icon: "bi-info-circle",
                    iconColor: "#777777",
                    tag: "metadatamodal",
                    borderColor:"success"
                });
            } else {
                $("#metadataModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#metadataModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#metadataModalCancel").prop('disabled', false);

                shashin.showToastMessage(shashin.getTranslatedValue('main.toast.metadata.title'), shashin.getTranslatedValue('main.toast.metadata.message.fail'), {
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

            shashin.showToastMessage(shashin.getTranslatedValue('main.toast.metadata.title'), shashin.getTranslatedValue('main.toast.metadata.message.fail'), {
                icon:"bi-exclamation-triangle",
                iconColor:"#FF0000",
                tag: "metadatamodal",
                borderColor:"danger"
            });
        }
        propMetadataModal._config.backdrop = true;
        propMetadataModal._config.keyboard = true;
        $('#propMetadata').modal({keyboard: true});

        $("#propMetadata").modal('hide');
    } else {
        $("#metadataModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
        propMetadataModal._config.backdrop = true;
        propMetadataModal._config.keyboard = true;
        $('#propMetadata').modal({keyboard: true});

        shashin.showToastMessage(shashin.getTranslatedValue('main.toast.metadata.title'), shashin.getTranslatedValue('main.toast.metadata.message.fail'), {
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

    $('#propMetadata').modal({keyboard: true});
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

$("#refreshTakenDate").off("click").on("click", function (e) {
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

$("#detailsTabLink").off("click").on("click", function (e) {
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

$("#mapTabLink").off("click").on("click", function (e) {
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

$("#exifTabLink").off("click").on("click", async function (e) {
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
        $("#exifInfo").val(shashin.getTranslatedValue('main.modal.exif.na'));
    }
});

$("#generalTabLink").off("click").on("click", function (e) {
    e.preventDefault();

    const propMetadataModal = document.getElementById('propMetadata');
    const modal = bootstrap.Modal.getInstance(propMetadataModal);
    modal.handleUpdate();
});

$("#isobject").off("click").on("click", function (e) {
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

// Scroll to top when clicking nav link
document.querySelectorAll('.nav-link').forEach(linkItem => {
    linkItem.addEventListener('click', _ => {
        document.getElementById("propMetadataBody").scrollTo(0, 0);
    });
});

// Prevent key events from propagating to the background
$('#propMetadata').on('keydown', function (e) {
    e.stopPropagation(); // prevent bubbling to background
});

// ── Ollama Ask tab ────────────────────────────────────────────────────────────

let askOllamaModel = null;
let askLoadedForMetadataId = null;

// Check once on page load
$.get("/ollama/status", function(data) {
    if (data.available) {
        askOllamaModel = data.model;
        $("#askTabNav").show();
        $("#askModelLabel").text("Model: " + askOllamaModel);
    }
});

function askAppendMessage(role, content, createdAt) {
    const $chat = $("#askChatMessages");
    if (role === "user") {
        $chat.append(
            $('<div class="d-flex justify-content-end mb-2">').append(
                $('<div class="p-3 rounded bg-primary text-white">').css({"max-width":"75%","word-break":"break-word"}).text(content)
            )
        );
    } else if (role === "assistant") {
        const timeStr = createdAt || new Date().toLocaleString();
        $chat.append(
            $('<div class="d-flex flex-column mb-2">').append(
                $('<div class="p-3 rounded">').css({"background":"var(--bs-secondary-bg)","word-break":"break-word"}).text(content),
                $('<small class="text-muted mt-1">').text(timeStr + " · " + (askOllamaModel || ""))
            )
        );
    } else if (role === "thinking") {
        $chat.append(
            $('<div id="askThinking" class="d-flex align-items-center gap-2 text-muted p-2">').append(
                $('<div class="spinner-border spinner-border-sm">'),
                $('<small>').text("Thinking...")
            )
        );
    } else if (role === "error") {
        $chat.append($('<div class="text-danger small p-2">').text(content));
    }
    const el = $chat[0];
    el.scrollTop = el.scrollHeight;
}

function askLoadConversation(metadataId) {
    if (askLoadedForMetadataId === metadataId) return;
    askLoadedForMetadataId = metadataId;
    $("#askChatMessages").empty();
    $.get("/photo/" + metadataId + "/conversation", function(messages) {
        messages.forEach(function(msg) {
            askAppendMessage(msg.role, msg.content, msg.createdAt);
        });
    });
}

function askSend() {
    const question = $("#askInput").val().trim();
    if (!question) return;
    const metadataId = $("#metadataId").val();
    if (!metadataId) return;

    $("#askInput").val("");
    askAppendMessage("user", question);
    askAppendMessage("thinking", "");

    $.ajax({
        url: "/photo/" + metadataId + "/ask",
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({ question: question }),
        success: function(data) {
            $("#askThinking").remove();
            if (data.error) {
                askAppendMessage("error", data.error);
                return;
            }
            askAppendMessage("assistant", data.answer || "", data.createdAt);
        },
        error: function() {
            $("#askThinking").remove();
            askAppendMessage("error", "Something went wrong. Please try again.");
        }
    });
}

// Reset loaded marker when modal opens a new photo so conversation reloads
$("#propMetadata").on("show.bs.modal", function() {
    askLoadedForMetadataId = null;
});

$("#askSendBtn").off("click").on("click", function() { askSend(); });

$("#askInput").on("keydown", function(e) {
    if ((e.key === "Enter" || e.keyCode === 13) && !e.shiftKey) {
        e.preventDefault();
        askSend();
    }
});

$("#askTabLink").off("click").on("click", function(e) {
    e.preventDefault();
    const propMetadataModal = document.getElementById("propMetadata");
    const modal = bootstrap.Modal.getInstance(propMetadataModal);
    modal.handleUpdate();
    askLoadConversation($("#metadataId").val());
});

// ── End Ask tab ────────────────────────────────────────────────────────────────

$('body').off("click").on("click", function(event) {
    const metadataId = $("#metadataId").val();

    if (!$(event.target).closest("#albumdropdown"+metadataId).length && !$(event.target).closest("#albumsList").length && $("#albumdropdown"+metadataId).hasClass("show")) {
        metadataModal.toggleAlbumDropdown(metadataId);
    }

    if (!$(event.target).closest("#tagpeopledropdown"+metadataId).length && !$(event.target).closest("#recognitionLabelsList").length && $("#tagpeopledropdown"+metadataId).hasClass("show")) {
        metadataModal.toggleTagPeopleDropdown(metadataId);
    }
});