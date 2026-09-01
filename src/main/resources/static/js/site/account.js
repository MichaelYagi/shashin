function initializeAccount(profileUrl, userId, username, status, toastTitle, toastBody, showGalleryPicker, isAdminOrSuper, apiKey) {
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
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.api.fail.title"), shashin.getTranslatedValue("main.toast.account.api.fail.body"), {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });

    $("#language").on("change", async function (e) {
        e.preventDefault();

        const language = $("#language option:selected").val();
        const http = new Http("language update");
        const json = {language: language};

        let data = await http.ajax("post", "/users/update/language", JSON.stringify(json));
        if (data.hasOwnProperty("updatedLanguage") && data.updatedLanguage === language) {
            window.top.location = window.top.location;
        } else {
            shashin.showToastMessage(shashin.getTranslatedValue("main.notupdated"), shashin.getTranslatedValue("main.notupdated"), {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }

    });

    $("#rssCopyLink").on("click", function (e) {
        e.preventDefault();

        copyLink($("#rssFeedLink").attr("href"),function (success) {
            if (success === true) {
                $("#rssCopyLink").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-x").addClass("bi-clipboard-check");
                $('#rssCopyLink').fadeOut(5000, function () {
                    $(this).removeClass("bi-clipboard-check").removeClass("bi-clipboard-x").addClass("bi-clipboard-plus");
                }).fadeIn(400);
            } else {
                $("#rssCopyLink").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-check").addClass("bi-clipboard-x");
            }
        });
    });
    $("#atomCopyLink").on("click", function (e) {
        e.preventDefault();

        copyLink($("#atomFeedLink").attr("href"),function (success) {
            if (success === true) {
                $("#atomCopyLink").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-x").addClass("bi-clipboard-check");
                $('#atomCopyLink').fadeOut(5000, function () {
                    $(this).removeClass("bi-clipboard-check").removeClass("bi-clipboard-x").addClass("bi-clipboard-plus");
                }).fadeIn(400);
            } else {
                $("#atomCopyLink").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-check").addClass("bi-clipboard-x");
            }
        });
    });

    function copyLink(link,callback) {
        if (link !== null && link !== "") {
            shashin.closeToastMessages({tag: "clipboard"});
            Util.copyToClipboard(link, function (successfullyCopied) {
                if (successfullyCopied) {
                    callback(true);
                } else {
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.copylink.fail.title"), shashin.getTranslatedValue("main.toast.account.copylink.fail.body"), {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "clipboard"});
                    callback(false);
                }
            });
        } else {
            shashin.closeToastMessages({tag: "clipboard"});
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.copylink.fail.title"), shashin.getTranslatedValue("main.toast.account.copylink.fail.body"), {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger", tag: "clipboard"});
            callback(false);
        }
    }

    $("#slideshowAlbumNameDataId").on("click", function (e) {
        e.preventDefault();
        $("#slideshowAlbumSelection").modal('show');

        $("body").on("keyup", function (e) {
            if (e.key === "Escape" || e.code === "Escape" || e.which === 27 || e.keyCode === 27) {
                if ($("#slideshowAlbumSelection").hasClass("show")) {
                    $("#slideshowAlbumSelection").modal('hide');
                }
            }
        });
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

                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.copylink.fail.title"), shashin.getTranslatedValue("main.toast.account.copylink.fail.title"), {
                        icon: "bi-info-circle",
                        iconColor: "#777777"
                    });
                } else {
                    shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.regenerateapi.fail.title"), shashin.getTranslatedValue("main.toast.account.regenerateapi.fail.msg"), {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor: "danger"
                    });
                }
            } else {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.regenerateapi.fail.title"), shashin.getTranslatedValue("main.toast.account.regenerateapi.blank.msg"), {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FF0000",
                    borderColor: "danger"
                });
            }
        } else {
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.settings.input.title.fail"), shashin.getTranslatedValue("main.toast.account.regenerateapi.invalid.msg"), {
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

        if ($("#newpassword").val().trim().length < 6) {
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.password.validationerror.title"), shashin.getTranslatedValue("main.user.reset.min"), {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
            fieldsValid = false;
        }

        if ($("#oldpassword").val().trim().length === 0 || $("#newpassword").val().trim().length === 0 || $("#newpasswordconfirm").val().trim().length === 0) {
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.password.validationerror.title"), shashin.getTranslatedValue("main.toast.account.password.validationerror.empty"), {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
            fieldsValid = false;
        }

        if ($("#newpassword").val().trim() !== $("#newpasswordconfirm").val().trim()) {
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.password.validationerror.title"), shashin.getTranslatedValue("main.toast.account.password.validationerror.nomatch"), {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
            fieldsValid = false;
        }

        return fieldsValid;
    }

    // Profile edit
    const initLink = shashin.getTranslatedValue("main.pages.account.profile.url");
    $("#profileMode").text(initLink);

    let randomString = Util.getMetadataLocalStorage();

    let cropperObject = null;

    if (profileUrl === null || profileUrl === "") {
        profileUrl = "#";
    }

    shashin.updateSearchInput(shashin.getTranslatedValue("main.pages.account.title"));

    $("#removeProfile").on("click", function () {
        $("#removeProfileConfirmationModal").modal('show');
    });

    function destroyCropper() {
        if (cropperObject !== null) {
            cropperObject.destroy();
            cropperObject = null;
        }
    }

    function resetProfile() {
        destroyCropper();

        $("#profilePictureEdit").css("display", "none");
        $("#profilePictureView").attr("src", profileUrl);
        $("#profilePictureView").css("display", "");
        $("#profilePictureEditWrapper").css("width", "16em");
        if (profileUrl === null || profileUrl === "" || profileUrl === "#") {
            $("#profilePictureEditWrapper").css("display", "none");
            $("#removeProfile").css("display", "none");
        } else {
            $("#profilePictureEditWrapper").css("display", "block");
            $("#removeProfile").css("display", "block");
        }

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
            $("#chooseProfilePhoto").attr("placeholder",shashin.getTranslatedValue("main.pages.account.profile.choose.placeholder"));
            $("#chooseProfilePhoto").focus();
            $("#profileMode").text(shashin.getTranslatedValue("main.pages.account.profile.choose.text"));
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
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail.title"), shashin.getTranslatedValue("main.toast.account.profile.fail.body"), {
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
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profileurl.fail.title"), shashin.getTranslatedValue("main.toast.account.profileurl.fail.body"), {
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
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profileurl.fail.title"), shashin.getTranslatedValue("main.toast.account.profileurl.fail.body"), {
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
        destroyCropper();

        if ($("#chooseProfilePhoto").attr("type") === "file") {
            processImage(this);
        }
    });

    function processImageMode(source) {
        destroyCropper();

        $("#profilePictureView").css("display", "none");
        $("#profilePictureEdit").css("display", "block");
        $("#profilePictureEditWrapper").css("width", "23em");
        $("#profilePictureEditWrapper").css("display", "block");
        $("#removeProfile").css("display", "none");
        $("#saveProfile").css("display", "block");
        $("#cancelProfile").css("display", "block");

        const editEl = document.getElementById("profilePictureEdit");
        cropperObject = new Kiri(editEl, {
            frame: { shape: "circle", width: 200, height: 200 },
            useExifOrientation: true,
            showZoomer: true,
            zoomerPosition: "bottom"
        });
        cropperObject.load(source);

        $("#cancelProfile").on("click", async function (e) {
            e.preventDefault();

            resetProfile();
            $("#chooseProfilePhoto").val("");
            location.replace(location.href.split('#')[0]);
        });

        $("#saveProfile").on("click", async function (e) {
            e.preventDefault();
            cropperObject.export({ type: "base64", format: "image/png" }).then(function (base64Result) {
                destroyCropper();
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
                            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail"), shashin.getTranslatedValue("main.toast.account.profile.fail") +". "+ + data.msg, {
                                icon: "bi-exclamation-triangle",
                                iconColor: "#FF0000",
                                borderColor: "danger"
                            });
                        }
                    } else {
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail"), shashin.getTranslatedValue("main.toast.account.profile.fail"), {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            borderColor: "danger"
                        });
                    }
                });
            });
        });
    }

    function processImage(input) {
        if ($("#chooseProfilePhoto").attr("type") === "url") {
            processImageMode($("#chooseProfilePhoto").val());
        } else if ($("#chooseProfilePhoto").attr("type") === "file" && input.files && input.files[0]) {
            processImageMode(input.files[0]);
        } else {
            // File dialog cancel pressed
            if (profileUrl === null || profileUrl === "" || profileUrl === "#") {
                $("#profileInfo").css("padding-left", "");
                $("#profilePictureEditWrapper").css("display", "none");
                $("#removeProfile").css("display", "none");

                $("#profilePictureView").attr('src', "#");
                $("#profileImage").attr('src', "#");
                $("#profileImage").css({"display": "none", "width": "32px", "height": "32px"});
                $("#profileImagePlaceholder").css({"display": "block", "font-size": "37px"});
                $("#removeProfile").css("display", "none");
            } else {
                $("#profileInfo").css("padding-left", "8em");
                $("#profilePictureEditWrapper").css("display", "block");
                $("#removeProfile").css("display", "block");

                $("#profilePictureView").attr('src', profileUrl + '?' + randomString);
                $("#profileImage").attr('src', profileUrl + '?' + randomString);
                $("#profileImage").css({"display": "inline-block", "width": "39px", "height": "39px"});
                $("#profileImagePlaceholder").css("display", "none");
                $("#removeProfile").css("display", "block");
            }
            $("#saveProfile").css("display", "none");
            $("#cancelProfile").css("display", "none");
        }
    }

    // Gallery picker
    if (showGalleryPicker) {
        let galleryPage = 0;
        let galleryAlbumId = null;
        let galleryLoading = false;
        let galleryDone = false;

        function resetGallery() {
            galleryPage = 0;
            galleryAlbumId = null;
            galleryLoading = false;
            galleryDone = false;
        }

        function appendPhotoThumbnail(item) {
            const thumbUrl = window.location.origin + item.thumbnailUrlCentered;
            const img = $('<img>')
                .attr('src', thumbUrl)
                .css({ width: '80px', height: '80px', 'object-fit': 'cover', cursor: 'pointer', margin: '2px', 'border-radius': '3px' });
            img.on('click', function () {
                processImageMode(thumbUrl);
                $('#galleryPickerModal').modal('hide');
            });
            $('#galleryPhotoGrid').append(img);
        }

        function checkFill() {
            const el = document.getElementById('galleryPickerBody');
            if (el && !galleryDone && !galleryLoading && el.scrollHeight <= el.clientHeight + 150) {
                if (isAdminOrSuper) {
                    loadMorePhotos();
                } else if (galleryAlbumId !== null) {
                    loadMoreAlbumPhotos();
                }
            }
        }

        function loadMorePhotos() {
            if (galleryLoading || galleryDone) return;
            galleryLoading = true;
            const http = new Http("gallery picker photos");
            http.setAdditionalParameters({headers: {'x-api-key': apiKey}});
            http.ajax("get", `/api/v1/taken?page=${galleryPage}&size=30`).then(function (data) {
                galleryLoading = false;
                const items = data.metadataList || [];
                for (const item of items) { appendPhotoThumbnail(item); }
                galleryPage++;
                if (galleryPage >= (data.totalPages || 1) || items.length === 0) { galleryDone = true; }
                checkFill();
            }).catch(function () { galleryLoading = false; });
        }

        function loadMoreAlbumPhotos() {
            if (galleryLoading || galleryDone) return;
            galleryLoading = true;
            const http = new Http("gallery album photos");
            http.setAdditionalParameters({headers: {'x-api-key': apiKey}});
            http.ajax("get", `/api/v1/album/${galleryAlbumId}?page=${galleryPage}&size=30`).then(function (data) {
                galleryLoading = false;
                const items = data.albumMetadataList || [];
                for (const item of items) { appendPhotoThumbnail(item); }
                galleryPage++;
                if (galleryPage >= (data.totalPages || 1) || items.length === 0) { galleryDone = true; }
                checkFill();
            }).catch(function () { galleryLoading = false; });
        }

        function showAlbumPhotos(albumId, albumName) {
            galleryAlbumId = albumId;
            galleryPage = 0;
            galleryDone = false;
            const body = $('#galleryPickerBody');
            const header = $('<div class="d-flex align-items-center p-2 border-bottom"></div>');
            const backBtn = $('<button type="button" class="btn btn-sm btn-secondary me-2">&#8592; Back</button>');
            backBtn.on('click', loadAlbumList);
            header.append(backBtn).append($('<strong></strong>').text(albumName));
            const grid = $('<div id="galleryPhotoGrid" class="d-flex flex-wrap p-2"></div>');
            body.empty().append(header).append(grid);
            loadMoreAlbumPhotos();
        }

        function loadAlbumList() {
            resetGallery();
            const body = $('#galleryPickerBody');
            body.html('<div class="text-center p-4"><div class="spinner-border"></div></div>');
            const http = new Http("gallery album list");
            http.setAdditionalParameters({headers: {'x-api-key': apiKey}});
            http.ajax("get", "/api/v1/albums").then(function (data) {
                const albums = data.albumsList || [];
                body.empty();
                if (albums.length === 0) {
                    body.html('<div class="text-center p-4">No albums available.</div>');
                    return;
                }
                const grid = $('<div class="d-flex flex-wrap p-2"></div>');
                for (const album of albums) {
                    const wrapper = $('<div class="text-center" style="width:100px;margin:4px;cursor:pointer;"></div>');
                    const thumb = $('<div style="width:92px;height:92px;overflow:hidden;background:#444;border-radius:4px;"></div>');
                    if (album.coverUrl) {
                        thumb.html(`<img src="${window.location.origin + album.coverUrl}" style="width:100%;height:100%;object-fit:cover;">`);
                    }
                    const name = $('<div style="font-size:0.75rem;overflow:hidden;white-space:nowrap;text-overflow:ellipsis;max-width:92px;margin-top:3px;"></div>').text(album.name);
                    wrapper.append(thumb).append(name);
                    wrapper.on('click', function () { showAlbumPhotos(album.id, album.name); });
                    grid.append(wrapper);
                }
                body.append(grid);
            }).catch(function () {
                body.html('<div class="text-center p-4">Could not load albums.</div>');
            });
        }

        $('#pickFromGallery').on('click', function (e) {
            e.preventDefault();
            resetGallery();
            const body = $('#galleryPickerBody');
            if (isAdminOrSuper) {
                body.empty().append('<div id="galleryPhotoGrid" class="d-flex flex-wrap p-2"></div>');
                loadMorePhotos();
            } else {
                loadAlbumList();
            }
            $('#galleryPickerModal').modal('show');
        });

        $('#galleryPickerModal').on('shown.bs.modal', function () {
            checkFill();
            $('#galleryPickerBody').on('scroll.galleryPicker', function () {
                const el = this;
                if (el.scrollTop + el.clientHeight >= el.scrollHeight - 150) {
                    if (isAdminOrSuper) {
                        loadMorePhotos();
                    } else if (galleryAlbumId !== null) {
                        loadMoreAlbumPhotos();
                    }
                }
            });
        });

        $('#galleryPickerModal').on('hide.bs.modal', function () {
            $('#galleryPickerBody').off('scroll.galleryPicker');
        });
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
                        shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail.body"), shashin.getTranslatedValue("main.toast.account.profile.fail.body") + ": " + data.msg, {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            borderColor: "danger"
                        });
                    }
                }
            } else {
                shashin.showToastMessage(shashin.getTranslatedValue("main.toast.settings.input.title.fail"), shashin.getTranslatedValue("main.toast.account.username.fail"), {
                    icon: "bi-exclamation-triangle",
                    iconColor: "#FD7E14",
                    borderColor: "warning"
                });
                $("#deleteAccountConfirmation").modal('hide');
                $("#deleteContentModalStatus").invisible();
                $("#deleteConfirm").val("");
            }
        } else {
            shashin.showToastMessage(shashin.getTranslatedValue("main.toast.account.profile.fail.body"), shashin.getTranslatedValue("main.toast.account.profile.fail.body"), {
                icon: "bi-exclamation-triangle",
                iconColor: "#FF0000",
                borderColor: "danger"
            });
        }
    });
}