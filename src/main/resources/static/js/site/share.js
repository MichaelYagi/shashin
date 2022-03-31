class ShareAlbum {

    constructor(shareLink, activePage, albumId, albumMetadataList) {
        this.http = new Http(activePage);
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

        const data = await this.http.ajaxGet("/share/"+self.getShareLink()+"/album/"+albumId+"/"+nextPage);

        const mediaContentList = [];

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === "success") {
                if (data.hasOwnProperty("albumMetadataList")) {
                    const albumMetadataList = data["albumMetadataList"];
                    const mediaLinkLength = $(".mediaLink").length;

                    for (const index in albumMetadataList) {
                        const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                        const metadata = albumMetadataList[index];

                        let dateHeadingObj = null;
                        let renderTopRight = true;
                        let renderTopLeft = false;
                        let renderBottomLeft = false;
                        let renderCenter = true;

                        const dateHeadingCount = $(".dateSection").length;
                        const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                        const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                        const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                        if (lastDateHeading !== currentDate) {
                            dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                        }

                        const overlayData = shashin.getOverlayData(metadata, {cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex});

                        mediaContentList.push(shashin.getMediaContent(metadata));

                        const appendClass = "appendAlbumPhotos";
                        $(PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, renderTopRight, renderTopLeft, renderBottomLeft, renderCenter, overlayData})).insertBefore($("."+appendClass).last());
                    }

                    this.rendering = false;
                    $("#spinner").css("display", "none");
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
    }
}