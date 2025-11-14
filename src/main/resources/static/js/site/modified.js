class Modified {

    constructor(metadataList, mediaTypeFilter, activePage, lastDate, locale, lastLgIndex) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.mediaTypeFilter = mediaTypeFilter;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.eol = false;
        this.lastDate = lastDate;
        this.locale = locale;
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
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',lgConfig,mediaElement);
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendModifiedPhotos", this.metadataList, this.activePage);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            this.lastLgIndex += 1;
            this.updateModified(this.page, this.activePage, this.mediaTypeFilter, this.lastLgIndex).then(function (additionalMediaContentList) {
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

    async updateModified(nextPage,activePage,mediaTypeFilter,lastLgIndex) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/modified/mediatype/" + mediaTypeFilter + "/page/" + nextPage);
        }

        const mediaContentList = [];
        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data.status === shashin.apiResponse.SUCCESS) {
            const metadataList = data.metadataList;
            const favoritesMap = data.favorites;

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;
                const appendClass = "appendModifiedPhotos";

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

                        let lastDate = metadataList.hasOwnProperty(index-1) ? dateFormat(metadataList[index-1].modifiedAt.replace(/-/g, "/"), "yyyy-m-d") : "";
                        if (this.lastDate !== "") {
                            lastDate = this.lastDate;
                            this.lastDate = "";
                        }
                        const currentDate = dateFormat(metadata.modifiedAt.replace(/-/g, "/"), "yyyy-m-d");
                        const nextDate = metadataList.hasOwnProperty(index+1) ? dateFormat(metadataList[index+1].modifiedAt.replace(/-/g, "/"), "yyyy-m-d") : "";
                        const currentDateArray = currentDate.split("-");
                        const displayCurrentDate = Util.getDateString(currentDateArray[0], currentDateArray[1], currentDateArray[2], this.locale);

                        if (lastDate !== currentDate || $("#"+currentDate).length === 0) {
                            dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                        }

                        const overlayData = shashin.getOverlayData(metadata, {
                            editControls: true,
                            editIcon: ((metadata.lat === null || metadata.lng === null) ? 'bi-info-square' : 'bi-info-circle'),
                            cOnClickFunction: "shashin.openGallery",
                            galleryIndex: currentMediaLinkIndex,
                            favoriteCount: favoriteCount,
                            favoriteIcon: favoriteIcon,
                            overlayFlags
                        });

                        mediaContentList.push(shashin.getMediaContent(metadata));

                        const uuid = uuidv4();

                        if ($("#"+currentDate).length === 0 && dateHeadingObj !== null) {
                            const headerAndBody = '<section class="dateSection" id="'+currentDate+'"><div class="dateHeader" id="dateHeader'+currentDate+'"><span id="select'+currentDate+'" class="bi-circle pe-2 day-select" style="font-size: 0.85rem;color: lightgray;display: none"></span><span class="text-muted">'+shashin.getTranslatedValue('main.pages.browse.header.modified')+' </span><strong>'+dateHeadingObj.display+'</strong>&nbsp;'+(dateHeadingObj.hasOwnProperty("placename")?dateHeadingObj.placename:'')+'</div><div id="dateBody'+currentDate+'" class="row" class="row" style="margin-left:-2px;"></div></section>';
                            $(headerAndBody).insertBefore($("." + appendClass).last());
                        }

                        const html = $(GalleryTemplates.PhotoGalleryItem({
                            lastLgIndex,
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

                        shashin.dayHeadingListener(currentDate, activePage, mediaTypeFilter);

                        lastLgIndex += 1;
                    }
                }

                this.lastLgIndex = lastLgIndex;
                $('a').attr('draggable', 'false');
                $('img').attr('draggable', 'false');
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