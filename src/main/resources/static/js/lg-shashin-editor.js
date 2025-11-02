/**
 * MY - Added Oct 2 2025
 * - Created this module to edit media
 */

! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgShashinEditor = l()
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
            shashinEditor: !0
        };

    return function() {
        function editMedia(metadata, lgIndex) {
            if (metadata !== null && metadata.hasOwnProperty("id")) {
                // Open editor
                initializeEditor(metadata, lgIndex);
            }
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

            this.core.outer
                .find('.bi-sliders')
                .first()
                .off('click.lg')
                .on('click.lg', () => {
                    let metadataObj = {};

                    if (Util.sessionStorageAvailable() === true) {
                        metadataObj = JSON.parse(sessionStorage.getItem("metadata"));
                    } else if (Util.localStorageAvailable() === true) {
                        metadataObj = JSON.parse(localStorage.getItem("metadata"));
                    } else {
                        metadataObj = JSON.parse($("#metadata").val());
                    }

                    if (metadataObj !== null && metadataObj.type.indexOf("image") >= 0 && metadataObj.type.indexOf("gif") < 0) {
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