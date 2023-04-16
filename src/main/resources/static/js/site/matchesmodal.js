(function (matchModalBatchSettings, $, undefined) {

    matchModalBatchSettings.toggleBatchTagPeopleDropdown = function () {
        $("#tagpeopledropdown").dropdown('toggle');
    }

    matchModalBatchSettings.closeBatchTagPeopleDropdown = function () {
        $("#tagpeopledropdown").dropdown('hide');
    }

    matchModalBatchSettings.populateBatchLabel = function () {
        const checkedBoxes = $('input[name="recognitionLabel[]"]:checked');
        let labelString = "";
        checkedBoxes.each(function () {
            labelString += $(this).val() + ",";
        });
        if (labelString.length > 0) {
            labelString = labelString.slice(0, -1)
        }
        $("#tagBatchDataInput").val(Util.decodeHtml(labelString));
    }
}(window.matchModalBatchSettings = window.matchModalBatchSettings || {}, jQuery));


$("#batchisobject").on("click", function (e) {
    matchModalBatchSettings.closeBatchTagPeopleDropdown();
    if ($(this).prop("checked") === true) {
        $("#tagBatchDataInput").val("");
    }
});

$("#saveBatchMetadata").on("click", async function (e) {
    e.preventDefault();

    matchModalBatchSettings.closeBatchTagPeopleDropdown();
    $("#matchesBatchModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
    $("#matchesBatchModalStatus").css("visibility", "visible");
    $("#matchesBatchModalStatus").attr("title", "");
    $("#matchesBatchModalCancel").prop('disabled', true);

    const http = new Http("saving persons matches");
    const batchObj = Util.serializeObject($('#saveBatchData'));
    const data = await http.ajax("post", "/timeline/update/batch", JSON.stringify(Util.getBatchData(batchObj)), function () {
        $("#matchesBatchModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
        $("#matchesBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
        $("#matchesBatchModalCancel").prop('disabled', false);
    });

    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
        if (data["status"] === "success") {
            if (data.hasOwnProperty("keywords") && data["keywords"] !== "") {
                $("#keywordsString").val(data["keywords"]);
                $("#keywordsBatchString").val(data["keywords"]);
            }

            if (data.hasOwnProperty("cameras") && data["cameras"] !== "") {
                $("#camerasString").val(data["cameras"]);
                $("#camerasBatchString").val(data["cameras"]);
            }

            if (data.hasOwnProperty("recognitionLabels") && data["recognitionLabels"].length > 0) {
                let renderRecognitionLabels = false;
                let batchHtml = "";
                const recognitionLabels = data["recognitionLabels"];

                const dummyMetadata = {};
                dummyMetadata.id = "";

                for (let index in recognitionLabels) {
                    const recognitionLabel = recognitionLabels[index];

                    if ($("#" + recognitionLabel.id).length === 0) {
                        renderRecognitionLabels = true;
                    }

                    batchHtml += ModalTemplates.PersonModalDropDown({
                        metadata: dummyMetadata,
                        recognitionLabel: recognitionLabel,
                        checkedString: ""
                    });
                }

                if (true === renderRecognitionLabels) {
                    $("#peopleNameList").html(batchHtml);
                    $(".recognitionLabel").on("click", function (e) {
                        matchModalBatchSettings.populateBatchLabel();
                    });
                }
            }

            $("#matchesBatchModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
            $("#matchesBatchModalCancel").prop('disabled', false);
        } else {
            $("#matchesBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            $("#matchesBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
            $("#matchesBatchModalCancel").prop('disabled', false);
        }
    } else {
        $("#matchesBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
        $("#matchesBatchModalStatus").attr("title", shashin.modalStatusFailMessage());
        $("#matchesBatchModalCancel").prop('disabled', false);
    }
});

// Clear message on modal close
$('#propBatchMetadata').on('hide.bs.modal', function () {
    $("#matchesBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#matchesBatchModalStatus").css("visibility","hidden");
    $("#msgBatchMetadata").html("");
    $('#tagBatchDataInput').val('');
    $('input:checkbox').prop('checked', false);
    matchModalBatchSettings.closeBatchTagPeopleDropdown();
    shashin.clearTimelineSelection();
});

// Clear message on input editing
$('#propBatchMetadata').bind('keypress', function () {
    $("#matchesBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#matchesBatchModalStatus").css("visibility","hidden");
    $("#msgBatchMetadata").html("");
});

(function (matchModalSettings, $, undefined) {
    matchModalSettings.toggleTagPeopleDropdown = function(metadataId) {
        $("#tagpeopledropdown" + metadataId).dropdown('toggle')
    }

    matchModalSettings.closeTagPeopleDropdown = function(metadataId) {
        $("#tagpeopledropdown" + metadataId).dropdown('hide')
    }

    matchModalSettings.populateLabel = function(metadataId) {
        const checkedBoxes = $('input[name="recognitionLabel' + metadataId + '[]"]:checked');
        let labelString = "";
        checkedBoxes.each(function () {
            labelString += $(this).val() + ",";
        });
        if (labelString.length > 0) {
            labelString = labelString.slice(0, -1)
        }
        $("#tagpeople" + metadataId).val(Util.decodeHtml(labelString));
    }

    matchModalSettings.renderMatchesModal = function(metadata, recognitionLabels, taggedPeopleList) {
        taggedPeopleList = taggedPeopleList.replaceAll("&quot;", "");
        shashin.printMessageToConsole('taggedPeopleList:'+taggedPeopleList)

        let html = ModalTemplates.PersonModalHead({module:"matches",metadata:metadata,recognitionLabels:recognitionLabels,taggedPeopleList:taggedPeopleList});

        if (recognitionLabels.length > 0) {
            for (const index in recognitionLabels) {
                const recognitionLabel = recognitionLabels[index];
                const taggedPeopleArray = taggedPeopleList.split(",");
                let checkedString = "";
                if ($.inArray(recognitionLabel.name, taggedPeopleArray) !== -1) {
                    checkedString = " checked";
                }
                html += ModalTemplates.PersonModalDropDown({metadata:metadata,recognitionLabel:recognitionLabel,checkedString:checkedString});
            }
        }
        html += ModalTemplates.PersonModalFooter({module:"person",metadata:metadata,recognitionLabels:recognitionLabels});

        $("#matchesmodal" + metadata.id).after(html);

        $("#tagpeople").on("focus", function (e) {
            e.preventDefault();
            matchModalSettings.closeTagPeopleDropdown(metadata.id);
        });

        $("#tagpeopledropdown" + metadata.id).on("click", function (e) {
            e.preventDefault();
            matchModalSettings.toggleTagPeopleDropdown(metadata.id);
        });

        $(".recognitionLabel").on("click", function (e) {
            matchModalSettings.populateLabel(metadata.id);
        });

        $("#isobject" + metadata.id).on("click", function (e) {
            matchModalSettings.closeTagPeopleDropdown(metadata.id);

            if ($(this).prop("checked") === true) {
                $("#tagpeople" + metadata.id).val("");
            }
        });

        $("#saveMetadata" + metadata.id).on("click", async function (e) {
            e.preventDefault();

            matchModalSettings.closeTagPeopleDropdown(metadata.id);
            $("#matchesModalStatus" + metadata.id).removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#matchesModalStatus" + metadata.id).css("visibility", "visible");
            $("#matchesModalStatus" + metadata.id).attr("title", "");
            $("#matchesModalCancel" + metadata.id).prop('disabled', true);

            const http = new Http("saving person matches");
            const json = {
                metadataId: metadata.id,
                tagpeople: $("#tagpeople" + metadata.id).val(),
                isObject: $("#isobject" + metadata.id).prop("checked")
            };
            const data = await http.ajax("post", "/person/update", JSON.stringify(json), function () {
                $("#matchesModalStatus" + metadata.id).removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#matchesModalStatus" + metadata.id).attr("title", shashin.modalStatusFailMessage());
                $("#matchesModalCancel" + metadata.id).prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("recognitionLabels") && data["recognitionLabels"].length > 0) {
                        let renderRecognitionLabels = false;
                        const recognitionLabels = data["recognitionLabels"];

                        let batchHtml = "";

                        for (let index in recognitionLabels) {
                            const recognitionLabel = recognitionLabels[index];

                            if ($("#" + metadata.id + '-' + recognitionLabel.id).length === 0) {
                                renderRecognitionLabels = true;
                            }

                            const taggedPeopleArray = $("#tagpeople" + metadata.id).val().split(",");
                            let checkedString = "";
                            if ($.inArray(recognitionLabel.name, taggedPeopleArray) !== -1) {
                                checkedString = " checked";
                            }
                            batchHtml += ModalTemplates.PersonModalDropDown({
                                metadata: metadata,
                                recognitionLabel: recognitionLabel,
                                checkedString: checkedString
                            });
                        }

                        if (true === renderRecognitionLabels) {
                            $(".dropdown-menu, .personDropdown").html(batchHtml);
                            $(".recognitionLabel").on("click", function (e) {
                                personModalSettings.populateLabel(metadata.id);
                            });
                        }
                    }

                    $("#matchesModalStatus" + metadata.id).addClass('bi-check-circle').removeClass('spinner-grow');
                    $("#matchesModalCancel" + metadata.id).prop('disabled', false);
                } else {
                    $("#matchesModalStatus" + metadata.id).addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#matchesModalStatus" + metadata.id).attr("title", shashin.modalStatusFailMessage());
                    $("#matchesModalCancel" + metadata.id).prop('disabled', false);
                }
            } else {
                $("#matchesModalStatus" + metadata.id).addClass('bi-x-circle').removeClass('spinner-grow');
                $("#matchesModalStatus" + metadata.id).attr("title", shashin.modalStatusFailMessage());
                $("#matchesModalCancel" + metadata.id).prop('disabled', false);
            }

            return false;
        });

        // Clear message on modal close
        $('#propmatches' + metadata.id).on('hide.bs.modal', function () {
            $("#matchesModalStatus" + metadata.id).attr("class","spinner-grow me-auto");
            $("#matchesModalStatus" + metadata.id).css("visibility","hidden");
            $("#msg" + metadata.id).html("");
            $("#isobject" + metadata.id)[0].checked = false;
        });

        // Clear message on input editing
        $('#propmatches' + metadata.id + ' input').bind('keypress', function () {
            $("#matchesModalStatus" + metadata.id).attr("class","spinner-grow me-auto");
            $("#matchesModalStatus" + metadata.id).css("visibility","hidden");
            $("#msg" + metadata.id).html("");
        });
    }
}(window.matchModalSettings = window.matchModalSettings || {}, jQuery));