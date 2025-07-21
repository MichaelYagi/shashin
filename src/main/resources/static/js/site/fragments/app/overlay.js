(function( shashin, $, undefined ) {
    shashin.setPhotoOverlays = function (metadata, view) {
        const opaque = 0.3;
        const transparent = 1.0;
        const id = metadata.id;
        let metadataIdArray = shashin.getMetadataIdList();
        const isVideo = metadata.type.includes("video");

        function show(...selectors) {
            selectors.forEach(sel => $(sel).show());
        }

        function hide(...selectors) {
            selectors.forEach(sel => $(sel).hide());
        }

        function updateOpacity(opacity) {
            $("#image" + id).css("opacity", opacity);
        }

        function isSelected() {
            return $("#tlicon" + id).hasClass("bi-circle-fill");
        }

        function updateIcon(selected) {
            $("#tlicon" + id)
                .toggleClass("bi-circle", !selected)
                .toggleClass("bi-circle-fill", selected);
        }

        function swapImageType(toGif) {
            const src = $("#image" + id).attr("src");
            const updated = toGif
                ? src.replace("/225/" + id, "/gif/" + id)
                : src.replace("/gif/" + id, "/225/" + id);
            $("#image" + id).attr("src", updated);
        }

        function updateThumbnailCommon() {
            metadataIdArray = shashin.getMetadataIdList();
            shashin.updateToolbarUI(view, metadataIdArray);
            shashin.updateSelectionCount(metadataIdArray);
            shashin.updateShareUI(view, metadataIdArray);
            shashin.setDateSection(id, view);
        }

        if (metadataIdArray.includes(id)) {
            $("#tntl" + id).show();
            updateIcon(true);
            updateOpacity(opaque);
            hide("#tncentered" + id, "#tnbr" + id, "#tnbl" + id);
        }

        $("#select" + id).on("click", function (e) {
            e.preventDefault();
            shashin.selectClick(id, view, opaque, transparent, metadataIdArray, true);
        });

        $("#image" + id).on("error", function () {
            const size = Util.isMobile() ? "100" : "225";
            $(this).attr("src", `/api/v1/thumbnails/${size}/${id}`);
        });

        $("#image" + id).on("click", function (e) {
            e.preventDefault();

            if (Util.isMobile() && e.detail === 1) {
                shashin.touchTimer = setTimeout(imageClickEvent, 200);
            } else {
                imageClickEvent();
            }

            function imageClickEvent() {
                const selected = !isSelected();
                shashin.lastSelectedMetadataId = id;
                shashin.lastSelectedMetadataSelected = selected;

                shashin.updateSelectionUI(id, selected, opaque);
                shashin.updateSelectionState(id, selected, isVideo, view);
                shashin.updateBorderUI(id);
                updateThumbnailCommon();
            }
        });

        $("#photoThumbnailContainer" + id).hover(
            function () {
                $(document).on("keydown", function (e) {
                    const isShift = e.key === "Shift" || e.keyCode === 16;
                    const isMacS = Util.getOS() === "MacOS" && (e.key.toLowerCase() === "s" || e.keyCode === 83);

                    if (isShift || isMacS) {
                        shashin.printMessageToConsole(`${e.key} key pressed`, { tag: "multiselect" });
                        metadataIdArray = shashin.batchSelect(id, view);
                    }
                });

                if (Util.isMobile()) {
                    $(document).on("dblclick", function () {
                        shashin.printMessageToConsole("double tap detected", { tag: "multiselect" });
                        metadataIdArray = shashin.batchSelect(id, view);
                        clearTimeout(shashin.touchTimer);
                    });
                }

                if (isVideo && !Util.isMobile() && (view !== "timeline" || !timelineSettings?.isScrolling)) {
                    const iconClass = $("#tlicon" + id).attr("class");
                    swapImageType(iconClass === "bi-circle");
                }
            },
            function () {
                $(document).off("keydown");
                $(document).off("dblclick");

                if (isVideo && !Util.isMobile()) {
                    swapImageType(false);
                }
            }
        );

        $("#image" + id).hover(
            function () {
                shashin.imageHover(this, id);
            },
            function () {
                metadataIdArray = shashin.getMetadataIdList();
                if (!isSelected() && !metadataIdArray.includes(id)) {
                    updateOpacity(transparent);
                    hide($(this).siblings(".thumbnail-tl"),
                        $(this).siblings(".thumbnail-bl"),
                        $(this).siblings(".thumbnail-centered"),
                        $(this).siblings(".thumbnail-br"));
                } else {
                    hide("#tncentered" + id, "#tnbl" + id, "#tnbr" + id);
                }
            }
        );

        // Overlay Hover Zones
        const overlayMap = {
            tncentered: () => {
                $('#currentlat').val(metadata.lat ?? "");
                $('#currentlng').val(metadata.lng ?? "");
                $('#currentyear').val(metadata.year ?? "");
                $('#currentmonth').val(metadata.month ?? "");
                $('#currentday').val(metadata.day ?? "");
                $('#currentfilename').val(metadata.fileName ?? "");
                const color = shashin.darkMode ? "slategray" : "white";
                $(".bi-play-btn, .bi-play-circle").css("color", color);
                show(`#tncentered${id}`, `#tntl${id}`, `#tnbl${id}`, `#tnbr${id}`, `#tntr${id}`);
                updateOpacity(opaque);
            },
            tntl: () => {
                if (!isSelected() && !metadataIdArray.includes(id)) {
                    show(`#tntl${id}`, `#tncentered${id}`, `#tntr${id}`, `#tnbr${id}`, `#tnbl${id}`);
                    updateOpacity(opaque);
                }
            },
            tnbl: () => {
                show(`#tnbl${id}`, `#tntl${id}`, `#tncentered${id}`, `#tntr${id}`, `#tnbr${id}`);
                updateOpacity(opaque);
            },
            tnbr: () => {
                show(`#tnbr${id}`, `#tntl${id}`, `#tncentered${id}`, `#tntr${id}`, `#tnbl${id}`);
                updateOpacity(opaque);
            },
            tntr: () => {
                show(`#tntr${id}`, `#tntl${id}`, `#tncentered${id}`, `#tnbl${id}`, `#tnbr${id}`);
                updateOpacity(opaque);
            }
        };

        const hideOverlay = () => {
            $(".bi-play-btn, .bi-play-circle").css("color", "lightgray");
            hide(`#tntl${id}`, `#tncentered${id}`, `#tnbl${id}`, `#tnbr${id}`, `#tntr${id}`);
            updateOpacity(transparent);
        };

        Object.keys(overlayMap).forEach(key => {
            $("#" + key + id).hover(
                function () {
                    metadataIdArray = shashin.getMetadataIdList();
                    overlayMap[key]();
                },
                function () {
                    if (key === "tntl") {
                        if (!isSelected()) {
                            hide(`#${key}${id}`);
                            updateOpacity(transparent);
                        } else {
                            updateOpacity(opaque);
                        }
                    } else {
                        hideOverlay();
                    }
                }
            );
        });
    };

    shashin.getOverlayData = function(metadata, args) {
        const overlays = [];
        const data = {};

        data.metadata = metadata;

        if (metadata.type.includes("video")) {
            overlays.push("isVideo");
            data.duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
        } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight*2) {
            overlays.push("isPan");
        } else if (metadata.expectedExtension === "gif") {
            overlays.push("isGif");
        }

        if (typeof args !== "undefined") {
            if (args.hasOwnProperty("overlayFlags")) {
                data.overlayFlags = args.overlayFlags;
            }

            if (args.hasOwnProperty("galleryIndex")) {
                data.galleryIndex = args.galleryIndex;
            }

            if (args.hasOwnProperty("labelPhotoMap")) {
                const labelPhotoMap = args.labelPhotoMap;
                if (labelPhotoMap.hasOwnProperty(metadata.id) === true && labelPhotoMap[metadata.id].hasOwnProperty("isTagged") === true && labelPhotoMap[metadata.id].isTagged === true) {
                    overlays.push("isTagged");
                }
            }

            if (args.hasOwnProperty("editControls") && args.editControls === true) {
                overlays.push("isEditControls");
            } else {
                overlays.push("isInfo");
            }

            if (args.hasOwnProperty("editIcon")) {
                data.editIcon = args.editIcon;
            }

            if (args.hasOwnProperty("blOnClickFunction") && args.hasOwnProperty("onClickIdPrefix")) {
                overlays.push("isBlOnClickFunction");
                data.blOnClickFunction = args.blOnClickFunction;
                data.onClickIdPrefix = args.onClickIdPrefix;
            } else if (args.hasOwnProperty("onClickIdPrefix")) {
                overlays.push("isOnClickIdPrefix");
                data.onClickIdPrefix = args.onClickIdPrefix;
            } else if (args.hasOwnProperty("blOnClickFunction")) {
                data.blOnClickFunction = args.blOnClickFunction;
            }

            if (args.hasOwnProperty("cOnClickFunction")) {
                data.cOnClickFunction = args.cOnClickFunction;
            }

            if (args.hasOwnProperty("favoriteCount")) {
                overlays.push("isFavorites");
                data.favoriteCount = args.favoriteCount;
                data.favoriteIcon = args.favoriteIcon;
            }

            if (args.hasOwnProperty("albumPhotoCommentsMap")) {
                overlays.push("isComments");
                data.albumPhotoCommentsMap = args.albumPhotoCommentsMap;
            }
        } else {
            overlays.push("isInfo");
        }

        return {overlays:overlays,data:data};
    };

    shashin.imageHover = function (_this, metadataId) {
        const metadataIdArray = shashin.getMetadataIdList();
        const index = metadataIdArray.indexOf(metadataId);

        $(_this).css("opacity", 0.3);
        $(_this).siblings().show();
        if ($("#tlicon" + metadataId).attr("class") === "bi-circle-fill" || index > -1) {
            $("#tncentered" + metadataId).hide();
            $("#tnbl" + metadataId).hide();
            $("#tnbr" + metadataId).hide();
            //$("#tntr" + metadata.id).hide();
        }
        if ($('.bi-circle-fill')[0] || $(_this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
            $('.thumbnail-bl').hide();
            $('.thumbnail-centered').hide();
            //$('.thumbnail-tr').hide();
            $('.thumbnail-br').hide();
        }
    };

    shashin.clearSelection = function (viewType) {
        if (shashin.downloadInstance !== null) {
            shashin.downloadInstance.abort();
            shashin.downloadInstance = null;
            shashin.showToastMessage("Download cancelled", "Download cancelled.", {
                icon: "bi-info-circle",
                iconColor: "#777777"
            });
            $("button").find("span").addClass('bi-download').removeClass('spinner-grow');
        }

        shashin.lastSelectedMetadataId = "";
        shashin.lastSelectedMetadataSelected = false;
        shashin.removeAllMetadataFilenamesList();
        shashin.removeAllMetadataThumbnailsList();
        shashin.removeAllMetadataIdList();

        $(".day-select").hide(); // You can toggle this per viewType if needed
        $(".thumbnail-centered").hide();
        $(".thumbnail-br").hide();
        $(".thumbnail-bl").hide();
        $(".thumbnail-tl").hide();
        $(".photo-thumbnail-image").css("opacity", 1.0);
        $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');
        $(".day-select").addClass('bi-circle').removeClass('bi-circle-fill');

        $("#appSearch").show();
        shashin.multiSelected = false;
        $(".photo-thumbnail-container").removeClass("border border-3 border-primary");
        $(".photo-thumbnail-image").removeClass("pb-1");

        // Hide all tool panels first
        $("#timelineAppTools").hide();
        $("#albumAppTools").hide();
        $("#matchesAppTools").hide();
        $("#comprefaceAppTools").hide();

        // Toggle specific tool panel based on viewType
        if (viewType === 'timeline') {
            $("#timelineTools").show();
            $("#albumTools").hide();
        } else if (viewType === 'album') {
            $("#timelineTools").hide();
            $("#albumTools").show();
        }
    };
}( window.shashin = window.shashin || {}, jQuery ));