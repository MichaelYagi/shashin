(function( shashin, $, undefined ) {
    shashin.initLightGallery = function(lgElement,additionalLgConfigs,mediaElement) {
        shashin.setLightGalleryElement(lgElement);
        shashin.setLightGallery(additionalLgConfigs);

        let mediaContentList = [];
        $.each($(mediaElement), function() {
            const mediaContent = {};
            mediaContent.metadataDetailFun = shashin.openEditMetadataModal;
            mediaContent.videoThumbnailFun = shashin.processVideoThumbnail;
            mediaContent.args = "";
            try {
                mediaContent.args = $(this).attr("tag");
            } catch(e) {}
            let subHtmlAttr = $(this).attr("data-sub-html");
            if (typeof subHtmlAttr !== 'undefined' && subHtmlAttr !== false) {
                mediaContent.subHtml = subHtmlAttr;
            }
            if ($(this).attr("data-src")) {
                mediaContent.src = $(this).attr("data-src");
                mediaContent.downloadUrl = $(this).attr("data-download-url");
            } else if ($(this).attr("data-video")) {
                mediaContent.video = $(this).attr("data-video");
                mediaContent.poster = $(this).attr("data-poster");
                mediaContent.lgSize = $(this).attr("data-lg-size");
                mediaContent.downloadUrl = $(this).attr("data-download-url");
            }
            mediaContent.metadataId = $(this).attr("data-metadata-id");
            mediaContentList.push(mediaContent);
        });

        shashin.initMediaContent(mediaContentList);

        return mediaContentList;
    };

    shashin.initMediaContent = function(mediaContentList) {
        if (mediaContentList.length > 0 && shashin.getLightGallery() !== null) {
            shashin.refreshAndActivateLgListener(mediaContentList);
        }
    };

    shashin.updateMediaContent = function(mediaContentList,additionalMediaContentList,activePage = "") {
        // Remove place name if date headings too long
        if (activePage !== 'timeline' && activePage !== 'person' && activePage !== 'matches') {
            Util.truncateHeading();
        }

        if (additionalMediaContentList && additionalMediaContentList.length > 0) {
            mediaContentList = mediaContentList.concat(additionalMediaContentList);
            shashin.refreshAndActivateLgListener(mediaContentList);
        }

        return mediaContentList;
    };

    shashin.refreshAndActivateLgListener = function (mediaContentList) {
        if (shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
            shashin.getLightGallery().refresh(mediaContentList);
            // shashin.getLightGalleryElement().addEventListener('lgAfterSlide', function (e) {
            //     shashin.jumpToLightGalleryIndex(e.detail.index);
            // })
        }
    };

    shashin.setLightGalleryElement = function (name) {
        shashin.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            shashin.infiniteScrollGallery = document.getElementById(name);

            // Event listeners for light gallery

            // Close gallery on browser/mobile back button
            shashin.infiniteScrollGallery.addEventListener('lgAfterOpen', function () {
                if (window.history && window.history.pushState) {
                    window.history.pushState('forward', null, "");

                    $(window).on('popstate', function() {
                        if (shashin.lg !== null) {
                            shashin.lg.closeGallery();
                        }
                    });

                }
            });

            shashin.infiniteScrollGallery.addEventListener('lgAfterClose', _ => {
                shashin.closeToastMessages({tags: ["subhtml"]});
            });

            // Hide sidebar when going to next slide
            shashin.infiniteScrollGallery.addEventListener('lgBeforeSlide', e => {
                const bsOffcanvasEl = document.getElementById('propInfoSidebar');
                const bsOffcanvas = bootstrap.Offcanvas.getInstance(bsOffcanvasEl);
                if (bsOffcanvas !== null) {
                    bsOffcanvas.hide();
                }

                if (shashin.lg !== null && shashin.lg.hasOwnProperty("galleryItems")) {
                    const galleryItems = shashin.lg.galleryItems;
                    const currentIndex = e.detail.index;
                    const galleryItem = galleryItems[currentIndex];

                    if (galleryItem.hasOwnProperty("subHtml") && galleryItem.subHtml !== "") {
                        let subhtml = galleryItem.subHtml;
                        shashin.showToastMessage(null, subhtml, {
                            tag: "subhtml",
                            autohide: false,
                            closeButton: false
                        });
                    }

                    if (galleryItem.hasOwnProperty("src")) {
                        shashin.lg.galleryItems[currentIndex].src = shashin.lg.galleryItems[currentIndex].src+"?v="+uuidv4();

                        if (galleryItem.hasOwnProperty("metadataId")) {
                            const metadataId = galleryItem.metadataId;
                            $("#metadataId").val(metadataId);
                            $("#lgIndex").val(currentIndex);

                            if ($("#image"+metadataId).length > 0 && ($("#image"+metadataId).attr("src")).indexOf("?v=") < 0) {
                                $("#image"+metadataId).attr("src", $("#image"+metadataId).attr("src")+"?v="+uuidv4());
                            }

                            const mediaLinkId = "#mediaLink" + metadataId;
                            if ($(mediaLinkId).length > 0) {
                                $(mediaLinkId).attr("data-src", encodeURI($(mediaLinkId).attr("data-src")).replace(";", "%3B") + "?v=" + Util.getMetadataLocalStorage());
                                if (parseInt($("img.lg-object.lg-image").attr("data-index")) === parseInt(currentIndex)) {
                                    $("img.lg-object.lg-image").attr("src", ($("img.lg-object.lg-image").attr("src") + "?v=" + Util.getMetadataLocalStorage()));
                                }
                            }
                        }
                    }
                }
            });

            // If info sidebar open, pressing escape key closes only the sidebar
            $("#propInfoSidebar").on('keydown', function(e) {
                // escape
                if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
                    e.stopPropagation();
                    const bsOffcanvasEl = document.getElementById('propInfoSidebar');
                    const bsOffcanvas = bootstrap.Offcanvas.getInstance(bsOffcanvasEl);
                    bsOffcanvas.hide();
                    return false;
                }
            });
        }
    };

    // Close gallery on browser/mobile back button
    shashin.setLightGallery = function (additionalConfigs) {
        let configs = shashin.getLightGalleryConfigs(additionalConfigs);
        shashin.lg = lightGallery(shashin.getLightGalleryElement(), configs);
    };

    shashin.getLightGalleryElement = function () {
        return shashin.infiniteScrollGallery;
    };

    shashin.getLightGallery = function () {
        return shashin.lg;
    };

    shashin.openGallery = function (e, index) {
        e.preventDefault();
        if (shashin.getLightGallery() !== null) {
            shashin.getLightGallery().openGallery(index);
        }
    };

    shashin.getLightGalleryConfigs = function(additionalConfigs) {
        // shashin.autoplayVideo = $("#autoplayVideoSwitch").is(':checked');

        const configs = {
            plugins: [lgZoom, lgVideo, lgRelativeCaption, lgFullscreen, /*lgRotate,*/ lgCastMedia, lgShashinEditor],
            videojs: false,
            hideBarsDelay: 5000,
            showBarsAfter: 5000,
            allowMediaOverlap: true,
            counter: false,
            castMedia: true,
            shashinEditor: true,
            fullScreen: true,
            download: true,
            zoomFromOrigin: true,
            // videoMaxSize: "7680-4320",
            speed: 0,
            preload: 0,
            autoplayFirstVideo: true,
            autoplayVideoOnSlide: true,
            gotoNextSlideOnVideoEnd: false,
            rotate: true,
            rotateLeft: true,
            rotateRight: true,
            flipHorizontal: true,
            flipVertical: false,
            licenseKey: Util.lgApiKey()
        };

        if (shashin.autoplayVideo === false) {
            configs.autoplayFirstVideo = false;
            configs.autoplayVideoOnSlide = false;
        }

        for (const key in additionalConfigs) {
            if (key === "plugins") {
                if ($.isArray(additionalConfigs[key])) {
                    $.each(additionalConfigs[key] , function(index, val) {
                        configs.plugins.push(val);
                    });
                } else {
                    configs.plugins.push(additionalConfigs[key]);
                }
            } else {
                configs[key] = additionalConfigs[key];
            }
        }

        return configs;
    };

    shashin.jumpToLightGalleryIndex = function (index) {
        const url = location.href;
        location.href = '#lightGalleryIndex'+index;
        history.replaceState(null,null,url);
    };
}( window.shashin = window.shashin || {}, jQuery ));