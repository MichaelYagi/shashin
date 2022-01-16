async function showMap(mapdata,authority) {
    const qslat = Util.getParameterByName("lat");
    const qslng = Util.getParameterByName("lng");

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

    function editLocation(...args) {
        const locationArgs = [].concat(...args);
        let metadataId = "";
        let lat = "";
        let lng = "";
        let modalLabel = "";

        const metadata = {}

        if (arguments.length > 0) {
            metadataId = locationArgs[0];
            metadata.lat = locationArgs[1];
            metadata.lng = locationArgs[2];
            modalLabel = locationArgs[3];
            metadata.path = locationArgs[4];
            metadata.compressionType = locationArgs[5];
            metadata.keywords = locationArgs[6];
            metadata.type = locationArgs[7];
            metadata.iso = locationArgs[8];
            metadata.exposure = locationArgs[9];
            metadata.fstopNumber = locationArgs[10];
            metadata.focalLength = locationArgs[11];
            metadata.camera = locationArgs[12];
            metadata.lens = locationArgs[13];
            metadata.quality = locationArgs[14];
            metadata.createdAt = locationArgs[15];
            metadata.modifiedAt = locationArgs[16];
            metadata.takenAt = locationArgs[17];
            metadata.year = locationArgs[18];
            metadata.month = locationArgs[19];
            metadata.day = locationArgs[20];
            metadata.time = locationArgs[21];
            metadata.timeZone = locationArgs[22];
            metadata.placeName = locationArgs[23];
        }
        if (modalLabel && modalLabel.length > 0) {
            $("#editPhotoLocationModalLabel").text("for " + modalLabel);
        }
        $("#mapMetadataId").val(metadataId);
        $("#metadataId").val(metadataId);
        if (metadata.lat && metadata.lng) {
            $("#locationDataInput").val(metadata.lat + "," + metadata.lng);
        }
        $("#propMetadataLocation").css('z-index', 9999);

        Util.populateDetailsInfo(metadata);

        $("#propMetadataLocation").modal('show');
    }

    let maxFeatureCount;
    let vector = null;
    const calculateClusterInfo = function (resolution) {
        maxFeatureCount = 0;
        const features = vector.getSource().getFeatures();
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
        if (resolution !== currentResolution) {
            calculateClusterInfo(resolution);
            currentResolution = resolution;
        }

        const features = feature.get('features');
        const size = features.length;

        if (size > 1) {
            return new ol.style.Style({
                image: new ol.style.Circle({
                    radius: feature.get('radius'),
                    fill: new ol.style.Fill({
                        color: [0, 77, 255, Math.min(0.8, 0.4 + size / maxFeatureCount)],
                    }),
                }),
                text: new ol.style.Text({
                    text: size.toString(),
                    fill: textFill,
                    stroke: textStroke,
                    scale: 1.5
                }),
            });
        } else {
            const originalFeature = features[0];
            return originalFeature.get('mapMarkerIcon');
        }
    }

    function selectStyleFunction(feature) {
        // Return first map marker
        const styles = [
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
                func: editLocation,
                args: [
                    featureProperties["metadataId"],
                    featureProperties["lat"],
                    featureProperties["lng"],
                    featureProperties["title"],
                    featureProperties["path"],
                    featureProperties["compressionType"],
                    featureProperties["keywords"],
                    featureProperties["type"],
                    featureProperties["iso"],
                    featureProperties["exposure"],
                    featureProperties["fstopNumber"],
                    featureProperties["focalLength"],
                    featureProperties["camera"],
                    featureProperties["lens"],
                    featureProperties["quality"],
                    featureProperties["createdAt"],
                    featureProperties["modifiedAt"],
                    featureProperties["takenAt"],
                    featureProperties["year"],
                    featureProperties["month"],
                    featureProperties["day"],
                    featureProperties["time"],
                    featureProperties["timeZone"],
                    featureProperties["placeName"]
                ]
            };
            if (featureProperties.type.includes("image")) {
                mediaContent.src = featureProperties.thumbnailUrlOriginal
                //mediaContent.subHtml = (featureProperties.placeName !== null ? featureProperties.placeName : "") + '<br>' + featureProperties.fileName + (dateString !== "" ? ' taken on ' + dateString : '')
                mediaContent.downloadUrl = encodeURI(featureProperties.thumbnailUrlOriginal);
            } else if (featureProperties.type.includes("video")) {
                mediaContent.video = {
                    "source": [{"src": featureProperties.videoUrl, "type": "video/mp4"}],
                    "attributes": {"preload": false, "controls": true}
                }
                mediaContent.downloadUrl = encodeURI(featureProperties.videoUrl)+"/download";
            }
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
                shashin.printMessageToConsole(e);
            }
            lightGalleryConfigs["dynamicEl"] = mediaContentList;
            dynamicGallery = lightGallery($dynamicGallery, lightGalleryConfigs);
            dynamicGallery.openGallery(0);
        }, 500);

        return styles;
    }

    let initialCoord = [-73.1234, 45.678];
    let initialZoom = 2;
    if (qslat !== null && qslng !== null && qslat !== '' && qslng !== '') {
        initialCoord = [qslng, qslat];
        initialZoom = 20;
    } else if ("lat" in localStorage && "lng" in localStorage) {
        initialCoord = [localStorage.getItem("lng"), localStorage.getItem("lat")];
        initialZoom = 20;
        localStorage.removeItem('lat');
        localStorage.removeItem('lng');
    }

    const mapView = new ol.View({
        center: ol.proj.fromLonLat(initialCoord),
        zoom: initialZoom
    });

    const lightGalleryConfigs = shashin.getLightGalleryConfigs();
    if (authority === "ROLE_ADMIN") {
        lightGalleryConfigs["plugins"].push(lgEditLocation);
    }
    lightGalleryConfigs["controls"] = true;
    lightGalleryConfigs["dynamic"] = true;
    lightGalleryConfigs["counter"] = true;
    lightGalleryConfigs["editLocation"] = true;
    const $dynamicGallery = document.getElementById('light-gallery-photo');
    let dynamicGallery = lightGallery($dynamicGallery, lightGalleryConfigs);

    const duration = 400;
    const interactions = [
        new ol.interaction.Select({
            condition: function (evt) {
                return evt.type === 'singleclick';
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
        controls: new ol.control.defaults({attributionOptions: {collapsible: true}})
    });
    const zoomSlider = new ol.control.ZoomSlider();
    map.addControl(zoomSlider);

    // After closing lightgallery, clear select interaction
    $dynamicGallery.addEventListener('lgAfterClose', function (event) {
        map.getInteractions().forEach(function (interaction) {
            if (interaction instanceof ol.interaction.Select) {
                interaction.getFeatures().clear();
            }
        });
    });

    const iconFeatures = [];
    for (let index in mapdata) {
        const data = mapdata[index];

        if (data["lat"] !== null && data["lng"] !== null &&
            data["lat"] !== "" && data["lng"] !== "") {

            const mapMarkerIcon = new ol.style.Style({
                //geometry: feature.getGeometry(),
                image: new ol.style.Icon(({
                    anchor: [0.5, 46],
                    anchorXUnits: 'fraction',
                    anchorYUnits: 'pixels',
                    opacity: 1.0,
                    src: encodeURI(data["mapMarkerUrl"])
                }))
            });

            const iconFeature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.transform([data["lng"], data["lat"]], 'EPSG:4326', 'EPSG:900913')),
                fileName: data["fileName"],
                compressionType: data["compressionType"],
                thumbnailUrlSmall: data["thumbnailUrlSmall"],
                thumbnailUrlOriginal: data["thumbnailUrlOriginal"],
                mapMarkerUrl: data["mapMarkerUrl"],
                mapMarkerIcon: mapMarkerIcon,
                videoUrl: data["videoUrl"],
                year: data["year"],
                month: data["month"],
                day: data["day"],
                placeName: data["placeName"],
                metadataId: data["id"],
                title: data["title"],
                lat: data["lat"],
                lng: data["lng"],
                type: data["type"],
                path: data["path"],
                keywords: data["keywords"],
                iso: data["iso"],
                exposure: data["exposure"],
                fstopNumber: data["fstopNumber"],
                camera: data["camera"],
                lens: data["lens"],
                quality: data["quality"],
                createdAt: data["createdAt"],
                modifiedAt: data["modifiedAt"],
                takenAt: data["takenAt"],
                time: data["time"],
                timeZone: data["timeZone"]
            });

            iconFeature.setStyle(data["mapMarkerIcon"]);
            iconFeatures.push(iconFeature);
        }

        const currentProgress = (index + 1) / mapdata.length * 100;
        $("#progressBar").attr("aria-valuenow", String(parseInt(currentProgress)));
        const width = String(parseInt(currentProgress)) + "%";
        $("#progressBar").css("width", width);
        shashin.printMessageToConsole(currentProgress);
    }
    $("#progressBarWrapper").css("visibility", "hidden");

    if (iconFeatures.length > 0) {
        const vectorSource = new ol.source.Vector({
            features: iconFeatures //add an array of features
        });
        const clusterSource = new ol.source.Cluster({
            distance: 40,
            source: vectorSource,
        });
        vector = new ol.layer.Vector({
            source: clusterSource,
            style: styleFunction
        });
        map.addLayer(vector);
    }

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

    $('#propMetadataLocation').on('hide.bs.modal', function () {
        $("#locationMapResponseMsg").html("");
        $("#saveMetadata").prop('disabled', false);
        $("#metadataLocationModalStatus").attr("class","spinner-grow me-auto");
        $("#metadataLocationModalStatus").css("visibility","hidden");
        $(this).find(':input').val('');

        const tab = new bootstrap.Tab($("#locationTabLink"));
        tab.show();
    });

    $("#detailsTabLink").click(function (e) {
        e.preventDefault();
        $("#locationMapResponseMsg").html("");
        $("#saveMetadata").prop('disabled', true);
    });

    $("#locationTabLink").click(function (e) {
        e.preventDefault();
        $("#saveMetadata").prop('disabled', false);
    });

    $("#saveMetadata").click(function (e) {
        e.preventDefault();

        $("#metadataLocationModalStatus").css("visibility", "visible");
        let metadataIdList = [];
        metadataIdList.push($("#mapMetadataId").val());

        const data = {
            "batchMetadataIds": metadataIdList,
            "latlngBatchData": $("#locationDataInput").val()
        };

        const ajaxParams = {
            type: "post",
            url: "/timeline/update/batch",
            data: JSON.stringify(data),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " saving map location data")}).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    $("#metadataLocationModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');

                    const latlng = $("#locationDataInput").val();
                    const latlngArray = latlng.split(",");
                    const lat = latlngArray[0].trim();
                    const lng = latlngArray[1].trim();
                    localStorage.setItem("lat", lat);
                    localStorage.setItem("lng", lng);

                    window.top.location = window.location.href.split("?")[0];
                } else {
                    $("#metadataLocationModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                }
            }
        });

    });
}