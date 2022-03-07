class Search {

    constructor() {
        this.rendering = false;
    }

    updateSearch(nextPage,searchTerm,activePage) {
        this.rendering = true;
        $("#spinner").css("display","block");

        const ajaxParams = {
            type: 'get',
            url: "/search/"+nextPage+"?searchTerm="+encodeURIComponent(searchTerm),
            contentType: 'application/json; charset=utf-8',
            async:true,
            retries: shashin.ajaxRetries
        }

        return $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating search")}).then(function (data) {
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("metadataSearchList") && data["status"] === "success") {
                const metadataList = data["metadataSearchList"];
                const keywordMap = data["keywordMap"];

                if (metadataList !== null && metadataList.length > 0) {
                    let html = "";
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

                        html += '<div id="photoThumbnailContainer' + metadata.id + '" class="photo-thumbnail-container photo-thumbnail" style="width:'+metadata.thumbnailSmallWidth+'px;height:'+metadata.thumbnailSmallHeight+'px;padding-left:0;padding-right:0;">\n';
                        html +=
                            '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+currentMediaLinkIndex+'"></a>\n' +
                            '   <img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\''+metadata.title+'\',Util.thumbnailHeight())">\n' +
                            '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n';

                        html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);

                        const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                        html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                        const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                        html += centeredObj.html;
                        mediaContentList.push(centeredObj.mediaContent);

                        html += '</div>\n<span class="appendSearchPhotos" style="width:0;height:0;padding:0"></span>\n';
                        $(html).insertAfter($(".appendSearchPhotos").last()).ready(function () {
                            this.rendering = false;
                        });

                        shashin.setPhotoOverlays(metadata, activePage);
                        Util.activateMetadataListeners(metadata);
                        $("#mediaLink" + metadata.id).attr("tag", metadata.id);
                        $("#infoModalEdit"+metadata.id).click(function(e) {
                            e.preventDefault();
                            shashin.openInfoModal(metadata.id);
                        });

                        html = "";
                    }
                } else {
                    $(".appendSearchPhotos").last().text("EOL").css("display","none")
                    this.rendering = false;
                }
            } else {
                $(".appendSearchPhotos").last().text("EOL").css("display","none")
                this.rendering = false;
            }

            $("#spinner").css("display","none");
            return mediaContentList;
        });
    }
}