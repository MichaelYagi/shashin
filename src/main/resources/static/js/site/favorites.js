class Favorites {

    constructor(metadataList, activePage) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.eol = false;
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendMetadataPhotos", this.metadataList);

        shashin.mouseMoveListener();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateFavorites(this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }
    }

    async updateFavorites(nextPage,activePage) {
        this.rendering = true;

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/favorites/" + nextPage);
        }

        const mediaContentList = [];
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === "success") {
                if (data.hasOwnProperty("metadataList")) {
                    const metadataList = data["metadataList"];

                    if (metadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;

                        for (const index in metadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = metadataList[index];

                            let dateHeadingObj = null;
                            const overlayFlags = {};
                            overlayFlags.renderTopRight = true;
                            overlayFlags.renderTopLeft = true;
                            overlayFlags.renderBottomLeft = true;
                            overlayFlags.renderCenter = true;

                            const dateHeadingCount = $(".dateSection").length;
                            const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                            const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                            const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                            if (lastDateHeading !== currentDate) {
                                dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                            }

                            const overlayData = shashin.getOverlayData(metadata, {cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,overlayFlags});
                            mediaContentList.push(shashin.getMediaContent(metadata));

                            const appendClass = "appendMetadataPhotos";
                            $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData})).insertBefore($("."+appendClass).last());
                        }

                        this.rendering = false;
                        $("#spinner").css("display", "none");
                    } else {
                        $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                        this.rendering = false;
                        this.eol = true;
                    }
                }
            } else {
                $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                this.rendering = false;
                this.eol = true;
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                $("#msgTimeline").html(message);
            }
        } else {
            $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}