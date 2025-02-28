class Taken {

    constructor(metadataList, mediaTypeFilter, activePage, lastDate) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.mediaTypeFilter = mediaTypeFilter;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.lastDate = lastDate;
        this.eol = false;
        const lgConfig = {
            dynamic:true,
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig.metadataDetail = true;
            lgConfig.metadataDetailFun = shashin.openInfoSidebar;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig.videoThumbnail = true;
            lgConfig.videoThumbnailFun = shashin.processVideoThumbnail;
        }
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',lgConfig,'.mediaLink');
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendTakenPhotos", this.metadataList, this.activePage);
        }, 0);
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

        return this.eol;
    }

    async updateTaken(nextPage,activePage,mediaTypeFilter) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/taken/mediatype/" + mediaTypeFilter + "/page/" + nextPage);
        }

        const mediaContentList = [];
        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data.status === shashin.apiResponse.SUCCESS) {
            const metadataList = data.metadataList;
            const favoritesMap = data.favorites;
            const placenameMap = data.placenameMap;

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;
                const appendClass = "appendTakenPhotos";

                for (let index in metadataList) {
                    index = parseInt(index);

                    const currentMediaLinkIndex = (mediaLinkLength + index);
                    const metadata = metadataList[index];

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


                    let placename = "";
                    if (index === 0 || (index > 0 && placenameMap[metadataList[index - 1].year + "-" + metadataList[index - 1].month + "-" + metadataList[index - 1].day].join(",") !== placenameMap[metadata.year + '-' + metadata.month + '-' + metadata.day].join(","))) {
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

                    if (lastDate !== currentDate || $("#"+currentDate).length === 0) {
                        dateHeadingObj = {
                            heading: currentDate,
                            display: displayCurrentDate,
                            placename: placename
                        };
                    }

                    const overlayData = shashin.getOverlayData(metadata, {
                        editControls: true,
                        editIcon: ((metadata.lat === null || metadata.lng === null) ? "bi-info-square" : "bi-info-circle"),
                        cOnClickFunction: "shashin.openGallery",
                        galleryIndex: currentMediaLinkIndex,
                        favoriteCount: favoriteCount,
                        favoriteIcon: favoriteIcon,
                        overlayFlags
                    });

                    mediaContentList.push(shashin.getMediaContent(metadata));

                    const uuid = uuidv4();

                    if ($("#"+currentDate).length === 0 && dateHeadingObj !== null) {
                        const headerAndBody = '<section class="dateSection" id="' + currentDate + '"><div class="mb-3" id="dateHeader' + currentDate + '"><span class="text-muted">Taken </span><strong>' + dateHeadingObj.display + '</strong>&nbsp;' + (dateHeadingObj.hasOwnProperty("placename") ? dateHeadingObj.placename : '') + '</div><div id="dateBody' + currentDate + '" class="row" style="margin-left:-2px;"></div></section>';
                        $(headerAndBody).insertBefore($("." + appendClass).last());
                    }

                    const html = $(GalleryTemplates.PhotoGalleryItem({
                        activePage,
                        metadata,
                        overlayData,
                        uuid
                    }));

                    if ($("#dateBody"+currentDate).length > 0) {
                        $(html).appendTo($("#dateBody" + currentDate));
                        shashin.updateFavorites("#favorite","#brfavoriteicon","#briconcount", metadata.id);
                    }

                    if ($("#"+currentDate).length > 0 && nextDate !== "" && currentDate !== nextDate) {
                        $("<span class='"+appendClass+"' style='width:0;height:0;padding:0'></span>").insertAfter($("#"+currentDate));
                    }
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