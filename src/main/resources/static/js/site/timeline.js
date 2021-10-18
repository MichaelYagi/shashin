(function( timelineSettings, $, undefined ) {
    timelineSettings.lightGalleryEl = document.getElementById('infinite-scroll-gallery');
    timelineSettings.lightGalleryConfigs = shashin.getLightGalleryConfigs();
    timelineSettings.lightGalleryConfigs["selector"] = '.mediaLink';
    timelineSettings.lg = null; //lightGallery(timelineSettings.lightGalleryEl, timelineSettings.lightGalleryConfigs);
    timelineSettings.enableScrollSpy = true;
    timelineSettings.prevAnchor = "";
    timelineSettings.lastOffset = $("#container").scrollTop() ? $("#container").scrollTop() : $("#main").scrollTop();
    timelineSettings.lastDate = new Date().getTime();
    timelineSettings.lastScrollTop = 0;
    timelineSettings.scrollSpeedInpxPerMs = 0;
    timelineSettings.scrollDirection = "down";
    timelineSettings.successBelowMsg = "success_below";
    timelineSettings.successAboveMsg = "success_above";
    timelineSettings.successMidMsg = "success_mid";
    timelineSettings.retryLimit = 3;
    timelineSettings.tryCount = 0;

    timelineSettings.setLightGalleryElement = function (name) {
        if (document.getElementById(name)) {
            timelineSettings.infiniteScrollGallery = document.getElementById(name);
        }
    };

    timelineSettings.setLightGallery = function () {
        timelineSettings.lg = lightGallery(timelineSettings.getLightGalleryElement(), timelineSettings.lightGalleryConfigs);
    }

    timelineSettings.getLightGalleryElement = function () {
        return timelineSettings.infiniteScrollGallery;
    };

    timelineSettings.getLightGallery = function () {
        return timelineSettings.lg;
    }

    timelineSettings.jumpToLightGalleryMetadata = function (metadataId) {
        // const aTag = $("#lightGalleryIndex" + metadataId);
        // $("#container").animate({scrollTop: aTag.offset().top},'slow');
        // $("main").animate({scrollTop: aTag.offset().top},'slow');

        const url = location.href;
        location.href = '#lightGalleryIndex'+metadataId;
        history.replaceState(null,null,url);
    }

    timelineSettings.refreshTimeline = function (mediaTypeFilter,currentOffCanvasId) {
        $.ajax({
            type: 'get',
            url: "/timeline/dates/"+mediaTypeFilter,
            contentType: 'application/json; charset=utf-8',
            async:true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error refreshing timeline TOC. Attempt: "+timelineSettings.tryCount+"/"+timelineSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                timelineSettings.tryCount++;
                if (timelineSettings.tryCount <= timelineSettings.retryLimit) {
                    //try again
                    timelineSettings.refreshTimeline(mediaTypeFilter);
                }
            }
        }).then(function(data) {
            timelineSettings.tryCount = 0;
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
                        html += '<a id="'+offcanvasDate+'" class="list-group-item list-group-item-action'+active+'" onclick="return timelineSettings.jumpToTimelineToc(event,\'undated\',\''+mediaTypeFilter+'\')" href="#undated">'+text+'</a>\n';
                    } else {
                        offcanvasDate = "offcanvas_"+year+"-"+month+"-"+day;
                        if (currentOffCanvasId === offcanvasDate) {
                            active = " active";
                        }
                        const dateObj = new Date(year, month-1, day);
                        text = dateObj.format("mmm d, yyyy");
                        html += '<a id="'+offcanvasDate+'" class="list-group-item list-group-item-action'+active+'" onclick="return timelineSettings.jumpToTimelineToc(event,\''+year+'-'+month+'-'+day+'\',\''+mediaTypeFilter+'\')" href="#'+year+'-'+month+'-'+day+'">'+text+'</a>\n';
                    }
                }
                $("#offcanvasTocBody").append(html);
            }
        });
    }

    timelineSettings.reinitLightGalleryInstance = function () {
        if (timelineSettings.getLightGallery() !== null) {
            const closeTimeout = timelineSettings.getLightGallery().closeGallery(true);
            setTimeout(() => {
                if (timelineSettings.getLightGallery() !== null) {
                    timelineSettings.getLightGallery().destroyModules(true);
                    timelineSettings.getLightGallery().invalidateItems();
                    $(window).off(`.lg.global${timelineSettings.getLightGallery().lgId}`);
                    timelineSettings.getLightGallery().LGel.off('.lg');
                    // https://github.com/sachinchoolur/lightGallery/blob/383d51852657ab44bb8697748c570cf110723f97/src/lightgallery.ts#L2396
                    // Hack because lg.destroy() errors out
                    // when photos appear slower than destroy called, then there's an error
                    try {
                        timelineSettings.getLightGallery().$container.remove();
                    } catch (e) {
                        shashin.printMessageToConsole(e)
                    }

                    timelineSettings.lg = null;

                    setTimeout(() => {
                        timelineSettings.setLightGallery();
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

    timelineSettings.renderThumbnailsInViewport = function (elements,mediaTypeFilter) {
        // If no scrollspy elements found, find current thumbnail container
        // and closest previous scrollspy element
        if (elements.length === 0) {
            const thumbnailsInViewport = $(".photo-thumbnail-container").withinviewport();
            elements = $(thumbnailsInViewport.parent().prevAll(".scrollspy")[0])
        }

        elements.each(function(index) {
            let id = $(this).attr("id");
            if (id.indexOf("tail_") === -1 && timelineSettings.prevAnchor !== id && (index === 0 || index === 1)) {
                timelineSettings.renderThumbnails(id,mediaTypeFilter).then(function (msg) {
                    if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                        timelineSettings.setScrollSpyActive(id);
                        timelineSettings.reinitLightGalleryInstance();
                    }
                });
                timelineSettings.prevAnchor = id;
            }
        });
    }

    timelineSettings.jumpToTimelineToc = function (e,anchor,mediaTypeFilter) {
        e.preventDefault();

        timelineSettings.scrollDirection = "down";
        timelineSettings.enableScrollSpy = false;

        shashin.printMessageToConsole("jumpToTimelineToc anchor:"+anchor);
        shashin.printMessageToConsole("jumpToTimelineToc mediaTypeFilter:"+mediaTypeFilter);

        timelineSettings.renderThumbnails(anchor,mediaTypeFilter).then(function (msg) {
            if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                timelineSettings.setScrollSpyActive(anchor);
                timelineSettings.reinitLightGalleryInstance();
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
        const timer = setInterval(function () {
            if (navElem.attr("class") === 'list-group-item list-group-item-action active') {
                timelineSettings.enableScrollSpy = true;
                clearInterval(timer);
            }
        }, 200);
    }

    // Set the active nav
    timelineSettings.setScrollSpyActive = function (id) {
        const navElem = $('a[href="#' + id + '"]');
        navElem.addClass('active').siblings().removeClass('active');
    }

    timelineSettings.scrollToTimeline = function(elementsInViewport) {
        elementsInViewport.each(function(index) {
            let id = $(this).attr("id");
            if (id.indexOf("tail_") < 0 && $("#offcanvas_"+id).length > 0 && (index === 0 || index === 1)) {
                document.getElementById("offcanvas_"+id).scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    }

    timelineSettings.openTimelineModal = function(metadata,recognitionLabels,taggedPeopleList) {
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

        var latlngValue = (metadata.lat == null || metadata.lng == null || metadata.lat === "" || metadata.lng === "") ? '' : ($.trim(metadata.lat) + ',' + $.trim(metadata.lng));
        $("#latlng").val(latlngValue);

        var taggedPeopleArray = taggedPeopleList.split(",");
        var isObject = false;
        var taggedPeopleString = "";
        for (var index in taggedPeopleArray) {
            var person = taggedPeopleArray[index];
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
            var html = '<div class="input-group-append" id="recognitionLabelInput">\n' +
                '           <button class="btn btn-outline-secondary dropdown-toggle" onclick="return timelineModal.toggleTagPeopleDropdown(\'' + metadata.id + '\');" id="tagpeopledropdown'+metadata.id+'" type="button" aria-haspopup="true" aria-expanded="false">People</button>\n' +
                '           <div class="dropdown-menu" id="recognitionLabelsList">\n';
            for (var index in recognitionLabels) {
                var recognitionLabel = recognitionLabels[index];
                var checkedString = "";
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

        if (isObject === true) {
            $("#isobject")[0].checked = true;
        }

        if (metadata.hidden !== null && metadata.hidden === true) {
            $("#hidden")[0].checked = true;
        }

        $("#keywords").val(metadata.keywords);

        timelineSettings.populateDetailsTab(metadata);

        // Open modal window
        $("#propTimelineModal").modal('show');
    }

    // Render only what's needed
    timelineSettings.renderThumbnails = function(id,mediaTypeFilter) {
        let deferred = new $.Deferred();

        let queryLimit = 3;

        // Depth of results in section of page above and below anchor
        let depthDown = queryLimit;
        let depthUp = queryLimit;

        shashin.printMessageToConsole("scrollSpeedInpxPerMs:"+timelineSettings.scrollSpeedInpxPerMs);
        shashin.printMessageToConsole("scrollDirection:"+timelineSettings.scrollDirection);

        // If velocity of scroll is really fast, add padding to # of results
        if (timelineSettings.scrollSpeedInpxPerMs > 5 || timelineSettings.scrollSpeedInpxPerMs < -5) {
            if (timelineSettings.scrollDirection === "down") {
                depthDown = depthDown + 3;
            } else {
                depthUp = depthUp + 3;
            }
        }

        shashin.printMessageToConsole("depthDown:"+depthDown);
        shashin.printMessageToConsole("depthUp:"+depthUp);
        shashin.printMessageToConsole("renderThumbnails id:"+id);

        let offCanvasId = $("#offcanvas_"+id);

        // Remove elements that are not visible
        let prevElementId = "";
        $('section').each(function(index, element) {
            shashin.printMessageToConsole(element.id + " checking to remove beginning");
            if ($("#"+element.id).length > 1 || prevElementId === element.id) {
                shashin.printMessageToConsole(element.id + " removed beginning");
                $("#br"+element.id).remove();
                $("#row"+element.id).remove();
                $("#amp_"+element.id).remove();
                $("#tail_"+element.id).remove();
                $("#"+element.id).remove();
            }
            prevElementId = element.id;
        });

        // Hack to prevent infinite scroll upwards and throttle scrolling
        if (timelineSettings.scrollDirection === "up" && $('section')[0].id === id && timelineSettings.enableScrollSpy === true) {
            document.getElementById("container").scrollBy({top: 1});
            if (document.getElementsByTagName("MAIN").length > 0) {
                document.getElementsByTagName("MAIN")[0].scrollBy({top: 1});
            }
        }

        const attachAboveArray = [];
        let tempOffCanvasIdAbove = offCanvasId;
        for (let i = 0; i <= depthUp - 1; i++) {
            tempOffCanvasIdAbove = tempOffCanvasIdAbove.prev();
            if (typeof tempOffCanvasIdAbove.attr("id") !== 'undefined') {
                const offcanvasIdParts = tempOffCanvasIdAbove.attr("id").split("_");
                const offcanvasId = offcanvasIdParts[1];
                attachAboveArray.unshift(offcanvasId);
            }
        }

        const attachBelowArray = [];
        let tempOffCanvasIdBelow = offCanvasId;
        for (let i = 0;i <= depthDown-1;i++) {
            tempOffCanvasIdBelow = tempOffCanvasIdBelow.next();
            if (typeof tempOffCanvasIdBelow.attr("id") !== 'undefined') {
                const offcanvasIdParts = tempOffCanvasIdBelow.attr("id").split("_");
                const offcanvasId = offcanvasIdParts[1];
                attachBelowArray.push(offcanvasId);
            }
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

                timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint).then(function (msg) {
                    if (msg === "success" && $("#"+currentId).length > 0 && $("#tail_"+currentId).length > 0) {
                        timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                    }
                });
                action = "below";
            }
            attachPoint = currentId;
        }

        // Render bottom
        action = "below"
        if (attachAboveArray.length === 0 && $("#"+id).length === 0) {
            attachPoint = null
        }
        // attachPoint = id;
        for (let index in attachBelowArray) {
            const currentId = attachBelowArray[index];
            shashin.printMessageToConsole("attempting to attaching id below:" + currentId);
            if ($("#"+currentId).length === 0) {
                shashin.printMessageToConsole("attaching below attachPoint:" + attachPoint);
                shashin.printMessageToConsole("attaching id:" + currentId);
                shashin.printMessageToConsole("actionBelow:"+action)

                timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint).then(function (msg) {
                    if (msg === "success" && $("#"+currentId).length > 0 && $("#tail_"+currentId).length > 0) {
                        timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                    }
                });
            }
            attachPoint = currentId;
        }

        // Render mid
        action = "new";
        if (attachAboveArray.length > 0) {
            attachPoint = attachAboveArray[attachAboveArray.length-1];
            action = "below";
        } else if (attachBelowArray.length > 0) {
            attachPoint = attachBelowArray[0];
            action = "above";
        }

        shashin.printMessageToConsole("attempting to attaching id mid:"+id)

        if ($("#"+id).length === 0) {
            shashin.printMessageToConsole("attaching mid attachPoint:"+attachPoint)
            shashin.printMessageToConsole("attaching id:" + id);
            shashin.printMessageToConsole("attaching mid action:"+action)

            timelineSettings.updateTimeline(id, mediaTypeFilter, action, attachPoint).then(function (msg) {
                if (msg === "success" && $("#"+id).length > 0 && $("#tail_"+id).length > 0) {
                    timelineSettings.attachAssociatedMetadata(id, mediaTypeFilter);
                }
            });
        }

        // Remove elements that are not visible
        prevElementId = "";
        $('section').each(function(index, element) {
            shashin.printMessageToConsole(element.id + " checking to remove end");
            if (($.inArray(element.id, attachAboveArray) === -1 && $.inArray(element.id, attachBelowArray) === -1 && element.id !== id) || ($("#"+element.id).length > 1 || prevElementId === element.id)) {
                shashin.printMessageToConsole(element.id + " removed end");

                $("#br"+element.id).remove();
                $("#row"+element.id).remove();
                $("#amp_"+element.id).remove();
                $("#tail_"+element.id).remove();
                $("#"+element.id).remove();
            }
            prevElementId = element.id;
        });

        shashin.printMessageToConsole("==============================================");

        deferred.resolve(timelineSettings.successMidMsg);
        return deferred.promise();
    }

    // Hook up data to edit albums, favorites and people labels
    timelineSettings.attachAssociatedMetadata = function(date,mediaTypeFilter) {
        $.ajax({
            type: 'get',
            url: "/timeline/mediatype/"+mediaTypeFilter+"/date/"+date,
            contentType: 'application/json; charset=utf-8',
            async:true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error attaching associated metadata. Attempt: "+timelineSettings.tryCount+"/"+timelineSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                timelineSettings.tryCount++;
                if (timelineSettings.tryCount <= timelineSettings.retryLimit) {
                    //try again
                    timelineSettings.attachAssociatedMetadata(date,mediaTypeFilter);
                }
            }
        }).then(function(data) {
            timelineSettings.tryCount = 0;
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

                        if (metadataList.length > 0) {

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

                                html = '<img src="'+encodeURI(metadata.thumbnailUrlSmall)+'" class="photo-thumbnail-image" id="image'+metadata.id+'" width="'+metadata.thumbnailSmallWidth+'" height="'+metadata.thumbnailSmallHeight+'" style="background-color:lightgray;" onError="shashin.errorImg(this,\''+metadata.title+'\',209)">';
                                if ($("#image" + metadata.id).length === 0) {
                                    $("#photoThumbnailContainer" + metadata.id).prepend(html);
                                }

                                // const img = new Image();
                                // img.onload = function () {
                                //     const imageId = img.id;
                                //     const metadataId = imageId.substring("image".length,imageId.length);
                                //     $("#photoThumbnailContainer"+metadataId).prepend(img);
                                // }
                                // img.src = metadata.thumbnailUrlSmall;
                                // img.className = "photo-thumbnail-image";
                                // img.id = "image"+metadata.id;
                                // img.width = metadata.thumbnailSmallWidth;
                                // img.height = metadata.thumbnailSmallHeight;
                                // img.style.backgroundColor  = "lightgray";

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
                                    html =
                                        '<a class="mediaLink" id="mediaLink' + metadata.id + '" ' +
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
                                    html =
                                        '<a class="mediaLink" id="mediaLink'+metadata.id+'" ' +
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
                                    $("#tncentered"+metadata.id).append(html);
                                }

                                const editIcon = (metadata.lat === null || metadata.lng === null) ? "bi-pencil-square" : "bi-pencil";
                                html = '<a href="#" id="timelineModalEdit'+metadata.id+'" data-bs-target="#propTimelinModal"><span class="'+editIcon+'" style="font-size: 1rem;color: lightgray;"></span></a>';
                                if ($("#timelineModalEdit"+metadata.id).length === 0) {
                                    $("#tnbl" + metadata.id).append(html);
                                    $("#timelineModalEdit"+metadata.id).attr("tag",JSON.stringify(metadata));
                                    $("#timelineModalEdit"+metadata.id).click(function(e) {
                                        e.preventDefault();

                                        const metadataObj = JSON.parse($(this).attr("tag"));
                                        timelineSettings.openTimelineModal(metadataObj,recognitionLabels,labelPhotoMap[metadataObj.id]);
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

    timelineSettings.updateTimeline = function(date,mediaTypeFilter,action,attachToId) {
        const promise = $.ajax({
            type: 'get',
            url: "/timeline/mediatype/" + mediaTypeFilter + "/date/" + date,
            contentType: 'application/json; charset=utf-8',
            async: false
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating timeline. Attempt: "+timelineSettings.tryCount+"/"+timelineSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                timelineSettings.tryCount++;
                if (timelineSettings.tryCount <= timelineSettings.retryLimit) {
                    //try again
                    timelineSettings.updateTimeline(date,mediaTypeFilter,action,attachToId);
                }
            }
        }).then(function (data) {
            timelineSettings.tryCount = 0;
            let deferred = new $.Deferred();
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("metadataList") &&
                        data.hasOwnProperty("favorites") &&
                        data.hasOwnProperty("albumList") &&
                        data.hasOwnProperty("recognitionLabels") &&
                        data.hasOwnProperty("labelPhotoMap")
                    ) {
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
                                            '   <input type="hidden" name="thumbnailUrl-undated[]" id="thumbnailUrl_' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlSmall) + '">';
                                    } else {
                                        html +=
                                            '   <input type="hidden" name="thumbnailUrl-' + metadata.year + '-' + metadata.month + '-' + metadata.day + '[]" id="thumbnailUrl_' + metadata.id + '" value="' + encodeURI(metadata.thumbnailUrlSmall) + '">';
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

                                if (metadataList[0].year == null || metadataList[0].month == null || metadataList[0].day == null) {
                                    html += '<span class="scrollspy metadataprocessed" id="tail_undated"></span>';
                                    html += '</div><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                } else {
                                    html += '<span class="scrollspy metadataprocessed" id="tail_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                    html += '</div><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                }

                                if (action === "above") {
                                    $(html).insertBefore($("#br" + attachToId)).ready(function () {
                                        deferred.resolve("success");
                                    });
                                } else if (action === "new") {
                                    $("#infinite-scroll-gallery").prepend(html).ready(function () {
                                        deferred.resolve("success");
                                    });
                                } else {
                                    if (attachToId == null) {
                                        if ($(".attachMetadataPhotos").length > 0) {
                                            $(html).insertAfter($(".attachMetadataPhotos").last()).ready(function () {
                                                deferred.resolve("success");
                                            });
                                        } else {
                                            $("#infinite-scroll-gallery").prepend(html).ready(function () {
                                                deferred.resolve("success");
                                            });
                                        }
                                    } else {
                                        $(html).insertAfter($("#amp_" + attachToId)).ready(function () {
                                            deferred.resolve("success");
                                        });
                                    }
                                }
                            } else {
                                // Already attached
                                deferred.resolve("success");
                            }
                        } else {
                            $(".attachMetadataPhotos").last().text("EOL").css("display", "none")
                            deferred.resolve("success");
                        }
                    }
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                    deferred.resolve("fail");
                }
            }

            //deferred.resolve("success");
            $("#spinner").css("display", "none");
            return deferred.promise();

        });

        return promise.done(function(data) {
            return data;
        });
    }

    timelineSettings.populateDetailsTab = function(metadata) {
        // Clear data
        $("#pathDetails").text("");
        $("#typeDetails").text("");
        $("#isoDetails").text("");
        $("#exposureDetails").text("");
        $("#fNumberDetails").text("");
        $("#focalLengthDetails").text("");
        $("#cameraDetails").text("");
        $("#lensDetails").text("");
        $("#qualityDetails").text("");
        $("#createdAtDetails").text("");
        $("#modifiedAtDetails").text("");
        $("#takenAtDetails").text("");
        $("#manualTakenAtDetails").text("");
        $("#timeZoneDetails").text("");

        // Fill in details tab data
        if (metadata.path != null) {
            $("#pathDetails").text(metadata.path);
        }
        if (metadata.type != null) {
            $("#typeDetails").text(metadata.type);
        }
        if (metadata.iso != null) {
            $("#isoDetails").text(metadata.iso);
        }
        if (metadata.exposure != null) {
            $("#exposureDetails").text(metadata.exposure);
        }
        if (metadata.fNumber != null) {
            $("#fNumberDetails").text(metadata.fNumber);
        }
        if (metadata.focalLength != null) {
            $("#focalLengthDetails").text(metadata.focalLength);
        }
        if (metadata.camera != null) {
            $("#cameraDetails").text(metadata.camera);
        }
        if (metadata.lens != null) {
            $("#lensDetails").text(metadata.lens);
        }
        if (metadata.quality != null) {
            $("#qualityDetails").text(metadata.quality);
        }
        if (metadata.createdAt != null) {
            $("#createdAtDetails").text(metadata.createdAt);
        }
        if (metadata.modifiedAt != null) {
            $("#modifiedAtDetails").text(metadata.modifiedAt);
        }
        if (metadata.takenAt != null) {
            $("#takenAtDetails").text(metadata.takenAt);
        }
        if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
            var takenDetails = metadata.year+'-'+metadata.month+'-'+metadata.day;
            if (metadata.time !== null && metadata.time !== "") {
                takenDetails += ' ' + metadata.time;
            }
            $("#manualTakenAtDetails").text(takenDetails);
        }
        if (metadata.timeZone != null) {
            $("#timeZoneDetails").text(metadata.timeZone);
        }
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
        var dayValidate = "([1-9]|[12]\d|3[01])";
        var monthValidate = "^(0?[1-9]|1[012])$";
        var timeValidate = "^(\\d{2}:\\d{2}:\\d{2})$";
        var offsetValidate = "^([+-±](?:2[0-3]|[01][0-9]):[0-5][0-9])$";

        var msg = "";
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
            var latlngArr = latlng.split(",");

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