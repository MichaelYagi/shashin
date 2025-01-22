function initializeUploads(activePage) {
    $("#uploadToAlbum").on("click", function (e) {
        e.preventDefault();
        chooseMedia("album");
    });

    $("#uploadToMedia").on("click", function (e) {
        e.preventDefault();
        chooseMedia("browse");
    });

    function chooseMedia(destination) {
        if (destination === "album") {
            $("#uploadMediaAlbum").trigger('click');
        } else if (destination === "browse") {
            $("#uploadMedia").trigger('click');
        }
    }

    $("#uploadMediaAlbum").on("change", function (e) {
        e.preventDefault();
        const fi = document.getElementById("uploadMediaAlbum");
        uploadData(fi, "uploadToAlbumForm");
    });

    $("#uploadMedia").on("change", function (e) {
        e.preventDefault();
        const fi = document.getElementById("uploadMedia");
        uploadData(fi, "uploadForm");
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }
    $("header,#container").on('dragover', function (e) {
        preventDefaults(e);
        $("header,#container").css({"background-color": "white", "opacity": ".5"});
    });
    $("header,#container").on('dragenter', function (e) {
        preventDefaults(e);
        $("header,#container").css({"background-color": "white", "opacity": ".5"});
        if ($("#"+shashin.toast.target.default).hasClass("show") === false) {
            shashin.showToastMessage("Drop Media", "Drag and drop media anywhere to upload.", {
                placement: shashin.toast.placement.top.center,
                autohide: false
            });
        }
    });
    $("header,#container").on('dragleave', function (e) {
        preventDefaults(e);
        if (e.originalEvent.pageX !== 0 && e.originalEvent.pageY !== 0) {
            return false;
        }

        $("header,#container").css({"background-color": "white", "opacity": "1"});
        shashin.closeToastMessage();
    });
    $("header,#container").on("drop", function (e) {
        e.preventDefault();
        $("header,#container").css({"background-color": "white", "opacity": "1"});
        shashin.closeToastMessage();

        const dt = e.originalEvent.dataTransfer;
        if (dt.types && (dt.types.indexOf ? dt.types.indexOf('Files') !== -1 : dt.types.includes('Files'))) {
            if (activePage === "album") {
                uploadData(dt, "uploadToAlbumForm");
            } else {
                uploadData(dt, "uploadForm");
            }
        }
    });

    function uploadData(fi, uploadForm) {
        if (fi.files.length > 0) {
            const formData  = new FormData();
            let filelist = "";
            for (let i = 0; i <= fi.files.length - 1; i++) {
                formData.append('files[]', fi.files.item(i), fi.files.item(i).name);
                const fsize = fi.files.item(i).size;
                if (fsize > 0) {
                    filelist += fi.files.item(i).name + "<br>";
                }
            }

            const uploadUrl = $("#"+uploadForm).attr("action");

            fetch(uploadUrl, {
                method: 'POST',
                body: formData
            }).then(
                response => response.json()
            ).then(
                success => {
                    const status = success.hasOwnProperty("status") === true ? success["status"] : "fail";
                    if (status === "success") {
                        shashin.showToastMessage("Media uploaded", success["msg"] + ":<br>" + filelist + "Refresh page to view.", {
                            icon: "bi-info-circle",
                            placement: shashin.toast.placement.top.center,
                            iconColor: "#777777",
                            delay: 5000,
                            borderColor: "success"
                        });
                    } else {
                        shashin.showToastMessage("Something went wrong", "Check media upload directory in settings", {
                            icon: "bi-exclamation-triangle",
                            placement: shashin.toast.placement.top.center,
                            iconColor: "#FF0000",
                            delay: 5000,
                            borderColor: "danger"
                        });
                    }
                }
            ).catch(
                error => {
                    shashin.showToastMessage("Something went wrong", error, {
                        icon: "bi-exclamation-triangle",
                        placement: shashin.toast.placement.top.center,
                        iconColor: "#FF0000",
                        delay: 5000,
                        borderColor: "danger"
                    });
                }
            );
        }
    }
}