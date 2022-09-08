class Settings {

    constructor() {

    }

    async init() {
        // Populate dirs
        const http = new Http("settings");
        let data = await http.ajax("post", "/settings/directorytree", '{"path":""}');
        let os = ""
        if (data.hasOwnProperty("os")) {
            os = data["os"];
        }
        populateDirs(data);

        listClick();

        function populateDirs(data) {
            $("#dirList").empty();

            if (data.hasOwnProperty("dirs")) {
                let dirs = data["dirs"];
                let dirList = dirs[Object.keys(dirs)[0]];

                $.each(dirList, function (index, dir) {
                    $("#dirList").append('<li><a href="#">' + dir + '</a></li>');
                })

                listClick();
            }
        }

        function listClick() {
            $('li').click(async function (e) {
                await selectPath(e);
            });
        }

        async function selectPath(e) {
            e.preventDefault();
            const selectedPath = $("#selectedPath").text() === "Select Folder" ? "" : $("#selectedPath").text().trim();
            const seperator = (os.toLowerCase().indexOf('windows') !== -1 ? "\\" : "/");
            const listText = $(e.target).text();
            let path = (selectedPath.length > 0 && selectedPath !== listText) ? selectedPath + seperator + $(e.target).text() : $(e.target).text();
            let pathArr = path.split(seperator);
            pathArr = pathArr.filter(e => e);

            if (pathArr.length > 1) {
                $("#dirListUpButton").html('<button id="parentFolder" type="button" class="btn btn-secondary btn-sm"><i class="bi-arrow-90deg-up"></i></button>&nbsp;');
                $("#parentFolder").on("click", function (e) {
                    e.preventDefault();

                    pathArr.pop();
                    path = pathArr.join(seperator);
                    if ((path.match(/,/g) || []).length === 0) {
                        path = path + seperator;
                    }
                    getSubdirs(path);
                })
            } else {
                $("#dirListUpButton").html("");
            }

            await getSubdirs(path);
        }

        async function getSubdirs(path) {
            // Display path
            $("#selectedPath").text(path);
            $("#dirListSelect").html('<button type="button" id="selectFolder" class="btn btn-primary btn-sm">Select Folder</button>');
            $("#selectFolder").on("click", function (e) {
                e.preventDefault();
                const path = $("#selectedPath").text().trim();
                if (path.length > 0) {
                    const mediaDirArray = $("#mediaDirTextArea").val().split(",").map(element => element.trim());
                    if (mediaDirArray.indexOf(path) === -1) {
                        mediaDirArray.push(path);
                    }
                    $("#mediaDirTextArea").val(mediaDirArray.join(', '));
                }
            });

            // Get sub directories
            data = await http.ajax("post", "/settings/directorytree", '{"path":"' + Util.stringEscape(path) + '"}');
            populateDirs(data);
        }

        $("#deleteContent").on("click", function (e) {
            e.preventDefault();
            $("#propDeleteContent").modal('show');
        });

        $("#deleteContentConfirm").on("click", async function (e) {
            e.preventDefault();

            $("#deleteContentModalStatus").css("visibility", "visible");

            const http = new Http("deleting all content");
            let json = {deleteContent: true}
            const data = await http.ajax("post", "/settings/content/delete", JSON.stringify(json));

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                }
                $("#msgDeleteContent").html(message);
            }
            $("#deleteContentModalStatus").css("visibility", "hidden");
        });
    }
}