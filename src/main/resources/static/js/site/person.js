class Person {

    constructor(metadataList, activePage, personId) {
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.personId = personId;
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    init() {
        $(function() {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        shashin.pageLoader(this.loadNextPage.bind(this), ".appendPersonPhotos", this.metadataList,true);

        shashin.matchingListeners();
        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    loadNextPage() {
        const currentPage = parseInt($("#currentPage").val());
        const nextPage = currentPage + 1;
        this.updatePerson(this.personId,nextPage,this.activePage).then(function(additionalMediaContentList) {
            this.mediaContentList = shashin.updateMediaContent(this.mediaContentList,additionalMediaContentList);
        }.bind(this));
        $("#currentPage").val(nextPage);
    }

    updatePerson(personId,nextPage,activePage) {
        $("#spinner").css("display","block");

        const ajaxParams = {
            type: 'get',
            url: "/person/" + personId + "/" + nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        // Get paged results
        const promise = $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating person")}).then(function (data) {
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList")) {
                        const metadataList = data["metadataList"];
                        const recognitionLabels = data["recognitionLabels"];
                        const labelPhotoMap = data["labelPhotoMap"];
                        const currentUser = data["currentUser"];
                        const keywordMap = data["keywordMap"];

                        let html = "";

                        if (metadataList.length > 0) {
                            const mediaLinkLength = $(".mediaLink").length;
                            for (const index in metadataList) {
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                const metadata = metadataList[index];

                                html += '<div id="photoThumbnailContainer' + metadata.id + '" class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex' + currentMediaLinkIndex + '"></a>\n' +
                                    '   <img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\'' + metadata.title + '\',Util.thumbnailHeight())">\n' +
                                    '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n' +
                                    '   <input type="hidden" name="thumbnailCentered' + metadata.id + '" id="thumbnailCentered' + metadata.id + '" th:value="' + metadata.thumbnailUrlCentered + '">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, (currentUser.authority === "ROLE_ADMIN" && labelPhotoMap[metadata.id]["isTagged"] === true));

                                if (currentUser.authority === "ROLE_ADMIN") {
                                    html += shashin.getTopLeftOverlay(metadata.id);
                                    html += shashin.getBottomLeftOverlay(metadata.id, 'propperson', null, null, null);
                                } else {
                                    html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);
                                }

                                const centeredObj = shashin.getCenteredOverlay(metadata, 'shashin.openGallery', currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                html += '</div>\n<span id="personmodal'+metadata.id+'" style="width:0;height:0;padding:0"></span>\n';

                                $(html).insertBefore($(".appendPersonPhotos").last())

                                shashin.setPhotoOverlays(metadata, activePage);
                                personModalSettings.renderPersonModal(metadata, recognitionLabels, labelPhotoMap[metadata.id]["labels"]);
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
                            $(".appendPersonPhotos").last().text("EOL").css("display", "none")
                        }
                    }
                } else {
                    $(".appendPersonPhotos").last().text("EOL").css("display", "none")
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendPersonPhotos").last().text("EOL").css("display", "none")
            }

            $("#spinner").css("display","none");
            return mediaContentList;
        });

        return promise.done(function(data) {
            $("#spinner").css("display","none");
            return data;
        });
    }
}