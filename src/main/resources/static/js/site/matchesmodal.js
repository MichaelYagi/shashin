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
        $("#tagBatchDataInput").val(shashin.decodeHtml(labelString));
    }
}(window.matchModalBatchSettings = window.matchModalBatchSettings || {}, jQuery));


$("#batchisobject").click(function (e) {
    matchModalBatchSettings.closeBatchTagPeopleDropdown();
    if ($(this).prop("checked") === true) {
        $("#tagBatchDataInput").val("");
    }
});

$("#saveBatchMetadata").click(function (e) {
    e.preventDefault();
    matchModalBatchSettings.closeBatchTagPeopleDropdown();
    $("#matchesBatchModalStatus").css("visibility","visible");

    const shashinUtil = new ShashinUtil();
    const ajaxParams = {
        type: "post",
        url: "/timeline/update/batch",
        data: JSON.stringify($('#saveBatchData').serializeObject()),
        contentType: 'application/json; charset=utf-8'
    }

    $.ajax(ajaxParams)
    .fail(function (xhr, textStatus) {
        shashin.printMessageToConsole("AJAX error saving persons matches. Attempt: "+shashinUtil.getTryCount() + "/" + shashinUtil.getRetryLimit()+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
        if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
            shashinUtil.setTryCount(shashinUtil.getTryCount()+1);

            if (shashinUtil.getTryCount() <= shashinUtil.getRetryLimit()) {
                //try again
                $.ajax(ajaxParams);
            }
        }
    }).then(function (data) {
        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === "success") {
                message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                // window.top.location = window.top.location

                $("#matchesBatchModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
            } else {
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';

                $("#matchesBatchModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
            }
            //$("#msgBatchMetadata").html(message);
            shashin.clearTimelineSelection();
        }
    });

});

// Clear message on modal close
$('#propBatchMetadata').on('hide.bs.modal', function () {
    $("#matchesBatchModalStatus").attr("class","spinner-grow me-auto");
    $("#matchesBatchModalStatus").css("visibility","hidden");
    $("#msgBatchMetadata").html("");
    $('#tagBatchDataInput').val('');
    $('input:checkbox').prop('checked', false);
    matchModalBatchSettings.closeBatchTagPeopleDropdown();
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
        $("#tagpeople" + metadataId).val(shashin.decodeHtml(labelString));
    }

    matchModalSettings.renderMatchesModal = function(metadata, recognitionLabels, taggedPeopleList) {
        taggedPeopleList = taggedPeopleList.replaceAll("&quot;", "");
        shashin.printMessageToConsole('taggedPeopleList:'+taggedPeopleList)

        let html = '<div class="modal fade" id="propmatches' + metadata.id + '" tabindex="-1" role="dialog" aria-labelledby="label' + metadata.id + '" aria-hidden="true"><div class="modal-dialog modal-lg" role="document"><div class="modal-content">\n' +
            '<div class="modal-header"><h5 class="modal-title" id="exampleModalLabel">Edit ' + metadata.fileName + '<div id="propmatchesThumbnail' + metadata.id + '"><img src="' + encodeURI(metadata.thumbnailUrlCentered) + '" width="100" height="100" onError="shashin.errorImg(this,\''+metadata.title+'\',100)"></div></h5>\n' +
            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>\n' +
            '<div class="modal-body">\n' +
            '   <form th:id="saveData' + metadata.id + '">\n' +
            '       <input type="hidden" name="id" value="' + metadata.id + '">\n' +
            '       <div class="form-group row">\n' +
            '           <div class="col-sm">\n' +
            '               <label for="tagpeople' + metadata.id + '" class="col-form-label">Tag People (comma separated)</label>\n' +
            '               <div class="input-group">\n' +
            '                   <input type="text" onfocus="return matchModalSettings.closeTagPeopleDropdown(\'' + metadata.id + '\');" class="form-control" aria-label="Tag People" id="tagpeople' + metadata.id + '" name="tagpeople" value="' + taggedPeopleList + '">\n';
        if (recognitionLabels.length > 0) {
            html += '           <div class="input-group-append">\n' +
                '                   <button class="btn btn-outline-secondary dropdown-toggle" onclick="return matchModalSettings.toggleTagPeopleDropdown(\'' + metadata.id + '\');" id="tagpeopledropdown' + metadata.id + '" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                '                   <div class="dropdown-menu" id="recognitionLabelsList' + metadata.id + '">\n';
            for (const index in recognitionLabels) {
                const recognitionLabel = recognitionLabels[index];
                const taggedPeopleArray = taggedPeopleList.split(",");
                let checkedString = "";
                if ($.inArray(recognitionLabel.name, taggedPeopleArray) !== -1) {
                    checkedString = " checked";
                }
                html += '               <button class="dropdown-item" type="button">\n' +
                    '                       <input type="checkbox" onclick="return matchModalSettings.populateLabel(\'' + metadata.id + '\',' + recognitionLabel.id + ');" value="' + recognitionLabel.name + '" name="recognitionLabel' + metadata.id + '[]" id="' + metadata.id + '-' + recognitionLabel.id + '"' + checkedString + '>\n' +
                    '                       <label for="' + metadata.id + '-' + recognitionLabel.id + '" id="label-' + metadata.id + '-' + recognitionLabel.id + '">' + recognitionLabel.name + '</label>\n' +
                    '                   </button>\n';
            }
            html += '               </div>\n' +
                '               </div>\n';

        }
        html +=
            '               </div>\n' +
            '           </div>\n' +
            '       </div>\n' +
            '       <div class="form-group row">\n' +
            '           <div class="col-sm">\n' +
            '               <input type="checkbox" name="isobject' + metadata.id + '" id="isobject' + metadata.id + '">\n' +
            '               <label class="form-check-label" for="isobject' + metadata.id + '">This is not a person</label>\n' +
            '           </div>\n' +
            '       </div>\n' +
            '   </form>\n';
        html +=
            '   <div class="col-sm">\n' +
            '       <span id="msg' + metadata.id + '"></span>\n' +
            '   </div>\n' +
            '</div>\n' +
            '<div class="modal-footer">\n' +
            '   <div id="matchesModalStatus'+metadata.id+'" class="spinner-grow me-auto" style="visibility: hidden;font-size: 2rem;" role="status" aria-hidden="true"></div>' +
            '   <button type="button" class="btn btn-primary" id="saveMetadata' + metadata.id + '">Save</button>\n' +
            '   <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>\n' +
            '</div></div></div></div>';

        $("#matchesmodal" + metadata.id).after(html);

        $("#isobject" + metadata.id).click(function (e) {
            matchModalSettings.closeTagPeopleDropdown(metadata.id);

            if ($(this).prop("checked") === true) {
                $("#tagpeople" + metadata.id).val("");
            }
        });

        $("#saveMetadata" + metadata.id).click(function (e) {
            e.preventDefault();
            matchModalSettings.closeTagPeopleDropdown(metadata.id);
            $("#matchesModalStatus"+metadata.id).css("visibility","visible");

            const shashinUtil = new ShashinUtil();
            const json = {
                metadataId: metadata.id,
                tagpeople: $("#tagpeople" + metadata.id).val(),
                isObject: $("#isobject" + metadata.id).prop("checked")
            };
            const ajaxParams = {
                type: "post",
                url: "/person/update",
                data: JSON.stringify(json),
                contentType: 'application/json; charset=utf-8'
            }

            $.ajax(ajaxParams)
            .fail(function (xhr, textStatus) {
                shashin.printMessageToConsole("AJAX error saving person matches. Attempt: "+shashinUtil.getTryCount() + "/" + shashinUtil.getRetryLimit()+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
                if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                    shashinUtil.setTryCount(shashinUtil.getTryCount()+1);

                    if (shashinUtil.getTryCount() <= shashinUtil.getRetryLimit()) {
                        //try again
                        $.ajax(ajaxParams);
                    }
                }
            }).then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let message = "Error";
                    if (data["status"] === "success") {
                        message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        // window.top.location = window.top.location
                        $("#matchesModalStatus"+metadata.id).addClass('bi-check-circle').removeClass('spinner-grow');
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        $("#matchesModalStatus"+metadata.id).addClass('bi-x-circle').removeClass('spinner-grow');
                    }
                    //$("#msg" + metadata.id).html(message);
                }
            });

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