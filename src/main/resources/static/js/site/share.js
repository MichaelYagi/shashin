(function( shareAlbumSettings, $, undefined ) {
    shareAlbumSettings.retryLimit = 3;
    shareAlbumSettings.tryCount = 0;
    shareAlbumSettings.shareLink = '';

    shareAlbumSettings.setShareLink = function (shareLink) {
        shareAlbumSettings.shareLink = shareLink;
    }

    shareAlbumSettings.updateAlbum = function (albumId, nextPage, activePage) {
        return $.ajax({
            type: 'get',
            url: "/share/"+shareAlbumSettings.shareLink+"/album/"+albumId+"/"+nextPage,
            contentType: 'application/json; charset=utf-8'
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating share album. Attempt: "+shareAlbumSettings.tryCount+"/"+shareAlbumSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                shareAlbumSettings.tryCount++;
                if (shareAlbumSettings.tryCount <= shareAlbumSettings.retryLimit) {
                    //try again
                    shareAlbumSettings.updateAlbum(albumId, nextPage, activePage);
                }
            }
        }).then(function (data) {
            shareAlbumSettings.tryCount = 0;
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("albumMetadataList")) {
                        const albumMetadataList = data["albumMetadataList"] === "" ? [] : data["albumMetadataList"];

                        for (const index in albumMetadataList) {
                            const metadata = albumMetadataList[index];

                            let html =
                                '<div class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" class="photo-thumbnail-image" id="image' + metadata.id + '" onError="shashin.errorImg(this,\'' + metadata.title + '\',209)">\n';

                            const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                            html += shashin.renderTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight);

                            const centeredObj = shashin.renderCenteredOverlay(metadata,'favoritesSettings.openGallery',currentMediaLinkIndex);
                            html += centeredObj.html;

                            html += '</div>\n';

                            // Append HTML
                            $(html).insertBefore($(".appendAlbumPhotos").last())

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
        });
    }
}( window.shareAlbumSettings = window.shareAlbumSettings || {}, jQuery ));