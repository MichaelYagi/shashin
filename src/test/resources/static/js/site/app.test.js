const {assert} = require("chai");
const { expect } = require('chai')
require('../helper.js')

const shashin = require('../../../../../main/resources/static/js/site/app');
global.lightGallery = require('../../../../../main/resources/static/js/lightgallery.min');
global.lgZoom = require('../../../../../main/resources/static/js/lg-zoom.min');
global.lgVideo = require('../../../../../main/resources/static/js/lg-video.min');
global.lgRelativeCaption = require('../../../../../main/resources/static/js/lg-relative-caption.min');
global.lgFullscreen = require('../../../../../main/resources/static/js/lg-fullscreen.min');
global.ol = require('../../../../../main/resources/static/js/ol.min');

describe('#shashin app tests', function() {
    it('enable debug console output', function() {
        //console.log(shashin)
        shashin.enableDebug();
        assert.isTrue(shashin.showDebug);
    })

    it('disable debug console output', function() {
        shashin.disableDebug();
        assert.isFalse(shashin.showDebug);
    })

    it('seriarlize form', function() {
        $("body").append($("<form/>", {
                action: '#',
                method: '#',
                id: 'form1'
            }).append(
                $("<input/>", {
                    type: 'text',
                    id: 'vname',
                    name: 'name',
                    value: 'somename'
                }), // Creating Input Element With Attribute.
                $("<input/>", {
                    type: 'text',
                    id: 'vemail',
                    name: 'email',
                    value: 'someemail'
                }), $("<input/>", {
                    type: 'submit',
                    id: 'submit',
                    value: 'Submit'
                })
            )
        )

        const serialized = $('#form1').serializeObject();
        assert.equal(serialized.name,'somename');
        assert.equal(serialized.email,'someemail');
    })

    it('MetadataIdList tests', function() {
        $("body").append($("<input/>", {
            type: 'hidden',
            id: 'multiSelectMetadataIds',
            name: 'multiSelectMetadataIds',
            value: '[]'
        }))

        shashin.addToMetadataIdList("first_metadata");
        shashin.addToMetadataIdList("second_metadata");
        shashin.addToMetadataIdList("third_metadata");
        let metadataList = shashin.getMetdataIdList();
        assert.isArray(metadataList);
        assert.lengthOf(metadataList, 3);
        expect(metadataList).to.eql(["first_metadata", "second_metadata", "third_metadata"]);
        shashin.removeFromMetadataIdList("second_metadata");
        metadataList = shashin.getMetdataIdList();
        assert.lengthOf(metadataList, 2);
        expect(metadataList).to.eql(["first_metadata", "third_metadata"]);
        shashin.removeAllMetadataIdList();
        metadataList = shashin.getMetdataIdList();
        assert.lengthOf(metadataList, 0);
        expect(metadataList).to.eql([]);
    })

    it('URL query parameters', function() {
        const url = "http://localhost/asdf?qp1=test1&qp2=test2&qp3=test3";
        assert.equal(shashin.getParameterByName("qp2", url),"test2");
        assert.equal(shashin.getParameterByName("qp4", url),null);
    })

    it('Map sources', function() {
        let source = shashin.getMapSource();
        expect(source.urls.join('|')).to.include('openstreetmap')

        source = shashin.getMapSource("invalidSourceAndDefaultingToOSM");
        expect(source.urls.join('|')).to.include('openstreetmap')

        source = shashin.getMapSource("maptiler");
        expect(source.urls.join('|')).to.include('maptiler')
    })

    it('Date formatter', function() {
        assert.equal(shashin.getDateString(2021,10,17),"Sun, Oct 17, 2021");
        assert.equal(shashin.getDateString(2021,9,2),"Thu, Sep 2, 2021");
        assert.equal(shashin.getDateString(2021,2,9),"Tue, Feb 9, 2021");
        assert.equal(shashin.getDateString(2021,14,32),"");
        assert.equal(shashin.getDateString(2021,14,9),"");
        assert.equal(shashin.getDateString("asdf","asdf","asdf"),"");
    })

    it('Numeric check', function() {
        assert.equal(shashin.isNumeric(),false)
        assert.equal(shashin.isNumeric("a"),false)
        assert.equal(shashin.isNumeric("5"),true)
        assert.equal(shashin.isNumeric("5.5"),true)
        assert.equal(shashin.isNumeric("0.5"),true)
        assert.equal(shashin.isNumeric("1e5"),true)
        assert.equal(shashin.isNumeric(5),false)
        assert.equal(shashin.isNumeric(5.5),false)
        assert.equal(shashin.isNumeric(0.5),false)
    })

    it('Encode/Decode HTML string', function() {
        const encodedString = shashin.encodeHtml('{"a":"b"}');
        assert.equal(encodedString,"{&quot;a&quot;:&quot;b&quot;}")
        const decodedString = shashin.decodeHtml(encodedString);
        assert.equal(decodedString,'{"a":"b"}')
    })

    it('Img error', function() {
        $("body").append($("<img/>", {
            id: 'someid',
            src: 'http://asdfasdfasdf.com/'
        }))

        let imgEl = document.getElementById("someid");

        assert.equal(imgEl.src,'http://asdfasdfasdf.com/')

        shashin.errorImg(imgEl,'Some Title',199)
        assert.equal(imgEl.src,'https://via.placeholder.com/199?text=Some%20Title')

        shashin.errorImg(imgEl,'Some Title')
        assert.equal(imgEl.src,'https://via.placeholder.com/209?text=Some%20Title')

        $("#someid").attr("height","100");
        $("#someid").attr("width","200");

        imgEl = document.getElementById("someid");
        shashin.errorImg(imgEl,'Some Title')
        assert.equal(imgEl.src,'https://via.placeholder.com/200x100?text=Some%20Title')
    })

    it('Overlay tests', function() {
        const overlayId = "someId";

        $("body").append($("<div/>", {
            id: 'diva'
        }))

        $("body").append($("<div/>", {
            id: 'divb'
        }))

        $("#diva").append($("<div/>", {
            class: 'thumbnail-tl',
            id: 'tntl'+overlayId
        }).append(
            $("<a/>", {
                href: '#',
                id: 'select'+overlayId
            }).append(
                $("<span/>", {
                    class: 'bi-circle',
                    id: 'tlicon'+overlayId,
                    style: 'font-size: 1rem;color: lightgray;'
                })
            )
        ))

        let tntlElA = $("#diva div");

        const topLeftOverlayHtml = shashin.getTopLeftOverlay(overlayId)
        $("#divb").append(topLeftOverlayHtml)
        let tntlElB = $("#divb div");

        assert.equal(tntlElA.attr("id"),tntlElB.attr("id"))
        assert.equal(tntlElA.attr("class"),tntlElB.attr("class"))
        assert.equal(tntlElA.children("a").attr("id"),tntlElB.children("a").attr("id"))
        assert.equal(tntlElA.children("a").attr("href"),tntlElB.children("a").attr("href"))
        assert.equal(tntlElA.children("a").children("span").attr("id"),tntlElB.children("a").children("span").attr("id"))
        assert.equal(tntlElA.children("a").children("span").attr("class"),tntlElB.children("a").children("span").attr("class"))
        assert.equal(tntlElA.children("a").children("span").attr("style"),tntlElB.children("a").children("span").attr("style"))

    })

    it('gallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelelement'
        }))

        shashin.setLightGalleryElement('someelelement')
        assert.equal(shashin.getLightGalleryElement().id,'someelelement');

        shashin.setLightGalleryElement('asdf')
        assert.isNull(shashin.getLightGalleryElement());
    })

    it('lightgallery element', function () {
        $("body").append($("<div/>", {
            id: 'someelement'
        }))

        shashin.setLightGalleryElement('someelement');
        shashin.setLightGallery();
        let lightGallery = shashin.getLightGallery();
        assert.equal(lightGallery.settings.licenseKey,'A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3');

        shashin.setLightGalleryElement('asdf');
        shashin.setLightGallery();
        lightGallery = shashin.getLightGallery();
        assert.isFalse(lightGallery.hasOwnProperty("settings"))

        shashin.setLightGalleryElement('someelement');
        shashin.setLightGallery({"selector":".mediaLink"});
        lightGallery = shashin.getLightGallery();
        assert.equal(lightGallery.settings.selector,".mediaLink")
    })
})
