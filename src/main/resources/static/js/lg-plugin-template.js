/**
 * MY - Added Oct 28 2023
 * - Working Plugin template. See below example.
 *
 * function pluginFunction(metadataId, lgId, lgIndex) {}
 *
 * const configs = {
 *     plugins: [lgPluginName],
 *     pluginSettingAttribute: true,
 *     pluginFunctionName: pluginFunction, // Optional, see below
 *     // ... other settings
 * };
 *
 * const lg = lightGallery(lgEl, configs);
 *
 * lg.openGallery();
 */

! function(_window, pluginFunction) {
    // TODO: Define the plugin name here
    let pluginName = "lgPluginName";

    // Boilerplate
    if ("object" == typeof exports && "undefined" != typeof module) {
        module.exports = l();
    } else if ("function" == typeof define && define.amd) {
        define(pluginFunction);
    } else {
        (_window = "undefined" != typeof globalThis ? globalThis : _window || self)[pluginName] = pluginFunction();
    }
} (this, (function() {
    // Boilerplate
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

    // TODO: Define attributes and set in configs
    const pluginSettings = {
        pluginSettingAttribute: true
    };

    return (function() {
        // TODO: Define class. Change class name in line with functionality.
        function Plugin(instance, $LG) {
            // get lightGallery core plugin instance
            this.core = instance;

            this.$LG = $LG;

            // Extends module default settings with lightGallery core settings
            this.settings = __assign(__assign({}, pluginSettings), this.core.settings);

            return this;
        }

        // TODO: Initialize the plugin
        Plugin.prototype.init = function () {
            // TODO: Define the plugin icon
            const menuIcon = "bi-0-circle";

            // TODO: Get from attribute in pluginSettings
            if (this.settings.pluginSettingAttribute) {
                // TODO: Initialize the plugin toolbar button
                const pluginMenuItem = '<button type="button" aria-label="Plugin Name" title="Plugin Name" id="captureThumbnail" class="'+menuIcon+' lg-icon" style="font-size: 1rem;"></button>';

                this.core.$toolbar.append(pluginMenuItem);

                // Additional optional init call
                this.plugin();
            }

            this.core.LGel.off('lgBeforeSlide').on('lgBeforeSlide', (e) => {
                console.log("You are on slide " + e.detail.index);

                shashin.closeToastMessages();
            });

            // TODO: Define what happens when clicking the button in the menu
            this.core.outer
                .find('.'+menuIcon)
                .first()
                .off('click.lg')
                .on('click.lg', () => {
                    if (typeof shashin !== 'undefined') {
                        shashin.showToastMessage(null, "This is a template for setting up a plugin!", {
                            tag: "lgPluginName",
                            autohide: false,
                            closeButton: true
                        });
                    } else {
                        console.log("This is a template for setting up a plugin!");
                    }

                    /* This is an optional call from the settings depending on integration */
                    // Or call an external function, eg. shashin.showToastMessage
                    const funObject = Util.getLgFunction(this, "pluginFunctionName");

                    // Execute function
                    if (typeof funObject.fn === 'function' && funObject.args !== null && funObject.args.length > 0) {
                        // if this.settings.pluginFunctionName is found in settings, metadataId will be in funObject.args
                        funObject.fn(funObject.args, this.core.lgId, this.core.index);
                    } else if (typeof shashin !== 'undefined') {
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.lgplugin.title"),
                            "Config property pluginFunctionName not defined in configs",
                            {
                                icon:"bi-exclamation-triangle",
                                iconColor:"#FF0000",
                                autohide: false,
                                borderColor:"danger"
                            }
                        );
                    } else {
                        console.warn("Config property pluginFunctionName not defined in config");
                    }
                    /* End optional call */
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