class Dashboard {
    displaySiteStatChart(data) {
        const ctx = $('#siteStatChart');
        return new Chart(ctx, {
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

    displayUserChart(data) {
        const ctx = $('#userChart');
        return new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Registered Users', 'Registered Admins', 'Pending Users', 'Pending Admins'],
                datasets: [{
                    data: [data.activeUserCount, data.activeAdminCount, data.pendingUserCount, data.pendingAdminCount],
                    backgroundColor: [
                        'rgba(54, 162, 235, 0.2)',
                        'rgba(255, 206, 86, 0.2)',
                        'rgba(54, 162, 235, 0.2)',
                        'rgba(255, 206, 86, 0.2)',
                        'rgba(153, 102, 255, 0.2)'
                    ],
                    borderColor: [
                        'rgba(54, 162, 235, 1)',
                        'rgba(255, 206, 86, 1)',
                        'rgba(54, 162, 235, 1)',
                        'rgba(255, 206, 86, 1)',
                        'rgba(153, 102, 255, 1)'
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
        return new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Photos', 'Videos', 'Missing GPS Data', 'Removed'],
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

        // Click label
        document.getElementById("cameraChart").onclick = function (evt) {
            let point = Chart.helpers.getRelativePosition(event, cameraChart);
            let datasetIndex = cameraChart.scales.y.getValueForPixel(point.y);
            if (datasetIndex >= 0) {
                let label = cameraCountObj[datasetIndex].y;
                if (typeof label !== "undefined" && label !== "" && label !== "Unknown") {
                    window.open("/search?searchTerm=" + encodeURI(label.split(' ').join('+')), '_blank').focus();
                }
            }
        };

        return new Chart(ctx, {
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
    }

    displayKeywordChart(data) {
        const ctx = $('#keywordChart');
        const keywordCounts = JSON.parse(Util.decodeHtml(data.keywordCountJson));
        const keywordCountObj = JSON.parse(data.keywordCountJson);

        const openLinkFromLabel = function () {
            let point = Chart.helpers.getRelativePosition(event, keywordChart);
            let datasetIndex = keywordChart.scales.y.getValueForPixel(point.y);
            if (datasetIndex >= 0) {
                let label = keywordCountObj[datasetIndex].y;
                if (typeof label !== "undefined" && label !== "" && label !== "Unknown") {
                    window.open("/search?searchTerm=" + encodeURI(label.split(' ').join('+')), '_blank').focus();
                }
            }
        }

        // Click label
        $('#keywordChart').on('click', openLinkFromLabel);

        return new Chart(ctx, {
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
                        tension: 0 //0.5 for curved
                    },
                    {
                        label: 'System CPU %',
                        data: [],
                        fill: false,
                        borderColor: 'rgb(255, 206, 86)',
                        tension: 0
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
                        tension: 0
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
            elapsedMS += refreshRateMS
            setTimeout(scanRefresh, refreshRateMS);
        }

        let counter = 0;
        function connect() {
            const socket = new SockJS('/websocket-endpoint');
            stompClient = Stomp.over(socket);
            if (shashin.showDebug === false) {
                stompClient.debug = null
            }

            shashin.printMessageToConsole("Socket Connecting");

            stompClient.connect({}, function() {
                scanRefresh();
                shashin.printMessageToConsole( "Connected STOMP client");

                this.subscribe("/topic/statmessages", function (message) {
                    let respMessageJsonString = JSON.parse(message.body).content;
                    const systemStats = JSON.parse(respMessageJsonString);

                    const processCpuLoadPercent = Math.ceil(systemStats["processCpuLoadPercentDouble"]*100)|0;
                    const processCpuLoadData = ~~processCpuLoadPercent;
                    shashin.printMessageToConsole("processCpuLoadData: "+processCpuLoadData);

                    const systemCpuLoadPercent = Math.ceil(systemStats["systemCpuLoadPercentDouble"]*100)|0;
                    const systemCpuLoadData = ~~systemCpuLoadPercent
                    shashin.printMessageToConsole("systemCpuLoadData: "+systemCpuLoadData);

                    const memoryUsedPercent = Math.ceil(systemStats["usedHeapMemoryGB"]/systemStats["maxHeapMemoryGB"]*100)|0;
                    const memoryUsedData = ~~memoryUsedPercent
                    shashin.printMessageToConsole("usedHeapMemoryGB: "+systemStats["usedHeapMemoryGB"]);
                    shashin.printMessageToConsole("maxHeapMemoryGB: "+systemStats["maxHeapMemoryGB"]);
                    shashin.printMessageToConsole("memoryUsedData: "+memoryUsedData);

                    if (elapsedMS >= maxElapsedMS && elapsedMS % intervalMS === 0) {
                        Dashboard.removeData(cpuChart);
                        Dashboard.removeData(memoryChart);
                    }
                    if (elapsedMS === refreshRateMS || elapsedMS % intervalMS === 0) {
                        Dashboard.addData(cpuChart, systemStats["timestamp"], processCpuLoadData, 0)
                        Dashboard.addData(cpuChart, null, systemCpuLoadData, 1)
                        Dashboard.addData(memoryChart, systemStats["timestamp"], memoryUsedData, 0)
                    }
                    shashin.printMessageToConsole("Message:"+respMessageJsonString);
                });
            }, function(e) {
                if (counter > 0) {
                    shashin.printMessageToConsole("Oops, something went wrong! " + e.toString() + ". Probably already scanning.");
                } else {
                    shashin.printMessageToConsole("Oops, something went wrong! " + e.toString() + ". Click the Scan button once, to proceed with indexing.");
                }
                if (counter < 10) {
                    counter = 0;
                    scanRefresh();
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
                shashin.printMessageToConsole("Trying to send message but STOMP client is null")
            }
        }
    }
}
