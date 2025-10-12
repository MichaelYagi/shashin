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

    shashin.printMessageToConsole("--------------",{tag:"editor"});
    shashin.printMessageToConsole("initializeEditor after applying default transitions",{tag:"editor"});
    shashin.printMessageToConsole("rotation: "+rotation,{tag:"editor"});
    shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
    shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
    shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
    shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
    shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});

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
        styleBottomControl("#editorBrightnessActionButton", "2em", "155px", "right", "10px", "#editorBrightnessIcon");
    } else {
        thirdRowMenuHeightWidth = applyStyles(thirdRowButtons, sideValue, "right");
    }

    // Fourth row buttons
    sideValue = 10;
    let fourthRowButtons = [
        { id: "#editorContrastActionButton", fontSize: "2rem", top: "200px" }
    ];
    let fourthRowMenuHeightWidth = 0;
    if (Util.isMobile()) {
        styleBottomControl("#editorContrastActionButton", "2em", "95px", "right", "10px", "#editorContrastIcon");
    } else {
        fourthRowMenuHeightWidth = applyStyles(fourthRowButtons, sideValue, "right");
    }

    // Fifth row buttons
    sideValue = 10;
    let fifthRowButtons = [
        { id: "#editorSaturationActionButton", fontSize: "2rem", top: "270px" }
    ];
    let fifthRowMenuHeightWidth = 0;
    if (Util.isMobile()) {
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

        if (isSpinnerHidden()) {
            brightness = 1.0;
            $("#editorBrightnessAction").val(0);
            applyAttributes();
        }
    });

    $("#editorContrastIcon").off("click").on('click', function(event) {
        event.preventDefault();

        if (isSpinnerHidden()) {
            contrast = 1.0;
            $("#editorContrastAction").val(0);
            applyAttributes();
        }
    });

    $("#editorSaturationIcon").off("click").on('click', function(event) {
        event.preventDefault();

        if (isSpinnerHidden()) {
            saturation = 1.0;
            $("#editorSaturationAction").val(0);
            applyAttributes();
        }
    });

    // Slider adjustment
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

    function webglSupport () {
        try {
            const canvas = document.createElement('canvas');
            return !!window.WebGLRenderingContext &&
                (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
        } catch(e) {
            return false;
        }
    }

    function applyAttributes() {
        disableButtons();
        $("#editorSpinner").css("display", "block");
        $("#editorCloseActionButton").css("display", "none");

        document.body.style.overflowY = 'hidden';

        shashin.printMessageToConsole("--------------",{tag:"editor"});
        shashin.printMessageToConsole("Applying attributes for preview",{tag:"editor"});
        shashin.printMessageToConsole("Rotation: "+rotation,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
        shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
        shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
        shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});

        // I there is WebGL support
        const vertexShaderSource = `
            attribute vec2 a_position;
            attribute vec2 a_texCoord;
            varying vec2 v_texCoord;
            void main() {
                gl_Position = vec4(a_position, 0, 1);
                v_texCoord = a_texCoord;
            }
        `;

        const fragmentShaderSource = `
    precision mediump float;
    uniform sampler2D u_image;
    uniform float u_brightness;
    uniform float u_contrast;
    uniform float u_saturation;
    varying vec2 v_texCoord;

    void main() {
        vec4 color = texture2D(u_image, v_texCoord);

        // Brightness
        color.rgb *= u_brightness;

        // Contrast
        color.rgb = ((color.rgb - 0.5) * u_contrast) + 0.5;

        // Saturation with red dampening
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        vec3 delta = color.rgb - vec3(gray);
        vec3 saturated = vec3(gray) + delta * u_saturation;

        if (u_saturation > 1.0) {
            float redFactor = 1.0 - 0.10 * (u_saturation - 1.0);
            saturated.r = gray + (color.r - gray) * u_saturation * redFactor;
        }

        // Perceptual brightness boost (applied after saturation)
        float brightnessBoost = (u_saturation > 1.0) ? (u_saturation - 1.0) * 0.006 : 0.0;
        saturated += vec3(brightnessBoost);

        // Clamp final output
        color.rgb = clamp(saturated, 0.0, 1.0);

        gl_FragColor = color;
    }
`;

        function createShader(gl, type, source) {
            const shader = gl.createShader(type);
            gl.shaderSource(shader, source);
            gl.compileShader(shader);
            return shader;
        }

        function createProgram(gl, vsSource, fsSource) {
            const program = gl.createProgram();
            gl.attachShader(program, createShader(gl, gl.VERTEX_SHADER, vsSource));
            gl.attachShader(program, createShader(gl, gl.FRAGMENT_SHADER, fsSource));
            gl.linkProgram(program);
            return program;
        }

        function setupImageAdjustments(image, canvas, brightnessInput = 1.0, contrastInput = 1.0, saturationInput = 1.0) {
            const gl = canvas.getContext("webgl");
            const program = createProgram(gl, vertexShaderSource, fragmentShaderSource);
            gl.useProgram(program);
            gl.viewport(0, 0, canvas.width, canvas.height);

            // Geometry
            const positionBuffer = gl.createBuffer();
            gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
            gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
                -1, -1, 1, -1, -1, 1,
                1, -1, 1, 1, -1, 1
            ]), gl.STATIC_DRAW);
            const aPosition = gl.getAttribLocation(program, "a_position");
            gl.enableVertexAttribArray(aPosition);
            gl.vertexAttribPointer(aPosition, 2, gl.FLOAT, false, 0, 0);

            const texCoordBuffer = gl.createBuffer();
            gl.bindBuffer(gl.ARRAY_BUFFER, texCoordBuffer);
            gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
                0, 0, 1, 0, 0, 1,
                1, 0, 1, 1, 0, 1
            ]), gl.STATIC_DRAW);
            const aTexCoord = gl.getAttribLocation(program, "a_texCoord");
            gl.enableVertexAttribArray(aTexCoord);
            gl.vertexAttribPointer(aTexCoord, 2, gl.FLOAT, false, 0, 0);

            // Texture
            const texture = gl.createTexture();
            gl.bindTexture(gl.TEXTURE_2D, texture);
            gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
            gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
            gl.activeTexture(gl.TEXTURE0);
            gl.bindTexture(gl.TEXTURE_2D, texture);
            gl.uniform1i(gl.getUniformLocation(program, "u_image"), 0);

            // Uniforms
            gl.uniform1f(gl.getUniformLocation(program, "u_brightness"), brightnessInput);
            gl.uniform1f(gl.getUniformLocation(program, "u_contrast"), contrastInput);
            gl.uniform1f(gl.getUniformLocation(program, "u_saturation"), saturationInput);

            // Draw
            gl.drawArrays(gl.TRIANGLES, 0, 6);

            const imageURL = canvas.toDataURL("image/jpeg", 0.2);
            $("#editShashinImage").attr("src", imageURL);
        }

        if (webglSupport() === false) {
            // Make network call to transform: inputs - brightness, contrast, saturation, rotation and x/y flips
            shashin.processEditedPreviewThumbnail(editMetadataObj.id, editMetadataObj.path, brightness, contrast, saturation, function (data) {
                if (data !== null) {
                    shashin.printMessageToConsole("--------------",{tag:"editor"});
                    shashin.printMessageToConsole("Saturation editing time: " + data.saturationProcessingMS + "ms", {tag: "editor"});
                    shashin.printMessageToConsole("Contrast editing time: " + data.contrastProcessingMS + "ms", {tag: "editor"});
                    shashin.printMessageToConsole("Brightness editing time: " + data.brightnessProcessingMS + "ms", {tag: "editor"});
                    shashin.printMessageToConsole("Total time editing image: " + data.totalTimeMS + "ms", {tag: "editor"});
                    // console.log("Total time editing image: "+data.totalTimeMS+"ms");
                    $("#editShashinImage").attr("src", "data:image/jpg;base64," + data.image);
                    updateTransform(false);
                    document.body.style.overflow = 'auto';
                    enableButtons();
                    $("#editorSpinner").css("display", "none");
                    $("#editorCloseActionButton").css("display", "block");
                }
            });
        } else {
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
                setupImageAdjustments(img, canvas, brightness, contrast, saturation);
                updateTransform(false);
                document.body.style.overflow = 'auto';
                enableButtons();
                $("#editorSpinner").css("display", "none");
                $("#editorCloseActionButton").css("display", "block");
            };
        }
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

            shashin.printMessageToConsole("--------------",{tag:"editor"});
            shashin.printMessageToConsole("Restoring attributes",{tag:"editor"});
            shashin.printMessageToConsole("Rotation: "+rotation,{tag:"editor"});
            shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
            shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
            shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
            shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
            shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});

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

        shashin.printMessageToConsole("--------------",{tag:"editor"});
        shashin.printMessageToConsole("Saving attributes",{tag:"editor"});
        shashin.printMessageToConsole("Rotation: "+rotation,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedHorizontally: "+isFlippedHorizontally,{tag:"editor"});
        shashin.printMessageToConsole("isFlippedVertically: "+isFlippedVertically,{tag:"editor"});
        shashin.printMessageToConsole("brightness: "+brightness,{tag:"editor"});
        shashin.printMessageToConsole("contrast: "+contrast,{tag:"editor"});
        shashin.printMessageToConsole("saturation: "+saturation,{tag:"editor"});

        shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, normalizedRotation(rotation), isFlippedHorizontally, isFlippedVertically, brightness, contrast, saturation, false,  function (success) {
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

        if ($("#glcanvas").length > 0) {
            $("#glcanvas").remove();
        }

        rotation = 0;
        brightness = 1.0;
        contrast = 1.0;
        saturation = 1.0;
        isFlippedHorizontally = false;
        isFlippedVertically = false;
    }
}