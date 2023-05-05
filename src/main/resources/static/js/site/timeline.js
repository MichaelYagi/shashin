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
    timelineSettings.metadataYearMonthCount = [];
    timelineSettings.thumbnailsPerRow = 4;

    const calculateDistanceToFooter = function() {
        return $(window).height() - $('#subfooter').offset().top;
    }

    const closeToFooter = function() {
        return (timelineSettings.distanceToFooter === 9999 || (timelineSettings.distanceToFooter > -100 && timelineSettings.distanceToFooter < 1) || Util.elementsInViewport($("#subfooter")).length > 0);
    }

    const scrollByOne = function() {
        document.getElementById("container").scrollBy({top: 1, behavior: "smooth"});
        if (document.getElementsByTagName("MAIN").length > 0) {
            document.getElementsByTagName("MAIN")[0].scrollBy({top: 1, behavior: "smooth"});
        }
    }

    timelineSettings.init = function(mediaTypeFilter, metadataDates, metadataYearMonthCount) {
        timelineSettings.timelineDates = metadataDates;
        timelineSettings.metadataYearMonthCount = metadataYearMonthCount;

        Util.setMetadataLocalStorage();

        if (Util.isMobile() === false) {
            $("#infinite-scroll-gallery").attr('style', 'width: 97%');
        }

        shashin.setLightGalleryElement('infinite-scroll-gallery');
        shashin.setLightGallery({"selector":".mediaLink",plugins:[lgMetadataDetail],metadataDetail:true,metadataDetailFunc:shashin.openInfoSidebar});

        let topScroll = true;
        let topOfPage = true;
        let scrollTimer = null;
        // let sliderTimer = null;

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

        $(window).bind("scrollStop", function() {
            if (timelineSettings.enableScrollSpy === true) {
                topScroll = false;
                const elementsInViewport = Util.elementsInViewport($(".scrollspy"));
                timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);
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
            }

            if (Util.isMobile() === false && $("#dateSliderWrapper:not(:hover)").length > 0) {
                $("#dateSlider").hide();
            }

            setTimeout(() => {
                timelineSettings.reinitLightGalleryInstance();
            }, 500);
        });

        // $(window).bind("sliderScrollStop", function() {
        //     if ($("#dateSliderWrapper:not(:hover)").length === 1) {
        //         console.log("sliderScrollStop");
        //         $("#dateSlider").hide();
        //     }
        // });

        // Scroll event handler
        const scrollHandler = function (e) {
            if (scrollTimer !== null) {
                clearTimeout(scrollTimer);
            }
            scrollTimer = setTimeout(function() {
                $(window).trigger("scrollStop");
            }, 200);

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

            // if (Util.isMobile() === false) {
            //     if (sliderTimer !== null) {
            //         clearTimeout(sliderTimer);
            //     }
            //     sliderTimer = setTimeout(function () {
            //         $(window).trigger("sliderScrollStop");
            //     }, 1000);
            // }

            if (timelineSettings.enableScrollSpy === true) {
                topScroll = false;
                timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);
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

            if ($("#offcanvas_"+hash).length > 0) {
                timelineSettings.jumpFromTimelineToc(null, hash, mediaTypeFilter);
            }
        }

        if (scrollTimer !== null) {
            clearTimeout(scrollTimer);
        }
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
                if (Util.isInViewport($("#tail_"+lastDate)) === true) {
                    timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.up;
                } else {
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
            }

            elements.each(function (index) {
                let id = $(this).attr("id");

                if (id.indexOf("tail_") === -1 && index < 2 && timelineSettings.prevAnchor !== id) {
                    // Scrolling behavior different on Chrome iOS
                    if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                        timelineSettings.renderThumbnailsAlt(id, mediaTypeFilter).then(function (msg) {
                            if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                                timelineSettings.setScrollSpyActive(id);
                                Util.checkErrorImage();
                            }
                        });
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

            // Scrolling behavior different on Chrome iOS
            if ((Util.isSafari() === false || Util.isFirefox() === true) && !(Util.getOS() === "iOS" && Util.isChrome() === true)) {
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
                        Util.checkErrorImage();
                    }
                });
            }

            $("img").hover(function () {
                if (reinitGalleryFlag === false && timelineSettings.enableScrollSpy === true) {
                    reinitGalleryFlag = true;
                    timelineSettings.reinitLightGalleryInstance();
                }
            });

            prevElements = elements;
        }
    }

    // Render only what's needed
    timelineSettings.renderThumbnailsAlt = async function(id,mediaTypeFilter) {
        if (timelineSettings.initialized === false) {
            timelineSettings.initialized = true;
        } else {
            $("#spinner_top").css("display", "block");
        }
        if ($(".attachMetadataPhotos").last().text() !== "EOL") {
            $("#spinner_bottom").css("display", "block");
        }

        timelineSettings.enableScrollSpy = false;
        //let deferred = new $.Deferred();

        // Depth of results in section of page above and below anchor
        // Dynamic depending on current number of results on page
        const idsInView = Util.elementsInViewport($(".scrollspy")).map(function() {
            let id = $(this).attr('id');
            if (id.indexOf("tail_") > -1) {
                id = id.split("tail_")[1];
            }
            return id;
        }).get().filter(
            function(a){if (!this[a]) {this[a] = 1; return a;}},
            {}
        );

        let depth = (Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) ? 5 : (idsInView.length < 3 ? 3 : idsInView.length);
        let depthDown = depth-1;
        let depthUp = depth;

        shashin.printMessageToConsole("depthDown:"+depthDown);
        shashin.printMessageToConsole("depthUp:"+depthUp);
        shashin.printMessageToConsole("renderThumbnails id:"+id);

        let offCanvasId = $("#offcanvas_"+id);

        let dateCount = 0;

        const attachAboveArray = [];
        let innerLoopBreak = false;
        let offCanvasDate = offCanvasId.attr("id").split("_")[1];
        $($("#offcanvasTocBody").children().get().reverse()).each(function () {
            if ($(this).attr('class') === 'list-group') {
                $($(this).children().get().reverse()).each(function () {
                    const attr = $(this).attr("id");
                    if (typeof attr !== 'undefined' && attr !== false) {
                        const dateParts = attr.split("offcanvas_");
                        const date = dateParts[1];
                        if (Util.getDateObject(offCanvasDate) < Util.getDateObject(date)) {
                            attachAboveArray.unshift(date);
                            if (dateCount >= depthUp) {
                                innerLoopBreak = true;
                                return false;
                            }
                            dateCount++;
                        }
                    }
                });
            }
            if (innerLoopBreak === true) {
                return false;
            }
        });

        const attachBelowArray = [];
        dateCount = 0;
        innerLoopBreak = false;
        $("#offcanvasTocBody").children().each(function () {
            if ($(this).attr('class') === 'list-group') {
                $(this).children().each(function () {
                    const attr = $(this).attr("id");
                    if (typeof attr !== 'undefined' && attr !== false) {
                        const dateParts = attr.split("offcanvas_");
                        const date = dateParts[1];
                        if (Util.getDateObject(date) < Util.getDateObject(offCanvasDate)) {
                            attachBelowArray.push(date);
                            if (dateCount > depthUp) {
                                innerLoopBreak = true;
                                return false;
                            }
                            dateCount++;
                        }
                    }
                });
            }
            if (innerLoopBreak === true) {
                return false;
            }
        });

        // Remove elements that are not visible
        let prevElementId = "";
        let topHeight = 0;
        let tempScrollTop = $("#container").scrollTop();

        if (Util.isSafari() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) {
            $('section').each(function (index, element) {
                shashin.printMessageToConsole(element.id + " checking to remove end");
                if (($.inArray(element.id, attachAboveArray) === -1 && $.inArray(element.id, attachBelowArray) === -1 && element.id !== id) || ($("#" + element.id).length > 1 || prevElementId === element.id)) {

                    // Get height to set scrollTop for non chrome browsers
                    if (Util.getDateObject(id) < Util.getDateObject(element.id)) {
                        topHeight += Util.getDateGalleryHeight(element.id);
                    }

                    shashin.printMessageToConsole(element.id + " removed end");
                    Util.removeDateGallery(element.id);
                }
                prevElementId = element.id;
            });
        }

        // Smooth scrolling when element is removed for non chrome browsers
        if ((Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) && timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down && topHeight > 0) {
            $("#container").scrollTop(tempScrollTop - topHeight);
        }

        shashin.printMessageToConsole("attachAboveArray");
        shashin.printMessageToConsole(attachAboveArray);
        shashin.printMessageToConsole("attachBelowArray");
        shashin.printMessageToConsole(attachBelowArray);

        // Render top
        let action = "new";
        let attachPoint = id;
        for (let index in attachAboveArray) {
            const currentId = attachAboveArray[index];
            shashin.printMessageToConsole("attempting to attaching id above:" + currentId);
            if ($("#" + currentId).length === 0) {
                if (action === "new") {
                    attachPoint = null;
                }
                shashin.printMessageToConsole("attaching above attachPoint:" + attachPoint);
                shashin.printMessageToConsole("attaching id:" + currentId);
                shashin.printMessageToConsole("actionAbove:" + action)
                const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint);
                $("#container_"+currentId).outerHeight(true);
                if (msg === timelineSettings.success && $("#"+currentId).length === 1) {
                    timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                }

                action = "below";
            }
            attachPoint = currentId;
        }

        // Render bottom
        action = "below"
        if (attachAboveArray.length === 0 && $("#"+id).length === 0) {
            attachPoint = null
        }
        for (let index in attachBelowArray) {
            const currentId = attachBelowArray[index];
            shashin.printMessageToConsole("attempting to attaching id below:" + currentId);
            if ($("#"+currentId).length === 0) {
                shashin.printMessageToConsole("attaching below attachPoint:" + attachPoint);
                shashin.printMessageToConsole("attaching id:" + currentId);
                shashin.printMessageToConsole("actionBelow:"+action)
                const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint);
                if (msg === timelineSettings.success && $("#"+currentId).length === 1) {
                    timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                }
            }
            attachPoint = currentId;
        }

        if (Util.isSafari() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) {
            let rendered = false;
            while (true) {
                let dateFound = false;
                let currentId = attachPoint;
                $("#offcanvasTocBody").children().each(function () {
                    if ($(this).attr('class') === 'list-group') {
                        $(this).children().each(function () {
                            const attr = $(this).attr("id");
                            if (typeof attr !== 'undefined' && attr !== false) {
                                const dateParts = attr.split("offcanvas_");
                                const date = dateParts[1];
                                const nextattr = $(this).next().attr("id");
                                if (typeof nextattr !== 'undefined' && nextattr !== false && $(this).next().length > 0 && currentId === date) {
                                    currentId = nextattr.split("offcanvas_")[1];
                                    dateFound = true;
                                    return false;
                                }
                            }
                        });
                    }
                });

                if (dateFound === true && currentId !== null && $("#" + currentId).length === 0) {
                    if (action === "new") {
                        attachPoint = null;
                    }

                    const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint)
                    if (msg === timelineSettings.success && $("#" + currentId).length === 1) {
                        timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                    }

                    action = "below";
                    attachPoint = currentId;
                    attachBelowArray.push(currentId);

                    if (
                        rendered === false &&
                        //($("footer").withinviewport().length === 0) &&
                        (//$("#br" + currentId).withinviewport().length === 0 ||
                            //$("#row" + currentId).withinviewport().length === 0 ||
                            $("#amp_" + currentId).withinviewport().length === 0 //||
                            //$("#tail_" + currentId).withinviewport().length === 0 ||
                            //$("#" + currentId).withinviewport().length === 0 ||
                            //$(".photo-thumbnail-image.thumbnailTag_" + currentId).withinviewport().length === 0
                        )
                    ) {
                        rendered = true;
                        continue;
                    }
                }

                if (dateFound === false || rendered === true) {
                    break;
                }

                attachPoint = currentId;
            }
        }

        // Render mid
        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
            action = "new";
            if (attachAboveArray.length > 0) {
                attachPoint = attachAboveArray[attachAboveArray.length - 1];
                action = "below";
            } else if (attachBelowArray.length > 0) {
                attachPoint = attachBelowArray[0];
                action = "above";
            }

            shashin.printMessageToConsole("attempting to attaching id mid " + id + " " + action + " " + attachPoint + " length " + $("#" + id).length)

            // Hack for attaching mid point
            if (attachAboveArray.length > 0 && attachBelowArray.length > 0 && $('section')[$('section').length - 1].id === id && $("#" + id).length === 1) {
                shashin.printMessageToConsole("removing already existing id " + id + " for mid point")
                Util.removeDateGallery(id);
            }

            // Render mid
            if ($("#" + id).length === 0) {
                shashin.printMessageToConsole("attaching mid attachPoint:" + attachPoint)
                shashin.printMessageToConsole("attaching id:" + id);
                shashin.printMessageToConsole("attaching mid action:" + action)
                const msg = await timelineSettings.updateTimeline(id, mediaTypeFilter, action, attachPoint);
                if (msg === timelineSettings.success && $("#" + id).length === 1) {
                    timelineSettings.attachAssociatedMetadata(id, mediaTypeFilter);
                }
            }
        }

        shashin.printMessageToConsole("==============================================");
        //deferred.resolve(timelineSettings.successMidMsg);
        //return deferred.promise();

        $("#spinner_top").css("display", "none");
        $("#spinner_bottom").css("display", "none");
        timelineSettings.enableScrollSpy = true;

        return timelineSettings.successMidMsg;
    }

    timelineSettings.renderThumbnails = async function(elements,mediaTypeFilter,timelineDates) {
        timelineSettings.enableScrollSpy = false;

        if ($(".attachMetadataPhotos").last().text() !== "EOL") {
            $("#spinner_bottom").css("display", "block");
        }

        let firstElementId = $(elements[0]).attr("id");
        let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
        let lastElementId = $(elements[elements.length-1]).attr("id");
        let lastVisibleId = lastElementId.indexOf("tail_") === -1 ? lastElementId : lastElementId.substring(5, lastElementId.length);
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
        let removeHeight = 0;
        let topHeight = 0;
        let tempScrollTop = $('#container').scrollTop();
        const section = $('section');

        const removedElements = [];
        section.each(function (index, element) {
            if (Util.isInViewport($("#" + element.id)) === false &&
                Util.isInViewport($("#br" + element.id)) === false &&
                Util.isInViewport($("#row" + element.id)) === false &&
                Util.isInViewport($("#tail_" + element.id)) === false &&
                Util.isInViewport($("#container_" + element.id)) === false &&
                Util.elementsInViewport($(".photo-thumbnail-image.thumbnailTag_" + element.id)).length === 0 &&
                ((Util.isSafari() === false && Util.isFirefox() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) ||
                    ((timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up && (Util.getDateObject(lastVisibleId) > Util.getDateObject(element.id) || Util.getDateObject(firstVisibleId) < Util.getDateObject(element.id))) ||
                        (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down && Util.getDateObject(firstVisibleId) < Util.getDateObject(element.id)))
                ) &&
                ((timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down && element.id !== $(section[section.length-1]).attr("id")) ||
                    (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up && element.id !== ignoreTimelineDate))
            ) {
                if (Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                    section.css('visibility', 'hidden');
                }

                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
                    removeHeight += Util.getDateGalleryHeight(element.id);
                    Util.removeDateGallery(element.id);
                    removedElements.push(element.id);
                } else if (Util.getDateObject(firstVisibleId) < Util.getDateObject(element.id)) {
                    topHeight += $("#container_"+element.id).outerHeight(true) + $("#amp_"+element.id).outerHeight(true);
                    Util.removeDateGallery(element.id);
                } else if (Util.getDateObject(lastVisibleId) > Util.getDateObject(element.id)) {
                    Util.removeDateGallery(element.id);
                }
            }
        });

        if (Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
            if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
                $('#container').scrollTop(tempScrollTop - removeHeight);
            } else if (topHeight > 0) {
                $('#container').scrollTop(tempScrollTop - topHeight);
            }
        }
        section.css('visibility', 'visible');

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
                    if ($("#" + currentDate).length === 0 && ((Util.isSafari() === false && Util.isFirefox() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) ||
                        ((Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) && $.inArray(currentDate, removedElements) === -1))) {

                        // Render currentDate
                        const anchorPoint = timelineDates[index - 2].year + "-" + timelineDates[index - 2].month + "-" + timelineDates[index - 2].day;

                        const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, "above", anchorPoint);

                        if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                        }

                        // Break if top not in viewport
                        if (Util.elementsInViewport($("#" + currentDate)).length === 0) {
                            scrollByOne();
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
                    if ($("#" + currentDate).length === 0 && ((Util.isSafari() === false && Util.isFirefox() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) || ((Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) && $.inArray(currentDate, removedElements) === -1))) {

                        const numberOfPhotos = timelineSettings.metadataYearMonthCount[timelineDate.year + "-" + timelineDate.month];

                        let sectionHeight = 0;

                        if (numberOfPhotos !== null && numberOfPhotos > 0) {
                            // sectionHeight = (Math.ceil(numberOfPhotos / timelineSettings.thumbnailsPerRow) * Util.thumbnailHeight()) + ((Math.ceil(numberOfPhotos / timelineSettings.thumbnailsPerRow) * Util.thumbnailHeight()) + 5)
                            sectionHeight = 11705;
                        }

                        let action = "below";

                        // Render currentDate
                        // Stage 1 - create a placeholder dive to enable scrolling through additional content based on current date section
                        const anchorPoint = timelineDates[index - 2].year + "-" + timelineDates[index - 2].month + "-" + timelineDates[index - 2].day;
                        if (anchorPoint !== (timelineDates[0].year + "-" + timelineDates[0].month + "-" + timelineDates[0].day) &&
                            anchorPoint !== (timelineDates[1].year + "-" + timelineDates[1].month + "-" + timelineDates[1].day) &&
                            anchorPoint !== (timelineDates[2].year + "-" + timelineDates[2].month + "-" + timelineDates[2].day) &&
                            anchorPoint !== (timelineDates[3].year + "-" + timelineDates[3].month + "-" + timelineDates[3].day)
                        ) {
                            // Stage 1 - create an empty block
                            await timelineSettings.createEmptyContainer(currentDate, anchorPoint, sectionHeight);
                            action = "emptyContainer";
                        } else {
                            action = "below";
                        }

                        // Stage 2 - network call to create image placeholders and UI skeleton for month
                        const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, action, anchorPoint);

                        // Stage 3 - network call to embed the image URL and complete the process
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

        const idsInView = Util.elementsInViewport($(".scrollspy")).map(function() {
            let id = $(this).attr('id');
            if (id.indexOf("tail_") > -1) {
                id = id.split("tail_")[1];
            }
            return id;
        }).get().filter(
            function(a){if (!this[a]) {this[a] = 1; return a;}},
            {}
        );

        let depth = (Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) ? 5 : (idsInView.length < 3 ? 3 : idsInView.length);
        let counter = 0;

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
                    if (Util.elementsInViewport($("#" + currentDate)).length === 0 || counter > depth) {
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

                    counter++;
                }
            }

            // Render above visibleContainers going from bottom up
            let prevDate = "";
            currentDate = anchor;
            const timelineDatesReverse = timelineDates.slice().reverse();
            counter = 0;
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
                    if (Util.elementsInViewport($("#" + currentDate)).length === 0 || counter > depth) {
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
                    counter++;
                }
            }

            timelineSettings.setScrollSpyActive(anchor);
            timelineSettings.scrollToTimelineToc(Util.elementsInViewport($(".scrollspy")));

            // timelineSettings.renderThumbnails(Util.elementsInViewport($(".scrollspy")), mediaTypeFilter, timelineDates).then(function (msg) {
            //     if (Util.elementsInViewport($("#" + firstDate)).length > 0 ||
            //         Util.elementsInViewport($("#br" + firstDate)).length > 0 ||
            //         Util.elementsInViewport($("#row" + firstDate)).length > 0) {
            //         scrollByOne();
            //     }
            // });

            $(".scrollspy").each(function (index) {
                let id = $(this).attr("id");

                if (id.indexOf("tail_") === -1 && index < 2 && timelineSettings.prevAnchor !== id) {
                    // Scrolling behavior different on Chrome iOS
                    if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                        timelineSettings.renderThumbnailsAlt(id, mediaTypeFilter).then(function (msg) {
                            if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                                timelineSettings.setScrollSpyActive(id);
                                Util.checkErrorImage();
                            }
                        });
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

            // Jump to anchor after rendering
            location.href = "#" + anchor;

            if (window.location.hash) {
                // Remove hash from URL
                history.pushState("", document.title, window.location.pathname + window.location.search);
            }
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
                                            $("#tnbr" + metadata.id).append(TimelineTemplates.TimelineGalleryBottomRightOverlay({metadata:metadata}));
                                            const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["favorite"] === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                                            const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["count"] > 0 ? favoritesMap[metadata.id]["count"] : 0;
                                            $("#bricon" + metadata.id).addClass(favoriteIcon);
                                            $("#briconcount" + metadata.id).text(favoriteCount);
                                        }

                                        if ($("#image" + metadata.id).length === 1) {
                                            $("#image" + metadata.id).attr("src", encodeURI(metadata.thumbnailUrlSmall));
                                            $("#image" + metadata.id).css("background-color", "lightgray");
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
                                            mediaContent.video = '{"source": [{"src":"' + encodeURI(metadata.videoUrl) + '", "type":"video/mp4"}], "attributes": {"preload": "auto", "controls": true, "autoplay": true}}';
                                            mediaContent.downloadUrl = encodeURI(metadata.videoUrl) + "/download";
                                        } else {
                                            mediaContent.src = metadata.thumbnailUrlOriginal;
                                            mediaContent.downloadUrl = encodeURI(metadata.thumbnailUrlOriginal) + "/download";
                                        }

                                        if (metadata.originalImageWidth !== null) {
                                            mediaContent.width = metadata.originalImageWidth;
                                        }

                                        if ($("#mediaLink" + metadata.id).length === 0) {
                                            $("#tncentered" + metadata.id).append(TimelineTemplates.TimelineGalleryCenterOverlay({metadata:metadata,mediaContent:mediaContent}));
                                        }

                                        if ($("#timelineModalEdit" + metadata.id).length === 0) {
                                            const editIcon = (metadata.lat === null || metadata.lng === null) ? "bi-pencil-square" : "bi-pencil";
                                            $("#tnbl" + metadata.id).append(TimelineTemplates.TimelineGalleryBottomLeftOverlay({metadata:metadata,editIcon:editIcon}));
                                            $("#timelineModalEdit" + metadata.id).attr("tag", metadata.id);
                                            $("#timelineModalEdit" + metadata.id).on("click", function (e) {
                                                e.preventDefault();
                                                shashin.openEditMetadataModal(metadata.id, timelineSettings)
                                            });
                                        }

                                        if ($("#select" + metadata.id).length === 0) {
                                            $("#tntl" + metadata.id).append(TimelineTemplates.TimelineGalleryTopLeftOverlay({metadata:metadata})).ready(function () {
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
                                                $("#tntr" + metadata.id).append(TimelineTemplates.TimelineGalleryTopRightOverlay({metadata:metadata})).ready(function () {
                                                    timelineSettings.rendered = true;
                                                });
                                            }
                                            if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                                                $("#tntr" + metadata.id).addClass("thumbnail-tr");
                                            }
                                        } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight * 2) {
                                            if ($("#panorama" + metadata.id).length === 0) {
                                                $("#tntr" + metadata.id).append(TimelineTemplates.TimelineGalleryTopRightOverlay({metadata:metadata})).ready(function () {
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

    timelineSettings.createEmptyContainer = async function(date, attachToId, height) {
        $("#msgTimeline").html("");
        let ret = "fail";
        const dateArray = date.split("-");

        if (dateArray.length > 0) {
            const year = dateArray[0];
            const month = dateArray[1];
            const day = dateArray[2];

            $('<span class="dateContainer" id="container_'+year+'-'+month+'-'+day+'" style="display: block;height: '+height+'px;"></span>').insertAfter($("#amp_" + attachToId))
            ret = timelineSettings.success;
        }

        return ret
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
                                let internalHtml = "";

                                let idCheck = "undated";
                                if (metadataList[0]["year"] === null ||
                                    metadataList[0]["month"] === null ||
                                    metadataList[0]["day"] === null)
                                {
                                    idCheck = metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day;
                                }

                                html += TimelineTemplates.TimelinePreLoadGalleryHeader({metadata:metadataList[0]});
                                internalHtml += '<br id="br'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'">' +
                                    '<section class="scrollspy" id="'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'"><p><strong class="dateHeading p-1">'+Util.getDateString(metadataList[0].year, metadataList[0].month, metadataList[0].day)+'</strong></p></section>' +
                                    '<div class="row image-group-padding" id="row'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'">' +
                                    '<span style="display: none;" class="yearTaken">'+metadataList[0].year+'</span>' +
                                    '<span style="display: none;" class="monthTaken">'+metadataList[0].month+'</span>' +
                                    '<span style="display: none;" class="dayTaken">'+metadataList[0].day+'</span>';

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

                                        let loopedHtml = TimelineTemplates.TimelinePreLoadGalleryBody({metadata:metadata});
                                        html += loopedHtml;
                                        internalHtml += loopedHtml;

                                        $("#timelineModalEdit" + metadata.id).attr("tag", metadata.id);
                                    }

                                    const lastDateParts = $("#offcanvasTocBody div a").last().attr("id").split("offcanvas_");
                                    const lastDate = lastDateParts[1];

                                    html += TimelineTemplates.TimelinePreLoadGalleryFooter({metadata:metadataList[0],lastDate:lastDate});
                                    internalHtml += '<span class="scrollspy metadataprocessed" id="tail_'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'"></span></div>';

                                    const tempScrollTop = $("#container").scrollTop();

                                    let htmlEl = $(html);

                                    if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                        if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                                            $("#infinite-scroll-gallery").css('visibility', 'hidden');
                                        }
                                    }

                                    setTimeout(function () {
                                        if (action === "above") {
                                            htmlEl.insertBefore($("#container_" + attachToId)).ready(function () {
                                                // deferred.resolve(timelineSettings.success);
                                                ret = timelineSettings.success;
                                                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                    if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                                                        $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                        $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                    }
                                                }
                                            });
                                        } else if (action === "emptyContainer") {
                                            $("#container_"+date).removeAttr("style");
                                            $("#container_"+date).html(internalHtml);
                                            $('<span class="attachMetadataPhotos" id="amp_'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'" style="visibility: hidden"></span>').insertAfter($("#container_"+date));
                                        } else if (action === "new") {
                                            $("#infinite-scroll-gallery").prepend(htmlEl).ready(function () {
                                                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                    if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                                                        $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                        $("#infinite-scroll-gallery").css('visibility', 'visible');
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
                                                            if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                                                                $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                                $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                            }
                                                        }
                                                        // deferred.resolve(timelineSettings.success);
                                                        ret = timelineSettings.success;
                                                    });
                                                } else {
                                                    $("#infinite-scroll-gallery").prepend(htmlEl).ready(function () {
                                                        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                            if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                                                                $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                                $("#infinite-scroll-gallery").css('visibility', 'visible');
                                                            }
                                                        }
                                                        // deferred.resolve(timelineSettings.success);
                                                        ret = timelineSettings.success;
                                                    });
                                                }
                                            } else {
                                                htmlEl.insertAfter($("#amp_" + attachToId)).ready(function () {
                                                    if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                                        if (Util.isSafari() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                                                            $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                                            $("#infinite-scroll-gallery").css('visibility', 'visible');
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
                                    ret = timelineSettings.success;
                                }
                            } else {
                                $(".attachMetadataPhotos").last().text("EOL").css("display", "none")
                                ret = timelineSettings.success;
                            }
                            ret = timelineSettings.success;
                        }
                        ret = timelineSettings.success;
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        $("#msgTimeline").html(message);
                        ret = "fail";
                    }
                }

                //$("#placeholder").remove();

                return ret;
            });
    }

    timelineSettings.activateMetadataListeners = function(metadataId) {
        Util.activateMetadataListeners(metadataId);

        shashin.updateFavorites("#favorite","#bricon","#briconcount",metadataId);
    }

    timelineSettings.refreshTimeline = async function (mediaTypeFilter) {
        const http = new Http("refreshing timeline TOC");
        const data = await http.ajax("get", "/api/v1/timeline/dates/"+mediaTypeFilter);

        if (data.hasOwnProperty("metadataDates")) {
            const metadataDates = data["metadataDates"];
            timelineSettings.timelineDates = metadataDates;

            // Rebuild slider
            if (Util.isMobile() === false) {
                $("#dateSlider").empty();
                $("#dateSlider").show();
                timelineSettings.initializeTimelineSlider(mediaTypeFilter);
            }

            // Clear offcanvas TOC and rebuild
            $("#timelineTocToggle").show();
            $("#offcanvasTocBody").empty();

            let html = "";

            for (let index = 0; index < metadataDates.length; index++) {
                const metadataDate = metadataDates[index];
                const year = metadataDate["year"];
                const month = metadataDate["month"];
                const day = metadataDate["day"];

                html += TimelineTemplates.TimelineToc({index:index,mediaTypeFilter:mediaTypeFilter,metadataDates:metadataDates,year:year,month:month,day:day});
            }

            $("#offcanvasTocBody").append(html);

            if (Util.isMobile() === false) {
                setTimeout(function() {
                    $("#timelineTocToggle").hide();
                },0);
            }
        }
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