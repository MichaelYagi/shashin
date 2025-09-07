(function( shashin, $, undefined ) {
    shashin.batchSelect = function(metadataId, view, addBorder = true, opaque = 0.3, transparent = 1.0) {
        shashin.printMessageToConsole("Select action", { tag: "multiselect" });

        function enableToolbar() {
            // Restore all links inside divs
            $("#appToolsBatchEdit, #appToolsBatchDownload, #appToolsDeselectAll, #albumAppToolsRemoveFavorites, #albumAppToolsBatchDownload, #albumAppToolsDeselectAll, #albumAppToolsRemoveAlbum").each(function() {
                $(this).css({
                    'pointer-events': 'auto',
                    'cursor': 'pointer' // or '' to reset to default
                });
            });
            $("#appToolsBatchEdit span, #appToolsBatchDownload span, #appToolsDeselectAll span, #albumAppToolsRemoveFavorites span, #albumAppToolsBatchDownload span, #albumAppToolsDeselectAll span, #albumAppToolsRemoveAlbum span").each(function() {
                $(this).css({
                    'color': '' // or use a specific color like '#000' if needed
                });
            });
            $("#appToolsBatchEdit, #appToolsBatchDownload, #appToolsDeselectAll, #albumAppToolsRemoveFavorites, #albumAppToolsBatchDownload, #albumAppToolsDeselectAll, #albumAppToolsRemoveAlbum").off('click.disableClick');
        }

        function disableToolbar() {
            $("#appToolsBatchEdit, #appToolsBatchDownload, #appToolsDeselectAll, #albumAppToolsRemoveFavorites, #albumAppToolsBatchDownload, #albumAppToolsDeselectAll, #albumAppToolsRemoveAlbum").each(function () {
                $(this).css({
                    'pointer-events': 'none',
                    'cursor': 'default'
                });
            });
            $("#appToolsBatchEdit span, #appToolsBatchDownload span, #appToolsDeselectAll span, #albumAppToolsRemoveFavorites span, #albumAppToolsBatchDownload span, #albumAppToolsDeselectAll span, #albumAppToolsRemoveAlbum span").each(function () {
                $(this).css({
                    'color': 'gray',
                });
            });
            $("#appToolsBatchEdit, #appToolsBatchDownload, #appToolsDeselectAll, #albumAppToolsRemoveFavorites, #albumAppToolsBatchDownload, #albumAppToolsDeselectAll, #albumAppToolsRemoveAlbum").on('click.disableClick', function (e) {
                e.preventDefault();
                e.stopImmediatePropagation();
            });
        }

        // Disable all links inside divs
        disableToolbar();
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
            const $image = $("#image" + id);
            if ($image.length > 0) {
                const imageUrl = $image.attr("src").replace("/gif/" + id, "/" + (Util.isMobile() ? "100" : "225") + "/" + id);
                $image.attr("src", imageUrl).css("opacity", opacityLevel);

                const $icon = $("#tlicon" + id);
                const iconClass = $icon.attr("class");
                const shouldSelect = (isSelected && iconClass === "bi-circle") || (!isSelected && iconClass === "bi-circle-fill");

                if (shouldSelect) {
                    shashin.selectClick(id, view, opaque, transparent, metadataArray, false);

                    if (!isSelected && id !== metadataId && shashin.lastSelectedMetadataId !== id) {
                        $("#tntl" + id).css("display", "none");
                        $image.attr("src", imageUrl).css("opacity", transparent);
                    } else {
                        $image.attr("src", imageUrl).css("opacity", opaque);
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

            const http = new Http("get ranged metadata");
            let version = Util.getMetadataLocalStorage();

            let url = `/metadata/range/${shashin.lastSelectedMetadataId}/${metadataId}/${view}/${shashin.mediaTypeFilter}`;

            if (version === null || version === undefined) {
                version = uuidv4();
            }

            if (view === "album") {
                const albumId = $("#albumId").val();
                url += `?albumId=${albumId}&v=${version}`;
            } else {
                url += `?v=${version}`;
            }

            http.ajax("get", url, null, function () {
                // Fail
                // Restore all links inside divs
                enableToolbar();
                Util.showSpinner(false);
            }).then(data => {
                if (data.hasOwnProperty("metadataIdArray") &&
                    data.hasOwnProperty("metadataFilenameArray") &&
                    data.hasOwnProperty("metadataThumbnailArray") &&
                    data.hasOwnProperty("metadataDatesArray")
                ) {
                    const metadataIdArray = data.metadataIdArray;
                    const metadataFilenameArray = data.metadataFilenameArray;
                    const metadataThumbnailArray = data.metadataThumbnailArray;
                    const isSelected = shashin.lastSelectedMetadataSelected;
                    const pendingUpdates = [];

                    setTimeout(function () {
                        if (isSelected) {
                            shashin.addAllToMetadataIdList(metadataIdArray, true);
                            shashin.addAllToFilenameList(metadataFilenameArray, true);
                            shashin.addAllToThumbnailList(metadataThumbnailArray, true);
                        } else {
                            shashin.removeMetadataIdListWithArray(metadataIdArray);
                            shashin.removeMetadataFilenamesListWithArray(metadataFilenameArray);
                            shashin.removeMetadataThumbnailsListWithArray(metadataThumbnailArray);
                        }

                        for (let i = 0; i < metadataIdArray.length; i++) {
                            const id = metadataIdArray[i];
                            const isSelected = shashin.lastSelectedMetadataSelected;
                            const opacityLevel = isSelected ? opaque : transparent;

                            pendingUpdates.push(() => {
                                updateImageSelection(id, view, isSelected, opacityLevel, metadataIdArray);

                                const $image = $("#image" + id);
                                if ($image.length > 0) {
                                    const imageUrl = $image.attr("src").replace("/gif/" + id, "/" + (Util.isMobile() ? "100" : "225") + "/" + id);
                                    $image.attr("src", imageUrl);
                                }
                            });
                        }

                        requestAnimationFrame(() => {
                            pendingUpdates.forEach(fn => fn());
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

                // Restore all links inside divs
                enableToolbar();
                Util.showSpinner(false);
            });
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
}( window.shashin = window.shashin || {}, jQuery ));