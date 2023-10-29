class Person {
    constructor(metadataList, activePage, personId, canEdit) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.metadataList = metadataList;
        this.activePage = activePage;
        this.personId = personId;
        this.canEdit = canEdit;
        this.eol = false;
        const lgConfig = {
            dynamic:true,
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig["metadataDetail"] = true;
            lgConfig["metadataDetailFun"] = shashin.openInfoSidebar;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig["videoThumbnail"] = true;
            lgConfig["videoThumbnailFun"] = shashin.processVideoThumbnail;
        }
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',lgConfig,'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendPersonPhotos", this.metadataList);
        shashin.matchingListeners();
        shashin.mouseMoveListener();

        $('#savePersonModal').on("click", async function (e) {

            e.preventDefault();
            $("#personModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#personModalStatus").css("visibility", "visible");
            $("#personModalStatus").attr("title", "");
            $("#personModalCancel").prop('disabled', true);

            $('#personModalMsg').html("");
            const metadataId = $("#metadataId").val();
            const personId = $("#personId").val();

            let requestJson = {
                setCoverPerson: $('#setCoverPerson').prop("checked"),
                metadataId: metadataId,
                personId: personId
            }

            const http = new Http("updating album");
            const data = await http.ajax("post", "/person/update", JSON.stringify(requestJson), function () {
                $("#personModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#personModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#personModalCancel").prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === "redirect") {
                    // window.location.replace(data["msg"]);
                    window.top.location = window.top.location
                    $("#personMessage").html('<div class="alert alert-success" role="alert">' + data["msg"] + '</div>');
                } else {
                    if (data["status"] === "success") {
                        $("#personModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                        $("#personModalCancel").prop('disabled', false);
                    } else {
                        $("#personModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                        $("#personModalStatus").attr("title", shashin.modalStatusFailMessage());
                        $("#personModalCancel").prop('disabled', false);
                    }
                }
            } else {
                $("#personModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#personModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#personModalCancel").prop('disabled', false);
            }

            return false;
        });

        $('#propPersonModal').on('hide.bs.modal', function () {
            $("#personModalStatus").attr("class","spinner-grow me-auto");
            $("#personModalStatus").css("visibility","hidden");
        });
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updatePerson(this.personId, this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);

                if (this.eol) {
                    setTimeout(() => {Util.reinitLightGalleryInstance({timeoutValue:0,mediaContentList:additionalMediaContentList,refreshContent:true});}, 0);
                }
            }.bind(this));
        }
    }

    async updatePerson(personId,nextPage,activePage) {
        this.rendering = true;

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/person/" + personId + "/" + nextPage);
        }

        const mediaContentList = [];
        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === "success") {
                if (data.hasOwnProperty("metadataList")) {
                    const metadataList = data["metadataList"];
                    const recognitionLabels = data["recognitionLabels"];
                    const labelPhotoMap = data["labelPhotoMap"];

                    if (metadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;
                        const appendClass = "appendPersonPhotos";

                        for (const index in metadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = metadataList[index];

                            let dateHeadingObj = null;
                            const overlayFlags = {};
                            overlayFlags.renderTopRight = true;
                            overlayFlags.renderTopLeft = true;
                            overlayFlags.renderBottomLeft = true;
                            overlayFlags.renderCenter = true;

                            let overlayData;

                            if (this.canEdit === true) {
                                overlayData = shashin.getOverlayData(metadata, {labelPhotoMap:labelPhotoMap,/*onClickIdPrefix:"propperson"*/blOnClickFunction:"personSettings.openPersonModal",cOnClickFunction:"shashin.openGallery",onClickIdPrefix:"personModalEdit",galleryIndex:currentMediaLinkIndex,overlayFlags});
                            } else {
                                overlayData = shashin.getOverlayData(metadata, {labelPhotoMap:labelPhotoMap,cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,overlayFlags});
                            }

                            mediaContentList.push(shashin.getMediaContent(metadata));

                            const uuid = uuidv4();
                            $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData, uuid})).insertBefore($("."+appendClass).last()).ready(function () {
                                // Call JS and modal
                                // personModalSettings.renderPersonModal(metadata, recognitionLabels, labelPhotoMap[metadata.id]["labels"]);
                            });
                        }

                        $("#spinner").css("display","none");
                        this.rendering = false;
                    } else {
                        $(".appendPersonPhotos").last().text("EOL").css("display", "none");
                        this.rendering = false;
                        this.eol = true;
                    }
                }
            } else {
                $(".appendPersonPhotos").last().text("EOL").css("display", "none");
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                $("#msgTimeline").html(message);
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $(".appendPersonPhotos").last().text("EOL").css("display", "none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return mediaContentList;
    }
}

(function( personSettings, $, undefined ) {
    personSettings.openPersonModal = function (e,metadataId) {
    e.preventDefault();

    shashin.getMetadata(metadataId).then(function (metadata) {
        if (metadata !== null) {
            // Clear modal data
            $("#personModalTitle").text(metadata.title)
            $('#propPersonModal').find(':input').val('');
            $("#setCoverPerson")[0].checked = true;
            $("#propPersonModalThumbnail").html("");

            $("#metadataId").val(metadata.id);
            if (metadata.thumbnailUrlCentered !== null) {
                $("#propPersonModalThumbnail").html('<img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlCentered) + '" height="100" width="100">');
            }

            // Open modal window
            $("#propPersonModal").modal('show');
        }
    });
}
}( window.personSettings = window.personSettings || {}, jQuery ));