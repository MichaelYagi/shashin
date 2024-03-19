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
        createOnScrollListener($("#container"),this.loadNextPage());
        createOnScrollListener($("main"),this.loadNextPage());

        function createOnScrollListener(element, fun) {
            const appendClassObj = $(".appendFoldersPhotos");
            element.on('scroll', async function () {
                shashin.showScrollToTop(element);
                if (Util.atEndOfPage(this) && appendClassObj[appendClassObj.length-1].textContent !== "EOL") {
                    await fun();
                }
            })
        }

        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            scrollToTopButton.on("click",function () {
                $("main")[0].scrollTo({top: 0, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: 0, behavior: 'smooth'});
            });
        }
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

        let data = null

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/" + activePage + "/" + nextPage);
        }

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("foldersList") && data["status"] === shashin.apiResponse.SUCCESS) {
            foldersList = data["foldersList"];

            if (foldersList !== null && foldersList.length > 0) {

                for (const index in foldersList) {
                    const folderObj = foldersList[index];
                    const folderId = folderObj.id;
                    const folder = folderObj.folder;
                    const thumbnailUrlCentered = folderObj.thumbnailUrlCentered;
                    const count = folderObj.count;

                    if ($("#folder"+folderId).length === 0) {
                        $(GalleryTemplates.getFoldersCard({
                            folderId,
                            folder,
                            thumbnailUrlCentered,
                            count,
                            appendClass
                        })).insertBefore($("." + appendClass).last());
                    }
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