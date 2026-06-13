class Taken {

    constructor(metadataList, mediaTypeFilter, activePage, lastDate, locale, lastLgIndex) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.mediaTypeFilter = mediaTypeFilter;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.lastSectionDate = lastDate;
        this.lastSectionId = lastDate;
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
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',lgConfig,mediaElement);
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendTakenPhotos", this.metadataList, this.activePage);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            this.updateTaken(this.page, this.activePage, this.mediaTypeFilter, this.lastLgIndex).then(function (additionalMediaContentList) {
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

    async updateTaken(nextPage,activePage,mediaTypeFilter,lastLgIndex) {
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

                    const overlayFlags = {};
                    overlayFlags.renderTopRight = true;
                    overlayFlags.renderTopLeft = true;
                    overlayFlags.renderBottomLeft = true;
                    overlayFlags.renderCenter = true;
                    overlayFlags.renderBottomRight = true;

                    const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].favorite === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                    const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].count > 0 ? favoritesMap[metadata.id].count : 0;

                    const currentDate = metadata.year + "-" + metadata.month + "-" + metadata.day;
                    const displayCurrentDate = Util.getDateString(metadata.year, metadata.month, metadata.day, this.locale);

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

                    // Always append new sections at the end of the gallery (never insert into an
                    // earlier date section) so that mediaContentList order matches DOM order, which
                    // is required for lightGallery's index-based next/prev navigation.
                    let sectionId = this.lastSectionId;

                    if (this.lastSectionDate !== currentDate) {
                        sectionId = currentDate + ($("#"+currentDate).length > 0 ? "-" + uuid : "");
                        const headerAndBody = '<section class="dateSection" id="' + sectionId + '"><div class="dateHeader" id="dateHeader' + sectionId + '"><span id="select'+sectionId+'" class="bi-circle pe-2 day-select" style="font-size: 0.85rem;color: lightgray;display: none"></span><span class="text-muted">'+shashin.getTranslatedValue('main.pages.browse.header.taken')+' </span><strong>' + displayCurrentDate + '</strong>&nbsp;' + placename + '</div><div id="dateBody' + sectionId + '" class="row" style="margin-left:-2px;"></div></section>';
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

                    shashin.dayHeadingListener(sectionId, activePage, mediaTypeFilter);

                    lastLgIndex += 1;
                }

                this.lastLgIndex = lastLgIndex;
                $('a').attr('draggable', 'false');
                $('img').attr('draggable', 'false');
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