const {assert} = require("chai")
const { expect } = require('chai')
require('../helper.js')

global.shashin = require('../../../../../main/resources/static/js/site/app');
global.lightGallery = require('../../../../../main/resources/static/js/lightgallery.min');
global.lgZoom = require('../../../../../main/resources/static/js/lg-zoom.min');
global.lgVideo = require('../../../../../main/resources/static/js/lg-video.min');
global.lgRelativeCaption = require('../../../../../main/resources/static/js/lg-relative-caption.min');
global.lgFullscreen = require('../../../../../main/resources/static/js/lg-fullscreen.min');

const timelineSettings = require('../../../../../main/resources/static/js/site/timeline');

describe('#shashin timeline tests', function() {
    it('gallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelelement'
        }))

        timelineSettings.setLightGalleryElement('someelelement')
        assert.equal(timelineSettings.getLightGalleryElement().id,'someelelement');

        timelineSettings.setLightGalleryElement('asdf')
        assert.isNull(timelineSettings.getLightGalleryElement());
    })

    it('lightgallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelement'
        }))

        timelineSettings.setLightGalleryElement('someelement');
        timelineSettings.setLightGallery();
        let lightGallery = timelineSettings.getLightGallery();
        assert.equal(lightGallery.settings.licenseKey,'A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3');


        timelineSettings.setLightGalleryElement('asdf');
        timelineSettings.setLightGallery();
        lightGallery = timelineSettings.getLightGallery();
        assert.isFalse(lightGallery.hasOwnProperty("settings"))
    })

    it('lightgallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelement'
        }))
        assert.isTrue(timelineSettings.validateMetadataInputs("1", "1", "2021", "00:00:00", "-07:00", "123.1234,-123.1234", "someelement"));
        assert.isFalse(timelineSettings.validateMetadataInputs("1", "1", "2021", "00:00:0", "-07:00", "123.1234,-123.1234", "someelement"));
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Time</div>");
        assert.isFalse(timelineSettings.validateMetadataInputs("1", "13", "2021", "00:00:00", "-07:00", "123.1234,-123.1234", "someelement"));
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Month</div>");
        assert.isFalse(timelineSettings.validateMetadataInputs("1", "12", "2021", "00:00:00", "-99:00", "123.1234,-123.1234", "someelement"));
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Offset</div>");
        assert.isFalse(timelineSettings.validateMetadataInputs("1", "1", "2021", "00:00:00", "-07:00", "1231234,-abc.1234", "someelement"));
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Latitude/Longitude</div>");
    })
})