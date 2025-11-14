class Favorites {

    constructor(metadataList, mediaTypeFilter, activePage, lastDate, locale, lastLgIndex) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.metadataList = metadataList;
        this.mediaTypeFilter = mediaTypeFilter;
        this.activePage = activePage;
        this.lastDate = lastDate;
        this.eol = false;
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
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',lgConfig,mediaElement);
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendMetadataPhotos", this.metadataList, this.activePage);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            this.lastLgIndex += 1;
            this.updateFavorites(this.page, this.activePage, this.mediaTypeFilter, this.lastLgIndex).then(function (additionalMediaContentList) {
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

    async updateFavorites(nextPage,activePage,mediaTypeFilter,lastLgIndex) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/favorites/mediatype/" + mediaTypeFilter + "/page/" + nextPage);
        }

        const mediaContentList = [];
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data.status === shashin.apiResponse.SUCCESS) {
                if (data.hasOwnProperty("metadataList")) {
                    const metadataList = data.metadataList;

                    if (metadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;
                        const appendClass = "appendMetadataPhotos";

                        for (let index in metadataList) {
                            index = parseInt(index);
                            const currentMediaLinkIndex = (mediaLinkLength + index);
                            const metadata = metadataList[index];

                            if ($("#photoThumbnailContainer" + metadata.id).length === 0) {
                                let dateHeadingObj = null;
                                const overlayFlags = {};
                                overlayFlags.renderTopRight = true;
                                overlayFlags.renderTopLeft = true;
                                overlayFlags.renderBottomLeft = true;
                                overlayFlags.renderCenter = true;

                                let lastDate = metadataList.hasOwnProperty(index-1) ? metadataList[index-1].year+ "-" + metadataList[index-1].month + "-" + metadataList[index-1].day : "";
                                if (this.lastDate !== "") {
                                    lastDate = this.lastDate;
                                    this.lastDate = "";
                                }
                                const currentDate = metadata.year + "-" + metadata.month + "-" + metadata.day;
                                const nextDate = metadataList.hasOwnProperty(index+1) ? metadataList[index+1].year + "-" + metadataList[index+1].month + "-" + metadataList[index+1].day : "";
                                const displayCurrentDate = Util.getDateString(metadata.year, metadata.month, metadata.day, this.locale);

                                if (lastDate !== currentDate || $("#"+currentDate).length === 0) {
                                    dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                                }

                                const overlayData = shashin.getOverlayData(metadata, {
                                    cOnClickFunction: "shashin.openGallery",
                                    galleryIndex: currentMediaLinkIndex,
                                    overlayFlags
                                });
                                mediaContentList.push(shashin.getMediaContent(metadata));

                                const uuid = uuidv4();

                                if ($("#"+currentDate).length === 0 && dateHeadingObj !== null) {
                                    const headerAndBody = '<section class="dateSection" id="'+dateHeadingObj.heading+'"><div class="mb-3" id="dateHeader'+dateHeadingObj.heading+'"><strong>'+dateHeadingObj.display+'</strong>&nbsp;'+(dateHeadingObj.hasOwnProperty("placename")?dateHeadingObj.placename:'')+'</div><div id="dateBody'+dateHeadingObj.heading+'" class="row" class="row" style="margin-left:-2px;"></div></section>';
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
                                }

                                if ($("#"+currentDate).length > 0 && nextDate !== "" && currentDate !== nextDate) {
                                    $("<span class='"+appendClass+"' style='width:0;height:0;padding:0'></span>").insertAfter($("#"+currentDate));
                                }

                                lastLgIndex += 1;
                            }
                        }

                        this.lastLgIndex = lastLgIndex;
                        this.rendering = false;
                        $("#spinner").css("display", "none");
                    } else {
                        $(".appendMetadataPhotos").last().text("EOL").css("display", "none");
                        this.rendering = false;
                        this.eol = true;
                    }
                }
            } else {
                $(".appendMetadataPhotos").last().text("EOL").css("display", "none");
                this.rendering = false;
                this.eol = true;
                message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
                $("#msgTimeline").html(message);
            }
        } else {
            $(".appendMetadataPhotos").last().text("EOL").css("display", "none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}