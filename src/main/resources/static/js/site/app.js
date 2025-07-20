(function( shashin, $, undefined ) {
    function fixContentHeight() {
        if ($("div[data-role='dialog']").is(":visible")) {
            const dialog = $("div[data-role='dialog']:visible:visible");
            const contentHeight = 400;
            dialog.height(contentHeight);
            shashin.map.updateSize();
        }
    }

    shashin.updateSearchInput = function(title) {
        $("#appSearchInput").val(title);
        $("#appSearchInput").on('blur', function() {
            if ($(this).val().length === 0) {
                $("#appSearchInput").val(title);
            }
        });
    };

    /*
    options:
        icon = bootstrap icon
        iconColor = CSS color
        headerSubtext = string, sits beside the title in smaller print
        delay = in ms
        autohide = boolean
        placement = one of shashin.toast.placement.*
        borderColor = one of primary, secondary, success, danger, warning, info, light, dark, white
        tag = string, identifies and labels the toast
        refreshTag = if tag is already defined, overwrites the tag with updated content
        closeButton = boolean
    */
    shashin.showToastMessage = function(title, message, options) {
        shashin.printMessageToConsole(title, {tag: "toast"});
        shashin.printMessageToConsole(JSON.stringify(options), {tag: "toast"});

        const createToast = function (index, placement, tag, title, closeButton) {

            let html = '<div id="'+placement+'_ToastTarget_'+index+'" class="toast" role="alert" aria-live="assertive" aria-atomic="true">';
            if (tag !== null) {
                html = '<div id="'+placement+'_ToastTarget_'+index+'" data-tag="'+tag+'" class="toast" role="alert" aria-live="assertive" aria-atomic="true">';
            }

            if (title !== null && title !== "") {
                html += '<div class="toast-header">' +
                    '<span id="' + placement + '_ToastIcon_' + index + '" class="toast-icon"></span><span id="' + placement + '_ToastSpacer_' + index + '"></span>' +
                    '<strong id="' + placement + '_ToastTitle_' + index + '" class="me-auto toast-title"></strong>' +
                    '<small id="' + placement + '_HeaderSubtext_' + index + '" class="toast-subtext"></small>';
                if (closeButton === true) {
                    html += '<button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>';
                }
                html += '</div>';
            }

            if (title === null || title === "") {
                html += '<div class="d-flex">';
            }

            html += '<div id="'+placement+'_ToastMessage_'+index+'" class="toast-body"></div>';

            if (closeButton === true && (title === null || title === "")) {
                html += '<button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>';
            }

            html += '</div>';

            $(html).insertBefore($("#" + placement + "_ToastTargetAttach"));
        };

        let icon = null;
        let iconColor = "lightgray";
        let headerSubtext = null;
        let borderColor = null;
        let placement = shashin.toast.placement.bottom.center;
        let autohide = true;
        let delay = 5000;
        let tag = null;
        let refreshTag = null;
        let closeButton = true;

        if (options === undefined || options === null) {
            placement = shashin.toast.placement.bottom.center;
        } else {
            const validOptions = [];

            if (options.hasOwnProperty("autohide")) {
                autohide = options.autohide;
                validOptions.push("autohide");
            }

            if (options.hasOwnProperty("delay")) {
                delay = options.delay;
                validOptions.push("delay");
            }

            if (options.hasOwnProperty("placement")) {
                placement = options.placement;
                validOptions.push("placement");
            }

            if (options.hasOwnProperty("headerSubtext")) {
                headerSubtext = options.headerSubtext;
                validOptions.push("headerSubtext");
            }

            if (options.hasOwnProperty("borderColor")) {
                borderColor = options.borderColor;
                validOptions.push("borderColor");
            }

            if (options.hasOwnProperty("iconColor")) {
                iconColor = options.iconColor;
                validOptions.push("iconColor");
            }

            if (options.hasOwnProperty("icon")) {
                icon = options.icon;
                validOptions.push("icon");
            }

            if (options.hasOwnProperty("tag")) {
                tag = options.tag;
                validOptions.push("tag");
            }

            if (options.hasOwnProperty("refreshTag")) {
                refreshTag = options.refreshTag;
                validOptions.push("refreshTag");
            }

            if (options.hasOwnProperty("closeButton")) {
                closeButton = options.closeButton;
                validOptions.push("closeButton");
            }

            const invalidOptions = [];
            for (let key in options) {
                if (options.hasOwnProperty(key) === false || (options.hasOwnProperty(key) === true && validOptions.includes(key) === false)) {
                    invalidOptions.push(key);
                }
            }
            if (invalidOptions.length > 0) {
                shashin.printMessageToConsole("Invalid toast options: " + invalidOptions.join(), {tag: "toast"});
            }
        }

        const container = placement+"ToastContainer";
        const attachPoint = $("#" + container).find(".attachPoint");
        const siblingCount = attachPoint.siblings().length;

        let toastId = "";
        if (siblingCount === 0) {
            toastId = container.slice(0, container.indexOf("ToastContainer")) + "_ToastTarget_1";
        }

        const previousSibling = attachPoint.prev();
        const lastToast = previousSibling.attr("id");
        const lastToastArray = toastId !== "" ? toastId.split("_") : lastToast.split("_");

        if (lastToastArray.length === 3 && placement === lastToastArray[0]) {
            const target = lastToastArray[1];
            const lastIteration = lastToastArray[2];

            if ($.isNumeric(lastIteration)) {

                let nextIteration = parseInt(lastIteration);
                nextIteration = nextIteration + 1;

                let toastId = placement + "_" + target + "_" + nextIteration;

                let attached = false;

                // Test if closed - use it, otherwise create new after last open
                if (tag !== null) {
                    // check if tag exists in the target placement, exit if exists to prevent flashing
                    if (shashin.hasToast(placement, {tag: tag}) === true) {
                        if (refreshTag === true) {
                            shashin.removeElements($("#" + placement + "_ToastTargetAttach").siblings(), tag);
                        } else {
                            return true;
                        }
                    }

                    if (shashin.hasToast(placement, {tag: tag}) === false) {
                        createToast(nextIteration, placement, tag, title, closeButton);

                        const attr = $("#" + toastId).attr('data-tag');

                        if (typeof attr !== 'undefined' && attr !== false && $("#" + toastId).attr('data-tag') === tag) { // && $("#" + toastId).hasClass('show') === false
                            attached = true;
                        }
                    }
                } else if ($("#" + toastId).length === 0 || ($("#" + toastId).length > 0 && $("#" + toastId).hasClass('in') === false && $("#" + toastId).hasClass('show') === false)) {
                    createToast(nextIteration, placement, tag, title, closeButton);
                    attached = true;
                }

                if (attached === true) {
                    const messageId = placement + "_ToastMessage_" + nextIteration;
                    $("#" + messageId).html(message);
                    const titleId = placement + "_ToastTitle_" + nextIteration;
                    $("#" + titleId).html(title);

                    $("#" + toastId).removeClass(function (index, className) {
                        return (className.match(/(^|\s)border-\S+/g) || []).join(' ');
                    });
                    $("#" + toastId).removeClass("border");
                    if (borderColor === "primary" ||
                        borderColor === "secondary" ||
                        borderColor === "success" ||
                        borderColor === "danger" ||
                        borderColor === "warning" ||
                        borderColor === "info" ||
                        borderColor === "light" ||
                        borderColor === "dark" ||
                        borderColor === "white") {
                        $("#" + toastId).addClass("border border-" + borderColor);
                    }

                    if (headerSubtext !== null) {
                        const headerSubtextId = placement + "_HeaderSubtext_" + nextIteration;
                        $("#" + headerSubtextId).html(headerSubtext);
                    }

                    if (autohide === false || autohide === true) {
                        $("#" + toastId).attr("data-bs-autohide", autohide);

                        if (autohide === true) {
                            $("#" + toastId).attr("data-bs-delay", delay);
                        }
                    } else {
                        $("#" + toastId).attr("data-bs-delay", delay);
                    }

                    if (icon !== undefined && icon !== null) {
                        const iconEl = placement + "_ToastIcon_" + nextIteration;
                        const iconField = $("#" + iconEl);
                        const spacerEl = placement + "_ToastSpacer_" + nextIteration;
                        const spacerField = $("#" + spacerEl);
                        let cssStyle = {"font-size": "1rem"};
                        if (iconColor !== null) {
                            cssStyle.color = iconColor;
                        }
                        iconField.css(cssStyle);
                        iconField.addClass(icon);
                        spacerField.html("&nbsp;");
                    }

                    const toastLive = document.getElementById(toastId);
                    const toast = new bootstrap.Toast(toastLive);
                    toast.show();

                    toastLive.addEventListener('hidden.bs.toast', () => {
                        $("#" + toastId).remove();
                    });
                }
            }
        }
    };

    function hideRemoveElements(elements, tag, remove) {
        elements.each( function () {
            const attr = $(this).attr('data-tag');
            if (tag === null || (typeof attr !== 'undefined' && attr !== false && attr === tag)) {
                if (remove === true) {
                    $(this).remove();
                } else {
                    $(this).hide();
                }
            }
        });
    }

    shashin.removeElements = function (elements, tag) {
        hideRemoveElements(elements, tag, true);
    };

    shashin.hideElements = function (elements, tag) {
        hideRemoveElements(elements, tag, false);
    };

    // placement - topLeft, topCenter, etc
    shashin.closeToastMessages = function (options) {
        shashin.printMessageToConsole(JSON.stringify(options), {tag: "toast"});
        let tags = [];
        if (options && options.hasOwnProperty("tags")) {
            tags = options.tags;
        }
        if (options && options.hasOwnProperty("tag")) {
            tags.push(options.tag);
        }
        if (tags.length === 0) {
            tags = [null];
        }

        let placements = [];

        const validPlacements = [
            shashin.toast.placement.default,
            shashin.toast.placement.top.left,
            shashin.toast.placement.top.center,
            shashin.toast.placement.top.right,
            shashin.toast.placement.middle.left,
            shashin.toast.placement.middle.center,
            shashin.toast.placement.middle.right,
            shashin.toast.placement.bottom.left,
            shashin.toast.placement.bottom.center,
            shashin.toast.placement.bottom.right
        ];
        if (options && options.hasOwnProperty("placements") && options.placements.length > 0) {
            placements = options.placements;
        }
        if (options && options.hasOwnProperty("placement")) {
            placements.push(options.placement);
        }
        if (placements.length === 0) {
            placements = validPlacements;
        }

        let hide = false;
        if (options && options.hasOwnProperty("hide")) {
            hide = options.hide;
        }

        if (Array.isArray(tags) && Array.isArray(placements)) {
            let invalidPlacements = [];
            placements.forEach(function (placement, index) {
                if (validPlacements.includes(placement)) {
                    tags.forEach(function (tag, index) {
                        if (hide === true) {
                            shashin.hideElements($("#" + placement + "_ToastTargetAttach").siblings(), tag);
                        } else {
                            shashin.removeElements($("#" + placement + "_ToastTargetAttach").siblings(), tag);
                        }
                    });
                } else {
                    invalidPlacements.push(placement);
                }
            });

            if (invalidPlacements.length > 0) {
                shashin.printMessageToConsole("Invalid placements detected: " + invalidPlacements.join(","), {tag: "toast"});
            }
        } else {
            shashin.printMessageToConsole("Tags or placement are not arrays", {tag: "toast"});
        }
    };

    shashin.hasToast = function (placement, options) {
        let tag = null;
        let findHidden = false;

        if (options && options.hasOwnProperty("tag")) {
            tag = options.tag;
        }
        if (options && options.hasOwnProperty("findHidden")) {
            findHidden = options.findHidden;
        }

        let counter = 0;
        let foundTag = false;
        $("#"+placement+"ToastContainer div.toast-container").children().each(function(i, obj) {
            if ((findHidden === true ||
                (findHidden === false &&
                    (typeof $(obj).attr('style') === 'undefined' || $(obj).attr('style') === false) || $(obj).css('display') === "block")) && $(obj).hasClass("attachPoint") === false
            ) {
                if (tag !== null && $(obj).attr("data-tag") === tag) {
                    foundTag = true;
                    return foundTag;
                }
                counter++;
            }
        });

        return foundTag || (tag === null && counter > 0);
    };

    shashin.getToastElement = function (placement, tag) {
        let foundObj = null;

        if (placement === null || tag === null) {
            return null;
        }

        $("#"+placement+"ToastContainer div.toast-container").children().each(function(i, obj) {
            if ($(obj).hasClass("attachPoint") === false) {
                if (tag !== null && $(obj).attr("data-tag") === tag) {
                    foundObj = obj;
                    return true;
                }
            }
        });

        return foundObj;
    };

    shashin.getToastElements = function (placement) {
        let foundObjs = null;

        if (placement === null) {
            return null;
        }

        $("#"+placement+"ToastContainer div.toast-container").children().each(function(i, obj) {
            if ($(obj).hasClass("attachPoint") === false) {
                foundObjs = obj;
            }
        });

        return foundObjs;
    };

    shashin.getMediaContent = function(metadata) {
        const mediaContent = {};

        mediaContent.metadataDetailFun = shashin.openEditMetadataModal;
        mediaContent.videoThumbnailFun = shashin.processVideoThumbnail;
        mediaContent.args = metadata.id;
        mediaContent.metadataId = metadata.id;

        if (metadata.type.includes("video")) {
            mediaContent.video = {
                "source": [{"src": metadata.videoUrl, "type": "video/mp4"}],
                "attributes": {
                    "preload": "auto",
                    "controls": true,
                    "autoplay": shashin.autoplayVideo
                }
            };
            mediaContent.lgSize = metadata.originalImageWidth+"-"+metadata.originalImageHeight;
            mediaContent.poster = ((null === metadata.thumbnailUrlOriginal || "" === metadata.thumbnailUrlOriginal) ? "/api/v1/thumbnails/225/"+metadata.id : "/api/v1/thumbnails/original/"+metadata.id) + "?v=" + Util.getMetadataLocalStorage();
            mediaContent.downloadUrl = encodeURI(metadata.videoUrl).replace(";", "%3B") + "/download";
        } else {
            mediaContent.src = "/api/v1/image/"+metadata.id;
            mediaContent.downloadUrl = "/api/v1/image/"+metadata.id + "/download";
        }

        if (metadata.description !== null && metadata.description !== "") {
            mediaContent.subHtml = metadata.description;
        }

        return mediaContent;
    };

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
                $(countPrefix + metadataId).text(data.count);
            }
        });
    };

    shashin.modalStatusFailMessage = function() {
        return "Something went wrong. Please try again.";
    };

    shashin.onFail = function(xhr, textStatus, ajaxParams, description, failFunction) {
        $("#spinner").hide();
        shashin.showToastMessage("AJAX error", "AJAX error"+description+". Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".", {tag:"ajaxError",icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
        shashin.printMessageToConsole("AJAX error"+description+". Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".", {
            consoleType: shashin.consoleTypes.error,
            tag:"http"
        });
        if (xhr.status === 403 || xhr.status === 401) {
            $(location).prop('href', '/users/login');
        } else if ((textStatus === 'timeout' || textStatus === 'error') && ajaxParams.retries-- > 0) {
            $.ajax(ajaxParams).fail(function (xhr, textStatus) {
                shashin.onFail(xhr, textStatus, ajaxParams, description, failFunction);
            });
        } else if (xhr.status !== 200 && ajaxParams.retries-- > 0) {
            $.ajax(ajaxParams).fail(function (xhr, textStatus) {
                shashin.onFail(xhr, textStatus, ajaxParams, description, failFunction);
            });
        } else if (typeof failFunction !== "undefined" && typeof failFunction === "function") {
            failFunction();
        }
    };

    shashin.checkMetadata = function(metadataId) {
        let metadata = {};

        if ($("#infoModalEdit"+metadataId).attr("tag") && $("#infoModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#infoModalEdit"+metadataId).attr("tag"));
        }

        if  ($("#mediaLink"+metadataId).attr("tag") && $("#mediaLink"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#mediaLink"+metadataId).attr("tag"));
        }

        if  ($("#metadataModalEdit"+metadataId).attr("tag") && $("#metadataModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#metadataModalEdit"+metadataId).attr("tag"));
        }

        return metadata;
    };

    // Get metadata with albums,tagged people and keywords
    shashin.getCompleteMetadata = async function(metadataId) {
        const http = new Http("get timeline metadata");
        const data = await http.ajax("get", "/complete/metadata/"+metadataId+"?v="+uuidv4());

        shashin.printMessageToConsole("shashin.getCompleteMetadata",{tag:"metadata"});
        shashin.printMessageToConsole(JSON.stringify(data),{tag: "metadata"});

        let ret = {};

        if (data.hasOwnProperty("metadata")) {
            ret = data;
        }

        return ret;
    };

    // Get just the metadata with all keywords and albums
    shashin.getMetadata = async function(metadataId) {
        const http = new Http("get metadata");
        const data = await http.ajax("get", "/metadata/"+metadataId+"?v="+uuidv4());

        shashin.printMessageToConsole("shashin.getMetadata");
        shashin.printMessageToConsole(JSON.stringify(data),{tag: "metadata"});

        let metadata = {};
        metadata.keywords = [];
        metadata.albumMap = {};
        metadata.lastAccessedByDetails = "";
        metadata.uploadedByDetails = "";
        metadata.baseUrl = "";

        if (data.hasOwnProperty("metadata") && data.hasOwnProperty("keywordList") && data.hasOwnProperty("albumMap") && data.hasOwnProperty("lastAccessedByDetails") && data.hasOwnProperty("uploadedByDetails") && data.hasOwnProperty("baseUrl")) {
            metadata = data.metadata;
            metadata.keywords = data.keywordList;
            metadata.albumMap = data.albumMap;
            metadata.lastAccessedByDetails = data.lastAccessedByDetails;
            metadata.uploadedByDetails = data.uploadedByDetails;
            metadata.baseUrl = data.baseUrl;
        }

        return metadata;
    };

    shashin.openEditMetadataModal = function (metadataId) {
        shashin.showToastMessage("Opening Media Info", "Opening Info", {
            placement:shashin.toast.placement.middle.center,
            icon:"bi-info-circle",
            iconColor:"#777777",
            autohide:false,
            tag:"metadatamodal"
        });

        shashin.getCompleteMetadata(metadataId).then(async function (data) {
            if (data.hasOwnProperty("metadata") &&
                data.hasOwnProperty("taggedPeopleList") &&
                data.hasOwnProperty("keywordList") &&
                data.hasOwnProperty("allRecognitionLabels") &&
                data.hasOwnProperty("allAlbumList") &&
                data.hasOwnProperty("albumMap")
            ) {
                const metadata = data.metadata;

                const taggedPeopleArray = data.taggedPeopleList;

                let keywordList = data.keywordList;
                const keywordInArray = $.inArray("unidentified objects", keywordList);
                if (keywordInArray !== -1) {
                    keywordList.splice(keywordInArray, 1);
                }
                metadata.keywords = keywordList;
                const albumMap = data.albumMap;
                metadata.albumMap = albumMap;

                const recognitionLabels = data.allRecognitionLabels;
                const allAlbumList = data.allAlbumList;
                let index;

                let keywordsAvailable = "";
                if ($('#keywordsString').length > 0) {
                    keywordsAvailable = $('#keywordsString').val();
                    let keywordArr = keywordsAvailable.split(",");
                    const keywordInArr = $.inArray("unidentified objects", keywordArr);
                    if (keywordInArr !== -1) {
                        keywordList.splice(keywordInArr, 1);
                    }
                    keywordsAvailable = keywordArr.join(",");
                }

                const camerasList = $('#camerasString').val();
                const lensList = $('#lensesString').val();

                // Clear modal data
                $('#propMetadata').find(':input').val('');
                $("#propMetadataModalThumbnail").html("");
                if ($("#isobject").length > 0) {
                    $("#isobject")[0].checked = false;
                }
                if ($("#hidden").length > 0) {
                    $("#hidden")[0].checked = false;
                }

                $("#saveTimelineModalForm :input").prop("disabled", false);
                if ($("#rescan").length > 0) {
                    $("#rescan")[0].checked = false;
                }

                $("#metadataId").val(metadata.id);
                $("#albumList").val("");
                $("#peopleList").val("");
                $("#metadataModalTitle").text(metadata.title);
                $("#currentfilename").val(metadata.fileName);
                $("#currentlat").val(metadata.lat);
                $("#currentlng").val(metadata.lng);
                $("#keywordsString").val(keywordsAvailable);
                $("#camerasString").val(camerasList);
                $("#lensesString").val(lensList);
                $("#videoduration").css("display","none");

                if (metadata.thumbnailUrlCentered !== null) {
                    $("#propMetadataModalThumbnail").html(TimelineTemplates.HeaderThumbnail({metadata: metadata, version: Util.getMetadataLocalStorage(), showMap: false}));
                }

                // put place name beside lat lng
                $("#shortLocationLabel").html("");
                $("#shortLocationLabel").attr("title", "");
                if (metadata.hasOwnProperty("placeName") && metadata.placeName !== null && metadata.placeName !== "" &&
                    data.hasOwnProperty("shortPlaceName") && data.shortPlaceName !== null && data.shortPlaceName !== "") {
                    const fullPlacenameArray = metadata.placeName.split(";");
                    $("#shortLocationLabel").html(data.shortPlaceName);
                    $("#shortLocationLabel").attr("title", fullPlacenameArray[0]);
                }

                if (metadata.title !== null) {
                    $("#title").val(metadata.title);
                }

                if (metadata.description !== null) {
                    $("#description").val(metadata.description);
                    $("#descriptionCharacterCount").text(500-metadata.description.length);
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
                    metadata.keywords = keywordList;
                }

                if (metadata.type.indexOf("video") >= 0) {
                    $("#videoduration").css("display","block");
                    let duration = metadata.duration;
                    if (duration === "" || duration === null) {
                        duration = "0:00";
                    }
                    $("#duration").val(duration);
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

                if (metadata.placeName !== null) {
                    const placeNameArr = metadata.placeName.split(";");
                    let placeName = metadata.placeName;
                    let placeType = "";
                    if (placeNameArr.length === 2) {
                        placeName = placeNameArr[0].trim();
                        placeType = placeNameArr[1].trim();
                    }
                    $("#placeName").val(placeName);
                    $("#placeType").val(placeType);
                    $("#placeName").prop('disabled', false);
                } else {
                    $("#placeName").prop('disabled', true);
                }

                $("#latlng").on('focus', function() {
                    if ($(this).val().length === 0) {
                        $("#placeName").prop('disabled', true);
                    } else {
                        $("#placeName").prop('disabled', false);
                    }
                });

                $("#latlng").on('blur', function() {
                    if ($(this).val().length === 0) {
                        $("#placeName").prop('disabled', true);
                    } else {
                        $("#placeName").prop('disabled', false);
                    }
                });

                const latlngValue = (metadata.hasOwnProperty("lat") && metadata.hasOwnProperty("lng") && metadata.lat != null && metadata.lng != null && metadata.lat !== "" && metadata.lng !== "") ? ($.trim(metadata.lat) + ',' + $.trim(metadata.lng)) : '';
                $("#latlng").val(latlngValue);
                $("#mapTabNav").show();
                if (latlngValue === "" && $("#generalTabNav").length === 0) {
                    $("#mapTabNav").hide();
                } else {
                    $("#placeName").prop('disabled', false);
                }

                let taggedPeopleString = "";
                let isObject = false;
                for (index in taggedPeopleArray) {
                    const person = taggedPeopleArray[index];
                    if (person === shashin.objectName) {
                        isObject = true;
                    } else {
                        taggedPeopleString += person + ",";
                    }
                }
                taggedPeopleString = taggedPeopleString.replace(/,\s*$/, "");
                taggedPeopleString = taggedPeopleString.trim();

                if (isObject === true) {
                    $("#tagpeople").val();
                    $("#peopleList").val();
                    $("#isobject").prop("checked", true);
                } else if (taggedPeopleString !== "") {
                    $("#tagpeople").val(taggedPeopleString);
                    $("#peopleList").val(taggedPeopleString);
                } else if (metadata.tagpeople !== null) {
                    $("#tagpeople").val(metadata.tagpeople);
                    $("#peopleList").val(metadata.tagpeople);
                }

                if ($("#recognitionLabelInput").length > 0) {
                    $("#recognitionLabelInput").remove();
                }
                if (recognitionLabels !== null && recognitionLabels.length > 0) {
                    let html = "";
                    const recognitionLabelNames = [];

                    for (index in recognitionLabels) {
                        const recognitionLabel = recognitionLabels[index];
                        let checkedString = "";

                        if ($.inArray(recognitionLabel.name, taggedPeopleArray) !== -1) {
                            checkedString = " checked";
                        }

                        html += ModalTemplates.PersonModalDropDown({
                            metadata: metadata,
                            recognitionLabel: recognitionLabel,
                            checkedString: checkedString
                        });

                        recognitionLabelNames.push(recognitionLabel.name);
                    }

                    if (recognitionLabelNames.length > 0) {
                        $("#peopleNameData").css("display", "block");
                    } else {
                        $("#peopleNameData").css("display", "none");
                    }
                    $("#peopleNameData").on("click", function (e) {
                        e.preventDefault();

                        shashin.createModalMultiselect(metadata.id, "people", html);

                        // metadataModal.toggleTagPeopleDropdown(metadata.id);
                    });
                    $(".recognitionLabel").on("click", function (e) {
                        metadataModal.populateLabel(metadata.id);
                    });

                    shashin.createAutocomplete("#tagpeople", recognitionLabelNames, false);
                    shashin.syncCheckboxInputs("#tagpeople", "recognitionLabel"+metadataId);
                }

                const albumListArray = [];
                let albumListString = "";
                $.each(albumMap , function( key, value ) {
                    albumListString += value + ",";
                    albumListArray.push(value);
                });

                albumListString = albumListString.replace(/,\s*$/, "");
                albumListString = albumListString.trim();

                if (albumListString !== "") {
                    $("#albumnames").val(albumListString);
                    $("#albumList").val(albumListString);
                } else if (metadata.albumlist !== null) {
                    $("#albumnames").val(metadata.albumlist);
                    $("#albumList").val(metadata.albumlist);
                }

                if ($("#albumListInput").length > 0) {
                    $("#albumListInput").remove();
                }

                // Create dropdown checkboxes
                if (allAlbumList !== null && allAlbumList.length > 0) {
                    let html = "";
                    const albumNames = [];

                    for (index in allAlbumList) {
                        const eachAlbum = allAlbumList[index];
                        let checkedString = "";

                        if ($.inArray(eachAlbum.name, albumListArray) !== -1) {
                            checkedString = " checked";
                        }

                        html += ModalTemplates.AlbumModalDropDown({
                            metadata: metadata,
                            album: eachAlbum,
                            checkedString: checkedString
                        });

                        albumNames.push(eachAlbum.name);
                    }

                    if (albumNames.length > 0) {
                        $("#albumNameData").css("display", "block");
                    } else {
                        $("#albumNameData").css("display", "none");
                    }

                    $("#albumNameData").on("click", function (e) {
                        e.preventDefault();

                        shashin.createModalMultiselect(metadata.id, "album", html);
                    });
                    $(".album").on("click", function (e) {
                        metadataModal.populateAlbum(metadata.id);
                    });

                    shashin.createAutocomplete("#albumnames", albumNames, false);
                    shashin.syncCheckboxInputs("#albumnames", "album"+metadataId);
                }

                if ($("#hidden").length > 0 && metadata.hidden !== null && metadata.hidden === true) {
                    $("#hidden")[0].checked = true;
                }

                $("#albumDetailRow").remove();
                Util.populateDetailsInfo(metadata);

                if ($("#keywordsString").length > 0) {
                    const keywordAvailableList = $($("#keywordsString").val().split(",")).not($("#keywords").val().split(",")).get().filter(function (v) {
                        return v !== '';
                    });
                    shashin.createAutocomplete("#keywords", keywordAvailableList, true, 10);
                }

                if ($("#camerasString").length > 0) {
                    const camerasAvailableList = $($("#camerasString").val().split(",")).not($("#camera").val().split(",")).get().filter(function (v) {
                        return v !== '';
                    });
                    shashin.createAutocomplete("#camera", camerasAvailableList, false);
                }

                if ($("#lensesString").length > 0) {
                    const lensesAvailableList = $($("#lensesString").val().split(",")).not($("#lens").val().split(",")).get().filter(function (v) {
                        return v !== '';
                    });
                    shashin.createAutocomplete("#lens", lensesAvailableList, false);
                }


                // Open modal window
                $("#propMetadata").modal('show');
            }

            shashin.closeToastMessages({tag:"metadatamodal"});

        });
    };

    shashin.createModalMultiselect = function(metadataId, type, html) {
        $("#"+type+"SelectionList").html(html);

        const inputData = (type === "album") ? $("#albumnames").val() : $("#tagpeople").val();
        let inputDataArray = $.map(inputData.split(","), $.trim);

        const inputs = $('input[name="' + ((type === "album") ? "album" : "recognitionLabel") + metadataId + '[]"]');

        for (let index in inputs) {
            if ($.isNumeric(index) && inputs.hasOwnProperty(index)) {
                const inputEl = inputs[index];
                const nameValue = $(inputEl).val();

                if ($.inArray(nameValue, inputDataArray) !== -1) {
                    $(inputEl).attr('checked', true);
                } else {
                    $(inputEl).attr('checked', false);
                }
            }
        }

        $("#"+type+"SelectionLabel").text("Select " + type.charAt(0).toUpperCase() + type.slice(1));
        $("#"+type+"Selection").modal('show');

        $("#"+type+"Selection").on('hide.bs.modal', function () {
            $("#"+type+"SelectionLabel").text("");
        });

        inputs.on("click", function () {
            const nameValue = $(this).attr("value");
            const inputData = (type === "album") ? $("#albumnames").val() : $("#tagpeople").val();
            let inputDataArray = $.map(inputData.split(","), $.trim);

            if ($(this).is(":checked") === true) {
                if ($.inArray(nameValue, inputDataArray) === -1) {
                    if (type === "album") {
                        $("#albumnames").val((($("#albumnames").val().trim().length === 0) ? "" : $("#albumnames").val().trim() + ",") + nameValue);
                    } else {
                        $("#tagpeople").val((($("#tagpeople").val().trim().length === 0) ? "" : $("#tagpeople").val().trim() + ",") + nameValue);
                    }
                }
            } else {
                if ($.inArray(nameValue, inputDataArray) !== -1) {
                    // Take value out of array
                    inputDataArray = $.grep(inputDataArray, function (value) {
                        return value !== nameValue;
                    });

                    if (type === "album") {
                        $("#albumnames").val(inputDataArray.join(","));
                    } else {
                        $("#tagpeople").val(inputDataArray.join(","));
                    }
                }
            }
        });

        $("#confirm"+type.charAt(0).toUpperCase() + type.slice(1)+"Selection").on("click", function () {
            if (type === "album") {
                const checkedBoxes = $('input[name="album' + metadataId + '[]"]:checked');
                let albumString = "";

                checkedBoxes.each(function() {
                    albumString += $(this).val() + ",";
                });

                if (albumString.length > 0) {
                    albumString = albumString.slice(0,-1);
                }

                $("#albumnames").val(albumString);
            } else {
                const checkedBoxes = $('input[name="recognitionLabel' + metadataId + '[]"]:checked');
                let labelString = "";

                checkedBoxes.each(function () {
                    labelString += $(this).val() + ",";
                });

                if (labelString.length > 0) {
                    labelString = labelString.slice(0, -1);
                }

                $("#tagpeople").val(labelString);

                if (labelString !== "") {
                    $("#isobject").prop("checked", false);
                }
            }
        });
    };

    shashin.createBatchModalMultiselect = function(type) {
        $("#"+type+"BatchSelectionLabel").text("Select " + type.charAt(0).toUpperCase() + type.slice(1));
        $("#"+type+"BatchSelection").modal('show');

        $("#"+type+"BatchSelection").on('hide.bs.modal', function () {
            $("#"+type+"BatchSelectionLabel").text("");
        });

        const inputData = (type === "album") ? $("#albumNameInput").val() : $("#tagBatchDataInput").val();
        let inputDataArray = $.map(inputData.split(","), $.trim);

        const inputs = $('input[name="' + ((type === "album") ? "albums" : "recognitionLabel") + '[]"]');

        for (let index in inputs) {
            if ($.isNumeric(index) && inputs.hasOwnProperty(index)) {
                const inputEl = inputs[index];
                const nameValue = $(inputEl).val();

                if ($.inArray(nameValue, inputDataArray) !== -1) {
                    $(inputEl).attr('checked', true);
                } else {
                    $(inputEl).attr('checked', false);
                }
            }
        }

        $('input[name="' + ((type === "album") ? "albums" : "recognitionLabel") + '[]"]').on("click", function () {
            const nameValue = $(this).attr("value");
            const inputData = (type === "album") ? $("#albumNameInput").val() : $("#tagBatchDataInput").val();
            let inputDataArray = $.map(inputData.split(","), $.trim);

            if ($(this).is(":checked") === true) {
                if ($.inArray(nameValue, inputDataArray) === -1) {
                    if (type === "album") {
                        $("#albumNameInput").val((($("#albumNameInput").val().trim().length === 0) ? "" : $("#albumNameInput").val().trim() + ",") + nameValue);
                    } else {
                        $("#tagBatchDataInput").val((($("#tagBatchDataInput").val().trim().length === 0) ? "" : $("#tagBatchDataInput").val().trim() + ",") + nameValue);
                    }
                }
            } else {
                if ($.inArray(nameValue, inputDataArray) !== -1) {
                    // Take value out of array
                    inputDataArray = $.grep(inputDataArray, function (value) {
                        return value !== nameValue;
                    });

                    if (type === "album") {
                        $("#albumNameInput").val(inputDataArray.join(","));
                    } else {
                        $("#tagBatchDataInput").val(inputDataArray.join(","));
                    }
                }
            }
        });

        $("#confirmBatch"+type.charAt(0).toUpperCase() + type.slice(1)+"Selection").on("click", function () {
            if (type === "album") {
                const checkedBoxes = $('#albumBatchSelectionList :checked');
                let albumString = "";

                checkedBoxes.each(function() {
                    albumString += $(this).val() + ",";
                });

                if (albumString.length > 0) {
                    albumString = albumString.slice(0,-1);
                }

                $("#albumNameInput").val(albumString);
            } else {
                const checkedBoxes = $('#peopleBatchSelectionList :checked');
                let labelString = "";

                checkedBoxes.each(function () {
                    labelString += $(this).val() + ",";
                });

                if (labelString.length > 0) {
                    labelString = labelString.slice(0, -1);
                }

                $("#tagBatchDataInput").val(labelString);

                if (labelString !== "") {
                    $("#isobject").prop("checked", false);
                }
            }
        });
    };

    shashin.syncCheckboxInputs = function(inputEl, checkboxElName) {
        $(inputEl).on( "blur", function(e) {
            const terms = shashin.autocompleteSplit(this.value.trim());
            const checkBoxes = $('input[name="'+checkboxElName+'[]');

            checkBoxes.each(function() {
                if ($.inArray($(this).val(), terms) !== -1) {
                    $(this).prop("checked", true);
                } else {
                    $(this).prop("checked", false);
                }
            });
        });
    };

    shashin.createAutocomplete = function(inputEl, source, commaDelimited, resultLimit, functionOnSelect) {

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
                    filter = filter.slice(0, resultLimit);
                }

                response(filter);
            },
            focus: function () {
                // prevent value inserted on focus
                return false;
            },
            select: function (event, ui) {
                event.preventDefault();
                event.stopPropagation();

                const inputValues = this.value.split(",");
                $.each(inputValues, function(index, keywordItem) {
                    // do something with `item` (or `this` is also `item` if you like)
                    const keywordIndex = source.indexOf(keywordItem.trim());
                    if (keywordIndex !== -1) {
                        source.splice(keywordIndex, 1);
                    }
                });
                const terms = shashin.autocompleteSplit(this.value.trim());
                // remove the current input
                terms.pop();
                // add the selected item
                terms.push(ui.item.value.trim());

                if (true === commaDelimited) {
                    // add placeholder to get the comma-and-space at the end
                    terms.push("");
                    this.value = terms.join(",");
                    this.value = this.value.replace(/,\s*$/, "");
                } else {
                    this.value = terms;
                }

                if (functionOnSelect !== undefined && typeof functionOnSelect === 'function') {
                    functionOnSelect();
                }

                return false;
            }
        }).focus(function () {
            // Show dropdown on focus
            $(this).autocomplete("search");
        });
    };

    shashin.initLightGallery = function(lgElement,additionalLgConfigs,mediaElement) {
        shashin.setLightGalleryElement(lgElement);
        shashin.setLightGallery(additionalLgConfigs);

        let mediaContentList = [];
        $.each($(mediaElement), function() {
            const mediaContent = {};
            mediaContent.metadataDetailFun = shashin.openEditMetadataModal;
            mediaContent.videoThumbnailFun = shashin.processVideoThumbnail;
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
                mediaContent.downloadUrl = $(this).attr("data-download-url");
            } else if ($(this).attr("data-video")) {
                mediaContent.video = $(this).attr("data-video");
                mediaContent.poster = $(this).attr("data-poster");
                mediaContent.lgSize = $(this).attr("data-lg-size");
                mediaContent.downloadUrl = $(this).attr("data-download-url");
            }
            mediaContent.metadataId = $(this).attr("data-metadata-id");
            mediaContentList.push(mediaContent);
        });

        shashin.initMediaContent(mediaContentList);

        return mediaContentList;
    };

    shashin.initMediaContent = function(mediaContentList) {
        if (mediaContentList.length > 0 && shashin.getLightGallery() !== null) {
            shashin.refreshAndActivateLgListener(mediaContentList);
        }
    };

    shashin.updateMediaContent = function(mediaContentList,additionalMediaContentList) {
        if (additionalMediaContentList && additionalMediaContentList.length > 0) {
            mediaContentList = mediaContentList.concat(additionalMediaContentList);
            shashin.refreshAndActivateLgListener(mediaContentList);
        }

        return mediaContentList;
    };

    shashin.refreshAndActivateLgListener = function (mediaContentList) {
        if (shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
            shashin.getLightGallery().refresh(mediaContentList);
            // shashin.getLightGalleryElement().addEventListener('lgAfterSlide', function (e) {
            //     shashin.jumpToLightGalleryIndex(e.detail.index);
            // })
        }
    };

    shashin.pageLoader = function(func, appendClass, list, activePage) {
        let eol = false;

        const refreshIntervalId = window.setInterval(function () {
            if (!Util.hasScrollBar($("#container")) && !Util.hasScrollBar($("main"))) {
                setTimeout(async () => {
                    eol = await func();
                }, 1000);
            } else {
                clearInterval(refreshIntervalId);
            }

            if ((eol !== undefined && eol === true) || list === '' || list === '[]') {
                clearInterval(refreshIntervalId);
            }
        }, 200);

        function setupPlaceholders(activePage, speedInpxPerMs) {
            if (activePage !== undefined &&
                (activePage === "album" ||
                    activePage === "favorites" ||
                    activePage === "folder" ||
                    activePage === "recent" ||
                    activePage === "search" ||
                    activePage === "share" ||
                    activePage === "taken" ||
                    activePage === "accessed" ||
                    activePage === "trash" ||
                    activePage === "modified"))
            {
                // Show image from data
                if ((speedInpxPerMs < 0.20 && speedInpxPerMs > 0.15) || speedInpxPerMs === -1.0) {
                    const elementsInViewport = Util.elementsInViewport($(".photo-thumbnail-container"));
                    $.map(elementsInViewport, function (element) {
                        $(element).children('img').attr("src",$(element).children('img').attr("data-smallthumb"));
                        $(element).children('img').css("z-index", 0);
                    });
                }
            }
        }

        // xsmall/blurry images when scrolling
        createOnScrollListener($("#container"),eol);
        createOnScrollListener($("main"),eol);

        function createOnScrollListener(element, eol) {
            let lastOffset = $("#container").scrollTop();
            let lastDate = new Date().getTime();
            let scrollTimer = null;

            element.on('scroll', async function (e) {
                shashin.showScrollToTop(element);

                let st = $(e.target).scrollTop();
                let delayInMs = e.timeStamp - lastDate;
                let offset = st - lastOffset;
                let speedInpxPerMs = offset / delayInMs;

                if (scrollTimer !== null) {
                    clearTimeout(scrollTimer);
                }
                scrollTimer = setTimeout(function() {
                    $(window).trigger("scrollStop");
                }, 200);

                setupPlaceholders(activePage, speedInpxPerMs);

                if (Util.atEndOfPage(this) && eol === false) {
                    setTimeout(async function () {
                        eol = await func();
                    }, 200);
                }
            });

            $(window).bind("scrollStop", function() {
                setupPlaceholders(activePage, -1.0);
            });
        }

        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            scrollToTopButton.on("click",function () {
                $("main")[0].scrollTo({top: 0, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: 0, behavior: 'smooth'});
            });
        }
    };

    shashin.showScrollToTop = function(scrollEl) {
        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            if ((scrollEl[0].scrollTop > 20)) {
                scrollToTopButton.css("display","block");
            } else {
                scrollToTopButton.css("display","none");
            }
        }
    };

    shashin.showScrollToBottom = function(scrollEl) {
        const scrollToBottomButton = $("#btn-to-bottom");

        if (scrollToBottomButton.length > 0) {
            if ((scrollEl.innerHeight() + scrollEl.scrollTop()) >= scrollEl[0].scrollHeight) {
                scrollToBottomButton.css("display","none");
            } else {
                scrollToBottomButton.css("display","block");
            }
        }
    };

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
    };

    shashin.activateScrollToBottom = function() {
        const scrollToBottomButton = $("#btn-to-bottom");

        if (scrollToBottomButton.length > 0) {

            if ($("#container").hasScrollBar() || $("main").hasScrollBar()) {
                scrollToBottomButton.css("display","block");
            }

            $("#container").on('scroll', function () {
                shashin.showScrollToBottom($(this));
            });
            $("main").on('scroll', function () {
                shashin.showScrollToBottom($(this));
            });

            scrollToBottomButton.on("click",function () {
                $("main")[0].scrollTo({top: $("main")[0].scrollHeight, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: $("#container")[0].scrollHeight, behavior: 'smooth'});
            });
        }
    };

    shashin.openHeaderMap = function (metadata) {
        shashin.printMessageToConsole("Opening Siderbar with Map with metadata");
        shashin.printMessageToConsole(metadata);

        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            if (shashin.map === null) {
                const duration = 400;
                const interactions = [
                    new ol.interaction.DoubleClickZoom({
                        duration: duration,
                        useAnchor: false
                    }),
                    new ol.interaction.KeyboardZoom({
                        duration: duration,
                        useAnchor: false
                    }),
                    new ol.interaction.MouseWheelZoom({
                        duration: duration,
                        useAnchor: false
                    }),
                    new ol.interaction.DblClickDragZoom({
                        useAnchor: false
                    }),
                    new ol.interaction.DragZoom({
                        useAnchor: false
                    })
                ];

                shashin.map = new ol.Map({
                    controls: [],
                    layers: [
                        new ol.layer.Tile({
                            visible: true,
                            source: shashin.getMapSource("osm")
                        })
                    ],
                    target: 'headerMap',
                    interactions: interactions
                });
            } else {
                const baseLayer = new ol.layer.Tile({
                    visible: true,
                    source: shashin.getMapSource("osm")
                });
                shashin.map.addLayer(baseLayer);
            }

            if (shashin.layer !== null) {
                shashin.layer.getSource().clear();
            }

            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
            shashin.map.getView().setZoom(18);

            shashin.feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([metadata.lng, metadata.lat])),
                name: metadata.title
            });

            const iconSize = 30;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
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
        }
    };

    shashin.openMap = function (metadata) {
        if (metadata === undefined) {
            metadata = {};
        }
        shashin.printMessageToConsole("Opening Map with metadata",{tag:"latlng"});
        shashin.printMessageToConsole(metadata,{tag:"latlng"});

        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            $("#map").css("display","block");
            $("#mapTabMessage").css("display","block");
            let placeNameDisplayName = (metadata.placeName === null) ? 'Unknown location' : metadata.placeName;
            let placeNameDisplayNameArray = placeNameDisplayName.split(";");
            if (placeNameDisplayNameArray.length > 1) {
                placeNameDisplayName = placeNameDisplayNameArray[0];
            }
            shashin.printMessageToConsole("Opening modal map - original placename: " + metadata.placeName + " - Display placename: " + placeNameDisplayName,{tag:"latlng"});
            let queryParamDates = "";
            if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
                let month = metadata.month;
                if (month < 10) {
                    month = '0'+month;
                }
                let lastDay = metadata.day;
                if (lastDay < 29) {
                    lastDay = 28;
                }
                queryParamDates = '&sd='+metadata.year+'-'+month+'-01&ed='+metadata.year+'-'+month+'-'+lastDay;
            }
            $("#mapTabMessage").html(TimelineTemplates.MapLinks({metadata:metadata, placeNameDisplayName:placeNameDisplayName, queryParamDates:queryParamDates}));
        }

        if (shashin.map === null) {
            const duration = 400;
            const interactions = [
                new ol.interaction.DoubleClickZoom({
                    duration: duration
                }),
                new ol.interaction.KeyboardPan({
                    pixelDelta: 256
                }),
                new ol.interaction.KeyboardZoom({
                    duration: duration
                }),
                new ol.interaction.MouseWheelZoom({
                    duration: duration
                }),
                new ol.interaction.PinchRotate(),
                new ol.interaction.PinchZoom({
                    duration: duration
                }),
                new ol.interaction.DragPan({
                    kinetic: new ol.Kinetic(-0.005, 0.05, 100)
                }),
                new ol.interaction.DblClickDragZoom(),
                new ol.interaction.DragZoom(),
                new ol.interaction.DragRotate()
            ];

            let target = 'modalmap';

            if (metadata.hasOwnProperty("lat") === false && metadata.hasOwnProperty("lng") === false) {
                target = 'modalbatchmap';
            }

            shashin.map = new ol.Map({
                controls: [],
                layers: [
                    new ol.layer.Tile({
                        visible: true,
                        source: shashin.getMapSource("osm")
                    })
                ],
                target: target,
                interactions: interactions
            });
        } else {
            const baseLayer = new ol.layer.Tile({
                visible: true,
                source: shashin.getMapSource("osm")
            });
            shashin.map.addLayer(baseLayer);
        }

        const attributions = new ol.control.Attribution({collapsible: true});

        shashin.map.addControl(attributions);

        const copyPlacename = function (obj) {
            if (obj.hasOwnProperty("data") && obj.data !== null && obj.data !== "" && obj.data.placename !== null && obj.data.placename !== "") {
                const copyText = obj.data.placename;
                Util.copyToClipboard(copyText);
            }
        };

        const saveCoordinates = function (obj) {
            shashin.showToastMessage("Saving location", "Saving location", {
                icon: "bi-info-circle",
                iconColor: "#777777",
                tag: "latlng",
                autohide: false,
                borderColor:"success"
            });
            const coordArray = ol.proj.toLonLat(obj.coordinate);
            if (coordArray.length > 1) {
                const coords = coordArray[1]+","+coordArray[0];
                const json = {
                    id: metadata.id,
                    latlng: coords
                };
                const http = new Http("save location");
                http.ajax("put", "/metadata/update/coordinates/" + metadata.id + "?v="+uuidv4(), JSON.stringify(json), function (response) {
                    shashin.closeToastMessages({
                        tag: "latlng"
                    });
                    shashin.showToastMessage("Could not update location", "Could not update location", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        tag: "latlng",
                        borderColor:"danger"
                    });
                }).then(function (response) {
                    shashin.closeToastMessages({
                        tag: "latlng"
                    });
                    if (response.hasOwnProperty("status")) {
                        if (response.status !== shashin.apiResponse.SUCCESS) {
                            shashin.showToastMessage("Could not update location", "Could not update location", {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000",
                                tag: "latlng",
                                borderColor: "danger"
                            });
                        } else {
                            Util.setMetadataLocalStorage();

                            shashin.showToastMessage("Location saved", "Location saved. Refreshing map.", {
                                icon: "bi-info-circle",
                                iconColor: "#777777",
                                tag: "latlng",
                                borderColor:"success"
                            });

                            if (response.hasOwnProperty("metadata") && (response.metadata).hasOwnProperty("lat") && (response.metadata).hasOwnProperty("lng")) {
                                metadata.lat = response.metadata.lat;
                                metadata.lng = response.metadata.lng;

                                if ((response.metadata).hasOwnProperty("timeZone")) {
                                    $("#offsetTaken").val(response.metadata.timeZone);
                                }

                                if ((response.metadata).hasOwnProperty("placeName")) {
                                    const placeNameDisplayNameArr = (response.metadata.placeName).split(";");
                                    const placeNameDisplayName = placeNameDisplayNameArr[0];
                                    const placeName = TimelineTemplates.MapLinks({metadata:metadata, placeNameDisplayName:placeNameDisplayName, queryParamDates:""});
                                    $("#mapTabMessage").html(placeName);

                                    if (response.hasOwnProperty("shortPlaceName")) {
                                        $("#shortLocationLabel").html(response.shortPlaceName);
                                        $("#shortLocationLabel").attr("title", placeNameDisplayName);
                                    }
                                }

                                $("#metadataModalEdit"+metadata.id+" span").removeClass("bi-info-square").addClass("bi-info-circle");
                            }

                            // Update marker and center
                            shashin.map.getLayers().forEach(layer => {
                                if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "maplocation") {
                                    shashin.map.removeLayer(layer);
                                }
                            });

                            shashin.map.getView().setCenter(ol.proj.fromLonLat([coordArray[0], coordArray[1]]));
                            shashin.map.getView().setZoom(18);

                            shashin.feature = new ol.Feature({
                                geometry: new ol.geom.Point(ol.proj.fromLonLat([coordArray[0], coordArray[1]]))
                            });

                            const iconSize = 30;
                            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
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
                            shashin.layer.set('name', 'maplocation');
                            shashin.map.addLayer(shashin.layer);
                            $("#latlng").val(coordArray[1]+","+coordArray[0]);
                            $("#metadataModalEdit" + metadata.id + " span").removeClass("bi-info-square").addClass("bi-info-circle");
                        }
                    } else {
                        shashin.showToastMessage("Could not update location", "Could not update location", {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            tag: "latlng",
                            borderColor:"danger"
                        });
                    }
                });
            }
        };

        const setBatchCoordinates = function (obj) {
            const coordArray = ol.proj.toLonLat(obj.coordinate);
            if (coordArray.length > 1) {
                // const coords = coordArray[1]+","+coordArray[0];
                // const json = {
                //     id: metadata.id,
                //     latlng: coords
                // };
                // const http = new Http("save location");
                // http.ajax("put", "/metadata/update/batch/coordinates?v="+uuidv4(), JSON.stringify(json), function (response) {
                //     shashin.showToastMessage("Could not update location", "Could not update location", {
                //         icon: "bi-exclamation-triangle",
                //         iconColor: "#FF0000",
                //         tag: "latlng",
                //         borderColor:"danger"
                //     });
                // }).then(function (response) {
                    // Update marker and center
                    shashin.map.getLayers().forEach(layer => {
                        if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "maplocation") {
                            shashin.map.removeLayer(layer);
                        }
                    });

                    shashin.map.getView().setCenter(ol.proj.fromLonLat([coordArray[0], coordArray[1]]));
                    shashin.map.getView().setZoom(18);

                    shashin.feature = new ol.Feature({
                        geometry: new ol.geom.Point(ol.proj.fromLonLat([coordArray[0], coordArray[1]]))
                    });

                    const iconSize = 30;
                    const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
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
                    shashin.layer.set('name', 'maplocation');
                    shashin.map.addLayer(shashin.layer);
                    $("#latlngBatchData").val(coordArray[1]+","+coordArray[0]);

                    shashin.showToastMessage("Location set", "Coordinates set in Latitude/Longitude field", {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        tag: "latlng",
                        borderColor:"success"
                    });
                // });
            }
        };

        const copyCoordinates = function (obj) {
            const coordArray = ol.proj.toLonLat(obj.coordinate);
            if (coordArray.length > 1) {
                const copyText = coordArray[1]+","+coordArray[0];
                Util.copyToClipboard(copyText);
            }
        };

        const recenterCoordinates = function (obj) {
            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
            shashin.map.getView().setZoom(shashin.initialMapZoom);
        };

        shashin.contextMenu = new ContextMenu({
            width: 300,
            defaultItems: false // defaultItems are (for now) Zoom In/Zoom Out
        });
        shashin.contextMenu.on('close', function (evt) {
            shashin.map.getLayers().forEach(layer => {
                if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "tempCoordinates") {
                    shashin.map.removeLayer(layer);
                }
            });
        });

        const showContextMenu = (evt, coordArray, data) => {
            // Clear all previous coordinates
            shashin.map.getLayers().forEach(layer => {
                if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "tempCoordinates") {
                    shashin.map.removeLayer(layer);
                }
            });

            // Create icon for temp coordinate
            const feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat(coordArray)),
                name: 'tempMarker'
            });

            const iconSize = 25;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: grey;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
            const icon = 'data:image/svg+xml;utf8,' + svg;

            const styleIcon = new ol.style.Style({
                image: new ol.style.Icon({
                    opacity: 1,
                    src: icon,
                    anchor: [0.5, iconSize],
                    anchorXUnits: 'fraction',
                    anchorYUnits: 'pixels',
                    anchorOrigin: 'top-left',
                    offset: [0, 0]
                })
            });

            feature.setStyle(styleIcon);
            feature.setId("tempCoordinates");

            shashin.tempVector = new ol.source.Vector({
                features: [feature]
            });

            const layer = new ol.layer.Vector({
                source: shashin.tempVector
            });
            layer.set('name', 'tempCoordinates');
            shashin.map.addLayer(layer);

            feature.setStyle(styleIcon);
            layer.getSource().addFeature(feature);

            // Create menu for context menu
            const copyText = "Copy " + coordArray[1] + "," + coordArray[0];
            shashin.contextMenu.updatePosition([evt.pixel[0], evt.pixel[1] + 12]);

            const contextValueArray = [];

            let contextItem = {};
            if (data.hasOwnProperty("placename") && data.placename.length > 0) {
                contextItem = {
                    text: "<strong>" + data.placename + "</strong>",
                    // classname: "ol-ctx-menu-separator" // Make unselectable text
                    classname: "context-text-wrap",
                    callback: copyPlacename
                };
                contextItem.data = {placename: data.placename};

                contextValueArray.push(contextItem);
                contextValueArray.push("-");
            }

            if ($("#propMetadata").hasClass('show') === true && $("#generalTabNav").length > 0) {
                contextValueArray.push(
                    {
                        text: "Save Coordinates", // Set coordinates in modal field
                        callback: saveCoordinates
                    }
                );
            } else if ($("#propBatchMetadata").hasClass('show') === true) {
                contextValueArray.push(
                    {
                        text: "Set Lat/Lng Field", // Set coordinates in modal field
                        callback: setBatchCoordinates
                    }
                );
            }


            contextValueArray.push(
                {
                    text: copyText, // Copy coordinates from context menu
                    callback: copyCoordinates
                },
                {
                    text: "Recenter", // Recenter map to media location
                    callback: recenterCoordinates
                }
            );

            shashin.contextMenu.extend(contextValueArray);
        };

        shashin.contextMenu.on('open', function (evt) {
            shashin.contextMenu.clear();
            const coordArray = ol.proj.toLonLat(evt.coordinate);
            const http = new Http("get place data");

            if (coordArray.length > 1) {
                const json = {
                    lat: coordArray[1],
                    lng: coordArray[0]
                };

                if (shashin.showPlacename === true) {
                    http.ajax("post", "/placedata", JSON.stringify(json)).then(function (data) {
                        showContextMenu(evt, coordArray, data);
                    });
                } else {
                    showContextMenu(evt, coordArray, {});
                }
            }
        });

        shashin.map.addControl(shashin.contextMenu);

        if (shashin.layer !== null) {
            shashin.layer.getSource().clear();
        }

        if (metadata.hasOwnProperty("lat") && metadata.hasOwnProperty("lng")) {
            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
        } else {
            shashin.map.getView().setCenter(ol.proj.fromLonLat([0, 0]));
        }

        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            shashin.map.getView().setZoom(shashin.initialMapZoom);

            shashin.feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([metadata.lng, metadata.lat])),
                name: metadata.title
            });

            const iconSize = 30;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
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
            shashin.layer.set('name', 'maplocation');
            shashin.map.addLayer(shashin.layer);
        } else {
            shashin.map.getView().setZoom(0);
        }

        setTimeout(fixContentHeight, 1000);
        // else {
        //     if (shashin.layer !== null) {
        //         shashin.layer.getSource().clear();
        //     }
        //     $("#map").css("display","none");
        //     $("#mapTabMessage > .wrapper").contents().unwrap();
        //     $("#mapTabMessage").text("No map data");
        //     $("#mapTabMessage").css("display","block");
        // }
    };

    shashin.processVideoThumbnail = function(metadataId, lightGalleryId, lightGalleryIndex) {
        const mediaContentList = shashin.getLightGallery().galleryItems;

        shashin.getMetadata(metadataId).then(function (data) {
            let metadata = data;

            $(".lg-current").css("background-color", "#FFFFFF");

            if (metadata.type.indexOf("video") !== -1) {
                let canvas = document.createElement('canvas');
                $(canvas).attr("id", "videoCanvas");

                let video = null;
                if ($("#lg-item-"+lightGalleryId+"-"+lightGalleryIndex).length > 0) {
                    video = $("#lg-item-" + lightGalleryId + "-" + lightGalleryIndex).find(".lg-video-object")[0];
                }

                let image = "";

                try {
                    if (video !== null && $(video).length > 0) {
                        canvas.width = metadata.originalImageWidth;
                        canvas.height = metadata.originalImageHeight;

                        let ctx = canvas.getContext('2d');
                        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                        image = canvas.toDataURL('image/jpeg');
                    }
                } catch (e) {
                    shashin.printMessageToConsole("Error capturing thumbnail: " + e, {
                        consoleType: shashin.consoleTypes.error
                    });
                }

                $(canvas).remove();

                if (image.length > 0) {
                    const http = new Http("update video metadata");
                    const version = Util.getMetadataLocalStorage();
                    const json = {
                        metadataId: metadataId,
                        base64Data: image
                    };
                    http.ajax("post", "/metadata/update/videothumbs" + (version === "" ? "" : "?v=" + version), JSON.stringify(json)).then(function (data) {
                        if (data.hasOwnProperty("msg") && data.hasOwnProperty("status") && data.hasOwnProperty("posterUrl")) {
                            // Refresh image
                            Util.setMetadataLocalStorage();
                            const version = Util.getMetadataLocalStorage();
                            $("#image" + metadataId).attr("src", $("#image" + metadataId).attr("src") + (version === "" ? "" : "?v=" + version));
                            shashin.showToastMessage("Thumbnail image updated", "Thumbnails have been updated.", {
                                icon: "bi-info-circle",
                                iconColor: "#777777",
                                delay: 2000,
                                borderColor:"success"
                            });

                            if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null  && typeof shashin.getLightGallery().refresh === 'function' && mediaContentList.length > 0) {
                                const mediaContent = mediaContentList[lightGalleryIndex];

                                if (mediaContent.hasOwnProperty("video") &&
                                    // mediaContent.hasOwnProperty("poster") &&
                                    mediaContent.hasOwnProperty("downloadUrl") &&
                                    mediaContent.downloadUrl.includes(metadataId)
                                ) {
                                    mediaContentList[lightGalleryIndex].poster = data.posterUrl;
                                    const mediaLinkId = "#mediaLink"+metadataId;
                                    if ($(mediaLinkId).length > 0) {
                                        $(mediaLinkId).attr("data-poster", encodeURI(data.posterUrl).replace(";", "%3B")+"?v="+Util.getMetadataLocalStorage());
                                    }
                                }

                                shashin.getLightGallery().refresh(mediaContentList);
                            }

                            $(".lg-current").animate({backgroundColor: "transparent"}, 2000);
                        } else {
                            shashin.showToastMessage("Could not update thumbnail", "Could not update thumbnails", {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000",
                                borderColor:"danger"
                            });
                            $(".lg-current").css("background-color", "transparent");
                        }
                        $("#captureThumbnail").show();
                        $("#captureThumbnailSpinner").hide();
                        $("#captureThumbnail").prop( "disabled", false);
                        $("#captureThumbnailSpinner").prop( "disabled", false);
                    });
                } else {
                    shashin.showToastMessage("Could not update thumbnails", "Could not update thumbnails. Failed to capture image.", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor:"danger"
                    });
                    $("#captureThumbnail").show();
                    $("#captureThumbnailSpinner").hide();
                    $("#captureThumbnail").prop( "disabled", false);
                    $("#captureThumbnailSpinner").prop( "disabled", false);
                }
            } else {
                $(".lg-current").css("background-color", "transparent");
                shashin.showToastMessage("Could not update thumbnails", "Could not update thumbnails. "+metadata.fileName+" not a video.", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });
                $("#captureThumbnail").show();
                $("#captureThumbnailSpinner").hide();
                $("#captureThumbnail").prop( "disabled", false);
                $("#captureThumbnailSpinner").prop( "disabled", false);
            }

            if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
                shashin.getLightGallery().refresh();
            }
        });
    };

    shashin.openInfoSidebar = function(metadataId) {
        // Populate modal data
        shashin.getMetadata(metadataId).then(function (data) {
            let metadata = data;

            $("#infoSidebarTitle").text(metadata.title);
            $("#currentfilename").val(metadata.fileName);
            $("#currentlat").val(metadata.lat);
            $("#currentlng").val(metadata.lng);
            $("#metadataId").val(metadata.id);

            if (metadata.thumbnailUrlCentered !== null) {
                $("#propInfoSidebarThumbnail").html(TimelineTemplates.HeaderThumbnail({metadata:metadata, version: Util.getMetadataLocalStorage(), showMap: true}));
                shashin.openHeaderMap(metadata);
            }

            Util.populateDetailsInfo(metadata);

            // Open info sidebar
            $("#propInfoSidebar").css('z-index', 9999);
            const infoSidebar = document.getElementById('propInfoSidebar');
            const bsInfoSidebar = new bootstrap.Offcanvas(infoSidebar);
            bsInfoSidebar.show();
        });
    };

    shashin.addToMetadataThumbnailsList = function(thumbnail) {
        if ($("#multiSelectThumbnails").length > 0) {
            const metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
            if (metadataThumbnailsArray.indexOf(thumbnail) === -1) {
                metadataThumbnailsArray.push(thumbnail);
                $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
            }
        }
    };

    shashin.removeFromMetadataThumbnailsList = function(thumbnail) {
        if ($("#multiSelectThumbnails").length > 0) {
            const metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
            const index = metadataThumbnailsArray.indexOf(thumbnail);
            if (index > -1) {
                metadataThumbnailsArray.splice(index, 1);
            }
            $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
        }
    };

    shashin.getMetadataThumbnailsList = function() {
        if ($("#multiSelectThumbnails").length > 0) {
            return JSON.parse($("#multiSelectThumbnails").val());
        }

        return [];
    };

    shashin.removeFromMetadataFilenamesList = function(filename) {
        if ($("#multiSelectFilenames").length > 0) {
            const metadataFilenamesArray = shashin.getMetadataFilenamesList();
            const index = metadataFilenamesArray.indexOf(filename);
            if (index > -1) {
                metadataFilenamesArray.splice(index, 1);
            }
            $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
        }
    };

    shashin.getMetadataFilenamesList = function() {
        if ($("#multiSelectFilenames").length > 0) {
            return JSON.parse($("#multiSelectFilenames").val());
        }

        return [];
    };

    shashin.addToMetadataFilenamesList = function (filename) {
        if ($("#multiSelectFilenames").length > 0) {
            const metadataFilenamesArray = shashin.getMetadataFilenamesList();
            if (metadataFilenamesArray.indexOf(filename) === -1) {
                metadataFilenamesArray.push(filename);
                $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
            }
        }
    };

    shashin.addToMetadataIdList = function (metadataId) {
        if ($("#multiSelectMetadataIds").length > 0) {
            const metadataIdArray = shashin.getMetadataIdList();
            if (metadataIdArray.indexOf(metadataId) === -1) {
                metadataIdArray.push(metadataId);
                $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
            }
        }
    };

    shashin.removeFromMetadataIdList = function (metadataId) {
        if ($("#multiSelectMetadataIds").length > 0) {
            const metadataIdArray = shashin.getMetadataIdList();
            const index = metadataIdArray.indexOf(metadataId);
            if (index > -1) {
                metadataIdArray.splice(index, 1);
            }
            $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
        }
    };

    shashin.getMetadataIdList = function() {
        if ($("#multiSelectMetadataIds").length > 0) {
            return JSON.parse($("#multiSelectMetadataIds").val());
        }

        return [];
    };

    shashin.downloadSelected = async function (buttonId) {

        let span = null;
        if (typeof buttonId !== 'undefined') {
            span = $("#" + buttonId).find("span");
        }

        let activePage = "";
        if ($("#activePage").length > 0) {
            activePage = $("#activePage").val();
        }

        if (typeof buttonId === 'undefined' || (span !== null && span.hasClass('bi-download'))) {
            if ((span !== null && span.hasClass('bi-download'))) {
                span.addClass('spinner-grow').removeClass('bi-download');
            }

            let metadataIdList = shashin.getMetadataIdList();
            if (shashin.getMetadataIdList().length === 0) {
                $('.bi-circle-fill').each(function (i, obj) {
                    metadataIdList.push(obj.id.substring(6, obj.id.length));
                });
            }

            const endpoint = "/metadata/download/batch";

            shashin.closeToastMessages({tag:"downloadselected"});
            shashin.showToastMessage("Downloading media", "Downloading "+activePage+" media.", {
                icon:"bi-info-circle",
                iconColor:"#777777",
                autohide:false,
                tag:"downloadselected"
            });

            if (Util.isMobile() === false) {
                shashin.downloadInstance = $.fileDownload(endpoint, {
                    httpMethod: "POST",
                    data: "batchMetadataIds=" + JSON.stringify(metadataIdList),
                    successCallback: function (url) {
                        shashin.printMessageToConsole("Media ZIP download success");
                        shashin.printMessageToConsole(url);

                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }

                        shashin.closeToastMessages({tag:"downloadselected"});
                    },
                    failCallback: function (html, url) {
                        shashin.printMessageToConsole("Media ZIP download fail", {
                            consoleType: shashin.consoleTypes.error
                        });
                        shashin.printMessageToConsole(url, {
                            consoleType: shashin.consoleTypes.error
                        });
                        shashin.printMessageToConsole(html, {
                            consoleType: shashin.consoleTypes.error
                        });

                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }

                        shashin.closeToastMessages({tag:"downloadselected"});
                    }
                });
            } else {
                shashin.downloadInstance = fetch(endpoint, {
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

                        shashin.closeToastMessages({tag:"downloadselected"});
                    }).catch(() => {
                        shashin.printMessageToConsole("Media ZIP download fail using fetch()", {
                            consoleType: shashin.consoleTypes.error
                        });
                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }

                        shashin.closeToastMessages({tag:"downloadselected"});
                    });
            }
        }
    };

    shashin.removeAllMetadataIdList = function () {
        if ($("#multiSelectMetadataIds").length > 0) {
            $("#multiSelectMetadataIds").val(JSON.stringify([]));
        }
    };

    shashin.removeAllMetadataFilenamesList = function () {
        if ($("#multiSelectFilenames").length > 0) {
            $("#multiSelectFilenames").val(JSON.stringify([]));
        }
    };

    shashin.removeAllMetadataThumbnailsList = function () {
        if ($("#multiSelectThumbnails").length > 0) {
            $("#multiSelectThumbnails").val(JSON.stringify([]));
        }
    };

    shashin.jumpToLightGalleryIndex = function (index) {
        const url = location.href;
        location.href = '#lightGalleryIndex'+index;
        history.replaceState(null,null,url);
    };

    shashin.setLightGalleryElement = function (name) {
        shashin.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            shashin.infiniteScrollGallery = document.getElementById(name);

            // Event listeners for light gallery

            // Close gallery on browser/mobile back button
            shashin.infiniteScrollGallery.addEventListener('lgAfterOpen', function () {
                if (window.history && window.history.pushState) {
                    window.history.pushState('forward', null, "");

                    $(window).on('popstate', function() {
                        if (shashin.lg !== null) {
                            shashin.lg.closeGallery();
                        }
                    });

                }
            });

            shashin.infiniteScrollGallery.addEventListener('lgAfterClose', _ => {
                shashin.closeToastMessages({tags: ["subhtml"]});
            });

            // Hide sidebar when going to next slide
            shashin.infiniteScrollGallery.addEventListener('lgBeforeSlide', e => {
                const bsOffcanvasEl = document.getElementById('propInfoSidebar');
                const bsOffcanvas = bootstrap.Offcanvas.getInstance(bsOffcanvasEl);
                if (bsOffcanvas !== null) {
                    bsOffcanvas.hide();
                }

                if (shashin.lg !== null && shashin.lg.hasOwnProperty("galleryItems")) {
                    const galleryItems = shashin.lg.galleryItems;
                    const currentIndex = e.detail.index;
                    const galleryItem = galleryItems[currentIndex];

                    if (galleryItem.hasOwnProperty("subHtml") && galleryItem.subHtml !== "") {
                        let subhtml = galleryItem.subHtml;
                        shashin.showToastMessage(null, subhtml, {
                            tag: "subhtml",
                            autohide: false,
                            closeButton: false
                        });
                    }
                }
            });

            // If info sidebar open, pressing escape key closes only the sidebar
            $("#propInfoSidebar").on('keydown', function(e) {
                // escape
                if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
                    e.stopPropagation();
                    const bsOffcanvasEl = document.getElementById('propInfoSidebar');
                    const bsOffcanvas = bootstrap.Offcanvas.getInstance(bsOffcanvasEl);
                    bsOffcanvas.hide();
                    return false;
                }
            });
        }
    };

    // Close gallery on browser/mobile back button
    shashin.setLightGallery = function (additionalConfigs) {
        let configs = shashin.getLightGalleryConfigs(additionalConfigs);
        shashin.lg = lightGallery(shashin.getLightGalleryElement(), configs);
    };

    shashin.getLightGalleryElement = function () {
        return shashin.infiniteScrollGallery;
    };

    shashin.getLightGallery = function () {
        return shashin.lg;
    };

    shashin.openGallery = function (e, index) {
        e.preventDefault();
        if (shashin.getLightGallery() !== null) {
            shashin.getLightGallery().openGallery(index);
        }
    };

    shashin.getLightGalleryConfigs = function(additionalConfigs) {
        // shashin.autoplayVideo = $("#autoplayVideoSwitch").is(':checked');

        const configs = {
            plugins: [lgZoom, lgVideo, lgRelativeCaption, lgFullscreen, lgRotate, lgCastMedia],
            videojs: false,
            hideBarsDelay: 5000,
            showBarsAfter: 5000,
            allowMediaOverlap: true,
            counter: false,
            castMedia: true,
            fullScreen: true,
            download: true,
            zoomFromOrigin: true,
            // videoMaxSize: "7680-4320",
            speed: 0,
            preload: 0,
            autoplayFirstVideo: true,
            autoplayVideoOnSlide: true,
            gotoNextSlideOnVideoEnd: false,
            rotate: true,
            rotateLeft: true,
            rotateRight: true,
            flipHorizontal: true,
            flipVertical: false,
            licenseKey: Util.lgApiKey()
        };

        if (shashin.autoplayVideo === false) {
            configs.autoplayFirstVideo = false;
            configs.autoplayVideoOnSlide = false;
        }

        for (const key in additionalConfigs) {
            if (key === "plugins") {
                if ($.isArray(additionalConfigs[key])) {
                    $.each(additionalConfigs[key] , function(index, val) {
                        configs.plugins.push(val);
                    });
                } else {
                    configs.plugins.push(additionalConfigs[key]);
                }
            } else {
                configs[key] = additionalConfigs[key];
            }
        }

        return configs;
    };

    shashin.createPagination = function(currentPage,totalPages,activePage,mediaTypeFilter,identifier=0,shareId="") {
        const lgConfig = {
            dynamic: true,
            plugins: []
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig.metadataDetail = true;
            lgConfig.metadataDetailFun = shashin.openEditMetadataModal;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig.videoThumbnail = true;
            lgConfig.videoThumbnailFun = shashin.processVideoThumbnail;
        }

        let lgElement = 'scroll-gallery';
        if (activePage === "album" || activePage === "share" || activePage === "favorites") {
            lgElement = 'infinite-scroll-gallery';
        }

        let initGallery = true;
        if ($("#"+lgElement).length === 0) {
            shashin.printMessageToConsole("lightGallery element '"+lgElement+"' DNE",{tag:"pagination"});
            initGallery = false;
        }

        if ($('.mediaLink').length === 0) {
            shashin.printMessageToConsole("media element '.mediaLink' DNE",{tag:"pagination"});
            initGallery = false;
        }

        if (initGallery === true) {
            shashin.initLightGallery(lgElement, lgConfig, '.mediaLink');
        }

        if (totalPages > 1 && currentPage <= totalPages) {
            const options = {
                currentPage: currentPage,
                totalPages: totalPages,
                truncate: true,
                href: function (index) { //index starts from 0
                    let link = '/' + activePage + '/' + index + '/' + mediaTypeFilter;
                    if (activePage === "folders") {
                        link = '/' + activePage + '/' + index;
                    } else if (activePage === "folder") {
                        link = '/' + activePage + '/' + encodeURIComponent(encodeURIComponent(identifier)).replace(";", "%3B") + '/' + index;
                    } else if (shareId !== "" && identifier > 0) {
                        link = '/' + activePage + '/' + shareId + '/album/' + identifier + '/' + index;
                    } else if (identifier > 0) {
                        link = '/' + activePage + '/' + identifier + '/' + index + '/' + mediaTypeFilter;
                    }
                    return link;
                }
            };

            if (Util.isMobile()) {
                options.innerWindow = 2;
            } else {
                options.innerWindow = 3;
                options.outerWindow = 1;
                options.first = null;
                options.last = null;
            }

            $('#pagination').pagy(options);
        }
    };

    shashin.getMapSource = function (source) {
        let mapSource = new ol.source.OSM();

        switch(source) {
            case "osm":
                mapSource = new ol.source.OSM();
                break;
            case "arcGisWSM":
                mapSource = new ol.source.XYZ({
                    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
                    maxZoom: 19
                });
                break;
            case "arcGisWI":
                mapSource = new ol.source.XYZ({
                    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
                    maxZoom: 19
                });
                break;
            case "bingmaps":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "AerialWithLabels", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsROD":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "RoadOnDemand", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsBE":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "BirdseyeWithLabels", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsCD":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "CanvasDark", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsSS":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "Streetside", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "maptiler":
                mapSource =  new ol.source.TileJSON({
                    url: 'https://api.maptiler.com/maps/streets-v2/256/tiles.json?key=YlQvLcNKq0a4aFDX2z3O',
                    maxZoom: 19
                });
                break;
            case "maptilerHY":
                mapSource = new ol.source.TileJSON({
                    url: 'https://api.maptiler.com/maps/hybrid/256/tiles.json?key=YlQvLcNKq0a4aFDX2z3O',
                    maxZoom: 19
                });
                break;
            case "maptilerBA":
                mapSource = new ol.source.XYZ({
                    url: 'https://api.maptiler.com/maps/basic/256/{z}/{x}/{y}.png?key=YlQvLcNKq0a4aFDX2z3O',
                    maxZoom: 19
                });
                break;
            case "stadiaSA":
                mapSource =  new ol.source.StadiaMaps({
                    layer: 'alidade_satellite',
                    retina: false
                });
                break;
            case "mapbox":
                mapSource = new ol.source.XYZ({
                    url: 'https://api.mapbox.com/v4/mapbox.mapbox-streets-v8/1/0/0.mvt?access_token=pk.eyJ1IjoibWljaGFlbHR5YWdpIiwiYSI6ImNsdHQyeGY5azBxb3YyamxhdGttMzU3aW4ifQ.-2vN-mfBbj-HZh7VWGwFug',
                    maxZoom: 19
                });
                break;
            default:
                mapSource = new ol.source.OSM();
        }

        return mapSource;
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

            selectClick(metadata.id, view, opaque, transparent, metadataIdArray, true);
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
                if ($('.bi-circle-fill')[0] || metadataIdArray.length > 0) {
                    const isSelected = $("#tlicon" + metadata.id).attr("class") === "bi-circle";
                    const isVideo = metadata.type.includes("video");

                    shashin.lastSelectedMetadataId = metadata.id;
                    shashin.lastSelectedMetadataSelected = isSelected;

                    updateSelectionUI(metadata.id, isSelected, opaque);

                    updateSelectionState(metadata.id, isSelected, isVideo, view);

                    updateBorderUI(metadata.id);
                }

                metadataIdArray = shashin.getMetadataIdList();

                updateToolbarUI(view, metadataIdArray);

                updateSelectionCount(metadataIdArray);

                updateShareUI(view, metadataIdArray);

                setDateSection(metadata.id, view);
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
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
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
            metadataIdArray = shashin.getMetadataIdList();
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
            metadataIdArray = shashin.getMetadataIdList();
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
            metadataIdArray = shashin.getMetadataIdList();
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
    };

    function trackShareDownload(albumId,albumName,shareLink) {
        let downloadTimer;
        const tokenName = "ShashinShareAlbumName";
        const tokenSize = "ShashinShareAlbumSize";
        const configuredAttempts = 120;

        shashin.closeToastMessages({tag:"sharedownload"});
        shashin.showToastMessage("Downloading share album", "Downloading share album \""+albumName+"\".", {
            icon:"bi-info-circle",
            iconColor:"#777777",
            autohide:false,
            tag:"sharedownload"
        });
        setTimeout(function () { $("#download"+albumId).removeAttr("href"); }, 0);
        Util.setCookie(tokenName, "", "/");
        Util.setCookie(tokenSize, "", "/");

        let attempts = configuredAttempts;

        downloadTimer = setInterval( function() {
            const tokenCookieValue = Util.getCookie(tokenName);
            const tokenCookieSize = Util.getCookie(tokenSize);

            if ((tokenCookieValue !== "" && tokenCookieSize !== "") || attempts === 0) {
                if (attempts === 0) {
                    // $("#albumsMessage").html("&nbsp;").animate({opacity: 0}, 5000);
                } else {
                    shashin.showToastMessage("Share album download", "<strong>File name</strong> " + tokenCookieValue + " <strong>File size</strong> " + Util.formatBytes(tokenCookieSize), {icon:"bi-info-circle", iconColor:"#777777"});
                    Util.deleteCookie(tokenName, "/");
                    Util.deleteCookie(tokenSize, "/");
                    window.clearInterval(downloadTimer);

                    shashin.clearAlbumSelection();
                    $("#clearMultiSelect").hide();
                    $("#multiSelectMetadataIds").val("[]");
                    $("#albumNumberSelected").hide();
                    shashin.closeToastMessages({tag:"sharedownload"});
                    const downloadEl = $("#download" + albumId);
                    downloadEl.attr("name", "download");
                    downloadEl.attr("value", albumId);
                    downloadEl.attr("title", "Download all photos");
                    downloadEl.on("click", function() {
                        trackShareDownload(albumId,albumName,shareLink);
                    });
                }
            }

            attempts--;
        }, 1000);
    }

    shashin.batchSelect = function(metadataId, view, addBorder = true, opaque = 0.3, transparent = 1.0) {
        shashin.printMessageToConsole("Select action", { tag: "multiselect" });

        let metadataIdArrayCopy = shashin.getMetadataIdList();

        if (metadataIdArrayCopy.length === 0) {
            return metadataIdArrayCopy;
        }

        const resetBorders = () => {
            $('.photo-thumbnail-container').removeClass("border border-3 border-primary");
            $('.photo-thumbnail-image').removeClass("pb-1");
        };

        const updateImageSelection = (id, view, isSelected, opacityLevel, metadataArray) => {
            const imageUrl = $("#image" + id).attr("src").replace("/gif/" + id, "/" + (Util.isMobile() ? "100" : "225") + "/" + id);
            $("#image" + id).attr("src", imageUrl);

            const iconClass = $("#tlicon" + id).attr("class");
            const shouldSelect = (isSelected && iconClass === "bi-circle") || (!isSelected && iconClass === "bi-circle-fill");

            if (shouldSelect) {
                selectClick(id, view, opaque, transparent, metadataArray, false);
                $("#image" + id).css("opacity", opacityLevel);

                if (!isSelected && id !== metadataId && shashin.lastSelectedMetadataId !== id) {
                    $("#tntl" + id).css("display", "none");
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

        if (shashin.lastSelectedMetadataId && shashin.lastSelectedMetadataId !== metadataId) {
            const selectionHash = getElementLocation($("#photoThumbnailContainer" + shashin.lastSelectedMetadataId)[0]);
            const pointerHash = getElementLocation($("#photoThumbnailContainer" + metadataId)[0]);

            const direction = (pointerHash.y > selectionHash.y || (pointerHash.x > selectionHash.x && pointerHash.y >= selectionHash.y)) ? "down" : "up";

            if (view !== "timeline" && addBorder) {
                // Non-timeline selection logic...
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

                for (const currentMetadataId of selectedRowMetadataIds) {
                    const compareOne = direction === "down" ? lastSelectedMetadataId : metadataId;
                    const compareTwo = direction === "down" ? metadataId : lastSelectedMetadataId;

                    if (currentMetadataId === compareOne || start) {
                        if (currentMetadataId === compareOne) {
                            lastSelectedMetadataId = direction === "down" ? currentMetadataId : shashin.lastSelectedMetadataId;
                            start = true;
                            continue;
                        }

                        updateImageSelection(currentMetadataId, view, shashin.lastSelectedMetadataSelected, shashin.lastSelectedMetadataSelected ? opaque : transparent, metadataIdArrayCopy);
                        if (direction === "down") lastSelectedMetadataId = currentMetadataId;
                    }

                    if (currentMetadataId === compareTwo) {
                        updateImageSelection(metadataId, view, shashin.lastSelectedMetadataSelected, shashin.lastSelectedMetadataSelected ? opaque : transparent, metadataIdArrayCopy);
                        updateImageSelection(currentMetadataId, view, shashin.lastSelectedMetadataSelected, shashin.lastSelectedMetadataSelected ? opaque : transparent, metadataIdArrayCopy);
                        break;
                    }
                }

                resetBorders();
                applyBorderToLastSelected();
            } else if (["timeline", "accessed", "modified", "recent", "taken"].includes(view) || !addBorder) {
                const http = new Http("get ranged metadata");
                const version = Util.getMetadataLocalStorage();
                let url = view === "timeline"
                    ? `/metadata/range/${direction}/${shashin.lastSelectedMetadataId}/${metadataId}`
                    : `/browse/range/${metadataId}/${view}`;

                if (version) url += `?v=${version}`;

                http.ajax("get", url).then(data => {
                    if (data.hasOwnProperty("metadataIdArray")) {
                        const metadataIdArray = data.metadataIdArray;

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

                        updateSelectionCount(shashin.getMetadataIdList());
                        resetBorders();
                        applyBorderToLastSelected();

                        if (!addBorder) {
                            shashin.lastSelectedMetadataId = "";
                        }
                    }
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

    function selectClick(metadataId, view, opaque, transparent, metadataIdArray, clicked) {
        const isSelected = $("#tlicon" + metadataId).attr("class") === "bi-circle";
        const isVideo = $("#photoThumbnailContainer" + metadataId).hasClass("is-video");

        updateSelectionUI(metadataId, isSelected, opaque);
        updateSelectionState(metadataId, isSelected, isVideo, view);

        if (clicked) {
            shashin.lastSelectedMetadataId = metadataId;
            shashin.lastSelectedMetadataSelected = isSelected;
            updateBorderUI(metadataId);
        }

        metadataIdArray = shashin.getMetadataIdList();

        updateToolbarUI(view, metadataIdArray);

        updateSelectionCount(metadataIdArray);

        updateShareUI(view, metadataIdArray);

        setDateSection(metadataId, view);
    }

    function updateSelectionUI(metadataId, isSelected, opaque) {
        $("#tntl" + metadataId).show();
        $("#tlicon" + metadataId)
            .toggleClass('bi-circle-fill', isSelected)
            .toggleClass('bi-circle', !isSelected);
        $("#image" + metadataId).css("opacity", opaque);
        $("#tncentered" + metadataId).toggle(!isSelected);
        $("#tnbr" + metadataId).toggle(!isSelected);
        $("#tnbl" + metadataId).toggle(!isSelected);
    }

    function updateSelectionState(metadataId, isSelected, isVideo, view) {
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
    }

    function updateBorderUI(metadataId) {
        $('.photo-thumbnail-container').removeClass("border border-3 border-primary");
        $('.photo-thumbnail-image').removeClass("pb-1");
        if (shashin.multiSelected === true) {
            $("#photoThumbnailContainer" + metadataId).addClass("border border-3 border-primary");
            $("#image" + metadataId).addClass("pb-1");
        }
    }

    function updateToolbarUI(view, metadataIdArray) {
        const hasSelection = $('.bi-circle-fill')[0] || metadataIdArray.length > 0;
        $("#appSearch").toggle(!hasSelection);

        const showTools = (selector, hideSelector) => {
            $(selector).show();
            if (hideSelector) $(hideSelector).hide();
        };

        if (hasSelection) {
            if (["album", "favorites", "archived"].includes(view)) {
                showTools("#albumAppTools", view === "album" ? "#albumTools" : null);
            } else if (["timeline", "recent", "modified", "taken", "folder", "search"].includes(view)) {
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
    }

    function updateSelectionCount(metadataIdArray) {
        const count = metadataIdArray.length || $('.bi-circle-fill').length;
        const label = count + " Selected";
        $("#timelineNumberSelected, #matchesNumberSelected, #favoritesNumberSelected, #trashNumberSelected, #albumNumberSelected").text(label);
    }

    function updateShareUI(view, metadataIdArray) {
        if (view !== "share") return;

        const albumId = $("#albumId").val();
        const albumName = $("#albumName").val();
        const shareLink = $("#shareLink").val();
        const downloadEl = $("#download" + albumId);

        if ($('.bi-circle-fill').length > 0) {
            $("#clearMultiSelect").show();
            $("#albumNumberSelected").show();
            downloadEl.attr({
                name: "downloadArray",
                value: JSON.stringify(metadataIdArray),
                title: "Download selected media"
            });
        } else {
            shashin.clearAlbumSelection();
            $("#clearMultiSelect").hide();
            $("#multiSelectMetadataIds").val("[]");
            $("#albumNumberSelected").hide();
            downloadEl.attr({
                name: "download",
                value: albumId,
                title: "Download all photos"
            });
        }

        downloadEl.off("click").on("click", () => {
            trackShareDownload(albumId, albumName, shareLink);
        });
    }

    function setDateSection(metadataId, view) {
        setTimeout(function () {
            const rowId = $($("#photoThumbnailContainer" + metadataId).parent()[0]).attr("id");

            let date = rowId.replace("row", "");
            date = date.replace("dateBody", "");
            const selectedMetadata = JSON.parse($("#multiSelectMetadataIds").val());

            if (view === "timeline" || view === "taken" || view === "album" || view === "accessed" || view === "modified" || view === "recent") {
                let url = "/timeline/mediatype/" + shashin.mediaTypeFilter + "/date/" + date + "/metadata";
                if (view === "album") {
                    const albumId = $("#albumId").val();
                    url = "/album/mediatype/" + shashin.mediaTypeFilter + "/date/" + date + "/" + albumId;
                } else if (view === "accessed" || view === "modified" || view === "recent") {
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
    }

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
                    } else if (activePage === "accessed" || activePage === "modified" || activePage === "recent") {
                        url = "/browse/mediatype/" + mediaTypeFilter + "/date/" + date + "/" + activePage;
                    }

                    http.ajax("get", url).then(function (data) {
                        if (data.hasOwnProperty("status")) {
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

    shashin.clearTimelineSelection = function () {
        if (shashin.downloadInstance !== null) {
            shashin.downloadInstance.abort();
            shashin.downloadInstance = null;
            shashin.showToastMessage("Download cancelled", "Download cancelled.", {icon:"bi-info-circle", iconColor:"#777777"});
            $("button").find("span").addClass('bi-download').removeClass('spinner-grow');
        }
        shashin.lastSelectedMetadataId = "";
        shashin.lastSelectedMetadataSelected = false;
        shashin.removeAllMetadataFilenamesList();
        shashin.removeAllMetadataThumbnailsList();
        shashin.removeAllMetadataIdList();
        // $(".day-select").hide();
        $(".thumbnail-centered").hide();
        //$(".thumbnail-tr").hide();
        $(".thumbnail-br").hide();
        $(".thumbnail-bl").hide();
        $(".thumbnail-tl").hide();
        $(".photo-thumbnail-image").css("opacity", 1.0);
        $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');
        $(".day-select").addClass('bi-circle').removeClass('bi-circle-fill');

        $("#appSearch").show();
        shashin.multiSelected = false;
        $('.photo-thumbnail-container').removeClass("border").removeClass("border-3").removeClass("border-primary");
        $('.photo-thumbnail-image').removeClass("pb-1");
        $("#timelineAppTools").hide();
        $("#timelineTools").show();
        $("#albumTools").hide();
        $("#albumAppTools").hide();
        $("#matchesAppTools").hide();
        $("#comprefaceAppTools").hide();
    };

    shashin.clearAlbumSelection = function () {
        if (shashin.downloadInstance !== null) {
            shashin.downloadInstance.abort();
            shashin.downloadInstance = null;
            shashin.showToastMessage("Download cancelled", "Download cancelled.", {icon:"bi-info-circle", iconColor:"#777777"});
            $("button").find("span").addClass('bi-download').removeClass('spinner-grow');
        }
        shashin.lastSelectedMetadataId = "";
        shashin.lastSelectedMetadataSelected = false;
        shashin.removeAllMetadataFilenamesList();
        shashin.removeAllMetadataThumbnailsList();
        shashin.removeAllMetadataIdList();
        $(".day-select").hide();
        $(".thumbnail-centered").hide();
        //$(".thumbnail-tr").hide();
        $(".thumbnail-br").hide();
        $(".thumbnail-bl").hide();
        $(".thumbnail-tl").hide();
        $(".photo-thumbnail-image").css("opacity", 1.0);
        $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');
        $(".day-select").addClass('bi-circle').removeClass('bi-circle-fill');

        $("#appSearch").show();
        shashin.multiSelected = false;
        $('.photo-thumbnail-container').removeClass("border").removeClass("border-3").removeClass("border-primary");
        $('.photo-thumbnail-image').removeClass("pb-1");
        $("#timelineAppTools").hide();
        $("#timelineTools").hide();
        $("#albumTools").show();
        $("#albumAppTools").hide();
        $("#matchesAppTools").hide();
        $("#comprefaceAppTools").hide();
    };

    shashin.matchingListeners = function () {
        $("#matchToolsDeselectAll").on("click", function(e) {
            e.preventDefault();

            shashin.lastSelectedMetadataId = "";
            shashin.lastSelectedMetadataSelected = false;
            $(".thumbnail-centered").hide();
            //$(".thumbnail-tr").hide();
            $(".thumbnail-br").hide();
            $(".thumbnail-bl").hide();
            $(".thumbnail-tl").hide();
            $(".photo-thumbnail-image").css("opacity", 1.0);
            $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');

            $("#appSearch").show();
            shashin.multiSelected = false;
            $('.photo-thumbnail-container').removeClass("border").removeClass("border-3").removeClass("border-primary");
            $('.photo-thumbnail-image').removeClass("pb-1");
            $("#timelineAppTools").hide();
            $("#timelineTools").show();
            $("#albumTools").hide();
            $("#albumAppTools").hide();
            $("#matchesAppTools").hide();
        });

        $("#matchesAppTools").hide();

        $("#matchToolsBatchEdit").on("click", function(e) {
            e.preventDefault();

            let metadataIdList = [];
            let metadataFilenamesArray = shashin.getMetadataFilenamesList();
            let metadataThumbnailsArray = shashin.getMetadataThumbnailsList();

            let thumbnailList = "";
            $('.bi-circle-fill').each(function(i, obj) {
                const metadataId = obj.id.substring(6, obj.id.length);
                metadataIdList.push(metadataId);
            });

            if (Util.isMobile() && metadataIdList.length > 3) {
                thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[0] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[0] + '" draggable="false">';
                thumbnailList += '<span class="bi-arrow-left ms-1 me-1 display-6 align-middle"></span><span class="display-6 align-middle">' + (metadataIdList.length - 2).toString() + '</span><span class="bi-arrow-right ms-1 me-1 display-6 align-middle"></span>';
                thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[metadataThumbnailsArray.length - 1] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[metadataFilenamesArray.length - 1] + '" draggable="false">';
            } else if (Util.isMobile() === false && metadataIdList.length > 5) {
                thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[0] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[0] + '" draggable="false">';
                thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[1] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[0] + '" draggable="false">';
                thumbnailList += '<span class="bi-arrow-left ms-1 me-1 display-6 align-middle"></span><span class="display-6 align-middle">'+(metadataIdList.length-4).toString()+'</span><span class="bi-arrow-right ms-1 me-1 display-6 align-middle"></span>';
                thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[metadataThumbnailsArray.length-2] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[metadataFilenamesArray.length-2] + '" draggable="false">';
                thumbnailList += '<img class="me-1" loading="lazy" src="' + metadataThumbnailsArray[metadataThumbnailsArray.length-1] + '" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="' + metadataFilenamesArray[metadataFilenamesArray.length-1] + '" draggable="false">';
            } else {
                for (let index in metadataIdList) {
                    const metadataId = metadataIdList[index];
                    thumbnailList += TimelineTemplates.BatchHeaderThumbnail({
                        thumbnailImage: $("#thumbnailCentered" + metadataId).val(),
                        title: $("#filename" + metadataId).val().trim(),
                        version: Util.getMetadataLocalStorage()
                    });
                }
            }

            $("#batchMetadataIds").val(JSON.stringify(metadataIdList));
            if (thumbnailList !== "") {
                $("#editPhotosNamesModalLabel").html(thumbnailList);
            }

            const keywordAvailableList = $("#keywordsBatchString").length > 0 ? $("#keywordsBatchString").val().split(",") : [];
            shashin.createAutocomplete("#keywordsBatchData", keywordAvailableList, true, 10);

            const cameraList = $("#camerasBatchString").val().split(",");
            shashin.createAutocomplete("#cameraBatchData", cameraList, false);

            const lensList = $("#lensesBatchString").val().split(",");
            shashin.createAutocomplete("#lensBatchData", lensList, false);

            const albumcheckedBoxes = $('input[name="albums[]');
            const albumNames = [];
            albumcheckedBoxes.each(function() {
                albumNames.push($(this).val().replace(/ +(?= )/g,'').trim());
            });
            shashin.createAutocomplete("#albumNameInput", albumNames, false);
            shashin.syncCheckboxInputs("#albumNameInput", "albums");

            const peoplecheckedBoxes = $('input[name="recognitionLabel[]');
            const peopleNames = [];
            peoplecheckedBoxes.each(function() {
                peopleNames.push($(this).val().replace(/ +(?= )/g,'').trim());
            });
            shashin.createAutocomplete("#tagBatchDataInput", peopleNames, false);
            shashin.syncCheckboxInputs("#tagBatchDataInput", "recognitionLabel");

            $("#propBatchMetadata").modal('show');
        });
    };

    // Call in console
    // eg: shashin.enableDebug({tags: all, consoleTypes:[shashin.consoleTypes.log,shashin.consoleTypes.error],showTrace:true,writeLog:true})
    shashin.enableDebug = function (options) {
        this.showDebug = true;
        shashin.showTrace = false;
        shashin.writeLog = false;
        shashin.consoleFilterTypes = [];

        let showTrace = false;
        //[shashin.consoleTypes.error, shashin.consoleTypes.info, shashin.consoleTypes.log, shashin.consoleTypes.warn]
        let consoleTypes = [];
        let tags = ["all"];

        if (options === undefined || options === null) {
            showTrace = false;
            consoleTypes = [];
            shashin.writeLog = false;
            tags = ["all"];
        } else {
            if (options.hasOwnProperty("showTrace")) {
                showTrace = options.showTrace;
            }

            if (options.hasOwnProperty("consoleTypes")) {
                consoleTypes = options.consoleTypes;
            }

            if (options.hasOwnProperty("writeLog")) {
                shashin.writeLog = options.writeLog;
            }

            if (options.hasOwnProperty("tags")) {
                tags = options.tags;
            }

            if (options.hasOwnProperty("tag")) {
                if (options.hasOwnProperty("tags")) {
                    tags.push(options.tag);
                } else {
                    tags = [options.tag];
                }

            }
        }

        shashin.showTrace = showTrace;
        shashin.consoleFilterTypes = consoleTypes;
        shashin.consoleTags = tags;

        if (Util.localStorageAvailable() === true) {
            localStorage.setItem("showDebug", "on");
        }
    };

    // Call in console
    shashin.disableDebug = function () {
        this.showDebug = false;
        shashin.showTrace = false;
        shashin.writeLog = false;
        shashin.consoleFilterTypes = [];
        shashin.consoleTags = ["all"];

        if (Util.localStorageAvailable() === true && localStorage.getItem("showDebug") !== null) {
            localStorage.removeItem("showDebug");
        }
    };

    function getStackTrace() {
        let stack;

        try {
            throw new Error('');
        }
        catch (error) {
            stack = error.stack || '';
        }

        stack = stack.split('\n').map(function (line) { return line.trim(); });
        return stack.splice(stack[0] === 'Error' ? 2 : 1);
    }

    shashin.printMessageToConsole = function (msg, options) {
        // error, info, log, warn
        let consoleType = shashin.consoleTypes.log;
        let tags = ["all"];

        if (options === undefined || options === null) {
            consoleType = shashin.consoleTypes.log;
            tags = ["all"];
        } else {
            if (options.hasOwnProperty("consoleType")) {
                consoleType = options.consoleType;
            }

            if (options.hasOwnProperty("tag")) {
                tags = [options.tag];
            }

            if (options.hasOwnProperty("tags")) {
                tags = options.tags;
            }

            if (tags.includes("all")) {
                tags = ["all"];
            }
        }

        let localStorageDebugFlag = false;
        if (Util.localStorageAvailable() === true && localStorage.getItem("showDebug") !== null && localStorage.getItem("showDebug").length > 0) {
            let getFlag = localStorage.getItem("showDebug");
            if (getFlag === "on") {
                localStorageDebugFlag = true;
            }
        }

        if ((tags.some(r=> shashin.consoleTags.includes(r)) === true || ["all"].some(r=> shashin.consoleTags.includes(r)) === true) && (shashin.showDebug === true || localStorageDebugFlag === true)) {
            if (consoleType === shashin.consoleTypes.log && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.log, shashin.consoleFilterTypes) !== -1)) {
                console.log(msg + ". Tags: " + tags.join());
            } else if (consoleType === shashin.consoleTypes.error && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.error, shashin.consoleFilterTypes) !== -1)) {
                console.error(msg + ". Tags: " + tags.join());
            } else if (consoleType === shashin.consoleTypes.info && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.info, shashin.consoleFilterTypes) !== -1)) {
                console.info(msg + ". Tags: " + tags.join());
            } else if (consoleType === shashin.consoleTypes.warn && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.warn, shashin.consoleFilterTypes) !== -1)) {
                console.warn(msg + ". Tags: " + tags.join());
            } else {
                console.log(msg + ". Tags: " + tags.join());
            }

            if (shashin.showTrace === true) {
                console.log(getStackTrace().join('\n'));
            }

            if (shashin.writeLog === true) {
                let log = "";
                if (msg.length > 0) {
                    if (shashin.consoleFilterTypes.length === 0) {
                        log = "console.log: " + msg;
                    } else if (consoleType === shashin.consoleTypes.log && $.inArray(shashin.consoleTypes.log, shashin.consoleFilterTypes) !== -1) {
                        log = "console.log: " + msg;
                    } else if (consoleType === shashin.consoleTypes.error && $.inArray(shashin.consoleTypes.error, shashin.consoleFilterTypes) !== -1) {
                        log = "console.error: " + msg;
                    } else if (consoleType === shashin.consoleTypes.info && $.inArray(shashin.consoleTypes.info, shashin.consoleFilterTypes) !== -1) {
                        log = "console.info: " + msg;
                    } else if (consoleType === shashin.consoleTypes.warn && $.inArray(shashin.consoleTypes.warn, shashin.consoleFilterTypes) !== -1) {
                        log = "console.warn: " + msg;
                    }

                    if (log.length > 0) {
                        const http = new Http("log console output");
                        if (shashin.showTrace === true) {
                            log += "\n" + getStackTrace().join('\n');
                        }

                        setTimeout(function () {
                            let json = {consoleType: consoleType, log: log, tag: tag};
                            http.ajax("post", "/console/log", JSON.stringify(json)).then(function (data) {
                                if (data.hasOwnProperty("status") && data.status === "fail" && data.hasOwnProperty("msg")) {
                                    console.error("Could not log console output");
                                }
                            });
                        }, 0);
                    }
                }
            }
        }
    };

    shashin.removeThumbnail = function(metadataId) {
        let dateGalleryRemoved = false;
        const targetElement = $("#photoThumbnailContainer" + metadataId);

        const rowId = targetElement.parent().attr("id");
        const sectionId = $(targetElement.siblings("section")[0]).attr("id");
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
    };

    shashin.autocompleteSplit = function(val) {
        return val.split(/,\s*/);
    };

    shashin.autocompleteExtractLast = function(term) {
        return shashin.autocompleteSplit(term).pop();
    };

    shashin.processBatchAlbumList = function(data, albumInputVal) {
        if (albumInputVal === undefined) {
            albumInputVal = "";
        }

        if (data.hasOwnProperty("allAlbumList")) {
            // let renderAlbumList = false;
            const albumList = data.allAlbumList;
            const albumNames = [];
            const inputArr = albumInputVal.split(",");

            let batchHtml = "";

            for (let index in albumList) {
                const album = albumList[index];

                // if ($("#"+album.id).length === 0) {
                //     renderAlbumList = true;
                // }

                if (album.name.trim().length > 0) {
                    batchHtml +=
                        '<button class="dropdown-item" type="button">\n' +
                        '    <input type="checkbox" class="album" id="album' + album.id + '" value="' + Util.escapeHtml(album.name) + '" name="albums[]">\n' +
                        '    <label for="album' + album.id + '">' + Util.escapeHtml(album.name) + '</label>\n' +
                        '</button>\n';

                    albumNames.push(Util.escapeHtml(album.name));
                }
            }

            for (let index in inputArr) {
                const albumName = inputArr[index].trim();

                if (albumName.length > 0 && albumNames.includes(albumName) === false) {
                    batchHtml +=
                        '<button class="dropdown-item" type="button">\n' +
                        '    <input type="checkbox" class="album" id="'+albumName+'" value="'+Util.escapeHtml(albumName)+'" name="albums[]">\n' +
                        '    <label for="'+albumName+'">'+Util.escapeHtml(albumName)+'</label>\n' +
                        '</button>\n';

                    albumNames.push(albumName);
                }
            }

            if (albumNames.length > 0) {
                $("#albumBatchNameData").css("display", "block");

                shashin.createAutocomplete("#albumNameInput", albumNames, false);
                shashin.syncCheckboxInputs("#albumNameInput", "albums");

                $("#albumBatchSelectionList").html(batchHtml);
                $("#albumBatchNameData").on("click", function (e) {
                    e.preventDefault();
                    shashin.createBatchModalMultiselect("album");
                });
            } else {
                $("#albumBatchNameData").css("display", "none");
            }
        }
    };

    shashin.processBatchPeopleList = function(data, subjectInputVal) {
        if (subjectInputVal === undefined) {
            subjectInputVal = "";
        }

        if (data.hasOwnProperty("recognitionLabels")) {
            // let renderRecognitionLabels = false;
            const recognitionLabels = data.recognitionLabels;
            const recognitionLabelNames = [];
            const inputArr = subjectInputVal.split(",");

            let batchHtml = '';
            for (let index in recognitionLabels) {
                const recognitionLabel = recognitionLabels[index];

                // if ($("#"+recognitionLabel.id).length === 0) {
                //     renderRecognitionLabels = true;
                // }

                if (recognitionLabel.name !== null && recognitionLabel.name !== "null" && recognitionLabel.name.trim().length > 0 && recognitionLabel.id > 0) {
                    batchHtml +=
                        '           <button class="dropdown-item" type="button">\n' +
                        '               <input type="checkbox" class="recognitionLabel" id="recognitionLabel' + recognitionLabel.id + '" value="' + Util.escapeHtml(recognitionLabel.name) + '" name="recognitionLabel[]">\n' +
                        '               <label for="recognitionLabel' + recognitionLabel.id + '">' + Util.escapeHtml(recognitionLabel.name) + '</label>\n' +
                        '           </button>';

                    recognitionLabelNames.push(recognitionLabel.name);
                }
            }

            for (let index in inputArr) {
                const recognitionName = inputArr[index].trim();

                if (recognitionName.length > 0 && recognitionLabelNames.includes(recognitionName) === false) {
                    batchHtml +=
                        '<button class="dropdown-item" type="button">\n' +
                        '    <input type="checkbox" class="recognitionLabel" id="'+recognitionName+'" value="'+Util.escapeHtml(recognitionName)+'" name="recognitionLabel[]">\n' +
                        '    <label for="'+recognitionName+'">'+Util.escapeHtml(recognitionName)+'</label>\n' +
                        '</button>\n';

                    recognitionLabelNames.push(recognitionName);
                }
            }

            if (recognitionLabelNames.length > 0) {
                $("#peopleBatchNameData").css("display", "block");

                shashin.createAutocomplete("#tagBatchDataInput", recognitionLabelNames, false);
                shashin.syncCheckboxInputs("#tagBatchDataInput", "recognitionLabel");

                $("#peopleBatchSelectionList").html(batchHtml);
                $("#peopleBatchNameData").on("click", function (e) {
                    e.preventDefault();
                    shashin.createBatchModalMultiselect("people");
                });
            } else {
                $("#peopleBatchNameData").css("display", "none");
            }
        }
    };
}( window.shashin = window.shashin || {}, jQuery ));

if (typeof module !== 'undefined') {
    module.exports = window.shashin;
}