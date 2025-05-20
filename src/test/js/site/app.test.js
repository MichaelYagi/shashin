const {assert,expect} = require("chai");
const sinon = require("sinon");
require("mocha");
require('../helper.js');

const shashin = require('../../../main/resources/static/js/site/app');
global.Castjs = require('../../../main/resources/static/js/cast.min');
global.lightGallery = require('../../../main/resources/static/js/lightgallery.min');
global.lgZoom = require('../../../main/resources/static/js/lg-zoom.min');
global.lgVideo = require('../../../main/resources/static/js/lg-video.min');
global.lgCastMedia = require('../../../main/resources/static/js/lg-cast-media');
global.lgRelativeCaption = require('../../../main/resources/static/js/lg-relative-caption.min');
global.lgFullscreen = require('../../../main/resources/static/js/lg-fullscreen');
global.lgRotate = require('../../../main/resources/static/js/lg-rotate.min');
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

        assert.isTrue(consoleSpy.calledWith("test message. Tags: test"));
        assert.isTrue(consoleSpy.calledWith("tik. Tags: all"));
        assert.isTrue(consoleSpy.calledWith("tok. Tags: tik"));
        assert.isTrue(consoleSpy.calledWith("fail. Tags: sinon,tik,tok"));
        assert.isFalse(consoleSpy.calledWith("failz. Tag: sinon"));
        assert.isTrue(consoleSpy.calledWith("fail. Tags: all"));

        consoleSpy.restore();

        // assert.isTrue(true);
    });

    it('disable debug console output', function() {
        shashin.disableDebug();
        assert.isFalse(shashin.showDebug);
    });

    it('MetadataIdList tests', function() {
        $("body").append($("<input/>", {
            type: 'hidden',
            id: 'multiSelectMetadataIds',
            name: 'multiSelectMetadataIds',
            value: '[]'
        }));

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

    it('gallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelelement'
        }));

        shashin.setLightGalleryElement('someelelement');
        assert.equal(shashin.getLightGalleryElement().id,'someelelement');

        shashin.setLightGalleryElement('asdf');
        assert.isNull(shashin.getLightGalleryElement());
    });

    it('lightgallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelement'
        }));
        shashin.setLightGalleryElement('someelement');
        shashin.setLightGallery();
        let lightGallery = shashin.getLightGallery();
        assert.equal(lightGallery.settings.licenseKey,'A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3');

        shashin.setLightGalleryElement('asdf');
        shashin.setLightGallery();
        lightGallery = shashin.getLightGallery();
        assert.isFalse(lightGallery.hasOwnProperty("settings"));

        shashin.setLightGalleryElement('someelement');
        shashin.setLightGallery({"selector":".mediaLink"});
        lightGallery = shashin.getLightGallery();
        assert.equal(lightGallery.settings.selector,".mediaLink");
    });
});
