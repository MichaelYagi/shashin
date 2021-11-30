class Trash {
    static updateTrash(nextPage, activePage) {
        const ajaxParams = {
            type: 'get',
            url: "/trash/" + nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        // Get paged results
        const promise = $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating trash")}).then(function (data) {
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
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\''+metadata.title+'\',209)">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                                html += shashin.getTopLeftOverlay(metadata.id);

                                html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);

                                const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                html += '</div>\n';

                                $(html).insertBefore($(".appendMetadataPhotos").last())

                                shashin.setPhotoOverlays(metadata, activePage);

                                $("#mediaLink"+metadata.id).attr("tag",JSON.stringify(metadata));
                                $("#infoModalEdit"+metadata.id).click(function(e) {
                                    e.preventDefault();
                                    const metadataObj = JSON.parse($("#mediaLink"+metadata.id).attr("tag"));
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
}