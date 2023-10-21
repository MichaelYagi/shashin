class Search {

    constructor(term, activePage, metadataSearchList) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.term = term;
        this.activePage = activePage;
        this.metadataSearchList = metadataSearchList;
        this.eol = false;

        const lgConfig = {
            dynamic:true,
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig["metadataDetail"] = true;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig["videoThumbnail"] = true;
        }
        this.mediaContentList = shashin.initLightGallery(
            'scroll-gallery',
            lgConfig,
            '.mediaLink'
        );
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendSearchPhotos", this.metadataSearchList);
        shashin.mouseMoveListener();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateSearch(this.page, this.term, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);

                if (this.eol) {
                    setTimeout(() => {Util.reinitLightGalleryInstance({timeoutValue:0,mediaContentList:additionalMediaContentList,activePage:this.activePage});}, 0);
                }
            }.bind(this));
        }
    }

    async updateSearch(nextPage,term,activePage) {
        this.rendering = true;

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/search/" + nextPage + "?term=" + encodeURIComponent(term));
        }

        const mediaContentList = [];
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataSearchList") && data["status"] === "success") {
            const metadataList = data["metadataSearchList"];

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;

                for (const index in metadataList) {
                    const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                    const metadata = metadataList[index];

                    let dateHeadingObj = null;
                    const overlayFlags = {};
                    overlayFlags.renderTopRight = true;
                    overlayFlags.renderTopLeft = false;
                    overlayFlags.renderBottomLeft = true;
                    overlayFlags.renderCenter = true;

                    const dateHeadingCount = $(".dateSection").length;
                    const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                    const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                    const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                    if (lastDateHeading !== currentDate) {
                        dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                    }

                    const overlayData = shashin.getOverlayData(metadata,{cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,overlayFlags});

                    mediaContentList.push(shashin.getMediaContent(metadata));

                    const appendClass = "appendSearchPhotos";
                    const uuid = uuidv4();
                    $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData, uuid})).insertAfter($("."+appendClass).last());
                }

                $("#spinner").css("display", "none");
                this.rendering = false;
            } else {
                $(".appendSearchPhotos").last().text("EOL").css("display","none")
                this.rendering = false;
                this.eol = true;
                $("#spinner").css("display","none");
            }
        } else {
            $(".appendSearchPhotos").last().text("EOL").css("display","none")
            this.rendering = false;
            this.eol = true;
            $("#spinner").css("display","none");
        }

        return mediaContentList;
    }
}