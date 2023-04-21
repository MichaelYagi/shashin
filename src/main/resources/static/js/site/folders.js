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
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendFoldersPhotos", this.foldersList);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateFolders(this.page, this.activePage).then(function(data) {
                this.page++;
            }.bind(this));
        }
    }

    async updateFolders(nextPage,activePage) {
        this.rendering = true;

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/" + activePage + "/" + nextPage);
        }

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("foldersList") && data["status"] === "success") {
            const foldersList = data["foldersList"];

            if (foldersList !== null && foldersList.length > 0) {

                for (const index in foldersList) {
                    const folderObj = foldersList[index];
                    const appendClass = "appendFoldersPhotos";
                    const folder = folderObj.folder;
                    const thumbnailUrlCentered = folderObj.thumbnailUrlCentered;
                    const count = folderObj.count;

                    $(GalleryTemplates.getFoldersCard({folder, thumbnailUrlCentered, count, appendClass})).insertBefore($("."+appendClass).last());
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $(".appendFoldersPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $(".appendFoldersPhotos").last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return data;
    }
}