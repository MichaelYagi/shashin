class Folders {

    constructor(foldersList, activePage) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.foldersList = foldersList;
        this.activePage = activePage;
    }

    async init() {
        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendFoldersPhotos", this.foldersList);

        $('[data-bs-toggle="tooltip"]').tooltip();
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
        $("#spinner").css("display","block");

        const data = await this.http.ajax("get","/"+activePage+"/"+nextPage);

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("foldersList") && data["status"] === "success") {
            const foldersList = data["foldersList"];

            if (foldersList !== null && foldersList.length > 0) {

                for (const index in foldersList) {
                    let folderObj = foldersList[index];
                    let appendClass = "appendFoldersPhotos";
                    let folder = folderObj.folder;
                    let thumbnailUrlCentered = folderObj.thumbnailUrlCentered;
                    let count = folderObj.count;

                    $(getFoldersCard({folder, thumbnailUrlCentered, count, appendClass})).insertBefore($("."+appendClass).last());
                }

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $(".appendFoldersPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
            }
        } else {
            $(".appendFoldersPhotos").last().text("EOL").css("display","none");
            this.rendering = false;
        }

        $("#spinner").css("display","none");

        return data;
    }
}