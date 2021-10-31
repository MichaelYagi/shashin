const {assert} = require("chai")
require('../helper.js')

const timelineSettings = require('../../../../../main/resources/static/js/site/timeline');
global.shashin = require('../../../../../main/resources/static/js/site/app');

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