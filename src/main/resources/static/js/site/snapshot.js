class Snapshot {

    constructor() {
        this.tokenName = "ShashinSnapshotName";
        this.tokenSize = "ShashinSnapshotSize";
        this.tokenDbBackupName = "ShashinDbBackupName";
        this.configuredAttempts = 120;
    }

    init() {
        const fileChooser = $("#snapshotFile");

        if (fileChooser.val() === "") {
            $("#import").addClass('disabled');
        }

        fileChooser.on("change", function(){
            if (fileChooser.val() === "") {
                $("#import").addClass('disabled');
            } else {
                $("#import").removeClass('disabled');
            }
        });

        $("#import").on("click", function() {
            $("#msg").text("Importing metadata.");
        });

        const tokenName = this.tokenName;
        const tokenSize = this.tokenSize;
        const tokenDbBackupName = this.tokenDbBackupName;
        let downloadTimer;

        $("#export").on("click", function() {
            let attempts = this.configuredAttempts;

            $("#msg").text("Exporting metadata.");
            setTimeout(function () { $("#export").prop("disabled", true); }, 0);

            Util.setCookie(tokenName, "", "/settings/snapshot");
            Util.setCookie(tokenSize, "", "/settings/snapshot");
            Util.setCookie(tokenDbBackupName, "", "/settings/snapshot");

            downloadTimer = window.setInterval( function() {
                const tokenCookieValue = Util.getCookie(tokenName);
                const tokenCookieSize = Util.getCookie(tokenSize);
                const tokenCookieDbBackupName = Util.getCookie(tokenDbBackupName);

                if ((tokenCookieValue !== "" && tokenCookieSize !== "" && tokenCookieDbBackupName !== "") || attempts === 0) {
                    if (attempts === 0) {
                        $("#msg").html("&nbsp;");
                    } else {
                        const dbBackupNameString = tokenCookieDbBackupName === "" ? "Error encountered":tokenCookieDbBackupName;
                        $("#msg").text("Database backup name: " + dbBackupNameString + ". File name: " + tokenCookieValue + ". File size: " + Util.formatBytes(tokenCookieSize) + ".");
                        $("#export").prop("disabled", false);
                        Util.deleteCookie(tokenName, "/settings/snapshot");
                        Util.deleteCookie(tokenSize, "/settings/snapshot");
                        Util.deleteCookie(tokenDbBackupName, "/settings/snapshot");
                        window.clearInterval(downloadTimer);
                    }
                }

                attempts--;
            }, 1000);
        });
    }
}