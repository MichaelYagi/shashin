class Settings {

    constructor() {

    }

    async init() {
        // Populate dirs
        const http = new Http("settings");
        let data = await http.ajax("post", "/settings/directorytree", '{"path":""}');
        let os = "";
        if (data.hasOwnProperty("os")) {
            os = data.os;
        }
        const seperator = (os.toLowerCase().indexOf('windows') !== -1 ? "\\" : "/");
        // $("#selectFolder").hide();

        let lastFieldFocused = "mediaDirTextArea";
        $("#"+lastFieldFocused).focus();
        $("#selectFolder").text(shashin.getTranslatedValue('main.pages.settings.control.browse.button.selectfolder'));

        $("#mediaDirTextArea").on("focus", function () {
            lastFieldFocused = "mediaDirTextArea";
            $("#selectFolder").text(shashin.getTranslatedValue('main.pages.settings.control.browse.button.selectfolder'));
        });
        $("#mediaExcludeDirTextArea").on("focus", function () {
            lastFieldFocused = "mediaExcludeDirTextArea";
            $("#selectFolder").text(shashin.getTranslatedValue('main.pages.settings.control.browse.button.excludefolder'));
        });
        $("#uploadMediaDirectory").on("focus", function () {
            lastFieldFocused = "uploadMediaDirectory";
            $("#selectFolder").text(shashin.getTranslatedValue('main.pages.settings.control.browse.button.uploadfolder'));
        });

        $("#selectFolder").on("click", function (e) {
            e.preventDefault();
            const path = $("#selectedPath").val().trim();
            if (path.length > 0) {
                if (lastFieldFocused === "mediaDirTextArea" || lastFieldFocused === "mediaExcludeDirTextArea") {
                    let mediaDirStr = $("#"+lastFieldFocused).val().trim();

                    let mediaDirArray = mediaDirStr.split('\n');

                    if ($.inArray(path, mediaDirArray) === -1) {
                        let mediaDirArray = [];

                        if (mediaDirStr.length > 0) {
                            mediaDirArray = mediaDirStr.split("\\n").map(element => element.trim());
                        }

                        if (mediaDirArray.indexOf(path) === -1) {
                            mediaDirArray.push(path);
                        }

                        $("#" + lastFieldFocused).val(mediaDirArray.join('\n'));
                    }
                } else if (lastFieldFocused === "uploadMediaDirectory") {
                    $("#"+lastFieldFocused).val(path);
                }
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
        });

        populateDirs(data);
        listClick();

        function populateDirs(data) {
            $("#dirList").empty();

            if (data.hasOwnProperty("dirs")) {
                let dirs = data.dirs;
                let dirList = dirs[Object.keys(dirs)[0]];

                $.each(dirList, function (index, dir) {
                    $("#dirList").append('<li><a href="#">' + dir + '</a></li>');
                });

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
            const selectedPath = $("#selectedPath").val().trim();

            $("#selectedPathInput").val($("#selectedPath").val().trim());

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

            $("#deleteContentModalStatus").visible();

            if ($("#deleteConfirm").val() === "DELETE") {

                const http = new Http("deleting all content");
                let json = {deleteContent: true};
                const data = await http.ajax("post", "/settings/content/delete", JSON.stringify(json));

                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let message = "Error";
                    if (data.status === shashin.apiResponse.SUCCESS) {
                        shashin.showToastMessage(shashin.getTranslatedValue('main.toast.settings.title.success'), data.msg, {
                            icon: "bi-info-circle",
                            iconColor: "#777777",
                            autohide: false
                        });
                        $("#propDeleteContent").modal('hide');
                    } else {
                        shashin.showToastMessage(shashin.getTranslatedValue('main.toast.settings.title.fail'), data.msg, {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            autohide: false,
                            borderColor: "danger"
                        });
                        $("#propDeleteContent").modal('hide');
                    }
                }
                $("#deleteContentModalStatus").invisible();
            } else {
                shashin.showToastMessage(shashin.getTranslatedValue('main.toast.settings.input.title.fail'), shashin.getTranslatedValue('main.toast.settings.input.message.fail'), {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FD7E14",
                    borderColor: "warning"
                });
                $("#propDeleteContent").modal('hide');
                $("#deleteContentModalStatus").invisible();
                $("#deleteConfirm").val("");
            }
        });
    }
}