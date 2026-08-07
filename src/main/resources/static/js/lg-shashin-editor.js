(function(global) {
    global.lgShashinEditor = {
        name: 'shashinEditor',
        init: function(ctx) {
            if (!ctx.gallery.options.shashinEditor) return;

            var btn = document.createElement('button');
            btn.type = 'button';
            btn.id = 'shashineditor';
            btn.className = 'shoji-toolbar-button bi-sliders';
            btn.style.fontSize = '1rem';
            btn.style.display = 'none';
            var title = shashin.getTranslatedValue("main.pages.lg.plugins.editor.msg");
            btn.title = title;
            btn.setAttribute('aria-label', 'Edit Media');

            btn.addEventListener('click', function() {
                var idx = ctx.gallery.currentIndex;
                var metadataObj = null;

                if (Util.sessionStorageAvailable()) {
                    metadataObj = JSON.parse(sessionStorage.getItem("metadata"));
                } else if (Util.localStorageAvailable()) {
                    metadataObj = JSON.parse(localStorage.getItem("metadata"));
                } else {
                    metadataObj = JSON.parse($("#metadata").val());
                }

                if (metadataObj && metadataObj.id) {
                    shashin.getMetadata(metadataObj.id).then(function(metadata) {
                        if (Util.sessionStorageAvailable()) {
                            sessionStorage.setItem("metadata", JSON.stringify(metadata));
                        } else if (Util.localStorageAvailable()) {
                            localStorage.setItem("metadata", JSON.stringify(metadata));
                        } else {
                            $("#metadata").val(JSON.stringify(metadata));
                        }

                        if (metadata && metadata.type.indexOf("image") >= 0 && metadata.type.indexOf("gif") < 0) {
                            var lgIndex = idx;
                            if ((lgIndex === undefined || lgIndex === null || lgIndex < 0) && $("#lgIndex").val().length > 0) {
                                lgIndex = parseInt($("#lgIndex").val());
                            }
                            initializeEditor(metadata, lgIndex);
                        }
                    });
                }
            });

            return ctx.ui.toolbar('right', btn);
        }
    };
})(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this);
