class Trash {
    constructor(activePage, metadataList) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.activePage = activePage;
        this.metadataList = metadataList;
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendMetadataPhotos", this.metadataList);

        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateTrash(this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }
    }

    async updateTrash(nextPage, activePage) {
        this.rendering = true;
        $("#spinner").css("display","block");

        const data = await this.http.ajaxGet("/trash/" + nextPage);

        const mediaContentList = [];
        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === "success") {
                if (data.hasOwnProperty("metadataList")) {
                    const metadataList = data["metadataList"];

                    if (metadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;

                        for (const index in metadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = metadataList[index];

                            let dateHeadingObj = null;
                            let renderTopRight = null;
                            let renderTopLeft = null;
                            let renderBottomLeft = null;
                            let renderCenter = null;

                            const dateHeadingCount = $(".dateSection").length;
                            const lastModifiedDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                            const currentModifiedDate = dateFormat(metadata["modifiedAt"], "isoDate");
                            const displayCurrentModifiedDate = dateFormat(metadata["modifiedAt"], "ddd, mmm d, yyyy");

                            if (lastModifiedDateHeading !== currentModifiedDate) {
                                dateHeadingObj = {heading: currentModifiedDate, display: displayCurrentModifiedDate};
                            }

                            const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                            renderTopRight = {type:metadata.type, id:metadata.id, content:duration, width:metadata.originalImageWidth, height:metadata.originalImageHeight, isTagged:false};
                            renderTopLeft = {id:metadata.id};
                            renderBottomLeft = {id:metadata.id, targetPrefix:null, onclickIdPrefix:null, onclickFunctionCall:null, editControls: false};
                            renderCenter = {metadata:metadata,onclickFunctionCall:"shashin.openGallery",index:currentMediaLinkIndex};

                            mediaContentList.push(shashin.getMediaContent(metadata));

                            const appendClass = "appendMetadataPhotos";
                            $(PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, renderTopRight, renderTopLeft, renderBottomLeft, renderCenter})).insertBefore($("."+appendClass).last());
                        }

                        this.rendering = false;
                        $("#spinner").css("display", "none");
                    } else {
                        this.rendering = false;
                        $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                    }
                }
            } else {
                this.rendering = false;
                $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                $("#msgTimeline").html(message);
            }
        } else {
            this.rendering = false;
            $(".appendMetadataPhotos").last().text("EOL").css("display", "none")
        }

        $("#spinner").css("display","none");
        return mediaContentList;
    }
}