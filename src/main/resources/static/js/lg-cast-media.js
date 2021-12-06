! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgCastMedia = l()
}(this, (function() {
    "use strict";

    // Create new Castjs instance
    const cjs = new Castjs();

    var e = function() {
            return (e = Object.assign || function(e) {
                for (var l, n = 1, t = arguments.length; n < t; n++)
                    for (var c in l = arguments[n])
                        Object.prototype.hasOwnProperty.call(l, c) && (e[c] = l[c]);
                return e
            }).apply(this, arguments)
        },
        l = {
            castMedia: !0
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
            if (this.settings.castMedia) {
                e = '<button type="button" aria-label="Cast Media" id="cast" class="bi-cast lg-icon" style="font-size: 1rem;visibility: hidden"></button>',
                    this.core.$toolbar.append(e),
                    this.castMedia()
            }

            cjs.on('available',function () {
                $("#cast").css("visibility","visible");
            });

            this.core.outer
                .find('.bi-cast')
                .first()
                .on('click.lg', () => {
                    if (cjs.available) {
                        const getUrl = window.location;
                        const baseUrl = getUrl.protocol + "//" + getUrl.host;
                        let metadata = null;
                        let currentDynamicEl = this.settings.dynamicEl[this.core.index] && this.settings.dynamicEl[this.core.index].hasOwnProperty("func") ? this.settings.dynamicEl[this.core.index] : this.core.galleryItems[this.core.index];
                        if (currentDynamicEl.hasOwnProperty("func")) {
                            $("#metadataId").val(currentDynamicEl.args.id);
                            metadata = currentDynamicEl.args;
                        } else if ($($(".thumbnail-bl")[this.core.index].firstChild).attr("tag")) {
                            //console.log($($(".thumbnail-bl")[this.core.index].firstChild).attr("tag"))
                            const fn = this.settings.metadataDetailFunc;
                            let toArgObj = {};
                            try {
                                toArgObj = JSON.parse($($(".thumbnail-bl")[this.core.index].firstChild).attr("tag"));
                            } catch (e) {
                            }
                            metadata = toArgObj;
                        }

                        if (metadata !== null && metadata.hasOwnProperty("id")) {
                            if (metadata.videoUrl !== null) {
                                cjs.cast(baseUrl + metadata.videoUrl);
                            } else if (metadata.thumbnailUrlOriginal !== null) {
                                cjs.cast(baseUrl + metadata.thumbnailUrlOriginal);
                            }
                        }
                    }
                });
        },
            n.prototype.castMedia = function() {
                // Chromecast
                return ""
            },
            n.prototype.destroy = function() {},
            n
    }()
}));