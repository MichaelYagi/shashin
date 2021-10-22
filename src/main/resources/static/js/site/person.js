(function( personSettings, $, undefined ) {
    personSettings.infiniteScrollGallery = null;
    personSettings.lg = null;
    personSettings.lightGalleryConfigs = shashin.getLightGalleryConfigs();
    personSettings.lightGalleryConfigs["dynamic"] = true;
    personSettings.retryLimit = 3;
    personSettings.tryCount = 0;

    personSettings.setLightGalleryElement = function (name) {
        personSettings.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            personSettings.infiniteScrollGallery = document.getElementById(name);
        }
    };

    personSettings.setLightGallery = function () {
        personSettings.lg = lightGallery(personSettings.getLightGalleryElement(), personSettings.lightGalleryConfigs);
    }

    personSettings.getLightGalleryElement = function () {
        return personSettings.infiniteScrollGallery;
    };

    personSettings.getLightGallery = function () {
        return personSettings.lg;
    }

    personSettings.openGallery = function (e,index) {
        e.preventDefault();
        if (personSettings.getLightGallery() !== null) {
            personSettings.getLightGallery().openGallery(index);
        }
    }

    personSettings.activateMetadataListeners = function (metadata) {
        shashin.printMessageToConsole(metadata.id);

        $("#image"+metadata.id).on('load', function() {
            $(this).css("background-color","transparent");
        });
    }

    personSettings.updatePerson = function (personId,nextPage,activePage) {
        // Get paged results
        var promise = $.ajax({
            type: 'get',
            url: "/person/"+personId+"/"+nextPage,
            contentType: 'application/json; charset=utf-8',
            async:true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating person. Attempt: "+personSettings.tryCount+"/"+personSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                personSettings.tryCount++;
                if (personSettings.tryCount <= personSettings.retryLimit) {
                    //try again
                    personSettings.updatePerson(personId,nextPage,activePage);
                }
            }
        }).then(function (data) {
            personSettings.tryCount = 0;
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList")) {
                        const metadataList = data["metadataList"] === "" ? [] : data["metadataList"];
                        const recognitionLabels = data["recognitionLabels"] === "" ? "" : data["recognitionLabels"];
                        const labelPhotoMap = data["labelPhotoMap"] === "" ? null : data["labelPhotoMap"];
                        const currentUser = data["currentUser"] === "" ? null : data["currentUser"];

                        let html = "";

                        if (metadataList.length > 0) {
                            const mediaLinkLength = $(".mediaLink").length;
                            for (const index in metadataList) {
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                const mediaContent = {};
                                const metadata = metadataList[index];

                                let dateString = shashin.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                                html += '<div class="photo-thumbnail-container photo-thumbnail" style="width:'+metadata.thumbnailSmallWidth+'px;height:'+metadata.thumbnailSmallHeight+'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+currentMediaLinkIndex+'"></a>\n' +
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="shashin.errorImg(this,\''+metadata.title+'\',209)">\n' +
                                    '   <input type="hidden" name="filename'+metadata.id+'" id="filename'+metadata.id+'" value="'+metadata.fileName+'">\n' +
                                    '   <input type="hidden" name="thumbnailCentered'+metadata.id+'" id="thumbnailCentered'+metadata.id+'" th:value="'+metadata.thumbnailUrlCentered+'">\n';

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


                                if (currentUser.authority === "ROLE_ADMIN") {
                                    if (labelPhotoMap[metadata.id]["isTagged"] === true) {

                                        html +=
                                            '<div class="thumbnail-br" id="tntr'+metadata.id+'">\n' +
                                            '   <span class="bi-bookmark-fill overlayIconBackground" style="font-size: 1rem;color: lightsalmon;"></span>\n' +
                                            '</div>\n';
                                    }


                                    html +=
                                        '   <div class="thumbnail-tl" id="tntl'+metadata.id+'">\n' +
                                        '       <a href="#" id="select'+metadata.id+'">\n' +
                                        '           <span id="tlicon'+metadata.id+'" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                        '       </a>\n' +
                                        '   </div>\n';
                                    html +=
                                        '   <div class="thumbnail-bl" id="tnbl'+metadata.id+'">\n' +
                                        '       <a href="#" id="infoModalEdit'+metadata.id+'">\n' +
                                        '           <span class="bi-info-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                        '       </a><br>\n' +
                                        '       <a href="#" data-bs-toggle="modal" data-bs-target="#propperson'+metadata.id+'">\n' +
                                        '           <span className="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                        '       </a>\n' +
                                        '   </div>\n';



                                } else {
                                    html +=
                                        '   <div class="thumbnail-bl" id="tnbl'+metadata.id+'">\n' +
                                        '       <a href="#" id="infoModalEdit'+metadata.id+'">\n' +
                                        '           <span class="bi-info-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                        '       </a>\n' +
                                        '   </div>\n';
                                }

                                html += '<div class="thumbnail-centered" id="tncentered' + metadata.id + '">\n';

                                mediaContent.subHtml = (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '');
                                if (metadata.type.includes("video")) {
                                    mediaContent.video = {"source":[{"src":metadata.videoUrl,"type":"video/mp4"}],"attributes":{"preload":false,"controls":true}};
                                    html +=
                                        '   <a class="mediaLink" onclick="return personSettings.openGallery(event,'+currentMediaLinkIndex+')" \n' +
                                        '       data-video=\'{"source": [{"src":"' + metadata.videoUrl + '", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}\'\n' +
                                        '       data-sub-html="' + (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                                        '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                        '   </a>\n';
                                } else {
                                    mediaContent.src = metadata.thumbnailUrlOriginal;
                                    html +=
                                        '   <a class="mediaLink" onclick="return personSettings.openGallery(event,'+currentMediaLinkIndex+')" data-src="' + metadata.thumbnailUrlOriginal + '" href="' + metadata.thumbnailUrlOriginal + '"' +
                                        '       data-sub-html="' + (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                                        '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                        '   </a>\n';
                                }
                                mediaContentList.push(mediaContent);
                                html += '</div></div>\n';

                                $(html).insertBefore($(".appendPersonPhotos").last())

                                shashin.setPhotoOverlays(metadata, activePage);
                                personModalSettings.renderPersonModal(metadata,recognitionLabels,labelPhotoMap[metadata.id]["labels"]);
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
                            $(".appendPersonPhotos").last().text("EOL").css("display","none")
                        }
                    }
                } else {
                    $(".appendPersonPhotos").last().text("EOL").css("display","none")
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendPersonPhotos").last().text("EOL").css("display","none")
            }

            return mediaContentList;
        });

        return promise.done(function(data) {
            return data;
        });
    }
}( window.personSettings = window.personSettings || {}, jQuery ));