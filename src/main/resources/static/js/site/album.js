(function( albumSettings, $, undefined ) {
    albumSettings.rendering = false;

    albumSettings.init = async function (albumId, activePage, albumMetadataList) {
        let mediaContentList = shashin.initLightGallery('infinite-scroll-gallery', {
            dynamic: true,
            plugins: [lgMetadataDetail],
            metadataDetail: true
        }, '.mediaLink');

        async function loadNextPage() {
            if (albumSettings.rendering === false) {
                const currentPage = parseInt($("#currentPage").val());
                const nextPage = currentPage + 1;
                $("#currentPage").val(nextPage);

                const additionalMediaContentList = await albumSettings.updateAlbum(albumId, nextPage, activePage);
                mediaContentList = shashin.updateMediaContent(mediaContentList, additionalMediaContentList);
            }
        }

        shashin.pageLoader(await loadNextPage, ".appendAlbumPhotos", albumMetadataList);

        shashin.mouseMoveListener();

        $('[data-bs-toggle="tooltip"]').tooltip();
    }

    albumSettings.openAlbumModal = function (e,metadataId) {
        e.preventDefault();

        shashin.getMetadata(metadataId).then(function (metadata) {
            if (metadata !== null) {
                // Clear modal data
                $("#albumModalTitle").text(metadata.title)
                $('#propAlbumModal').find(':input').val('');
                $("#removeFromAlbum")[0].checked = false;
                $("#setCoverAlbum")[0].checked = false;
                $("#propAlbumModalThumbnail").html("");

                $("#metadataId").val(metadata.id);
                if (metadata.thumbnailUrlCentered !== null) {
                    $("#propAlbumModalThumbnail").html('<img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlCentered) + '" height="100" width="100" onError="Util.errorImg(this,\'' + metadata.title + '\',100)">');
                }

                // Open modal window
                $("#propAlbumModal").modal('show');
            }
        });
    }

    albumSettings.updateAlbum = async function(albumId,nextPage,activePage) {
        albumSettings.rendering = true;
        $("#spinner").css("display","block");

        const ajaxParams = {
            type: 'get',
            url: "/album/"+albumId+"/page/"+nextPage,
            contentType: 'application/json; charset=utf-8',
            async: true,
            retries: shashin.ajaxRetries
        }

        return $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating album")})
        .then(function (data) {
            const mediaContentList = [];
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data["status"] === "success") {
                    if (data.hasOwnProperty("album") &&
                        data.hasOwnProperty("albumMetadataList") &&
                        data.hasOwnProperty("albumPhotoCommentsMap") &&
                        data.hasOwnProperty("userMap") &&
                        data.hasOwnProperty("notificationMap")
                    ) {
                        const albumData = data["album"];
                        const albumMetadataList = data["albumMetadataList"];
                        const albumPhotoCommentsMap = data["albumPhotoCommentsMap"];
                        const userMap = data["userMap"];
                        const notificationMap = data["notificationMap"];
                        const favoritesMap = data["favorites"];
                        const keywordMap = data["keywordMap"];

                        shashin.printMessageToConsole(albumData);
                        shashin.printMessageToConsole(albumMetadataList);
                        shashin.printMessageToConsole(albumPhotoCommentsMap);
                        shashin.printMessageToConsole(userMap);
                        shashin.printMessageToConsole(albumMetadataList.length);

                        if (albumMetadataList.length > 0) {
                            const mediaLinkLength = $(".mediaLink").length;

                            for (const index in albumMetadataList) {
                                let html = "";
                                const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                                const metadata = albumMetadataList[index];

                                const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["favorite"] === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                                const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["count"] > 0 ? favoritesMap[metadata.id]["count"] : 0;

                                const dateHeadingCount = $(".dateSection").length;
                                const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                                const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                                const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                                if (lastDateHeading !== currentDate) {
                                    html += '<section class="dateSection" id="'+currentDate+'"><p><strong>' + displayCurrentDate + '</strong></p></section>\n';
                                }

                                html +=
                                    '<div id="photoThumbnailContainer' + metadata.id + '" class="photo-thumbnail-container photo-thumbnail" style="width:' + metadata.thumbnailSmallWidth + 'px;height:' + metadata.thumbnailSmallHeight + 'px;padding-left:0;padding-right:0;">\n' +
                                    '   <a class="lightGalleryIndexAnchor" name="lightGalleryIndex' + currentMediaLinkIndex + '"></a>\n' +
                                    '   <img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlSmall) + '" class="photo-thumbnail-image" id="image' + metadata.id + '" width="' + metadata.thumbnailSmallWidth + '" height="' + metadata.thumbnailSmallHeight + '" style="background-color:lightgray;" onError="Util.errorImg(this,\''+metadata.title+'\',Util.thumbnailHeight())">\n';

                                const duration = (metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00";
                                html += shashin.getTopRightOverlay(metadata.type, metadata.id, duration, metadata.originalImageWidth, metadata.originalImageHeight, false);

                                if (userMap.showControls === true) {
                                    html += shashin.getTopLeftOverlay(metadata.id);
                                    html += shashin.getBottomLeftOverlay(metadata.id, null, 'albumModalEdit', 'albumSettings.openAlbumModal', 'overlayCommentText');
                                } else {
                                    html += shashin.getBottomLeftOverlay(metadata.id, null, null, null, null);
                                }

                                html +=
                                    '   <div class="thumbnail-br" id="tnbr' + metadata.id + '">\n' +
                                    '       <a href="#" id="favorite' + metadata.id + '" class="text-decoration-none">\n' +
                                    '           <span class="overlayIconBackground">\n' +
                                    '               <span id="briconcount' + metadata.id + '">'+favoriteCount+'</span>&nbsp;<span class="'+favoriteIcon+' overlayIcon" id="brfavoriteicon'+metadata.id+'"></span>\n' +
                                    '           </span>\n' +
                                    '       </a>\n' +
                                    '       <br>' +
                                    '       <a href="#" data-bs-toggle="modal" data-bs-target="#propalbumphotocomment' + metadata.id + '" class="overlayCommentIconBackground overlayCommentText">\n' +
                                    '           <span id="brcommentcount' + metadata.id + '">' + (albumPhotoCommentsMap.hasOwnProperty(metadata.id) ? albumPhotoCommentsMap[metadata.id].length : '0') + '</span> <span id="bricon' + metadata.id + '" class="bi-chat-square position-relative overlayCommentIcon">';

                                if (notificationMap !== null && notificationMap[metadata.id] === true) {
                                    html +=
                                        '           <span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle">\n' +
                                        '               <span class="visually-hidden">New alerts</span>\n' +
                                        '           </span>';
                                }
                                html +=
                                    '           </span>\n' +
                                    '       </a>\n' +
                                    '   </div>\n';

                                const centeredObj = shashin.getCenteredOverlay(metadata,'shashin.openGallery',currentMediaLinkIndex);
                                html += centeredObj.html;
                                mediaContentList.push(centeredObj.mediaContent);

                                html +=
                                    '</div>\n' +
                                    '<span id="albummodal' + metadata.id + '" style="width:0;height:0;padding:0"></span>\n';

                                // Append HTML
                                $(html).insertBefore($(".appendAlbumPhotos").last()).ready(function () {
                                    albumSettings.rendering = false;
                                });

                                // Call JS and modal
                                shashin.setPhotoOverlays(metadata, activePage);
                                albumModal.renderAlbumCommentsModal(albumData, metadata, userMap, albumPhotoCommentsMap);
                                albumSettings.activateAlbumListeners(metadata, albumData);
                                $("#mediaLink" + metadata.id).attr("tag", metadata.id);
                                $("#infoModalEdit"+metadata.id).on("click", function(e) {
                                    e.preventDefault();
                                    shashin.openInfoModal(metadata.id);
                                });
                            }
                        } else {
                            $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                            albumSettings.rendering = false;
                        }
                    }
                } else {
                    $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                    albumSettings.rendering = false;
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    $("#msgTimeline").html(message);
                }
            } else {
                $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                albumSettings.rendering = false;
            }

            $("#spinner").css("display","none");
            return mediaContentList;
        });

        // return promise.done(function(data) {
        //     return data;
        // });
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
            const ajaxParams = {
                type: 'get',
                url: "/notifications/markread/metadata/"+metadata.id,
                contentType: 'application/json; charset=utf-8',
                async:true,
                retries: shashin.ajaxRetries
            }

            $.ajax(ajaxParams).fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating photo comment")});
        });

        shashin.updateFavorites("#favorite","#brfavoriteicon","#briconcount",metadata.id);

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
        const json = {commentId: commentId, metadataId: metadata.id}
        const ajaxParams = {
            type: "post",
            url: "/comment/albumphoto/delete/",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " deleting album photo comment")}).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                let commentId = data["commentId"];
                let message = "Error";
                if (data["status"] === "success") {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                    // Delete comment
                    $("#comment" + commentId).remove();
                    let currentCount = parseInt($("#brcommentcount"+metadata.id).text());
                    if (currentCount > 0) {
                        currentCount--;
                    }
                    $("#brcommentcount"+metadata.id).text(currentCount);
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
            const commentText = $("#commentcontent" + commentId).text();
            $("#textareacontainer" + commentId).html('<textarea class="form-control" id="commenttext' + commentId + '" rows="2">' + commentText + '</textarea>');
        }
    }

    albumSettings.albumCommentsDeleteEditModalListener = function(commentId, metadata) {
        $("#deletecomment" + commentId).on("click", function (e) {
            e.preventDefault();
            albumSettings.deleteComment(commentId, metadata);
        });

        $("#editcomment" + commentId).on("click", function (e) {
            e.preventDefault();
            albumSettings.editComment(commentId, metadata);
        });
    }

    albumSettings.albumCommentsUpdateSaveModalListener = function(metadata, album, userMap) {
        $("#updateCommentMetadata"+metadata.id).hide();
        $("#cancelEditCommentMetadata"+metadata.id).hide();

        $("#cancelEditCommentMetadata"+metadata.id).on("click", function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId" + metadata.id).val();

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

        $("#updateCommentMetadata"+metadata.id).on("click", function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId" + metadata.id).val();

            if (currentCommentId !== "") {
                const updatedComment = $.trim($("#commenttext" + currentCommentId).val());

                if (updatedComment.length > 0) {
                    const json = {commentId: currentCommentId, comment: updatedComment}
                    const ajaxParams = {
                        type: "post",
                        url: "/comment/update",
                        data: JSON.stringify(json),
                        contentType: 'application/json; charset=utf-8',
                        retries: shashin.ajaxRetries
                    }

                    $.ajax(ajaxParams)
                    .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating album photo comment")}).then(function (data) {
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

        $("#saveCommentMetadata"+metadata.id).on("click", function(e) {
            e.preventDefault();

            let comment = $.trim($("#commentText"+metadata.id).val());

            if (comment.length > 0) {
                const json = {metadataId: metadata.id, albumId: album.id, comment: comment};
                const ajaxParams = {
                    type: "post",
                    url: "/comment/albumphoto/save/",
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8',
                    retries: shashin.ajaxRetries
                }

                $.ajax(ajaxParams)
                .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " saving album photo comment")}).then(function (data) {
                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                        let commentId = data["commentId"];
                        let message = "Error";
                        if (data["status"] === "success") {
                            message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        } else {
                            message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        }

                        // Insert comment at top of list
                        const commentItem = '<li class="list-group-item list-group-item-secondary" id="comment' + commentId + '">\n' +
                            '<span id="commentcontainer' + commentId + '">\n<p id="commentcontent' + commentId + '">' + comment + '</p>\n' +
                            '<small>' + userMap.username + '<span style="float: right"><a href="#" id="deletecomment' + commentId + '"><span class="bi-trash"></span></a>&nbsp;&nbsp;<a href="#" id="editcomment' + commentId + '"><span class="bi-pencil"></span></a></span></small></span>' +
                            '<span id="textareacontainer' + commentId + '"></span></li>';
                        $("#commentText"+metadata.id).val("")
                        $("#commentList"+metadata.id).prepend(commentItem);

                        $("#deletecomment" + commentId).on("click", function (e) {
                            e.preventDefault();
                            albumSettings.deleteComment(commentId, metadata);
                        });

                        $("#editcomment" + commentId).on("click", function (e) {
                            e.preventDefault();
                            albumSettings.editComment(commentId, metadata);
                        });

                        let currentCount = parseInt($("#brcommentcount"+metadata.id).text());
                        $("#brcommentcount"+metadata.id).text(currentCount+1)
                    }
                });
            }
        });
    }
}( window.albumSettings = window.albumSettings || {}, jQuery ));

(function( albumModal, $, undefined ) {
    albumModal.renderAlbumCommentsModal = function (albumData,metadata,userMap,albumPhotoCommentsMap) {
        let index;
        let html =
            '<div class="modal fade" id="propalbumphotocomment' + metadata.id + '" tabindex="-1" role="dialog" aria-labelledby="label' + metadata.id + '" aria-hidden="true">\n' +
            '    <div class="modal-dialog modal-lg modal-dialog-scrollable" role="document">\n' +
            '        <div class="modal-content">\n' +
            '            <div class="modal-header">\n' +
            '                <h5 class="modal-title" id="commentModalLabel"><div id="propalbumphotocomment' + metadata.id + '"><img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlCentered) + '" width="100" height="100" onError="Util.errorImg(this,\''+metadata.title+'\',100)"></div>Comments for ' + metadata.fileName + '</h5>\n' +
            '                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>\n' +
            '            </div>\n' +
            '            <div class="modal-body">\n' +
            '                <input type="hidden" id="currentCommentId' + metadata.id + '" name="currentCommentId' + metadata.id + '">\n' +
            '                <ul class="list-group" id="commentList' + metadata.id + '">\n';

        const commentIdArray = [];
        for (index in albumPhotoCommentsMap[metadata.id]) {
            const comments = albumPhotoCommentsMap[metadata.id][index];
            commentIdArray.push(comments["commentId"]);

            html +=
                '       <li id="comment'+comments["commentId"]+'" class="list-group-item'+(comments['userId'] === userMap.id ? ' list-group-item-secondary' : '')+'">\n' +
                '           <span id="commentcontainer'+comments["commentId"]+'">\n' +
                '               <p id="commentcontent'+comments["commentId"]+'">'+comments["comment"]+'</p>\n' +
                '               <small><strong>'+comments["username"]+'</strong><span> on '+comments["createdAt"]+'</span></small>';
            if (comments["userId"] === userMap.id) {
                html +=
                    '       <small><span style="float: right">\n' +
                    '           <a href="#" id="deletecomment'+comments["commentId"]+'"><span class="bi-trash"></span></a>&nbsp;&nbsp;\n' +
                    '           <a href="#" id="editcomment'+comments["commentId"]+'"><span class="bi-pencil"></span></a>\n' +
                    '       </span></small>\n';
            }
            html +=
                '       </span>\n' +
                '       <span id="textareacontainer'+comments["commentId"]+'"></span>\n' +
                '   </li>';
        }

        html +=
            '             </ul>\n' +
            '           </div>\n' +
            '           <div class="modal-footer">\n' +
            '               <textarea class="form-control" id="commentText'+metadata.id+'" rows="2"></textarea>\n' +
            '               <button type="button" class="btn btn-primary" id="saveCommentMetadata'+metadata.id+'">Save</button>\n' +
            '               <button type="button" class="btn btn-primary" id="updateCommentMetadata'+metadata.id+'">Update</button>\n' +
            '               <button type="button" class="btn btn-secondary" id="dismissModalCommentMetadata'+metadata.id+'" data-bs-dismiss="modal">Cancel</button>\n' +
            '               <button type="button" class="btn btn-secondary" id="cancelEditCommentMetadata'+metadata.id+'" data-bs-dismiss="modal">Cancel</button>\n' +
            '           </div>\n' +
            '       </div>\n' +
            '   </div>\n' +
            '</div>';

        $("#albummodal"+metadata.id).after(html);
        for (index in commentIdArray) {
            const commentId = commentIdArray[index];
            albumSettings.albumCommentsDeleteEditModalListener(commentId, metadata);
        }

        albumSettings.albumCommentsUpdateSaveModalListener(metadata, albumData, userMap);
    }
}( window.albumModal = window.albumModal || {}, jQuery ));