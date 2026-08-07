(function(global) {
    global.lgMetadataDetail = {
        name: 'metadataDetail',
        init: function(ctx) {
            if (!ctx.gallery.options.metadataDetail) return;

            var btn = document.createElement('button');
            btn.type = 'button';
            btn.id = 'metadataDetail';
            btn.className = 'shoji-toolbar-button bi-info-circle';
            btn.style.fontSize = '1rem';
            var title = shashin.getTranslatedValue("main.pages.lg.plugins.metadata.msg");
            btn.title = title;
            btn.setAttribute('aria-label', title);

            btn.addEventListener('click', function() {
                var idx = ctx.gallery.currentIndex;
                var item = ctx.gallery.items[idx];
                if (!item) return;

                var fn = null;
                var args = null;

                if (typeof item.metadataDetailFun === 'function' && item.args != null && String(item.args).length > 0) {
                    fn = item.metadataDetailFun;
                    args = item.args;
                } else if (item.metadataId && typeof ctx.gallery.options.metadataDetailFun === 'function') {
                    fn = ctx.gallery.options.metadataDetailFun;
                    args = item.metadataId;
                }

                if (fn && args) {
                    $("#metadataId").val(args);
                    fn(args);
                    if ($('#propMetadata').length > 0) {
                        shashin.openedInfoFromLG = true;
                    }
                } else if (shashin) {
                    shashin.showToastMessage(
                        shashin.getTranslatedValue("main.toast.lgmetadata.title"),
                        shashin.getTranslatedValue("main.toast.lgmetadata.body"),
                        {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor: "danger"}
                    );
                }
            });

            return ctx.ui.toolbar('right', btn);
        }
    };
})(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this);
