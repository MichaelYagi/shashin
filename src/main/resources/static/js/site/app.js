// Globals
$.fn.hasScrollBar = function() {
    return this.get(0).scrollHeight > this.get(0).clientHeight;
}

$.fn.serializeObject = function() {
    const o = {};
    const a = this.serializeArray();
    $.each(a, function() {
        if (o[this.name]) {
            if (!o[this.name].push) {
                o[this.name] = [o[this.name]];
            }
            o[this.name].push(this.value || '');
        } else {
            o[this.name] = this.value || '';
        }
    });
    return o;
};

(function( shashin, $, undefined ) {
    // private function
    function fallbackCopyTextToClipboard(text) {
        let msg = "";
        const textArea = document.createElement("textarea");
        textArea.value = text;

        // Avoid scrolling to bottom
        textArea.style.top = "0";
        textArea.style.left = "0";
        textArea.style.position = "fixed";

        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();

        try {
            const successful = document.execCommand('copy');
            msg = "<div class=\"alert alert-warning\" role=\"alert\">Could not copy text</div>";
            if (successful === true) {
                msg = "<div class=\"alert alert-success\" role=\"alert\">Link copied to clipboard!</div>";
            }
        } catch (err) {
            msg = "<div class=\"alert alert-warning\" role=\"alert\">Could not copy text</div>";
        }

        document.body.removeChild(textArea);

        return msg;
    }

    function getShortDay(index) {
        const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        return days[index];
    }
    function getShortMonths(index) {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        return months[index];
    }

    shashin.showDebug = false;
    shashin.map = null;
    shashin.layer = null;
    shashin.feature = null;
    shashin.infiniteScrollGallery = null;
    shashin.lg = null;
    shashin.ajaxRetries = 3;
    shashin.timelineQueryLimit = 1;
    shashin.scrollDirection = "down";

    function fixContentHeight(){
        const viewHeight = $(window).height();
        const header = $("div[data-role='header']:visible:visible");
        const navbar = $("div[data-role='navbar']:visible:visible");
        const content = $("div[data-role='content']:visible:visible");
        const contentHeight = viewHeight - header.outerHeight() - navbar.outerHeight();
        content.height(contentHeight);
        shashin.map.updateSize();
    }

    shashin.initLightGallery = function(lgElement,additionalLgConfigs,mediaElement) {
        shashin.setLightGalleryElement(lgElement);
        shashin.setLightGallery(additionalLgConfigs);

        let mediaContentList = [];
        $.each($(mediaElement), function() {
            const mediaContent = {};
            mediaContent.subHtml =$(this).attr("data-sub-html")
            if ($(this).attr("data-src")) {
                mediaContent.src = $(this).attr("data-src");
                mediaContent.downloadUrl = $(this).attr("data-src");
            } else if ($(this).attr("data-video")) {
                mediaContent.video = $(this).attr("data-video");
                mediaContent.downloadUrl = $(this).attr("data-video")+"/download";
            }
            mediaContentList.push(mediaContent);
        });
        shashin.initMediaContent(mediaContentList);

        return mediaContentList;
    }

    shashin.initMediaContent = function(mediaContentList) {
        if (mediaContentList.length > 0 && shashin.getLightGallery() !== null) {
            shashin.refreshAndActivateLgListener(mediaContentList);
        }
    }

    shashin.updateMediaContent = function(mediaContentList,additionalMediaContentList) {
        if (additionalMediaContentList.length > 0) {
            mediaContentList = mediaContentList.concat(additionalMediaContentList);
            shashin.refreshAndActivateLgListener(mediaContentList);
        }

        return mediaContentList;
    }

    shashin.refreshAndActivateLgListener = function (mediaContentList) {
        if (shashin.getLightGallery() !== null) {
            shashin.getLightGallery().refresh(mediaContentList);
            shashin.getLightGalleryElement().addEventListener('lgAfterSlide', function (e) {
                shashin.jumpToLightGalleryIndex(e.detail.index);
            })
        }
    }

    shashin.pageLoader = function(func, appendClass, list, conditionOnNext) {
        const refreshIntervalId = window.setInterval(function () {
            if (!$("#container").hasScrollBar() && !$("main").hasScrollBar()) {
                setTimeout(() => {
                    if (conditionOnNext === true) {
                        func();
                    }
                }, 1000);
            } else {
                clearInterval(refreshIntervalId);
            }

            if ($(appendClass).last().text() === "EOL" || list === '' || list === '[]') {
                clearInterval(refreshIntervalId);
            }
        }, 200);
        $("#container").on('scroll', function() {
            if (shashin.atEndOfPage(this) && $(appendClass).last().text() !== "EOL") {
                if (conditionOnNext === true) {
                    func();
                }
            }
        })
        $("main").on('scroll', function() {
            if (shashin.atEndOfPage(this) && $(appendClass).last().text() !== "EOL") {
                if (conditionOnNext === true) {
                    func();
                }
            }
        })
    }

    shashin.openMap = function (metadata) {
        if (Object.keys(metadata).length > 0 &&
            metadata.lat !== null && metadata.lng !== null &&
            metadata.lat !== "" && metadata.lng !== ""
        ) {
            $("#map").css("display","block");
            $("#mapTabMessage").css("display","block");
            $("#mapTabMessage").text(metadata.placeName);
            $("#mapTabMessage").wrapInner('<a href="/map?lat='+metadata.lat+'&lng='+metadata.lng+'" target="_blank" class="bi-pin-fill" style="text-decoration: none;"></a>');

            if (shashin.map === null) {
                shashin.map = new ol.Map({
                    controls: new ol.control.defaults({
                        attributionOptions: {
                            collapsible: true
                        }
                    }),
                    layers: [
                        new ol.layer.Tile({
                            visible: true,
                            source: shashin.getMapSource("osm")
                        })
                    ],
                    target: 'map',
                    view: new ol.View({
                        center: ol.proj.fromLonLat([metadata.lng, metadata.lat]),
                        maxZoom: 19,
                        zoom: 15
                    })
                });
            }

            if (shashin.layer !== null && shashin.feature !== null) {
                shashin.layer.getSource().clear();
            }

            shashin.map.getView().setCenter(ol.proj.fromLonLat([metadata.lng, metadata.lat]));
            shashin.map.getView().setZoom(15);

            shashin.feature = new ol.Feature({
                geometry: new ol.geom.Point(ol.proj.fromLonLat([metadata.lng, metadata.lat])),
                name: metadata.title
            });

            const iconSize = 20;
            const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + iconSize + '" height="' + iconSize + '" fill="currentColor" class="bi bi-pin-fill" style="color: orangered;" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M4.146.146A.5.5 0 014.5 0h7a.5.5 0 01.5.5c0 .68-.342 1.174-.646 1.479-.126.125-.25.224-.354.298v4.431l.078.048c.203.127.476.314.751.555C12.36 7.775 13 8.527 13 9.5a.5.5 0 01-.5.5h-4v4.5c0 .276-.224 1.5-.5 1.5s-.5-1.224-.5-1.5V10h-4a.5.5 0 01-.5-.5c0-.973.64-1.725 1.17-2.189A5.921 5.921 0 015 6.708V2.277a2.77 2.77 0 01-.354-.298C4.342 1.674 4 1.179 4 .5a.5.5 0 01.146-.354z"/></svg>';
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

            setTimeout(fixContentHeight, 1000);
        } else {
            if (shashin.layer !== null && shashin.feature !== null) {
                shashin.layer.getSource().clear();
            }
            $("#map").css("display","none");
            $("#mapTabMessage > .wrapper").contents().unwrap();
            $("#mapTabMessage").text("No map data");
            $("#mapTabMessage").css("display","block");
        }
    }

    shashin.populateDetailsTab = function(metadata) {
        // Clear data
        $("#pathDetails").text("");
        $("#typeDetails").text("");
        $("#isoDetails").text("");
        $("#compressionDetails").text("");
        $("#exposureDetails").text("");
        $("#fNumberDetails").text("");
        $("#focalLengthDetails").text("");
        $("#cameraDetails").text("");
        $("#lensDetails").text("");
        $("#qualityDetails").text("");
        $("#createdAtDetails").text("");
        $("#modifiedAtDetails").text("");
        $("#takenAtDetails").text("");
        $("#manualTakenAtDetails").text("");
        $("#timeZoneDetails").text("");
        $("#keywordsDetails").text("");

        // Fill in details tab data
        if (metadata.path != null) {
            $("#pathDetails").text(metadata.path);
        }
        if (metadata.keywords != null) {
            $("#keywordsDetails").text(metadata.keywords);
        }
        if (metadata.type != null) {
            $("#typeDetails").text(metadata.type);
        }
        if (metadata.iso != null) {
            $("#isoDetails").text(metadata.iso);
        }
        if (metadata.compressionType != null) {
            $("#compressionDetails").text(metadata.compressionType);
        }
        if (metadata.exposure != null) {
            $("#exposureDetails").text(metadata.exposure);
        }
        if (metadata.fNumber != null) {
            $("#fNumberDetails").text(metadata.fNumber);
        }
        if (metadata.focalLength != null) {
            $("#focalLengthDetails").text(metadata.focalLength);
        }
        if (metadata.camera != null) {
            $("#cameraDetails").text(metadata.camera);
        }
        if (metadata.lens != null) {
            $("#lensDetails").text(metadata.lens);
        }
        if (metadata.quality != null) {
            $("#qualityDetails").text(metadata.quality);
        }
        if (metadata.createdAt != null) {
            $("#createdAtDetails").text(metadata.createdAt);
        }
        if (metadata.modifiedAt != null) {
            $("#modifiedAtDetails").text(metadata.modifiedAt);
        }
        if (metadata.takenAt != null) {
            $("#takenAtDetails").text(metadata.takenAt);
        }
        if (metadata.year !== null && metadata.month !== null && metadata.day !== null) {
            let takenDetails = metadata.year + '-' + metadata.month + '-' + metadata.day;
            if (metadata.time !== null && metadata.time !== "") {
                takenDetails += ' ' + metadata.time;
            }
            $("#manualTakenAtDetails").text(takenDetails);
        }
        if (metadata.timeZone != null) {
            $("#timeZoneDetails").text(metadata.timeZone);
        }
    }

    shashin.openInfoModal = function(metadata) {
        // Populate modal data

        if ($("#infoModalEdit"+metadata.id).attr("tag") && $("#infoModalEdit"+metadata.id).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#infoModalEdit"+metadata.id).attr("tag"));
        }

        $("#infoModalTitle").text(metadata.fileName);
        $("#currentfilename").val(metadata.fileName)
        $("#currentlat").val(metadata.lat)
        $("#currentlng").val(metadata.lng)
        $("#metadataId").val(metadata.id);

        if (metadata.thumbnailUrlCentered !== null) {
            $("#propInfoModalThumbnail").html('<img src="'+encodeURI(metadata.thumbnailUrlCentered)+'" height="100" width="100" onError="shashin.errorImg(this,\''+metadata.title+'\',100)">');
        }

        shashin.populateDetailsTab(metadata);

        // Open modal window
        $("#propInfoModal").modal('show');
    }

    shashin.addToMetadataThumbnailsList = function(thumbnail) {
        if ($("#multiSelectThumbnails").length > 0) {
            const metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
            if (metadataThumbnailsArray.indexOf(thumbnail) === -1) {
                metadataThumbnailsArray.push(thumbnail);
                $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
            }
        }
    }

    shashin.removeFromMetadataThumbnailsList = function(thumbnail) {
        if ($("#multiSelectThumbnails").length > 0) {
            const metadataThumbnailsArray = shashin.getMetadataThumbnailsList();
            const index = metadataThumbnailsArray.indexOf(thumbnail);
            if (index > -1) {
                metadataThumbnailsArray.splice(index, 1);
            }
            $("#multiSelectThumbnails").val(JSON.stringify(metadataThumbnailsArray));
        }
    }

    shashin.getMetadataThumbnailsList = function() {
        if ($("#multiSelectThumbnails").length > 0) {
            return JSON.parse($("#multiSelectThumbnails").val());
        }

        return [];
    }

    shashin.removeFromMetadataFilenamesList = function(filename) {
        if ($("#multiSelectFilenames").length > 0) {
            const metadataFilenamesArray = shashin.getMetadataFilenamesList();
            const index = metadataFilenamesArray.indexOf(filename);
            if (index > -1) {
                metadataFilenamesArray.splice(index, 1);
            }
            $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
        }
    }

    shashin.getMetadataFilenamesList = function() {
        if ($("#multiSelectFilenames").length > 0) {
            return JSON.parse($("#multiSelectFilenames").val());
        }

        return [];
    }

    shashin.addToMetadataFilenamesList = function (filename) {
        if ($("#multiSelectFilenames").length > 0) {
            const metadataFilenamesArray = shashin.getMetadataFilenamesList();
            if (metadataFilenamesArray.indexOf(filename) === -1) {
                metadataFilenamesArray.push(filename);
                $("#multiSelectFilenames").val(JSON.stringify(metadataFilenamesArray));
            }
        }
    }

    shashin.addToMetadataIdList = function (metadataId) {
        if ($("#multiSelectMetadataIds").length > 0) {
            const metadataIdArray = shashin.getMetdataIdList();
            if (metadataIdArray.indexOf(metadataId) === -1) {
                metadataIdArray.push(metadataId);
                $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
            }
        }
    }

    shashin.removeFromMetadataIdList = function (metadataId) {
        if ($("#multiSelectMetadataIds").length > 0) {
            const metadataIdArray = shashin.getMetdataIdList();
            const index = metadataIdArray.indexOf(metadataId);
            if (index > -1) {
                metadataIdArray.splice(index, 1);
            }
            $("#multiSelectMetadataIds").val(JSON.stringify(metadataIdArray));
        }
    }

    shashin.getMetdataIdList = function() {
        if ($("#multiSelectMetadataIds").length > 0) {
            return JSON.parse($("#multiSelectMetadataIds").val());
        }

        return [];
    }

    shashin.removeAllMetadataIdList = function () {
        if ($("#multiSelectMetadataIds").length > 0) {
            $("#multiSelectMetadataIds").val(JSON.stringify([]));
        }
    }

    shashin.removeAllMetadataFilenamesList = function () {
        if ($("#multiSelectFilenames").length > 0) {
            $("#multiSelectFilenames").val(JSON.stringify([]));
        }
    }

    shashin.removeAllMetadataThumbnailsList = function () {
        if ($("#multiSelectThumbnails").length > 0) {
            $("#multiSelectThumbnails").val(JSON.stringify([]));
        }
    }

    shashin.jumpToLightGalleryIndex = function (index) {
        const url = location.href;
        location.href = '#lightGalleryIndex'+index;
        history.replaceState(null,null,url);
    }

    shashin.getParameterByName = function (name, url = window.location.href) {
        name = name.replace(/[\[\]]/g, '\\$&');
        const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    // Detect scrolling to bottom of page
    shashin.atEndOfPage = function (element) {
        return ((window.innerHeight + element.scrollTop)  >= element.scrollHeight) // compare with scroll position + some give (*1.5)
    }

    shashin.copyTextToClipboard = function (text,id) {
        if (!navigator.clipboard) {
            $("#msg"+id).html(fallbackCopyTextToClipboard(text));
        } else {
            navigator.clipboard.writeText(text).then(function () {
                $("#msg" + id).html("<div class=\"alert alert-success\" role=\"alert\">Link copied to clipboard!</div>");
            }, function (err) {
                $("#msg" + id).html("<div class=\"alert alert-warning\" role=\"alert\">Could not copy text</div>");
            });
        }
    }

    shashin.setLightGalleryElement = function (name) {
        shashin.infiniteScrollGallery = null;
        if (document.getElementById(name)) {
            shashin.infiniteScrollGallery = document.getElementById(name);
        }
    };

    shashin.setLightGallery = function (additionalConfigs) {
        let configs = shashin.getLightGalleryConfigs(additionalConfigs);
        shashin.lg = lightGallery(shashin.getLightGalleryElement(), configs);
    }

    shashin.getLightGalleryElement = function () {
        return shashin.infiniteScrollGallery;
    };

    shashin.getLightGallery = function () {
        return shashin.lg;
    }

    shashin.openGallery = function (e, index) {
        e.preventDefault();
        if (shashin.getLightGallery() !== null) {
            shashin.getLightGallery().openGallery(index);
        }
    }

    shashin.getLightGalleryConfigs = function(additionalConfigs) {
        const configs = {
            plugins: [lgZoom, lgVideo, lgRelativeCaption, lgFullscreen],
            counter: false,
            preload: 0,
            fullScreen: true,
            download: true,
            zoomFromOrigin: true,
            speed: 0,
            licenseKey: "A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3"
        }

        for (const key in additionalConfigs) {
            configs[key] = additionalConfigs[key];
        }

        return configs;
    }

    shashin.getMapSource = function (source) {
        const validSources = ["osm","bingmaps","maptiler","mapbox"];

        let mapSource = new ol.source.OSM();

        if (validSources.includes(source)) {
            switch(source) {
                case "osm":
                    mapSource = new ol.source.OSM();
                    break
                case "bingmaps":
                    mapSource = new ol.source.BingMaps({
                        key: 'AgLAysLWWJdjeVeOTVUYlNfUddxeF6QFeXCciHblaYSG7xYx3OUuAnpX98MNQUFR', // Dev/Test
                        imagerySet: 'RoadOnDemand', // 'Aerial', 'RoadOnDemand', 'AerialWithLabels', etc.
                        // use maxZoom 19 to see stretched tiles instead of the Bing Maps
                        // "no photos at this zoom level" tiles
                        maxZoom: 19
                    });
                    break
                case "maptiler":
                    mapSource =  new ol.source.XYZ({
                        url: 'https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=YlQvLcNKq0a4aFDX2z3O',
                        maxZoom: 19
                    });
                    break;
                case "mapbox":
                    mapSource = new ol.source.XYZ({
                        url: 'https://{a-c}.tiles.mapbox.com/v4/mapbox.mapbox-streets-v6/{z}/{x}/{y}.vector.pbf?access_token=pk.eyJ1IjoibWljaGFlbHR5YWdpIiwiYSI6ImNrdGszZXNrdTFocTcyd29sMG1hYXprdmsifQ.RVimlhqPIKKTYmaGyr-ThQ'
                    });
                    break;
                default:
                    mapSource = new ol.source.OSM();
            }
        }

        return mapSource
    }

    shashin.getDateString = function (year,month,day) {
        if (year !== null && year !== "" &&
            month !== null && month !== "" &&
            day !== null && day !== ""
        ) {
            let date = new Date(month+"/"+day+"/"+year);
            if (date.toString() !== "Invalid Date") {
                let shortMonth = getShortMonths(date.getMonth());
                let adjustedDay = date.getDate();
                let dayOfWeek = getShortDay(date.getDay());
                return dayOfWeek + ", " + shortMonth + " " + adjustedDay + ", " + year;
            }
        }
        return "";
    }

    shashin.getDateObject = function (dateString) {
        if (dateString.indexOf("tail_") >= 0) {
            const idParts = dateString.split("tail_");
            dateString = idParts[1];
        }
        if (typeof dateString !== "undefined" && dateString !== null) {
            const dateStringParts = dateString.split("-");
            if (dateStringParts.length === 3) {
                const year = dateStringParts[0];
                const month = dateStringParts[1];
                const day = dateStringParts[2];

                if (year !== null && year !== "" &&
                    month !== null && month !== "" &&
                    day !== null && day !== ""
                ) {
                    return new Date(month + "/" + day + "/" + year);
                }
            }
        }
        return null;
    }

    shashin.isNumeric = function (str) {
        if (typeof str != "string") return false // we only process strings!
        return !isNaN(str) && // use type coercion to parse the _entirety_ of the string (`parseFloat` alone does not do this)...
            !isNaN(parseFloat(str)) // ...and ensure strings of whitespace fail
    }

    shashin.decodeHtml = function(html) {
        const txt = document.createElement("textarea");
        txt.innerHTML = html;
        return txt.value;
    }

    shashin.encodeHtml = function (str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    shashin.setPhotoOverlays = function (metadata, view) {
        const opaque = 0.3
        const transparent = 1.0

        let metadataIdArray = shashin.getMetdataIdList();
        shashin.printMessageToConsole(metadataIdArray);
        const index = metadataIdArray.indexOf(metadata.id);
        if (index > -1) {
            $("#tntl" + metadata.id).css("display", "block");
            $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
            $("#image" + metadata.id).css("opacity", opaque);
            $("#tncentered" + metadata.id).css("display", "none");
            $("#tnbr" + metadata.id).css("display", "none");
            $("#tnbl" + metadata.id).css("display", "none");
        }

        $("#select" + metadata.id).click(function (e) {
            e.preventDefault();

            if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                $("#tntl" + metadata.id).css("display", "block");
                $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
                $("#image" + metadata.id).css("opacity", opaque);
                //$("#tntr" + metadata.id).css("display", "none");
                $("#tncentered" + metadata.id).css("display", "none");
                $("#tnbr" + metadata.id).css("display", "none");
                $("#tnbl" + metadata.id).css("display", "none");
                shashin.addToMetadataIdList(metadata.id);
                shashin.addToMetadataFilenamesList($('#filename' + metadata.id).val());
                shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
            } else {
                $("#tntl" + metadata.id).css("display", "block");
                $("#tlicon" + metadata.id).addClass('bi-circle').removeClass('bi-circle-fill');
                $("#image" + metadata.id).css("opacity", opaque);
                $("#tntr" + metadata.id).css("display", "block");
                $("#tncentered" + metadata.id).css("display", "block");
                $("#tnbr" + metadata.id).css("display", "block");
                $("#tnbl" + metadata.id).css("display", "block");
                shashin.removeFromMetadataIdList(metadata.id);
                shashin.removeFromMetadataFilenamesList($('#filename' + metadata.id).val());
                shashin.removeFromMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
            }

            metadataIdArray = shashin.getMetdataIdList();

            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $("#appSearch").css("display", "none");
                if (view === "album" || view === "favorites" || view === "trash") {
                    $("#albumAppTools").css("display", "block");
                } else if (view === "timeline") {
                    $("#timelineAppTools").css("display", "block");
                } else if (view === "matches" || view === "person") {
                    $("#matchesAppTools").css("display", "block");
                }

                // Hide all center and bottom left icons
                $('.thumbnail-br').hide();
                $('.thumbnail-bl').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-centered').hide();
            } else {
                $("#appSearch").css("display", "block");
                $("#timelineAppTools").css("display", "none");
                $("#albumAppTools").css("display", "none");
                $("#matchesAppTools").css("display", "none");
            }

            const metadataList = shashin.getMetdataIdList();
            let timelineSelectCount = $('.bi-circle-fill').length;
            if (metadataList.length > 0) {
                timelineSelectCount = metadataList.length;
            }
            $("#timelineNumberSelected").text(timelineSelectCount+" Selected");
            $("#matchesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#favoritesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#trashNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#albumNumberSelected").text($('.bi-circle-fill').length+" Selected");
        });

        $("#image" + metadata.id).click(function (e) {
            e.preventDefault();

            // Fill top left icon when clicking anywhere on thumbnail
            if ($('.bi-circle-fill')[0] || metadataIdArray.length > 0) {
                if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                    $("#tntl" + metadata.id).css("display", "block");
                    $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
                    $("#image" + metadata.id).css("opacity", opaque);
                    $("#tncentered" + metadata.id).css("display", "none");
                    $("#tnbr" + metadata.id).css("display", "none");
                    $("#tnbl" + metadata.id).css("display", "none");
                    shashin.addToMetadataIdList(metadata.id);
                    shashin.addToMetadataFilenamesList($('#filename' + metadata.id).val());
                    shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
                } else {
                    $("#tntl" + metadata.id).css("display", "block");
                    $("#tlicon" + metadata.id).addClass('bi-circle').removeClass('bi-circle-fill');
                    $("#image" + metadata.id).css("opacity", opaque);
                    $("#tncentered" + metadata.id).css("display", "block");
                    $("#tnbr" + metadata.id).css("display", "block");
                    $("#tnbl" + metadata.id).css("display", "block");
                    shashin.removeFromMetadataIdList(metadata.id);
                    shashin.removeFromMetadataFilenamesList($('#filename' + metadata.id).val());
                    shashin.removeFromMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
                }
            }

            metadataIdArray = shashin.getMetdataIdList();

            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $("#appSearch").css("display", "none");
                if (view === "album" || view === "favorites" || view === "trash") {
                    $("#albumAppTools").css("display", "block");
                } else if (view === "timeline") {
                    $("#timelineAppTools").css("display", "block");
                } else if (view === "matches" || view === "person") {
                    $("#matchesAppTools").css("display", "block");
                }

                // Hide all center and bottom left icons
                $('.thumbnail-br').hide();
                $('.thumbnail-bl').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-centered').hide();
            } else {
                $("#appSearch").css("display", "block");
                //$('.thumbnail-br').show();
                $("#timelineAppTools").css("display", "none");
                $("#albumAppTools").css("display", "none");
                $("#matchesAppTools").css("display", "none");
            }

            let timelineSelectCount = $('.bi-circle-fill').length;
            if (metadataIdArray.length > 0) {
                timelineSelectCount = metadataIdArray.length;
            }
            $("#timelineNumberSelected").text(timelineSelectCount+" Selected");
            $("#matchesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#favoritesNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#trashNumberSelected").text($('.bi-circle-fill').length+" Selected");
            $("#albumNumberSelected").text($('.bi-circle-fill').length+" Selected");
        });

        $("#image" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            const index = metadataIdArray.indexOf(metadata.id);

            $(this).css("opacity", 0.3);
            $(this).siblings().css("display", "block");
            if ($("#tlicon" + metadata.id).attr("class") === "bi-circle-fill" || index > -1) {
                $("#tncentered" + metadata.id).css("display", "none");
                $("#tnbl" + metadata.id).css("display", "none");
                $("#tnbr" + metadata.id).css("display", "none");
                //$("#tntr" + metadata.id).css("display", "none");
            }
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            metadataIdArray = shashin.getMetdataIdList();
            const index = metadataIdArray.indexOf(metadata.id);

            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill" && index <= -1) {
                $(this).css("opacity", 1.0);
                $(this).siblings(".thumbnail-tl").css("display", "none");
                $(this).siblings(".thumbnail-bl").css("display", "none");
                $(this).siblings(".thumbnail-centered").css("display", "none");
                //$(this).siblings(".thumbnail-tr").css("display", "none");
                $(this).siblings(".thumbnail-br").css("display", "none");
            } else {
                if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                    $('.thumbnail-bl').hide();
                    $('.thumbnail-centered').hide();
                    //$('.thumbnail-tr').hide();
                    $('.thumbnail-br').hide();
                }
                $("#tncentered" + metadata.id).css("display", "none");
                $("#tnbl" + metadata.id).css("display", "none");
                //$("#tntr" + metadata.id).css("display", "none");
                $("#tnbr" + metadata.id).css("display", "none");
            }
        });

        $("#tncentered" + metadata.id).hover(function () {
            $('#currentlat').val(metadata.lat === null ? "" : metadata.lat);
            $('#currentlng').val(metadata.lng === null ? "" : metadata.lng);
            $('#currentyear').val(metadata.year === null ? "" : metadata.year);
            $('#currentmonth').val(metadata.month === null ? "" : metadata.month);
            $('#currentday').val(metadata.day === null ? "" : metadata.day);
            $('#currentfilename').val(metadata.fileName === null ? "" : metadata.fileName);
            metadataIdArray = shashin.getMetdataIdList();

            $('.bi-play-circle').css("color", "midnightblue");
            $(this).css("display", "block");
            $(this).siblings(".thumbnail-tl").css("display", "block");
            $(this).siblings(".thumbnail-bl").css("display", "block");
            $(this).siblings(".thumbnail-br").css("display", "block");
            $(this).siblings(".thumbnail-tr").css("display", "block");
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            $('.bi-play-circle').css("color", "lightgray");
            $(this).css("display", "none");
            $(this).siblings(".thumbnail-tl").css("display", "none");
            $(this).siblings(".thumbnail-bl").css("display", "none");
            $(this).siblings(".thumbnail-br").css("display", "none");
            //$(this).siblings(".thumbnail-tr").css("display", "none");
            $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
        });

        $("#tntl" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            const index = metadataIdArray.indexOf(metadata.id);
            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill" && index <= -1) {
                $(this).css("display", "block");
                $(this).siblings(".thumbnail-centered").css("display", "block");
                $(this).siblings(".thumbnail-tr").css("display", "block");
                $(this).siblings(".thumbnail-br").css("display", "block");
                $(this).siblings(".thumbnail-bl").css("display", "block");
                $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
                if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                    $('.thumbnail-bl').hide();
                    $('.thumbnail-centered').hide();
                    //$('.thumbnail-tr').hide();
                    $('.thumbnail-br').hide();
                }
            }
        }, function () {
            if ($("#tlicon" + metadata.id).attr("class") !== "bi-circle-fill") {
                $(this).css("display", "none");
                $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
            } else {
                $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            }
            $(this).siblings(".thumbnail-centered").css("display", "none");
            //$(this).siblings(".thumbnail-tr").css("display", "none");
            $(this).siblings(".thumbnail-br").css("display", "none");
            $(this).siblings(".thumbnail-bl").css("display", "none");
        });

        $("#tnbl" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            $(this).css("display", "block");
            $(this).siblings(".thumbnail-tl").css("display", "block");
            $(this).siblings(".thumbnail-centered").css("display", "block");
            $(this).siblings(".thumbnail-tr").css("display", "block");
            $(this).siblings(".thumbnail-br").css("display", "block");
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            $(this).css("display", "none");
            $(this).siblings(".thumbnail-tl").css("display", "none");
            $(this).siblings(".thumbnail-centered").css("display", "none");
            //$(this).siblings(".thumbnail-tr").css("display", "none");
            $(this).siblings(".thumbnail-br").css("display", "none");
            $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
        });

        $("#tnbr" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            $(this).css("display", "block");
            $(this).siblings(".thumbnail-tl").css("display", "block");
            $(this).siblings(".thumbnail-centered").css("display", "block");
            $(this).siblings(".thumbnail-tr").css("display", "block");
            $(this).siblings(".thumbnail-bl").css("display", "block");
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            $(this).css("display", "none");
            $(this).siblings(".thumbnail-tl").css("display", "none");
            $(this).siblings(".thumbnail-centered").css("display", "none");
            //$(this).siblings(".thumbnail-tr").css("display", "none");
            $(this).siblings(".thumbnail-bl").css("display", "none");
            $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
        });

        $("#tntr" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            $(this).css("display", "block");
            $(this).siblings(".thumbnail-tl").css("display", "block");
            $(this).siblings(".thumbnail-centered").css("display", "block");
            $(this).siblings(".thumbnail-bl").css("display", "block");
            $(this).siblings(".thumbnail-br").css("display", "block");
            $(this).siblings(".photo-thumbnail-image").css("opacity", opaque);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            if ($(this).siblings(".thumbnail-tl").find('.bi-circle-fill').length === 0) {
                $(this).siblings(".thumbnail-tl").css("display", "none");
                $(this).siblings(".thumbnail-centered").css("display", "none");
                $(this).siblings(".thumbnail-bl").css("display", "none");
                $(this).siblings(".thumbnail-br").css("display", "none");
                $(this).siblings(".photo-thumbnail-image").css("opacity", transparent);
            }
        });
    }

    shashin.getTopRightOverlay = function (type, id, content, width, height, isTagged) {
        let html = '<div class="thumbnail-tr" id="tntr' + id + '">\n';

        if (type.includes("video")) {
            html +=
                '       <span class="overlayIconBackground">'+content+'&nbsp;<span id="video' + id + '" class="bi-camera-video overlayIcon"></span></span><br>\n';
        } else if (width !== null && height !== null && width > height*2) {
            html +=
                '       <span id="panorama' + id + '" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span><br>\n';
        }
        if (isTagged === true) {
            html +=
                '       <span class="bi-bookmark-fill overlayIconBackground" style="font-size: 1rem;color: lightsalmon;"></span>\n';
        }

        html += '</div>\n';

        return html;
    }

    shashin.getTopLeftOverlay = function (id) {
        return '<div class="thumbnail-tl" id="tntl' + id + '">\n' +
            '   <a href="#" id="select' + id + '">\n' +
            '       <span id="tlicon' + id + '" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
            '   </a>\n' +
            '</div>\n';
    }

    shashin.getBottomLeftOverlay = function (id, targetPrefix, onclickIdPrefix, onclickFunctionCall, editClass) {
        let html = "";

        html =
            '<div class="thumbnail-bl" id="tnbl'+id+'">\n' +
            '   <a href="#" id="infoModalEdit'+id+'">\n' +
            '       <span class="bi-info-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
            '   </a>\n';

        if (onclickFunctionCall != null || targetPrefix != null) {
            html +=
                '<br>\n';

            if (onclickFunctionCall != null) {
                html +=
                '<a href="#" id="'+onclickIdPrefix+id+'"\n' +
                '   onclick="return '+onclickFunctionCall+'(event, \''+id+'\')" class="'+editClass+'">\n' +
                '   <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>\n' +
                '</a>\n';
            } else if (targetPrefix != null) {
                html +=
                '<a href="#" data-bs-toggle="modal" data-bs-target="#'+targetPrefix+id+'">\n' +
                '   <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>\n' +
                '</a>\n';
            }
        }

        html += '</div>\n';

        return html;
    }

    shashin.getCenteredOverlay = function (metadata,onclickFunctionCall,index) {
        let html = "";
        const dateString = shashin.getDateString(metadata["year"], metadata["month"], metadata["day"]);
        const mediaContent = {};

        html +=
            '   <div class="thumbnail-centered" id="tncentered' + metadata.id + '">\n';

        mediaContent.subHtml = (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '');
        if (metadata.type.includes("video")) {
            mediaContent.video = {"source":[{"src":metadata.videoUrl,"type":"video/mp4"}],"attributes":{"preload":false,"controls":true}};
            mediaContent.downloadUrl = encodeURI(metadata.videoUrl)+"/download";
            html +=
                '   <a class="mediaLink" onclick="return '+(onclickFunctionCall === null ? 'false':(onclickFunctionCall+'(event,'+index+')'))+'"\n' +
                '       data-download-url="'+encodeURI(metadata.videoUrl)+'/download" \n' +
                '       data-video=\'{"source": [{"src":"' + metadata.videoUrl + '", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}\'\n' +
                '       data-sub-html="' + (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                '   </a>\n';
        } else {
            mediaContent.src = metadata.thumbnailUrlOriginal;
            mediaContent.downloadUrl = encodeURI(metadata.thumbnailUrlOriginal);
            html +=
                '   <a class="mediaLink" onclick="return '+(onclickFunctionCall === null ? 'false':(onclickFunctionCall+'(event,'+index+')'))+'" data-src="' + metadata.thumbnailUrlOriginal + '" href="' + metadata.thumbnailUrlOriginal + '"' +
                '       data-download-url="'+encodeURI(metadata.thumbnailUrlOriginal)+'" \n' +
                '       data-sub-html="' + (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                '   </a>\n';
        }

        html +=
            '   </div>\n';

        return {html:html,mediaContent:mediaContent}
    }


    shashin.clearTimelineSelection = function () {
        shashin.removeAllMetadataFilenamesList();
        shashin.removeAllMetadataThumbnailsList();
        shashin.removeAllMetadataIdList();
        $(".thumbnail-centered").css("display", "none");
        //$(".thumbnail-tr").css("display", "none");
        $(".thumbnail-br").css("display", "none");
        $(".thumbnail-bl").css("display", "none");
        $(".thumbnail-tl").css("display", "none");
        $(".photo-thumbnail-image").css("opacity", 1.0);
        $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');

        $("#appSearch").css("display", "block");
        $("#timelineAppTools").css("display", "none");
        $("#albumAppTools").css("display", "none");
        $("#matchesAppTools").css("display", "none");
    }

    shashin.matchingListeners = function () {
        $("#matchToolsDeselectAll").click(function(e) {
            e.preventDefault();

            $(".thumbnail-centered").css("display", "none");
            //$(".thumbnail-tr").css("display", "none");
            $(".thumbnail-br").css("display", "none");
            $(".thumbnail-bl").css("display", "none");
            $(".thumbnail-tl").css("display", "none");
            $(".photo-thumbnail-image").css("opacity", 1.0);
            $(".thumbnail-tl a span").addClass('bi-circle').removeClass('bi-circle-fill');

            $("#appSearch").css("display", "block");
            $("#timelineAppTools").css("display", "none");
            $("#albumAppTools").css("display", "none");
            $("#matchesAppTools").css("display", "none");
        })

        $("#matchesAppTools").css("display", "none");

        $("#matchToolsBatchEdit").click(function(e) {
            e.preventDefault();

            let metadataIdList = [];
            let thumbnailList = "";
            $('.bi-circle-fill').each(function(i, obj) {
                const metadataId = obj.id.substring(6, obj.id.length);
                metadataIdList.push(metadataId);
                thumbnailList += '<img src="'+$("#thumbnailCentered"+metadataId).val()+'" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="'+$("#filename"+metadataId).val().trim()+'" onError="shashin.errorImg(this,\''+$("#filename"+metadataId).val().trim()+'\',75)">';
            });

            $("#batchMetadataIds").val(JSON.stringify(metadataIdList));
            if (thumbnailList !== "") {
                $("#editPhotosNamesModalLabel").html(thumbnailList);
            }
            $("#propBatchMetadata").modal('show');
        });
    }

    // Call in console
    shashin.enableDebug = function () {
        shashin.showDebug = true;
    }

    // Call in console
    shashin.disableDebug = function () {
        shashin.showDebug = false;
    }

    shashin.printMessageToConsole = function (msg) {
        if (shashin.showDebug === true) {
            console.log(msg);
        }
    }

    shashin.errorImg = function (_this,text,defaulWidthtHeight) {
        let dimensions = "/209";
        if (defaulWidthtHeight != null) {
            dimensions = "/"+defaulWidthtHeight;
        }
        if (_this.width != null && _this.width > 0 && _this.height != null && _this.height > 0) {
            dimensions = "/"+_this.width+"x"+_this.height;
        }
        _this.src = "https://via.placeholder.com"+dimensions+"?text="+encodeURI(text);
    }

    shashin.removeDateGallery = function (id) {
        $("#br"+id).remove();
        $("#row"+id).remove();
        $("#amp_"+id).remove();
        $("#tail_"+id).remove();
        $("#"+id).remove();
    }
}( window.shashin = window.shashin || {}, jQuery ));

if (typeof module !== 'undefined') {
    module.exports = window.shashin;
}