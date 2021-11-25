require("../helper.js")

const sum = require('../../../../../main/resources/static/js/site/test')
const expect = require('chai').expect

describe('#sum()', function() {

    context('without arguments', function() {
        it('should return 3', function() {
            expect(sum(1, 2)).to.equal(3)
        })
    })
})