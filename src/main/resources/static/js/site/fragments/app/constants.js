(function( shashin, $, undefined ) {
    shashin.openedInfoFromLG = false;
    shashin.showDebug = false;
    shashin.showTrace = false;
    shashin.writeLog = false;
    shashin.consoleFilterTypes = [];
    shashin.consoleTags = ["all"];
    shashin.map = null;
    shashin.layer = null;
    shashin.feature = null;
    shashin.infiniteScrollGallery = null;
    shashin.lg = null;
    shashin.ajaxRetries = 3;
    shashin.darkMode = false;
    shashin.showPlacename = false;
    shashin.autoplayVideo = false;
    shashin.lgSubHtmlTimeout = null;
    shashin.nonce = "";
    shashin.contextMenu = null;
    shashin.tempVector = null;
    shashin.downloadInstance = null;
    shashin.initialMapZoom = 17;
    shashin.toast = {};
    shashin.toast.placement = {};
    shashin.toast.placement.top = {};
    shashin.toast.placement.middle = {};
    shashin.toast.placement.bottom = {};
    shashin.toast.placement.default = "bottomCenter";
    shashin.toast.placement.top.left = "topLeft";
    shashin.toast.placement.top.center = "topCenter";
    shashin.toast.placement.top.right = "topRight";
    shashin.toast.placement.middle.left = "midLeft";
    shashin.toast.placement.middle.center = "midCenter";
    shashin.toast.placement.middle.right = "midRight";
    shashin.toast.placement.bottom.left = "bottomLeft";
    shashin.toast.placement.bottom.center = "bottomCenter";
    shashin.toast.placement.bottom.right = "bottomRight";
    shashin.apiResponse = {};
    shashin.apiResponse.SUCCESS = "success";
    shashin.apiResponse.WARN = "warn";
    shashin.apiResponse.FAIL = "fail";
    shashin.objectName = "shashinobject";
    shashin.slideshowXDown = null;
    shashin.slideshowYDown = null;
    shashin.touchTimer = undefined;
    shashin.lastSelectedMetadataId = "";
    shashin.lastSelectedMetadataSelected = false;
    shashin.multiSelected = false;
    shashin.mediaTypeFilter = 'all';
    shashin.consoleTypes = Object.freeze({
        error: 0,
        info: 1,
        log: 2,
        warn: 3
    });
    shashin.uglified = false;
}( window.shashin = window.shashin || {}, jQuery ));