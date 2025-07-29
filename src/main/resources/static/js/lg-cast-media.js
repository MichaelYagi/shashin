/**
 * MY - Added Dec 6 2021
 * - Created this module to cast media using castjs
 */

! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgCastMedia = l()
}(this, (function() {
    "use strict";

    // Create new Castjs instance
    let cjs = null;

    if (typeof Castjs != "undefined") {
        cjs = new Castjs();

        cjs.on('available', () => {
            $("#chromecasting").css({"display": "block", "font-size": "1rem"});
        });

        cjs.on('event', (e) => {
            if (shashin) {
                shashin.printMessageToConsole("Castjs Event: " + e, {
                    tag: "cast"
                });
            }

            if (e === "connect" && $("#chromecasting").hasClass("bi-cast")) {
                $("#chromecasting").addClass('bi-stop-circle').removeClass('bi-cast');
            }
        });

        cjs.on('error', (e) => {
            if (shashin) {
                shashin.printMessageToConsole("Castjs Error: " + e, {
                    tag: "cast",
                    consoleType: shashin.consoleTypes.error
                });
            }

            if (e !== "invalid_parameter") {
                $("#chromecasting").css({"display": "none", "font-size": "1rem"});
            }
        });
    } else {
        if (shashin) {
            shashin.printMessageToConsole("Castjs Error: Object DNE", {
                tag: "cast"
            });
        }
    }

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
            if (cjs !== null && this.settings.castMedia) {
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
                    if (cjs !== null && cjs.available) {
                        if ($("#chromecasting").hasClass("bi-stop-circle")) {
                            $("#chromecasting").addClass('bi-cast').removeClass('bi-stop-circle');
                            cjs.disconnect();
                        } else {

                            let index = this.core.index;

                            let currentDynamicEl = this.settings.dynamicEl[index] && this.settings.dynamicEl[index].hasOwnProperty("args") ?
                                this.settings.dynamicEl : this.core.galleryItems;

                            // Play slide show too if casting and playing
                            this.core.LGel.on('lgBeforeSlide', (e) => {
                                if (shashin) {
                                    shashin.printMessageToConsole("lgBeforeSlide state: "+cjs.state, {
                                        tag: "cast"
                                    });
                                }
                                if (cjs.state === "connected" || cjs.state === "paused" || cjs.state === "playing" || cjs.state === "ended") {
                                    currentDynamicEl = this.settings.dynamicEl[e.detail.index] && this.settings.dynamicEl[e.detail.index].hasOwnProperty("args") ?
                                        this.settings.dynamicEl : this.core.galleryItems;
                                    if (shashin) {
                                        shashin.printMessageToConsole("currentDynamicEl null: "+(currentDynamicEl === null), {
                                            tag: "cast"
                                        });
                                        shashin.printMessageToConsole("currentDynamicEl index: "+e.detail.index, {
                                            tag: "cast"
                                        });
                                    }
                                    getMediaMetadata(currentDynamicEl, e.detail.index, this.core);
                                }
                            });

                            this.core.LGel.on('lgBeforeClose', (e) => {
                                cjs.disconnect();
                                this.core.LGel.off('lgBeforeSlide');
                            });

                            getMediaMetadata(currentDynamicEl, index, this.core);
                        }
                    }

                    function getMediaMetadata(currentDynamicEl, index, lgCore) {
                        let metadataId = "";
                        let currentDynamicElIndex = currentDynamicEl[index];

                        if (typeof currentDynamicElIndex !== 'undefined' && currentDynamicElIndex !== null) {
                            if (currentDynamicElIndex.hasOwnProperty("args")) {
                                metadataId = currentDynamicElIndex.args;
                            } else if (currentDynamicElIndex.hasOwnProperty("metadataId")) {
                                metadataId = currentDynamicElIndex.metadataId;
                            }
                        }

                        if (shashin &&
                            /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(metadataId)
                        ) {
                            $("#metadataId").val(metadataId);
                            shashin.getMetadata(metadataId).then(function (metadata) {
                                castMetadataMedia(metadata, cjs, lgCore);
                            });
                        } else {
                            if (shashin) {
                                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.lgcast.media.title"), shashin.getTranslatedValue("main.toast.lgcast.media.message"), {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
                            }
                        }
                    }

                    function castMetadataMedia(metadata, cjs, lgCore) {
                        if (metadata !== null && metadata.hasOwnProperty("id")) {
                            const getUrl = window.location;
                            let baseUrl = getUrl.protocol + "//" + getUrl.host;
                            if (metadata.hasOwnProperty("baseUrl")) {
                                baseUrl = metadata.baseUrl;
                            }

                            const cjsMetadata = {
                                title: metadata.title
                            }

                            if (metadata.description !== null && metadata.description !== "") {
                                cjsMetadata["description"] = metadata.description;
                            }

                            if (metadata.videoUrl !== null) {
                                try {
                                    cjsMetadata["poster"] = baseUrl + ((metadata.thumbnailUrlOriginal !== undefined && metadata.thumbnailUrlOriginal !== null) ? "/api/v1/thumbnails/original/"+metadata.id : "/api/v1/thumbnails/225/"+metadata.id);
                                    cjs.src = baseUrl + metadata.videoUrl;

                                    cjs.cast(baseUrl + metadata.videoUrl, cjsMetadata);
                                    cjs.seek(0);

                                    if (shashin) {
                                        shashin.printMessageToConsole("Castjs casting video: " + baseUrl + metadata.videoUrl, {
                                            tag: "cast"
                                        });
                                    }
                                } catch(e) {
                                    // Error
                                    if (shashin) {
                                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.lgcast.media.title"), shashin.getTranslatedValue("main.toast.lgcast.video.message", (baseUrl + metadata.videoUrl))+baseUrl +": "+e.message, {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
                                    }
                                    $("#chromecasting").css({"display": "none", "font-size": "1rem"});
                                }
                            } else if (metadata.thumbnailUrlOriginal !== null) {
                                try {
                                    cjs.src = baseUrl + "/api/v1/image/"+metadata.id+".jpg";

                                    cjs.cast(baseUrl + "/api/v1/image/"+metadata.id+".jpg", cjsMetadata);

                                    if (shashin) {
                                        shashin.printMessageToConsole("Castjs casting image: " + baseUrl + "/api/v1/image/"+metadata.id+".jpg", {
                                            tag: "cast"
                                        });
                                    }
                                } catch(e) {
                                    // Error
                                    if (shashin) {
                                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.lgcast.media.title"), shashin.getTranslatedValue("main.toast.lgcast.image.message", (baseUrl + "/api/v1/image/"+metadata.id+".jpg"))+baseUrl +": "+e.message, {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
                                    }
                                    $("#chromecasting").css({"display": "none", "font-size": "1rem"});
                                }
                            }

                            cjs.on('disconnect', () => {
                                // lgCore.closeGallery();
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