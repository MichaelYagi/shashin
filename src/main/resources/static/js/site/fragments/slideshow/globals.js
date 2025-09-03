const slideshow = {};
slideshow.slideshowIntervalId = null;
slideshow.slideshowStarted = false;
slideshow.slideshowIsPaused = false;
slideshow.slideshowCurrentIndex = 0;
slideshow.slideshowMetadataIds = [];
slideshow.slideshowMouseTimer = null;
slideshow.closeTimer = null;
slideshow.nextTimer = null;
slideshow.prevTimer = null;
slideshow.infoTimer = null;
slideshow.shortcutTimer = null;
slideshow.screenTimer = null;
slideshow.downloadTimer = null;
slideshow.slideshowProceed = true;
slideshow.cjsc = null;
slideshow.currentPhotoUrl = null;
slideshow.currentMetadata = null;
slideshow.firstTime = true;
slideshow.isFileDialogOpened = false;
slideshow.slideTimer = null;
slideshow.elapsedBeforePause = 0;
slideshow.isActive = false;
slideshow.globalActive = false;
slideshow.startTime = Date.now();
slideshow.currentTime = Date.now();
slideshow.const = {};
slideshow.const.min = 10; // min seconds
slideshow.const.max = 120; // max seconds
slideshow.const.segments = 6; // # of segments
slideshow.const.spacing = (slideshow.const.max-slideshow.const.min)/(slideshow.const.segments-1);
slideshow.const.hideTime = 5000;
slideshow.const.playPauseHideTime = 3000;
slideshow.const.fadeOutTime = 1000;
slideshow.const.pollTimeout = 100;
slideshow.preloadedImage = null;
slideshow.const.orientationMap = {
    0: shashin.getTranslatedValue("main.pages.slideshow.all"),
    1: shashin.getTranslatedValue("main.pages.slideshow.landscape"),
    2: shashin.getTranslatedValue("main.pages.slideshow.portrait")
};