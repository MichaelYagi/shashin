class Search {

    constructor(term, activePage, metadataSearchList, lastDate, lastLgIndex) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.term = term;
        this.activePage = activePage;
        this.metadataSearchList = metadataSearchList;
        this.lastSectionDate = lastDate;
        this.lastSectionId = lastDate;
        this.eol = false;
        this.lastLgIndex = lastLgIndex;

        const mediaElement = '.mediaLink';

        const lgConfig = {
            dynamic:true,
            dynamicEl:shashin.getInitMediaContent(mediaElement),
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
            mediaElement
        );
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendSearchPhotos", this.metadataSearchList, this.activePage);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            this.updateSearch(this.page, this.term, this.activePage, this.lastLgIndex).then(function (additionalMediaContentList) {
                if (additionalMediaContentList.length > 0) {
                    this.page++;
                    this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList, this.activePage);

                    if (this.eol) {
                        setTimeout(() => {
                            Util.reinitLightGalleryInstance({
                                timeoutValue: 0,
                                mediaContentList: additionalMediaContentList,
                                refreshContent: true
                            });
                        }, 0);
                    }
                }
            }.bind(this));
        }

        return this.eol;
    }

    async updateSearch(nextPage,term,activePage,lastLgIndex) {
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
                let appendedCount = 0;
                const appendClass = "appendSearchPhotos";

                for (let index in metadataList) {
                    index = parseInt(index);
                    const currentMediaLinkIndex = (mediaLinkLength + appendedCount);
                    const metadata = metadataList[index];

                    if ($("#photoThumbnailContainer"+metadata.id).length === 0) {
                        const overlayFlags = {};
                        overlayFlags.renderTopRight = true;
                        overlayFlags.renderTopLeft = true;
                        overlayFlags.renderBottomLeft = true;
                        overlayFlags.renderCenter = true;
                        overlayFlags.renderBottomRight = true;

                        const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].favorite === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                        const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].count > 0 ? favoritesMap[metadata.id].count : 0;

                        const currentDate = metadata.year + "-" + metadata.month + "-" + metadata.day;
                        const displayCurrentDate = dateFormat(currentDate.replace(/-/g, "/"), "ddd, mmm d, yyyy");

                        const overlayData = shashin.getOverlayData(metadata, {
                            cOnClickFunction: "shashin.openGallery",
                            galleryIndex: currentMediaLinkIndex,
                            favoriteCount: favoriteCount,
                            favoriteIcon: favoriteIcon,
                            overlayFlags
                        });

                        mediaContentList.push(shashin.getMediaContent(metadata));
                        appendedCount += 1;

                        const uuid = uuidv4();

                        // Always append new sections at the end of the gallery (never insert into an
                        // earlier date section) so that mediaContentList order matches DOM order, which
                        // is required for lightGallery's index-based next/prev navigation.
                        let sectionId = this.lastSectionId;

                        if (this.lastSectionDate !== currentDate) {
                            sectionId = currentDate + ($("#"+currentDate).length > 0 ? "-" + uuid : "");
                            const headerAndBody = '<section class="dateSection" id="' + sectionId + '"><div class="mb-3" id="dateHeader' + sectionId + '"><strong>' + displayCurrentDate + '</strong></div><div id="dateBody' + sectionId + '" class="row" style="margin-left:-2px;"></div></section>';
                            $(headerAndBody).insertBefore($("." + appendClass).last());
                            $("<span class='"+appendClass+"' style='width:0;height:0;padding:0'></span>").insertAfter($("#"+sectionId));

                            this.lastSectionDate = currentDate;
                            this.lastSectionId = sectionId;
                        }

                        const html = $(GalleryTemplates.PhotoGalleryItem({
                            lastLgIndex,
                            activePage,
                            metadata,
                            overlayData,
                            uuid,
                            isMobile: Util.isMobile()
                        }));

                        $(html).appendTo($("#dateBody" + sectionId));
                        shashin.updateFavorites("#favorite","#brfavoriteicon","#briconcount", metadata.id);

                        lastLgIndex += 1;
                    }
                }

                this.lastLgIndex = lastLgIndex;
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