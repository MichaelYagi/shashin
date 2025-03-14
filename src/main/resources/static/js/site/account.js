function initializeAccount(profileUrl, userId, username, status, toastTitle, toastBody) {
    // API management
    $("#copyapikey").on("click", function (e) {
        e.preventDefault();

        let apikey = $("#apikey").val();

        if (apikey !== null && apikey !== "") {
            Util.copyToClipboard(apikey, function (successfullyCopied) {
                if (successfullyCopied) {
                    $("#apikeycopyicon").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-x").addClass("bi-clipboard-check");
                    $('#apikeycopyicon').fadeOut(5000, function () {
                        $(this).removeClass("bi-clipboard-check").removeClass("bi-clipboard-x").addClass("bi-clipboard-plus");
                    }).fadeIn(400);
                } else {
                    $("#apikeycopyicon").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-check").addClass("bi-clipboard-x");
                }
            });
        } else {
            $("#apikeycopyicon").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-check").addClass("bi-clipboard-x");
            shashin.showToastMessage("Operation failed", "API key must not be blank", {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });

    $("#updateapikey").on("click", async function (e) {
        $("#apikeyUpdateConfirmation").modal('show');
    });

    $("#confirmUpdateApiKey").on("click", async function (e) {
        e.preventDefault();

        if ($("#updateConfirm").val() === "UPDATE") {
            const currentApikey = $("#apikey").val();

            if (currentApikey !== null && currentApikey !== "") {
                const http = new Http("apikey update");
                const json = {currentApikey: currentApikey};

                let data = await http.ajax("post", "/users/update/apikey", JSON.stringify(json));
                if (data.hasOwnProperty("updatedApikey") && data.updatedApikey !== "" &&
                    data.hasOwnProperty("rssFeedLink") && data.rssFeedLink !== "") {
                    $("#apikey").val(data.updatedApikey);

                    $("#rssFeedLink").text(data.rssFeedLink);
                    $("#rssFeedLink").attr("href", data.rssFeedLink);

                    $("#atomFeedLink").text(data.atomFeedLink);
                    $("#atomFeedLink").attr("href", data.atomFeedLink);

                    shashin.showToastMessage("API key updated", "API key has been updated.", {
                        icon: "bi-info-circle",
                        iconColor: "#777777"
                    });
                } else {
                    shashin.showToastMessage("Operation failed", "Could not regenerate API key", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor: "danger"
                    });
                }
            } else {
                shashin.showToastMessage("Operation failed", "API key must not be blank", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            shashin.showToastMessage("Input not valid", "Try again. You must type UPDATE in all caps.", {
                icon: "bi-exclamation-triangle",
                iconColor: "#FD7E14",
                borderColor: "warning"
            });
            $("#apikeyUpdateConfirmation").modal('hide');
        }
        $("#updateConfirm").val("");
    });

    // Password edit
    if (status !== "" && toastTitle !== "" && toastBody !== "") {
        if (status === shashin.apiResponse.SUCCESS) {
            shashin.showToastMessage(toastTitle, toastBody, {icon: "bi-info-circle", iconColor: "#777777"});
        } else {
            shashin.showToastMessage(toastTitle, toastBody, {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    }

    $("#passwordForm").on("submit", function (e) {
        e.preventDefault();

        if (validateFields() === true) {
            this.submit();
        }
    });

    $("#unhideold").on("click", function (e) {
        e.preventDefault();
        changeTypes($("#oldpassword"), $("#unhideoldicon"));
    });

    $("#unhidenew").on("click", function (e) {
        e.preventDefault();
        changeTypes($("#newpassword"), $("#unhidenewicon"));
    });

    $("#unhideconfirm").on("click", function (e) {
        e.preventDefault();
        changeTypes($("#newpasswordconfirm"), $("#unhideconfirmicon"));
    });

    function changeTypes(elem, icon) {
        if (elem.attr("type") === "password") {
            elem.prop("type", "text");
            icon.removeClass("bi-eye").addClass("bi-eye-slash");
        } else {
            elem.prop("type", "password");
            icon.removeClass("bi-eye-slash").addClass("bi-eye");
        }
    }

    function validateFields() {
        let fieldsValid = true;

        if ($("#oldpassword").val().trim().length === 0 || $("#newpassword").val().trim().length === 0 || $("#newpasswordconfirm").val().trim().length === 0) {
            shashin.showToastMessage("Validation error", "Password fields cannot be empty.", {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
            fieldsValid = false;
        }

        if ($("#newpassword").val().trim() !== $("#newpasswordconfirm").val().trim()) {
            shashin.showToastMessage("Validation error", "Passwords do not match.", {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
            fieldsValid = false;
        }

        return fieldsValid;
    }

    // Profile edit
    const initLink = "Enter Image URL";
    $("#profileMode").text(initLink);

    let randomString = Util.getMetadataLocalStorage();

    let croppieObject = null;

    if (profileUrl === null || profileUrl === "") {
        profileUrl = "#";
    }

    shashin.updateSearchInput("Manage Account");

    $("#removeProfile").on("click", function () {
        $("#removeProfileConfirmationModal").modal('show');
    });

    function resetProfile() {
        if (croppieObject !== null) {
            $("#profilePictureEdit").croppie('destroy');
            croppieObject = null;
        }

        $("#profilePictureEdit").attr("src", profileUrl);
        $("#profilePictureEditWrapper").css("width", "16em");
        if (profileUrl === null || profileUrl === "" || profileUrl === "#") {
            $("#profilePictureEditWrapper").css("display", "none");
            $("#removeProfile").css("display", "none");
        } else {
            $("#profilePictureEditWrapper").css("display", "block");
            $("#removeProfile").css("display", "block");
        }

        $("#profilePictureEdit").croppie('destroy');
        croppieObject = null;
        $("#profileInfo").css("padding-left", "");
        $("#saveProfile").css("display", "none");
        $("#cancelProfile").css("display", "none");
    }

    $("#profileMode").on("click", function (e) {
        e.preventDefault();

        resetProfile();

        if ($("#chooseProfilePhoto").attr("type") === "file") {
            // Change to text input
            $("#chooseProfilePhoto").attr("type","url");
            $("#chooseProfilePhoto").attr("placeholder","Enter URL To Upload Image From Web And Press Enter Key to Upload");
            $("#chooseProfilePhoto").focus();
            $("#profileMode").text("Choose Image From File");
        } else {
            $("#chooseProfilePhoto").attr("type","file");
            $("#chooseProfilePhoto").attr("placeholder","");
            $("#profileMode").text(initLink);
        }
    });

    $("#removeProfileConfirmation").on("click", function () {
        const http = new Http("delete profile picture");
        let json = {userId: userId};
        http.ajax("post", "/users/delete/profile", JSON.stringify(json)).then(function (data) {
            if (data.hasOwnProperty("status") && data.status === "success" && data.hasOwnProperty("msg")) {
                window.top.location = window.top.location;
            } else {
                shashin.showToastMessage("Could not delete profile", "Something went wrong", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        });
    });

    $("#chooseProfilePhoto").on("keydown", function(e) {
        if ($("#chooseProfilePhoto").attr("type") === "url" && (e.key === "Enter" || e.code === "Enter" || e.which === 13 || e.keyCode === 13)) {
            resetProfile();

            const url = $("#chooseProfilePhoto").val();
            let validUrl = true;
            try {
                const testUrl = new URL(url);
            } catch (_) {
                validUrl = false;
            }

            if (validUrl === true) {
                testImage($("#chooseProfilePhoto").val()).then(record.bind(null, url), record.bind(null, url));
            } else {
                shashin.showToastMessage("URL invalid", "Could not load image. Check URL.", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        }
    });

    function record(url, result) {
        if (result === "success") {
            processImage(this);
        } else {
            shashin.showToastMessage("URL invalid", "Could not load image. Request "+result+". Check URL: "+url+".", {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    }

    function testImage(url, timeoutT) {
        return new Promise(function(resolve, reject) {
            const timeout = timeoutT || 3000;
            let timer, img = new Image();
            img.onerror = img.onabort = function() {
                clearTimeout(timer);
                reject("error");
            };
            img.onload = function() {
                clearTimeout(timer);
                resolve("success");
            };
            timer = setTimeout(function() {
                // reset .src to invalid URL so it stops previous
                // loading, but doesn't trigger new load
                reject("timeout");
            }, timeout);
            img.src = url;
        });
    }

    $("#chooseProfilePhoto").on("change", function () {
        if (croppieObject !== null) {
            $("#profilePictureEdit").croppie('destroy');
            croppieObject = null;
        }

        if ($("#chooseProfilePhoto").attr("type") === "file") {
            processImage(this);
        }
    });

    function processImageMode(event) {
        if ($("#chooseProfilePhoto").attr("type") === "file") {
            $("#profilePictureEdit").attr('src', event.target.result);
        } else {
            // $("#profilePictureEdit").attr("referrerPolicy", "no-referrer");
            // $("#profilePictureEdit").attr("crossorigin", "anonymous");
            $("#profilePictureEdit").attr('src', $("#chooseProfilePhoto").val());
        }
        $("#profilePictureEditWrapper").css("width", "23em");
        $("#profilePictureEditWrapper").css("display", "block");
        $("#removeProfile").css("display", "none");
        $("#saveProfile").css("display", "block");
        $("#cancelProfile").css("display", "block");

        croppieObject = $("#profilePictureEdit").croppie({
            customClass: "croppieContainer",
            enableExif: true,
            viewport: {
                width: 200,
                height: 200,
                type: 'circle'
            },
            boundary: {
                width: 300,
                height: 300
            }
        });

        $("#cancelProfile").on("click", async function (e) {
            e.preventDefault();

            resetProfile();
            $("#chooseProfilePhoto").val("");
            location.replace(location.href.split('#')[0]);
        });

        $("#saveProfile").on("click", async function (e) {
            e.preventDefault();
            croppieObject.croppie('result', 'base64').then(function (base64Result) {
                // send to server
                const http = new Http("upload profile picture");
                http.setAdditionalParameters({cache: false});
                http.setAdditionalHeaders({
                    'Cache-Control': 'no-cache, no-store, max-age=0',
                    'Expires': 'Thu, 1 Jan 1970 00:00:00 GMT',
                    'Pragma': 'no-cache'
                });
                let json = {base64: base64Result};
                http.ajax("post", "/users/profile", JSON.stringify(json)).then(function (data) {
                    $("#profilePictureEditWrapper").css("width", "16em");
                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                        if (data.hasOwnProperty("imageUrl") && data.imageUrl !== "") {
                            window.top.location = window.top.location;
                        } else {
                            shashin.showToastMessage("Could not save profile", "Could not save profile. " + data.msg, {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000",
                                borderColor: "danger"
                            });
                        }
                    } else {
                        shashin.showToastMessage("Could not save profile", "Something went wrong", {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            borderColor: "danger"
                        });
                    }
                });

                $("#profilePictureEdit").croppie('destroy');
                croppieObject = null;
            });
        });
    }

    function processImage(input) {
        if ($("#chooseProfilePhoto").attr("type") === "url") {
            processImageMode();
        } else if ($("#chooseProfilePhoto").attr("type") === "file" && input.files && input.files[0]) {
            const reader = new FileReader();

            reader.onload = function (e) {
                processImageMode(e);
            };

            reader.readAsDataURL(input.files[0]);
        } else {
            // File dialog cancel pressed
            if (profileUrl === null || profileUrl === "" || profileUrl === "#") {
                $("#profileInfo").css("padding-left", "");
                $("#profilePictureEditWrapper").css("display", "none");
                $("#removeProfile").css("display", "none");

                $("#profilePictureEdit").attr('src', "#");
                $("#profileImage").attr('src', "#");
                $("#profileImage").css({"display": "none", "width": "32px", "height": "32px"});
                $("#profileImagePlaceholder").css({"display": "block", "font-size": "37px"});
                $("#removeProfile").css("display", "none");
                $("#profileCard").css("width", "300px");
            } else {
                $("#profileInfo").css("padding-left", "8em");
                $("#profilePictureEditWrapper").css("display", "block");
                $("#removeProfile").css("display", "block");

                $("#profilePictureEdit").attr('src', profileUrl + '?' + randomString);
                $("#profileImage").attr('src', profileUrl + '?' + randomString);
                $("#profileImage").css({"display": "inline-block", "width": "39px", "height": "39px"});
                $("#profileImagePlaceholder").css("display", "none");
                $("#removeProfile").css("display", "block");
            }
            $("#saveProfile").css("display", "none");
            $("#cancelProfile").css("display", "none");
        }
    }

    // Delete account
    $("#deleteAccount").on("click", async function (e) {
        e.preventDefault();
        $("#deleteAccountConfirmation").modal('show');
    });

    $("#confirmDeleteAccount").on("click", async function (e) {
        e.preventDefault();

        if (userId > 0) {
            if ($("#deleteConfirm").val() === username) {

                const http = new Http("deleting account");
                let json = {
                    userId: userId
                };
                const data = await http.ajax("post", "/users/account/delete", JSON.stringify(json));

                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    if (data.status === "success") {
                        window.location.replace("/users/logout");
                    } else {
                        shashin.showToastMessage("Uh-oh!", "Something went wrong! " + data.msg, {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            borderColor: "danger"
                        });
                    }
                }
            } else {
                shashin.showToastMessage("Input not valid", "Try again. You must type your username.", {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FD7E14",
                    borderColor: "warning"
                });
                $("#deleteAccountConfirmation").modal('hide');
                $("#deleteContentModalStatus").invisible();
                $("#deleteConfirm").val("");
            }
        } else {
            shashin.showToastMessage("Uh-oh!", "Something went wrong!", {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });
}