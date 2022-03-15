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

$("#saveMetadata").on("click", function (e) {
    e.preventDefault();
    $("#timelineModalMsg").html("");
    $("#timelineModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#timelineModalStatus").css("visibility","visible");
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
            id:metadataId,
            title:Util.decodeHtml($("#title").val().trim()),
            description:Util.decodeHtml($("#description").val().trim()),
            camera:Util.decodeHtml($("#camera").val().trim()),
            year:$("#yearTaken").val(),
            month:$("#monthTaken").val(),
            day:$("#dayTaken").val(),
            time:$("#timeTaken").val(),
            offset:$("#offsetTaken").val() === null ? "" : $("#offsetTaken").val(),
            latlng:Util.decodeHtml($("#latlng").val()),
            keywords:Util.decodeHtml($("#keywords").val()),
            tagpeople:Util.decodeHtml($("#tagpeople").val()),
            albumnames:Util.decodeHtml($("#albumnames").val()),
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
        .fail(function(xhr, textStatus) {
            $("#timelineModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
            $("#timelineModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#timelineModalCancel").prop('disabled', false);
            shashin.onFail(xhr, textStatus, ajaxParams, " updating timeline data");
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
                            '   <button class="btn btn-outline-secondary dropdown-toggle" id="tagalbumdropdown" type="button" aria-haspopup="true" aria-expanded="false">Albums</button>\n' +
                            '   <div class="dropdown-menu" id="albumNameList">\n';

                        for (let index in albumList) {
                            const album = albumList[index];

                            if ($("#"+album.id).length === 0) {
                                renderAlbumList = true;
                            }

                            batchHtml +=
                                '<button class="dropdown-item" type="button">\n' +
                                '    <input type="checkbox" class="album" id="'+album.id+'" value="'+album.name+'" name="albums[]">\n' +
                                '    <label for="'+album.id+'">'+album.name+'</label>\n' +
                                '</button>\n';
                        }

                        batchHtml +=
                            '   </div>\n' +
                            '</div>\n';

                        if (true === renderAlbumList) {
                            $("#albumListForModal").html(batchHtml);
                            $("#tagalbumdropdown").on("click", function (e) {
                                e.preventDefault();
                                timelineBatchModal.toggleBatchTagAlbumDropdown();
                            });
                            $(".album").on("click", function (e) {
                                timelineBatchModal.populateBatchAlbum();
                            });
                        }
                    }

                    if (data.hasOwnProperty("recognitionLabels") && data["recognitionLabels"].length > 0) {
                        let renderRecognitionLabels = false;
                        const recognitionLabels = data["recognitionLabels"];

                        let batchHtml =
                            '       <input type="text" class="form-control" aria-label="Tag People" id="tagBatchDataInput" name="tagBatchDataInput" value="">\n' +
                            '       <div class="input-group-append">\n' +
                            '           <button class="btn btn-outline-secondary dropdown-toggle" id="tagpeopledropdown" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                            '           <div class="dropdown-menu" id="peopleNameList">';

                        for (let index in recognitionLabels) {
                            const recognitionLabel = recognitionLabels[index];

                            if ($("#"+recognitionLabel.id).length === 0) {
                                renderRecognitionLabels = true;
                            }

                            batchHtml +=
                                '           <button class="dropdown-item" type="button">\n' +
                                '               <input type="checkbox" class="recognitionLabel" id="'+recognitionLabel.id+'" value="'+recognitionLabel.name+'" name="recognitionLabel[]">\n' +
                                '               <label for="'+recognitionLabel.id+'">'+recognitionLabel.name+'</label>\n' +
                                '           </button>'
                        }
                        batchHtml +=
                            '   </div>\n' +
                            '</div>\n';

                        if (true === renderRecognitionLabels) {
                            $("#batchLabelIds").html(batchHtml);
                            $("#tagBatchDataInput").on("focus", function (e) {
                                e.preventDefault();
                                timelineBatchModal.closeBatchTagPeopleDropdown();
                            });
                            $("#tagpeopledropdown").on("click", function (e) {
                                e.preventDefault();
                                timelineBatchModal.toggleBatchTagPeopleDropdown();
                            });
                            $(".recognitionLabel").on("click", function (e) {
                                timelineBatchModal.populateBatchLabel();
                            });
                            $("#tagBatchDataInput").on("focus", function (e) {
                                e.preventDefault();
                                timelineBatchModal.closeBatchTagPeopleDropdown();
                            });
                        }
                    }

                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    // window.top.location = window.top.location

                    // Update tag
                    shashin.getMetadata(metadataId).then(function (metadataObj) {
                        let dateGalleryRemoved = false;
                        metadataObj.title = $("#title").val().trim() === "" ? $("#currentfilename").val() : $("#title").val().trim()
                        metadataObj.description = $("#description").val()
                        if (captionUpdated === true) {
                            $("#mediaLink" + metadataId).attr("data-sub-html", $("#description").val());
                        }
                        metadataObj.year = $("#yearTaken").val()
                        metadataObj.month = $("#monthTaken").val()
                        metadataObj.day = $("#dayTaken").val()
                        metadataObj.time = $("#timeTaken").val()
                        metadataObj.timeZone = $("#offsetTaken").val()
                        metadataObj.keywords = $("#keywords").val()
                        metadataObj.camera = $("#camera").val()
                        const latlngArray = $("#latlng").val().split(",");
                        metadataObj.lat = $.trim(latlngArray[0])
                        metadataObj.lng = $.trim(latlngArray[1])
                        if (metadataObj.lat !== null && metadataObj.lng !== null && metadataObj.lat !== "" && metadataObj.lng !== "") {
                            $("#latlng").val(metadataObj.lat + "," + metadataObj.lng)
                        }
                        metadataObj.tagpeople = $("#tagpeople").val()
                        metadataObj.albumlist = $("#albumnames").val()
                        metadataObj.hidden = $("#hidden").prop("checked")

                        if (metadataObj.hidden === false) {
                            Util.populateDetailsInfo(metadataObj, "propTimelineModal")

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
                            shashin.refreshTimeline($("#mediaTypeFilter").val()).then(function () {
                                // If a date section was removed refresh the timeline
                                if (dateGalleryRemoved === true) {
                                    const elements = $(".scrollspy").withinviewport()
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
                    });
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#timelineModalStatus").attr("title", shashin.modalStatusFailMessage());
                    $("#timelineModalCancel").prop('disabled', false);
                }
                //$("#timelineModalMsg").html(message);
            } else {
                $("#timelineModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#timelineModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#timelineModalCancel").prop('disabled', false);
            }
            //$("#timelineModalStatus").css("visibility","hidden");
        });
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
});

$("#mapTabLink").on("click", function (e) {
    e.preventDefault();

    const propTimelineModal = document.getElementById('propTimelineModal');
    const modal = bootstrap.Modal.getInstance(propTimelineModal);
    modal.handleUpdate();
    $("#timelineModalMsg").html("");
    $("#saveMetadata").prop('disabled', true);

    const metadataId = $("#metadataId").val();

    shashin.getMetadata(metadataId).then(function (data) {
        shashin.openMap(data)
    });
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