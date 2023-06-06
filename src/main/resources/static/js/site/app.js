(function( shashin, $, undefined ) {
    shashin.showDebug = false;
    shashin.map = null;
    shashin.layer = null;
    shashin.feature = null;
    shashin.infiniteScrollGallery = null;
    shashin.lg = null;
    shashin.ajaxRetries = 3;
    shashin.darkMode = true;
    shashin.lgSubHtmlTimeout = null;
    shashin.nonce = "";

    function fixContentHeight() {
        if ($("div[data-role='dialog']").is(":visible")) {
            const dialog = $("div[data-role='dialog']:visible:visible");
            const contentHeight = 400;
            dialog.height(contentHeight);
            shashin.map.updateSize();
        }
    }

    shashin.getMediaContent = function(metadata) {
        const mediaContent = {};

        mediaContent.func = shashin.openInfoSidebar;
        mediaContent.args = metadata.id;

        if (metadata.type.includes("video")) {
            mediaContent.video = {
                "source": [{"src": metadata.videoUrl, "type": "video/mp4"}],
                "attributes": {
                    "preload": "auto",
                    "controls": true,
                    "autoplay": true
                }
            };
            mediaContent.downloadUrl = encodeURI(metadata.videoUrl) + "/download";
        } else {
            mediaContent.src = metadata.thumbnailUrlOriginal;
            mediaContent.downloadUrl = encodeURI(metadata.thumbnailUrlOriginal) + "/download";
        }

        if (metadata.description !== null && metadata.description !== "") {
            mediaContent.subHtml = metadata.description;
        }

        return mediaContent;
    }

    shashin.updateFavorites = function(listenerPrefix, iconPrefix, countPrefix, metadataId) {
        $(listenerPrefix+metadataId).on("click", async function (e) {
            e.preventDefault();

            if ($(iconPrefix + metadataId).hasClass("bi-suit-heart")) {
                $(iconPrefix + metadataId).removeClass("bi-suit-heart").addClass("bi-suit-heart-fill");
            } else if ($(iconPrefix + metadataId).hasClass("bi-suit-heart-fill")) {
                $(iconPrefix + metadataId).removeClass("bi-suit-heart-fill").addClass("bi-suit-heart");
            }

            const isFavorite = ($(iconPrefix + metadataId).hasClass("bi-suit-heart-fill"));

            const http = new Http("favorite");
            const json = {metadataId: metadataId, isFavorite: isFavorite};

            let data;

            if (isFavorite === true) {
                data = await http.ajax("post", "/favorite/save", JSON.stringify(json));
            } else {
                data = await http.ajax("post", "/favorite/delete", JSON.stringify(json));
            }

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("count")) {
                Util.setMetadataLocalStorage();
                $(countPrefix + metadataId).text(data["count"]);
            }
        });
    }

    shashin.modalStatusFailMessage = function() {
        return "Something went wrong. Please try again.";
    }

    shashin.onFail = function(xhr, textStatus, ajaxParams, description, failFunction) {
        $("#spinner").hide();
        shashin.printMessageToConsole("AJAX error"+description+". Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
        if (xhr.status === 403 || xhr.status === 401) {
            $(location).prop('href', '/users/login');
        } else if ((textStatus === 'timeout' || textStatus === 'error') && ajaxParams.retries-- > 0) {
            $.ajax(ajaxParams).fail(function (xhr, textStatus) {
                shashin.onFail(xhr, textStatus, ajaxParams, description, failFunction)
            });
        } else if (xhr.status !== 200 && ajaxParams.retries-- > 0) {
            $.ajax(ajaxParams).fail(function (xhr, textStatus) {
                shashin.onFail(xhr, textStatus, ajaxParams, description, failFunction)
            });
        } else if (typeof failFunction !== "undefined" && typeof failFunction === "function") {
            failFunction();
        }
    }

    shashin.checkMetadata = function(metadataId) {
        let metadata = {};

        if ($("#infoModalEdit"+metadataId).attr("tag") && $("#infoModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#infoModalEdit"+metadataId).attr("tag"));
        }

        if  ($("#mediaLink"+metadataId).attr("tag") && $("#mediaLink"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#mediaLink"+metadataId).attr("tag"));
        }

        if  ($("#timelineModalEdit"+metadataId).attr("tag") && $("#timelineModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#timelineModalEdit"+metadataId).attr("tag"));
        }

        return metadata;
    }

    shashin.getTimelineMetadata = async function(metadataId) {
        const http = new Http("get timeline metadata");
        const data = await http.ajax("get", "/api/v1/timeline/metadata/"+metadataId);

        let ret = {};
        if (data.hasOwnProperty("metadata")) {
            ret = data;
        }

        return ret;
    }

    shashin.getMetadata = async function(metadataId) {
        const http = new Http("get metadata");
        const data = await http.ajax("get", "/api/v1/metadata/"+metadataId);

        let metadata = {};
        if (data.hasOwnProperty("metadata") && data.hasOwnProperty("keywordList")) {
            metadata = data["metadata"];
            metadata["keywords"] = data["keywordList"];
        }

        return metadata;
    }

    shashin.openEditMetadataModal = function (metadataId) {
        shashin.getTimelineMetadata(metadataId).then(function (data) {
            if (data.hasOwnProperty("metadata") &&
                data.hasOwnProperty("taggedPeopleList") &&
                data.hasOwnProperty("albumList") &&
                data.hasOwnProperty("keywordList") &&
                data.hasOwnProperty("allRecognitionLabels") &&
                data.hasOwnProperty("allAlbumList"))
            {
                const metadata = data["metadata"];
                const taggedPeopleArray = data["taggedPeopleList"];
                const albumListArray = data["albumList"];
                const keywordList = data["keywordList"];
                const recognitionLabels = data["allRecognitionLabels"];
                const allAlbumList = data["allAlbumList"];
                let index;

                const keywordsAvailable = $('#keywordsString').val();
                const camerasList = $('#camerasString').val();
                const lensList = $('#lensesString').val();

                // Clear modal data
                $('#propTimelineModal').find(':input').val('');
                $("#propTimelineModalThumbnail").html("");
                $("#isobject")[0].checked = false;
                $("#hidden")[0].checked = false;

                $("#timelineModalTitle").text(metadata.title);
                $("#currentfilename").val(metadata.fileName)
                $("#currentlat").val(metadata.lat)
                $("#currentlng").val(metadata.lng)
                $("#metadataId").val(metadata.id);
                $("#keywordsString").val(keywordsAvailable);
                $("#camerasString").val(camerasList);
                $("#lensesString").val(lensList);

                if (metadata.thumbnailUrlCentered !== null) {
                    $("#propTimelineModalThumbnail").html(TimelineTemplates.HeaderThumbnail({metadata:metadata}));
                }

                if (metadata.title !== null) {
                    $("#title").val(metadata.title);
                }

                if (metadata.description !== null) {
                    $("#description").val(metadata.description);
                }

                if (metadata.camera !== null) {
                    $("#camera").val(metadata.camera);
                }

                if (metadata.lens !== null) {
                    $("#lens").val(metadata.lens);
                }

                if (metadata.timeZone !== null) {
                    $("#offsetTaken").val(metadata.timeZone);
                }
                if (metadata.time !== null) {
                    $("#timeTaken").val(metadata.time);
                }

                if (metadata.hasOwnProperty("keywords") && metadata.keywords !== null) {
                    $("#keywords").val(metadata.keywords);
                } else {
                    $("#keywords").val(keywordList);
                    metadata.keywords = keywordList
                }

                if (metadata.day !== null) {
                    $("#dayTaken").val(metadata.day);
                }
                if (metadata.month !== null) {
                    $("#monthTaken").val(metadata.month);
                }
                if (metadata.year !== null) {
                    $("#yearTaken").val(metadata.year);
                }
                if (metadata.timeZone !== null) {
                    $("#offsetTaken option[value='" + metadata.timeZone + "']").attr('selected', 'selected');
                }

                const latlngValue = (metadata.hasOwnProperty("lat") && metadata.hasOwnProperty("lng") && metadata.lat != null && metadata.lng != null && metadata.lat !== "" && metadata.lng !== "") ? ($.trim(metadata.lat) + ',' + $.trim(metadata.lng)) : '';
                $("#latlng").val(latlngValue);

                let taggedPeopleString = "";
                let isObject = false;
                for (index in taggedPeopleArray) {
                    const person = taggedPeopleArray[index];
                    if (person === "object") {
                        isObject = true;
                    } else {
                        taggedPeopleString += person + ",";
                    }
                }
                taggedPeopleString = taggedPeopleString.replace(/,\s*$/, "");
                taggedPeopleString = taggedPeopleString.trim();

                if (taggedPeopleString !== "") {
                    $("#tagpeople").val(taggedPeopleString);
                } else if (metadata.tagpeople !== null) {
                    $("#tagpeople").val(metadata.tagpeople);
                }

                if ($("#recognitionLabelInput").length > 0) {
                    $("#recognitionLabelInput").remove();
                }
                if (recognitionLabels !== null && recognitionLabels.length > 0) {
                    let html = ModalTemplates.PersonModalDropdownHead({metadata:metadata});
                        // '<div class="input-group-append dropdown" id="recognitionLabelInput">\n' +
                        // '           <button class="btn btn-outline-secondary dropdown-toggle" id="tagpeopledropdown' + metadata.id + '" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                        // '           <div class="dropdown-menu" id="recognitionLabelsList">\n';

                    for (index in recognitionLabels) {
                        const recognitionLabel = recognitionLabels[index];
                        let checkedString = "";

                        if ($.inArray(recognitionLabel.name, taggedPeopleArray) !== -1) {
                            checkedString = " checked";
                        }

                        html += ModalTemplates.PersonModalDropDown({metadata:metadata,recognitionLabel:recognitionLabel,checkedString:checkedString});
                            // '           <button class="dropdown-item" type="button">\n' +
                            // '               <input type="checkbox" class="recognitionLabel" value="' + recognitionLabel.name + '" name="recognitionLabel' + metadata.id + '[]" id="' + metadata.id + '-' + recognitionLabel.id + '"' + checkedString + '>\n' +
                            // '               <label for="' + metadata.id + '-' + recognitionLabel.id + '" id="label-' + metadata.id + '-' + recognitionLabel.id + '">' + Util.escapeHtml(recognitionLabel.name) + '</label>\n' +
                            // '           </button>\n';
                    }
                    html += ModalTemplates.PersonModalDropdownFooter();
                        // '   </div>\n' +
                        // '</div>\n';

                    $(html).insertAfter($("#labelIdData"));
                    $("#tagpeopledropdown" + metadata.id).on("click", function (e) {
                       e.preventDefault();
                       timelineModal.toggleTagPeopleDropdown(metadata.id);
                    });
                    $(".recognitionLabel").on("click", function (e) {
                        timelineModal.populateLabel(metadata.id);
                    });
                }

                let albumListString = "";
                for (index in albumListArray) {
                    const album = albumListArray[index];
                    albumListString += album + ",";
                }

                albumListString = albumListString.replace(/,\s*$/, "");
                albumListString = albumListString.trim();

                if (albumListString !== "") {
                    $("#albumnames").val(albumListString);
                } else if (metadata.albumlist !== null) {
                    $("#albumnames").val(metadata.albumlist);
                }

                if ($("#albumListInput").length > 0) {
                    $("#albumListInput").remove();
                }

                if (allAlbumList !== null && allAlbumList.length > 0) {
                    let html = ModalTemplates.AlbumModalDropdownHeader({metadata:metadata});
                        // '<div class="input-group-append dropdown" id="albumListInput">\n' +
                        // '   <button class="btn btn-outline-secondary dropdown-toggle" id="albumdropdown' + metadata.id + '" type="button" aria-haspopup="true" aria-expanded="false">Albums</button>\n' +
                        // '   <div class="dropdown-menu" id="albumsList">\n';

                    for (index in allAlbumList) {
                        const eachAlbum = allAlbumList[index];
                        let checkedString = "";

                        if ($.inArray(eachAlbum.name, albumListArray) !== -1) {
                            checkedString = " checked";
                        }

                        html += ModalTemplates.AlbumModalDropDown({metadata:metadata,album:eachAlbum,checkedString:checkedString});
                            // '   <button class="dropdown-item" type="button">\n' +
                            // '       <input type="checkbox" class="album" value="' + eachAlbum.name + '" name="album' + metadata.id + '[]" id="' + metadata.id + '-' + eachAlbum.id + '"' + checkedString + '>\n' +
                            // '       <label for="' + metadata.id + '-' + eachAlbum.id + '" id="album-' + metadata.id + '-' + eachAlbum.id + '">' + Util.escapeHtml(eachAlbum.name) + '</label>\n' +
                            // '   </button>\n';
                    }
                    html += ModalTemplates.AlbumModalDropdownFooter();
                        // '</div>\n' +
                        // '</div>\n';

                    $(html).insertAfter($("#albumNameData"))
                    $("#albumdropdown" + metadata.id).on("click", function (e) {
                        e.preventDefault();
                        timelineModal.toggleAlbumDropdown(metadata.id);
                    });
                    $(".album").on("click", function (e) {
                        timelineModal.populateAlbum(metadata.id);
                    });
                }

                if (isObject === true) {
                    $("#isobject")[0].checked = true;
                }

                if (metadata.hidden !== null && metadata.hidden === true) {
                    $("#hidden")[0].checked = true;
                }

                $("#albumDetailRow").remove();
                Util.populateDetailsInfo(metadata, "propTimelineModal");

                const keywordAvailableList = $($("#keywordsString").val().split(",")).not($("#keywords").val().split(",")).get().filter(function (v) {
                    return v !== ''
                });
                shashin.createAutocomplete("#keywords", keywordAvailableList, true, 10);

                const camerasAvailableList = $($("#camerasString").val().split(",")).not($("#camera").val().split(",")).get().filter(function (v) {
                    return v !== ''
                });
                shashin.createAutocomplete("#camera", camerasAvailableList, false);

                const lensesAvailableList = $($("#lensesString").val().split(",")).not($("#lens").val().split(",")).get().filter(function (v) {
                    return v !== ''
                });
                shashin.createAutocomplete("#lens", lensesAvailableList, false);

                // Open modal window
                $("#propTimelineModal").modal('show');
            }
        });
    }

    shashin.createAutocomplete = function(inputEl, source, commaDelimited, resultLimit) {

        $(inputEl).autocomplete({
            minLength: 0,
            source: function (request, response) {
                // delegate back to autocomplete, but extract the last term
                const inputValues = request.term.split(",");
                $.each(inputValues, function(index, keywordItem) {
                    // do something with `item` (or `this` is also `item` if you like)
                    const keywordIndex = source.indexOf(keywordItem.trim());
                    if (keywordIndex !== -1) {
                        source.splice(keywordIndex, 1);
                    }
                });

                let filter = $.ui.autocomplete.filter(
                    source,
                    shashin.autocompleteExtractLast(request.term)
                );

                if (typeof resultLimit !== "undefined" && Number.isInteger(resultLimit)) {
                    filter = filter.slice(0, resultLimit)
                }

                response(filter)
            },
            focus: function () {
                // prevent value inserted on focus
                return false;
            },
            select: function (event, ui) {
                const inputValues = this.value.split(",");
                $.each(inputValues, function(index, keywordItem) {
                    // do something with `item` (or `this` is also `item` if you like)
                    const keywordIndex = source.indexOf(keywordItem.trim());
                    if (keywordIndex !== -1) {
                        source.splice(keywordIndex, 1);
                    }
                });
                const terms = shashin.autocompleteSplit(this.value);
                // remove the current input
                terms.pop();
                // add the selected item
                terms.push(ui.item.value);

                if (true === commaDelimited) {
                    // add placeholder to get the comma-and-space at the end
                    terms.push("");
                    this.value = terms.join(",");
                    this.value = this.value.replace(/,\s*$/, "");
                } else {
                    this.value = terms;
                }
                return false;
            }
        });
    }

    shashin.initLightGallery = function(lgElement,additionalLgConfigs,mediaElement) {
        shashin.setLightGalleryElement(lgElement);
        shashin.setLightGallery(additionalLgConfigs);

        let mediaContentList = [];
        $.each($(mediaElement), function() {
            const mediaContent = {};
            mediaContent.func = shashin.openInfoSidebar;
            mediaContent.args = "";
            try {
                mediaContent.args = $(this).attr("tag");
            } catch(e) {}
            let subHtmlAttr = $(this).attr("data-sub-html");
            if (typeof subHtmlAttr !== 'undefined' && subHtmlAttr !== false) {
                mediaContent.subHtml = subHtmlAttr;
            }
            if ($(this).attr("data-src")) {
                mediaContent.src = $(this).attr("data-src");
                mediaContent.downloadUrl = $(this).attr("data-src")+"/download";
            } else if ($(this).attr("data-video")) {
                mediaContent.video = $(this).attr("data-video");
                mediaContent.downloadUrl = $(this).attr("data-video")+"/download";
            }
            mediaContentList.push(mediaContent);
        });

        shashin.initMediaContent(mediaContentList);

        return mediaContentList;
    }

    shashin.initMediaContent = function(mediaContentList) {
        if (mediaContentList.length > 0 && shashin.getLightGallery() !== null) {
            shashin.refreshAndActivateLgListener(mediaContentList);
        }
    }

    shashin.updateMediaContent = function(mediaContentList,additionalMediaContentList) {
        if (additionalMediaContentList && additionalMediaContentList.length > 0) {
            mediaContentList = mediaContentList.concat(additionalMediaContentList);
            shashin.refreshAndActivateLgListener(mediaContentList);
        }

        return mediaContentList;
    }

    shashin.refreshAndActivateLgListener = function (mediaContentList) {
        if (shashin.getLightGallery() !== null) {
            shashin.getLightGallery().refresh(mediaContentList);
            // shashin.getLightGalleryElement().addEventListener('lgAfterSlide', function (e) {
            //     shashin.jumpToLightGalleryIndex(e.detail.index);
            // })
        }
    }

    shashin.pageLoader = function(func, appendClass, list) {
        const refreshIntervalId = window.setInterval(function () {
            if (!Util.hasScrollBar($("#container")) && !Util.hasScrollBar($("main"))) {
                setTimeout(async () => {
                    const page = await func();
                }, 1000);
            } else {
                clearInterval(refreshIntervalId);
            }

            if ($(appendClass).last().text() === "EOL" || list === '' || list === '[]') {
                clearInterval(refreshIntervalId);
            }
        }, 200);
        $("#container").on('scroll', async function () {
            shashin.showScrollToTop($("#container"));
            if (Util.atEndOfPage(this) && $(appendClass).last().text() !== "EOL") {
                const page = await func();
            }
        })
        $("main").on('scroll', async function () {
            shashin.showScrollToTop($("main"));
            if (Util.atEndOfPage(this) && $(appendClass).last().text() !== "EOL") {
                const page = await func();
            }
        })

        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            scrollToTopButton.on("click",function () {
                $("main")[0].scrollTo({top: 0, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: 0, behavior: 'smooth'});
            });
        }
    }

    shashin.showScrollToTop = function(scrollEl) {
        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            if ((scrollEl[0].scrollTop > 20)) {
                scrollToTopButton.css("display","block");
            } else {
                scrollToTopButton.css("display","none");
            }
        }
    }

    shashin.activateScrollToTop = function() {
        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            $("#container").on('scroll', function () {
                shashin.showScrollToTop($(this));
            });
            $("main").on('scroll', function () {
                shashin.showScrollToTop($(this));
            });

            scrollToTopButton.on("click",function () {
                $("main")[0].scrollTo({top: 0, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: 0, behavior: 'smooth'});
            });
        }
    }

    shashin.openMap = function (metadata) {
        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            $("#map").css("display","block");
            $("#mapTabMessage").css("display","block");
            $("#mapTabMessage").html(TimelineTemplates.MapLinks({metadata:metadata}));

            if (shashin.map === null) {
                shashin.map = new ol.Map({
                    controls: [],
                    layers: [
                        new ol.layer.Tile({
                            visible: true,
                            source: shashin.getMapSource("osm")
                        })
                    ],
                    target: 'map',
                    view: new ol.View({
                        center: ol.proj.fromLonLat([metadata.lng, metadata.lat]),
                        maxZoom: 19,
                        zoom: 19
                    })
                });

                const attributions = new ol.control.Attribution({collapsible: true});
                shashin.map.addControl(attributions);
            }

            if (shashin.layer !== null && shashin.feature !== null) {
                shashin.layer.getSource().clear();
            }

            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
            shashin.map.getView().setZoom(15);

            shashin.feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([metadata.lng, metadata.lat])),
                name: metadata.title
            });

            const iconSize = 20;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-pin-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M4.146.146A.5.5 0 014.5 0h7a.5.5 0 01.5.5c0 .68-.342 1.174-.646 1.479-.126.125-.25.224-.354.298v4.431l.078.048c.203.127.476.314.751.555C12.36 7.775 13 8.527 13 9.5a.5.5 0 01-.5.5h-4v4.5c0 .276-.224 1.5-.5 1.5s-.5-1.224-.5-1.5V10h-4a.5.5 0 01-.5-.5c0-.973.64-1.725 1.17-2.189A5.921 5.921 0 015 6.708V2.277a2.77 2.77 0 01-.354-.298C4.342 1.674 4 1.179 4 .5a.5.5 0 01.146-.354z"/></svg>';
            const styleIcon = new ol.style.Style({
                image: new ol.style.Icon({
                    opacity: 1,
                    src: 'data:image/svg+xml;utf8,' + svg,
                    anchor: [0.5, iconSize],
                    anchorXUnits: 'fraction',
                    anchorYUnits: 'pixels',
                    anchorOrigin: 'top-left',
                    offset: [0, 0]
                })
            });
            shashin.feature.setStyle(styleIcon);
            shashin.layer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: [shashin.feature]
                })
            });
            shashin.map.addLayer(shashin.layer);

            setTimeout(fixContentHeight, 1000);
        } else {
            if (shashin.layer !== null && shashin.feature !== null) {
                shashin.layer.getSource().clear();
            }
            $("#map").css("display","none");
            $("#mapTabMessage > .wrapper").contents().unwrap();
            $("#mapTabMessage").text("No map data");
            $("#mapTabMessage").css("display","block");
        }
    }

    shashin.openInfoModal = function(metadataId) {
        shashin.getMetadata(metadataId).then(function (metadata) {
            $("#infoModalTitle").text(metadata.title);
            $("#currentfilename").val(metadata.fileName);
            $("#currentlat").val(metadata.lat);
            $("#currentlng").val(metadata.lng);
            $("#metadataId").val(metadata.id);

            if (metadata.thumbnailUrlCentered !== null) {
                $("#propInfoModalThumbnail").html(TimelineTemplates.HeaderThumbnail({metadata:metadata}));
            }

            Util.populateDetailsInfo(metadata,"propInfoModal");

            // Open modal window
            $("#propInfoModal").modal('show');
        });
    }

    shashin.openInfoSidebar = function(metadataId) {
        // Populate modal data
        shashin.getMetadata(metadataId).then(function (data) {
            let metadata = data;

            $("#infoSidebarTitle").text(metadata.title);
            $("#currentfilename").val(metadata.fileName)
            $("#currentlat").val(metadata.lat)
            $("#currentlng").val(metadata.lng)
            $("#metadataId").val(metadata.id);

            if (metadata.thumbnailUrlCentered !== null) {
                $("#propInfoSidebarThumbnail").html(TimelineTemplates.HeaderThumbnail({metadata:metadata}));
            }

            Util.populateDetailsInfo(metadata, "propInfoSidebar");

            // Open info sidebar
            $("#propInfoSidebar").css('z-index', 9999);
            const infoSidebar = document.getElementById('propInfoSidebar');
            const bsInfoSidebar = new bootstrap.Offcanvas(infoSidebar);
            bsInfoSidebar.show()
        });
    }

    shashin.addToMetadataThumbnailsList = function(thumbnail) {
        if ($("#multiSelectThumbnails").length > 0) {
            const metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
            if (metadataThumbnailsArray.indexOf(thumbnail) === -1) {
                metadataThumbnailsArray.push(thumbnail);
                $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
            }
        }
    }

    shashin.removeFromMetadataThumbnailsList = function(thumbnail) {
        if ($("#multiSelectThumbnails").length > 0) {
            const metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
            const index = metadataThumbnailsArray.indexOf(thumbnail);
            if (index > -1) {
                metadataThumbnailsArray.splice(index, 1);
            }
            $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
        }
    }

    shashin.getMetadataThumbnailsList = function() {
        if ($("#multiSelectThumbnails").length > 0) {
            return JSON.parse($("#multiSelectThumbnails").val());
        }

        return [];
    }

    shashin.removeFromMetadataFilenamesList = function(filename) {
        if ($("#multiSelectFilenames").length > 0) {
            const metadataFilenamesArray = shashin.getMetadataFilenamesList();
            const index = metadataFilenamesArray.indexOf(filename);
            if (index > -1) {
                metadataFilenamesArray.splice(index, 1);
            }
            $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
        }
    }

    shashin.getMetadataFilenamesList = function() {
        if ($("#multiSelectFilenames").length > 0) {
            return JSON.parse($("#multiSelectFilenames").val());
        }

        return [];
    }

    shashin.addToMetadataFilenamesList = function (filename) {
        if ($("#multiSelectFilenames").length > 0) {
            const metadataFilenamesArray = shashin.getMetadataFilenamesList();
            if (metadataFilenamesArray.indexOf(filename) === -1) {
                metadataFilenamesArray.push(filename);
                $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
            }
        }
    }

    shashin.addToMetadataIdList = function (metadataId) {
        if ($("#multiSelectMetadataIds").length > 0) {
            const metadataIdArray = shashin.getMetdataIdList();
            if (metadataIdArray.indexOf(metadataId) === -1) {
                metadataIdArray.push(metadataId);
                $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
            }
        }
    }

    shashin.removeFromMetadataIdList = function (metadataId) {
        if ($("#multiSelectMetadataIds").length > 0) {
            const metadataIdArray = shashin.getMetdataIdList();
            const index = metadataIdArray.indexOf(metadataId);
            if (index > -1) {
                metadataIdArray.splice(index, 1);
            }
            $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
        }
    }

    shashin.getMetdataIdList = function() {
        if ($("#multiSelectMetadataIds").length > 0) {
            return JSON.parse($("#multiSelectMetadataIds").val());
        }

        return [];
    }

    shashin.downloadSelected = async function (buttonId) {

        let span = null;
        if (typeof buttonId !== 'undefined') {
            span = $("#" + buttonId).find("span");
        }

        if (typeof buttonId === 'undefined' || (span !== null && span.hasClass('bi-download'))) {
            if ((span !== null && span.hasClass('bi-download'))) {
                span.addClass('spinner-grow').removeClass('bi-download');
            }

            let metadataIdList = shashin.getMetdataIdList();
            if (shashin.getMetdataIdList().length === 0) {
                $('.bi-circle-fill').each(function (i, obj) {
                    metadataIdList.push(obj.id.substring(6, obj.id.length));
                });
            }

            const endpoint = "/timeline/download/batch";

            if (Util.isMobile() === false) {
                $.fileDownload(endpoint, {
                    httpMethod: "POST",
                    data: "batchMetadataIds=" + JSON.stringify(metadataIdList),
                    successCallback: function (url) {
                        shashin.printMessageToConsole("Media ZIP download success");
                        shashin.printMessageToConsole(url);

                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }
                    },
                    failCallback: function (html, url) {
                        shashin.printMessageToConsole("Media ZIP download fail");
                        shashin.printMessageToConsole(url);
                        shashin.printMessageToConsole(html);

                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }
                    }
                });
            } else {
                fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: "batchMetadataIds=" + JSON.stringify(metadataIdList)
                })
                    .then(response => response.blob())
                    .then(blob => {
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        const d = new Date();
                        a.download = "shashin_download_"+d.getFullYear()+("0" + (d.getMonth() + 1)).slice(-2)+("0" + d.getDate()).slice(-2)+"_"+("0" + d.getHours()).slice(-2)+d.getMinutes()+("0" + d.getSeconds()).slice(-2)+".zip";
                        document.body.appendChild(a); // we need to append the element to the dom -> otherwise it will not work in firefox
                        a.click();
                        a.remove();  //afterwards we remove the element again
                        shashin.printMessageToConsole("Media ZIP download success using fetch()");
                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }
                    }).catch(() => {
                        shashin.printMessageToConsole("Media ZIP download fail using fetch()");
                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }
                    });
            }
        }
    }

    shashin.removeAllMetadataIdList = function () {
        if ($("#multiSelectMetadataIds").length > 0) {
            $("#multiSelectMetadataIds").val(JSON.stringify([]));
        }
    }

    shashin.removeAllMetadataFilenamesList = function () {
        if ($("#multiSelectFilenames").length > 0) {
            $("#multiSelectFilenames").val(JSON.stringify([]));
        }
    }

    shashin.removeAllMetadataThumbnailsList = function () {
        if ($("#multiSelectThumbnails").length > 0) {
            $("#multiSelectThumbnails").val(JSON.stringify([]));
        }
    }

    shashin.jumpToLightGalleryIndex = function (index) {
        const url = location.href;
        location.href = '#lightGalleryIndex'+index;
        history.replaceState(null,null,url);
    }

    shashin.setLightGalleryElement = function (name) {
        shashin.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            shashin.infiniteScrollGallery = document.getElementById(name);
        }
    };

    shashin.setLightGallery = function (additionalConfigs) {
        let configs = shashin.getLightGalleryConfigs(additionalConfigs);
        shashin.lg = lightGallery(shashin.getLightGalleryElement(), configs);
    }

    shashin.setVideoWidth = function (element) {
        element.addEventListener("lgAfterSlide", function (e) {
            Util.setVideoWidth();
        });

        element.addEventListener("lgAfterOpen", function (e) {
            Util.setVideoWidth();
        });

        element.addEventListener("lgContainerResize", function (e) {
            Util.setVideoWidth();
        });
    }

    shashin.mouseMoveListener = function () {
        // Hide caption when showing lg gallery
        shashin.lgSubHtmlTimeout = null;
        $("html").mousemove(function() {
            shashin.captionListener();
        });
    }

    shashin.captionListener = function () {
        clearTimeout(shashin.lgSubHtmlTimeout);
        $(".lg-sub-html").show('slide', {direction: 'down'}, 200);
        shashin.lgSubHtmlTimeout = setTimeout(function () {
            $(".lg-sub-html").hide('slide', {direction: 'down'}, 200);
        }, 5000);
    }

    shashin.getLightGalleryElement = function () {
        return shashin.infiniteScrollGallery;
    };

    shashin.getLightGallery = function () {
        return shashin.lg;
    }

    shashin.openGallery = function (e, index) {
        e.preventDefault();
        if (shashin.getLightGallery() !== null) {
            shashin.getLightGallery().openGallery(index);
        }
    }

    shashin.getLightGalleryConfigs = function(additionalConfigs) {
        const configs = {
            plugins: [lgZoom, lgVideo, lgRelativeCaption, lgFullscreen, lgRotate],
            videojs: false,
            hideBarsDelay: 5000,
            allowMediaOverlap: true,
            counter: false,
            preload: "auto",
            fullScreen: true,
            download: true,
            zoomFromOrigin: true,
            videoMaxSize: "7680-4320",
            speed: 0,
            autoplayFirstVideo: false,
            gotoNextSlideOnVideoEnd: false,
            rotate: true,
            rotateLeft: true,
            rotateRight: true,
            flipHorizontal: false,
            flipVertical: false,
            licenseKey: Util.lgApiKey()
        }

        for (const key in additionalConfigs) {
            if (key === "plugins") {
                if ($.isArray(additionalConfigs[key])) {
                    $.each(additionalConfigs[key] , function(index, val) {
                        configs["plugins"].push(val);
                    });
                } else {
                    configs["plugins"].push(additionalConfigs[key]);
                }
            } else {
                configs[key] = additionalConfigs[key];
            }
        }

        return configs;
    }

    shashin.getMapSource = function (source) {
        const validSources = ["osm","bingmaps","maptiler","mapbox"];

        let mapSource = new ol.source.OSM();

        if (validSources.includes(source)) {
            switch(source) {
                case "osm":
                    mapSource = new ol.source.OSM();
                    break
                case "bingmaps":
                    mapSource = new ol.source.BingMaps({
                        key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                        imagerySet: 'RoadOnDemand', // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                        // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                        // "no photos at this zoom level" tiles
                        maxZoom: 19
                    });
                    break
                case "maptiler":
                    mapSource =  new ol.source.XYZ({
                        url: 'https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=YlQvLcNKq0a4aFDX2z3O',
                        maxZoom: 19
                    });
                    break;
                case "mapbox":
                    mapSource = new ol.source.XYZ({
                        url: 'https://{a-c}.tiles.mapbox.com/v4/mapbox.mapbox-streets-v6/{z}/{x}/{y}.vector.pbf?access_token=pk.eyJ1IjoibWljaGFlbHR5YWdpIiwiYSI6ImNrdGszZXNrdTFocTcyd29sMG1hYXprdmsifQ.RVimlhqPIKKTYmaGyr-ThQ'
                    });
                    break;
                default:
                    mapSource = new ol.source.OSM();
            }
        }

        return mapSource
    }

    shashin.imageHover = function (_this, metadataId) {
        const metadataIdArray = shashin.getMetdataIdList();
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
    }

    shashin.setPhotoOverlays = function (metadata, view) {
        const opaque = 0.3
        const transparent = 1.0

        let metadataIdArray = shashin.getMetdataIdList();
        shashin.printMessageToConsole(metadataIdArray);
        const index = metadataIdArray.indexOf(metadata.id);
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

            if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                $("#tntl" + metadata.id).show();
                $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
                $("#image" + metadata.id).css("opacity", opaque);
                //$("#tntr" + metadata.id).hide();
                $("#tncentered" + metadata.id).hide();
                $("#tnbr" + metadata.id).hide();
                $("#tnbl" + metadata.id).hide();
                shashin.addToMetadataIdList(metadata.id);
                shashin.addToMetadataFilenamesList($('#filename' + metadata.id).val());
                shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
            } else {
                $("#tntl" + metadata.id).show();
                $("#tlicon" + metadata.id).addClass('bi-circle').removeClass('bi-circle-fill');
                $("#image" + metadata.id).css("opacity", opaque);
                $("#tntr" + metadata.id).show();
                $("#tncentered" + metadata.id).show();
                $("#tnbr" + metadata.id).show();
                $("#tnbl" + metadata.id).show();
                shashin.removeFromMetadataIdList(metadata.id);
                shashin.removeFromMetadataFilenamesList($('#filename' + metadata.id).val());
                shashin.removeFromMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
            }

            metadataIdArray = shashin.getMetdataIdList();

            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $("#appSearch").hide();
                if (view === "album" || view === "favorites" || view === "trash") {
                    $("#albumAppTools").show();
                } else if (view === "timeline" || view === "recent" || view === "modified" || view === "folder") {
                    $("#timelineAppTools").show();
                    if (view === "timeline") {
                        $("#timelineTools").hide();
                    }
                } else if (view === "matches" || view === "person") {
                    $("#matchesAppTools").show();
                }

                // Hide all center and bottom left icons
                $('.thumbnail-br').hide();
                $('.thumbnail-bl').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-centered').hide();
            } else {
                $("#appSearch").show();
                $("#timelineAppTools").hide();
                $("#albumAppTools").hide();
                $("#matchesAppTools").hide();
                if (view === "timeline") {
                    $("#timelineTools").show();
                }
            }

            const metadataList = shashin.getMetdataIdList();
            let timelineSelectCount = $('.bi-circle-fill').length;
            if (metadataList.length > 0) {
                timelineSelectCount = metadataList.length;
            }
            $("#timelineNumberSelected").text(timelineSelectCount+" Selected");
            $("#matchesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#favoritesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#trashNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#albumNumberSelected").text($('.bi-circle-fill').length+" Selected");
        });

        $("#image" + metadata.id).on("click", function (e) {
            e.preventDefault();

            // Fill top left icon when clicking anywhere on thumbnail
            if ($('.bi-circle-fill')[0] || metadataIdArray.length > 0) {
                if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                    $("#tntl" + metadata.id).show();
                    $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
                    $("#image" + metadata.id).css("opacity", opaque);
                    $("#tncentered" + metadata.id).hide();
                    $("#tnbr" + metadata.id).hide();
                    $("#tnbl" + metadata.id).hide();
                    shashin.addToMetadataIdList(metadata.id);
                    shashin.addToMetadataFilenamesList($('#filename' + metadata.id).val());
                    shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
                } else {
                    $("#tntl" + metadata.id).show();
                    $("#tlicon" + metadata.id).addClass('bi-circle').removeClass('bi-circle-fill');
                    $("#image" + metadata.id).css("opacity", opaque);
                    $("#tncentered" + metadata.id).show();
                    $("#tnbr" + metadata.id).show();
                    $("#tnbl" + metadata.id).show();
                    shashin.removeFromMetadataIdList(metadata.id);
                    shashin.removeFromMetadataFilenamesList($('#filename' + metadata.id).val());
                    shashin.removeFromMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
                }
            }

            metadataIdArray = shashin.getMetdataIdList();

            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $("#appSearch").hide();
                if (view === "album" || view === "favorites" || view === "trash") {
                    $("#albumAppTools").show();
                } else if (view === "timeline" || view === "recent" || view === "modified" || view === "folder") {
                    $("#timelineAppTools").show();
                    if (view === "timeline") {
                        $("#timelineTools").hide();
                    }
                } else if (view === "matches" || view === "person") {
                    $("#matchesAppTools").show();
                }

                // Hide all center and bottom left icons
                $('.thumbnail-br').hide();
                $('.thumbnail-bl').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-centered').hide();
            } else {
                $("#appSearch").show();
                //$('.thumbnail-br').show();
                $("#timelineAppTools").hide();
                if (view === "timeline") {
                    $("#timelineTools").show();
                }
                $("#albumAppTools").hide();
                $("#matchesAppTools").hide();
            }

            let timelineSelectCount = $('.bi-circle-fill').length;
            if (metadataIdArray.length > 0) {
                timelineSelectCount = metadataIdArray.length;
            }
            $("#timelineNumberSelected").text(timelineSelectCount+" Selected");
            $("#matchesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#favoritesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#trashNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#albumNumberSelected").text($('.bi-circle-fill').length+" Selected");
        });

        $("#image" + metadata.id).hover(function () {
            // Only show overlays when scrolling stopped in timeline view
            if (view !== "timeline" || (view === "timeline" && timelineSettings && timelineSettings.isScrolling === false)) {
                shashin.imageHover(this, metadata.id);
            }
        }, function () {
            metadataIdArray = shashin.getMetdataIdList();
            const index = metadataIdArray.indexOf(metadata.id);

            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill" && index <= -1) {
                $(this).css("opacity", 1.0);
                $(this).siblings(".thumbnail-tl").hide();
                $(this).siblings(".thumbnail-bl").hide();
                $(this).siblings(".thumbnail-centered").hide();
                //$(this).siblings(".thumbnail-tr").hide();
                $(this).siblings(".thumbnail-br").hide();
            } else {
                if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
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
            metadataIdArray = shashin.getMetdataIdList();

            let hoverColor = "white";
            if (shashin.darkMode === true) {
                hoverColor = "slategray";
            }
            $('.bi-play-circle').css("color", hoverColor);
            $('.bi-play-btn').css("color", hoverColor);
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-bl").show();
            $(this).siblings(".thumbnail-br").show();
            $(this).siblings(".thumbnail-tr").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            $('.bi-play-circle').css("color", "lightgray");
            $('.bi-play-btn').css("color", "lightgray");
            $(this).hide();
            $(this).siblings(".thumbnail-tl").hide();
            $(this).siblings(".thumbnail-bl").hide();
            $(this).siblings(".thumbnail-br").hide();
            //$(this).siblings(".thumbnail-tr").hide();
            $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
        });

        $("#tntl" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            const index = metadataIdArray.indexOf(metadata.id);
            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill" && index <= -1) {
                $(this).show();
                $(this).siblings(".thumbnail-centered").show();
                $(this).siblings(".thumbnail-tr").show();
                $(this).siblings(".thumbnail-br").show();
                $(this).siblings(".thumbnail-bl").show();
                $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
                if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
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
            metadataIdArray = shashin.getMetdataIdList();
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-centered").show();
            $(this).siblings(".thumbnail-tr").show();
            $(this).siblings(".thumbnail-br").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
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
            metadataIdArray = shashin.getMetdataIdList();
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-centered").show();
            $(this).siblings(".thumbnail-tr").show();
            $(this).siblings(".thumbnail-bl").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
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
            metadataIdArray = shashin.getMetdataIdList();
            $(this).show();
            $(this).siblings(".thumbnail-tl").show();
            $(this).siblings(".thumbnail-centered").show();
            $(this).siblings(".thumbnail-bl").show();
            $(this).siblings(".thumbnail-br").show();
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
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
    }

    shashin.getOverlayData = function(metadata, args) {
        const overlays = [];
        const data = {};

        data["metadata"] = metadata;

        const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
        if (metadata.type.includes("video")) {
            overlays.push("isVideo");
            data["duration"] = duration;
        } else if (metadata.width !== null && metadata.height !== null && metadata.width > metadata.height*2) {
            overlays.push("isPan");
        }

        if (typeof args !== "undefined") {
            if (args.hasOwnProperty("overlayFlags")) {
                data["overlayFlags"] = args.overlayFlags;
            }

            if (args.hasOwnProperty("galleryIndex")) {
                data["galleryIndex"] = args.galleryIndex;
            }

            if (args.hasOwnProperty("labelPhotoMap")) {
                const labelPhotoMap = args.labelPhotoMap;
                if (labelPhotoMap.hasOwnProperty(metadata.id) === true && labelPhotoMap[metadata.id].hasOwnProperty("isTagged") === true && labelPhotoMap[metadata.id]["isTagged"] === true) {
                    overlays.push("isTagged");
                }
            }

            if (args.hasOwnProperty("editControls") && args["editControls"] === true) {
                overlays.push("isEditControls");
            } else {
                overlays.push("isInfo");
            }

            if (args.hasOwnProperty("editIcon")) {
                data["editIcon"] = args["editIcon"];
            }

            if (args.hasOwnProperty("blOnClickFunction") && args.hasOwnProperty("onClickIdPrefix")) {
                overlays.push("isBlOnClickFunction");
                data["blOnClickFunction"] = args["blOnClickFunction"];
                data["onClickIdPrefix"] = args["onClickIdPrefix"];
            } else if (args.hasOwnProperty("onClickIdPrefix")) {
                overlays.push("isOnClickIdPrefix");
                data["onClickIdPrefix"] = args["onClickIdPrefix"];
            } else if (args.hasOwnProperty("blOnClickFunction")) {
                data["blOnClickFunction"] = args["blOnClickFunction"];
            }

            if (args.hasOwnProperty("cOnClickFunction")) {
                data["cOnClickFunction"] = args["cOnClickFunction"];
            }

            if (args.hasOwnProperty("favoriteCount")) {
                overlays.push("isFavorites");
                data["favoriteCount"] = args["favoriteCount"];
                data["favoriteIcon"] = args["favoriteIcon"];
            }

            if (args.hasOwnProperty("albumPhotoCommentsMap")) {
                overlays.push("isComments");
                data["albumPhotoCommentsMap"] = args["albumPhotoCommentsMap"];
                data["notificationMap"] = args["notificationMap"];
            }
        } else {
            overlays.push("isInfo");
        }

        return {overlays:overlays,data:data};
    }

    shashin.clearTimelineSelection = function () {
        shashin.removeAllMetadataFilenamesList();
        shashin.removeAllMetadataThumbnailsList();
        shashin.removeAllMetadataIdList();
        $(".thumbnail-centered").hide();
        //$(".thumbnail-tr").hide();
        $(".thumbnail-br").hide();
        $(".thumbnail-bl").hide();
        $(".thumbnail-tl").hide();
        $(".photo-thumbnail-image").css("opacity", 1.0);
        $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');

        $("#appSearch").show();
        $("#timelineAppTools").hide();
        $("#timelineTools").show();
        $("#albumAppTools").hide();
        $("#matchesAppTools").hide();
    }

    shashin.matchingListeners = function () {
        $("#matchToolsDeselectAll").on("click", function(e) {
            e.preventDefault();

            $(".thumbnail-centered").hide();
            //$(".thumbnail-tr").hide();
            $(".thumbnail-br").hide();
            $(".thumbnail-bl").hide();
            $(".thumbnail-tl").hide();
            $(".photo-thumbnail-image").css("opacity", 1.0);
            $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');

            $("#appSearch").show();
            $("#timelineAppTools").hide();
            $("#timelineTools").show();
            $("#albumAppTools").hide();
            $("#matchesAppTools").hide();
        })

        $("#matchesAppTools").hide();

        $("#matchToolsBatchEdit").on("click", function(e) {
            e.preventDefault();

            let metadataIdList = [];
            let thumbnailList = "";
            $('.bi-circle-fill').each(function(i, obj) {
                const metadataId = obj.id.substring(6, obj.id.length);
                metadataIdList.push(metadataId);
                thumbnailList += TimelineTemplates.BatchHeaderThumbnail({thumbnailImage:$("#thumbnailCentered"+metadataId).val(),title:$("#filename"+metadataId).val().trim()});
            });

            $("#batchMetadataIds").val(JSON.stringify(metadataIdList));
            if (thumbnailList !== "") {
                $("#editPhotosNamesModalLabel").html(thumbnailList);
            }

            const keywordAvailableList = $("#keywordsBatchString").length > 0 ? $("#keywordsBatchString").val().split(",") : [];
            shashin.createAutocomplete("#keywordsBatchData", keywordAvailableList, true, 10);

            $("#propBatchMetadata").modal('show');
        });
    }

    // Call in console
    shashin.enableDebug = function () {
        shashin.showDebug = true;
    }

    // Call in console
    shashin.disableDebug = function () {
        shashin.showDebug = false;
    }

    shashin.printMessageToConsole = function (msg) {
        if (shashin.showDebug === true) {
            console.log(msg);
        }
    }

    shashin.removeThumbnail = function(metadataId) {
        let dateGalleryRemoved = false;
        const targetElement = $("#photoThumbnailContainer" + metadataId);

        const rowId = targetElement.parent().attr("id");
        const sectionId = $(targetElement.siblings("section")[0]).attr("id")
        const headingId = typeof rowId !== "undefined" ? rowId.replace("row", "") : sectionId;

        // Count children
        const currentNumChildren = targetElement.siblings("div").length;

        // Remove metadata
        targetElement.remove();

        if (currentNumChildren === 0 && headingId && headingId.length > 0) {
            Util.removeDateGallery(headingId);
            dateGalleryRemoved = true;
        }

        return dateGalleryRemoved;
    }

    shashin.autocompleteSplit = function(val) {
        return val.split(/,\s*/);
    }

    shashin.autocompleteExtractLast = function(term) {
        return shashin.autocompleteSplit(term).pop();
    }

    shashin.processAlbumList = function(data) {
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
                $(".album").on("click", function (e) {
                    timelineBatchModal.populateBatchAlbum();
                });

                $("#tagalbumdropdown").on("click", function (e) {
                    e.preventDefault();
                    timelineBatchModal.toggleBatchTagAlbumDropdown();
                });
            }
        }
    }

    shashin.processPeopleList = function(data) {
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

                if (recognitionLabel.name !== null && recognitionLabel.name !== "null") {
                    batchHtml +=
                        '           <button class="dropdown-item" type="button">\n' +
                        '               <input type="checkbox" class="recognitionLabel" id="' + recognitionLabel.id + '" value="' + recognitionLabel.name + '" name="recognitionLabel[]">\n' +
                        '               <label for="' + recognitionLabel.id + '">' + recognitionLabel.name + '</label>\n' +
                        '           </button>'
                }
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
            }
        }
    }
}( window.shashin = window.shashin || {}, jQuery ));

if (typeof module !== 'undefined') {
    module.exports = window.shashin;
}