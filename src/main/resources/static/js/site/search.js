class Search {

    constructor(searchTerm, activePage, metadataSearchList) {
        this.page = 1;
        this.rendering = false;
        this.searchTerm = searchTerm;
        this.activePage = activePage;
        this.metadataSearchList = metadataSearchList;
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    async init() {
        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendSearchPhotos", this.metadataSearchList);

        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateSearch(this.page, this.searchTerm, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }
    }

    async updateSearch(nextPage,searchTerm,activePage) {
        this.rendering = true;
        $("#spinner").css("display","block");

        const ajaxParams = {
            type: 'get',
            url: "/search/"+nextPage+"?searchTerm="+encodeURIComponent(searchTerm),
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        return await $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating search")}).then(function (data) {
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("metadataSearchList") && data["status"] === "success") {
                const metadataList = data["metadataSearchList"];

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
                            '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+currentMediaLinkIndex+'" id="lightGalleryIndexAnchor_'+currentDate+'"></a>\n' +
                            '   <img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\''+metadata.title+'\',Util.thumbnailHeight())">\n' +
                            '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n';

                        html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);

                        const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                        html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                        const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                        html += centeredObj.html;
                        mediaContentList.push(centeredObj.mediaContent);

                        html += '</div>\n<span class="appendSearchPhotos" style="width:0;height:0;padding:0"></span>\n';

                        $(html).insertAfter($(".appendSearchPhotos").last()).ready(async function () {
                            $("#spinner").css("display", "none");

                            shashin.setPhotoOverlays(metadata, activePage);
                            Util.activateMetadataListeners(metadata);
                            $("#mediaLink" + metadata.id).attr("tag", metadata.id);
                            $("#infoModalEdit" + metadata.id).on("click", function (e) {
                                e.preventDefault();
                                shashin.openInfoModal(metadata.id);
                            });

                            if (parseInt(index) === parseInt(metadataList.length) - 1) {
                                this.rendering = false;
                            }
                        }.bind(this));

                        html = "";
                    }
                } else {
                    $(".appendSearchPhotos").last().text("EOL").css("display","none")
                    this.rendering = false;
                    $("#spinner").css("display","none");
                }
            } else {
                $(".appendSearchPhotos").last().text("EOL").css("display","none")
                this.rendering = false;
                $("#spinner").css("display","none");
            }

            return mediaContentList;
        }.bind(this));
    }
}