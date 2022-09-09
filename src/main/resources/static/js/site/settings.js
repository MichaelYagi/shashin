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
        const seperator = (os.toLowerCase().indexOf('windows') !== -1 ? "\\" : "/");
        $("#selectFolder").hide();

        $("#selectFolder").on("click", function (e) {
            e.preventDefault();
            const path = $("#selectedPath").val().trim();
            if (path.length > 0) {
                let mediaDirStr = $("#mediaDirTextArea").val().trim();
                let mediaDirArray = [];

                if (mediaDirStr.length > 0) {
                    mediaDirArray = mediaDirStr.split(",").map(element => element.trim());
                }

                if (mediaDirArray.indexOf(path) === -1) {
                    mediaDirArray.push(path);
                }

                $("#mediaDirTextArea").val(mediaDirArray.join(', '));
            }
        });

        $("#parentFolder").on("click", function (e) {
            e.preventDefault();

            let path = $("#pathInput").val();

            let pathArr = path.split(seperator);
            pathArr = pathArr.filter(e => e);

            pathArr.pop();

            path = pathArr.join(seperator);

            const selectedPath = $("#selectedPathInput").val();

            if (selectedPath.slice(-1) === seperator) {
                path = path + (os.toLowerCase().indexOf('windows') !== -1 ? "" : "/");
            }

            if (pathArr.length === 1 && os.toLowerCase().indexOf('windows') !== -1) {
                path = path + seperator;
            }

            if (os.toLowerCase().indexOf('windows') === -1 && pathArr.length > 0) {
                path = "/" + path;
            }

            $("#pathInput").val(path);

            getSubdirs(path);
        })

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
            $('#dirList li').click(async function (e) {
                await selectPath(e);
            });
        }

        async function selectPath(e) {
            e.preventDefault();
            const selectedPath = $("#selectedPath").val() === "Select Folder" ? "" : $("#selectedPath").val().trim();
            if (selectedPath.length > 0) {
                $("#selectFolder").show();
            } else {
                $("#selectFolder").hide();
            }
            $("#selectedPathInput").val(selectedPath);

            const listText = $(e.target).text();

            let path = (selectedPath.length > 0 && selectedPath !== listText) ? selectedPath + ((selectedPath.slice(-1) === seperator) ? "" : seperator) + listText : listText;
            $("#pathInput").val(path);

            await getSubdirs(path);
        }

        async function getSubdirs(path) {
            // Display path
            $("#selectedPath").val(path);

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