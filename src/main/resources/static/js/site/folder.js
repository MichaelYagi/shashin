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
            lgConfig.metadataDetail = true;
            lgConfig.metadataDetailFun = shashin.openEditMetadataModal;
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
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendFolderPhotos", this.metadataList, this.activePage);
        }, 0);
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

        return this.eol;
    }

    async updateRecent(nextPage,folderName,activePage) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/folder/" + nextPage + "/" + encodeURI(encodeURIComponent(folderName.replace(";", "%3B"))));
        }

        const mediaContentList = [];
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data.status === shashin.apiResponse.SUCCESS) {
            const metadataList = data.metadataList;
            const favoritesMap = data.favorites;

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
                        overlayFlags.renderBottomRight = true;

                        const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].favorite === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                        const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].count > 0 ? favoritesMap[metadata.id].count : 0;

                        const overlayData = shashin.getOverlayData(metadata, {
                            blOnClickFunction: "Folder.openFolderModal",
                            onClickIdPrefix: "folderModalEdit",
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
                        $(GalleryTemplates.PhotoGalleryItem({
                            activePage,
                            metadata,
                            overlayData,
                            uuid
                        })).insertBefore($("." + appendClass).last());
                        shashin.updateFavorites("#favorite","#brfavoriteicon","#briconcount", metadata.id);
                    }
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                this.rendering = false;
                this.eol = true;
                $(".appendFolderPhotos").last().text("EOL").css("display","none");
            }
        } else {
            this.rendering = false;
            this.eol = true;
            $(".appendFolderPhotos").last().text("EOL").css("display","none");
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }

    static openFolderModal(e,metadataId) {
        e.preventDefault();

        shashin.getMetadata(metadataId).then(function (metadata) {
            if (metadata !== null) {
                // Clear modal data
                $("#folderModalTitle").text(metadata.title);
                $('#propFolderModal').find(':input').val('');
                $("#setCoverFolder").val("yes");
                $("#propFolderModalThumbnail").html("");

                $("#metadataId").val(metadata.id);
                if (metadata.thumbnailUrlCentered !== null) {
                    const version = Util.getMetadataLocalStorage();
                    $("#propFolderModalThumbnail").html('<img loading="lazy" src="/api/v1/thumbnails/centered/' + metadata.id + (version === "" ? "" : "?v=" + version) + '" height="100" width="100" draggable="false">');
                    $("#folderCoverUrl").val("/api/v1/thumbnails/centered/" + metadata.id);
                }

                // Open modal window
                $("#propFolderModal").modal('show');
            }
        });
    }
}