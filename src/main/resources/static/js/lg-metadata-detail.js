! function(e, l) {
    "object" == typeof exports && "undefined" != typeof module ? module.exports = l() : "function" == typeof define && define.amd ? define(l) : (e = "undefined" != typeof globalThis ? globalThis : e || self).lgMetadataDetail = l()
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
            metadataDetail: !0
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
            if (this.settings.metadataDetail) {
                e = '<button type="button" aria-label="View Photo Details" title="View Photo Details" class="bi-info-circle lg-icon" style="font-size: 1rem;"></button>',
                    this.core.$toolbar.append(e),
                    this.metadataDetail()
            }

            this.core.outer
                .find('.bi-info-circle')
                .first()
                .on('click.lg', () => {
                    const functionName = "metadataDetailFun";
                    let error = false;

                    let currentDynamicEl = this.settings.dynamicEl[this.core.index] && this.settings.dynamicEl[this.core.index].hasOwnProperty(functionName) ?
                        this.settings.dynamicEl[this.core.index] : this.core.galleryItems[this.core.index];

                    if (currentDynamicEl.hasOwnProperty(functionName) && currentDynamicEl.hasOwnProperty("args")) {
                        if (currentDynamicEl.args.length > 0 && typeof currentDynamicEl[functionName] === 'function') {
                            $("#metadataId").val(currentDynamicEl.args);
                            currentDynamicEl[functionName](currentDynamicEl.args);
                        } else {
                            error = true;
                        }
                    } else {
                        let fn = null;
                        let metadataId = "";

                        if (currentDynamicEl.hasOwnProperty("metadataId") && this.settings.hasOwnProperty(functionName)) {
                            metadataId = currentDynamicEl.metadataId;
                            fn = this.settings[functionName];
                        }

                        if (typeof fn === 'function' && metadataId.length > 0) {
                            fn(metadataId);
                        } else {
                            error = true;
                        }
                    }

                    if (error === true && shashin) {
                        shashin.showToastMessage("Could not get media details", "Could not get media details. Function or metadata ID not found", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
                    }
                });
        },
            n.prototype.metadataDetail = function() {
                // Edit metadata detail
                return ""
            },
            n.prototype.destroy = function() {},
            n
    }()
}));