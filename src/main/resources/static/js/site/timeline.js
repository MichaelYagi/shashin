(function( timelineSettings, $, undefined ) {
    timelineSettings.ScrollDirection = Object.freeze({"up":1, "down":0})
    timelineSettings.enableScrollSpy = true;
    timelineSettings.isScrolling = false;
    timelineSettings.prevAnchor = "";
    timelineSettings.successBelowMsg = "success_below";
    timelineSettings.successAboveMsg = "success_above";
    timelineSettings.successMidMsg = "success_mid";
    timelineSettings.success = "success";
    timelineSettings.currentScrollTop = 0;
    timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
    timelineSettings.initialized = false;
    timelineSettings.rendered = false;
    timelineSettings.timelineDates = [];
    timelineSettings.distanceToFooter = 9999;

    const calculateDistanceToFooter = function() {
        return $(window).height() - $('#subfooter').offset().top;
    }

    const closeToFooter = function() {
        return (timelineSettings.distanceToFooter === 9999 || (timelineSettings.distanceToFooter > -100 && timelineSettings.distanceToFooter < 1) || Util.elementsInViewport($("#subfooter")).length > 0);
    }

    const scrollByOne = function() {
        document.getElementById("container").scrollBy({top: 1});
        if (document.getElementsByTagName("MAIN").length > 0) {
            document.getElementsByTagName("MAIN")[0].scrollBy({top: 1});
        }
    }

    timelineSettings.init = function(mediaTypeFilter, metadataDates) {
        timelineSettings.timelineDates = metadataDates;

        Util.setMetadataLocalStorage();

        if (Util.isMobile() === false) {
            $("#infinite-scroll-gallery").attr('style', 'width: 97%');
        }

        shashin.setLightGalleryElement('infinite-scroll-gallery');
        shashin.setLightGallery({"selector":".mediaLink",plugins:[lgMetadataDetail],metadataDetail:true,metadataDetailFunc:shashin.openInfoSidebar});

        let topScroll = true;
        let topOfPage = true;
        let scrollTimer, sliderTimer;

        // Initialize
        if (Util.isMobile() === false) {
            timelineSettings.initializeTimelineSlider(mediaTypeFilter);
        } else {
            $("#timelineTocToggle").show();
            $("#dateSliderContainer").hide();
        }

        if ($('.scrollspy').length > 0) {
            const firstElem = $('.scrollspy')[0];
            const elementsInViewport = Util.elementsInViewport($(".scrollspy"));

            timelineSettings.attachAssociatedMetadata(firstElem.id, mediaTypeFilter);
            timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);
            timelineSettings.setScrollSpyActive($(firstElem));
            timelineSettings.reinitLightGalleryInstance();
        } else {
            timelineSettings.enableScrollSpy = false;
        }

        $('[data-bs-toggle="tooltip"]').tooltip();

        $(window).bind("scrollStop", function() {
            timelineSettings.isScrolling = false;

            // Only show overlays when scrolling stopped for current hovered image
            let hovered = false;
            $(".photo-thumbnail-image").mousemove(function () {
                if (hovered === false) {
                    const attrId = $(this).attr("id");
                    const metadataId = attrId.substring(5, attrId.length);
                    shashin.imageHover(this, metadataId);
                }
                hovered = true;
            });
        });

        $(window).bind("sliderScrollStop", function() {
            if ($("#dateSliderWrapper:not(:hover)").length === 1) {
                $("#dateSlider").hide();
            }
        });

        // Scroll event handler
        const scrollHandler = function (e) {
            timelineSettings.distanceToFooter = calculateDistanceToFooter();
            timelineSettings.isScrolling = true;
            let st = $(e.target).scrollTop();

            if (st === 0) {
                topScroll = true;
            }

            $("#dateSlider").show();

            // Hack to prevent infinite scroll upwards and throttle scrolling
            if (topScroll === true && topOfPage === false) {
                scrollByOne();
            }

            const firstDate = $("#offcanvasTocBody div a").first().attr("id").split("offcanvas_")[1];
            const elementsInViewport = Util.elementsInViewport($(".scrollspy"));
            topOfPage = $(elementsInViewport[0]).attr("id") === firstDate;

            // Scroll to the timeline TOC
            if (typeof $("#offcanvasToc").css('visibility') !== 'undefined' && $("#offcanvasToc").css('visibility') === "visible" && timelineSettings.enableScrollSpy === true) {
                timelineSettings.scrollToTimelineToc(elementsInViewport);
            }

            if (Util.isMobile() === false) {
                clearTimeout(sliderTimer);
                sliderTimer = setTimeout(function () {
                    $(window).trigger("sliderScrollStop");
                }, 1000);
            }

            if (timelineSettings.enableScrollSpy === true) {
                topScroll = false;
                timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);

                clearTimeout(scrollTimer);
                scrollTimer = setTimeout(function() {
                    $(window).trigger("scrollStop");
                }, 1000);
            }
        };
        $("#container").on('scroll', scrollHandler);

        $("#offcanvasToc").on('show.bs.offcanvas', function () {
            if (timelineSettings.enableScrollSpy === true) {
                timelineSettings.scrollToTimelineToc(Util.elementsInViewport($(".scrollspy")));
            }
        });

        shashin.mouseMoveListener();

        // Jump to date
        if (window.location.hash) {
            //Puts hash in variable, and removes the # character
            const hash = window.location.hash.substring(1);
            history.pushState("", document.title, window.location.pathname + window.location.search);

            if ($("#offcanvas_"+hash).length > 0) {

                // Remove hash
                timelineSettings.jumpFromTimelineToc(null, hash, mediaTypeFilter);
            }
        }

        clearTimeout(scrollTimer);
        scrollTimer = setTimeout(function() {
            //$(".photo-thumbnail-image").mousemove();

            // Only show overlays when scrolling stopped for current hovered image
            let hovered = false;
            $(".photo-thumbnail-image").mousemove(function () {
                if (hovered === false && timelineSettings.rendered === true && timelineSettings.enableScrollSpy === true) {
                    scrollByOne();
                    hovered = true;
                }
            });
        }, 1500);
    }

    timelineSettings.jumpToLightGalleryMetadata = function (metadataId) {
        const url = location.href;
        location.href = '#lightGalleryIndex'+metadataId;
        history.replaceState(null,null,url);
    }

    timelineSettings.reinitLightGalleryInstance = function () {
        if (shashin.getLightGallery() !== null) {
            const closeTimeout = shashin.getLightGallery().closeGallery(true);
            setTimeout(() => {
                if (shashin.getLightGallery() !== null) {
                    shashin.getLightGallery().destroyModules(true);
                    shashin.getLightGallery().invalidateItems();
                    $(window).off(`.lg.global${shashin.getLightGallery().lgId}`);
                    shashin.getLightGallery().LGel.off('.lg');
                    // https://github.com/sachinchoolur/lightGallery/blob/383d51852657ab44bb8697748c570cf110723f97/src/lightgallery.ts#L2396
                    // Hack because lg.destroy() errors out
                    // when photos appear slower than destroy called, then there's an error
                    try {
                        shashin.getLightGallery().$container.remove();
                    } catch (e) {
                        shashin.printMessageToConsole(e)
                    }

                    shashin.lg = null;

                    setTimeout(() => {
                        shashin.setLightGallery({"selector":".mediaLink",plugins:[lgMetadataDetail],metadataDetail:true,metadataDetailFunc:shashin.openInfoSidebar});
                        $(".bi-play-circle").css("visibility", "visible");
                        $(".bi-play-btn").css("visibility", "visible");
                        $(".mediaLink").bind('click');
                    }, 500);
                }
            }, closeTimeout);
        }
    }

    let reinitGalleryFlag = true;

    let prevElements = null;
    timelineSettings.renderThumbnailsInViewport = function (elements,mediaTypeFilter) {
        const timelineDates = timelineSettings.timelineDates;
        const lastDate = timelineDates[timelineDates.length-1].year + "-" + timelineDates[timelineDates.length-1].month + "-" + timelineDates[timelineDates.length-1].day;

        if (prevElements === null || (elements.length > 0 && Util.arraysEqual(elements, prevElements) === false) || (Util.elementsInViewport($("#"+lastDate)).length === 0 && closeToFooter() === true && Util.atEndOfPage($("#container")[0]))) {
            $(".bi-play-circle").css("visibility", "hidden");
            $(".bi-play-btn").css("visibility", "hidden");
            $(".mediaLink").unbind('click');
            reinitGalleryFlag = false;

            if (elements.length === 0) {
                const thumbnailsInViewport = Util.elementsInViewport($(".photo-thumbnail-container"));
                elements = $(thumbnailsInViewport.parent().prevAll(".scrollspy")[0])
            }

            const prevFirstElement = prevElements !== null ? $(prevElements[0]).attr('id') : prevElements;
            const firstElement = $(elements[0]).attr('id');
            const prevLastElement = prevElements !== null ? $(prevElements[prevElements.length-1]).attr('id') : prevElements;
            const lastElement = $(elements[elements.length-1]).attr('id');

            const prevFirstWithoutTail = (prevElements !== null && prevFirstElement.indexOf("tail_") > -1) ? prevFirstElement.split("tail_")[1] : prevFirstElement;
            const firstWithoutTail = firstElement.indexOf("tail_") > -1 ? firstElement.split("tail_")[1] : firstElement;
            const prevLastWithoutTail = (prevElements !== null && prevLastElement.indexOf("tail_") > -1) ? prevLastElement.split("tail_")[1] : prevLastElement;
            const lastWithoutTail = lastElement.indexOf("tail_") > -1 ? lastElement.split("tail_")[1] : lastElement;

            if (prevElements !== null) {
                if ((Util.getDateObject(prevFirstWithoutTail) > Util.getDateObject(firstWithoutTail)) ||
                    (Util.getDateObject(prevLastWithoutTail) > Util.getDateObject(lastWithoutTail))
                ) {
                    timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
                } else if ((Util.getDateObject(prevFirstWithoutTail) < Util.getDateObject(firstWithoutTail)) ||
                    (Util.getDateObject(prevLastWithoutTail) < Util.getDateObject(lastWithoutTail))
                ) {
                    timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.up;
                }
            }

            elements.each(function (index) {
                let id = $(this).attr("id");

                if (id.indexOf("tail_") === -1 && index < 2 && timelineSettings.prevAnchor !== id) {
                    if (Util.isSafari() === true || Util.isFirefox() === true) {
                        timelineSettings.prevAnchor = id;
                    }

                    // Set the timeline slider while scrolling
                    if (Util.isMobile() === false) {
                        timelineDates.forEach(function (timelineDate, i) {
                            if (id === timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day) {
                                $("#dateSlider").slider("option", "value", timelineDates.length - i - 1);
                                return false;
                            }
                        });
                    }
                }
            });

            timelineSettings.renderThumbnails(elements, mediaTypeFilter, timelineDates).then(function (msg) {
                if (msg === timelineSettings.success) {
                    // Set TOC active element
                    const elementsInViewport = Util.elementsInViewport($(".scrollspy"));
                    elementsInViewport.each(function (index) {
                        let id = $(this).attr("id");
                        if (id.indexOf("tail_") === -1 && timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
                            timelineSettings.setScrollSpyActive(id);
                            return false;
                        } else if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                            if (id.indexOf("tail_") > -1) {
                                id = id.split("tail_")[1];
                            }
                            timelineSettings.setScrollSpyActive(id);
                            return false;
                        }
                    });
                }
            });

            $("img").hover(function () {
                if (reinitGalleryFlag === false && timelineSettings.enableScrollSpy === true) {
                    reinitGalleryFlag = true;
                    timelineSettings.reinitLightGalleryInstance();
                }
            });


            prevElements = elements;
        }
    }

    timelineSettings.renderThumbnails = async function(elements,mediaTypeFilter,timelineDates) {
        timelineSettings.enableScrollSpy = false;

        if ($(".attachMetadataPhotos").last().text() !== "EOL") {
            $("#spinner_bottom").css("display", "block");
        }

        let firstElementId = $(elements[0]).attr("id");
        let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
        let ignoreTimelineDate = firstVisibleId;

        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
            let firstVisibleArr = firstVisibleId.split("-");
            let startIndex = 0;
            let timelineObjArr = timelineDates.slice().reverse();
            timelineObjArr.map(function(item, i) {
                if (item.year === parseInt(firstVisibleArr[0]) && item.month === parseInt(firstVisibleArr[1]) && item.day === parseInt(firstVisibleArr[2])) {
                    startIndex = i;
                }
            })
            for (let index = startIndex; index < timelineObjArr.length; index ++) {
                const timelineDateObj = timelineObjArr[index];
                ignoreTimelineDate = timelineDateObj.year + "-" + timelineDateObj.month + "-" + timelineDateObj.day;

                if (Util.getDateObject(firstVisibleId) < Util.getDateObject(ignoreTimelineDate)) {
                    break;
                }
            }
        }

        // Remove elements not visible in viewport
        let topHeight = 0;
        const content = $('#container');
        let tempScrollTop = content.scrollTop();
        const section = $('section');
        let count = 0;
        section.each(function (index, element) {
            if (Util.isInViewport($("#" + element.id)) === false &&
                Util.isInViewport($("#br" + element.id)) === false &&
                Util.isInViewport($("#row" + element.id)) === false &&
                Util.isInViewport($("#amp_" + element.id)) === false &&
                Util.isInViewport($("#tail_" + element.id)) === false &&
                Util.isInViewport($("#container_" + element.id)) === false &&
                ((Util.isSafari() === false && Util.isFirefox() === false) || ((Util.isSafari() === true || Util.isFirefox() === true) && count < 1)) &&
                Util.isInViewport($(".photo-thumbnail-image.thumbnailTag_" + element.id)) === false &&
                ((timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down && element.id !== $(section[section.length-1]).attr("id")) ||
                (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up && element.id !== ignoreTimelineDate))
            ) {
                section.hide();
                count++;
                topHeight += Util.getDateGalleryHeight(element.id);
                Util.removeDateGallery(element.id);
            }
        });

        if ((Util.isSafari() === true || Util.isFirefox() === true) && timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
            content.scrollTop(tempScrollTop - topHeight);
            section.hide();
        }

        console.log("================")

        // Get list of visible elements
        const firstVisibleContainer = $('section').length > 0 ? $('section')[0] : null;
        const lastVisibleContainer = $('section').length > 0 ? $('section')[$('section').length-1] : null;

        if (firstVisibleContainer !== null) {

            // Render above visibleContainers going from bottom up
            let currentDate = $(firstVisibleContainer).attr("id");
            let prevDate = "";
            const firstDate = timelineDates[0].year + "-" + timelineDates[0].month + "-" + timelineDates[0].day;
            const lastDate = timelineDates[timelineDates.length-1].year + "-" + timelineDates[timelineDates.length-1].month + "-" + timelineDates[timelineDates.length-1].day;

            let firstVisibleDateArr = $(firstVisibleContainer).attr("id").split("-");
            let lastVisibleDateArr = $(lastVisibleContainer).attr("id").split("-");

            let startingIndexTop = 0;
            let startingIndexBottom = 0;

            let timelineDateArr = timelineDates;
            const halfwayPoint = Math.floor(timelineDates.length/2);
            let reversed = false;
            if (Util.getDateObject(currentDate) < Util.getDateObject(timelineDates[halfwayPoint].year+"-"+timelineDates[halfwayPoint].month+"-"+timelineDates[halfwayPoint].day)) {
                timelineDateArr = timelineDates.slice().reverse();
                reversed = true;
            }

            for (let i = 0; i < timelineDateArr.length; i ++) {
                if (timelineDateArr[i].year === parseInt(firstVisibleDateArr[0]) && timelineDateArr[i].month === parseInt(firstVisibleDateArr[1]) && timelineDateArr[i].day === parseInt(firstVisibleDateArr[2])) {
                    startingIndexTop = i;
                    if (reversed === true) {
                        break;
                    }
                }

                if (timelineDates[i].year === parseInt(lastVisibleDateArr[0]) && timelineDates[i].month === parseInt(lastVisibleDateArr[1]) && timelineDates[i].day === parseInt(lastVisibleDateArr[2])) {
                    startingIndexBottom = i;
                    if (reversed === false) {
                        break;
                    }
                }
            }

            let timelineArr = timelineDates.reverse();
            for (let index = startingIndexTop; index < timelineArr.length; index++) {
                const timelineDate = timelineArr[index];
                prevDate = timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day;

                if (Util.getDateObject(currentDate) < Util.getDateObject(prevDate)) {
                    if ($("#" + currentDate).length === 0) {
                        // Render currentDate
                        const anchorPoint = timelineDates[index - 2].year + "-" + timelineDates[index - 2].month + "-" + timelineDates[index - 2].day;
                        const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, "above", anchorPoint);
                        if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                        }

                        // Break if top not in viewport
                        if (Util.elementsInViewport($("#" + currentDate)).length === 0) {
                            break;
                        }
                    }

                    if (prevDate !== firstDate) {
                        currentDate = prevDate;
                    } else {
                        const msg = await timelineSettings.updateTimeline(firstDate, mediaTypeFilter, "above", currentDate);
                        if (msg === timelineSettings.success && $("#" + firstDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(firstDate, mediaTypeFilter);
                        }
                    }
                }
            }

            // Render below visibleContainers going from top down
            currentDate = $(lastVisibleContainer).attr("id");
            timelineArr = timelineDates.reverse();

            for (let index = startingIndexBottom; index < timelineArr.length; index ++) {
                const timelineDate = timelineArr[index];
                let prevDate = timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day;

                if (Util.getDateObject(prevDate) < Util.getDateObject(currentDate) && closeToFooter() === true) {
                    if ($("#" + currentDate).length === 0) {
                        // Render currentDate
                        const anchorPoint = timelineDates[index - 2].year + "-" + timelineDates[index - 2].month + "-" + timelineDates[index - 2].day;
                        const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, "below", anchorPoint);
                        if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                            timelineSettings.distanceToFooter = calculateDistanceToFooter();
                        }

                        // Break if footer not in viewport
                        if (closeToFooter() === false) {
                            break;
                        }
                    }

                    if (prevDate !== lastDate) {
                        currentDate = prevDate;
                    } else {
                        const msg = await timelineSettings.updateTimeline(lastDate, mediaTypeFilter, "below", currentDate);
                        if (msg === timelineSettings.success && $("#" + lastDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(lastDate, mediaTypeFilter);
                        }
                    }
                }
            }
        }

        $("#spinner_top").css("display", "none");
        $("#spinner_bottom").css("display", "none");

        timelineSettings.enableScrollSpy = true;

        return timelineSettings.success;
    }

    timelineSettings.initializeTimelineSlider = async function (mediaTypeFilter) {
        const dateList = timelineSettings.timelineDates;
        const dateSliderHeight = $("#dateSlider").height();
        const containerHeight = $("body").height();
        const sliderOffset = (dateSliderHeight/containerHeight)-0.2;

        if (dateList.length > 0) {
            // Tooltip for handle
            const handleTooltip = $('<span class="badge bg-secondary" id="tooltip" style="background-color: slategray" />').css({
                'position': 'absolute',
                'right': 17
            }).hide();

            handleTooltip.text(Util.getShortMonths(dateList[0].month - 1) + ' ' + dateList[0].year);

            $("#dateSlider").slider({
                orientation: "vertical",
                value: dateList.length - 1,
                min: 0,
                max: dateList.length - 1,
                step: 0.0001,
                range: false,
                slide: function (event, ui) {
                    const currentDateObj = dateList[Math.round((dateList.length - 1) - ui.value)];

                    if (currentDateObj) {
                        const prevDateObj = dateList.length > 1 ? dateList[Math.round((dateList.length - 2) - ui.value)] : currentDateObj;

                        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
                            handleTooltip.text(Util.getShortMonths(currentDateObj.month - 1) + ' ' + currentDateObj.day + ', ' + currentDateObj.year);
                            $(".monthYearSlider").hide();
                        } else if (prevDateObj) {
                            handleTooltip.text(Util.getShortMonths(prevDateObj.month - 1) + ' ' + prevDateObj.day + ', ' + prevDateObj.year);
                            $(".monthYearSlider").hide();
                        }
                    }
                },
                stop: function (event, ui) {
                    const currentDateObj = dateList[Math.round((dateList.length - 1) - ui.value)];

                    if (currentDateObj && timelineSettings.enableScrollSpy === true) {
                        timelineSettings.jumpFromTimelineToc(event, currentDateObj.year + '-' + currentDateObj.month + '-' + currentDateObj.day, mediaTypeFilter);
                    }
                    $(".monthYearSlider").show();
                },
                change: function (event, ui) {
                    const currentDateObj = dateList[Math.round((dateList.length - 1) - ui.value)];
                    if (currentDateObj) {
                        const prevDateObj = dateList.length > 1 ? dateList[Math.round((dateList.length - 2) - ui.value)] : currentDateObj;

                        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
                            handleTooltip.text(Util.getShortMonths(currentDateObj.month - 1) + ' ' + currentDateObj.day + ', ' + currentDateObj.year);
                        } else if (prevDateObj) {
                            handleTooltip.text(Util.getShortMonths(prevDateObj.month - 1) + ' ' + prevDateObj.day + ', ' + prevDateObj.year);
                        }

                        handleTooltip.show();
                    }
                }
            }).find(".ui-slider-handle").append(handleTooltip).hover(function () {
                handleTooltip.show();
            });

            // Handle style
            $(".ui-slider-handle").css({"z-index": "9999"});

            // Render ticks
            let prevEl = null;
            let prevTickEl = null;

            for (let i = 0; i < dateList.length; i++) {
                const timelineDateObj = dateList[i];
                const tickTop = i / dateList.length * 100;

                if (timelineDateObj) {
                    const dateObj = new Date(timelineDateObj.month + "/" + timelineDateObj.day + "/" + timelineDateObj.year)
                    if (i === 0 || i === dateList.length-1 || (i < dateList.length && dateList[i + 1].year !== timelineDateObj.year)) {
                        // Label for year
                        const el = $('<span class="badge rounded-pill bg-secondary yearLabel" id="sliderLabel' + dateObj.getFullYear() + '" style="background-color: slategray">' + dateObj.getFullYear() + '</span>').css({
                            'width': '35px',
                            'right': '15px',
                            'font-size': 'xx-small',
                            'position': 'absolute',
                            'z-index': '2',
                            'top': (tickTop-sliderOffset) + '%'
                        });

                        $("#dateSlider").append(el);
                        setTimeout(function() {
                            if (prevEl !== null && Util.isOverlap($("#" + prevEl.attr("id")), $("#" + el.attr("id"))) === true) {
                                $("#" + el.attr("id")).hide();

                                if (prevTickEl !== null && Util.isOverlap($("#" + prevTickEl.attr("id")), $("#" + el.attr("id"))) === true) {
                                    $("#" + prevTickEl.attr("id")).hide();
                                }
                            } else {
                                prevEl = el;
                            }
                        },0);
                    } else if (i > 0 && (dateList[i - 1].year !== timelineDateObj.year || dateList[i - 1].month !== timelineDateObj.month)) {
                        // Tick for month/year
                        const tickEl = $('<span id="tickLabel' + timelineDateObj.year + '-' + timelineDateObj.month + '" style="color: #777777">' + '-' + '</span>').css({
                            'width': '10px',
                            'right': '15px',
                            'position': 'absolute',
                            'z-index': '1',
                            'bottom': '50%',
                            'top': (tickTop-sliderOffset) + '%'
                        });

                        $("#dateSlider").append(tickEl);
                        setTimeout(function() {
                                prevTickEl = tickEl;
                        },0);
                    }

                    // Tooltip for month/year on slider
                    const sliderTooltip = $('<span class="badge bg-secondary" style="background-color: slategray" />').css({
                        position: 'absolute',
                        right: 15,
                        bottom: "50%"
                    }).hide();

                    sliderTooltip.text(Util.getShortMonths(timelineDateObj.month - 1) + ' ' + timelineDateObj.year);

                    const sliderEl = $('<span class="monthYearSlider" data-slider-id="' + timelineDateObj.year + '-' + timelineDateObj.month + '">&nbsp;</span>').css({
                        'width': '73px',
                        'right': '0px',
                        'margin-right': '-3px',
                        'cursor': 'default',
                        'z-index': '3',
                        'position': 'absolute',
                        'top': tickTop + '%'
                    });

                    $(sliderEl).append(sliderTooltip);
                    $("#dateSlider").append(sliderEl);

                    sliderEl.hover(function () {
                        sliderTooltip.show();
                    }, function () {
                        sliderTooltip.hide();
                    });
                }
            }

            $("#dateSliderWrapper").hover(function () {
                $("#dateSlider").show();
            }, function () {
                $("#dateSlider").hide();
            });

            // setTimeout(function() {
            //     $("#dateSlider").hide();
            // },0);
        }
    }

    timelineSettings.jumpFromTimelineToc = async function (e, anchor, mediaTypeFilter) {
        if (e) {
            e.preventDefault();
        }

        timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
        timelineSettings.enableScrollSpy = false;

        shashin.printMessageToConsole("jumpFromTimelineToc anchor:" + anchor);
        shashin.printMessageToConsole("jumpFromTimelineToc mediaTypeFilter:" + mediaTypeFilter);

        $('section').each(function (index, element) {
            Util.removeDateGallery(element.id);
        });
        const msg = await timelineSettings.updateTimeline(anchor, mediaTypeFilter, "new", null)
        if (msg === timelineSettings.success && $("#" + anchor).length === 1) {
            timelineSettings.attachAssociatedMetadata(anchor, mediaTypeFilter);

            // Render below visibleContainers going from top down
            const timelineDates = timelineSettings.timelineDates;
            let currentDate = anchor;
            const firstDate = timelineDates[0].year + "-" + timelineDates[0].month + "-" + timelineDates[0].day;
            const lastDate = timelineDates[timelineDates.length-1].year + "-" + timelineDates[timelineDates.length-1].month + "-" + timelineDates[timelineDates.length-1].day;

            for (const [index, timelineDate] of timelineDates.entries()) {
                let prevDate = timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day;

                if (Util.getDateObject(prevDate) < Util.getDateObject(currentDate)) {
                    if ($("#" + currentDate).length === 0) {
                        // Render currentDate
                        const anchorPoint = timelineDates[index - 2].year + "-" + timelineDates[index - 2].month + "-" + timelineDates[index - 2].day;
                        const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, "below", anchorPoint);
                        if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                        }
                    }

                    // Break if top not in viewport
                    if (Util.elementsInViewport($("#" + currentDate)).length === 0) {
                        //Util.removeDateGallery(currentDate);
                        break;
                    }

                    if (prevDate !== lastDate) {
                        currentDate = prevDate;
                    } else {
                        const msg = await timelineSettings.updateTimeline(lastDate, mediaTypeFilter, "below", currentDate);
                        if (msg === timelineSettings.success && $("#" + lastDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(lastDate, mediaTypeFilter);
                        }
                    }
                }
            }

            // Render above visibleContainers going from bottom up
            let prevDate = "";
            currentDate = anchor;
            const timelineDatesReverse = timelineDates.slice().reverse();
            for (const [index, timelineDate] of timelineDatesReverse.entries()) {
                prevDate = timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day;

                if (Util.getDateObject(currentDate) < Util.getDateObject(prevDate)) {
                    if ($("#" + currentDate).length === 0) {
                        // Render currentDate
                        const anchorPoint = timelineDatesReverse[index - 2].year + "-" + timelineDatesReverse[index - 2].month + "-" + timelineDatesReverse[index - 2].day;
                        const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, "above", anchorPoint);
                        if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                        }
                    }

                    scrollByOne();

                    // Break if top not in viewport
                    if (Util.elementsInViewport($("#" + currentDate)).length === 0) {
                        //Util.removeDateGallery(currentDate);
                        currentDate = prevDate;
                        break;
                    }

                    if (prevDate !== firstDate) {
                        currentDate = prevDate;
                    } else {
                        const msg = await timelineSettings.updateTimeline(firstDate, mediaTypeFilter, "above", currentDate);
                        if (msg === timelineSettings.success && $("#" + firstDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(firstDate, mediaTypeFilter);
                        }
                    }
                }
            }

            timelineSettings.setScrollSpyActive(anchor);
            timelineSettings.scrollToTimelineToc(Util.elementsInViewport($(".scrollspy")));
            timelineSettings.renderThumbnails(Util.elementsInViewport($(".scrollspy")), mediaTypeFilter, timelineDates).then(function () {
                if (Util.elementsInViewport($("#" + firstDate)).length > 0 ||
                    Util.elementsInViewport($("#br" + firstDate)).length > 0 ||
                    Util.elementsInViewport($("#row" + firstDate)).length > 0) {
                    scrollByOne();
                }
            });
        }
    }

    timelineSettings.observeAnchorChange = function(id, functionCall) {
        if (MutationObserver) {
            let anchorVisible = false;
            let offcanvasAnchorVisible = false;
            const observer = new MutationObserver(function (mutations, me) {
                const anchorEl = document.getElementById(id);
                const offcanvasAnchorEl = document.getElementById("offcanvas_"+id);

                if (anchorEl) {
                    anchorVisible = true;
                }
                if (offcanvasAnchorEl) {
                    offcanvasAnchorVisible = true;
                }

                if (anchorVisible === true && offcanvasAnchorVisible === true) {
                    functionCall(id);
                    me.disconnect(); // stop observing
                    return true;
                }
            });

            observer.observe(document, {
                childList: true,
                subtree: true
            });
        } else {
            const existCondition = setInterval(function () {
                if ($("#" + id).length > 0 && $("#offcanvas_" + id).length > 0) {
                    clearInterval(existCondition);
                    functionCall(id);
                }
            }, 100);
        }
    }

    timelineSettings.scrollToToc = function(anchor) {
        const url = location.href;
        location.href = '#' + anchor;
        history.replaceState(null, null, url);

        const navElem = $("#offcanvas_" + anchor);
        const timer = setInterval(function () {
            if (navElem.hasClass("active") === true) {
                timelineSettings.enableScrollSpy = true;
                clearInterval(timer);
            }
        }, 200);
    }

    // Set the active nav
    timelineSettings.setScrollSpyActive = function (id) {
        if (typeof id === 'string' || id instanceof String) {
            $("#offcanvasTocBody").find('.active').removeClass('active');
            const idArray = id.split("-");

            const navElem = $('a[href^="#' + idArray[0] + '-' + idArray[1] + '-"]');
            navElem.addClass('active');
        }
    }

    timelineSettings.scrollToTimelineToc = function(elements) {
        let scrolled = false;
        elements.each(function(index) {
            let id = $(this).attr("id");

            if (id.indexOf("tail_") < 0 && $(id).is(":visible") === true) {
                $("#offcanvas_"+id).scrollIntoView({
                    behavior: 'smooth'
                });
                scrolled = true;
                return false;
            }
        });
        if (scrolled === false) {
            $("#offcanvasTocBody").children().each(function () {
                if ($(this).attr('class') === 'list-group') {
                    $(this).children().each(function () {
                        if ($(this).hasClass("active")) {
                            $(this)[0].scrollIntoView({
                                behavior: 'smooth'
                            });
                            scrolled = true;
                            return false;
                        }
                    });
                }
                if (scrolled === true) {
                    return false;
                }
            });
        }
    }

    // Hook up data to edit albums, favorites and people labels
    timelineSettings.attachAssociatedMetadata = function(date,mediaTypeFilter) {
        timelineSettings.rendered = false;

        const http = new Http("attaching associated metadata");
        const version = Util.getMetadataLocalStorage();
        http.ajax("get", "/timeline/mediatype/" + mediaTypeFilter + "/date/" + date + (version === "" ? "" : "?v=" + version)).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === timelineSettings.success) {
                    if (data.hasOwnProperty("metadataList") &&
                        data.hasOwnProperty("favorites")
                    ) {
                        const metadataList = data["metadataList"];
                        const favoritesMap = data["favorites"];

                        if (metadataList.length > 0) {
                            for (const index in metadataList) {
                                const metadata = metadataList[index];

                                setTimeout(function () {
                                    if (Util.elementsInViewport($("#image" + metadata.id))) {

                                        if ($("#tnbr" + metadata.id + ".thumbnail-br").length === 0) {
                                            $("#tnbr" + metadata.id).addClass("thumbnail-br");
                                        }

                                        if ($("#favorite" + metadata.id).length === 0) {
                                            $("#tnbr" + metadata.id).append(TimelineGalleryBottomRightOverlay({metadata:metadata}));
                                            const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["favorite"] === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                                            const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["count"] > 0 ? favoritesMap[metadata.id]["count"] : 0;
                                            $("#bricon" + metadata.id).addClass(favoriteIcon);
                                            $("#briconcount" + metadata.id).text(favoriteCount);
                                        }

                                        if ($("#image" + metadata.id).length === 1) {
                                            $("#image" + metadata.id).attr("src", encodeURI(metadata.thumbnailUrlSmall));
                                            $("#image" + metadata.id).css("background-color", "lightgray");
                                            $("#image" + metadata.id).attr("onError", "Util.errorImg(this,\'" + metadata.title + "\',Util.thumbnailHeight())");
                                        }

                                        if ($("#tnbl" + metadata.id + ".thumbnail-bl").length === 0) {
                                            $("#tnbl" + metadata.id).addClass("thumbnail-bl");
                                        }

                                        if ($("#tncentered" + metadata.id + ".thumbnail-centered").length === 0) {
                                            $("#tncentered" + metadata.id).addClass("thumbnail-centered");
                                        }

                                        const mediaContent = {};
                                        mediaContent.func = shashin.openInfoSidebar;
                                        mediaContent.args = metadata.id;
                                        mediaContent.thumb = encodeURI(metadata.thumbnailUrlSmall);

                                        if (metadata.type.indexOf("video") >= 0) {
                                            mediaContent.video = '{"source": [{"src":"' + encodeURI(metadata.videoUrl) + '", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}';
                                            mediaContent.downloadUrl = encodeURI(metadata.videoUrl) + "/download";
                                        } else {
                                            mediaContent.src = metadata.thumbnailUrlOriginal;
                                            mediaContent.downloadUrl = encodeURI(metadata.thumbnailUrlOriginal);
                                        }

                                        if (metadata.originalImageWidth !== null) {
                                            mediaContent.width = metadata.originalImageWidth;
                                        }

                                        if ($("#mediaLink" + metadata.id).length === 0) {
                                            $("#tncentered" + metadata.id).append(TimelineGalleryCenterOverlay({metadata:metadata,mediaContent:mediaContent}));
                                        }

                                        if ($("#timelineModalEdit" + metadata.id).length === 0) {
                                            const editIcon = (metadata.lat === null || metadata.lng === null) ? "bi-pencil-square" : "bi-pencil";
                                            $("#tnbl" + metadata.id).append(TimelineGalleryBottomLeftOverlay({metadata:metadata,editIcon:editIcon}));
                                            $("#timelineModalEdit" + metadata.id).attr("tag", metadata.id);
                                            $("#timelineModalEdit" + metadata.id).on("click", function (e) {
                                                e.preventDefault();
                                                shashin.openEditMetadataModal(metadata.id, timelineSettings)
                                            });
                                        }

                                        if ($("#select" + metadata.id).length === 0) {
                                            $("#tntl" + metadata.id).append(TimelineGalleryTopLeftOverlay({metadata:metadata})).ready(function () {
                                                timelineSettings.rendered = true;
                                            });
                                        }

                                        if ($("#tntl" + metadata.id + ".thumbnail-tl").length === 0) {
                                            $("#tntl" + metadata.id).addClass("thumbnail-tl");
                                            shashin.setPhotoOverlays(metadata, "timeline")
                                            timelineSettings.activateMetadataListeners(metadata.id);
                                        }

                                        if (metadata.type.indexOf("video") >= 0) {
                                            if ($("#video" + metadata.id).length === 0) {
                                                $("#tntr" + metadata.id).append(TimelineGalleryTopRightOverlay({metadata:metadata})).ready(function () {
                                                    timelineSettings.rendered = true;
                                                });
                                            }
                                            if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                                                $("#tntr" + metadata.id).addClass("thumbnail-tr");
                                            }
                                        } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight * 2) {
                                            if ($("#panorama" + metadata.id).length === 0) {
                                                $("#tntr" + metadata.id).append(TimelineGalleryTopRightOverlay({metadata:metadata})).ready(function () {
                                                    timelineSettings.rendered = true;
                                                });
                                            }
                                            if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                                                $("#tntr" + metadata.id).addClass("thumbnail-tr");
                                            }
                                        }
                                    }
                                }, 500);
                            }
                        }
                    }
                } else {
                    timelineSettings.rendered = true;
                }
            } else {
                timelineSettings.rendered = true;
            }
        });
    }

    timelineSettings.updateTimeline = async function(date,mediaTypeFilter,action,attachToId) {
        $("#msgTimeline").html("");

        const version = Util.getMetadataLocalStorage();

        const ajaxParams = {
            type: 'get',
            url: "/timeline/mediatype/" + mediaTypeFilter + "/date/" + date + "/metadata" + (version === "" ? "" : "?v=" + version),
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        return await $.ajax(ajaxParams)
            .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating timeline")}).then(function (data) {
            let ret = "fail";
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === timelineSettings.success) {
                    if (data.hasOwnProperty("metadataList")) {
                        const metadataList = data["metadataList"];

                        if (metadataList.length > 0) {
                            let html = "";

                            let idCheck = "undated";
                            if (metadataList[0]["year"] === null ||
                                metadataList[0]["month"] === null ||
                                metadataList[0]["day"] === null)
                            {
                                idCheck = metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day;
                            }

                            html += TimelinePreLoadGalleryHeader({metadata:metadataList[0]});

                            if ($("#"+idCheck).length === 0) {
                                for (let index in metadataList) {
                                    index = parseInt(index);
                                    const metadata = metadataList[index];

                                    const yearTakenCount = $(".yearTaken").length;
                                    const monthTakenCount = $(".monthTaken").length;
                                    const dayTakenCount = $(".dayTaken").length;
                                    let lastYearTaken = $(".yearTaken").length === 0 ? (metadataList[0]["year"] === null ? "" : metadataList[0]["year"]) : $(".yearTaken").get(yearTakenCount - 1).innerText;
                                    let lastMonthTaken = $(".monthTaken").length === 0 ? (metadataList[0]["month"] === null ? "" : metadataList[0]["month"]) : $(".monthTaken").get(monthTakenCount - 1).innerText;
                                    let lastDayTaken = $(".dayTaken").length === 0 ? (metadataList[0]["day"] === null ? "" : metadataList[0]["day"]) : $(".dayTaken").get(dayTakenCount - 1).innerText;
                                    lastYearTaken = lastYearTaken !== "" ? parseInt(lastYearTaken) : 0;
                                    lastMonthTaken = lastMonthTaken !== "" ? parseInt(lastMonthTaken) : 0;
                                    lastDayTaken = lastDayTaken !== "" ? parseInt(lastDayTaken) : 0;

                                    html += TimelinePreLoadGalleryBody({metadata:metadata});

                                    $("#timelineModalEdit" + metadata.id).attr("tag", metadata.id);
                                }

                                const lastDateParts = $("#offcanvasTocBody div a").last().attr("id").split("offcanvas_");
                                const lastDate = lastDateParts[1];

                                html += TimelinePreLoadGalleryFooter({metadata:metadataList[0],lastDate:lastDate});

                                const tempScrollTop = $("#container").scrollTop();

                                let htmlEl = $(html);

                                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                    if (Util.isSafari() === true) {
                                        $("#infinite-scroll-gallery").css('visibility', 'hidden');
                                    } else if (Util.isFirefox() === true) {
                                        htmlEl.hide();
                                    }
                                }

                                setTimeout(function () {
                                    if (action === "above") {
                                        htmlEl.insertBefore($("#container_" + attachToId)).ready(function () {
                                            // deferred.resolve(timelineSettings.success);
                                            ret = timelineSettings.success;
                                            if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                if (Util.isSafari() === true) {
                                                    $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                    $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                } else if (Util.isFirefox() === true) {
                                                    $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                    htmlEl.show();
                                                }
                                            }
                                        });
                                    } else if (action === "new") {
                                        $("#infinite-scroll-gallery").prepend(htmlEl).ready(function () {
                                            if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                if (Util.isSafari() === true) {
                                                    $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                    $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                } else if (Util.isFirefox() === true) {
                                                    $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                    htmlEl.show();
                                                }
                                            }
                                            // deferred.resolve(timelineSettings.success);
                                            ret = timelineSettings.success;
                                        });
                                    } else {
                                        if (attachToId == null) {
                                            if ($(".attachMetadataPhotos").length > 0) {
                                                htmlEl.insertAfter($(".attachMetadataPhotos").last()).ready(function () {
                                                    if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                        if (Util.isSafari() === true) {
                                                            $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                            $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                        } else if (Util.isFirefox() === true) {
                                                            $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                            htmlEl.show();
                                                        }
                                                    }
                                                    // deferred.resolve(timelineSettings.success);
                                                    ret = timelineSettings.success;
                                                });
                                            } else {
                                                $("#infinite-scroll-gallery").prepend(htmlEl).ready(function () {
                                                    if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                        if (Util.isSafari() === true) {
                                                            $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                            $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                        } else if (Util.isFirefox() === true) {
                                                            $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                            htmlEl.show();
                                                        }
                                                    }
                                                    // deferred.resolve(timelineSettings.success);
                                                    ret = timelineSettings.success;
                                                });
                                            }
                                        } else {
                                            htmlEl.insertAfter($("#amp_" + attachToId)).ready(function () {
                                                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                    if (Util.isSafari() === true) {
                                                        $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                        $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                    } else if (Util.isFirefox() === true) {
                                                        $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                        htmlEl.show();
                                                    }
                                                }
                                                // deferred.resolve(timelineSettings.success);
                                                ret = timelineSettings.success;
                                            });
                                        }
                                    }
                                }, 0);
                            } else {
                                // Already attached
                                // deferred.resolve(timelineSettings.success);
                                ret = timelineSettings.success;
                            }
                        } else {
                            $(".attachMetadataPhotos").last().text("EOL").css("display", "none")
                            // deferred.resolve(timelineSettings.success);
                            ret = timelineSettings.success;
                        }
                        ret = timelineSettings.success;
                    }
                    ret = timelineSettings.success;
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                    // deferred.resolve("fail");
                    ret = "fail";
                }
            }

            return ret;
        });
    }

    timelineSettings.activateMetadataListeners = function(metadataId) {
        Util.activateMetadataListeners(metadataId);

        shashin.updateFavorites("#favorite","#bricon","#briconcount",metadataId);
    }

}( window.timelineSettings = window.timelineSettings || {}, jQuery ));

// Hack to close TOC canvas
$(document).on("click", function(event) {
    const $target = $(event.target);
    if (!$target.closest('#offcanvasToc').length &&
        !$target.closest('#timelineTocToggle').length &&
        $("#offcanvasToc").css("visibility") === "visible")
    {
        $("#offcanvasTocCloseButton").click();
    }
});

if (typeof module !== 'undefined') {
    module.exports = window.timelineSettings;
}