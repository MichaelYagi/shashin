class Compreface {

    constructor(resultList, personId, activePage) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.resultList = resultList;
        this.activePage = activePage;
        this.personId = personId;
        this.eol = false;
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendCompreFacePhotos", this.resultList);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateCompreface(this.page, this.personId).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
            }.bind(this));
        }

        return this.eol;
    }

    async updateCompreface(nextPage,personId) {
        this.rendering = true;

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/person/compreface/"+personId+"/"+nextPage);
        }

        if (data !== null && data.hasOwnProperty("resultList")) {
            const resultList = data.resultList;

            if (resultList !== null && resultList.length > 0) {
                for (const index in resultList) {
                    const resultObj = resultList[index];

                    let html = '<div id="photoThumbnailContainer'+resultObj.image_id+'" class="photo-thumbnail-container photo-thumbnail" ';
                    html += 'style="float: left; padding-left:0;padding-right:0;">';
                    html += '<a class="lightGalleryIndexAnchor" name="lightGalleryIndex'+index+'"></a>';
                    html += '<img loading="lazy" src="data:image/png;base64,'+resultObj.image_base64+'" ';
                    html += 'style="height:225px;" class="photo-thumbnail-image" id="image'+resultObj.image_id+'" draggable="false">';
                    html += '<div class="thumbnail-tl" id="tntl'+resultObj.image_id+'">';
                    html += '<a href="#" id="select'+resultObj.image_id+'">';
                    html += '<span id="tlicon'+resultObj.image_id+'" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>';
                    html += '</a></div>';
                    if (resultObj.metadata_date != null && resultObj.metadata_date !== '') {
                        html += '<div class="thumbnail-centered" id="tncentered' + resultObj.image_id +'">';
                        html += '<a href="/timeline#' + resultObj.metadata_date + '" target="_blank">';
                        html += '<span class="bi-play-btn" style="font-size: 4rem;color: lightgray;">';
                        html += '</span></a></div>';
                        html += '<div class="thumbnail-tr" id="tntr' + resultObj.image_id +'">';
                        html += '<span id=timelineviewable' + resultObj.image_id + '" class="bi-calendar overlayIcon overlayIconBackground">';
                        html += '</span><br></div>';
                    }
                    html += '</div>';

                    $(html).insertBefore($(".appendCompreFacePhotos").last());

                    setPhotoOverlays(resultObj.image_id);
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $(".appendCompreFacePhotos").last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            this.rendering = false;
            this.eol = true;
            $(".appendCompreFacePhotos").last().text("EOL").css("display","none");
        }

        $("#spinner").css("display","none");

        return data;
    }
}