(function( shashin, $, undefined ) {
    shashin.showDebug = false;
    shashin.showTrace = false;
    shashin.map = null;
    shashin.layer = null;
    shashin.feature = null;
    shashin.infiniteScrollGallery = null;
    shashin.lg = null;
    shashin.ajaxRetries = 3;
    shashin.darkMode = false;
    shashin.showPlacename = false;
    shashin.autoplayVideo = false;
    shashin.lgSubHtmlTimeout = null;
    shashin.nonce = "";
    shashin.contextMenu = null;
    shashin.tempVector = null;
    shashin.downloadInstance = null;
    shashin.initialMapZoom = 17;
    shashin.toast = {};
    shashin.toast.target = {};
    shashin.toast.target.default = "defaultToastTarget";
    shashin.toast.target.one = "toastTarget1";
    shashin.toast.target.two = "toastTarget2";
    shashin.toast.target.three = "toastTarget3";
    shashin.toast.target.four = "toastTarget4";
    shashin.toast.placement = {};
    shashin.toast.placement.top = {};
    shashin.toast.placement.middle = {};
    shashin.toast.placement.bottom = {};
    shashin.toast.placement.top.left = "top_left";
    shashin.toast.placement.top.center = "top_center";
    shashin.toast.placement.top.right = "top_right";
    shashin.toast.placement.middle.left = "middle_left";
    shashin.toast.placement.middle.center = "middle_center";
    shashin.toast.placement.middle.right = "middle_right";
    shashin.toast.placement.bottom.left = "bottom_left";
    shashin.toast.placement.bottom.center = "bottom_center";
    shashin.toast.placement.bottom.right = "bottom_right";
    shashin.apiResponse = {};
    shashin.apiResponse.SUCCESS = "success";
    shashin.apiResponse.WARN = "warn";
    shashin.apiResponse.FAIL = "fail";

    function fixContentHeight() {
        if ($("div[data-role='dialog']").is(":visible")) {
            const dialog = $("div[data-role='dialog']:visible:visible");
            const contentHeight = 400;
            dialog.height(contentHeight);
            shashin.map.updateSize();
        }
    }

    /*
    options:
    icon = bootstrap icon
    iconColor = CSS color
    delay = in ms
    target = default defaultToastTarget, or one of toastTarget1,toastTarget2,toastTarget3,toastTarget4
    autohide = boolean
    */
    shashin.showToastMessage = function(title, message, options) {
        let titleField = null;
        let headerSubtextField = null;
        let messageField = null;
        let spacerField = null;
        let iconField = null;
        let icon = null;
        let iconColor = null;
        let target = null;
        let autohide = null;
        let headerSubtext = null;
        let delay = 5000;
        let placement = shashin.toast.placement.bottom.center;

        if (options === undefined || options === null) {
            target = shashin.toast.target.default;
            autohide = true;
        } else {
            if (options.hasOwnProperty("placement")) {
                placement = options["placement"];
                if (placement === shashin.toast.placement.top.left ||
                    placement === shashin.toast.placement.top.center ||
                    placement === shashin.toast.placement.top.right ||
                    placement === shashin.toast.placement.middle.left ||
                    placement === shashin.toast.placement.middle.center ||
                    placement === shashin.toast.placement.middle.right ||
                    placement === shashin.toast.placement.bottom.left ||
                    placement === shashin.toast.placement.bottom.center ||
                    placement === shashin.toast.placement.bottom.right
                ) {
                    $("#toastContainer")
                        .removeClass("top-0")
                        .removeClass("top-50")
                        .removeClass("bottom-0")
                        .removeClass("start-0")
                        .removeClass("start-50")
                        .removeClass("end-0")
                        .removeClass("translate-middle-x")
                        .removeClass("translate-middle-y")
                        .removeClass("translate-middle");

                    switch (placement) {
                        case shashin.toast.placement.top.left:
                            $("#toastContainer").addClass("top-0").addClass("start-0");
                            break;
                        case shashin.toast.placement.top.center:
                            $("#toastContainer").addClass("top-0").addClass("start-50").addClass("translate-middle-x");
                            break;
                        case shashin.toast.placement.top.right:
                            $("#toastContainer").addClass("top-0").addClass("end-0");
                            break;
                        case shashin.toast.placement.middle.left:
                            $("#toastContainer").addClass("top-50").addClass("start-0").addClass("translate-middle-y");
                            break;
                        case shashin.toast.placement.middle.center:
                            $("#toastContainer").addClass("top-50").addClass("start-50").addClass("translate-middle");
                            break;
                        case shashin.toast.placement.middle.right:
                            $("#toastContainer").addClass("top-50").addClass("end-0").addClass("translate-middle-y");
                            break;
                        case shashin.toast.placement.bottom.left:
                            $("#toastContainer").addClass("bottom-0").addClass("start-0");
                            break;
                        case shashin.toast.placement.bottom.center:
                            $("#toastContainer").addClass("bottom-0").addClass("start-50").addClass("translate-middle-x");
                            break;
                        case shashin.toast.placement.bottom.right:
                            $("#toastContainer").addClass("bottom-0").addClass("end-0");
                            break;
                        default:
                            $("#toastContainer").addClass("bottom-0").addClass("start-50").addClass("translate-middle-x");
                    }
                }
            } else {
                $("#toastContainer").addClass("bottom-0").addClass("start-50").addClass("translate-middle-x");
            }
            if (options.hasOwnProperty("icon")) {
                icon = options["icon"];
            }
            if (options.hasOwnProperty("iconColor")) {
                iconColor = options["iconColor"];
            }
            if (options.hasOwnProperty("delay")) {
                delay = options["delay"];
            }
            if (options.hasOwnProperty("headerSubtext")) {
                headerSubtext = options["headerSubtext"];
            }
            if (options.hasOwnProperty("target")) {
                target = options["target"];
            } else {
                target = shashin.toast.target.default;
            }
            if (options.hasOwnProperty("autohide")) {
                autohide = options["autohide"];
            } else {
                autohide = true;
            }
        }

        if (target === shashin.toast.target.one) {
            titleField = $("#toastTitle1");
            headerSubtextField = $("#headerSubtext1");
            messageField = $("#toastMessage1");
            iconField = $("#toastIcon1");
            spacerField = $("#toastSpacer1");
        } else if (target === shashin.toast.target.two) {
            titleField = $("#toastTitle2");
            headerSubtextField = $("#headerSubtext2");
            messageField = $("#toastMessage2");
            iconField = $("#toastIcon2");
            spacerField = $("#toastSpacer2");
        } else if (target === shashin.toast.target.three) {
            titleField = $("#toastTitle3");
            headerSubtextField = $("#headerSubtext3");
            messageField = $("#toastMessage3");
            iconField = $("#toastIcon3");
            spacerField = $("#toastSpacer3");
        } else if (target === shashin.toast.target.four) {
            titleField = $("#toastTitle4");
            headerSubtextField = $("#headerSubtext4");
            messageField = $("#toastMessage4");
            iconField = $("#toastIcon4");
            spacerField = $("#toastSpacer4");
        } else {
            target = shashin.toast.target.default;
            titleField = $("#toastTitle");
            headerSubtextField = $("#headerSubtext");
            messageField = $("#toastMessage");
            iconField = $("#toastIcon");
            spacerField = $("#toastSpacer");
        }

        if (title !== undefined && title !== null) {
            titleField.html(title);
        }

        if (headerSubtext !== undefined && headerSubtext !== null) {
            headerSubtextField.html(headerSubtext);
        }

        if (message !== undefined && message !== null) {
            messageField.html(message);
        }

        if (icon !== undefined && icon !== null) {
            let cssStyle = {"font-size":"1rem"};
            if (iconColor !== null) {
                cssStyle["color"] = iconColor;
            }
            iconField.css(cssStyle);
            iconField.addClass(icon);
            spacerField.html("&nbsp;");
        }

        if (autohide === false || autohide === true) {
            $("#" + target).attr("data-bs-autohide", autohide);

            if (autohide === true) {
                $("#"+target).attr("data-bs-delay", delay);
            }
        } else {
            $("#"+target).attr("data-bs-delay", delay);
        }

        const toastLive = document.getElementById(target);
        const toast = new bootstrap.Toast(toastLive);
        toast.show();
    }

    shashin.closeToastMessage = function (options) {
        let target = null;

        if (options === undefined || options === null) {
            target = shashin.toast.target.default;
            let toastLive = document.getElementById(target);
            let toast = new bootstrap.Toast(toastLive);
            toast.hide();

            target = shashin.toast.target.one;
            toastLive = document.getElementById(target);
            toast = new bootstrap.Toast(toastLive);
            toast.hide();

            target = shashin.toast.target.two;
            toastLive = document.getElementById(target);
            toast = new bootstrap.Toast(toastLive);
            toast.hide();

            target = shashin.toast.target.three;
            toastLive = document.getElementById(target);
            toast = new bootstrap.Toast(toastLive);
            toast.hide();

            target = shashin.toast.target.four;
            toastLive = document.getElementById(target);
            toast = new bootstrap.Toast(toastLive);
            toast.hide();
        } else if (options.hasOwnProperty("target")) {
            target = options.target;

            if (target !== shashin.toast.target.default &&
                target !== shashin.toast.target.one &&
                target !== shashin.toast.target.two &&
                target !== shashin.toast.target.three &&
                target !== shashin.toast.target.four) {
                target = shashin.toast.target.default;
            }

            const toastLive = document.getElementById(target);
            const toast = new bootstrap.Toast(toastLive);
            toast.hide();
        } else {
            target = shashin.toast.target.default;
            const toastLive = document.getElementById(target);
            const toast = new bootstrap.Toast(toastLive);
            toast.hide();
        }
    }

    shashin.getMediaContent = function(metadata) {
        const mediaContent = {};

        mediaContent.metadataDetailFun = shashin.openInfoSidebar;
        mediaContent.videoThumbnailFun = shashin.processVideoThumbnail;
        mediaContent.args = metadata.id;
        mediaContent.metadataId = metadata.id;

        if (metadata.type.includes("video")) {
            mediaContent.video = {
                "source": [{"src": metadata.videoUrl, "type": "video/mp4"}],
                "attributes": {
                    "preload": "auto",
                    "controls": true,
                    "autoplay": true
                }
            };
            mediaContent.lgSize = metadata.originalImageWidth+"-"+metadata.originalImageHeight;
            mediaContent.poster = ((null === metadata.thumbnailUrlOriginal || "" === metadata.thumbnailUrlOriginal) ? metadata.thumbnailUrlSmall : encodeURI(metadata.thumbnailUrlOriginal)) + "?v=" + Util.getMetadataLocalStorage();
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
        shashin.showToastMessage("AJAX error", "AJAX error"+description+". Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
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

        if  ($("#metadataModalEdit"+metadataId).attr("tag") && $("#metadataModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#metadataModalEdit"+metadataId).attr("tag"));
        }

        return metadata;
    }

    // Get metadata with albums,tagged people and keywords
    shashin.getCompleteMetadata = async function(metadataId) {
        const http = new Http("get timeline metadata");
        const version = Util.getMetadataLocalStorage();
        const data = await http.ajax("get", "/complete/metadata/"+metadataId+(version === "" ? "" : "?v=" + version));

        let ret = {};
        if (data.hasOwnProperty("metadata")) {
            ret = data;
        }

        return ret;
    }

    // Get just the metadata with all keywords and albums
    shashin.getMetadata = async function(metadataId) {
        const http = new Http("get metadata");
        const version = Util.getMetadataLocalStorage();
        const data = await http.ajax("get", "/metadata/"+metadataId+(version === "" ? "" : "?v=" + version));

        let metadata = {};
        metadata["keywords"] = [];
        metadata["albumMap"] = {};

        if (data.hasOwnProperty("metadata") && data.hasOwnProperty("keywordList") && data.hasOwnProperty("albumMap")) {
            metadata = data["metadata"];
            metadata["keywords"] = data["keywordList"];
            metadata["albumMap"] = data["albumMap"];
        }

        return metadata;
    }

    shashin.openEditMetadataModal = function (metadataId) {
        shashin.getCompleteMetadata(metadataId).then(async function (data) {
            if (data.hasOwnProperty("metadata") &&
                data.hasOwnProperty("taggedPeopleList") &&
                data.hasOwnProperty("keywordList") &&
                data.hasOwnProperty("allRecognitionLabels") &&
                data.hasOwnProperty("allAlbumList") &&
                data.hasOwnProperty("albumMap")) {

                const metadata = data["metadata"];

                const taggedPeopleArray = data["taggedPeopleList"];

                const keywordList = data["keywordList"];
                metadata["keywords"] = keywordList;
                const albumMap = data["albumMap"];
                metadata["albumMap"] = albumMap;

                const recognitionLabels = data["allRecognitionLabels"];
                const allAlbumList = data["allAlbumList"];
                let index;

                const keywordsAvailable = $('#keywordsString').val();
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
                $("#currentfilename").val(metadata.fileName)
                $("#currentlat").val(metadata.lat)
                $("#currentlng").val(metadata.lng)
                $("#keywordsString").val(keywordsAvailable);
                $("#camerasString").val(camerasList);
                $("#lensesString").val(lensList);
                $("#videoduration").css("display","none");

                if (metadata.thumbnailUrlCentered !== null) {
                    $("#propMetadataModalThumbnail").html(TimelineTemplates.HeaderThumbnail({metadata: metadata, version: Util.getMetadataLocalStorage()}));
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

                if (metadata.type.indexOf("video") >= 0) {
                    $("#videoduration").css("display","block");
                    let duration = metadata.duration
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

                const latlngValue = (metadata.hasOwnProperty("lat") && metadata.hasOwnProperty("lng") && metadata.lat != null && metadata.lng != null && metadata.lat !== "" && metadata.lng !== "") ? ($.trim(metadata.lat) + ',' + $.trim(metadata.lng)) : '';
                $("#latlng").val(latlngValue);
                $("#mapTabNav").show();
                if (latlngValue === "") {
                    $("#mapTabNav").hide();
                }

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
                    $("#peopleList").val(taggedPeopleString);
                } else if (metadata.tagpeople !== null) {
                    $("#tagpeople").val(metadata.tagpeople);
                    $("#peopleList").val(metadata.tagpeople);
                }

                if ($("#recognitionLabelInput").length > 0) {
                    $("#recognitionLabelInput").remove();
                }
                if (recognitionLabels !== null && recognitionLabels.length > 0) {
                    let html = ModalTemplates.PersonModalDropdownHead({metadata: metadata});
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
                    html += ModalTemplates.PersonModalDropdownFooter();

                    $(html).insertAfter($("#labelIdData"));
                    $("#tagpeopledropdown" + metadata.id).on("click", function (e) {
                        e.preventDefault();
                        metadataModal.toggleTagPeopleDropdown(metadata.id);
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

                if (allAlbumList !== null && allAlbumList.length > 0) {
                    let html = ModalTemplates.AlbumModalDropdownHeader({metadata: metadata});
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
                    html += ModalTemplates.AlbumModalDropdownFooter();

                    $(html).insertAfter($("#albumNameData"))
                    $("#albumdropdown" + metadata.id).on("click", function (e) {
                        e.preventDefault();
                        metadataModal.toggleAlbumDropdown(metadata.id);
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
                Util.populateDetailsInfo(metadata, "propMetadata");

                if ($("#keywordsString").length > 0) {
                    const keywordAvailableList = $($("#keywordsString").val().split(",")).not($("#keywords").val().split(",")).get().filter(function (v) {
                        return v !== ''
                    });
                    shashin.createAutocomplete("#keywords", keywordAvailableList, true, 10);
                }

                if ($("#camerasString").length > 0) {
                    const camerasAvailableList = $($("#camerasString").val().split(",")).not($("#camera").val().split(",")).get().filter(function (v) {
                        return v !== ''
                    });
                    shashin.createAutocomplete("#camera", camerasAvailableList, false);
                }

                if ($("#lensesString").length > 0) {
                    const lensesAvailableList = $($("#lensesString").val().split(",")).not($("#lens").val().split(",")).get().filter(function (v) {
                        return v !== ''
                    });
                    shashin.createAutocomplete("#lens", lensesAvailableList, false);
                }

                // Open modal window
                $("#propMetadata").modal('show');
            }
        });
    }

    shashin.syncCheckboxInputs = function(inputEl, checkboxElName) {
        $(inputEl).on( "blur", function(e) {
            const terms = shashin.autocompleteSplit(this.value.trim());
            const checkBoxes = $('input[name="'+checkboxElName+'[]"]');

            checkBoxes.each(function() {
                if ($.inArray($(this).val(), terms) !== -1) {
                    $(this).prop("checked", true);
                } else {
                    $(this).prop("checked", false);
                }
            });
        });
    }

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
    }

    shashin.initLightGallery = function(lgElement,additionalLgConfigs,mediaElement) {
        shashin.setLightGalleryElement(lgElement);
        shashin.setLightGallery(additionalLgConfigs);

        let mediaContentList = [];
        $.each($(mediaElement), function() {
            const mediaContent = {};
            mediaContent.metadataDetailFun = shashin.openInfoSidebar;
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
                mediaContent.downloadUrl = $(this).attr("data-src")+"/download";
            } else if ($(this).attr("data-video")) {
                mediaContent.video = $(this).attr("data-video");
                mediaContent.poster = $(this).attr("data-poster");
                mediaContent.lgSize = $(this).attr("data-lg-size");
                mediaContent.downloadUrl = $(this).attr("data-video")+"/download";
            }
            mediaContent.metadataId = $(this).attr("data-metadata-id");
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
        const appendClassObj = $(appendClass);

        const refreshIntervalId = window.setInterval(function () {
            if (!Util.hasScrollBar($("#container")) && !Util.hasScrollBar($("main"))) {
                setTimeout(async () => {
                    const page = await func();
                }, 1000);
            } else {
                clearInterval(refreshIntervalId);
            }

            if (appendClassObj[appendClassObj.length-1].textContent === "EOL" || list === '' || list === '[]') {
                clearInterval(refreshIntervalId);
            }
        }, 200);

        let eol = false;

        createOnScrollListener($("#container"),eol);
        createOnScrollListener($("main"),eol);

        function createOnScrollListener(element, eol) {
            element.on('scroll', async function () {
                shashin.showScrollToTop(element);
                if (Util.atEndOfPage(this)) {
                    setTimeout(async function () {
                        eol = await func();

                        if (eol !== undefined && eol === true) {
                            element.off('scroll');
                        }
                    }, 200);
                }
            })
        }

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
        shashin.printMessageToConsole("Opening Map with metadata");
        shashin.printMessageToConsole(metadata);

        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            $("#map").css("display","block");
            $("#mapTabMessage").css("display","block");
            let placeNameDisplayName = (metadata.placeName === null) ? 'Unknown location name' : metadata.placeName;
            let placeNameDisplayNameArray = placeNameDisplayName.split(";");
            if (placeNameDisplayNameArray.length > 1) {
                placeNameDisplayName = placeNameDisplayNameArray[0];
            }
            shashin.printMessageToConsole("Opening modal map - original placename: " + metadata.placeName + " - Display placename: " + placeNameDisplayName);
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

                shashin.map = new ol.Map({
                    controls: [],
                    layers: [
                        new ol.layer.Tile({
                            visible: true,
                            source: shashin.getMapSource("osm")
                        })
                    ],
                    target: 'modalmap',
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

            const processCopyText = function(obj, copyText, msgType) {
                const tempText = document.createElement("input");
                tempText.value = copyText;
                tempText.type = "hidden";
                tempText.id = "tempClipboardMapId";
                tempText.setAttribute('data-clipboard-text', copyText);
                document.body.appendChild(tempText);
                tempText.select();

                let clipboard = null;

                if ($("#propMetadata").length > 0) {
                    clipboard = new ClipboardJS('#tempClipboardMapId', {container: document.getElementById("propMetadata")});
                } else if ($("#propInfoModal").length > 0) {
                    clipboard = new ClipboardJS('#tempClipboardMapId', {container: document.getElementById("propInfoModal")});
                }

                if (clipboard !== null) {
                    $("#tempClipboardMapId").on("click", function () {

                        clipboard.on('success', function (e) {
                            shashin.showToastMessage(msgType + "copied to clipboard", e.text + " copied to clipboard", {
                                icon: "bi-info-circle",
                                iconColor: "#777777"
                            });
                        });

                        clipboard.on('error', function (e) {
                            shashin.showToastMessage("Could not copy " + msgType, copyText + " could not be copied: " + e, {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000"
                            });
                        });
                    });
                    $("#tempClipboardMapId").trigger("click");

                    $("#tempClipboardMapId").remove();
                    clipboard.destroy();
                }
            }

            const copyPlacename = function (obj) {
                if (obj.hasOwnProperty("data") && obj.data !== null && obj.data !== "" && obj.data.placename !== null && obj.data.placename !== "") {
                    const copyText = obj.data.placename;
                    processCopyText(obj, copyText, "location");
                }
            }

            const copyCoordinates = function (obj) {
                const coordArray = ol.proj.toLonLat(obj.coordinate);
                if (coordArray.length > 1) {
                    const copyText = coordArray[1]+","+coordArray[0];
                    processCopyText(obj, copyText, "coordinates");
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
                    if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties()["name"] === "tempCoordinates") {
                        shashin.map.removeLayer(layer);
                    }
                });
            });

            function showContextMenu(evt, coordArray, data) {
                let placeJson = {};
                if (data.hasOwnProperty("msg") && data.hasOwnProperty("status") && data.hasOwnProperty("placedata") && data["status"] === shashin.apiResponse.SUCCESS) {
                    placeJson = JSON.parse(data["placedata"]);
                }

                shashin.printMessageToConsole("Placedata:");
                shashin.printMessageToConsole(placeJson);

                // Clear all previous coordinates
                shashin.map.getLayers().forEach(layer => {
                    if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties()["name"] === "tempCoordinates") {
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
                layer.set('name', 'tempCoordinates')
                shashin.map.addLayer(layer);

                feature.setStyle(styleIcon);
                layer.getSource().addFeature(feature);

                // Create menu for context menu
                const copyText = coordArray[1] + "," + coordArray[0];
                shashin.contextMenu.updatePosition([evt.pixel[0], evt.pixel[1] + 12]);

                const contextValueArray = [];

                if (placeJson.hasOwnProperty("name") && placeJson["name"] !== null && placeJson["name"] !== "") {
                    const contextItem = {
                        text: "<strong>" + placeJson["name"] + "</strong>",
                        // classname: "ol-ctx-menu-separator" // Make unselectable text
                        classname: "context-text-wrap",
                        callback: copyPlacename
                    }
                    contextItem.data = { placename: placeJson["name"] };

                    contextValueArray.push(
                        contextItem,
                        "-"
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
            }
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

            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
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
            shashin.map.addLayer(shashin.layer);

            setTimeout(fixContentHeight, 1000);
        } else {
            if (shashin.layer !== null) {
                shashin.layer.getSource().clear();
            }
            $("#map").css("display","none");
            $("#mapTabMessage > .wrapper").contents().unwrap();
            $("#mapTabMessage").text("No map data");
            $("#mapTabMessage").css("display","block");
        }
    }

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
                    shashin.printMessageToConsole("Error capturing thumbnail: " + e);
                }

                $(canvas).remove();

                if (image.length > 0) {
                    const http = new Http("update video metadata");
                    const version = Util.getMetadataLocalStorage();
                    const json = {
                        metadataId: metadataId,
                        base64Data: image
                    }
                    http.ajax("post", "/metadata/update/videothumbs" + (version === "" ? "" : "?v=" + version), JSON.stringify(json)).then(function (data) {
                        if (data.hasOwnProperty("msg") && data.hasOwnProperty("status") && data.hasOwnProperty("posterUrl")) {
                            // Refresh image
                            Util.setMetadataLocalStorage();
                            const version = Util.getMetadataLocalStorage();
                            $("#image" + metadataId).attr("src", $("#image" + metadataId).attr("src") + (version === "" ? "" : "?v=" + version));
                            shashin.showToastMessage("Thumbnail image updated", "Thumbnails have been updated.", {
                                icon: "bi-info-circle",
                                iconColor: "#777777",
                                delay: 2000
                            });

                            if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null && mediaContentList.length > 0) {
                                const mediaContent = mediaContentList[lightGalleryIndex];

                                if (mediaContent.hasOwnProperty("video") &&
                                    // mediaContent.hasOwnProperty("poster") &&
                                    mediaContent.hasOwnProperty("downloadUrl") &&
                                    mediaContent.downloadUrl.includes(metadataId)
                                ) {
                                    mediaContentList[lightGalleryIndex].poster = data["posterUrl"];
                                    const mediaLinkId = "#mediaLink"+metadataId;
                                    if ($(mediaLinkId).length > 0) {
                                        $(mediaLinkId).attr("data-poster", encodeURI(data["posterUrl"])+"?v="+Util.getMetadataLocalStorage());
                                    }
                                }

                                shashin.getLightGallery().refresh(mediaContentList);
                            }

                            $(".lg-current").animate({backgroundColor: "transparent"}, 2000);
                        } else {
                            shashin.showToastMessage("Could not update thumbnail", "Could not update thumbnails", {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000"
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
                        iconColor: "#FF0000"
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
                    iconColor: "#FF0000"
                });
                $("#captureThumbnail").show();
                $("#captureThumbnailSpinner").hide();
                $("#captureThumbnail").prop( "disabled", false);
                $("#captureThumbnailSpinner").prop( "disabled", false);
            }

            if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null) {
                shashin.getLightGallery().refresh();
            }
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
                $("#propInfoSidebarThumbnail").html(TimelineTemplates.HeaderThumbnail({metadata:metadata, version: Util.getMetadataLocalStorage()}));
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
            const metadataIdArray = shashin.getMetadataIdList();
            if (metadataIdArray.indexOf(metadataId) === -1) {
                metadataIdArray.push(metadataId);
                $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
            }
        }
    }

    shashin.removeFromMetadataIdList = function (metadataId) {
        if ($("#multiSelectMetadataIds").length > 0) {
            const metadataIdArray = shashin.getMetadataIdList();
            const index = metadataIdArray.indexOf(metadataId);
            if (index > -1) {
                metadataIdArray.splice(index, 1);
            }
            $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
        }
    }

    shashin.getMetadataIdList = function() {
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

            let metadataIdList = shashin.getMetadataIdList();
            if (shashin.getMetadataIdList().length === 0) {
                $('.bi-circle-fill').each(function (i, obj) {
                    metadataIdList.push(obj.id.substring(6, obj.id.length));
                });
            }

            const endpoint = "/metadata/download/batch";

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

    // Close gallery on browser/mobile back button
    shashin.setLightGallery = function (additionalConfigs) {
        let configs = shashin.getLightGalleryConfigs(additionalConfigs);
        shashin.lg = lightGallery(shashin.getLightGalleryElement(), configs);
    }

    // Close gallery on clicking browser back button
    shashin.closeGalleryOnBack = function (options) {
        // let galleryElement = shashin.getLightGalleryElement();
        // let lg = shashin.getLightGallery();
        //
        // if (options &&
        //     options.hasOwnProperty("galleryElement") && options.galleryElement !== null &&
        //     options.hasOwnProperty("lg") && options.lg !== null
        // ) {
        //     galleryElement = options.galleryElement;
        //     lg = options.lg;
        // }
        //
        // if (galleryElement !== null && window.history && window.history.pushState) {
        //     let backPressed = false;
        //
        //     galleryElement.addEventListener('lgAfterOpen', function () {
        //         window.history.pushState({}, null, "");
        //
        //         $(window).on('popstate', popStateListener);
        //     });
        //
        //     galleryElement.addEventListener('lgBeforeClose', function () {
        //         if (false === backPressed) {
        //             window.history.back();
        //         }
        //
        //         backPressed = false;
        //     });
        //
        //     function popStateListener(event) {
        //         backPressed = true;
        //         if ($(".lg-show").length > 0) {
        //             if (true === Util.isMobile()) {
        //                 lg.closeGallery();
        //             } else {
        //                 $(".lg-close").click();
        //             }
        //         }
        //     }
        // }
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
        shashin.autoplayVideo = $("#autoplayVideoSwitch").is(':checked');

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
        }

        if (shashin.autoplayVideo === false) {
            configs.autoplayFirstVideo = false;
            configs.autoplayVideoOnSlide = false;
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
        let mapSource = new ol.source.OSM();

        switch(source) {
            case "osm":
                mapSource = new ol.source.OSM();
                break
            case "arcGisWSM":
                mapSource = new ol.source.XYZ({
                    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
                    maxZoom: 19
                });
                break
            case "arcGisWI":
                mapSource = new ol.source.XYZ({
                    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
                    maxZoom: 19
                });
                break
            case "bingmaps":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "AerialWithLabels", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break
            case "bingmapsROD":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "RoadOnDemand", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break
            case "bingmapsBE":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "BirdseyeWithLabels", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break
            case "bingmapsCD":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "CanvasDark", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break
            case "bingmapsSS":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "Streetside", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break
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
            case "maptilerSA":
                mapSource =  new ol.source.TileJSON({
                    url: 'https://api.maptiler.com/tiles/satellite-v2/256/tiles.json?key=YlQvLcNKq0a4aFDX2z3O',
                    maxZoom: 19
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

        return mapSource
    }

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
    }

    shashin.setPhotoOverlays = function (metadata, view) {
        const opaque = 0.3
        const transparent = 1.0

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

            if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                $("#tntl" + metadata.id).show();
                $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
                $("#image" + metadata.id).css("opacity", opaque);
                //$("#tntr" + metadata.id).hide();
                $("#tncentered" + metadata.id).hide();
                $("#tnbr" + metadata.id).hide();
                $("#tnbl" + metadata.id).hide();
                // List of selected media
                shashin.addToMetadataIdList(metadata.id);
                shashin.addToMetadataFilenamesList($('#filename' + metadata.id).val());
                shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
                if (metadata.type.includes("video")) {
                    const jpgUrl = $("#image" + metadata.id).attr("src").replace("_225.gif", "_225.jpg");
                    $("#image" + metadata.id).attr("src", jpgUrl);
                }
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
                if (metadata.type.includes("video") && (view !== "timeline" || (view === "timeline" && timelineSettings && timelineSettings.isScrolling === false))) {
                    const gifUrl = $("#image" + metadata.id).attr("src").replace("_225.jpg", "_225.gif");
                    $("#image" + metadata.id).attr("src", gifUrl);
                }
            }

            metadataIdArray = shashin.getMetadataIdList();

            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $("#appSearch").hide();
                if (view === "album" || view === "favorites" || view === "trash") {
                    $("#albumAppTools").show();
                    if (view === "album") {
                        $("#albumTools").hide();
                    }
                } else if (view === "timeline" || view === "recent" || view === "modified" || view === "taken" || view === "folder" || view === "search") {
                    $("#timelineAppTools").show();
                    if (view === "timeline" || view === "folder") {
                        $("#timelineTools").hide();
                    }
                } else if (view === "matches" || view === "person" || view === "compreface") {
                    $("#matchesAppTools").show();
                    $("#timelineTools").hide();
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
                if (view === "timeline" || view === "folder" || view === "matches" || view === "person" || view === "compreface") {
                    $("#timelineTools").show();
                } else if (view === "album") {
                    $("#albumTools").show();
                }
            }

            const metadataList = shashin.getMetadataIdList();
            let timelineSelectCount = $('.bi-circle-fill').length;
            if (metadataList.length > 0) {
                timelineSelectCount = metadataList.length;
            }

            $("#timelineNumberSelected").text(timelineSelectCount + " Selected");
            $("#matchesNumberSelected").text($('.bi-circle-fill').length + " Selected");
            $("#favoritesNumberSelected").text($('.bi-circle-fill').length + " Selected");
            $("#trashNumberSelected").text($('.bi-circle-fill').length + " Selected");
            $("#albumNumberSelected").text($('.bi-circle-fill').length + " Selected");

            if (view === "share") {
                const albumId = $("#albumId").val();
                const albumName = $("#albumName").val();
                const shareLink = $("#shareLink").val();
                const downloadEl = $("#download" + albumId);

                if ($('.bi-circle-fill').length > 0) {
                    $("#clearMultiSelect").show();
                    $("#albumNumberSelected").show();
                    downloadEl.attr("name", "downloadArray");
                    downloadEl.attr("value", JSON.stringify(metadataList));
                    downloadEl.attr("title", "Download selected media");
                } else {
                    shashin.clearAlbumSelection();
                    $("#clearMultiSelect").hide();
                    $("#multiSelectMetadataIds").val("[]");
                    $("#albumNumberSelected").hide();
                    downloadEl.attr("name", "download");
                    downloadEl.attr("value", albumId);
                    downloadEl.attr("title", "Download all photos");
                }

                $("#download"+albumId).on("click", function() {
                    trackShareDownload(albumId,albumName,shareLink);
                });
            }
        });

        $("#image" + metadata.id).on('error', function() {
            $("#image" + metadata.id).attr("src", metadata.thumbnailUrlSmall);
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
                    // List of selected media
                    shashin.addToMetadataIdList(metadata.id);
                    shashin.addToMetadataFilenamesList($('#filename' + metadata.id).val());
                    shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
                    if (metadata.type.includes("video")) {
                        const jpgUrl = $("#image" + metadata.id).attr("src").replace("_225.gif", "_225.jpg");
                        $("#image" + metadata.id).attr("src", jpgUrl);
                    }
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
                    if (metadata.type.includes("video") && (view !== "timeline" || (view === "timeline" && timelineSettings && timelineSettings.isScrolling === false))) {
                        const gifUrl = $("#image" + metadata.id).attr("src").replace("_225.jpg", "_225.gif");
                        $("#image" + metadata.id).attr("src", gifUrl);
                    }
                }
            }

            metadataIdArray = shashin.getMetadataIdList();

            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $("#appSearch").hide();
                if (view === "album" || view === "favorites" || view === "trash") {
                    $("#albumAppTools").show();
                    if (view === "album") {
                        $("#albumTools").hide();
                    }
                } else if (view === "timeline" || view === "recent" || view === "modified" || view === "taken" || view === "folder" || view === "search") {
                    $("#timelineAppTools").show();
                    if (view === "timeline" || view === "folder") {
                        $("#timelineTools").hide();
                    }
                } else if (view === "matches" || view === "person" || view === "compreface") {
                    $("#matchesAppTools").show();
                    $("#timelineTools").hide();
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
                if (view === "timeline" || view === "folder" || view === "matches" || view === "person" || view === "compreface") {
                    $("#timelineTools").show();
                } else if (view === "album") {
                    $("#albumTools").show();
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

            if (view === "share") {
                const albumId = $("#albumId").val();
                const albumName = $("#albumName").val();
                const shareLink = $("#shareLink").val();
                const downloadEl = $("#download" + albumId);

                if ($('.bi-circle-fill').length > 0) {
                    $("#clearMultiSelect").show();
                    $("#albumNumberSelected").show();
                    downloadEl.attr("name", "downloadArray");
                    downloadEl.attr("value", JSON.stringify(metadataIdArray));
                    downloadEl.attr("title", "Download selected media");
                } else {
                    shashin.clearAlbumSelection();
                    $("#clearMultiSelect").hide();
                    $("#multiSelectMetadataIds").val("[]");
                    $("#albumNumberSelected").hide();
                    downloadEl.attr("name", "download");
                    downloadEl.attr("value", albumId);
                    downloadEl.attr("title", "Download all photos");
                }

                $("#download"+albumId).on("click", function() {
                    trackShareDownload(albumId,albumName,shareLink);
                });
            }
        });

        $("#photoThumbnailContainer" + metadata.id).hover(function () {
            if (metadata.type.includes("video") && (view !== "timeline" || (view === "timeline" && timelineSettings && timelineSettings.isScrolling === false))) {
                if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                    const gifUrl = $("#image" + metadata.id).attr("src").replace("_225.jpg", "_225.gif");
                    $("#image" + metadata.id).attr("src", gifUrl);
                } else if ($("#tlicon" + metadata.id).attr("class") === "bi-circle-fill") {
                    const jpgUrl = $("#image" + metadata.id).attr("src").replace("_225.gif", "_225.jpg");
                    $("#image" + metadata.id).attr("src", jpgUrl);
                }
            }
        }, function () {
            if (metadata.type.includes("video")) {
                const jpgUrl = $("#image" + metadata.id).attr("src").replace("_225.gif", "_225.jpg");
                $("#image" + metadata.id).attr("src", jpgUrl);
            }
        });

        $("#image" + metadata.id).hover(function () {
            // Only show overlays when scrolling stopped in timeline view
            if (view !== "timeline" || (view === "timeline" && timelineSettings && timelineSettings.isScrolling === false)) {
                shashin.imageHover(this, metadata.id);
            }
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

        function trackShareDownload(albumId,albumName,shareLink) {
            let downloadTimer;
            const tokenName = "ShashinShareAlbumName";
            const tokenSize = "ShashinShareAlbumSize";
            const configuredAttempts = 120;

            shashin.showToastMessage("Downloading share album", "Downloading share album \""+albumName+"\". Downloading photos only.", {icon:"bi-info-circle", iconColor:"#777777", autohide:false});
            setTimeout(function () { $("#download"+albumId).removeAttr("href") }, 0);
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
    }

    shashin.getOverlayData = function(metadata, args) {
        const overlays = [];
        const data = {};

        data["metadata"] = metadata;

        if (metadata.type.includes("video")) {
            overlays.push("isVideo");
            data["duration"] = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
        } else if (metadata.width !== null && metadata.height !== null && metadata.width > metadata.height*2) {
            overlays.push("isPan");
        } else if (metadata.expectedExtension === "gif") {
            overlays.push("isGif");
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
            }
        } else {
            overlays.push("isInfo");
        }

        return {overlays:overlays,data:data};
    }

    shashin.clearTimelineSelection = function () {
        if (shashin.downloadInstance !== null) {
            shashin.downloadInstance.abort();
            shashin.downloadInstance = null;
            shashin.showToastMessage("Download cancelled", "Download cancelled.", {icon:"bi-info-circle", iconColor:"#777777"});
            $("button").find("span").addClass('bi-download').removeClass('spinner-grow');
        }
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
        $("#albumTools").hide();
        $("#albumAppTools").hide();
        $("#matchesAppTools").hide();
        $("#comprefaceAppTools").hide();
    }

    shashin.clearAlbumSelection = function () {
        if (shashin.downloadInstance !== null) {
            shashin.downloadInstance.abort();
            shashin.downloadInstance = null;
            shashin.showToastMessage("Download cancelled", "Download cancelled.", {icon:"bi-info-circle", iconColor:"#777777"});
            $("button").find("span").addClass('bi-download').removeClass('spinner-grow');
        }
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
        $("#timelineTools").hide();
        $("#albumTools").show();
        $("#albumAppTools").hide();
        $("#matchesAppTools").hide();
        $("#comprefaceAppTools").hide();
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
            $("#albumTools").hide();
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
                thumbnailList += TimelineTemplates.BatchHeaderThumbnail({thumbnailImage:$("#thumbnailCentered"+metadataId).val(),title:$("#filename"+metadataId).val().trim(), version: Util.getMetadataLocalStorage()});
            });

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

            const albumcheckedBoxes = $('input[name="albums[]"]');
            const albumNames = [];
            albumcheckedBoxes.each(function() {
                albumNames.push($(this).val().replace(/ +(?= )/g,'').trim());
            });
            shashin.createAutocomplete("#albumNameInput", albumNames, false);
            shashin.syncCheckboxInputs("#albumNameInput", "albums");

            const peoplecheckedBoxes = $('input[name="recognitionLabel[]"]');
            const peopleNames = [];
            peoplecheckedBoxes.each(function() {
                peopleNames.push($(this).val().replace(/ +(?= )/g,'').trim());
            });
            shashin.createAutocomplete("#tagBatchDataInput", peopleNames, false);
            shashin.syncCheckboxInputs("#tagBatchDataInput", "recognitionLabel");

            $("#propBatchMetadata").modal('show');
        });
    }

    // Call in console
    shashin.enableDebug = function (showTrace) {
        shashin.showDebug = true;

        if (showTrace === undefined) {
            showTrace = false;
        }

        shashin.showTrace = showTrace;

        if (Util.localStorageAvailable() === true) {
            localStorage.setItem("showDebug", "on");
        }
    }

    // Call in console
    shashin.disableDebug = function () {
        shashin.showDebug = false;

        if (Util.localStorageAvailable() === true && localStorage.getItem("showDebug") !== null) {
            localStorage.removeItem("showDebug");
        }
    }

    shashin.printMessageToConsole = function (msg) {
        let localStorageDebugFlag = false;
        if (Util.localStorageAvailable() === true && localStorage.getItem("showDebug") !== null && localStorage.getItem("showDebug").length > 0) {
            let getFlag = localStorage.getItem("showDebug")
            if (getFlag === "on") {
                localStorageDebugFlag = true;
            }
        }

        if (shashin.showDebug === true || localStorageDebugFlag === true) {
            console.log(msg);
            if (shashin.showTrace === true) {
                console.trace();
            }
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

    shashin.processBatchAlbumList = function(data, albumInputVal) {
        if (albumInputVal === undefined) {
            albumInputVal = "";
        }
        if (data.hasOwnProperty("allAlbumList") && data["allAlbumList"].length > 0) {
            let renderAlbumList = false;
            const albumList = data["allAlbumList"];
            const albumNames = [];

            let batchHtml =
                '<input type="text" class="form-control" aria-label="Albums Name" id="albumNameInput" name="albumNameInput" value="'+albumInputVal+'">\n' +
                '<div class="input-group-append dropdown">\n' +
                '   <button class="btn btn-secondary dropdown-toggle" id="tagalbumdropdown" type="button" aria-haspopup="true" aria-expanded="false">Albums</button>\n' +
                '   <div class="dropdown-menu" id="albumNameList">\n';

            for (let index in albumList) {
                const album = albumList[index];

                if ($("#"+album.id).length === 0) {
                    renderAlbumList = true;
                }

                batchHtml +=
                    '<button class="dropdown-item" type="button">\n' +
                    '    <input type="checkbox" class="album" id="album'+album.id+'" value="'+Util.escapeHtml(album.name)+'" name="albums[]">\n' +
                    '    <label for="album'+album.id+'">'+Util.escapeHtml(album.name)+'</label>\n' +
                    '</button>\n';

                albumNames.push(Util.escapeHtml(album.name));
            }

            batchHtml +=
                '   </div>\n' +
                '</div>\n';

            if (true === renderAlbumList) {
                shashin.createAutocomplete("#albumNameInput", albumNames, false);
                shashin.syncCheckboxInputs("#albumNameInput", "albums");

                $("#albumListForModal").html(batchHtml);
                $(".album").on("click", function (e) {
                    metadataBatchModal.populateBatchAlbum();
                });

                $("#tagalbumdropdown").on("click", function (e) {
                    e.preventDefault();
                    metadataBatchModal.toggleBatchTagAlbumDropdown();
                });
            }
        }
    }

    shashin.processBatchPeopleList = function(data, subjectInputVal) {
        if (subjectInputVal === undefined) {
            subjectInputVal = "";
        }
        if (data.hasOwnProperty("recognitionLabels") && data["recognitionLabels"].length > 0) {
            let renderRecognitionLabels = false;
            const recognitionLabels = data["recognitionLabels"];
            const recognitionLabelNames = [];

            let batchHtml =
                '       <input type="text" class="form-control" aria-label="Tag People" id="tagBatchDataInput" name="tagBatchDataInput" value="'+subjectInputVal+'">\n' +
                '       <div class="input-group-append">\n' +
                '           <button class="btn btn-secondary dropdown-toggle" id="tagbatchpeopledropdown" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                '           <div class="dropdown-menu" id="peopleNameList">';

            for (let index in recognitionLabels) {
                const recognitionLabel = recognitionLabels[index];

                if ($("#"+recognitionLabel.id).length === 0) {
                    renderRecognitionLabels = true;
                }

                if (recognitionLabel.name !== null && recognitionLabel.name !== "null") {
                    batchHtml +=
                        '           <button class="dropdown-item" type="button">\n' +
                        '               <input type="checkbox" class="recognitionLabel" id="recognitionLabel' + recognitionLabel.id + '" value="' + Util.escapeHtml(recognitionLabel.name) + '" name="recognitionLabel[]">\n' +
                        '               <label for="recognitionLabel' + recognitionLabel.id + '">' + Util.escapeHtml(recognitionLabel.name) + '</label>\n' +
                        '           </button>'

                    recognitionLabelNames.push(recognitionLabel.name);
                }
            }
            batchHtml +=
                '   </div>\n' +
                '</div>\n';

            if (true === renderRecognitionLabels) {
                shashin.createAutocomplete("#tagBatchDataInput", recognitionLabelNames, false);
                shashin.syncCheckboxInputs("#tagBatchDataInput", "recognitionLabel");

                $("#batchLabelIds").html(batchHtml);
                $("#tagBatchDataInput").on("focus", function (e) {
                    e.preventDefault();
                    metadataBatchModal.closeBatchTagPeopleDropdown();
                });
                $("#tagbatchpeopledropdown").on("click", function (e) {
                    e.preventDefault();
                    metadataBatchModal.toggleBatchTagPeopleDropdown();
                });
                $(".recognitionLabel").on("click", function (e) {
                    metadataBatchModal.populateBatchLabel();
                });
            }
        }
    }
}( window.shashin = window.shashin || {}, jQuery ));

if (typeof module !== 'undefined') {
    module.exports = window.shashin;
}