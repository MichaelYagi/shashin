(function( timelineSettings, $, undefined ) {
    timelineSettings.ScrollDirection = Object.freeze({"up":1, "down":0});
    timelineSettings.enableScrollSpy = true;
    timelineSettings.isScrolling = true;
    timelineSettings.prevAnchor = "";
    timelineSettings.successBelowMsg = "success_below";
    timelineSettings.successAboveMsg = "success_above";
    timelineSettings.successMidMsg = "success_mid";
    timelineSettings.success = "success";
    timelineSettings.currentScrollTop = 0;
    timelineSettings.currentScrollDirection = timelineSettings.ScrollDirection.down;
    timelineSettings.initialized = false;
    timelineSettings.timelineDates = [];
    timelineSettings.timelineDatesHash = {};
    timelineSettings.distanceToFooter = 9999;
    timelineSettings.metadataYearMonthCount = [];
    timelineSettings.thumbnailsPerRow = 4;
    timelineSettings.heightArray = [];
    timelineSettings.elementTracking = [];
    timelineSettings.heightCounter = 0;
    timelineSettings.scrollBarIsSliding = false;
    timelineSettings.scrollBar = {};
    timelineSettings.scrollBar.fadeInTime = 100;
    timelineSettings.scrollBar.fadeOutTime = 100;
    timelineSettings.didJumpFromTimelineToc = false;
    timelineSettings.thumbnailType = "225";
    timelineSettings.thumbnailHeight = "225";
    timelineSettings.favoritesMap = null;
    timelineSettings.placeNameHeaders = null;
    timelineSettings.db = null;
    timelineSettings.locale = "en";
    timelineSettings.dbOperationComplete = false;
    timelineSettings.lastDateMarker = null;
    timelineSettings.slidePercent = 0;
    if (Util.isMobile()) {
        timelineSettings.thumbnailType = "centered";
        timelineSettings.thumbnailHeight = "120";
    }
    timelineSettings.useDexie = false;
}( window.timelineSettings = window.timelineSettings || {}, jQuery ));

if (typeof module !== 'undefined') {
    module.exports = window.timelineSettings;
}