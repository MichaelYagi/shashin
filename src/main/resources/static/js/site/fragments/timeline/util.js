(function( timelineSettings, $, undefined ) {
    timelineSettings.calculateDistanceToFooter = function() {
        return $(window).height() - $('#subfooter').offset().top;
    };

    timelineSettings.closeToFooter = function() {
        let threshold = -500;
        return (
            timelineSettings.distanceToFooter === 9999 ||
            (timelineSettings.distanceToFooter > threshold && timelineSettings.distanceToFooter < 1) ||
            Util.elementsInViewport($("#subfooter")).length > 0
        );
    };

    timelineSettings.scrollByN = function(scrollBy = 1) {
        document.getElementById("container").scrollBy({top: scrollBy, behavior: "instant"});
        if (document.getElementsByTagName("MAIN").length > 0) {
            document.getElementsByTagName("MAIN")[0].scrollBy({top: scrollBy, behavior: "instant"});
        }
    };
}( window.timelineSettings = window.timelineSettings || {}, jQuery ));