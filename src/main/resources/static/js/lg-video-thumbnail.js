/**
 * MY - Added Oct 1 2023
 * - Created this module to capture video thumbnails
 */

! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgVideoThumbnail = l()
}(this, (function() {
    "use strict";
    var e = function() {
            return (e = Object.assign || function(e) {
                for (var l, n = 1, t = arguments.length; n < t; n++)
                    for (var c in l = arguments[n])
                        Object.prototype.hasOwnProperty.call(l, c) && (e[c] = l[c]);
                return e
            }).apply(this, arguments)
        },
        l = {
            videoThumbnail: !0
        };
    return function() {
        function n(n, t) {
            return this.core = n,
                this.$LG = t,
                this.settings = e(e({}, l), this.core.settings),
                this
        }
        return n.prototype.init = function() {
            var e = "";
            var captureIcon = "bi-lightning";

            if (this.settings.videoThumbnail) {
                e = '<button type="button" aria-label="Capture Thumbnail" title="Capture Thumbnail" id="captureThumbnail" class="'+captureIcon+' lg-icon" style="font-size: 1rem;display: none"></button>' +
                    '<div class="spinner-border spinner-border-sm float-end mt-3 me-3" role="status" id="captureThumbnailSpinner"></div>',
                    this.core.$toolbar.append(e),
                    this.videoThumbnail()
            }

            this.core.LGel.on('lgHasVideo', (event) => {
                // Video thumbnail icon
                $("."+captureIcon+".lg-icon").css("display", "none");

                if (event.detail.hasPoster === false ||
                    ((this.settings.hasOwnProperty("autoplayFirstVideo") && this.settings.autoplayFirstVideo === true) &&
                    (this.settings.hasOwnProperty("autoplayVideoOnSlide") && this.settings.autoplayVideoOnSlide === true))
                ) {
                    $("."+captureIcon+".lg-icon").css("display", "block");
                } else if (event.detail.hasPoster === true) {
                    this.core.LGel.on('lgPosterClick', () => {
                        $("."+captureIcon+".lg-icon").css("display", "block");
                    });
                }
            });

            this.core.LGel.on('lgBeforeOpen', (e) => {
                $("."+captureIcon+".lg-icon").css("display", "none");
            });

            this.core.LGel.on('lgAfterClose', () => {
                $("."+captureIcon+".lg-icon").css("display", "none");
            });

            this.core.LGel.on('lgBeforeSlide', (e) => {
                $("."+captureIcon+".lg-icon").css("display", "none");

                // Show screenshot button when changing slides
                if ($(".lg-outer").length > 0) {
                    const lgOuterId = $($(".lg-outer")[0]).attr("id");
                    if (lgOuterId !== undefined) {
                        const lgOuterIdArray = lgOuterId.split("-");
                        if (lgOuterIdArray.length > 0) {
                            const lgOuterIdValue = lgOuterIdArray[lgOuterIdArray.length - 1];
                            const lgItemId = "lg-item-" + lgOuterIdValue + "-" + e.detail.index;
                            const lgItemEl = $("#" + lgItemId);

                            if (lgItemEl.length > 0 && lgItemEl.children().find('video')[0] !== undefined) {
                                const video = lgItemEl.children().find('video')[0];
                                if (video.paused || video.ended || !!(video.currentTime > 0 && !video.paused && !video.ended && video.readyState > 2)) {
                                    $("."+captureIcon+".lg-icon").css("display", "block");
                                }
                            }
                        }
                    }
                }
            });

            this.core.outer
                .find('.'+captureIcon)
                .first()
                .on('click.lg', () => {
                    let currentDynamicEl = this.settings.dynamicEl[this.core.index] && this.settings.dynamicEl[this.core.index].hasOwnProperty("func") ? this.settings.dynamicEl[this.core.index] : this.core.galleryItems[this.core.index];

                    $("#captureThumbnail").hide();
                    $("#captureThumbnailSpinner").show();

                    if (currentDynamicEl.hasOwnProperty("func")) {
                        $("#metadataId").val(currentDynamicEl.args);
                        currentDynamicEl.vtfunc(currentDynamicEl.args);
                    } else if ($($(".thumbnail-bl")[this.core.index]).children('a').attr("tag")) {
                        //console.log($($(".thumbnail-bl")[this.core.index]).children('a').attr("tag"))
                        const fn = this.settings.videoThumbnailFunc;
                        let id = "";
                        try {
                            id = $($(".thumbnail-bl")[this.core.index]).children('a').attr("tag");
                        } catch (e) {
                            if (shashin) {
                                shashin.showToastMessage("Could not capture screenshot", "Could not capture screenshot. " + e.message, {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                            }
                            $("#captureThumbnail").show();
                            $("#captureThumbnailSpinner").hide();
                        }

                        if (typeof fn === 'function' && id.length > 0) {
                            fn(id);
                        } else {
                            if (shashin) {
                                shashin.showToastMessage("Could not capture screenshot", "Could not capture screenshot. Tag or function not found", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                            }
                            $("#captureThumbnail").show();
                            $("#captureThumbnailSpinner").hide();
                        }
                    } else if ($($(".thumbnail-centered")[this.core.index]).children('a').attr("tag")) {
                        //console.log($($(".thumbnail-centered")[this.core.index]).children('a').attr("tag"))
                        const fn = this.settings.videoThumbnailFunc;
                        let id = "";
                        try {
                            id = $($(".thumbnail-centered")[this.core.index]).children('a').attr("tag");
                        } catch (e) {
                            if (shashin) {
                                shashin.showToastMessage("Could not capture screenshot", "Could not capture screenshot. " + e.message, {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                            }
                            $("#captureThumbnail").show();
                            $("#captureThumbnailSpinner").hide();
                        }

                        if (typeof fn === 'function' && id.length > 0) {
                            fn(id);
                        } else {
                            if (shashin) {
                                shashin.showToastMessage("Could not capture screenshot", "Could not capture screenshot. Tag or function not found", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                            }
                            $("#captureThumbnail").show();
                            $("#captureThumbnailSpinner").hide();
                        }
                    } else {
                        if (shashin) {
                            shashin.showToastMessage("Could not capture screenshot", "Could not capture screenshot. Tag or function not found", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                        }
                        $("#captureThumbnail").show();
                        $("#captureThumbnailSpinner").hide();
                    }
                });
        },
            n.prototype.videoThumbnail = function(e) {
                $("#captureThumbnail").show();
                $("#captureThumbnailSpinner").hide();

                // Edit video thumbnail
                return ""
            },
            n.prototype.destroy = function() {},
            n
    }()
}));