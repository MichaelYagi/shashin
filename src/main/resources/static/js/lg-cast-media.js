! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgCastMedia = l()
}(this, (function() {
    "use strict";

    // Create new Castjs instance
    let cjs = new Castjs();

    cjs.on('available', () => {
        $("#chromecasting").css({"display": "block", "font-size": "1rem"});
    });

    cjs.on('event', (e) => {
        // console.log(e)
        if (e === "connect" && $("#chromecasting").hasClass("bi-cast")) {
            $("#chromecasting").addClass('bi-stop').removeClass('bi-cast');
        }
    });

    cjs.on('error', (e) => {
        //console.log(e)
        $("#chromecasting").css({"display": "none", "font-size": "1rem"});
    });

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
                e = '<button type="button" aria-label="Cast Media" id="chromecasting" class="bi-cast lg-icon" style="font-size: 1rem;display: none;"></button>',
                    this.core.$toolbar.append(e),
                    this.castMedia()

                if (cjs.available) {
                    $("#chromecasting").css({"display": "block", "font-size": "1rem"});
                }
            }

            this.core.outer
                .find('.bi-cast, .bi-stop')
                .first()
                .on('click.lg', () => {
                    if (cjs.available) {
                        if ($("#chromecasting").hasClass("bi-stop")) {
                            $("#chromecasting").addClass('bi-cast').removeClass('bi-stop');
                            cjs.disconnect();
                        } else {

                            let metadataId = null;
                            let currentDynamicEl = this.settings.dynamicEl[this.core.index] && this.settings.dynamicEl[this.core.index].hasOwnProperty("func") ? this.settings.dynamicEl[this.core.index] : this.core.galleryItems[this.core.index];

                            if (currentDynamicEl.hasOwnProperty("func")) {
                                $("#metadataId").val(currentDynamicEl.args);
                                metadataId = currentDynamicEl.args;
                            } else if ($($(".thumbnail-bl")[this.core.index])) {
                                //console.log($($(".thumbnail-bl")[this.core.index]))
                                let toArgObj = "";
                                try {
                                    toArgObj = $($(".thumbnail-bl")[this.core.index]).attr("id").substring(4);
                                } catch (e) {
                                }
                                metadataId = toArgObj;
                            }

                            if (metadataId !== null && $.isArray(metadataId) === true && metadataId.length > 0) {
                                metadataId = metadataId[0];
                            }

                            if (shashin &&
                                /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(metadataId)) {
                                shashin.getMetadata(metadataId).then(function (metadata) {
                                    castMetadataMedia(metadata, cjs);
                                });
                            }
                        }
                    }

                    function castMetadataMedia(metadata, cjs) {
                        if (metadata !== null && metadata.hasOwnProperty("id")) {
                            const getUrl = window.location;
                            const baseUrl = getUrl.protocol + "//" + getUrl.host;

                            if (metadata.videoUrl !== null) {
                                try {
                                    cjs.cast(baseUrl + metadata.videoUrl);
                                } catch(e) {
                                    // Error
                                    // console.log(e)
                                    $("#chromecasting").css({"display": "none", "font-size": "1rem"});
                                }
                            } else if (metadata.thumbnailUrlOriginal !== null) {
                                try {
                                    cjs.cast(baseUrl + metadata.thumbnailUrlOriginal + ".jpg");
                                } catch(e) {
                                    // Error
                                    // console.log(e)
                                    $("#chromecasting").css({"display": "none", "font-size": "1rem"});
                                }
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