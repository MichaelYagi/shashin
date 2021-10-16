// Globals
$.fn.hasScrollBar = function() {
    return this.get(0).scrollHeight > this.get(0).clientHeight;
}

$.fn.serializeObject = function() {
    var o = {};
    var a = this.serializeArray();
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
        var msg = "";
        var textArea = document.createElement("textarea");
        textArea.value = text;

        // Avoid scrolling to bottom
        textArea.style.top = "0";
        textArea.style.left = "0";
        textArea.style.position = "fixed";

        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();

        try {
            var successful = document.execCommand('copy');
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
        var days = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
        return days[index];
    }
    function getShortMonths(index) {
        var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
        return months[index];
    }

    shashin.showDebug = false;

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

    shashin.jumpToLightGalleryIndex = function (index) {
        var url = location.href;
        location.href = '#lightGalleryIndex'+index;
        history.replaceState(null,null,url);
    }

    shashin.getParameterByName = function (name, url = window.location.href) {
        name = name.replace(/[\[\]]/g, '\\$&');
        var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
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

    shashin.getLightGalleryConfigs = function() {
        return {
            plugins: [lgZoom, lgVideo, lgRelativeCaption, lgFullscreen],
            counter: false,
            fullScreen: true,
            download: true,
            zoomFromOrigin: true,
            speed:0,
            licenseKey: "A8E2CC75-7F9D45CA-9CE65C4E-FFF50CE3"
        }
    }

    shashin.getMapSource = function (source) {
        const validSources = ["osm","bingmaps","maptiler","mapbox"];

        var mapSource = new ol.source.OSM();

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
            let shortMonth = getShortMonths(date.getMonth());
            let adjustedDay = date.getDate();
            let dayOfWeek = getShortDay(date.getDay());
            return dayOfWeek + ", " + shortMonth + " " + adjustedDay + ", " + year;
        }
        return "";
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

    shashin.setPhotoOverlays = function (rawMetadata, view) {
        const metadata = JSON.parse(shashin.decodeHtml(rawMetadata));

        let metadataIdArray = shashin.getMetdataIdList();
        shashin.printMessageToConsole(metadataIdArray);
        const index = metadataIdArray.indexOf(metadata.id);
        if (index > -1) {
            $("#tntl" + metadata.id).css("display", "block");
            $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
            $("#image" + metadata.id).css("opacity", 0.3);
            $("#tncentered" + metadata.id).css("display", "none");
            $("#tnbr" + metadata.id).css("display", "none");
            $("#tnbl" + metadata.id).css("display", "none");
        }

        $("#select" + metadata.id).click(function (e) {
            e.preventDefault();

            if ($("#tlicon" + metadata.id).attr("class") === "bi-circle") {
                $("#tntl" + metadata.id).css("display", "block");
                $("#tlicon" + metadata.id).addClass('bi-circle-fill').removeClass('bi-circle');
                $("#image" + metadata.id).css("opacity", 0.3);
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
                $("#image" + metadata.id).css("opacity", 0.3);
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
                $('.thumbnail-br').show();
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
                    $("#image" + metadata.id).css("opacity", 0.3);
                    $("#tncentered" + metadata.id).css("display", "none");
                    $("#tnbr" + metadata.id).css("display", "none");
                    $("#tnbl" + metadata.id).css("display", "none");
                    shashin.addToMetadataIdList(metadata.id);
                    shashin.addToMetadataFilenamesList($('#filename' + metadata.id).val());
                    shashin.addToMetadataThumbnailsList($('#thumbnailCentered' + metadata.id).val());
                } else {
                    $("#tntl" + metadata.id).css("display", "block");
                    $("#tlicon" + metadata.id).addClass('bi-circle').removeClass('bi-circle-fill');
                    $("#image" + metadata.id).css("opacity", 0.3);
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
            $(this).siblings(".photo-thumbnail-image").css("opacity", 0.3);
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
            $(this).siblings(".photo-thumbnail-image").css("opacity", 1.0);
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
                $(this).siblings(".photo-thumbnail-image").css("opacity", 0.3);
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
                $(this).siblings(".photo-thumbnail-image").css("opacity", 1.0);
            } else {
                $(this).siblings(".photo-thumbnail-image").css("opacity", 0.3);
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
            $(this).siblings(".photo-thumbnail-image").css("opacity", 0.3);
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
            $(this).siblings(".photo-thumbnail-image").css("opacity", 1.0);
        });

        $("#tnbr" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            $(this).css("display", "block");
            $(this).siblings(".thumbnail-tl").css("display", "block");
            $(this).siblings(".thumbnail-centered").css("display", "block");
            $(this).siblings(".thumbnail-tr").css("display", "block");
            $(this).siblings(".thumbnail-bl").css("display", "block");
            $(this).siblings(".photo-thumbnail-image").css("opacity", 0.3);
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
            $(this).siblings(".photo-thumbnail-image").css("opacity", 1.0);
        });

        $("#tntr" + metadata.id).hover(function () {
            metadataIdArray = shashin.getMetdataIdList();
            $(this).css("display", "block");
            $(this).siblings(".thumbnail-tl").css("display", "block");
            $(this).siblings(".thumbnail-centered").css("display", "block");
            $(this).siblings(".thumbnail-bl").css("display", "block");
            $(this).siblings(".thumbnail-br").css("display", "block");
            $(this).siblings(".photo-thumbnail-image").css("opacity", 0.3);
            if ($('.bi-circle-fill')[0] || $(this).attr("class") === "bi-circle-fill" || metadataIdArray.length > 0) {
                $('.thumbnail-bl').hide();
                $('.thumbnail-centered').hide();
                //$('.thumbnail-tr').hide();
                $('.thumbnail-br').hide();
            }
        }, function () {
            //$(this).css("display", "none");
            $(this).siblings(".thumbnail-tl").css("display", "none");
            $(this).siblings(".thumbnail-centered").css("display", "none");
            $(this).siblings(".thumbnail-bl").css("display", "none");
            $(this).siblings(".thumbnail-br").css("display", "none");
            $(this).siblings(".photo-thumbnail-image").css("opacity", 1.0);
        });
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
                var metadataId = obj.id.substring(6, obj.id.length);
                metadataIdList.push(metadataId);
                thumbnailList += '<img src="'+$("#thumbnailCentered"+metadataId).val()+'" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="'+$("#filename"+metadataId).val().trim()+'">';
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
}( window.shashin = window.shashin || {}, jQuery ));

if (typeof module !== 'undefined') {
    module.exports = window.shashin;
}