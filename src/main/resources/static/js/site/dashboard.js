class Dashboard {

    constructor() {
        this.tension = 0.4;
    }

    static randomPastelHsla(hue) {
        if (hue < 1) {
            hue = 1;
        }

        // const saturation = Math.floor(Dashboard.getRandomInt(60,70));
        // const lightness = Math.floor(Dashboard.getRandomInt(62,72));
        const saturation = 70;
        const lightness = 72;
        hue = Math.floor(hue);

        return [
            'hsla('+hue+', '+saturation+'%, '+lightness+'%, 0.8)',
            'hsla('+hue+', '+saturation+'%, '+lightness+'%, 1)'
        ];
    }

    static getRandomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1) + min);
    }

    displaySiteStatChart(data) {
        const ctx = $('#siteStatChart');
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Photos With People Tagged', 'Total Favorites', 'Total Comments', 'Total Albums'],
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
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
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
                labels: ['Photos', 'Videos', 'No GPS Data', 'Removed'],
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
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
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
                        'rgba(54, 162, 235, 0.2)'
                    ],
                    borderColor: [
                        'rgba(54, 162, 235, 1)'
                    ],
                    borderWidth: 1
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
                                    labelValue = labelValue.substr(0, 8) + "...";
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
                            stepSize: 1
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

    displayUserChart(data) {
        const ctx = $('#userChart');

        let counts = [];
        let labels = [];

        if (data.activeUserCount > 0) {
            counts.push(data.activeUserCount);
            labels.push("Authorized Users");
        }
        if (data.activeAdminCount > 0) {
            counts.push(data.activeAdminCount);
            labels.push("Authorized Admins");
        }
        if (data.activeSuperCount > 0) {
            counts.push(data.activeSuperCount);
            labels.push("Authorized Super Admins");
        }
        if (data.pendingUserCount > 0) {
            counts.push(data.pendingUserCount);
            labels.push("Unauthorized Users");
        }
        if (data.pendingAdminCount > 0) {
            counts.push(data.pendingAdminCount);
            labels.push("Unauthorized Admins");
        }
        if (data.pendingSuperCount > 0) {
            counts.push(data.pendingSuperCount);
            labels.push("Unauthorized Super Admins");
        }

        const userColors = [];
        const userBorders = [];

        for (let i = 0; i < counts.length; i++) {
            const hsl = Dashboard.randomPastelHsla(Dashboard.getRandomInt(1,360));
            userColors.push(hsl[0]);
            userBorders.push(hsl[1]);
        }

        new Chart(ctx, {
            type: 'doughnut',
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
                        display: true,
                        position: "left",
                        align: "end"
                    },
                    title: {
                        display: true,
                        text: 'Users'
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
            const hsl = Dashboard.randomPastelHsla(Dashboard.getRandomInt(1,360));
            const agentNameObj = agentNameCountObj[i];
            agentNameCounts.push(agentNameObj.x);
            agentNames.push(agentNameObj.y);
            agentNameColors.push(hsl[0]);
            agentNameBorders.push(hsl[1]);
        }

        new Chart(ctx, {
            type: 'doughnut',
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
                        display: true,
                        position: "left",
                        align: "end"
                    },
                    title: {
                        display: true,
                        text: 'Browsers'
                    }
                },
                layout: {
                    padding: 20
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
            const hsl = Dashboard.randomPastelHsla(Dashboard.getRandomInt(1,360));
            const osNameObj = osNameCountObj[i];
            osNameCounts.push(osNameObj.x);
            osNames.push(osNameObj.y);
            osNameColors.push(hsl[0]);
            osNameBorders.push(hsl[1]);
        }

        new Chart(ctx, {
            type: 'doughnut',
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
                        display: true,
                        position: "left",
                        align: "end"
                    },
                    title: {
                        display: true,
                        text: 'OS'
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
                        'rgba(54, 162, 235, 0.2)'
                    ],
                    borderColor: [
                        'rgba(54, 162, 235, 1)'
                    ],
                    borderWidth: 1
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
                                    labelValue = labelValue.substr(0, 8) + "...";
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
                            stepSize: 1
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
                        label: 'JVM CPU %',
                        data: [],
                        fill: false,
                        borderColor: 'rgb(54, 162, 235)',
                        tension: this.tension //0.5 for curved
                    },
                    {
                        label: 'System CPU %',
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
                        ticks: {
                            stepSize: 20
                        },
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
                        label: 'Memory Used %',
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
                        ticks: {
                            stepSize: 20
                        },
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
        }

        let counter = 0;
        function connect() {
            const socket = new SockJS('/websocket-endpoint');
            stompClient = Stomp.over(socket);
            if (shashin.showDebug === false) {
                stompClient.debug = null;
            }

            shashin.printMessageToConsole("Socket Connecting");

            stompClient.connect({}, function() {
                scanRefresh();
                shashin.printMessageToConsole( "Connected STOMP client");

                this.subscribe("/topic/statmessages", function (message) {
                    let respMessageJsonString = JSON.parse(message.body).content;
                    const systemStats = JSON.parse(respMessageJsonString);

                    const processCpuLoadPercent = Math.ceil(systemStats.processCpuLoadPercentDouble*100)|0;
                    const processCpuLoadData = ~~processCpuLoadPercent;
                    shashin.printMessageToConsole("processCpuLoadData: "+processCpuLoadData);

                    const systemCpuLoadPercent = Math.ceil(systemStats.systemCpuLoadPercentDouble*100)|0;
                    const systemCpuLoadData = ~~systemCpuLoadPercent;
                    shashin.printMessageToConsole("systemCpuLoadData: "+systemCpuLoadData);

                    const memoryUsedPercent = Math.ceil(systemStats.usedHeapMemoryGB/systemStats.maxHeapMemoryGB*100)|0;
                    const memoryUsedData = ~~memoryUsedPercent;
                    shashin.printMessageToConsole("usedHeapMemoryGB: "+systemStats.usedHeapMemoryGB);
                    shashin.printMessageToConsole("maxHeapMemoryGB: "+systemStats.maxHeapMemoryGB);
                    shashin.printMessageToConsole("memoryUsedData: "+memoryUsedData);

                    if (elapsedMS >= maxElapsedMS && elapsedMS % intervalMS === 0) {
                        Dashboard.removeData(cpuChart);
                        Dashboard.removeData(memoryChart);
                    }
                    if (elapsedMS === refreshRateMS || elapsedMS % intervalMS === 0) {
                        Dashboard.addData(cpuChart, systemStats.timestamp, processCpuLoadData, 0);
                        Dashboard.addData(cpuChart, null, systemCpuLoadData, 1);
                        Dashboard.addData(memoryChart, systemStats.timestamp, memoryUsedData, 0);
                    }
                    shashin.printMessageToConsole("Message:"+respMessageJsonString);
                });
            }, function(e) {
                if (counter > 10) {
                    shashin.printMessageToConsole("Oops, something went wrong! " + e.toString() + ". Probably already scanning.", {
                        consoleType: shashin.consoleTypes.error
                    });
                    counter = 0;
                    window.top.location = window.top.location;
                } else {
                    shashin.printMessageToConsole("Oops, something went wrong! " + e.toString() + ".", {
                        consoleType: shashin.consoleTypes.error
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
            shashin.printMessageToConsole("Disconnected");
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
