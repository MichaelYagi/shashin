(function( albumsCommentsSettings, $, undefined ) {
    albumsCommentsSettings.deleteComment = function(commentId, albumId) {

        let json = {commentId: commentId,albumId: albumId}
        const ajaxParams = {
            type: "post",
            url: "/comment/album/delete",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: shashin.ajaxRetries
        }

        $.ajax(ajaxParams)
        .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " deleting album comment")}).then(function (data) {
            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId") && data.hasOwnProperty("commentCount")) {
                let commentId = data["commentId"];
                let commentCount = data["commentCount"];
                let message = "Error";
                if (data["status"] === "success") {
                    message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';

                    // Delete comment
                    $("#comment"+commentId).remove();

                    $("#commentcount"+albumId).text(commentCount);
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                }
            }
        });

        $("#currentCommentId"+albumId).val("");
    }

    albumsCommentsSettings.editComment = function(albumId,commentId) {
        if ($("#currentCommentId"+albumId).val() === "") {
            $("#currentCommentId"+albumId).val(commentId);

            $("#saveCommentAlbum"+albumId).hide();
            $("#dismissModalCommentAlbum"+albumId).hide();
            $("#updateCommentAlbum"+albumId).show();
            $("#cancelEditCommentAlbum"+albumId).show();

            $("#commentcontainer"+commentId).hide();
            $("#textareacontainer"+commentId).show();
            const commentText = $("#commentcontent" + commentId).text();
            $("#textareacontainer"+commentId).html('<textarea class="form-control" id="commenttext' + commentId + '" rows="2">'+commentText+'</textarea>');
        }
    }
}( window.albumsCommentsSettings = window.albumsCommentsSettings || {}, jQuery ));

(function (albumsModalListeners, $, undefined) {
    albumsModalListeners.setDeleteAlbumsListeners = function (albumId) {
        $("#deleteAlbum"+albumId).click(function(e) {
            e.preventDefault();

            let json = {albumId: albumId, delete: true}
            const ajaxParams = {
                type: "post",
                url: "/album/delete/" + albumId,
                data: JSON.stringify(json),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }

            $.ajax(ajaxParams)
            .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " deleting album")}).then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let message = "Error";
                    if (data["status"] === "success") {
                        message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        window.top.location = window.top.location
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                    }
                    $("#albumsMessage").html(message);
                }
            });

        });
    }

    albumsModalListeners.setEditAlbumsListeners = function (albumId) {
        $("#editAlbum"+albumId).click(function(e) {
            e.preventDefault();

            $("#editAlbumNameStatus"+albumId).removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#editAlbumNameStatus"+albumId).css("visibility","visible");
            $("#editAlbumNameStatus"+albumId).attr("title","");
            $("#cancelAlbum"+albumId).prop('disabled', true);

            const albumName = $("#albumEditName"+albumId).val();

            let json = {albumId: albumId, albumName: albumName}
            const ajaxParams = {
                type: "post",
                url: "/album/updatename/"+albumId,
                data: JSON.stringify(json),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }

            $.ajax(ajaxParams)
                .fail(function(xhr, textStatus) {
                    $("#editAlbumNameStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#editAlbumNameStatus"+albumId).attr("title",shashin.modalStatusFailMessage());
                    shashin.onFail(xhr, textStatus, ajaxParams, " updating album name");
                }).then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data["status"] === "success") {
                    $("#albumName"+albumId).text(albumName);
                    $("#editAlbumNameStatus"+albumId).addClass('bi-check-circle').removeClass('spinner-grow');
                } else {
                    $("#editAlbumNameStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#editAlbumNameStatus"+albumId).attr("title",shashin.modalStatusFailMessage());
                }

                $("#cancelAlbum"+albumId).prop('disabled', false);
            });
        });

        $('#propeditalbums'+albumId).on('hide.bs.modal', function () {
            $("#editAlbumNameStatus"+albumId).addClass('spinner-grow').removeClass('bi-check-circle').removeClass('bi-x-circle');
            $("#editAlbumNameStatus"+albumId).css("visibility","hidden");
            $("#albumEditName"+albumId).val("");
        });

        $('#propeditalbums'+albumId).on('show.bs.modal', function () {
            $("#albumEditName"+albumId).val($("#albumName"+albumId).text());
        });
    }

    albumsModalListeners.setAlbumModalListeners = function (albumId, baseUrl) {
        if ($("#shareLink"+albumId).val() === "") {
            $("#copyLink"+albumId).prop('disabled', true);
        }

        $("#clearLink"+albumId).click(function (e) {
            e.preventDefault();
            albumsModalSettings.updateShareLink(baseUrl, albumId, "clear");
        });

        $("#generateLink"+albumId).click(function (e) {
            e.preventDefault();
            albumsModalSettings.updateShareLink(baseUrl, albumId, "generate");
        });

        $("#propsharealbums"+albumId).on('hide.bs.modal', function () {
            $("#albumsModalStatus"+albumId).attr("class","spinner-grow me-auto");
            $("#albumsModalStatus"+albumId).css("visibility","hidden");
            $("#msg"+albumId).html("");
        })

        $("#copyLink"+albumId).click(function (e) {
            e.preventDefault();

            const shareLink = $("#copyLink"+albumId).attr("data-clipboard-text");

            if (shareLink !== null && shareLink !== "") {
                const clipboard = new ClipboardJS("#copyLink" + albumId,{container: document.getElementById("propsharealbums"+albumId)});

                clipboard.on('success', function (e) {
                    $("#msg" + albumId).html("<div class=\"alert alert-success\" role=\"alert\">Link copied to clipboard!</div>");
                });

                clipboard.on('error', function (e) {
                    $("#msg" + albumId).html("<div class=\"alert alert-warning\" role=\"alert\">Could not copy text</div>");
                });
            } else {
                $("#msg"+albumId).html("<div class=\"alert alert-warning\" role=\"alert\">Link must not be blank</div>");
            }
        });

        $("#saveUserShare"+albumId).click(function (e) {
            e.preventDefault();

            $("#albumsModalStatus"+albumId).removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#albumsModalStatus"+albumId).css("visibility","visible");
            $("#albumsModalStatus"+albumId).attr("title", "");
            $("#cancelUserShare"+albumId).prop('disabled', true);

            let userShareMap = {};
            $('input[name^="userShare'+albumId+'"]').each(function () {
                let checkboxId = $(this).attr('id');
                let isChecked = $(this).prop("checked");

                let checkboxIdArray = checkboxId.split("-");
                let userId = checkboxIdArray[1];
                userShareMap[userId] = isChecked
            });

            let json = {albumId: albumId, userShareMap: JSON.stringify(userShareMap)}
            const ajaxParams = {
                type: "post",
                url: "/album/share/" + albumId,
                data: JSON.stringify(json),
                contentType: 'application/json; charset=utf-8',
                retries: shashin.ajaxRetries
            }

            $.ajax(ajaxParams)
            .fail(function(xhr, textStatus) {
                $("#albumsModalStatus"+albumId).attr("title", shashin.modalStatusFailMessage());
                $("#cancelUserShare"+albumId).prop('disabled', false);
                shashin.onFail(xhr, textStatus, ajaxParams, " saving user share");
            }).then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let message = "Error";
                    if (data["status"] === "success") {
                        message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        $("#albumsModalStatus"+albumId).addClass('bi-check-circle').removeClass('spinner-grow');
                        $("#cancelUserShare"+albumId).prop('disabled', false);
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        $("#albumsModalStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                        $("#albumsModalStatus"+albumId).attr("title", shashin.modalStatusFailMessage());
                        $("#cancelUserShare"+albumId).prop('disabled', false);
                    }
                    //$("#albumsMessage").html(message);
                } else {
                    $("#albumsModalStatus"+albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#albumsModalStatus"+albumId).attr("title", shashin.modalStatusFailMessage());
                    $("#cancelUserShare"+albumId).prop('disabled', false);
                }

                //$("#propsharealbums"+albumId).modal('hide');
            });
        });
    }

    albumsModalListeners.setCommentModalListeners = function (albumId, username) {
        $("#propcommentalbums" + albumId).on('show.bs.modal', function () {
            const ajaxParams = {
                type: 'get',
                url: "/notifications/markread/album/" + albumId,
                contentType: 'application/json; charset=utf-8',
                async: true,
                retries: shashin.ajaxRetries
            }

            $.ajax(ajaxParams).fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " marking notification on album comment")});
        })

        $("#updateCommentAlbum" + albumId).hide();
        $("#cancelEditCommentAlbum" + albumId).hide();

        $("#cancelEditCommentAlbum" + albumId).click(function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId" + albumId).val();

            if (currentCommentId !== "") {
                $("#saveCommentAlbum" + albumId).show();
                $("#dismissModalCommentAlbum" + albumId).show();
                $("#updateCommentAlbum" + albumId).hide();
                $("#cancelEditCommentAlbum" + albumId).hide();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId" + albumId).val("");
            }
        });

        $("#updateCommentAlbum" + albumId).click(function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId" + albumId).val();

            if (currentCommentId !== "") {
                const updatedComment = $.trim($("#commenttext" + currentCommentId).val());

                if (updatedComment.length > 0) {
                    let json = {commentId: currentCommentId, comment: updatedComment}
                    const ajaxParams = {
                        type: "post",
                        url: "/comment/update",
                        data: JSON.stringify(json),
                        contentType: 'application/json; charset=utf-8',
                        retries: shashin.ajaxRetries
                    }

                    $.ajax(ajaxParams)
                    .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " updating album comment")}).then(function (data) {
                        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                            let commentId = data["commentId"];
                            let message = "Error";
                            if (data["status"] === "success") {
                                message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                            } else {
                                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                            }

                            // Update comment
                            $("#commentcontent" + commentId).text(updatedComment);

                            $("#saveCommentAlbum" + albumId).show();
                            $("#dismissModalCommentAlbum" + albumId).show();
                            $("#updateCommentAlbum" + albumId).hide();
                            $("#cancelEditCommentAlbum" + albumId).hide();

                            $("#commentcontainer" + commentId).show();
                            $("#textareacontainer" + commentId).html('');
                            $("#textareacontainer" + commentId).hide();
                            $("#commenttext" + commentId).val("");
                        }
                    });
                }

                $("#saveCommentAlbum" + albumId).show();
                $("#dismissModalCommentAlbum" + albumId).show();
                $("#updateCommentAlbum" + albumId).hide();
                $("#cancelEditCommentAlbum" + albumId).hide();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId" + albumId).val("");
            }
        });

        $("#saveCommentAlbum" + albumId).click(function (e) {
            e.preventDefault();

            let comment = $.trim($("#commentText" + albumId).val());

            if (comment.length > 0) {
                let json = {albumId: albumId, comment: comment}
                const ajaxParams = {
                    type: "post",
                    url: "/comment/album/save",
                    data: JSON.stringify(json),
                    contentType: 'application/json; charset=utf-8',
                    retries: shashin.ajaxRetries
                }

                $.ajax(ajaxParams)
                .fail(function(xhr, textStatus) {shashin.onFail(xhr, textStatus, ajaxParams, " saving album comment")}).then(function (data) {
                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId") && data.hasOwnProperty("commentCount")) {
                        let commentId = data["commentId"];
                        let commentCount = data["commentCount"];

                        let message = "Error";
                        if (data["status"] === "success") {
                            $("#commentcount"+albumId).text(commentCount);

                            // Insert comment at top of list
                            const commentItem = '<li class="list-group-item list-group-item-secondary" id="comment' + commentId + '">\n' +
                                '<span id="commentcontainer' + commentId + '">\n<p id="commentcontent' + commentId + '">' + comment + '</p>\n' +
                                '<small>'+username+'<span style="float: right"><a href="#" id="deletecomment' + commentId + '"><span class="bi-trash"></span></a>&nbsp;&nbsp;<a href="#" id="editcomment' + commentId + '"><span class="bi-pencil"></span></a></span></small></span>' +
                                '<span id="textareacontainer' + commentId + '"></span></li>';
                            $("#commentText" + albumId).val("")
                            $("#commentList" + albumId).prepend(commentItem);

                            $("#deletecomment" + commentId).click(function (e) {
                                e.preventDefault();
                                albumsCommentsSettings.deleteComment(commentId, albumId);
                            });

                            $("#editcomment" + commentId).click(function (e) {
                                e.preventDefault();
                                albumsCommentsSettings.editComment(albumId, commentId);
                            });

                            message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        } else {
                            message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        }

                        //$("#albumCommentMessage"+albumId).html(message);
                    }
                });
            }
        });
    }

    albumsModalListeners.setEditCommentModalListeners = function (commentId, albumId) {
        $("#deletecomment"+commentId).click(function (e) {
            e.preventDefault();
            albumsCommentsSettings.deleteComment(commentId, albumId);
        });

        $("#editcomment"+commentId).click(function (e) {
            e.preventDefault();
            albumsCommentsSettings.editComment(albumId, commentId);
        });
    }
}(window.albumsModalListeners = window.albumsModalListeners || {}, jQuery));