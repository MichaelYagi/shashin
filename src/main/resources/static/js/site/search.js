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
            lgConfig["metadataDetailFun"] = shashin.openInfoSidebar;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig["videoThumbnail"] = true;
            lgConfig["videoThumbnailFun"] = shashin.processVideoThumbnail;
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
        shashin.closeGalleryOnBack();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            setTimeout(function () {
                this.updateSearch(this.page, this.term, this.activePage).then(function (additionalMediaContentList) {
                    // console.log(additionalMediaContentList)
                    this.page++;
                    this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);

                    if (this.eol) {
                        setTimeout(() => {Util.reinitLightGalleryInstance({timeoutValue:0,mediaContentList:additionalMediaContentList,refreshContent:true});}, 0);
                    }
                }.bind(this));
            }.bind(this), 0);
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
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataSearchList") && data["status"] === shashin.apiResponse.SUCCESS) {
            const metadataList = data["metadataSearchList"];
            const favoritesMap = data["favorites"];

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;
                const appendClass = "appendSearchPhotos";

                for (const index in metadataList) {
                    const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                    const metadata = metadataList[index];

                    let dateHeadingObj = null;
                    const overlayFlags = {};
                    overlayFlags.renderTopRight = true;
                    overlayFlags.renderTopLeft = true;
                    overlayFlags.renderBottomLeft = true;
                    overlayFlags.renderCenter = true;
                    overlayFlags.renderBottomRight = true;

                    const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["favorite"] === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                    const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["count"] > 0 ? favoritesMap[metadata.id]["count"] : 0;

                    const dateHeadingCount = $(".dateSection").length;
                    const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                    const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                    const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                    if (lastDateHeading !== currentDate) {
                        dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                    }

                    const overlayData = shashin.getOverlayData(metadata,{cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,favoriteCount:favoriteCount,favoriteIcon:favoriteIcon,overlayFlags});

                    mediaContentList.push(shashin.getMediaContent(metadata));

                    const uuid = uuidv4();
                    $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData, uuid})).insertAfter($("."+appendClass).last()).ready(function () {
                        // Call JS and modal
                        shashin.updateFavorites("#favorite","#brfavoriteicon","#briconcount",metadata.id);
                    });
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