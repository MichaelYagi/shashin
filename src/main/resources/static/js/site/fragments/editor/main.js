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

    // console.log("editMetadataObj");
    // console.log(editMetadataObj.rotation);
    // console.log(editMetadataObj.flipHorizontally);
    // console.log(editMetadataObj.flipVertically);
    // console.log(editMetadataObj.brightness);
    // console.log(editMetadataObj.contrast);
    // console.log(editMetadataObj.saturation);
    // console.log(editMetadataObj.sharpness);

    let originalRotation = (editMetadataObj.hasOwnProperty("rotation") && editMetadataObj.rotation !== null) ? editMetadataObj.rotation : rotation;
    let originalIsFlippedHorizontally = (editMetadataObj.hasOwnProperty("flipHorizontally") && editMetadataObj.flipHorizontally !== null) ? editMetadataObj.flipHorizontally : isFlippedHorizontally;
    let originalIsFlippedVertically = (editMetadataObj.hasOwnProperty("flipVertically") && editMetadataObj.flipVertically !== null) ? editMetadataObj.flipVertically : isFlippedVertically;
    let originalBrightness = (editMetadataObj.hasOwnProperty("brightness") && editMetadataObj.brightness !== null) ? editMetadataObj.brightness : brightness;
    let originalContrast = (editMetadataObj.hasOwnProperty("contrast") && editMetadataObj.contrast !== null) ? editMetadataObj.contrast : contrast;
    let originalSaturation = (editMetadataObj.hasOwnProperty("saturation") && editMetadataObj.saturation !== null) ? editMetadataObj.saturation : saturation;
    let originalSharpness = (editMetadataObj.hasOwnProperty("sharpness") && editMetadataObj.sharpness !== null) ? editMetadataObj.sharpness : sharpness;

    // Loading spinner
    // styleTopControl("#editorSpinner", "2em", "35px", "right", "25px");
    $("#editorContainer").css("display", "block");
    $("#editorToolContainer").css("display", "block");
    $("#editorSpinner").css("display", "block");
    $("#editorCloseActionButton").prop('disabled', true).css({"pointer-events": "none"});
    $("#editorCloseAction").css({
        "color": "#808080",
        "text-shadow": "#969595 2px 2px 5px"
    });
    applyDefaultTransformations();

    // console.log("initializeEditor after applying transitions");
    // console.log("rotation:"+rotation);
    // console.log("isFlippedHorizontally:"+isFlippedHorizontally);
    // console.log("isFlippedVertically:"+isFlippedVertically);
    // console.log("brightness:"+brightness);
    // console.log("contrast:"+contrast);
    // console.log("saturation:"+saturation);
    // console.log("sharpness:"+sharpness);

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

    showModule();
    toggleResetSaveButtons();
    $("#editorContainer").css("display", "block");

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
            '#editorToolContainer, ' +
            '#editorTitle'
        ).length) {
            if (isSpinnerHidden()) {
                hideModule();
            }
        }
    });

    // Key actions
    $("body").off("keydown").on("keydown", async function (e) {
        if (isSpinnerHidden()) {
            // Close editor
            if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
                e.preventDefault();
                shashin.closeToastMessages({tag:"editorCurrentSettings"});
                $("#editorCloseAction").click();
            }

            // Rotate left
            if (e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37) {
                e.preventDefault();
                if ($("#editorRotateLeftActionButton").css("pointer-events") !== "none") {
                    shashin.closeToastMessages({tag:"editorCurrentSettings"});
                    $("#editorRotateLeftActionButton").click();
                }
            }

            // Flip horizontally
            if (e.key === "ArrowUp" || e.code === "ArrowUp" || e.which === 38 || e.keyCode === 38) {
                e.preventDefault();
                if ($("#editorFlipHorizontalActionButton").css("pointer-events") !== "none") {
                    shashin.closeToastMessages({tag:"editorCurrentSettings"});
                    $("#editorFlipHorizontalActionButton").click();
                }
            }

            // Rotate right
            if (e.key === "ArrowRight" || e.code === "ArrowRight" || e.which === 39 || e.keyCode === 39) {
                e.preventDefault();
                if ($("#editorRotateRightActionButton").css("pointer-events") !== "none") {
                    shashin.closeToastMessages({tag:"editorCurrentSettings"});
                    $("#editorRotateRightActionButton").click();
                }
            }

            // Flip vertically
            if (e.key === "ArrowDown" || e.code === "ArrowDown" || e.which === 40 || e.keyCode === 40) {
                e.preventDefault();
                if ($("#editorFlipVerticalActionButton").css("pointer-events") !== "none") {
                    shashin.closeToastMessages({tag:"editorCurrentSettings"});
                    $("#editorFlipVerticalActionButton").click();
                }
            }

            // Reset
            if (e.key === "r" || e.code === "KeyR" || e.which === 82 || e.keyCode === 82) {
                e.preventDefault();
                if ($("#editorResetActionButton").css("pointer-events") !== "none") {
                    shashin.closeToastMessages({tag:"editorCurrentSettings"});
                    $("#editorResetActionButton").click();
                }
            }

            // Restore
            if (e.key === "o" || e.code === "KeyO" || e.which === 79 || e.keyCode === 79) {
                e.preventDefault();
                if ($("#editorRestoreActionButton").css("pointer-events") !== "none") {
                    shashin.closeToastMessages({tag:"editorCurrentSettings"});
                    $("#editorRestoreActionButton").click();
                }
            }

            // Save
            if (e.key === "s" || e.code === "KeyS" || e.which === 83 || e.keyCode === 83) {
                e.preventDefault();
                if ($("#editorSaveActionButton").css("pointer-events") !== "none") {
                    shashin.closeToastMessages({tag:"editorCurrentSettings"});
                    $("#editorSaveActionButton").click();
                }
            }
        }
    });

    $("#editorCloseAction").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if (isSpinnerHidden()) {
            hideModule();
        }
    });

    // Slider icon
    $("#editorBrightnessIcon").off("click").on('click', function(event) {
        event.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if ($("#editorBrightnessAction").val() === "0") {
            $("#brightnessTick").css("display", "none");
        } else {
            $("#brightnessTick").css("display", "block");
        }
    });
    $("#brightnessTick").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if ($("#editorContrastAction").val() === "0") {
            $("#contrastTick").css("display", "none");
        } else {
            $("#contrastTick").css("display", "block");
        }
    });
    $("#contrastTick").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if ($("#editorSaturationAction").val() === "0") {
            $("#saturationTick").css("display", "none");
        } else {
            $("#saturationTick").css("display", "block");
        }
    });
    $("#saturationTick").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if ($("#editorSharpnessAction").val() === "1") {
            $("#sharpnessTick").css("display", "none");
        } else {
            $("#sharpnessTick").css("display", "block");
        }
    });
    $("#sharpnessTick").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if (isSpinnerHidden()) {
            let number = $("#editorSharpnessAction").val();
            sharpness = parseFloat(number+".0");
            applyAttributes();
        }
        toggleResetSaveButtons();
    });

    $("#editorToolContainerTitleLink").off("click").on("click", function () {
        shashin.showToastMessage(
            shashin.getTranslatedValue("main.pages.lg.plugins.editor.modal.title"),
            "                <div>\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"col-md-6\"><strong>"+shashin.getTranslatedValue("main.pages.lg.plugins.editor.rotate")+"</strong></div>\n" +
            "                        <div class=\"col-md-3\" id=\"savedRotation\">"+highlight(normalizedRotation(originalRotation), 0)+"</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"col-md-6\"><strong>"+shashin.getTranslatedValue("main.pages.lg.plugins.editor.flipx")+"</strong></div>\n" +
            "                        <div class=\"col-md-3\" id=\"savedFlipX\">"+highlight(originalIsFlippedHorizontally, false)+"</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"col-md-6\"><strong>"+shashin.getTranslatedValue("main.pages.lg.plugins.editor.flipy")+"</strong></div>\n" +
            "                        <div class=\"col-md-3\" id=\"savedFlipY\">"+highlight(originalIsFlippedVertically, false)+"</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"col-md-6\"><strong>"+shashin.getTranslatedValue("main.pages.lg.plugins.editor.brightness")+"</strong></div>\n" +
            "                        <div class=\"col-md-3\" id=\"savedBrightness\">"+highlightNumber(originalBrightness)+"</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"col-md-6\"><strong>"+shashin.getTranslatedValue("main.pages.lg.plugins.editor.contrast")+"</strong></div>\n" +
            "                        <div class=\"col-md-3\" id=\"savedContrast\">"+highlightNumber(originalContrast)+"</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"col-md-6\"><strong>"+shashin.getTranslatedValue("main.pages.lg.plugins.editor.saturation")+"</strong></div>\n" +
            "                        <div class=\"col-md-3\" id=\"savedSaturation\">"+highlightNumber(originalSaturation)+"</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"col-md-6\"><strong>"+shashin.getTranslatedValue("main.pages.lg.plugins.editor.sharpness")+"</strong></div>\n" +
            "                        <div class=\"col-md-3\" id=\"savedSharpness\">"+highlightNumber(originalSharpness)+"</div>\n" +
            "                    </div>\n" +
            "                </div>",
            {
                icon: "bi-info-circle",
                autohide: false,
                placement: shashin.toast.placement.top.center,
                iconColor: "#777777",
                tag: "editorCurrentSettings"
            }
        );
    });

    function applyAttributes(showTransition = true) {
        disableButtons();
        $("#editorSpinner").css("display", "block");
        $("#editorCloseActionButton").prop('disabled', true).css({"pointer-events": "none"});
        $("#editorCloseAction").css({
            "color": "#808080",
            "text-shadow": "#969595 2px 2px 5px"
        });

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

                setupImageAdjustments(img, canvas, brightness, contrast, saturation, sharpness)
                .then((success) => {
                    if (success === true) {
                        updateTransform(showTransition);
                        enableButtons();
                        toggleResetSaveButtons();
                        $("#editorSpinner").css("display", "none");
                        $("#editorCloseActionButton").prop('disabled', false).css({"pointer-events": "auto"});
                        $("#editorCloseAction").css({
                            "color": "#FFFFFF",
                            "text-shadow": "#EDEBEB 2px 2px 5px"
                        });
                        $("#editShashinImage").css("display", "block");
                    } else {
                        shashin.printMessageToConsole("Error rendering image", {tag: "editor"});
                        fallbackRender(editMetadataObj.id, editMetadataObj.path, brightness, contrast, saturation, sharpness, showTransition);
                    }
                }).catch(err => {
                    shashin.printMessageToConsole("Error rendering image: " + err, {tag: "editor"});
                    fallbackRender(editMetadataObj.id, editMetadataObj.path, brightness, contrast, saturation, sharpness, showTransition);
                });
            };

            img.onerror = () => {
                shashin.printMessageToConsole("Error rendering image", {tag: "editor"});
                fallbackRender(editMetadataObj.id, editMetadataObj.path, brightness, contrast, saturation, sharpness, showTransition);
            };
        } else {
            shashin.printMessageToConsole("WebGL not present", {tag: "editor"});
            fallbackRender(editMetadataObj.id, editMetadataObj.path, brightness, contrast, saturation, sharpness, showTransition);
        }

        function fallbackRender(id, path, brightness, contrast, saturation, sharpness, showTransition = true) {
            // Make network call to transform: inputs - brightness, contrast, and saturation
            shashin.processEditedPreviewThumbnail(id, path, brightness, contrast, saturation, sharpness, function (data) {
                if (data !== null) {
                    shashin.printMessageToConsole("--------------",{tag:"editor"});
                    shashin.printMessageToConsole("Editing time: " + data.totalTimeMS + "ms", {tag: "editor"});
                    // console.log("Total time editing image: "+data.totalTimeMS+"ms");

                    const img = $("#editShashinImage");
                    img.off("load").on("load", () => {
                        updateTransform(showTransition);
                        enableButtons();
                        toggleResetSaveButtons();
                        $("#editorSpinner").css("display", "none");
                        $("#editorCloseActionButton").prop('disabled', false).css({"pointer-events": "auto"});
                        $("#editorCloseAction").css({
                            "color": "#FFFFFF",
                            "text-shadow": "#EDEBEB 2px 2px 5px"
                        });
                    });
                    $("#editShashinImage").attr("src", "data:image/jpg;base64," + data.image);
                }
            });
        }
    }

    function applyDefaultTransformations(showTransition = true) {
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
        applyAttributes(showTransition);
    }

    $("#editorRotateRightActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if (isSpinnerHidden()) {
            rotation += 90;
            updateTransform();
        }
        toggleResetSaveButtons();
    });

    $("#editorRotateLeftActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if (isSpinnerHidden()) {
            rotation -= 90;
            updateTransform();
        }
        toggleResetSaveButtons();
    });

    $("#editorFlipVerticalActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if (isSpinnerHidden()) {
            if ($("#glcanvas").length > 0) {
                $("#glcanvas").remove();
            }

            applyDefaultTransformations(true);

            updateTransform(false);

            // showModule();

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
        }
        toggleResetSaveButtons();
    });

    $("#editorRestoreActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        shashin.closeToastMessages({tag:"editorCurrentSettings"});

        if (isSpinnerHidden()) {
            // Just preview
            rotation = 0;
            isFlippedHorizontally = false;
            isFlippedVertically = false;
            brightness = 1.0;
            $("#editorBrightnessAction").val(0);
            $("#brightnessTick").css("display", "none");
            contrast = 1.0;
            $("#editorContrastAction").val(0);
            $("#contrastTick").css("display", "none");
            saturation = 1.0;
            $("#editorSaturationAction").val(0);
            $("#saturationTick").css("display", "none");
            sharpness = 1.0;
            $("#editorSharpnessAction").val(0);
            $("#sharpnessTick").css("display", "none");

            applyAttributes();

            updateTransform(false);

            // Auto saves
            // if ($("#glcanvas").length > 0) {
            //     $("#glcanvas").remove();
            // }
            // disableButtons();
            // $("#editorSpinner").css("display", "block");
            // $("#editorCloseActionButton").prop('disabled', true).css({"pointer-events": "none"});
            // $("#editorCloseAction").css({
            //     "color": "#808080",
            //     "text-shadow": "#969595 2px 2px 5px"
            // });
            //
            // rotation = 0;
            // isFlippedHorizontally = false;
            // isFlippedVertically = false;
            // brightness = 1.0;
            // $("#editorBrightnessAction").val(0);
            // contrast = 1.0;
            // $("#editorContrastAction").val(0);
            // saturation = 1.0;
            // $("#editorSaturationAction").val(0);
            // sharpness = 1.0;
            // $("#editorSharpnessAction").val(0);
            //
            // originalRotation = rotation;
            // originalIsFlippedHorizontally = isFlippedHorizontally;
            // originalIsFlippedVertically = isFlippedVertically;
            // originalBrightness = brightness;
            // originalContrast = contrast;
            // originalSaturation = saturation;
            // originalSharpness = sharpness;
            //
            // $("#brightnessTick").css("display", "none");
            // $("#contrastTick").css("display", "none");
            // $("#saturationTick").css("display", "none");
            // $("#sharpnessTick").css("display", "none");
            //
            // shashin.printMessageToConsole("--------------",{tag:"editor"});
            // shashin.printMessageToConsole("Restoring attributes",{tag:"editor"});
            // shashin.printMessageToConsole("Rotation: "+rotation,{tag:"editor"});
            // shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
            // shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
            // shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
            // shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
            // shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});
            // shashin.printMessageToConsole("sharpness: "+sharpness,{tag:"editor"});
            //
            // shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, rotation, isFlippedHorizontally, isFlippedVertically, brightness, contrast, saturation, sharpness, true,  function (success) {
            //     shashin.printMessageToConsole("Restoring edited metadata:"+success,{tag:"editor"});
            //
            //     if (success === true) {
            //         rotation = 0;
            //         isFlippedHorizontally = false;
            //         isFlippedVertically = false;
            //         brightness = 1.0;
            //         $("#editorBrightnessAction").val(0);
            //         contrast = 1.0;
            //         $("#editorContrastAction").val(0);
            //         saturation = 1.0;
            //         $("#editorSaturationAction").val(0);
            //         sharpness = 1.0;
            //         $("#editorSharpnessAction").val(0);
            //
            //         originalRotation = rotation;
            //         originalIsFlippedHorizontally = isFlippedHorizontally;
            //         originalIsFlippedVertically = isFlippedVertically;
            //         originalBrightness = brightness;
            //         originalContrast = contrast;
            //         originalSaturation = saturation;
            //         originalSharpness = sharpness;
            //
            //         editMetadataObj.brightness = brightness;
            //         editMetadataObj.contrast = contrast;
            //         editMetadataObj.saturation = saturation;
            //         editMetadataObj.sharpness = sharpness;
            //         editMetadataObj.rotation = rotation;
            //         editMetadataObj.flipHorizontally = isFlippedHorizontally;
            //         editMetadataObj.flipVertically = isFlippedVertically;
            //
            //         applyDefaultTransformations();
            //
            //         updateTransform(false);
            //
            //         shashin.showToastMessage(shashin.getTranslatedValue("main.pages.map.modal.restored"), shashin.getTranslatedValue("main.pages.map.modal.restored"), {
            //             icon: "bi-info-circle",
            //             iconColor: "#777777",
            //             delay: 2000,
            //             borderColor: "success"
            //         });
            //     } else {
            //         shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail.body"), shashin.getTranslatedValue("main.toast.account.profile.fail.body"), {
            //             icon: "bi-exclamation-triangle",
            //             iconColor: "#FF0000",
            //             borderColor:"danger"
            //         });
            //     }
            //
            //     enableButtons();
            //     toggleResetSaveButtons();
            //     $("#editorSpinner").css("display", "none");
            //     $("#editorCloseActionButton").prop('disabled', false).css({"pointer-events": "auto"});
            //     $("#editorCloseAction").css({
            //         "color": "#FFFFFF",
            //         "text-shadow": "#EDEBEB 2px 2px 5px"
            //     });
            // });
        }
    });

    $("#editorSaveActionButton").off("click").on("click", function (e) {
        e.preventDefault();
        saveImage();
    });

    function saveImage() {
        disableButtons();
        $("#editorSpinner").css("display", "block");
        $("#editorCloseActionButton").prop('disabled', true).css({"pointer-events": "none"});
        $("#editorCloseAction").css({
            "color": "#808080",
            "text-shadow": "#969595 2px 2px 5px"
        });

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
            $("#editorCloseActionButton").prop('disabled', true).css({"pointer-events": "none"});
            $("#editorCloseAction").css({
                "color": "#808080",
                "text-shadow": "#969595 2px 2px 5px"
            });
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
        requestAnimationFrame(() => {
            $("#editShashinImage").css(style);
        });
    }

    function highlight(value, defaultValue) {
        const str = value.toString();
        return (value === defaultValue)
            ? str
            : `<span style="color:#0a53be">${str}</span>`;
    }

    function highlightNumber(num, defaultValue = 1.0) {
        const fixed = parseFloat(num).toFixed(1);
        return (parseFloat(fixed) === defaultValue)
            ? fixed
            : `<span style="color:#0a53be">${fixed}</span>`;
    }

    function showModule() {
        $("#editorMedia").html("<img id='editShashinImage' src='/api/v1/image/" + editMetadataObj.id + "?v=" + uuidv4() + "' style='display: none;'>");
        $("#editShashinImage").on('load', function () {
            const flipX = isFlippedHorizontally ? -1 : 1;
            const flipY = isFlippedVertically ? -1 : 1;
            const isEvenRotation = rotation % 180 === 0;

            const targetWidth = isEvenRotation ? $(window).innerWidth() + 1 : $(window).innerHeight() + 1;
            const targetHeight = isEvenRotation ? $(window).innerHeight() + 1 : $(window).innerWidth() + 1;

            $("#editShashinImage").css({
                position: "absolute",
                top: "50%",
                left: "50%",
                transform: `translate(-50%, -50%) rotate(${rotation}deg) scale(${flipX}, ${flipY})`,
                transformOrigin: "center center",
                maxWidth: targetWidth,
                maxHeight: targetHeight
            });

            $("#editorToolContainer").css("display", "block");
            $("#editorContainer").css("display", "block");
        });
    }

    function hideModule() {
        // Show lightgallery toolbar
        $(".lg-toolbar").css("display", "block");
        $(".lg-outer").removeClass("lg-hide-items");

        // $(".lg-object.lg-image").on('load', function() {
        $("#editorContainer").css("display", "none");
        $("#editorToolContainer").css("display", "none");

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

        shashin.closeToastMessages({tag:"editorCurrentSettings"});

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