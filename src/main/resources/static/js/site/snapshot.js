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
            $("#importDatabase").prop('disabled', true);
        }

        fileChooser.on("change", function(){
            if (fileChooser.val() === "") {
                $("#import").addClass('disabled');
                $("#importDatabase").prop('disabled', true);
            } else {
                $("#import").removeClass('disabled');
                $("#importDatabase").prop('disabled', false);
            }
        });

        $("#import").off("click").on("click", function() {
            $("#msg").text("Importing data.");
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.snapshot.import.title"), shashin.getTranslatedValue("main.toast.snapshot.import.body"), {tag:"importexport", icon:"bi-info-circle", iconColor:"#777777", autohide:false});
        });

        const tokenName = this.tokenName;
        const tokenSize = this.tokenSize;
        const tokenDbBackupName = this.tokenDbBackupName;
        let downloadTimer;

        $("#export").off("click").on("click", function() {
            let attempts = this.configuredAttempts;

            // $("#msg").text("Exporting data.");
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.snapshot.export.title"), shashin.getTranslatedValue("main.toast.snapshot.export.body"), {tag:"importexport", icon:"bi-info-circle", iconColor:"#777777", autohide:false});
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
                        // $("#msg").html("&nbsp;");
                    } else {
                        shashin.closeToastMessages({tag:"importexport"});
                        const dbBackupNameString = tokenCookieDbBackupName === "" ? "Error encountered":tokenCookieDbBackupName;
                        // $("#msg").text("Database backup name: " + dbBackupNameString + ". File name: " + tokenCookieValue + ". File size: " + Util.formatBytes(tokenCookieSize) + ".");
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.snapshot.saving.title"), shashin.getTranslatedValue("main.toast.snapshot.saving.name") + ": " + dbBackupNameString + shashin.getTranslatedValue("main.toast.snapshot.saving.filename") + ": " + tokenCookieValue + shashin.getTranslatedValue("main.toast.snapshot.saving.filesize") + ": " + Util.formatBytes(tokenCookieSize), {icon:"bi-info-circle", iconColor:"#777777"});
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