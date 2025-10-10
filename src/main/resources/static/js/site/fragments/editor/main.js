function initializeEditor(editMetadataObj, lgIndex) {
    // console.log("--------------------");
    // console.log(editMetadataObj.id);
    // console.log(editMetadataObj);
    // console.log(lgIndex);

    // 1. Load original from path
    // 2. Apply transformations stored in DB
    // 3. When reseting, do step 1 & 2
    // 4. When restoring, delete original thumb

    // 2. Get stored transformations
    // brightness, contrast, rotation, flipHorizontally, flipVertically
    let rotation = 0;
    let isFlippedHorizontally = false;
    let isFlippedVertically = false;
    let brightness = 1.0;
    let contrast = 1.0;
    let saturation = 1.0;

    // Loading spinner
    styleTopControl("#editorSpinner", "2em", "35px", "right", "15px");
    $("#editorContainer").css("display", "block");
    $("#editorToolContainer").css("display", "block");
    $("#editorSpinner").css("display", "block");
    $("#editorCloseActionButton").css("display", "none");
    applyDefaultTransformations();

    // console.log("initializeEditor after applying transitions");
    // console.log("rotation:"+rotation);
    // console.log("isFlippedHorizontally:"+isFlippedHorizontally);
    // console.log("isFlippedVertically:"+isFlippedVertically);
    // console.log("brightness:"+brightness);
    // console.log("contrast:"+contrast);
    // console.log("saturation:"+saturation);
    shashin.printMessageToConsole("initializeEditor after applying transitions",{tag:"editor"});
    shashin.printMessageToConsole("rotation:"+rotation,{tag:"editor"});
    shashin.printMessageToConsole("isFlippedHorizontally:"+isFlippedHorizontally,{tag:"editor"});
    shashin.printMessageToConsole("isFlippedVertically:"+isFlippedVertically,{tag:"editor"});
    shashin.printMessageToConsole("brightness:"+brightness,{tag:"editor"});
    shashin.printMessageToConsole("contrast:"+contrast,{tag:"editor"});
    shashin.printMessageToConsole("saturation:"+saturation,{tag:"editor"});

    const gammaTables = createGammaTables(2.2);

    function getDigitsAfterDot(num) {
        const parts = (Math.round(num*10)/10).toString().split(".");
        return parts[1] || "0";
    }

    function isSpinnerHidden() {
        return $("#editorSpinner").css("display") === "none";
    }

    function normalizedRotation(rotationInput) {
        return Math.abs(((rotationInput % 360) + 360) % 360);
    }

    // Title
    styleTopControl("#editorTitle", "2rem", "23px", "left", "30px");

    // First row buttons
    let sideValue = 8;
    const firstRowButtons = [
        { id: "#editorCloseActionButton", fontSize: "3rem", top: "10px" },
        { id: "#editorSaveActionButton", fontSize: "2rem", top: "23px" },
        { id: "#editorRestoreActionButton", fontSize: "2rem", top: "23px" },
        { id: "#editorResetActionButton", fontSize: "2rem", top: "23px" }
    ];
    const firstRowMenuHeightWidth = applyStyles(firstRowButtons, sideValue, "right");
    
    // Second row buttons
    sideValue = 13;
    const secondRowButtons = [
        { id: "#editorFlipHorizontalActionButton", fontSize: "2rem", top: "75px" },
        { id: "#editorFlipVerticalActionButton", fontSize: "2rem", top: "75px" },
        { id: "#editorRotateLeftActionButton", fontSize: "2rem", top: "76px" },
        { id: "#editorRotateRightActionButton", fontSize: "2rem", top: "76px" }
    ];
    const secondRowMenuHeightWidth = applyStyles(secondRowButtons, sideValue, "right");

    // Third row buttons
    sideValue = 10;
    let thirdRowButtons = [
        { id: "#editorBrightnessActionButton", fontSize: "2rem", top: "130px" }
    ];
    let thirdRowMenuHeightWidth = 0;
    if (Util.isMobile()) {
        $(".range-ticks span").css("display", "none");
        styleBottomControl("#editorBrightnessActionButton", "2em", "155px", "right", "10px", "#editorBrightnessIcon");
    } else {
        thirdRowMenuHeightWidth = applyStyles(thirdRowButtons, sideValue, "right");
    }

    // Fourth row buttons
    sideValue = 10;
    let fourthRowButtons = [
        { id: "#editorContrastActionButton", fontSize: "2rem", top: "220px" }
    ];
    let fourthRowMenuHeightWidth = 0;
    if (Util.isMobile()) {
        $(".range-ticks span").css("display", "none");
        styleBottomControl("#editorContrastActionButton", "2em", "95px", "right", "10px", "#editorContrastIcon");
    } else {
        fourthRowMenuHeightWidth = applyStyles(fourthRowButtons, sideValue, "right");
    }

    // Fifth row buttons
    sideValue = 10;
    let fifthRowButtons = [
        { id: "#editorSaturationActionButton", fontSize: "2rem", top: "310px" }
    ];
    let fifthRowMenuHeightWidth = 0;
    if (Util.isMobile()) {
        $(".range-ticks span").css("display", "none");
        styleBottomControl("#editorSaturationActionButton", "2em", "35px", "right", "10px", "#editorSaturationIcon");
    } else {
        fifthRowMenuHeightWidth = applyStyles(fifthRowButtons, sideValue, "right");
    }

    const rowWidths = [
        firstRowMenuHeightWidth[0],
        secondRowMenuHeightWidth[0],
        thirdRowMenuHeightWidth[0],
        fourthRowMenuHeightWidth[0],
        fifthRowMenuHeightWidth[0]
    ];
    let blockWidth = Math.max(...rowWidths);
    let blockHeight = firstRowMenuHeightWidth[1] +
        secondRowMenuHeightWidth[1] +
        thirdRowMenuHeightWidth[1] +
        fourthRowMenuHeightWidth[1] +
        fifthRowMenuHeightWidth[1]
    ;

    function styleTopControl(id, fontSize, top, side, sideValue) {
        $(id).css({
            "font-size": fontSize,
            "color": "#FFFFFF",
            "z-index": 99998,
            "position": "absolute",
            "top": top,
            [side]: sideValue
        });
    }

    function styleBottomControl(id, fontSize, bottom, side, sideValue, iconId) {
        $(id).css({
            "font-size": fontSize,
            "color": "#FFFFFF",
            "z-index": 99998,
            "position": "absolute",
            "bottom": bottom,
            [side]: sideValue
        });

        $(iconId).css({
            "z-index": 999999
        });
    }

    function applyStyles(buttons, startValue, side) {
        let offset = startValue;
        let maxTop = 0;
        for (const { id, fontSize, top } of buttons) {
            styleTopControl(id, fontSize, top, side, offset + "px");
            const topPixels = parseInt(top.replace("px", ""));
            if (topPixels > maxTop) {
                maxTop = topPixels;
            }
            if (id === "#editorBrightnessActionButton" || id === "#editorContrastActionButton") {
                offset += 150;
            } else {
                offset += 75;
            }
        }

        return [offset, maxTop];
    }

    if (!Util.isMobile()) {
        $("#editorBlock").css({
            "position": "absolute",
            "height": (blockHeight - 339) + "px",
            "width": (blockWidth + 5) + "px",
            "right": "0"
        });
    }

    showModule();

    $("#editorContainer").css({
        "width": "100%",
        "height": "100%",
        "display": "block",
        "z-index": 9999,
        "background-color": "#000000",
        "overflow": "hidden"
    });

    $(".centerFit").css({
        "max-width": $(window).innerWidth() + 1,
        "height": "auto",
        "max-height": $(window).innerHeight() + 1,
        "position": "absolute",
        "top": "50%",
        "left": "50%",
        "transform": "translate(-50%, -50%)"
    });

    $("#editorContainer").off("click").on('click', function(event) {
        event.preventDefault();

        // Ensure all edit inputs and buttons are listed
        if (!$(event.target).closest(
            '#editorCloseActionButton, ' +
            '#editorFlipHorizontalActionButton, ' +
            '#editorFlipVerticalActionButton, ' +
            '#editorRotateRightActionButton, ' +
            '#editorRotateLeftActionButton, ' +
            '#editorRestoreActionButton, ' +
            '#editorSaveActionButton, ' +
            '#editorResetActionButton, ' +
            '#editorBrightnessActionButton, ' +
            '#editorBrightnessAction, ' +
            '#editorContrastActionButton, ' +
            '#editorContrastAction, ' +
            '#editorSaturationActionButton, ' +
            '#editorSaturationAction, ' +
            '#editorBlock'
        ).length) {
            if ($("#editorSpinner").css("display") === "none") {
                hideModule();
            }
        }
    });

    $("#editorCloseAction").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            hideModule();
        }
    });

    function updateTransform(showTransition = true) {
        const flipX = isFlippedHorizontally ? -1 : 1;
        const flipY = isFlippedVertically ? -1 : 1;
        const isEvenRotation = rotation % 180 === 0;

        let style = {
            "transform": `translate(-50%, -50%) rotate(${rotation}deg) scale(${flipX}, ${flipY})`,
            "max-width": isEvenRotation ? $(window).innerWidth() + 1 : $(window).innerHeight() + 1,
            "max-height": isEvenRotation ? $(window).innerHeight() + 1 : $(window).innerWidth() + 1
        };
        if (showTransition === true) {
            style.transition = "transform 0.3s ease-in-out";
        } else {
            style.transition = "none";
        }

        $("#editShashinImage").css(style);
    }

    // Slider icon
    $("#editorBrightnessIcon").off("click").on('click', function(event) {
        event.preventDefault();

        let oldBrightness = brightness;
        brightness = 1.0;
        $("#editorBrightnessAction").val(0);

        if (oldBrightness !== brightness) {
            applyCanvasAttributes(brightness, contrast, saturation);
        }
    });

    $("#editorContrastIcon").off("click").on('click', function(event) {
        event.preventDefault();

        let oldContrast = contrast;
        contrast = 1.0;
        $("#editorContrastAction").val(0);

        if (oldContrast !== contrast) {
            applyCanvasAttributes(brightness, contrast, saturation);
        }
    });

    $("#editorSaturationIcon").off("click").on('click', function(event) {
        event.preventDefault();

        let oldSaturation = saturation;
        saturation = 1.0;
        $("#editorSaturationAction").val(0);

        if (oldSaturation !== saturation) {
            applyCanvasAttributes(brightness, contrast, saturation);
        }
    });

    // Slider adjustment
    $("#editorBrightnessAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let oldBrightness = brightness;
            let number = $("#editorBrightnessAction").val();
            brightness = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                brightness = 1-parseFloat("0."+number);
            }

            if (oldBrightness !== brightness) {
                applyCanvasAttributes(brightness, contrast, saturation);
            }
        }
    });

    $("#editorContrastAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let oldContrast = contrast;
            let number = $("#editorContrastAction").val();
            contrast = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                contrast = 1-parseFloat("0."+number);
            }

            if (oldContrast !== contrast) {
                applyCanvasAttributes(brightness, contrast, saturation);
            }
        }
    });

    $("#editorSaturationAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let oldSaturation = saturation;
            let number = $("#editorSaturationAction").val();
            saturation = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                saturation = 1-parseFloat("0."+number);
            }

            if (oldSaturation !== saturation) {
                applyCanvasAttributes(brightness, contrast, saturation);
            }
        }
    });

    function applyCanvasAttributes(brightnessInput, contrastInput, saturationInput, applyTransformation = false) {
        disableButtons();
        $("#editorSpinner").css("display", "block");
        $("#editorCloseActionButton").css("display", "none");

        document.body.style.overflowY= 'hidden';
        const canvas = document.createElement("canvas");
        $(canvas).attr("id", "editShashinImageCanvas");

        document.body.appendChild(canvas);
        const ctx = canvas.getContext("2d");

        const img = new Image();

        $(img).on("load", function (e) {
            e.preventDefault();

            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);
            const imageData = ctx.getImageData(0, 0, img.width, img.height);

            let adjustedImageData = adjustBrightnessContrast(imageData, brightnessInput, contrastInput, gammaTables);
            adjustedImageData = adjustSaturation(imageData, saturationInput);
            ctx.putImageData(adjustedImageData, 0, 0);
            const imageURL = canvas.toDataURL("image/jpeg", 0.2);

            $("#editShashinImage").attr("src", imageURL);

            document.body.style.overflow = 'auto';
            $(canvas).remove();

            if (applyTransformation) {
                updateTransform(false);
            }

            enableButtons();
            $("#editorSpinner").css("display", "none");
            $("#editorCloseActionButton").css("display", "block");
        });

        $(img).on("error", function (e) {
            e.preventDefault();

            enableButtons();
            $("#editorSpinner").css("display", "none");
            $("#editorCloseActionButton").css("display", "block");
        });

        img.src = "/api/v1/image/original/"+editMetadataObj.id+"?v="+uuidv4();
    }

    function applyDefaultTransformations() {
        rotation = 0;
        isFlippedHorizontally = false;
        isFlippedVertically = false;
        brightness = 1.0;
        contrast = 1.0;
        saturation = 1.0;

        if (editMetadataObj.hasOwnProperty("rotation") && editMetadataObj.rotation !== null) {
            rotation = parseInt(editMetadataObj.rotation);
        }

        // TODO: Flipping around works here. why
        if (editMetadataObj.hasOwnProperty("flipHorizontally") && editMetadataObj.flipHorizontally !== null) {
            isFlippedVertically = editMetadataObj.flipHorizontally;
        }

        if (editMetadataObj.hasOwnProperty("flipVertically") && editMetadataObj.flipVertically !== null) {
            isFlippedHorizontally = editMetadataObj.flipVertically;
        }

        if (editMetadataObj.hasOwnProperty("brightness") && editMetadataObj.brightness !== null) {
            brightness = parseFloat(editMetadataObj.brightness);
        }
        if (brightness >= 1.0) {
            $("#editorBrightnessAction").val(parseInt(getDigitsAfterDot(brightness)));
        } else {
            $("#editorBrightnessAction").val(-parseInt(getDigitsAfterDot(1 - brightness)));
        }

        if (editMetadataObj.hasOwnProperty("contrast") && editMetadataObj.contrast !== null) {
            contrast = parseFloat(editMetadataObj.contrast);
        }
        if (contrast >= 1.0) {
            $("#editorContrastAction").val(parseInt(getDigitsAfterDot(contrast)));
        } else {
            $("#editorContrastAction").val(-parseInt(getDigitsAfterDot(1 - contrast)));
        }

        if (editMetadataObj.hasOwnProperty("saturation") && editMetadataObj.saturation !== null) {
            saturation = parseFloat(editMetadataObj.saturation);
        }
        if (saturation >= 1.0) {
            $("#editorSaturationAction").val(parseInt(getDigitsAfterDot(saturation)));
        } else {
            $("#editorSaturationAction").val(-parseInt(getDigitsAfterDot(1 - saturation)));
        }

        // Apply transformations
        applyCanvasAttributes(brightness, contrast, saturation, true);
    }

    function createGammaTables(gamma = 2.2) {
        const decode = new Array(256);
        const encode = new Array(256);
        for (let i = 0; i < 256; i++) {
            const lin = Math.pow(i / 255, gamma);
            decode[i] = lin;
            encode[i] = Math.pow(lin, 1 / gamma) * 255;
        }
        return { decode, encode };
    }

    function adjustBrightnessContrast(imageData, brightness, contrast, gammaTables) {
        const data = imageData.data;
        const decode = gammaTables.decode;

        function truncate(value) {
            return value < 0 ? 0 : value > 255 ? 255 : value;
        }

        for (let i = 0; i < data.length; i += 4) {
            for (let j = 0; j < 3; j++) { // R, G, B
                let lin = decode[data[i + j]];
                let contrasted = (lin - 0.5) * contrast + 0.5;
                let brightened = contrasted * brightness;
                data[i + j] = truncate(Math.pow(brightened, 1 / 2.2) * 255);
            }
            data[i + 3] = data[i + 3]; // Preserve alpha
        }

        return imageData;
    }

    function adjustSaturation(imageData, saturation) {
        const data = imageData.data;
        for (let i = 0; i < data.length; i += 4) {
            const [r, g, b] = [data[i], data[i + 1], data[i + 2]];
            const [h, s, l] = rgbToHsl(r, g, b);
            const [newR, newG, newB] = hslToRgb(h, Math.min(s * saturation, 1), l);
            [data[i], data[i + 1], data[i + 2]] = [newR, newG, newB];
        }
        return imageData;
    }

    function rgbToHsl(r, g, b) {
        r /= 255; g /= 255; b /= 255;
        const max = Math.max(r, g, b), min = Math.min(r, g, b);
        let h, s, l = (max + min) / 2;
        if (max === min) {
            h = s = 0;
        } else {
            const d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            switch (max) {
                case r: h = ((g - b) / d + (g < b ? 6 : 0)); break;
                case g: h = ((b - r) / d + 2); break;
                case b: h = ((r - g) / d + 4); break;
            }
            h /= 6;
        }
        return [h, s, l];
    }

    function hslToRgb(h, s, l) {
        let r, g, b;
        if (s === 0) {
            r = g = b = l;
        } else {
            const hue2rgb = (p, q, t) => {
                if (t < 0) t += 1;
                if (t > 1) t -= 1;
                if (t < 1 / 6) return p + (q - p) * 6 * t;
                if (t < 1 / 2) return q;
                if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
                return p;
            };
            const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            const p = 2 * l - q;
            r = hue2rgb(p, q, h + 1 / 3);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1 / 3);
        }
        return [Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)];
    }

    $("#editorRotateRightActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        if (isSpinnerHidden()) {
            rotation += 90;
            updateTransform();
        }
    });

    $("#editorRotateLeftActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        if (isSpinnerHidden()) {
            rotation -= 90;
            updateTransform();
        }
    });

    $("#editorFlipVerticalActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        if (isSpinnerHidden()) {
            if (normalizedRotation(rotation) === 90 || normalizedRotation(rotation) === 270) {
                isFlippedVertically = !isFlippedVertically;
            } else {
                isFlippedHorizontally = !isFlippedHorizontally;
            }
            updateTransform();
        }
    });

    $("#editorFlipHorizontalActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        if (isSpinnerHidden()) {
            if (normalizedRotation(rotation) === 90 || normalizedRotation(rotation) === 270) {
                isFlippedHorizontally = !isFlippedHorizontally;
            } else {
                isFlippedVertically = !isFlippedVertically;
            }
            updateTransform();
        }
    });

    $("#editorResetActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            applyDefaultTransformations();

            updateTransform(false);

            showModule();

            $("#editorContainer").css({
                "width": "100%",
                "height": "100%",
                "display": "block",
                "z-index": 9999,
                "background-color": "#000000",
                "overflow": "hidden"
            });

            $(".centerFit").css({
                "max-width": $(window).innerWidth() + 1,
                "height": "auto",
                "max-height": $(window).innerHeight() + 1,
                "position": "absolute",
                "top": "50%",
                "left": "50%",
                "transform": "translate(-50%, -50%)"
            });
        }
    });

    $("#editorRestoreActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            disableButtons();
            $("#editorSpinner").css("display", "block");
            $("#editorCloseActionButton").css("display", "none");

            rotation = 0;
            isFlippedHorizontally = false;
            isFlippedVertically = false;
            brightness = 1.0;
            $("#editorBrightnessAction").val(0);
            contrast = 1.0;
            $("#editorContrastAction").val(0);
            saturation = 1.0;
            $("#editorSaturationAction").val(0);

            shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, rotation, isFlippedHorizontally, isFlippedVertically, brightness, contrast, saturation, true,  function (success) {
                shashin.printMessageToConsole("Restoring edited metadata:"+success,{tag:"editor"});

                if (success === true) {
                    rotation = 0;
                    isFlippedHorizontally = false;
                    isFlippedVertically = false;
                    brightness = 1.0;
                    $("#editorBrightnessAction").val(0);
                    contrast = 1.0;
                    $("#editorContrastAction").val(0);
                    saturation = 1.0;
                    $("#editorSaturationAction").val(0);

                    editMetadataObj.brightness = brightness;
                    editMetadataObj.contrast = contrast;
                    editMetadataObj.saturation = saturation;
                    editMetadataObj.rotation = rotation;
                    editMetadataObj.flipHorizontally = isFlippedHorizontally;
                    editMetadataObj.flipVertically = isFlippedVertically;

                    applyDefaultTransformations();

                    updateTransform(false);

                    shashin.showToastMessage(shashin.getTranslatedValue("main.pages.map.modal.restored"), shashin.getTranslatedValue("main.pages.map.modal.restored"), {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        delay: 2000,
                        borderColor: "success"
                    });
                } else {
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail.body"), shashin.getTranslatedValue("main.toast.account.profile.fail.body"), {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor:"danger"
                    });
                }

                enableButtons();
                $("#editorSpinner").css("display", "none");
                $("#editorCloseActionButton").css("display", "block");
                // hideModule();
            });
        }
    });

    $("#editorSaveActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        disableButtons();
        $("#editorSpinner").css("display", "block");
        $("#editorCloseActionButton").css("display", "none");
        const swapFlip = isFlippedVertically;
        isFlippedVertically = isFlippedHorizontally;
        isFlippedHorizontally = swapFlip;

        shashin.printMessageToConsole("Saving with settings: Rotation: "+normalizedRotation(rotation)+", isFlippedHorizontally:"+isFlippedHorizontally+", isFlippedVertically:"+isFlippedVertically+", brightness:"+brightness+", contrast:"+contrast,{tag:"editor"});
        shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, normalizedRotation(rotation), isFlippedHorizontally, isFlippedVertically, brightness, contrast, saturation, false,  function (success) {
            shashin.printMessageToConsole("Edited metadata:"+success,{tag:"editor"});

            if (success === true) {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.upload"), shashin.getTranslatedValue("main.toast.app.image.upload"), {
                    icon: "bi-info-circle",
                    iconColor: "#777777",
                    delay: 2000,
                    borderColor: "success"
                });

                editMetadataObj.brightness = brightness;
                editMetadataObj.contrast = contrast;
                editMetadataObj.saturation = saturation;
                editMetadataObj.rotation = normalizedRotation(rotation);
                editMetadataObj.flipHorizontally = isFlippedHorizontally;
                editMetadataObj.flipVertically = isFlippedVertically;
            } else {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.notupload"), shashin.getTranslatedValue("main.toast.app.image.notupload"), {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });
            }

            enableButtons();
            $("#editorSpinner").css("display", "none");
            $("#editorCloseActionButton").css("display", "block");
            hideModule();
        });
    });

    function enableButtons() {
        const buttonIds = [
            "editorSaveAction",
            "editorBrightnessAction",
            "editorContrastAction",
            "editorSaturationAction",
            "editorRestoreAction",
            "editorResetAction",
            "editorRotateRightAction",
            "editorRotateLeftAction",
            "editorFlipHorizontalAction",
            "editorFlipVerticalAction",
            "editorBrightnessIcon",
            "editorContrastIcon",
            "editorSaturationIcon"
        ];

        buttonIds.forEach(id => {
            $('#' + id)
                .prop('disabled', false)
                .css({'color': "#FFFFFF", "text-shadow": "#EDEBEB 1px 0 10px"});
        });
    }

    function disableButtons() {
        const buttonIds = [
            "editorSaveAction",
            "editorBrightnessAction",
            "editorContrastAction",
            "editorSaturationAction",
            "editorRestoreAction",
            "editorResetAction",
            "editorRotateRightAction",
            "editorRotateLeftAction",
            "editorFlipHorizontalAction",
            "editorFlipVerticalAction",
            "editorBrightnessIcon",
            "editorContrastIcon",
            "editorSaturationIcon"
        ];

        buttonIds.forEach(id => {
            $('#' + id)
                .prop('disabled', true)
                .css({'color': "#808080", "text-shadow": "#969595 1px 0 10px"});
        });
    }

    function showModule() {
        $("#editorContainer").css("display", "block");

        $("#editorMedia").css("display", "block");
        $("#editorMedia").html("<img class='centerFit' id='editShashinImage' src='/api/v1/image/" + editMetadataObj.id + "?v=" + uuidv4() + "'>");
        $("#editShashinImage").on('load', function () {
            $("#editorToolContainer").css("display", "block");
            $("#editorBlock").css("display", "block");
        });
    }

    function hideModule() {
        // Show lightgallery toolbar
        $(".lg-toolbar").css("display", "block");
        $(".lg-outer").removeClass("lg-hide-items");

        // $(".lg-object.lg-image").on('load', function() {
        $("#editorContainer").css("display", "none");
        $("#editorToolContainer").css("display", "none");
        $("#editorBlock").css("display", "none");

        $("#editorMedia").css("display", "none");
        $("#editorMedia").html("");

        rotation = 0;
        brightness = 1.0;
        contrast = 1.0;
        saturation = 1.0;
        isFlippedHorizontally = false;
        isFlippedVertically = false;
    }
}