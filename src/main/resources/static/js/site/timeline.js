(function( timelineSettings, $, undefined ) {
    function preloadAdjacentSections(anchor, mediaTypeFilter) {
        const index = timelineSettings.timelineDatesHash[anchor];
        const preloadDepth = 2;

        for (let i = 1; i <= preloadDepth; i++) {
            const aboveIndex = index - i;
            const belowIndex = index + i;

            if (aboveIndex >= 0) {
                const aboveDate = timelineSettings.timelineDates[aboveIndex];
                const aboveId = `${aboveDate.year}-${aboveDate.month}-${aboveDate.day}`;
                if ($("#" + aboveId).length === 0) {
                    timelineSettings.updateTimeline(aboveId, mediaTypeFilter, "above", anchor).then(function(msg) {
                        if (msg === timelineSettings.success) {
                            timelineSettings.attachAssociatedMetadata(aboveId, mediaTypeFilter).then(() => {}).catch(error => {
                                shashin.printMessageToConsole("Metadata attachment failed:" + error,{tag:"timeline"});
                            });
                        }
                    });
                }
            }

            if (belowIndex < timelineSettings.timelineDates.length) {
                const belowDate = timelineSettings.timelineDates[belowIndex];
                const belowId = `${belowDate.year}-${belowDate.month}-${belowDate.day}`;
                if ($("#" + belowId).length === 0) {
                    timelineSettings.updateTimeline(belowId, mediaTypeFilter, "below", anchor).then(function(msg) {
                        if (msg === timelineSettings.success) {
                            timelineSettings.attachAssociatedMetadata(belowId, mediaTypeFilter).then(() => {}).catch(error => {
                                shashin.printMessageToConsole("Metadata attachment failed:" + error,{tag:"timeline"});
                            });
                        }
                    });
                }
            }
        }
    }

    timelineSettings.rescanElements = function (preCalculatedElements) {
        setTimeout(() => {
            let elements;
            if (preCalculatedElements !== undefined && preCalculatedElements !== null) {
                elements = preCalculatedElements;
            } else {
                elements = Util.elementsInViewport($('img.photo-thumbnail-image:not([src*="/api/v1/thumbnails/'+timelineSettings.thumbnailType+'/)'));
            }
            $.each(elements, function(index, value) {
                const imageId = $(value).attr('id');
                const imageMetadataId = imageId.substring(5);
                timelineSettings.renderMetadata(imageMetadataId);
            });
            setTimeout(() => {
                if (elements.length > 0 && shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
                    shashin.getLightGallery().refresh();
                }
            }, 1000);
        }, 0);
    };

    timelineSettings.renderMetadata = function(metadataId) {
        if (timelineSettings.dbOperationComplete === true) {
            const query = timelineSettings.db.metadataList.where({id: metadataId});

            shashin.printMessageToConsole("Using dexie to renderMetadata " + metadataId, {tags: ["timelineRenderMetadata","dexie"]});

            query.toArray(function (metadataList) {
                const favoritesMap = timelineSettings.favoritesMap;

                if (metadataList.length > 0) {
                    const metadata = metadataList[0];

                    setTimeout(function () {
                        timelineSettings.renderThumbnailPreviews(metadata, favoritesMap);
                    }, 0);
                }
            });
        } else {
            shashin.printMessageToConsole("Using backend to render metadata " + metadataId, {tag: "timelineRenderMetadata"});

            const imageIdentifier = "#image" + metadataId;
            if ($(imageIdentifier).length > 0 && $(imageIdentifier).src === undefined) {
                const http = new Http("attaching associated metadata in viewport");
                const version = Util.getMetadataLocalStorage();
                http.ajax("get", "/metadata/" + metadataId + (version === "" ? "" : "?v=" + version)).then(function (data) {
                    if (data !== undefined && data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                        const metadata = data.metadata;
                        const favoritesMap = data.favorites;
                        timelineSettings.renderThumbnailPreviews(metadata, favoritesMap);
                    }
                });
            }
        }
    };

    timelineSettings.jumpToLightGalleryMetadata = function (metadataId) {
        const url = location.href;
        location.href = '#lightGalleryIndex'+metadataId;
        history.replaceState(null,null,url);
    };

    let reinitGalleryFlag = true;

    let prevElements = null;
    timelineSettings.renderThumbnailsInViewport = function (elements,mediaTypeFilter) {
        const timelineDates = timelineSettings.timelineDates;
        const lastDate = timelineDates[timelineDates.length-1].year + "-" + timelineDates[timelineDates.length-1].month + "-" + timelineDates[timelineDates.length-1].day;

        if (prevElements === null ||
            (elements.length > 0 && Util.arraysEqual(elements, prevElements) === false && timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) ||
            (elements.length > 0 && timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) ||
            (Util.elementsInViewport($("#"+lastDate)).length === 0 && timelineSettings.closeToFooter() === true && Util.atEndOfPage($("#container")[0]))
        ) {
            $(".bi-play-btn").invisible();
            $(".bi-play-circle").invisible();
            $(".mediaLink").unbind('click');
            reinitGalleryFlag = false;

            if (elements.length === 0) {
                const thumbnailsInViewport = Util.elementsInViewport($(".photo-thumbnail-container"));
                elements = $(thumbnailsInViewport.parent().prevAll(".scrollspy")[0]);
            }

            const prevFirstElement = prevElements !== null ? $(prevElements[0]).attr('id') : prevElements;
            const firstElement = $(elements[0]).attr('id');
            const prevLastElement = prevElements !== null ? $(prevElements[prevElements.length-1]).attr('id') : prevElements;
            const lastElement = $(elements[elements.length-1]).attr('id');

            if (firstElement !== undefined) {
                const prevFirstWithoutTail = (prevElements !== null && prevFirstElement.indexOf("tail_") > -1) ? prevFirstElement.split("tail_")[1] : prevFirstElement;
                const firstWithoutTail = firstElement.indexOf("tail_") > -1 ? firstElement.split("tail_")[1] : firstElement;
                const prevLastWithoutTail = (prevElements !== null && prevLastElement.indexOf("tail_") > -1) ? prevLastElement.split("tail_")[1] : prevLastElement;
                const lastWithoutTail = lastElement.indexOf("tail_") > -1 ? lastElement.split("tail_")[1] : lastElement;
                if (prevElements !== null) {
                    if (Util.isInViewport($("#tail_" + lastDate)) === true) {
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
                        shashin.dayHeadingListener(id, "timeline", mediaTypeFilter);

                        // Scrolling behavior different on Chrome iOS
                        if (Util.isFirefox() === true || Util.isMobile() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                            timelineSettings.renderThumbnailsAlt(id, mediaTypeFilter).then(function (msg) {
                                if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                                    timelineSettings.setScrollSpyActive(id);
                                    // Util.checkErrorImage();
                                }
                            });
                        }

                        // Set the timeline slider while scrolling
                        if (Util.isMobile() === false && $("#dateSlider").length > 0) {
                            timelineDates.forEach(function (timelineDate, i) {
                                if (id === timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day) {
                                    $("#dateSlider").slider("option", "value", timelineDates.length - i - 1);
                                    return false;
                                }
                            });
                        }
                    }
                });

                if (Util.isMobile() === false) {
                    // Scrolling behavior different on Chrome iOS
                    if (((timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up && timelineSettings.isScrolling === false) || timelineSettings.isScrolling === true) && Util.isFirefox() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) {
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
                                // Util.checkErrorImage();
                            }
                        });
                    }

                    prevElements = elements;
                }
            }
        }
    };

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

        shashin.printMessageToConsole("depthDown:"+depthDown,{tag:"timeline"});
        shashin.printMessageToConsole("depthUp:"+depthUp,{tag:"timeline"});
        shashin.printMessageToConsole("renderThumbnails id:"+id,{tag:"timeline"});

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
        let prevIndex = 0;

        $('section').each(function (index, element) {
            shashin.printMessageToConsole(element.id + " checking to remove end",{tag:"timeline"});
            if (($.inArray(element.id, attachAboveArray) === -1 && $.inArray(element.id, attachBelowArray) === -1 && element.id !== id) || ($("#" + element.id).length > 1 || prevElementId === element.id)) {

                // Get height to set scrollTop for non chrome browsers
                if (Util.getDateObject(id) < Util.getDateObject(element.id)) {
                    topHeight += Util.getDateGalleryHeight(element.id);
                }

                shashin.printMessageToConsole(element.id + " removed end",{tag:"timeline"});
                Util.removeDateGallery(element.id);
            }

            // Remove elements out of order
            const currentTimelineIndex = timelineSettings.timelineDatesHash[element.id];

            if (prevIndex > 0 && prevIndex + 1 !== currentTimelineIndex) {
                shashin.printMessageToConsole("Removing from timeline " + element.id,{tag:"timeline"});
                Util.removeDateGallery(element.id);
            }

            prevIndex = currentTimelineIndex;
            prevElementId = element.id;
        });

        // Smooth scrolling when element is removed for non chrome browsers
        if ((Util.isSafari() === true || Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true))
            //&& timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down && topHeight > 0
        ) {
            $("#container").scrollTop(tempScrollTop - topHeight);
        }

        shashin.printMessageToConsole("attachAboveArray",{tag:"timeline"});
        shashin.printMessageToConsole(attachAboveArray,{tag:"timeline"});
        shashin.printMessageToConsole("attachBelowArray",{tag:"timeline"});
        shashin.printMessageToConsole(attachBelowArray,{tag:"timeline"});

        // Render top
        let action = "new";
        let attachPoint = id;
        for (let index in attachAboveArray) {
            const currentId = attachAboveArray[index];
            shashin.printMessageToConsole("attempting to attaching id above:" + currentId,{tag:"timeline"});
            if ($("#" + currentId).length === 0) {
                if (action === "new") {
                    attachPoint = null;
                }
                const wasAtTop = document.getElementById("container").scrollTop === 0;

                const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint);
                if (msg === timelineSettings.success && $("#" + currentId).length === 1) {
                    await timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);

                    // Apply upward scroll nudge if stuck at top
                    if (wasAtTop) {
                        document.getElementById("container").scrollTop = 1;
                    }
                }

                action = "below";
            }
            attachPoint = currentId;
        }

        // Render bottom
        action = "below";
        if (attachAboveArray.length === 0 && $("#"+id).length === 0) {
            attachPoint = null;
        }
        for (let index in attachBelowArray) {
            const currentId = attachBelowArray[index];
            shashin.printMessageToConsole("attempting to attaching id below:" + currentId,{tag:"timeline"});
            if ($("#"+currentId).length === 0) {
                shashin.printMessageToConsole("attaching below attachPoint:" + attachPoint,{tag:"timeline"});
                shashin.printMessageToConsole("attaching id:" + currentId,{tag:"timeline"});
                shashin.printMessageToConsole("actionBelow:"+action,{tag:"timeline"});
                const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint);
                if (msg === timelineSettings.success && $("#"+currentId).length === 1) {
                    await timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
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

                    const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint);
                    if (msg === timelineSettings.success && $("#" + currentId).length === 1) {
                        await timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                    }

                    action = "below";
                    attachPoint = currentId;
                    attachBelowArray.push(currentId);

                    if (rendered === false && $("#amp_" + currentId).withinviewport().length === 0) {
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
        // if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
        action = "new";
        if (attachAboveArray.length > 0) {
            attachPoint = attachAboveArray[attachAboveArray.length - 1];
            action = "below";
        } else if (attachBelowArray.length > 0) {
            attachPoint = attachBelowArray[0];
            action = "above";
        }

        shashin.printMessageToConsole("attempting to attaching id mid " + id + " " + action + " " + attachPoint + " length " + $("#" + id).length,{tag:"timeline"});

        // Hack for attaching mid point
        if (attachAboveArray.length > 0 && attachBelowArray.length > 0 && $('section')[$('section').length - 1].id === id && $("#" + id).length === 1) {
            shashin.printMessageToConsole("removing already existing id " + id + " for mid point",{tag:"timeline"});
            Util.removeDateGallery(id);
        }

        // Render mid
        if ($("#" + id).length === 0) {
            shashin.printMessageToConsole("attaching mid attachPoint:" + attachPoint,{tag:"timeline"});
            shashin.printMessageToConsole("attaching id:" + id,{tag:"timeline"});
            shashin.printMessageToConsole("attaching mid action:" + action,{tag:"timeline"});
            const msg = await timelineSettings.updateTimeline(id, mediaTypeFilter, action, attachPoint);
            if (msg === timelineSettings.success && $("#" + id).length === 1) {
                await timelineSettings.attachAssociatedMetadata(id, mediaTypeFilter);
            }
        }
        // }

        shashin.printMessageToConsole("==============================================",{tag:"timeline"});
        $("#spinner_top").css("display", "none");
        $("#spinner_bottom").css("display", "none");
        timelineSettings.enableScrollSpy = true;

        return timelineSettings.successMidMsg;
    };

    timelineSettings.renderThumbnails = async function(elements,mediaTypeFilter,timelineDates,initiatedFromToc) {
        if (initiatedFromToc === undefined) {
            initiatedFromToc = false;
        }

        timelineSettings.enableScrollSpy = false;

        if ($(".attachMetadataPhotos").last().text() !== "EOL" && $("#spinner_bottom").css("display") === "block") {
            timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
        }

        if ($(".attachMetadataPhotos").last().text() !== "EOL") {
            $("#spinner_bottom").css("display", "block");
        } else if (initiatedFromToc === false && $(".attachMetadataPhotos").last().text() === "EOL" && Util.isInViewport($(".attachMetadataPhotos").last()) === true) {
            timelineSettings.enableScrollSpy = true;

            if (timelineSettings.initialized === false) {
                timelineSettings.initialized = true;
            }

            return timelineSettings.success;
        }

        if (elements.length > 0) {
            let firstElementId = $(elements[0]).attr("id");
            let firstVisibleId = firstElementId.indexOf("tail_") === -1 ? firstElementId : firstElementId.substring(5, firstElementId.length);
            let ignoreTimelineDate = firstVisibleId;

            if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                let startIndex = timelineSettings.timelineDatesHash[firstVisibleId];
                let timelineObjArr = timelineDates.slice().reverse();

                for (let index = startIndex; index < timelineObjArr.length; index++) {
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
            const sectionArray = [];
            let prevIndex = 0;

            section.each(function (index, element) {
                sectionArray.push(element);

                if (((timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up && timelineSettings.isScrolling === false) || timelineSettings.isScrolling === true) &&
                    Util.isInViewport($("#" + element.id)) === false &&
                    Util.isInViewport($("#br" + element.id)) === false &&
                    Util.isInViewport($("#row" + element.id)) === false &&
                    Util.isInViewport($("#tail_" + element.id)) === false &&
                    Util.isInViewport($("#container_" + element.id)) === false &&
                    Util.elementsInViewport($(".photo-thumbnail-image.thumbnailTag_" + element.id)).length === 0
                ) {
                    if (Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                        section.invisible();
                    }

                    if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down &&
                        Util.isInViewport($("#br" + element.id)) === false &&
                        Util.isInViewport($("#row" + element.id)) === false &&
                        Util.isInViewport($("#tail_" + element.id)) === false &&
                        Util.isInViewport($("#container_" + element.id)) === false &&
                        Util.isInViewport($("#amp_" + element.id)) === false
                    ) {
                        // removeHeight += Util.getDateGalleryHeight(element.id);
                        if (Util.isSafari() === false) {
                            Util.removeDateGallery(element.id);
                        }
                        removedElements.push(element.id);
                        sectionArray.pop();
                    } else if (section[index + 1] !== undefined &&
                        Util.isInViewport($("#br" + section[index + 1].id)) === false &&
                        Util.isInViewport($("#row" + section[index + 1].id)) === false &&
                        Util.isInViewport($("#tail_" + section[index + 1].id)) === false &&
                        Util.isInViewport($("#container_" + section[index + 1].id)) === false &&
                        Util.isInViewport($("#amp_" + section[index + 1].id)) === false
                    ) {
                        if (Util.isSafari() === false) {
                            Util.removeDateGallery(element.id);
                        }
                        sectionArray.pop();
                    }
                }

                // Remove elements out of order
                const currentTimelineIndex = timelineSettings.timelineDatesHash[element.id];

                if (prevIndex > 0 && prevIndex + 1 !== currentTimelineIndex) {
                    shashin.printMessageToConsole("Removing from timeline " + element.id,{tag:"timeline"});
                    if (Util.isSafari() === false) {
                        Util.removeDateGallery(element.id);
                    }
                }

                prevIndex = currentTimelineIndex;
            });

            if (Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) {
                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
                    $('#container').scrollTop(tempScrollTop - removeHeight);
                } else if (topHeight > 0) {
                    $('#container').scrollTop(tempScrollTop - topHeight);
                }
            }
            section.visible();

            // Get list of visible elements
            const firstVisibleContainer = sectionArray.length > 0 ? sectionArray[0] : null;
            const lastVisibleContainer = sectionArray.length > 0 ? sectionArray[sectionArray.length - 1] : null;

            if (timelineSettings.isScrolling === true &&
                firstVisibleContainer !== null) {

                // Divide dates into 2
                // TODO: Optimize this further
                let currentDate = $(firstVisibleContainer).attr("id");
                let prevDate = "";
                const firstDate = timelineDates[0].year + "-" + timelineDates[0].month + "-" + timelineDates[0].day;
                const lastDate = timelineDates[timelineDates.length - 1].year + "-" + timelineDates[timelineDates.length - 1].month + "-" + timelineDates[timelineDates.length - 1].day;

                let timelineDateArr = timelineDates;
                const halfwayPoint = Math.floor(timelineDates.length / 2);
                let reversed = false;
                if (Util.getDateObject(currentDate) < Util.getDateObject(timelineDates[halfwayPoint].year + "-" + timelineDates[halfwayPoint].month + "-" + timelineDates[halfwayPoint].day)) {
                    timelineDateArr = timelineDates.slice().reverse();
                    reversed = true;
                }

                if (currentDate === firstDate) {
                    timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
                }

                let startingIndexTop = timelineSettings.timelineDatesHash[$(firstVisibleContainer).attr("id")];
                let startingIndexBottom = timelineSettings.timelineDatesHash[$(lastVisibleContainer).attr("id")];

                if (reversed === true) {
                    startingIndexTop = timelineDateArr.length - startingIndexTop - 1;
                    startingIndexBottom = 0;
                }

                // Render above visibleContainers going from bottom up
                let timelineArr = timelineDates.reverse();
                let lastTopPosition = $("#infinite-scroll-gallery").position().top;
                for (let index = startingIndexTop; index < timelineArr.length; index++) {
                    const timelineDate = timelineArr[index];
                    prevDate = timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day;
                    shashin.dayHeadingListener(prevDate, "timeline", mediaTypeFilter);

                    if (Util.getDateObject(currentDate) < Util.getDateObject(prevDate)) {
                        if ($("#" + currentDate).length === 0 && ((Util.isFirefox() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) ||
                            ((Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) && $.inArray(currentDate, removedElements) === -1))) {
                            // Render currentDate
                            const anchorPoint = timelineDates[index - 2].year + "-" + timelineDates[index - 2].month + "-" + timelineDates[index - 2].day;

                            const currentTopPosition = $("#infinite-scroll-gallery").position().top;

                            const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, "above", anchorPoint);

                            if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                                await timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                            }

                            // Prevent auto scrolling & div flickering
                            if ($("#container").position().top === $("#infinite-scroll-gallery").position().top ||
                                (Util.isMobile() === false && lastTopPosition === currentTopPosition)
                            ) {
                                break;
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
                                await timelineSettings.attachAssociatedMetadata(firstDate, mediaTypeFilter);
                            }
                        }
                    }
                    lastTopPosition = $("#infinite-scroll-gallery").position().top;
                }

                // Render below visibleContainers going from top down
                currentDate = $(lastVisibleContainer).attr("id");
                timelineArr = timelineDates.reverse();

                for (let index = startingIndexBottom; index < timelineArr.length; index++) {
                    const timelineDate = timelineArr[index];
                    let prevDate = timelineDate.year + "-" + timelineDate.month + "-" + timelineDate.day;

                    shashin.dayHeadingListener(prevDate, "timeline", mediaTypeFilter);

                    if (Util.getDateObject(prevDate) < Util.getDateObject(currentDate) && timelineSettings.closeToFooter() === true) {
                        if (timelineSettings.currentScrollDirection ===
                            timelineSettings.ScrollDirection.down && $("#" + currentDate).length === 0 && ((Util.isFirefox() === false && !(Util.getOS() === "iOS" && Util.isChrome() === true)) || ((Util.isFirefox() === true || (Util.getOS() === "iOS" && Util.isChrome() === true)) && $.inArray(currentDate, removedElements) === -1))) {
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
                            // if (Util.isMobile() === false) {
                            shashin.printMessageToConsole("timelineSettings.createEmptyContainer called",{tag:"timeline"});
                            // Stage 1 - create an empty block
                            await timelineSettings.createEmptyContainer(currentDate, anchorPoint, sectionHeight);
                            action = "emptyContainer";
                            // } else {
                            //     action = "below";
                            // }

                            // Stage 2 - network call to create image placeholders and UI skeleton for month
                            const msg = await timelineSettings.updateTimeline(currentDate, mediaTypeFilter, action, anchorPoint);

                            // Stage 3 - network call to embed the image URL and complete the process
                            if (timelineSettings.initialized === false) {
                                if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                                    await timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                                    timelineSettings.distanceToFooter = timelineSettings.calculateDistanceToFooter();
                                }
                            } else {
                                // 1 sec delay for smoother scrolling
                                let toValue = 1000;
                                if (timelineSettings.dbOperationComplete === true) {
                                    toValue = 0;
                                }

                                setTimeout(async() => {
                                    if (msg === timelineSettings.success && $("#" + currentDate).length === 1) {
                                        await timelineSettings.attachAssociatedMetadata(currentDate, mediaTypeFilter);
                                        timelineSettings.distanceToFooter = timelineSettings.calculateDistanceToFooter();
                                    }
                                }, toValue);
                            }

                            timelineSettings.distanceToFooter = timelineSettings.calculateDistanceToFooter();

                            // Break if footer not in viewport
                            if (timelineSettings.closeToFooter() === false) {
                                currentDate = prevDate;
                                break;
                            }

                        }

                        if (prevDate !== lastDate) {
                            currentDate = prevDate;
                        } else {
                            const msg = await timelineSettings.updateTimeline(lastDate, mediaTypeFilter, "below", currentDate);
                            if (msg === timelineSettings.success && $("#" + lastDate).length === 1) {
                                await timelineSettings.attachAssociatedMetadata(lastDate, mediaTypeFilter);
                            }
                        }
                    }
                }
            }
        }

        $("#spinner_top").css("display", "none");
        $("#spinner_bottom").css("display", "none");

        timelineSettings.enableScrollSpy = true;

        if (timelineSettings.initialized === false) {
            timelineSettings.initialized = true;
        }

        return timelineSettings.success;
    };

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
            }).invisible();

            handleTooltip.text(Util.getShortMonths(dateList[0].month - 1, timelineSettings.locale) + ' ' + Util.getYearLocale(dateList[0].year, timelineSettings.locale));

            $("#dateSlider").slider({
                orientation: "vertical",
                value: dateList.length - 1,
                min: 0,
                max: dateList.length - 1,
                step: 0.0001,
                range: false,
                slide: function (event, ui) {
                    timelineSettings.scrollBarIsSliding = true;
                    const currentDateObj = dateList[Math.round((dateList.length - 1) - ui.value)];

                    if (currentDateObj) {
                        const prevDateObj = dateList.length > 1 ? dateList[Math.round((dateList.length - 2) - ui.value)] : currentDateObj;

                        if (
                            timelineSettings.currentScrollDirection ===
                            timelineSettings.ScrollDirection.down
                        ) {
                            handleTooltip.text(Util.getDateString(currentDateObj.year,currentDateObj.month,currentDateObj.day,timelineSettings.locale,false));
                            $(".monthYearSlider").invisible();
                        } else if (prevDateObj) {
                            handleTooltip.text(Util.getDateString(prevDateObj.year,prevDateObj.month,prevDateObj.day,timelineSettings.locale,false));
                            $(".monthYearSlider").invisible();
                        }
                    }
                },
                stop: function (event, ui) {
                    timelineSettings.scrollBarIsSliding = false;
                    const currentDateObj = dateList[Math.round((dateList.length - 1) - ui.value)];

                    if (currentDateObj && timelineSettings.enableScrollSpy === true) {
                        timelineSettings.jumpFromTimelineToc(null, currentDateObj.year + '-' + currentDateObj.month + '-' + currentDateObj.day, mediaTypeFilter);
                        timelineSettings.enableScrollSpy = true;
                    }

                    $(".monthYearSlider").visible();
                },
                change: function (event, ui) {
                    const currentDateObj = dateList[Math.round((dateList.length - 1) - ui.value)];
                    if (currentDateObj) {
                        const prevDateObj = dateList.length > 1 ? dateList[Math.round((dateList.length - 2) - ui.value)] : currentDateObj;

                        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.down) {
                            handleTooltip.text(Util.getDateString(currentDateObj.year,currentDateObj.month,currentDateObj.day,timelineSettings.locale,false));
                        } else if (prevDateObj) {
                            handleTooltip.text(Util.getDateString(prevDateObj.year,prevDateObj.month,prevDateObj.day,timelineSettings.locale,false));
                        }

                        handleTooltip.visible();
                    }
                }
            }).find(".ui-slider-handle").append(handleTooltip).hover(function () {
                handleTooltip.visible();
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
                    const dateObj = new Date(timelineDateObj.month + "/" + timelineDateObj.day + "/" + timelineDateObj.year);
                    if (i === 0 || i === dateList.length-1 || (i < dateList.length && dateList[i + 1].year !== timelineDateObj.year)) {
                        // if ($('#sliderLabel' + dateObj.getFullYear()).length === 0) {
                        // Label for year
                        const el = $('<span class="badge rounded-pill bg-secondary yearLabel" id="sliderLabel' + dateObj.getFullYear() + '" style="background-color: slategray">' + dateObj.getFullYear() + '</span>').css({
                            'width': '35px',
                            'right': '15px',
                            'font-size': 'xx-small',
                            'position': 'absolute',
                            'z-index': '2',
                            'top': (tickTop - sliderOffset) + '%'
                        });

                        $("#dateSlider").append(el);

                        // Hide overlapping year on date slider
                        setTimeout(function () {
                            if (prevEl !== null && Util.isOverlap($("#" + prevEl.attr("id")), $("#" + el.attr("id"))) === true) {
                                $("#" + el.attr("id")).invisible();

                                if (prevTickEl !== null && Util.isOverlap($("#" + prevTickEl.attr("id")), $("#" + el.attr("id"))) === true) {
                                    $("#" + prevTickEl.attr("id")).invisible();
                                }
                            } else {
                                prevEl = el;
                            }
                        }, 0);
                        // }
                    } else if (i > 0 && (dateList[i - 1].year !== timelineDateObj.year || dateList[i - 1].month !== timelineDateObj.month)) {
                        if ($('#tickLabel' + timelineDateObj.year + '-' + timelineDateObj.month).length === 0) {
                            // Tick for month/year
                            const tickEl = $('<span id="tickLabel' + timelineDateObj.year + '-' + timelineDateObj.month + '" style="color: #777777">' + '-' + '</span>').css({
                                'width': '10px',
                                'right': '15px',
                                'position': 'absolute',
                                'z-index': '1',
                                'bottom': '50%',
                                'top': (tickTop - sliderOffset) + '%'
                            });

                            $("#dateSlider").append(tickEl);
                            setTimeout(function () {
                                prevTickEl = tickEl;
                            }, 0);
                        }
                    }

                    // Tooltip for month/year on slider
                    const sliderTooltip = $('<span class="badge bg-secondary" style="background-color: slategray" />').css({
                        position: 'absolute',
                        right: 15,
                        bottom: "50%"
                    }).invisible();

                    sliderTooltip.text(Util.getShortMonths(timelineDateObj.month - 1, timelineSettings.locale) + ' ' + timelineDateObj.year);

                    const sliderEl = $('<span class="monthYearSlider" data-slider-id="' + timelineDateObj.year + '-' + timelineDateObj.month + '">&nbsp;</span>').css({
                        'width': '73px',
                        'right': '0px',
                        'margin-right': '-3px',
                        'cursor': 'pointer',
                        'z-index': '3',
                        'position': 'absolute',
                        'top': tickTop + '%'
                    });

                    $(sliderEl).append(sliderTooltip);
                    $("#dateSlider").append(sliderEl);

                    sliderEl.hover(function () {
                        sliderTooltip.visible();
                    } , function () {
                        sliderTooltip.invisible();
                    });
                }
            }

            $("#dateSliderWrapper").hover(function () {
                $("#dateSlider").fadeIn(timelineSettings.scrollBar.fadeInTime).visible();
                document.getElementById("dateSliderWrapper").style.cursor = "pointer";
            }, function () {
                if (timelineSettings.scrollBarIsSliding === false) {
                    $("#dateSlider").fadeOut(timelineSettings.scrollBar.fadeOutTime).invisible();
                } else {
                    $("#dateSlider").fadeIn(timelineSettings.scrollBar.fadeInTime).visible();
                }
                document.getElementById("dateSliderWrapper").style.cursor = "default";
            });

            $("#dateSliderWrapper").mousemove(function () {
                $("#dateSlider").fadeIn(timelineSettings.scrollBar.fadeInTime).visible();
            });
        }
    };

    timelineSettings.jumpFromTimelineToc = async function (e, anchor, mediaTypeFilter) {
        if (e) e.preventDefault();
        if (timelineSettings.jumpInProgress === true) return;
        timelineSettings.jumpInProgress = true;

        const getContainer = () => document.getElementById('container') || document.scrollingElement || document.body;
        const scrollToInContainer = (el) => {
            const c = getContainer();
            if (!c || !el) return;
            const cRect = c.getBoundingClientRect();
            const eRect = el.getBoundingClientRect();
            const top = (eRect.top - cRect.top) + c.scrollTop;
            c.scrollTo({ top, behavior: 'auto' });
        };
        const waitFor = (fn, { timeout = 5000, interval = 50 } = {}) =>
            new Promise((resolve, reject) => {
                const start = Date.now();
                const tick = () => {
                    try {
                        const val = fn();
                        if (val) return resolve(val);
                        if (Date.now() - start >= timeout) return reject(new Error("waitFor timeout"));
                    } catch (err) { return reject(err); }
                    setTimeout(tick, interval);
                };
                tick();
            });
        const waitForElement = (id, opts) => waitFor(() => document.getElementById(id), opts);

        const preloadAdjacent = () => {
            try { preloadAdjacentSections(anchor, mediaTypeFilter); } catch (_) {}
        };

        const isMobile = (Util.isMobile() === true);

        try {
            timelineSettings.enableScrollSpy = false;
            timelineSettings.isScrolling = false;
            timelineSettings.didJumpFromTimelineToc = true;
            timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;

            if (isMobile) {
                $("#timelineTocToggle").attr("data-bs-backdrop", "static").attr("data-bs-keyboard", "false");
                $("#offcanvasTocCloseButton").prop('disabled', true);
            }

            $('section').each((_, el) => Util.removeDateGallery(el.id));
            $("#spinner_top, #spinner_bottom").hide();

            let ok = await timelineSettings.updateTimeline(anchor, mediaTypeFilter, "new", null);
            if (ok !== timelineSettings.success || !document.getElementById(anchor)) {
                throw new Error("Failed to render anchor");
            }

            const timelineDates = timelineSettings.timelineDates || [];
            const idx = timelineSettings.timelineDatesHash[anchor];
            if (typeof idx !== 'number') throw new Error("Invalid anchor index");

            if (isMobile) {
                // unchanged mobile logic...
            } else {
                // --- DESKTOP LOGIC ---
                timelineSettings.jumpRenderInProgress = true; // NEW FLAG START

                const depthAbove = 5, depthBelow = 4;
                let apAbove = anchor;
                for (let i = idx - 1; i >= Math.max(0, idx - depthAbove); i--) {
                    const id = `${timelineDates[i].year}-${timelineDates[i].month}-${timelineDates[i].day}`;
                    if (!document.getElementById(id)) {
                        await timelineSettings.updateTimeline(id, mediaTypeFilter, "above", apAbove);
                    }
                    apAbove = id;
                }

                let apBelow = anchor;
                for (let i = idx + 1; i <= Math.min(timelineDates.length - 1, idx + depthBelow); i++) {
                    const id = `${timelineDates[i].year}-${timelineDates[i].month}-${timelineDates[i].day}`;
                    if (!document.getElementById(id)) {
                        await timelineSettings.updateTimeline(id, mediaTypeFilter, "below", apBelow);
                    }
                    apBelow = id;
                }

                await waitForElement(anchor);
                await new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)));
                scrollToInContainer(document.getElementById(anchor));

                await timelineSettings.attachAssociatedMetadata(anchor, mediaTypeFilter);

                timelineSettings.jumpRenderInProgress = false; // FLAG END
            }

            timelineSettings.setScrollSpyActive(anchor);
            const tocEl = document.getElementById(`offcanvas_${anchor}`);
            if (tocEl) {
                try { tocEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' }); } catch (_) {}
            }

            if ('requestIdleCallback' in window) {
                requestIdleCallback(preloadAdjacent);
            } else {
                setTimeout(preloadAdjacent, 300);
            }
        } catch (err) {
            shashin.printMessageToConsole(`jumpFromTimelineToc error: ${err.message}`, { tag: "jumpFromTimelineToc" });
        } finally {
            if (isMobile) {
                $("#timelineTocToggle").removeAttr("data-bs-backdrop").removeAttr("data-bs-keyboard");
                $("#offcanvasTocCloseButton").prop('disabled', false);
            }
            timelineSettings.enableScrollSpy = true;
            timelineSettings.isScrolling = false;
            timelineSettings.jumpInProgress = false;
        }
    };

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
    };

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
    };

    // Set the active nav
    timelineSettings.setScrollSpyActive = function (id) {
        if (typeof id === 'string' || id instanceof String) {
            $("#offcanvasTocBody").find('.active').removeClass('active');
            const idArray = id.split("-");

            const navElem = $('a[href^="#' + idArray[0] + '-' + idArray[1] + '-');
            navElem.addClass('active');
        }
    };

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
    };

    timelineSettings.renderThumbnailPreviews = function(metadata, favoritesMap) {
        if ($("#tnbr" + metadata.id + ".thumbnail-br").length === 0) {
            $("#tnbr" + metadata.id).addClass("thumbnail-br");
        }

        if ($("#favorite" + metadata.id).length === 0) {
            $("#tnbr" + metadata.id).append(TimelineTemplates.TimelineGalleryBottomRightOverlay({metadata:metadata}));
            const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].favorite === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
            const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].count > 0 ? favoritesMap[metadata.id].count : 0;
            $("#bricon" + metadata.id).addClass(favoriteIcon);
            $("#briconcount" + metadata.id).text(favoriteCount);
        }

        if ($("#image" + metadata.id).length === 1) {
            const version = Util.getMetadataLocalStorage();
            if ($("#image" + metadata.id).attr("src") === Util.getPlaceholderBackground()) {
                $("#image" + metadata.id).attr("src", "/api/v1/thumbnails/"+timelineSettings.thumbnailType+"/" + metadata.id + (version === "" ? "" : "?v=" + version));
            }
            $("#image" + metadata.id).css("background-color", "transparent");
        }

        if ($("#tnbl" + metadata.id + ".thumbnail-bl").length === 0) {
            $("#tnbl" + metadata.id).addClass("thumbnail-bl");
        }

        if ($("#tncentered" + metadata.id + ".thumbnail-centered").length === 0) {
            $("#tncentered" + metadata.id).addClass("thumbnail-centered");
        }

        const mediaContent = {};
        mediaContent.metadataDetailFun = shashin.openEditMetadataModal;
        mediaContent.videoThumbnailFun = shashin.processVideoThumbnail;
        mediaContent.args = metadata.id;
        mediaContent.thumb = "/api/v1/thumbnails/"+timelineSettings.thumbnailType+"/"+metadata.id;
        mediaContent.metadataId = metadata.id;

        if (metadata.type.indexOf("video") >= 0) {
            mediaContent.video = '{"source": [{"src":"' + encodeURI(metadata.videoUrl).replace(";", "%3B") + '", "type":"video/mp4"}], "attributes": {"preload": "auto", "controls": true, "autoplay": true}}';
            mediaContent.downloadUrl = encodeURI(metadata.videoUrl).replace(";", "%3B") + "/download";
            mediaContent.lgSize = metadata.originalImageWidth+"-"+metadata.originalImageHeight;
            mediaContent.poster = ((null === metadata.thumbnailUrlOriginal || "" === metadata.thumbnailUrlOriginal) ? "/api/v1/thumbnails/"+timelineSettings.thumbnailType+"/"+metadata.id : "/api/v1/thumbnails/original/"+metadata.id) + "?v=" + Util.getMetadataLocalStorage();
        } else {
            mediaContent.src = "/api/v1/thumbnails/original/"+metadata.id;
            mediaContent.downloadUrl = "/api/v1/"+metadata.id + "/download";
        }

        if (metadata.originalImageWidth !== null) {
            mediaContent.width = metadata.originalImageWidth;
        }

        if ($("#mediaLink" + metadata.id).length === 0) {
            $("#tncentered" + metadata.id).append(TimelineTemplates.TimelineGalleryCenterOverlay({metadata:metadata,mediaContent:mediaContent,uuid:Util.getMetadataLocalStorage(), isMobile:Util.isMobile(), thumbnailType:timelineSettings.thumbnailType, thumbnailHeight:timelineSettings.thumbnailHeight}));
        }

        if ($("#metadataModalEdit" + metadata.id).length === 0) {
            const editIcon = (metadata.lat === null || metadata.lng === null) ? "bi-info-square" : "bi-info-circle";
            $("#tnbl" + metadata.id).append(TimelineTemplates.TimelineGalleryBottomLeftOverlay({metadata:metadata,editIcon:editIcon}));
            $("#metadataModalEdit" + metadata.id).attr("tag", metadata.id);
            $("#metadataModalEdit" + metadata.id).on("click", function (e) {
                e.preventDefault();
                shashin.openEditMetadataModal(metadata.id);
            });
        }

        if ($("#select" + metadata.id).length === 0) {
            $("#tntl" + metadata.id).append(TimelineTemplates.TimelineGalleryTopLeftOverlay({metadata:metadata}));
        }

        if ($("#tntl" + metadata.id + ".thumbnail-tl").length === 0) {
            $("#tntl" + metadata.id).addClass("thumbnail-tl");
            shashin.setPhotoOverlays(metadata, "timeline");
            timelineSettings.activateMetadataListeners(metadata.id);
        }

        if (metadata.type.indexOf("video") >= 0) {
            setTimeout(function () {
                if ($("#video" + metadata.id).length === 0) {
                    $("#tntr" + metadata.id).append(TimelineTemplates.TimelineGalleryTopRightOverlay({metadata:metadata}));
                }
                if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                    $("#tntr" + metadata.id).addClass("thumbnail-tr");
                }
            }, 0);
        } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight * 2) {
            setTimeout(function () {
                if ($("#panorama" + metadata.id).length === 0) {
                    $("#tntr" + metadata.id).append(TimelineTemplates.TimelineGalleryTopRightOverlay({metadata:metadata}));
                }
                if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                    $("#tntr" + metadata.id).addClass("thumbnail-tr");
                }
            }, 0);
        } else if (metadata.expectedExtension === "gif") {
            setTimeout(function () {
                if ($("#gif" + metadata.id).length === 0) {
                    $("#tntr" + metadata.id).append(TimelineTemplates.TimelineGalleryTopRightOverlay({metadata:metadata}));
                }
                if ($("#tntr" + metadata.id + ".thumbnail-tr").length === 0) {
                    $("#tntr" + metadata.id).addClass("thumbnail-tr");
                }
            }, 0);
        }
    };

    function renderPreviewsUsingBackend(date, mediaTypeFilter) {
        shashin.printMessageToConsole("Using backend with media type "+mediaTypeFilter,{tag:"timelineQuery"});

        const http = new Http("attaching associated metadata");
        const version = Util.getMetadataLocalStorage();
        http.ajax("get", "/timeline/mediatype/" + mediaTypeFilter + "/date/" + date + (version === "" ? "" : "?v=" + version)).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data.status === timelineSettings.success) {
                    if (data.hasOwnProperty("metadataList") &&
                        data.hasOwnProperty("favorites")
                    ) {
                        const metadataList = data.metadataList;
                        const favoritesMap = data.favorites;

                        if (metadataList.length > 0) {
                            for (const index in metadataList) {
                                const metadata = metadataList[index];

                                setTimeout(function () {
                                    if (Util.isInViewport($("#photoThumbnailContainer" + metadata.id)) === true) {
                                        timelineSettings.renderThumbnailPreviews(metadata, favoritesMap);
                                    }
                                }, 0);
                            }
                        }
                    }
                }
            }
        });
    }

    // Hook up data to edit albums, favorites and people labels
    timelineSettings.attachAssociatedMetadata = async function(date, mediaTypeFilter) {
        const dateArray = date.split("-");
        if (timelineSettings.dbOperationComplete === true && dateArray.length === 3) {
            const year = parseInt(dateArray[0]);
            const month = parseInt(dateArray[1]);
            const day = parseInt(dateArray[2]);

            if (mediaTypeFilter === "all" || mediaTypeFilter === "") {
                timelineSettings.db.metadataList.where({year: year, month: month, day: day}).reverse().sortBy("time", function (metadataList) {
                    display(metadataList);
                });
            } else {
                timelineSettings.db.metadataList.where({year: year, month: month, day: day}).and(metadata => metadata.type.includes(mediaTypeFilter)).reverse().sortBy("time", function (metadataList) {
                    display(metadataList);
                });
            }

            shashin.printMessageToConsole("attachAssociatedMetadata using dexie with media type " + mediaTypeFilter + " for " + date, {tags: ["timelineQuery","dexie"]});

            const display = function(metadataList) {
                const favoritesMap = timelineSettings.favoritesMap;

                if (metadataList.length > 0) {
                    for (const index in metadataList) {
                        const metadata = metadataList[index];

                        setTimeout(function () {
                            if (Util.isInViewport($("#photoThumbnailContainer" + metadata.id)) === true) {
                                timelineSettings.renderThumbnailPreviews(metadata, favoritesMap);
                            }
                        }, 0);
                    }
                }
            }
        } else {
            renderPreviewsUsingBackend(date, mediaTypeFilter);
        }
    };

    timelineSettings.createEmptyContainer = async function(date, attachToId, height) {
        $("#msgTimeline").html("");
        let ret = shashin.apiResponse.FAIL;
        const dateArray = date.split("-");

        if (date !== attachToId && dateArray.length > 0) {
            const year = dateArray[0];
            const month = dateArray[1];
            const day = dateArray[2];

            $('<span class="dateContainer" id="container_'+year+'-'+month+'-'+day+'" style="display: block;height: '+height+'px;"></span>').insertAfter($("#amp_" + attachToId));
            ret = timelineSettings.success;
        }

        return ret;
    };

    function updateTimeline(date,mediaTypeFilter,action,attachToId,metadataList,placeNameHeaders) {
        let ret = shashin.apiResponse.FAIL;

        let html = "";
        // When it's an empty container, create barebones html
        let internalHtml = "";

        let idCheck = "undated";
        if (metadataList[0].year !== null &&
            metadataList[0].month !== null &&
            metadataList[0].day !== null)
        {
            idCheck = metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day;
        }

        const currentTimelineIndex = timelineSettings.timelineDatesHash[idCheck];
        let nextTimelineIndex = currentTimelineIndex-1;

        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
            nextTimelineIndex = currentTimelineIndex+1;
        }

        let hideText = false;
        if (timelineSettings.timelineDates.hasOwnProperty(nextTimelineIndex)) {
            const nextTimeline = timelineSettings.timelineDates[nextTimelineIndex];
            const nextTimelineDate = nextTimeline.year + "-" + nextTimeline.month + "-" + nextTimeline.day;

            if (placeNameHeaders.length === 1 &&
                placeNameHeaders[0] === $("#placeNameHeader"+nextTimelineDate).text()
            ) {
                //placeNameHeaders[0] = "";
                hideText = true;
            }
        }

        let listHtml = "";
        if (placeNameHeaders.length > 1) {
            for (const index in placeNameHeaders) {
                const placeNameHeader = placeNameHeaders[index];
                listHtml += '        <li class="text-muted"><a class="dropdown-item" href="/search?term='+placeNameHeader+'" target="_blank">'+placeNameHeader+'</a></li>\n';
            }
        }

        html += TimelineTemplates.TimelinePreLoadGalleryHeader({metadata:metadataList[0],placeNameHeaders:placeNameHeaders,listHtml:listHtml,isMobile:Util.isMobile(),locale:timelineSettings.locale});
        internalHtml += '<br id="br'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'">' +
            '<section class="scrollspy" id="'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'">' +
            '<div class="dateHeader"><span id="select'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'" class="bi-circle pe-2 day-select" style="font-size: 0.85rem;color: lightgray;display: none"></span><strong class="dateHeading pe-1">'+Util.getDateString(metadataList[0].year, metadataList[0].month, metadataList[0].day, timelineSettings.locale)+'</strong>';

        if (placeNameHeaders.length === 1 && placeNameHeaders[0].length > 0) {
            internalHtml += '<span class="text-muted"><a class="link-unstyled" href="/search?term='+placeNameHeaders[0]+'" target="_blank" id="placeNameHeader'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'"'+(hideText === true ? " style='display: none;'" : "")+'>'+placeNameHeaders[0]+'</a></span>';
        } else if (placeNameHeaders.length > 1) {
            internalHtml += '<span class="text-muted"><div class="dropdown" style="display: inline-block;"><a class="dropdown-toggle link-unstyled" data-bs-toggle="dropdown" href="#">'+placeNameHeaders[0]+'</a>\n' +
                '    <ul class="dropdown-menu">\n';
            internalHtml += listHtml;
            internalHtml += '    </ul></div></span>';
        }

        internalHtml += '</div></section>' +
            '<div class="row'+(Util.isMobile() ? " ms-0" : " image-group-padding")+'" id="row'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'">' +
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
                let lastYearTaken = $(".yearTaken").length === 0 ? (metadataList[0].year === null ? "" : metadataList[0].year) : $(".yearTaken").get(yearTakenCount - 1).innerText;
                let lastMonthTaken = $(".monthTaken").length === 0 ? (metadataList[0].month === null ? "" : metadataList[0].month) : $(".monthTaken").get(monthTakenCount - 1).innerText;
                let lastDayTaken = $(".dayTaken").length === 0 ? (metadataList[0].day === null ? "" : metadataList[0].day) : $(".dayTaken").get(dayTakenCount - 1).innerText;
                lastYearTaken = lastYearTaken !== "" ? parseInt(lastYearTaken) : 0;
                lastMonthTaken = lastMonthTaken !== "" ? parseInt(lastMonthTaken) : 0;
                lastDayTaken = lastDayTaken !== "" ? parseInt(lastDayTaken) : 0;

                let loopedHtml = TimelineTemplates.TimelinePreLoadGalleryBody({metadata:metadata, isMobile:Util.isMobile(), thumbnailType:timelineSettings.thumbnailType, thumbnailHeight:timelineSettings.thumbnailHeight});
                html += loopedHtml;
                internalHtml += loopedHtml;

                $("#metadataModalEdit" + metadata.id).attr("tag", metadata.id);
            }

            const lastDateParts = $("#offcanvasTocBody div a").last().attr("id").split("offcanvas_");
            const lastDate = lastDateParts[1];

            html += TimelineTemplates.TimelinePreLoadGalleryFooter({metadata:metadataList[0],lastDate:lastDate});
            internalHtml += '<span class="scrollspy metadataprocessed" id="tail_'+metadataList[0].year+'-'+metadataList[0].month+'-'+metadataList[0].day+'"></span></div>';

            const tempScrollTop = $("#container").scrollTop();

            let htmlEl = $(html);

            if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                if (Util.getOS() === "iOS" && Util.isChrome() === true) {
                    $("#infinite-scroll-gallery").invisible();
                }
            }

            setTimeout(function () {
                if (action === "above") {
                    // Find anchor element in viewport
                    const anchorElement = $(".scrollspy:visible").first()[0];

                    htmlEl.insertBefore($("#container_" + attachToId)).ready(function () {
                        // deferred.resolve(timelineSettings.success);
                        ret = timelineSettings.success;
                        if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                            if (Util.getOS() === "iOS" && Util.isChrome() === true && Util.isSafari() === true) {
                                //$("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                // Scroll back to anchor element
                                requestAnimationFrame(() => {
                                    if (anchorElement) {
                                        anchorElement.scrollIntoView({ behavior: 'auto', block: 'start' });
                                    }
                                });
                                $("#infinite-scroll-gallery").visible();
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
                            if (Util.getOS() === "iOS" && Util.isChrome() === true && Util.isSafari() === true) {
                                $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                $("#infinite-scroll-gallery").visible();
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
                                    if (Util.getOS() === "iOS" && Util.isChrome() === true && Util.isSafari() === true) {
                                        $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                        $("#infinite-scroll-gallery").visible();
                                    }
                                }
                                // deferred.resolve(timelineSettings.success);
                                ret = timelineSettings.success;
                            });
                        } else {
                            $("#infinite-scroll-gallery").prepend(htmlEl).ready(function () {
                                if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                    if (Util.getOS() === "iOS" && Util.isChrome() === true && Util.isSafari() === true) {
                                        $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                        $("#infinite-scroll-gallery").visible();
                                    }
                                }
                                // deferred.resolve(timelineSettings.success);
                                ret = timelineSettings.success;
                            });
                        }
                    } else {
                        htmlEl.insertAfter($("#amp_" + attachToId)).ready(function () {
                            if (timelineSettings.currentScrollDirection === timelineSettings.ScrollDirection.up) {
                                if (Util.getOS() === "iOS" && Util.isChrome() === true && Util.isSafari() === true) {
                                    $("#container").scrollTop(tempScrollTop + Util.getDateGalleryHeight(date));
                                    $("#infinite-scroll-gallery").visible();
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

        return ret;
    }

    timelineSettings.updateTimeline = async function(date,mediaTypeFilter,action,attachToId) {
        let ret = shashin.apiResponse.FAIL;
        $("#msgTimeline").html("");

        // if (timelineSettings.dbOperationComplete === true) {
        //     const dateArray = date.split("-");
        //     const year = parseInt(dateArray[0]);
        //     const month = parseInt(dateArray[1]);
        //     const day = parseInt(dateArray[2]);
        //
        //     if (mediaTypeFilter === "all" || mediaTypeFilter === "") {
        //         timelineSettings.db.metadataList.where({year: year, month: month, day: day}).reverse().sortBy("time", function (metadataList) {
        //             display(metadataList);
        //         });
        //     } else {
        //         timelineSettings.db.metadataList.where({year: year, month: month, day: day}).and(metadata => metadata.type.includes(mediaTypeFilter)).reverse().sortBy("time", function (metadataList) {
        //             display(metadataList);
        //         });
        //     }
        //
        //     shashin.printMessageToConsole("Using dexie with media type " + mediaTypeFilter, {tags: ["timelineQuery","dexie"]});
        //
        //     const display = function(metadataList) {
        //         if (metadataList.length > 0) {
        //             ret = updateTimeline(date, mediaTypeFilter, action, attachToId, metadataList, timelineSettings.placeNameHeaders);
        //         } else {
        //             $(".attachMetadataPhotos").last().text("EOL").css("display", "none");
        //             ret = timelineSettings.success;
        //         }
        //         return ret;
        //     };
        // } else {
        const version = Util.getMetadataLocalStorage();

        const ajaxParams = {
            type: 'get',
            url: "/timeline/mediatype/" + mediaTypeFilter + "/date/" + date + "/metadata" + (version === "" ? "" : "?v=" + version),
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        };

        return await $.ajax(ajaxParams)
            .fail(function (xhr, textStatus) {
                shashin.onFail(xhr, textStatus, ajaxParams, " updating timeline");
            }).then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let message = "Error";
                    if (data.status === timelineSettings.success) {
                        if (data.hasOwnProperty("metadataList")) {
                            const metadataList = data.metadataList;

                            if (metadataList.length > 0) {
                                ret = updateTimeline(date, mediaTypeFilter, action, attachToId, metadataList, data.placeNameHeaders);
                            } else {
                                $(".attachMetadataPhotos").last().text("EOL").css("display", "none");
                                ret = timelineSettings.success;
                            }
                            ret = timelineSettings.success;
                        }
                        ret = timelineSettings.success;
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
                        $("#msgTimeline").html(message);
                        ret = shashin.apiResponse.FAIL;
                    }
                }

                return ret;
            });
        // }
    };

    timelineSettings.activateMetadataListeners = function(metadataId) {
        Util.activateMetadataListeners(metadataId);

        shashin.updateFavorites("#favorite","#bricon","#briconcount",metadataId);
    };

    timelineSettings.refreshTimeline = async function (mediaTypeFilter) {
        const http = new Http("refreshing timeline TOC");
        const data = await http.ajax("get", "/timeline/dates/"+mediaTypeFilter);

        if (data.hasOwnProperty("metadataDates")) {
            const metadataDates = data.metadataDates;
            timelineSettings.timelineDates = metadataDates;

            // Rebuild slider
            if (Util.isMobile() === false) {
                $("#dateSlider").empty();
                $("#dateSlider").hide();
                $("#dateSlider").show();
                $("#dateSlider").visible();
                timelineSettings.initializeTimelineSlider(mediaTypeFilter);
            }

            // Clear offcanvas TOC and rebuild
            $("#timelineTocToggle").show();
            $("#offcanvasTocBody").empty();

            let html = "";

            for (let index = 0; index < metadataDates.length; index++) {
                const metadataDate = metadataDates[index];
                const year = metadataDate.year;
                const month = metadataDate.month;
                const day = metadataDate.day;

                html += TimelineTemplates.TimelineToc({index:index,mediaTypeFilter:mediaTypeFilter,metadataDates:metadataDates,year:year,month:month,day:day});
            }

            $("#offcanvasTocBody").append(html);

            if (Util.isMobile() === false) {
                setTimeout(function() {
                    $("#timelineTocToggle").hide();
                },0);
            }
        }
    };

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