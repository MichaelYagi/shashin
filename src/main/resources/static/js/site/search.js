class Search {

    constructor(term, activePage, metadataSearchList, lastDate) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.term = term;
        this.activePage = activePage;
        this.metadataSearchList = metadataSearchList;
        this.lastDate = lastDate;
        this.eol = false;

        const lgConfig = {
            dynamic:true,
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig.metadataDetail = true;
            lgConfig.metadataDetailFun = shashin.openEditMetadataModal;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig.videoThumbnail = true;
            lgConfig.videoThumbnailFun = shashin.processVideoThumbnail;
        }
        this.mediaContentList = shashin.initLightGallery(
            'scroll-gallery',
            lgConfig,
            '.mediaLink'
        );
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendSearchPhotos", this.metadataSearchList, this.activePage);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateSearch(this.page, this.term, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList, this.activePage);

                if (this.eol) {
                    setTimeout(() => {Util.reinitLightGalleryInstance({timeoutValue:0,mediaContentList:additionalMediaContentList,refreshContent:true});}, 0);
                }
            }.bind(this));
        }

        return this.eol;
    }

    async updateSearch(nextPage,term,activePage) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/search/" + nextPage + "?term=" + encodeURIComponent(term).replace(";", "%3B"));
        }

        const mediaContentList = [];
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataSearchList") && data.status === shashin.apiResponse.SUCCESS) {
            const metadataList = data.metadataSearchList;
            const favoritesMap = data.favorites;

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;
                const appendClass = "appendSearchPhotos";

                for (let index in metadataList) {
                    index = parseInt(index);
                    const currentMediaLinkIndex = (mediaLinkLength + index);
                    const metadata = metadataList[index];

                    if ($("#photoThumbnailContainer"+metadata.id).length === 0) {
                        let dateHeadingObj = null;
                        const overlayFlags = {};
                        overlayFlags.renderTopRight = true;
                        overlayFlags.renderTopLeft = true;
                        overlayFlags.renderBottomLeft = true;
                        overlayFlags.renderCenter = true;
                        overlayFlags.renderBottomRight = true;

                        const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].favorite === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                        const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].count > 0 ? favoritesMap[metadata.id].count : 0;

                        let lastDate = metadataList.hasOwnProperty(index-1) ? metadataList[index-1].year+ "-" + metadataList[index-1].month + "-" + metadataList[index-1].day : "";
                        if (this.lastDate !== "") {
                            lastDate = this.lastDate;
                            this.lastDate = "";
                        }
                        const currentDate = metadata.year + "-" + metadata.month + "-" + metadata.day;
                        const nextDate = metadataList.hasOwnProperty(index+1) ? metadataList[index+1].year + "-" + metadataList[index+1].month + "-" + metadataList[index+1].day : "";
                        const displayCurrentDate = dateFormat(currentDate.replace(/-/g, "/"), "ddd, mmm d, yyyy");

                        if (lastDate !== currentDate || $("#"+currentDate).length === 0) {
                            dateHeadingObj = {
                                heading: currentDate,
                                display: displayCurrentDate
                            };
                        }

                        const overlayData = shashin.getOverlayData(metadata, {
                            cOnClickFunction: "shashin.openGallery",
                            galleryIndex: currentMediaLinkIndex,
                            favoriteCount: favoriteCount,
                            favoriteIcon: favoriteIcon,
                            overlayFlags
                        });

                        mediaContentList.push(shashin.getMediaContent(metadata));

                        const uuid = uuidv4();

                        if ($("#"+currentDate).length === 0 && dateHeadingObj !== null) {
                            const headerAndBody = '<section class="dateSection" id="' + currentDate + '"><div class="mb-3" id="dateHeader' + currentDate + '"><strong>' + dateHeadingObj.display + '</strong>&nbsp;' + (dateHeadingObj.hasOwnProperty("placename") ? dateHeadingObj.placename : '') + '</div><div id="dateBody' + currentDate + '" class="row" style="margin-left:-2px;"></div></section>';
                            $(headerAndBody).insertBefore($("." + appendClass).last());
                        }

                        const html = $(GalleryTemplates.PhotoGalleryItem({
                            activePage,
                            metadata,
                            overlayData,
                            uuid,
                            isMobile: Util.isMobile()
                        }));

                        if ($("#dateBody"+currentDate).length > 0) {
                            $(html).appendTo($("#dateBody" + currentDate));
                            shashin.updateFavorites("#favorite","#brfavoriteicon","#briconcount", metadata.id);
                        }

                        if ($("#"+currentDate).length > 0 && nextDate !== "" && currentDate !== nextDate) {
                            $("<span class='"+appendClass+"' style='width:0;height:0;padding:0'></span>").insertAfter($("#"+currentDate));
                        }
                    }
                }

                $("#spinner").css("display", "none");
                this.rendering = false;
            } else {
                $(".appendSearchPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
                $("#spinner").css("display","none");
            }
        } else {
            $(".appendSearchPhotos").last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
            $("#spinner").css("display","none");
        }

        return mediaContentList;
    }
}