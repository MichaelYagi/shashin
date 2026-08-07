(function(global) {
    // Module-level Castjs instance — initialised once regardless of how many gallery instances open
    var cjs = null;

    if (typeof Castjs !== 'undefined') {
        cjs = new Castjs();

        cjs.on('available', function() {
            var el = document.getElementById('chromecasting');
            if (el) el.style.display = 'block';
        });

        cjs.on('event', function(e) {
            if (shashin) {
                shashin.printMessageToConsole("Castjs Event: " + e, {tag: "cast"});
            }
            if (e === 'connect') {
                var el = document.getElementById('chromecasting');
                if (el && el.classList.contains('bi-cast')) {
                    el.classList.add('bi-stop-circle');
                    el.classList.remove('bi-cast');
                }
            }
        });

        cjs.on('error', function(e) {
            if (shashin) {
                shashin.printMessageToConsole("Castjs Error: " + e, {tag: "cast", consoleType: shashin.consoleTypes && shashin.consoleTypes.error});
            }
            if (e !== 'invalid_parameter') {
                var el = document.getElementById('chromecasting');
                if (el) el.style.display = 'none';
            }
        });
    } else if (shashin) {
        shashin.printMessageToConsole("Castjs Error: Object DNE", {tag: "cast"});
    }

    global.lgCastMedia = {
        name: 'castMedia',
        init: function(ctx) {
            if (!cjs || !ctx.gallery.options.castMedia) return;

            var btn = document.createElement('button');
            btn.type = 'button';
            btn.id = 'chromecasting';
            btn.className = 'shoji-toolbar-button bi-cast';
            btn.style.fontSize = '1rem';
            btn.style.display = 'none';
            btn.title = shashin.getTranslatedValue("main.pages.lg.plugins.cast.msg");
            btn.setAttribute('aria-label', 'Cast Media');

            if (cjs.available) btn.style.display = 'block';
            if (cjs.connected && btn.classList.contains('bi-cast')) {
                btn.classList.add('bi-stop-circle');
                btn.classList.remove('bi-cast');
            }

            var offBeforeSlide = null;
            var offBeforeClose = null;

            btn.addEventListener('click', function() {
                if (!cjs || !cjs.available) return;

                if (btn.classList.contains('bi-stop-circle')) {
                    btn.classList.add('bi-cast');
                    btn.classList.remove('bi-stop-circle');
                    cjs.disconnect();
                    return;
                }

                var idx = ctx.gallery.currentIndex;

                // Wire slide-change and close handlers for the casting session
                if (offBeforeSlide) { offBeforeSlide(); offBeforeSlide = null; }
                if (offBeforeClose) { offBeforeClose(); offBeforeClose = null; }

                offBeforeSlide = ctx.on('beforeSlide', function(detail) {
                    if (cjs.state === 'connected' || cjs.state === 'paused' || cjs.state === 'playing' || cjs.state === 'ended') {
                        if (shashin) shashin.printMessageToConsole("lgBeforeSlide state: " + cjs.state, {tag: "cast"});
                        getMediaMetadata(ctx.gallery.items, detail.to);
                    }
                });

                offBeforeClose = ctx.on('beforeClose', function() {
                    cjs.disconnect();
                    if (offBeforeSlide) { offBeforeSlide(); offBeforeSlide = null; }
                });

                getMediaMetadata(ctx.gallery.items, idx);
            });

            function getMediaMetadata(items, index) {
                var item = items[index];
                var metadataId = item && (item.metadataId || item.args);

                if (shashin && metadataId && /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(metadataId)) {
                    $("#metadataId").val(metadataId);
                    shashin.getMetadata(metadataId).then(function(metadata) {
                        castMetadataMedia(metadata);
                    });
                } else if (shashin) {
                    shashin.showToastMessage(
                        shashin.getTranslatedValue("main.toast.lgcast.media.title"),
                        shashin.getTranslatedValue("main.toast.lgcast.media.message"),
                        {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor: "danger"}
                    );
                }
            }

            function castMetadataMedia(metadata) {
                if (!metadata || !metadata.id) return;

                var loc = window.location;
                var baseUrl = metadata.baseUrl || (loc.protocol + '//' + loc.host);
                var cjsMeta = {title: metadata.title};

                if (metadata.description) cjsMeta.description = metadata.description;

                try {
                    if (metadata.videoUrl) {
                        cjsMeta.poster = baseUrl + (metadata.thumbnailUrlOriginal
                            ? '/api/v1/image/original/' + metadata.id
                            : '/api/v1/thumbnails/225/' + metadata.id);
                        cjs.src = baseUrl + metadata.videoUrl;
                        cjs.cast(baseUrl + metadata.videoUrl, cjsMeta);
                        cjs.seek(0);
                        if (shashin) shashin.printMessageToConsole("Castjs casting video: " + baseUrl + metadata.videoUrl, {tag: "cast"});
                    } else if (metadata.thumbnailUrlOriginal) {
                        cjs.src = baseUrl + '/api/v1/image/original/' + metadata.id + '.jpg';
                        cjs.cast(baseUrl + '/api/v1/image/original/' + metadata.id + '.jpg', cjsMeta);
                        if (shashin) shashin.printMessageToConsole("Castjs casting image: " + cjs.src, {tag: "cast"});
                    }
                } catch(e) {
                    if (shashin) {
                        shashin.showToastMessage(
                            shashin.getTranslatedValue("main.toast.lgcast.media.title"),
                            shashin.getTranslatedValue("main.toast.lgcast.media.message") + ': ' + e.message,
                            {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor: "danger"}
                        );
                    }
                    var el = document.getElementById('chromecasting');
                    if (el) el.style.display = 'none';
                }

                cjs.on('disconnect', function() {
                    var el = document.getElementById('chromecasting');
                    if (el) {
                        el.classList.add('bi-cast');
                        el.classList.remove('bi-stop-circle');
                    }
                });
            }

            var removeBtn = ctx.ui.toolbar('right', btn);

            return function() {
                if (offBeforeSlide) offBeforeSlide();
                if (offBeforeClose) offBeforeClose();
                removeBtn();
            };
        }
    };
})(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this);
