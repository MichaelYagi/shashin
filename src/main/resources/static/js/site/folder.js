class Folder {

    constructor(metadataList, activePage, folderName) {
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.folderName = folderName;
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    init() {
        $(function() {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        shashin.pageLoader(this.loadNextPage.bind(this), ".appendFolderPhotos", this.metadataList, true);

        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    loadNextPage() {
        const currentPage = parseInt($("#currentPage").val());
        const nextPage = currentPage + 1;

        const additionalMediaContentList = this.updateRecent(nextPage, this.folderName, this.activePage);
        this.mediaContentList = shashin.updateMediaContent(this.mediaContentList,additionalMediaContentList);
        $("#currentPage").val(nextPage);
    }

    async updateRecent(nextPage,folderName,activePage) {
        $("#spinner").css("display","block");

        const ajaxParams = {
            type: 'get',
            url: "/folder/"+nextPage+"/"+encodeURI(encodeURIComponent(folderName)),
            contentType: 'application/json; charset=utf-8',
            async:true,
            retries: shashin.ajaxRetries
        }

        const promise = $.ajax(ajaxParams)
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
                            $(html).insertAfter($(".appendFolderPhotos").last());

                            $("#timelineModalEdit"+metadata.id).attr("tag", metadata.id);
                            $("#timelineModalEdit"+metadata.id).on("click", function(e) {
                                e.preventDefault();
                                shashin.openEditMetadataModal(metadata.id);
                            });

                            shashin.setPhotoOverlays(metadata, activePage);
                            Util.activateMetadataListeners(metadata);

                            html = "";
                        }
                    } else {
                        $(".appendFolderPhotos").last().text("EOL").css("display","none")
                    }
                } else {
                    $(".appendFolderPhotos").last().text("EOL").css("display","none")
                }

                $("#spinner").css("display","none");

                return mediaContentList;
            });

        return promise.done(function(data) {
            return data;
        });
    }
}