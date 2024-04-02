/**
 * MY - Added Dec 6 2021
 * - Created this module to cast media using castjs
 */

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
        shashin.printMessageToConsole("Castjs Event: " + e);
        if (e === "connect" && $("#chromecasting").hasClass("bi-cast")) {
            $("#chromecasting").addClass('bi-stop-circle').removeClass('bi-cast');
        }
    });

    cjs.on('error', (e) => {
        shashin.printMessageToConsole("Castjs Error: " + e);
        if (e !== "invalid_parameter") {
            $("#chromecasting").css({"display": "none", "font-size": "1rem"});
        }
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
                e = '<button type="button" aria-label="Cast Media" title="Cast Media" id="chromecasting" class="bi-cast lg-icon" style="font-size: 1rem;display: none;"></button>',
                    this.core.$toolbar.append(e),
                    this.castMedia()

                if (cjs.available) {
                    $("#chromecasting").css({"display": "block", "font-size": "1rem"});
                }

                if (cjs.connected && $("#chromecasting").hasClass('bi-cast')) {
                    $("#chromecasting").addClass('bi-stop-circle').removeClass('bi-cast');
                }
            }

            this.core.outer
                .find('.bi-cast, .bi-stop-circle')
                .first()
                .on('click.lg', () => {
                    if (cjs.available) {
                        if ($("#chromecasting").hasClass("bi-stop-circle")) {
                            $("#chromecasting").addClass('bi-cast').removeClass('bi-stop-circle');
                            cjs.disconnect();
                        } else {

                            let index = this.core.index;

                            let currentDynamicEl = this.settings.dynamicEl[index] && this.settings.dynamicEl[index].hasOwnProperty("args") ?
                                this.settings.dynamicEl : this.core.galleryItems;

                            // Play slide show too if casting and playing
                            this.core.LGel.on('lgBeforeSlide', (e) => {
                                getMediaMetadata(currentDynamicEl, e.detail.index);
                            });
                        }
                    }

                    function getMediaMetadata(currentDynamicEl, index) {
                        let metadataId = "";
                        let currentDynamicElIndex = currentDynamicEl[index];

                        if (currentDynamicElIndex.hasOwnProperty("args")) {
                            metadataId = currentDynamicElIndex.args;
                        } else if (currentDynamicElIndex.hasOwnProperty("metadataId")) {
                            metadataId = currentDynamicElIndex.metadataId;
                        }

                        if (shashin &&
                            /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(metadataId)
                        ) {
                            $("#metadataId").val(metadataId);
                            shashin.getMetadata(metadataId).then(function (metadata) {
                                castMetadataMedia(metadata, cjs);
                            });
                        } else {
                            if (shashin) {
                                shashin.showToastMessage("Could not get cast", "Could not cast. Metadata ID not valid", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                            }
                        }
                    }

                    function castMetadataMedia(metadata, cjs) {
                        if (metadata !== null && metadata.hasOwnProperty("id")) {
                            const getUrl = window.location;
                            const baseUrl = getUrl.protocol + "//" + getUrl.host;
                            const cjsMetadata = {
                                title      : metadata.title
                            }

                            if (metadata.description !== null && metadata.description !== "") {
                                cjsMetadata["description"] = metadata.description;
                            }

                            if (metadata.videoUrl !== null) {
                                try {
                                    cjsMetadata["poster"] = baseUrl + ((metadata.thumbnailUrlOriginal !== undefined && metadata.thumbnailUrlOriginal !== null) ? metadata.thumbnailUrlOriginal : metadata.thumbnailUrlSmall);

                                    cjs.cast(baseUrl + metadata.videoUrl, cjsMetadata);
                                } catch(e) {
                                    // Error
                                    if (shashin) {
                                        shashin.showToastMessage("Could not cast", "Castjs could not cast video "+metadata.videoUrl+": "+e.message, {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                                    }
                                    // console.log(e)
                                    $("#chromecasting").css({"display": "none", "font-size": "1rem"});
                                }
                            } else if (metadata.thumbnailUrlOriginal !== null) {
                                try {
                                    cjs.cast(baseUrl + metadata.thumbnailUrlOriginal + ".jpg", cjsMetadata);
                                } catch(e) {
                                    // Error
                                    if (shashin) {
                                        shashin.showToastMessage("Could not cast", "Castjs could not cast video "+metadata.videoUrl+": "+e.message, {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                                    }
                                    $("#chromecasting").css({"display": "none", "font-size": "1rem"});
                                }
                            }

                            cjs.on('disconnect', () => {
                                this.core.closeGallery();
                                $("#chromecasting").addClass('bi-cast').removeClass('bi-stop-circle');
                            });
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