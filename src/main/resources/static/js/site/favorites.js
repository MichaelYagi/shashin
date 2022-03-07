class Favorites {

    constructor() {
        this.rendering = false;
    }

    updateFavorites(nextPage,activePage) {
        this.rendering = true;
        $("#spinner").css("display","block");

        const ajaxParams = {
            type: 'get',
            url: "/favorites/" + nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        // Get paged results
        return $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating favorites")}).then(function (data) {
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList")) {
                        const metadataList = data["metadataList"];
                        const keywordMap = data["keywordMap"];

                        let html = "";

                        if (metadataList.length > 0) {
                            const mediaLinkLength = $(".mediaLink").length;

                            for (const index in metadataList) {
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                const metadata = metadataList[index];

                                const dateHeadingCount = $(".dateSection").length;
                                const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                                const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                                const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                                if (lastDateHeading !== currentDate) {
                                    html += '<section class="dateSection" id="'+currentDate+'"><p><strong>' + displayCurrentDate + '</strong></p></section>\n';
                                }

                                html += '<div id="photoThumbnailContainer' + metadata.id + '" class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex' + currentMediaLinkIndex + '"></a>\n' +
                                    '   <img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\'' + metadata.title + '\',Util.thumbnailHeight())">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                                html += shashin.getTopLeftOverlay(metadata.id);

                                html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);

                                const centeredObj = shashin.getCenteredOverlay(metadata, 'shashin.openGallery', currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                $(html).insertBefore($(".appendMetadataPhotos").last()).ready(function () {
                                    this.rendering = false;
                                });

                                shashin.setPhotoOverlays(metadata, activePage);

                                $("#mediaLink" + metadata.id).attr("tag", metadata.id);
                                $("#infoModalEdit" + metadata.id).click(function (e) {
                                    e.preventDefault();
                                    shashin.openInfoModal(metadata.id);
                                });

                                $("#image" + metadata.id).on('load', function () {
                                    $(this).css("background-color", "transparent");
                                });

                                html = "";
                            }
                        } else {
                            $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                            this.rendering = false;
                        }
                    }
                } else {
                    $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                    this.rendering = false;
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                this.rendering = false;
            }

            $("#spinner").css("display","none");
            return mediaContentList;
        });
    }
}