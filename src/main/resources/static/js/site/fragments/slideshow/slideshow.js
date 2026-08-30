(function (shashin, $, undefined) {
    'use strict';

    var MIN_INTERVAL = 10;
    var MAX_INTERVAL = 120;
    var SEGMENTS = 6;
    var SPACING = (MAX_INTERVAL - MIN_INTERVAL) / (SEGMENTS - 1);
    var ORIENTATION_MAP = {0: 'All', 1: 'Landscape', 2: 'Portrait'};

    var _lg = null;
    var _lgEl = null;
    var _fetchingNext = false;
    var _wakeLock = null;

    // State — set by initializeSlideshow() and mutated by controls
    var _accessTimeline = false;
    var _locale = 'en';
    var _albumImageCount = 0;
    var _interval = 10;
    var _orientation = 0;
    var _fillScreen = false;
    var _showProgress = false;

    // Items + index saved across an interval-change reinit
    var _reinitItems = null;
    var _reinitIndex = 0;

    var _fillStyleEl = null;
    function applyFillScreen(active) {
        if (!_fillStyleEl) {
            _fillStyleEl = document.createElement('style');
            document.head.appendChild(_fillStyleEl);
        }
        _fillStyleEl.textContent = active
            ? '.shoji-outer .shoji-slide img { object-fit: cover !important; width: 100% !important; height: 100% !important; max-width: none !important; max-height: none !important; }'
            : '';
    }

    var _captionHidden = false;
    function toggleCaption() {
        _captionHidden = !_captionHidden;
        if (_lgEl) _lgEl.classList.toggle('shashin-caption-hidden', _captionHidden);
    }

    // Inject caption-hide rule once
    (function () {
        var s = document.createElement('style');
        s.textContent = '.shashin-caption-hidden .shoji-caption { display: none !important; }';
        document.head.appendChild(s);
    }());

    function buildCaption(data) {
        var metadata = data.metadata;
        var takenDateStr = metadata.year + '-' + metadata.month + '-' + metadata.day;
        var takenDate = new Date(takenDateStr);
        var opts = {weekday: 'long', year: 'numeric', month: 'short', day: 'numeric'};
        var desc;
        if (_accessTimeline === false && data.albumIds && data.albumIds[0] !== undefined) {
            desc = "<a style='color:#DBE9F4;text-decoration:none;' href='/album/" + data.albumIds[0] + "' target='_blank'>" + takenDate.toLocaleDateString(_locale, opts) + "</a>";
        } else if (_accessTimeline === true) {
            desc = "<a style='color:#DBE9F4;text-decoration:none;' href='/timeline#" + takenDateStr + "' target='_blank'>" + takenDate.toLocaleDateString(_locale, opts) + "</a>";
        } else {
            desc = takenDate.toLocaleDateString(_locale, opts);
        }
        if (data.shortPlaceName) desc += ' • ' + data.shortPlaceName;
        if (metadata.description) desc += '<div>' + metadata.description + '</div>';
        return desc;
    }

    function buildItem(data) {
        var metadata = data.metadata;
        var baseUrl = data.baseUrl || (location.protocol + '//' + location.host);
        return {
            metadataId: metadata.id,
            id: metadata.id,
            src: baseUrl + '/api/v1/image/original/' + metadata.id,
            downloadUrl: '/api/v1/image/' + metadata.id + '/download',
            caption: {dangerouslySetInnerHTML: buildCaption(data)}
        };
    }

    function fetchRandom(callback) {
        var http = new Http("slideshow random");
        http.ajax("get", "/random/metadata/type/image?includeSlideAlbums=true&orientation=" + _orientation)
            .then(function (data) {
                if (data && data.metadata) callback(data);
            });
    }

    function acquireWakeLock() {
        if ('wakeLock' in navigator) {
            navigator.wakeLock.request('screen').then(function (lock) {
                _wakeLock = lock;
            }).catch(function () {});
        }
    }

    function releaseWakeLock() {
        if (_wakeLock) {
            _wakeLock.release().catch(function () {});
            _wakeLock = null;
        }
    }

    function changeSlideshowInterval(val) {
        var http = new Http("slideshow interval");
        http.ajax("post", "/users/slideshowinterval", JSON.stringify({slideshowInterval: val}));
    }

    function changeSlideshowOrientation(val) {
        var http = new Http("orientation");
        http.ajax("post", "/users/slideshoworientation", JSON.stringify({slideshowOrientation: val}));
    }

    function changeShowProgress(val) {
        var http = new Http("show progress");
        http.ajax("post", "/users/slideshowprogress", JSON.stringify({slideshowProgress: val}));
    }

    function changeFillScreen(val) {
        var http = new Http("fill screen");
        http.ajax("post", "/users/slideshowfillscreen", JSON.stringify({slideshowFillScreen: val}));
    }

    function showInstruction() {
        var options = {};
        if (!Util.isMobile()) {
            options = {icon: "bi-keyboard", autohide: false, tag: "slide"};
        } else {
            options = {icon: "bi-hand-index", autohide: false, tag: "slide"};
        }

        var title, message;
        if (!Util.isMobile()) {
            message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>i</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.window") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Esc</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.exit") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>" + shashin.getTranslatedValue("main.pages.slideshow.space") + "</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.playpause") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>d</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.slideinfo") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>← →</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.nextprev") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>a</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.albumfilter") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>- =</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.idinterval") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>[ ]</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.toggleorientation") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>f</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.togglefs") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showFill'" + (_fillScreen ? ' checked' : '') + "></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.fill") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showProgress'" + (_showProgress ? ' checked' : '') + "></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.showprogress") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='3' id='slideshowOrientationSlide'></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.orientation") + " - <span id='orientationValue'>" + ORIENTATION_MAP[_orientation] + "</span></div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='" + SEGMENTS + "' id='slideshowIntervalSlide'></div><div class='col-8'><span id='intervalValue'>" + _interval + "</span>s " + shashin.getTranslatedValue("main.pages.slideshow.info.interval") + "</div></div>";
            if ((_albumImageCount > 1 && !_accessTimeline) || (_albumImageCount > 0 && _accessTimeline)) {
                message += "<div class='row mb-1'><button class='btn btn-secondary' type='button' id='slideshowAlbumNameData'>" + shashin.getTranslatedValue("main.pages.slideshow.info.albumfilter") + "</button></div>";
            }
            message += "</div>";
            title = shashin.getTranslatedValue("main.pages.slideshow.info.title.keyboard");
        } else {
            message = "<div class='container'>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Swipe Up</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.window") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Swipe Down</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.slideinfo") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><span class='badge bg-secondary'><strong>Swipe ← →</strong></span></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.nextprev") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showFill'" + (_fillScreen ? ' checked' : '') + "></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.fill") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 d-flex justify-content-center form-check form-switch'><input class='form-check-input' type='checkbox' role='switch' id='showProgress'" + (_showProgress ? ' checked' : '') + "></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.info.showprogress") + "</div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='3' id='slideshowOrientationSlide'></div><div class='col-8'>" + shashin.getTranslatedValue("main.pages.slideshow.orientation") + " - <span id='orientationValue'>" + ORIENTATION_MAP[_orientation] + "</span></div></div>" +
                "<div class='row mb-1'><div class='col-4 text-center'><input type='range' class='form-range' min='1' max='" + SEGMENTS + "' id='slideshowIntervalSlide'></div><div class='col-8'><span id='intervalValue'>" + _interval + "</span>s " + shashin.getTranslatedValue("main.pages.slideshow.info.interval") + "</div></div>";
            if ((_albumImageCount > 1 && !_accessTimeline) || (_albumImageCount > 0 && _accessTimeline)) {
                message += "<div class='row mb-1'><button class='btn btn-secondary' type='button' id='slideshowAlbumNameData'>" + shashin.getTranslatedValue("main.pages.slideshow.info.albumfilter") + "</button></div>";
            }
            message += "</div>";
            title = shashin.getTranslatedValue("main.pages.slideshow.info.title.touch");
        }

        shashin.showToastMessage(title, message, options);

        $("#slideshowIntervalSlide").val(Math.floor(((_interval - MIN_INTERVAL) / SPACING) + 1));
        $("#slideshowOrientationSlide").val(_orientation + 1);

        $("#slideshowIntervalSlide").on("input", function () {
            _interval = Math.floor(MIN_INTERVAL + ($(this).val() - 1) * SPACING);
            $("#intervalValue").text(_interval);
            changeSlideshowInterval(_interval);
            if (_lg) {
                _reinitItems = _lg.items.slice();
                _reinitIndex = _lg.currentIndex;
                _lg.close();
            }
        });

        $("#slideshowOrientationSlide").on("input", function () {
            _orientation = parseInt($(this).val()) - 1;
            $("#orientationValue").text(ORIENTATION_MAP[_orientation]);
            changeSlideshowOrientation(_orientation);
        });

        $("#showProgress").on("input", function () {
            _showProgress = $("#showProgress").prop('checked');
            changeShowProgress(_showProgress);
        });

        $("#showFill").on("input", function () {
            _fillScreen = $("#showFill").prop('checked');
            applyFillScreen(_fillScreen);
            changeFillScreen(_fillScreen);
        });

        $("#slideshowAlbumNameData").on("click", function (e) {
            e.preventDefault();
            $("#slideshowAlbumSelection").modal('show');
        });
    }

    function makeSlideshowPlugin() {
        return {
            name: 'slideshowControls',
            init: function (ctx) {
                var gallery = ctx.gallery;
                var cleanups = [];

                cleanups.push(ctx.on('autoplayStart', acquireWakeLock));
                cleanups.push(ctx.on('autoplayStop', releaseWakeLock));

                cleanups.push(ctx.on('afterSlide', function (detail) {
                    var toIdx = detail ? detail.to : gallery.currentIndex;
                    if (!_fetchingNext && toIdx >= gallery.items.length - 2) {
                        _fetchingNext = true;
                        fetchRandom(function (data) {
                            _fetchingNext = false;
                            gallery.addSlides([buildItem(data)]);
                        });
                    }
                }));

                cleanups.push(ctx.ui.registerShortcut('d', function () {
                    toggleCaption();
                }));

                cleanups.push(ctx.ui.registerShortcut('i', function () {
                    if (shashin.hasToast(shashin.toast.placement.bottom.center, {tag: "slide"}) === false) {
                        showInstruction();
                    } else {
                        shashin.closeToastMessages({tag: "slide"});
                    }
                }));

                cleanups.push(ctx.ui.registerShortcut('f', function () {
                    _fillScreen = !_fillScreen;
                    applyFillScreen(_fillScreen);
                    changeFillScreen(_fillScreen);
                }));

                cleanups.push(ctx.ui.registerShortcut('[', function () {
                    if (_orientation > 0) {
                        _orientation--;
                        changeSlideshowOrientation(_orientation);
                        shashin.showToastMessage(
                            shashin.getTranslatedValue("main.pages.slideshow.orientation"),
                            shashin.getTranslatedValue("main.toast.slideshow.orientation.body", ORIENTATION_MAP[_orientation].toLowerCase()),
                            {icon: "bi-info-circle", autohide: true, placement: shashin.toast.placement.top.center, tag: "slide"}
                        );
                    }
                }));

                cleanups.push(ctx.ui.registerShortcut(']', function () {
                    if (_orientation < 2) {
                        _orientation++;
                        changeSlideshowOrientation(_orientation);
                        shashin.showToastMessage(
                            shashin.getTranslatedValue("main.pages.slideshow.orientation"),
                            shashin.getTranslatedValue("main.toast.slideshow.orientation.body", ORIENTATION_MAP[_orientation].toLowerCase()),
                            {icon: "bi-info-circle", autohide: true, placement: shashin.toast.placement.top.center, tag: "slide"}
                        );
                    }
                }));

                cleanups.push(ctx.ui.registerShortcut('-', function () {
                    if ((_interval - SPACING) >= MIN_INTERVAL) {
                        _interval = Math.round(_interval - SPACING);
                        changeSlideshowInterval(_interval);
                        shashin.showToastMessage(
                            shashin.getTranslatedValue("main.toast.slideshow.decrease.title"),
                            shashin.getTranslatedValue("main.toast.slideshow.decrease.body", _interval),
                            {icon: "bi-info-circle", autohide: true, placement: shashin.toast.placement.top.center, tag: "slide"}
                        );
                        _reinitItems = gallery.items.slice();
                        _reinitIndex = gallery.currentIndex;
                        _lg.close();
                    }
                }));

                cleanups.push(ctx.ui.registerShortcut('=', function () {
                    if ((_interval + SPACING) <= MAX_INTERVAL) {
                        _interval = Math.round(_interval + SPACING);
                        changeSlideshowInterval(_interval);
                        shashin.showToastMessage(
                            shashin.getTranslatedValue("main.toast.slideshow.increase"),
                            shashin.getTranslatedValue("main.toast.slideshow.increase.body", _interval),
                            {icon: "bi-info-circle", autohide: true, placement: shashin.toast.placement.top.center, tag: "slide"}
                        );
                        _reinitItems = gallery.items.slice();
                        _reinitIndex = gallery.currentIndex;
                        _lg.close();
                    }
                }));

                cleanups.push(ctx.ui.registerShortcut('a', function () {
                    $('#slideshowAlbumSelection').modal('show');
                }));

                return function () {
                    cleanups.forEach(function (fn) { if (typeof fn === 'function') fn(); });
                    releaseWakeLock();
                };
            }
        };
    }

    function buildAndOpen(items, startIndex) {
        if (!_lgEl) {
            _lgEl = document.createElement('div');
            _lgEl.id = 'slideshowGalleryContainer';
            document.body.appendChild(_lgEl);
        }

        applyFillScreen(_fillScreen);

        var downloadPlugin = (typeof lgDownload !== 'undefined') ? [lgDownload] : [];
        var castPlugin = (typeof lgCastMedia !== 'undefined') ? [lgCastMedia] : [];

        var configs = shashin.getLightGalleryConfigs({
            overrideBaseConfigs: true,
            plugins: [Shoji.Autoplay, Shoji.Fullscreen].concat(downloadPlugin).concat(castPlugin).concat([makeSlideshowPlugin()]),
            items: items,
            counter: false,
            mode: 'fade',
            download: (typeof lgDownload !== 'undefined'),
            castMedia: (typeof lgCastMedia !== 'undefined'),
            autoplay: {
                autoStart: true,
                interval: _interval * 1000,
                showProgress: _showProgress,
                stopOnManualNavigate: false
            },
            preload: 2
        });

        if (_lg) {
            try { _lg.destroy(); } catch (e) {}
            _lg = null;
        }

        _lg = new Shoji(_lgEl, configs);

        _lg.on('afterOpen', function () {
            if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
                document.documentElement.requestFullscreen().catch(function () {});
            }
            // Preload the next slide immediately so autoplay has something to advance to
            if (!_fetchingNext && _lg.items.length < 2) {
                _fetchingNext = true;
                fetchRandom(function (data) {
                    _fetchingNext = false;
                    if (_lg) _lg.addSlides([buildItem(data)]);
                });
            }
        });

        _lg.on('afterClose', function () {
            applyFillScreen(false);
            releaseWakeLock();
            _captionHidden = false;
            if (_lgEl) _lgEl.classList.remove('shashin-caption-hidden');
            shashin.closeToastMessages({tag: "slide"});
            if (document.fullscreenElement && document.exitFullscreen) {
                document.exitFullscreen().catch(function () {});
            }
            document.cookie = "activePage=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";

            if (_reinitItems) {
                var items = _reinitItems;
                var idx = _reinitIndex;
                _reinitItems = null;
                _reinitIndex = 0;
                setTimeout(function () { buildAndOpen(items, idx); }, 50);
            }
        });

        _lg.open(startIndex || 0);
    }

    function initializeSlideshow(accessTimelineView, queryLimit, slideshowInterval, albumImageCount, locale, slideshowProgress, orientation, fillScreen) {
        _accessTimeline = accessTimelineView;
        _interval = slideshowInterval;
        _orientation = orientation;
        _fillScreen = fillScreen;
        _showProgress = slideshowProgress;
        _locale = locale;
        _albumImageCount = albumImageCount;

        var cachedSlideshowAlbums = null;

        // Load album selection list
        var http = new Http("getAlbums");
        http.ajax("get", "/slideshowalbums").then(function (data) {
            if (data.hasOwnProperty("albumsList") && data.hasOwnProperty("slideshowAlbum") && data.slideshowAlbum.hasOwnProperty("albums") && data.slideshowAlbum.albums.length > 0) {
                var albumsArray = data.albumsList;
                var slideshowAlbumArray = data.slideshowAlbum.albums;
                cachedSlideshowAlbums = slideshowAlbumArray;

                var html = "";
                if (slideshowAlbumArray.length > 0) {
                    html += '<button class="dropdown-item" type="button">' +
                        '<input type="checkbox" data-album-id=0 class="slideshowAlbum" value="all" name="album[]" id="album-0"' + (slideshowAlbumArray.includes("all") ? ' checked="checked"' : '') + '> ' +
                        '<label for="album-0">' + shashin.getTranslatedValue("main.pages.slideshow.all") + '</label>' +
                        '</button>';
                }

                var hrFlag = false;
                for (var index in albumsArray) {
                    if (albumsArray.hasOwnProperty(index)) {
                        var album = albumsArray[index];
                        if (album.hasOwnProperty("albumPhotoCount") && album.albumPhotoCount > 0) {
                            if (!hrFlag) { html += "<hr>"; hrFlag = true; }
                            html += '<button class="dropdown-item" type="button">' +
                                '<input type="checkbox" data-album-id=' + album.id + ' class="slideshowAlbum" value="' + album.name + '" name="album[]" id="album-' + album.id + '"' + (slideshowAlbumArray.includes(String(album.id)) ? ' checked="checked"' : '') + '> ' +
                                '<label for="album-' + album.id + '">' + album.name + '</label>' +
                                '</button>';
                        }
                    }
                }
                $("#slideshowAlbumSelectionList").html(html);

                $(".slideshowAlbum").on("click", function () {
                    var albumId = parseInt($(this).attr("data-album-id"));
                    if (albumId === 0) {
                        if ($('#album-0').prop("checked") === true) {
                            $(".slideshowAlbum").prop('checked', false);
                            $('#album-0').prop('checked', true);
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
                    var postHttp = new Http("getAlbums");
                    var checkedValues = [];
                    $('.slideshowAlbum:checked').each(function () {
                        checkedValues.push($(this).attr("data-album-id"));
                    });
                    postHttp.ajax("post", "/slideshowalbums", JSON.stringify({albums: checkedValues})).then(function (data) {
                        if (data.status === "success") {
                            cachedSlideshowAlbums = checkedValues;
                        } else {
                            shashin.showToastMessage(
                                shashin.getTranslatedValue("main.toast.media.upload.errors"),
                                shashin.getTranslatedValue("main.toast.media.random.image.errors"),
                                {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor: "danger", autohide: true}
                            );
                        }
                    });
                });
            }
        });

        $('#slideshowAlbumSelection').on('hidden.bs.modal', function () {
            if (cachedSlideshowAlbums !== null && cachedSlideshowAlbums.length > 0) {
                $(".slideshowAlbum").prop('checked', false);
                for (var index in cachedSlideshowAlbums) {
                    if (cachedSlideshowAlbums.hasOwnProperty(index)) {
                        var slideshowId = (cachedSlideshowAlbums[index] === "all" ? 0 : parseInt(cachedSlideshowAlbums[index]));
                        $('#album-' + slideshowId).prop('checked', true);
                    }
                }
            }
        });

        // Open slideshow
        $("#viewSlideshow").on("click", function (e) {
            e.preventDefault();

            var date = new Date();
            date.setTime(date.getTime() + (30 * 24 * 60 * 60 * 1000));
            document.cookie = "activePage=slideshow; expires=" + date.toUTCString() + "; path=/";

            fetchRandom(function (data) {
                buildAndOpen([buildItem(data)], 0);
            });
        });
    }

    window.initializeSlideshow = initializeSlideshow;

}(window.shashin = window.shashin || {}, jQuery));
