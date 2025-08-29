(function( shashin, $, undefined ) {
    function removeMiddleKeepEnds(arr) {
        if (arr.length <= 6) {
            return [...arr]; // If 6 or fewer elements, return a copy of the original array
        }
        const firstThree = arr.slice(0, 3);
        const lastThree = arr.slice(-3);
        return [...firstThree, ...lastThree];
    }

    function isLocalStorageAvailable(){
        const test = 'test';
        try {
            localStorage.setItem(test, test);
            localStorage.removeItem(test);
            return true;
        } catch(e) {
            return false;
        }
    }

    if (isLocalStorageAvailable()){
        localStorage.setItem("selectedMetadataIds", JSON.stringify([]));
        localStorage.setItem("selectedMetadataFilenames", JSON.stringify([]));
        localStorage.setItem("selectedMetadataThumbnails", JSON.stringify([]));
    }

    shashin.addToMetadataThumbnailsList = function(thumbnail) {
        if (isLocalStorageAvailable()) {
            let metadataThumbnailsArray = JSON.parse(localStorage.selectedMetadataThumbnails);
            if (metadataThumbnailsArray.indexOf(thumbnail) === -1) {
                metadataThumbnailsArray.push(thumbnail);
                if (metadataThumbnailsArray.length > 5) {
                    metadataThumbnailsArray = removeMiddleKeepEnds(metadataThumbnailsArray);
                }
                localStorage.setItem("selectedMetadataThumbnails", JSON.stringify(metadataThumbnailsArray));
            }
        } else {
            if ($("#multiSelectThumbnails").length > 0) {
                let metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
                if (metadataThumbnailsArray.indexOf(thumbnail) === -1) {
                    metadataThumbnailsArray.push(thumbnail);
                    if (metadataThumbnailsArray.length > 5) {
                        metadataThumbnailsArray = removeMiddleKeepEnds(metadataThumbnailsArray);
                    }
                    $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
                }
            }
        }
    };

    shashin.removeFromMetadataThumbnailsList = function(thumbnail) {
        if (isLocalStorageAvailable()) {
            let metadataThumbnailsArray = JSON.parse(localStorage.selectedMetadataThumbnails);
            if (metadataThumbnailsArray.length > 0 && metadataThumbnailsArray.indexOf(thumbnail) > -1) {
                const index = metadataThumbnailsArray.indexOf(thumbnail);
                if (index > -1) {
                    metadataThumbnailsArray.splice(index, 1);
                }
                localStorage.setItem("selectedMetadataThumbnails", JSON.stringify(metadataThumbnailsArray));
            }
        } else {
            if ($("#multiSelectThumbnails").length > 0) {
                const metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
                const index = metadataThumbnailsArray.indexOf(thumbnail);
                if (index > -1) {
                    metadataThumbnailsArray.splice(index, 1);
                }
                $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
            }
        }
    };

    shashin.getMetadataThumbnailsList = function() {
        if (isLocalStorageAvailable()) {
            let metadataThumbnailsArray = JSON.parse(localStorage.selectedMetadataThumbnails);
            if (metadataThumbnailsArray.length > 5) {
                metadataThumbnailsArray = removeMiddleKeepEnds(metadataThumbnailsArray);
            }
            return metadataThumbnailsArray;
        } else {
            if ($("#multiSelectThumbnails").length > 0) {
                let thumbnailList = JSON.parse($("#multiSelectThumbnails").val());

                if (thumbnailList.length > 5) {
                    thumbnailList = removeMiddleKeepEnds(thumbnailList);
                }
                return thumbnailList;
            }
        }

        return [];
    };

    shashin.removeFromMetadataFilenamesList = function(filename) {
        if (isLocalStorageAvailable()) {
            let metadataFilenamesArray = JSON.parse(localStorage.selectedMetadataFilenames);

            if (metadataFilenamesArray.length > 0 && metadataFilenamesArray.indexOf(filename) > -1) {
                const index = metadataFilenamesArray.indexOf(filename);
                if (index > -1) {
                    metadataFilenamesArray.splice(index, 1);
                }
                localStorage.setItem("selectedMetadataFilenames", JSON.stringify(metadataFilenamesArray));
            }
        } else {
            if ($("#multiSelectFilenames").length > 0) {
                const metadataFilenamesArray = shashin.getMetadataFilenamesList();
                const index = metadataFilenamesArray.indexOf(filename);
                if (index > -1) {
                    metadataFilenamesArray.splice(index, 1);
                }
                $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
            }
        }
    };

    shashin.getMetadataFilenamesList = function() {
        if (isLocalStorageAvailable()) {
            let metadataFilenamesArray = JSON.parse(localStorage.selectedMetadataFilenames);
            if (metadataFilenamesArray.length > 5) {
                metadataFilenamesArray = removeMiddleKeepEnds(metadataFilenamesArray);
            }
            return metadataFilenamesArray;
        } else {
            if ($("#multiSelectFilenames").length > 0) {
                let filenameList = JSON.parse($("#multiSelectFilenames").val());

                if (filenameList.length > 5) {
                    filenameList = removeMiddleKeepEnds(filenameList);
                }
                return filenameList;
            }
        }

        return [];
    };

    shashin.addToMetadataFilenamesList = function (filename) {
        if (isLocalStorageAvailable()) {
            let metadataFilenamesArray = JSON.parse(localStorage.selectedMetadataFilenames);
            if (metadataFilenamesArray.indexOf(filename) === -1) {
                metadataFilenamesArray.push(filename);
                if (metadataFilenamesArray.length > 5) {
                    metadataFilenamesArray = removeMiddleKeepEnds(metadataFilenamesArray);
                }
                localStorage.setItem("selectedMetadataFilenames", JSON.stringify(metadataFilenamesArray));
            }
        } else {
            if ($("#multiSelectFilenames").length > 0) {
                let metadataFilenamesArray = shashin.getMetadataFilenamesList();
                if (metadataFilenamesArray.indexOf(filename) === -1) {
                    metadataFilenamesArray.push(filename);
                    if (metadataFilenamesArray.length > 5) {
                        metadataFilenamesArray = removeMiddleKeepEnds(metadataFilenamesArray);
                    }
                    $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
                }
            }
        }
    };

    shashin.addToMetadataIdList = function (metadataId) {
        if (isLocalStorageAvailable()) {
            let metadataIdsArray = JSON.parse(localStorage.selectedMetadataIds);
            if (metadataIdsArray.indexOf(metadataId) === -1) {
                metadataIdsArray.push(metadataId);
                localStorage.setItem("selectedMetadataIds", JSON.stringify(metadataIdsArray));
            }
        } else {
            if ($("#multiSelectMetadataIds").length > 0) {
                const metadataIdArray = shashin.getMetadataIdList();
                if (metadataIdArray.indexOf(metadataId) === -1) {
                    metadataIdArray.push(metadataId);
                    $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
                }
            }
        }
    };

    shashin.removeFromMetadataIdList = function (metadataId) {
        if (isLocalStorageAvailable()) {
            let metadataIdsArray = JSON.parse(localStorage.selectedMetadataIds);
            if (metadataIdsArray.length > 0 && metadataIdsArray.indexOf(metadataId) > -1) {
                const index = metadataIdsArray.indexOf(metadataId);
                if (index > -1) {
                    metadataIdsArray.splice(index, 1);
                }
                localStorage.setItem("selectedMetadataIds", JSON.stringify(metadataIdsArray));
            }
        } else {
            if ($("#multiSelectMetadataIds").length > 0) {
                const metadataIdArray = shashin.getMetadataIdList();
                const index = metadataIdArray.indexOf(metadataId);
                if (index > -1) {
                    metadataIdArray.splice(index, 1);
                }
                $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
            }
        }
    };

    shashin.getMetadataIdList = function() {
        if (isLocalStorageAvailable()) {
            return JSON.parse(localStorage.selectedMetadataIds);
        } else {
            if ($("#multiSelectMetadataIds").length > 0) {
                return JSON.parse($("#multiSelectMetadataIds").val());
            }
        }

        return [];
    };

    shashin.removeAllMetadataIdList = function () {
        if (isLocalStorageAvailable()) {
            localStorage.setItem("selectedMetadataIds", JSON.stringify([]));
        } else {
            if ($("#multiSelectMetadataIds").length > 0) {
                $("#multiSelectMetadataIds").val(JSON.stringify([]));
            }
        }
    };

    shashin.removeAllMetadataFilenamesList = function () {
        if (isLocalStorageAvailable()) {
            localStorage.setItem("selectedMetadataFilenames", JSON.stringify([]));
        } else {
            if ($("#multiSelectFilenames").length > 0) {
                $("#multiSelectFilenames").val(JSON.stringify([]));
            }
        }
    };

    shashin.removeAllMetadataThumbnailsList = function () {
        if (isLocalStorageAvailable()) {
            localStorage.setItem("selectedMetadataThumbnails", JSON.stringify([]));
        } else {
            if ($("#multiSelectThumbnails").length > 0) {
                $("#multiSelectThumbnails").val(JSON.stringify([]));
            }
        }
    };
}( window.shashin = window.shashin || {}, jQuery ));