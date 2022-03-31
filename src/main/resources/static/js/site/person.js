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
                            let renderTopRight = null;
                            let renderTopLeft = null;
                            let renderBottomLeft = null;
                            let renderCenter = null;

                            const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                            renderTopRight = {type:metadata.type, id:metadata.id, content:duration, width:metadata.originalImageWidth, height:metadata.originalImageHeight, isTagged:(this.canEdit === true && labelPhotoMap[metadata.id]["isTagged"] === true)};

                            if (this.canEdit === true) {
                                renderTopLeft = {id:metadata.id};
                                renderBottomLeft = {id:metadata.id, targetPrefix:'propperson', onclickIdPrefix:null, onclickFunctionCall:null, editControls: false};
                            } else {
                                renderBottomLeft = {id:metadata.id, targetPrefix:null, onclickIdPrefix:null, onclickFunctionCall:null, editControls: false};
                            }

                            renderCenter = {metadata:metadata,onclickFunctionCall:"shashin.openGallery",index:currentMediaLinkIndex};

                            mediaContentList.push(shashin.getMediaContent(metadata));

                            const appendClass = "appendPersonPhotos";
                            $(PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, renderTopRight, renderTopLeft, renderBottomLeft, renderCenter})).insertBefore($("."+appendClass).last()).ready(function () {
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