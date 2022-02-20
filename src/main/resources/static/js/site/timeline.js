(function( timelineSettings, $, undefined ) {
    timelineSettings.ScrollDirection = Object.freeze({"up":1, "down":0})
    timelineSettings.enableScrollSpy = true;
    timelineSettings.prevAnchor = "";
    timelineSettings.successBelowMsg = "success_below";
    timelineSettings.successAboveMsg = "success_above";
    timelineSettings.successMidMsg = "success_mid";
    timelineSettings.success = "success";
    timelineSettings.currentScrollTop = 0;
    timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
    timelineSettings.initialized = false;
    timelineSettings.timelineDates = [];

    let isOverlap = function (div1, div2) {
        if (div1.length > 0 && div2.length > 0) {
            const x1 = div1.offset().left;
            const y1 = div1.offset().top;
            const h1 = div1.outerHeight(true);
            const w1 = div1.outerWidth(true);
            const b1 = y1 + h1;
            const r1 = x1 + w1;
            const x2 = div2.offset().left;
            const y2 = div2.offset().top;
            const h2 = div2.outerHeight(true);
            const w2 = div2.outerWidth(true);
            const b2 = y2 + h2;
            const r2 = x2 + w2;

            return !(b1 < y2 || y1 > b2 || r1 < x2 || x1 > r2);

        } else {
            return false;
        }
    }

    timelineSettings.init = function(mediaTypeFilter, metadataDates) {
        timelineSettings.timelineDates = metadataDates;

        shashin.setLightGalleryElement('infinite-scroll-gallery');
        shashin.setLightGallery({"selector":".mediaLink",plugins:[lgMetadataDetail],metadataDetail:true,metadataDetailFunc:shashin.openInfoSidebar});

        let topScroll = true;
        let topOfPage = true;
        let scrollTimer;

        // Initialize
        if (Util.isMobile() === false) {
            timelineSettings.initializeTimelineSlider(mediaTypeFilter);
        } else {
            $("#timelineTocToggle").show();
        }

        if ($('.scrollspy').length > 0) {
            const firstElem = $('.scrollspy')[0];
            const elementsInViewport = $(".scrollspy").withinviewport();

            timelineSettings.attachAssociatedMetadata(firstElem.id, mediaTypeFilter);
            timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);
            timelineSettings.setScrollSpyActive($(firstElem));
            timelineSettings.reinitLightGalleryInstance();
        } else {
            timelineSettings.enableScrollSpy = false;
        }

        $('[data-bs-toggle="tooltip"]').tooltip();

        $(window).bind("scrollStop", function() {
            if ($("#dateSliderWrapper:not(:hover)").length === 1) {
                $("#dateSlider").hide();
            }
            timelineSettings.reinitLightGalleryInstance();
        });

        // Scroll event handler
        const scrollHandler = function (e) {
            let st = $(e.target).scrollTop();

            if (st === 0) {
                topScroll = true;
            }

            $("#dateSlider").show();

            // Hack to prevent infinite scroll upwards and throttle scrolling
            if (topScroll === true && topOfPage === false) {
                document.getElementById("container").scrollBy({top: 1});
                if (document.getElementsByTagName("MAIN").length > 0) {
                    document.getElementsByTagName("MAIN")[0].scrollBy({top: 1});
                }
            }

            const firstDate = $("#offcanvasTocBody div").children().first().attr("id").split("offcanvas_")[1];
            const elementsInViewport = $(".scrollspy").withinviewport()
            topOfPage = $(elementsInViewport[0]).attr("id") === firstDate;

            // Scroll to the timeline TOC
            if (typeof $("#offcanvasToc").css('visibility') !== 'undefined' && $("#offcanvasToc").css('visibility') === "visible" && timelineSettings.enableScrollSpy === true) {
                timelineSettings.scrollToTimelineToc(elementsInViewport);
            }

            if (timelineSettings.enableScrollSpy === true) {
                topScroll = false;
                timelineSettings.renderThumbnailsInViewport(elementsInViewport, mediaTypeFilter);

                $(".bi-play-circle").css("visibility", "hidden");
                $(".bi-play-btn").css("visibility", "hidden");
                $(".mediaLink").unbind('click');
                clearTimeout(scrollTimer);
                scrollTimer = setTimeout(function() {
                    $(window).trigger("scrollStop");
                }, 1000);
            }
        };
        $("#container").on('scroll', scrollHandler);

        $("#offcanvasToc").on('show.bs.offcanvas', function () {
            if (timelineSettings.enableScrollSpy === true) {
                timelineSettings.scrollToTimelineToc($(".scrollspy").withinviewport());
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

    let prevElements = null;
    timelineSettings.renderThumbnailsInViewport = function (elements,mediaTypeFilter) {
        const timelineDates = timelineSettings.timelineDates;
        const lastDate = timelineDates[timelineDates.length-1].year + "-" + timelineDates[timelineDates.length-1].month + "-" + timelineDates[timelineDates.length-1].day;

        if (prevElements === null || (elements.length > 0 && Util.arraysEqual(elements, prevElements) === false) || ($("#"+lastDate).withinviewport().length === 0 && $("footer").withinviewport().length > 0 && Util.atEndOfPage($("#container")[0]))) {

            if (elements.length === 0) {
                const thumbnailsInViewport = $(".photo-thumbnail-container").withinviewport();
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
                        timelineSettings.renderThumbnails(id, mediaTypeFilter).then(function (msg) {
                            if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                                timelineSettings.setScrollSpyActive(id);
                            }
                        });
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

            if (Util.isSafari() === false && Util.isFirefox() === false) {
                timelineSettings.renderThumbnailsSimple(elements, mediaTypeFilter, timelineDates).then(function (msg) {
                    if (msg === timelineSettings.success) {
                        // Set TOC active element
                        const elementsInViewport = $(".scrollspy").withinviewport();
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
            }

            prevElements = elements;
        }
    }

    timelineSettings.renderThumbnailsSimple = async function(elements,mediaTypeFilter,timelineDates) {
        timelineSettings.enableScrollSpy = false;

        $("#spinner_bottom").css("display", "block");

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
        $('section').each(function (index, element) {
            if ($("#" + element.id).withinviewport().length === 0 &&
                $("#br" + element.id).withinviewport().length === 0 &&
                $("#row" + element.id).withinviewport().length === 0 &&
                $("#amp_" + element.id).withinviewport().length === 0 &&
                $("#tail_" + element.id).withinviewport().length === 0 &&
                $(".photo-thumbnail-image.thumbnailTag_" + element.id).withinviewport().length === 0 &&
                (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down ||
                (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up && element.id !== ignoreTimelineDate))
            ) {
                Util.removeDateGallery(element.id);
            }
        });

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
                        if ($("#" + currentDate).withinviewport().length === 0) {
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

                if (Util.getDateObject(prevDate) < Util.getDateObject(currentDate)) {
                    if ($("#" + currentDate).length === 0) {
                        // Render currentDate
                        const anchorPoint = timelineDates[index - 2].year + "-" + timelineDates[index - 2].month + "-" + timelineDates[index - 2].day;
                        const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, "below", anchorPoint);
                        if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                            timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                        }

                        // Break if top not in viewport
                        if ($("#" + currentDate).withinviewport().length === 0) {
                            // Mobile Chrome browsers prevents further rendering
                            if (Util.isMobile() === false || Util.isChrome() === false) {
                                Util.removeDateGallery(currentDate);
                            }
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

    // Render only what's needed
    timelineSettings.renderThumbnails = async function(id,mediaTypeFilter) {
        if (timelineSettings.initialized === false) {
            timelineSettings.initialized = true;
        } else {
            $("#spinner_top").css("display", "block");
        }
        $("#spinner_bottom").css("display", "block");
        timelineSettings.enableScrollSpy = false;
        //let deferred = new $.Deferred();

        // Depth of results in section of page above and below anchor
        // Dynamic depending on current number of results on page
        const idsInView = $(".scrollspy").withinviewport().map(function() {
            let id = $(this).attr('id');
            if (id.indexOf("tail_") > -1) {
                id = id.split("tail_")[1];
            }
            return id;
        }).get().filter(
            function(a){if (!this[a]) {this[a] = 1; return a;}},
            {}
        );

        let depth = (Util.isSafari() === true || Util.isFirefox() === true) ? 5 : (idsInView.length < 3 ? 3 : idsInView.length);
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
                    const dateParts = $(this).attr("id").split("offcanvas_");
                    const date = dateParts[1];
                    if (Util.getDateObject(offCanvasDate) < Util.getDateObject(date)) {
                        attachAboveArray.unshift(date);
                        if (dateCount >= depthUp) {
                            innerLoopBreak = true;
                            return false;
                        }
                        dateCount++;
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
                    const dateParts = $(this).attr("id").split("offcanvas_");
                    const date = dateParts[1];
                    if (Util.getDateObject(date) < Util.getDateObject(offCanvasDate)) {
                        attachBelowArray.push(date);
                        if (dateCount > depthUp) {
                            innerLoopBreak = true;
                            return false;
                        }
                        dateCount++;
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

        // Smooth scrolling when element is removed for non chrome browsers
        if ((Util.isSafari() === true || Util.isFirefox() === true) && timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down && topHeight > 0) {
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

        if (Util.isSafari() === false) {
            let rendered = false;
            while (true) {
                let dateFound = false;
                let currentId = attachPoint;
                $("#offcanvasTocBody").children().each(function () {
                    if ($(this).attr('class') === 'list-group') {
                        $(this).children().each(function () {
                            const dateParts = $(this).attr("id").split("offcanvas_");
                            const date = dateParts[1];
                            if ($(this).next().length > 0 && currentId === date) {
                                currentId = $(this).next().attr("id").split("offcanvas_")[1];
                                dateFound = true;
                                return false;
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

    timelineSettings.initializeTimelineSlider = async function (mediaTypeFilter) {
        const dateList = timelineSettings.timelineDates;
        if (dateList.length > 0) {
            // Tooltip for handle
            const handleTooltip = $('<span class="badge bg-secondary" id="tooltip" style="background-color: slategray" />').css({
                position: 'absolute',
                right: 17,
                zIndex: 2000
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
                        } else if (prevDateObj) {
                            handleTooltip.text(Util.getShortMonths(prevDateObj.month - 1) + ' ' + prevDateObj.day + ', ' + prevDateObj.year);
                        }
                    }
                },
                stop: function (event, ui) {
                    const currentDateObj = dateList[Math.round((dateList.length - 1) - ui.value)];

                    if (currentDateObj && timelineSettings.enableScrollSpy === true) {
                        timelineSettings.jumpFromTimelineToc(event, currentDateObj.year + '-' + currentDateObj.month + '-' + currentDateObj.day, mediaTypeFilter);
                    }
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
            // }, function () {
            //     handleTooltip.hide();
            });

            // Render ticks
            let prevEl = null;
            let prevTickEl = null;
            for (let i = 0; i < dateList.length; i++) {
                const timelineDateObj = dateList[i];
                const tickTopMargin = (i / dateList.length * 100)-0.5;

                if (timelineDateObj) {
                    const dateObj = new Date(timelineDateObj.month + "/" + timelineDateObj.day + "/" + timelineDateObj.year)
                    if (i === 0 || i > 0 && dateList[i - 1].year !== timelineDateObj.year) {
                        // Label for year
                        const el = $('<span class="badge rounded-pill bg-secondary" id="sliderLabel' + dateObj.getFullYear() + '" style="background-color: slategray">' + dateObj.getFullYear() + '</span>').css({
                            'width': '35px',
                            'right': '15px',
                            'position': 'absolute',
                            'top': tickTopMargin + '%'
                        });

                        $("#dateSlider").append(el);
                        setTimeout(function() {
                            if (prevEl !== null && isOverlap($("#" + prevEl.attr("id")), $("#" + el.attr("id"))) === true) {
                                $("#" + el.attr("id")).hide();

                                if (prevTickEl !== null && isOverlap($("#" + prevTickEl.attr("id")), $("#" + el.attr("id"))) === true) {
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
                            'top': tickTopMargin + '%'
                        });

                        $("#dateSlider").append(tickEl);
                        setTimeout(function() {
                            if (prevTickEl !== null && isOverlap($("#" + prevEl.attr("id")), $("#" + tickEl.attr("id"))) === true) {
                                $("#" + tickEl.attr("id")).hide();
                            } else {
                                prevTickEl = tickEl;
                            }
                        },0);
                    }

                    // Tooltip for month/year on slider
                    const sliderTooltip = $('<span class="badge bg-secondary" style="background-color: slategray" />').css({
                        position: 'absolute',
                        right: 15,
                        zIndex: 2000
                    }).hide();

                    sliderTooltip.text(Util.getShortMonths(timelineDateObj.month - 1) + ' ' + timelineDateObj.year);

                    const sliderEl = $('<span data-slider-id="' + timelineDateObj.year + '-' + timelineDateObj.month + '">&nbsp;</span>').css({
                        'width': '73px',
                        'right': '0px',
                        'margin-right': '-3px',
                        'cursor': 'default',
                        // 'background-color': 'grey',
                        'position': 'absolute',
                        'top': tickTopMargin + '%'
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

            setTimeout(function() {
                $("#dateSlider").hide();
            },0);
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
                    if ($("#" + currentDate).withinviewport().length === 0) {
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

                    document.getElementById("container").scrollBy({top: 1});
                    if (document.getElementsByTagName("MAIN").length > 0) {
                        document.getElementsByTagName("MAIN")[0].scrollBy({top: 1});
                    }

                    // Break if top not in viewport
                    if ($("#" + currentDate).withinviewport().length === 0) {
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
            timelineSettings.scrollToTimelineToc($(".scrollspy").withinviewport());
            timelineSettings.renderThumbnailsSimple($(".scrollspy").withinviewport(), mediaTypeFilter, timelineDates).then(function () {
                if ($("#" + firstDate).withinviewport().length > 0 ||
                    $("#br" + firstDate).withinviewport().length > 0 ||
                    $("#row" + firstDate).withinviewport().length > 0) {
                    document.getElementById("container").scrollBy({top: 1});
                    if (document.getElementsByTagName("MAIN").length > 0) {
                        document.getElementsByTagName("MAIN")[0].scrollBy({top: 1});
                    }
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
        const ajaxParams = {
            type: 'get',
            url: "/timeline/mediatype/"+mediaTypeFilter+"/date/"+date,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        $.ajax(ajaxParams)
            .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " attaching associated metadata")}).then(function(data) {
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

                                if ($("#image" + metadata.id).withinviewport()) {

                                    let dateReformatted = "";
                                    if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
                                        const dateObj = new Date(metadata.year, metadata.month - 1, metadata.day);
                                        dateReformatted = dateObj.format("ddd, mmm dd, yyyy");
                                    }

                                    if ($("#tnbr" + metadata.id + ".thumbnail-br").length === 0) {
                                        $("#tnbr" + metadata.id).addClass("thumbnail-br");
                                    }
                                    let html = '<a href="#" id="favorite' + metadata.id + '" class="text-decoration-none">\n' +
                                        '       <span class="overlayIconBackground">\n' +
                                        '           <span id="briconcount' + metadata.id + '"></span> <span id="bricon' + metadata.id + '" class="overlayIcon"></span>\n' +
                                        '       </span>\n' +
                                        '   </a>';
                                    if ($("#favorite" + metadata.id).length === 0) {
                                        $("#tnbr" + metadata.id).append(html);
                                        const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["favorite"] === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                                        const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["count"] > 0 ? favoritesMap[metadata.id]["count"] : 0;
                                        $("#bricon" + metadata.id).addClass(favoriteIcon);
                                        $("#briconcount" + metadata.id).text(favoriteCount);
                                    }

                                    if ($("#image" + metadata.id).length === 1) {
                                        $("#image" + metadata.id).attr("src", encodeURI(metadata.thumbnailUrlSmall));
                                        $("#image" + metadata.id).css("background-color", "lightgray");
                                        $("#image" + metadata.id).attr("onError", "Util.errorImg(this,\'" + metadata.title + "\',209)");
                                    }

                                    if ($("#tnbl" + metadata.id + ".thumbnail-bl").length === 0) {
                                        $("#tnbl" + metadata.id).addClass("thumbnail-bl");
                                    }

                                    if ($("#tncentered" + metadata.id + ".thumbnail-centered").length === 0) {
                                        $("#tncentered" + metadata.id).addClass("thumbnail-centered");
                                    }

                                    // metadata.keywords = keywordMap.hasOwnProperty(metadata.id) ? keywordMap[metadata.id] : "";

                                    const mediaContent = {};
                                    mediaContent.func = shashin.openInfoSidebar;
                                    mediaContent.args = metadata.id;
                                    mediaContent.thumb = encodeURI(metadata.thumbnailUrlSmall);
                                    //mediaContent.subHtml = (metadata.placeName !== null ? '<a href="/map?lat=' + metadata.lat + '&lng=' + metadata.lng + '" target="_blank">' + metadata.placeName + '</a><br>' : '<br>') + metadata.title + (metadata.year === null || metadata.month === null || metadata.day === null ? '' : ' taken on ' + dateReformatted);
                                    if (metadata.type.indexOf("video") >= 0) {
                                        mediaContent.video = '{"source": [{"src":"' + encodeURI(metadata.videoUrl) + '", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}';
                                        mediaContent.downloadUrl = encodeURI(metadata.videoUrl) + "/download";
                                        html =
                                            '<a class="mediaLink" id="mediaLink' + metadata.id + '" ' +
                                            'data-download-url="' + encodeURI(metadata.videoUrl) + '/download" ' +
                                            'data-metadataid="' + metadata.id + '" ' +
                                            'data-video="' + Util.encodeHtml(mediaContent.video) + '" ';
                                        if (metadata.description != null) {
                                            html +=
                                            'data-sub-html="'+metadata.description+'" ';
                                        }
                                        if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
                                            metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) {
                                            html +=
                                                'data-lg-size="' + metadata.thumbnailSmallWidth + '-' + metadata.thumbnailSmallHeight + '-' + metadata.thumbnailSmallWidth + ',' + metadata.originalImageWidth + '-' + metadata.originalImageHeight + '" ' +
                                                'data-responsive="' + encodeURI(metadata.thumbnailUrlSmall) + ' ' + metadata.thumbnailSmallWidth + '" ' +
                                                'data-thumb="' + encodeURI(metadata.thumbnailUrlSmall) + '" ' +
                                                'data-width="' + metadata.originalImageWidth + '"';
                                        }
                                        html +=
                                            '>' +
                                            '<span class="bi-play-btn" style="font-size: 4rem;color: lightgray;"></span>' +
                                            '</a>';

                                    } else {
                                        mediaContent.src = metadata.thumbnailUrlOriginal;
                                        mediaContent.downloadUrl = encodeURI(metadata.thumbnailUrlOriginal);
                                        html =
                                            '<a class="mediaLink" id="mediaLink' + metadata.id + '" ' +
                                            'data-download-url="' + encodeURI(metadata.thumbnailUrlOriginal) + '" ' +
                                            'data-metadataid="' + metadata.id + '" ' +
                                            'data-src="' + encodeURI(metadata.thumbnailUrlOriginal) + '" ';
                                        if (metadata.description != null) {
                                            html +=
                                            'data-sub-html="'+metadata.description+'" ';
                                        }
                                        if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
                                            metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) {
                                            html +=
                                                'data-lg-size="' + metadata.thumbnailSmallWidth + '-' + metadata.thumbnailSmallHeight + '-' + metadata.thumbnailSmallWidth + ',' + metadata.originalImageWidth + '-' + metadata.originalImageHeight + '" ' +
                                                'data-responsive="' + encodeURI(metadata.thumbnailUrlSmall) + ' ' + metadata.thumbnailSmallWidth + '" ' +
                                                'data-thumb="' + encodeURI(metadata.thumbnailUrlSmall) + '" ' +
                                                'data-width="' + metadata.originalImageWidth + '"';
                                        }
                                        html +=
                                            '>' +
                                            '<span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>' +
                                            '</a>';
                                    }
                                    if (metadata.originalImageWidth !== null) {
                                        mediaContent.width = metadata.originalImageWidth;
                                    }
                                    if ($("#mediaLink" + metadata.id).length === 0) {
                                        $("#tncentered" + metadata.id).append(html);
                                    }

                                    const editIcon = (metadata.lat === null || metadata.lng === null) ? "bi-pencil-square" : "bi-pencil";
                                    html = '<a href="#" id="timelineModalEdit' + metadata.id + '" data-bs-target="#propTimelinModal"><span class="' + editIcon + '" style="font-size: 1rem;color: lightgray;"></span></a>';
                                    if ($("#timelineModalEdit" + metadata.id).length === 0) {
                                        $("#tnbl" + metadata.id).append(html);
                                        $("#timelineModalEdit" + metadata.id).attr("tag", metadata.id);
                                        $("#timelineModalEdit" + metadata.id).click(function (e) {
                                            e.preventDefault();
                                            shashin.openEditMetadataModal(metadata.id)
                                        });
                                    }

                                    html = '<a href="#" id="select' + metadata.id + '"><span id="tlicon' + metadata.id + '" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span></a>';
                                    if ($("#select" + metadata.id).length === 0) {
                                        $("#tntl" + metadata.id).append(html);
                                    }
                                    if ($("#tntl" + metadata.id + ".thumbnail-tl").length === 0) {
                                        $("#tntl" + metadata.id).addClass("thumbnail-tl");
                                        shashin.setPhotoOverlays(metadata, "timeline")
                                        timelineSettings.activateMetadataListeners(metadata);
                                    }

                                    if (metadata.type.indexOf("video") >= 0) {
                                        const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                        html = '<span class="overlayIconBackground">' + duration + '&nbsp;<span id="video' + metadata.id + '" class="bi-camera-video overlayIcon"></span></span>';
                                        if ($("#video" + metadata.id).length === 0) {
                                            $("#tntr" + metadata.id).append(html);
                                        }
                                        if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                                            $("#tntr" + metadata.id).addClass("thumbnail-tr");
                                        }
                                    } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight * 2) {
                                        html = '<span id="panorama' + metadata.id + '" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>';
                                        if ($("#panorama" + metadata.id).length === 0) {
                                            $("#tntr" + metadata.id).append(html);
                                        }
                                        if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                                            $("#tntr" + metadata.id).addClass("thumbnail-tr");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    timelineSettings.updateTimeline = async function(date,mediaTypeFilter,action,attachToId) {
        $("#msgTimeline").html("");

        const ajaxParams = {
            type: 'get',
            url: "/timeline/mediatype/" + mediaTypeFilter + "/date/" + date + "/metadata",
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        //const promise =
        return await $.ajax(ajaxParams)
            .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating timeline")}).then(function (data) {
                // let deferred = new $.Deferred();
                let ret = "fail";
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let message = "Error";
                    if (data["status"] === timelineSettings.success) {
                        if (data.hasOwnProperty("metadataList")) {
                            const metadataList = data["metadataList"];

                            if (metadataList.length > 0) {
                                let html = "";

                                let dateString = Util.getDateString(metadataList[0]["year"], metadataList[0]["month"], metadataList[0]["day"]);

                                let idCheck = "undated";
                                if (metadataList[0]["year"] === null ||
                                    metadataList[0]["month"] === null ||
                                    metadataList[0]["day"] === null)
                                {
                                    html += '<span class="dateContainer" id="container_undated">\n' +
                                        '<br id="brundated"><section class="scrollspy" id="undated"><p><strong class="undatedTimelinePhotos p-1">Undated</strong></p></section>\n' +
                                        '<div class="row p-3" id="rowundated">\n';
                                } else {
                                    idCheck = metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day;
                                    html += '<span class="dateContainer" id="container_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '">\n' +
                                        '<br id="br' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"><section class="scrollspy" id="' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"><p><strong class="dateHeading p-1">' + dateString + '</strong></p></section>\n' +
                                        '<div class="row p-3" id="row' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '">\n' +
                                        '<span style="display: none;" class="yearTaken">' + metadataList[0]["year"] + '</span>\n' +
                                        '<span style="display: none;" class="monthTaken">' + metadataList[0]["month"] + '</span>\n' +
                                        '<span style="display: none;" class="dayTaken">' + metadataList[0]["day"] + '</span>\n';
                                }

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

                                        html += '<div id="photoThumbnailContainer' + metadata.id + '" class="photo-thumbnail-container photo-thumbnail ' + (metadata.type.includes('video') ? 'is-video' : 'is-not-video') + '" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n';
                                        html += '   <a class="lightGalleryIndexAnchor" id="lightGalleryIndex' + metadata.id + '"></a>\n'
                                        html +=
                                            '       <input type="hidden" name="filename' + metadata.id + '" id="filename' + metadata.id + '" value="' + metadata.fileName + '">\n' +
                                            '       <input type="hidden" name="thumbnailCentered' + metadata.id + '" id="thumbnailCentered' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlCentered) + '">\n';

                                        if (metadata.year == null || metadata.month == null || metadata.day == null) {
                                            html +=
                                                '   <input type="hidden" name="thumbnailUrl-undated[]" id="thumbnailUrl_' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlSmall) + '">\n' +
                                                '   <img loading="lazy" class="photo-thumbnail-image thumbnailTag_undated" id="image'+metadata.id+'" width="'+metadata.thumbnailSmallWidth+'" height="'+metadata.thumbnailSmallHeight+'">\n';

                                        } else {
                                            html +=
                                                '   <input type="hidden" name="thumbnailUrl-' + metadata.year + '-' + metadata.month + '-' + metadata.day + '[]" id="thumbnailUrl_' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlSmall) + '">\n' +
                                                '   <img loading="lazy" class="photo-thumbnail-image thumbnailTag_'+metadata.year + '-' + metadata.month + '-' + metadata.day+'" id="image'+metadata.id+'" width="'+metadata.thumbnailSmallWidth+'" height="'+metadata.thumbnailSmallHeight+'">\n';
                                        }

                                        html += '   <div id="tntl' + metadata.id + '"></div>\n' +
                                            '       <div id="tnbr' + metadata.id + '"></div>\n' +
                                            '       <div id="tnbl' + metadata.id + '"></div>\n' +
                                            '       <div id="tntr' + metadata.id + '"></div>\n' +
                                            '       <div id="tncentered' + metadata.id + '"></div>\n';

                                        html += '   <span id="timelinemodal' + metadata.id + '"></span>' +
                                            '   </div>\n';

                                        $("#timelineModalEdit" + metadata.id).attr("tag", JSON.stringify(metadata));
                                    }

                                    const lastDateParts = $("#offcanvasTocBody div").children().last().attr("id").split("offcanvas_");
                                    const lastDate = lastDateParts[1];

                                    if (metadataList[0].year == null || metadataList[0].month == null || metadataList[0].day == null) {
                                        html += '<span class="scrollspy metadataprocessed" id="tail_undated"></span>';
                                        html += '</div></span><span class="attachMetadataPhotos" id="amp_undated" style="visibility: hidden">EOL</span>';
                                    } else if (lastDate === (metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day)) {
                                        html += '<span class="scrollspy metadataprocessed" id="tail_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                        html += '</div></span><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '" style="visibility: hidden">EOL</span>';
                                    } else {
                                        html += '<span class="scrollspy metadataprocessed" id="tail_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                        html += '</div></span><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                    }

                                    const tempScrollTop = $("#container").scrollTop();

                                    let htmlEl = $(html);

                                    if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                        if (Util.isSafari() === true) {
                                            $("#infinite-scroll-gallery").css('visibility', 'hidden');
                                        } else if (Util.isFirefox() === true) {
                                            htmlEl.hide();
                                        }
                                    }

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

                //deferred.resolve(timelineSettings.success);
                // return deferred.promise();
                return ret;
            });

        // return promise.done(function(data) {
        //     return data;
        // });
    }

    timelineSettings.activateMetadataListeners = function(metadata) {
        Util.activateMetadataListeners(metadata);

        $("#favorite"+metadata.id).click(function (e) {
            e.preventDefault();

            const metadataId = metadata.id;

            if ($("#bricon" + metadataId).hasClass("bi-suit-heart")) {
                $("#bricon" + metadataId).removeClass("bi-suit-heart").addClass("bi-suit-heart-fill");
            } else if ($("#bricon" + metadataId).hasClass("bi-suit-heart-fill")) {
                $("#bricon" + metadataId).removeClass("bi-suit-heart-fill").addClass("bi-suit-heart");
            }

            const isFavorite = ($("#bricon" + metadataId).hasClass("bi-suit-heart-fill"));

            const json = {metadataId: metadataId, isFavorite: isFavorite};

            let posting;

            if (isFavorite === true) {
                posting = $.post({
                    url: "/favorite/save",
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8'
                });
            } else {
                posting = $.post({
                    url: "/favorite/delete",
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8'
                });
            }

            posting.done(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let currentCount = parseInt($("#briconcount"+metadata.id).text());
                    if (isFavorite === true) {
                        currentCount++;
                    } else {
                        currentCount--;
                        if (currentCount < 0) {
                            currentCount = 0;
                        }
                    }
                    $("#briconcount"+metadata.id).text(currentCount)
                }
            });
        });
    }

}( window.timelineSettings = window.timelineSettings || {}, jQuery ));

// Hack to close TOC canvas
$(document).click(function(event) {
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