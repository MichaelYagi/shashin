const {assert,expect} = require("chai");
const sinon = require("sinon");
require("mocha");
require('../helper.js');

const { JSDOM } = require("jsdom");

const dom = new JSDOM(`<!DOCTYPE html><html><body></body></html>`, { url: "http://localhost" });
global.window = dom.window;
global.document = dom.window.document;
global.navigator = dom.window.navigator;

const constantsjs = require('../../../main/resources/static/js/site/fragments/app/constants.js');
const toastjs = require('../../../main/resources/static/js/site/fragments/app/toast.js');
const modaljs = require('../../../main/resources/static/js/site/fragments/app/modal.js');
const batchjs = require('../../../main/resources/static/js/site/fragments/app/batch.js');
const lightgalleryjs = require('../../../main/resources/static/js/site/fragments/app/lightgallery.js');
const mapjs = require('../../../main/resources/static/js/site/fragments/app/map.js');
const metadatajs = require('../../../main/resources/static/js/site/fragments/app/metadata.js');
const overlayjs = require('../../../main/resources/static/js/site/fragments/app/overlay.js');
const utiljs = require('../../../main/resources/static/js/site/fragments/app/util.js');
const appjs = require('../../../main/resources/static/js/site/app.js');

global.shashin = { ...constantsjs, ...toastjs, ...modaljs, ...batchjs, ...lightgalleryjs, ...mapjs, ...metadatajs, ...overlayjs, ...utiljs, ...appjs };
global.Castjs = require('../../../main/resources/static/js/cast.min');
global.lgCastMedia = require('../../../main/resources/static/js/lg-cast-media');
global.lgDownload = require('../../../main/resources/static/js/lg-download');
global.lgShashinEditor = require('../../../main/resources/static/js/lg-shashin-editor');

const shojiPlugin = function(name) { return { name: name, init: function() {} }; };
function MockShoji(el, options) {
    this.element = el;
    this.options = options || {};
    this.items = (options && options.items) ? options.items : [];
    this.currentIndex = 0;
}
MockShoji.prototype.open = function() {};
MockShoji.prototype.close = function() { return 0; };
MockShoji.prototype.goTo = function() {};
MockShoji.prototype.updateSlides = function(items) { this.items = items || []; };
MockShoji.prototype.on = function() { return function() {}; };
MockShoji.prototype.reinit = function() {};
MockShoji.prototype.destroy = function() {};
MockShoji.Zoom = shojiPlugin('zoom');
MockShoji.Fullscreen = shojiPlugin('fullscreen');
MockShoji.RotateFlip = shojiPlugin('rotateFlip');
MockShoji.Autoplay = shojiPlugin('autoplay');
MockShoji.Layout = shojiPlugin('layout');
MockShoji.ActiveThumbnail = shojiPlugin('activeThumbnail');
global.Shoji = MockShoji;
global.Worker = require('../../../main/resources/static/js/subworkers');
global.ol = require('../../../main/resources/static/js/ol.min');
global.Util = require('../../../main/resources/static/js/site/util');

describe('#shashin app tests', function() {
    it('enable debug console output', function() {
        const consoleSpy = sinon.spy(console, 'log');

        shashin.enableDebug();
        assert.isTrue(shashin.showDebug);

        shashin.printMessageToConsole("test message", {tags: ["test"]});
        shashin.printMessageToConsole("tik");
        shashin.printMessageToConsole("tok", {tags: ["tik"]});
        shashin.printMessageToConsole("fail", {tags: ["sinon","tik","tok"]});
        shashin.printMessageToConsole("fail", {tags: ["sinon","tik","all","tok"]});

        assert.isTrue(consoleSpy.calledWith("test message - Tags: test"));
        assert.isTrue(consoleSpy.calledWith("tik - Tags: all"));
        assert.isTrue(consoleSpy.calledWith("tok - Tags: tik"));
        assert.isTrue(consoleSpy.calledWith("fail - Tags: sinon,tik,tok"));
        assert.isFalse(consoleSpy.calledWith("failz - Tag: sinon"));
        assert.isTrue(consoleSpy.calledWith("fail - Tags: all"));

        consoleSpy.restore();

        // assert.isTrue(true);
    });

    it('disable debug console output', function() {
        shashin.disableDebug();
        assert.isFalse(shashin.showDebug);
    });

    it('MetadataIdList tests', function() {
        const input = document.createElement("input");
        input.type = "hidden";
        input.id = "multiSelectMetadataIds";
        input.name = "multiSelectMetadataIds";
        input.value = "[]";
        document.body.appendChild(input);

        shashin.addToMetadataIdList("first_metadata");
        shashin.addToMetadataIdList("second_metadata");
        shashin.addToMetadataIdList("third_metadata");
        let metadataList = shashin.getMetadataIdList();
        assert.isArray(metadataList);
        assert.lengthOf(metadataList, 3);
        expect(metadataList).to.eql(["first_metadata", "second_metadata", "third_metadata"]);
        shashin.removeFromMetadataIdList("second_metadata");
        metadataList = shashin.getMetadataIdList();
        assert.lengthOf(metadataList, 2);
        expect(metadataList).to.eql(["first_metadata", "third_metadata"]);
        shashin.removeAllMetadataIdList();
        metadataList = shashin.getMetadataIdList();
        assert.lengthOf(metadataList, 0);
        expect(metadataList).to.eql([]);
    });


    it('Map sources', function() {
        let source = shashin.getMapSource();
        expect(source.urls.join('|')).to.include('openstreetmap');

        source = shashin.getMapSource("maptilerBA");
        expect(source.urls.join('|')).to.include('maptiler');

        source = shashin.getMapSource("invalidSourceAndDefaultingToOSM");
        expect(source.urls.join('|')).to.include('openstreetmap');
    });

    it('Shashin lightgallery init', function () {
        const div = document.createElement("div");
        div.id = "someElement";
        document.body.appendChild(div);

        const additionalConfig = {
            overrideBaseConfigs: true,
            plugins: []
        };
        shashin.initLightGallery('someElement', additionalConfig, ".mediaLink");
        const gallery = shashin.getLightGallery();
        assert.isNotNull(gallery);
        assert.isFunction(gallery.open);
        assert.isFunction(gallery.openGallery);
        assert.isFunction(gallery.refresh);
    });

    it('gallery element', function () {
        const div = document.createElement("div");
        div.id = "someElement";
        document.body.appendChild(div);

        shashin.setLightGalleryElement('someElement');
        assert.equal(shashin.getLightGalleryElement().id,'someElement');

        shashin.setLightGalleryElement('asdf');
        assert.isNull(shashin.getLightGalleryElement());
    });

    it('lightgallery element', function () {
        const div = document.createElement("div");
        div.id = "someElement";
        document.body.appendChild(div);

        shashin.setLightGalleryElement('someElement');
        shashin.setLightGallery();
        let gallery = shashin.getLightGallery();
        assert.isNotNull(gallery);
        assert.isFunction(gallery.open);
        assert.isFunction(gallery.openGallery);

        shashin.setLightGalleryElement('asdf');
        shashin.setLightGallery();
        gallery = shashin.getLightGallery();
        assert.isNull(gallery);

        shashin.setLightGalleryElement('someElement');
        shashin.setLightGallery({"selector":".mediaLink"});
        gallery = shashin.getLightGallery();
        assert.isNotNull(gallery);
        assert.isFunction(gallery.refresh);
    });
});
