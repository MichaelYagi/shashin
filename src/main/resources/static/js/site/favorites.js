(function( favoritesSettings, $, undefined ) {
    favoritesSettings.updateFavorites = function (nextPage,activePage) {
        const shashinUtil = new ShashinUtil();

        // Get paged results
        const promise = $.ajax({
            type: 'get',
            url: "/favorites/" + nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating favorites. Attempt: " + shashinUtil.getTryCount() + "/" + ShashinUtil.getRetryLimit() + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                shashinUtil.setTryCount(shashinUtil.getTryCount()+1);

                if (shashinUtil.getTryCount() <= ShashinUtil.getRetryLimit()) {
                    //try again
                    favoritesSettings.updateFavorites(nextPage, activePage);
                }
            }
        }).then(function (data) {
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
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="shashin.errorImg(this,\'' + metadata.title + '\',209)">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                                html += shashin.getTopLeftOverlay(metadata.id);

                                html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);


                                html += '<div class="thumbnail-centered" id="tncentered' + metadata.id + '">\n';

                                const centeredObj = shashin.getCenteredOverlay(metadata, 'shashin.openGallery', currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                html += '</div>\n';

                                $(html).insertBefore($(".appendMetadataPhotos").last())

                                shashin.setPhotoOverlays(metadata, activePage);

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

        return promise.done(function(data) {
            return data;
        });
    }
}( window.favoritesSettings = window.favoritesSettings || {}, jQuery ));