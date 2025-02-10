const puppeteer = require('puppeteer');
const { expect, assert } = require('chai');
const _ = require('lodash');
const globalVariables = _.pick(global, ['browser', 'expect']);
const opts = {
    headless: true,
    timeout: 10000
};

describe('#shashin toast tests', function() {
    let page;

    before (async function () {
        global.expect = expect;
        global.browser = await puppeteer.launch(opts);

        page = await browser.newPage();
        await page.goto('http://127.0.0.1:8080/test.html');
    });

    after (async function () {
        browser.close();

        global.browser = globalVariables.browser;
        global.expect = globalVariables.expect;

        // await page.close();
    });

    it('should have the correct page title', async function () {
        expect(await page.title()).to.eql('Test Page Title');
    });
});