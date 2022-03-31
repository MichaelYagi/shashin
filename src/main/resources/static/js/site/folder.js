class Folder {

    constructor(metadataList, activePage, folderName) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.folderName = folderName;
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    async init() {
        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendFolderPhotos", this.metadataList);

        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateRecent(this.page, this.folderName, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }
    }

    async updateRecent(nextPage,folderName,activePage) {
        this.rendering = true;
        $("#spinner").css("display","block");

        const data = await this.http.ajaxGet("/folder/"+nextPage+"/"+encodeURI(encodeURIComponent(folderName)));

        const mediaContentList = [];
        if (data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data["status"] === "success") {
            const metadataList = data["metadataList"];

            if (metadataList !== null && metadataList.length > 0) {
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
                    renderTopRight = {type:metadata.type, id:metadata.id, content:duration, width:metadata.originalImageWidth, height:metadata.originalImageHeight, isTagged:false};
                    renderCenter = {metadata:metadata,onclickFunctionCall:"shashin.openGallery",index:currentMediaLinkIndex};
                    renderTopLeft = {id:metadata.id};
                    renderBottomLeft = {id:metadata.id, targetPrefix:null, onclickIdPrefix:null, onclickFunctionCall:null, editControls: true, editIcon: ((metadata.lat === null || metadata.lng === null) ? 'bi-pencil-square' : 'bi-pencil')};

                    mediaContentList.push(shashin.getMediaContent(metadata));

                    const appendClass = "appendFolderPhotos";
                    $(PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, renderTopRight, renderTopLeft, renderBottomLeft, renderCenter})).insertBefore($("."+appendClass).last());
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                this.rendering = false;
                $(".appendFolderPhotos").last().text("EOL").css("display","none")
            }
        } else {
            this.rendering = false;
            $(".appendFolderPhotos").last().text("EOL").css("display","none")
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}