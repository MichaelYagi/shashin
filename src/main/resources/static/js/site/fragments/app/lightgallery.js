(function( shashin, $, undefined ) {
    shashin.videoPlaying = false;

    document.addEventListener('play', function(e) {
        if (e.target.tagName !== 'VIDEO') return;
        shashin.videoPlaying = true;
        shashin.closeToastMessages({tags:["subhtml", "lgSubhtml", "shashinSubhtml", "viewerSubhtml", "playerSubhtml"]});
    }, true);

    document.addEventListener('pause', function(e) {
        if (e.target.tagName !== 'VIDEO') return;
        shashin.videoPlaying = false;
    }, true);

    document.addEventListener('ended', function(e) {
        if (e.target.tagName !== 'VIDEO') return;
        shashin.videoPlaying = false;
    }, true);

    document.addEventListener('mousemove', function() {
        const video = document.querySelector('.shoji-dialog video');
        if (!video || video.paused || video.ended) return;
        requestAnimationFrame(function() {
            const caption = document.querySelector('.shoji-dialog .shoji-caption');
            if (caption) {
                caption.style.setProperty('opacity', '0', 'important');
                caption.style.setProperty('visibility', 'hidden', 'important');
            }
            shashin.closeToastMessages({tags:["subhtml", "lgSubhtml", "shashinSubhtml", "viewerSubhtml", "playerSubhtml"]});
        });
    });

    shashin.initLightGallery = function(lgElement, additionalLgConfigs, mediaElement) {
        shashin.setLightGalleryElement(lgElement);
        shashin.setLightGallery(additionalLgConfigs);

        let mediaContentList = shashin.getInitMediaContent(mediaElement);
        shashin.initMediaContent(mediaContentList);

        return mediaContentList;
    };

    shashin.getInitMediaContent = function(mediaElement) {
        let mediaContentList = [];
        $.each($(mediaElement), function() {
            const mediaContent = {};
            mediaContent.metadataDetailFun = shashin.openEditMetadataModal;
            mediaContent.videoThumbnailFun = shashin.processVideoThumbnail;
            mediaContent.args = "";
            try {
                mediaContent.args = $(this).attr("tag");
            } catch(e) {}

            const metadataId = $(this).attr("data-metadata-id");
            mediaContent.metadataId = metadataId;
            mediaContent.id = metadataId;

            const subHtmlAttr = $(this).attr("data-sub-html");
            if (typeof subHtmlAttr !== 'undefined' && subHtmlAttr !== false) {
                mediaContent.caption = { dangerouslySetInnerHTML: subHtmlAttr };
            }

            if ($(this).attr("data-src")) {
                mediaContent.src = $(this).attr("data-src");
                mediaContent.downloadUrl = $(this).attr("data-download-url");
            } else if ($(this).attr("data-video")) {
                try {
                    const videoConfig = JSON.parse($(this).attr("data-video"));
                    if (videoConfig.source && videoConfig.source.length > 0) {
                        mediaContent.src = videoConfig.source[0].src;
                    }
                } catch(e) {}
                mediaContent.video = { provider: 'html5' };
                mediaContent.poster = $(this).attr("data-poster");
                const lgSize = $(this).attr("data-lg-size");
                if (lgSize) {
                    const parts = lgSize.split('-');
                    if (parts.length === 2) {
                        mediaContent.width = parseInt(parts[0], 10);
                        mediaContent.height = parseInt(parts[1], 10);
                    }
                }
                mediaContent.downloadUrl = $(this).attr("data-download-url");
            }

            mediaContentList.push(mediaContent);
        });

        return mediaContentList;
    };

    shashin.initMediaContent = function(mediaContentList) {
        if (mediaContentList.length > 0 && shashin.getLightGallery() !== null) {
            shashin.refreshAndActivateLgListener(mediaContentList);
        }
    };

    shashin.updateMediaContent = function(mediaContentList, additionalMediaContentList, activePage = "") {
        if (activePage !== 'timeline' && activePage !== 'person' && activePage !== 'matches') {
            Util.truncateHeading();
        }

        if (additionalMediaContentList && additionalMediaContentList.length > 0) {
            mediaContentList = mediaContentList.concat(additionalMediaContentList);
            shashin.refreshAndActivateLgListener(mediaContentList);
        }

        return mediaContentList;
    };

    shashin.refreshAndActivateLgListener = function(mediaContentList) {
        if (shashin.getLightGallery() !== null && typeof shashin.getLightGallery().refresh === 'function') {
            shashin.getLightGallery().refresh(mediaContentList);
        }
    };

    shashin.setLightGalleryElement = function(name) {
        shashin.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            shashin.infiniteScrollGallery = document.getElementById(name);
        }

        // Escape key in info sidebar closes only the sidebar, not the lightbox
        $("#propInfoSidebar").on('keydown', function(e) {
            if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
                e.stopPropagation();
                const bsOffcanvasEl = document.getElementById('propInfoSidebar');
                const bsOffcanvas = bootstrap.Offcanvas.getInstance(bsOffcanvasEl);
                bsOffcanvas.hide();
                return false;
            }
        });
    };

    shashin.setLightGallery = function(additionalConfigs) {
        // Remove any previous selector-mode delegated click handler before replacing the gallery
        if (shashin._selectorClickHandler && shashin.getLightGalleryElement()) {
            shashin.getLightGalleryElement().removeEventListener('click', shashin._selectorClickHandler);
            shashin._selectorClickHandler = null;
        }

        if (!shashin.getLightGalleryElement()) {
            shashin.lg = null;
            return;
        }

        let configs = shashin.getLightGalleryConfigs(additionalConfigs);

        // Extract selector before Shoji sees it — we emulate LG's selector mode ourselves
        // so that timeline's `setLightGallery({ selector: ".mediaLink" })` flow keeps working.
        const gallerySelector = configs.selector || null;
        delete configs.selector;

        // Force dynamic mode — items are loaded via updateSlides after construction
        if (!configs.items) {
            configs.items = [];
        }
        shashin.lg = new Shoji(shashin.getLightGalleryElement(), configs);

        // Backward-compat aliases so existing callers of shashin.getLightGallery() keep working
        Object.defineProperty(shashin.lg, 'galleryItems', {
            get: function() { return shashin.lg.items; },
            configurable: true
        });
        Object.defineProperty(shashin.lg, 'index', {
            get: function() { return shashin.lg.currentIndex; },
            configurable: true
        });
        shashin.lg.openGallery = function(index) { shashin.lg.open(index); };
        shashin.lg.closeGallery = function() { shashin.lg.close(); return 0; };
        // refresh with items → updateSlides; no args in selector mode → rescan DOM
        shashin.lg.refresh = function(items) {
            if (items !== undefined) {
                shashin.lg.updateSlides(items);
            } else if (gallerySelector && shashin.getLightGalleryElement()) {
                const allEls = shashin.getLightGalleryElement().querySelectorAll(gallerySelector);
                shashin.lg.updateSlides(shashin.getInitMediaContent(allEls));
            }
        };
        shashin.lg.destroyModules = function() {};
        shashin.lg.invalidateItems = function() {};
        shashin.lg.lgId = '';
        shashin.lg.LGel = { off: function() {} };

        // Close gallery on browser/mobile back button
        shashin.lg.on('afterOpen', function(detail) {
            const initialIndex = detail ? detail.index : 0;
            const initialItem = shashin.lg.items[initialIndex];
            if (initialItem && initialItem.metadataId) {
                $("#metadataId").val(initialItem.metadataId);
                $("#lgIndex").val(initialIndex);
            }

            // Hide any page-level custom scrollbar (e.g. timeline date slider) while lightbox is open
            $("#dateSliderWrapper").hide();

            // Shoji doesn't fire beforeSlide on open(), so trigger the metadata/button update for the first slide here
            if (initialItem && initialItem.metadataId) {
                shashin.getMetadata(initialItem.metadataId).then(function(metadata) {
                    if (Util.sessionStorageAvailable() === true) {
                        sessionStorage.setItem("metadata", JSON.stringify(metadata));
                    } else if (Util.localStorageAvailable() === true) {
                        localStorage.setItem("metadata", JSON.stringify(metadata));
                    } else {
                        $("#metadata").val(JSON.stringify(metadata));
                    }
                    if (metadata.type.indexOf("image") >= 0 && metadata.type.indexOf("gif") < 0) {
                        $("#shashineditor").css("display", "block");
                    } else {
                        $("#shashineditor").css("display", "none");
                    }
                    if (metadata.type.indexOf("video") >= 0) {
                        $("#captureThumbnail").css("display", "block");
                    } else {
                        $("#captureThumbnail").css("display", "none");
                    }
                });
            }

            if (window.history && window.history.pushState) {
                window.history.pushState('forward', null, "");
                $(window).on('popstate', function() {
                    if (shashin.lg !== null) {
                        shashin.lg.close();
                    }
                });
            }
        });

        shashin.lg.on('afterClose', function() {
            if (Util.sessionStorageAvailable() === true) {
                sessionStorage.removeItem("metadata");
            } else if (Util.localStorageAvailable() === true) {
                localStorage.removeItem("metadata");
            } else {
                $("#metadata").val("");
            }
            shashin.videoPlaying = false;
            shashin.closeToastMessages({tags:["subhtml", "lgSubhtml", "shashinSubhtml"]});
            // Restore page-level custom scrollbar
            $("#dateSliderWrapper").show();
        });

        // Hide info sidebar when navigating; cache-bust image src; prefetch metadata
        shashin.lg.on('beforeSlide', function(detail) {
            if (!detail) return;
            const toIndex = detail.to;

            const bsOffcanvasEl = document.getElementById('propInfoSidebar');
            const bsOffcanvas = bootstrap.Offcanvas.getInstance(bsOffcanvasEl);
            if (bsOffcanvas !== null) {
                bsOffcanvas.hide();
            }

            if (shashin.lg === null) return;
            const galleryItems = shashin.lg.items;
            const galleryItem = galleryItems[toIndex];
            if (!galleryItem || galleryItem.metadataId === undefined || galleryItem.metadataId === null) return;

            const metadataId = galleryItem.metadataId;
            $("#metadataId").val(metadataId);
            $("#lgIndex").val(toIndex);

            if ($("#image"+metadataId).length > 0) {
                const imgSrc = $("#image"+metadataId).attr("src") || "";
                if (imgSrc.indexOf("?v=") < 0) {
                    $("#image"+metadataId).attr("src", imgSrc + "?v=" + uuidv4());
                }
            }

            // Cache-bust image src on slide navigation (skip for video items)
            if (galleryItem.src && !galleryItem.video) {
                const src = Util.deleteAfterSubstring(galleryItems[toIndex].src, "?v=");
                galleryItems[toIndex].src = src + "?v=" + uuidv4();
            }

            shashin.getMetadata(metadataId).then(function(metadata) {
                if (Util.sessionStorageAvailable() === true) {
                    sessionStorage.setItem("metadata", JSON.stringify(metadata));
                } else if (Util.localStorageAvailable() === true) {
                    localStorage.setItem("metadata", JSON.stringify(metadata));
                } else {
                    $("#metadata").val(JSON.stringify(metadata));
                }

                if (metadata.type.indexOf("image") >= 0 && metadata.type.indexOf("gif") < 0) {
                    $("#shashineditor").css("display", "block");
                } else {
                    $("#shashineditor").css("display", "none");
                }

                if (metadata.type.indexOf("video") >= 0) {
                    $("#captureThumbnail").css("display", "block");
                } else {
                    $("#captureThumbnail").css("display", "none");
                }
            });
        });

        // Emulate LG's selector mode via a delegated click handler on the container.
        // This fires for pages (like timeline) that call setLightGallery({ selector: ".mediaLink" })
        // rather than initLightGallery, and where Shoji's own selector-mode scan isn't viable
        // (items are dynamic DOM descendants, not static direct children).
        if (gallerySelector && shashin.getLightGalleryElement()) {
            const containerEl = shashin.getLightGalleryElement();
            shashin._selectorClickHandler = function(e) {
                const el = e.target.closest(gallerySelector);
                if (!el || !containerEl.contains(el)) return;
                e.preventDefault();
                const allEls = containerEl.querySelectorAll(gallerySelector);
                const index = Array.from(allEls).indexOf(el);
                if (index < 0) return;
                const items = shashin.getInitMediaContent(allEls);
                shashin.lg.updateSlides(items);
                shashin.lg.open(index);
            };
            containerEl.addEventListener('click', shashin._selectorClickHandler);
        }
    };

    shashin.getLightGalleryElement = function() {
        return shashin.infiniteScrollGallery;
    };

    shashin.getLightGallery = function() {
        return shashin.lg;
    };

    shashin.openGallery = function(e, index) {
        e.preventDefault();
        if (shashin.getLightGallery() !== null) {
            shashin.getLightGallery().open(index);
        }
    };

    shashin.getLightGalleryConfigs = function(additionalConfigs) {
        shashin.autoplayVideo = $("#autoplayVideo").val() === "true";

        let configs = {};
        if (additionalConfigs !== undefined && additionalConfigs !== null &&
            additionalConfigs.hasOwnProperty("overrideBaseConfigs") &&
            additionalConfigs.overrideBaseConfigs === true) {
            configs = Object.assign({}, additionalConfigs);
            // Filter out LG-format plugins (constructors) — only pass { name, init } Shoji plugins
            if (Array.isArray(configs.plugins)) {
                configs.plugins = configs.plugins.filter(function(p) {
                    return p && typeof p === 'object' && typeof p.init === 'function' && typeof p.name === 'string';
                });
            }
        } else {
            const downloadPlugin = (typeof lgDownload !== 'undefined') ? [lgDownload] : [];
            const castPlugin = (typeof lgCastMedia !== 'undefined') ? [lgCastMedia] : [];
            const editorPlugin = (typeof lgShashinEditor !== 'undefined') ? [lgShashinEditor] : [];
            const rotateFlipPlugin = (typeof lgShashinEditor === 'undefined') ? [Shoji.RotateFlip] : [];
            configs = {
                plugins: [Shoji.Zoom, Shoji.Fullscreen, ...rotateFlipPlugin, ...downloadPlugin, ...castPlugin, ...editorPlugin],
                counter: false,
                preload: 1,
                autoHideDelay: 5000,
                mode: 'fade',
                download: (typeof lgDownload !== 'undefined'),
                castMedia: (typeof lgCastMedia !== 'undefined'),
                shashinEditor: (typeof lgShashinEditor !== 'undefined')
            };

            for (const key in additionalConfigs) {
                if (key === "plugins") {
                    const pluginList = Array.isArray(additionalConfigs[key]) ? additionalConfigs[key] : [additionalConfigs[key]];
                    pluginList.forEach(function(p) {
                        // Only Shoji plugins: plain objects with name (string) + init (function)
                        if (p && typeof p === 'object' && typeof p.init === 'function' && typeof p.name === 'string') {
                            configs.plugins.push(p);
                        }
                    });
                } else if (key !== 'overrideBaseConfigs') {
                    configs[key] = additionalConfigs[key];
                }
            }
        }

        return configs;
    };

    shashin.jumpToLightGalleryIndex = function(index) {
        const url = location.href;
        location.href = '#lightGalleryIndex' + index;
        history.replaceState(null, null, url);
    };
}( window.shashin = window.shashin || {}, jQuery ));
