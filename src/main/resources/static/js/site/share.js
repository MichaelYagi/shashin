class ShareAlbum {

    constructor(shareLink, activePage, albumId, albumMetadataList) {
        this.page = 1;
        this.rendering = false;
        this.shareLink = shareLink;
        this.activePage = activePage;
        this.albumId = albumId;
        this.albumMetadataList = albumMetadataList;
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',{dynamic:true},'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendAlbumPhotos", this.albumMetadataList);

        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    async loadNextPage() {
        if (this.albumId > 0 && this.rendering === false) {
            // console.log(this.page)
            this.updateAlbum(this.albumId, this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }

        return parseInt($("#currentPage").val());
    }

    getShareLink() {
        return this.shareLink;
    }

    async updateAlbum(albumId, nextPage, activePage) {
        const self = this;
        self.rendering = true;
        $("#spinner").css("display","block");

        const ajaxParams = {
            type: 'get',
            url: "/share/"+self.getShareLink()+"/album/"+albumId+"/"+nextPage,
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        return await $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating share album")}).then(function (data) {
            const mediaContentList = [];

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("albumMetadataList")) {
                        const albumMetadataList = data["albumMetadataList"];
                        const mediaLinkLength = $(".mediaLink").length;

                        for (const index in albumMetadataList) {
                            let html = "";
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = albumMetadataList[index];

                            const dateHeadingCount = $(".dateSection").length;
                            const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                            const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                            const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                            if (lastDateHeading !== currentDate) {
                                html += '<section class="dateSection" id="'+currentDate+'"><p><strong>' + displayCurrentDate + '</strong></p></section>\n';
                            }

                            html +=
                                '<div id="photoThumbnailContainer' + metadata.id + '" class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                '   <img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlSmall) + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" class="photo-thumbnail-image" id="image' + metadata.id + '" onError="Util.errorImg(this,\'' + metadata.title + '\',Util.thumbnailHeight())">\n';

                            const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                            html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                            const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                            html += centeredObj.html;
                            mediaContentList.push(centeredObj.mediaContent);

                            html += '</div>\n<span class="appendAlbumPhotos" style="width:0;height:0;padding:0"></span>\n';

                            $(html).insertAfter($(".appendAlbumPhotos").last()).ready(function () {
                                // Call JS and modal
                                shashin.setPhotoOverlays(metadata, activePage);

                                if (parseInt(index) === parseInt(albumMetadataList.length) - 1) {
                                    this.rendering = false;
                                }
                            }.bind(this));
                        }
                    } else {
                        $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                        this.rendering = false;
                    }
                } else {
                    $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                    this.rendering = false;
                }
            } else {
                $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
            }

            $("#spinner").css("display","none");
            return mediaContentList;
        }.bind(this));
    }
}