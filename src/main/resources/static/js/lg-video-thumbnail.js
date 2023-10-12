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

            if (this.settings.videoThumbnail) {
                e = '<button type="button" aria-label="Capture Thumbnail" title="Capture Thumbnail" class="bi-bounding-box-circles lg-icon" style="font-size: 1rem;display: none"></button>',
                    this.core.$toolbar.append(e),
                    this.videoThumbnail()
            }

            this.core.LGel.on('lgHasVideo.video', () => {
                if ($(".lg-video-play-button").length === 0) {
                    $(".bi-bounding-box-circles.lg-icon").css("display", "block");
                } else {
                    $(".lg-video-play-button").on('click', function () {
                        $(".bi-bounding-box-circles.lg-icon").css("display", "block");
                    });
                }
            })

            this.core.LGel.on('lgBeforeOpen', () => {
                $(".bi-bounding-box-circles.lg-icon").css("display", "none");
            })

            this.core.outer
                .find('.bi-bounding-box-circles')
                .first()
                .on('click.lg', () => {
                    let currentDynamicEl = this.settings.dynamicEl[this.core.index] && this.settings.dynamicEl[this.core.index].hasOwnProperty("func") ? this.settings.dynamicEl[this.core.index] : this.core.galleryItems[this.core.index];
                    if (currentDynamicEl.hasOwnProperty("func")) {
                        $("#metadataId").val(currentDynamicEl.args);
                        currentDynamicEl.vtfunc(currentDynamicEl.args);
                    } else if ($($(".thumbnail-bl")[this.core.index]).children('a').attr("tag")) {
                        //console.log($($(".thumbnail-bl")[this.core.index]).children('a').attr("tag"))
                        const fn = this.settings.videoThumbnailFunc;
                        let id = "";
                        try {
                            id = $($(".thumbnail-bl")[this.core.index]).children('a').attr("tag");
                        } catch (e) {}

                        if(typeof fn === 'function' && id.length > 0) {
                            fn(id);
                        }
                    }
                });
        },
            n.prototype.videoThumbnail = function(e) {

                // Edit video thumbnail
                return ""
            },
            n.prototype.destroy = function() {},
            n
    }()
}));