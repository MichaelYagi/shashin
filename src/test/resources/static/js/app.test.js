const JSDOM = require('jsdom').JSDOM;
const {assert} = require("chai");
const { expect } = require('chai')
const cfg       = { url: "http://localhost" };
const dom       = new JSDOM( "", cfg );
global.window   = dom.window;
global.document = dom.window.document;
global.$ = global.jQuery = require('../../../../main/resources/static/js/jquery-3.5.1.min');
Object.keys( global.window ).forEach(( property ) => {
    if ( typeof global[ property ] === "undefined" ) {
        global[ property ] = global.window[ property ];
    }
});

const shashin = require('../../../../main/resources/static/js/app');

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
    })

    it('URL query parameters', function() {
        const url = "http://localhost/asdf?qp1=test1&qp2=test2&qp3=test3";
        assert.equal(shashin.getParameterByName("qp2", url),"test2");
        assert.equal(shashin.getParameterByName("qp4", url),null);
    })
})
