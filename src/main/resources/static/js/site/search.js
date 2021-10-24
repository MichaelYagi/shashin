(function( searchSettings, $, undefined ) {
    searchSettings.infiniteScrollGallery = null;
    searchSettings.lg = null;
    searchSettings.lightGalleryConfigs = shashin.getLightGalleryConfigs();
    searchSettings.lightGalleryConfigs["dynamic"] = true;
    searchSettings.retryLimit = 3;
    searchSettings.tryCount = 0;

    searchSettings.setLightGalleryElement = function (name) {
        searchSettings.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            searchSettings.infiniteScrollGallery = document.getElementById(name);
        }
    };

    searchSettings.setLightGallery = function () {
        searchSettings.lg = lightGallery(searchSettings.getLightGalleryElement(), searchSettings.lightGalleryConfigs);
    }

    searchSettings.getLightGalleryElement = function () {
        return searchSettings.infiniteScrollGallery;
    };

    searchSettings.getLightGallery = function () {
        return searchSettings.lg;
    }

    searchSettings.openGallery = function (e,index) {
        e.preventDefault();
        if (searchSettings.getLightGallery() !== null) {
            searchSettings.getLightGallery().openGallery(index);
        }
    }

    searchSettings.activateMetadataListeners = function (metadata) {
        shashin.printMessageToConsole(metadata.id);

        $("#image"+metadata.id).on('load', function() {
            $(this).css("background-color","transparent");
        });
    }

    searchSettings.updateSearch = function(nextPage,searchTerm,activePage) {
        const promise = $.ajax({
            type: 'get',
            url: "/search/"+nextPage+"?searchTerm="+encodeURIComponent(searchTerm),
            contentType: 'application/json; charset=utf-8',
            async:true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating search. Attempt: "+searchSettings.tryCount+"/"+searchSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                searchSettings.tryCount++;
                if (searchSettings.tryCount <= searchSettings.retryLimit) {
                    //try again
                    searchSettings.updateSearch(nextPage,searchTerm,activePage);
                }
            }
        }).then(function (data) {
            searchSettings.tryCount = 0;
            var mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("metadataSearchList") && data["status"] === "success") {
                const metadataList = data["metadataSearchList"] === "" ? null : data["metadataSearchList"];

                if (metadataList !== null && metadataList.length > 0) {
                    var html = "";
                    var mediaLinkLength = $(".mediaLink").length;
                    for (var index in metadataList) {
                        const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                        var mediaContent = {}
                        var metadata = metadataList[index];
                        let dateString = shashin.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                        html += '<div class="photo-thumbnail-container photo-thumbnail" style="width:'+metadata.thumbnailSmallWidth+'px;height:'+metadata.thumbnailSmallHeight+'px;padding-left:0;padding-right:0;">\n';
                        html +=
                            '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+currentMediaLinkIndex+'"></a>\n' +
                            '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="shashin.errorImg(this,\''+metadata.title+'\',209)">\n' +
                            '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n';

                        html += shashin.renderBottomLeftOverlay(id, 'propperson', null, null, null);

                        const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                        html += shashin.renderTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight);

                        const centeredObj = shashin.renderCenteredOverlay(metadata,'searchSettings.openGallery',currentMediaLinkIndex);
                        html += centeredObj.html;
                        mediaContentList.push(centeredObj.mediaContent);

                        html += '</div>\n<span class="appendSearchPhotos" style="width:0;height:0;padding:0"></span>\n';
                        $(html).insertAfter($(".appendSearchPhotos").last())

                        shashin.setPhotoOverlays(metadata, activePage);
                        searchSettings.activateMetadataListeners(metadata);
                        $("#infoModalEdit"+metadata.id).attr("tag",JSON.stringify(metadata));
                        $("#infoModalEdit"+metadata.id).click(function(e) {
                            e.preventDefault();
                            const metadataObj = JSON.parse($(this).attr("tag"));
                            shashin.openInfoModal(metadataObj);
                        });

                        html = "";
                    }
                } else {
                    $(".appendSearchPhotos").last().text("EOL").css("display","none")
                }
            } else {
                $(".appendSearchPhotos").last().text("EOL").css("display","none")
            }

            $("#spinner").css("display","none");

            return mediaContentList;
        });

        return promise.done(function(data) {
            return data;
        });
    }
}( window.searchSettings = window.searchSettings || {}, jQuery ));