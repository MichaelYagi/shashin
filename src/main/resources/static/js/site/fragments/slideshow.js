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
    let slideshowCursorVisible = true;
    let slideshowProceed = true;
    let cjsc = null;
    let currentPhotoUrl = null;
    let currentMetadata = null;

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

            tempImage.onload = function () {
                slideshowProceed = true;

                $("#mediaSrc").fadeOut((slideshowStarted === false) ? 0 : 300, function () {
                    $("#mediaSrc").attr("src", photoUrl).fadeIn((slideshowStarted === false) ? 0 : 600);

                    $("#playPause").css({
                        "font-size": "10rem",
                        "color": "#FFFFFF",
                        "z-index": 99999,
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

                    if (Util.isMobile() === false) {
                        $("#mediaInfo").css({
                            "max-width": ($(window).width() + 1),
                            "font-size": "1.7rem"
                        });

                        $("#prevSlide").css({
                            "font-size": "4rem",
                            "color": "#FFFFFF",
                            "z-index": 99999,
                            "position": "absolute",
                            "top": "50%",
                            "left": "2%"
                        });

                        $("#nextSlide").css({
                            "font-size": "4rem",
                            "color": "#FFFFFF",
                            "z-index": 99999,
                            "position": "absolute",
                            "top": "50%",
                            "right": "2%"
                        });

                        $("#closeAction").css({
                            "font-size": "4rem",
                            "color": "#FFFFFF",
                            "z-index": 99999,
                            "position": "absolute",
                            "top": "2%",
                            "right": "2%"
                        });

                        $("#infoAction").css({
                            "font-size": "2rem",
                            "color": "#FFFFFF",
                            "z-index": 99999,
                            "position": "absolute",
                            "top": "3.6%",
                            "right": "5.5%"
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
                });
            };

            tempImage.src = photoUrl;
        } else if (callback !== undefined && typeof callback === 'function') {
            callback(false);
        }
    }

    function exitSlideshowGallery() {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.documentElement.exitFullscreen) {
            document.documentElement.exitFullscreen();
        }

        document.body.style.overflow = 'visible';

        $("#slideshowGallery").css({
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

        let castLink = "";
        if (cjsc !== null) {
            // Create
            if (cjsc.state === "disconnected") {
                options.headerSubtext = "<a href='#' id='toggleCast' style='display: none;'><span id='toggleCastIcon' class='bi-cast' style='font-size:1rem;color: lightgray;'></span></a>";
            } else {
                options.headerSubtext = "<a href='#' id='toggleCast' style='display: none;'><span id='toggleCastIcon' class='bi-stop-circle' style='font-size:1rem;color: lightgray;'></span></a>";
            }
        }

        if (Util.isMobile() === false) {
            let message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>d</strong></span></div><div class='col-9'>Show/close this window</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Esc</strong></span></div><div class='col-9'>Exit fullscreen. Press Esc again to exit slideshow.</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>f</strong></span></div><div class='col-9'>Fullscreen</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Space</strong></span></div><div class='col-9'>Play/pause</div></div>" +
                "<span id='castKey' style='display: none;'><div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>c</strong></span></div><div class='col-9'>Start/stop casting</div></div></span>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>i</strong></span></div><div class='col-9'>Slide info</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>← →</strong></span></div><div class='col-9'>Got to next/previous slide</div></div>" +
                "</div>";

            shashin.showToastMessage("Keyboard Shortcuts" + castLink,
                message,
                options
            );
        } else {
            let message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Swipe Up</strong></span></div><div class='col-9'>Show/close this window when in fullscreen</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Swipe Down</strong></span></div><div class='col-9'>Slide info</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Single Tap</strong></span></div><div class='col-9'>Play/pause</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Double Tap</strong></span></div><div class='col-9'>Exit fullscreen. Double tap again to exit slideshow or swipe up to go back to fullscreen</div></div>" +
                "<div class='row mb-1'><div class='col-3 text-center'><span class='badge bg-secondary'><strong>Swipe ← →</strong></span></div><div class='col-9'>Got to next/previous slide</div></div>" +
                "</div>";

            shashin.showToastMessage("Touch Bindings",
                message,
                options
            );
        }

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
        if (Util.isMobile() === true && $("#slideshowGallery").css("display") === "block") {
            if (document.fullscreenElement !== null && (document.documentElement.exitFullscreen || document.exitFullscreen)) {
                if (document.exitFullscreen) {
                    document.exitFullscreen();
                } else {
                    document.documentElement.exitFullscreen();
                }
            } else {
                $("#mediaInfo").css("display", "none");
                exitSlideshowGallery();
                shashin.closeToastMessages({tag: "slide"});
                if (cjsc !== null && cjsc.available) {
                    cjsc.disconnect();
                }
            }
        }
    });

    $("body").on("keyup", function (e) {
        if ($("#slideshowGallery").css("display") === "block") {
            if (e.code === "Escape" || e.keyCode === 27) {
                if (document.fullscreenElement !== null && (document.documentElement.exitFullscreen || document.exitFullscreen)) {
                    if (document.exitFullscreen) {
                        document.exitFullscreen();
                    } else {
                        document.documentElement.exitFullscreen();
                    }
                } else {
                    $("#mediaInfo").css("display", "none");
                    exitSlideshowGallery();
                    shashin.closeToastMessages({tag: "slide"});
                    if (cjsc !== null && cjsc.available) {
                        cjsc.disconnect();
                    }
                }
            }

            // Pause/play slideshow
            if (e.key === " " || e.code === "Space" || e.keyCode === 32) {
                slideshowGalleryPlayPause();
            }

            // Show info
            if (e.key === "i" || e.code === "KeyI" || e.keyCode === 73) {
                slideshowInfo();
            }

            // Cast slideshow
            if (e.key === "c" || e.code === "KeyC" || e.keyCode === 67) {
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
            if (e.key === "d" || e.code === "KeyD" || e.keyCode === 68) {
                if (shashin.hasToast(shashin.toast.placement.bottom.center,{tag: "slide"}) === false) {
                    showInstruction();
                } else {
                    shashin.closeToastMessages({tag: "slide"});
                }
            }

            if (e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.keyCode === "37" || e.key === "ArrowRight" || e.code === "ArrowRight" || e.keyCode === "39") {
                if (((e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.keyCode === "37") && slideshowCurrentIndex === 0) === false && slideshowProceed === true) {
                    if ((e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.keyCode === "37") && slideshowCurrentIndex > 0) {
                        slideshowCurrentIndex--;
                    } else if ((e.key === "ArrowRight" || e.code === "ArrowRight" || e.keyCode === "39") && slideshowCurrentIndex <= slideshowMetadataIds.length - 1) {
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

            if ((e.key === "f" || e.code === "KeyF" || e.keyCode === 70) && document.fullscreenElement === null) {
                document.documentElement.requestFullscreen();
            }
        }
    });

    $("#infoActionButton").on("click", function (e) {
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

    $("#prevSlideButton").on("click", function (e) {
        e.preventDefault();

        if (slideshowCurrentIndex > 0 && slideshowProceed === true) {
            slideshowCurrentIndex--;
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

        if (slideshowCurrentIndex <= slideshowMetadataIds.length - 1) {
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
    });

    Util.detectSwipe("#slideshowGallery", function (direction) {
        if (direction === "up" || direction === "down") {
            if (direction === "up") {
                if (document.fullscreenElement === null) { // Not full screen, return to full screen
                    document.documentElement.requestFullscreen();
                } else if (shashin.hasToast(shashin.toast.placement.bottom.center, {tag: "slide"}) === false) {
                    showInstruction();
                } else {
                    shashin.closeToastMessages({tag: "slide"});
                    if (cjsc !== null && cjsc.available) {
                        cjsc.disconnect();
                    }
                }
            } else {
                slideshowInfo();
            }
        } else if (direction === "left" || direction === "right") {
            if ((direction === "right" && slideshowCurrentIndex === 0) === false && slideshowProceed === true) {
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
        if (Util.isMobile() === false && $("#slideshowGallery").css("display") === "block") {

            if (slideshowMouseTimer) {
                clearTimeout(slideshowMouseTimer);
            }

            if (nextTimer) {
                clearTimeout(nextTimer);
            }
            if (prevTimer) {
                clearTimeout(prevTimer);
            }
            if (closeTimer) {
                clearTimeout(closeTimer);
            }
            if (infoTimer) {
                clearTimeout(infoTimer);
            }

            if (slideshowCursorVisible === false) {
                document.body.style.cursor = "default";
                slideshowCursorVisible = true;
            }

            slideshowMouseTimer = setTimeout(disappearCursor, 3000);

            $("#infoAction").show();
            infoTimer = setTimeout(function () {
                $("#infoAction").fadeOut(1000);
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
        if ($("#slideshowGallery").css("display") === "block") {
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

        document.body.style.overflow = 'hidden';

        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
        }

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

                $("#slideshowGallery").css({
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
                // showInstruction();
            }
        });
    });
}