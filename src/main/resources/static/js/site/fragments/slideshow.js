function initializeSlideshow(accessTimelineView, queryLimit) {
    let slideshowIntervalId;
    let slideshowStarted = false;
    let slideshowIsPaused = false;
    let slideshowIsElapsed = 30; // Seconds
    let slideshowCurrentIndex = 0;
    let slideshowMetadataIds = [];
    let slideshowMouseTimer = null;
    let closeTimer = null;
    let nextTimer = null;
    let prevTimer = null;
    let infoTimer = null;
    let shortcutTimer = null;
    let slideshowCursorVisible = true;
    let slideshowProceed = true;
    let cjsc = null;
    let currentPhotoUrl = null;
    let currentMetadata = null;
    let firstTime = true;

    function getSlideshowImage(callback) {
        const http = new Http("show slideshow");

        shashin.printMessageToConsole("slideshowCurrentIndex: " + slideshowCurrentIndex, {tag: "slideshow"});
        shashin.printMessageToConsole("slideshowMetadataIds:", {tag: "slideshow"});
        shashin.printMessageToConsole(slideshowMetadataIds, {tag: "slideshow"});

        if (slideshowProceed === true) {
            slideshowProceed = false;
            if (slideshowMetadataIds.length > 0 && slideshowCurrentIndex >= 0 && slideshowCurrentIndex <= slideshowMetadataIds.length - 1) {
                shashin.printMessageToConsole("Looking up " + slideshowMetadataIds[slideshowCurrentIndex], {tag: "slideshow"});
                http.ajax("get", "/media/metadata/" + slideshowMetadataIds[slideshowCurrentIndex]).then(function (data) {
                    processSlideData(data, "existing", callback);
                });
            } else {
                shashin.printMessageToConsole("New random image", {tag: "slideshow"});
                http.ajax("get", "/random/metadata/type/image").then(function (data) {
                    processSlideData(data, "new", callback);
                });
            }
        }
    }

    function processSlideData(data, type, callback) {
        if (data && data.hasOwnProperty("metadata") === true &&
            data.metadata.hasOwnProperty("thumbnailUrlOriginal") === true &&
            data.metadata.thumbnailUrlOriginal !== "" &&
            data.hasOwnProperty("baseUrl") === true &&
            data.baseUrl !== ""
        ) {
            $("#mediaSrc").css("display", "block");
            const photoUrl = data.baseUrl + "/api/v1/image/"+data.metadata.id;

            currentPhotoUrl = photoUrl;
            currentMetadata = data.metadata;

            if (cjsc !== null && cjsc.available && cjsc.connected) {
                const cjscMetadata = {
                    title: data.metadata.title
                };

                if (data.metadata.description !== null && data.metadata.description !== "") {
                    cjscMetadata.description = data.metadata.description;
                }
                cjsc.cast(photoUrl+".jpg", cjscMetadata);
            }

            const tempImage = new Image();

            tempImage.onerror = function (error) {
                shashin.printMessageToConsole("Error: " + error, {tag: "slideshow"});
                slideshowProceed = true;
            };

            if (firstTime === true) {
                waitingScreen();
            }

            tempImage.onload = function () {
                slideshowProceed = true;
                firstTime = false;

                $("#mediaSrc").fadeOut((slideshowStarted === false) ? 0 : 300, function () {
                    $("#mediaSrc").attr("src", photoUrl).fadeIn((slideshowStarted === false) ? 0 : 600);

                    $("#playPause").css({
                        "font-size": "10rem",
                        "color": "#FFFFFF",
                        "z-index": 99998,
                        "max-width": $(window).innerWidth() + 1,
                        "height": "auto",
                        "max-height": $(window).innerHeight() + 1,
                        "position": "absolute",
                        "top": "50%",
                        "left": "50%",
                        "transform": "translate(-50%, -50%)"
                    });

                    $(".centerFit").css({
                        "max-width": $(window).innerWidth() + 1,
                        "height": "auto",
                        "max-height": $(window).innerHeight() + 1,
                        "position": "absolute",
                        "top": "50%",
                        "left": "50%",
                        "transform": "translate(-50%, -50%)"
                    });

                    $("#prevSlide").css({
                        "font-size": "4rem",
                        "color": "#FFFFFF",
                        "z-index": 99998,
                        "position": "absolute",
                        "top": "46%",
                        "left": "2%"
                    });

                    $("#nextSlide").css({
                        "font-size": "4rem",
                        "color": "#FFFFFF",
                        "z-index": 99998,
                        "position": "absolute",
                        "top": "46%",
                        "right": "2%"
                    });

                    $("#closeAction").css({
                        "font-size": "4rem",
                        "color": "#FFFFFF",
                        "z-index": 99998,
                        "position": "absolute",
                        "top": "5px",
                        "right": "5px"
                    });

                    $("#shortcutAction").css({
                        "font-size": "2rem",
                        "color": "#FFFFFF",
                        "z-index": 99998,
                        "position": "absolute",
                        "top": "22px",
                        "right": "77px"
                    });

                    $("#infoAction").css({
                        "font-size": "2rem",
                        "color": "#FFFFFF",
                        "z-index": 99998,
                        "position": "absolute",
                        "top": "23px",
                        "right": "135px"
                    });

                    $("#slideSpinner").css({
                        "font-size": "2rem",
                        "color": "#FFFFFF",
                        "z-index": 99998,
                        "position": "absolute",
                        "top": "23px",
                        "left": "2%"
                    });

                    if (Util.isMobile() === false) {
                        $("#mediaInfo").css({
                            "max-width": ($(window).width() + 1),
                            "font-size": "1.7rem"
                        });
                    } else {
                        $("#mediaInfo").css({
                            "font-size": "1rem"
                        });
                    }

                    if (type === "new") {
                        slideshowMetadataIds.push(data.metadata.id);
                    }
                    if (slideshowMetadataIds.length > queryLimit) {
                        slideshowMetadataIds.splice(0, 1); // At position 0, remove 1
                        slideshowCurrentIndex--;
                    }

                    const takenDateString = data.metadata.year + "-" + data.metadata.month + "-" + data.metadata.day;
                    const takenDate = new Date(takenDateString);
                    const options = {weekday: 'long', year: 'numeric', month: 'short', day: 'numeric'};
                    let description = takenDate.toLocaleDateString('en-us', options);

                    if (accessTimelineView === false && data.hasOwnProperty("albumIds") === true && data.albumIds.hasOwnProperty(0) === true) {
                        description = "<a style='color:#DBE9F4;text-decoration:none;' href='/album/" + data.albumIds[0] + "' target='_blank'>" + takenDate.toLocaleDateString('en-us', options) + "</a>";
                    } else if (accessTimelineView === true) {
                        description = "<a style='color:#DBE9F4;text-decoration:none;' href='/timeline#" + takenDateString + "' target='_blank'>" + takenDate.toLocaleDateString('en-us', options) + "</a>";
                    }

                    if (data.shortPlaceName !== null && data.shortPlaceName !== "") {
                        description += " • " + data.shortPlaceName;
                    }

                    if (data.metadata.description !== null && data.metadata.description !== "") {
                        description += "<div class='text-center'>" + data.metadata.description + "</div>";
                    }
                    $("#mediaInfo").html(description);

                    slideshowStarted = true;

                    if (callback !== undefined && typeof callback === 'function') {
                        callback(true);
                    }

                    $("#slideSpinner").hide();
                });
            };

            tempImage.src = photoUrl;
        } else if (callback !== undefined && typeof callback === 'function') {
            callback(false);

            $("#slideSpinner").hide();
        }
    }

    function waitingScreen() {
        $("#slideshowContainer").css({
            "width": "101%",
            "height": "101%",
            "display": "block",
            "z-index": 9999,
            "background-color": "#000000",
            "overflow": "hidden"
        });

        $("#slideSpinner").css({
            "font-size": "2rem",
            "color": "#FFFFFF",
            "z-index": 99998,
            "position": "absolute",
            "top": "23px",
            "left": "2%"
        });

        $("#slideSpinner").show();
        $("#infoAction").css("display", "none");
        $("#shortcutAction").css("display", "none");
        $("#nextSlide").css("display", "none");
        $("#prevSlide").css("display", "none");
        $("#closeAction").css("display", "none");
    }

    function exitSlideshowGallery() {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.documentElement.exitFullscreen) {
            document.documentElement.exitFullscreen();
        }

        document.body.style.overflow = 'visible';

        $("#slideshowContainer").css({
            "display": "none"
        });

        slideshowCurrentIndex = 0;
        slideshowMetadataIds = [];

        if (slideshowIntervalId) {
            clearInterval(slideshowIntervalId);
            slideshowIntervalId = 0;
        }

        if (slideshowMouseTimer) {
            clearTimeout(slideshowMouseTimer);
        }

        document.body.style.cursor = "default";
        slideshowCursorVisible = true;
        slideshowStarted = false;
        slideshowProceed = true;

        $("#mediaInfo").html("");

        $("#mediaSrc").attr("src", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII");
    }

    function showInstruction() {
        let options = {};

        if (Util.isMobile() === false) {
            options = {
                icon: "bi-keyboard",
                autohide: false,
                tag: "slide",
            };
        } else {
            options = {
                icon: "bi-hand-index",
                autohide: false,
                tag: "slide"
            };
        }

        if (typeof Castjs != "undefined" && cjsc === null) {
            cjsc = new Castjs();
        }

        if (cjsc !== null) {
            // Create
            if (cjsc.state === "disconnected") {
                options.headerSubtext = "<a href='#' id='toggleCast' style='display: none;'><span id='toggleCastIcon' class='bi-cast' style='font-size:1rem;color: lightgray;'></span></a>";
            } else {
                options.headerSubtext = "<a href='#' id='toggleCast' style='display: none;'><span id='toggleCastIcon' class='bi-stop-circle' style='font-size:1rem;color: lightgray;'></span></a>";
            }
        }

        let title = "";
        let message = "";
        if (Util.isMobile() === false) {
            message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>d</strong></span></div><div class='col-9'>Show/close this window</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>x</strong></span></div><div class='col-9'>Exit slideshow.</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Space</strong></span></div><div class='col-9'>Play/pause</div></div>" +
                "<span id='castKey' style='display: none;'><div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>c</strong></span></div><div class='col-9'>Start/stop casting</div></div></span>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>i</strong></span></div><div class='col-9'>Slide info</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>← →</strong></span></div><div class='col-9'>Go to next/previous slide</div></div>" +
                "</div>";

            title = "Keyboard Shortcuts";
        } else {
            message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Swipe Up</strong></span></div><div class='col-9'>Show/close this window</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Swipe Down</strong></span></div><div class='col-9'>Slide info</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Single Tap</strong></span></div><div class='col-9'>Play/pause</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Double Tap</strong></span></div><div class='col-9'>Exit slideshow</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Swipe ← →</strong></span></div><div class='col-9'>Go to next/previous slide</div></div>" +
                "</div>";

            title = "Touch Bindings";
        }

        shashin.showToastMessage(title,
            message,
            options
        );

        if (typeof Castjs != "undefined" && cjsc !== null) {
            cjsc.on('available', () => {
                $("#castKey").css({"display": "block"});
                $("#toggleCast").css({"display": "block"});
            });
        }

        $("#toggleCast").on("click", function (e) {
            e.preventDefault();

            if (cjsc !== null && cjsc.available) {
                if ($("#toggleCastIcon").hasClass('bi-cast')) {
                    if (currentPhotoUrl !== null) {
                        $("#toggleCastIcon").addClass('bi-stop-circle').removeClass('bi-cast');
                        let cjscMetadata = {};
                        if (currentMetadata !== null) {
                            cjscMetadata = {
                                title: currentMetadata.title
                            };

                            if (currentMetadata.description !== null && currentMetadata.description !== "") {
                                cjscMetadata.description = currentMetadata.description;
                            }
                        }
                        cjsc.cast(currentPhotoUrl+".jpg", cjscMetadata);
                    }
                } else {
                    cjsc.disconnect();
                    $("#toggleCastIcon").addClass('bi-cast').removeClass('bi-stop-circle');
                }
            }
        });
    }

    $("body").on("dblclick", function (e) {
        e.preventDefault();

        if (Util.isMobile() === true && $("#slideshowContainer").css("display") === "block") {
            if (document.exitFullscreen) {
                document.exitFullscreen();
            } else if (document.documentElement.exitFullscreen) {
                document.documentElement.exitFullscreen();
            }

            $("#mediaInfo").css("display", "none");
            exitSlideshowGallery();
            shashin.closeToastMessages({tag: "slide"});
            if (cjsc !== null && cjsc.available) {
                cjsc.disconnect();
            }
        }
    });

    $("body").on("keyup", function (e) {
        if ($("#slideshowContainer").css("display") === "block") {
            if (e.key === "x" || e.code === "KeyX" || e.which === 88 || e.keyCode === 88) {
                if (document.exitFullscreen) {
                    document.exitFullscreen();
                } else if (document.documentElement.exitFullscreen) {
                    document.documentElement.exitFullscreen();
                }

                $("#mediaInfo").css("display", "none");
                exitSlideshowGallery();
                shashin.closeToastMessages({tag: "slide"});
                if (cjsc !== null && cjsc.available) {
                    cjsc.disconnect();
                }
            }

            // Pause/play slideshow
            if (e.key === " " || e.code === "Space" || e.which === 32 || e.keyCode === 32) {
                slideshowGalleryPlayPause();
            }

            // Show info
            if (e.key === "i" || e.code === "KeyI" || e.which === 73 || e.keyCode === 73) {
                slideshowInfo();
            }

            // Cast slideshow
            if (e.key === "c" || e.code === "KeyC" || e.which === 67 || e.keyCode === 67) {
                if (cjsc !== null && cjsc.available) {
                    if ($("#toggleCastIcon").hasClass('bi-cast')) {
                        if (currentPhotoUrl !== null) {
                            $("#toggleCastIcon").addClass('bi-stop-circle').removeClass('bi-cast');
                            let cjscMetadata = {};
                            if (currentMetadata !== null) {
                                cjscMetadata = {
                                    title: currentMetadata.title
                                };

                                if (currentMetadata.description !== null && currentMetadata.description !== "") {
                                    cjscMetadata.description = currentMetadata.description;
                                }
                            }
                            cjsc.cast(currentPhotoUrl+".jpg", cjscMetadata);
                        }
                    } else {
                        cjsc.disconnect();
                        $("#toggleCastIcon").addClass('bi-cast').removeClass('bi-stop-circle');
                    }
                }
            }

            // Show key binding toast
            if (e.key === "d" || e.code === "KeyD" || e.which === 68 || e.keyCode === 68) {
                if (shashin.hasToast(shashin.toast.placement.bottom.center,{tag: "slide"}) === false) {
                    showInstruction();
                } else {
                    shashin.closeToastMessages({tag: "slide"});
                }
            }

            if (e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37 || e.key === "ArrowRight" || e.code === "ArrowRight" || e.which === 39 || e.keyCode === 39) {
                if (((e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37) && slideshowCurrentIndex === 0) === false && slideshowProceed === true) {

                    $("#slideSpinner").show();

                    if ((e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37) && slideshowCurrentIndex > 0) {
                        slideshowCurrentIndex--;
                    } else if ((e.key === "ArrowRight" || e.code === "ArrowRight" || e.which === 39 || e.keyCode === 39) && slideshowCurrentIndex <= slideshowMetadataIds.length - 1) {
                        slideshowCurrentIndex++;
                    }

                    getSlideshowImage(function () {
                        slideshowProceed = true;
                    });

                    if (slideshowIsPaused === false) {
                        clearInterval(slideshowIntervalId);
                        slideshowIntervalId = window.setInterval(function () {
                            if (slideshowIsPaused === false) {
                                slideshowCurrentIndex++;
                                getSlideshowImage(function () {
                                    slideshowProceed = true;
                                });
                            }
                        }, (slideshowIsElapsed * 1000));
                    }
                } else {
                    return false;
                }
            }
        }
    });

    $("#shortcutActionButton").on("click", function (e) {
        e.preventDefault();

        if (shashin.hasToast(shashin.toast.placement.bottom.center,{tag: "slide"}) === false) {
            showInstruction();
        } else {
            shashin.closeToastMessages({tag: "slide"});
        }
    });

    $("#closeActionButton").on("click", function (e) {
        e.preventDefault();

        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.documentElement.exitFullscreen) {
            document.documentElement.exitFullscreen();
        }

        $("#mediaInfo").css("display", "none");
        exitSlideshowGallery();
        shashin.closeToastMessages({tag: "slide"});
        if (cjsc !== null && cjsc.available) {
            cjsc.disconnect();
        }
    });

    $("#infoActionButton").on("click", function (e) {
        e.preventDefault();

        slideshowInfo();
    });

    $("#prevSlideButton").on("click", function (e) {
        e.preventDefault();

        $("#slideSpinner").show();

        if (slideshowCurrentIndex > 0 && slideshowProceed === true) {
            slideshowCurrentIndex--;
        } else {
            return false;
        }

        getSlideshowImage(function () {
            slideshowProceed = true;
        });

        if (slideshowIsPaused === false) {
            clearInterval(slideshowIntervalId);
            slideshowIntervalId = window.setInterval(function () {
                if (slideshowIsPaused === false) {
                    slideshowCurrentIndex++;
                    getSlideshowImage(function () {
                        slideshowProceed = true;
                    });
                }
            }, (slideshowIsElapsed * 1000));
        }
    });

    $("#nextSlideButton").on("click", function (e) {
        e.preventDefault();

        $("#slideSpinner").show();

        if (slideshowCurrentIndex <= slideshowMetadataIds.length - 1 && slideshowProceed === true) {
            slideshowCurrentIndex++;
        } else {
            return false;
        }

        getSlideshowImage(function () {
            slideshowProceed = true;
        });

        if (slideshowIsPaused === false) {
            clearInterval(slideshowIntervalId);
            slideshowIntervalId = window.setInterval(function () {
                if (slideshowIsPaused === false) {
                    slideshowCurrentIndex++;
                    getSlideshowImage(function () {
                        slideshowProceed = true;
                    });
                }
            }, (slideshowIsElapsed * 1000));
        }
    });

    Util.detectSwipe("#slideshowContainer", function (direction) {
        if (direction === "up" || direction === "down") {
            if (direction === "up") {
                if (shashin.hasToast(shashin.toast.placement.bottom.center,{tag: "slide"}) === false) {
                    showInstruction();
                } else {
                    shashin.closeToastMessages({tag: "slide"});
                }
            } else {
                slideshowInfo();
            }
        } else if (direction === "left" || direction === "right") {
            if (slideshowProceed === false) {
                return false;
            }

            if ((direction === "right" && slideshowCurrentIndex === 0) === false && slideshowProceed === true) {
                $("#slideSpinner").show();

                if (slideshowCurrentIndex > 0 && direction === "right") {
                    slideshowCurrentIndex--;
                } else if (slideshowCurrentIndex <= slideshowMetadataIds.length - 1 && direction === "left") {
                    slideshowCurrentIndex++;
                }

                getSlideshowImage(function () {
                    slideshowProceed = true;
                });
                if (slideshowIsPaused === false) {
                    clearInterval(slideshowIntervalId);
                    slideshowIntervalId = window.setInterval(function () {
                        if (slideshowIsPaused === false) {
                            slideshowCurrentIndex++;
                            getSlideshowImage(function () {
                                slideshowProceed = true;
                            });
                        }
                    }, (slideshowIsElapsed * 1000));
                }
            }
        }
    });

    function disappearCursor() {
        slideshowMouseTimer = null;
        document.body.style.cursor = "none";
        slideshowCursorVisible = false;
    }

    $("body").on("mousemove", function () {
        if (Util.isMobile() === false && $("#slideshowContainer").css("display") === "block") {

            if (slideshowMouseTimer) {
                clearTimeout(slideshowMouseTimer);
            }

            if (slideshowCursorVisible === false) {
                document.body.style.cursor = "default";
                slideshowCursorVisible = true;
            }

            slideshowMouseTimer = setTimeout(disappearCursor, 3000);
        }

        if (firstTime === false || (slideshowProceed === true && firstTime === true)) {
            if (nextTimer) {
                clearTimeout(nextTimer);
            }
            if (prevTimer) {
                clearTimeout(prevTimer);
            }
            if (closeTimer) {
                clearTimeout(closeTimer);
            }
            if (shortcutTimer) {
                clearTimeout(shortcutTimer);
            }
            if (infoTimer) {
                clearTimeout(infoTimer);
            }

            $("#infoAction").show();
            infoTimer = setTimeout(function () {
                $("#infoAction").fadeOut(1000);
            }, 5000);
            $("#shortcutAction").show();
            shortcutTimer = setTimeout(function () {
                $("#shortcutAction").fadeOut(1000);
            }, 5000);
            $("#nextSlide").show();
            nextTimer = setTimeout(function () {
                $("#nextSlide").fadeOut(1000);
            }, 5000);
            $("#prevSlide").show();
            prevTimer = setTimeout(function () {
                $("#prevSlide").fadeOut(1000);
            }, 5000);
            $("#closeAction").show();
            closeTimer = setTimeout(function () {
                $("#closeAction").fadeOut(1000);
            }, 5000);
        }
    });

    $("#mediaSrc, #playPause").on("click", function (e) {
        if ($("#slideshowContainer").css("display") === "block") {
            slideshowGalleryPlayPause();
        }
    });

    function slideshowInfo() {
        if ($("#mediaInfo").is(":visible")) {
            $("#mediaInfo").css("display", "none");
        } else {
            $("#mediaInfo").css("display", "block");
        }
    }

    function slideshowGalleryPlayPause() {
        $("#playPause").stop(true, true);

        if (slideshowIsPaused === false) {
            $("#playPause").removeClass("bi-play-circle").addClass("bi-pause-circle");
            slideshowIsPaused = true;
            // $("#mediaInfo").css("display", "block");
            $("#playPause").show();
        } else {
            $("#playPause").removeClass("bi-pause-circle").addClass("bi-play-circle");
            slideshowIsPaused = false;
            // $("#mediaInfo").css("display", "none");
            $("#playPause").show();
            $("#playPause").fadeOut(3000);
        }
    }

    $("#viewSlideshow").on("click", function (e) {
        e.preventDefault();

        if (Util.isMobile()) {
            $("#shortcutAction").addClass("bi-hand-index").removeClass("bi-keyboard");
        } else {
            $("#shortcutAction").addClass("bi-keyboard").removeClass("bi-hand-index");
        }

        document.body.style.overflow = 'hidden';

        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
        }

        firstTime = true;

        slideshowIntervalId = window.setInterval(function () {
            if (slideshowIsPaused === false) {
                slideshowCurrentIndex++;
                getSlideshowImage(function () {
                    slideshowProceed = true;
                });
            }
        }, (slideshowIsElapsed * 1000));

        getSlideshowImage(function (loaded) {
            slideshowProceed = true;

            if (loaded === true) {
                $("#playPause").show();
                $("#playPause").fadeOut(3000);

                $("#slideshowContainer").css({
                    "width": "101%",
                    "height": "101%",
                    "display": "block",
                    "z-index": 9999,
                    "background-color": "#000000",
                    "line-height": 1,
                    "overflow": "hidden"
                });

                if (Util.isMobile() === false) {
                    $("#mediaInfo").css({
                        "max-width": ($(window).width() + 1),
                        "white-space": "nowrap"
                    });
                }

                $("#playPause").css("display", "block");

                shashin.closeToastMessages({tags:["subhtml"]});
            }
        });
    });
}