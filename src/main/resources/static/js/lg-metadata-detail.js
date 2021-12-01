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
                e = '<button type="button" aria-label="View Photo Details" class="bi-info-circle lg-icon" style="font-size: 1rem;"></button>',
                    this.core.$toolbar.append(e),
                    this.metadataDetail()
            }

            this.core.outer
                .find('.bi-info-circle')
                .first()
                .on('click.lg', () => {
                    let currentDynamicEl = this.settings.dynamicEl[this.core.index] && this.settings.dynamicEl[this.core.index].hasOwnProperty("func") ? this.settings.dynamicEl[this.core.index] : this.core.galleryItems[this.core.index];
                    if (currentDynamicEl.hasOwnProperty("func")) {
                        $("#metadataId").val(currentDynamicEl.args.id);
                        currentDynamicEl.func(currentDynamicEl.args);
                    } else if ($($(".thumbnail-bl")[this.core.index].firstChild).attr("tag")) {
                        //console.log($($(".thumbnail-bl")[this.core.index].firstChild).attr("tag"))
                        const fn = this.settings.metadataDetailFunc;
                        let toArgObj = {};
                        try {
                            toArgObj = JSON.parse($($(".thumbnail-bl")[this.core.index].firstChild).attr("tag"));
                        } catch (e) {}

                        if(typeof fn === 'function') {
                            fn(toArgObj);
                        }
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