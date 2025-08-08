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
                e = '<button type="button" id="metadataDetail" aria-label="View Photo Details" title="'+shashin.getTranslatedValue("main.pages.lg.plugins.metadata.msg")+'" class="bi-info-circle lg-icon" style="font-size: 1rem;"></button>',
                    this.core.$toolbar.append(e),
                    this.metadataDetail()
            }

            this.core.outer
                .find('.bi-info-circle')
                .first()
                .on('click.lg', () => {
                    const funObject = Util.getLgFunction(this, "metadataDetailFun");

                    // Execute function
                    if (typeof funObject.fn === 'function' && funObject.args !== null && funObject.args.length > 0) {
                        $("#metadataId").val(funObject.args);
                        funObject.fn(funObject.args);
                        if ($('#propMetadata').length > 0) {
                            shashin.openedInfoFromLG = true;
                            $('#propMetadata').modal({keyboard: false});
                        }
                    } else if (shashin) {
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.lgmetadata.title"), shashin.getTranslatedValue("main.toast.lgmetadata.body"), {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
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