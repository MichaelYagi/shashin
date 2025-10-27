function initializeEditor(editMetadataObj, lgIndex) {
    // console.log("--------------------");
    // console.log(editMetadataObj.id);
    // console.log(editMetadataObj);
    // console.log(lgIndex);
    $("#editorContainer").css('cursor', 'auto');
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

    let originalRotation = (editMetadataObj.hasOwnProperty("rotation") && editMetadataObj.rotation !== null) ? editMetadataObj.rotation : rotation;
    let originalIsFlippedHorizontally = (editMetadataObj.hasOwnProperty("flipHorizontally") && editMetadataObj.flipHorizontally !== null) ? editMetadataObj.flipHorizontally : isFlippedHorizontally;
    let originalIsFlippedVertically = (editMetadataObj.hasOwnProperty("flipVertically") && editMetadataObj.flipVertically !== null) ? editMetadataObj.flipVertically : isFlippedVertically;
    let originalBrightness = (editMetadataObj.hasOwnProperty("brightness") && editMetadataObj.brightness !== null) ? editMetadataObj.brightness : brightness;
    let originalContrast = (editMetadataObj.hasOwnProperty("contrast") && editMetadataObj.contrast !== null) ? editMetadataObj.contrast : contrast;
    let originalSaturation = (editMetadataObj.hasOwnProperty("saturation") && editMetadataObj.saturation !== null) ? editMetadataObj.saturation : saturation;
    let originalSharpness = (editMetadataObj.hasOwnProperty("sharpness") && editMetadataObj.sharpness !== null) ? editMetadataObj.sharpness : sharpness;

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
    let thirdRowMenuHeightWidth = applyStyles(thirdRowButtons, sideValue, "right");

    // Fourth row buttons
    sideValue = 10+20;
    let fourthRowButtons = [
        { id: "#editorContrastActionButton", fontSize: "2rem", top: "200px" }
    ];
    let fourthRowMenuHeightWidth = applyStyles(fourthRowButtons, sideValue, "right");

    // Fifth row buttons
    sideValue = 10+20;
    let fifthRowButtons = [
        { id: "#editorSaturationActionButton", fontSize: "2rem", top: "270px" }
    ];
    let fifthRowMenuHeightWidth = applyStyles(fifthRowButtons, sideValue, "right");

    // Sixth row buttons
    sideValue = 10+20;
    let sixthRowButtons = [
        { id: "#editorSharpnessActionButton", fontSize: "2rem", top: "340px" }
    ];
    let sixthRowMenuHeightWidth = applyStyles(sixthRowButtons, sideValue, "right");

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

    $("#editorBlock").css({
        "position": "absolute",
        "height": (blockHeight - 589) + "px",
        "width": (blockWidth + 5) + "px",
        "right": "0"
    });

    showModule();
    toggleResetSaveButtons();

    $("#editorContainer").css("display", "block");

    // $(".centerFit").css({
    //     "max-width": $(window).innerWidth() + 1,
    //     "height": "auto",
    //     "max-height": $(window).innerHeight() + 1,
    //     "position": "absolute",
    //     "top": "50%",
    //     "left": "50%",
    //     "transform": "translate(-50%, -50%)"
    // });

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
            if (isSpinnerHidden()) {
                hideModule();
            }
        }
    });

    // Key actions
    $("body").off("keydown").on("keydown", async function (e) {
        if (isSpinnerHidden()) {
            // Close editor - setTimeout 100 so it doesn't also close LightGallery slide
            if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
                e.preventDefault();
                setTimeout(hideModule, 100);
            }

            // Rotate left
            if (e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37) {
                e.preventDefault();

                setTimeout(function () {
                    if (isSpinnerHidden()) {
                        rotation -= 90;
                        updateTransform();
                    }
                    toggleResetSaveButtons();
                }, 100);
            }

            // Flip horizontally
            if (e.key === "ArrowUp" || e.code === "ArrowUp" || e.which === 38 || e.keyCode === 38) {
                e.preventDefault();
                if (isSpinnerHidden()) {
                    if (normalizedRotation(rotation) === 90 || normalizedRotation(rotation) === 270) {
                        isFlippedHorizontally = !isFlippedHorizontally;
                    } else {
                        isFlippedVertically = !isFlippedVertically;
                    }
                    updateTransform();
                }
                toggleResetSaveButtons();
            }

            // Rotate right
            if (e.key === "ArrowRight" || e.code === "ArrowRight" || e.which === 39 || e.keyCode === 39) {
                e.preventDefault();

                setTimeout(function () {
                    if (isSpinnerHidden()) {
                        rotation += 90;
                        updateTransform();
                    }
                    toggleResetSaveButtons();
                }, 100);
            }

            // Flip vertically
            if (e.key === "ArrowDown" || e.code === "ArrowDown" || e.which === 40 || e.keyCode === 40) {
                e.preventDefault();
                if (isSpinnerHidden()) {
                    if (normalizedRotation(rotation) === 90 || normalizedRotation(rotation) === 270) {
                        isFlippedVertically = !isFlippedVertically;
                    } else {
                        isFlippedHorizontally = !isFlippedHorizontally;
                    }
                    updateTransform();
                }
                toggleResetSaveButtons();
            }

            // Save
            if (e.key === "s" || e.code === "KeyS" || e.which === 83 || e.keyCode === 83) {
                e.preventDefault();
                saveImage();
            }
        }
    });

    $("#editorCloseAction").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            hideModule();
        }
    });

    function updateTransform(showTransition = true) {
        const flipX = isFlippedHorizontally ? -1 : 1;
        const flipY = isFlippedVertically ? -1 : 1;
        const isEvenRotation = rotation % 180 === 0;

        // Calculate target dimensions
        const targetWidth = isEvenRotation ? $(window).innerWidth() + 1 : $(window).innerHeight() + 1;
        const targetHeight = isEvenRotation ? $(window).innerHeight() + 1 : $(window).innerWidth() + 1;

        // Apply transition styles
        const style = {
            transform: `translate(-50%, -50%) rotate(${rotation}deg) scale(${flipX}, ${flipY})`,
            maxWidth: targetWidth,
            maxHeight: targetHeight,
            transition: showTransition ?
                "transform 0.3s ease-in-out, max-width 0.3s ease-in-out, max-height 0.3s ease-in-out"
                : "none"
        };

        // Apply styles with a frame delay to ensure smooth transition
        window.requestAnimationFrame(() => {
            $("#editShashinImage").css(style);
        });

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
        toggleResetSaveButtons();
    });

    $("#editorContrastIcon").off("click").on('click', function(event) {
        event.preventDefault();

        $("#contrastTick").css("display", "none");
        if (isSpinnerHidden()) {
            contrast = 1.0;
            $("#editorContrastAction").val(0);
            applyAttributes();
        }
        toggleResetSaveButtons();
    });

    $("#editorSaturationIcon").off("click").on('click', function(event) {
        event.preventDefault();

        $("#saturationTick").css("display", "none");
        if (isSpinnerHidden()) {
            saturation = 1.0;
            $("#editorSaturationAction").val(0);
            applyAttributes();
        }
        toggleResetSaveButtons();
    });

    $("#editorSharpnessIcon").off("click").on('click', function(event) {
        event.preventDefault();

        $("#sharpnessTick").css("display", "none");
        if (isSpinnerHidden()) {
            sharpness = 1.0;
            $("#editorSharpnessAction").val(1);
            applyAttributes();
        }
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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
            sharpness = parseFloat(number+".0");
            applyAttributes();
        }
        toggleResetSaveButtons();
    });
    $("#editorSharpnessAction").off("change").on("change", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = $("#editorSharpnessAction").val();
            sharpness = parseFloat(number+".0");
            applyAttributes();
        }
        toggleResetSaveButtons();
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
                    toggleResetSaveButtons();
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
                    toggleResetSaveButtons();
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
        toggleResetSaveButtons();
    });

    $("#editorRotateLeftActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        if (isSpinnerHidden()) {
            rotation -= 90;
            updateTransform();
        }
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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
        toggleResetSaveButtons();
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

            $("#editorContainer").css("display", "block");

            // $(".centerFit").css({
            //     "max-width": $(window).innerWidth() + 1,
            //     "height": "auto",
            //     "max-height": $(window).innerHeight() + 1,
            //     "position": "absolute",
            //     "top": "50%",
            //     "left": "50%",
            //     "transform": "translate(-50%, -50%)"
            // });
        }
        toggleResetSaveButtons();
    });

    $("#editorRestoreActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
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

            originalRotation = rotation;
            originalIsFlippedHorizontally = isFlippedHorizontally;
            originalIsFlippedVertically = isFlippedVertically;
            originalBrightness = brightness;
            originalContrast = contrast;
            originalSaturation = saturation;
            originalSharpness = sharpness;

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

                    originalRotation = rotation;
                    originalIsFlippedHorizontally = isFlippedHorizontally;
                    originalIsFlippedVertically = isFlippedVertically;
                    originalBrightness = brightness;
                    originalContrast = contrast;
                    originalSaturation = saturation;
                    originalSharpness = sharpness;

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
                toggleResetSaveButtons();
                $("#editorSpinner").css("display", "none");
                $("#editorCloseActionButton").css("display", "block");
                // hideModule();
            });
        }
    });

    $("#editorSaveActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        saveImage();
    });

    function saveImage() {
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
            toggleResetSaveButtons();
            $("#editorSpinner").css("display", "none");
            $("#editorCloseActionButton").css("display", "block");
            hideModule();
        });
    }

    function enableButtons() {
        const buttonIds = [
            "editorSaveAction",
            "editorBrightnessAction",
            "editorContrastAction",
            "editorSaturationAction",
            "editorRestoreAction",
            "editorRotateRightAction",
            "editorRotateLeftAction",
            "editorFlipHorizontalAction",
            "editorFlipVerticalAction",
            "editorResetAction",
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
            "editorRotateRightAction",
            "editorRotateLeftAction",
            "editorFlipHorizontalAction",
            "editorFlipVerticalAction",
            "editorResetAction",
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
        $("#editorMedia").html("<img class='centerFit' id='editShashinImage' src='/api/v1/image/" + editMetadataObj.id + "?v=" + uuidv4() + "'>");
        $("#editShashinImage").on('load', function () {
            const vv = window.visualViewport;
            const vw = vv ? Math.round(vv.width) : Math.round(window.innerWidth);
            const vh = vv ? Math.round(vv.height) : Math.round(window.innerHeight);
            const vLeft = vv ? (vv.offsetLeft || 0) : 0;
            const vTop = vv ? (vv.offsetTop || 0) : 0;

            // center in pixel coords relative to the visible viewport
            const centerX = Math.round(vLeft + vw / 2);
            const centerY = Math.round(vTop + vh / 2);

            $(".centerFit").css({
                "max-width": (vw + 1) + "px",
                "height": "auto",
                "max-height": (vh + 1) + "px",
                "position": "fixed",
                "left": centerX + "px",
                "top": centerY + "px",
                "transform": "translate(-50%, -50%)",
                "pointer-events": "none"   // let clicks pass through to controls
            });
            $("#editorToolContainer").css("display", "block");
            $("#editorBlock").css("display", "block");
            $("#editorContainer").css("display", "block");
            $("#editorMedia").css("display", "block");
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

    function normalizeFlipRotation(isFlippedHorizontallyInput, isFlippedVerticallyInput, rotationInput) {
        let retVal = {
            isFlippedVertically: isFlippedVerticallyInput,
            isFlippedHorizontally: isFlippedHorizontallyInput,
            rotation: normalizedRotation(rotationInput)
        };

        // FlipX + FlipY Normalizations
        if (isFlippedHorizontallyInput && isFlippedVerticallyInput && normalizedRotation(rotationInput) === 0) {
            retVal = {
                isFlippedVertically: false,
                isFlippedHorizontally: false,
                rotation: 180
            };
        } else if (isFlippedHorizontallyInput && isFlippedVerticallyInput && normalizedRotation(rotationInput) === 90) {
            retVal = {
                isFlippedVertically: false,
                isFlippedHorizontally: false,
                rotation: 270
            };
        } else if (isFlippedHorizontallyInput && isFlippedVerticallyInput && normalizedRotation(rotationInput) === 180) {
            retVal = {
                isFlippedVertically: false,
                isFlippedHorizontally: false,
                rotation: 0
            };
        } else if (isFlippedHorizontallyInput && isFlippedVerticallyInput && normalizedRotation(rotationInput) === 270) {
            retVal = {
                isFlippedVertically: false,
                isFlippedHorizontally: false,
                rotation: 90
            };
        }

        return retVal;
    }

    function toggleResetSaveButtons() {
        const normalizedDynamicValues = normalizeFlipRotation(isFlippedHorizontally, isFlippedVertically, rotation);
        const normalizedDynamicRotation = normalizedDynamicValues.rotation;
        const normalizedDynamicIsFlippedHorizontally = normalizedDynamicValues.isFlippedHorizontally;
        const normalizedDynamicIsFlippedVertically = normalizedDynamicValues.isFlippedVertically;

        const normalizedOriginalValues = normalizeFlipRotation(originalIsFlippedVertically, originalIsFlippedHorizontally, originalRotation);
        const normalizedOriginalRotation = normalizedOriginalValues.rotation;
        const normalizedOriginalIsFlippedHorizontally = normalizedOriginalValues.isFlippedHorizontally;
        const normalizedOriginalIsFlippedVertically = normalizedOriginalValues.isFlippedVertically;

        if (normalizedOriginalRotation === normalizedDynamicRotation &&
            normalizedOriginalIsFlippedHorizontally === normalizedDynamicIsFlippedHorizontally &&
            normalizedOriginalIsFlippedVertically === normalizedDynamicIsFlippedVertically &&
            parseFloat(originalBrightness) === parseFloat(brightness) &&
            parseFloat(originalContrast) === parseFloat(contrast) &&
            parseFloat(originalSaturation) === parseFloat(saturation) &&
            parseFloat(originalSharpness) === parseFloat(sharpness))
        {
            $("#editorResetActionButton")
                .prop('disabled', true)
                .css({
                    "pointer-events": "none"
                });
            $("#editorResetAction")
                .css({
                    "color": "#808080",
                    "text-shadow": "#969595 2px 2px 5px"
                });
            $("#editorSaveActionButton")
                .prop('disabled', true)
                .css({
                    "pointer-events": "none"
                });
            $("#editorSaveAction")
                .css({
                    "color": "#808080",
                    "text-shadow": "#969595 2px 2px 5px"
                });
        } else {
            $("#editorResetActionButton")
                .prop('disabled', false)
                .css({
                    "pointer-events": "auto"
                });
            $("#editorResetAction")
                .css({
                    "color": "#FFFFFF",
                    "text-shadow": "#EDEBEB 2px 2px 5px"
                });
            $("#editorSaveActionButton")
                .prop('disabled', false)
                .css({
                    "pointer-events": "auto"
                });
            $("#editorSaveAction")
                .css({
                    "color": "#FFFFFF",
                    "text-shadow": "#EDEBEB 2px 2px 5px"
                });
        }

        if (normalizedDynamicRotation === 0 &&
            normalizedDynamicIsFlippedHorizontally === false &&
            normalizedDynamicIsFlippedVertically === false &&
            brightness === 1 &&
            contrast === 1 &&
            saturation === 1 &&
            sharpness === 1
        ) {
            $("#editorRestoreActionButton")
                .prop('disabled', true)
                .css({
                    "pointer-events": "none"
                });
            $("#editorRestoreAction")
                .css({
                    "color": "#808080",
                    "text-shadow": "#969595 2px 2px 5px"
                });
        } else {
            $("#editorRestoreActionButton")
                .prop('disabled', false)
                .css({
                    "pointer-events": "auto"
                });
            $("#editorRestoreAction")
                .css({
                    "color": "#FFFFFF",
                    "text-shadow": "#EDEBEB 2px 2px 5px"
                });
        }
    }
}