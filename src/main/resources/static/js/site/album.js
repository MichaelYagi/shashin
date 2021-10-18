(function( albumSettings, $, undefined ) {
    albumSettings.infiniteScrollGallery = null;
    albumSettings.lg = null;
    albumSettings.lightGalleryConfigs = shashin.getLightGalleryConfigs();
    albumSettings.lightGalleryConfigs["dynamic"] = true;
    albumSettings.retryLimit = 3;
    albumSettings.tryCount = 0;

    albumSettings.openGallery = function(e,index) {
        e.preventDefault();
        if (albumSettings.getLightGallery() !== null) {
            albumSettings.getLightGallery().openGallery(index);
        }
    }

    albumSettings.setLightGalleryElement = function (name) {
        if (document.getElementById(name)) {
            albumSettings.infiniteScrollGallery = document.getElementById(name);
        }
    };

    albumSettings.setLightGallery = function () {
        albumSettings.lg = lightGallery(albumSettings.getLightGalleryElement(), albumSettings.lightGalleryConfigs);
    }

    albumSettings.getLightGalleryElement = function () {
        return albumSettings.infiniteScrollGallery;
    };

    albumSettings.getLightGallery = function () {
        return albumSettings.lg;
    }

    albumSettings.openAlbumModal = function (e,metadataId) {
        e.preventDefault();
        let metadata;
        if ($("#albumModalEdit"+metadataId).attr("tag") && $("#albumModalEdit"+metadataId).attr("tag").trim() !== "") {
            metadata = JSON.parse($("#albumModalEdit"+metadataId).attr("tag"));
        }

        if (metadata !== null) {
            // Clear modal data
            $("#albumModalTitle").text(metadata.fileName)
            $('#propAlbumModal').find(':input').val('');
            $("#removeFromAlbum")[0].checked = false;
            $("#setCoverAlbum")[0].checked = false;
            $("#propAlbumModalThumbnail").html("");

            $("#metadataId").val(metadata.id);
            if (metadata.thumbnailUrlCentered !== null) {
                $("#propAlbumModalThumbnail").html('<img src="'+encodeURI(metadata.thumbnailUrlCentered)+'" height="100" width="100" onError="shashin.errorImg(this,\''+metadata.title+'\',100)">');
            }

            // Open modal window
            $("#propAlbumModal").modal('show');
        }
    }

    albumSettings.updateAlbum = function(albumId,nextPage,activePage) {
        const promise =  $.ajax({
            type: 'get',
            url: "/album/"+albumId+"/page/"+nextPage,
            contentType: 'application/json; charset=utf-8',
            async:true
        }).fail(function (xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error updating album. Attempt: "+albumSettings.tryCount+"/"+albumSettings.retryLimit+". Status: " + xhr.status + ". Text Status: " + textStatus + ".");

            if (textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) {
                albumSettings.tryCount++;
                if (albumSettings.tryCount <= albumSettings.retryLimit) {
                    //try again
                    albumSettings.updateAlbum(albumId,nextPage,activePage);
                }
            }
        }).then(function (data) {
            albumSettings.tryCount = 0;
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("album") && data.hasOwnProperty("albumMetadataList") && data.hasOwnProperty("albumPhotoCommentsMap") && data.hasOwnProperty("currentUser") && data.hasOwnProperty("notificationMap")) {
                        const albumData = data["album"] === "" ? null : data["album"];
                        const albumMetadataList = data["albumMetadataList"] === "" ? null : data["albumMetadataList"];
                        const albumPhotoCommentsMap = data["albumPhotoCommentsMap"] === "" ? null : data["albumPhotoCommentsMap"];
                        const currentUser = data["currentUser"] === "" ? null : data["currentUser"];
                        const notificationMap = data["notificationMap"] === "" ? null : data["notificationMap"];
                        const favoritesMap = data["favorites"] === "" ? null : data["favorites"];

                        shashin.printMessageToConsole(albumData);
                        shashin.printMessageToConsole(albumMetadataList);
                        shashin.printMessageToConsole(albumPhotoCommentsMap);
                        shashin.printMessageToConsole(currentUser);
                        shashin.printMessageToConsole(albumMetadataList.length);

                        if (albumMetadataList.length > 0) {
                            const mediaLinkLength = $(".mediaLink").length;
                            for (const index in albumMetadataList) {
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                const mediaContent = {};
                                const metadata = albumMetadataList[index];

                                let dateString = shashin.getDateString(metadata["year"], metadata["month"], metadata["day"]);
                                const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["favorite"] === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                                const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["count"] > 0 ? favoritesMap[metadata.id]["count"] : 0;

                                let html =
                                    '<div class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex' + currentMediaLinkIndex + '"></a>\n' +
                                    '   <img src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="shashin.errorImg(this,\''+metadata.title+'\',209)">\n';
                                if (metadata.type.includes("video")) {
                                    const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                    html +=
                                        '   <div class="thumbnail-tr" id="tntr' + metadata.id + '">\n' +
                                        '       <span class="overlayIconBackground">'+duration+'&nbsp;<span id="video' + metadata.id + '" class="bi-camera-video overlayIcon"></span></span>\n' +
                                        '   </div>\n';
                                } else if (metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight*2) {
                                    html +=
                                        '   <div class="thumbnail-tr" id="tntr' + metadata.id + '">\n' +
                                        '       <span id="panorama' + metadata.id + '" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>\n' +
                                        '   </div>\n';
                                }

                                if (currentUser.authority === "ROLE_ADMIN") {
                                    html +=
                                        '   <div class="thumbnail-tl" id="tntl' + metadata.id + '">\n' +
                                        '       <a href="#" id="select' + metadata.id + '">\n' +
                                        '           <span id="tlicon' + metadata.id + '" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                        '       </a>\n' +
                                        '   </div>\n' +
                                        '   <div class="thumbnail-bl" id="tnbl' + metadata.id + '">\n' +
                                        '       <a href="#" id="albumModalEdit'+metadata.id+'" onclick="return albumSettings.openAlbumModal(event,\''+metadata.id+'\')" class="overlayCommentText">\n' +
                                        '           <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>\n' +
                                        '       </a>\n' +
                                        '   </div>\n';
                                }
                                html +=
                                    '   <div class="thumbnail-br" id="tnbr' + metadata.id + '">\n' +
                                    '       <a href="#" id="favorite' + metadata.id + '" class="text-decoration-none">\n' +
                                    '           <span class="overlayIconBackground">\n' +
                                    '               <span id="briconcount' + metadata.id + '">'+favoriteCount+' </span><span class="'+favoriteIcon+' overlayIcon" id="brfavoriteicon'+metadata.id+'"></span>\n' +
                                    '           </span>\n' +
                                    '       </a>\n' +
                                    '       <br>' +
                                    '       <a href="#" data-bs-toggle="modal" data-bs-target="#propalbumphotocomment' + metadata.id + '" class="overlayCommentIconBackground overlayCommentText">\n' +
                                    '           ' + albumPhotoCommentsMap[metadata.id].length + ' <span id="bricon' + metadata.id + '" class="bi-chat-square position-relative overlayCommentIcon">';

                                if (notificationMap !== null && notificationMap[metadata.id] === true) {
                                    html +=
                                        '           <span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle">\n' +
                                        '               <span class="visually-hidden">New alerts</span>\n' +
                                        '           </span>';
                                }
                                html +=
                                    '           </span>\n' +
                                    '       </a>\n' +
                                    '   </div>\n' +
                                    '   <div class="thumbnail-centered" id="tncentered' + metadata.id + '">\n';

                                mediaContent.subHtml = (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '');
                                if (metadata.type.includes("video")) {
                                    mediaContent.video = {"source":[{"src":metadata.videoUrl,"type":"video/mp4"}],"attributes":{"preload":false,"controls":true}};
                                    html +=
                                        '   <a class="mediaLink" onclick="return albumSettings.openGallery(event,'+currentMediaLinkIndex+')"\n' +
                                        '       data-video=\'{"source": [{"src":"' + metadata.videoUrl + '", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}\'\n' +
                                        '       data-sub-html="' + (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                                        '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                        '   </a>\n';
                                } else {
                                    mediaContent.src = metadata.thumbnailUrlOriginal;
                                    html +=
                                        '   <a class="mediaLink" onclick="return albumSettings.openGallery(event,'+currentMediaLinkIndex+')" data-src="' + metadata.thumbnailUrlOriginal + '" href="' + metadata.thumbnailUrlOriginal + '"' +
                                        '       data-sub-html="' + (metadata.placeName !== null ? '<a href=\'/map?lat='+metadata.lat+'&lng='+metadata.lng+'\' target=\'_blank\'>'+metadata.placeName+'</a><br>' : "<br>") + metadata.fileName + (dateString !== "" ? ' taken on ' + dateString : '') + '">\n' +
                                        '       <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>\n' +
                                        '   </a>\n';
                                }
                                mediaContentList.push(mediaContent);
                                html +=
                                    '   </div>\n' +
                                    '</div>\n' +
                                    '<span id="albummodal' + metadata.id + '" style="width:0;height:0;padding:0"></span>\n';

                                // Append HTML
                                $(html).insertBefore($(".appendAlbumPhotos").last())

                                // Call JS and modal
                                $("#albumModalEdit"+metadata.id).attr("tag", JSON.stringify(metadata));
                                shashin.setPhotoOverlays(metadata, activePage);
                                albumModal.renderAlbumModal(albumData, metadata, currentUser, albumPhotoCommentsMap);
                                albumSettings.activateAlbumListeners(metadata, albumData);
                            }
                        } else {
                            $(".appendAlbumPhotos").last().text("EOL").css("display","none")
                        }
                    }
                } else {
                    $(".appendAlbumPhotos").last().text("EOL").css("display","none")
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendAlbumPhotos").last().text("EOL").css("display","none")
            }

            return mediaContentList;
        });

        return promise.done(function(data) {
            return data;
        });
    }

    albumSettings.activateAlbumListeners = function(metadata,album) {
        $("#image"+metadata.id).on('load', function() {
            $(this).css("background-color","transparent");
        });

        $('#removeFromAlbum'+metadata.id).change(function() {
            if (this.checked) {
                $('#setCoverAlbum'+metadata.id).prop("checked", false);
                $('#setCoverAlbum'+metadata.id).attr("disabled", true);
            } else {
                $('#setCoverAlbum'+metadata.id).removeAttr("disabled");
            }
        });

        $('#propalbumphotocomment'+metadata.id).on('show.bs.modal', function () {
            const promise = $.ajax({
                type: 'get',
                url: "/notifications/markread/metadata/"+metadata.id,
                contentType: 'application/json; charset=utf-8',
                async:true
            }).then(function (data) {

            });
        });

        $("#favorite"+metadata.id).click(function (e) {
            e.preventDefault();

            const metadataId = metadata.id;

            if ($("#brfavoriteicon" + metadataId).hasClass("bi-suit-heart")) {
                $("#brfavoriteicon" + metadataId).removeClass("bi-suit-heart").addClass("bi-suit-heart-fill");
            } else if ($("#brfavoriteicon" + metadataId).hasClass("bi-suit-heart-fill")) {
                $("#brfavoriteicon" + metadataId).removeClass("bi-suit-heart-fill").addClass("bi-suit-heart");
            }

            const isFavorite = ($("#brfavoriteicon" + metadataId).hasClass("bi-suit-heart-fill"));

            const json = {metadataId: metadataId, isFavorite: isFavorite};
            let posting;

            if (isFavorite === true) {
                posting = $.post({
                    url: "/favorite/save",
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8'
                });
            } else {
                posting = $.post({
                    url: "/favorite/delete",
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8'
                });
            }

            posting.done(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    shashin.printMessageToConsole(data["status"]);
                    shashin.printMessageToConsole(data["msg"]);
                }
            });

            return false;
        });

        // Clear message on modal close
        $('#propAlbumModal').on('hide.bs.modal', function () {
            $("#albumModalMsg").html("");
        });

        // Clear message on input editing
        $('#propAlbumModal input').bind('keypress', function() {
            $("#albumModalMsg").html("");
        });
    }

    albumSettings.deleteComment = function(commentId, metadata) {
        let json = {commentId: commentId, metadataId: metadata.id}

        const posting = $.post({
            url: "/comment/albumphoto/delete/",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8'
        });

        posting.done(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                let commentId = data["commentId"];
                let message = "Error";
                if (data["status"] === "success") {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    // Delete comment
                    $("#comment" + commentId).remove();
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                }
            }
        });

        $("#currentCommentId" + metadata.id).val("");
    }

    albumSettings.editComment = function(commentId, metadata) {
        if ($("#currentCommentId" + metadata.id).val() === "") {
            $("#currentCommentId" + metadata.id).val(commentId);

            $("#saveCommentMetadata" + metadata.id).hide();
            $("#dismissModalCommentMetadata" + metadata.id).hide();
            $("#updateCommentMetadata" + metadata.id).show();
            $("#cancelEditCommentMetadata" + metadata.id).show();

            $("#commentcontainer" + commentId).hide();
            $("#textareacontainer" + commentId).show();
            var commentText = $("#commentcontent" + commentId).text();
            $("#textareacontainer" + commentId).html('<textarea class="form-control" id="commenttext' + commentId + '" rows="2">' + commentText + '</textarea>');
        }
    }

    albumSettings.albumCommentsDeleteEditModalListener = function(commentId, metadata) {
        $("#deletecomment" + commentId).click(function (e) {
            e.preventDefault();
            albumSettings.deleteComment(commentId, metadata);
        });

        $("#editcomment" + commentId).click(function (e) {
            e.preventDefault();
            albumSettings.editComment(commentId, metadata);
        });
    }

    albumSettings.albumCommentsUpdateSaveModalListener = function(metadata, album, currentUser) {
        $("#updateCommentMetadata"+metadata.id).hide();
        $("#cancelEditCommentMetadata"+metadata.id).hide();

        $("#cancelEditCommentMetadata"+metadata.id).click(function (e) {
            e.preventDefault();

            var currentCommentId = $("#currentCommentId"+metadata.id).val()

            if (currentCommentId !== "") {
                $("#saveCommentMetadata"+metadata.id).show();
                $("#dismissModalCommentMetadata"+metadata.id).show();
                $("#updateCommentMetadata"+metadata.id).hide();
                $("#cancelEditCommentMetadata"+metadata.id).hide();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId"+metadata.id).val("");
            }
        });

        $("#updateCommentMetadata"+metadata.id).click(function (e) {
            e.preventDefault();

            var currentCommentId = $("#currentCommentId"+metadata.id).val()

            if (currentCommentId !== "") {
                var updatedComment = $.trim($("#commenttext"+currentCommentId).val());

                if (updatedComment.length > 0) {
                    let json = {commentId: currentCommentId, comment: updatedComment}

                    const posting = $.post({
                        url: "/comment/update",
                        data: JSON.stringify(json),
                        contentType: 'application/json; charset=utf-8'
                    });

                    posting.done(function (data) {
                        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                            let commentId = data["commentId"];
                            let message = "Error";
                            if (data["status"] === "success") {
                                message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                            } else {
                                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                            }

                            // Update comment
                            $("#commentcontent"+commentId).text(updatedComment);

                            $("#saveCommentMetadata"+metadata.id).show();
                            $("#dismissModalCommentMetadata"+metadata.id).show();
                            $("#updateCommentMetadata"+metadata.id).hide();
                            $("#cancelEditCommentMetadata"+metadata.id).hide();

                            $("#commentcontainer" + commentId).show();
                            $("#textareacontainer" + commentId).html('');
                            $("#textareacontainer" + commentId).hide();
                            $("#commenttext"+commentId).val("");
                        }
                    });
                }

                $("#saveCommentMetadata"+metadata.id).show();
                $("#dismissModalCommentMetadata"+metadata.id).show();
                $("#updateCommentMetadata"+metadata.id).hide();
                $("#cancelEditCommentMetadata"+metadata.id).hide();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId"+metadata.id).val("");
            }
        });

        $("#saveCommentMetadata"+metadata.id).click(function(e) {
            e.preventDefault();

            let comment = $.trim($("#commentText"+metadata.id).val());

            if (comment.length > 0) {
                let json = {metadataId: metadata.id, albumId: album.id, comment: comment};

                const posting = $.post({
                    url: "/comment/albumphoto/save/",
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8'
                });

                posting.done(function (data) {
                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                        let commentId = data["commentId"];
                        let message = "Error";
                        if (data["status"] === "success") {
                            message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        } else {
                            message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        }

                        // Insert comment at top of list
                        var commentItem = '<li class="list-group-item list-group-item-secondary" id="comment'+commentId+'">\n' +
                            '<span id="commentcontainer'+commentId+'">\n<p id="commentcontent'+commentId+'">'+comment+'</p>\n' +
                            '<small>'+currentUser.username+'<span style="float: right"><a href="#" id="deletecomment'+commentId+'"><span class="bi-trash"></span></a>&nbsp;&nbsp;<a href="#" id="editcomment'+commentId+'"><span class="bi-pencil"></span></a></span></small></span>' +
                            '<span id="textareacontainer'+commentId+'"></span></li>';
                        $("#commentText"+metadata.id).val("")
                        $("#commentList"+metadata.id).prepend(commentItem);

                        $("#deletecomment" + commentId).click(function (e) {
                            e.preventDefault();
                            albumSettings.deleteComment(commentId, metadata);
                        });

                        $("#editcomment" + commentId).click(function (e) {
                            e.preventDefault();
                            albumSettings.editComment(commentId, metadata);
                        });
                    }
                });
            }
        });
    }
}( window.albumSettings = window.albumSettings || {}, jQuery ));