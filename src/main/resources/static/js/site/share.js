class ShareAlbum {

    constructor(shareLink, activePage, albumId, albumMetadataList, lastDate, locale) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.shareLink = shareLink;
        this.activePage = activePage;
        this.albumId = albumId;
        this.albumMetadataList = albumMetadataList;
        this.eol = false;
        this.lastDate = lastDate;
        this.locale = locale;
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',{dynamic:true},'.mediaLink');
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendAlbumPhotos", this.albumMetadataList, this.activePage);
        }, 0);
        await this.clearMultiSelectListener();
    }

    async loadNextPage() {
        if (this.albumId > 0 && this.rendering === false) {
            // console.log(this.page)
            this.updateAlbum(this.albumId, this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }

        return this.eol;
    }

    getShareLink() {
        return this.shareLink;
    }

    async updateAlbum(albumId, nextPage, activePage) {
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

                    for (let index in albumMetadataList) {
                        index = parseInt(index);
                        const currentMediaLinkIndex = (mediaLinkLength + index);
                        const metadata = albumMetadataList[index];

                        if ($("#photoThumbnailContainer"+metadata.id).length === 0) {
                            let dateHeadingObj = null;
                            const overlayFlags = {};
                            overlayFlags.renderTopRight = true;
                            overlayFlags.renderTopLeft = true;
                            overlayFlags.renderBottomLeft = false;
                            overlayFlags.renderCenter = true;

                            let lastDate = albumMetadataList.hasOwnProperty(index-1) ? albumMetadataList[index-1].year+ "-" + albumMetadataList[index-1].month + "-" + albumMetadataList[index-1].day : "";
                            if (this.lastDate !== "") {
                                lastDate = this.lastDate;
                                this.lastDate = "";
                            }
                            const currentDate = metadata.year + "-" + metadata.month + "-" + metadata.day;
                            const nextDate = albumMetadataList.hasOwnProperty(index+1) ? albumMetadataList[index+1].year + "-" + albumMetadataList[index+1].month + "-" + albumMetadataList[index+1].day : "";
                            const displayCurrentDate = Util.getDateString(metadata.year, metadata.month, metadata.day, this.locale);

                            if (lastDate !== currentDate || $("#"+currentDate).length === 0) {
                                dateHeadingObj = {
                                    heading: currentDate,
                                    display: displayCurrentDate
                                };
                            }

                            const overlayData = shashin.getOverlayData(metadata, {
                                cOnClickFunction: "shashin.openGallery",
                                galleryIndex: currentMediaLinkIndex,
                                overlayFlags
                            });

                            mediaContentList.push(shashin.getMediaContent(metadata));

                            const appendClass = "appendAlbumPhotos";
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
                            }

                            if ($("#"+currentDate).length > 0 && nextDate !== "" && currentDate !== nextDate) {
                                $("<span class='"+appendClass+"' style='width:0;height:0;padding:0'></span>").insertAfter($("#"+currentDate));
                            }
                        }
                    }

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

    async clearMultiSelectListener() {
        const albumId = this.albumId;
        const shareLink = this.shareLink;

        $("#clearMultiSelect").off("click").on("click", function() {
            shashin.clearSelection("album");
            $("#clearMultiSelect").hide();
            $("#albumNumberSelected").hide();
            shashin.removeAllMetadataIdList();
            $("#downloadWrapper").attr("action", '/download/share/' + shareLink + '/album/' + albumId);
            const downloadEl = $("#download" + albumId);
            downloadEl.attr("name", "download");
            downloadEl.attr("value", albumId);
            downloadEl.attr("title", "Download all photos");
        });
    }
}