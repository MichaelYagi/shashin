(function( timelineSettings, $, undefined ) {
    timelineSettings.enableScrollSpy = true;
    timelineSettings.prevAnchor = "";
    timelineSettings.successBelowMsg = "success_below";
    timelineSettings.successAboveMsg = "success_above";
    timelineSettings.successMidMsg = "success_mid";
    timelineSettings.scrollDirection = "down";
    timelineSettings.lastScrollTop = 0;

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

    let prevElements = null;
    timelineSettings.renderThumbnailsInViewport = function (elements,mediaTypeFilter) {
        const lastDate = $("#offcanvasTocBody").children().last().attr("id").split("offcanvas_")[1];

        if (prevElements === null || JSON.stringify(prevElements) !== JSON.stringify(elements) || ($("#"+lastDate).withinviewport() === 0 && $("footer").withinviewport().length > 0)) {

            // If no scrollspy elements found, find current thumbnail container
            // and closest previous scrollspy element
            if (elements.length === 0) {
                const thumbnailsInViewport = $(".photo-thumbnail-container").withinviewport();
                elements = $(thumbnailsInViewport.parent().prevAll(".scrollspy")[0])
            }

            elements.each(function (index) {
                let id = $(this).attr("id");

                if (id.indexOf("tail_") === -1 && index < 2 && timelineSettings.prevAnchor !== id) {
                    timelineSettings.renderThumbnails(id, mediaTypeFilter).then(function (msg) {
                        if (msg === timelineSettings.successBelowMsg || msg === timelineSettings.successAboveMsg || msg === timelineSettings.successMidMsg) {
                            timelineSettings.setScrollSpyActive(id);
                        }
                    });
                    timelineSettings.prevAnchor = id;
                }
            });
        }

        prevElements = elements;
    }

    // Render only what's needed
    timelineSettings.renderThumbnails = async function(id,mediaTypeFilter) {

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

        let depth = idsInView.length < 2 ? 2 : idsInView.length;
        let depthDown = depth-1; //2;
        let depthUp = depth; //3;

        shashin.printMessageToConsole("depthDown:"+depthDown);
        shashin.printMessageToConsole("depthUp:"+depthUp);
        shashin.printMessageToConsole("renderThumbnails id:"+id);

        let offCanvasId = $("#offcanvas_"+id);

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

        // Remove elements that are not visible
        let prevElementId = "";
        let topHeight = 0;
        let bottomHeight = 0;
        const tempScrollTop = $("#container").scrollTop();
        $('section').each(function (index, element) {
            shashin.printMessageToConsole(element.id + " checking to remove end");
            if (($.inArray(element.id, attachAboveArray) === -1 && $.inArray(element.id, attachBelowArray) === -1 && element.id !== id) || ($("#" + element.id).length > 1 || prevElementId === element.id)) {

                // Get height to set scrollTop for non chrome browsers
                if (timelineSettings.scrollDirection === "down" && shashin.getDateObject(id) < shashin.getDateObject(element.id)) {
                    topHeight += $("#br" + element.id).outerHeight(true) +
                       $("#row" + element.id).outerHeight(true) +
                       $("#amp_" + element.id).outerHeight(true) +
                       $("#tail_" + element.id).outerHeight(true) +
                       $("#" + element.id).outerHeight(true);
                } else {
                    bottomHeight += $("#br" + element.id).outerHeight(true) +
                        $("#row" + element.id).outerHeight(true) +
                        $("#amp_" + element.id).outerHeight(true) +
                        $("#tail_" + element.id).outerHeight(true) +
                        $("#" + element.id).outerHeight(true);
                }

                shashin.printMessageToConsole(element.id + " removed end");
                shashin.removeDateGallery(element.id);
            }
            prevElementId = element.id;
        });

        // Smooth scrolling when element is removed for non chrome browsers
        if (shashin.isChrome() === false && timelineSettings.scrollDirection === "down" && topHeight > 0) {
            $("#container").scrollTop(tempScrollTop - topHeight);
            timelineSettings.lastScrollTop = (tempScrollTop - topHeight);
        }

        shashin.printMessageToConsole("attachAboveArray");
        shashin.printMessageToConsole(attachAboveArray);
        shashin.printMessageToConsole("attachBelowArray");
        shashin.printMessageToConsole(attachBelowArray);

        // Render top
        let action = "new";
        let attachPoint = id;
        let topHeightAdded = 0;

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
                if (msg === "success" && $("#"+currentId).length === 1) {
                    timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                }

                if (timelineSettings.scrollDirection === "up") {
                    topHeightAdded +=
                        $("#br" + currentId).height() +
                        $("#" + currentId).height() +
                        $("#row" + currentId).height();
                }

                action = "below";
            }
            attachPoint = currentId;
        }

        if (shashin.isChrome() === false &&
            //shashin.isSafari() === true &&
            timelineSettings.scrollDirection === "up" &&
            topHeightAdded > 0 && bottomHeight > 0
        ) {
            $("#container").scrollTop(tempScrollTop + topHeightAdded);
            timelineSettings.lastScrollTop = (tempScrollTop + topHeightAdded);
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
                if (msg === "success" && $("#"+currentId).length === 1) {
                    timelineSettings.attachAssociatedMetadata(currentId, mediaTypeFilter);
                }
            }
            attachPoint = currentId;
        }

        let rendered = false;
        while (true) {
            let dateFound = false;
            let currentId = attachPoint;
            $("#offcanvasTocBody").children().each(function () {
                const dateParts = $(this).attr("id").split("offcanvas_");
                const date = dateParts[1];
                if ($(this).next().length > 0 && currentId === date) {
                    currentId = $(this).next().attr("id").split("offcanvas_")[1];
                    dateFound = true;
                    return false;
                }

            });

            if (dateFound === true && currentId !== null && $("#" + currentId).length === 0) {
                if (action === "new") {
                    attachPoint = null;
                }

                const msg = await timelineSettings.updateTimeline(currentId, mediaTypeFilter, action, attachPoint)
                if (msg === "success" && $("#" + currentId).length === 1) {
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

        // Render mid
        action = "new";
        if (attachAboveArray.length > 0) {
            attachPoint = attachAboveArray[attachAboveArray.length-1];
            action = "below";
        } else if (attachBelowArray.length > 0) {
            attachPoint = attachBelowArray[0];
            action = "above";
        }

        shashin.printMessageToConsole("attempting to attaching id mid "+id+" "+action+" "+attachPoint+" length "+$("#"+id).length)

        // Hack for attaching mid point
        if (attachAboveArray.length > 0 && attachBelowArray.length > 0 && $('section')[$('section').length-1].id === id && $("#"+id).length === 1) {
            shashin.printMessageToConsole("removing already existing id "+id+" for mid point")
            shashin.removeDateGallery(id);
        }

        // Render mid
        if ($("#"+id).length === 0) {
            shashin.printMessageToConsole("attaching mid attachPoint:"+attachPoint)
            shashin.printMessageToConsole("attaching id:" + id);
            shashin.printMessageToConsole("attaching mid action:"+action)
            const msg = await timelineSettings.updateTimeline(id, mediaTypeFilter, action, attachPoint);
            if (msg === "success" && $("#"+id).length === 1) {
                timelineSettings.attachAssociatedMetadata(id, mediaTypeFilter);
            }
        }

        shashin.printMessageToConsole("==============================================");
        //deferred.resolve(timelineSettings.successMidMsg);
        //return deferred.promise();

        return timelineSettings.successMidMsg;
    }

    timelineSettings.jumpFromTimelineToc = function (e,anchor,mediaTypeFilter) {
        e.preventDefault();

        timelineSettings.scrollDirection = "down";
        timelineSettings.enableScrollSpy = false;

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

        shashin.printMessageToConsole("jumpFromTimelineToc anchor:"+anchor);
        shashin.printMessageToConsole("jumpFromTimelineToc mediaTypeFilter:"+mediaTypeFilter);

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
        const timer = setInterval(function () {
            if (navElem.hasClass("active") === true) {
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
                                    '           <div class="dropdown-menu" id="peopleNameList">';

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
                                    $("#image" + metadata.id).attr("onError","shashin.errorImg(this,\'"+metadata.title+"\',209)");
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
                                        shashin.openEditMetadataModal(metadataObj,recognitionLabels,labelPhotoMap[metadataObj.id],albumList,albumMap[metadataObj.id]);
                                    });
                                }

                                html = '<a href="#" id="select' + metadata.id + '"><span id="tlicon' + metadata.id + '" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span></a>';
                                if ($("#select"+metadata.id).length === 0) {
                                    $("#tntl" + metadata.id).append(html);
                                }
                                if ($("#tntl"+metadata.id+".thumbnail-tl").length === 0) {
                                    $("#tntl" + metadata.id).addClass("thumbnail-tl");
                                    shashin.setPhotoOverlays(metadata, "timeline")
                                    timelineSettings.activateMetadataListeners(metadata);
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
                                        html += '</div></span><span class="attachMetadataPhotos" id="amp_undated" style="visibility: hidden">EOL</span>';
                                    } else if (lastDate === (metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day)) {
                                        html += '<span class="scrollspy metadataprocessed" id="tail_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                        html += '</div></span><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '" style="visibility: hidden">EOL</span>';
                                    } else {
                                        html += '<span class="scrollspy metadataprocessed" id="tail_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                        html += '</div></span><span class="attachMetadataPhotos" id="amp_' + metadataList[0].year + '-' + metadataList[0].month + '-' + metadataList[0].day + '"></span>';
                                    }

                                    if (action === "above") {
                                        $(html).insertBefore($("#container_" + attachToId)).ready(function () {
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