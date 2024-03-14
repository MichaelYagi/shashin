async function showMap(mapdata) {
    let qslat = Util.getParameterByName("lat");
    let qslng = Util.getParameterByName("lng");
    let version = Util.getMetadataLocalStorage();
    const qslatlng = Util.getParameterByName("latlng");
    // Must be format yyyy-mm-dd
    const qssd = Util.getParameterByName("sd");
    const qsed = Util.getParameterByName("ed");
    const qsvo = Util.getParameterByName("vo");
    // Media type filter
    const qsmtf = Util.getParameterByName("mtf");
    const qsaid = Util.getParameterByName("aid");

    const videoOnlyCheckbox = $("#videoOnlyInput");
    const showMarkersCheckbox = $("#showMarkersInput");
    const startDateField = $("#startDateInput");
    const endDateField = $("#endDateInput");
    const filterInputs = $("#filterInputs");
    const albumSelect = $("#albumSelect");
    const progressBarWrapper = $("#progressBarWrapper");
    const progressBar = $("#progressBar");

    let filtered = false;
    let originalFromInput = "";
    let originalToInput = "";
    let originalAlbumFilter = "";
    let originalVideoOnly = "";
    let originalMapMarkers = "";

    shashin.mouseMoveListener();

    const textFill = new ol.style.Fill({
        color: '#fff',
    });
    const textStroke = new ol.style.Stroke({
        color: 'rgba(0, 0, 0, 0.6)',
        width: 5,
    });
    const invisibleFill = new ol.style.Fill({
        color: 'rgba(255, 255, 255, 0.01)',
    });

    // Set an initial date to 500 photos ago
    let initIndex = 500;
    if (mapdata.length < initIndex) {
        initIndex = mapdata.length;
    }
    initIndex = initIndex - 1;

    // Date fields are format "yyyy-MM-dd"
    let initialStartDate = "";
    if (mapdata.length > 0) {
    initialStartDate = mapdata[initIndex]["year"] + '-' +
        ((mapdata[initIndex]["month"] > 9) ? (mapdata[initIndex]["month"]) : ('0' + (mapdata[initIndex]["month"]))) + '-' +
        ((mapdata[initIndex]["day"] > 9) ? mapdata[initIndex]["day"] : ('0' + mapdata[initIndex]["day"]));
    }
    startDateField.val(initialStartDate);

    let initialCoord = [-73.1234, 45.678];
    let initialZoom = 2;
    if (qslat !== null && qslng !== null && qslat !== '' && qslng !== '') {
        if (true === isValidQsLatLon(qslat,qslng)) {
            initialCoord = [qslng, qslat];
            initialZoom = shashin.initialMapZoom;
            startDateField.val("");
        } else {
            shashin.showToastMessage("Validation error", "Invalid lat/lng format.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
        }
    } else if (qslatlng !== null && qslatlng !== "") {
        const latlngArr = qslatlng.split(",");

        if (latlngArr.length > 1) {
            qslat = latlngArr[0].trim();
            qslng = latlngArr[1].trim();

            if (true === isValidQsLatLon(qslat, qslng)) {
                initialCoord = [qslng, qslat];
                initialZoom = shashin.initialMapZoom;
                startDateField.val("");
            } else {
                shashin.showToastMessage("Validation error", "Invalid lat/lng format.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            }
        } else {
            shashin.showToastMessage("Validation error", "Invalid lat/lng format.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
        }
    } else if (Util.localStorageAvailable() === true && "lat" in localStorage && "lng" in localStorage) {
        initialCoord = [localStorage.getItem("lng"), localStorage.getItem("lat")];
        initialZoom = shashin.initialMapZoom;
        startDateField.val("");
        localStorage.removeItem('lat');
        localStorage.removeItem('lng');
    } else if (Util.localStorageAvailable() === true && "latlng" in localStorage) {
        const latlngArr = localStorage.getItem("latlng").split(",");

        if (latlngArr.length > 1) {
            const lslat = latlngArr[0].trim();
            const lslng = latlngArr[1].trim();

            initialCoord = [lslat, lslng];
            initialZoom = shashin.initialMapZoom;
            startDateField.val("");
            localStorage.removeItem('latlng');
        }
    }

    // Query param takes precedence over localstorage
    if ((qssd !== null && qssd !== "") || (qsed !== null && qsed !== "") || qsvo !== null) {
        if (qssd !== null && qssd !== "") {
            if (true === isValidQsDate(qssd)) {
                startDateField.val(qssd);
            } else {
                shashin.showToastMessage("Validation error", "Date format must be yyyy-mm-dd.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            }
        }
        if (qsed !== null && qsed !== "") {
            if (true === isValidQsDate(qsed)) {
                endDateField.val(qsed);
            } else {
                shashin.showToastMessage("Validation error", "Date format must be yyyy-mm-dd.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            }
        }
        if (qsvo !== null) {
            videoOnlyCheckbox.prop("checked", qsvo === "true");
        }
    } else if (
      Util.localStorageAvailable() === true &&
        ("sd" in localStorage || "ed" in localStorage || "vo" in localStorage)
    ) {
        const sd = localStorage.getItem("sd");
        const ed = localStorage.getItem("ed");
        const vo = localStorage.getItem("vo") === "true";

        if (sd !== "" && sd !== null) {
            startDateField.val(sd);
        }
        if (ed !== "" && ed !== null) {
            endDateField.val(ed);
        }
        videoOnlyCheckbox.prop("checked",vo);

        localStorage.removeItem("sd");
        localStorage.removeItem("ed");
        localStorage.removeItem("vo");
    }

    function isValidQsDate(dateString) {
        if (dateString === "" || dateString === null) {
            return true;
        }

        const regEx = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateString.match(regEx)) {
            return false;
        }
        // Invalid format
        const d = new Date(dateString);
        const dNum = d.getTime();
        if (!dNum && dNum !== 0) {
            return false;
        }
        // NaN value, Invalid date
        return d.toISOString().slice(0, 10) === dateString;
    }

    function isValidQsLatLon(lat, lon) {
        const regexLat = /^(-?[1-8]?\d(?:\.\d{1,18})?|90(?:\.0{1,18})?)$/;
        const regexLon = /^(-?(?:1[0-7]|[1-9])?\d(?:\.\d{1,18})?|180(?:\.0{1,18})?)$/;

        let validLat = regexLat.test(lat);
        let validLon = regexLon.test(lon);
        return validLat && validLon;
    }
    
    function validateDate(d) {
        if (Object.prototype.toString.call(d) === "[object Date]") {
            // it is a date
            if (isNaN(d)) { // d.getTime() or d.valueOf() will also work
                // date object is not valid
                return null;
            } else {
                // date object is valid
                return new Date(d.getTime() - d.getTimezoneOffset() * -60000);
            }
        } else {
            // not a date object
            return null;
        }
    }

    function checkDates(takenAtDateFormat,startDateFormat,endDateFormat) {
        shashin.printMessageToConsole("startDateFormat before validateDate reformatted: " + startDateFormat);
        shashin.printMessageToConsole("endDateFormat before validateDate reformatted: " + endDateFormat);
        shashin.printMessageToConsole("takenAtDateFormat before validateDate reformatted: " + takenAtDateFormat);

        startDateFormat = validateDate(startDateFormat);
        endDateFormat = validateDate(endDateFormat);
        takenAtDateFormat = validateDate(takenAtDateFormat);

        shashin.printMessageToConsole("startDateFormat after validateDate reformatted: " + startDateFormat);
        shashin.printMessageToConsole("endDateFormat after validateDate reformatted: " + endDateFormat);
        shashin.printMessageToConsole("takenAtDateFormat after validateDate reformatted: " + takenAtDateFormat);
        shashin.printMessageToConsole("-----------");

        if (takenAtDateFormat !== null) {
            if (startDateFormat && endDateFormat) {
                return takenAtDateFormat >= startDateFormat && takenAtDateFormat <= endDateFormat;
            } else if (endDateFormat) {
                return takenAtDateFormat <= endDateFormat;
            } else if (startDateFormat) {
                return takenAtDateFormat >= startDateFormat;
            } else return startDateFormat === null && endDateFormat === null;
        }

        return false;
    }

    function checkDateInputs(startDateFormat,endDateFormat) {
        shashin.printMessageToConsole("startDateFormat before processing: " + startDateFormat);
        shashin.printMessageToConsole("endDateFormat before processing: " + endDateFormat);

        startDateFormat = validateDate(startDateFormat);
        endDateFormat = validateDate(endDateFormat);

        shashin.printMessageToConsole("startDateFormat after processing: " + startDateFormat);
        shashin.printMessageToConsole("endDateFormat after processing: " + endDateFormat);
        shashin.printMessageToConsole("-----------");

        if (startDateField.val() === "" && endDateField.val() === "") {
            return true;
        } else if (startDateField.val() !== "" && startDateFormat == null && endDateField.val() !== "" && endDateFormat === null) {
            shashin.showToastMessage("Validation error", "Invalid dates.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            return false;
        } else if (startDateFormat && endDateFormat) {
            if (endDateFormat < startDateFormat) {
                shashin.showToastMessage("Validation error", "Start date must be before end date.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            }
            return endDateFormat >= startDateFormat;
        } else if (startDateField.val() !== "" && startDateFormat === null) {
            shashin.showToastMessage("Validation error", "Invalid start date.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            return false;
        } else if (endDateField.val() !== "" && endDateFormat === null) {
            shashin.showToastMessage("Validation error", "Invalid end date.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            return false;
        }

        return true;
    }

    function setLayer(startDate, endDate, videoOnly, metadataList, resetMap) {
        version = Util.getMetadataLocalStorage();
        map.removeLayer(vectorLayer);
        const iconFeatures = [];

        if (resetMap === undefined) {
            resetMap = false;
        }

        progressBarWrapper.visible();

        let minLat = null;
        let maxLat = null;
        let minLng = null;
        let maxLng = null;

        for (let index in mapdata) {
            const data = mapdata[index];

            const currentProgress = (parseInt(index) + 1) / mapdata.length * 100;
            progressBar.attr("aria-valuenow", currentProgress.toString());
            const width = currentProgress.toString() + "%";
            progressBar.css("width", width);
            shashin.printMessageToConsole("currentProgress for map: "+currentProgress.toString());

            if ((videoOnly === true && data["type"].includes("video") === false) || (qsmtf !== null && qsmtf !== "" && data["type"].includes(qsmtf) === false)) {
                continue;
            }

            if (metadataList !== undefined && metadataList.length > 0 && $.inArray(data["id"], metadataList) === -1) {
                continue;
            }

            if (data["lat"] !== null && data["lng"] !== null &&
                data["lat"] !== "" && data["lng"] !== "") {

                const lat = data["lat"];
                const lng = data["lng"];

                let dateTakenObj = new Date(data["year"],parseInt(data["month"])-1,data["day"]);

                let startDateObj = null;
                let dateArray = null;
                let year = null;
                let month = null;
                let day = null;

                if (startDate) {
                    dateArray = startDate.split("-");
                    year = dateArray[0];
                    month = parseInt(dateArray[1], 10) - 1;
                    day = dateArray[2];
                    startDateObj = new Date(year, month, day);
                }

                let endDateObj = null;
                if (endDate) {
                    dateArray = endDate.split("-");
                    year = dateArray[0];
                    month = parseInt(dateArray[1], 10) - 1;
                    day = dateArray[2];
                    endDateObj = new Date(year, month, day);
                }

                if (true === checkDates(dateTakenObj,startDateObj,endDateObj)) {
                    const mapMarkerIcon = new ol.style.Style({
                        //geometry: feature.getGeometry(),
                        image: new ol.style.Icon(({
                            anchor: [0.5, 46],
                            anchorXUnits: 'fraction',
                            anchorYUnits: 'pixels',
                            opacity: 1.0,
                            src: encodeURI(data["mapMarkerUrl"]) + (version === "" ? "" : "?v=" + version)
                        }))
                    });

                    const iconFeature = new ol.Feature({
                        geometry: new ol.geom.Point(ol.proj.transform([data["lng"], data["lat"]], 'EPSG:4326', 'EPSG:900913')),
                        thumbnailUrlSmall: data["thumbnailUrlSmall"],
                        thumbnailUrlOriginal: data["thumbnailUrlOriginal"],
                        mapMarkerUrl: data["mapMarkerUrl"],
                        mapMarkerIcon: mapMarkerIcon,
                        videoUrl: data["videoUrl"],
                        originalImageWidth: data["originalImageWidth"],
                        originalImageHeight: data["originalImageHeight"],
                        metadataId: data["id"],
                        lat: lat,
                        lng: lng,
                        type: data["type"]
                    });

                    iconFeature.setStyle(data["mapMarkerIcon"]);
                    iconFeatures.push(iconFeature);

                    if (minLat === null || lat < minLat) {
                        minLat = lat;
                    }

                    if (maxLat === null || lat > maxLat) {
                        maxLat = lat;
                    }

                    if (minLng === null || lng < minLng) {
                        minLng = lng;
                    }

                    if (maxLng === null || lng > maxLng) {
                        maxLng = lng;
                    }
                }
            }
        }

        progressBar.attr("aria-valuenow", 0);
        progressBar.css("width", "0%");
        progressBarWrapper.invisible();
        $("#mapFilterButton").removeClass("disabled");

        if (iconFeatures.length > 0) {

            const vectorSource = new ol.source.Vector({
                features: iconFeatures //add an array of features
            });

            let clusterDistance = 50;
            if (showMarkersCheckbox.prop("checked") === true) {
                clusterDistance = 200;
            }

            const clusterSource = new ol.source.Cluster({
                distance: clusterDistance, // Bigger number for better performance, smaller number for better accuracy
                source: vectorSource,
            });

            vectorLayer = new ol.layer.Vector({
                source: clusterSource,
                style: styleFunction
            });

            map.addLayer(vectorLayer);

            shashin.printMessageToConsole("minLat for map filtering: "+minLat);
            shashin.printMessageToConsole("minLng for map filtering: "+minLng)
            shashin.printMessageToConsole("maxLat for map filtering: "+maxLat)
            shashin.printMessageToConsole("maxLng for map filtering: "+maxLng)

            if (resetMap === false && minLat !== null && minLng !== null && maxLat !== null && maxLng !== null) {
                const mapSize = map.getSize();
                const mapSizeAdjust = 200;
                let sizeX = mapSize[0]-mapSizeAdjust;
                if (sizeX <= 0) {
                    sizeX = mapSize[0];
                }
                let sizeY = mapSize[1]-mapSizeAdjust;
                if (sizeY <= 0) {
                    sizeY = mapSize[1];
                }
                map.getView().fit(ol.proj.transformExtent([minLng, minLat, maxLng, maxLat], 'EPSG:4326', map.getView().getProjection()), { size: [sizeX,sizeY] });
            } else {
                map.getView().setZoom(initialZoom);
            }

            initialZoom = 2;
        }
    }

    function editLocation(...args) {
        const locationArgs = [].concat(...args);
        let metadataId = "";

        if (arguments.length > 0) {
            metadataId = locationArgs[0];
        }

        $("#metadataId").val(metadataId);

        shashin.openEditMetadataModal(metadataId);
    }

    let maxFeatureCount;
    let vectorLayer = null;
    const calculateClusterInfo = function (resolution) {
        maxFeatureCount = 0;
        const features = vectorLayer.getSource().getFeatures();
        let feature, radius;

        // Sort by number of features for radius calculation
        features.sort((a, b) => a.get('features').length > b.get('features').length ? 1 : -1);

        shashin.printMessageToConsole("============================================");
        shashin.printMessageToConsole("number of features:" + features.length);

        for (let i = features.length - 1; i >= 0; --i) {
            feature = features[i];
            const originalFeatures = feature.get('features');
            const extent = ol.extent.createEmpty();
            let j, jj;
            for (j = 0, jj = originalFeatures.length; j < jj; ++j) {
                ol.extent.extend(extent, originalFeatures[j].getGeometry().getExtent());
            }
            maxFeatureCount = Math.max(maxFeatureCount, jj);

            //radius = (0.45 * (ol.extent.getWidth(extent) + ol.extent.getHeight(extent))) / resolution;
            radius = (15 * ((maxFeatureCount + jj) / maxFeatureCount));

            shashin.printMessageToConsole("---------------------------");
            shashin.printMessageToConsole("originalRadius:" + (0.45 * (ol.extent.getWidth(extent) + ol.extent.getHeight(extent))) / resolution);
            shashin.printMessageToConsole("extentWidth:" + ol.extent.getWidth(extent));
            shashin.printMessageToConsole("extentHeight:" + ol.extent.getHeight(extent));
            shashin.printMessageToConsole("resolution:" + resolution);
            shashin.printMessageToConsole("Feature count:" + jj);
            shashin.printMessageToConsole("maxFeatureCount:" + maxFeatureCount);
            shashin.printMessageToConsole("radius: " + radius);

            feature.set('radius', radius);
        }
    };

    let currentResolution;

    function styleFunction(feature, resolution) {
        calculateClusterInfo(resolution);
        if (resolution !== currentResolution) {
            currentResolution = resolution;
        }

        const features = feature.get('features');
        const size = features.length;

        let style = {
            image: new ol.style.Circle({
                radius: feature.get('radius'),
                fill: new ol.style.Fill({
                    color: [0, 77, 255, Math.min(0.8, 0.4 + size / maxFeatureCount)],
                }),
            })
        };
        if (size > 2) {
            style["text"] = new ol.style.Text({
                text: size.toString(),
                fill: textFill,
                stroke: textStroke,
                scale: 1.5
            });
        }

        const thisStyle = new ol.style.Style(style);

        if (showMarkersCheckbox.prop("checked") === true) {
            if (size > 1) {
                return thisStyle;
            } else {
                // Performance hit when there are a lot of markers
                const originalFeature = features[0];
                return originalFeature.get('mapMarkerIcon');
            }
        } else {
            return thisStyle;
        }
    }

    let clicked = false;
    function selectStyleFunction(feature) {
        let styles = [];

        if (clicked === true) {
            // Return first map marker
            styles = [
                new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: feature.get('radius'),
                        fill: invisibleFill,
                    }),
                }),
            ];
            const originalFeatures = feature.get('features');
            let originalFeature = originalFeatures[originalFeatures.length - 1];
            styles.push(originalFeature.getProperties()["mapMarkerIcon"]);

            // Show gallery for each cluster
            const mediaContentList = [];
            for (let i = originalFeatures.length - 1; i >= 0; --i) {
                const featureProperties = originalFeatures[i].getProperties();

                const mediaContent = {
                    metadataDetailFun: editLocation,
                    args: [
                        featureProperties["metadataId"]
                    ]
                };

                if (featureProperties.type.includes("image")) {
                    mediaContent.src = featureProperties.thumbnailUrlOriginal;
                    mediaContent.downloadUrl = encodeURI(featureProperties.thumbnailUrlOriginal) + "/download";
                } else if (featureProperties.type.includes("video")) {
                    mediaContent.video = {
                        "source": [{"src": featureProperties.videoUrl, "type": "video/mp4"}],
                        "attributes": {
                            "preload": "auto",
                            "controls": true,
                            "autoplay": true,
                            "id": featureProperties.metadataId
                        }
                    }

                    mediaContent.poster = ((featureProperties.thumbnailUrlOriginal === null || featureProperties.thumbnailUrlOriginal === "") ? featureProperties.thumbnailUrlSmall : encodeURI(featureProperties.thumbnailUrlOriginal)) + "?v=" + Util.getMetadataLocalStorage();
                    mediaContent.lgSize = featureProperties.originalImageWidth+"-"+featureProperties.originalImageHeight;
                    mediaContent.downloadUrl = encodeURI(featureProperties.videoUrl) + "/download";
                }
                mediaContent.metadataId = featureProperties.metadataId;
                mediaContent.subHtml = featureProperties.description;
                mediaContentList.push(mediaContent);
            }

            // Destroy gallery instance hack
            dynamicGallery.closeGallery(true);
            dynamicGallery.destroyModules(true);
            dynamicGallery.invalidateItems();
            $(window).off(`.lg.global${dynamicGallery.lgId}`);
            dynamicGallery.LGel.off('.lg');
            setTimeout(() => {
                // https://github.com/sachinchoolur/lightGallery/blob/383d51852657ab44bb8697748c570cf110723f97/src/lightgallery.ts#L2396
                // Hack because lg.destroy() errors out
                // when photos appear slower than destroy called, then there's an error
                try {
                    dynamicGallery.$container.remove();
                } catch (e) {
                    shashin.printMessageToConsole("Error removing lightGallery instance: "+e.message);
                }
                lightGalleryConfigs["dynamicEl"] = mediaContentList;
                dynamicGallery = lightGallery($dynamicGallery, lightGalleryConfigs);
                dynamicGallery.openGallery(0);
            }, 500);
        }

        clicked = false;

        return styles;
    }

    const mapView = new ol.View({
        center: ol.proj.fromLonLat(initialCoord),
        zoom: initialZoom
    });

    const lightGalleryConfigs = shashin.getLightGalleryConfigs();
    lightGalleryConfigs["plugins"].push(lgMetadataDetail);
    lightGalleryConfigs["plugins"].push(lgCastMedia);
    lightGalleryConfigs["controls"] = true;
    lightGalleryConfigs["dynamic"] = true;
    lightGalleryConfigs["counter"] = true;
    lightGalleryConfigs["metadataDetail"] = true;
    // lightGalleryConfigs["editLocation"] = true;
    // lightGalleryConfigs["showControls"] = showControls;
    lightGalleryConfigs["castMedia"] = true;

    const $dynamicGallery = document.getElementById('light-gallery-photo');
    let dynamicGallery = lightGallery($dynamicGallery, lightGalleryConfigs);

    const duration = 400;
    const interactions = [
        new ol.interaction.Select({
            condition: function (evt) {
                // TODO: Fix selectStyleFunction, it should run only once per click
                //  It runs twice because a style is set for the radius
                clicked = evt.type === 'singleclick';
                return clicked;
            },
            style: selectStyleFunction,
        }),
        new ol.interaction.DoubleClickZoom({
            duration: duration
        }),
        new ol.interaction.KeyboardPan({
            pixelDelta: 256
        }),
        new ol.interaction.KeyboardZoom({
            duration: duration
        }),
        new ol.interaction.MouseWheelZoom({
            duration: duration
        }),
        new ol.interaction.PinchRotate(),
        new ol.interaction.PinchZoom({
            duration: duration
        }),
        new ol.interaction.DragPan({
            kinetic: new ol.Kinetic(-0.005, 0.05, 100)
        }),
        new ol.interaction.DblClickDragZoom(),
        new ol.interaction.DragZoom(),
        new ol.interaction.DragRotate()
    ];

    const map = new ol.Map({
        target: 'map',
        interactions: interactions,
        layers: [
            new ol.layer.Tile({
                source: shashin.getMapSource("osm"),
                visible: true
            })
        ],
        view: mapView,
        controls: []
    });

    const attributions = new ol.control.Attribution({collapsible: true});

    map.addControl(attributions);

    const processCopyText = function(obj, copyText, msgType) {
        const tempText = document.createElement("input");
        tempText.value = copyText;
        tempText.type = "hidden";
        tempText.id = "tempClipboardMapId";
        tempText.setAttribute('data-clipboard-text', copyText);
        document.body.appendChild(tempText);
        tempText.select();

        const clipboard = new ClipboardJS('#tempClipboardMapId');

        $("#tempClipboardMapId").on( "click", function () {

            clipboard.on('success', function(e) {
                shashin.showToastMessage(msgType + "copied to clipboard", e.text + " copied to clipboard", {icon:"bi-info-circle", iconColor:"#777777"});
            });

            clipboard.on('error', function(e) {
                shashin.showToastMessage("Could not copy " + msgType, copyText + " could not be copied: " + e, {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
            });
        });
        $("#tempClipboardMapId").trigger( "click" );

        $("#tempClipboardMapId").remove();
        clipboard.destroy();
    }

    const copyPlacename = function (obj) {
        if (obj.hasOwnProperty("data") && obj.data !== null && obj.data !== "" && obj.data.placename !== null && obj.data.placename !== "") {
            const copyText = obj.data.placename;
            processCopyText(obj, copyText, "location");
        }
    }

    const copyCoordinates = function (obj) {
        const coordArray = ol.proj.toLonLat(obj.coordinate);
        if (coordArray.length > 1) {
            const copyText = coordArray[1]+","+coordArray[0];
            processCopyText(obj, copyText, "coordinates");
        }
    };

    const contextmenu = new ContextMenu({
        width: 300,
        defaultItems: false // defaultItems are (for now) Zoom In/Zoom Out
    });
    contextmenu.on('close', function (evt) {
        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties()["name"] === "tempCoordinates") {
                map.removeLayer(layer);
            }
        });
    });

    function showContextMenu(evt, coordArray, data) {
        let placeJson = {};
        if (data.hasOwnProperty("msg") && data.hasOwnProperty("status") && data.hasOwnProperty("placedata") && data["status"] === shashin.apiResponse.SUCCESS) {
            placeJson = JSON.parse(data["placedata"]);
        }

        shashin.printMessageToConsole("Placedata:");
        shashin.printMessageToConsole(placeJson);

        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") && (layer.getProperties()["name"] === "tempCoordinates" || layer.getProperties()["name"] === "tempQpCoordinates")) {
                map.removeLayer(layer);
            }
        });

        renderMarker('tempCoordinates', coordArray[1], coordArray[0], "grey");

        const copyText = coordArray[1] + "," + coordArray[0];
        contextmenu.updatePosition([evt.pixel[0], evt.pixel[1] + 12]);
        const contextValueArray = [];

        if (placeJson.hasOwnProperty("name") && placeJson["name"] !== null && placeJson["name"] !== "") {
            const contextItem = {
                text: "<strong>" + placeJson["name"] + "</strong>",
                // classname: "ol-ctx-menu-separator" // Make unselectable text
                classname: "context-text-wrap",
                callback: copyPlacename
            }
            contextItem.data = { placename: placeJson["name"] };

            contextValueArray.push(
                contextItem,
                "-"
            );
        }

        contextValueArray.push({
            text: copyText,
            callback: copyCoordinates
        });

        contextmenu.extend(contextValueArray);
    }

    contextmenu.on('open', function (evt) {
        const coordArray = ol.proj.toLonLat(evt.coordinate);
        const http = new Http("get place data");
        contextmenu.clear();

        if (coordArray.length > 1) {
            const json = {
                lat: coordArray[1],
                lng: coordArray[0]
            };

            if (shashin.showPlacename === true) {
                http.ajax("post", "/placedata", JSON.stringify(json)).then(function (data) {
                    showContextMenu(evt, coordArray, data);
                });
            } else {
                showContextMenu(evt, coordArray, {});
            }
        }
    });
    map.addControl(contextmenu);

    // Query params marker
    if (qslat !== "" && qslng !== "") {
        renderMarker('tempQpCoordinates',qslat,qslng,"red");
    }

    // Query params for albumId
    if (qsaid !== null && qsaid !== "") {
        const options = $('#albumSelect option');
        const albumIds = $.map(options, function (option) {
            return option.value;
        });

        if (albumIds.includes(qsaid)) {
            // Clear dates
            startDateField.val("");
            endDateField.val("");
            albumSelect.val(qsaid);
            renderAlbumSelected();
        } else {
            shashin.showToastMessage("Album does not exist", "Invalid album ID " + qsaid + ".", {icon:"bi-exclamation-triangle", iconColor:"#FF0000"});
        }
    }

    map.on('click', function () {
        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties()["name"] === "tempQpCoordinates") {
                map.removeLayer(layer);
            }
        });
    });

    map.once("postrender", function() {
        filterInputs.visible();
    });

    // After closing lightgallery, clear select interaction
    $dynamicGallery.addEventListener('lgAfterClose', function () {
        map.getInteractions().forEach(function (interaction) {
            if (interaction instanceof ol.interaction.Select) {
                interaction.getFeatures().clear();
            }
        });
    });

    // Close gallery on browser/mobile back button
    $dynamicGallery.addEventListener('lgAfterOpen', function () {
        if (window.history && window.history.pushState) {
            window.history.pushState('forward', null, "");

            $(window).on('popstate', function() {
                dynamicGallery.closeGallery();
            });

        }
    });

    checkDateInputs(new Date(startDateField.val()),new Date(endDateField.val()))
    setLayer(startDateField.val(),endDateField.val(),videoOnlyCheckbox.prop("checked"),[],true);

    $("#filterMap").on("click", function(e) {
        e.preventDefault();

        filtered = true;
        shashin.showToastMessage("Applying filter", "Applying filter", {icon:"bi-info-circle", iconColor:"#777777", autohide: false});
        if (true === setLayerInputs()) {
            shashin.showToastMessage("Filter applied", "Filter applied", {
                icon: "bi-info-circle",
                iconColor: "#777777",
                delay: 3000
            });

            $("#propMapFilter").modal('hide');
        }
    });

    albumSelect.on("change", function(e) {
        if ($(this).val() !== "0") {
            startDateField.val("");
            endDateField.val("");

            // const albumId = $(this).val();
            // // Query metadata in album with lat/lng
            // const http = new Http("get album data");
            //
            // http.ajax("get", "/album/mapdata/"+albumId).then(function (data) {
            //     if (data.hasOwnProperty("albummapdata")) {
            //         setLayer(startDateField.val(),endDateField.val(),videoOnlyCheckbox.prop("checked"), data["albummapdata"]);
            //     }
            // });
        }
    });

    // get all date input fields
    let allDateInputs = document.querySelectorAll('[type="date"]');
    allDateInputs.forEach(el => {
        const singleDateInput = $(el);

        // register double click event to change date input to text input and select the value
        singleDateInput.on('dblclick', () => {
            el.type = "text";
            el.placeholder = "yyyy-mm-dd";

            // After changing input type with JS .select() wont work as usual
            // Needs timeout fn() to make it work
            setTimeout(() => {
                el.select();
            })
        });

        // register the focusout event to reset the input back to a date input field
        singleDateInput.on('focusout', () => {
            el.type = "date";
            el.placeholder = "mm/dd/yyyy";
        });
        singleDateInput.on('keypress',function(e) {
            // Enter key
            if(e.which === 13) {
                el.type = "date";
                el.placeholder = "mm/dd/yyyy";
            }
        });
    });

    function setLayerInputs() {
        const dateInputsValid = checkDateInputs(new Date(startDateField.val()),new Date(endDateField.val()));

        // Validate fields
        if (true === dateInputsValid) {
            initialZoom = 2; //map.getView().getZoom();

            if (false === renderAlbumSelected()) {
                // Filter results
                setLayer(startDateField.val(),endDateField.val(),videoOnlyCheckbox.prop("checked"));
            }
        }

        return dateInputsValid;
    }

    function renderAlbumSelected() {
        if (albumSelect.length > 0 && albumSelect.val() !== "0") {
            const albumId = albumSelect.val();
            // Query metadata in album with lat/lng
            const http = new Http("get album map data");

            http.ajax("get", "/album/mapdata/" + albumId).then(function (data) {
                if (data.hasOwnProperty("albummapdata")) {
                    setLayer(startDateField.val(), endDateField.val(), videoOnlyCheckbox.prop("checked"), data["albummapdata"]);
                }
            });

            return true;
        }

        return false;
    }

    function renderMarker(id,lat,lng,color) {
        if (color === undefined || color === null || color === "") {
            color = "grey";
        }
        if (lat && lat !== "" && lng && lng !== "") {
            const qslatlngfeature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([lng, lat])),
                name: 'tempMarker'
            });

            const iconSize = 25;

            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: ' + color + ';" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
            const icon = 'data:image/svg+xml;utf8,' + svg;

            const qslatlngstyleIcon = new ol.style.Style({
                image: new ol.style.Icon({
                    opacity: 1,
                    src: icon,
                    anchor: [0.5, iconSize],
                    anchorXUnits: 'fraction',
                    anchorYUnits: 'pixels',
                    anchorOrigin: 'top-left',
                    offset: [0, 0]
                })
            });

            qslatlngfeature.setStyle(qslatlngstyleIcon);
            qslatlngfeature.setId("tempCoordinates");

            const qslatlngLayer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: [qslatlngfeature]
                })
            });
            qslatlngLayer.set('name', id);
            qslatlngLayer.setZIndex(1000);
            map.addLayer(qslatlngLayer);

            qslatlngfeature.setStyle(qslatlngstyleIcon);
            qslatlngLayer.getSource().addFeature(qslatlngfeature);
        }
    }

    $("#resetMap").on("click", function(e) {
        e.preventDefault();

        // Remove query params
        window.history.replaceState(null, '', window.location.pathname);

        // Remove marker
        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties()["name"] === "tempQpCoordinates") {
                map.removeLayer(layer);
            }
        });

        filtered = true;

        startDateField.val("");
        endDateField.val("");
        videoOnlyCheckbox.prop("checked", false);
        showMarkersCheckbox.prop("checked", false);

        startDateField.val(initialStartDate);

        if (albumSelect.length > 0) {
            albumSelect.val("0");
        }

        setLayer(startDateField.val(),endDateField.val(),videoOnlyCheckbox.prop("checked"),[],true);
        shashin.showToastMessage("Map reset", "Map reset", {icon:"bi-info-circle", iconColor:"#777777", delay: 3000});

        $("#propMapFilter").modal('hide');
    });

    $("#mapFilterButton").on("click", function (e) {
        e.preventDefault();

        $("#propMapFilter").modal('show');
    });

    $("#propMapFilter").on('shown.bs.modal', function () {
        originalFromInput = $("#startDateInput").val();
        originalToInput = $("#endDateInput").val();
        originalAlbumFilter = $("#albumSelect").val();
        originalVideoOnly = $('#videoOnlyInput').is(":checked");
        originalMapMarkers = $('#showMarkersInput').is(":checked");
    });

    $("#propMapFilter").on('hidden.bs.modal', function () {
        if (filtered === false) {
            $("#startDateInput").val(originalFromInput);
            $("#endDateInput").val(originalToInput);
            if ($("#albumSelect").length > 0) {
                $("#albumSelect").val(originalAlbumFilter);
            }
            if (originalVideoOnly === true) {
                $("#videoOnlyInput").prop("checked", true);
            } else {
                $("#videoOnlyInput").prop("checked", false);
            }
            if (originalMapMarkers === true) {
                $("#showMarkersInput").prop("checked", true);
            } else {
                $("#showMarkersInput").prop("checked", false);
            }
        }
        filtered = false;
        originalFromInput = "";
        originalToInput = "";
        originalAlbumFilter = "";
        originalVideoOnly = "";
        originalMapMarkers = "";
    });

    map.on("pointermove", function (evt) {
        const hit = this.forEachFeatureAtPixel(evt.pixel, function () {
            return true;
        });
        if (hit) {
            this.getTargetElement().style.cursor = 'pointer';
        } else {
            this.getTargetElement().style.cursor = '';
        }
    });
}