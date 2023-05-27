// inViewport 1.0.0
(function($){
  $.fn.inViewport = function(offsetVh){
    var elOffsetVh = ~~Math.round(parseFloat(offsetVh)) || 0;
    var elT = $(this).offset().top;
    var elB = elT + $(this).outerHeight();
    var viewT = $(window).scrollTop();
    var viewB = viewT + ($(window).height() + elOffsetVh);
    return elB > viewT && elT < viewB;
  };
}(jQuery));