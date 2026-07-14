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

        let embedModelAvailable = false;

        function setEmbedStatusText(text) {
            const el = $("#embedStatusText");
            if (el.length) el.text(text);
        }

        function updateEmbedStatus(available, model) {
            const statusEl = $("#embedModelStatus");
            const btn = $("#generateEmbeddingsBtn");
            embedModelAvailable = available && !!model;
            if (embedModelAvailable) {
                statusEl.html('<span class="bi-check-circle-fill text-success"></span> <code>' + model + '</code> <span id="embedStatusText">ready for semantic search</span>');
                btn.prop("disabled", false);
            } else {
                statusEl.html('Run <code>ollama pull nomic-embed-text</code> to enable semantic search');
                btn.prop("disabled", true);
            }
        }

        async function checkEmbedModel(url) {
            if (!url) { updateEmbedStatus(false); return; }
            try {
                const resp = await fetch("/settings/ollama/embed-status?url=" + encodeURIComponent(url));
                const data = await resp.json();
                updateEmbedStatus(data.available, data.model);
                if (data.available && data.model) {
                    $("#ollamaEmbedModel").val(data.model);
                }
            } catch (e) {
                updateEmbedStatus(false);
            }
        }

        async function loadOllamaModels() {
            const url = $("#ollamaUrl").val().trim();
            const statusEl = $("#ollamaStatus");
            if (!url) {
                statusEl.hide();
                $("#visionModelSection").hide();
                updateEmbedStatus(false);
                return;
            }
            const btn = $("#loadOllamaModels");
            btn.prop("disabled", true).text("Loading…");
            statusEl.hide();
            try {
                const resp = await fetch("/settings/ollama/vision-models?url=" + encodeURIComponent(url));
                const models = await resp.json();
                const select = $("#ollamaVisionModel");
                const current = select.val();
                select.empty().append('<option value="">-- none --</option>');
                models.forEach(function (m) {
                    const selected = m === current ? " selected" : "";
                    select.append('<option value="' + m + '"' + selected + '>' + m + '</option>');
                });
                if (models.length === 0) {
                    select.append('<option value="" disabled>No vision models found</option>');
                    statusEl.removeClass("bi-check-circle-fill text-success bi-x-circle-fill text-danger")
                            .addClass("bi-x-circle-fill text-danger").show();
                    $("#visionModelSection").hide();
                } else {
                    statusEl.removeClass("bi-check-circle-fill text-success bi-x-circle-fill text-danger")
                            .addClass("bi-check-circle-fill text-success").show();
                    $("#visionModelSection").show();
                }
            } catch (e) {
                console.error("Could not load Ollama models", e);
                statusEl.removeClass("bi-check-circle-fill text-success bi-x-circle-fill text-danger")
                        .addClass("bi-x-circle-fill text-danger").show();
                $("#visionModelSection").hide();
            } finally {
                btn.prop("disabled", false).text("Load models");
            }
            await checkEmbedModel(url);
        }

        $("#loadOllamaModels").on("click", loadOllamaModels);

        // Auto-check on page load if URL is already saved
        if ($("#ollamaUrl").val().trim()) {
            loadOllamaModels();
        }

        let embeddingPollTimer = null;

        function pollEmbeddingProgress() {
            $.ajax({
                url: "/search/embeddings/progress",
                method: "GET",
                success: function(data) {
                    if (data.running) {
                        setEmbedStatusText("Processing " + data.processed + " / " + data.total + "…");
                        embeddingPollTimer = setTimeout(pollEmbeddingProgress, 2000);
                    } else {
                        setEmbedStatusText("ready for semantic search");
                        $("#generateEmbeddingsBtn").prop("disabled", false).text("Generate embeddings");
                    }
                },
                error: function() {
                    setEmbedStatusText("ready for semantic search");
                    $("#generateEmbeddingsBtn").prop("disabled", false).text("Generate embeddings");
                }
            });
        }

        // Resume progress display if a job is already running when the page loads
        $.ajax({
            url: "/search/embeddings/progress",
            method: "GET",
            success: function(data) {
                if (data.running) {
                    $("#generateEmbeddingsBtn").prop("disabled", true).text("Running…");
                    setEmbedStatusText("Processing " + data.processed + " / " + data.total + "…");
                    embeddingPollTimer = setTimeout(pollEmbeddingProgress, 2000);
                }
            }
        });

        $("#generateEmbeddingsBtn").on("click", function() {
            const btn = $(this);
            btn.prop("disabled", true).text("Starting…");
            if (embeddingPollTimer) { clearTimeout(embeddingPollTimer); embeddingPollTimer = null; }
            $.ajax({
                url: "/search/embeddings/generate",
                method: "POST",
                success: function(data) {
                    if (data.status === shashin.apiResponse.SUCCESS) {
                        const total = (data.msg.match(/\d+/) || ["?"])[0];
                        setEmbedStatusText("Processing 0 / " + total + "…");
                        embeddingPollTimer = setTimeout(pollEmbeddingProgress, 2000);
                    } else {
                        setEmbedStatusText("ready for semantic search");
                        btn.prop("disabled", false).text("Generate embeddings");
                    }
                },
                error: function() {
                    setEmbedStatusText("ready for semantic search");
                    btn.prop("disabled", false).text("Generate embeddings");
                }
            });
        });

    }
}