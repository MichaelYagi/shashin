(function( albumsCommentsSettings, $, undefined ) {
    albumsCommentsSettings.deleteComment = async function (commentId, albumId) {
        const http = new Http("delete comment");
        let json = {commentId: commentId, albumId: albumId}
        const data = await http.ajax("post", "/comment/album/delete", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId") && data.hasOwnProperty("commentCount")) {
            let commentId = data["commentId"];
            let commentCount = data["commentCount"];
            if (data["status"] === "success") {
                // Delete comment
                $("#comment" + commentId).remove();
                $("#commentcount" + albumId).text(commentCount);
            }
        }

        $("#currentCommentId" + albumId).val("");
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
        $("#deleteAlbum"+albumId).on("click", async function (e) {
            e.preventDefault();

            const http = new Http("delete album");
            let json = {albumId: albumId, delete: true}
            const data = await http.ajax("post", "/album/delete/" + albumId, JSON.stringify(json));

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
    }

    albumsModalListeners.setEditAlbumsListeners = function (albumId) {
        $("#editAlbum"+albumId).on("click", async function (e) {
            e.preventDefault();

            $("#editAlbumNameStatus" + albumId).removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#editAlbumNameStatus" + albumId).css("visibility", "visible");
            $("#editAlbumNameStatus" + albumId).attr("title", "");
            $("#cancelAlbum" + albumId).prop('disabled', true);

            const http = new Http("edit album");
            const albumName = $("#albumEditName" + albumId).val();
            let json = {albumId: albumId, albumName: Util.htmlDecode(albumName)}
            const data = await http.ajax("post", "/album/updatename/" + albumId, JSON.stringify(json), function () {
                $("#editAlbumNameStatus" + albumId).removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#editAlbumNameStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
                $("#cancelAlbum" + albumId).prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data["status"] === "success") {
                $("#albumName" + albumId).text(albumName);
                $("#editAlbumNameStatus" + albumId).addClass('bi-check-circle').removeClass('spinner-grow');
            } else {
                $("#editAlbumNameStatus" + albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                $("#editAlbumNameStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
            }

            $("#cancelAlbum" + albumId).prop('disabled', false);
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

        $("#clearLink"+albumId).on("click", function (e) {
            e.preventDefault();
            albumsModalSettings.updateShareLink(baseUrl, albumId, "clear");
        });

        $("#generateLink"+albumId).on("click", function (e) {
            e.preventDefault();
            albumsModalSettings.updateShareLink(baseUrl, albumId, "generate");
        });

        $("#propsharealbums"+albumId).on('hide.bs.modal', function () {
            $("#albumsModalStatus"+albumId).attr("class","spinner-grow me-auto");
            $("#albumsModalStatus"+albumId).css("visibility","hidden");
            $("#msg"+albumId).html("");
        })

        $("#copyLink"+albumId).on("click", function (e) {
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

        $("#saveUserShare"+albumId).on("click", async function (e) {
            e.preventDefault();

            $("#albumsModalStatus" + albumId).removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#albumsModalStatus" + albumId).css("visibility", "visible");
            $("#albumsModalStatus" + albumId).attr("title", "");
            $("#cancelUserShare" + albumId).prop('disabled', true);

            let userShareMap = {};
            $('input[name^="userShare' + albumId + '"]').each(function () {
                let checkboxId = $(this).attr('id');
                let isChecked = $(this).prop("checked");

                let checkboxIdArray = checkboxId.split("-");
                let userId = checkboxIdArray[1];
                userShareMap[userId] = isChecked
            });

            const http = new Http("share album");
            let json = {albumId: albumId, userShareMap: JSON.stringify(userShareMap)}
            const data = await http.ajax("post", "/album/share/" + albumId, JSON.stringify(json), function () {
                $("#albumsModalStatus" + albumId).removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#albumsModalStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
                $("#cancelUserShare" + albumId).prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === "success") {
                    $("#albumsModalStatus" + albumId).addClass('bi-check-circle').removeClass('spinner-grow');
                    $("#cancelUserShare" + albumId).prop('disabled', false);
                } else {
                    $("#albumsModalStatus" + albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#albumsModalStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
                    $("#cancelUserShare" + albumId).prop('disabled', false);
                }
            } else {
                $("#albumsModalStatus" + albumId).addClass('bi-x-circle').removeClass('spinner-grow');
                $("#albumsModalStatus" + albumId).attr("title", shashin.modalStatusFailMessage());
                $("#cancelUserShare" + albumId).prop('disabled', false);
            }
        });
    }

    albumsModalListeners.setCommentModalListeners = function (albumId, username) {
        $("#propcommentalbums" + albumId).on('show.bs.modal', async function () {
            const http = new Http("album notification read");
            const data = await http.ajax("get", "/notifications/markread/album/" + albumId);
        })

        $("#updateCommentAlbum" + albumId).hide();
        $("#cancelEditCommentAlbum" + albumId).hide();

        $("#cancelEditCommentAlbum" + albumId).on("click", function (e) {
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

        $("#updateCommentAlbum" + albumId).on("click", async function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId" + albumId).val();

            if (currentCommentId !== "") {
                const updatedComment = $.trim($("#commenttext" + currentCommentId).val());

                if (updatedComment.length > 0) {
                    const http = new Http("update comment");
                    let json = {commentId: currentCommentId, comment: updatedComment}
                    const data = await http.ajax("post", "/comment/update", JSON.stringify(json));

                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                        let commentId = data["commentId"];

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

        $("#saveCommentAlbum" + albumId).on("click", async function (e) {
            e.preventDefault();

            let comment = $.trim($("#commentText" + albumId).val());

            if (comment.length > 0) {
                const http = new Http("save comment");
                let json = {albumId: albumId, comment: comment}
                const data = await http.ajax("post", "/comment/album/save", JSON.stringify(json));

                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId") && data.hasOwnProperty("commentCount")) {
                    let commentId = data["commentId"];
                    let commentCount = data["commentCount"];

                    if (data["status"] === "success") {
                        $("#commentcount" + albumId).text(commentCount);

                        // Insert comment at top of list
                        const commentItem = '<li class="list-group-item list-group-item-secondary" id="comment' + commentId + '">\n' +
                            '<span id="commentcontainer' + commentId + '">\n<p id="commentcontent' + commentId + '">' + comment + '</p>\n' +
                            '<small>' + username + '<span style="float: right"><a href="#" id="deletecomment' + commentId + '"><span class="bi-trash"></span></a>&nbsp;&nbsp;<a href="#" id="editcomment' + commentId + '"><span class="bi-pencil"></span></a></span></small></span>' +
                            '<span id="textareacontainer' + commentId + '"></span></li>';
                        $("#commentText" + albumId).val("")
                        $("#commentList" + albumId).prepend(commentItem);

                        $("#deletecomment" + commentId).on("click", function (e) {
                            e.preventDefault();
                            albumsCommentsSettings.deleteComment(commentId, albumId);
                        });

                        $("#editcomment" + commentId).on("click", function (e) {
                            e.preventDefault();
                            albumsCommentsSettings.editComment(albumId, commentId);
                        });
                    }
                }
            }
        });
    }

    albumsModalListeners.setEditCommentModalListeners = function (commentId, albumId) {
        $("#deletecomment"+commentId).on("click", function (e) {
            e.preventDefault();
            albumsCommentsSettings.deleteComment(commentId, albumId);
        });

        $("#editcomment"+commentId).on("click", function (e) {
            e.preventDefault();
            albumsCommentsSettings.editComment(albumId, commentId);
        });
    }
}(window.albumsModalListeners = window.albumsModalListeners || {}, jQuery));