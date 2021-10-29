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