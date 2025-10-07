function initializeEditor(editMetadataObj, lgIndex) {
    // console.log("--------------------");
    // console.log(editMetadataObj.id);
    // console.log(editMetadataObj);
    // console.log(lgIndex);

    let rotation = 0;
    let isFlippedHorizontally = false;
    let isFlippedVertically = false;

    let brightness = 1.0;
    $("#editorBrightnessAction").val(0);
    if (editMetadataObj.hasOwnProperty("brightness") && editMetadataObj.brightness !== null) {
        brightness = parseFloat(editMetadataObj.brightness);
        if (brightness >= 1.0) {
            $("#editorBrightnessAction").val(parseInt(getDigitsAfterDot(brightness)));
        } else {
            $("#editorBrightnessAction").val(-parseInt(getDigitsAfterDot(1-brightness)));
        }
    }

    let contrast = 1.0;
    $("#editorContrastAction").val(0);
    if (editMetadataObj.hasOwnProperty("contrast") && editMetadataObj.contrast !== null) {
        contrast = parseFloat(editMetadataObj.contrast);
        if (contrast >= 1.0) {
            $("#editorContrastAction").val(parseInt(getDigitsAfterDot(contrast)));
        } else {
            $("#editorContrastAction").val(-parseInt(getDigitsAfterDot(1-contrast)));
        }
    }

    const editMetadataId = editMetadataObj.id;

    function getDigitsAfterDot(num) {
        const parts = (Math.round(num*10)/10).toString().split(".");
        return parts[1] || "0";
    }

    // Title
    styleControl("#editorTitle", "2rem", "23px", "left", "30px");

    // Loading spinner
    styleControl("#editorSpinner", "2em", "35px", "right", "15px");

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
    sideValue = 13;
    const thirdRowButtons = [
        { id: "#editorBrightnessActionButton", fontSize: "2rem", top: "130px" },
        { id: "#editorContrastActionButton", fontSize: "2rem", top: "130px" }
    ];
    const thirdRowMenuHeightWidth = applyStyles(thirdRowButtons, sideValue, "right");

    const rowWidths = [firstRowMenuHeightWidth[0], secondRowMenuHeightWidth[0], thirdRowMenuHeightWidth[0]];
    let blockWidth = Math.max(...rowWidths);
    let blockHeight = firstRowMenuHeightWidth[1] + secondRowMenuHeightWidth[1] + thirdRowMenuHeightWidth[1];

    function styleControl(id, fontSize, top, side, sideValue) {
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
            styleControl(id, fontSize, top, side, offset + "px");
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

    $("#editorBlock").css({
        "position": "absolute",
        "height": blockHeight+"px",
        "width": (blockWidth+5)+"px",
        "right": "0"
    });

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

    function isSpinnerHidden() {
        return $("#editorSpinner").css("display") === "none";
    }

    function normalizedRotation() {
        return Math.abs(((rotation % 360) + 360) % 360);
    }

    $("#editorBrightnessAction").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = $("#editorBrightnessAction").val();
            brightness = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                brightness = 1-parseFloat("0."+number);
            }
            createCanvas(brightness, contrast);
        }
    });

    $("#editorContrastAction").off("click").on("click", function (e) {
        e.preventDefault();

        if (isSpinnerHidden()) {
            let number = $("#editorContrastAction").val();
            contrast = parseFloat("1."+number);
            if (number.charAt(0) === "-") {
                number = number.slice(1);
                contrast = 1-parseFloat("0."+number);
            }
            createCanvas(brightness, contrast);
        }
    });

    function createCanvas(brightnessInput, contrastInput) {
        document.body.style.overflowY= 'hidden';
        const canvas = document.createElement("canvas");
        $(canvas).attr("id", "editShashinImageCanvas");

        document.body.appendChild(canvas);
        const ctx = canvas.getContext("2d");

        const img = new Image();
        img.src = "/api/v1/image/"+editMetadataId+"?v="+uuidv4();

        img.onload = () => {
            canvas.width = img.width;
            canvas.height = img.height;
            ctx.drawImage(img, 0, 0);
            const imageData = ctx.getImageData(0, 0, img.width, img.height);

            const adjustedImageData = adjustBrightnessContrast(ctx, imageData, brightnessInput, contrastInput);
            ctx.putImageData(adjustedImageData, 0, 0);
            const imageURL = canvas.toDataURL("image/jpeg");

            $("#editShashinImage").attr("src", imageURL);

            document.body.style.overflow = 'auto';
            $(canvas).remove();
        };
    }

    function adjustBrightnessContrast(ctx, imageData, brightness, contrast) {
        const data = imageData.data;

        function truncate(value) {
            return Math.min(255, Math.max(0, value));
        }

        for (let i = 0; i < data.length; i += 4) {
            data[i] = truncate((data[i] - 128) * contrast + 128 * brightness);
            // R
            data[i + 1] = truncate((data[i + 1] - 128) * contrast + 128 * brightness);
            // G
            data[i + 2] = truncate((data[i + 2] - 128) * contrast + 128 * brightness);
            // B
            data[i + 3] = truncate((data[i + 3] - 128) * contrast + 128 * brightness);
        }

        return imageData;
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
            if (normalizedRotation() === 90 || normalizedRotation() === 270) {
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
            if (normalizedRotation() === 90 || normalizedRotation() === 270) {
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
            rotation = 0;
            isFlippedHorizontally = false;
            isFlippedVertically = false;
            brightness = 1.0;
            $("#editorBrightnessAction").val(0);
            contrast = 1.0;
            $("#editorContrastAction").val(0);

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
            $("#editorSpinner").css("display", "block");
            $("#editorCloseActionButton").css("display", "none");

            rotation = 0;
            isFlippedHorizontally = false;
            isFlippedVertically = false;
            brightness = 1.0;
            $("#editorBrightnessAction").val(0);
            contrast = 1.0;
            $("#editorContrastAction").val(0);

            shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, rotation, isFlippedHorizontally, isFlippedVertically, brightness, contrast, true,  function (success) {
                shashin.printMessageToConsole("Edited metadata:"+success,{tag:"editor"});

                if (success === true) {
                    shashin.showToastMessage(shashin.getTranslatedValue("main.pages.map.modal.restored"), shashin.getTranslatedValue("main.pages.map.modal.restored"), {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        delay: 2000,
                        borderColor: "success"
                    });
                    editMetadataObj.brightness = brightness;
                    editMetadataObj.contrast = contrast;
                } else {
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail.body"), shashin.getTranslatedValue("main.toast.account.profile.fail.body"), {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor:"danger"
                    });
                }

                $("#editorSpinner").css("display", "none");
                $("#editorCloseActionButton").css("display", "block");
                hideModule();
            });
        }
    });

    $("#editorSaveActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        $("#editorSpinner").css("display", "block");
        $("#editorCloseActionButton").css("display", "none");
        const normalizedRotation = ((rotation % 360) + 360) % 360;
        const swapFlip = isFlippedVertically;
        isFlippedVertically = isFlippedHorizontally;
        isFlippedHorizontally = swapFlip;

        shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, normalizedRotation, isFlippedHorizontally, isFlippedVertically, brightness, contrast, false,  function (success) {
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
            } else {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.notupload"), shashin.getTranslatedValue("main.toast.app.image.notupload"), {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });
            }

            $("#editorSpinner").css("display", "none");
            $("#editorCloseActionButton").css("display", "block");
            hideModule();
        });
    });

    function showModule() {
        $("#editorContainer").css("display", "block");

        $("#editorMedia").css("display", "block");
        $("#editorMedia").html("<img class='centerFit' id='editShashinImage' src='/api/v1/image/"+editMetadataId+"?v="+uuidv4()+"'>");
        $("#editShashinImage").on('load', function() {
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
        isFlippedHorizontally = false;
        isFlippedVertically = false;
        // });
    }
}