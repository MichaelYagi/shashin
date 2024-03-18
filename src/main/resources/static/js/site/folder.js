class Folder {

    constructor(metadataList, activePage, folderName) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.folderName = folderName;
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
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',lgConfig,'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendFolderPhotos", this.metadataList);
        shashin.mouseMoveListener();
        shashin.closeGalleryOnBack();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateRecent(this.page, this.folderName, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);

                if (this.eol) {
                    setTimeout(() => {Util.reinitLightGalleryInstance({timeoutValue:0,mediaContentList:additionalMediaContentList,refreshContent:true});}, 0);
                }
            }.bind(this));
        }
    }

    async updateRecent(nextPage,folderName,activePage) {
        setTimeout(async () => {
            this.rendering = true;

            let data = null

            if (false === this.eol) {
                $("#spinner").css("display", "block");
                data = await this.http.ajax("get", "/folder/" + nextPage + "/" + encodeURI(encodeURIComponent(folderName)));
            }

            const mediaContentList = [];
            if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data["status"] === shashin.apiResponse.SUCCESS) {
                const metadataList = data["metadataList"];

                if (metadataList !== null && metadataList.length > 0) {
                    const mediaLinkLength = $(".mediaLink").length;
                    const appendClass = "appendFolderPhotos";

                    for (const index in metadataList) {
                        const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                        const metadata = metadataList[index];
                        if ($("#photoThumbnailContainer" + metadata.id).length === 0) {
                            let dateHeadingObj = null;
                            const overlayFlags = {};
                            overlayFlags.renderTopRight = true;
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
                                activePage,
                                appendClass,
                                dateHeadingObj,
                                metadata,
                                currentMediaLinkIndex,
                                overlayData,
                                uuid
                            })).insertBefore($("." + appendClass).last());
                        }
                    }

                    this.rendering = false;
                    $("#spinner").css("display", "none");
                } else {
                    this.rendering = false;
                    this.eol = true;
                    $(".appendFolderPhotos").last().text("EOL").css("display","none")
                }
            } else {
                this.rendering = false;
                this.eol = true;
                $(".appendFolderPhotos").last().text("EOL").css("display","none")
            }

            $("#spinner").css("display","none");

            return mediaContentList;
        }, 0);
    }
}