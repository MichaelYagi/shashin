function initializeEditor(editMetadataObj, lgIndex) {
    // console.log("--------------------");
    // console.log(editMetadataObj.id);
    // console.log(editMetadataObj);
    // console.log(lgIndex);

    let sideValue = 19;
    styleControl("#editorCloseActionButton", "3rem", "10px", "right", sideValue+"px");
    // sideValue += 58;
    // styleControl("#editorFlipHorizontalActionButton", "2rem", "22px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorFlipVerticalActionButton", "2rem", "22px", "right",sideValue+"px");
    sideValue += 58;
    styleControl("#editorRotateLeftActionButton", "2rem", "23px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorRotateRightActionButton", "2rem", "23px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorRestoreActionButton", "2rem", "23px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorSaveActionButton", "2rem", "23px", "right", sideValue+"px");
    sideValue += 58;
    styleControl("#editorSpinner", "2rem", "32px", "right", sideValue+"px");

    let rotation = 0;
    let visualRotation = 0;
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

        if (!$(event.target).closest('#editorCloseActionButton, #editorFlipHorizontalActionButton, #editorFlipVerticalActionButton, #editorRotateRightActionButton, #editorRotateLeftActionButton, #editorRestoreActionButton, #editorSaveActionButton').length) {
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

    $("#editorRotateRightActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            rotation = (rotation + 90) % 360;
            visualRotation += 90;
            const flipVertically = isFlippedVertically ? -1 : 1;

            $("#editShashinImage").css({
                "transition": "transform 0.3s ease-in-out",
                "transform": `translate(-50%, -50%) rotate(${visualRotation}deg) scaleX(${flipVertically})`,
                "max-width": rotation % 180 === 0 ? $(window).innerWidth() + 1 : $(window).innerHeight() + 1,
                "max-height": rotation % 180 === 0 ? $(window).innerHeight() + 1 : $(window).innerWidth() + 1
            });
        }
    });

    $("#editorRotateLeftActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            rotation = (rotation - 90) % 360;
            visualRotation -= 90;
            const flipVertically = isFlippedVertically ? -1 : 1;

            $("#editShashinImage").css({
                "transition": "transform 0.3s ease-in-out",
                "transform": `translate(-50%, -50%) rotate(${visualRotation}deg) scaleX(${flipVertically})`,
                "max-width": rotation % 180 === 0 ? $(window).innerWidth() + 1 : $(window).innerHeight() + 1,
                "max-height": rotation % 180 === 0 ? $(window).innerHeight() + 1 : $(window).innerWidth() + 1
            });
        }
    });

    $("#editorFlipVerticalActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            // if (Math.abs(rotation%360%180) === 0) {
                isFlippedVertically = !isFlippedVertically;
                const flipVertically = isFlippedVertically ? -1 : 1;

                $("#editShashinImage").css({
                    "transition": "transform 0.3s ease-in-out",
                    "transform": "translate(-50%, -50%) rotate(" + visualRotation + "deg) scaleX(" + flipVertically + ")"
                });
            // } else {
            //     isFlippedHorizontally = !isFlippedHorizontally;
            //     const flipHorizontally = isFlippedHorizontally ? -1 : 1;
            //
            //     $("#editShashinImage").css({
            //         "transition": "transform 0.3s ease-in-out",
            //         "transform": "translate(-50%, -50%) rotate(" + visualRotation + "deg) scaleY(" + flipHorizontally + ")"
            //     });
            // }
        }
    });

    $("#editorFlipHorizontalActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            if (Math.abs(rotation%360%180) === 0) {
                isFlippedHorizontally = !isFlippedHorizontally;
                const flipHorizontally = isFlippedHorizontally ? -1 : 1;

                $("#editShashinImage").css({
                    "transition": "transform 0.3s ease-in-out",
                    "transform": "translate(-50%, -50%) rotate(" + visualRotation + "deg) scaleY(" + flipHorizontally + ")"
                });
            } else {
                isFlippedVertically = !isFlippedVertically;
                const flipVertically = isFlippedVertically ? -1 : 1;

                $("#editShashinImage").css({
                    "transition": "transform 0.3s ease-in-out",
                    "transform": "translate(-50%, -50%) rotate(" + visualRotation + "deg) scaleX(" + flipVertically + ")"
                });
            }
        }
    });

    $("#editorRestoreActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        if ($("#editorSpinner").css("display") === "none") {
            $("#editorSpinner").css("display", "block");
            rotation = 0;
            visualRotation = 0;
            isFlippedHorizontally = false;
            isFlippedVertically = false;

            const http = new Http("Restore thumbnail");
            const version = Util.getMetadataLocalStorage();

            const json = {
                metadataId: editMetadataObj.id, //$("#metadataId").val(),
                rotation: 0,
                flipX: false,
                flipY: false
            };

            http.ajax("post", "/metadata/edit/thumbs" + (version === "" ? "?restore=true" : "?v=" + version + "&restore=true"), JSON.stringify(json)).then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    Util.setMetadataLocalStorage();

                    const retMetadata = data.metadata;
                    // Refresh image
                    const version = Util.getMetadataLocalStorage();
                    $("#photoThumbnailContainer" + retMetadata.id).css({
                        "width": retMetadata.thumbnailSmallWidth + "px",
                        "height": retMetadata.thumbnailSmallHeight + "px"
                    });
                    $("#image" + retMetadata.id).attr("src", $("#image" + retMetadata.id).attr("src") + "?v=" + uuidv4());
                    $("#image" + retMetadata.id).attr("width", retMetadata.thumbnailSmallWidth);
                    $("#image" + retMetadata.id).attr("height", retMetadata.thumbnailSmallHeight);
                    $("#editShashinImage").attr("src", "/api/v1/image/"+retMetadata.id+"?v="+uuidv4());

                    // Refresh lightgallery
                    const mediaContentList = shashin.getLightGallery().galleryItems;

                    if (shashin.getLightGallery() !== undefined && shashin.getLightGallery() !== null  && typeof shashin.getLightGallery().refresh === 'function' && mediaContentList.length > 0) {
                        let lightGalleryIndex = lgIndex;

                        if (/^-?\d+$/.test(lightGalleryIndex)) {
                            lightGalleryIndex = parseInt(lightGalleryIndex);
                            const mediaContent = mediaContentList[lightGalleryIndex];

                            if (mediaContent.hasOwnProperty("downloadUrl") &&
                                mediaContent.downloadUrl.includes(retMetadata.id)
                            ) {
                                mediaContentList[lightGalleryIndex].src = mediaContentList[lightGalleryIndex].src + "?v=" + Util.getMetadataLocalStorage();
                                const mediaLinkId = "#mediaLink" + retMetadata.id;
                                if ($(mediaLinkId).length > 0) {
                                    $(mediaLinkId).attr("data-src", encodeURI($(mediaLinkId).attr("data-src")).replace(";", "%3B") + "?v=" + uuidv4());
                                    if (parseInt($("img.lg-object.lg-image").attr("data-index")) === lightGalleryIndex) {
                                        $("img.lg-object.lg-image").attr("src", ($("img.lg-object.lg-image").attr("src") + "?v=" + uuidv4()));
                                    }
                                }
                            }
                        }

                        shashin.getLightGallery().refresh(mediaContentList);
                    }

                    $("#editorSpinner").css("display", "none");
                }
            });
        }
    });

    $("#editorSaveActionButton").off("click").on("click", function (e) {
        e.preventDefault();

        $("#editorSpinner").css("display", "block");

        shashin.processEditedThumbnail($("#metadataId").val(), lgIndex, rotation, isFlippedHorizontally, isFlippedVertically, function (success) {
            shashin.printMessageToConsole("Edited metadata:"+success,{tag:"editor"});

            $("#editorSpinner").css("display", "none");
            hideModule();
        });
    });

    function showModule() {
        $("#editorContainer").css("display", "block");
        $("#editorToolContainer").css("display", "block");

        $("#editorMedia").css("display", "block");
        $("#editorMedia").html("<img class='centerFit' id='editShashinImage' src='/api/v1/image/"+editMetadataId+"?v="+uuidv4()+"'>");
    }

    function hideModule() {
        $("#editorContainer").css("display", "none");
        $("#editorToolContainer").css("display", "none");

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