class ShareAlbum {

    constructor(shareLink, activePage, albumId, albumMetadataList, lastDate, locale, lastLgIndex, clearMultiSelectListener) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.shareLink = shareLink;
        this.activePage = activePage;
        this.albumId = albumId;
        this.albumMetadataList = albumMetadataList;
        this.eol = false;
        this.lastSectionDate = lastDate;
        this.lastSectionId = lastDate;
        this.locale = locale;
        this.clearMultiSelectListener = clearMultiSelectListener;
        this.lastLgIndex = lastLgIndex;

        const mediaElement = '.mediaLink';

        const lgConfig = {
            dynamic:true,
            dynamicEl:shashin.getInitMediaContent(mediaElement),
            shashinEditor: false // No editing on share page
        };

        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',lgConfig,mediaElement);
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendAlbumPhotos", this.albumMetadataList, this.activePage);
        }, 0);
        await this.clearMultiSelectListener(this.shareLink, this.albumId);
    }

    async loadNextPage() {
        if (this.albumId > 0 && this.rendering === false) {
            this.updateAlbum(this.albumId, this.page, this.activePage, this.lastLgIndex).then(function (additionalMediaContentList) {
                if (additionalMediaContentList.length > 0) {
                    this.page++;
                    this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList, "share");
                }
            }.bind(this));
        }

        return this.eol;
    }

    getShareLink() {
        return this.shareLink;
    }

    async updateAlbum(albumId, nextPage, activePage, lastLgIndex) {
        const self = this;
        self.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/share/" + self.getShareLink() + "/album/" + albumId + "/page/" + nextPage);
        }

        const mediaContentList = [];

        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data.status === shashin.apiResponse.SUCCESS) {
                if (data.hasOwnProperty("albumMetadataList")) {
                    const albumMetadataList = data.albumMetadataList;
                    const mediaLinkLength = $(".mediaLink").length;
                    let appendedCount = 0;

                    for (let index in albumMetadataList) {
                        index = parseInt(index);
                        const currentMediaLinkIndex = (mediaLinkLength + appendedCount);
                        const metadata = albumMetadataList[index];

                        if ($("#photoThumbnailContainer"+metadata.id).length === 0) {
                            const overlayFlags = {};
                            overlayFlags.renderTopRight = true;
                            overlayFlags.renderTopLeft = true;
                            overlayFlags.renderBottomLeft = false;
                            overlayFlags.renderCenter = true;

                            const currentDate = metadata.year + "-" + metadata.month + "-" + metadata.day;
                            const displayCurrentDate = Util.getDateString(metadata.year, metadata.month, metadata.day, this.locale);

                            const overlayData = shashin.getOverlayData(metadata, {
                                cOnClickFunction: "shashin.openGallery",
                                galleryIndex: currentMediaLinkIndex,
                                overlayFlags
                            });

                            mediaContentList.push(shashin.getMediaContent(metadata));
                            appendedCount += 1;

                            const appendClass = "appendAlbumPhotos";
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

                            lastLgIndex += 1;
                        }
                    }

                    this.lastLgIndex = lastLgIndex;
                    this.rendering = false;
                    $("#spinner").css("display", "none");
                } else {
                    $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                    this.rendering = false;
                    this.eol = true;
                }
            } else {
                $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
                $("#msgTimeline").html(message);
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $(".appendAlbumPhotos").last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");
        return mediaContentList;
    }
}