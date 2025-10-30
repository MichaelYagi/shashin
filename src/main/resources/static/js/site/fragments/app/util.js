(function( shashin, $, undefined ) {
    /* Based on this http://jsfiddle.net/brettwp/J4djY/*/
    shashin.detectDoubleTap = function(doubleTapMs) {
        let timeout, lastTap = 0;
        return function detectDoubleTap(event) {
            const currentTime = new Date().getTime();
            const tapLength = currentTime - lastTap;
            if (0 < tapLength && tapLength < doubleTapMs) {
                event.preventDefault();
                const doubleTap = new CustomEvent("doubletap", {
                    bubbles: true,
                    detail: event
                });
                event.target.dispatchEvent(doubleTap);
            } else {
                timeout = setTimeout(() => clearTimeout(timeout), doubleTapMs);
            }
            lastTap = currentTime;
        };
    };

    // Call in console
    // eg: shashin.enableDebug({tags: all, consoleTypes:[shashin.consoleTypes.log,shashin.consoleTypes.error],showTrace:true,writeToLog:true})
    shashin.enableDebug = function (options) {
        this.showDebug = true;
        shashin.showTrace = false;
        shashin.writeToLog = false;
        shashin.consoleFilterTypes = [];

        let showTrace = false;
        //[shashin.consoleTypes.error, shashin.consoleTypes.info, shashin.consoleTypes.log, shashin.consoleTypes.warn]
        let consoleTypes = [];
        let tags = ["all"];

        if (options === undefined || options === null) {
            showTrace = false;
            consoleTypes = [];
            shashin.writeToLog = false;
            tags = ["all"];
        } else {
            if (options.hasOwnProperty("showTrace")) {
                showTrace = options.showTrace;
            }

            if (options.hasOwnProperty("consoleTypes")) {
                consoleTypes = options.consoleTypes;
            }

            if (options.hasOwnProperty("writeToLog")) {
                shashin.writeToLog = options.writeToLog;
            }

            if (options.hasOwnProperty("tags")) {
                tags = options.tags;
            }

            if (options.hasOwnProperty("tag")) {
                if (options.hasOwnProperty("tags")) {
                    tags.push(options.tag);
                } else {
                    tags = [options.tag];
                }

            }
        }

        shashin.showTrace = showTrace;
        shashin.consoleFilterTypes = consoleTypes;
        shashin.consoleTags = tags;

        if (Util.localStorageAvailable() === true) {
            localStorage.setItem("showDebug", "on");
        }
    };

    // Call in console
    shashin.disableDebug = function () {
        this.showDebug = false;
        shashin.showTrace = false;
        shashin.writeToLog = false;
        shashin.consoleFilterTypes = [];
        shashin.consoleTags = ["all"];

        if (Util.localStorageAvailable() === true && localStorage.getItem("showDebug") !== null) {
            localStorage.removeItem("showDebug");
        }
    };

    shashin.printMessageToConsole = function (msg, options) {
        // error, info, log, warn
        let consoleType = shashin.consoleTypes.log;
        let tags = ["all"];

        if (options === undefined || options === null) {
            consoleType = shashin.consoleTypes.log;
            tags = ["all"];
        } else {
            if (options.hasOwnProperty("consoleType")) {
                consoleType = options.consoleType;
            }

            if (options.hasOwnProperty("tag")) {
                tags = [options.tag];
            }

            if (options.hasOwnProperty("tags")) {
                tags = options.tags;
            }

            if (tags.includes("all")) {
                tags = ["all"];
            }
        }

        let localStorageDebugFlag = false;
        if (Util.localStorageAvailable() === true && localStorage.getItem("showDebug") !== null && localStorage.getItem("showDebug").length > 0) {
            let getFlag = localStorage.getItem("showDebug");
            if (getFlag === "on") {
                localStorageDebugFlag = true;
            }
        }

        if ((tags.some(r=> shashin.consoleTags.includes(r)) === true || ["all"].some(r=> shashin.consoleTags.includes(r)) === true) && (shashin.showDebug === true || localStorageDebugFlag === true)) {
            if (consoleType === shashin.consoleTypes.log && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.log, shashin.consoleFilterTypes) !== -1)) {
                console.log(msg + " - Tags: " + tags.join());
            } else if (consoleType === shashin.consoleTypes.error && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.error, shashin.consoleFilterTypes) !== -1)) {
                console.error(msg + " - Tags: " + tags.join());
            } else if (consoleType === shashin.consoleTypes.info && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.info, shashin.consoleFilterTypes) !== -1)) {
                console.info(msg + " - Tags: " + tags.join());
            } else if (consoleType === shashin.consoleTypes.warn && (shashin.consoleFilterTypes.length === 0 || $.inArray(shashin.consoleTypes.warn, shashin.consoleFilterTypes) !== -1)) {
                console.warn(msg + " - Tags: " + tags.join());
            } else {
                console.log(msg + " - Tags: " + tags.join());
            }

            if (shashin.showTrace === true) {
                console.log(getStackTrace().join('\n'));
            }

            if (shashin.writeToLog === true) {
                let log = "";
                if (msg.length > 0) {
                    if (shashin.consoleFilterTypes.length === 0) {
                        log = "console.log: " + msg;
                    } else if (consoleType === shashin.consoleTypes.log && $.inArray(shashin.consoleTypes.log, shashin.consoleFilterTypes) !== -1) {
                        log = "console.log: " + msg;
                    } else if (consoleType === shashin.consoleTypes.error && $.inArray(shashin.consoleTypes.error, shashin.consoleFilterTypes) !== -1) {
                        log = "console.error: " + msg;
                    } else if (consoleType === shashin.consoleTypes.info && $.inArray(shashin.consoleTypes.info, shashin.consoleFilterTypes) !== -1) {
                        log = "console.info: " + msg;
                    } else if (consoleType === shashin.consoleTypes.warn && $.inArray(shashin.consoleTypes.warn, shashin.consoleFilterTypes) !== -1) {
                        log = "console.warn: " + msg;
                    } else if (consoleType === shashin.consoleTypes.trace && $.inArray(shashin.consoleTypes.trace, shashin.consoleFilterTypes) !== -1) {
                        log = "console.trace: " + msg;
                    }

                    if (log.length > 0) {
                        const http = new Http("log console output");
                        if (shashin.showTrace === true) {
                            log += "\n" + getStackTrace().join('\n');
                        }

                        setTimeout(function () {
                            let json = {consoleType: consoleType, log: log, tag: tags};
                            http.ajax("post", "/console/log", JSON.stringify(json)).then(function (data) {
                                if (data.hasOwnProperty("status") && data.status === "fail" && data.hasOwnProperty("msg")) {
                                    console.error("Could not log console output");
                                }
                            });
                        }, 0);
                    }
                }
            }
        }

        function getStackTrace() {
            let stack;

            try {
                throw new Error('');
            }
            catch (error) {
                stack = error.stack || '';
            }

            stack = stack.split('\n').map(function (line) { return line.trim(); });
            return stack.splice(stack[0] === 'Error' ? 2 : 1);
        }
    };

    shashin.onFail = function(xhr, textStatus, ajaxParams, description, failFunction) {
        $("#spinner").hide();
        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.error.ajax"), "AJAX error"+description+". Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".", {tag:"ajaxError",icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
        shashin.printMessageToConsole("AJAX error"+description+". Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".", {
            consoleType: shashin.consoleTypes.error,
            tag:"http"
        });
        if (xhr.status === 403 || xhr.status === 401) {
            $(location).prop('href', '/users/login');
        } else if ((textStatus === 'timeout' || textStatus === 'error') && ajaxParams.retries-- > 0) {
            $.ajax(ajaxParams).fail(function (xhr, textStatus) {
                shashin.onFail(xhr, textStatus, ajaxParams, description, failFunction);
            });
        } else if (xhr.status !== 200 && ajaxParams.retries-- > 0) {
            $.ajax(ajaxParams).fail(function (xhr, textStatus) {
                shashin.onFail(xhr, textStatus, ajaxParams, description, failFunction);
            });
        } else if (typeof failFunction !== "undefined" && typeof failFunction === "function") {
            failFunction();
        }
    };

    function hideRemoveElements(elements, tag, remove) {
        elements.each( function () {
            const attr = $(this).attr('data-tag');
            if (tag === null || (typeof attr !== 'undefined' && attr !== false && attr === tag)) {
                if (remove === true) {
                    $(this).remove();
                } else {
                    $(this).hide();
                }
            }
        });
    }

    shashin.removeElements = function (elements, tag) {
        hideRemoveElements(elements, tag, true);
    };

    shashin.hideElements = function (elements, tag) {
        hideRemoveElements(elements, tag, false);
    };
}( window.shashin = window.shashin || {}, jQuery ));