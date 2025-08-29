(function( shashin, $, undefined ) {
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
        if ($('.bi-circle-fill').length > 0 || $(_this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
            $('.thumbnail-bl').hide();
            $('.thumbnail-centered').hide();
            //$('.thumbnail-tr').hide();
            $('.thumbnail-br').hide();
        }
    };

    shashin.setPhotoOverlays = function (metadata, view) {
        const opaque = 0.3;
        const transparent = 1.0;

        let metadataIdArray = shashin.getMetadataIdList();
        shashin.printMessageToConsole("shashin.setPhotoOverlays for "+metadata.id);
        shashin.printMessageToConsole(metadataIdArray);
        // Track already selected
        const index = metadataIdArray.indexOf(metadata.id);

        // If the current metadata present in metadata array, mark as selected while scrolling
        if (index > -1) {
            $("#tntl" + metadata.id).show();
            $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
            $("#image" + metadata.id).css("opacity", opaque);
            $("#tncentered" + metadata.id).hide();
            $("#tnbr" + metadata.id).hide();
            $("#tnbl" + metadata.id).hide();
        }

        $("#select" + metadata.id).on("click", function (e) {
            e.preventDefault();

            shashin.selectClick(metadata.id, view, opaque, transparent, metadataIdArray, true);
        });

        $("#image" + metadata.id).on('error', function() {
            $("#image" + metadata.id).attr("src", "/api/v1/thumbnails/"+(Util.isMobile() ? "100" : "225")+"/"+metadata.id);
        });

        $("#image" + metadata.id).on("click", function (e) {
            e.preventDefault();

            // Check for single click, avoid dblclick events
            if (Util.isMobile() === true) {
                if (e.detail === 1) {
                    shashin.touchTimer = setTimeout(() => {
                        imageClickEvent();
                    }, 200);
                }
            } else {
                imageClickEvent();
            }

            function imageClickEvent() {
                // Fill top left icon when clicking anywhere on thumbnail
                if ($('.bi-circle-fill').length > 0 || metadataIdArray.length > 0) {
                    const isSelected = $("#tlicon" + metadata.id).attr("class") === "bi-circle";
                    const isVideo = metadata.type.includes("video");

                    shashin.lastSelectedMetadataId = metadata.id;
                    shashin.lastSelectedMetadataSelected = isSelected;

                    shashin.updateSelectionUI(metadata.id, isSelected, opaque);

                    shashin.updateSelectionState(metadata.id, isSelected, isVideo, view);

                    shashin.updateBorderUI(metadata.id);
                } else {
                    shashin.removeAllMetadataFilenamesList();
                    shashin.removeAllMetadataThumbnailsList();
                    shashin.removeAllMetadataIdList();
                }

                metadataIdArray = shashin.getMetadataIdList();

                shashin.updateToolbarUI(view, metadataIdArray);

                shashin.updateSelectionCount(metadataIdArray);

                shashin.updateShareUI(view, metadataIdArray);

                shashin.setDateSection(metadata.id, view);
            }
        });

        $("#photoThumbnailContainer" + metadata.id).hover(function (e) {
            e.preventDefault();

            // Multi select
            $(document).bind("keydown", function (e) {
                e.preventDefault();

                // Shift key may not be available for Mac
                if (Util.getOS() === "MacOS" && (e.key === "s" || e.code === "KeyS" || e.which === 83 || e.keyCode === 83)) {
                    shashin.printMessageToConsole("s key pressed", {tag: "multiselect"});

                    metadataIdArray = shashin.batchSelect(metadata.id, view);
                }

                if (e.key === "Shift" || e.code === "ShiftLeft" || e.code === "ShiftRight" || e.which === 16 || e.keyCode === 16) {
                    shashin.printMessageToConsole("Shift key pressed", {tag: "multiselect"});

                    metadataIdArray = shashin.batchSelect(metadata.id, view);
                }
            });

            if (Util.isMobile() === true) {
                $(document).bind("dblclick", function (e) {
                    e.preventDefault();

                    shashin.printMessageToConsole("double tap detected", {tag: "multiselect"});
                    metadataIdArray = shashin.batchSelect(metadata.id, view);

                    clearTimeout(shashin.touchTimer);
                });
            }

            if (metadata.type.includes("video") && Util.isMobile() === false && (view !== "timeline" || (view === "timeline" && timelineSettings && timelineSettings.isScrolling === false))
            ) {
                if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                    const gifUrl = $("#image" + metadata.id).attr("src").replace("/225/" + metadata.id, "/gif/" + metadata.id);
                    $("#image" + metadata.id).attr("src", gifUrl);
                } else if ($("#tlicon" + metadata.id).attr("class") === "bi-circle-fill") {
                    const jpgUrl = $("#image" + metadata.id).attr("src").replace("/gif/" + metadata.id, "/225/" + metadata.id);
                    $("#image" + metadata.id).attr("src", jpgUrl);
                }
            }
        }, function () {
            $(document).unbind("keydown");
            $(document).unbind("dblclick");

            if (metadata.type.includes("video") && Util.isMobile() === false) {
                const jpgUrl = $("#image" + metadata.id).attr("src").replace("/gif/" + metadata.id, "/225/" + metadata.id);
                $("#image" + metadata.id).attr("src", jpgUrl);
            }
        });

        $("#image" + metadata.id).hover(function () {
            // Only show overlays when scrolling stopped in timeline view
            //if (view !== "timeline" || (view === "timeline" && timelineSettings && timelineSettings.isScrolling === false)) {
            shashin.imageHover(this, metadata.id);
            //}
        }, function () {
            metadataIdArray = shashin.getMetadataIdList();
            const index = metadataIdArray.indexOf(metadata.id);

            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill" && index <= -1) {
                $(this).css("opacity", 1.0);
                $(this).siblings(".thumbnail-tl").hide();
                $(this).siblings(".thumbnail-bl").hide();
                $(this).siblings(".thumbnail-centered").hide();
                //$(this).siblings(".thumbnail-tr").hide();
                $(this).siblings(".thumbnail-br").hide();
            } else {
                if ($('.bi-circle-fill').length > 0 || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                    $('.thumbnail-bl').hide();
                    $('.thumbnail-centered').hide();
                    //$('.thumbnail-tr').hide();
                    $('.thumbnail-br').hide();
                }
                $("#tncentered" + metadata.id).hide();
                $("#tnbl" + metadata.id).hide();
                //$("#tntr" + metadata.id).hide();
                $("#tnbr" + metadata.id).hide();
            }
        });

        $("#tncentered" + metadata.id).hover(function () {
            $('#currentlat').val(metadata.lat === null ? "" : metadata.lat);
            $('#currentlng').val(metadata.lng === null ? "" : metadata.lng);
            $('#currentyear').val(metadata.year === null ? "" : metadata.year);
            $('#currentmonth').val(metadata.month === null ? "" : metadata.month);
            $('#currentday').val(metadata.day === null ? "" : metadata.day);
            $('#currentfilename').val(metadata.fileName === null ? "" : metadata.fileName);
            metadataIdArray = shashin.getMetadataIdList();

            let hoverColor = "white";
            if (shashin.darkMode === true) {
                hoverColor = "slategray";
            }
            $('.bi-play-btn').css("color", hoverColor);
            $('.bi-play-circle').css("color", hoverColor);
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-bl").show();
            $(this).siblings(".thumbnail-br").show();
            $(this).siblings(".thumbnail-tr").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill').length > 0 || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            $('.bi-play-btn').css("color", "lightgray");
            $('.bi-play-circle').css("color", "lightgray");
            $(this).hide();
            $(this).siblings(".thumbnail-tl").hide();
            $(this).siblings(".thumbnail-bl").hide();
            $(this).siblings(".thumbnail-br").hide();
            //$(this).siblings(".thumbnail-tr").hide();
            $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
        });

        $("#tntl" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetadataIdList();
            const index = metadataIdArray.indexOf(metadata.id);
            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill" && index <= -1) {
                $(this).show();
                $(this).siblings(".thumbnail-centered").show();
                $(this).siblings(".thumbnail-tr").show();
                $(this).siblings(".thumbnail-br").show();
                $(this).siblings(".thumbnail-bl").show();
                $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
                if ($('.bi-circle-fill').length > 0 || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                    $('.thumbnail-bl').hide();
                    $('.thumbnail-centered').hide();
                    //$('.thumbnail-tr').hide();
                    $('.thumbnail-br').hide();
                }
            }
        }, function () {
            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill") {
                $(this).hide();
                $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
            } else {
                $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            }
            $(this).siblings(".thumbnail-centered").hide();
            //$(this).siblings(".thumbnail-tr").hide();
            $(this).siblings(".thumbnail-br").hide();
            $(this).siblings(".thumbnail-bl").hide();
        });

        $("#tnbl" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetadataIdList();
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-centered").show();
            $(this).siblings(".thumbnail-tr").show();
            $(this).siblings(".thumbnail-br").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill').length > 0 || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            $(this).hide();
            $(this).siblings(".thumbnail-tl").hide();
            $(this).siblings(".thumbnail-centered").hide();
            //$(this).siblings(".thumbnail-tr").hide();
            $(this).siblings(".thumbnail-br").hide();
            $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
        });

        $("#tnbr" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetadataIdList();
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-centered").show();
            $(this).siblings(".thumbnail-tr").show();
            $(this).siblings(".thumbnail-bl").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill').length > 0 || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            $(this).hide();
            $(this).siblings(".thumbnail-tl").hide();
            $(this).siblings(".thumbnail-centered").hide();
            //$(this).siblings(".thumbnail-tr").hide();
            $(this).siblings(".thumbnail-bl").hide();
            $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
        });

        $("#tntr" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetadataIdList();
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-centered").show();
            $(this).siblings(".thumbnail-bl").show();
            $(this).siblings(".thumbnail-br").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill').length > 0 || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            if ($(this).siblings(".thumbnail-tl").find('.bi-circle-fill').length === 0) {
                $(this).siblings(".thumbnail-tl").hide();
                $(this).siblings(".thumbnail-centered").hide();
                $(this).siblings(".thumbnail-bl").hide();
                $(this).siblings(".thumbnail-br").hide();
                $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
            }
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

    shashin.clearSelection = function (viewType) {
        if (shashin.downloadInstance !== null) {
            shashin.downloadInstance.abort();
            shashin.downloadInstance = null;
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.cancel.download"), shashin.getTranslatedValue("main.toast.cancel.download"), {
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