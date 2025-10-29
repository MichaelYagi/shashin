// editor-carousel-snap.js (updated - move title outside carousel into #editorContainer for mobile)
// - DOES NOT clone the title. Moves the original #editorTitle element out of the carousel
//   into the #editorContainer (so it is no longer a child of the tool track) when mobile+portrait.
// - Restores the original DOM position when not mobile.
// - Keeps existing carousel behavior (prev/next, keyboard, scroll-snap).
// - Stores the original parent/nextSibling to restore exact placement when leaving mobile.

(function () {
    'use strict';

    const editorContainerId = 'editorContainer';
    const toolContainerId = 'editorToolContainer';
    const wrapperTrackSelector = '.tool-carousel-track';
    const prevBtnSelector = '.mobile-carousel-prev';
    const nextBtnSelector = '.mobile-carousel-next';
    const titleId = 'editorTitle';

    function id(name) { return document.getElementById(name); }
    function q(sel) { return document.querySelector(sel); }

    let titleOriginalParent = null;
    let titleOriginalNextSibling = null;
    let titleSaved = false;

    function saveTitleLocation() {
        const title = id(titleId);
        if (!title || titleSaved) return;
        titleOriginalParent = title.parentNode;
        titleOriginalNextSibling = title.nextSibling; // may be null
        titleSaved = true;
    }

    // Move the original title element to be a child of #editorContainer (outside the carousel track)
    function moveTitleOutOfCarousel() {
        const title = id(titleId);
        const editorContainer = id(editorContainerId);
        if (!title || !editorContainer) return;
        // If already moved into editorContainer, do nothing
        if (title.parentNode === editorContainer) return;
        // Save original location first time
        saveTitleLocation();
        // Append into editorContainer (keeps same document, no cloning)
        editorContainer.appendChild(title);
        title.setAttribute('data-editor-title-moved', 'true');
    }

    function restoreTitlePosition() {
        const title = id(titleId);
        if (!title || !titleSaved) return;
        // If title already back to its original parent, nothing to do
        if (titleOriginalParent === title.parentNode) {
            title.removeAttribute('data-editor-title-moved');
            return;
        }
        // Insert at original position (before the original nextSibling if present)
        try {
            if (titleOriginalNextSibling && titleOriginalParent.contains(titleOriginalNextSibling)) {
                titleOriginalParent.insertBefore(title, titleOriginalNextSibling);
            } else {
                titleOriginalParent.appendChild(title);
            }
            title.removeAttribute('data-editor-title-moved');
        } catch (e) {
            // fallback: place back before the carousel track inside the tool container
            const tc = id(toolContainerId);
            const track = q(wrapperTrackSelector);
            if (tc && track) {
                tc.insertBefore(title, track);
            }
            title.removeAttribute('data-editor-title-moved');
        }
    }

    function isPortrait() {
        if (window.matchMedia) {
            return window.matchMedia('(orientation: portrait)').matches || window.innerHeight > window.innerWidth;
        }
        return window.innerHeight > window.innerWidth;
    }

    // Safety move: if title still ends up inside the track, move it to be before the track in tool container
    function ensureTitleOutsideTrack() {
        const title = id(titleId);
        const track = q(wrapperTrackSelector);
        const toolContainer = id(toolContainerId);
        if (!title || !track || !toolContainer) return;
        if (track.contains(title)) {
            toolContainer.insertBefore(title, track);
            title.style.display = '';
        }
    }

    function getTrack() { return q(wrapperTrackSelector); }

    function pageCount(track) {
        if (!track) return 0;
        return track.querySelectorAll('.tool-group').length;
    }

    function pageWidth(track) {
        return track ? track.clientWidth : 0;
    }

    function currentPageIndex(track) {
        if (!track) return 0;
        const w = pageWidth(track) || 1;
        return Math.round(track.scrollLeft / w);
    }

    function scrollToPage(track, index, smooth = true) {
        if (!track) return;
        const pages = pageCount(track);
        if (pages === 0) return;
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

        // Move the original title out of the carousel into the editor container
        moveTitleOutOfCarousel();

        // Show container & controls
        toolContainer.style.display = '';
        if (prevBtn) prevBtn.style.display = '';
        if (nextBtn) nextBtn.style.display = '';

        // Prev/Next
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

        // Keyboard support
        function onKey(ev) {
            if (ev.key === 'ArrowLeft') {
                const prev = (currentPageIndex(track) - 1 + pageCount(track)) % pageCount(track);
                scrollToPage(track, prev, true);
            } else if (ev.key === 'ArrowRight') {
                const next = (currentPageIndex(track) + 1) % pageCount(track);
                scrollToPage(track, next, true);
            }
        }
        track.addEventListener('keydown', onKey);

        // Scroll snapping after end of scroll
        let isScrolling;
        function onScroll() {
            window.clearTimeout(isScrolling);
            isScrolling = setTimeout(function () {
                const idx = currentPageIndex(track);
                scrollToPage(track, idx, true);
            }, 120);
        }
        track.addEventListener('scroll', onScroll, { passive: true });

        // start at page 0
        scrollToPage(track, 0, false);

        // keep handlers for cleanup
        track._editorHandlers = { onKey, onScroll };
    }

    function deactivateMobileUI() {
        const toolContainer = id(toolContainerId);
        const prevBtn = q(prevBtnSelector);
        const nextBtn = q(nextBtnSelector);
        const track = getTrack();
        if (toolContainer) toolContainer.style.display = '';
        if (prevBtn) prevBtn.style.display = 'none';
        if (nextBtn) nextBtn.style.display = 'none';

        // cleanup handlers if attached
        if (track && track._editorHandlers) {
            const h = track._editorHandlers;
            track.removeEventListener('keydown', h.onKey);
            track.removeEventListener('scroll', h.onScroll);
            delete track._editorHandlers;
        }

        // reset scroll
        if (track) track.scrollTo({ left: 0, behavior: 'auto' });

        // Restore the title back to its original place in the DOM
        restoreTitlePosition();

        // ensure title visible
        const title = id(titleId);
        if (title) title.style.display = '';
    }

    function updateUI() {
        // make sure original location is saved early
        saveTitleLocation();
        // small safety: ensure title isn't inside the track
        ensureTitleOutsideTrack();

        const isSmall = window.matchMedia('(max-width: 767.98px)').matches;
        if (isSmall && isPortrait()) {
            activateMobileUI();
        } else {
            deactivateMobileUI();
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        // save original location now
        saveTitleLocation();
        updateUI();
        window.addEventListener('orientationchange', function () { setTimeout(updateUI, 120); }, { passive: true });
        window.addEventListener('resize', function () { updateUI(); }, { passive: true });
    });

    // debug API
    window.editorCarouselSnap = {
        updateUI,
        moveTitleOutOfCarousel,
        restoreTitlePosition,
        scrollToPageIndex: (i) => {
            const t = getTrack();
            scrollToPage(t, i, true);
        }
    };
})();