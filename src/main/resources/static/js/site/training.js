class Training {

    constructor(resultList, personId, activePage, initialCursor = null, initialHasMore = false) {
        this.http = new Http(activePage);
        this.cursor = initialCursor;
        this.rendering = false;
        this.resultList = resultList;
        this.activePage = activePage;
        this.personId = personId;
        this.eol = !initialHasMore;
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendTrainingPhotos", this.resultList);
        }, 0);

        $("#resyncArgusBtn").on("click", async (e) => {
            e.preventDefault();
            $("#resyncArgusBtn").prop("disabled", true);
            $("#resyncArgusStatus").text("Syncing…").show();
            const data = await this.http.ajax("get", "/person/argus/" + this.personId + "/resync");
            if (data && data.status === shashin.apiResponse.SUCCESS) {
                $("#resyncArgusStatus").text(data.msg);
                setTimeout(() => location.reload(), 1000);
            } else {
                $("#resyncArgusStatus").text((data && data.msg) ? data.msg : "Sync failed.");
                $("#resyncArgusBtn").prop("disabled", false);
            }
        });
    }

    async loadNextPage() {
        if (this.rendering === false) {
            this.updateTraining(this.cursor, this.personId).then(function (data) {
                this.cursor = data ? (data.next_cursor || null) : null;
            }.bind(this));
        }

        return this.eol;
    }

    async updateTraining(cursor, personId) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            const url = cursor
                ? "/person/argus/" + personId + "/gallery?cursor=" + encodeURIComponent(cursor)
                : "/person/argus/" + personId + "/gallery";
            data = await this.http.ajax("get", url);
        }

        if (data !== null && data.hasOwnProperty("resultList")) {
            const resultList = data.resultList;

            if (resultList !== null && resultList.length > 0) {
                for (const index in resultList) {
                    const resultObj = resultList[index];
                    const embeddingId = resultObj.id;
                    const imgSrc = argusServer + resultObj.crop_url;

                    const metadataId = resultObj.metadata_id || '';

                    let html = '<div id="photoThumbnailContainer'+embeddingId+'" class="photo-thumbnail-container photo-thumbnail" ';
                    html += 'data-metadata-id="'+metadataId+'" ';
                    html += 'style="float: left; padding-left:0;padding-right:0;">';
                    html += '<a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+index+'"></a>';
                    html += '<img loading="lazy" src="'+imgSrc+'" ';
                    html += 'style="height:225px;" class="photo-thumbnail-image" id="image'+embeddingId+'" draggable="false">';
                    html += '<div class="thumbnail-tl" id="tntl'+embeddingId+'">';
                    html += '<a href="#" id="select'+embeddingId+'">';
                    html += '<span id="tlicon'+embeddingId+'" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>';
                    html += '</a></div>';
                    if (metadataId !== '') {
                        html += '<div class="thumbnail-centered" id="tncentered' + embeddingId +'" style="cursor:pointer;">';
                        html += '<a href="#" class="training-gallery-btn" data-embedding-id="' + embeddingId + '">';
                        html += '<span class="bi-play-btn" style="font-size: 4rem;color: lightgray;">';
                        html += '</span></a></div>';
                    }
                    html += '<div class="thumbnail-tr" id="tntr' + embeddingId +'">';
                    if (resultObj.metadata_date != null && resultObj.metadata_date !== '') {
                        html += '<a href="/timeline#' + resultObj.metadata_date + '" target="_blank" style="color:inherit;">';
                        html += '<span id="timelineviewable' + embeddingId + '" class="bi-calendar overlayIcon overlayIconBackground"></span></a><br>';
                    }
                    html += '<a href="#" class="training-reassign-btn" data-detection-id="' + embeddingId + '" data-crop-url="' + imgSrc + '">';
                    html += '<span class="bi-person-fill-x overlayIcon overlayIconBackground" style="color:orange;"></span></a>';
                    html += '</div>';
                    html += '</div>';

                    $(html).insertBefore($(".appendTrainingPhotos").last());

                    setPhotoOverlays(embeddingId);
                    if (metadataId && typeof shashin.addToTrainingGallery === 'function') {
                        shashin.addToTrainingGallery(embeddingId, metadataId);
                    }
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
                if (data.has_more === false) {
                    this.eol = true;
                    $(".appendTrainingPhotos").last().text("EOL").css("display","none");
                }
            } else {
                $(".appendTrainingPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            this.rendering = false;
            this.eol = true;
            $(".appendTrainingPhotos").last().text("EOL").css("display","none");
        }

        $("#spinner").css("display","none");

        return data;
    }
}