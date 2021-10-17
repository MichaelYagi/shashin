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
                        var albumMetadataList = data["albumMetadataList"] === "" ? [] : data["albumMetadataList"];

                        for (var index in albumMetadataList) {
                            var metadata = albumMetadataList[index];

                            let dateString = shashin.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                            var html =
                                '<div class="photo-thumbnail-container photo-thumbnail" style="width:'+metadata.thumbnailSmallWidth+'px;height:'+metadata.thumbnailSmallHeight+'px;padding-left:0;padding-right:0;">\n' +
                                '   <img src="'+encodeURI(metadata.thumbnailUrlSmall)+'" class="photo-thumbnail-image" id="image'+metadata.id+'" onError="shashin.errorImg(this,\''+metadata.title+'\')">\n';
                            if (metadata.type.includes("video")) {
                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html +=
                                    '   <div class="thumbnail-tr" id="tntr'+metadata.id+'">\n' +
                                    '       <span class="overlayIconBackground">'+duration+'&nbsp;<span id="video' + metadata.id + '" class="bi-camera-video overlayIcon"></span></span>\n' +
                                    '   </div>\n';
                            } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight*2) {
                                html +=
                                    '   <div class="thumbnail-tr" id="tntr' + metadata.id + '">\n' +
                                    '       <span id="panorama' + metadata.id + '" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>\n' +
                                    '   </div>\n';
                            }

                            html +=
                                '   <div class="thumbnail-centered" id="tncentered'+metadata.id+'">\n';

                            if (metadata.type.includes("video")) {
                                html +=
                                    '   <a class="mediaLink" onclick="return false"\n' +
                                    '       data-video=\'{"source": [{"src":"'+metadata.videoUrl+'", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}\'\n' +
                                    '       data-sub-html="'+(metadata.placeName !== null ? metadata.placeName+'<br>' : "<br>")+metadata.fileName+(dateString !== "" ? ' taken on '+dateString : '')+'">\n' +
                                    '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                    '   </a>\n';
                            } else {
                                html +=
                                    '   <a class="mediaLink" onclick="return false" data-src="'+metadata.thumbnailUrlOriginal+'" href="'+metadata.thumbnailUrlOriginal+'"' +
                                    '       data-sub-html="'+(metadata.placeName !== null ? metadata.placeName+'<br>' : "<br>")+metadata.fileName+(dateString !== "" ? ' taken on '+dateString : '')+'">\n' +
                                    '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                    '   </a>\n';
                            }

                            html +=
                                '   </div>\n' +
                                '</div>\n';

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