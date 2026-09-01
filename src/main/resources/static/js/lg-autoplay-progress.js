(function(global) {
    global.lgAutoplayProgress = {
        name: 'autoplayProgress',
        init: function(ctx) {
            if (!ctx.gallery.options.autoplayProgress) return;

            var barHidden = false;

            function getBar() {
                return ctx.ui.outer().querySelector('.shoji-autoplay-progress');
            }

            function setButtonIcon(hidden) {
                btn.style.opacity = '0';
                setTimeout(function() {
                    if (hidden) {
                        btn.classList.add('bi-chevron-bar-up');
                        btn.classList.remove('bi-chevron-bar-down');
                    } else {
                        btn.classList.add('bi-chevron-bar-down');
                        btn.classList.remove('bi-chevron-bar-up');
                    }
                    btn.style.opacity = '1';
                }, 150);
            }

            function toggle() {
                var bar = getBar();
                if (!bar) return;
                barHidden = !barHidden;
                bar.style.visibility = barHidden ? 'hidden' : '';
                setButtonIcon(barHidden);
            }

            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'shoji-toolbar-button bi-chevron-bar-down';
            btn.style.fontSize = '1rem';
            btn.style.transition = 'opacity 0.15s';
            btn.title = 'Toggle progress bar (P)';
            btn.setAttribute('aria-label', 'Toggle progress bar');
            btn.addEventListener('click', toggle);

            var removeBtn = ctx.ui.toolbar('right', btn);
            var unregister = ctx.ui.registerShortcut('p', toggle);

            return function() {
                unregister();
                removeBtn();
            };
        }
    };
})(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this);
