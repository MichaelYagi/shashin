! function(_window, pluginFunction) {
    // TODO: Define the plugin name here
    let pluginName = "lgShashinEditor";

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

    // TODO: Define attributes to be used in setting
    const pluginSettings = {
        shashinEditor: true
    };

    let metadataId = null;
    let metadataObj = null;

    return (function() {
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

        // TODO: Define class
        function Plugin(instance, $LG) {
            // get lightGallery core plugin instance
            this.core = instance;

            this.$LG = $LG;

            // TODO: Extend module default settings with lightGallery core settings
            this.settings = __assign(__assign({}, pluginSettings), this.core.settings);

            return this;
        }

        // TODO: Initialize the plugin
        Plugin.prototype.init = function () {
            // TODO: Initialize the plugin icon
            const menuIcon = "bi-sliders";

            // TODO: Get from pluginSettings variable
            if (this.settings.shashinEditor) {
                // TODO: Initialize the plugin button
                const pluginMenuItem = '<button type="button" aria-label="'+shashin.getTranslatedValue("main.pages.lg.plugins.editor.msg")+'" title="'+shashin.getTranslatedValue("main.pages.lg.plugins.editor.msg")+'" id="shashineditor" class="'+menuIcon+' lg-icon" style="font-size: 1rem;display: none"></button>';

                this.core.$toolbar.append(pluginMenuItem);

                this.plugin();
            }

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

            // TODO: Define what happens when clicking the menu button
            this.core.outer
                .find('.'+menuIcon)
                .first()
                .off('click.lg')
                .on('click.lg', () => {
                    if (metadataObj !== null) {
                        let lgIndex = this.core.index;
                        if ($("#lgIndex").val().length > 0) {
                            lgIndex = $("#lgIndex").val();
                        }
                        editMedia(metadataObj, lgIndex);
                    }
                });
        };

        // TODO: Call after button element attached
        Plugin.prototype.plugin = function (e) {

            return "";
        };

        // TODO: Define cleanup procedures
        Plugin.prototype.destroy = function() {
            // Call to clean up stuff
        };

        return Plugin;
    }());
}));