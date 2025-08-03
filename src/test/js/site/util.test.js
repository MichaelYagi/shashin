const {assert,expect} = require("chai");
require('../helper.js');

const Util = require('../../../main/resources/static/js/site/util');

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
        );

        const serialized = Util.serializeObject($('#form1'));
        assert.equal(serialized.name,'somename');
        assert.equal(serialized.email,'someemail');
    });

    it('URL query parameters test', function() {
        const url = "http://localhost/asdf?qp1=test1&qp2=test2&qp3=test3";
        assert.equal(Util.getParameterByName("qp2", url),"test2");
        assert.equal(Util.getParameterByName("qp4", url),null);
    });

    it('Date formatter test', function() {
        assert.equal(Util.getDateString(2021,10,17),"Sun, Oct 17, 2021");
        assert.equal(Util.getDateString(2021,9,2),"Thu, Sep 2, 2021");
        assert.equal(Util.getDateString(2021,2,9),"Tue, Feb 9, 2021");
        assert.equal(Util.getDateString(2021,14,32),"");
        assert.equal(Util.getDateString(2021,14,9),"");
        assert.equal(Util.getDateString("asdf","asdf","asdf"),"");
        // Should match with TextUtilsTest.kt: formatToLongDateTest() tests - tested in UITests: equalDateTranslations
        assert.equal(Util.getDateString(2021,10,17, "pt"),"dom, out 17, 2021");
        assert.equal(Util.getDateString(2021,10,17, "pt", false),"out 17, 2021");
        assert.equal(Util.getDateString(2021,10,17, "fr", true),"dim, oct 17, 2021");
        assert.equal(Util.getDateString(2021,10,17, "fr", false),"oct 17, 2021");
        assert.equal(Util.getDateString(2021,10,17, "ja", false),"2021年10月17日");
        assert.equal(Util.getDateString(2021,10,17, "ja", true),"2021年10月17日(日)");
        assert.equal(Util.getDateString(2021,10,17, "es", true),"dom, oct 17, 2021");
    });

    it('Date formatter test', function() {
        const getDate = () => new Date(new Date().toLocaleString("en-US", { timeZone: "UTC" }));

        const formatDate = (date) => {
            return date.toISOString().split('.')[0].replace('T', ' ');
        };

        const getAdjustedDate = (now, { years = 0, months = 0, hours = 0, minutes = 0, days = 0 }) => {
            const adjusted = new Date(now);
            adjusted.setHours(adjusted.getHours() - hours);
            adjusted.setMinutes(adjusted.getMinutes() - minutes);
            adjusted.setDate(adjusted.getDate() - days);
            adjusted.setMonth(adjusted.getMonth() - months);
            adjusted.setFullYear(adjusted.getFullYear() - years);
            return formatDate(adjusted);
        };

        const now = getDate(); // single reference point
        const offsetHours = 0;

        // 4 minutes ago
        let date = getAdjustedDate(now, { hours: offsetHours, minutes: 4 });
        assert.equal(Util.getMessageSubText(date, "UTC", "ja"), "<small class='text-muted'>4 分前</small>");

        // 5 hours ago
        date = getAdjustedDate(now, { hours: offsetHours + 5 });
        assert.equal(Util.getMessageSubText(date, "UTC", "en"), "<small class='text-muted'>5 hours ago</small>");

        // 8 days ago
        date = getAdjustedDate(now, { hours: offsetHours, days: 8 });
        assert.equal(Util.getMessageSubText(date, "UTC", "en"), "<small class='text-muted'>8 days ago</small>");

        // 1 month and 2 days ago
        date = getAdjustedDate(now, { hours: offsetHours, months: 1, days: 2 });
        assert.equal(Util.getMessageSubText(date, "UTC", "en"), "<small class='text-muted'>last month</small>");

        // 1 month and 2 days ago
        date = getAdjustedDate(now, { hours: offsetHours, months: 1, days: 2 });
        assert.equal(Util.getMessageSubText(date, "UTC", "pt"), "<small class='text-muted'>mês passado</small>");

        // 1 year and 11 months ago
        date = getAdjustedDate(now, { hours: offsetHours, months: 11, years: 1 });
        assert.equal(Util.getMessageSubText(date, "UTC", "en"), "<small class='text-muted'>last year</small>");

        // 3 years
        date = getAdjustedDate(now, { hours: offsetHours, months: 12, years: 2 });
        assert.equal(Util.getMessageSubText(date, "UTC", "en"), "<small class='text-muted'>3 years ago</small>");

        date = getAdjustedDate(now, { hours: 0 });
        assert.equal(Util.getMessageSubText(date, "UTC", "en"), "<small class='text-muted'>now</small>");

        date = getAdjustedDate(now, { hours: -2 });
        assert.equal(Util.getMessageSubText(date, "UTC", "en"), "<small class='text-muted'>now</small>");
    });

    it('Numeric string test', function() {
        assert.equal(Util.isNumericString(),false);
        assert.equal(Util.isNumericString("a"),false);
        assert.equal(Util.isNumericString("5"),true);
        assert.equal(Util.isNumericString("5.5"),true);
        assert.equal(Util.isNumericString("0.5"),true);
        assert.equal(Util.isNumericString("1e5"),true);
        assert.equal(Util.isNumericString(5),false);
        assert.equal(Util.isNumericString(5.5),false);
        assert.equal(Util.isNumericString(0.5),false);
    });

    it('Encode/Decode HTML string test', function() {
        const encodedString = Util.encodeHtml('{"a":"b"}');
        assert.equal(encodedString,"{&quot;a&quot;:&quot;b&quot;}");
        const decodedString = Util.decodeHtml(encodedString);
        assert.equal(decodedString,'{"a":"b"}');
    });

    it('Validate metadata inputs test', function () {
        assert.isTrue(Util.validateMetadataInputs("1", "1", "2021", "00:00:00", "-07:00", "123.1234,-123.1234", "0:00", true));
        assert.isFalse(Util.validateMetadataInputs("1", "1", "2021", "00:00:0", "-07:00", "123.1234,-123.1234", "0:00", true));
        assert.isFalse(Util.validateMetadataInputs("1", "13", "2021", "00:00:00", "-07:00", "123.1234,-123.1234", "0:00", true));
        assert.isFalse(Util.validateMetadataInputs("1", "12", "2021", "00:00:00", "-99:00", "123.1234,-123.1234", "0:00", true));
        assert.isFalse(Util.validateMetadataInputs("1", "1", "2021", "00:00:00", "-07:00", "1231234,-abc.1234", "0:00", true));
        assert.isFalse(Util.validateMetadataInputs("1", "1", "2021", "00:00:00", "-07:00", "123.1234,-123.1234", "asdf", true));
    });

    it('Gallery removal test', function () {
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
            }),
            $("<div/>", {
                id: 'container_metadataelement',
                height: 23
            })
        );

        assert.equal(Util.getDateGalleryHeight("metadataelement"),23);
        Util.removeDateGallery("metadataelement");
        assert.equal(Util.getDateGalleryHeight("metadataelement"),0);
    });

    it('Date object from string test', function () {
        let dateOneString = "2021-11-25";
        let dateTwoString = "2021-11-24";

        let dateOneObj = Util.getDateObject(dateOneString);
        let dateTwoObj = Util.getDateObject(dateTwoString);
        expect(dateOneObj).to.be.gt(dateTwoObj);

        dateOneString = "tail_2021-11-25";
        dateTwoString = "tail_2021-11-24";

        dateOneObj = Util.getDateObject(dateOneString);
        dateTwoObj = Util.getDateObject(dateTwoString);
        expect(dateOneObj).to.be.gt(dateTwoObj);

        dateOneString = "asdf-qw-df";
        dateOneObj = Util.getDateObject(dateOneString);
        assert.equal(dateOneObj.toString(),"Invalid Date");
    });

    it('Get date string from year month day test', function () {
        let dateString = Util.getDateString("2021","11","25");
        assert.equal(dateString,"Thu, Nov 25, 2021");

        dateString = Util.getDateString("asdf","11","25");
        expect(dateString).to.be.empty;
    });

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
        );

        Util.populateDetailsInfo({});
        expect($(".pathDetails").text()).to.be.empty;

        Util.populateDetailsInfo({path:"test"});
        assert.equal($(".pathDetails").text(),"test");

        Util.populateDetailsInfo({year:2021,month:11,day:5,time:"00:00:00"});
        assert.equal($(".manualTakenAtDetails").text(),"2021-11-05 00:00:00");
    });

    it('Date index tests', function() {
        let day = Util.getShortDay(1);
        assert.equal(day, 'Mon');

        let month = Util.getShortMonths(1);
        assert.equal(month, 'Feb');
    });

    it('Format string date tests', function() {
        let date = Util.formatDate("2023-12-09");
        assert.equal(date, "2023-12-09");

        date = Util.formatDate("2023-12-9");
        assert.equal(date, "2023-12-09");

        date = Util.formatDate("2023-9-8");
        assert.equal(date, "2023-09-08");

        date = Util.formatDate(1);
        assert.equal(date, null);
    });

    it('Valid date test', function() {
        let dateCheck = Util.isValidDate("2023-12-09");
        assert.equal(dateCheck, true);

        dateCheck = Util.isValidDate("2023-12-9");
        assert.equal(dateCheck, false);

        dateCheck = Util.isValidDate("12-2023-09");
        assert.equal(dateCheck, false);
    });

    it('Format date object test', function() {
        let dateFormat = Util.formatDateTime(new Date("12/09/2023"));
        assert.equal(dateFormat.getFullYear(), 2023);
        // 0 based month for some reason
        assert.equal(dateFormat.getMonth(), 11);
        assert.equal(dateFormat.getDate(), 9);

        dateFormat = Util.formatDateTime(new Date("2023-12-09"));
        assert.equal(dateFormat.getFullYear(), 2023);
        assert.equal(dateFormat.getMonth(), 11);
        assert.equal(dateFormat.getDate(), 9);

        dateFormat = Util.formatDateTime(1);
        assert.equal(dateFormat, null);

        dateFormat = Util.formatDateTime(new Date(1));
        assert.equal(dateFormat.getFullYear(), 1970);
        assert.equal(dateFormat.getMonth(), 0);
        assert.equal(dateFormat.getDate(), 1);
    });

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
    });

    it('MS to string tests', function() {
        let msToString = Util.convertMSToRelativeTime(1000);
        assert.equal(msToString, "00:00:01");

        msToString = Util.convertMSToRelativeTime(60*1000);
        assert.equal(msToString, "00:01:00");

        msToString = Util.convertMSToRelativeTime(60*60*1000);
        assert.equal(msToString, "01:00:00");

        msToString = Util.convertMSToRelativeTime(35*57*25*1000);
        assert.equal(msToString, "13:51:15");

        msToString = Util.convertMSToRelativeTime(86400000);
        assert.equal(msToString, "1 day 00:00:00");

        msToString = Util.convertMSToRelativeTime(172800000);
        assert.equal(msToString, "2 days 00:00:00");

        msToString = Util.convertMSToRelativeTime(2992500000);
        assert.equal(msToString, "1 month 3 days 15:15:00");

        msToString = Util.convertMSToRelativeTime(59850000000);
        assert.equal(msToString, "1 year 10 months 23 days 17:00:00");

        msToString = Util.convertMSToRelativeTime(604800000);
        assert.equal(msToString, "7 days 00:00:00");

        msToString = Util.convertMSToRelativeTime(691200000);
        assert.equal(msToString, "8 days 00:00:00");

        msToString = Util.convertMSToRelativeTime(1468423000);
        assert.equal(msToString, "16 days 23:53:43");

        msToString = Util.convertMSToRelativeTime(1814400000);
        assert.equal(msToString, "21 days 00:00:00");

        msToString = Util.convertMSToRelativeTime(109725000000);
        assert.equal(msToString, "3 years 5 months 22 days 23:10:00");

        msToString = Util.convertMSToRelativeTime(63115200000);
        assert.equal(msToString, "2 years 12:00:00");

        msToString = Util.convertMSToRelativeTime(31536000000);
        assert.equal(msToString, "1 year 00:00:00");

        msToString = Util.convertMSToRelativeTime(31449600000);
        assert.equal(msToString, "11 months 30 days 00:00:00");

        msToString = Util.convertMSToRelativeTime(31536000000);
        assert.equal(msToString, "1 year 00:00:00");

        msToString = Util.convertMSToRelativeTime(24445800000);
        assert.equal(msToString, "9 months 9 days 22:30:00");

        msToString = Util.convertMSToRelativeTime(2592000000);
        assert.equal(msToString, "30 days 00:00:00");

        msToString = Util.convertMSToRelativeTime(2678400000);
        assert.equal(msToString, "1 month 00:00:00");

        msToString = Util.convertMSToRelativeTime(598500000000);
        assert.equal(msToString, "18 years 11 months 18 days 02:00:00");
    });

    it('Version checking', function() {
        let currVersion = "v1.0.0";
        let latestVersion = "v1.0.0";
        assert.isFalse(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "0.99.99";
        latestVersion = "v1.0.0";
        assert.isTrue(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "v0.99.99";
        latestVersion = "v1.0.0";
        assert.isTrue(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "v0.1b.0";
        latestVersion = "v1.0.0";
        assert.isFalse(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "v0..0";
        latestVersion = "v1.0.0";
        assert.isFalse(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "v0. .0";
        latestVersion = "v1.0.0";
        assert.isFalse(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "v0. 2.0";
        latestVersion = "v1.0.0";
        assert.isFalse(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "v1.12.7";
        latestVersion = "v3.12.7";
        assert.isTrue(Util.downloadLatestVersion(currVersion,latestVersion));

        currVersion = "v0.2.0.1";
        latestVersion = "v1.0.0";
        assert.isFalse(Util.downloadLatestVersion(currVersion,latestVersion));

        // Edge case for dev environment
        currVersion = "v2.10.0";
        latestVersion = "v2.9.1";
        assert.isFalse(Util.downloadLatestVersion(currVersion,latestVersion));
    });

    it('Get OS tests', function() {
        const windowRef = global.window;

        global.window = {
            navigator: {
                userAgent: undefined,
                platform: undefined,
                userAgentData: {
                    platform: undefined
                }
            }
        };
        let os = Util.getOS();
        assert.equal(os, "");

        global.window = {
            navigator: {
                userAgent: undefined,
                platform: "macppc",
                userAgentData: {
                    platform: undefined
                }
            }
        };
        os = Util.getOS();
        assert.equal(os, "MacOS");

        global.window = {
            navigator: {
                userAgent: undefined,
                platform: undefined,
                userAgentData: {
                    platform: "WiNdOwS"
                }
            }
        };
        os = Util.getOS();
        assert.equal(os, "Windows");

        global.window = {
            navigator: {
                userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36',
                platform: undefined,
                userAgentData: {
                    platform: undefined
                }
            }
        };
        os = Util.getOS();
        assert.equal(os, "");

        global.window = {
            navigator: {
                userAgent: 'Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.6834.164 Mobile Safari/537.36',
                platform: undefined,
                userAgentData: {
                    platform: undefined
                }
            }
        };
        os = Util.getOS();
        assert.equal(os, "Android");

        global.window = windowRef;
    });
});