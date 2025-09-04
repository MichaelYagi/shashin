(function( shashin, $, undefined ) {
    shashin.batchSelect = function(metadataId, view, addBorder = true, opaque = 0.3, transparent = 1.0) {
        shashin.printMessageToConsole("Select action", { tag: "multiselect" });

        Util.showSpinner(true);

        let metadataIdArrayCopy = shashin.getMetadataIdList();

        if (metadataIdArrayCopy.length === 0) {
            return metadataIdArrayCopy;
        }

        const resetBorders = () => {
            $('.photo-thumbnail-container').removeClass("border border-3 border-primary");
            $('.photo-thumbnail-image').removeClass("pb-1");
        };

        const updateImageSelection = (id, view, isSelected, opacityLevel, metadataArray) => {
            if ($("#image" + id).length > 0) {
                const imageUrl = $("#image" + id).attr("src").replace("/gif/" + id, "/" + (Util.isMobile() ? "100" : "225") + "/" + id);

                $("#image" + id).attr("src", imageUrl);

                const iconClass = $("#tlicon" + id).attr("class");
                const shouldSelect = (isSelected && iconClass === "bi-circle") || (!isSelected && iconClass === "bi-circle-fill");

                if (shouldSelect) {
                    shashin.selectClick(id, view, opaque, transparent, metadataArray, false);
                    $("#image" + id).css("opacity", opacityLevel);

                    if (!isSelected && id !== metadataId && shashin.lastSelectedMetadataId !== id) {
                        $("#tntl" + id).css("display", "none");
                    }
                }
            }
        };

        const applyBorderToLastSelected = () => {
            if (shashin.getMetadataIdList().length > 0 && addBorder) {
                $("#photoThumbnailContainer" + shashin.lastSelectedMetadataId).addClass("border border-3 border-primary");
                $("#image" + shashin.lastSelectedMetadataId).addClass("pb-1");
                shashin.multiSelected = true;
            }
        };

        resetBorders();

        shashin.printMessageToConsole("lastSelectedMetadataId: " + shashin.lastSelectedMetadataId, { tag: "multiselect" });
        shashin.printMessageToConsole("metadataId: " + metadataId, { tag: "multiselect" });

        if (shashin.lastSelectedMetadataId && shashin.lastSelectedMetadataId !== metadataId) {
            shashin.printMessageToConsole("Select view: " + view, { tag: "multiselect" });
            shashin.printMessageToConsole("addBorder: " + addBorder, { tag: "multiselect" });

            if (view !== "timeline" && addBorder) {
                // Non-timeline selection logic. No DB lookup...
                setTimeout(function () {
                    // Get selection direction
                    const selectionHash = getElementLocation($("#photoThumbnailContainer" + shashin.lastSelectedMetadataId)[0]);
                    const pointerHash = getElementLocation($("#photoThumbnailContainer" + metadataId)[0]);
                    const direction = (pointerHash.y > selectionHash.y || (pointerHash.x > selectionHash.x && pointerHash.y >= selectionHash.y)) ? "down" : "up";

                    shashin.printMessageToConsole("Selected Media point [x, y]: " + JSON.stringify([selectionHash.x, selectionHash.y]), { tag: "multiselect" });
                    shashin.printMessageToConsole("Shift Key point [x, y]: " + JSON.stringify([pointerHash.x, pointerHash.y]), { tag: "multiselect" });
                    shashin.printMessageToConsole("Select direction: " + direction, { tag: "multiselect" });

                    const whileLimit = 1000;
                    let container = $("#photoThumbnailContainer" + (direction === "down" ? shashin.lastSelectedMetadataId : metadataId));
                    let selectedRowMetadataIds = container.siblings().addBack().map(function () {
                        return this.id.split("photoThumbnailContainer")[1];
                    }).toArray();

                    let found = selectedRowMetadataIds.includes(direction === "down" ? metadataId : shashin.lastSelectedMetadataId);
                    let index = 0;

                    while (!found && index < whileLimit) {
                        let nextContainer = container.parent().parent().nextUntil().filter(view === "timeline" ? ".dateContainer:first" : ".dateSection:first");
                        container = $(nextContainer[0]).children("div.row").children("div");

                        metadataIdArrayCopy = container.siblings().addBack().map(function () {
                            return this.id.split("photoThumbnailContainer")[1];
                        }).toArray();

                        found = metadataIdArrayCopy.includes(direction === "down" ? metadataId : shashin.lastSelectedMetadataId);

                        $.merge(selectedRowMetadataIds, metadataIdArrayCopy);
                        index++;
                    }

                    shashin.printMessageToConsole("Looped " + index + " times finding metadata", { tag: "multiselect" });

                    let start = false;
                    let lastSelectedMetadataId = shashin.lastSelectedMetadataId;

                    const compareOne = direction === "down" ? lastSelectedMetadataId : metadataId;
                    const compareTwo = direction === "down" ? metadataId : lastSelectedMetadataId;

                    for (const currentMetadataId of selectedRowMetadataIds) {
                        if (currentMetadataId === compareOne || start) {
                            if (currentMetadataId === compareOne) {
                                lastSelectedMetadataId = direction === "down" ? currentMetadataId : shashin.lastSelectedMetadataId;
                                start = true;
                                continue;
                            }

                            updateImageSelection(currentMetadataId, view, shashin.lastSelectedMetadataSelected, shashin.lastSelectedMetadataSelected ? opaque : transparent, metadataIdArrayCopy);
                            if (direction === "down") {
                                lastSelectedMetadataId = currentMetadataId;
                            }
                        }

                        if (currentMetadataId === compareTwo) {
                            updateImageSelection(metadataId, view, shashin.lastSelectedMetadataSelected, shashin.lastSelectedMetadataSelected ? opaque : transparent, metadataIdArrayCopy);
                            updateImageSelection(currentMetadataId, view, shashin.lastSelectedMetadataSelected, shashin.lastSelectedMetadataSelected ? opaque : transparent, metadataIdArrayCopy);
                            break;
                        }
                    }

                    metadataIdArrayCopy = [];
                    $(".thumbnail-tl .bi-circle-fill").each(function (i, obj) {
                        metadataIdArrayCopy.push(obj.id.substring(6, obj.id.length));
                    });

                    metadataIdArrayCopy = [...new Set(metadataIdArrayCopy)];
                    shashin.addAllToMetadataIdList(metadataIdArrayCopy);
                    shashin.updateToolbarUI(view, metadataIdArrayCopy);
                    shashin.updateSelectionCount(metadataIdArrayCopy);
                    resetBorders();
                    applyBorderToLastSelected();

                    Util.showSpinner(false);
                }, 0);

            } else if (["timeline", "accessed", "modified", "recent", "taken", "album"].includes(view) || !addBorder) {
                shashin.printMessageToConsole("Select ranged metadata: " + view, { tag: "multiselect" });

                const http = new Http("get ranged metadata");
                const version = Util.getMetadataLocalStorage();

                let url = "";
                if (view === "timeline") {
                    url = `/metadata/range/${shashin.lastSelectedMetadataId}/${metadataId}/${shashin.mediaTypeFilter}`;
                } else if (view === "album") {
                    const albumId = $("#albumId").val();
                    url = `/album/${albumId}/range/${metadataId}/${shashin.mediaTypeFilter}`;
                } else {
                    url = `/browse/range/${metadataId}/${view}/${shashin.mediaTypeFilter}`;
                }

                if (version) url += `?v=${version}`;

                http.ajax("get", url, null, function () {
                    // Fail
                    Util.showSpinner(false);
                }).then(data => {
                    if (data.hasOwnProperty("metadataIdArray")) {
                        const metadataIdArray = data.metadataIdArray;

                        setTimeout(function () {
                            metadataIdArray.forEach(([id, filename, thumbnail]) => {
                                updateImageSelection(id, view, shashin.lastSelectedMetadataSelected, shashin.lastSelectedMetadataSelected ? opaque : transparent, metadataIdArray);

                                if (shashin.lastSelectedMetadataSelected) {
                                    shashin.addToMetadataIdList(id);
                                    shashin.addToMetadataFilenamesList(filename);
                                    shashin.addToMetadataThumbnailsList(thumbnail);
                                } else {
                                    shashin.removeFromMetadataIdList(id);
                                    shashin.removeFromMetadataFilenamesList(filename);
                                    shashin.removeFromMetadataThumbnailsList(thumbnail);
                                }

                                const imageId = $("#image" + id);
                                if (imageId.length > 0) {
                                    const imageUrl = imageId.attr("src").replace("/gif/" + id, "/" + (Util.isMobile() ? "100" : "225") + "/" + id);
                                    imageId.attr("src", imageUrl);
                                }
                            });

                            shashin.updateToolbarUI(view, shashin.getMetadataIdList());
                            shashin.updateSelectionCount(shashin.getMetadataIdList());
                            resetBorders();
                            applyBorderToLastSelected();

                            if (!addBorder) {
                                shashin.lastSelectedMetadataId = "";
                            }
                        }, 0);
                    }

                    Util.showSpinner(false);
                });

            } else {
                shashin.printMessageToConsole("lastSelectionPos undefined or null", { tag: "multiselect" });
            }
        } else {
            shashin.lastSelectedMetadataId = "";
            shashin.multiSelected = false;
        }

        return metadataIdArrayCopy;
    };

    shashin.selectClick = function(metadataId, view, opaque, transparent, metadataIdArray, clicked) {
        const isSelected = $("#tlicon" + metadataId).attr("class") === "bi-circle";
        const isVideo = $("#photoThumbnailContainer" + metadataId).hasClass("is-video");

        shashin.updateSelectionUI(metadataId, isSelected, opaque, clicked);
        shashin.updateSelectionState(metadataId, isSelected, isVideo, view);
        if (clicked) {
            shashin.lastSelectedMetadataId = metadataId;
            shashin.lastSelectedMetadataSelected = isSelected;
            shashin.updateBorderUI(metadataId);
        }

        metadataIdArray = shashin.getMetadataIdList();

        shashin.updateToolbarUI(view, metadataIdArray);

        shashin.updateSelectionCount(metadataIdArray);

        shashin.updateShareUI(view, metadataIdArray);

        shashin.setDateSection(metadataId, view);
    };

    shashin.updateSelectionUI = function(metadataId, isSelected, opaque, clicked) {
        if (isSelected === false && clicked === false) {
            $("#tntl" + metadataId).hide();
        } else {
            $("#tntl" + metadataId).show();
            $("#tncentered" + metadataId).toggle(!isSelected);
            $("#tnbr" + metadataId).toggle(!isSelected);
            $("#tnbl" + metadataId).toggle(!isSelected);
        }

        $("#tlicon" + metadataId)
            .toggleClass('bi-circle-fill', isSelected)
            .toggleClass('bi-circle', !isSelected);
        $("#image" + metadataId).css("opacity", opaque);

    };

    shashin.updateSelectionState = function(metadataId, isSelected, isVideo, view) {
        if (isSelected) {
            shashin.addToMetadataIdList(metadataId);
            shashin.addToMetadataFilenamesList($('#filename' + metadataId).val());
            shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadataId).val());
            if (isVideo && !Util.isMobile()) {
                const jpgUrl = $("#image" + metadataId).attr("src").replace("/gif/" + metadataId, "/225/" + metadataId);
                $("#image" + metadataId).attr("src", jpgUrl);
            }
        } else {
            shashin.removeFromMetadataIdList(metadataId);
            shashin.removeFromMetadataFilenamesList($('#filename' + metadataId).val());
            shashin.removeFromMetadataThumbnailsList($('#thumbnailCentered' + metadataId).val());
            if (isVideo && !Util.isMobile() && (view !== "timeline" || (timelineSettings && !timelineSettings.isScrolling))) {
                const gifUrl = $("#image" + metadataId).attr("src").replace("/225/" + metadataId, "/gif/" + metadataId);
                $("#image" + metadataId).attr("src", gifUrl);
            }
        }
    };

    shashin.updateBorderUI = function(metadataId) {
        $('.photo-thumbnail-container').removeClass("border border-3 border-primary");
        $('.photo-thumbnail-image').removeClass("pb-1");
        if (shashin.multiSelected === true) {
            $("#photoThumbnailContainer" + metadataId).addClass("border border-3 border-primary");
            $("#image" + metadataId).addClass("pb-1");
        }
    };

    shashin.updateToolbarUI = function(view, metadataIdArray) {
        const hasSelection = $('.bi-circle-fill').length > 0 || metadataIdArray.length > 0;
        $("#appSearch").toggle(!hasSelection);

        const showTools = (selector, hideSelector) => {
            $(selector).show();
            if (hideSelector) $(hideSelector).hide();
        };

        if (hasSelection) {
            if (["album", "favorites", "archived"].includes(view)) {
                showTools("#albumAppTools", view === "album" ? "#albumTools" : null);
            } else if (["timeline", "recent", "accessed", "modified", "taken", "folder", "search"].includes(view)) {
                showTools("#timelineAppTools", ["timeline", "folder"].includes(view) ? "#timelineTools" : null);
            } else if (["matches", "person", "compreface"].includes(view)) {
                showTools("#matchesAppTools", "#timelineTools");
            }

            $(".thumbnail-br, .thumbnail-bl, .thumbnail-centered").hide();
        } else {
            $(".photo-thumbnail-container").removeClass("border border-3 border-primary");
            $(".photo-thumbnail-image").removeClass("pb-1");
            shashin.multiSelected = false;
            $("#timelineAppTools, #albumAppTools, #matchesAppTools").hide();

            if (["timeline", "folder", "matches", "person", "compreface"].includes(view)) {
                $("#timelineTools").show();
            } else if (view === "album") {
                $("#albumTools").show();
            }
        }
    };

    shashin.updateSelectionCount = function(metadataIdArray) {
        const count = metadataIdArray.length || $('.thumbnail-tl .bi-circle-fill').length;
        const label = count + " Selected";
        $("#timelineNumberSelected, #matchesNumberSelected, #favoritesNumberSelected, #trashNumberSelected, #albumNumberSelected").text(label);
    };

    shashin.updateShareUI = function(view, metadataIdArray) {
        if (view !== "share") return;

        const albumId = $("#albumId").val();
        const albumName = $("#albumName").val();
        const shareLink = $("#shareLink").val();
        const downloadEl = $("#download" + albumId);

        if ($('.thumbnail-tl .bi-circle-fill').length > 0) {
            $("#clearMultiSelect").show();
            $("#albumNumberSelected").show();
            downloadEl.attr({
                name: "downloadArray",
                value: JSON.stringify(metadataIdArray),
                title: "Download selected media"
            });
        } else {
            shashin.clearSelection("album");
            $("#clearMultiSelect").hide();
            shashin.removeAllMetadataIdList();
            $("#albumNumberSelected").hide();
            downloadEl.attr({
                name: "download",
                value: albumId,
                title: "Download all photos"
            });
        }

        downloadEl.off("click").on("click", () => {
            shashin.trackShareDownload(albumId, albumName, shareLink);
        });
    };

    shashin.setDateSection = function(metadataId, view) {
        setTimeout(function () {
            const rowId = $($("#photoThumbnailContainer" + metadataId).parent()[0]).attr("id");

            let date = rowId.replace("row", "");
            date = date.replace("dateBody", "");
            const selectedMetadata = shashin.getMetadataIdList();

            if (view === "timeline" || view === "taken" || view === "album" || view === "accessed" || view === "modified" || view === "recent") {
                let url = "/timeline/mediatype/" + shashin.mediaTypeFilter + "/date/" + date + "/metadata";
                if (view === "album") {
                    const albumId = $("#albumId").val();
                    url = "/album/mediatype/" + shashin.mediaTypeFilter + "/date/" + date + "/" + albumId;
                } else if (view === "accessed" || view === "modified" || view === "recent" || view === "taken") {
                    url = "/browse/mediatype/" + shashin.mediaTypeFilter + "/date/" + date + "/" + view;
                }

                const http = new Http("get month data");
                http.ajax("get", url).then(function (data) {
                    if (data && data.hasOwnProperty("status")) {
                        const metadataList = data.metadataList;
                        let dateAllSelected = true;

                        for (let index in metadataList) {
                            const metadata = metadataList[index];

                            if (selectedMetadata.includes(metadata.id) !== true) {
                                dateAllSelected = false;
                                break;
                            }
                        }

                        if (dateAllSelected) {
                            $("#select" + date).addClass("bi-circle-fill").removeClass("bi-circle");
                        } else {
                            $("#select" + date).removeClass("bi-circle-fill").addClass("bi-circle");
                        }
                    }
                });
            }
        }, 0);
    };

    shashin.dayHeadingListener = function (date, activePage, mediaTypeFilter) {
        function enterAction(date, view) {
            let listenerEl = "#dateHeader" + date;
            let dateBody = "#dateBody";
            let animateEl = "#dateHeader" + date + " > span.text-muted";

            if (view === "timeline") {
                listenerEl = "#" + date;
                dateBody = "#row";
                animateEl = "#" + date + " .dateHeading";
            } else if (view === "album") {
                animateEl = "#dateHeader" + date + " > strong";
            }

            $(listenerEl).off('mouseenter').on("mouseenter", function () {
                if ($("#select" + date).length > 0 && $(dateBody + date + " div.photo-thumbnail-container").length > 1) {
                    $(animateEl).first().animate({"marginLeft": "13.609px"}, "fast", function () {
                        // Complete
                        $(animateEl).first().css("margin-left", "0");
                        $("#select" + date).fadeIn("fast");
                        $("#select" + date).addClass("show-day-select");
                        $("#select" + date).css("display", "inline-block");
                    });
                }
            });
        }

        function leaveAction(date, view) {
            let listenerEl = "#dateHeader" + date;
            let dateBody = "#dateBody";
            let animateEl = "#" + date + " span.text-muted";

            if (view === "timeline") {
                listenerEl = "#"+date;
                dateBody = "#row";
                animateEl = "#" + date + " .dateHeading";
            } else if (view === "album") {
                animateEl = "#" + date + " strong";
            }

            $(listenerEl).off('mouseleave').on("mouseleave", function () {
                if ($("#select" + date).length > 0 && $(dateBody + date + " div.photo-thumbnail-container").length > 1) {
                    $(animateEl).first().stop(false, true);
                    $("#select" + date).stop(false, true);
                    $("#select" + date).removeClass("show-day-select");
                    $("#select" + date).css("display", "none");

                    $("#select" + date).fadeOut(500, function () {
                        $("#select" + date).removeClass("show-day-select");
                        $("#select" + date).css("display", "none");
                    });
                }
            });
        }

        enterAction(date, activePage);
        leaveAction(date, activePage);

        let clickEl = "#select"+date;

        $(clickEl).off("click").on("click", function () {
            if ($("#" + date).length > 0 && $("#select" + date).css("display") === "inline-block") {
                setTimeout(function () {
                    const http = new Http("get month data");

                    let url = "/timeline/mediatype/" + mediaTypeFilter + "/date/" + date + "/metadata";
                    if (activePage === "album") {
                        const albumId = $("#albumId").val();
                        url = "/album/mediatype/" + mediaTypeFilter + "/date/" + date + "/" + albumId;
                    } else if (activePage === "accessed" || activePage === "modified" || activePage === "recent" || activePage === "taken") {
                        url = "/browse/mediatype/" + mediaTypeFilter + "/date/" + date + "/" + activePage;
                    }

                    http.ajax("get", url).then(function (data) {
                        if (data.hasOwnProperty("status")) {
                            setTimeout(function () {
                                let firstMetadataId = null;
                                for (let index in data.metadataList) {
                                    index = parseInt(index);

                                    if (isNaN(index) === false) {
                                        const metadataId = data.metadataList[index].id;

                                        if (index === 0) {
                                            shashin.lastSelectedMetadataId = metadataId;
                                        } else if (index === data.metadataList.length-1) {
                                            firstMetadataId = metadataId;
                                        }

                                        shashin.addToMetadataIdList(metadataId);
                                    }
                                }

                                if ($("#select"+date).hasClass("bi-circle-fill")) {
                                    $("#select"+date).removeClass("bi-circle-fill").addClass("bi-circle");
                                    shashin.lastSelectedMetadataSelected = false;
                                } else {
                                    $("#select"+date).addClass("bi-circle-fill").removeClass("bi-circle");
                                    shashin.lastSelectedMetadataSelected = true;
                                }

                                shashin.batchSelect(firstMetadataId, activePage, false);
                            });
                        }
                    });
                }, 0);
            }
        });
    };

    function getElementLocation(el) {
        if (el) {
            const rect = el.getBoundingClientRect();
            return {
                x: rect.left + window.scrollX,
                y: rect.top + window.scrollY
            };
        } else {
            return {x: null,y: null};
        }
    }
}( window.shashin = window.shashin || {}, jQuery ));