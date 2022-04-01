class Search {

    constructor(searchTerm, activePage, metadataSearchList) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.searchTerm = searchTerm;
        this.activePage = activePage;
        this.metadataSearchList = metadataSearchList;
        this.mediaContentList = shashin.initLightGallery('scroll-gallery',{dynamic:true,plugins:[lgMetadataDetail],metadataDetail:true},'.mediaLink');
    }

    async init() {
        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendSearchPhotos", this.metadataSearchList);

        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateSearch(this.page, this.searchTerm, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }
    }

    async updateSearch(nextPage,searchTerm,activePage) {
        this.rendering = true;
        $("#spinner").css("display","block");

        const data = await this.http.ajax("get","/search/"+nextPage+"?searchTerm="+encodeURIComponent(searchTerm));

        const mediaContentList = [];
        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("metadataSearchList") && data["status"] === "success") {
            const metadataList = data["metadataSearchList"];

            if (metadataList !== null && metadataList.length > 0) {
                const mediaLinkLength = $(".mediaLink").length;

                for (const index in metadataList) {
                    const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                    const metadata = metadataList[index];

                    let dateHeadingObj = null;
                    const overlayFlags = {};
                    overlayFlags.renderTopRight = true;
                    overlayFlags.renderTopLeft = false;
                    overlayFlags.renderBottomLeft = true;
                    overlayFlags.renderCenter = true;

                    const dateHeadingCount = $(".dateSection").length;
                    const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                    const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                    const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                    if (lastDateHeading !== currentDate) {
                        dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                    }

                    const overlayData = shashin.getOverlayData(metadata,{cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,overlayFlags});

                    mediaContentList.push(shashin.getMediaContent(metadata));

                    const appendClass = "appendSearchPhotos";
                    $(PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData})).insertAfter($("."+appendClass).last());
                }

                $("#spinner").css("display", "none");
                this.rendering = false;
            } else {
                $(".appendSearchPhotos").last().text("EOL").css("display","none")
                this.rendering = false;
                $("#spinner").css("display","none");
            }
        } else {
            $(".appendSearchPhotos").last().text("EOL").css("display","none")
            this.rendering = false;
            $("#spinner").css("display","none");
        }

        return mediaContentList;
    }
}