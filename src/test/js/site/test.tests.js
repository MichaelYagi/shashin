require("../helper.js");

const test = require('../../../main/resources/static/js/site/test');
const expect = require('chai').expect;

describe('#sum()', function() {
    context('without arguments', function() {
        it('should return 3', function() {
            //expect(test(1, 2)).to.equal(3)
            expect(test.AssertionError).to.equal(3);
        });
    });
});