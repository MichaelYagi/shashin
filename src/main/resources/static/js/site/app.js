(function( shashin, $, undefined ) {
    shashin.fixContentHeight = function() {
        if ($("div[data-role='dialog']").is(":visible")) {
            const dialog = $("div[data-role='dialog']:visible:visible");
            const contentHeight = 400;
            dialog.height(contentHeight);
            shashin.map.updateSize();
        }
    };

    shashin.updateSearchInput = function(title) {
        $("#appSearchInput").val(title);
        $("#appSearchInput").on('blur', function() {
            if ($(this).val().length === 0) {
                $("#appSearchInput").val(title);
            }
        });
    };

    shashin.getMediaContent = function(metadata) {
        const mediaContent = {};

        mediaContent.metadataDetailFun = shashin.openEditMetadataModal;
        mediaContent.videoThumbnailFun = shashin.processVideoThumbnail;
        mediaContent.args = metadata.id;
        mediaContent.metadataId = metadata.id;

        if (metadata.type.includes("video")) {
            mediaContent.video = {
                "source": [{"src": metadata.videoUrl, "type": "video/mp4"}],
                "attributes": {
                    "preload": "auto",
                    "controls": true,
                    "autoplay": shashin.autoplayVideo
                }
            };
            mediaContent.lgSize = metadata.originalImageWidth+"-"+metadata.originalImageHeight;
            mediaContent.poster = ((null === metadata.thumbnailUrlOriginal || "" === metadata.thumbnailUrlOriginal) ? "/api/v1/thumbnails/225/"+metadata.id : "/api/v1/thumbnails/original/"+metadata.id) + "?v=" + Util.getMetadataLocalStorage();
            mediaContent.downloadUrl = encodeURI(metadata.videoUrl).replace(";", "%3B") + "/download";
        } else {
            mediaContent.src = "/api/v1/image/"+metadata.id+"?v="+uuidv4();
            mediaContent.downloadUrl = "/api/v1/image/"+metadata.id + "/download";
        }

        if (metadata.description !== null && metadata.description !== "") {
            mediaContent.subHtml = metadata.description;
        }

        return mediaContent;
    };

    shashin.updateFavorites = function(listenerPrefix, iconPrefix, countPrefix, metadataId) {
        $(listenerPrefix+metadataId).off("click").on("click", async function (e) {
            e.preventDefault();

            if ($(iconPrefix + metadataId).hasClass("bi-suit-heart")) {
                $(iconPrefix + metadataId).removeClass("bi-suit-heart").addClass("bi-suit-heart-fill");
            } else if ($(iconPrefix + metadataId).hasClass("bi-suit-heart-fill")) {
                $(iconPrefix + metadataId).removeClass("bi-suit-heart-fill").addClass("bi-suit-heart");
            }

            const isFavorite = ($(iconPrefix + metadataId).hasClass("bi-suit-heart-fill"));

            const http = new Http("favorite");
            const json = {metadataId: metadataId, isFavorite: isFavorite};

            let data;

            if (isFavorite === true) {
                data = await http.ajax("post", "/favorite/save", JSON.stringify(json));
            } else {
                data = await http.ajax("post", "/favorite/delete", JSON.stringify(json));
            }

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("count")) {
                Util.setMetadataLocalStorage();
                $(countPrefix + metadataId).text(data.count);
            }
        });
    };

    shashin.checkMetadata = function(metadataId) {
        let metadata = {};

        if ($("#infoModalEdit"+metadataId).attr("tag") && $("#infoModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#infoModalEdit"+metadataId).attr("tag"));
        }

        if  ($("#mediaLink"+metadataId).attr("tag") && $("#mediaLink"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#mediaLink"+metadataId).attr("tag"));
        }

        if  ($("#metadataModalEdit"+metadataId).attr("tag") && $("#metadataModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#metadataModalEdit"+metadataId).attr("tag"));
        }

        return metadata;
    };

    // Get metadata with albums,tagged people and keywords
    shashin.getCompleteMetadata = async function(metadataId) {
        const http = new Http("get timeline metadata");
        const data = await http.ajax("get", "/complete/metadata/"+metadataId+"?v="+uuidv4());

        shashin.printMessageToConsole("shashin.getCompleteMetadata",{tag:"metadata"});
        shashin.printMessageToConsole(JSON.stringify(data),{tag: "metadata"});

        let ret = {};

        if (data.hasOwnProperty("metadata")) {
            ret = data;
        }

        return ret;
    };

    // Get just the metadata with all keywords and albums
    shashin.getMetadata = async function(metadataId) {
        const http = new Http("get metadata");
        const data = await http.ajax("get", "/metadata/"+metadataId+"?v="+uuidv4());

        shashin.printMessageToConsole("shashin.getMetadata");
        shashin.printMessageToConsole(JSON.stringify(data),{tag: "metadata"});

        let metadata = {};
        metadata.keywords = [];
        metadata.albumMap = {};
        metadata.lastAccessedByDetails = "";
        metadata.uploadedByDetails = "";
        metadata.baseUrl = "";

        if (data.hasOwnProperty("metadata") && data.hasOwnProperty("keywordList") && data.hasOwnProperty("albumMap") && data.hasOwnProperty("lastAccessedByDetails") && data.hasOwnProperty("uploadedByDetails") && data.hasOwnProperty("baseUrl")) {
            metadata = data.metadata;
            metadata.keywords = data.keywordList;
            metadata.albumMap = data.albumMap;
            metadata.lastAccessedByDetails = data.lastAccessedByDetails;
            metadata.uploadedByDetails = data.uploadedByDetails;
            metadata.baseUrl = data.baseUrl;
        }

        return metadata;
    };

    shashin.pageLoader = function(func, appendClass, list, activePage) {
        let eol = false;

        const refreshIntervalId = window.setInterval(function () {
            if (!Util.hasScrollBar($("#container")) && !Util.hasScrollBar($("main"))) {
                setTimeout(async () => {
                    eol = await func();
                }, 1000);
            } else {
                clearInterval(refreshIntervalId);
            }

            if ((eol !== undefined && eol === true) || list === '' || list === '[]') {
                clearInterval(refreshIntervalId);
            }
        }, 200);

        function setupPlaceholders(activePage, speedInpxPerMs) {
            if (activePage !== undefined &&
                (activePage === "album" ||
                    activePage === "favorites" ||
                    activePage === "folder" ||
                    activePage === "recent" ||
                    activePage === "search" ||
                    activePage === "share" ||
                    activePage === "taken" ||
                    activePage === "accessed" ||
                    activePage === "trash" ||
                    activePage === "modified"))
            {
                // Show image from data
                if ((speedInpxPerMs < 0.20 && speedInpxPerMs > 0.15) || speedInpxPerMs === -1.0) {
                    const elementsInViewport = Util.elementsInViewport($(".photo-thumbnail-container"));
                    $.map(elementsInViewport, function (element) {
                        $(element).children('img').attr("src",$(element).children('img').attr("data-smallthumb"));
                        $(element).children('img').css("z-index", 0);
                    });
                }
            }
        }

        // xsmall/blurry images when scrolling
        createOnScrollListener($("#container"),eol);
        createOnScrollListener($("main"),eol);

        function createOnScrollListener(element, eol) {
            let lastOffset = $("#container").scrollTop();
            let lastDate = new Date().getTime();
            let scrollTimer = null;

            element.on('scroll', async function (e) {
                shashin.showScrollToTop(element);

                let st = $(e.target).scrollTop();
                let delayInMs = e.timeStamp - lastDate;
                let offset = st - lastOffset;
                let speedInpxPerMs = offset / delayInMs;

                if (scrollTimer !== null) {
                    clearTimeout(scrollTimer);
                }
                scrollTimer = setTimeout(function() {
                    $(window).trigger("scrollStop");
                }, 200);

                setupPlaceholders(activePage, speedInpxPerMs);

                if (Util.atEndOfPage(this) && eol === false) {
                    setTimeout(async function () {
                        eol = await func();
                    }, 200);
                }
            });

            $(window).bind("scrollStop", function() {
                setupPlaceholders(activePage, -1.0);
            });
        }

        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            scrollToTopButton.on("click",function () {
                $("main")[0].scrollTo({top: 0, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: 0, behavior: 'smooth'});
            });
        }
    };

    shashin.showScrollToTop = function(scrollEl) {
        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            if ((scrollEl[0].scrollTop > 20)) {
                scrollToTopButton.css("display","block");
            } else {
                scrollToTopButton.css("display","none");
            }
        }
    };

    shashin.showScrollToBottom = function(scrollEl) {
        const scrollToBottomButton = $("#btn-to-bottom");

        if (scrollToBottomButton.length > 0) {
            if ((scrollEl.innerHeight() + scrollEl.scrollTop()) >= scrollEl[0].scrollHeight) {
                scrollToBottomButton.css("display","none");
            } else {
                scrollToBottomButton.css("display","block");
            }
        }
    };

    shashin.activateScrollToTop = function() {
        const scrollToTopButton = $("#btn-back-to-top");

        if (scrollToTopButton.length > 0) {
            $("#container").on('scroll', function () {
                shashin.showScrollToTop($(this));
            });
            $("main").on('scroll', function () {
                shashin.showScrollToTop($(this));
            });

            scrollToTopButton.on("click",function () {
                $("main")[0].scrollTo({top: 0, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: 0, behavior: 'smooth'});
            });
        }
    };

    shashin.activateScrollToBottom = function() {
        const scrollToBottomButton = $("#btn-to-bottom");

        if (scrollToBottomButton.length > 0) {

            if ($("#container").hasScrollBar() || $("main").hasScrollBar()) {
                scrollToBottomButton.css("display","block");
            }

            $("#container").on('scroll', function () {
                shashin.showScrollToBottom($(this));
            });
            $("main").on('scroll', function () {
                shashin.showScrollToBottom($(this));
            });

            scrollToBottomButton.on("click",function () {
                $("main")[0].scrollTo({top: $("main")[0].scrollHeight, behavior: 'smooth'});
                $("#container")[0].scrollTo({top: $("#container")[0].scrollHeight, behavior: 'smooth'});
            });
        }
    };

    shashin.processEditedThumbnail = function(metadataId, lightGalleryIndex, rotation, isFlippedHorizontally, isFlippedVertically, brightness, contrast, saturation, restore, callback) {
        const http = new Http("update edited photo metadata");
        const version = Util.getMetadataLocalStorage();
        const json = {
            metadataId: metadataId,
            rotation: rotation,
            flipX: isFlippedHorizontally,
            flipY: isFlippedVertically,
            brightness: brightness,
            contrast: contrast,
            saturation: saturation
        };

        http.ajax("post", "/metadata/edit/thumbs" + ((restore === true) ? (version === "" ? "?restore=true" : "?v=" + version + "&restore=true") : (version === "" ? "" : "?v=" + version)), JSON.stringify(json)).then(function (data) {
            if (data.hasOwnProperty("msg") && data.hasOwnProperty("status") && data.hasOwnProperty("metadata")) {
                const metadata = data.metadata;
                const metadataId = metadata.id;

                const thumbnailSmallHeight = metadata.thumbnailSmallHeight;
                const thumbnailSmallWidth = metadata.thumbnailSmallWidth;

                // Refresh image
                Util.setMetadataLocalStorage();
                $("#photoThumbnailContainer" + metadataId).css({
                    "width": thumbnailSmallWidth + "px",
                    "height": thumbnailSmallHeight + "px"
                });
                $("#image" + metadataId).attr("src", $("#image" + metadataId).attr("src") + "?v=" + uuidv4());
                $("#image" + metadataId).attr("width", thumbnailSmallWidth);
                $("#image" + metadataId).attr("height", thumbnailSmallHeight);
                $("#editShashinImage").attr("src", "/api/v1/image/"+metadataId+"?v="+uuidv4());

                // Refresh lightgallery
                const mediaContentList = shashin.getLightGallery().galleryItems;

                if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null  && typeof shashin.getLightGallery().refresh === 'function' && mediaContentList.length > 0) {
                    lightGalleryIndex = parseInt(lightGalleryIndex);

                    const mediaContent = mediaContentList[lightGalleryIndex];

                    if (mediaContent.hasOwnProperty("downloadUrl") &&
                        mediaContent.downloadUrl.includes(metadataId)
                    ) {
                        mediaContentList[lightGalleryIndex].src = mediaContentList[lightGalleryIndex].src + "?v=" + uuidv4();
                        const mediaLinkId = "#mediaLink"+metadataId;
                        if ($(mediaLinkId).length > 0) {
                            $(mediaLinkId).attr("data-src", encodeURI($(mediaLinkId).attr("data-src")).replace(";", "%3B") + "?v=" + uuidv4());
                            if (parseInt($("img.lg-object.lg-image").attr("data-index")) === lightGalleryIndex) {
                                $("img.lg-object.lg-image").attr("src", ($("img.lg-object.lg-image").attr("src") + "?v=" + uuidv4()));
                            }
                        }
                    }

                    shashin.getLightGallery().refresh(mediaContentList);
                }

                $(".lg-current").animate({backgroundColor: "transparent"}, 2000);

                callback(true);
            } else {
                $(".lg-current").css("background-color", "transparent");

                callback(false);
            }
        });
    };

    shashin.processVideoThumbnail = function(metadataId, lightGalleryId, lightGalleryIndex) {
        const mediaContentList = shashin.getLightGallery().galleryItems;

        shashin.getMetadata(metadataId).then(function (data) {
            let metadata = data;

            $(".lg-current").css("background-color", "#FFFFFF");

            if (metadata.type.indexOf("video") !== -1) {
                let canvas = document.createElement('canvas');
                $(canvas).attr("id", "videoCanvas");

                let video = null;
                if ($("#lg-item-"+lightGalleryId+"-"+lightGalleryIndex).length > 0) {
                    video = $("#lg-item-" + lightGalleryId + "-" + lightGalleryIndex).find(".lg-video-object")[0];
                }

                let image = "";

                try {
                    if (video !== null && $(video).length > 0) {
                        canvas.width = metadata.originalImageWidth;
                        canvas.height = metadata.originalImageHeight;

                        let ctx = canvas.getContext('2d');
                        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
                        image = canvas.toDataURL('image/jpeg');
                    }
                } catch (e) {
                    shashin.printMessageToConsole("Error capturing thumbnail: " + e, {
                        consoleType: shashin.consoleTypes.error
                    });
                }

                $(canvas).remove();

                if (image.length > 0) {
                    const http = new Http("update video metadata");
                    const version = Util.getMetadataLocalStorage();
                    const json = {
                        metadataId: metadataId,
                        base64Data: image,
                        isImage: false
                    };
                    http.ajax("post", "/metadata/update/thumbs" + (version === "" ? "" : "?v=" + version), JSON.stringify(json)).then(function (data) {
                        if (data.hasOwnProperty("msg") && data.hasOwnProperty("status") && data.hasOwnProperty("posterUrl")) {
                            // Refresh image
                            Util.setMetadataLocalStorage();
                            const version = Util.getMetadataLocalStorage();
                            $("#image" + metadataId).attr("src", $("#image" + metadataId).attr("src") + (version === "" ? "" : "?v=" + version));
                            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.upload"), shashin.getTranslatedValue("main.toast.app.image.upload"), {
                                icon: "bi-info-circle",
                                iconColor: "#777777",
                                delay: 2000,
                                borderColor:"success"
                            });

                            if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null  && typeof shashin.getLightGallery().refresh === 'function' && mediaContentList.length > 0) {
                                const mediaContent = mediaContentList[lightGalleryIndex];

                                if (mediaContent.hasOwnProperty("video") &&
                                    // mediaContent.hasOwnProperty("poster") &&
                                    mediaContent.hasOwnProperty("downloadUrl") &&
                                    mediaContent.downloadUrl.includes(metadataId)
                                ) {
                                    mediaContentList[lightGalleryIndex].poster = data.posterUrl;
                                    const mediaLinkId = "#mediaLink"+metadataId;
                                    if ($(mediaLinkId).length > 0) {
                                        $(mediaLinkId).attr("data-poster", encodeURI(data.posterUrl).replace(";", "%3B")+"?v="+Util.getMetadataLocalStorage());
                                    }
                                }

                                shashin.getLightGallery().refresh(mediaContentList);
                            }

                            $(".lg-current").animate({backgroundColor: "transparent"}, 2000);
                        } else {
                            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.notupload"), shashin.getTranslatedValue("main.toast.app.image.notupload"), {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000",
                                borderColor:"danger"
                            });
                            $(".lg-current").css("background-color", "transparent");
                        }
                        $("#captureThumbnail").show();
                        $("#captureThumbnailSpinner").hide();
                        $("#captureThumbnail").prop( "disabled", false);
                        $("#captureThumbnailSpinner").prop( "disabled", false);
                    });
                } else {
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.notupload"), shashin.getTranslatedValue("main.toast.app.image.notupload"), {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor:"danger"
                    });
                    $("#captureThumbnail").show();
                    $("#captureThumbnailSpinner").hide();
                    $("#captureThumbnail").prop( "disabled", false);
                    $("#captureThumbnailSpinner").prop( "disabled", false);
                }
            } else {
                $(".lg-current").css("background-color", "transparent");
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.notupload"), shashin.getTranslatedValue("main.toast.app.image.notvideo"), {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });
                $("#captureThumbnail").show();
                $("#captureThumbnailSpinner").hide();
                $("#captureThumbnail").prop( "disabled", false);
                $("#captureThumbnailSpinner").prop( "disabled", false);
            }

            if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
                shashin.getLightGallery().refresh();
            }
        });
    };

    shashin.downloadSelected = async function (buttonId) {
        let span = null;
        if (typeof buttonId !== 'undefined') {
            span = $("#" + buttonId).find("span");
        }

        let activePage = "";
        if ($("#activePage").length > 0) {
            activePage = $("#activePage").val();
        }

        if (typeof buttonId === 'undefined' || (span !== null && span.hasClass('bi-download'))) {
            if ((span !== null && span.hasClass('bi-download'))) {
                span.addClass('spinner-grow').removeClass('bi-download');
            }

            let metadataIdList = shashin.getMetadataIdList();
            if (shashin.getMetadataIdList().length === 0) {
                $('.bi-circle-fill').each(function (i, obj) {
                    metadataIdList.push(obj.id.substring(6, obj.id.length));
                });
            }

            const endpoint = "/metadata/download/batch";

            shashin.closeToastMessages({tag:"downloadselected"});
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.media.downloading"), shashin.getTranslatedValue("main.toast.app.media.downloading"), {
                icon:"bi-info-circle",
                iconColor:"#777777",
                autohide:false,
                tag:"downloadselected"
            });

            if (Util.isMobile() === false) {
                shashin.downloadInstance = $.fileDownload(endpoint, {
                    httpMethod: "POST",
                    data: "batchMetadataIds=" + JSON.stringify(metadataIdList),
                    successCallback: function (url) {
                        shashin.printMessageToConsole("Media ZIP download success");
                        shashin.printMessageToConsole(url);

                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }

                        shashin.closeToastMessages({tag:"downloadselected"});
                    },
                    failCallback: function (html, url) {
                        shashin.printMessageToConsole("Media ZIP download fail", {
                            consoleType: shashin.consoleTypes.error
                        });
                        shashin.printMessageToConsole(url, {
                            consoleType: shashin.consoleTypes.error
                        });
                        shashin.printMessageToConsole(html, {
                            consoleType: shashin.consoleTypes.error
                        });

                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }

                        shashin.closeToastMessages({tag:"downloadselected"});
                    }
                });
            } else {
                shashin.downloadInstance = fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: "batchMetadataIds=" + JSON.stringify(metadataIdList)
                })
                    .then(response => response.blob())
                    .then(blob => {
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        const d = new Date();
                        a.download = "shashin_download_"+d.getFullYear()+("0" + (d.getMonth() + 1)).slice(-2)+("0" + d.getDate()).slice(-2)+"_"+("0" + d.getHours()).slice(-2)+d.getMinutes()+("0" + d.getSeconds()).slice(-2)+".zip";
                        document.body.appendChild(a); // we need to append the element to the dom -> otherwise it will not work in firefox
                        a.click();
                        a.remove();  //afterwards we remove the element again
                        shashin.printMessageToConsole("Media ZIP download success using fetch()");
                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }

                        shashin.closeToastMessages({tag:"downloadselected"});
                    }).catch(() => {
                        shashin.printMessageToConsole("Media ZIP download fail using fetch()", {
                            consoleType: shashin.consoleTypes.error
                        });
                        if (span !== null) {
                            span.addClass('bi-download').removeClass('spinner-grow');
                        }

                        shashin.closeToastMessages({tag:"downloadselected"});
                    });
            }
        }
    };

    shashin.createPagination = function(currentPage,totalPages,activePage,mediaTypeFilter,identifier=0,shareId="") {
        const lgConfig = {
            dynamic: true,
            plugins: []
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

        let lgElement = 'scroll-gallery';
        if (activePage === "album" || activePage === "share" || activePage === "favorites") {
            lgElement = 'infinite-scroll-gallery';
        }

        let initGallery = true;
        if ($("#"+lgElement).length === 0) {
            shashin.printMessageToConsole("lightGallery element '"+lgElement+"' DNE",{tag:"pagination"});
            initGallery = false;
        }

        if ($('.mediaLink').length === 0) {
            shashin.printMessageToConsole("media element '.mediaLink' DNE",{tag:"pagination"});
            initGallery = false;
        }

        if (initGallery === true) {
            shashin.initLightGallery(lgElement, lgConfig, '.mediaLink');
        }

        if (totalPages > 1 && currentPage <= totalPages) {
            const options = {
                currentPage: currentPage,
                totalPages: totalPages,
                truncate: true,
                href: function (index) { //index starts from 0
                    let link = '/' + activePage + '/' + index + '/' + mediaTypeFilter;
                    if (activePage === "folders") {
                        link = '/' + activePage + '/' + index;
                    } else if (activePage === "search") {
                        link = '/' + activePage + '/' + index + '/term/' + mediaTypeFilter;
                    } else if (activePage === "folder") {
                        link = '/' + activePage + '/' + encodeURIComponent(encodeURIComponent(identifier)).replace(";", "%3B") + '/' + index;
                    } else if (shareId !== "" && identifier > 0) {
                        link = '/' + activePage + '/' + shareId + '/album/' + identifier + '/' + index;
                    } else if (identifier > 0) {
                        link = '/' + activePage + '/' + identifier + '/' + index + '/' + mediaTypeFilter;
                    }
                    return link;
                }
            };

            if (Util.isMobile()) {
                options.innerWindow = 2;
            } else {
                options.innerWindow = 3;
                options.outerWindow = 1;
                options.first = null;
                options.last = null;
            }

            $('#pagination').pagy(options);
        }
    };

    shashin.trackShareDownload = function(albumId,albumName,shareLink) {
        let downloadTimer;
        const tokenName = "ShashinShareAlbumName";
        const tokenSize = "ShashinShareAlbumSize";
        const configuredAttempts = 120;

        shashin.closeToastMessages({tag:"sharedownload"});
        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.media.downloading"), shashin.getTranslatedValue("main.toast.app.media.downloading"), {
            icon:"bi-info-circle",
            iconColor:"#777777",
            autohide:false,
            tag:"sharedownload"
        });
        setTimeout(function () { $("#download"+albumId).removeAttr("href"); }, 0);
        Util.setCookie(tokenName, "", "/");
        Util.setCookie(tokenSize, "", "/");

        let attempts = configuredAttempts;

        downloadTimer = setInterval( function() {
            const tokenCookieValue = Util.getCookie(tokenName);
            const tokenCookieSize = Util.getCookie(tokenSize);

            if ((tokenCookieValue !== "" && tokenCookieSize !== "") || attempts === 0) {
                if (attempts === 0) {
                    // $("#albumsMessage").html("&nbsp;").animate({opacity: 0}, 5000);
                } else {
                    shashin.showToastMessage(shashin.getTranslatedValue("main.pages.albums.downloading.title"), "<strong>"+shashin.getTranslatedValue("main.pages.albums.downloading.filename")+"</strong> " + tokenCookieValue + " <strong>"+shashin.getTranslatedValue("main.pages.albums.downloading.filesize")+"</strong> " + Util.formatBytes(tokenCookieSize), {icon:"bi-info-circle", iconColor:"#777777"});
                    Util.deleteCookie(tokenName, "/");
                    Util.deleteCookie(tokenSize, "/");
                    window.clearInterval(downloadTimer);

                    shashin.clearSelection("album");
                    $("#clearMultiSelect").hide();
                    shashin.removeAllMetadataIdList();
                    $("#albumNumberSelected").hide();
                    shashin.closeToastMessages({tag:"sharedownload"});
                    const downloadEl = $("#download" + albumId);
                    downloadEl.attr("name", "download");
                    downloadEl.attr("value", albumId);
                    downloadEl.attr("title", "Download all photos");
                    downloadEl.on("click", function() {
                        shashin.trackShareDownload(albumId,albumName,shareLink);
                    });
                }
            }

            attempts--;
        }, 1000);
    };

    shashin.removeThumbnail = function(metadataId) {
        let dateGalleryRemoved = false;
        const targetElement = $("#photoThumbnailContainer" + metadataId);

        const rowId = targetElement.parent().attr("id");
        const sectionId = $(targetElement.siblings("section")[0]).attr("id");
        const headingId = typeof rowId !== "undefined" ? rowId.replace("row", "") : sectionId;

        // Count children
        const currentNumChildren = targetElement.siblings("div").length;

        // Remove metadata
        targetElement.remove();

        if (currentNumChildren === 0 && headingId && headingId.length > 0) {
            Util.removeDateGallery(headingId);
            dateGalleryRemoved = true;
        }

        return dateGalleryRemoved;
    };

    shashin.autocompleteSplit = function(val) {
        return val.split(/,\s*/);
    };

    shashin.autocompleteExtractLast = function(term) {
        return shashin.autocompleteSplit(term).pop();
    };

    shashin.processBatchAlbumList = function(data, albumInputVal) {
        if (albumInputVal === undefined) {
            albumInputVal = "";
        }

        if (data.hasOwnProperty("allAlbumList")) {
            // let renderAlbumList = false;
            const albumList = data.allAlbumList;
            const albumNames = [];
            const inputArr = albumInputVal.split(",");

            let batchHtml = "";

            for (let index in albumList) {
                const album = albumList[index];

                // if ($("#"+album.id).length === 0) {
                //     renderAlbumList = true;
                // }

                if (album.name.trim().length > 0) {
                    batchHtml +=
                        '<button class="dropdown-item" type="button">\n' +
                        '    <input type="checkbox" class="album" id="album' + album.id + '" value="' + Util.escapeHtml(album.name) + '" name="albums[]">\n' +
                        '    <label for="album' + album.id + '">' + Util.escapeHtml(album.name) + '</label>\n' +
                        '</button>\n';

                    albumNames.push(Util.escapeHtml(album.name));
                }
            }

            for (let index in inputArr) {
                const albumName = inputArr[index].trim();

                if (albumName.length > 0 && albumNames.includes(albumName) === false) {
                    batchHtml +=
                        '<button class="dropdown-item" type="button">\n' +
                        '    <input type="checkbox" class="album" id="'+albumName+'" value="'+Util.escapeHtml(albumName)+'" name="albums[]">\n' +
                        '    <label for="'+albumName+'">'+Util.escapeHtml(albumName)+'</label>\n' +
                        '</button>\n';

                    albumNames.push(albumName);
                }
            }

            if (albumNames.length > 0) {
                $("#albumBatchNameData").css("display", "block");

                shashin.createAutocomplete("#albumNameInput", albumNames, false);
                shashin.syncCheckboxInputs("#albumNameInput", "albums");

                $("#albumBatchSelectionList").html(batchHtml);
                $("#albumBatchNameData").off("click").on("click", function (e) {
                    e.preventDefault();
                    shashin.createBatchModalMultiselect("album");
                });
            } else {
                $("#albumBatchNameData").css("display", "none");
            }
        }
    };

    shashin.processBatchPeopleList = function(data, subjectInputVal) {
        if (subjectInputVal === undefined) {
            subjectInputVal = "";
        }

        if (data.hasOwnProperty("recognitionLabels")) {
            // let renderRecognitionLabels = false;
            const recognitionLabels = data.recognitionLabels;
            const recognitionLabelNames = [];
            const inputArr = subjectInputVal.split(",");

            let batchHtml = '';
            for (let index in recognitionLabels) {
                const recognitionLabel = recognitionLabels[index];

                // if ($("#"+recognitionLabel.id).length === 0) {
                //     renderRecognitionLabels = true;
                // }

                if (recognitionLabel.name !== null && recognitionLabel.name !== "null" && recognitionLabel.name.trim().length > 0 && recognitionLabel.id > 0) {
                    batchHtml +=
                        '           <button class="dropdown-item" type="button">\n' +
                        '               <input type="checkbox" class="recognitionLabel" id="recognitionLabel' + recognitionLabel.id + '" value="' + Util.escapeHtml(recognitionLabel.name) + '" name="recognitionLabel[]">\n' +
                        '               <label for="recognitionLabel' + recognitionLabel.id + '">' + Util.escapeHtml(recognitionLabel.name) + '</label>\n' +
                        '           </button>';

                    recognitionLabelNames.push(recognitionLabel.name);
                }
            }

            for (let index in inputArr) {
                const recognitionName = inputArr[index].trim();

                if (recognitionName.length > 0 && recognitionLabelNames.includes(recognitionName) === false) {
                    batchHtml +=
                        '<button class="dropdown-item" type="button">\n' +
                        '    <input type="checkbox" class="recognitionLabel" id="'+recognitionName+'" value="'+Util.escapeHtml(recognitionName)+'" name="recognitionLabel[]">\n' +
                        '    <label for="'+recognitionName+'">'+Util.escapeHtml(recognitionName)+'</label>\n' +
                        '</button>\n';

                    recognitionLabelNames.push(recognitionName);
                }
            }

            if (recognitionLabelNames.length > 0) {
                $("#peopleBatchNameData").css("display", "block");

                shashin.createAutocomplete("#tagBatchDataInput", recognitionLabelNames, false);
                shashin.syncCheckboxInputs("#tagBatchDataInput", "recognitionLabel");

                $("#peopleBatchSelectionList").html(batchHtml);
                $("#peopleBatchNameData").off("click").on("click", function (e) {
                    e.preventDefault();
                    shashin.createBatchModalMultiselect("people");
                });
            } else {
                $("#peopleBatchNameData").css("display", "none");
            }
        }
    };
}( window.shashin = window.shashin || {}, jQuery ));

if (typeof module !== 'undefined') {
    module.exports = window.shashin;
}