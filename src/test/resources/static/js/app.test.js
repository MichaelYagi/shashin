const JSDOM = require('jsdom').JSDOM;
const {expect} = require("chai");

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
        expect(shashin.showDebug).to.equal(true);
    })

    it('disable debug console output', function() {
        //console.log(shashin)
        shashin.disableDebug();
        expect(shashin.showDebug).to.equal(false);
    })
})
