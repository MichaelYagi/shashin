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
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',lgConfig,mediaElement);
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendPersonPhotos", this.metadataList, this.activePage);
        }, 0);

        $('#savePersonModal').on("click", async function (e) {

            e.preventDefault();
            $("#personModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#personModalStatus").visible();
            $("#personModalStatus").attr("title", "");
            $("#personModalCancel").prop('disabled', true);

            $('#personModalMsg').html("");
            const metadataId = $("#metadataId").val();
            const personId = $("#personId").val();

            let requestJson = {
                setCoverPerson: $('#setCoverPerson').val() === "yes",
                metadataId: metadataId,
                personId: personId
            };

            const http = new Http("updating album");
            const data = await http.ajax("post", "/person/update", JSON.stringify(requestJson), function () {
                $("#personModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#personModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#personModalCancel").prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data.status === "redirect") {
                    // window.location.replace(data.msg);
                    window.top.location = window.top.location;
                    $("#personMessage").html('<div class="alert alert-success" role="alert">' + data.msg + '</div>');
                } else {
                    if (data.status === shashin.apiResponse.SUCCESS) {
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
            $("#personModalStatus").invisible();
        });
    }

    async loadNextPage() {
        if (this.rendering === false) {
            this.updatePerson(this.personId, this.page, this.activePage).then(function (additionalMediaContentList) {
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

    async updatePerson(personId,nextPage,activePage) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/person/" + personId + "/" + nextPage);
        }

        const mediaContentList = [];
        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data.status === shashin.apiResponse.SUCCESS) {
                if (data.hasOwnProperty("metadataList")) {
                    const metadataList = data.metadataList;
                    const recognitionLabels = data.recognitionLabels;
                    const labelPhotoMap = data.labelPhotoMap;
                    const favoritesMap = data.favorites;

                    if (metadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;
                        const appendClass = "appendPersonPhotos";

                        for (const index in metadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = metadataList[index];

                            if ($("#photoThumbnailContainer"+metadata.id).length === 0) {

                                let dateHeadingObj = null;
                                const overlayFlags = {};
                                overlayFlags.renderTopRight = true;
                                overlayFlags.renderTopLeft = true;
                                overlayFlags.renderBottomLeft = true;
                                overlayFlags.renderCenter = true;
                                overlayFlags.renderBottomRight = true;

                                const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].favorite === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                                const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].count > 0 ? favoritesMap[metadata.id].count : 0;

                                let overlayData;

                                if (this.canEdit === true) {
                                    overlayData = shashin.getOverlayData(metadata, {
                                        labelPhotoMap: labelPhotoMap,/*onClickIdPrefix:"propperson"*/
                                        blOnClickFunction: "personSettings.openPersonModal",
                                        cOnClickFunction: "shashin.openGallery",
                                        onClickIdPrefix: "personModalEdit",
                                        galleryIndex: currentMediaLinkIndex,
                                        favoriteCount: favoriteCount,
                                        favoriteIcon: favoriteIcon,
                                        overlayFlags
                                    });
                                } else {
                                    overlayData = shashin.getOverlayData(metadata, {
                                        labelPhotoMap: labelPhotoMap,
                                        cOnClickFunction: "shashin.openGallery",
                                        galleryIndex: currentMediaLinkIndex,
                                        favoriteCount: favoriteCount,
                                        favoriteIcon: favoriteIcon,
                                        overlayFlags
                                    });
                                }

                                mediaContentList.push(shashin.getMediaContent(metadata));

                                const uuid = uuidv4();
                                $(GalleryTemplates.PhotoGalleryItem({
                                    activePage,
                                    metadata,
                                    overlayData,
                                    uuid,
                                    isMobile: Util.isMobile()
                                })).insertBefore($("." + appendClass).last()).ready(function () {
                                    // Call JS and modal
                                    // personModalSettings.renderPersonModal(metadata, recognitionLabels, labelPhotoMap[metadata.id].labels);
                                    shashin.updateFavorites("#favorite","#brfavoriteicon","#briconcount", metadata.id);
                                });
                            }
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
                message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
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
            $("#personModalTitle").text(metadata.title);
            $('#propPersonModal').find(':input').val('');
            $("#setCoverPerson").val("yes");
            $("#propPersonModalThumbnail").html("");

            $("#metadataId").val(metadata.id);
            if (metadata.thumbnailUrlCentered !== null) {
                $("#propPersonModalThumbnail").html('<img loading="lazy" src="/api/v1/thumbnails/centered/' + metadata.id + '" height="100" width="100" draggable="false">');
            }

            // Open modal window
            $("#propPersonModal").modal('show');
        }
    });
};
}( window.personSettings = window.personSettings || {}, jQuery ));