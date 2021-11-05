(function( albumsCommentsSettings, $, undefined ) {
    albumsCommentsSettings.deleteComment = function(commentId, albumId) {

        let json = {commentId: commentId,albumId: albumId}
        const ajaxParams = {
            type: "post",
            url: "/comment/album/delete",
            data: JSON.stringify(json),
            contentType: 'application/json; charset=utf-8',
            retries: 3
        }

        function onFail(xhr, textStatus) {
            shashin.printMessageToConsole("AJAX error deleting album comment. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
            if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                $.ajax(ajaxParams).fail(onFail);
            }
        }

        $.ajax(ajaxParams)
        .fail(onFail).then(function (data) {
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
                retries: 3
            }

            function onFail(xhr, textStatus) {
                shashin.printMessageToConsole("AJAX error deleting album. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
                if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                    $.ajax(ajaxParams).fail(onFail);
                }
            }

            $.ajax(ajaxParams)
            .fail(onFail).then(function (data) {
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

        $("#propalbums"+albumId).on('hide.bs.modal', function () {
            $("#albumsModalStatus").attr("class","spinner-grow me-auto");
            $("#albumsModalStatus").css("visibility","hidden");
            $("#msg"+albumId).html("");
        })

        $("#copyLink"+albumId).click(function (e) {
            e.preventDefault();

            const shareLink = $("#shareLink"+albumId).val().trim();

            if (shareLink !== "") {
                const fullShareLink = baseUrl+ "share/" + shareLink + "/album/"+albumId;
                shashin.copyTextToClipboard(fullShareLink, albumId);
            } else {
                $("#msg"+albumId).html("<div class=\"alert alert-warning\" role=\"alert\">Link must not be blank</div>");
            }
        });

        $("#saveUserShare"+albumId).click(function (e) {
            e.preventDefault();
            $("#albumsModalStatus").css("visibility","visible");

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
                retries: 3
            }

            function onFail(xhr, textStatus) {
                shashin.printMessageToConsole("AJAX saving user share. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
                if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                    $.ajax(ajaxParams).fail(onFail);
                }
            }

            $.ajax(ajaxParams)
            .fail(onFail).then(function (data) {
                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    let message = "Error";
                    if (data["status"] === "success") {
                        message = '<div class="alert alert-success" role="alert">' + data["msg"] + '</div>';
                        $("#albumsModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                    } else {
                        message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                        $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                    }
                    //$("#albumsMessage").html(message);
                }

                //$("#propalbums"+albumId).modal('hide');
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
                retries: 3
            }

            function onFail(xhr, textStatus) {
                shashin.printMessageToConsole("AJAX error marking notification on album comment. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
                if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                    $.ajax(ajaxParams).fail(onFail);
                }
            }

            $.ajax(ajaxParams).fail(onFail);
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
                        retries: 3
                    }

                    function onFail(xhr, textStatus) {
                        shashin.printMessageToConsole("AJAX error updating album comment. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
                        if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                            $.ajax(ajaxParams).fail(onFail);
                        }
                    }

                    $.ajax(ajaxParams)
                    .fail(onFail).then(function (data) {
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
                    retries: 3
                }

                function onFail(xhr, textStatus) {
                    shashin.printMessageToConsole("AJAX error saving album comment. Attempts left: "+ajaxParams.retries + ". Status: " + xhr.status + ". Text Status: " + textStatus + ".");
                    if ((textStatus === 'timeout' || textStatus === 'error' || xhr.status !== 200) && ajaxParams.retries-- > 0) {
                        $.ajax(ajaxParams).fail(onFail);
                    }
                }

                $.ajax(ajaxParams)
                .fail(onFail).then(function (data) {
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