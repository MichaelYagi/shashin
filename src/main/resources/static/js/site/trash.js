(function( trashSettings, $, undefined ) {
    trashSettings.infiniteScrollGallery = null;
    trashSettings.lg = null;
    trashSettings.lightGalleryConfigs = shashin.getLightGalleryConfigs();
    trashSettings.lightGalleryConfigs["dynamic"] = true;
    trashSettings.retryLimit = 3;
    trashSettings.tryCount = 0;

    trashSettings.setLightGalleryElement = function (name) {
        trashSettings.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            trashSettings.infiniteScrollGallery = document.getElementById(name);
        }
    };

    trashSettings.setLightGallery = function () {
        trashSettings.lg = lightGallery(trashSettings.getLightGalleryElement(), trashSettings.lightGalleryConfigs);
    }

    trashSettings.getLightGalleryElement = function () {
        return trashSettings.infiniteScrollGallery;
    };

    trashSettings.getLightGallery = function () {
        return trashSettings.lg;
    }
    
    trashSettings.openGallery = function (e, index) {
        e.preventDefault();
        if (trashSettings.getLightGallery() !== null) {
            trashSettings.getLightGallery().openGallery(index);
        }
    }

    trashSettings.updateTrash = function (nextPage, activePage) {
        // Get paged results
        const promise = $.ajax({
            type: 'get',
            url: "/trash/" + nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating trash. Attempt: "+trashSettings.tryCount+"/"+trashSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                trashSettings.tryCount++;
                if (trashSettings.tryCount <= trashSettings.retryLimit) {
                    //try again
                    trashSettings.updateTrash(nextPage, activePage);
                }
            }
        }).then(function (data) {
            trashSettings.tryCount = 0;
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList")) {
                        const metadataList = data["metadataList"] === "" ? null : data["metadataList"];
                        let html = "";

                        if (metadataList.length > 0) {
                            const mediaLinkLength = $(".mediaLink").length;

                            for (const index in metadataList) {
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                const metadata = metadataList[index];

                                html += '<div class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex' + currentMediaLinkIndex + '"></a>\n' +
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="shashin.errorImg(this,\''+metadata.title+'\',209)">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.renderTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                                html += shashin.renderTopLeftOverlay(metadata.id);

                                html += shashin.renderBottomLeftOverlay(metadata.id, null, null, null, null);

                                const centeredObj = shashin.renderCenteredOverlay(metadata,'trashSettings.openGallery',currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                html += '</div>\n';

                                $(html).insertBefore($(".appendMetadataPhotos").last())

                                shashin.setPhotoOverlays(metadata, activePage);

                                $("#infoModalEdit"+metadata.id).attr("tag",JSON.stringify(metadata));
                                $("#infoModalEdit"+metadata.id).click(function(e) {
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
                            $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                        }
                    }
                } else {
                    $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
            }

            return mediaContentList;
        });

        return promise.done(function (data) {
            return data;
        });
    }
}( window.trashSettings = window.trashSettings || {}, jQuery ));