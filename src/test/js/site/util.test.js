const {assert} = require("chai")
const { expect } = require('chai')
require('../helper.js')

const Util = require('../../../main/resources/static/js/site/util')
const shashin = require("../../../main/resources/static/js/site/app");

describe('#Util tests', function() {
    it('seriarlize form test', function() {
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

        const serialized = Util.serializeObject($('#form1'))
        assert.equal(serialized.name,'somename')
        assert.equal(serialized.email,'someemail')
    })

    it('URL query parameters test', function() {
        const url = "http://localhost/asdf?qp1=test1&qp2=test2&qp3=test3"
        assert.equal(Util.getParameterByName("qp2", url),"test2")
        assert.equal(Util.getParameterByName("qp4", url),null)
    })

    it('Date formatter test', function() {
        assert.equal(Util.getDateString(2021,10,17),"Sun, Oct 17, 2021")
        assert.equal(Util.getDateString(2021,9,2),"Thu, Sep 2, 2021")
        assert.equal(Util.getDateString(2021,2,9),"Tue, Feb 9, 2021")
        assert.equal(Util.getDateString(2021,14,32),"")
        assert.equal(Util.getDateString(2021,14,9),"")
        assert.equal(Util.getDateString("asdf","asdf","asdf"),"")
    })

    it('Numeric string test', function() {
        assert.equal(Util.isNumericString(),false)
        assert.equal(Util.isNumericString("a"),false)
        assert.equal(Util.isNumericString("5"),true)
        assert.equal(Util.isNumericString("5.5"),true)
        assert.equal(Util.isNumericString("0.5"),true)
        assert.equal(Util.isNumericString("1e5"),true)
        assert.equal(Util.isNumericString(5),false)
        assert.equal(Util.isNumericString(5.5),false)
        assert.equal(Util.isNumericString(0.5),false)
    })

    it('Encode/Decode HTML string test', function() {
        const encodedString = Util.encodeHtml('{"a":"b"}')
        assert.equal(encodedString,"{&quot;a&quot;:&quot;b&quot;}")
        const decodedString = Util.decodeHtml(encodedString)
        assert.equal(decodedString,'{"a":"b"}')
    })

    it('Validate metadata inputs test', function () {
        $("body").append($("<div/>", {
            id: 'someelement'
        }))
        assert.isTrue(Util.validateMetadataInputs("1", "1", "2021", "00:00:00", "-07:00", "123.1234,-123.1234", "someelement"))
        assert.isFalse(Util.validateMetadataInputs("1", "1", "2021", "00:00:0", "-07:00", "123.1234,-123.1234", "someelement"))
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Time</div>")
        assert.isFalse(Util.validateMetadataInputs("1", "13", "2021", "00:00:00", "-07:00", "123.1234,-123.1234", "someelement"))
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Month</div>")
        assert.isFalse(Util.validateMetadataInputs("1", "12", "2021", "00:00:00", "-99:00", "123.1234,-123.1234", "someelement"))
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Offset</div>")
        assert.isFalse(Util.validateMetadataInputs("1", "1", "2021", "00:00:00", "-07:00", "1231234,-abc.1234", "someelement"))
        assert.equal($("#someelement").html(),"<div class=\"alert alert-danger\" role=\"alert\">Enter Valid Latitude/Longitude</div>")
    })

    it('Validate metadata inputs test', function () {
        $("body").append($("<div/>", {
                id: 'brmetadataelement',
                height: 15
            }),
            $("<div/>", {
                id: 'rowmetadataelement',
                height: 13
            }),
            $("<div/>", {
                id: 'amp_metadataelement',
                height: 17
            }),
            $("<div/>", {
                id: 'metadataelement',
                height: 3
            }),
            $("<div/>", {
                id: 'tail_metadataelement',
                height: 9
            })
        )

        assert.equal(Util.getDateGalleryHeight("metadataelement"),31)
        Util.removeDateGallery("metadataelement")
        assert.equal(Util.getDateGalleryHeight("metadataelement"),0)
    })

    it('Date object from string test', function () {
        let dateOneString = "2021-11-25"
        let dateTwoString = "2021-11-24"

        let dateOneObj = Util.getDateObject(dateOneString)
        let dateTwoObj = Util.getDateObject(dateTwoString)
        expect(dateOneObj).to.be.gt(dateTwoObj)

        dateOneString = "tail_2021-11-25"
        dateTwoString = "tail_2021-11-24"

        dateOneObj = Util.getDateObject(dateOneString)
        dateTwoObj = Util.getDateObject(dateTwoString)
        expect(dateOneObj).to.be.gt(dateTwoObj)

        dateOneString = "asdf-qw-df"
        dateOneObj = Util.getDateObject(dateOneString)
        assert.equal(dateOneObj.toString(),"Invalid Date")
    })

    it('Get date string from year month day test', function () {
        let dateString = Util.getDateString("2021","11","25")
        assert.equal(dateString,"Thu, Nov 25, 2021")

        dateString = Util.getDateString("asdf","11","25")
        expect(dateString).to.be.empty
    })

    it('Populate details tab data test', function () {
        $("body").append($("<div/>", {
                class: 'pathDetails'
            }),
            $("<div/>", {
                class: 'typeDetails'
            }),
            $("<div/>", {
                class: 'isoDetails'
            }),
            $("<div/>", {
                class: 'compressionDetails'
            }),
            $("<div/>", {
                class: 'exposureDetails'
            }),
            $("<div/>", {
                class: 'fNumberDetails'
            }),
            $("<div/>", {
                class: 'focalLengthDetails'
            }),
            $("<div/>", {
                class: 'cameraDetails'
            }),
            $("<div/>", {
                class: 'lensDetails'
            }),
            $("<div/>", {
                class: 'qualityDetails'
            }),
            $("<div/>", {
                class: 'addedAtDetails'
            }),
            $("<div/>", {
                class: 'createdAtDetails'
            }),
            $("<div/>", {
                class: 'modifiedAtDetails'
            }),
            $("<div/>", {
                class: 'takenAtDetails'
            }),
            $("<div/>", {
                class: 'manualTakenAtDetails'
            }),
            $("<div/>", {
                class: 'timeZoneDetails'
            }),
            $("<div/>", {
                class: 'resolutionDetails'
            }),
            $("<div/>", {
                class: 'keywordsDetails'
            })
        )

        Util.populateDetailsInfo({})
        expect($(".pathDetails").text()).to.be.empty

        Util.populateDetailsInfo({path:"test"})
        assert.equal($(".pathDetails").text(),"test")

        Util.populateDetailsInfo({year:2021,month:11,day:5,time:"00:00:00"})
        assert.equal($(".manualTakenAtDetails").text(),"2021-11-5 00:00:00")
    })

    it('Date index tests', function() {
        let day = Util.getShortDay(1)
        assert.equal(day, 'Mon')

        let month = Util.getShortMonths(1)
        assert.equal(month, 'Feb')
    })

    it('Cookie tests', function() {
        let aCookie = Util.getCookie("somecookiename");
        assert.equal(aCookie, "");

        Util.setCookie("somecookiename","somecookievalue");
        aCookie = Util.getCookie("somecookiename");
        assert.equal(aCookie, "somecookievalue");

        Util.deleteCookie("somecookiename");
        aCookie = Util.getCookie("somecookiename");
        assert.equal(aCookie, "");

        Util.setCookie("someothercookiename","someothercookievalue", "/", "localhost");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "someothercookievalue");

        Util.deleteCookie("someothercookiename");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "");

        Util.setCookie("someothercookiename","someothercookievalue", "/", "localhost");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "someothercookievalue");

        Util.deleteCookie("someothercookiename", "/unknowncookiepath");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "someothercookievalue"); //Not deleted

        Util.deleteCookie("someothercookiename", "/", "unknowncookiedomain");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "someothercookievalue"); //Not deleted

        Util.deleteCookie("someothercookiename", "/", "localhost");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "");

        Util.setCookie("someothercookiename","someothercookievalue", "/", "localhost");

        Util.deleteCookie("someothercookiename", "/asdf");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "someothercookievalue");

        Util.deleteCookie("someothercookiename", "/");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "");

        Util.setCookie("someothercookiename","someothercookievalue", "/");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "someothercookievalue");

        Util.deleteCookie("someothercookiename", "/");
        aCookie = Util.getCookie("someothercookiename");
        assert.equal(aCookie, "");
    })
})