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
    $("header,#container,ul.nav,#topLeftToastContainer,#topCenterToastContainer,#topRightToastContainer,#midLeftToastContainer,#midCenterToastContainer,#midRightToastContainer,#bottomLeftToastContainer,#bottomCenterToastContainer,#bottomRightToastContainer").on('dragover', function (e) {
        preventDefaults(e);

        const isModalShown = ($('.modal').hasClass('in') || $('.modal').hasClass('show'));
        const isOffcanvasShown = ($('.offcanvas').hasClass('in') || $('.offcanvas').hasClass('show'));

        if (isOffcanvasShown === false && isModalShown === false) {
            let backgroundColor = "white";
            if (shashin.darkMode === true) {
                backgroundColor = "#222222";
            }
            $("header,#container,ul.nav").css({"background-color": backgroundColor, "opacity": ".5"});
        }
    });
    $("header,#container,ul.nav").on('dragenter', function (e) {
        preventDefaults(e);

        const isModalShown = ($('.modal').hasClass('in') || $('.modal').hasClass('show'));
        const isOffcanvasShown = ($('.offcanvas').hasClass('in') || $('.offcanvas').hasClass('show'));

        if (isOffcanvasShown === false && isModalShown === false) {
            let backgroundColor = "white";
            if (shashin.darkMode === true) {
                backgroundColor = "#222222";
            }
            $("header,#container").css({"background-color": backgroundColor, "opacity": ".5"});

            shashin.showToastMessage("Drop Media", "Drag and drop media anywhere to upload.", {
                placement: shashin.toast.placement.top.center,
                forceDisplay: true,
                tag: "uploadMedia",
                autohide: false
            });
        }
    });
    $("header,#container,ul.nav").on('dragleave', function (e) {
        preventDefaults(e);

        if (e.originalEvent.pageX !== 0 && e.originalEvent.pageY !== 0) {
            return false;
        }

        let backgroundColor = "white";
        if (shashin.darkMode === true) {
            backgroundColor = "#222222";
        }
        $("header,#container,ul#browserGroup").css({"background-color": backgroundColor, "opacity": "1"});

        let offcanvasBackgroundColor = "white";
        if (shashin.darkMode === true) {
            offcanvasBackgroundColor = "#2F2F2F";
        }
        $("ul#offcanvasList").css({"background-color": offcanvasBackgroundColor, "opacity": "1"});

        shashin.closeToastMessages({tag: "uploadMedia"});
    });
    $("header,#container,ul:not(#offcanvasList),ul#browserGroup,#topLeftToastContainer,#topCenterToastContainer,#topRightToastContainer,#midLeftToastContainer,#midCenterToastContainer,#midRightToastContainer,#bottomLeftToastContainer,#bottomCenterToastContainer,#bottomRightToastContainer").on("drop", function (e) {
        e.preventDefault();

        const isModalShown = ($('.modal').hasClass('in') || $('.modal').hasClass('show'));
        const isOffcanvasShown = ($('.offcanvas').hasClass('in') || $('.offcanvas').hasClass('show'));

        if (isOffcanvasShown === false && isModalShown === false) {
            let backgroundColor = "white";
            if (shashin.darkMode === true) {
                backgroundColor = "#222222";
            }
            $("header,#container,ul#browserGroup").css({"background-color": backgroundColor, "opacity": "1"});

            let offcanvasBackgroundColor = "white";
            if (shashin.darkMode === true) {
                offcanvasBackgroundColor = "#2F2F2F";
            }
            $("ul#offcanvasList").css({"background-color": offcanvasBackgroundColor, "opacity": "1"});

            shashin.closeToastMessages({tag: "uploadMedia"});

            const dt = e.originalEvent.dataTransfer;
            if (dt.types && (dt.types.indexOf ? dt.types.indexOf('Files') !== -1 : dt.types.includes('Files'))) {
                if (activePage === "album") {
                    uploadData(dt, "uploadToAlbumForm");
                } else {
                    uploadData(dt, "uploadForm");
                }
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
                        shashin.showToastMessage("Media uploaded", success["msg"] + ":<br>" + filelist + "<a href='javascript:window.location.href=window.location.href'>Refresh</a> page to view.", {
                            icon: "bi-info-circle",
                            placement: shashin.toast.placement.top.center,
                            tag: "successUpload",
                            iconColor: "#777777",
                            forceDisplay: true,
                            autohide: false,
                            borderColor: "success"
                        });
                    } else {
                        shashin.showToastMessage("Something went wrong", "Check media upload directory in settings", {
                            icon: "bi-exclamation-triangle",
                            placement: shashin.toast.placement.top.center,
                            tag: "failUpload",
                            iconColor: "#FF0000",
                            forceDisplay: true,
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
                        tag: "failUpload",
                        iconColor: "#FF0000",
                        forceDisplay: true,
                        delay: 5000,
                        borderColor: "danger"
                    });
                }
            );
        }
    }
}