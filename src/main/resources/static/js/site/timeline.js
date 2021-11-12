(function( timelineSettings, $, undefined ) {
    timelineSettings.enableScrollSpy = true;
    timelineSettings.prevAnchor = "";
    timelineSettings.successBelowMsg = "success_below";
    timelineSettings.successAboveMsg = "success_above";
    timelineSettings.successMidMsg = "success_mid";
    timelineSettings.didScroll = false;
    timelineSettings.stopTrackingScroll = false;
    timelineSettings.processRender = true;
    timelineSettings.lastScrollTop = 0;

    timelineSettings.jumpToLightGalleryMetadata = function (metadataId) {
        const url = location.href;
        location.href = '#lightGalleryIndex'+metadataId;
        history.replaceState(null,null,url);
    }

    timelineSettings.refreshTimeline = function (mediaTypeFilter,currentOffCanvasId) {
        const ajaxParams = {
            type: 'get',
            url: "/timeline/dates/"+mediaTypeFilter,
            contentType: 'application/json; charset=utf-8',
            async:true,
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error refreshing timeline TOC. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        $.ajax(ajaxParams)
        .fail(onFail).then(function(data) {
            if (data.hasOwnProperty("metadataDates")) {
                $("#offcanvasTocBody").empty();

                const metadataDates = data["metadataDates"];
                let html = "";
                for (const index in metadataDates) {
                    const metadataDate = metadataDates[index];
                    const year = metadataDate["year"];
                    const month = metadataDate["month"];
                    const day = metadataDate["day"];

                    let offcanvasDate = "";
                    let text = "Undated";
                    let active = "";
                    if (year == null || month == null || day == null) {
                        offcanvasDate = "offcanvas_undated";
                        if (currentOffCanvasId === offcanvasDate) {
                            active = " active";
                        }
                        html += '<a id="'+offcanvasDate+'" class="list-group-item list-group-item-action'+active+'" onclick="return timelineSettings.jumpFromTimelineToc(event,\'undated\',\''+mediaTypeFilter+'\')" href="#undated">'+text+'</a>\n';
                    } else {
                        offcanvasDate = "offcanvas_"+year+"-"+month+"-"+day;
                        if (currentOffCanvasId === offcanvasDate) {
                            active = " active";
                        }
                        const dateObj = new Date(year, month-1, day);
                        text = dateObj.format("mmm d, yyyy");
                        html += '<a id="'+offcanvasDate+'" class="list-group-item list-group-item-action'+active+'" onclick="return timelineSettings.jumpFromTimelineToc(event,\''+year+'-'+month+'-'+day+'\',\''+mediaTypeFilter+'\')" href="#'+year+'-'+month+'-'+day+'">'+text+'</a>\n';
                    }
                }
                $("#offcanvasTocBody").append(html);
            }
        });
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
                        shashin.setLightGallery({"selector":".mediaLink"});
                    }, 500);
                }
            }, closeTimeout);
        }

        // lg.refresh();

        // lg.destroy();
        // setTimeout(() => {
        //     lg = lightGallery(lightGalleryEl, lightGalleryConfigs);
        // }, 500);
    }

    let lastElements = null;
    timelineSettings.renderThumbnailsInViewport = function (elements,mediaTypeFilter,scrollHack) {
        timelineSettings.stopTrackingScroll = true;

        if (typeof scrollHack === "undefined") {
            scrollHack = false;
        }

        // If no scrollspy elements found, find current thumbnail container
        // and closest previous scrollspy element
        if (elements.length === 0) {
            const thumbnailsInViewport = $(".photo-thumbnail-container").withinviewport();
            elements = $(thumbnailsInViewport.parent().prevAll(".scrollspy")[0])
        }

        let render = false;
        const diff = $(lastElements).not(elements).get();
        const lastIdToc = $("#offcanvasTocBody").children().last();
        const lastDate = lastIdToc.attr("id").split("offcanvas_")[1];
        let lastDateFound = false;
        const dateElements = $(elements).map(function() {
            return $(this).attr('id');
        }).get();
        const lastDateElements = $(lastElements).map(function() {
            return $(this).attr('id');
        }).get();
        if($.inArray(lastDate, dateElements) !== -1) {
            lastDateFound = true;
        }

        // Additional hacks for scroll direction as the detection gets "confused" when dealing with dynamic content
        if (dateElements[0] && lastDateElements[0] && shashin.getDateObject(dateElements[0]) > shashin.getDateObject(lastDateElements[0])) {
            shashin.scrollDirection = "up";
        }
        if (dateElements[0] && lastDateElements[0] && shashin.getDateObject(dateElements[0]) < shashin.getDateObject(lastDateElements[0])) {
            shashin.scrollDirection = "down";
        }
        if (dateElements[0] === lastDateElements[0] && diff.length === 1 && lastDateElements[lastDateElements.length-1] === $(diff[0]).attr("id") && $(diff[0]).attr("id").indexOf("tail_") >= 0) {
            shashin.scrollDirection = "up";
        }

        if (scrollHack === true || lastElements === null || diff.length > 0 || (diff.length > 0 && shashin.scrollDirection === "up" && dateElements[0] !== lastDateElements[0]) || (lastDateFound === false && shashin.scrollDirection === "down" && $("footer").withinviewport().length > 0)) {
            render = true;
        }

        if (render === true) {
            elements.each(function (index) {
                let id = $(this).attr("id");

                if (shashin.scrollDirection === "up") {
                    id = $(elements).first().attr("id");
                }

                if ((shashin.scrollDirection === "down" && elements.length === 1 && id.indexOf("tail_") >= 0) || (shashin.scrollDirection === "up" && id.indexOf("tail_") >= 0)) {
                    const idParts = id.split("tail_");
                    id = idParts[1];
                }

                if (id.indexOf("tail_") < 0 && timelineSettings.prevAnchor !== id && (shashin.scrollDirection === "down" || (shashin.scrollDirection === "up" && index < 2))) {
                    timelineSettings.processRender = false;
                    timelineSettings.renderThumbnails(id, mediaTypeFilter).then(function (msg) {
                        if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                            timelineSettings.setScrollSpyActive(id);
                        }
                        shashin.scrollDirection = "up";
                        timelineSettings.stopTrackingScroll = false;
                        timelineSettings.lastScrollTop = 9999;
                    });

                    timelineSettings.prevAnchor = id;

                    if (shashin.scrollDirection === "up") {
                        return false;
                    }
                }

                timelineSettings.prevAnchor = "";
            });
        } else {
            shashin.scrollDirection = "up";
            timelineSettings.stopTrackingScroll = false;
            timelineSettings.lastScrollTop = 9999;
        }

        lastElements = elements;
    }

    timelineSettings.jumpFromTimelineToc = function (e,anchor,mediaTypeFilter) {
        e.preventDefault();

        shashin.scrollDirection = "down";
        timelineSettings.processRender = false;
        timelineSettings.enableScrollSpy = false;
        shashin.timelineQueryLimit = 1;
        timelineSettings.didScroll = false;
        timelineSettings.stopTrackingScroll = false;
        //timelineSettings.lastScrollTop = 9999;
        lastElements = null;

        shashin.printMessageToConsole("jumpFromTimelineToc anchor:"+anchor);
        shashin.printMessageToConsole("jumpFromTimelineToc mediaTypeFilter:"+mediaTypeFilter);

        // Remove all elements
        $('section').each(function(index, element) {
            shashin.removeDateGallery(element.id);
        })

        timelineSettings.renderThumbnails(anchor,mediaTypeFilter).then(function (msg) {
            if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                timelineSettings.setScrollSpyActive(anchor);
                timelineSettings.observeAnchorChange(anchor, timelineSettings.scrollToToc);
            }
        });
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
        // const timer = setInterval(function () {
            if (navElem.hasClass("active") === true) {
                timelineSettings.enableScrollSpy = true;
                timelineSettings.stopTrackingScroll = true;
                // timelineSettings.didScroll = true;
                shashin.scrollDirection = "down";
                //clearInterval(timer);
            }
        // }, 200);
    }

    // Set the active nav
    timelineSettings.setScrollSpyActive = function (id) {
        const navElem = $('a[href="#' + id + '"]');
        navElem.addClass('active').siblings().removeClass('active');
    }

    timelineSettings.scrollToTimelineToc = function(elements) {
        elements.each(function(index) {
            let id = $(this).attr("id");

            if (id.indexOf("tail_") < 0 && index === 1) {
                document.getElementById("offcanvas_"+id).scrollIntoView({
                    behavior: 'smooth'
                });
                return false;
            }
        });
    }

    timelineSettings.openTimelineModal = function(metadata,recognitionLabels,taggedPeopleList,allAlbumList,albumList) {
        let index;

        // Populate modal data
        if ($("#timelineModalEdit"+metadata.id).attr("tag") && $("#timelineModalEdit"+metadata.id).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#timelineModalEdit"+metadata.id).attr("tag"));
        }

        // Clear modal data
        $('#propTimelineModal').find(':input').val('');
        $("#propTimelineModalThumbnail").html("");
        $("#isobject")[0].checked = false;
        $("#hidden")[0].checked = false;

        $("#timelineModalTitle").text(metadata.fileName);
        $("#currentfilename").val(metadata.fileName)
        $("#currentlat").val(metadata.lat)
        $("#currentlng").val(metadata.lng)
        $("#metadataId").val(metadata.id);

        if (metadata.thumbnailUrlCentered !== null) {
            $("#propTimelineModalThumbnail").html('<img src="'+encodeURI(metadata.thumbnailUrlCentered)+'" height="100" width="100" onError="shashin.errorImg(this,\''+metadata.title+'\',100)">');
        }

        if (metadata.title !== null) {
            $("#title").val(metadata.title);
        }

        if (metadata.timeZone !== null) {
            $("#offsetTaken").val(metadata.timeZone);
        }
        if (metadata.time !== null) {
            $("#timeTaken").val(metadata.time);
        }
        if (metadata.day !== null) {
            $("#dayTaken").val(metadata.day);
        }
        if (metadata.month !== null) {
            $("#monthTaken").val(metadata.month);
        }
        if (metadata.year !== null) {
            $("#yearTaken").val(metadata.year);
        }
        if (metadata.hidden !== null && metadata.hidden === true) {
            $("#offsetTaken").prop('checked', true);
        }

        const latlngValue = (metadata.lat == null || metadata.lng == null || metadata.lat === "" || metadata.lng === "") ? '' : ($.trim(metadata.lat) + ',' + $.trim(metadata.lng));
        $("#latlng").val(latlngValue);

        const taggedPeopleArray = taggedPeopleList.split(",");
        let isObject = false;
        let taggedPeopleString = "";
        for (index in taggedPeopleArray) {
            const person = taggedPeopleArray[index];
            if (person === "object") {
                isObject = true;
            } else {
                taggedPeopleString += person + ",";
            }
        }
        taggedPeopleString = taggedPeopleString.replace(/,\s*$/, "");
        taggedPeopleString = taggedPeopleString.trim();
        if (taggedPeopleString !== "") {
            $("#tagpeople").val(taggedPeopleString);
        } else if (metadata.tagpeople !== null) {
            $("#tagpeople").val(metadata.tagpeople);
        }

        if ($("#recognitionLabelInput").length > 0) {
            $("#recognitionLabelInput").remove();
        }
        if (recognitionLabels !== null && recognitionLabels.length > 0) {
            let html = '<div class="input-group-append" id="recognitionLabelInput">\n' +
                '           <button class="btn btn-outline-secondary dropdown-toggle" onclick="return timelineModal.toggleTagPeopleDropdown(\'' + metadata.id + '\');" id="tagpeopledropdown' + metadata.id + '" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                '           <div class="dropdown-menu" id="recognitionLabelsList">\n';

            for (index in recognitionLabels) {
                const recognitionLabel = recognitionLabels[index];
                let checkedString = "";

                if ($.inArray(recognitionLabel.name, taggedPeopleArray) !== -1) {
                    checkedString = " checked";
                }

                html +=
                    '           <button class="dropdown-item" type="button">\n' +
                    '               <input type="checkbox" onclick="return timelineModal.populateLabel(\'' + metadata.id + '\');" value="' + recognitionLabel.name + '" name="recognitionLabel' + metadata.id + '[]" id="' + metadata.id + '-' + recognitionLabel.id + '"' + checkedString + '>\n' +
                    '               <label for="' + metadata.id + '-' + recognitionLabel.id + '" id="label-' + metadata.id + '-' + recognitionLabel.id + '">' + recognitionLabel.name + '</label>\n' +
                    '           </button>\n';
            }
            html += '   </div>\n' +
                '</div>\n';

            $(html).insertAfter($("#labelIdData"))
        }

        const albumListArray = albumList.split(",");
        let albumListString = "";
        for (index in albumListArray) {
            const album = albumListArray[index];
            albumListString += album + ",";
        }
        albumListString = albumListString.replace(/,\s*$/, "");
        albumListString = albumListString.trim();
        if (albumListString !== "") {
            $("#albumnames").val(albumListString);
        } else if (metadata.albumlist !== null) {
            $("#albumnames").val(metadata.albumlist);
        }

        if ($("#albumListInput").length > 0) {
            $("#albumListInput").remove();
        }
        if (allAlbumList !== null && allAlbumList.length > 0) {
            let html =
                '<div class="input-group-append" id="albumListInput">\n' +
                '   <button class="btn btn-outline-secondary dropdown-toggle" onclick="return timelineModal.toggleAlbumDropdown(\'' + metadata.id + '\');" id="albumdropdown' + metadata.id + '" type="button" aria-haspopup="true" aria-expanded="false">Albums</button>\n' +
                '   <div class="dropdown-menu" id="albumsList">\n';

            for (index in allAlbumList) {
                const eachAlbum = allAlbumList[index];
                let checkedString = "";

                if ($.inArray(eachAlbum.name, albumListArray) !== -1) {
                    checkedString = " checked";
                }

                html +=
                    '   <button class="dropdown-item" type="button">\n' +
                    '       <input type="checkbox" onclick="return timelineModal.populateAlbum(\'' + metadata.id + '\');" value="' + eachAlbum.name + '" name="album' + metadata.id + '[]" id="' + metadata.id + '-' + eachAlbum.id + '"' + checkedString + '>\n' +
                    '       <label for="' + metadata.id + '-' + eachAlbum.id + '" id="album-' + metadata.id + '-' + eachAlbum.id + '">' + eachAlbum.name + '</label>\n' +
                    '   </button>\n';
            }
            html += '</div>\n' +
                '</div>\n';

            $(html).insertAfter($("#albumNameData"))
        }

        if (isObject === true) {
            $("#isobject")[0].checked = true;
        }

        if (metadata.hidden !== null && metadata.hidden === true) {
            $("#hidden")[0].checked = true;
        }

        $("#keywords").val(metadata.keywords);

        $("#albumDetailRow").remove();
        shashin.populateDetailsTab(metadata);

        // Open modal window
        $("#propTimelineModal").modal('show');
    }

    // Render only what's needed
    timelineSettings.renderThumbnails = async function(id,mediaTypeFilter) {
        let deferred = new $.Deferred();

        //shashin.enableDebug();
        // Depth of results in section of page above and below anchor
        let depthDown = 1;
        let depthUp = 4;
        let action = "new";
        let attachPoint = id;

        shashin.printMessageToConsole("scrollDirection:"+shashin.scrollDirection);
        shashin.printMessageToConsole("depthDown:"+depthDown);
        shashin.printMessageToConsole("depthUp:"+depthUp);
        shashin.printMessageToConsole("renderThumbnails id:"+id);

        const lastIdToc = $("#offcanvasTocBody").children().last();
        const lastDate = lastIdToc.attr("id").split("offcanvas_")[1];

        // Render top
        if ((/*attachBelowArray.length > 0 && */shashin.scrollDirection === "up") || lastDate === id) {
            // Remove elements that are not visible
            $('section').each(function (index, element) {
                shashin.printMessageToConsole(element.id + " checking to remove beginning");
                if ($("#" + element.id).withinviewport().length === 0 &&
                    $("#br" + element.id).withinviewport().length === 0 &&
                    $("#row" + element.id).withinviewport().length === 0 &&
                    $("#amp_" + element.id).withinviewport().length === 0 &&
                    $("#tail_" + element.id).withinviewport().length === 0 &&
                    $(".photo-thumbnail-image.thumbnailTag_" + element.id).withinviewport().length === 0
                    //&& $("footer").withinviewport().length === 0
                ) {
                    shashin.printMessageToConsole(element.id + " removed beginning");
                    shashin.removeDateGallery(element.id);
                }
            });

            let currentId = id;
            let numberAdded = 0;
            let nextAttachPoint = null;

            while (true) {

                let firstDate = null;
                $("#offcanvasTocBody").children().each(function () {
                    const dateParts = $(this).attr("id").split("offcanvas_");
                    const date = dateParts[1];

                    if (firstDate === null) {
                        firstDate = date;
                    }

                    if ($(this).prev().length > 0 && currentId === date) {
                        currentId = $(this).prev().attr("id").split("offcanvas_")[1];
                        return false;
                    }

                });

                if (currentId !== null && $("#" + currentId).length === 0) {
                    if (action === "new") {
                        attachPoint = null;
                    }

                    if (nextAttachPoint === null) {
                        nextAttachPoint = currentId;
                    }

                    const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint)
                    if (msg === "success" && $("#" + currentId).length === 1) {
                        timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                    }

                    action = "above";
                    numberAdded++;
                    attachPoint = currentId;

                    if (//numberAdded > depthUp ||
                        // $("#br" + currentId).withinviewport().length === 0 ||
                        // $("#row" + currentId).withinviewport().length === 0 ||
                        $("#amp_" + currentId).withinviewport().length === 0 ||
                        // $("#tail_" + currentId).withinviewport().length === 0 ||
                        // $("#" + currentId).withinviewport().length === 0 ||
                        //$(".photo-thumbnail-image.thumbnailTag_" + currentId).withinviewport().length === 0// ||
                        /*(($("#br"+currentId).withinviewport().length > 0 && $("#amp_" + currentId).withinviewport().length === 0) && $("footer").withinviewport().length === 0) || */
                        (firstDate !== null && currentId === firstDate)
                    ) {
                        break;
                    }
                } else if (currentId === firstDate) {
                    attachPoint = currentId;
                    break;
                }
                attachPoint = currentId;
            }

            let tempOffCanvasIdAbove = $("#offcanvas_"+attachPoint);
            for (let i = 0; i <= depthUp; i++) {
                tempOffCanvasIdAbove = tempOffCanvasIdAbove.prev();
                if (typeof tempOffCanvasIdAbove.attr("id") !== 'undefined') {
                    const offcanvasId = tempOffCanvasIdAbove.attr("id").split("_")[1];
                    const msg = await timelineSettings.updateTimeline(offcanvasId, mediaTypeFilter, action, attachPoint)
                    if (msg === "success" && $("#" + offcanvasId).length === 1) {
                        timelineSettings.attachAssociatedMetadata(offcanvasId, mediaTypeFilter);
                    }
                    attachPoint = offcanvasId;
                }
            }

            // Render mid
            action = "below";
            shashin.printMessageToConsole("attempting to attach id mid "+id+" "+action+" "+attachPoint+" length "+$("#"+id).length)
            if (id !== null && attachPoint !== null && $("#"+id).length === 0 && $("#"+attachPoint).length > 0) {
                shashin.printMessageToConsole("attaching mid attachPoint:"+attachPoint)
                shashin.printMessageToConsole("attaching id:" + id);
                shashin.printMessageToConsole("attaching mid action:"+action)

                const msg = await timelineSettings.updateTimeline(id, mediaTypeFilter, action, attachPoint);
                if (msg === "success" && $("#"+id).length === 1) {
                    timelineSettings.attachAssociatedMetadata(id, mediaTypeFilter);
                }
            }
        } else { // Scrolling down

            // Delete visible elements
            $('section').each(function (index, element) {
                shashin.printMessageToConsole(element.id + " checking to remove beginning");

                if ($("#" + element.id).withinviewport().length > 0 ||
                    $("#br" + element.id).withinviewport().length > 0 ||
                    $("#row" + element.id).withinviewport().length > 0 ||
                    $("#amp_" + element.id).withinviewport().length > 0 ||
                    $("#tail_" + element.id).withinviewport().length > 0
                    || $(".photo-thumbnail-image.thumbnailTag_" + element.id).withinviewport().length > 0
                ) {
                    return;
                } else {
                    shashin.printMessageToConsole(element.id + " removed beginning");
                    shashin.removeDateGallery(element.id);
                }
            });

            // Render bottom
            action = null;
            if ($("#"+id).length === 0) {
                attachPoint = null;
                const msg = await timelineSettings.updateTimeline(id, mediaTypeFilter, action, attachPoint);
                if (msg === "success" && $("#"+id).length === 1) {
                    timelineSettings.attachAssociatedMetadata(id, mediaTypeFilter);
                }
                attachPoint = id;
            }

            // Render bottom until footer not visible
            action = "below";
            let deleteMarker = false;
            while (true) {
                let currentId = attachPoint;

                $("#offcanvasTocBody").children().each(function () {
                    const date = $(this).attr("id").split("offcanvas_")[1];
                    if (currentId === date) {
                        if ($(this).next().length > 0) {
                            currentId = $(this).next().attr("id").split("offcanvas_")[1];
                        }
                        return false;
                    }
                });

                if (currentId !== null && $("#" + currentId).length === 0) {
                    const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint);
                    if (msg === "success" && $("#"+currentId).length === 1) {
                        timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                    }
                }

                if (deleteMarker === true) {
                    break;
                }

                if ($("footer").withinviewport().length === 0 || currentId === attachPoint) {
                    deleteMarker = true;
                }

                attachPoint = currentId;
            }
        }

        timelineSettings.processRender = true;

        shashin.printMessageToConsole("==============================================");

        deferred.resolve(timelineSettings.successMidMsg);
        return deferred.promise();
    }

    // Hook up data to edit albums, favorites and people labels
    timelineSettings.attachAssociatedMetadata = function(date,mediaTypeFilter) {
        const ajaxParams = {
            type: 'get',
            url: "/timeline/mediatype/"+mediaTypeFilter+"/date/"+date,
            contentType: 'application/json; charset=utf-8',
            async:true,
            retries: shashin.ajaxRetries
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error attaching associated metadata. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        $.ajax(ajaxParams)
        .fail(onFail).then(function(data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList") &&
                        data.hasOwnProperty("favorites") &&
                        data.hasOwnProperty("albumList") &&
                        data.hasOwnProperty("recognitionLabels") &&
                        data.hasOwnProperty("labelPhotoMap")
                    ) {
                        const metadataList = data["metadataList"] === "" ? null : data["metadataList"];
                        const favoritesMap = data["favorites"] === "" ? null : data["favorites"];
                        const recognitionLabels = data["recognitionLabels"] === "" ? null : data["recognitionLabels"];
                        const labelPhotoMap = data["labelPhotoMap"] === "" ? null : data["labelPhotoMap"];
                        const albumMap = data["albumMap"] === "" ? null : data["albumMap"];
                        const albumList = data["albumList"] === "" ? null : data["albumList"];

                        if (metadataList.length > 0) {

                            // Populate batch modals
                            if (recognitionLabels !== null && recognitionLabels.length > 0) {
                                let batchHtml =
                                    '       <input type="text" class="form-control" onfocus="return timelineBatchModal.closeBatchTagPeopleDropdown();" aria-label="Tag People" id="tagBatchDataInput" name="tagBatchDataInput" value="">\n' +
                                    '       <div class="input-group-append">\n' +
                                    '           <button class="btn btn-outline-secondary dropdown-toggle" onclick="return timelineBatchModal.toggleBatchTagPeopleDropdown();" id="tagpeopledropdown" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                                    '           <div class="dropdown-menu" id="albumNameList">';

                                for (let index in recognitionLabels) {
                                    const recognitionLabel = recognitionLabels[index];
                                    batchHtml +=
                                        '           <button class="dropdown-item" type="button">\n' +
                                        '               <input type="checkbox" onclick="return timelineBatchModal.populateBatchLabel();" id="'+recognitionLabel.id+'" value="'+recognitionLabel.name+'" name="recognitionLabel[]">\n' +
                                        '               <label for="'+recognitionLabel.id+'">'+recognitionLabel.name+'</label>\n' +
                                        '           </button>'
                                }
                                batchHtml +=
                                    '   </div>\n' +
                                    '</div>\n';

                                $("#batchLabelIds").html(batchHtml);
                            }

                            if (albumList !== null && albumList.length > 0) {
                                let batchHtml =
                                    '<input type="text" class="form-control" aria-label="Albums Name" id="albumNameInput" name="albumNameInput" value="">\n' +
                                    '<div class="input-group-append">\n' +
                                    '   <button class="btn btn-outline-secondary dropdown-toggle" onClick="return timelineBatchModal.toggleBatchTagAlbumDropdown();" id="tagalbumdropdown" type="button" aria-haspopup="true" aria-expanded="false">Albums</button>\n' +
                                    '   <div class="dropdown-menu" id="albumNameList">\n';

                                for (let index in albumList) {
                                    const album = albumList[index];
                                    batchHtml +=
                                        '<button class="dropdown-item" type="button">\n' +
                                        '    <input type="checkbox" onclick="return timelineBatchModal.populateBatchAlbum();" id="'+album.id+'" value="'+album.name+'" name="albums[]">\n' +
                                        '    <label for="'+album.id+'">'+album.name+'</label>\n' +
                                        '</button>\n';
                                }

                                batchHtml +=
                                    '   </div>\n' +
                                    '</div>\n';

                                $("#albumListForModal").html(batchHtml);
                            }

                            for (const index in metadataList) {
                                const metadata = metadataList[index];

                                let dateReformatted = "";
                                if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
                                    const dateObj = new Date(metadata.year, metadata.month-1, metadata.day);
                                    dateReformatted = dateObj.format("ddd, mmm dd, yyyy");
                                }

                                if ($("#tnbr"+metadata.id+".thumbnail-br").length === 0) {
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
                                    $("#bricon"+metadata.id).addClass(favoriteIcon);
                                    $("#briconcount"+metadata.id).text(favoriteCount);
                                }

                                if ($("#image" + metadata.id).length === 1) {
                                    $("#image" + metadata.id).attr("src",encodeURI(metadata.thumbnailUrlSmall));
                                    $("#image" + metadata.id).css("background-color","lightgray");
                                    $("#image" + metadata.id).attr("onError","shashin.errorImg(this,\''+metadata.title+'\',209)");
                                }

                                if ($("#tnbl"+metadata.id+".thumbnail-bl").length === 0) {
                                    $("#tnbl" + metadata.id).addClass("thumbnail-bl");
                                }

                                if ($("#tncentered"+metadata.id+".thumbnail-centered").length === 0) {
                                    $("#tncentered" + metadata.id).addClass("thumbnail-centered");
                                }

                                const mediaContent = {};
                                mediaContent.thumb = encodeURI(metadata.thumbnailUrlSmall);
                                mediaContent.subHtml = (metadata.placeName !== null ? '<a href="/map?lat=' + metadata.lat + '&lng=' + metadata.lng + '" target="_blank">' + metadata.placeName + '</a><br>' : '<br>') + metadata.title + (metadata.year === null || metadata.month === null || metadata.day === null ? '' : ' taken on ' + dateReformatted);
                                if (metadata.type.indexOf("video") >= 0) {
                                    mediaContent.video = '{"source": [{"src":"' + encodeURI(metadata.videoUrl) + '", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}';
                                    mediaContent.downloadUrl = encodeURI(metadata.videoUrl)+"/download";
                                    html =
                                        '<a class="mediaLink" id="mediaLink' + metadata.id + '" ' +
                                        'data-download-url="'+encodeURI(metadata.videoUrl)+'/download" ' +
                                        'data-metadataid="'+metadata.id+'" ' +
                                        'data-video="'+shashin.encodeHtml(mediaContent.video)+'" ';
                                    if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
                                        metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) {
                                        html +=
                                            'data-lg-size="'+metadata.thumbnailSmallWidth+'-'+metadata.thumbnailSmallHeight+'-'+metadata.thumbnailSmallWidth+','+metadata.originalImageWidth+'-'+metadata.originalImageHeight+'" ' +
                                            'data-responsive="'+encodeURI(metadata.thumbnailUrlSmall)+' '+metadata.thumbnailSmallWidth+'" ' +
                                            'data-thumb="'+encodeURI(metadata.thumbnailUrlSmall)+'" ' +
                                            'data-width="'+metadata.originalImageWidth+'" ';
                                    }
                                    html +=
                                        'data-sub-html="'+shashin.encodeHtml(mediaContent.subHtml)+'">' +
                                        '<span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>' +
                                        '</a>';

                                } else {
                                    mediaContent.src = metadata.thumbnailUrlOriginal;
                                    mediaContent.downloadUrl = encodeURI(metadata.thumbnailUrlOriginal);
                                    html =
                                        '<a class="mediaLink" id="mediaLink'+metadata.id+'" ' +
                                        'data-download-url="'+encodeURI(metadata.thumbnailUrlOriginal)+'" ' +
                                        'data-metadataid="'+metadata.id+'" ' +
                                        'data-src="'+encodeURI(metadata.thumbnailUrlOriginal)+'" ';
                                    if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
                                        metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) {
                                        html +=
                                            'data-lg-size="'+metadata.thumbnailSmallWidth+'-'+metadata.thumbnailSmallHeight+'-'+metadata.thumbnailSmallWidth+','+metadata.originalImageWidth+'-'+metadata.originalImageHeight+'" ' +
                                            'data-responsive="'+encodeURI(metadata.thumbnailUrlSmall)+' '+metadata.thumbnailSmallWidth+'" ' +
                                            'data-thumb="'+encodeURI(metadata.thumbnailUrlSmall)+'" ' +
                                            'data-width="'+metadata.originalImageWidth+'" ';
                                    }
                                    html +=
                                        'data-sub-html="'+shashin.encodeHtml(mediaContent.subHtml)+'">' +
                                        '<span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>' +
                                        '</a>';
                                }
                                if (metadata.originalImageWidth !== null) {
                                    mediaContent.width = metadata.originalImageWidth;
                                }
                                if ($("#mediaLink"+metadata.id).length === 0) {
                                    $("#tncentered"+metadata.id).append(html).ready(function () {
                                        timelineSettings.reinitLightGalleryInstance();
                                    });
                                }

                                const editIcon = (metadata.lat === null || metadata.lng === null) ? "bi-pencil-square" : "bi-pencil";
                                html = '<a href="#" id="timelineModalEdit'+metadata.id+'" data-bs-target="#propTimelinModal"><span class="'+editIcon+'" style="font-size: 1rem;color: lightgray;"></span></a>';
                                if ($("#timelineModalEdit"+metadata.id).length === 0) {
                                    $("#tnbl" + metadata.id).append(html);
                                    $("#timelineModalEdit"+metadata.id).attr("tag",JSON.stringify(metadata));
                                    $("#timelineModalEdit"+metadata.id).click(function(e) {
                                        e.preventDefault();

                                        const metadataObj = JSON.parse($(this).attr("tag"));
                                        timelineSettings.openTimelineModal(metadataObj,recognitionLabels,labelPhotoMap[metadataObj.id],albumList,albumMap[metadataObj.id]);
                                    });
                                    $("#timelineModalEdit"+metadata.id).attr("tag",JSON.stringify(metadata));
                                }

                                html = '<a href="#" id="select' + metadata.id + '"><span id="tlicon' + metadata.id + '" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span></a>';
                                if ($("#select"+metadata.id).length === 0) {
                                    $("#tntl" + metadata.id).append(html);
                                }
                                if ($("#tntl"+metadata.id+".thumbnail-tl").length === 0) {
                                    $("#tntl" + metadata.id).addClass("thumbnail-tl");
                                }

                                if (metadata.type.indexOf("video") >= 0) {
                                    const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                    html = '<span class="overlayIconBackground">'+duration+'&nbsp;<span id="video' + metadata.id + '" class="bi-camera-video overlayIcon"></span></span>';
                                    if ($("#video"+metadata.id).length === 0) {
                                        $("#tntr"+metadata.id).append(html);
                                    }
                                    if ($("#tntr"+metadata.id+".thumbnail-tr").length === 0) {
                                        $("#tntr" + metadata.id).addClass("thumbnail-tr");
                                    }
                                } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight*2) {
                                    html = '<span id="panorama' + metadata.id + '" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>';
                                    if ($("#panorama"+metadata.id).length === 0) {
                                        $("#tntr"+metadata.id).append(html);
                                    }
                                    if ($("#tntr"+metadata.id+".thumbnail-tr").length === 0) {
                                        $("#tntr" + metadata.id).addClass("thumbnail-tr");
                                    }
                                }

                                shashin.setPhotoOverlays(metadata, "timeline")
                                timelineSettings.activateMetadataListeners(metadata);
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

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating timeline. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        //const promise =
        return await $.ajax(ajaxParams)
        .fail(onFail).then(function (data) {
            // let deferred = new $.Deferred();
            let ret = "fail";
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList")) {
                        const metadataList = data["metadataList"] === "" ? null : data["metadataList"];

                        if (metadataList.length > 0) {
                            let html = "";

                            let dateString = shashin.getDateString(metadataList[0]["year"], metadataList[0]["month"], metadataList[0]["day"]);

                            let idCheck = "undated";
                            if (metadataList[0]["year"] === null ||
                                metadataList[0]["month"] === null ||
                                metadataList[0]["day"] === null)
                            {
                                html += '<br id="brundated"><section class="scrollspy" id="undated"><p><strong class="undatedTimelinePhotos p-1">Undated</strong></p></section>\n' +
                                    '<div class="row p-3" id="rowundated">\n';
                            } else {
                                idCheck = metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day;
                                html += '<br id="br' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"><section class="scrollspy" id="' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"><p><strong class="dateHeading p-1">' + dateString + '</strong></p></section>\n' +
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
                                            '   <img class="photo-thumbnail-image thumbnailTag_undated" id="image'+metadata.id+'" width="'+metadata.thumbnailSmallWidth+'" height="'+metadata.thumbnailSmallHeight+'">\n';

                                    } else {
                                        html +=
                                            '   <input type="hidden" name="thumbnailUrl-' + metadata.year + '-' + metadata.month + '-' + metadata.day + '[]" id="thumbnailUrl_' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlSmall) + '">\n' +
                                            '   <img class="photo-thumbnail-image thumbnailTag_'+metadata.year + '-' + metadata.month + '-' + metadata.day+'" id="image'+metadata.id+'" width="'+metadata.thumbnailSmallWidth+'" height="'+metadata.thumbnailSmallHeight+'">\n';
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

                                const lastDateParts = $("#offcanvasTocBody").children().last().attr("id").split("offcanvas_");
                                const lastDate = lastDateParts[1];

                                if (metadataList[0].year == null || metadataList[0].month == null || metadataList[0].day == null) {
                                    html += '<span class="scrollspy metadataprocessed" id="tail_undated"></span>';
                                    html += '</div><span class="attachMetadataPhotos" id="amp_undated" style="visibility: hidden">EOL</span>';
                                } else if (lastDate === (metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day)) {
                                    html += '<span class="scrollspy metadataprocessed" id="tail_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                    html += '</div><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '" style="visibility: hidden">EOL</span>';
                                } else {
                                    html += '<span class="scrollspy metadataprocessed" id="tail_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                    html += '</div><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                }

                                if (action === "above") {
                                    $(html).insertBefore($("#br" + attachToId)).ready(function () {
                                        // deferred.resolve("success");
                                        ret = "success";
                                    });
                                } else if (action === "new") {
                                    $("#infinite-scroll-gallery").prepend(html).ready(function () {
                                        // deferred.resolve("success");
                                        ret = "success";
                                    });
                                } else {
                                    if (attachToId == null) {
                                        if ($(".attachMetadataPhotos").length > 0) {
                                            $(html).insertAfter($(".attachMetadataPhotos").last()).ready(function () {
                                                // deferred.resolve("success");
                                                ret = "success";
                                            });
                                        } else {
                                            $("#infinite-scroll-gallery").prepend(html).ready(function () {
                                                // deferred.resolve("success");
                                                ret = "success";
                                            });
                                        }
                                    } else {
                                        $(html).insertAfter($("#amp_" + attachToId)).ready(function () {
                                            // deferred.resolve("success");
                                            ret = "success";
                                        });
                                    }
                                }
                            } else {
                                // Already attached
                                // deferred.resolve("success");
                                ret = "success";
                            }
                        } else {
                            $(".attachMetadataPhotos").last().text("EOL").css("display", "none")
                            // deferred.resolve("success");
                            ret = "success";
                        }
                        ret = "success";
                    }
                    ret = "success";
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                    // deferred.resolve("fail");
                    ret = "fail";
                }
            }

            //deferred.resolve("success");
            $("#spinner").css("display", "none");
            // return deferred.promise();
            return ret;
        });

        // return promise.done(function(data) {
        //     return data;
        // });
    }

    timelineSettings.activateMetadataListeners = function(metadata) {
        //const metadata = JSON.parse(shashin.decodeHtml(rawMetadata));

        $("#image"+metadata.id).on('load', function() {
            $(this).css("background-color","transparent");
        });

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

    timelineSettings.validateMetadataInputs = function(day, month, year, time, offset, latlng, msgId) {
        if (offset === null ) {
            offset = "";
        }
        const dayValidate = "([1-9]|[12]\d|3[01])";
        const monthValidate = "^(0?[1-9]|1[012])$";
        const timeValidate = "^(\\d{2}:\\d{2}:\\d{2})$";
        const offsetValidate = "^([+-±](?:2[0-3]|[01][0-9]):[0-5][0-9])$";

        let msg = "";
        if (day !== "" && !day.match(dayValidate)) {
            msg = "Enter Valid Day";
        }

        if (month !== "" && !month.match(monthValidate)) {
            msg = "Enter Valid Month";
        }

        if (year !== "" && !(+year >= 1888 && +year <= new Date().getFullYear())) {
            msg = "Enter Valid Year";
        }

        if (time !== "" && !time.match(timeValidate)) {
            msg = "Enter Valid Time";
        }

        if (offset !== "" && !offset.match(offsetValidate)) {
            msg = "Enter Valid Offset";
        }

        if (latlng !== "") {
            latlng = $.trim(latlng);
            const latlngArr = latlng.split(",");

            if (latlngArr.length !== 2 || latlng.split(".").length !== 3 || !shashin.isNumeric(latlngArr[0]) || !shashin.isNumeric(latlngArr[1])) {
                msg = "Enter Valid Latitude/Longitude";
            }
        }

        if (msg !== "") {
            $("#"+msgId).html('<div class="alert alert-danger" role="alert">'+msg+'</div>');
            return false;
        } else {
            return true;
        }

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