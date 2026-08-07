(function(global) {
    global.lgDownload = {
        name: 'download',
        init: function(ctx) {
            if (!ctx.gallery.options.download) return;

            var btn = document.createElement('button');
            btn.type = 'button';
            btn.id = 'lgDownloadButton';
            btn.className = 'shoji-toolbar-button bi-download';
            btn.style.fontSize = '1rem';
            var title = shashin.getTranslatedValue("main.pages.lg.plugins.download.msg");
            btn.title = title;
            btn.setAttribute('aria-label', title);

            btn.addEventListener('click', function() {
                var idx = ctx.gallery.currentIndex;
                var item = ctx.gallery.items[idx];
                if (!item) return;

                var url = item.downloadUrl;
                if (!url && item.metadataId) {
                    url = (item.video ? item.src : '/api/v1/image/' + item.metadataId) + '/download?v=' + Date.now();
                }
                if (!url) return;

                var a = document.createElement('a');
                a.href = url;
                a.download = '';
                a.style.display = 'none';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
            });

            return ctx.ui.toolbar('right', btn);
        }
    };
})(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this);
