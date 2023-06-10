const {assert,expect} = require("chai")
require('../helper.js')

const shashin = require('../../../main/resources/static/js/site/app')
global.Castjs = require('../../../main/resources/static/js/cast.min')
global.lightGallery = require('../../../main/resources/static/js/lightgallery.min')
global.lgZoom = require('../../../main/resources/static/js/lg-zoom.min')
global.lgVideo = require('../../../main/resources/static/js/lg-video.min')
global.lgCastMedia = require('../../../main/resources/static/js/lg-cast-media')
global.lgRelativeCaption = require('../../../main/resources/static/js/lg-relative-caption.min')
global.lgFullscreen = require('../../../main/resources/static/js/lg-fullscreen.min')
global.lgRotate = require('../../../main/resources/static/js/lg-rotate.min')
global.Worker = require('../../../main/resources/static/js/subworkers')
global.ol = require('../../../main/resources/static/js/ol.min')
global.Util = require('../../../main/resources/static/js/site/util')

describe('#shashin app tests', function() {
    it('enable debug console output', function() {
        //console.log(shashin)
        shashin.enableDebug()
        assert.isTrue(shashin.showDebug)
    })

    it('disable debug console output', function() {
        shashin.disableDebug()
        assert.isFalse(shashin.showDebug)
    })

    it('MetadataIdList tests', function() {
        $("body").append($("<input/>", {
            type: 'hidden',
            id: 'multiSelectMetadataIds',
            name: 'multiSelectMetadataIds',
            value: '[]'
        }))

        shashin.addToMetadataIdList("first_metadata")
        shashin.addToMetadataIdList("second_metadata")
        shashin.addToMetadataIdList("third_metadata")
        let metadataList = shashin.getMetdataIdList()
        assert.isArray(metadataList)
        assert.lengthOf(metadataList, 3)
        expect(metadataList).to.eql(["first_metadata", "second_metadata", "third_metadata"])
        shashin.removeFromMetadataIdList("second_metadata")
        metadataList = shashin.getMetdataIdList()
        assert.lengthOf(metadataList, 2)
        expect(metadataList).to.eql(["first_metadata", "third_metadata"])
        shashin.removeAllMetadataIdList()
        metadataList = shashin.getMetdataIdList()
        assert.lengthOf(metadataList, 0)
        expect(metadataList).to.eql([])
    })


    it('Map sources', function() {
        let source = shashin.getMapSource()
        expect(source.urls.join('|')).to.include('openstreetmap')

        source = shashin.getMapSource("invalidSourceAndDefaultingToOSM")
        expect(source.urls.join('|')).to.include('openstreetmap')

        source = shashin.getMapSource("maptiler")
        expect(source.urls.join('|')).to.include('maptiler')
    })

    it('gallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelelement'
        }))

        shashin.setLightGalleryElement('someelelement')
        assert.equal(shashin.getLightGalleryElement().id,'someelelement')

        shashin.setLightGalleryElement('asdf')
        assert.isNull(shashin.getLightGalleryElement())
    })

    it('lightgallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelement'
        }))

        shashin.setLightGalleryElement('someelement')
        shashin.setLightGallery()
        let lightGallery = shashin.getLightGallery()
        assert.equal(lightGallery.settings.licenseKey,'A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3')

        shashin.setLightGalleryElement('asdf')
        shashin.setLightGallery()
        lightGallery = shashin.getLightGallery()
        assert.isFalse(lightGallery.hasOwnProperty("settings"))

        shashin.setLightGalleryElement('someelement')
        shashin.setLightGallery({"selector":".mediaLink"})
        lightGallery = shashin.getLightGallery()
        assert.equal(lightGallery.settings.selector,".mediaLink")
    })
})
