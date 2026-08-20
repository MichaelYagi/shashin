(function(global) {
    global.lgVideoThumbnail = {
        name: 'videoThumbnail',
        init: function(ctx) {
            if (!ctx.gallery.options.videoThumbnail) return;

            var btn = document.createElement('button');
            btn.type = 'button';
            btn.id = 'captureThumbnail';
            btn.className = 'shoji-toolbar-button bi-lightning';
            btn.style.fontSize = '1rem';
            btn.style.display = 'none';
            var title = shashin.getTranslatedValue("main.pages.lg.plugins.video.msg");
            btn.title = title;
            btn.setAttribute('aria-label', 'Capture Video Poster');

            var spinner = document.createElement('div');
            spinner.id = 'captureThumbnailSpinner';
            spinner.className = 'spinner-border spinner-border-sm float-end mt-3 me-3';
            spinner.setAttribute('role', 'status');
            spinner.style.display = 'none';

            btn.addEventListener('click', function() {
                var idx = ctx.gallery.currentIndex;
                var item = ctx.gallery.items[idx];
                if (!item) return;

                var fn = null;
                var args = null;

                if (typeof item.videoThumbnailFun === 'function' && item.args != null && String(item.args).length > 0) {
                    fn = item.videoThumbnailFun;
                    args = item.args;
                } else if (item.metadataId && typeof ctx.gallery.options.videoThumbnailFun === 'function') {
                    fn = ctx.gallery.options.videoThumbnailFun;
                    args = item.metadataId;
                }

                if (fn && args) {
                    $("#captureThumbnail").hide();
                    $("#captureThumbnailSpinner").show();
                    $("#captureThumbnail").prop("disabled", true);
                    $("#metadataId").val(args);
                    fn(args, '', idx);
                } else if (shashin) {
                    shashin.showToastMessage(
                        shashin.getTranslatedValue("main.toast.lgvideothumb.title"),
                        shashin.getTranslatedValue("main.toast.lgvideothumb.body"),
                        {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor: "danger"}
                    );
                    $("#captureThumbnail").show();
                    $("#captureThumbnailSpinner").hide();
                    $("#captureThumbnail").prop("disabled", false);
                }
            });

            btn.appendChild(spinner);
            var removeBtn = ctx.ui.toolbar('right', btn);

            return function() {
                removeBtn();
            };
        }
    };
})(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this);
