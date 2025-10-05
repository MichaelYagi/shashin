function initializeEditor(editMetadataObj, lgIndex) {
    // console.log("--------------------");
    // console.log(editMetadataObj.id);
    // console.log(editMetadataObj);
    // console.log(lgIndex);

    $("#editorBlock").css({
        "position": "absolute",
        "height": "150px",
        "width": "300px",
        "right": "0"
    });
    styleControl("#editorTitle", "2rem", "23px", "left", `30px`);
    let sideValue = 13;
    styleControl("#editorCloseActionButton", "3rem", "10px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorFlipHorizontalActionButton", "2rem", "22px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorFlipVerticalActionButton", "2rem", "22px", "right",sideValue+"px");
    sideValue += 58;
    styleControl("#editorRotateLeftActionButton", "2rem", "23px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorRotateRightActionButton", "2rem", "23px", "right", sideValue+"px");
    sideValue = 19;
    styleControl("#editorRestoreActionButton", "2rem", "75px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorSaveActionButton", "2rem", "75px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorSpinner", "2rem", "33px", "right", sideValue+"px");

    let rotation = 0;
    let isFlippedHorizontally = false;
    let isFlippedVertically = false;
    const editMetadataId = editMetadataObj.id;

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

        if (!$(event.target).closest('#editorCloseActionButton, #editorFlipHorizontalActionButton, #editorFlipVerticalActionButton, #editorRotateRightActionButton, #editorRotateLeftActionButton, #editorRestoreActionButton, #editorSaveActionButton, #editorBlock').length) {
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

    function updateTransform() {
        const flipX = isFlippedHorizontally ? -1 : 1;
        const flipY = isFlippedVertically ? -1 : 1;
        const isEvenRotation = rotation % 180 === 0;

        $("#editShashinImage").css({
            "transition": "transform 0.3s ease-in-out",
            "transform": `translate(-50%, -50%) rotate(${rotation}deg) scale(${flipX}, ${flipY})`,
            "max-width": isEvenRotation ? $(window).innerWidth() + 1 : $(window).innerHeight() + 1,
            "max-height": isEvenRotation ? $(window).innerHeight() + 1 : $(window).innerWidth() + 1
        });
    }

    function isSpinnerHidden() {
        return $("#editorSpinner").css("display") === "none";
    }

    function normalizedRotation() {
        return Math.abs(((rotation % 360) + 360) % 360);
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

    $("#editorRestoreActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            $("#editorSpinner").css("display", "block");

            rotation = 0;
            isFlippedHorizontally = false;
            isFlippedVertically = false;

            shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, rotation, isFlippedVertically, isFlippedHorizontally, true,  function (success) {
                shashin.printMessageToConsole("Edited metadata:"+success,{tag:"editor"});

                if (success === true) {
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

                $("#editorSpinner").css("display", "none");
                hideModule();
            });
        }
    });

    $("#editorSaveActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        $("#editorSpinner").css("display", "block");
        const normalizedRotation = ((rotation % 360) + 360) % 360;

        shashin.processEditedThumbnail(editMetadataObj.id, lgIndex, normalizedRotation, isFlippedVertically, isFlippedHorizontally, false,  function (success) {
            shashin.printMessageToConsole("Edited metadata:"+success,{tag:"editor"});

            if (success === true) {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.upload"), shashin.getTranslatedValue("main.toast.app.image.upload"), {
                    icon: "bi-info-circle",
                    iconColor: "#777777",
                    delay: 2000,
                    borderColor: "success"
                });
            } else {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.app.image.notupload"), shashin.getTranslatedValue("main.toast.app.image.notupload"), {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger"
                });
            }

            $("#editorSpinner").css("display", "none");
            hideModule();
        });
    });

    function showModule() {
        $("#editorContainer").css("display", "block");
        $("#editorToolContainer").css("display", "block");
        $("#editorBlock").css("display", "block");

        $("#editorMedia").css("display", "block");
        $("#editorMedia").html("<img class='centerFit' id='editShashinImage' src='/api/v1/image/"+editMetadataId+"?v="+uuidv4()+"'>");
    }

    function hideModule() {
        $("#editorContainer").css("display", "none");
        $("#editorToolContainer").css("display", "none");
        $("#editorBlock").css("display", "none");

        $("#editorMedia").css("display", "none");
        $("#editorMedia").html("");

        rotation = 0;
        visualRotation = 0;
        isFlippedHorizontally = false;
        isFlippedVertically = false;
    }

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
}