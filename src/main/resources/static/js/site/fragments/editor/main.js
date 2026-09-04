function initializeEditor(editMetadataObj, lgIndex) {
    document.body.style.overflowY = 'hidden';

    let originalRotation      = (editMetadataObj.rotation        != null) ? editMetadataObj.rotation        : 0;
    let originalIsFlippedHoriz = (editMetadataObj.flipHorizontally != null) ? editMetadataObj.flipHorizontally : false;
    let originalIsFlippedVert  = (editMetadataObj.flipVertically   != null) ? editMetadataObj.flipVertically   : false;
    let originalBrightness = (editMetadataObj.brightness != null) ? parseFloat(editMetadataObj.brightness) : 1.0;
    let originalContrast   = (editMetadataObj.contrast   != null) ? parseFloat(editMetadataObj.contrast)   : 1.0;
    let originalSaturation = (editMetadataObj.saturation != null) ? parseFloat(editMetadataObj.saturation) : 1.0;
    let originalSharpness  = (editMetadataObj.sharpness  != null) ? parseFloat(editMetadataObj.sharpness)  : 1.0;

    let kiroCropper = null;
    let kiriInitializing = false;
    const kiriContainer = document.getElementById("editorMedia");

    $("#editorContainer").css("display", "block");
    $("#editorToolContainer").css("display", "block");
    showSpinner();
    disableCloseButton();

    wireHandlers();
    showModule();

    // ─── helpers ────────────────────────────────────────────────────────────

    function normalizedRotation(r) {
        return Math.abs(((r % 360) + 360) % 360);
    }

    function showSpinner()    { $("#editorSpinner").css("display", "block"); }
    function hideSpinner()    { $("#editorSpinner").css("display", "none"); }
    function isSpinnerHidden() { return $("#editorSpinner").css("display") === "none"; }

    function enableCloseButton() {
        $("#editorCloseActionButton").prop('disabled', false).css({"pointer-events": "auto"});
        $("#editorCloseAction").css({"color": "#FFFFFF", "text-shadow": "#EDEBEB 2px 2px 5px"});
    }

    function disableCloseButton() {
        $("#editorCloseActionButton").prop('disabled', true).css({"pointer-events": "none"});
        $("#editorCloseAction").css({"color": "#808080", "text-shadow": "#969595 2px 2px 5px"});
    }

    function sliderToFilter(val, type) {
        const n = parseInt(val);
        if (type === 'sharpness') return n;
        if (n >= 0) return parseFloat("1." + n);
        return 1 - parseFloat("0." + Math.abs(n));
    }

    function filterToSlider(val, type) {
        if (type === 'sharpness') return parseInt(val);
        const f = parseFloat(val);
        if (f >= 1.0) {
            const d = (Math.round(f * 10) / 10).toString().split(".")[1] || "0";
            return parseInt(d);
        }
        const d = (Math.round((1 - f) * 10) / 10).toString().split(".")[1] || "0";
        return -parseInt(d);
    }

    function getCurrentFilters() {
        return {
            brightness: sliderToFilter($("#editorBrightnessAction").val(), 'brightness'),
            contrast:   sliderToFilter($("#editorContrastAction").val(),   'contrast'),
            saturation: sliderToFilter($("#editorSaturationAction").val(), 'saturation'),
            sharpness:  sliderToFilter($("#editorSharpnessAction").val(),  'sharpness')
        };
    }

    function initSliders() {
        const bSlider = filterToSlider(originalBrightness, 'brightness');
        $("#editorBrightnessAction").val(bSlider);
        $("#brightnessTick").css("display", bSlider === 0 ? "none" : "block");

        const cSlider = filterToSlider(originalContrast, 'contrast');
        $("#editorContrastAction").val(cSlider);
        $("#contrastTick").css("display", cSlider === 0 ? "none" : "block");

        const sSlider = filterToSlider(originalSaturation, 'saturation');
        $("#editorSaturationAction").val(sSlider);
        $("#saturationTick").css("display", sSlider === 0 ? "none" : "block");

        const shSlider = filterToSlider(originalSharpness, 'sharpness');
        $("#editorSharpnessAction").val(shSlider);
        $("#sharpnessTick").css("display", shSlider === 1 ? "none" : "block");
    }

    function resetSliders() {
        $("#editorBrightnessAction").val(0); $("#brightnessTick").css("display", "none");
        $("#editorContrastAction").val(0);   $("#contrastTick").css("display", "none");
        $("#editorSaturationAction").val(0); $("#saturationTick").css("display", "none");
        $("#editorSharpnessAction").val(1);  $("#sharpnessTick").css("display", "none");
    }

    function enableButtons() {
        const ids = [
            "editorSaveAction","editorBrightnessAction","editorContrastAction",
            "editorSaturationAction","editorRestoreAction","editorRotateRightAction",
            "editorRotateLeftAction","editorFlipHorizontalAction","editorFlipVerticalAction",
            "editorResetAction","editorBrightnessIcon","editorContrastIcon",
            "editorSaturationIcon","editorSharpnessAction","editorSharpnessIcon"
        ];
        ids.forEach(id => $('#'+id).prop('disabled', false).css({'color': "#FFFFFF", "text-shadow": "#EDEBEB 2px 2px 5px"}));
    }

    function disableButtons() {
        const ids = [
            "editorSaveAction","editorBrightnessAction","editorContrastAction",
            "editorSaturationAction","editorRestoreAction","editorRotateRightAction",
            "editorRotateLeftAction","editorFlipHorizontalAction","editorFlipVerticalAction",
            "editorResetAction","editorBrightnessIcon","editorContrastIcon",
            "editorSaturationIcon","editorSharpnessAction","editorSharpnessIcon"
        ];
        ids.forEach(id => $('#'+id).prop('disabled', true).css({'color': "#808080", "text-shadow": "#969595 2px 2px 5px"}));
    }

    function toggleResetSaveButtons() {
        if (!kiroCropper) return;

        const state    = kiroCropper.getState();
        const curRot   = normalizedRotation(state.rotation);
        const curFlipH = state.flip.horizontal;
        const curFlipV = state.flip.vertical;
        const f        = getCurrentFilters();

        const isUnchanged =
            curRot   === normalizedRotation(originalRotation) &&
            curFlipH === originalIsFlippedHoriz &&
            curFlipV === originalIsFlippedVert &&
            parseFloat(f.brightness) === parseFloat(originalBrightness) &&
            parseFloat(f.contrast)   === parseFloat(originalContrast)   &&
            parseFloat(f.saturation) === parseFloat(originalSaturation) &&
            parseFloat(f.sharpness)  === parseFloat(originalSharpness);

        const isNeutral =
            curRot === 0 && !curFlipH && !curFlipV &&
            f.brightness === 1.0 && f.contrast === 1.0 &&
            f.saturation === 1.0 && f.sharpness === 1;

        if (isUnchanged) {
            $("#editorResetActionButton").prop('disabled', true).css({"pointer-events": "none"});
            $("#editorResetAction").css({"color": "#808080", "text-shadow": "#969595 2px 2px 5px"});
            $("#editorSaveActionButton").prop('disabled', true).css({"pointer-events": "none"});
            $("#editorSaveAction").css({"color": "#808080", "text-shadow": "#969595 2px 2px 5px"});
        } else {
            $("#editorResetActionButton").prop('disabled', false).css({"pointer-events": "auto"});
            $("#editorResetAction").css({"color": "#FFFFFF", "text-shadow": "#EDEBEB 2px 2px 5px"});
            $("#editorSaveActionButton").prop('disabled', false).css({"pointer-events": "auto"});
            $("#editorSaveAction").css({"color": "#FFFFFF", "text-shadow": "#EDEBEB 2px 2px 5px"});
        }

        if (isNeutral) {
            $("#editorRestoreActionButton").prop('disabled', true).css({"pointer-events": "none"});
            $("#editorRestoreAction").css({"color": "#808080", "text-shadow": "#969595 2px 2px 5px"});
        } else {
            $("#editorRestoreActionButton").prop('disabled', false).css({"pointer-events": "auto"});
            $("#editorRestoreAction").css({"color": "#FFFFFF", "text-shadow": "#EDEBEB 2px 2px 5px"});
        }
    }

    // ─── Kiri lifecycle ─────────────────────────────────────────────────────

    function buildKiri() {
        const rect = kiriContainer.getBoundingClientRect();
        const w = Math.max(Math.floor(rect.width), 100);
        const h = Math.max(Math.floor(rect.height), 100);
        kiroCropper = new Kiri(kiriContainer, {
            resizableFrame: false,
            useExifOrientation: true,
            autoSizeStage: false,
            frame: { width: w, height: h }
        });
        kiroCropper.on('change', function () {
            if (!kiriInitializing) toggleResetSaveButtons();
        });
    }

    function rebuildKiri() {
        if (kiroCropper) { kiroCropper.destroy(); kiroCropper = null; }
        buildKiri();
    }

    function loadImageIntoKiri() {
        const url = "/api/v1/image/original/" + editMetadataObj.id + "?v=" + uuidv4();
        return fetch(url)
            .then(function (r) { return r.blob(); })
            .then(function (blob) {
                kiriInitializing = true;
                return kiroCropper.load(blob);
            })
            .then(function () { kiriInitializing = false; })
            .catch(function (err) {
                kiriInitializing = false;
                shashin.printMessageToConsole("Kiri load error: " + err, {tag: "editor"});
            });
    }

    function applyStoredStateToKiri() {
        if (!kiroCropper) return;
        kiriInitializing = true;
        if (originalRotation !== 0)  kiroCropper.rotate(originalRotation);
        if (originalIsFlippedHoriz)  kiroCropper.flipHorizontal();
        if (originalIsFlippedVert)   kiroCropper.flipVertical();
        kiroCropper.setFilters({
            brightness: originalBrightness,
            contrast:   originalContrast,
            saturation: originalSaturation,
            sharpness:  originalSharpness
        });
        kiriInitializing = false;
    }

    function showModule() {
        $("#editorMedia").css("display", "block");
        buildKiri();
        loadImageIntoKiri().then(function () {
            applyStoredStateToKiri();
            initSliders();
            enableButtons();
            toggleResetSaveButtons();
            hideSpinner();
            enableCloseButton();
        });
    }

    function hideModule() {
        if (kiroCropper) { kiroCropper.destroy(); kiroCropper = null; }
        $("#editorContainer").css("display", "none");
        $("#editorToolContainer").css("display", "none");
        $("#editorMedia").css("display", "none");
        shashin.closeToastMessages({tag: "editorCurrentSettings"});
        document.body.style.overflowY = 'auto';
    }

    function saveImage() {
        if (!kiroCropper) return;
        disableButtons();
        showSpinner();
        disableCloseButton();

        const state    = kiroCropper.getState();
        const rotation = normalizedRotation(state.rotation);
        const flipH    = state.flip.horizontal;
        const flipV    = state.flip.vertical;
        const f        = getCurrentFilters();

        shashin.printMessageToConsole("--------------", {tag: "editor"});
        shashin.printMessageToConsole("Saving: rotation=" + rotation + " flipH=" + flipH + " flipV=" + flipV, {tag: "editor"});
        shashin.printMessageToConsole("brightness=" + f.brightness + " contrast=" + f.contrast + " saturation=" + f.saturation + " sharpness=" + f.sharpness, {tag: "editor"});

        shashin.processEditedThumbnail(
            editMetadataObj.id, lgIndex,
            rotation, flipH, flipV,
            f.brightness, f.contrast, f.saturation, f.sharpness,
            false,
            function (success) {
                if (success === true) {
                    editMetadataObj.rotation         = rotation;
                    editMetadataObj.flipHorizontally  = flipH;
                    editMetadataObj.flipVertically    = flipV;
                    editMetadataObj.brightness        = f.brightness;
                    editMetadataObj.contrast          = f.contrast;
                    editMetadataObj.saturation        = f.saturation;
                    editMetadataObj.sharpness         = f.sharpness;

                    shashin.showToastMessage(
                        shashin.getTranslatedValue("main.toast.app.image.upload"),
                        shashin.getTranslatedValue("main.toast.app.image.upload"),
                        {icon: "bi-info-circle", iconColor: "#777777", delay: 2000, borderColor: "success"}
                    );
                } else {
                    shashin.showToastMessage(
                        shashin.getTranslatedValue("main.toast.app.image.notupload"),
                        shashin.getTranslatedValue("main.toast.app.image.notupload"),
                        {icon: "bi-exclamation-triangle", iconColor: "#FF0000", borderColor: "danger"}
                    );
                }

                enableButtons();
                toggleResetSaveButtons();
                hideSpinner();
                disableCloseButton();
                hideModule();
            }
        );
    }

    // ─── event wiring ───────────────────────────────────────────────────────

    function wireHandlers() {
        $("#editorContainer").css("cursor", "auto");

        // Click outside tools → close
        $("#editorContainer").off("click").on('click', function (event) {
            event.preventDefault();
            if (!$(event.target).closest(
                '#editorCloseActionButton, #editorFlipHorizontalActionButton, ' +
                '#editorFlipVerticalActionButton, #editorRotateRightActionButton, ' +
                '#editorRotateLeftActionButton, #editorRestoreActionButton, ' +
                '#editorSaveActionButton, #editorResetActionButton, ' +
                '#editorBrightnessActionButton, #editorBrightnessAction, ' +
                '#editorContrastActionButton, #editorContrastAction, ' +
                '#editorSaturationActionButton, #editorSaturationAction, ' +
                '#editorSharpnessActionButton, #editorSharpnessAction, ' +
                '#editorToolContainer, #editorTitle'
            ).length) {
                if (isSpinnerHidden()) hideModule();
            }
        });

        // Keyboard shortcuts
        $("body").off("keydown").on("keydown", function (e) {
            if (e.key === "Escape" || e.keyCode === 27) {
                e.preventDefault();
                shashin.closeToastMessages({tag: "editorCurrentSettings"});
                setTimeout(function () { if (isSpinnerHidden()) hideModule(); }, 100);
            }
            if (e.key === "ArrowLeft" || e.keyCode === 37) {
                e.preventDefault();
                if ($("#editorRotateLeftActionButton").css("pointer-events") !== "none")
                    $("#editorRotateLeftActionButton").click();
            }
            if (e.key === "ArrowUp" || e.keyCode === 38) {
                e.preventDefault();
                if ($("#editorFlipHorizontalActionButton").css("pointer-events") !== "none")
                    $("#editorFlipHorizontalActionButton").click();
            }
            if (e.key === "ArrowRight" || e.keyCode === 39) {
                e.preventDefault();
                if ($("#editorRotateRightActionButton").css("pointer-events") !== "none")
                    $("#editorRotateRightActionButton").click();
            }
            if (e.key === "ArrowDown" || e.keyCode === 40) {
                e.preventDefault();
                if ($("#editorFlipVerticalActionButton").css("pointer-events") !== "none")
                    $("#editorFlipVerticalActionButton").click();
            }
            if (e.key === "r" || e.keyCode === 82) {
                e.preventDefault();
                if ($("#editorResetActionButton").css("pointer-events") !== "none")
                    $("#editorResetActionButton").click();
            }
            if (e.key === "o" || e.keyCode === 79) {
                e.preventDefault();
                if ($("#editorRestoreActionButton").css("pointer-events") !== "none")
                    $("#editorRestoreActionButton").click();
            }
            if (e.key === "s" || e.keyCode === 83) {
                e.preventDefault();
                if ($("#editorSaveActionButton").css("pointer-events") !== "none")
                    $("#editorSaveActionButton").click();
            }
        });

        // Close button
        $("#editorCloseAction").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (isSpinnerHidden()) hideModule();
        });

        // Info modal
        $("#editorToolContainerTitleLink").off("click").on("click", function () {
            function highlight(value, defaultValue) {
                const str = value.toString();
                return (value === defaultValue) ? str : '<span style="color:#0a53be">' + str + '</span>';
            }
            function highlightNumber(num, defaultValue) {
                defaultValue = defaultValue !== undefined ? defaultValue : 1.0;
                const fixed = parseFloat(num).toFixed(1);
                return (parseFloat(fixed) === defaultValue) ? fixed : '<span style="color:#0a53be">' + fixed + '</span>';
            }
            shashin.showToastMessage(
                shashin.getTranslatedValue("main.pages.lg.plugins.editor.modal.title"),
                "<div>" +
                    "<div class=\"row\"><div class=\"col-md-6\"><strong>" + shashin.getTranslatedValue("main.pages.lg.plugins.editor.rotate") + "</strong></div>" +
                    "<div class=\"col-md-3\">" + highlight(normalizedRotation(originalRotation), 0) + "</div></div>" +
                    "<div class=\"row\"><div class=\"col-md-6\"><strong>" + shashin.getTranslatedValue("main.pages.lg.plugins.editor.flipx") + "</strong></div>" +
                    "<div class=\"col-md-3\">" + highlight(originalIsFlippedHoriz, false) + "</div></div>" +
                    "<div class=\"row\"><div class=\"col-md-6\"><strong>" + shashin.getTranslatedValue("main.pages.lg.plugins.editor.flipy") + "</strong></div>" +
                    "<div class=\"col-md-3\">" + highlight(originalIsFlippedVert, false) + "</div></div>" +
                    "<div class=\"row\"><div class=\"col-md-6\"><strong>" + shashin.getTranslatedValue("main.pages.lg.plugins.editor.brightness") + "</strong></div>" +
                    "<div class=\"col-md-3\">" + highlightNumber(originalBrightness) + "</div></div>" +
                    "<div class=\"row\"><div class=\"col-md-6\"><strong>" + shashin.getTranslatedValue("main.pages.lg.plugins.editor.contrast") + "</strong></div>" +
                    "<div class=\"col-md-3\">" + highlightNumber(originalContrast) + "</div></div>" +
                    "<div class=\"row\"><div class=\"col-md-6\"><strong>" + shashin.getTranslatedValue("main.pages.lg.plugins.editor.saturation") + "</strong></div>" +
                    "<div class=\"col-md-3\">" + highlightNumber(originalSaturation) + "</div></div>" +
                    "<div class=\"row\"><div class=\"col-md-6\"><strong>" + shashin.getTranslatedValue("main.pages.lg.plugins.editor.sharpness") + "</strong></div>" +
                    "<div class=\"col-md-3\">" + highlightNumber(originalSharpness) + "</div></div>" +
                "</div>",
                {icon: "bi-info-circle", autohide: false, placement: shashin.toast.placement.top.center, iconColor: "#777777", tag: "editorCurrentSettings"}
            );
        });

        // Rotate
        $("#editorRotateRightActionButton").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.rotate(90);
        });

        $("#editorRotateLeftActionButton").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.rotate(-90);
        });

        // Flip Vertical = top-bottom mirror (Photoshop convention) → Kiri flipHorizontal() (across horizontal axis)
        $("#editorFlipVerticalActionButton").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.flipHorizontal();
        });

        // Flip Horizontal = left-right mirror (Photoshop convention) → Kiri flipVertical() (across vertical axis)
        $("#editorFlipHorizontalActionButton").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.flipVertical();
        });

        // Reset → reload and re-apply stored state
        $("#editorResetActionButton").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (isSpinnerHidden()) {
                disableButtons();
                showSpinner();
                disableCloseButton();
                rebuildKiri();
                loadImageIntoKiri().then(function () {
                    applyStoredStateToKiri();
                    initSliders();
                    enableButtons();
                    toggleResetSaveButtons();
                    hideSpinner();
                    enableCloseButton();
                });
            }
        });

        // Restore → reload to neutral
        $("#editorRestoreActionButton").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (isSpinnerHidden()) {
                disableButtons();
                showSpinner();
                disableCloseButton();
                rebuildKiri();
                loadImageIntoKiri().then(function () {
                    resetSliders();
                    enableButtons();
                    toggleResetSaveButtons();
                    hideSpinner();
                    enableCloseButton();
                });
            }
        });

        // Save
        $("#editorSaveActionButton").off("click").on("click", function (e) {
            e.preventDefault();
            saveImage();
        });

        // Slider icons → reset that slider to neutral
        $("#editorBrightnessIcon").off("click").on('click', function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorBrightnessAction").val(0);
                $("#brightnessTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });
        $("#editorContrastIcon").off("click").on('click', function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorContrastAction").val(0);
                $("#contrastTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });
        $("#editorSaturationIcon").off("click").on('click', function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorSaturationAction").val(0);
                $("#saturationTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });
        $("#editorSharpnessIcon").off("click").on('click', function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorSharpnessAction").val(1);
                $("#sharpnessTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });

        // Slider tick resets
        $("#brightnessTick").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorBrightnessAction").val(0);
                $("#brightnessTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });
        $("#contrastTick").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorContrastAction").val(0);
                $("#contrastTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });
        $("#saturationTick").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorSaturationAction").val(0);
                $("#saturationTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });
        $("#sharpnessTick").off("click").on("click", function (e) {
            e.preventDefault();
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) {
                $("#editorSharpnessAction").val(1);
                $("#sharpnessTick").css("display", "none");
                kiroCropper.setFilters(getCurrentFilters());
            }
        });

        // Slider input → update tick visibility
        $("#editorBrightnessAction").off("input").on("input", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            $("#brightnessTick").css("display", $(this).val() === "0" ? "none" : "block");
        });
        $("#editorContrastAction").off("input").on("input", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            $("#contrastTick").css("display", $(this).val() === "0" ? "none" : "block");
        });
        $("#editorSaturationAction").off("input").on("input", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            $("#saturationTick").css("display", $(this).val() === "0" ? "none" : "block");
        });
        $("#editorSharpnessAction").off("input").on("input", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            $("#sharpnessTick").css("display", $(this).val() === "1" ? "none" : "block");
        });

        // Slider change → apply to Kiri
        $("#editorBrightnessAction").off("change").on("change", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.setFilters(getCurrentFilters());
        });
        $("#editorContrastAction").off("change").on("change", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.setFilters(getCurrentFilters());
        });
        $("#editorSaturationAction").off("change").on("change", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.setFilters(getCurrentFilters());
        });
        $("#editorSharpnessAction").off("change").on("change", function () {
            shashin.closeToastMessages({tag: "editorCurrentSettings"});
            if (kiroCropper && isSpinnerHidden()) kiroCropper.setFilters(getCurrentFilters());
        });
    }
}
