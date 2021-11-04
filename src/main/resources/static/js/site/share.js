class ShareAlbum {
    constructor(shareLink) {
        this.shareLink = shareLink;
    }

    getShareLink() {
        return this.shareLink;
    }

    updateAlbum(albumId, nextPage, activePage) {
        const self = this;
        const shashinUtil = new ShashinUtil();

        const promise = $.ajax({
            type: 'get',
            url: "/share/"+self.getShareLink()+"/album/"+albumId+"/"+nextPage,
            contentType: 'application/json; charset=utf-8'
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating share album. Attempt: "+shashinUtil.getTryCount()+"/"+shashinUtil.getRetryLimit()+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                shashinUtil.setTryCount(shashinUtil.getTryCount()+1);

                if (shashinUtil.getTryCount() <= shashinUtil.getRetryLimit()) {
                    //try again
                    self.updateAlbum(albumId, nextPage, activePage);
                }
            }
        }).then(function (data) {
            const mediaContentList = [];

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("albumMetadataList")) {
                        const albumMetadataList = data["albumMetadataList"] === "" ? [] : data["albumMetadataList"];
                        const mediaLinkLength = $(".mediaLink").length;

                        for (const index in albumMetadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = albumMetadataList[index];

                            let html =
                                '<div class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" class="photo-thumbnail-image" id="image' + metadata.id + '" onError="shashin.errorImg(this,\'' + metadata.title + '\',209)">\n';

                            const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                            html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                            const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                            html += centeredObj.html;
                            mediaContentList.push(centeredObj.mediaContent);

                            html += '</div>\n<span class="appendAlbumPhotos" style="width:0;height:0;padding:0"></span>\n';
                            $(html).insertAfter($(".appendAlbumPhotos").last())

                            // Call JS and modal
                            shashin.setPhotoOverlays(metadata, activePage);
                        }
                    } else {
                        $(".appendAlbumPhotos").last().text("EOL").css("display","none")
                    }
                } else {
                    $(".appendAlbumPhotos").last().text("EOL").css("display","none")
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendAlbumPhotos").last().text("EOL").css("display","none")
            }

            return mediaContentList;
        });

        return promise.done(function(data) {
            return data;
        });
    }
}