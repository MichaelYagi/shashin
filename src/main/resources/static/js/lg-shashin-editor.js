/**
 * MY - Added Oct 2 2025
 * - Created this module to edit media
 */

! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgShashinEditor = l()
}(this, (function() {
    "use strict";

    let metadataId = null;
    let metadataObj = null;

    var e = function() {
            return (e = Object.assign || function(e) {
                for (var l, n = 1, t = arguments.length; n < t; n++)
                    for (var c in l = arguments[n])
                        Object.prototype.hasOwnProperty.call(l, c) && (e[c] = l[c]);
                return e
            }).apply(this, arguments)
        },
        l = {
            shashinEditor: !0
        };

    return function() {
        function editMedia(metadata, lgIndex) {
            if (metadata !== null && metadata.hasOwnProperty("id")) {
                // Open editor
                initializeEditor(metadata, lgIndex);
            }
        }

        function getMediaMetadataId(currentDynamicEl, index) {
            let metadataId = null;
            let currentDynamicElIndex = currentDynamicEl[index];

            if (typeof currentDynamicElIndex !== 'undefined' && currentDynamicElIndex !== null) {
                if (currentDynamicElIndex.hasOwnProperty("args")) {
                    metadataId = currentDynamicElIndex.args;
                } else if (currentDynamicElIndex.hasOwnProperty("metadataId")) {
                    metadataId = currentDynamicElIndex.metadataId;
                }

                if (shashin &&
                    /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(metadataId)
                ) {
                    if ($("#metadataId").val().length > 0 && $("#metadataId").val() === metadataId) {
                        return metadataId;
                    }

                    if ($("#metadataId").val().length > 0) {
                        return $("#metadataId").val();
                    }

                    return metadataId;
                } else {
                    if (shashin) {
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.lgedit.media.title"), shashin.getTranslatedValue("main.toast.lgedit.media.message"), {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
                    }
                }
            } else {
                if (shashin) {
                    shashin.printMessageToConsole("currentDynamicEl null: "+(currentDynamicEl === null), {
                        tag: "edit"
                    });
                }
            }

            return metadataId;
        }

        function n(n, t) {
            return this.core = n,
                this.$LG = t,
                this.settings = e(e({}, l), this.core.settings),
                this
        }

        return n.prototype.init = function() {
            var e = "";
            if (this.settings.shashinEditor) {
                e = '<button type="button" aria-label="Edit Media" title="'+shashin.getTranslatedValue("main.pages.lg.plugins.editor.msg")+'" id="shashineditor" class="bi-sliders lg-icon" style="font-size: 1rem;display: none;"></button>',
                    this.core.$toolbar.append(e),
                    this.shashinEditor()
            }

            // Get the media type
            this.core.LGel.off('lgAfterSlide').on('lgAfterSlide', (e) => {

                const currentDynamicEl = this.settings.dynamicEl[e.detail.index] && this.settings.dynamicEl[e.detail.index].hasOwnProperty("args") ?
                    this.settings.dynamicEl : this.core.galleryItems;
                if (shashin) {
                    shashin.printMessageToConsole("currentDynamicEl null: "+(currentDynamicEl === null), {
                        tag: "edit"
                    });
                    shashin.printMessageToConsole("currentDynamicEl index: "+e.detail.index, {
                        tag: "edit"
                    });
                }
                metadataId = getMediaMetadataId(currentDynamicEl, e.detail.index);

                if (metadataId !== null) {
                    shashin.getMetadata(metadataId).then(function (metadata) {
                        if (metadata.type.indexOf("image") >= 0 && metadata.type.indexOf("gif") < 0) {
                            metadataObj = metadata;
                            $("#shashineditor").css("display", "block");
                        } else {
                            $("#shashineditor").css("display", "none");
                        }
                    });
                }
            });

            this.core.outer
                .find('.bi-sliders')
                .first()
                .off('click.lg')
                .on('click.lg', () => {
                    shashin.showToastMessage(null, shashin.getTranslatedValue("main.toast.dashboard.title"), {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        autohide: false,
                        tag: "editor"
                    });

                    if (metadataObj !== null) {
                        let lgIndex = this.core.index;
                        if ($("#lgIndex").val().length > 0) {
                            lgIndex = $("#lgIndex").val();
                        }
                        editMedia(metadataObj, lgIndex);
                    }
                });
        },
            n.prototype.shashinEditor = function() {
                // Chromecast
                return ""
            },
            n.prototype.destroy = function() {},
            n
    }()
}));