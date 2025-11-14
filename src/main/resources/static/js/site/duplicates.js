class Duplicates {

    constructor(metadataList, mediaTypeFilter, activePage, locale, lastLgIndex) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.mediaTypeFilter = mediaTypeFilter;
        this.metadataList = metadataList;
        this.activePage = activePage;
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
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',lgConfig,mediaElement);
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendDuplicatePhotos", this.metadataList, this.activePage);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            this.lastLgIndex += 1;
            this.updateDuplicates(this.page, this.activePage, this.mediaTypeFilter, this.lastLgIndex).then(function (additionalMediaContentList) {
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

    async updateDuplicates(nextPage,activePage,mediaTypeFilter, lastLgIndex) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/duplicates/page/" + nextPage);
        }

        const mediaContentList = [];
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data.status === shashin.apiResponse.SUCCESS) {
            const metadataList = data.metadataList;

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;
                const appendClass = "appendDuplicatePhotos";

                for (const index in metadataList) {
                    const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                    const metadata = metadataList[index];
                    if ($("#photoThumbnailContainer" + metadata.id).length === 0) {
                        const overlayFlags = {};
                        overlayFlags.renderTopLeft = true;
                        overlayFlags.renderBottomLeft = true;
                        overlayFlags.renderCenter = true;

                        const overlayData = shashin.getOverlayData(metadata, {
                            editControls: true,
                            editIcon: ((metadata.lat === null || metadata.lng === null) ? 'bi-info-square' : 'bi-info-circle'),
                            cOnClickFunction: "shashin.openGallery",
                            galleryIndex: currentMediaLinkIndex,
                            overlayFlags
                        });

                        mediaContentList.push(shashin.getMediaContent(metadata));

                        const uuid = uuidv4();
                        $(GalleryTemplates.PhotoGalleryItem({
                            lastLgIndex,
                            activePage,
                            metadata,
                            overlayData,
                            uuid,
                            isMobile: Util.isMobile()
                        })).insertBefore($("." + appendClass).last());

                        lastLgIndex += 1;
                    }
                }

                this.lastLgIndex = lastLgIndex;
                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                this.rendering = false;
                this.eol = true;
                $(".appendFolderPhotos").last().text("EOL").css("display","none");
            }
        } else {
            $(".appendDuplicatePhotos").last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}