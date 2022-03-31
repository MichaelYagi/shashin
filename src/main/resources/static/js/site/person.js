class Person {

    constructor(metadataList, activePage, personId, canEdit) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.personId = personId;
        this.canEdit = canEdit;
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    async init() {
        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendPersonPhotos", this.metadataList);

        shashin.matchingListeners();
        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updatePerson(this.personId, this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }
    }

    async updatePerson(personId,nextPage,activePage) {
        this.rendering = true;
        $("#spinner").css("display","block");

        const data = await this.http.ajaxGet("/person/" + personId + "/" + nextPage);

        const mediaContentList = [];
        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === "success") {
                if (data.hasOwnProperty("metadataList")) {
                    const metadataList = data["metadataList"];
                    const recognitionLabels = data["recognitionLabels"];
                    const labelPhotoMap = data["labelPhotoMap"];

                    if (metadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;
                        for (const index in metadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = metadataList[index];

                            let dateHeadingObj = null;
                            let renderTopRight = true;
                            let renderTopLeft = false;
                            let renderBottomLeft = true;
                            let renderCenter = true;

                            let overlayData;

                            if (this.canEdit === true) {
                                overlayData = shashin.getOverlayData(metadata, {labelPhotoMap:labelPhotoMap,onClickIdPrefix:"propperson",cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex});
                                renderTopLeft = {id:metadata.id, overlays:null, data:null};
                            } else {
                                overlayData = shashin.getOverlayData(metadata, {labelPhotoMap:labelPhotoMap,cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex});
                            }

                            mediaContentList.push(shashin.getMediaContent(metadata));

                            const appendClass = "appendPersonPhotos";
                            $(PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, renderTopRight, renderTopLeft, renderBottomLeft, renderCenter, overlayData})).insertBefore($("."+appendClass).last()).ready(function () {
                                // Call JS and modal
                                personModalSettings.renderPersonModal(metadata, recognitionLabels, labelPhotoMap[metadata.id]["labels"]);
                            });
                        }

                        $("#spinner").css("display","none");
                        this.rendering = false;
                    } else {
                        $(".appendPersonPhotos").last().text("EOL").css("display", "none");
                        this.rendering = false;
                    }
                }
            } else {
                $(".appendPersonPhotos").last().text("EOL").css("display", "none");
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                $("#msgTimeline").html(message);
                this.rendering = false;
            }
        } else {
            $(".appendPersonPhotos").last().text("EOL").css("display", "none");
            this.rendering = false;
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}