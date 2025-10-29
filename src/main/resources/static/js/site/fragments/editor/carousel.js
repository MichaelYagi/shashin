// editor-carousel-snap.js (updated)
// - Title is NOT part of the track (we clone & pin portrait title separately).
// - Prev/Next wrap-around (loop behavior).
// - Sliders remain original elements and now are touch-friendly via CSS.
// - Uses scroll-snap; prev/next scrolls by one page; when at end goes to beginning and vice versa.

(function () {
    'use strict';

    const editorContainerId = 'editorContainer';
    const toolContainerId = 'editorToolContainer';
    const wrapperTrackSelector = '.tool-carousel-track';
    const prevBtnSelector = '.mobile-carousel-prev';
    const nextBtnSelector = '.mobile-carousel-next';
    const titleId = 'editorTitle';
    const titlePortraitWrapperId = 'editorTitlePortraitWrapper';

    function id(id) { return document.getElementById(id); }
    function q(sel) { return document.querySelector(sel); }

    function isPortrait() {
        if (window.matchMedia) {
            return window.matchMedia('(orientation: portrait)').matches || window.innerHeight > window.innerWidth;
        }
        return window.innerHeight > window.innerWidth;
    }

    function ensurePortraitTitleClone() {
        const editorContainer = id(editorContainerId);
        const titleOrig = id(titleId);
        if (!editorContainer || !titleOrig) return null;
        let wrapper = id(titlePortraitWrapperId);
        if (!wrapper) {
            wrapper = document.createElement('div');
            wrapper.id = titlePortraitWrapperId;
            editorContainer.appendChild(wrapper);
            // ensure positioned ancestor
            const comp = window.getComputedStyle(editorContainer).position;
            if (!comp || comp === 'static') editorContainer.style.position = 'relative';
        }
        // clone title, remove id to prevent duplicate id
        const clone = titleOrig.cloneNode(true);
        if (clone.removeAttribute) clone.removeAttribute('id');
        (function stripIds(node) {
            if (node.nodeType !== 1) return;
            if (node.hasAttribute && node.hasAttribute('id')) node.removeAttribute('id');
            Array.from(node.children || []).forEach(c => stripIds(c));
        })(clone);
        wrapper.innerHTML = '';
        wrapper.appendChild(clone);
        $("#editorTitle").hide();
        return wrapper;
    }

    function removePortraitTitleClone() {
        const wrapper = id(titlePortraitWrapperId);
        if (wrapper && wrapper.parentNode) wrapper.parentNode.removeChild(wrapper);
    }

    function getTrack() {
        return q(wrapperTrackSelector);
    }

    function pageCount(track) {
        if (!track) return 0;
        // count the number of child .tool-group elements visible as slides
        return track.querySelectorAll('.tool-group').length;
    }

    function pageWidth(track) {
        // width of one view page is track.clientWidth
        return track ? track.clientWidth : 0;
    }

    function currentPageIndex(track) {
        if (!track) return 0;
        return Math.round(track.scrollLeft / pageWidth(track));
    }

    function scrollToPage(track, index, smooth = true) {
        if (!track) return;
        const pages = pageCount(track);
        if (pages === 0) return;
        // wrap index into [0, pages-1]
        let idx = ((index % pages) + pages) % pages;
        const left = idx * pageWidth(track);
        track.scrollTo({ left: left, behavior: smooth ? 'smooth' : 'auto' });
    }

    function activateMobileUI() {
        const toolContainer = id(toolContainerId);
        const track = getTrack();
        const prevBtn = q(prevBtnSelector);
        const nextBtn = q(nextBtnSelector);
        if (!toolContainer || !track) return;

        // show panel and controls
        toolContainer.style.display = '';
        if (prevBtn) prevBtn.style.display = '';
        if (nextBtn) nextBtn.style.display = '';

        // create title clone
        ensurePortraitTitleClone();

        // Prev button: go to previous page (wrap)
        if (prevBtn) {
            prevBtn.onclick = function () {
                const pages = pageCount(track);
                if (pages <= 0) return;
                const curr = currentPageIndex(track);
                const prev = (curr - 1 + pages) % pages;
                scrollToPage(track, prev, true);
            };
        }
        if (nextBtn) {
            nextBtn.onclick = function () {
                const pages = pageCount(track);
                if (pages <= 0) return;
                const curr = currentPageIndex(track);
                const next = (curr + 1) % pages;
                scrollToPage(track, next, true);
            };
        }

        // keyboard left/right support
        track.addEventListener('keydown', function (ev) {
            if (ev.key === 'ArrowLeft') {
                const curr = currentPageIndex(track);
                const prev = (curr - 1 + pageCount(track)) % pageCount(track);
                scrollToPage(track, prev, true);
            } else if (ev.key === 'ArrowRight') {
                const curr = currentPageIndex(track);
                const next = (curr + 1) % pageCount(track);
                scrollToPage(track, next, true);
            }
        });

        // after user scroll ends, snap to closest page (so partial drags round nicely)
        let isScrolling;
        track.addEventListener('scroll', function () {
            window.clearTimeout(isScrolling);
            isScrolling = setTimeout(function () {
                const idx = currentPageIndex(track);
                scrollToPage(track, idx, true);
            }, 120);
        }, { passive: true });

        // ensure initial position is 0
        scrollToPage(track, 0, false);
    }

    function deactivateMobileUI() {
        const toolContainer = id(toolContainerId);
        const prevBtn = q(prevBtnSelector);
        const nextBtn = q(nextBtnSelector);
        const track = getTrack();
        if (toolContainer) toolContainer.style.display = '';
        if (prevBtn) prevBtn.style.display = 'none';
        if (nextBtn) nextBtn.style.display = 'none';

        removePortraitTitleClone();

        // cleanup handlers (simple approach: remove by replacing nodes)
        const prev = q(prevBtnSelector);
        const next = q(nextBtnSelector);
        if (prev) { prev.onclick = null; }
        if (next) { next.onclick = null; }

        // reset track scroll
        if (track) track.scrollTo({ left: 0, behavior: 'auto' });
    }

    function updateUI() {
        const isSmall = window.matchMedia('(max-width: 767.98px)').matches;
        if (isSmall && isPortrait()) {
            activateMobileUI();
        } else {
            $("#editorTitle").show();
            deactivateMobileUI();
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        updateUI();
        window.addEventListener('orientationchange', function () { setTimeout(updateUI, 120); }, { passive: true });
        window.addEventListener('resize', function () { updateUI(); }, { passive: true });
    });
})();