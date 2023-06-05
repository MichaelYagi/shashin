class Modified {

    constructor(metadataList, activePage) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.eol = false;
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendModifiedPhotos", this.metadataList);
        shashin.setVideoWidth($("#scroll-gallery")[0]);
        shashin.mouseMoveListener();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateModified(this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
                shashin.setVideoWidth($("#scroll-gallery")[0]);
            }.bind(this));
        }
    }

    async updateModified(nextPage,activePage) {
        this.rendering = true;

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/modified/" + nextPage);
        }

        const mediaContentList = [];
        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data["status"] === "success") {
            const metadataList = data["metadataList"];

            if (metadataList !== null && metadataList.length > 0) {
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
                    const currentDate = dateFormat(metadata["modifiedAt"].replace(/-/g, "/"), "isoDate");
                    const displayCurrentDate = dateFormat(metadata["modifiedAt"].replace(/-/g, "/"), "ddd, mmm d, yyyy");

                    if (lastDateHeading !== currentDate) {
                        dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                    }

                    const overlayData = shashin.getOverlayData(metadata, {editControls:true,editIcon: ((metadata.lat === null || metadata.lng === null) ? 'bi-pencil-square' : 'bi-pencil'),cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,overlayFlags});

                    mediaContentList.push(shashin.getMediaContent(metadata));

                    const appendClass = "appendModifiedPhotos";
                    $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData})).insertBefore($("."+appendClass).last());
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $(".appendModifiedPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $(".appendModifiedPhotos").last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}