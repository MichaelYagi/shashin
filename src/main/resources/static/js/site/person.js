(function( personSettings, $, undefined ) {
    personSettings.infiniteScrollGallery = null;
    personSettings.lg = null;
    personSettings.lightGalleryConfigs = shashin.getLightGalleryConfigs();
    personSettings.lightGalleryConfigs["dynamic"] = true;
    personSettings.retryLimit = 3;
    personSettings.tryCount = 0;

    personSettings.setLightGalleryElement = function (name) {
        personSettings.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            personSettings.infiniteScrollGallery = document.getElementById(name);
        }
    };

    personSettings.setLightGallery = function () {
        personSettings.lg = lightGallery(personSettings.getLightGalleryElement(), personSettings.lightGalleryConfigs);
    }

    personSettings.getLightGalleryElement = function () {
        return personSettings.infiniteScrollGallery;
    };

    personSettings.getLightGallery = function () {
        return personSettings.lg;
    }

    personSettings.openGallery = function (e,index) {
        e.preventDefault();
        if (personSettings.getLightGallery() !== null) {
            personSettings.getLightGallery().openGallery(index);
        }
    }

    personSettings.activateMetadataListeners = function (metadata) {
        shashin.printMessageToConsole(metadata.id);

        $("#image"+metadata.id).on('load', function() {
            $(this).css("background-color","transparent");
        });
    }

    personSettings.updatePerson = function (personId,nextPage,activePage) {
        // Get paged results
        const promise = $.ajax({
            type: 'get',
            url: "/person/" + personId + "/" + nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating person. Attempt: " + personSettings.tryCount + "/" + personSettings.retryLimit + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                personSettings.tryCount++;
                if (personSettings.tryCount <= personSettings.retryLimit) {
                    //try again
                    personSettings.updatePerson(personId, nextPage, activePage);
                }
            }
        }).then(function (data) {
            personSettings.tryCount = 0;
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
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="shashin.errorImg(this,\'' + metadata.title + '\',209)">\n' +
                                    '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n' +
                                    '   <input type="hidden" name="thumbnailCentered' + metadata.id + '" id="thumbnailCentered' + metadata.id + '" th:value="' + metadata.thumbnailUrlCentered + '">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.renderTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight);

                                if (currentUser.authority === "ROLE_ADMIN") {
                                    if (labelPhotoMap[metadata.id]["isTagged"] === true) {
                                        html +=
                                            '<div class="thumbnail-br" id="tntr' + metadata.id + '">\n' +
                                            '   <span class="bi-bookmark-fill overlayIconBackground" style="font-size: 1rem;color: lightsalmon;"></span>\n' +
                                            '</div>\n';
                                    }

                                    html += shashin.renderTopLeftOverlay(metadata.id);
                                    html += shashin.renderBottomLeftOverlay(metadata.id, 'propperson', null, null, null);
                                } else {
                                    html += shashin.renderBottomLeftOverlay(metadata.id, null, null, null, null);
                                }

                                const centeredObj = shashin.renderCenteredOverlay(metadata, 'personSettings.openGallery', currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                html += '</div>\n';

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
}( window.personSettings = window.personSettings || {}, jQuery ));