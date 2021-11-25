class Person {
    static updatePerson(personId,nextPage,activePage) {
        const ajaxParams = {
            type: 'get',
            url: "/person/" + personId + "/" + nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating person. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        // Get paged results
        const promise = $.ajax(ajaxParams)
        .fail(onFail).then(function (data) {
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList")) {
                        const metadataList = data["metadataList"] === "" ? [] : data["metadataList"];
                        const recognitionLabels = data["recognitionLabels"] === "" ? "" : data["recognitionLabels"];
                        const labelPhotoMap = data["labelPhotoMap"] === "" ? null : data["labelPhotoMap"];
                        const currentUser = data["currentUser"] === "" ? null : data["currentUser"];

                        let html = "";

                        if (metadataList.length > 0) {
                            const mediaLinkLength = $(".mediaLink").length;
                            for (const index in metadataList) {
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                const metadata = metadataList[index];

                                html += '<div class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex' + currentMediaLinkIndex + '"></a>\n' +
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\'' + metadata.title + '\',209)">\n' +
                                    '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n' +
                                    '   <input type="hidden" name="thumbnailCentered' + metadata.id + '" id="thumbnailCentered' + metadata.id + '" th:value="' + metadata.thumbnailUrlCentered + '">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, (currentUser.authority === "ROLE_ADMIN" && labelPhotoMap[metadata.id]["isTagged"] === true));

                                if (currentUser.authority === "ROLE_ADMIN") {
                                    html += shashin.getTopLeftOverlay(metadata.id);
                                    html += shashin.getBottomLeftOverlay(metadata.id, 'propperson', null, null, null);
                                } else {
                                    html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);
                                }

                                const centeredObj = shashin.getCenteredOverlay(metadata, 'shashin.openGallery', currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                html += '</div>\n<span id="personmodal'+metadata.id+'" style="width:0;height:0;padding:0"></span>\n';

                                $(html).insertBefore($(".appendPersonPhotos").last())

                                shashin.setPhotoOverlays(metadata, activePage);
                                personModalSettings.renderPersonModal(metadata, recognitionLabels, labelPhotoMap[metadata.id]["labels"]);
                                $("#infoModalEdit" + metadata.id).attr("tag", JSON.stringify(metadata));
                                $("#infoModalEdit" + metadata.id).click(function (e) {
                                    e.preventDefault();
                                    const metadataObj = JSON.parse($(this).attr("tag"));
                                    shashin.openInfoModal(metadataObj);
                                });

                                $("#image" + metadata.id).on('load', function () {
                                    $(this).css("background-color", "transparent");
                                });

                                html = "";
                            }
                        } else {
                            $(".appendPersonPhotos").last().text("EOL").css("display", "none")
                        }
                    }
                } else {
                    $(".appendPersonPhotos").last().text("EOL").css("display", "none")
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendPersonPhotos").last().text("EOL").css("display", "none")
            }

            return mediaContentList;
        });

        return promise.done(function(data) {
            return data;
        });
    }
}