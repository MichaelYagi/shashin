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
            var mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList")) {
                        var metadataList = data["metadataList"] === "" ? null : data["metadataList"];

                        var html = "";

                        if (metadataList.length > 0) {
                            var mediaLinkLength = $(".mediaLink").length;
                            for (var index in metadataList) {
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                var mediaContent = {}
                                var metadata = metadataList[index];

                                let dateString = shashin.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                                html += '<div class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex' + currentMediaLinkIndex + '"></a>\n' +
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="shashin.errorImg(this,\''+metadata.title+'\',209)">\n';

                                if (metadata.type.includes("video")) {
                                    const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                    html +=
                                        '   <div class="thumbnail-tr" id="tntr' + metadata.id + '">\n' +
                                        '       <span class="overlayIconBackground">'+duration+'&nbsp;<span id="video' + metadata.id + '" class="bi-camera-video overlayIcon"></span></span>\n' +
                                        '   </div>\n';
                                } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight*2) {
                                    html +=
                                        '   <div class="thumbnail-tr" id="tntr' + metadata.id + '">\n' +
                                        '       <span id="panorama' + metadata.id + '" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>\n' +
                                        '   </div>\n';
                                }

                                html += '   <div class="thumbnail-tl" id="tntl' + metadata.id + '">\n' +
                                    '           <a href="#" id="select' + metadata.id + '">\n' +
                                    '               <span id="tlicon' + metadata.id + '" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                    '           </a>\n' +
                                    '       </div>';

                                html +=
                                    '       <div class="thumbnail-bl" id="tnbl'+metadata.id+'">\n' +
                                    '           <a href="#" id="infoModalEdit'+metadata.id+'">\n' +
                                    '               <span class="bi-info-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                    '           </a>\n' +
                                    '       </div>\n';

                                html += '<div class="thumbnail-centered" id="tncentered' + metadata.id + '">\n';

                                mediaContent.subHtml = (metadata.placeName !== null ? metadata.placeName + '<br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '');
                                if (metadata.type.includes("video")) {
                                    mediaContent.video = {
                                        "source": [{"src": metadata.videoUrl, "type": "video/mp4"}],
                                        "attributes": {"preload": false, "controls": true}
                                    };
                                    html +=
                                        '   <a class="mediaLink" onclick="return trashSettings.openGallery(event,' + currentMediaLinkIndex + ')" \n' +
                                        '       data-video=\'{"source": [{"src":"' + metadata.videoUrl + '", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}\'\n' +
                                        '       data-sub-html="' + (metadata.placeName !== null ? metadata.placeName + '<br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                                        '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                        '   </a>\n';
                                } else {
                                    mediaContent.src = metadata.thumbnailUrlOriginal;
                                    html +=
                                        '   <a class="mediaLink" onclick="return trashSettings.openGallery(event,' + currentMediaLinkIndex + ')" data-src="' + metadata.thumbnailUrlOriginal + '" href="' + metadata.thumbnailUrlOriginal + '"' +
                                        '       data-sub-html="' + (metadata.placeName !== null ? metadata.placeName + '<br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                                        '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                        '   </a>\n';
                                }
                                mediaContentList.push(mediaContent);
                                html += '</div></div>\n';

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