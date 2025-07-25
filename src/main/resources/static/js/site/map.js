async function showMap(mapdata, keywordMap, locale) {
    // Query parameters
    // Lat
    let qslat = Util.getParameterByName("lat");
    // Lng
    let qslng = Util.getParameterByName("lng");
    // Lat, Lng
    const qslatlng = Util.getParameterByName("latlng");
    // Must be format yyyy-mm-dd
    // Start date
    let qssd = Util.getParameterByName("sd");
    // End date
    let qsed = Util.getParameterByName("ed");
    // Video only
    const qsvo = Util.getParameterByName("vo");
    // Media type filter - video, gif, etc
    const qsmtf = Util.getParameterByName("mtf");
    // Album ID
    const qsaid = Util.getParameterByName("aid");
    // Album name
    const qsan = Util.getParameterByName("an");
    // Map style
    const qsms = Util.getParameterByName("ms");

    let version = Util.getMetadataLocalStorage();

    const videoOnlyCheckbox = $("#videoOnlyInput");
    const showMarkersCheckbox = $("#showMarkersInput");
    const startDateField = $("#startDateInput");
    const endDateField = $("#endDateInput");
    const filterInputs = $("#filterInputs");
    const albumSelect = $("#albumSelect");
    const coordZoom = 17;

    // Bigger number for better performance, smaller number for better accuracy
    let clusterDistance = 100; // 50
    let filtered = false;
    let previousFromInput = "";
    let previousToInput = "";
    let previousAlbumFilter = "";
    let previousVideoOnly = "";
    let previousMapMarkers = "";
    let previousFindNearest = "";
    let prevMapTile = "osm";
    let forcedFiltered = false;
    let progressBarShown = false;

    const validMapStyles = ["osm", "maptiler", "stadiaSA"];
    let osmMapTile = shashin.getMapSource("osm");
    let mapTilerTile = shashin.getMapSource("maptiler");
    let stadiaSa = shashin.getMapSource("stadiaSA");

    const layerTile = new ol.layer.Tile({
        visible: true,
        source: osmMapTile
    });

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
    initialStartDate = mapdata[initIndex].year + '-' +
        ((mapdata[initIndex].month > 9) ? (mapdata[initIndex].month) : ('0' + (mapdata[initIndex].month))) + '-' +
        ((mapdata[initIndex].day > 9) ? mapdata[initIndex].day : ('0' + mapdata[initIndex].day));
    }
    startDateField.val(initialStartDate);

    const defaultCoord = [-123.14659455430593, 49.16889576756705];
    let initialCoord = defaultCoord;
    let initialZoom = 2;

    // Query params for albumId
    if (qsaid !== null && qsaid !== "") {
        let albumId = -1;
        $("#albumSelect option").each(function(i, option) {
            if (i !== 0 && $(option).val() === qsaid) {
                albumId = $(option).val();
                return true;
            }
        });

        if (albumId > 0) {
            // Clear dates
            startDateField.val("");
            endDateField.val("");
            albumSelect.val(albumId);
            renderAlbumSelected();
        } else {
            shashin.showToastMessage("Album does not exist", "Invalid album ID " + qsaid + ".", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
        }
    } else if (qsan !== null && qsan !== "") {
        let albumId = -1;
        $("#albumSelect option").each(function(i, option) {
            if (i !== 0 && $(option).text().toLowerCase() === qsan.toLowerCase()) {
                albumId = $(option).val();
                return true;
            }
        });

        if (albumId > 0) {
            // Clear dates
            startDateField.val("");
            endDateField.val("");
            albumSelect.val(albumId);
            renderAlbumSelected();
        } else {
            shashin.showToastMessage("Album does not exist", "Invalid album name " + qsan + ".", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
        }
    }

    if (qslat !== null && qslng !== null && qslat !== '' && qslng !== '') {
        if (true === Util.isValidLatLon(qslat,qslng)) {
            initialCoord = [qslng, qslat];
            initialZoom = coordZoom;
            startDateField.val("");
        } else {
            shashin.showToastMessage("Validation error", "Invalid lat/lng format.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
        }
    } else if (qslatlng !== null && qslatlng !== "") {
        const latlngArr = qslatlng.split(",");

        if (latlngArr.length > 1) {
            qslat = latlngArr[0].trim();
            qslng = latlngArr[1].trim();

            if (true === Util.isValidLatLon(qslat, qslng)) {
                initialCoord = [qslng, qslat];
                initialZoom = coordZoom;
                startDateField.val("");
            } else {
                shashin.showToastMessage("Validation error", "Invalid lat/lng format.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
            }
        } else {
            shashin.showToastMessage("Validation error", "Invalid lat/lng format.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
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

            initialCoord = [lslng, lslat];
            initialZoom = shashin.initialMapZoom;
            startDateField.val("");
            localStorage.removeItem('latlng');
        }
    }

    // Query param takes precedence over localstorage
    if ((qssd !== null && qssd !== "") || (qsed !== null && qsed !== "") || qsvo !== null) {
        if (qssd !== null && qssd !== "") {
            qssd = Util.formatDate(qssd);
            if (qssd !== null && true === Util.isValidDate(qssd)) {
                startDateField.val(qssd);
            } else {
                shashin.showToastMessage("Validation error", "Start date is invalid or format not yyyy-mm-dd.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
            }
        }
        if (qsed !== null && qsed !== "") {
            qsed = Util.formatDate(qsed);
            if (qsed !== null && true === Util.isValidDate(qsed)) {
                endDateField.val(qsed);
            } else {
                shashin.showToastMessage("Validation error", "End date is invalid or format not yyyy-mm-dd.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
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

    function checkDateInputs(startDateFormat,endDateFormat,takenAtDateFormat) {
        if (takenAtDateFormat === undefined) {
            startDateFormat = Util.formatDateTime(startDateFormat);
            endDateFormat = Util.formatDateTime(endDateFormat);

            if (startDateField.val() === "" && endDateField.val() === "") {
                return true;
            } else if (startDateField.val() !== "" && startDateFormat == null && endDateField.val() !== "" && endDateFormat === null) {
                shashin.showToastMessage("Validation error", "Invalid dates.", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger",
                    tag: "mainmap"
                });
                return false;
            } else if (startDateFormat && endDateFormat) {
                if (endDateFormat < startDateFormat) {
                    shashin.showToastMessage("Validation error", "Start date must be before end date.", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor:"danger",
                        tag: "mainmap"
                    });
                }
                return endDateFormat >= startDateFormat;
            } else if (startDateField.val() !== "" && startDateFormat === null) {
                shashin.showToastMessage("Validation error", "Invalid start date.", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger",
                    tag: "mainmap"
                });
                return false;
            } else if (endDateField.val() !== "" && endDateFormat === null) {
                shashin.showToastMessage("Validation error", "Invalid end date.", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor:"danger",
                    tag: "mainmap"
                });
                return false;
            }

            return true;
        } else {
            startDateFormat = Util.formatDateTime(startDateFormat);
            endDateFormat = Util.formatDateTime(endDateFormat);
            takenAtDateFormat = Util.formatDateTime(takenAtDateFormat);

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
    }

    function calcCrow(lat1, lon1, lat2, lon2) {
        const R = 6371; // Radius of the earth in km
        const dLat = toRad(lat2 - lat1);
        const dLon = toRad(lon2 - lon1);
        lat1 = toRad(lat1);
        lat2 = toRad(lat2);

        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // https://stackoverflow.com/questions/62532283/get-the-extent-of-a-one-center-point
    // OL extent is {'bottom-left'} {'bottom-right'} {'top-left'} {'top-right'}
    // or {'bottom-left'} {'top-right'} or [minLat, minLng, maxLat, maxLng] or [minX, minY, maxX, maxY]
    // where X -> Lat, Y -> Lng
    // In OL lat, lng is switched around ie lng, lat
    function getExtentFromCenterCoordinate(centerCoordLngLat, distanceKm) {
        const lat = centerCoordLngLat[1];
        const lng = centerCoordLngLat[0];
        const hypotenuseEachQuardilator = Math.sqrt(Math.pow((distanceKm / 2), 2) + Math.pow((distanceKm / 2), 2));
        const radAngle = (Math.PI / 180) * 45;
        const distance = hypotenuseEachQuardilator / 100;

        //top-left
        const x1 = lat - (distance * Math.cos(radAngle));
        const y1 = lng + (distance * Math.sin(radAngle));
        //top-right
        const x2 = lat + (distance * Math.cos(radAngle));
        const y2 = lng + (distance * Math.sin(radAngle));
        //bottom-right
        const x3 = lat + (distance * Math.cos(radAngle));
        const y3 = lng - (distance * Math.sin(radAngle));
        //bottom-left
        const x4 = lat - (distance * Math.cos(radAngle));
        const y4 = lng - (distance * Math.sin(radAngle));

        return [
            [y4, x4], //bottom-left
            [y1, x1], //bottom-right
            [y3, x3], //top-left
            [y2, x2]  //top-right
        ];
    }

    // Converts numeric degrees to radians
    function toRad(val) {
        return val * Math.PI / 180;
    }

    function isFloat(value) {
        value = value.trim();
        return !isNaN(value) &&
            parseFloat(Number(value)) == value &&
            !isNaN(parseFloat(value));
    }

    function setLayer(options) {
        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "tempCoordinatesFN") {
                map.removeLayer(layer);
            }
        });
        $("#resultsText").text("");
        version = Util.getMetadataLocalStorage();
        map.removeLayer(vectorLayer);
        const iconFeatures = [];

        const searchTerm = $("#searchInput").val();
        const formCoordinates = $("#formCoordinates").val();
        let startDateObj = null;
        let endDateObj = null;
        let videoOnly = false;
        let metadataList = [];
        let zoomOnly = false;
        let maxDistance = 0;
        let contextCoordArray = [];
        let inputsChanged = false;
        let resetMap = false;

        if (options.hasOwnProperty("startDate") === true) {
            const startDate = options.startDate;

            const dateArray = startDate.split("-");
            const year = dateArray[0];
            const month = parseInt(dateArray[1], 10) - 1;
            const day = dateArray[2];
            startDateObj = new Date(year, month, day);
        }

        if (options.hasOwnProperty("endDate") === true) {
            const endDate = options.endDate;

            const dateArray = endDate.split("-");
            const year = dateArray[0];
            const month = parseInt(dateArray[1], 10) - 1;
            const day = dateArray[2];
            endDateObj = new Date(year, month, day);
        }

        if (options.hasOwnProperty("videoOnly") === true) {
            videoOnly = options.videoOnly;
        }

        if (options.hasOwnProperty("metadataList") === true) {
            metadataList = options.metadataList;
        }

        if (options.hasOwnProperty("resetMap") === true) {
            resetMap = options.resetMap;
        }

        if (options.hasOwnProperty("inputsChanged") === true) {
            inputsChanged = options.inputsChanged;
        }

        if (options.hasOwnProperty("contextCoordArray") === true) {
            contextCoordArray = options.contextCoordArray;
        }

        if (options.hasOwnProperty("maxDistance") === true) {
            maxDistance = options.maxDistance;
            if (typeof maxDistance === 'string' || maxDistance instanceof String) {
                maxDistance = parseFloat(maxDistance);
            }
        }

        if (options.hasOwnProperty("zoomOnly") === true) {
            zoomOnly = options.zoomOnly;
        }

        if (resetMap === true) {
            initialZoom = 2;
            if (qslat !== null && qslng !== null && qslat !== "" && qslng !== "") {
                initialZoom = coordZoom;
            }
        }

        if (qsms !== null && qsms !== "" && qsms !== prevMapTile && validMapStyles.includes(qsms)) {
            $("#mapSources").val(qsms);
            layerTile.setSource(shashin.getMapSource(qsms));
        }

        let minLat = null;
        let maxLat = null;
        let minLng = null;
        let maxLng = null;

        $("#distanceInfo").text("");
        $("#distanceInfo").css("display", "none");

        let filteredCount = 0;

        for (let index in mapdata) {
            const data = mapdata[index];

            if (progressBarShown === false) {
                const currentProgress = parseInt((((parseInt(index) + 1) / mapdata.length) * 100).toString(), 10);
                Util.updateProgressBar(currentProgress);
                shashin.printMessageToConsole("currentProgress for map: " + currentProgress.toString(),{tag:"map"});
            }

            if ((videoOnly === true && data.type.includes("video") === false) || (qsmtf !== null && qsmtf !== "" && data.type.includes(qsmtf) === false)) {
                continue;
            }

            if (metadataList !== undefined && metadataList.length > 0 && $.inArray(data.id, metadataList) === -1) {
                continue;
            }

            const placeName = data.placeName !== null ? data.placeName : "";
            const mapMarkerUrl = data.mapMarkerUrl !== null ? "/api/v1/thumbnails/map/"+data.id : "";
            const type = data.type !== null ? data.type : "";
            const keywords = keywordMap.hasOwnProperty(data.id) && keywordMap[data.id].length > 0 ? keywordMap[data.id] : "";

            if (searchTerm !== null && searchTerm !== "" &&
                placeName.toLowerCase().indexOf(searchTerm.toLowerCase()) === -1 &&
                mapMarkerUrl.toLowerCase().indexOf(searchTerm.toLowerCase()) === -1 &&
                type.toLowerCase().indexOf(searchTerm.toLowerCase()) === -1 &&
                keywords.toLowerCase().indexOf(searchTerm.toLowerCase()) === -1
            ) {
                continue;
            }

            if (data.lat !== null && data.lng !== null &&
                data.lat !== "" && data.lng !== "") {

                const lat = data.lat;
                const lng = data.lng;

                let dateTakenObj = new Date(data.year,parseInt(data.month)-1,data.day);

                if (true === checkDateInputs(startDateObj,endDateObj,dateTakenObj)) {
                    // Check distance
                    if (contextCoordArray.length > 0 && maxDistance > 0) {
                        if (zoomOnly === false) {
                            const kmDistance = calcCrow(contextCoordArray[1], contextCoordArray[0], lat, lng);
                            shashin.printMessageToConsole("center: " + contextCoordArray[1] + ", " + contextCoordArray[0],{tag:"map"});
                            shashin.printMessageToConsole("current coord: " + lat + ", " + lng,{tag:"map"});
                            shashin.printMessageToConsole("Distance: " + kmDistance,{tag:"map"});

                            if (kmDistance > maxDistance) {
                                continue;
                            }

                            $("#distanceInfo").text("Filtered results within a " + maxDistance + " km distance from " + contextCoordArray[1] + ", " + contextCoordArray[0]);
                            $("#distanceInfo").css("display", "block");
                        }
                    }

                    filteredCount++;

                    const mapMarkerIcon = new ol.style.Style({
                        //geometry: feature.getGeometry(),
                        image: new ol.style.Icon(({
                            anchor: [0.5, 46],
                            anchorXUnits: 'fraction',
                            anchorYUnits: 'pixels',
                            opacity: 1.0,
                            src: "/api/v1/thumbnails/map/"+data.id + (version === "" ? "" : "?v=" + version)
                        }))
                    });

                    const iconFeature = new ol.Feature({
                        geometry: new ol.geom.Point(ol.proj.transform([data.lng, data.lat], 'EPSG:4326', 'EPSG:900913')),
                        thumbnailUrlSmall: "/api/v1/thumbnails/225/"+data.id,
                        thumbnailUrlOriginal: "/api/v1/thumbnails/original/"+data.id,
                        mapMarkerUrl: "/api/v1/thumbnails/map/"+data.id,
                        mapMarkerIcon: mapMarkerIcon,
                        videoUrl: data.videoUrl,
                        originalImageWidth: data.originalImageWidth,
                        originalImageHeight: data.originalImageHeight,
                        metadataId: data.id,
                        lat: lat,
                        lng: lng,
                        type: data.type
                    });

                    iconFeature.setStyle(data.mapMarkerIcon);
                    iconFeatures.push(iconFeature);

                    if (zoomOnly === false) {
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

            if (mapdata.length === parseInt(index)+1) {
                Util.updateProgressBar(0);
                progressBarShown = true;
            }
        }

        let resultsText = filteredCount + " result" + (filteredCount === 1 ? "" : "s");
        $("#resultsText").text();
        if (locale === "ja") {
            resultsText = filteredCount + " " + shashin.getTranslatedValue("main.pages.map.modal.result");
        }
        $("#resultsText").text(resultsText);

        if (filteredCount === 0 && contextCoordArray.length > 0 && maxDistance > 0) {
            shashin.showToastMessage("No results", "No results for photos near " + contextCoordArray[1]+", "+contextCoordArray[0], {
                icon: "bi-info-circle",
                iconColor: "#777777",
                tag: "mainmap"
            });
            forcedFiltered = true;
            $("#filterMap").click();
        }

        $("#mapFilterButton").removeClass("disabled");

        let formLatLng = false;
        if (zoomOnly === true || formCoordinates !== "") {
            // Zoom to the coordinates at a distance of maxDistance
            let centerLat = contextCoordArray[1];
            let centerLng = contextCoordArray[0];
            if (zoomOnly === false && formCoordinates !== "") {
                const formCoordinatesArr = formCoordinates.split(",");
                if (formCoordinatesArr.length === 2 && isFloat(formCoordinatesArr[0]) && isFloat(formCoordinatesArr[1])) {
                    centerLat = parseFloat(formCoordinatesArr[0].trim());
                    centerLng = parseFloat(formCoordinatesArr[1].trim());
                    maxDistance = parseFloat($("#findNearestRadius").val().trim());
                    inputsChanged = true;
                    formLatLng = true;
                }
            }

            let pointExtent = getExtentFromCenterCoordinate([centerLng, centerLat], maxDistance);

            minLat = pointExtent[0][1];
            maxLat = pointExtent[3][1];
            minLng = pointExtent[0][0];
            maxLng = pointExtent[3][0];
        }

        if (iconFeatures.length > 0) {

            const vectorSource = new ol.source.Vector({
                features: iconFeatures //add an array of features
            });

            if (showMarkersCheckbox.prop("checked") === true) {
                clusterDistance = 250; //200
            }

            const clusterSource = new ol.source.Cluster({
                distance: clusterDistance,
                source: vectorSource,
            });

            vectorLayer = new ol.layer.Vector({
                source: clusterSource,
                style: styleFunction
            });

            map.addLayer(vectorLayer);

            shashin.printMessageToConsole("minLat for map filtering: "+minLat,{tag:"map"});
            shashin.printMessageToConsole("minLng for map filtering: "+minLng,{tag:"map"});
            shashin.printMessageToConsole("maxLat for map filtering: "+maxLat,{tag:"map"});
            shashin.printMessageToConsole("maxLng for map filtering: "+maxLng,{tag:"map"});

            if (forcedFiltered === true) {
                initialCoord = defaultCoord;
                initialZoom = 2;
                map.getView().setCenter(ol.proj.fromLonLat(initialCoord));
                map.getView().setZoom(initialZoom);
            } else if (inputsChanged === true && resetMap === false && minLat !== null && minLng !== null && maxLat !== null && maxLng !== null) {
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
                if (maxDistance > 0) {
                    if (contextCoordArray.length > 0) {
                        renderMarker('tempCoordinatesFN', contextCoordArray[1], contextCoordArray[0], "red");
                    } else if (formLatLng === true) {
                        const formCoordinatesArr = formCoordinates.split(",");
                        renderMarker('tempCoordinatesFN', parseFloat(formCoordinatesArr[0].trim()), parseFloat(formCoordinatesArr[1].trim()), "red");
                    }
                }
            } else {
                initialCoord = defaultCoord;
                if (qslat !== null && qslng !== null && qslat !== '' && qslng !== '') {
                    initialCoord = [qslng, qslat];
                }
                map.getView().setCenter(ol.proj.fromLonLat(initialCoord));
                map.getView().setZoom(initialZoom);
            }

            forcedFiltered = false;
        }

        shashin.closeToastMessages({
            tag: "mainmap"
        });
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

            // shashin.printMessageToConsole("---------------------------");
            // shashin.printMessageToConsole("originalRadius:" + (0.45 * (ol.extent.getWidth(extent) + ol.extent.getHeight(extent))) / resolution);
            // shashin.printMessageToConsole("extentWidth:" + ol.extent.getWidth(extent));
            // shashin.printMessageToConsole("extentHeight:" + ol.extent.getHeight(extent));
            // shashin.printMessageToConsole("resolution:" + resolution);
            // shashin.printMessageToConsole("Feature count:" + jj);
            // shashin.printMessageToConsole("maxFeatureCount:" + maxFeatureCount);
            // shashin.printMessageToConsole("radius: " + radius);

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

        const markerColor = $("#markerColors").val();
        const rgbMarker = hexToRgb(markerColor);
        let style = {
            image: new ol.style.Circle({
                radius: feature.get('radius'),
                fill: new ol.style.Fill({
                    color: [rgbMarker.r, rgbMarker.g, rgbMarker.b, Math.min(0.8, 0.4 + size / maxFeatureCount)],
                }),
            })
        };
        if (size > 2) {
            style.text = new ol.style.Text({
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
            styles.push(originalFeature.getProperties().mapMarkerIcon);

            // Show gallery for each cluster
            const mediaContentList = [];
            for (let i = originalFeatures.length - 1; i >= 0; --i) {
                const featureProperties = originalFeatures[i].getProperties();

                const mediaContent = {
                    metadataDetailFun: editLocation,
                    args: [
                        featureProperties.metadataId
                    ]
                };

                if (featureProperties.type.includes("image")) {
                    mediaContent.src = "/api/v1/image/"+featureProperties.metadataId;
                    mediaContent.downloadUrl = "/api/v1/image/"+featureProperties.metadataId + "/download";
                } else if (featureProperties.type.includes("video")) {
                    mediaContent.video = {
                        "source": [{"src": featureProperties.videoUrl, "type": "video/mp4"}],
                        "attributes": {
                            "preload": "auto",
                            "controls": true,
                            "autoplay": shashin.autoplayVideo,
                            "id": featureProperties.metadataId
                        }
                    };

                    mediaContent.poster = ((featureProperties.thumbnailUrlOriginal === null || featureProperties.thumbnailUrlOriginal === "") ? "/api/v1/thumbnails/225/"+featureProperties.metadataId : "/api/v1/thumbnails/original/"+featureProperties.metadataId) + "?v=" + Util.getMetadataLocalStorage();
                    mediaContent.lgSize = featureProperties.originalImageWidth+"-"+featureProperties.originalImageHeight;
                    mediaContent.downloadUrl = encodeURI(featureProperties.videoUrl).replace(";", "%3B") + "/download";
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
                    shashin.printMessageToConsole("Error removing lightGallery instance: "+e.message, {
                        consoleType: shashin.consoleTypes.error
                    });
                }
                lightGalleryConfigs.dynamicEl = mediaContentList;
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
    lightGalleryConfigs.plugins.push(lgMetadataDetail);
    lightGalleryConfigs.plugins.push(lgCastMedia);
    lightGalleryConfigs.controls = true;
    lightGalleryConfigs.dynamic = true;
    lightGalleryConfigs.counter = true;
    lightGalleryConfigs.metadataDetail = true;
    // lightGalleryConfigs.editLocation = true;
    // lightGalleryConfigs.showControls = showControls;
    lightGalleryConfigs.castMedia = true;

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
            layerTile
        ],
        view: mapView,
        controls: []
    });

    const attributions = new ol.control.Attribution({collapsible: true});

    map.addControl(attributions);

    const copyPlacename = function (obj) {
        if (obj.hasOwnProperty("data") && obj.data !== null && obj.data !== "" && obj.data.placename !== null && obj.data.placename !== "") {
            const copyText = obj.data.placename;
            Util.copyToClipboard(copyText);
        }
    };

    const copyCoordinates = function (obj) {
        const coordArray = ol.proj.toLonLat(obj.coordinate);
        if (coordArray.length > 1) {
            const copyText = coordArray[1]+","+coordArray[0];
            Util.copyToClipboard(copyText);
        }
    };

    const findMediaNear = function (obj) {
        const coordArray = ol.proj.toLonLat(obj.coordinate);
        const radius = $("#findNearestRadius").val();

        if (coordArray.length > 1 && radius > 0) {
            setLayer({
                startDate: startDateField.val(),
                endDate: endDateField.val(),
                videoOnly: videoOnlyCheckbox.prop("checked"),
                inputsChanged: true,
                contextCoordArray: coordArray,
                maxDistance: radius
            });
        }
    };

    const zoomIn = function (obj) {
        const coordArray = ol.proj.toLonLat(obj.coordinate);
        const radius = $("#findNearestRadius").val();

        if (coordArray.length > 1 && radius > 0) {
            setLayer({
                startDate: startDateField.val(),
                endDate: endDateField.val(),
                videoOnly: videoOnlyCheckbox.prop("checked"),
                inputsChanged: true,
                contextCoordArray: coordArray,
                maxDistance: radius,
                zoomOnly: true
            });
        }
    };

    const googleMapsLink = function (obj) {
        const coordArray = ol.proj.toLonLat(obj.coordinate);

        if (coordArray.length > 1) {
            const win = window.open('https://www.google.com/maps/search/?api=1&query='+coordArray[1]+"%2C"+coordArray[0], '_blank');
            if (win) {
                //Browser has allowed it to be opened
                win.focus();
                renderMarker('tempGoogleCoordinates',coordArray[1],coordArray[0],"red");
            }
        }
    };

    const contextmenu = new ContextMenu({
        width: 300,
        defaultItems: false // defaultItems are (for now) Zoom In/Zoom Out
    });
    contextmenu.on('close', function (evt) {
        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "tempCoordinates") {
                map.removeLayer(layer);
            }
        });
    });

    function showContextMenu(evt, coordArray, data) {
        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") &&
                (layer.getProperties().name === "tempQpCoordinates" ||
                layer.getProperties().name === "tempCoordinatesFN" ||
                layer.getProperties().name === "tempGoogleCoordinates"
            )) {
                map.removeLayer(layer);
            }
        });

        renderMarker('tempCoordinates', coordArray[1], coordArray[0], "grey");

        const copyText = coordArray[1] + "," + coordArray[0];
        contextmenu.updatePosition([evt.pixel[0], evt.pixel[1] + 12]);
        const contextValueArray = [];

        let contextItem = {};
        if (data.hasOwnProperty("placename") && data.placename.length > 0) {
            contextItem = {
                text: "<strong>" + data.placename + "</strong>",
                // classname: "ol-ctx-menu-separator" // Make unselectable text
                classname: "context-text-wrap",
                callback: copyPlacename
            };
            contextItem.data = {placename: data.placename};

            contextValueArray.push(contextItem);
            contextValueArray.push("-");
        }

        contextValueArray.push({
            text: copyText,
            callback: copyCoordinates
        });

        contextValueArray.push({
            text: "Find photos within " + $("#findNearestRadius").val() + " km",
            callback: findMediaNear
        });

        contextValueArray.push({
            text: "Zoom within " + $("#findNearestRadius").val() + " km",
            callback: zoomIn
        });

        contextValueArray.push({
            text: '<span class="bi-google"></span>',
            callback: googleMapsLink
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

    // Remove markers
    map.on('click', function () {
        map.getLayers().forEach(layer => {
            if (layer && layer.getProperties().hasOwnProperty("name") &&
                (
                    layer.getProperties().name === "tempQpCoordinates" ||
                    layer.getProperties().name === "tempCoordinatesFN" ||
                    layer.getProperties().name === "tempGoogleCoordinates"
                )
            ) {
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

    checkDateInputs(new Date(startDateField.val()),new Date(endDateField.val()));
    setLayer({
        startDate: startDateField.val(),
        endDate: endDateField.val(),
        videoOnly: videoOnlyCheckbox.prop("checked")
    });

    function filterClicked() {
        let mapSourceChanged = false;

        if ((qsms !== null && qsms !== "" && qsms !== prevMapTile) || $("#mapSources").val() !== prevMapTile) {
            mapSourceChanged = true;
            if (qsms !== null && qsms !== "" && qsms !== prevMapTile) {
                prevMapTile = qsms;
            } else {
                prevMapTile = $("#mapSources").val();
            }

            if (!validMapStyles.includes(prevMapTile)) {
                prevMapTile = "osm";
            }

            layerTile.setSource(shashin.getMapSource(prevMapTile));
        }

        filtered = true;
        shashin.showToastMessage("Applying filter", "Applying filter", {icon:"bi-info-circle", iconColor:"#777777", autohide: false, tag: "mainmap"});
        if (true === setLayerInputs(mapSourceChanged)) {
            if (MutationObserver) {
                const observer = new MutationObserver(function (mutations, me) {
                    const resultsTextEl = document.getElementById("resultsText");

                    if (resultsTextEl) {
                        let resultsString = "";
                        if ($("#resultsText").text().trim().length > 0) {
                            resultsString = " " + $("#resultsText").text().trim();
                        }
                        shashin.showToastMessage("Filter applied", "Filter applied." + resultsString, {
                            icon: "bi-info-circle",
                            iconColor: "#777777",
                            delay: 3000,
                            tag: "mainmap"
                        });

                        me.disconnect(); // stop observing
                        return true;
                    }
                });

                observer.observe(document, {
                    childList: true,
                    subtree: true
                });
            } else {
                setTimeout(function () {
                    let resultsString = "";
                    if ($("#resultsText").text().trim().length > 0) {
                        resultsString = " " + $("#resultsText").text();
                    }
                    shashin.showToastMessage("Filter applied", "Filter applied." + resultsString, {
                        icon: "bi-info-circle",
                        iconColor: "#777777",
                        delay: 3000,
                        tag: "mainmap"
                    });
                }, 1000);
            }

            $("#propMapFilter").modal('hide');
        }
    }

    $("#filterMap").on("click", function(e) {
        e.preventDefault();
        filterClicked();
    });

    $('#propMapFilter').on('keypress', function (e) {
        if (e.key === "Enter" || e.code === "Enter" || e.which === 13 || e.keyCode === 13) {
            filterClicked();
        }
    });

    albumSelect.on("change", function(e) {
        if ($(this).val() !== "0") {
            startDateField.val("");
            endDateField.val("");
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
            });
        });

        // register the focusout event to reset the input back to a date input field
        singleDateInput.on('focusout', () => {
            validateAndChangeDateType(singleDateInput,el);
        });

        singleDateInput.on('keypress',function(e) {
            // Enter key
            if (e.key === "Enter" || e.code === "Enter" || e.which === 13 || e.keyCode === 13) {
                validateAndChangeDateType(singleDateInput,el);
            }
        });
    });

    function validateAndChangeDateType(singleDateInput, el) {
        singleDateInput.val(Util.formatDate(singleDateInput.val()));
        if (false === Util.isValidDate(singleDateInput.val())) {
            shashin.showToastMessage("Validation error", singleDateInput.val() + " is invalid or format not yyyy-mm-dd.", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "mainmap"});
        }
        el.type = "date";
        el.placeholder = "mm/dd/yyyy";
    }

    function setLayerInputs(mapSourceChanged) {
        if (mapSourceChanged === undefined) {
            mapSourceChanged = false;
        }

        const dateInputsValid = checkDateInputs(new Date(startDateField.val()),new Date(endDateField.val()));

        // Validate fields
        if (true === dateInputsValid) {
            let inputsChanged = false;
            if (initialStartDate !== $("#startDateInput").val() ||
                "" !== $("#endDateInput").val() ||
                '0' !== $("#albumSelect").val() ||
                false !== $('#videoOnlyInput').is(":checked") ||
                false !== $('#showMarkersInput').is(":checked"))
            {
                inputsChanged = true;
            }

            if (inputsChanged === false && (mapSourceChanged === true || previousFindNearest !== $("#findNearestRadius").val() && $("#formCoordinates").val() !== "")) {
                initialZoom = map.getView().getZoom();
            } else {
                initialZoom = 2;
            }

            if (false === renderAlbumSelected()) {
                // Filter results
                setLayer({
                    startDate: startDateField.val(),
                    endDate: endDateField.val(),
                    videoOnly: videoOnlyCheckbox.prop("checked"),
                    inputsChanged: inputsChanged
                });
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
                    setLayer({
                        startDate: startDateField.val(),
                        endDate: endDateField.val(),
                        videoOnly: videoOnlyCheckbox.prop("checked"),
                        metadataList: data.albummapdata,
                        inputsChanged: true
                    });
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
            if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "tempQpCoordinates") {
                map.removeLayer(layer);
            }
        });

        $("#markerColors").val("#004DFF");

        $("#bingMapsImageryContainer").css("display", "none");
        $("#maptilerContainer").css("display", "none");

        $("#distanceInfo").text("");
        $("#distanceInfo").css("display", "none");

        $("#findNearestRadius").val("5");

        prevMapTile = "osm";
        $("#mapSources").val(prevMapTile);
        layerTile.setSource(osmMapTile);

        filtered = true;

        $("#resultsText").text("");
        $("#searchInput").val("");
        $("#formCoordinates").val("");
        startDateField.val("");
        endDateField.val("");
        videoOnlyCheckbox.prop("checked", false);
        showMarkersCheckbox.prop("checked", false);

        startDateField.val(initialStartDate);

        if (albumSelect.length > 0) {
            albumSelect.val("0");
        }

        setLayer({
            startDate: startDateField.val(),
            endDate: endDateField.val(),
            videoOnly: videoOnlyCheckbox.prop("checked"),
            resetMap: true
        });

        shashin.showToastMessage("Map reset", "Map reset", {icon:"bi-info-circle", iconColor:"#777777", delay: 3000, tag: "mainmap"});

        $("#propMapFilter").modal('hide');

        window.location = window.location;

        // Hard reload of page
        // const url = window.location.href;
        // fetch(url, {
        //     headers: {
        //         Pragma: 'no-cache',
        //         Expires: '-1',
        //         'Cache-Control': 'no-cache',
        //     },
        // });
        // window.location.href = url;
        // window.location.reload();
    });

    $("#mapFilterButton").on("click", function (e) {
        e.preventDefault();

        contextmenu.closeMenu();
        $("#propMapFilter").modal('show');
    });

    $("#propMapFilter").on('shown.bs.modal', function () {
        previousFromInput = $("#startDateInput").val();
        previousToInput = $("#endDateInput").val();
        previousAlbumFilter = $("#albumSelect").val();
        previousVideoOnly = $('#videoOnlyInput').is(":checked");
        previousMapMarkers = $('#showMarkersInput').is(":checked");
        previousFindNearest = $('#findNearestRadius').val();
    });

    $("#propMapFilter").on('hidden.bs.modal', function () {
        if (filtered === false) {
            $("#startDateInput").val(previousFromInput);
            $("#endDateInput").val(previousToInput);
            $("#findNearestRadius").val(previousFindNearest);
            if ($("#albumSelect").length > 0) {
                $("#albumSelect").val(previousAlbumFilter);
            }
            if (previousVideoOnly === true) {
                $("#videoOnlyInput").prop("checked", true);
            } else {
                $("#videoOnlyInput").prop("checked", false);
            }
            if (previousMapMarkers === true) {
                $("#showMarkersInput").prop("checked", true);
            } else {
                $("#showMarkersInput").prop("checked", false);
            }
        }
        filtered = false;
        previousFromInput = "";
        previousToInput = "";
        previousAlbumFilter = "";
        previousVideoOnly = "";
        previousMapMarkers = "";
        previousFindNearest = "";
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

    $("#mapSources").on("change", function () {
       if ($(this).val() === "bingmaps") {
           $("#bingMapsImagerySet").val("AerialWithLabels");
           $("#bingMapsImageryContainer").css("display", "block");
           $("#maptilerContainer").css("display", "none");
       } else if ($(this).val() === "maptiler") {
           $("#maptilerImagerySet").val("maptiler");
           $("#maptilerContainer").css("display", "block");
           $("#bingMapsImageryContainer").css("display", "none");
       } else {
           $("#bingMapsImageryContainer").css("display", "none");
           $("#maptilerContainer").css("display", "none");
       }
    });

    $("#propMetadata").on('shown.bs.modal', _ => {
        $("#title").focus();
    });

    // Focus control on modal instead of gallery
    $("#propMetadata").on('keydown', function(e) {
        // left arrow
        if (e.key === "ArrowLeft" || e.code === "ArrowLeft" || e.which === 37 || e.keyCode === 37) {
            e.stopPropagation();
            e.currentTarget.setSelectionRange(
                e.currentTarget.selectionStart,
                e.currentTarget.selectionStart - 1,
            );
            e.currentTarget.focus();
            return false;
        }

        // right arrow
        if (e.key === "ArrowRight" || e.code === "ArrowRight" || e.which === 39 || e.keyCode === 39) {
            e.stopPropagation();
            e.currentTarget.setSelectionRange(
                e.currentTarget.selectionStart,
                e.currentTarget.selectionStart + 1,
            );
            e.currentTarget.focus();
            return false;
        }

        // escape
        if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
            e.stopPropagation();
            const bsModalEl = document.getElementById('propMetadata');
            const bsModal = bootstrap.Modal.getInstance(bsModalEl);
            bsModal.hide();
            return false;
        }
    });

    function hexToRgb(hex) {
        if (hex.charAt(0) === "#") {
            hex = hex.slice(1);
        }

        const bigint = parseInt(hex, 16);
        const r = (bigint >> 16) & 255;
        const g = (bigint >> 8) & 255;
        const b = bigint & 255;

        return {
            r: r,
            g: g,
            b: b
        };
    }
}