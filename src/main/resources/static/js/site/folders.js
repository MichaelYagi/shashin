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
                    let folder = foldersList[index];
                    let appendClass = "appendFoldersPhotos";

                    let html = '<div class="card" style="width:235px;padding-top:10px;">';

                    html += '<a href="/folder/'+encodeURIComponent(encodeURIComponent(folder.folder))+'" style="text-decoration: none !important;color: #777777;">';
                    html += '<img loading="lazy" class="card-img-top" src="'+folder.thumbnailUrlCentered+'" width="209" height="209" style="width: 209px;height: 209px;"></a>';
                    html += '<div class="card-body"><p class="card-text"><strong>'+folder.folder+'</strong></p>';
                    html += '<p class="card-text"><small class="text-muted">'+folder.count+' items</small></p></div></div>';
                    html += '<span class="'+appendClass+'" style="width:0;height:0;padding:0"></span>';
                    $(html).insertBefore($("."+appendClass).last());
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