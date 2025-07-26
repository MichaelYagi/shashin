/**
 * MY - Added Oct 1 2023
 * - Created this module to capture video thumbnails
 */

! function(_window, pluginFunction) {
    let pluginName = "lgVideoThumbnail";

    if ("object" == typeof exports && "undefined" != typeof module) {
        module.exports = l();
    } else if ("function" == typeof define && define.amd) {
        define(pluginFunction);
    } else {
        (_window = "undefined" != typeof globalThis ? globalThis : _window || self)[pluginName] = pluginFunction();
    }
} (this, (function() {
    "use strict";
    let __assign = function () {
        __assign = Object.assign || function __assign(t) {
            for (var s, i = 1, n = arguments.length; i < n; i++) {
                s = arguments[i];
                for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p)) t[p] = s[p];
            }
            return t;
        };
        return __assign.apply(this, arguments);
    };

    const videoThumbnailSettings = {
        videoThumbnail: true
    };

    return (function() {
        function VideoThumbnail(instance, $LG) {
            // get lightGallery core plugin instance
            this.core = instance;
            this.$LG = $LG;
            // extend module default settings with lightGallery core settings
            this.settings = __assign(__assign({}, videoThumbnailSettings), this.core.settings);
            return this;
        }

        VideoThumbnail.prototype.init = function () {
            const captureIcon = "bi-lightning";

            if (this.settings.videoThumbnail) {
                const videoThumbnailMenuItem = '<button type="button" aria-label="Capture Video Poster" title="Capture Video Poster" id="captureThumbnail" class="'+captureIcon+' lg-icon" style="font-size: 1rem;display: none"></button>' +
                    '<div class="spinner-border spinner-border-sm float-end mt-3 me-3" role="status" id="captureThumbnailSpinner"></div>';

                this.core.$toolbar.append(videoThumbnailMenuItem);

                this.videoThumbnail();
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
                    const lgItemId = "lg-item-" + this.core.lgId + "-" + e.detail.index;
                    const lgItemEl = $("#" + lgItemId);

                    if (lgItemEl.length > 0 && lgItemEl.children().find('video')[0] !== undefined) {
                        const video = lgItemEl.children().find('video')[0];
                        if (video.paused || video.ended || !!(video.currentTime > 0 && !video.paused && !video.ended && video.readyState > 2)) {
                            $("."+captureIcon+".lg-icon").css("display", "block");
                        }
                    }
                }
            });

            this.core.outer
                .find('.'+captureIcon)
                .first()
                .on('click.lg', () => {
                    $("#captureThumbnail").hide();
                    $("#captureThumbnailSpinner").show();
                    $("#captureThumbnail").prop( "disabled", true);
                    $("#captureThumbnailSpinner").prop( "disabled", true);

                    const funObject = Util.getLgFunction(this, "videoThumbnailFun");

                    // Execute function
                    if (typeof funObject.fn === 'function' && funObject.args !== null && funObject.args.length > 0) {
                        $("#metadataId").val(funObject.args);
                        funObject.fn(funObject.args, this.core.lgId, this.core.index);
                    } else {
                        if (shashin) {
                            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.lgvideothumb.title"), shashin.getTranslatedValue("main.toast.lgvideothumb.body"), {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
                        }
                        $("#captureThumbnail").show();
                        $("#captureThumbnailSpinner").hide();
                        $("#captureThumbnail").prop( "disabled", false);
                        $("#captureThumbnailSpinner").prop( "disabled", false);
                    }
                });
        }

        // Call after button element attached
        VideoThumbnail.prototype.videoThumbnail = function (e) {
            $("#captureThumbnailSpinner").hide();

            // Edit video thumbnail
            return ""
        }

        VideoThumbnail.prototype.destroy = function() {}

        return VideoThumbnail;
    }());
}));