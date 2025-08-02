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

        modalShown(function (isShown) {
            if (isShown === false) {
                blanketUI();
            }
        });

        Util.showSpinner(true);
        const fi = document.getElementById("uploadMediaAlbum");
        uploadData(fi, "uploadToAlbumForm");
    });

    $("#uploadMedia").on("change", function (e) {
        e.preventDefault();

        modalShown(function (isShown) {
            if (isShown === false) {
                blanketUI();
            }
        });

        Util.showSpinner(true);
        const fi = document.getElementById("uploadMedia");
        uploadData(fi, "uploadForm");
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }
    $("header,#container,ul.nav,#topLeftToastContainer,#topCenterToastContainer,#topRightToastContainer,#midLeftToastContainer,#midCenterToastContainer,#midRightToastContainer,#bottomLeftToastContainer,#bottomCenterToastContainer,#bottomRightToastContainer").on('dragover', function (e) {
        preventDefaults(e);

        modalShown(function (isShown) {
            if (isShown === false) {
                blanketUI();
            }
        });
    });
    $("header,#container,ul.nav").on('dragenter', function (e) {
        preventDefaults(e);

        modalShown(function (isShown) {
            if (isShown === false) {
                blanketUI();

                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.media.upload.title"), shashin.getTranslatedValue("main.toast.media.upload.body"), {
                    placement: shashin.toast.placement.top.center,
                    tag: "uploadMedia",
                    autohide: false
                });
            }
        });
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

        Util.showSpinner(true);

        modalShown(function (isShown) {
            if (isShown === false) {
                const dt = e.originalEvent.dataTransfer;
                if (dt.types && (dt.types.indexOf ? dt.types.indexOf('Files') !== -1 : dt.types.includes('Files'))) {
                    if (activePage === "album") {
                        uploadData(dt, "uploadToAlbumForm");
                    } else {
                        uploadData(dt, "uploadForm");
                    }
                }
            } else {
                revertUI();
            }
        });
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
                    revertUI();

                    const status = success.hasOwnProperty("status") === true ? success.status : "fail";
                    if (status === "success") {
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.media.upload.uploaded"), success.msg + ":<br>" + filelist + "<br><a href='javascript:window.location.href=window.location.href'>" + shashin.getTranslatedValue("main.toast.media.upload.refresh") + "</a>", {
                            icon: "bi-info-circle",
                            placement: shashin.toast.placement.top.center,
                            tag: "successUpload",
                            iconColor: "#777777",
                            autohide: false,
                            borderColor: "success"
                        });
                    } else {
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.media.upload.errors"), success.msg, {
                            icon: "bi-exclamation-triangle",
                            placement: shashin.toast.placement.top.center,
                            tag: "failUpload",
                            iconColor: "#FF8C00",
                            autohide: false,
                            borderColor: "warning"
                        });
                    }
                }
            ).catch(
                error => {
                    revertUI();

                    shashin.showToastMessage(shashin.getTranslatedValue("main.message.pta"), error, {
                        icon: "bi-exclamation-triangle",
                        placement: shashin.toast.placement.top.center,
                        tag: "failUpload",
                        iconColor: "#FF0000",
                        delay: 5000,
                        borderColor: "danger"
                    });
                }
            );
        } else {
            revertUI();
        }
    }

    function revertUI() {
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

        Util.showSpinner(false);
    }

    function blanketUI() {
        let backgroundColor = "white";
        if (shashin.darkMode === true) {
            backgroundColor = "#222222";
        }
        $("header,#container,ul.nav").css({"background-color": backgroundColor, "opacity": ".5"});
    }

    function modalShown(callback) {
        const isModalShown = ($('.modal').hasClass('in') || $('.modal').hasClass('show'));
        const isOffcanvasShown = ($('.offcanvas').hasClass('in') || $('.offcanvas').hasClass('show'));

        if (isOffcanvasShown === false && isModalShown === false) {
            callback(false);
        } else {
            callback(true);
        }
    }
}