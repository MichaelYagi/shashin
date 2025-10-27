/**
 * Mocha + Chai tests for justifiedGallery.explore.min.js
 *
 * This test stubs MutationObserver, provides rAF and Util, and deterministically
 * advances sinon fake timers so the script's async startup completes.
 *
 * The tests accept both fully-justified rows and last-row non-justified behavior:
 * - If layout fully-justified the row, assert sum widths + gaps ≈ container width.
 * - Otherwise (last non-justified row), assert widths ≈ aspect * targetRowHeight
 *   and the total width does not exceed the container width.
 *
 * Usage:
 *   npm install --save-dev mocha chai jsdom-global sinon
 *   npx mocha test/justifiedGallery.test.js
 */

const { expect } = require('chai');
const jsdomGlobal = require('jsdom-global');
const sinon = require('sinon');
const path = require('path');

const scriptPath = path.resolve(__dirname, '../../../main/resources/static/js/justifiedGallery.explore.min.js');
const pathToScript = require.resolve(scriptPath);

function clearModuleCache() {
    try { delete require.cache[pathToScript]; } catch (e) {}
}

describe('justifiedGallery.explore.min.js', function () {
    this.timeout(5000);

    let cleanupJsdom;
    let clock;

    beforeEach(() => {
        cleanupJsdom = jsdomGlobal();

        // Minimal no-op MutationObserver so script can construct one without error
        const NoopMutationObserver = class {
            constructor(cb) { this._cb = cb; }
            observe() {}
            disconnect() {}
            takeRecords() { return []; }
        };
        global.MutationObserver = NoopMutationObserver;
        if (typeof window !== 'undefined') window.MutationObserver = NoopMutationObserver;

        clock = sinon.useFakeTimers();

        // Globals the script expects
        global.Util = { isMobile: () => false };
        // rAF polyfilled via setTimeout so sinon fake timers can drive it
        global.requestAnimationFrame = cb => setTimeout(() => cb(Date.now()), 0);
        global.window.innerWidth = 1200;

        clearModuleCache();
    });

    afterEach(() => {
        if (clock) clock.restore();
        if (cleanupJsdom) cleanupJsdom();
        try { delete global.Util; } catch (e) {}
        try { delete global.requestAnimationFrame; } catch (e) {}
        try { delete global.window.innerWidth; } catch (e) {}
        try { delete global.MutationObserver; } catch (e) {}
        try { delete window.MutationObserver; } catch (e) {}
        clearModuleCache();
    });

    // helper to create gallery DOM BEFORE requiring script
    function makeGalleryRow({ containerId = 'scroll-gallery', itemAspects = [], containerWidth = 600, targetRowHeight = 100, gap = 4 } = {}) {
        document.documentElement.style.setProperty('--jg-target-row-height', String(targetRowHeight));
        document.documentElement.style.setProperty('--jg-gap', String(gap));

        const container = document.createElement('div');
        container.id = containerId;
        document.body.appendChild(container);

        const dateSection = document.createElement('div');
        dateSection.className = 'dateSection';
        container.appendChild(dateSection);

        const row = document.createElement('div');
        row.className = 'row';
        dateSection.appendChild(row);

        // stub bounding rects used by the script
        row.getBoundingClientRect = () => ({ width: containerWidth, height: 0, top: 0, left: 0, right: containerWidth, bottom: 0 });
        dateSection.getBoundingClientRect = () => ({ width: containerWidth, height: 0, top: 0, left: 0, right: containerWidth, bottom: 0 });

        itemAspects.forEach((aspect, i) => {
            const thumb = document.createElement('div');
            thumb.className = 'photo-thumbnail-container';
            const img = document.createElement('img');
            img.id = `image-${i}`;
            if (typeof aspect === 'number') img.dataset.aspect = String(aspect);
            // make waitForSectionImages quick
            img.decode = () => Promise.resolve();
            thumb.appendChild(img);
            row.appendChild(thumb);
        });

        return { container, dateSection, row };
    }

    function loadScriptAndEnsureInitialized() {
        require(pathToScript);

        // If script attached DOMContentLoaded listener, dispatch it so init runs.
        if (!window.ScrollJustifiedGallery) {
            const evt = new window.Event('DOMContentLoaded', { bubbles: true, cancelable: true });
            document.dispatchEvent(evt);
        }

        // Advance timers used during init: initial setTimeout(...,20) and rAF (0ms)
        clock.tick(25);
        clock.tick(1);
        clock.tick(20);
    }

    async function ensureRowLaidOut(row) {
        // Await the async layoutDateBody (it waits for images and computes plans),
        // then execute the rAF-applied DOM writes by advancing timers.
        await window.ScrollJustifiedGallery.layoutDateBody(row);

        // run queued rAF writes (polyfilled via setTimeout(...,0))
        clock.tick(1);
        clock.tick(20);

        // allow microtasks to settle so dataset changes are visible
        await Promise.resolve();
    }

    it('attaches a public API on window.ScrollJustifiedGallery when loaded', async () => {
        makeGalleryRow({ itemAspects: [1, 1] });
        loadScriptAndEnsureInitialized();

        expect(window.ScrollJustifiedGallery).to.be.an('object');
        expect(window.ScrollJustifiedGallery).to.have.property('layoutAll').that.is.a('function');
        expect(window.ScrollJustifiedGallery).to.have.property('layoutDateBody').that.is.a('function');
        expect(window.ScrollJustifiedGallery).to.have.property('waitForSectionImages').that.is.a('function');
    });

    it('sets widths/heights/margins in a predictable way (handles last-row non-justified case)', async () => {
        const containerWidth = 501; // large so last-row non-justified branch is likely
        const gap = 4;
        const targetRowHeight = 100;
        const itemAspects = [1, 1];

        const { row } = makeGalleryRow({ itemAspects, containerWidth, targetRowHeight, gap });

        loadScriptAndEnsureInitialized();
        await ensureRowLaidOut(row);

        const thumbs = Array.from(row.querySelectorAll('.photo-thumbnail-container'));
        expect(thumbs).to.have.length(itemAspects.length);

        const parsedWidths = thumbs.map(t => parseInt(t.style.width, 10));
        const parsedHeights = thumbs.map(t => parseInt(t.style.height, 10));
        const marginRights = thumbs.map(t => t.style.marginRight);

        // sanity: values were written
        parsedWidths.forEach(w => expect(w).to.be.a('number').and.to.be.greaterThan(0));
        parsedHeights.forEach(h => expect(h).to.be.a('number').and.to.be.greaterThan(0));

        const sumWidths = parsedWidths.reduce((s, v) => s + v, 0);

        // Tolerances
        const tolerancePx = 4;

        // Determine whether the row was fully-justified (filled the container) or left at target height
        const filledDelta = Math.abs(sumWidths + gap - containerWidth);

        if (filledDelta <= tolerancePx) {
            // Fully justified case: total should approximately match the container width
            expect(filledDelta).to.be.at.most(tolerancePx);
            // margins: first items should have gap as marginRight, last item 0px
            expect(marginRights[0]).to.equal(gap + 'px');
            expect(marginRights[marginRights.length - 1]).to.equal('0px');
        } else {
            // Last-row non-justified case: widths should be approx aspect * targetRowHeight
            const expectedWidths = itemAspects.map(a => Math.round(a * targetRowHeight));
            for (let i = 0; i < expectedWidths.length; i++) {
                expect(Math.abs(parsedWidths[i] - expectedWidths[i])).to.be.at.most(tolerancePx);
            }
            // total must not exceed container width (except a tiny rounding tolerance)
            expect(sumWidths + gap).to.be.at.most(containerWidth + tolerancePx);
            // margins still should be gap and 0 for last item
            expect(marginRights[0]).to.equal(gap + 'px');
            expect(marginRights[marginRights.length - 1]).to.equal('0px');
        }
    });

    it('sets widths for a last non-justified single-item row according to target height when justifyLastRow is false', async () => {
        const containerWidth = 1000;
        const targetRowHeight = 90;
        const itemAspects = [2];

        const { row } = makeGalleryRow({ itemAspects, containerWidth, targetRowHeight, gap: 4 });

        loadScriptAndEnsureInitialized();
        await ensureRowLaidOut(row);

        const thumb = row.querySelector('.photo-thumbnail-container');
        expect(thumb).to.exist;

        const width = parseInt(thumb.style.width, 10);
        const height = parseInt(thumb.style.height, 10);

        const expected = Math.round(2 * targetRowHeight);
        expect(Math.abs(width - expected)).to.be.at.most(4);
        expect(Math.abs(height - targetRowHeight)).to.be.at.most(4);
    });

    it('waitForSectionImages resolves when images support decode', async () => {
        const { row } = makeGalleryRow({ itemAspects: [1, 1] });

        loadScriptAndEnsureInitialized();

        await window.ScrollJustifiedGallery.waitForSectionImages(row);
    });
});