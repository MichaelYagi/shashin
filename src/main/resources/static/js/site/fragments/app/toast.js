(function( shashin, $, undefined ) {
    /*
        options:
            icon = bootstrap icon
            iconColor = CSS color
            headerSubtext = string, sits beside the title in smaller print
            delay = in ms
            autohide = boolean
            placement = one of shashin.toast.placement.*
            borderColor = one of primary, secondary, success, danger, warning, info, light, dark, white
            tag = string, identifies and labels the toast
            refreshTag = if tag is already defined, overwrites the tag with updated content
            width = Width of toast - string eg 300px
            closeButton = boolean
        */
    shashin.showToastMessage = function(title, message, options) {
        shashin.printMessageToConsole(title, {tag: "toast"});
        shashin.printMessageToConsole(JSON.stringify(options), {tag: "toast"});

        const createToast = function (index, placement, tag, title, width, closeButton) {

            let settingWidth = "";
            if (width !== null) {
                settingWidth = "width: "+width+";";
            }

            let tagString = "";
            if (tag !== null) {
                tagString = " data-tag='"+tag+"'";
            }

            let html = '<div id="'+placement+'_ToastTarget_'+index+'"'+tagString+' class="toast centered-div" role="alert" aria-live="assertive" aria-atomic="true" style="margin: 0 auto;'+settingWidth+'">';

            if (title !== null && title !== "") {
                html += '<div class="toast-header">' +
                    '<span id="' + placement + '_ToastIcon_' + index + '" class="toast-icon"></span><span id="' + placement + '_ToastSpacer_' + index + '"></span>' +
                    '<strong id="' + placement + '_ToastTitle_' + index + '" class="me-auto toast-title"></strong>' +
                    '<small id="' + placement + '_HeaderSubtext_' + index + '" class="toast-subtext"></small>';
                if (closeButton === true) {
                    html += '<button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>';
                }
                html += '</div>';
            }

            if (title === null || title === "") {
                html += '<div class="d-flex">';
            }

            html += '<div id="'+placement+'_ToastMessage_'+index+'" class="toast-body"></div>';

            if (closeButton === true && (title === null || title === "")) {
                html += '<button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>';
            }

            html += '</div>';

            $(html).insertBefore($("#" + placement + "_ToastTargetAttach"));
        };

        let icon = null;
        let iconColor = "lightgray";
        let headerSubtext = null;
        let borderColor = null;
        let placement = shashin.toast.placement.bottom.center;
        let autohide = true;
        let delay = 5000;
        let tag = null;
        let refreshTag = null;
        let closeButton = true;
        let width = null;

        if (options === undefined || options === null) {
            placement = shashin.toast.placement.bottom.center;
        } else {
            const validOptions = [];

            if (options.hasOwnProperty("autohide")) {
                autohide = options.autohide;
                validOptions.push("autohide");
            }

            if (options.hasOwnProperty("delay")) {
                delay = options.delay;
                validOptions.push("delay");
            }

            if (options.hasOwnProperty("placement")) {
                placement = options.placement;
                validOptions.push("placement");
            }

            if (options.hasOwnProperty("headerSubtext")) {
                headerSubtext = options.headerSubtext;
                validOptions.push("headerSubtext");
            }

            if (options.hasOwnProperty("borderColor")) {
                borderColor = options.borderColor;
                validOptions.push("borderColor");
            }

            if (options.hasOwnProperty("iconColor")) {
                iconColor = options.iconColor;
                validOptions.push("iconColor");
            }

            if (options.hasOwnProperty("icon")) {
                icon = options.icon;
                validOptions.push("icon");
            }

            if (options.hasOwnProperty("tag")) {
                tag = options.tag;
                validOptions.push("tag");
            }

            if (options.hasOwnProperty("refreshTag")) {
                refreshTag = options.refreshTag;
                validOptions.push("refreshTag");
            }

            if (options.hasOwnProperty("closeButton")) {
                closeButton = options.closeButton;
                validOptions.push("closeButton");
            }

            if (options.hasOwnProperty("width")) {
                width = options.width;
                validOptions.push("width");
            }

            const invalidOptions = [];
            for (let key in options) {
                if (options.hasOwnProperty(key) === false || (options.hasOwnProperty(key) === true && validOptions.includes(key) === false)) {
                    invalidOptions.push(key);
                }
            }
            if (invalidOptions.length > 0) {
                shashin.printMessageToConsole("Invalid toast options: " + invalidOptions.join(), {tag: "toast"});
            }
        }

        const container = placement+"ToastContainer";
        const attachPoint = $("#" + container).find(".attachPoint");
        const siblingCount = attachPoint.siblings().length;

        let toastId = "";
        if (siblingCount === 0) {
            toastId = container.slice(0, container.indexOf("ToastContainer")) + "_ToastTarget_1";
        }

        const previousSibling = attachPoint.prev();
        const lastToast = previousSibling.attr("id");
        const lastToastArray = toastId !== "" ? toastId.split("_") : lastToast.split("_");

        if (lastToastArray.length === 3 && placement === lastToastArray[0]) {
            const target = lastToastArray[1];
            const lastIteration = lastToastArray[2];

            if ($.isNumeric(lastIteration)) {

                let nextIteration = parseInt(lastIteration);
                nextIteration = nextIteration + 1;

                let toastId = placement + "_" + target + "_" + nextIteration;

                let attached = false;

                // Test if closed - use it, otherwise create new after last open
                if (tag !== null) {
                    // check if tag exists in the target placement, exit if exists to prevent flashing
                    if (shashin.hasToast(placement, {tag: tag}) === true) {
                        if (refreshTag === true) {
                            shashin.removeElements($("#" + placement + "_ToastTargetAttach").siblings(), tag);
                        } else {
                            return true;
                        }
                    }

                    if (shashin.hasToast(placement, {tag: tag}) === false) {
                        createToast(nextIteration, placement, tag, title, width, closeButton);

                        const attr = $("#" + toastId).attr('data-tag');

                        if (typeof attr !== 'undefined' && attr !== false && $("#" + toastId).attr('data-tag') === tag) { // && $("#" + toastId).hasClass('show') === false
                            attached = true;
                        }
                    }
                } else if ($("#" + toastId).length === 0 || ($("#" + toastId).length > 0 && $("#" + toastId).hasClass('in') === false && $("#" + toastId).hasClass('show') === false)) {
                    createToast(nextIteration, placement, tag, title, width, closeButton);
                    attached = true;
                }

                if (attached === true) {
                    const messageId = placement + "_ToastMessage_" + nextIteration;
                    $("#" + messageId).html(message);
                    const titleId = placement + "_ToastTitle_" + nextIteration;
                    $("#" + titleId).html(title);

                    $("#" + toastId).removeClass(function (index, className) {
                        return (className.match(/(^|\s)border-\S+/g) || []).join(' ');
                    });
                    $("#" + toastId).removeClass("border");
                    if (borderColor === "primary" ||
                        borderColor === "secondary" ||
                        borderColor === "success" ||
                        borderColor === "danger" ||
                        borderColor === "warning" ||
                        borderColor === "info" ||
                        borderColor === "light" ||
                        borderColor === "dark" ||
                        borderColor === "white") {
                        $("#" + toastId).addClass("border border-" + borderColor);
                    }

                    if (headerSubtext !== null) {
                        const headerSubtextId = placement + "_HeaderSubtext_" + nextIteration;
                        $("#" + headerSubtextId).html(headerSubtext);
                    }

                    if (autohide === false || autohide === true) {
                        $("#" + toastId).attr("data-bs-autohide", autohide);

                        if (autohide === true) {
                            $("#" + toastId).attr("data-bs-delay", delay);
                        }
                    } else {
                        $("#" + toastId).attr("data-bs-delay", delay);
                    }

                    if (icon !== undefined && icon !== null) {
                        const iconEl = placement + "_ToastIcon_" + nextIteration;
                        const iconField = $("#" + iconEl);
                        const spacerEl = placement + "_ToastSpacer_" + nextIteration;
                        const spacerField = $("#" + spacerEl);
                        let cssStyle = {"font-size": "1rem"};
                        if (iconColor !== null) {
                            cssStyle.color = iconColor;
                        }
                        iconField.css(cssStyle);
                        iconField.addClass(icon);
                        spacerField.html("&nbsp;");
                    }

                    const toastLive = document.getElementById(toastId);
                    const toast = new bootstrap.Toast(toastLive);
                    toast.show();

                    toastLive.addEventListener('hidden.bs.toast', () => {
                        $("#" + toastId).remove();
                    });
                }
            }
        }
    };

    // placement - topLeft, topCenter, etc
    shashin.closeToastMessages = function (options) {
        shashin.printMessageToConsole(JSON.stringify(options), {tag: "toast"});
        let tags = [];
        if (options && options.hasOwnProperty("tags")) {
            tags = options.tags;
        }
        if (options && options.hasOwnProperty("tag")) {
            tags.push(options.tag);
        }
        if (tags.length === 0) {
            tags = [null];
        }

        let placements = [];

        const validPlacements = [
            shashin.toast.placement.default,
            shashin.toast.placement.top.left,
            shashin.toast.placement.top.center,
            shashin.toast.placement.top.right,
            shashin.toast.placement.middle.left,
            shashin.toast.placement.middle.center,
            shashin.toast.placement.middle.right,
            shashin.toast.placement.bottom.left,
            shashin.toast.placement.bottom.center,
            shashin.toast.placement.bottom.right
        ];
        if (options && options.hasOwnProperty("placements") && options.placements.length > 0) {
            placements = options.placements;
        }
        if (options && options.hasOwnProperty("placement")) {
            placements.push(options.placement);
        }
        if (placements.length === 0) {
            placements = validPlacements;
        }

        let hide = false;
        if (options && options.hasOwnProperty("hide")) {
            hide = options.hide;
        }

        if (Array.isArray(tags) && Array.isArray(placements)) {
            let invalidPlacements = [];
            placements.forEach(function (placement, index) {
                if (validPlacements.includes(placement)) {
                    tags.forEach(function (tag, index) {
                        if (hide === true) {
                            shashin.hideElements($("#" + placement + "_ToastTargetAttach").siblings(), tag);
                        } else {
                            shashin.removeElements($("#" + placement + "_ToastTargetAttach").siblings(), tag);
                        }
                    });
                } else {
                    invalidPlacements.push(placement);
                }
            });

            if (invalidPlacements.length > 0) {
                shashin.printMessageToConsole("Invalid placements detected: " + invalidPlacements.join(","), {tag: "toast"});
            }
        } else {
            shashin.printMessageToConsole("Tags or placement are not arrays", {tag: "toast"});
        }
    };

    shashin.hasToast = function (placement, options) {
        let tag = null;
        let findHidden = false;

        if (options && options.hasOwnProperty("tag")) {
            tag = options.tag;
        }
        if (options && options.hasOwnProperty("findHidden")) {
            findHidden = options.findHidden;
        }

        let counter = 0;
        let foundTag = false;
        $("#"+placement+"ToastContainer div.toast-container").children().each(function(i, obj) {
            if ((findHidden === true ||
                (findHidden === false &&
                    (typeof $(obj).attr('style') === 'undefined' || $(obj).attr('style') === false) || $(obj).css('display') === "block")) && $(obj).hasClass("attachPoint") === false
            ) {
                if (tag !== null && $(obj).attr("data-tag") === tag) {
                    foundTag = true;
                    return foundTag;
                }
                counter++;
            }
        });

        return foundTag || (tag === null && counter > 0);
    };

    shashin.getToastElement = function (placement, tag) {
        let foundObj = null;

        if (placement === null || tag === null) {
            return null;
        }

        $("#"+placement+"ToastContainer div.toast-container").children().each(function(i, obj) {
            if ($(obj).hasClass("attachPoint") === false) {
                if (tag !== null && $(obj).attr("data-tag") === tag) {
                    foundObj = obj;
                    return true;
                }
            }
        });

        return foundObj;
    };

    shashin.getToastElements = function (placement) {
        let foundObjs = null;

        if (placement === null) {
            return null;
        }

        $("#"+placement+"ToastContainer div.toast-container").children().each(function(i, obj) {
            if ($(obj).hasClass("attachPoint") === false) {
                foundObjs = obj;
            }
        });

        return foundObjs;
    };
}( window.shashin = window.shashin || {}, jQuery ));