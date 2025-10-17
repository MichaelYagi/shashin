function initializeEditor(editMetadataObj, lgIndex) {
    // console.log("--------------------");
    // console.log(editMetadataObj.id);
    // console.log(editMetadataObj);
    // console.log(lgIndex);

    // 1. Load original from path
    // 2. Apply transformations stored in DB
    // 3. When reseting, do step 1 & 2
    // 4. When restoring, delete original thumb

    document.body.style.overflowY = 'hidden';

    // 2. Get stored transformations
    // brightness, contrast, rotation, flipHorizontally, flipVertically
    let rotation = 0;
    let isFlippedHorizontally = false;
    let isFlippedVertically = false;
    let brightness = 1.0;
    let contrast = 1.0;
    let saturation = 1.0;
    let sharpness = 1.0;

    // Loading spinner
    styleTopControl("#editorSpinner", "2em", "35px", "right", "25px");
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

    shashin.printMessageToConsole("--------------",{tag:"editor"});
    shashin.printMessageToConsole("initializeEditor after applying default transitions",{tag:"editor"});
    shashin.printMessageToConsole("rotation: "+rotation,{tag:"editor"});
    shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
    shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
    shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
    shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
    shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});
    shashin.printMessageToConsole("sharpness: "+sharpness,{tag:"editor"});

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
    styleTopControl("#editorTitle", "2rem", "23px", "left", "50px");

    // First row buttons
    let sideValue = 8+20;
    const firstRowButtons = [
        { id: "#editorCloseActionButton", fontSize: "3rem", top: "10px" },
        { id: "#editorSaveActionButton", fontSize: "2rem", top: "23px" },
        { id: "#editorRestoreActionButton", fontSize: "2rem", top: "23px" },
        { id: "#editorResetActionButton", fontSize: "2rem", top: "23px" }
    ];
    const firstRowMenuHeightWidth = applyStyles(firstRowButtons, sideValue, "right");
    
    // Second row buttons
    sideValue = 13+20;
    const secondRowButtons = [
        { id: "#editorFlipHorizontalActionButton", fontSize: "2rem", top: "75px" },
        { id: "#editorFlipVerticalActionButton", fontSize: "2rem", top: "75px" },
        { id: "#editorRotateLeftActionButton", fontSize: "2rem", top: "76px" },
        { id: "#editorRotateRightActionButton", fontSize: "2rem", top: "76px" }
    ];
    const secondRowMenuHeightWidth = applyStyles(secondRowButtons, sideValue, "right");

    // Third row buttons
    sideValue = 10+20;
    let thirdRowButtons = [
        { id: "#editorBrightnessActionButton", fontSize: "2rem", top: "130px" }
    ];
    let thirdRowMenuHeightWidth = 0;
    // if (Util.isMobile()) {
    //     styleBottomControl("#editorBrightnessActionButton", "2em", "215px", "right", "10px", "#editorBrightnessIcon");
    // } else {
        thirdRowMenuHeightWidth = applyStyles(thirdRowButtons, sideValue, "right");
    // }

    // Fourth row buttons
    sideValue = 10+20;
    let fourthRowButtons = [
        { id: "#editorContrastActionButton", fontSize: "2rem", top: "200px" }
    ];
    let fourthRowMenuHeightWidth = 0;
    // if (Util.isMobile()) {
    //     styleBottomControl("#editorContrastActionButton", "2em", "155px", "right", "10px", "#editorContrastIcon");
    // } else {
        fourthRowMenuHeightWidth = applyStyles(fourthRowButtons, sideValue, "right");
    // }

    // Fifth row buttons
    sideValue = 10+20;
    let fifthRowButtons = [
        { id: "#editorSaturationActionButton", fontSize: "2rem", top: "270px" }
    ];
    let fifthRowMenuHeightWidth = 0;
    // if (Util.isMobile()) {
    //     styleBottomControl("#editorSaturationActionButton", "2em", "95px", "right", "10px", "#editorSaturationIcon");
    // } else {
        fifthRowMenuHeightWidth = applyStyles(fifthRowButtons, sideValue, "right");
    // }

    // Sixth row buttons
    sideValue = 10+20;
    let sixthRowButtons = [
        { id: "#editorSharpnessActionButton", fontSize: "2rem", top: "340px" }
    ];
    let sixthRowMenuHeightWidth = 0;
    // if (Util.isMobile()) {
    //     styleBottomControl("#editorSharpnessActionButton", "2em", "35px", "right", "10px", "#editorSharpnessIcon");
    // } else {
        sixthRowMenuHeightWidth = applyStyles(sixthRowButtons, sideValue, "right");
    // }

    const rowWidths = [
        firstRowMenuHeightWidth[0],
        secondRowMenuHeightWidth[0],
        thirdRowMenuHeightWidth[0],
        fourthRowMenuHeightWidth[0],
        fifthRowMenuHeightWidth[0],
        sixthRowMenuHeightWidth[0],
    ];
    let blockWidth = Math.max(...rowWidths);
    let blockHeight = firstRowMenuHeightWidth[1] +
        secondRowMenuHeightWidth[1] +
        thirdRowMenuHeightWidth[1] +
        fourthRowMenuHeightWidth[1] +
        fifthRowMenuHeightWidth[1] +
        sixthRowMenuHeightWidth[1]
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

    // function styleBottomControl(id, fontSize, bottom, side, sideValue, iconId) {
    //     $(id).css({
    //         "font-size": fontSize,
    //         "color": "#FFFFFF",
    //         "z-index": 99998,
    //         "position": "absolute",
    //         "bottom": bottom,
    //         [side]: sideValue
    //     });
    //
    //     $(iconId).css({
    //         "z-index": 999999
    //     });
    // }

    function applyStyles(buttons, startValue, side) {
        let offset = startValue;
        let maxTop = 0;
        for (const { id, fontSize, top } of buttons) {
            styleTopControl(id, fontSize, top, side, offset + "px");
            const topPixels = parseInt(top.replace("px", ""));
            if (topPixels > maxTop) {
                maxTop = topPixels;
            }
            if (id === "#editorBrightnessActionButton" || id === "#editorContrastActionButton" || id === "#editorSaturationActionButton") {
                offset += 150;
            } else {
                offset += 75;
            }
        }

        return [offset, maxTop];
    }

    // if (!Util.isMobile()) {
        $("#editorBlock").css({
            "position": "absolute",
            "height": (blockHeight - 589) + "px",
            "width": (blockWidth + 5) + "px",
            "right": "0"
        });
    // }

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
            '#editorSharpnessActionButton, ' +
            '#editorSharpnessAction, ' +
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

        $("#brightnessTick").css("display", "none");
        if (isSpinnerHidden()) {
            brightness = 1.0;
            $("#editorBrightnessAction").val(0);
            applyAttributes();
        }
    });

    $("#editorContrastIcon").off("click").on('click', function(event) {
        event.preventDefault();

        $("#contrastTick").css("display", "none");
        if (isSpinnerHidden()) {
            contrast = 1.0;
            $("#editorContrastAction").val(0);
            applyAttributes();
        }
    });

    $("#editorSaturationIcon").off("click").on('click', function(event) {
        event.preventDefault();

        $("#saturationTick").css("display", "none");
        if (isSpinnerHidden()) {
            saturation = 1.0;
            $("#editorSaturationAction").val(0);
            applyAttributes();
        }
    });

    $("#editorSharpnessIcon").off("click").on('click', function(event) {
        event.preventDefault();

        $("#sharpnessTick").css("display", "none");
        if (isSpinnerHidden()) {
            sharpness = 1.0;
            $("#editorSharpnessAction").val(1);
            applyAttributes();
        }
    });

    // Slider adjustment
    $("#editorBrightnessAction").off("input").on("input", function (e) {
        e.preventDefault();

        if ($("#editorBrightnessAction").val() === "0") {
            $("#brightnessTick").css("display", "none");
        } else {
            $("#brightnessTick").css("display", "block");
        }
    });
    $("#brightnessTick").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = "0";
            $("#editorBrightnessAction").val(number);
            $("#brightnessTick").css("display", "none");
            brightness = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                brightness = 1-parseFloat("0."+number);
            }
            applyAttributes();
        }
    });
    $("#editorBrightnessAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = $("#editorBrightnessAction").val();
            brightness = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                brightness = 1-parseFloat("0."+number);
            }
            applyAttributes();
        }
    });

    $("#editorContrastAction").off("input").on("input", function (e) {
        e.preventDefault();

        if ($("#editorContrastAction").val() === "0") {
            $("#contrastTick").css("display", "none");
        } else {
            $("#contrastTick").css("display", "block");
        }
    });
    $("#contrastTick").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = "0";
            $("#editorContrastAction").val(number);
            $("#contrastTick").css("display", "none");
            contrast = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                contrast = 1-parseFloat("0."+number);
            }
            applyAttributes();
        }
    });
    $("#editorContrastAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = $("#editorContrastAction").val();
            contrast = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                contrast = 1-parseFloat("0."+number);
            }
            applyAttributes();
        }
    });

    $("#editorSaturationAction").off("input").on("input", function (e) {
        e.preventDefault();

        if ($("#editorSaturationAction").val() === "0") {
            $("#saturationTick").css("display", "none");
        } else {
            $("#saturationTick").css("display", "block");
        }
    });
    $("#saturationTick").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = "0";
            $("#editorSaturationAction").val(number);
            $("#saturationTick").css("display", "none");
            saturation = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                saturation = 1-parseFloat("0."+number);
            }
            applyAttributes();
        }
    });
    $("#editorSaturationAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = $("#editorSaturationAction").val();
            saturation = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                saturation = 1-parseFloat("0."+number);
            }
            applyAttributes();
        }
    });

    $("#editorSharpnessAction").off("input").on("input", function (e) {
        e.preventDefault();

        if ($("#editorSharpnessAction").val() === "1") {
            $("#sharpnessTick").css("display", "none");
        } else {
            $("#sharpnessTick").css("display", "block");
        }
    });
    $("#sharpnessTick").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = "0";
            $("#editorSharpnessAction").val(number);
            $("#sharpnessTick").css("display", "none");
            sharpness = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                sharpness = 1-parseFloat("0."+number);
            }
            applyAttributes();
        }
    });
    $("#editorSharpnessAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = $("#editorSharpnessAction").val();
            sharpness = parseFloat(number+".0");
            applyAttributes();
        }
    });

    function applyAttributes() {
        disableButtons();
        $("#editorSpinner").css("display", "block");
        $("#editorCloseActionButton").css("display", "none");

        shashin.printMessageToConsole("--------------",{tag:"editor"});
        shashin.printMessageToConsole("Applying attributes for preview",{tag:"editor"});
        shashin.printMessageToConsole("Rotation: "+rotation,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
        shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
        shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
        shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});
        shashin.printMessageToConsole("sharpness: "+sharpness,{tag:"editor"});

        if (Util.webglSupport() === true) {
            if ($("#glcanvas").length > 0) {
                $("#glcanvas").remove();
            }

            const canvas = document.createElement("canvas");
            $(canvas).attr("id", "glcanvas");
            document.body.appendChild(canvas);

            const img = new Image();
            img.src = "/api/v1/image/original/" + editMetadataObj.id + "?v=shashin" + uuidv4();
            img.onload = () => {
                canvas.width = img.width;
                canvas.height = img.height;

                setupImageAdjustments(img, canvas, brightness, contrast, saturation, sharpness).then(() => {
                    updateTransform(false);
                    enableButtons();
                    $("#editorSpinner").css("display", "none");
                    $("#editorCloseActionButton").css("display", "block");
                });
            };
        } else {
            // Make network call to transform: inputs - brightness, contrast, and saturation
            shashin.processEditedPreviewThumbnail(editMetadataObj.id, editMetadataObj.path, brightness, contrast, saturation, sharpness, function (data) {
                if (data !== null) {
                    shashin.printMessageToConsole("--------------",{tag:"editor"});
                    shashin.printMessageToConsole("Editing time: " + data.totalTimeMS + "ms", {tag: "editor"});
                    // console.log("Total time editing image: "+data.totalTimeMS+"ms");
                    $("#editShashinImage").attr("src", "data:image/jpg;base64," + data.image);
                    updateTransform(false);
                    enableButtons();
                    $("#editorSpinner").css("display", "none");
                    $("#editorCloseActionButton").css("display", "block");
                }
            });
        }
    }

    function applyDefaultTransformations() {
        rotation = 0;
        isFlippedHorizontally = false;
        isFlippedVertically = false;
        brightness = 1.0;
        contrast = 1.0;
        saturation = 1.0;
        sharpness = 1.0;

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
        if (brightness === 1.0) {
            $("#brightnessTick").css("display", "none");
        } else {
            $("#brightnessTick").css("display", "block");
        }
        if (brightness >= 1.0) {
            $("#editorBrightnessAction").val(parseInt(getDigitsAfterDot(brightness)));
        } else {
            $("#editorBrightnessAction").val(-parseInt(getDigitsAfterDot(1 - brightness)));
        }

        if (editMetadataObj.hasOwnProperty("contrast") && editMetadataObj.contrast !== null) {
            contrast = parseFloat(editMetadataObj.contrast);
        }
        if (contrast === 1.0) {
            $("#contrastTick").css("display", "none");
        } else {
            $("#contrastTick").css("display", "block");
        }
        if (contrast >= 1.0) {
            $("#editorContrastAction").val(parseInt(getDigitsAfterDot(contrast)));
        } else {
            $("#editorContrastAction").val(-parseInt(getDigitsAfterDot(1 - contrast)));
        }

        if (editMetadataObj.hasOwnProperty("saturation") && editMetadataObj.saturation !== null) {
            saturation = parseFloat(editMetadataObj.saturation);
        }
        if (saturation === 1.0) {
            $("#saturationTick").css("display", "none");
        } else {
            $("#saturationTick").css("display", "block");
        }
        if (saturation >= 1.0) {
            $("#editorSaturationAction").val(parseInt(getDigitsAfterDot(saturation)));
        } else {
            $("#editorSaturationAction").val(-parseInt(getDigitsAfterDot(1 - saturation)));
        }

        if (editMetadataObj.hasOwnProperty("sharpness") && editMetadataObj.sharpness !== null) {
            sharpness = parseFloat(editMetadataObj.sharpness);
        }
        if (sharpness === 1.0) {
            $("#sharpnessTick").css("display", "none");
        } else {
            $("#sharpnessTick").css("display", "block");
        }
        if (sharpness >= 1.0) {
            $("#editorSharpnessAction").val(parseInt(sharpness));
        }

        // Apply transformations
        applyAttributes();
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
            if ($("#glcanvas").length > 0) {
                $("#glcanvas").remove();
            }

            applyDefaultTransformations();

            updateTransform(false);

            showModule();

            if (brightness === 1.0) {
                $("#brightnessTick").css("display", "none");
            }
            if (contrast === 1.0) {
                $("#contrastTick").css("display", "none");
            }
            if (saturation === 1.0) {
                $("#saturationTick").css("display", "none");
            }
            if (sharpness === 1.0) {
                $("#sharpnessTick").css("display", "none");
            }

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
            if ($("#glcanvas").length > 0) {
                $("#glcanvas").remove();
            }
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
            sharpness = 1.0;
            $("#editorSharpnessAction").val(0);

            $("#brightnessTick").css("display", "none");
            $("#contrastTick").css("display", "none");
            $("#saturationTick").css("display", "none");
            $("#sharpnessTick").css("display", "none");

            shashin.printMessageToConsole("--------------",{tag:"editor"});
            shashin.printMessageToConsole("Restoring attributes",{tag:"editor"});
            shashin.printMessageToConsole("Rotation: "+rotation,{tag:"editor"});
            shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
            shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
            shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
            shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
            shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});
            shashin.printMessageToConsole("sharpness: "+sharpness,{tag:"editor"});

            shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, rotation, isFlippedHorizontally, isFlippedVertically, brightness, contrast, saturation, sharpness, true,  function (success) {
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
                    sharpness = 1.0;
                    $("#editorSharpnessAction").val(0);

                    editMetadataObj.brightness = brightness;
                    editMetadataObj.contrast = contrast;
                    editMetadataObj.saturation = saturation;
                    editMetadataObj.sharpness = sharpness;
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

        shashin.printMessageToConsole("--------------",{tag:"editor"});
        shashin.printMessageToConsole("Saving attributes",{tag:"editor"});
        shashin.printMessageToConsole("Rotation: "+rotation,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
        shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
        shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
        shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});
        shashin.printMessageToConsole("sharpness: "+sharpness,{tag:"editor"});

        shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, normalizedRotation(rotation), isFlippedHorizontally, isFlippedVertically, brightness, contrast, saturation, sharpness, false,  function (success) {
            shashin.printMessageToConsole("Edited metadata: "+success,{tag:"editor"});

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
                editMetadataObj.sharpness = sharpness;
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
            "editorSaturationIcon",
            "editorSharpnessAction",
            "editorSharpnessIcon"
        ];

        buttonIds.forEach(id => {
            $('#' + id)
                .prop('disabled', false)
                .css({'color': "#FFFFFF", "text-shadow": "#EDEBEB 2px 2px 5px"});
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
            "editorSaturationIcon",
            "editorSharpnessAction",
            "editorSharpnessIcon"
        ];

        buttonIds.forEach(id => {
            $('#' + id)
                .prop('disabled', true)
                .css({'color': "#808080", "text-shadow": "#969595 2px 2px 5px"});
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

        if ($("#glcanvas").length > 0) {
            $("#glcanvas").remove();
        }

        rotation = 0;
        brightness = 1.0;
        contrast = 1.0;
        saturation = 1.0;
        sharpness = 1.0;
        isFlippedHorizontally = false;
        isFlippedVertically = false;

        document.body.style.overflowY = 'auto';
    }
}