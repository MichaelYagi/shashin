(function( timelineSettings, $, undefined ) {
    // left: 37, up: 38, right: 39, down: 40,
    // spacebar: 32, pageup: 33, pagedown: 34, end: 35, home: 36
    const keys = {37: 1, 38: 1, 39: 1, 40: 1};

    function preventDefault(e) {
        e.preventDefault();
    }

    function preventDefaultForScrollKeys(e) {
        if (keys[e.keyCode]) {
            preventDefault(e);
            return false;
        }
    }

    // modern Chrome requires { passive: false } when adding event
    let supportsPassive = false;
    try {
        window.addEventListener("test", null, Object.defineProperty({}, 'passive', {
            get: function () { supportsPassive = true; }
        }));
    } catch(e) {}

    const wheelOpt = supportsPassive ? { passive: false } : false;
    const wheelEvent = 'onwheel' in document.createElement('div') ? 'wheel' : 'mousewheel';

    // call this to Disable
    timelineSettings.disableScroll = function() {
        window.addEventListener('DOMMouseScroll', preventDefault, false); // older FF
        window.addEventListener(wheelEvent, preventDefault, wheelOpt); // modern desktop
        window.addEventListener('touchmove', preventDefault, wheelOpt); // mobile
        window.addEventListener('keydown', preventDefaultForScrollKeys, false);
    };

    // call this to Enable
    timelineSettings.enableScroll = function() {
        window.removeEventListener('DOMMouseScroll', preventDefault, false);
        window.removeEventListener(wheelEvent, preventDefault, wheelOpt);
        window.removeEventListener('touchmove', preventDefault, wheelOpt);
        window.removeEventListener('keydown', preventDefaultForScrollKeys, false);
    };

    const renderInitPage = function(mediaTypeFilter) {
        const firstElem = $('.scrollspy')[0];
        const elementsInViewport = Util.elementsInViewport($(".scrollspy"));

        timelineSettings.attachAssociatedMetadata(firstElem.id, mediaTypeFilter).then(() => {}).catch(error => {
            shashin.printMessageToConsole("Metadata attachment failed:" + error,{tag:"timeline"});
        });
        timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);
        timelineSettings.setScrollSpyActive($(firstElem));
        Util.reinitLightGalleryInstance();
    };

    let scrollStopRAF;
    const triggerScrollStop = () => {
        if (scrollStopRAF) cancelAnimationFrame(scrollStopRAF);
        scrollStopRAF = requestAnimationFrame(() => {
            $(window).trigger("scrollStop");
        });
    };
    const debounce = (func, delay) => {
        let timer;
        return function(...args) {
            clearTimeout(timer);
            timer = setTimeout(() => func.apply(this, args), delay);
        };
    };

    timelineSettings.init = function(mediaTypeFilter, metadataDates, metadataYearMonthCount, timelineDatesHash, locale) {
        timelineSettings.disableScroll();
        Util.showSpinner(true);
        $("#dLabel").addClass("disabled");
        $("#dLabel").attr("aria-disabled", "true");
        $("#dLabel").attr("tabindex", "-1");
        timelineSettings.timelineDates = metadataDates;
        timelineSettings.metadataYearMonthCount = metadataYearMonthCount;
        timelineSettings.timelineDatesHash = timelineDatesHash;
        timelineSettings.locale = locale;
        shashin.removeAllMetadataFilenamesList();
        shashin.removeAllMetadataThumbnailsList();
        shashin.removeAllMetadataIdList();

        if (timelineSettings.useDexie === true) {
            setTimeout(function () {
                const http = new Http("indexeddb test");
                http.ajax("get", "/timeline/all/dates").then(async function (data) {
                    if (data.hasOwnProperty("allMetadata") && data.hasOwnProperty("favorites") && data.hasOwnProperty("placeNameHeaders")) {
                        const metadataList = data.allMetadata;
                        timelineSettings.favoritesMap = data.favorites;
                        timelineSettings.placeNameHeaders = data.placeNameHeaders;

                        timelineSettings.db = new Dexie("MetadataDatabase");
                        timelineSettings.db.version(1).stores({
                            metadataList: `id, [year+month+day]`
                        });

                        timelineSettings.db.metadataList.bulkPut(metadataList).then(() => {
                            timelineSettings.dbOperationComplete = true;
                        });
                    }
                });
            }, 0);
        }

        Util.setMetadataLocalStorage();

        shashin.setLightGalleryElement('infinite-scroll-gallery');

        const lgConfig = {
            "selector":".mediaLink",
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig.metadataDetail = true;
            lgConfig.metadataDetailFun = shashin.openEditMetadataModal;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig.videoThumbnail = true;
            lgConfig.videoThumbnailFun = shashin.processVideoThumbnail;
        }
        shashin.setLightGallery(lgConfig);

        let topScroll = true;
        let topOfPage = true;
        let scrollTimer = null;
        let firsthovered = false;

        // Initialize
        timelineSettings.initializeTimelineSlider(mediaTypeFilter);

        let hash = "";
        if (window.location.hash) {
            hash = window.location.hash.substring(1);
        }

        // Jump to date
        if (hash.length > 0) {
            if ($("#offcanvas_"+hash).length > 0) {
                const jumpFromTimelineToc = timelineSettings.once(timelineSettings.jumpFromTimelineToc);
                jumpFromTimelineToc(null, hash, mediaTypeFilter);
            } else if ($('.scrollspy').length > 0) {
                history.pushState("", document.title, window.location.pathname + window.location.search);

                const dateArray = hash.split("-");
                shashin.showToastMessage(hash+ " not found", "Could not find date " +Util.getDateString(dateArray[0],dateArray[1],dateArray[2])+ " on timeline.", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });

                renderInitPage(mediaTypeFilter);
            }

            timelineSettings.enableScroll();
            Util.showSpinner(false);
        } else if ($('.scrollspy').length > 0) {
            renderInitPage(mediaTypeFilter);
        } else {
            timelineSettings.enableScrollSpy = false;
        }

        timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
        document.body.addEventListener('wheel', function (event) {
            timelineSettings.currentScrollDirection = event.deltaY > 0 ? timelineSettings.ScrollDirection.down : timelineSettings.ScrollDirection.up;
        });

        $(window).bind("scrollStop", function() {
            firsthovered = true;
            timelineSettings.isScrolling = false;

            if ($(".attachMetadataPhotos").last().text() !== "EOL" && $("#spinner_bottom").css("display") === "block") {
                timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;

                setTimeout(function () {
                    if ($(".attachMetadataPhotos").last().text() !== "EOL" && $("#spinner_bottom").css("display") === "block") {
                        timelineSettings.enableScrollSpy = true;
                        debounce(renderViewport, 300);
                        timelineSettings.enableScrollSpy = false;
                    }
                }, 1500);
            }

            debounce(renderViewport, 300);

            const containerRect = document.getElementById("container").getBoundingClientRect();
            const galleryRect = document.getElementById("infinite-scroll-gallery").getBoundingClientRect();
            const containerTop = containerRect.top;
            const galleryTop = galleryRect.top;

            // Prevent getting stuck scrolling up
            if (containerTop  === galleryTop ||
                containerTop  === (galleryTop-1) ||
                containerTop  === (galleryTop+1)
            ) {
                timelineSettings.scrollByN();

                timelineSettings.enableScrollSpy = true;
                debounce(renderViewport, 300);

                setTimeout(function () {
                    if (containerTop  === galleryTop ||
                        containerTop  === (galleryTop-1) ||
                        containerTop  === (galleryTop+1)
                    ) {
                        timelineSettings.enableScrollSpy = true;
                        debounce(renderViewport, 300);

                    }
                }, 1000);
            }

            if ($("#dateSliderWrapper:not(:hover)").length > 0) {
                let timeoutValue = 1000;
                if (timelineSettings.dbOperationComplete === true) {
                    timeoutValue = 0;
                }
                setTimeout(() => {
                    $("#dateSlider").fadeOut(timelineSettings.scrollBar.fadeOutTime).invisible();
                    timelineSettings.rescanElements();
                }, timeoutValue);
            }

            timelineSettings.rescanElements();

            let timeoutValue = 2500;

            setTimeout(() => {
                const elements = Util.elementsInViewport($('img.photo-thumbnail-image:not([src*="/api/v1/thumbnails/'+timelineSettings.thumbnailType+'/)'));
                if (elements.length > 0) {
                    timelineSettings.rescanElements(elements);
                }
            }, timeoutValue);

            // Clean up
            if (timelineSettings.didJumpFromTimelineToc === true) {
                let prevClass = "";
                let deleteElements = false;
                $('#infinite-scroll-gallery').children().each(function () {
                    const currClass = $(this).attr("class");
                    if (prevClass === currClass || deleteElements === true) {
                        $(this).css("display", "none");
                        deleteElements = true;
                        shashin.printMessageToConsole("Cleaning up IDs after jump:"+$(this).attr("id"),{tag:"timeline"});
                    }
                    prevClass = $(this).attr("class");
                });
            }

            $(".hoverDateMarker").remove();
            $(".hoverDateText").remove();

            timelineSettings.slidePercent = Math.abs(($("#dateSlider").slider("value")/(metadataDates.length-1)*100)-100);

            const index = Math.ceil(((metadataDates.length-1)/100)*timelineSettings.slidePercent); // Get date index based on slide percentage
            if (index > -1 && index < metadataDates.length) {
                const hoverDateObj = metadataDates[index];

                const year = hoverDateObj.year;
                const month = hoverDateObj.month;
                const day = hoverDateObj.day;

                if (timelineSettings.lastDateMarker !== null) {
                    const lastDateMarker = timelineSettings.lastDateMarker;
                    $("#lastDateMarker" + lastDateMarker.year + '-' + lastDateMarker.month + '-' + lastDateMarker.day).remove();
                    timelineSettings.lastDateMarker = null;
                }

                timelineSettings.lastDateMarker = {
                    year: parseInt(year),
                    month: parseInt(month),
                    day: parseInt(day)
                };

                const tickEl = $('<span id="lastDateMarker' + year + '-' + month + '-' + day + '" style="color: #ADD8E6; font-size: x-large; margin-top: -20px;">&#8213;</span>').css({
                    'width': '10px',
                    'right': '5px',
                    'position': 'absolute',
                    'z-index': '1',
                    'top': timelineSettings.slidePercent + '%'
                });

                $("#dateSlider").append(tickEl);
            }

            Util.reinitLightGalleryInstance();
        });

        function renderViewport() {
            if (timelineSettings.enableScrollSpy === true) {
                topScroll = false;
                const elementsInViewport = Util.elementsInViewport($(".scrollspy"));
                timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);
                timelineSettings.isScrolling = false;
            }
        }

        // Scroll event handler
        const $container = $("#container");
        const $gallery = $("#infinite-scroll-gallery");
        const $spinnerBottom = $("#spinner_bottom");
        const $attachMetadataPhotos = $(".attachMetadataPhotos");
        const $footer = $("footer");
        const $offcanvasToc = $("#offcanvasToc");

        let lastOffset = $container.scrollTop();
        let lastDate = new Date().getTime();
        let finalDate = metadataDates[metadataDates.length-1];

        const scrollHandler = function (e) {
            if (timelineSettings.lastDateMarker !== null) {
                const lastDateMarker = timelineSettings.lastDateMarker;
                $("#lastDateMarker" + lastDateMarker.year + '-' + lastDateMarker.month + '-' + lastDateMarker.day).remove();
                timelineSettings.lastDateMarker = null;
            }

            firsthovered = true;

            if (scrollTimer !== null) {
                clearTimeout(scrollTimer);
            }
            scrollTimer = setTimeout(triggerScrollStop, 200);

            timelineSettings.distanceToFooter = timelineSettings.calculateDistanceToFooter();
            timelineSettings.isScrolling = true;
            let st = $(e.target).scrollTop();

            if (st === 0) {
                topScroll = true;
            }

            if (timelineSettings.dbOperationComplete === true) {
                timelineSettings.rescanElements();
            } else {
                let delayInMs = e.timeStamp - lastDate;
                let offset = st - lastOffset;
                let speedInpxPerMs = offset / delayInMs;
                if (speedInpxPerMs < 0.20 && speedInpxPerMs > 0.15) {
                    timelineSettings.rescanElements();
                }
            }

            if (shashin.lastSelectedMetadataId !== "" && shashin.multiSelected === true && shashin.getMetadataIdList().length > 0) {
                $("#photoThumbnailContainer" + shashin.lastSelectedMetadataId).addClass("border border-3 border-primary");
                $("#image" + shashin.lastSelectedMetadataId).addClass("pb-1");
            }

            const elementsInViewPort = Util.elementsInViewport($(".scrollspy"));
            let showSlider = true;

            if (!Util.isMobile()) {
                if (!Util.isInViewport($footer) &&
                    timelineSettings.elementTracking.length > 0 &&
                    elementsInViewPort.length === timelineSettings.elementTracking.length &&
                    timelineSettings.elementTracking[0].isSameNode(elementsInViewPort[0]) &&
                    timelineSettings.elementTracking[timelineSettings.elementTracking.length - 1].isSameNode(elementsInViewPort[elementsInViewPort.length - 1])
                ) {
                    timelineSettings.isScrolling = false;
                }
                timelineSettings.elementTracking = elementsInViewPort;
            }

            lastDate = e.timeStamp;
            lastOffset = st;

            if ($attachMetadataPhotos.last().text() !== "EOL" && $spinnerBottom.css("display") === "block") {
                timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
            }

            if (timelineSettings.isScrolling === true || showSlider === true) {
                $("#dateSlider").fadeIn(timelineSettings.scrollBar.fadeInTime).visible();

                document.querySelectorAll('.dropdown-toggle').forEach(dropdownToggleEl => {
                    const dropDown = new bootstrap.Dropdown(dropdownToggleEl);
                    dropDown.hide();
                });
            }

            if (topScroll === true && topOfPage === false && !Util.isMobile()) {
                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                    timelineSettings.isScrolling = true;
                }
            }

            const firstDate = $("#offcanvasTocBody div a").first().attr("id").split("offcanvas_")[1];
            topOfPage = $(elementsInViewPort[0]).attr("id") === firstDate;

            if (scrollTimer !== null) clearTimeout(scrollTimer);
            scrollTimer = setTimeout(() => {
                requestAnimationFrame(() => {
                    $(window).trigger("scrollStop");
                });
            }, 200);

            if ($offcanvasToc.css('visibility') === "visible" && timelineSettings.enableScrollSpy === true) {
                timelineSettings.scrollToTimelineToc(elementsInViewPort);
            }

            if (timelineSettings.enableScrollSpy === true) {
                if (timelineSettings.didJumpFromTimelineToc === true) {
                    let prevClass = "";
                    let deleteElements = false;
                    $gallery.children().each(function () {
                        const currClass = $(this).attr("class");
                        if (prevClass === currClass || deleteElements === true) {
                            $(this).css("display", "none");
                            deleteElements = true;
                            shashin.printMessageToConsole("Cleaning up IDs after jump:" + $(this).attr("id"), {tag: "timeline"});
                        }
                        prevClass = currClass;
                    });
                    timelineSettings.didJumpFromTimelineToc = false;
                }

                topScroll = false;
                timelineSettings.renderThumbnailsInViewport(elementsInViewPort, mediaTypeFilter);
            }

            // Nudge if bottoms out and not the end
            setTimeout(function () {
                let checkViewPort = Util.elementsInViewport($(".scrollspy"));
                if (Util.isInViewport($footer) && $(checkViewPort[checkViewPort.length-1])[0].hasAttribute("id") && $(checkViewPort[checkViewPort.length-1]).attr("id").replace("tail_", "") !== (finalDate.year + "-" + finalDate.month + "-" + finalDate.day)) {
                    timelineSettings.scrollByN(-1);
                }
            }, 2000);
        };
        document.getElementById("container").addEventListener('scroll', scrollHandler, { passive: true });

        $("#offcanvasToc").on('show.bs.offcanvas', function () {
            if (timelineSettings.enableScrollSpy === true) {
                timelineSettings.scrollToTimelineToc(Util.elementsInViewport($(".scrollspy")));
            }
        });

        scrollTimer = setTimeout(function() {
            timelineSettings.scrollByN(2);
            timelineSettings.enableScroll();
            Util.showSpinner(false);
            $("#dLabel").removeClass("disabled");
            $("#dLabel").removeAttr("aria-disabled");
            $("#dLabel").removeAttr("tabindex");
        }, 1500);
    };
}( window.timelineSettings = window.timelineSettings || {}, jQuery ));