class Taken {

    constructor(metadataList, mediaTypeFilter, activePage) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.mediaTypeFilter = mediaTypeFilter;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.eol = false;
        const lgConfig = {
            dynamic:true,
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig["metadataDetail"] = true;
            lgConfig["metadataDetailFunc"] = shashin.openInfoSidebar;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig["videoThumbnail"] = true;
            lgConfig["videoThumbnailFunc"] = shashin.processVideoThumbnail;
        }
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',lgConfig,'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendTakenPhotos", this.metadataList);
        shashin.mouseMoveListener();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateTaken(this.page, this.activePage, this.mediaTypeFilter).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);

                if (this.eol) {
                    setTimeout(() => {Util.reinitLightGalleryInstance({timeoutValue:0,mediaContentList:additionalMediaContentList,refreshContent:true});}, 0);
                }
            }.bind(this));
        }
    }

    async updateTaken(nextPage,activePage,mediaTypeFilter) {
        this.rendering = true;

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/taken/mediatype/" + mediaTypeFilter + "/page/" + nextPage);
        }

        const mediaContentList = [];
        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data["status"] === "success") {
            const metadataList = data["metadataList"];

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;
                const appendClass = "appendTakenPhotos";

                for (const index in metadataList) {
                    const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                    const metadata = metadataList[index];

                    let dateHeadingObj = null;
                    const overlayFlags = {};
                    overlayFlags.renderTopRight = true;
                    overlayFlags.renderTopLeft = true;
                    overlayFlags.renderBottomLeft = true;
                    overlayFlags.renderCenter = true;

                    const dateHeadingCount = $(".dateSection").length;
                    const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                    const currentDate = metadata["year"]+"-"+metadata["month"]+"-"+metadata["day"];
                    const displayCurrentDate = dateFormat((metadata["year"]+"-"+metadata["month"]+"-"+metadata["day"]).replace(/-/g, "/"), "ddd, mmm d, yyyy");

                    const placenameMap = data["placenameMap"];
                    let placename = "";
                    if (index === 0 || (index > 0 && placenameMap[metadataList[index-1].year+"-"+metadataList[index-1].month+"-"+metadataList[index-1].day].join(",") !== placenameMap[metadata.year+'-'+metadata.month+'-'+metadata.day].join(","))) {
                        if (placenameMap[metadata.year + '-' + metadata.month + '-' + metadata.day].length === 1) {
                            const placeNameHeaders = placenameMap[metadata.year + '-' + metadata.month + '-' + metadata.day];
                            placename = '<span class="text-muted"><a class="link-unstyled" href="/search?term=' + placeNameHeaders[0] + '" target="_blank">' + placeNameHeaders[0] + '</a></span>';
                        } else if (placenameMap[metadata.year + '-' + metadata.month + '-' + metadata.day].length > 1) {
                            const placeNameHeaders = placenameMap[metadata.year + '-' + metadata.month + '-' + metadata.day];
                            let listHtml = "";
                            if (placeNameHeaders.length > 1) {
                                for (const index in placeNameHeaders) {
                                    const placeNameHeader = placeNameHeaders[index];
                                    listHtml += '<li class="text-muted"><a class="dropdown-item" href="/search?term=' + placeNameHeader + '" target="_blank">' + placeNameHeader + '</a></li>\n';
                                }
                            }
                            placename = '<span class="text-muted"><div class="dropdown" style="display: inline-block;"><a class="dropdown-toggle link-unstyled" data-bs-toggle="dropdown" href="#">' + placeNameHeaders[0] + '</a>\n' +
                                '<ul class="dropdown-menu">' + listHtml + '</ul></div></span>';
                        }
                    }

                    if (lastDateHeading !== currentDate) {
                        dateHeadingObj = {heading: currentDate, display: displayCurrentDate, placename: placename};
                    }

                    const overlayData = shashin.getOverlayData(metadata, {editControls:true,editIcon: ((metadata.lat === null || metadata.lng === null) ? "bi-info-square" : "bi-info-circle"),cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,overlayFlags});

                    mediaContentList.push(shashin.getMediaContent(metadata));

                    const uuid = uuidv4();
                    $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData, uuid})).insertBefore($("."+appendClass).last());
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $(".appendTakenPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $(".appendTakenPhotos").last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}