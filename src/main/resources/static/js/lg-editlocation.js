/**
 * MY - Added Sept 13 2021
 * - Created this module to edit lat/lng location
 */

! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgEditLocation = l()
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
            editLocation: !0
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
            var icon = "bi-info-circle";
            if (this.settings.showControls) {
                icon = "bi-geo-alt";
            }
            if (this.settings.editLocation) {
                e = '<button type="button" aria-label="Edit location" class="'+icon+' lg-icon" style="font-size: 1rem;"></button>',
                    this.core.$toolbar.append(e),
                    this.editLocation()
            }

            this.core.outer
                .find('.'+icon)
                .first()
                .on('click.lg', () => {
                    let currentDynamicEl = this.settings.dynamicEl[this.core.index];
                    currentDynamicEl.func(currentDynamicEl.args);
                });
        },
            n.prototype.editLocation = function() {
                // Edit location action
                return ""
            },
            n.prototype.destroy = function() {},
            n
    }()
}));