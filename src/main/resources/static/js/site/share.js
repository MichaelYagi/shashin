class ShareAlbum {

    constructor(shareLink, activePage, albumId, albumName, albumMetadataList) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.shareLink = shareLink;
        this.activePage = activePage;
        this.albumId = albumId;
        this.albumName = albumName;
        this.albumMetadataList = albumMetadataList;
        this.eol = false;
        this.mediaContentList = shashin.initLightGallery('infinite-scroll-gallery',{dynamic:true},'.mediaLink');
    }

    async init() {
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendAlbumPhotos", this.albumMetadataList);
        shashin.mouseMoveListener();
        shashin.closeGalleryOnBack();
        this.renderDownload();
    }

    async loadNextPage() {
        if (this.albumId > 0 && this.rendering === false) {
            // console.log(this.page)
            this.updateAlbum(this.albumId, this.page, this.activePage).then(function (additionalMediaContentList) {
                // console.log(additionalMediaContentList)
                this.page++;
                this.mediaContentList = shashin.updateMediaContent(this.mediaContentList, additionalMediaContentList);
            }.bind(this));
        }

        return parseInt($("#currentPage").val());
    }

    getShareLink() {
        return this.shareLink;
    }

    async updateAlbum(albumId, nextPage, activePage) {
        const self = this;
        self.rendering = true;

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/share/" + self.getShareLink() + "/album/" + albumId + "/" + nextPage);
        }

        const mediaContentList = [];

        if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data["status"] === shashin.apiResponse.SUCCESS) {
                if (data.hasOwnProperty("albumMetadataList")) {
                    const albumMetadataList = data["albumMetadataList"];
                    const mediaLinkLength = $(".mediaLink").length;

                    for (const index in albumMetadataList) {
                        const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                        const metadata = albumMetadataList[index];

                        let dateHeadingObj = null;
                        const overlayFlags = {};
                        overlayFlags.renderTopRight = true;
                        overlayFlags.renderTopLeft = false;
                        overlayFlags.renderBottomLeft = false;
                        overlayFlags.renderCenter = true;

                        const dateHeadingCount = $(".dateSection").length;
                        const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                        const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                        const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                        if (lastDateHeading !== currentDate) {
                            dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                        }

                        const overlayData = shashin.getOverlayData(metadata, {cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,overlayFlags});

                        mediaContentList.push(shashin.getMediaContent(metadata));

                        const appendClass = "appendAlbumPhotos";
                        const uuid = uuidv4();
                        $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData, uuid})).insertBefore($("."+appendClass).last());
                    }

                    this.rendering = false;
                    $("#spinner").css("display", "none");
                } else {
                    $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                    this.rendering = false;
                    this.eol = true;
                }
            } else {
                $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                $("#msgTimeline").html(message);
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $(".appendAlbumPhotos").last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");
        return mediaContentList;
    }

    renderDownload() {
        const albumName = this.albumName;
        const albumId = this.albumId;
        const shareLink = this.shareLink;

        $("#downloadFormContainer").html('<form method="post" action="/download/share/' + shareLink + '/album/' + albumId + '" style="display: inline-block;white-space: nowrap;"><button class="bi-download link-button-lightmode" style="font-size: 2rem;color: #0d6efd;" type="submit" id="download' + albumId + '" name="download" value="' + albumId + '" title="Download share album photos (download videos individually)"></button></form>');

        $("#download"+this.albumId).on("click", function() {
            let downloadTimer;
            const tokenName = "ShashinShareAlbumName";
            const tokenSize = "ShashinShareAlbumSize";
            const configuredAttempts = 120;
            const downloadLocation = $("#download"+this.albumId).attr("href");

            shashin.showToastMessage("Downloading share album", "Downloading share album \""+albumName+"\". Downloading photos only.", {icon:"bi-info-circle", iconColor:"#777777"});
            setTimeout(function () { $("#download"+albumId).removeAttr("href") }, 0);
            Util.setCookie(tokenName, "", "/");
            Util.setCookie(tokenSize, "", "/");

            let attempts = configuredAttempts;

            downloadTimer = setInterval( function() {
                const tokenCookieValue = Util.getCookie(tokenName);
                const tokenCookieSize = Util.getCookie(tokenSize);

                if ((tokenCookieValue !== "" && tokenCookieSize !== "") || attempts === 0) {
                    if (attempts === 0) {
                        // $("#albumsMessage").html("&nbsp;").animate({opacity: 0}, 5000);
                    } else {
                        shashin.showToastMessage("Share album download", "<strong>File name</strong> " + tokenCookieValue + " <strong>File size</strong> " + Util.formatBytes(tokenCookieSize), {icon:"bi-info-circle", iconColor:"#777777"});
                        $("#download" + albumId).attr("href", downloadLocation);
                        Util.deleteCookie(tokenName, "/");
                        Util.deleteCookie(tokenSize, "/");
                        window.clearInterval(downloadTimer);
                    }
                }

                attempts--;
            }, 1000);
        });
    }
}