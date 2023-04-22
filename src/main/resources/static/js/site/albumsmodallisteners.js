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

        $("#currentCommentId").val("");
    }

    albumsCommentsSettings.editComment = function(albumId,commentId) {
        if ($("#currentCommentId").val() === "") {
            $("#currentCommentId").val(commentId);

            $("#saveCommentAlbum").hide();
            $("#dismissModalCommentAlbum").hide();
            $("#updateCommentAlbum").show();
            $("#cancelEditCommentAlbum").show();

            $("#commentcontainer"+commentId).hide();
            $("#textareacontainer"+commentId).show();
            const commentText = $("#commentcontent" + commentId).text();
            $("#textareacontainer"+commentId).html('<textarea class="form-control" id="commenttext' + commentId + '" rows="2">'+commentText+'</textarea>');
        }
    }
}( window.albumsCommentsSettings = window.albumsCommentsSettings || {}, jQuery ));

(function (albumsModalListeners, $, undefined) {
    albumsModalListeners.setDeleteAlbumsListeners = function (albumId) {
        $("#deleteAlbum").on("click", async function (e) {
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
        $("#editAlbum").on("click", async function (e) {
            e.preventDefault();

            $("#editAlbumNameStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#editAlbumNameStatus").css("visibility", "visible");
            $("#editAlbumNameStatus").attr("title", "");
            $("#cancelAlbum").prop('disabled', true);

            const http = new Http("edit album");
            const albumName = $("#albumEditName").val();
            let json = {albumId: albumId, albumName: Util.htmlDecode(albumName)}
            const data = await http.ajax("post", "/album/updatename/" + albumId, JSON.stringify(json), function () {
                $("#editAlbumNameStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#editAlbumNameStatus").attr("title", shashin.modalStatusFailMessage());
                $("#cancelAlbum").prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data["status"] === "success") {
                $("#albumName").text(albumName);
                $("#albumNameEdit").text(albumName);
                $("#albumName"+albumId).text(albumName);
                $("#editAlbumNameStatus").addClass('bi-check-circle').removeClass('spinner-grow');
            } else {
                $("#editAlbumNameStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#editAlbumNameStatus").attr("title", shashin.modalStatusFailMessage());
            }

            $("#cancelAlbum").prop('disabled', false);
        });

        $('#propeditalbums').on('hide.bs.modal', function () {
            $("#editAlbumNameStatus").addClass('spinner-grow').removeClass('bi-check-circle').removeClass('bi-x-circle');
            $("#editAlbumNameStatus").css("visibility","hidden");
            $("#albumEditName").val("");
        });

        $('#propeditalbums').on('show.bs.modal', function () {
            $("#albumEditName").val($("#albumName").text());
        });
    }

    albumsModalListeners.setAlbumModalListeners = function (albumId, baseUrl) {
        if ($("#shareLink").val() === "") {
            $("#copyLink").prop('disabled', true);
        }

        $("#clearLink").on("click", function (e) {
            e.preventDefault();
            albumsModalSettings.updateShareLink(baseUrl, albumId, "clear");
        });

        $("#generateLink").on("click", function (e) {
            e.preventDefault();
            albumsModalSettings.updateShareLink(baseUrl, albumId, "generate");
        });

        $("#propsharealbums").on('hide.bs.modal', function () {
            $("#albumsModalStatus").attr("class","spinner-grow me-auto");
            $("#albumsModalStatus").css("visibility","hidden");
            $("#msg").html("");
        })

        $("#copyLink").on("click", function (e) {
            e.preventDefault();

            const shareLink = $("#copyLink").attr("data-clipboard-text");

            if (shareLink !== null && shareLink !== "") {
                const clipboard = new ClipboardJS("#copyLink",{container: document.getElementById("propsharealbums")});

                clipboard.on('success', function (e) {
                    $("#msg").html("<div class=\"alert alert-success\" role=\"alert\">Link copied to clipboard!</div>");
                });

                clipboard.on('error', function (e) {
                    $("#msg").html("<div class=\"alert alert-warning\" role=\"alert\">Could not copy text</div>");
                });
            } else {
                $("#msg").html("<div class=\"alert alert-warning\" role=\"alert\">Link must not be blank</div>");
            }
        });

        $("#saveUserShare").on("click", async function (e) {
            e.preventDefault();

            $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#albumsModalStatus").css("visibility", "visible");
            $("#albumsModalStatus").attr("title", "");
            $("#cancelUserShare").prop('disabled', true);

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
                $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#cancelUserShare").prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data["status"] === "success") {
                    $("#albumsModalStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                    $("#cancelUserShare").prop('disabled', false);
                } else {
                    $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                    $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                    $("#cancelUserShare").prop('disabled', false);
                }
            } else {
                $("#albumsModalStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#cancelUserShare").prop('disabled', false);
            }
        });
    }

    albumsModalListeners.setCommentModalListeners = function (albumId, username) {
        $("#propcommentalbums").on('show.bs.modal', async function () {
            const http = new Http("album notification read");
            const data = await http.ajax("get", "/notifications/markread/album/" + albumId);
        })

        $("#updateCommentAlbum").hide();
        $("#cancelEditCommentAlbum").hide();

        $("#cancelEditCommentAlbum").on("click", function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId").val();

            if (currentCommentId !== "") {
                $("#saveCommentAlbum").show();
                $("#dismissModalCommentAlbum").show();
                $("#updateCommentAlbum").hide();
                $("#cancelEditCommentAlbum").hide();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId").val("");
            }
        });

        $("#updateCommentAlbum").on("click", async function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId").val();

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

                        $("#saveCommentAlbum").show();
                        $("#dismissModalCommentAlbum").show();
                        $("#updateCommentAlbum").hide();
                        $("#cancelEditCommentAlbum").hide();

                        $("#commentcontainer" + commentId).show();
                        $("#textareacontainer" + commentId).html('');
                        $("#textareacontainer" + commentId).hide();
                        $("#commenttext" + commentId).val("");
                    }
                }

                $("#saveCommentAlbum").show();
                $("#dismissModalCommentAlbum").show();
                $("#updateCommentAlbum").hide();
                $("#cancelEditCommentAlbum").hide();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId").val("");
            }
        });

        $("#saveCommentAlbum").on("click", async function (e) {
            e.preventDefault();

            let comment = $.trim($("#commentText").val());

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
                        $("#commentText").val("")
                        $("#commentList").prepend(commentItem);

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
            $("#currentCommentId").val("");
            albumsCommentsSettings.editComment(albumId, commentId);
        });
    }
}(window.albumsModalListeners = window.albumsModalListeners || {}, jQuery));