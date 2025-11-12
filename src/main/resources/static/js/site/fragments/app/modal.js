(function( shashin, $, undefined ) {
    shashin.modalStatusFailMessage = function() {
        return shashin.getTranslatedValue("main.message.pta");
    };

    shashin.openEditMetadataModal = function (metadataId) {
        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.media.info.title"), shashin.getTranslatedValue("main.toast.media.info.body"), {
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

                if (metadata.duplicateHash !== null) {
                    $("#ignoreduplicates")[0].checked = false;
                } else {
                    $("#ignoreduplicates")[0].checked = true;
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
}( window.shashin = window.shashin || {}, jQuery ));