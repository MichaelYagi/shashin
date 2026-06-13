class Archived {
    constructor(activePage, metadataList, lastDate, lastLgIndex, locale) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.activePage = activePage;
        this.metadataList = metadataList;
        this.lastSectionDate = lastDate;
        this.lastSectionId = lastDate;
        this.eol = false;
        this.locale = locale;
        this.lastLgIndex = lastLgIndex;

        const mediaElement = '.mediaLink';

        const lgConfig = {
            dynamic:true,
            dynamicEl:shashin.getInitMediaContent(mediaElement),
            shashinEditor: false, // No editing on archive page
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig.metadataDetail = true;
            lgConfig.metadataDetailFun = shashin.openEditMetadataModal;
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
            this.updateArchive(this.page, this.activePage, this.lastLgIndex).then(function (additionalMediaContentList) {
                if (additionalMediaContentList.length > 0) {
                    this.page++;
                    this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList, "archived");

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

    async updateArchive(nextPage, activePage, lastLgIndex) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/archived/page/" + nextPage);
        }

        const mediaContentList = [];
        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data.status === shashin.apiResponse.SUCCESS) {
                if (data.hasOwnProperty("metadataList")) {
                    const metadataList = data.metadataList;

                    if (metadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;
                        let appendedCount = 0;
                        const appendClass = "appendMetadataPhotos";

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

                                const currentDate = dateFormat(metadata.modifiedAt.replace(/-/g, "/"), "yyyy-m-d");
                                const currentDateArray = currentDate.split("-");
                                const displayCurrentDate = Util.getDateString(currentDateArray[0], currentDateArray[1], currentDateArray[2], this.locale);

                                const overlayData = shashin.getOverlayData(metadata, {
                                    cOnClickFunction: "shashin.openGallery",
                                    galleryIndex: currentMediaLinkIndex,
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
                                    const headerAndBody = '<section class="dateSection" id="' + sectionId + '"><div class="dateHeader" id="dateHeader' + sectionId + '"><span class="text-muted">'+shashin.getTranslatedValue('main.pages.browse.header.archived')+' </span><strong>' + displayCurrentDate + '</strong></div><div id="dateBody' + sectionId + '" class="row" style="margin-left:-2px;"></div></section>';
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

                                lastLgIndex += 1;
                            }
                        }

                        this.lastLgIndex = lastLgIndex;
                        this.rendering = false;
                        $("#spinner").css("display", "none");
                    } else {
                        this.rendering = false;
                        this.eol = true;
                        $(".appendMetadataPhotos").last().text("EOL").css("display", "none");
                    }
                }
            } else {
                this.rendering = false;
                this.eol = true;
                $(".appendMetadataPhotos").last().text("EOL").css("display", "none");
                message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
                $("#msgTimeline").html(message);
            }
        } else {
            this.rendering = false;
            $(".appendMetadataPhotos").last().text("EOL").css("display", "none");
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}