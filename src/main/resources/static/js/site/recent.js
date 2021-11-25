(function( recentSettings, $, undefined ) {
    recentSettings.updateRecent = function(nextPage,activePage) {
        const ajaxParams = {
            type: 'get',
            url: "/recent/"+nextPage,
            contentType: 'application/json; charset=utf-8',
            async:true,
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating recently added. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        const promise = $.ajax(ajaxParams)
            .fail(onFail).then(function (data) {
                const mediaContentList = [];
                if (data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data["status"] === "success") {
                    const metadataList = data["metadataList"] === "" ? null : data["metadataList"];
                    const favoritesMap = data["favorites"] === "" ? null : data["favorites"];
                    const recognitionLabels = data["recognitionLabels"] === "" ? null : data["recognitionLabels"];
                    const labelPhotoMap = data["labelPhotoMap"] === "" ? null : data["labelPhotoMap"];
                    const albumMap = data["albumMap"] === "" ? null : data["albumMap"];
                    const albumList = data["albumList"] === "" ? null : data["albumList"];

                    if (metadataList !== null && metadataList.length > 0) {
                        let html = "";
                        const mediaLinkLength = $(".mediaLink").length;

                        for (const index in metadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = metadataList[index];

                            html += '<div class="photo-thumbnail-container photo-thumbnail" style="width:'+metadata.thumbnailSmallWidth+'px;height:'+metadata.thumbnailSmallHeight+'px;padding-left:0;padding-right:0;">\n';
                            html +=
                                '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+currentMediaLinkIndex+'"></a>\n' +
                                '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\''+metadata.title+'\',209)">\n' +
                                '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n' +
                                '   <input type="hidden" name="thumbnailCentered' + metadata.id + '" id="thumbnailCentered' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlCentered) + '">\n';

                            html += shashin.getTopLeftOverlay(metadata.id);

                            const ediIcon = (metadata.lat === null || metadata.lng === null) ? 'bi-pencil-square' : 'bi-pencil';
                            html +=
                                '   <div class="thumbnail-bl" id="tnbl'+metadata.id+'">\n' +
                                '       <a href="#" id="timelineModalEdit'+metadata.id+'" data-bs-target="#propTimelinModal">\n' +
                                '           <span class="'+ediIcon+'" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                '       </a>\n' +
                                '   </div>\n';

                            const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                            html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                            const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                            html += centeredObj.html;
                            mediaContentList.push(centeredObj.mediaContent);

                            html += '</div>\n<span class="appendRecentPhotos" style="width:0;height:0;padding:0"></span>\n';
                            $(html).insertAfter($(".appendRecentPhotos").last())

                            $("#timelineModalEdit"+metadata.id).attr("tag",JSON.stringify(metadata));
                            $("#timelineModalEdit"+metadata.id).click(function(e) {
                                e.preventDefault();

                                shashin.openEditMetadataModal(metadata,recognitionLabels,labelPhotoMap[metadata.id],albumList,albumMap[metadata.id]);
                            });

                            shashin.setPhotoOverlays(metadata, activePage);
                            Util.activateMetadataListeners(metadata);

                            html = "";
                        }
                    } else {
                        $(".appendRecentPhotos").last().text("EOL").css("display","none")
                    }
                } else {
                    $(".appendRecentPhotos").last().text("EOL").css("display","none")
                }

                $("#spinner").css("display","none");

                return mediaContentList;
            });

        return promise.done(function(data) {
            return data;
        });
    }
}( window.recentSettings = window.recentSettings || {}, jQuery ));