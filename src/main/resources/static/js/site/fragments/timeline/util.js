(function( timelineSettings, $, undefined ) {
    timelineSettings.calculateDistanceToFooter = function() {
        return $(window).height() - $('#subfooter').offset().top;
    };
}( window.timelineSettings = window.timelineSettings || {}, jQuery ));