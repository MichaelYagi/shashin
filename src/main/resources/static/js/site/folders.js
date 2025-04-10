class Folders {

    constructor(foldersList, activePage) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.foldersList = foldersList;
        this.eol = false;
        this.activePage = activePage;
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendFoldersPhotos", this.foldersList);
        }, 0);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateFolders(this.page, this.activePage).then(function(folderList) {
                this.page++;
                this.foldersList.push(folderList);
            }.bind(this));
        }

        return this.eol;
    }

    async updateFolders(nextPage,activePage) {
        this.rendering = true;
        const appendClass = "appendFoldersPhotos";
        let foldersList = [];

        let data = null;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/" + activePage + "/page/" + nextPage);
        }

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("foldersList") && data.status === shashin.apiResponse.SUCCESS) {
            foldersList = data.foldersList;

            if (foldersList !== null && foldersList.length > 0) {

                for (const index in foldersList) {
                    const folderObj = foldersList[index];
                    const folder = folderObj.folder;
                    const thumbnailUrlCentered = "/api/v1/thumbnails/centered/"+folderObj.metadataId;
                    const count = folderObj.count;

                    $(GalleryTemplates.getFoldersCard({
                        folder,
                        thumbnailUrlCentered,
                        count,
                        appendClass
                    })).insertBefore($("." + appendClass).last());
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $("."+appendClass).last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $("."+appendClass).last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return foldersList;
    }
}