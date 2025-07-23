(function( shashin, $, undefined ) {
    shashin.getMapSource = function (source) {
        let mapSource = new ol.source.OSM();

        switch(source) {
            case "osm":
                mapSource = new ol.source.OSM();
                break;
            case "arcGisWSM":
                mapSource = new ol.source.XYZ({
                    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
                    maxZoom: 19
                });
                break;
            case "arcGisWI":
                mapSource = new ol.source.XYZ({
                    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
                    maxZoom: 19
                });
                break;
            case "bingmaps":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "AerialWithLabels", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsROD":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "RoadOnDemand", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsBE":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "BirdseyeWithLabels", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsCD":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "CanvasDark", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "bingmapsSS":
                mapSource = new ol.source.BingMaps({
                    key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                    imagerySet: "Streetside", // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                    // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                    // "no photos at this zoom level" tiles
                    maxZoom: 19
                });
                break;
            case "maptiler":
                mapSource =  new ol.source.TileJSON({
                    url: 'https://api.maptiler.com/maps/streets-v2/256/tiles.json?key=YlQvLcNKq0a4aFDX2z3O',
                    maxZoom: 19
                });
                break;
            case "maptilerHY":
                mapSource = new ol.source.TileJSON({
                    url: 'https://api.maptiler.com/maps/hybrid/256/tiles.json?key=YlQvLcNKq0a4aFDX2z3O',
                    maxZoom: 19
                });
                break;
            case "maptilerBA":
                mapSource = new ol.source.XYZ({
                    url: 'https://api.maptiler.com/maps/basic/256/{z}/{x}/{y}.png?key=YlQvLcNKq0a4aFDX2z3O',
                    maxZoom: 19
                });
                break;
            case "stadiaSA":
                mapSource =  new ol.source.StadiaMaps({
                    layer: 'alidade_satellite',
                    retina: false
                });
                break;
            case "mapbox":
                mapSource = new ol.source.XYZ({
                    url: 'https://api.mapbox.com/v4/mapbox.mapbox-streets-v8/1/0/0.mvt?access_token=pk.eyJ1IjoibWljaGFlbHR5YWdpIiwiYSI6ImNsdHQyeGY5azBxb3YyamxhdGttMzU3aW4ifQ.-2vN-mfBbj-HZh7VWGwFug',
                    maxZoom: 19
                });
                break;
            default:
                mapSource = new ol.source.OSM();
        }

        return mapSource;
    };

    shashin.openHeaderMap = function (metadata) {
        shashin.printMessageToConsole("Opening Siderbar with Map with metadata");
        shashin.printMessageToConsole(metadata);

        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            if (shashin.map === null) {
                const duration = 400;
                const interactions = [
                    new ol.interaction.DoubleClickZoom({
                        duration: duration,
                        useAnchor: false
                    }),
                    new ol.interaction.KeyboardZoom({
                        duration: duration,
                        useAnchor: false
                    }),
                    new ol.interaction.MouseWheelZoom({
                        duration: duration,
                        useAnchor: false
                    }),
                    new ol.interaction.DblClickDragZoom({
                        useAnchor: false
                    }),
                    new ol.interaction.DragZoom({
                        useAnchor: false
                    })
                ];

                shashin.map = new ol.Map({
                    controls: [],
                    layers: [
                        new ol.layer.Tile({
                            visible: true,
                            source: shashin.getMapSource("osm")
                        })
                    ],
                    target: 'headerMap',
                    interactions: interactions
                });
            } else {
                const baseLayer = new ol.layer.Tile({
                    visible: true,
                    source: shashin.getMapSource("osm")
                });
                shashin.map.addLayer(baseLayer);
            }

            if (shashin.layer !== null) {
                shashin.layer.getSource().clear();
            }

            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
            shashin.map.getView().setZoom(18);

            shashin.feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([metadata.lng, metadata.lat])),
                name: metadata.title
            });

            const iconSize = 30;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
            const styleIcon = new ol.style.Style({
                image: new ol.style.Icon({
                    opacity: 1,
                    src: 'data:image/svg+xml;utf8,' + svg,
                    anchor: [0.5, iconSize],
                    anchorXUnits: 'fraction',
                    anchorYUnits: 'pixels',
                    anchorOrigin: 'top-left',
                    offset: [0, 0]
                })
            });
            shashin.feature.setStyle(styleIcon);
            shashin.layer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: [shashin.feature]
                })
            });
            shashin.map.addLayer(shashin.layer);

            setTimeout(shashin.fixContentHeight, 1000);
        }
    };

    shashin.openMap = function (metadata) {
        if (metadata === undefined) {
            metadata = {};
        }
        shashin.printMessageToConsole("Opening Map with metadata",{tag:"latlng"});
        shashin.printMessageToConsole(metadata,{tag:"latlng"});

        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            $("#map").css("display","block");
            $("#mapTabMessage").css("display","block");
            let placeNameDisplayName = (metadata.placeName === null) ? 'Unknown location' : metadata.placeName;
            let placeNameDisplayNameArray = placeNameDisplayName.split(";");
            if (placeNameDisplayNameArray.length > 1) {
                placeNameDisplayName = placeNameDisplayNameArray[0];
            }
            shashin.printMessageToConsole("Opening modal map - original placename: " + metadata.placeName + " - Display placename: " + placeNameDisplayName,{tag:"latlng"});
            let queryParamDates = "";
            if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
                let month = metadata.month;
                if (month < 10) {
                    month = '0'+month;
                }
                let lastDay = metadata.day;
                if (lastDay < 29) {
                    lastDay = 28;
                }
                queryParamDates = '&sd='+metadata.year+'-'+month+'-01&ed='+metadata.year+'-'+month+'-'+lastDay;
            }
            $("#mapTabMessage").html(TimelineTemplates.MapLinks({metadata:metadata, placeNameDisplayName:placeNameDisplayName, queryParamDates:queryParamDates}));
        }

        if (shashin.map === null) {
            const duration = 400;
            const interactions = [
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

            let target = 'modalmap';

            if (metadata.hasOwnProperty("lat") === false && metadata.hasOwnProperty("lng") === false) {
                target = 'modalbatchmap';
            }

            shashin.map = new ol.Map({
                controls: [],
                layers: [
                    new ol.layer.Tile({
                        visible: true,
                        source: shashin.getMapSource("osm")
                    })
                ],
                target: target,
                interactions: interactions
            });
        } else {
            const baseLayer = new ol.layer.Tile({
                visible: true,
                source: shashin.getMapSource("osm")
            });
            shashin.map.addLayer(baseLayer);
        }

        const attributions = new ol.control.Attribution({collapsible: true});

        shashin.map.addControl(attributions);

        const copyPlacename = function (obj) {
            if (obj.hasOwnProperty("data") && obj.data !== null && obj.data !== "" && obj.data.placename !== null && obj.data.placename !== "") {
                const copyText = obj.data.placename;
                Util.copyToClipboard(copyText);
            }
        };

        const saveCoordinates = function (obj) {
            shashin.showToastMessage(shashini18n.main.toast.minimap.title, shashini18n.main.toast.minimap.message.success, {
                icon: "bi-info-circle",
                iconColor: "#777777",
                tag: "latlng",
                autohide: false,
                borderColor:"success"
            });
            const coordArray = ol.proj.toLonLat(obj.coordinate);
            if (coordArray.length > 1) {
                const coords = coordArray[1]+","+coordArray[0];
                const json = {
                    id: metadata.id,
                    latlng: coords
                };
                const http = new Http("save location");
                http.ajax("put", "/metadata/update/coordinates/" + metadata.id + "?v="+uuidv4(), JSON.stringify(json), function (response) {
                    shashin.closeToastMessages({
                        tag: "latlng"
                    });
                    shashin.showToastMessage(shashini18n.main.toast.minimap.title, shashini18n.main.toast.minimap.message.fail, {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        tag: "latlng",
                        borderColor:"danger"
                    });
                }).then(function (response) {
                    shashin.closeToastMessages({
                        tag: "latlng"
                    });
                    if (response.hasOwnProperty("status")) {
                        if (response.status !== shashin.apiResponse.SUCCESS) {
                            shashin.showToastMessage(shashini18n.main.toast.minimap.title, shashini18n.main.toast.minimap.message.fail, {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000",
                                tag: "latlng",
                                borderColor: "danger"
                            });
                        } else {
                            Util.setMetadataLocalStorage();

                            shashin.showToastMessage(shashini18n.main.toast.minimap.title, shashini18n.main.toast.minimap.message.success, {
                                icon: "bi-info-circle",
                                iconColor: "#777777",
                                tag: "latlng",
                                borderColor:"success"
                            });

                            if (response.hasOwnProperty("metadata") && (response.metadata).hasOwnProperty("lat") && (response.metadata).hasOwnProperty("lng")) {
                                metadata.lat = response.metadata.lat;
                                metadata.lng = response.metadata.lng;

                                if ((response.metadata).hasOwnProperty("timeZone")) {
                                    $("#offsetTaken").val(response.metadata.timeZone);
                                }

                                if ((response.metadata).hasOwnProperty("placeName")) {
                                    const placeNameDisplayNameArr = (response.metadata.placeName).split(";");
                                    const placeNameDisplayName = placeNameDisplayNameArr[0];
                                    const placeName = TimelineTemplates.MapLinks({metadata:metadata, placeNameDisplayName:placeNameDisplayName, queryParamDates:""});
                                    $("#mapTabMessage").html(placeName);

                                    if (response.hasOwnProperty("shortPlaceName")) {
                                        $("#shortLocationLabel").html(response.shortPlaceName);
                                        $("#shortLocationLabel").attr("title", placeNameDisplayName);
                                    }
                                }

                                $("#metadataModalEdit"+metadata.id+" span").removeClass("bi-info-square").addClass("bi-info-circle");
                            }

                            // Update marker and center
                            shashin.map.getLayers().forEach(layer => {
                                if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "maplocation") {
                                    shashin.map.removeLayer(layer);
                                }
                            });

                            shashin.map.getView().setCenter(ol.proj.fromLonLat([coordArray[0], coordArray[1]]));
                            shashin.map.getView().setZoom(18);

                            shashin.feature = new ol.Feature({
                                geometry: new ol.geom.Point(ol.proj.fromLonLat([coordArray[0], coordArray[1]]))
                            });

                            const iconSize = 30;
                            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
                            const styleIcon = new ol.style.Style({
                                image: new ol.style.Icon({
                                    opacity: 1,
                                    src: 'data:image/svg+xml;utf8,' + svg,
                                    anchor: [0.5, iconSize],
                                    anchorXUnits: 'fraction',
                                    anchorYUnits: 'pixels',
                                    anchorOrigin: 'top-left',
                                    offset: [0, 0]
                                })
                            });
                            shashin.feature.setStyle(styleIcon);
                            shashin.layer = new ol.layer.Vector({
                                source: new ol.source.Vector({
                                    features: [shashin.feature]
                                })
                            });
                            shashin.layer.set('name', 'maplocation');
                            shashin.map.addLayer(shashin.layer);
                            $("#latlng").val(coordArray[1]+","+coordArray[0]);
                            $("#metadataModalEdit" + metadata.id + " span").removeClass("bi-info-square").addClass("bi-info-circle");
                        }
                    } else {
                        shashin.showToastMessage(shashini18n.main.toast.minimap.title, shashini18n.main.toast.minimap.message.fail, {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            tag: "latlng",
                            borderColor:"danger"
                        });
                    }
                });
            }
        };

        const setBatchCoordinates = function (obj) {
            const coordArray = ol.proj.toLonLat(obj.coordinate);
            if (coordArray.length > 1) {
                // Update marker and center
                shashin.map.getLayers().forEach(layer => {
                    if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "maplocation") {
                        shashin.map.removeLayer(layer);
                    }
                });

                shashin.map.getView().setCenter(ol.proj.fromLonLat([coordArray[0], coordArray[1]]));
                shashin.map.getView().setZoom(18);

                shashin.feature = new ol.Feature({
                    geometry: new ol.geom.Point(ol.proj.fromLonLat([coordArray[0], coordArray[1]]))
                });

                const iconSize = 30;
                const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
                const styleIcon = new ol.style.Style({
                    image: new ol.style.Icon({
                        opacity: 1,
                        src: 'data:image/svg+xml;utf8,' + svg,
                        anchor: [0.5, iconSize],
                        anchorXUnits: 'fraction',
                        anchorYUnits: 'pixels',
                        anchorOrigin: 'top-left',
                        offset: [0, 0]
                    })
                });
                shashin.feature.setStyle(styleIcon);
                shashin.layer = new ol.layer.Vector({
                    source: new ol.source.Vector({
                        features: [shashin.feature]
                    })
                });
                shashin.layer.set('name', 'maplocation');
                shashin.map.addLayer(shashin.layer);
                $("#latlngBatchData").val(coordArray[1]+","+coordArray[0]);

                shashin.showToastMessage(shashini18n.main.toast.minimap.title, shashini18n.main.context.minimap.location.message, {
                    icon: "bi-info-circle",
                    iconColor: "#777777",
                    tag: "latlng",
                    borderColor:"success"
                });
                // });
            }
        };

        const copyCoordinates = function (obj) {
            const coordArray = ol.proj.toLonLat(obj.coordinate);
            if (coordArray.length > 1) {
                const copyText = coordArray[1]+","+coordArray[0];
                Util.copyToClipboard(copyText);
            }
        };

        const recenterCoordinates = function (obj) {
            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
            shashin.map.getView().setZoom(shashin.initialMapZoom);
        };

        shashin.contextMenu = new ContextMenu({
            width: 300,
            defaultItems: false // defaultItems are (for now) Zoom In/Zoom Out
        });
        shashin.contextMenu.on('close', function (evt) {
            shashin.map.getLayers().forEach(layer => {
                if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "tempCoordinates") {
                    shashin.map.removeLayer(layer);
                }
            });
        });

        const showContextMenu = (evt, coordArray, data) => {
            // Clear all previous coordinates
            shashin.map.getLayers().forEach(layer => {
                if (layer && layer.getProperties().hasOwnProperty("name") && layer.getProperties().name === "tempCoordinates") {
                    shashin.map.removeLayer(layer);
                }
            });

            // Create icon for temp coordinate
            const feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat(coordArray)),
                name: 'tempMarker'
            });

            const iconSize = 25;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: grey;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
            const icon = 'data:image/svg+xml;utf8,' + svg;

            const styleIcon = new ol.style.Style({
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

            feature.setStyle(styleIcon);
            feature.setId("tempCoordinates");

            shashin.tempVector = new ol.source.Vector({
                features: [feature]
            });

            const layer = new ol.layer.Vector({
                source: shashin.tempVector
            });
            layer.set('name', 'tempCoordinates');
            shashin.map.addLayer(layer);

            feature.setStyle(styleIcon);
            layer.getSource().addFeature(feature);

            // Create menu for context menu
            const copyText = shashini18n.main.context.minimap.copy + " " + coordArray[1] + "," + coordArray[0];
            shashin.contextMenu.updatePosition([evt.pixel[0], evt.pixel[1] + 12]);

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

            if ($("#propMetadata").hasClass('show') === true && $("#generalTabNav").length > 0) {
                contextValueArray.push(
                    {
                        text: shashini18n.main.context.minimap.savecoord, // Set coordinates in modal field
                        callback: saveCoordinates
                    }
                );
            } else if ($("#propBatchMetadata").hasClass('show') === true) {
                contextValueArray.push(
                    {
                        text: shashini18n.main.context.minimap.setlatlng, // Set coordinates in modal field
                        callback: setBatchCoordinates
                    }
                );
            }


            contextValueArray.push(
                {
                    text: copyText, // Copy coordinates from context menu
                    callback: copyCoordinates
                },
                {
                    text: shashini18n.main.context.minimap.recenter, // Recenter map to media location
                    callback: recenterCoordinates
                }
            );

            shashin.contextMenu.extend(contextValueArray);
        };

        shashin.contextMenu.on('open', function (evt) {
            shashin.contextMenu.clear();
            const coordArray = ol.proj.toLonLat(evt.coordinate);
            const http = new Http("get place data");

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

        shashin.map.addControl(shashin.contextMenu);

        if (shashin.layer !== null) {
            shashin.layer.getSource().clear();
        }

        if (metadata.hasOwnProperty("lat") && metadata.hasOwnProperty("lng")) {
            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
        } else {
            shashin.map.getView().setCenter(ol.proj.fromLonLat([0, 0]));
        }

        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            shashin.map.getView().setZoom(shashin.initialMapZoom);

            shashin.feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([metadata.lng, metadata.lat])),
                name: metadata.title
            });

            const iconSize = 30;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-geo-alt-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M8 16s6-5.686 6-10A6 6 0 0 0 2 6c0 4.314 6 10 6 10zm0-7a3 3 0 1 1 0-6 3 3 0 0 1 0 6z"/></svg>';
            const styleIcon = new ol.style.Style({
                image: new ol.style.Icon({
                    opacity: 1,
                    src: 'data:image/svg+xml;utf8,' + svg,
                    anchor: [0.5, iconSize],
                    anchorXUnits: 'fraction',
                    anchorYUnits: 'pixels',
                    anchorOrigin: 'top-left',
                    offset: [0, 0]
                })
            });
            shashin.feature.setStyle(styleIcon);
            shashin.layer = new ol.layer.Vector({
                source: new ol.source.Vector({
                    features: [shashin.feature]
                })
            });
            shashin.layer.set('name', 'maplocation');
            shashin.map.addLayer(shashin.layer);
        } else {
            shashin.map.getView().setZoom(0);
        }

        setTimeout(shashin.fixContentHeight, 1000);
        // else {
        //     if (shashin.layer !== null) {
        //         shashin.layer.getSource().clear();
        //     }
        //     $("#map").css("display","none");
        //     $("#mapTabMessage > .wrapper").contents().unwrap();
        //     $("#mapTabMessage").text("No map data");
        //     $("#mapTabMessage").css("display","block");
        // }
    };
}( window.shashin = window.shashin || {}, jQuery ));