class Dashboard {

    constructor() {
        this.tension = 0.4;
        this.showLabelThreshold = 10;
        this.pieChartFontSize = 13;
    }

    static randomPastelRgba() {
        const rgbaList = [
            ['rgba(159, 179, 223, 0.8)', 'rgba(159, 179, 223, 1)'],
            ['rgba(158, 198, 243, 0.8)', 'rgba(158, 198, 243, 1)'],
            ['rgba(189, 221, 228, 0.8)', 'rgba(189, 221, 228, 1)'],
            ['rgba(255, 241, 213, 0.8)', 'rgba(255, 241, 213, 1)'],
            ['rgba(173, 178, 212, 0.8)', 'rgba(173, 178, 212, 1)'],
            ['rgba(199, 217, 221, 0.8)', 'rgba(199, 217, 221, 1)'],
            ['rgba(213, 229, 213, 0.8)', 'rgba(213, 229, 213, 1)'],
            ['rgba(238, 241, 218, 0.8)', 'rgba(238, 241, 218, 1)'],
            ['rgba(183, 177, 242, 0.8)', 'rgba(183, 177, 242, 1)'],
            ['rgba(253, 183, 234, 0.8)','rgba(253, 183, 234, 1)'],
            ['rgba(255, 220, 204, 0.8)','rgba(255, 220, 204, 1)'],
            ['rgba(251, 243, 185, 0.8)','rgba(251, 243, 185, 1)'],
            ['rgba(247, 207, 216, 0.8)','rgba(247, 207, 216, 1)'],
            ['rgba(244, 248, 211, 0.8)','rgba(244, 248, 211, 1)'],
            ['rgba(166, 214, 214, 0.8)','rgba(166, 214, 214, 1)'],
            ['rgba(142, 125, 190, 0.8)','rgba(142, 125, 190, 1)'],
            ['rgba(92, 114, 133, 0.8)','rgba(92, 114, 133, 1)'],
            ['rgba(226, 224, 200, 0.8)','rgba(226, 224, 200, 1)'],
            ['rgba(191, 236, 255, 0.8)','rgba(191, 236, 255, 1)'],
            ['rgba(255, 246, 227, 0.8)','rgba(255, 246, 227, 1)'],
            ['rgba(205, 193, 255, 0.8)','rgba(205, 193, 255, 1)'],
            ['rgba(255, 204, 234, 0.8)','rgba(255, 204, 234, 1)'],
            ['rgba(120, 157, 188, 0.8)','rgba(120, 157, 188, 1)'],
            ['rgba(255, 227, 227, 0.8)','rgba(255, 227, 227, 1)'],
            ['rgba(254, 249, 242, 0.8)','rgba(254, 249, 242, 1)'],
            ['rgba(201, 233, 210, 0.8)','rgba(201, 233, 210, 1)']
        ];

        const randNum = Dashboard.getRandomInt(0,(rgbaList.length-1));

        return rgbaList[randNum];
    }

    static getRandomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1) + min);
    }

    static urlWithRndQueryParam(url, paramName) {
        const ulrArr = url.split('#');
        const urlQry = ulrArr[0].split('?');
        const usp = new URLSearchParams(urlQry[1] || '');
        usp.set(paramName || '_z', `${Date.now()}`);
        urlQry[1] = usp.toString();
        ulrArr[0] = urlQry.join('?');
        return ulrArr.join('#');
    }

    static async handleHardReload(url) {
        const newUrl = Dashboard.urlWithRndQueryParam(url);
        await fetch(newUrl, {
            headers: {
                Pragma: 'no-cache',
                Expires: '-1',
                'Cache-Control': 'no-cache',
            },
        });
        window.location.href = url;
        // This is to ensure reload with url's having '#'
        window.location.reload();
    }

    displaySiteStatChart(data) {
        const ctx = $('#siteStatChart');
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: [shashin.getTranslatedValue("main.pages.dashboard.peopletagged"), shashin.getTranslatedValue("main.pages.dashboard.favorites"), shashin.getTranslatedValue("main.pages.dashboard.comments"), shashin.getTranslatedValue("main.pages.dashboard.albums")],
                datasets: [{
                    data: [data.photosWithPeopleTaggedCount, data.favoritesCount, data.commentsCount, data.albumCount],
                    backgroundColor: [
                        'rgba(54, 162, 235, 0.2)',
                        'rgba(255, 206, 86, 0.2)',
                        'rgba(153, 102, 255, 0.2)',
                        'rgba(255, 159, 64, 0.2)'
                    ],
                    borderColor: [
                        'rgba(54, 162, 235, 1)',
                        'rgba(255, 206, 86, 1)',
                        'rgba(153, 102, 255, 1)',
                        'rgba(255, 159, 64, 1)'
                    ],
                    borderWidth: 1
                }]
            },
            options: {
                scales: {
                    y: {
                        beginAtZero: true
                    }
                },
                plugins: {
                    legend: false
                },
                maintainAspectRatio: false
            }
        });
    }

    displayMediaChart(data) {
        const ctx = $('#mediaChart');
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: [shashin.getTranslatedValue("main.pages.dashboard.totalphotos"), shashin.getTranslatedValue("main.pages.dashboard.totalvideos"), shashin.getTranslatedValue("main.pages.dashboard.missingcoords"), shashin.getTranslatedValue("main.pages.dashboard.totalarchived")],
                datasets: [{
                    data: [data.photoCount, data.videoCount, data.notLocatedCount, data.hiddenCount],
                    backgroundColor: [
                        'rgba(54, 162, 235, 0.2)',
                        'rgba(255, 206, 86, 0.2)',
                        'rgba(153, 102, 255, 0.2)',
                        'rgba(255, 159, 64, 0.2)'
                    ],
                    borderColor: [
                        'rgba(54, 162, 235, 1)',
                        'rgba(255, 206, 86, 1)',
                        'rgba(153, 102, 255, 1)',
                        'rgba(255, 159, 64, 1)'
                    ],
                    borderWidth: 1
                }]
            },
            options: {
                scales: {
                    y: {
                        beginAtZero: true
                    }
                },
                plugins: {
                    legend: false
                },
                maintainAspectRatio: false
            }
        });
    }

    displayCameraChart(data) {
        const ctx = $('#cameraChart');
        const cameraCountObj = JSON.parse(data.cameraCountJson);
        const cameraChart = new Chart(ctx, {
            type: 'bar',
            data: {
                datasets: [{
                    data: cameraCountObj,
                    backgroundColor: [
                        'rgba(54, 162, 235, 1)'
                    ]
                }]
            },
            options: {
                indexAxis: 'y',
                scales: {
                    y: {
                        ticks: {
                            // Truncate ticks
                            callback: function (index) {
                                let labelValue = cameraCountObj[index].y;
                                if (labelValue.length > 9) {
                                    labelValue = labelValue.substring(0, 8) + "...";
                                }
                                return labelValue;
                            },
                            maxRotation: 0,
                            minRotation: 0
                        }
                    },
                    x: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 20
                        }
                    }
                },
                plugins: {
                    legend: false
                },
                maintainAspectRatio: false
            }
        });

        // Click label
        $('#cameraChart').on('click', function (evt) {
            let point = Chart.helpers.getRelativePosition(evt, cameraChart);
            let datasetIndex = cameraChart.scales.y.getValueForPixel(point.y);
            if (datasetIndex >= 0) {
                let label = cameraCountObj[datasetIndex].y;
                if (typeof label !== "undefined" && label !== "" && label !== "Unknown") {
                    window.open("/search?term=" + encodeURI(label.split(' ').join('+')).replace(";", "%3B"), '_blank').focus();
                }
            }
        });
        $('#cameraChart').on('mouseenter', function (e) {
            e.target.style.cursor = 'pointer';
        });
        $('#cameraChart').on('mouseleave', function (e) {
            e.target.style.cursor = 'default';
        });
    }

    displayPlacenameChart(data) {
        const ctx = $('#placenameChart');
        const placenameCountObj = JSON.parse(data.placenameCountJson);
        const placenameChart = new Chart(ctx, {
            type: 'bar',
            data: {
                datasets: [{
                    data: placenameCountObj,
                    backgroundColor: [
                        'rgba(54, 162, 235, 1)'
                    ]
                }]
            },
            options: {
                indexAxis: 'y',
                scales: {
                    y: {
                        ticks: {
                            // Truncate ticks
                            callback: function (index) {
                                let labelValue = placenameCountObj[index].y;
                                if (labelValue.length > 24) {
                                    labelValue = labelValue.substring(0, 25) + "...";
                                }
                                return labelValue;
                            },
                            maxRotation: 0,
                            minRotation: 0
                        }
                    },
                    x: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 20
                        }
                    }
                },
                plugins: {
                    legend: false
                },
                maintainAspectRatio: false
            }
        });

        // Click label
        $('#placenameChart').on('click', function (evt) {
            let point = Chart.helpers.getRelativePosition(evt, placenameChart);
            let datasetIndex = placenameChart.scales.y.getValueForPixel(point.y);
            if (datasetIndex >= 0) {
                let label = placenameCountObj[datasetIndex].y;
                if (typeof label !== "undefined" && label !== "" && label !== "Unknown") {
                    window.open("/search?term=" + encodeURI(label.split(' ').join('+')).replace(";", "%3B"), '_blank').focus();
                }
            }
        });
        $('#placenameChart').on('mouseenter', function (e) {
            e.target.style.cursor = 'pointer';
        });
        $('#placenameChart').on('mouseleave', function (e) {
            e.target.style.cursor = 'default';
        });
    }

    displayUserChart(data) {
        const ctx = $('#userChart');

        let counts = [];
        let labels = [];

        if (data.activeUserCount > 0) {
            counts.push(data.activeUserCount);
            labels.push(shashin.getTranslatedValue("main.pages.dashboard.authorusers"));
        }
        if (data.activeAdminCount > 0) {
            counts.push(data.activeAdminCount);
            labels.push(shashin.getTranslatedValue("main.pages.dashboard.authoradmins"));
        }
        if (data.activeSuperCount > 0) {
            counts.push(data.activeSuperCount);
            labels.push(shashin.getTranslatedValue("main.pages.dashboard.authorsuperadmins"));
        }
        if (data.pendingUserCount > 0) {
            counts.push(data.pendingUserCount);
            labels.push(shashin.getTranslatedValue("main.pages.dashboard.unauthorusers"));
        }
        if (data.pendingAdminCount > 0) {
            counts.push(data.pendingAdminCount);
            labels.push(shashin.getTranslatedValue("main.pages.dashboard.unauthoradmins"));
        }
        if (data.pendingSuperCount > 0) {
            counts.push(data.pendingSuperCount);
            labels.push(shashin.getTranslatedValue("main.pages.dashboard.unauthorsuperadmins"));
        }

        const userColors = [];
        const userBorders = [];

        for (let i = 0; i < counts.length; i++) {
            const rgb = Dashboard.randomPastelRgba();
            userColors.push(rgb[0]);
            userBorders.push(rgb[1]);
        }

        new Chart(ctx, {
            type: 'doughnut',
            plugins: [ChartDataLabels],
            data: {
                labels: labels,
                datasets: [{
                    data: counts,
                    backgroundColor: userColors,
                    borderColor: userBorders,
                    borderWidth: 1
                }]
            },
            options: {
                plugins: {
                    legend: {
                        display: false,
                        position: "left",
                        align: "end"
                    },
                    title: {
                        display: true,
                        text: shashin.getTranslatedValue("main.pages.dashboard.userroles"),
                        font: {
                            size: 14,
                            weight: 'bold'
                        }
                    },
                    datalabels: {
                        textAlign: 'center',
                        clamp: true,
                        display: 'auto',
                        textShadowBlur: 5,
                        textShadowColor: "white",
                        font: {
                            size: this.pieChartFontSize,
                            weight: 'bold'
                        },
                        formatter: (value, ctx) => {
                            let sum = 0;

                            let userType = ctx.chart.data.labels[ctx.dataIndex];

                            let dataArr = ctx.chart.data.datasets[0].data;
                            dataArr.map(data => {
                                sum += data;
                            });

                            let percentage = Math.trunc(value*100 / sum);
                            if (percentage < this.showLabelThreshold) {
                                return '';
                            }

                            return percentage + '%\n' + userType;
                        }
                    }
                },
                maintainAspectRatio: false
            }
        });
    }

    displayAgentNameChart(data) {
        const ctx = $('#browserChart');
        const agentNameCountObj = JSON.parse(data.agentNameCountJson);
        const agentNameCounts = [];
        const agentNames = [];
        const agentNameColors = [];
        const agentNameBorders = [];

        for (let i = 0; i < agentNameCountObj.length; i++) {
            const rgb = Dashboard.randomPastelRgba();
            const agentNameObj = agentNameCountObj[i];
            agentNameCounts.push(agentNameObj.x);
            agentNames.push(agentNameObj.y);
            agentNameColors.push(rgb[0]);
            agentNameBorders.push(rgb[1]);
        }

        new Chart(ctx, {
            type: 'doughnut',
            plugins: [ChartDataLabels],
            data: {
                labels: agentNames,
                datasets: [{
                    data: agentNameCounts,
                    backgroundColor: agentNameColors,
                    borderColor: agentNameBorders,
                    hoverOffset: 4
                }]
            },
            options: {
                plugins: {
                    legend: {
                        display: false,
                        position: "left",
                        align: "end"
                    },
                    title: {
                        display: true,
                        text: shashin.getTranslatedValue("main.pages.dashboard.browserrequest"),
                        font: {
                            size: 14,
                            weight: 'bold'
                        }
                    },
                    datalabels: {
                        textAlign: 'center',
                        clamp: true,
                        display: 'auto',
                        textShadowBlur: 5,
                        textShadowColor: "white",
                        font: {
                            size: this.pieChartFontSize,
                            weight: 'bold'
                        },
                        formatter: (value, ctx) => {
                            let sum = 0;
                            let dataArr = ctx.chart.data.datasets[0].data;
                            dataArr.map(data => {
                                sum += data;
                            });
                            let percentage = Math.trunc(value*100 / sum);
                            if (percentage < this.showLabelThreshold) {
                                return '';
                            }
                            return percentage + '%\n' + ctx.chart.data.labels[ctx.dataIndex];
                        }
                    }
                },
                maintainAspectRatio: false
            }
        });
    }

    displayOsNameChart(data) {
        const ctx = $('#osChart');
        const osNameCountObj = JSON.parse(data.osNameCountJson);
        const osNameCounts = [];
        const osNames = [];
        const osNameColors = [];
        const osNameBorders = [];

        for (let i = 0; i < osNameCountObj.length; i++) {
            const rgb = Dashboard.randomPastelRgba();
            const osNameObj = osNameCountObj[i];
            osNameCounts.push(osNameObj.x);
            osNames.push(osNameObj.y);
            osNameColors.push(rgb[0]);
            osNameBorders.push(rgb[1]);
        }

        new Chart(ctx, {
            type: 'doughnut',
            plugins: [ChartDataLabels],
            data: {
                labels: osNames,
                datasets: [{
                    data: osNameCounts,
                    backgroundColor: osNameColors,
                    borderColor: osNameBorders,
                    hoverOffset: 4
                }]
            },
            options: {
                plugins: {
                    legend: {
                        display: false,
                        position: "left",
                        align: "end"
                    },
                    title: {
                        display: true,
                        text: shashin.getTranslatedValue("main.pages.dashboard.osrequest"),
                        font: {
                            size: 14,
                            weight: 'bold'
                        }
                    },
                    datalabels: {
                        textAlign: 'center',
                        clamp: true,
                        display: 'auto',
                        textShadowBlur: 5,
                        textShadowColor: "white",
                        font: {
                            size: this.pieChartFontSize,
                            weight: 'bold'
                        },
                        formatter: (value, ctx) => {
                            let sum = 0;
                            let dataArr = ctx.chart.data.datasets[0].data;
                            dataArr.map(data => {
                                sum += data;
                            });
                            let percentage = Math.trunc(value*100 / sum);
                            if (percentage < this.showLabelThreshold) {
                                return '';
                            }
                            return percentage + '%\n' + ctx.chart.data.labels[ctx.dataIndex];
                        }
                    }
                },
                maintainAspectRatio: false
            }
        });
    }

    displayKeywordChart(data) {
        const ctx = $('#keywordChart');
        const keywordCounts = JSON.parse(Util.decodeHtml(data.keywordCountJson));
        const keywordCountObj = JSON.parse(data.keywordCountJson);

        const keywordChart = new Chart(ctx, {
            type: 'bar',
            data: {
                datasets: [{
                    data: keywordCounts,
                    backgroundColor: [
                        'rgba(54, 162, 235, 1)'
                    ]
                }]
            },
            options: {
                indexAxis: 'y',
                scales: {
                    y: {
                        ticks: {
                            // Truncate ticks
                            callback: function (index) {
                                let labelValue = keywordCountObj[index].y;
                                if (labelValue.length > 9) {
                                    labelValue = labelValue.substring(0, 8) + "...";
                                }
                                return labelValue;
                            },
                            maxRotation: 0,
                            minRotation: 0
                        }
                    },
                    x: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 20
                        }
                    }
                },
                plugins: {
                    legend: false
                },
                maintainAspectRatio: false
            }
        });

        const openLinkFromLabel = function (e) {
            let point = Chart.helpers.getRelativePosition(e, keywordChart);
            let datasetIndex = keywordChart.scales.y.getValueForPixel(point.y);
            if (datasetIndex >= 0) {
                let label = keywordCountObj[datasetIndex].y;
                if (typeof label !== "undefined" && label !== "" && label !== "Unknown") {
                    window.open("/search?term=" + encodeURI(label.split(' ').join('+')).replace(";", "%3B"), '_blank').focus();
                }
            }
        };

        // Click label
        $('#keywordChart').on('click', openLinkFromLabel);
        $('#keywordChart').on('mouseenter', function (e) {
            e.target.style.cursor = 'pointer';
        });
        $('#keywordChart').on('mouseleave', function (e) {
            e.target.style.cursor = 'default';
        });
    }

    displayCpuChart() {
        const ctx = $('#cpuChart');
        return new Chart(ctx, {
            type: 'line',
            labels: [],
            data: {
                datasets: [
                    {
                        label: shashin.getTranslatedValue("main.pages.dashboard.jvmcpu"),
                        data: [],
                        fill: false,
                        borderColor: 'rgb(54, 162, 235)',
                        tension: this.tension //0.5 for curved
                    },
                    {
                        label: shashin.getTranslatedValue("main.pages.dashboard.systemcpu"),
                        data: [],
                        fill: false,
                        borderColor: 'rgb(255, 206, 86)',
                        tension: this.tension
                    }
                ]
            },
            options: {
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100
                    }
                },
                plugins: {
                    legend: true
                },
                maintainAspectRatio: false
            }
        });
    }

    displayMemoryChart() {
        const ctx = $('#memoryChart');
        return new Chart(ctx, {
            type: 'line',
            labels: [],
            data: {
                datasets: [
                    {
                        label: shashin.getTranslatedValue("main.pages.dashboard.memoryused"),
                        data: [],
                        fill: false,
                        borderColor: 'rgb(54, 162, 235)',
                        tension: this.tension
                    }
                ]
            },
            options: {
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100
                    }
                },
                plugins: {
                    legend: true
                },
                maintainAspectRatio: false
            }
        });
    }

    static addData(chart, label, data, index) {
        if (label !== null) {
            chart.data.labels.push(label);
        }
        chart.data.datasets[index].data.push(data);
        chart.update();
    }

    static removeData(chart) {
        chart.data.labels.shift();
        chart.data.datasets.forEach((dataset) => {
            dataset.data.shift();
        });
        chart.update();
    }

    startWebSocket(cpuChart, memoryChart) {
        let stompClient = null;
        // Socket polling refresh rate
        const refreshRateMS = 1000*5;
        // Update chart every 10 seconds
        const intervalMS = 1000*10;
        // Pop values after 3 minutes
        const maxElapsedMS = 1000*60*3;
        let elapsedMS = 0;

        connect();

        function scanRefresh() {
            sendMessage();
            elapsedMS += refreshRateMS;
            setTimeout(scanRefresh, refreshRateMS);
            $('#cpuChart').load(document.URL + ' #cpuChart');
            $('#memoryChart').load(document.URL + ' #memoryChart');
        }

        let counter = 0;
        function connect() {
            const socket = new SockJS('/websocket-endpoint');
            stompClient = Stomp.over(socket);
            if (shashin.showDebug === false) {
                stompClient.debug = null;
            }

            shashin.printMessageToConsole("Socket Connecting",{tag:"dashboard"});

            stompClient.connect({}, function() {
                scanRefresh();
                shashin.printMessageToConsole( "Connected STOMP client",{tag:"dashboard"});

                let invalidSystemCpuLoadCounter = 0;
                let invalidProcessCpuLoadCounter = 0;

                this.subscribe("/topic/statmessages", function (message) {
                    let respMessageJsonString = JSON.parse(message.body).content;
                    const systemStats = JSON.parse(respMessageJsonString);

                    invalidSystemCpuLoadCounter = systemStats.invalidSystemCpuLoadCounter;
                    invalidProcessCpuLoadCounter = systemStats.invalidProcessCpuLoadCounter;

                    shashin.printMessageToConsole("invalidSystemCpuLoadCounter: "+invalidSystemCpuLoadCounter,{tag:"dashboard"});
                    shashin.printMessageToConsole("invalidProcessCpuLoadCounter: "+invalidProcessCpuLoadCounter,{tag:"dashboard"});

                    // Reload page if NaN, 0 or greater than 1
                    if (invalidSystemCpuLoadCounter > 5 || invalidProcessCpuLoadCounter > 5) {
                        shashin.printMessageToConsole("Crossed threshold for invalid system stat counter values. Reloading page.",{tag:"dashboard"});
                        //Dashboard.handleHardReload(window.location.href).then(null);
                        disconnect();
                    }

                    const processCpuLoadPercent = Math.ceil(systemStats.processCpuLoadPercentDouble*100)|0;
                    const processCpuLoadData = ~~processCpuLoadPercent;
                    shashin.printMessageToConsole("processCpuLoadData: "+processCpuLoadData,{tag:"dashboard"});

                    const systemCpuLoadPercent = Math.ceil(systemStats.systemCpuLoadPercentDouble*100)|0;
                    const systemCpuLoadData = ~~systemCpuLoadPercent;
                    shashin.printMessageToConsole("systemCpuLoadData: "+systemCpuLoadData,{tag:"dashboard"});

                    const memoryUsedPercent = Math.ceil(systemStats.usedHeapMemoryGB/systemStats.maxHeapMemoryGB*100)|0;
                    const memoryUsedData = ~~memoryUsedPercent;
                    shashin.printMessageToConsole("usedHeapMemoryGB: "+systemStats.usedHeapMemoryGB,{tag:"dashboard"});
                    shashin.printMessageToConsole("maxHeapMemoryGB: "+systemStats.maxHeapMemoryGB,{tag:"dashboard"});
                    shashin.printMessageToConsole("memoryUsedData: "+memoryUsedData,{tag:"dashboard"});

                    if (elapsedMS >= maxElapsedMS && elapsedMS % intervalMS === 0) {
                        Dashboard.removeData(cpuChart);
                        Dashboard.removeData(memoryChart);
                    }
                    if (elapsedMS === refreshRateMS || elapsedMS % intervalMS === 0) {
                        Dashboard.addData(cpuChart, systemStats.timestamp, processCpuLoadData, 0);
                        Dashboard.addData(cpuChart, null, systemCpuLoadData, 1);
                        Dashboard.addData(memoryChart, systemStats.timestamp, memoryUsedData, 0);
                    }
                    shashin.printMessageToConsole("Message:"+respMessageJsonString,{tag:"dashboard"});
                });
            }, function(e) {
                if (counter > 10) {
                    shashin.printMessageToConsole("Oops, something went wrong! " + e.toString() + ". Probably already scanning.", {
                        consoleType: shashin.consoleTypes.error,
                        tag: "lightgallery"
                    });
                    counter = 0;
                    window.top.location = window.top.location;
                } else {
                    shashin.printMessageToConsole("Oops, something went wrong! " + e.toString() + ".", {
                        consoleType: shashin.consoleTypes.error,
                        tag: "lightgallery"
                    });
                    disconnect();
                    connect();
                }

                counter++;
            });
        }

        function disconnect() {
            if (stompClient !== null) {
                stompClient.disconnect();
            }
            shashin.printMessageToConsole("Disconnected",{tag:"album"});
        }

        function sendMessage() {
            if (stompClient !== null) {
                stompClient.send("/app/statmessage", {}, JSON.stringify({'message': "getStatMessage"}));
            } else {
                shashin.printMessageToConsole("Trying to send message but STOMP client is null", {
                    consoleType: shashin.consoleTypes.error
                });
            }
        }
    }
}
