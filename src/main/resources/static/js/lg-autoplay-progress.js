(function(global) {
    global.lgAutoplayProgress = {
        name: 'autoplayProgress',
        init: function(ctx) {
            if (!ctx.gallery.options.autoplayProgress) return;

            var unregister = ctx.ui.registerShortcut('p', function() {
                var bar = ctx.ui.outer().querySelector('.shoji-autoplay-progress');
                if (bar) bar.style.visibility = (bar.style.visibility === 'hidden') ? '' : 'hidden';
            });

            return function() {
                unregister();
            };
        }
    };
})(typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : this);
