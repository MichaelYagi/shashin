/**
 * MY - Added Oct 28 2023
 * - Working Plugin template
 * lightGallery(document.getElementById('lightgallery'), {
 *   plugins: [lgPluginName],
 *   licenseKey: 'your_license_key',
 *   speed: 500,
 *   pluginSettingAttribute: true,
 *   pluginFunctionName: someFunction
 *   // ... other settings
 * });
 */

! function(_window, pluginFunction) {
    // TODO: Define the plugin name here
    let pluginName = "lgPluginName";

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
        pluginSettingAttribute: true
    };

    return (function() {
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
            const menuIcon = "bi-0-circle";

            if (this.settings.pluginSettingAttribute) {
                // TODO: Initialize the plugin button
                const pluginMenuItem = '<button type="button" aria-label="Plugin Name" title="Plugin Name" id="captureThumbnail" class="'+menuIcon+' lg-icon"></button>';

                this.core.$toolbar.append(pluginMenuItem);

                this.plugin();
            }

            this.core.LGel.on('lgBeforeSlide', (e) => {
                console.log("You are on slide " + e.detail.index);
            });

            // TODO: Define what happens when clicking the menu button
            this.core.outer
                .find('.'+menuIcon)
                .first()
                .on('click.lg', () => {
                    alert("This is a template for setting up a plugin!");

                    // Get current slide
                    let currentDynamicEl = (this.settings.dynamicEl[this.core.index] && this.settings.dynamicEl[this.core.index].hasOwnProperty("pluginFunctionName")) ?
                        this.settings.dynamicEl[this.core.index] : this.core.galleryItems[this.core.index];

                    // Optionally get a function and args and execute when creating lg items dynamically
                    if (currentDynamicEl.hasOwnProperty("pluginFunctionName") && currentDynamicEl.hasOwnProperty("args")) {
                        $("#metadataId").val(currentDynamicEl.args);
                        // Execute function
                        currentDynamicEl.pluginFunctionName(currentDynamicEl.args, this.core.lgId, this.core.index);
                    } else if ($($(".thumbnail-bl")[this.core.index]).children('a').attr("tag") && this.settings.hasOwnProperty("pluginFunc")) {
                        const fn = this.settings.pluginFunc;
                        let metadataId = "";

                        // Get the metadataId from different sources
                        if ($($(".thumbnail-bl")[this.core.index]).children('a').attr("tag") !== undefined) {
                            metadataId = $($(".thumbnail-bl")[this.core.index]).children('a').attr("tag");
                        } else if ($($(".thumbnail-bl")[this.core.index]).children('a').attr("data-metadataid") !== undefined) {
                            metadataId = $($(".thumbnail-bl")[this.core.index]).children('a').attr("data-metadataid");
                        }

                        if (metadataId.length === 0 && $($(".thumbnail-bl")[this.core.index])) {
                            if ($($(".thumbnail-bl")[this.core.index]).children('a').attr("tag") !== undefined) {
                                metadataId = $($(".thumbnail-bl")[this.core.index]).children('a').attr("tag");
                            } else if ($($(".thumbnail-bl")[this.core.index]).children('a').attr("data-metadataid") !== undefined) {
                                metadataId = $($(".thumbnail-bl")[this.core.index]).children('a').attr("data-metadataid");
                            }
                        }

                        // Execute function
                        if (typeof fn === 'function' && metadataId.length > 0) {
                            fn(metadataId, this.core.lgId, this.core.index);
                        }
                    }
                });
        }

        // TODO: Call after button element attached
        Plugin.prototype.plugin = function (e) {

            return ""
        }

        // TODO: Define cleanup procedures
        Plugin.prototype.destroy = function() {
            // Call to clean up stuff
        }

        return Plugin;
    }());
}));