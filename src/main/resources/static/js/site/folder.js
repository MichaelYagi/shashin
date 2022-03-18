class Folder {

    constructor(metadataList, activePage, folderName) {
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

        const ajaxParams = {
            type: 'get',
            url: "/folder/"+nextPage+"/"+encodeURI(encodeURIComponent(folderName)),
            contentType: 'application/json; charset=utf-8',
            async:true,
            retries: shashin.ajaxRetries
        }

        return await $.ajax(ajaxParams)
            .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating recently added")}).then(function (data) {
                const mediaContentList = [];
                if (data.hasOwnProperty("status") && data.hasOwnProperty("metadataList") && data["status"] === "success") {
                    const metadataList = data["metadataList"];
                    const recognitionLabels = data["recognitionLabels"];
                    const labelPhotoMap = data["labelPhotoMap"];
                    const albumMap = data["albumMap"];
                    const albumList = data["albumList"];
                    const keywordMap = data["keywordMap"];

                    if (metadataList !== null && metadataList.length > 0) {
                        let html = "";
                        const mediaLinkLength = $(".mediaLink").length;

                        for (const index in metadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = metadataList[index];

                            html += '<div id="photoThumbnailContainer' + metadata.id + '" class="photo-thumbnail-container photo-thumbnail" style="width:'+metadata.thumbnailSmallWidth+'px;height:'+metadata.thumbnailSmallHeight+'px;padding-left:0;padding-right:0;">\n';
                            html +=
                                '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+currentMediaLinkIndex+'"></a>\n' +
                                '   <img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\''+metadata.title+'\',Util.thumbnailHeight())">\n' +
                                '   <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n' +
                                '   <input type="hidden" name="thumbnailCentered' + metadata.id + '" id="thumbnailCentered' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlCentered) + '">\n';

                            html += shashin.getTopLeftOverlay(metadata.id);

                            const ediIcon = (metadata.lat === null || metadata.lng === null) ? 'bi-pencil-square' : 'bi-pencil';
                            html +=
                                '   <div class="thumbnail-bl" id="tnbl'+metadata.id+'">\n' +
                                '       <a href="#" id="timelineModalEdit'+metadata.id+'" data-bs-target="#propTimelinModal">\n' +
                                '           <span class="'+ediIcon+'" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                '       </a>\n' +
                                '   </div>\n';

                            const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                            html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                            const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                            html += centeredObj.html;
                            mediaContentList.push(centeredObj.mediaContent);

                            html += '</div>\n<span class="appendFolderPhotos" style="width:0;height:0;padding:0"></span>\n';

                            $(html).insertAfter($(".appendFolderPhotos").last()).ready(function () {
                                $("#timelineModalEdit"+metadata.id).attr("tag", metadata.id);
                                $("#timelineModalEdit"+metadata.id).on("click", function(e) {
                                    e.preventDefault();
                                    shashin.openEditMetadataModal(metadata.id);
                                });

                                shashin.setPhotoOverlays(metadata, activePage);
                                Util.activateMetadataListeners(metadata);

                                if (parseInt(index) === parseInt(metadataList.length) - 1) {
                                    this.rendering = false;
                                }
                            }.bind(this));

                            html = "";
                        }
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
            }.bind(this));
    }
}