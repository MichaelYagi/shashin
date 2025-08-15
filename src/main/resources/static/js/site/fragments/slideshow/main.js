function initializeSlideshow(accessTimelineView, queryLimit, slideshowInterval, albumImageCount, locale, slideshowProgress, orientation, fillScreen) {
    $("#slideshowProgress").css("transition", "width "+slideshow.const.pollTimeout+"ms ease-in-out");
    $("#slideshowProgressContainer").css("z-index", 999999);

    // Start slideshow
    $("#viewSlideshow").on("click", function (e) {
        e.preventDefault();
        
        $("#slideshowProgress").css("transition", "width "+slideshow.const.pollTimeout+"ms ease-in-out");
        $("#slideshowProgressContainer").css("z-index", 999999);

        tearDownVideo();
        createVideo();

        if (slideshowProgress === true) {
            $("#slideshowProgressContainer").css("display", "");
        } else {
            $("#slideshowProgressContainer").css("display", "none");
        }

        if (Util.isMobile()) {
            $("#shortcutAction").addClass("bi-hand-index").removeClass("bi-keyboard");
        } else {
            $("#shortcutAction").addClass("bi-keyboard").removeClass("bi-hand-index");
        }

        if (document.fullscreenEnabled) {
            $("#screenAction").addClass("bi-fullscreen-exit").removeClass("bi-fullscreen");
        }

        document.body.style.overflow = 'hidden';

        if (!document.fullscreenElement) {
            document.documentElement.requestFullscreen();
        }

        slideshow.firstTime = true;

        setupSlideshowInterval();

        if (fillScreen === true) {
            $("#mediaSrc").addClass("slideshow-fill");
        } else {
            $("#mediaSrc").removeClass("slideshow-fill");
        }

        getSlideshowImage(function (loaded) {
            slideshow.slideshowProceed = true;

            if (loaded === true) {
                $("#playPause").show();
                $("#playPause").fadeOut(slideshow.const.playPauseHideTime);

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
                        "width": "50em" //($(window).width() + 1)
                    });
                }

                $("#playPause").css("display", "block");

                $("#mediaSrcContainer, #playPauseContainer, #prevSlideButton, #nextSlideButton, #closeActionButton, #shortcutActionButton, #infoActionButton, #screenActionButton, #downloadContainer").css("display", "block");

                shashin.closeToastMessages({tags:["subhtml"]});
            }
        });
    });

    function startProgressLoop() {
        const duration = slideshowInterval * 1000;
        const startTime = Date.now();

        function animate() {
            if (slideshow.slideshowIsPaused) return;

            const now = Date.now();
            const elapsed = now - startTime + slideshow.elapsedBeforePause;
            const progress = Math.min((elapsed / duration) * 100, 100);

            $("#slideshowProgress").css("width", progress + "%");
            $("#slideshowProgressContainer").attr("aria-valuenow", Math.round(progress));

            if (progress < 100) {
                slideshow.slideTimer = requestAnimationFrame(animate);
            }
        }

        cancelProgressLoop(); // Stop any previous animation
        slideshow.slideTimer = requestAnimationFrame(animate);
    }

    function setupSlideshowInterval() {
        clearInterval(slideshow.slideshowIntervalId);
        if (slideshow.slideTimer) cancelAnimationFrame(slideshow.slideTimer);

        slideshow.startTime = Date.now();
        startProgressLoop();

        slideshow.slideshowIntervalId = setInterval(() => {
            if (!slideshow.slideshowIsPaused) {
                slideshow.slideshowCurrentIndex++;
                getSlideshowImage(() => {
                    slideshow.slideshowProceed = true;

                    resetProgressBar();

                    if (slideshow.slideTimer) cancelAnimationFrame(slideshow.slideTimer);
                    slideshow.startTime = Date.now();
                    startProgressLoop();
                });
            }
        }, slideshowInterval * 1000);
    }

    function restartPoll() {
        slideshow.elapsedBeforePause = 0;
        slideshow.currentTime = Date.now();
        $("#slideshowProgress").css("width", slideshow.elapsedBeforePause.toString());
        $("#slideshowProgressContainer").attr("aria-valuenow", slideshow.elapsedBeforePause.toString());
    }

    function cancelProgressLoop() {
        if (slideshow.slideTimer) {
            cancelAnimationFrame(slideshow.slideTimer);
            slideshow.slideTimer = null;
        }
    }

    function resetProgressBar() {
        const progressBar = $("#slideshowProgress");
        progressBar.css("transition", "none");
        progressBar.css("width", "0");
        $("#slideshowProgressContainer").attr("aria-valuenow", "0");
        function forceReflow(element) {
            return element[0].offsetWidth;
        }
        forceReflow(progressBar);
    }

    const http = new Http("getAlbums");
    http.ajax("get", "/slideshowalbums").then(function (data) {
        if (data.hasOwnProperty("albumsList") && data.hasOwnProperty("slideshowAlbum")  && data.slideshowAlbum.hasOwnProperty("albums") && data.slideshowAlbum.albums.length > 0) {
            const albumsArray = data.albumsList;
            const slideshowAlbumArray = data.slideshowAlbum.albums;

            let html = "";
            if (slideshowAlbumArray.length > 0) {
                html += '<button class="dropdown-item" type="button">' +
                    '<input type="checkbox" data-album-id=0 class="slideshowAlbum" value="all" name="album[]" id="album-0"'+(slideshowAlbumArray.includes("all") ? ' checked="checked"' : '')+'> ' +
                    '<label for="album-0">'+shashin.getTranslatedValue("main.pages.slideshow.all")+'</label>' +
                '</button>';
            }

            let hrFlag = false;
            for (let index in albumsArray) {
                if (albumsArray.hasOwnProperty(index)) {
                    const album = albumsArray[index];
                    if (album.hasOwnProperty("albumPhotoCount") && album.albumPhotoCount > 0) {
                        if (hrFlag === false) {
                            html += "<hr>";
                            hrFlag = true;
                        }
                        html += '<button class="dropdown-item" type="button">' +
                            '<input type="checkbox" data-album-id=' + album.id + ' class="slideshowAlbum" value="' + album.name + '" name="album[]" id="album-' + album.id + '"' + (slideshowAlbumArray.includes(String(album.id)) ? ' checked="checked"' : '') + '> ' +
                            '<label for="album-' + album.id + '">' + album.name + '</label>' +
                            '</button>';
                    }
                }
            }
            $("#slideshowAlbumSelectionList").html(html);

            $(".slideshowAlbum").on("click", function () {
                const albumId = parseInt($(this).attr("data-album-id"));

                if (albumId === 0) {
                    if ($('#album-' + albumId).prop("checked") === true) {
                        $(".slideshowAlbum").prop('checked',false);
                        $('#album-' + albumId).prop('checked', true);
                    }
                } else {
                    if ($('#album-' + albumId).prop("checked") === true) {
                        $('#album-0').prop('checked', false);
                    }
                }

                if ($('.slideshowAlbum:checked').length === 0) {
                    $('#album-0').prop('checked', true);
                }
            });

            $("#confirmSlideshowAlbumSelection").on("click", function () {
                const http = new Http("getAlbums");
                const checkedValues = [];
                $('.slideshowAlbum:checked').each(function() {
                    checkedValues.push($(this).attr("data-album-id"));
                });
                const json = {
                    albums:checkedValues
                };
                http.ajax("post", "/slideshowalbums", JSON.stringify(json)).then(function (data) {
                    if (data.status !== "success") {
                        shashin.printMessageToConsole("Error getting random image after configuring albums chosen", {tag: "slideshow"});
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.media.upload.errors"),
                            shashin.getTranslatedValue("main.toast.media.random.image.errors"),
                            {
                                icon: "bi-exclamation-triangle",
                                iconColor:"#FF0000",
                                borderColor:"danger",
                                autohide: true
                            }
                        );
                    }
                });
            });
        }
    });

    $('#slideshowAlbumSelection').on('hidden.bs.modal', function (e) {
        const http = new Http("getAlbums");
        http.ajax("get", "/slideshowalbums").then(function (data) {
            if (data.hasOwnProperty("slideshowAlbum")  && data.slideshowAlbum.hasOwnProperty("albums") && data.slideshowAlbum.albums.length > 0) {
                $(".slideshowAlbum").prop('checked', false);

                const slideshowAlbumArray = data.slideshowAlbum.albums;
                for (let index in slideshowAlbumArray) {
                    if (slideshowAlbumArray.hasOwnProperty(index)) {
                        const slideshowId = (slideshowAlbumArray[index] === "all" ? 0 : parseInt(slideshowAlbumArray[index]));
                        $('#album-'+slideshowId).prop('checked', true);
                    }
                }
            }

        });
    });

    function getSlideshowImage(callback) {
        const currentIndex = slideshow.slideshowCurrentIndex;

        // Use preloaded image if available and matches current index
        if (
            slideshow.preloadedImage &&
            slideshow.preloadedImage.index === currentIndex &&
            slideshow.preloadedImage.metadata &&
            slideshow.preloadedImage.baseUrl
        ) {
            processSlideData({
                metadata: slideshow.preloadedImage.metadata,
                baseUrl: slideshow.preloadedImage.baseUrl
            }, "existing", callback);

            // Clear preloaded image after use
            slideshow.preloadedImage = null;

            // Preload the next image
            preloadNextImage();
            return;
        }

        // Otherwise, fetch image normally
        const http = new Http("show slideshow");

        if (slideshow.slideshowProceed === true) {
            slideshow.slideshowProceed = false;

            if (
                slideshow.slideshowMetadataIds.length > 0 &&
                currentIndex >= 0 &&
                currentIndex < slideshow.slideshowMetadataIds.length
            ) {
                const imageId = slideshow.slideshowMetadataIds[currentIndex];
                http.ajax("get", "/media/metadata/" + imageId).then(function (data) {
                    processSlideData(data, "existing", callback);
                    preloadNextImage(); // Preload next after current is shown
                });
            } else {
                http.ajax("get", "/random/metadata/type/image?includeSlideAlbums=true&orientation=" + orientation)
                    .then(function (data) {
                        processSlideData(data, "new", callback);
                        preloadNextImage(); // Preload next after current is shown
                    });
            }
        }
    }

    function preloadNextImage() {
        const nextIndex = slideshow.slideshowCurrentIndex + 1;

        if (nextIndex < slideshow.slideshowMetadataIds.length) {
            const nextId = slideshow.slideshowMetadataIds[nextIndex];
            const http = new Http("preload");

            http.ajax("get", "/media/metadata/" + nextId).then(function (data) {
                if (data && data.metadata && data.baseUrl) {
                    const img = new Image();
                    img.src = data.baseUrl + "/api/v1/image/" + data.metadata.id;

                    slideshow.preloadedImage = {
                        index: nextIndex,
                        metadata: data.metadata,
                        baseUrl: data.baseUrl,
                        image: img
                    };
                }
            });
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

            slideshow.currentPhotoUrl = photoUrl;
            slideshow.currentMetadata = data.metadata;

            // const downloadUrl = "/api/v1/image/"+data.metadata.id+"/download";
            // $("#downloadActionButton").attr("href",downloadUrl);

            if (slideshow.cjsc !== null && slideshow.cjsc.available && slideshow.cjsc.connected) {
                const cjscMetadata = {
                    title: data.metadata.title
                };

                if (data.metadata.description !== null && data.metadata.description !== "") {
                    cjscMetadata.description = data.metadata.description;
                }
                slideshow.cjsc.cast(photoUrl+".jpg", cjscMetadata);
            }

            const tempImage = new Image();

            tempImage.onerror = function (error) {
                shashin.printMessageToConsole("Error: " + error, {tag: "slideshow"});
                slideshow.slideshowProceed = true;
            };

            if (slideshow.firstTime === true) {
                waitingScreen();
            }

            tempImage.onload = function () {
                if (slideshow.firstTime === true) {
                    showControls();
                }

                slideshow.slideshowProceed = true;
                slideshow.firstTime = false;

                if ($("#mediaInfo").is(":visible")) {
                    $("#mediaInfo").fadeOut((slideshow.slideshowStarted === false) ? 0 : 300, function () {
                        $("#mediaInfo").attr("src", photoUrl).fadeIn((slideshow.slideshowStarted === false) ? 0 : 600);
                    });
                }

                $("#mediaSrc").fadeOut((slideshow.slideshowStarted === false) ? 0 : 300, function () {
                    $("#mediaSrc").attr("src", photoUrl).fadeIn((slideshow.slideshowStarted === false) ? 0 : 600);

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

                    function styleControl(id, fontSize, top, side, sideValue) {
                        $(id).css({
                            "font-size": fontSize,
                            "color": "#FFFFFF",
                            "z-index": 99998,
                            "position": "absolute",
                            "top": top,
                            [side]: sideValue
                        });
                    }

                    styleControl("#nextSlide", "4rem", "46%", "right", "2%");
                    styleControl("#prevSlide", "4rem", "46%", "left", "2%");
                    styleControl("#closeAction", "4rem", "5px", "right", "5px");
                    styleControl("#shortcutAction", "2rem", "22px", "right", "77px");
                    styleControl("#infoAction", "2rem", "23px", "right", "135px");
                    styleControl("#downloadAction", "2rem", "23px", "right", "251px");
                    styleControl("#slideSpinner", "2rem", "23px", "left", "2%");
                    if (document.fullscreenEnabled) {
                        styleControl("#screenAction", "2rem", "23px", "right", "193px");
                    }

                    $("#mediaInfo").addClass("text-center");
                    $("#mediaInfo").css({
                        "transform": "translate(-50%, -30px)"
                    });

                    if (Util.isMobile() === false) {
                        $("#mediaInfo").css({
                            "width": "50em", //($(window).width() + 1)
                            "font-size": "1.7rem"
                        });
                    } else {
                        $("#mediaInfo").css({
                            "font-size": "1rem"
                        });
                    }

                    if (type === "new") {
                        slideshow.slideshowMetadataIds.push(data.metadata.id);
                    }

                    // Remove first if over query limit
                    if (slideshow.slideshowMetadataIds.length > queryLimit) {
                        slideshow.slideshowMetadataIds.splice(0, 1); // At position 0, remove 1
                        slideshow.slideshowCurrentIndex--;
                    }

                    const takenDateString = data.metadata.year + "-" + data.metadata.month + "-" + data.metadata.day;
                    const takenDate = new Date(takenDateString);
                    const options = {weekday: 'long', year: 'numeric', month: 'short', day: 'numeric'};
                    let description = takenDate.toLocaleDateString(locale, options);

                    if (accessTimelineView === false && data.hasOwnProperty("albumIds") === true && data.albumIds.hasOwnProperty(0) === true) {
                        description = "<a style='color:#DBE9F4;text-decoration:none;' href='/album/" + data.albumIds[0] + "' target='_blank'>" + takenDate.toLocaleDateString(locale, options) + "</a>";
                    } else if (accessTimelineView === true) {
                        if (Util.isSafari()) {
                            description = "<a style='color:#DBE9F4;text-decoration:none;' href='/taken' target='_blank'>" + takenDate.toLocaleDateString(locale, options) + "</a>";
                        } else {
                            description = "<a style='color:#DBE9F4;text-decoration:none;' href='/timeline#" + takenDateString + "' target='_blank'>" + takenDate.toLocaleDateString(locale, options) + "</a>";
                        }
                    }

                    if (data.shortPlaceName !== null && data.shortPlaceName !== "") {
                        description += " • " + data.shortPlaceName;
                    }

                    if (data.metadata.description !== null && data.metadata.description !== "") {
                        description += "<div>" + data.metadata.description + "</div>";
                    }
                    $("#mediaInfo").html(description);

                    slideshow.slideshowStarted = true;

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

    $("#closeAction,#infoAction,#shortcutAction,#nextSlide,#prevSlide,#screenAction,#downloadAction").on("mouseenter", function (e) {
        e.preventDefault();

        [slideshow.nextTimer, slideshow.prevTimer, slideshow.closeTimer, slideshow.shortcutTimer, slideshow.downloadTimer, slideshow.infoTimer, slideshow.slideshowMouseTimer].forEach(timer => {
            if (timer) {
                clearTimeout(timer);
            }
        });

        if (document.fullscreenEnabled && slideshow.screenTimer) {
            clearTimeout(slideshow.screenTimer);
        }

        showCursor();

        $("#infoAction").show();
        $("#shortcutAction").show();
        $("#nextSlide").show();
        $("#prevSlide").show();
        $("#closeAction").show();
        if (document.fullscreenEnabled) {
            $("#screenAction").show();
        }
        $("#downloadAction").show();
    });

    $("#closeAction,#infoAction,#shortcutAction,#nextSlide,#prevSlide,#screenAction,#downloadAction").on("mouseleave", function (e) {
        e.preventDefault();

        setTimeout(function () {
            let hovered = $("#slideshowContainer").find("#closeAction:hover,#infoAction:hover,#shortcutAction:hover,#nextSlide:hover,#prevSlide:hover,#screenAction:hover,#downloadAction:hover").length;

            if (hovered === 0) {
                $("#infoAction").fadeOut(slideshow.const.fadeOutTime);
                $("#screenAction").fadeOut(slideshow.const.fadeOutTime);
                $("#downloadAction").fadeOut(slideshow.const.fadeOutTime);
                $("#shortcutAction").fadeOut(slideshow.const.fadeOutTime);
                $("#nextSlide").fadeOut(slideshow.const.fadeOutTime);
                $("#prevSlide").fadeOut(slideshow.const.fadeOutTime);
                $("#closeAction").fadeOut(slideshow.const.fadeOutTime);
                hideCursor();
            }
        }, slideshow.const.hideTime);

    });

    function showControls() {
        if ($("#closeAction,#infoAction,#shortcutAction,#nextSlide,#prevSlide,#screenAction,#downloadAction").is(":hidden")) {
            [slideshow.nextTimer, slideshow.prevTimer, slideshow.closeTimer, slideshow.shortcutTimer, slideshow.downloadTimer, slideshow.infoTimer, slideshow.slideshowMouseTimer].forEach(timer => {
                if (timer) {
                    clearTimeout(timer);
                }
            });

            if (document.fullscreenEnabled && slideshow.screenTimer) {
                clearTimeout(slideshow.screenTimer);
            }

            showCursor();

            if (slideshow.isActive === true) {
                slideshow.slideshowMouseTimer = setTimeout(hideCursor, slideshow.const.hideTime);
            }

            $("#infoAction").show();
            slideshow.infoTimer = setTimeout(function () {
                $("#infoAction").fadeOut(slideshow.const.fadeOutTime);
            }, slideshow.const.hideTime);
            if (document.fullscreenEnabled) {
                $("#screenAction").show();
                slideshow.screenTimer = setTimeout(function () {
                    $("#screenAction").fadeOut(slideshow.const.fadeOutTime);
                }, slideshow.const.hideTime);
            }
            $("#downloadAction").show();
            slideshow.downloadTimer = setTimeout(function () {
                $("#downloadAction").fadeOut(slideshow.const.fadeOutTime);
            }, slideshow.const.hideTime);
            $("#shortcutAction").show();
            slideshow.shortcutTimer = setTimeout(function () {
                $("#shortcutAction").fadeOut(slideshow.const.fadeOutTime);
            }, slideshow.const.hideTime);
            $("#nextSlide").show();
            slideshow.nextTimer = setTimeout(function () {
                $("#nextSlide").fadeOut(slideshow.const.fadeOutTime);
            }, slideshow.const.hideTime);
            $("#prevSlide").show();
            slideshow.prevTimer = setTimeout(function () {
                $("#prevSlide").fadeOut(slideshow.const.fadeOutTime);
            }, slideshow.const.hideTime);
            $("#closeAction").show();
            slideshow.closeTimer = setTimeout(function () {
                $("#closeAction").fadeOut(slideshow.const.fadeOutTime);
            }, slideshow.const.hideTime);
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
        $("#screenAction").css("display", "none");
        $("#downloadAction").css("display", "none");
        $("#shortcutAction").css("display", "none");
        $("#nextSlide").css("display", "none");
        $("#prevSlide").css("display", "none");
        $("#closeAction").css("display", "none");
    }

    function exitSlideshowGallery() {
        document.body.style.overflow = 'visible';

        $("#slideshowContainer").css({
            "display": "none"
        });

        slideshow.slideshowCurrentIndex = 0;
        slideshow.slideshowMetadataIds = [];

        if (slideshow.slideshowIntervalId) {
            clearInterval(slideshow.slideshowIntervalId);
            slideshow.slideshowIntervalId = 0;
        }

        if (slideshow.slideshowMouseTimer) {
            clearTimeout(slideshow.slideshowMouseTimer);
        }

        if (slideshow.slideTimer) {
            clearTimeout(slideshow.slideTimer);
        }

        showCursor();
        slideshow.slideshowStarted = false;
        slideshow.slideshowProceed = true;

        $("#mediaSrc").css("opacity", "1");
        $("#playPause").addClass("bi-pause-circle").removeClass("bi-play-circle");
        slideshow.slideshowIsPaused = false;

        shashin.closeToastMessages({tag: "slide"});

        $("#slideshowAlbumSelection").modal('hide');

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

        if (typeof Castjs != "undefined" && slideshow.cjsc === null) {
            slideshow.cjsc = new Castjs();
        }

        if (slideshow.cjsc !== null) {
            // Create
            if (slideshow.cjsc.state === "disconnected") {
                options.headerSubtext = "<a href='#' id='toggleCast' style='display: none;'><span id='toggleCastIcon' class='bi-cast' style='font-size:1rem;color: lightgray;'></span></a>";
            } else {
                options.headerSubtext = "<a href='#' id='toggleCast' style='display: none;'><span id='toggleCastIcon' class='bi-stop-circle' style='font-size:1rem;color: lightgray;'></span></a>";
            }
        }

        let title = "";
        let message = "";
        if (Util.isMobile() === false) {
            message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>i</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.window")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>x</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.exit")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>"+shashin.getTranslatedValue("main.pages.slideshow.space")+"</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.playpause")+"</div></div>" +
                "<span id='castKey' style='display: none;'><div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>c</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.startstop")+"</div></div></span>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>d</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.slideinfo")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>← →</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.nextprev")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>a</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.albumfilter")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>- =</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.idinterval")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>[ ]</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.toggleorientation")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>p</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.toggleprogress")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>f</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.togglefs")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showFill'"+(fillScreen === true ? ' checked' : '')+"></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.fill")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showProgress'"+(slideshowProgress === true ? ' checked' : '')+"></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.showprogress")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='3' id='slideshowOrientationSlide'></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.orientation") + " - <span id='orientationValue'>" + slideshow.const.orientationMap[orientation]+"</span></div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='"+slideshow.const.segments+"' id='slideshowIntervalSlide'></div><div class='col-8'><span id='intervalValue'>"+slideshowInterval+"</span>s "+shashin.getTranslatedValue("main.pages.slideshow.info.interval")+"</div></div>";
                if ((albumImageCount > 1 && accessTimelineView === false) || (albumImageCount > 0 && accessTimelineView === true)) {
                    message += "<div class='row mb-1'><button class='btn btn-secondary' type='button' id='slideshowAlbumNameData' value=''>"+shashin.getTranslatedValue("main.pages.slideshow.info.albumfilter")+"</button></div>";
                }
            message += "</div>";

            title = shashin.getTranslatedValue("main.pages.slideshow.info.title.keyboard");
        } else {
            message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Swipe Up</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.window")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Swipe Down</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.slideinfo")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Single Tap</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.playpause")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Double Tap</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.exit")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Swipe ← →</strong></span></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.nextprev")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showFill'"+(fillScreen === true ? ' checked' : '')+"></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.fill")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showProgress'"+(slideshowProgress === true ? ' checked' : '')+"></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.info.showprogress")+"</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='3' id='slideshowOrientationSlide'></div><div class='col-8'>"+shashin.getTranslatedValue("main.pages.slideshow.orientation") + " - <span id='orientationValue'>" + slideshow.const.orientationMap[orientation]+"</span></div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='"+slideshow.const.segments+"' id='slideshowIntervalSlide'></div><div class='col-8'><span id='intervalValue'>"+slideshowInterval+"</span>s "+shashin.getTranslatedValue("main.pages.slideshow.info.interval")+"</div></div>";
                if ((albumImageCount > 1 && accessTimelineView === false) || (albumImageCount > 0 && accessTimelineView === true)) {
                    message += "<div class='row mb-1'><button class='btn btn-secondary' type='button' id='slideshowAlbumNameData' value=''>"+shashin.getTranslatedValue("main.pages.slideshow.info.albumfilter")+"</button></div>";
                }
            message += "</div>";

            title = shashin.getTranslatedValue("main.pages.slideshow.info.title.touch");
        }

        shashin.showToastMessage(title,
            message,
            options
        );

        $("#slideshowIntervalSlide").val(Math.floor(((slideshowInterval-slideshow.const.min)/slideshow.const.spacing)+1));

        $("#slideshowOrientationSlide").val(orientation+1);

        $("#showProgress").val('checked', slideshowProgress);

        $('#showFill').val('checked', fillScreen);

        if (typeof Castjs != "undefined" && slideshow.cjsc !== null) {
            slideshow.cjsc.on('available', () => {
                $("#castKey").css({"display": "block"});
                $("#toggleCast").css({"display": "block"});
            });
        }

        $("#slideshowAlbumNameData").on("click", function (e) {
            e.preventDefault();

            if (slideshow.slideshowIsPaused === false) {
                slideshowGalleryPlayPause();
            }

            $("#slideshowAlbumSelection").modal('show');
        });

        $("#toggleCast").on("click", function (e) {
            e.preventDefault();

            if (slideshow.cjsc !== null && slideshow.cjsc.available) {
                if ($("#toggleCastIcon").hasClass('bi-cast')) {
                    if (slideshow.currentPhotoUrl !== null) {
                        $("#toggleCastIcon").addClass('bi-stop-circle').removeClass('bi-cast');
                        let cjscMetadata = {};
                        if (slideshow.currentMetadata !== null) {
                            cjscMetadata = {
                                title: slideshow.currentMetadata.title
                            };

                            if (slideshow.currentMetadata.description !== null && slideshow.currentMetadata.description !== "") {
                                cjscMetadata.description = slideshow.currentMetadata.description;
                            }
                        }
                        slideshow.cjsc.cast(slideshow.currentPhotoUrl+".jpg", cjscMetadata);
                    }
                } else {
                    slideshow.cjsc.disconnect();
                    $("#toggleCastIcon").addClass('bi-cast').removeClass('bi-stop-circle');
                }
            }
        });

        $("#slideshowIntervalSlide").on("input", function () {
            slideshowInterval = Math.floor(slideshow.const.min+($(this).val()-1) * slideshow.const.spacing);
            changeSideshowInterval();
        });

        $("#slideshowOrientationSlide").on("input", function () {
            orientation = $(this).val()-1;
            changeSideshowOrientation();
        });

        $("#showProgress").on("input", function () {
            slideshowProgress = $("#showProgress").prop('checked');
            if (slideshowProgress === true) {
                $("#slideshowProgressContainer").css("display", "");
            } else {
                $("#slideshowProgressContainer").css("display", "none");
            }
            changeShowProgress();
        });

        $("#showFill").on("input", function () {
            fillScreen = $("#showFill").prop('checked');
            if (fillScreen === true) {
                $("#mediaSrc").addClass("slideshow-fill");
            } else {
                $("#mediaSrc").removeClass("slideshow-fill");
            }
            changeFillScreen();
        });
    }

    function changeSideshowOrientation() {
        $("#orientationValue").text(slideshow.const.orientationMap[orientation]);
        $("#slideshowOrientationSlide").val(orientation+1);

        const http = new Http("orientation");
        const json = {slideshowOrientation: orientation};
        const data = http.ajax("post", "/users/slideshoworientation", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === "success") {
                shashin.printMessageToConsole("Slideshow orientation set: " + slideshow.const.orientationMap[orientation], {tag: "slideshow"});
            } else {
                shashin.printMessageToConsole("Slideshow orientation failed to set", {tag: "slideshow"});
            }
        } else {
            shashin.printMessageToConsole("Slideshow orientation failed request", {tag: "slideshow"});
        }
    }

    function changeSideshowInterval() {
        $("#intervalValue").text(slideshowInterval);
        $("#slideshowIntervalSlide").val(Math.floor((slideshowInterval-slideshow.const.min)/slideshow.const.spacing)+1);

        if (slideshow.slideshowIsPaused === false) {
            setupSlideshowInterval();
        }

        const http = new Http("slideshow interval");
        const json = {slideshowInterval: slideshowInterval};
        const data = http.ajax("post", "/users/slideshowinterval", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === "success") {
                shashin.printMessageToConsole("Slideshow interval set: " + slideshowInterval + "s", {tag: "slideshow"});
            } else {
                shashin.printMessageToConsole("Slideshow interval failed to set", {tag: "slideshow"});
            }
        } else {
            shashin.printMessageToConsole("Slideshow interval failed request", {tag: "slideshow"});
        }
    }

    function changeShowProgress() {
        const http = new Http("show progress");
        const json = {slideshowProgress: slideshowProgress};
        const data = http.ajax("post", "/users/slideshowprogress", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === "success") {
                shashin.printMessageToConsole("Slideshow progress set: " + slideshowprogress + "s", {tag: "slideshow"});
            } else {
                shashin.printMessageToConsole("Slideshow progress failed to set", {tag: "slideshow"});
            }
        } else {
            shashin.printMessageToConsole("Slideshow progress failed request", {tag: "slideshow"});
        }
    }

    function changeFillScreen() {
        const http = new Http("fill screen");
        const json = {slideshowFillScreen: fillScreen};
        const data = http.ajax("post", "/users/slideshowfillscreen", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            if (data.status === "success") {
                shashin.printMessageToConsole("Slideshow fill screen set: " + fillScreen + "s", {tag: "slideshow"});
            } else {
                shashin.printMessageToConsole("Slideshow fill screen failed to set", {tag: "slideshow"});
            }
        } else {
            shashin.printMessageToConsole("Slideshow fill screen failed request", {tag: "slideshow"});
        }
    }

    function exitSlideshow() {
        $("#mediaSrc").css("opacity", "1");
        slideshow.slideshowStarted = false;
        slideshow.slideshowIsPaused = false;
        slideshow.slideshowCurrentIndex = 0;
        slideshow.slideshowMetadataIds = [];
        slideshow.slideshowProceed = true;
        slideshow.firstTime = true;
        slideshow.isFileDialogOpened = false;
        slideshow.isActive = false;

        showCursor();

        $("#mediaInfo").css("display", "none");
        exitSlideshowGallery();

        // Background video teardown
        tearDownVideo();

        shashin.closeToastMessages({tag: "slide"});

        // Disconnect from cast if connected
        if (slideshow.cjsc !== null && slideshow.cjsc.available) {
            slideshow.cjsc.disconnect();
        }

        if (document.fullscreenEnabled && document.fullscreenElement !== null && document.exitFullscreen) {
            document.exitFullscreen();
            if (document.exitFullscreen) {
                document.exitFullscreen();
            }
        }
    }

    $("#downloadContainer").on("click", function (e) {
        e.preventDefault();

        if (slideshow.currentMetadata !== null) {
            if (slideshow.slideshowIsPaused === false) {
                slideshowGalleryPlayPause();
            }

            const downloadUrl = "/api/v1/image/"+slideshow.currentMetadata.id+"/download";
            const a = document.createElement('a');
            a.href = downloadUrl;
            a.download = downloadUrl.split('/').pop();
            document.body.appendChild(a);
            a.click();
            slideshow.isFileDialogOpened = true;
            document.body.removeChild(a);
        }
    });

    $(window).on('focus', function () {
        if (slideshow.isFileDialogOpened === true && slideshow.slideshowIsPaused === false) {
            slideshow.isFileDialogOpened = false;
            slideshowGalleryPlayPause();
        }
    });

    $("body").on("dblclick", function (e) {
        e.preventDefault();

        if (Util.isMobile() === true && $("#slideshowContainer").css("display") === "block") {
            exitSlideshow();
        }
    });

    $("body").on("pointerup", shashin.detectDoubleTap(200));
    $("body").on('doubletap', function(e) {
        e.preventDefault();

        if (Util.isMobile() === true && $("#slideshowContainer").css("display") === "block") {
            exitSlideshow();
        }
    });

    // Check for entering/leaving fullscreen
    $(window).on("resize", function () {
        const maxHeight = window.screen.height,
            maxWidth = window.screen.width,
            curHeight = window.innerHeight,
            curWidth = window.innerWidth;

        if (maxWidth === curWidth && maxHeight === curHeight) {
            $("#screenAction").removeClass("bi-fullscreen").addClass("bi-fullscreen-exit");
        } else {
            $("#screenAction").removeClass("bi-fullscreen-exit").addClass("bi-fullscreen");
        }
    });

    $("body").on("keyup", function (e) {
        if ($("#slideshowContainer").css("display") === "block") {

            if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
                if ($("#slideshowAlbumSelection").hasClass("show")) {
                    $("#slideshowAlbumSelection").modal('hide');
                } else {
                    document.body.style.overflow = 'visible';
                    exitSlideshow();
                }
            }

            if (e.key === "x" || e.code === "KeyX" || e.which === 88 || e.keyCode === 88) {
                exitSlideshow();
            }

            // Pause/play slideshow
            if (e.key === " " || e.code === "Space" || e.which === 32 || e.keyCode === 32) {
                slideshowGalleryPlayPause();
            }

            // Show info
            if (e.key === "d" || e.code === "KeyD" || e.which === 68 || e.keyCode === 68) {
                slideshowInfo();
            }

            // Cast slideshow
            if (e.key === "c" || e.code === "KeyC" || e.which === 67 || e.keyCode === 67) {
                if (slideshow.cjsc !== null && slideshow.cjsc.available) {
                    if ($("#toggleCastIcon").hasClass('bi-cast')) {
                        if (slideshow.currentPhotoUrl !== null) {
                            $("#toggleCastIcon").addClass('bi-stop-circle').removeClass('bi-cast');
                            let cjscMetadata = {};
                            if (slideshow.currentMetadata !== null) {
                                cjscMetadata = {
                                    title: slideshow.currentMetadata.title
                                };

                                if (slideshow.currentMetadata.description !== null && slideshow.currentMetadata.description !== "") {
                                    cjscMetadata.description = slideshow.currentMetadata.description;
                                }
                            }
                            slideshow.cjsc.cast(slideshow.currentPhotoUrl+".jpg", cjscMetadata);
                        }
                    } else {
                        slideshow.cjsc.disconnect();
                        $("#toggleCastIcon").addClass('bi-cast').removeClass('bi-stop-circle');
                    }
                }
            }

            // Show key binding toast
            if (e.key === "i" || e.code === "KeyI" || e.which === 73 || e.keyCode === 73) {
                if (shashin.hasToast(shashin.toast.placement.bottom.center,{tag: "slide"}) === false) {
                    showInstruction();
                } else {
                    shashin.closeToastMessages({tag: "slide"});
                }
            }

            // Show album filter
            if ((albumImageCount > 1 && accessTimelineView === false) || (albumImageCount > 0 && accessTimelineView === true) && (e.key === "a" || e.code === "KeyA" || e.which === 65 || e.keyCode === 65)) {
                if (slideshow.slideshowIsPaused === false) {
                    slideshowGalleryPlayPause();
                }

                $("#slideshowAlbumSelection").modal('show');
            }

            // Decrease elapsed time
            if (e.key === "-" || e.code === "Minus" || e.which === 189 || e.keyCode === 189) {
                const currElapsed = slideshowInterval;

                if ((currElapsed - slideshow.const.spacing) >= slideshow.const.min) {
                    slideshowInterval = currElapsed - slideshow.const.spacing;
                    changeSideshowInterval();

                    shashin.closeToastMessages({tag: "slide", placement: shashin.toast.placement.top.center});
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.slideshow.decrease.title"),
                        shashin.getTranslatedValue("main.toast.slideshow.decrease.body",slideshowInterval),
                        {
                            icon: "bi-info-circle",
                            autohide: true,
                            placement:shashin.toast.placement.top.center,
                            tag: "slide"
                        }
                    );
                } else {
                    shashin.closeToastMessages({tag: "slide", placement: shashin.toast.placement.top.center});
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.slideshow.decrease.title"),
                        shashin.getTranslatedValue("main.toast.slideshow.decrease.body2",slideshowInterval),
                        {
                            icon: "bi-info-circle",
                            autohide: true,
                            placement:shashin.toast.placement.top.center,
                            tag: "slide"
                        }
                    );
                }
            }

            // Increase elapsed time
            if (e.key === "=" || e.code === "Equal" || e.which === 187 || e.keyCode === 187) {
                const currElapsed = slideshowInterval;

                if ((currElapsed + slideshow.const.spacing) <= slideshow.const.max) {
                    slideshowInterval = currElapsed + slideshow.const.spacing;

                    changeSideshowInterval();

                    shashin.closeToastMessages({tag: "slide", placement: shashin.toast.placement.top.center});
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.slideshow.increase"),
                        shashin.getTranslatedValue("main.toast.slideshow.increase.body",slideshowInterval),
                        {
                            icon: "bi-info-circle",
                            autohide: true,
                            placement:shashin.toast.placement.top.center,
                            tag: "slide"
                        }
                    );
                } else {
                    shashin.closeToastMessages({tag: "slide", placement: shashin.toast.placement.top.center});
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.slideshow.decrease.title"),
                        shashin.getTranslatedValue("main.toast.slideshow.increase.body2",slideshowInterval),
                        {
                            icon: "bi-info-circle",
                            autohide: true,
                            placement:shashin.toast.placement.top.center,
                            tag: "slide"
                        }
                    );
                }
            }

            // Change orientation
            if (e.key === "]" || e.code === "BracketRight" || e.which === 221 || e.keyCode === 221) {
                const currOrientation = orientation;

                if ((currOrientation + 1) <= 2) {
                    orientation += 1;
                    changeSideshowOrientation();
                    shashin.closeToastMessages({tag: "slide", placement: shashin.toast.placement.top.center});
                    shashin.showToastMessage(shashin.getTranslatedValue("main.pages.slideshow.orientation"),
                        shashin.getTranslatedValue("main.toast.slideshow.orientation.body",slideshow.const.orientationMap[orientation].toLowerCase()),
                        {
                            icon: "bi-info-circle",
                            autohide: true,
                            placement:shashin.toast.placement.top.center,
                            tag: "slide"
                        }
                    );
                }
            }

            // Change orientation
            if (e.key === "[" || e.code === "BracketLeft" || e.which === 219 || e.keyCode === 219) {
                const currOrientation = orientation;

                if ((currOrientation - 1) >= 0) {
                    orientation -= 1;
                    changeSideshowOrientation();
                    shashin.closeToastMessages({tag: "slide", placement: shashin.toast.placement.top.center});
                    shashin.showToastMessage(shashin.getTranslatedValue("main.pages.slideshow.orientation"),
                        shashin.getTranslatedValue("main.toast.slideshow.orientation.body",slideshow.const.orientationMap[orientation].toLowerCase()),
                        {
                            icon: "bi-info-circle",
                            autohide: true,
                            placement:shashin.toast.placement.top.center,
                            tag: "slide"
                        }
                    );
                }
            }

            // Show progress
            if (e.key === "p" || e.code === "KeyP" || e.which === 80 || e.keyCode === 80) {
                if (slideshowProgress === true) {
                    $("#showProgress").val('checked', false);
                    $('#showProgress').prop('checked', false);
                    $("#slideshowProgressContainer").css("display", "none");
                    slideshowProgress = false;
                } else {
                    $("#showProgress").val('checked', true);
                    $('#showProgress').prop('checked', true);
                    $("#slideshowProgressContainer").css("display", "");
                    slideshowProgress = true;
                }
                changeShowProgress();
            }

            // Show fill to screen
            if (e.key === "f" || e.code === "KeyF" || e.which === 70 || e.keyCode === 70) {
                if (fillScreen === true) {
                    $("#mediaSrc").removeClass("slideshow-fill");
                    $("#showFill").val('checked', false);
                    $('#showFill').prop('checked', false);
                    fillScreen = false;
                } else {
                    $("#mediaSrc").addClass("slideshow-fill");
                    $("#showFill").val('checked', true);
                    $('#showFill').prop('checked', true);
                    fillScreen = true;
                }
                changeFillScreen();
            }

            if (e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37 || e.key === "ArrowRight" || e.code === "ArrowRight" || e.which === 39 || e.keyCode === 39) {
                if (((e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37) && slideshow.slideshowCurrentIndex === 0) === false && slideshow.slideshowProceed === true) {

                    $("#slideSpinner").show();

                    if ((e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37) && slideshow.slideshowCurrentIndex > 0) {
                        slideshow.slideshowCurrentIndex--;
                    } else if ((e.key === "ArrowRight" || e.code === "ArrowRight" || e.which === 39 || e.keyCode === 39) && slideshow.slideshowCurrentIndex <= slideshow.slideshowMetadataIds.length - 1) {
                        slideshow.slideshowCurrentIndex++;
                    }

                    getSlideshowImage(function () {
                        slideshow.slideshowProceed = true;
                    });

                    if (slideshow.slideshowIsPaused === false) {
                        setupSlideshowInterval();
                    } else {
                        restartPoll();
                    }
                } else {
                    return false;
                }
            }
        }
    });

    $("#screenActionButton").on("click", function (e) {
        e.preventDefault();

        // If fullscreen, show bi-fullscreen-exit, else bi-fullscreen
        if (document.fullscreenElement !== null && document.exitFullscreen) {
            document.exitFullscreen();
            $("#screenAction").removeClass("bi-fullscreen-exit").addClass("bi-fullscreen");
        } else if (document.fullscreenElement === null && document.documentElement.requestFullscreen) {
            document.documentElement.requestFullscreen();
            $("#screenAction").addClass("bi-fullscreen-exit").removeClass("bi-fullscreen");
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

        exitSlideshow();
    });

    $("#infoActionButton").on("click", function (e) {
        e.preventDefault();

        slideshowInfo();
    });

    $("#prevSlideButton").on("click", function (e) {
        e.preventDefault();

        if (slideshow.slideshowCurrentIndex > 0 && slideshow.slideshowProceed === true) {
            slideshow.slideshowCurrentIndex--;
        } else {
            return false;
        }

        $("#slideSpinner").show();

        getSlideshowImage(function () {
            slideshow.slideshowProceed = true;
        });

        if (slideshow.slideshowIsPaused === false) {
            setupSlideshowInterval();
        } else {
            restartPoll();
        }
    });

    $("#nextSlideButton").on("click", function (e) {
        e.preventDefault();

        if (slideshow.slideshowCurrentIndex <= slideshow.slideshowMetadataIds.length - 1 && slideshow.slideshowProceed === true) {
            slideshow.slideshowCurrentIndex++;
        } else {
            return false;
        }

        $("#slideSpinner").show();

        getSlideshowImage(function () {
            slideshow.slideshowProceed = true;
        });

        if (slideshow.slideshowIsPaused === false) {
            setupSlideshowInterval();
        } else {
            restartPoll();
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
            if (slideshow.slideshowProceed === false) {
                return false;
            }

            if ((direction === "right" && slideshow.slideshowCurrentIndex === 0) === false && slideshow.slideshowProceed === true) {
                $("#slideSpinner").show();

                if (slideshow.slideshowCurrentIndex > 0 && direction === "right") {
                    slideshow.slideshowCurrentIndex--;
                } else if (slideshow.slideshowCurrentIndex <= slideshow.slideshowMetadataIds.length - 1 && direction === "left") {
                    slideshow.slideshowCurrentIndex++;
                }

                getSlideshowImage(function () {
                    slideshow.slideshowProceed = true;
                });
                if (slideshow.slideshowIsPaused === false) {
                    setupSlideshowInterval();
                } else {
                    restartPoll();
                }
            }
        }
    });

    function hideCursor() {
        slideshow.slideshowMouseTimer = null;
        document.documentElement.style.cursor = "none";
        document.getElementById("mediaSrc").style.cursor = "none";
        document.getElementById("playPause").style.cursor = "none";
        $('body').css('cursor', 'none');
        slideshow.isActive = false;
    }

    function showCursor() {
        document.documentElement.style.cursor = "default";
        document.getElementById("mediaSrc").style.cursor = "pointer";
        document.getElementById("playPause").style.cursor = "pointer";
        $('body').css('cursor', 'default');
        slideshow.isActive = true;
    }

    $("#slideshowContainer").on("click", function () {
        showControls();
    });

    $("body").on("mousemove", function () {
        if (Util.isMobile() === false && $("#slideshowContainer").css("display") === "block") {

            showCursor();

            if (slideshow.slideshowMouseTimer) {
                clearTimeout(slideshow.slideshowMouseTimer);
            }

            if (slideshow.isActive === true) {
                slideshow.slideshowMouseTimer = setTimeout(hideCursor, slideshow.const.hideTime);
            }
        }

        if (slideshow.firstTime === false || (slideshow.slideshowProceed === true && slideshow.firstTime === true)) {
            showControls();
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

    function slideshowGalleryPlayPause(hideButton) {
        if (hideButton === undefined) {
            hideButton = false;
        }

        $("#playPause").stop(true, true);

        if (slideshow.slideshowIsPaused === false) {
            // Pause
            $("#slideshowProgress").css("width", Math.round((slideshow.elapsedBeforePause / (slideshowInterval * 1000)) * 100) + "%");
            $("#mediaSrc").css("opacity", "0.3");
            $("#playPause").addClass("bi-play-circle").removeClass("bi-pause-circle");
            slideshow.slideshowIsPaused = true;

            // Save elapsed time
            slideshow.elapsedBeforePause += Date.now() - slideshow.startTime;

            clearInterval(slideshow.slideshowIntervalId);
            slideshow.slideshowIntervalId = null;

            cancelProgressLoop(); // Stop animation loop

            if (hideButton === true) {
                $("#playPause").hide();
            } else {
                $("#playPause").show();
            }
        } else {
            // Resume
            $("#mediaSrc").css("opacity", "1");
            $("#playPause").addClass("bi-pause-circle").removeClass("bi-play-circle");
            slideshow.slideshowIsPaused = false;

            const remainingTime = (slideshowInterval * 1000) - slideshow.elapsedBeforePause;

            slideshow.startTime = Date.now();
            startProgressLoop(); // Restart animation loop

            slideshow.slideshowIntervalId = setTimeout(() => {
                slideshow.slideshowCurrentIndex++;
                getSlideshowImage(() => {
                    slideshow.slideshowProceed = true;

                    resetProgressBar();

                    slideshow.elapsedBeforePause = 0;
                    slideshow.startTime = Date.now();
                    startProgressLoop();

                    slideshow.slideshowIntervalId = setInterval(() => {
                        if (!slideshow.slideshowIsPaused) {
                            slideshow.slideshowCurrentIndex++;
                            getSlideshowImage(() => {
                                slideshow.slideshowProceed = true;

                                resetProgressBar();

                                cancelProgressLoop();
                                slideshow.elapsedBeforePause = 0;
                                slideshow.startTime = Date.now();
                                startProgressLoop();
                            });
                        }
                    }, slideshowInterval * 1000);
                });
            }, remainingTime);

            if (hideButton === true) {
                $("#playPause").hide();
            } else {
                $("#playPause").show();
                $("#playPause").fadeOut(slideshow.const.playPauseHideTime);
            }
        }
    }

    // Use to keep screen awake
    function createVideo() {
        const video = document.createElement('video');
        const source = document.createElement('source');

        source.setAttribute("src", "/media/muted-blank.mp4");
        source.setAttribute("type", "video/mp4");

        video.appendChild(source);
        document.getElementById("dummyVideoContainer").appendChild(video);
        video.play();

        // Loop
        video.addEventListener("ended", function(){
            if (document.hidden === false) {
                video.currentTime = 0;
                video.play().then(_ => shashin.printMessageToConsole("Looping video", {tag: "slideshow"}));
            }
        });

        document.addEventListener("visibilitychange", () => {
            if (document.hidden === false) {
                // Play bg video
                video.currentTime = 0;
                video.play().then(_ => shashin.printMessageToConsole("Looping video", {tag: "slideshow"}));
            } else if (slideshow.slideshowIsPaused === false) {
                slideshowGalleryPlayPause();
            }
        });
    }

    function tearDownVideo() {
        $("#dummyVideoContainer").children().remove();
    }
}