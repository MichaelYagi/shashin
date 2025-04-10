(function( albumsCommentsSettings, $, undefined ) {
    albumsCommentsSettings.deleteComment = async function (commentId, albumId) {
        const http = new Http("delete comment");
        let json = {commentId: commentId, albumId: albumId};
        const data = await http.ajax("delete", "/comment/album/delete", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId") && data.hasOwnProperty("commentCount")) {
            let commentId = data.commentId;
            let commentCount = data.commentCount;
            if (data.status === shashin.apiResponse.SUCCESS) {
                // Delete comment
                $("#comment" + commentId).remove();
                $("#commentcount" + albumId).text(commentCount);

                if (commentCount > 0) {
                    $("#commentListContainer").css("display","block");
                } else {
                    $("#commentListContainer").css("display","none");
                }
            }
        }

        $("#currentCommentId").val("");
    };

    albumsCommentsSettings.editComment = function(albumId,commentId) {
        if ($("#currentCommentId").val() === "") {
            $("#currentCommentId").val(commentId);

            $("#saveCommentAlbum").hide();
            $("#dismissModalCommentAlbum").hide();
            $("#updateCommentAlbum").show();
            $("#cancelEditCommentAlbum").show();

            $("#comment" + commentId).siblings().find('a').hide();

            $("#commentcontainer"+commentId).hide();
            $("#textareacontainer"+commentId).show();
            const commentText = $("#commentcontent" + commentId).html();
            $("#textareacontainer"+commentId).html('<textarea class="form-control" id="commenttext' + commentId + '" rows="2" placeholder="Comment">'+commentText+'</textarea>');
        }
    };
}( window.albumsCommentsSettings = window.albumsCommentsSettings || {}, jQuery ));

(function (albumsModalListeners, $, undefined) {
    albumsModalListeners.setDeleteAlbumsListeners = function (albumId) {
        $("#proptrashalbums").on('hide.bs.modal', function () {
            $("#deleteAlbum").off();

            $("#proptrashalbums").off();
        });

        $("#deleteAlbum").on("click", async function (e) {
            e.preventDefault();
            await deleteAlbum();
        });

        // $(document).keypress(async function (e) {
        //     const key = e.which;
        //     if(key === 13) {
        //         await deleteAlbum();
        //     }
        // });

        async function deleteAlbum() {
            const http = new Http("delete album");
            let json = {albumId: albumId};
            const data = await http.ajax("delete", "/album/delete", JSON.stringify(json));

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                let message = "Error";
                if (data.status === shashin.apiResponse.SUCCESS) {
                    message = '<div class="alert alert-success" role="alert">' + data.msg + '</div>';
                    window.top.location = window.top.location;
                } else {
                    message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
                }
                $("#albumsMessage").html(message);
            }
        }
    };

    albumsModalListeners.setEditAlbumsListeners = function (albumId) {
        $("#propeditalbums").on('hide.bs.modal', function () {
            $("#editAlbum").off();

            $("#propeditalbums").off();
            $("#originalAlbumName").val("");
        });

        $("#editAlbum").on("click", async function (e) {
            e.preventDefault();
            await editAlbum();
        });

        $("#editAlbumsModal").on("keypress", async function (e) {
            const albumName = $("#albumEditName").val();
            const originalAlbumName = $("#originalAlbumName").val();

            if (e.key === "Enter" && originalAlbumName.trim() !== albumName.trim()) {
                e.preventDefault();
                await editAlbum();
            }
        });

        $("#albumEditName").on("keyup", async function (e) {
            const albumName = $("#albumEditName").val();
            const originalAlbumName = $("#originalAlbumName").val();

            if (originalAlbumName.trim() !== albumName.trim() && albumName.trim() !== "") {
                $("#editAlbum").prop('disabled', false);
            } else {
                $("#editAlbum").prop('disabled', true);
            }
        });

        async function editAlbum() {
            const albumName = $("#albumEditName").val();
            const originalAlbumName = $("#originalAlbumName").val();

            if (originalAlbumName.trim() !== albumName.trim() && albumName.trim() !== "") {
                $("#editAlbumNameStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
                $("#editAlbumNameStatus").visible();
                $("#editAlbumNameStatus").attr("title", "");
                $("#cancelAlbum").prop('disabled', true);

                const http = new Http("edit album");

                let json = {albumId: albumId, albumName: Util.htmlDecode(albumName)};
                const data = await http.ajax("post", "/album/updatename/" + albumId, JSON.stringify(json), function () {
                    $("#editAlbumNameStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                    $("#editAlbumNameStatus").attr("title", shashin.modalStatusFailMessage());
                    $("#cancelAlbum").prop('disabled', false);
                });

                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                    if (data.status === shashin.apiResponse.SUCCESS) {
                        $("#albumName").text(albumName);
                        $("#albumNameEdit").text(albumName);
                        $("#albumName" + albumId).text(albumName);
                        $("#editAlbumNameStatus").addClass('bi-check-circle').removeClass('spinner-grow');
                        $("#editAlbum").prop('disabled', true);
                        $("#originalAlbumName").val(albumName);
                    } else {
                        shashin.showToastMessage("Could not edit album", data.msg, {
                            icon: "bi-exclamation-triangle",
                            iconColor: "#FF0000",
                            borderColor:"danger"
                        });
                        $("#editAlbumNameStatus").addClass('bi-x-circle').removeClass('spinner-grow');
                        $("#editAlbumNameStatus").attr("title", data.msg);
                    }
                } else {
                    shashin.showToastMessage("Could not edit album", "Something went wrong", {
                        icon: "bi-exclamation-triangle",
                        iconColor: "#FF0000",
                        borderColor:"danger"
                    });
                }

                $("#cancelAlbum").prop('disabled', false);
            }
        }
    };

    albumsModalListeners.setAlbumModalListeners = function () {
        $("#propsharealbums").on('hide.bs.modal', function () {
            // $("#clearLink").off();
            // $("#generateLink").off();
            // $("#copyLink").off();
            // $("#saveUserShare").off();

            $("#albumsModalStatus").attr("class","spinner-grow me-auto");
            $("#albumsModalStatus").invisible();
            $("#msg").html("");
            // $("#propsharealbums").off();
        });

        $("#propsharealbums").on('shown.bs.modal', function () {
            if ($("#shareLink").val() === "") {
                $("#copyLink").prop('disabled', true);
                $("#clearLink").prop('disabled', true);
            }
        });

        $("#clearLink").on("click", function (e) {
            if ($("#shareLink").val() !== "") {
                e.preventDefault();
                $("#shareConfirmationModalInfo").show();

                const action = "clear";
                $("#shareConfirmationModalTitle").text(action.charAt(0).toUpperCase() + action.slice(1));
                $("#shareConfirmationModalAction").text(action);
                $("#shareConfirmationAction").val(action);
                $("#shareConfirmationModal").modal('show');
            }
        });

        $("#generateLink").on("click", function (e) {
            e.preventDefault();
            $("#shareConfirmationModalInfo").hide();

            const action = "generate";
            $("#shareConfirmationModalTitle").text(action.charAt(0).toUpperCase() + action.slice(1));
            $("#shareConfirmationModalAction").text(action);
            $("#shareConfirmationAction").val(action);
            $("#shareConfirmationModal").modal('show');
            if ($("#shareLink").val() !== "") {
                $("#shareConfirmationModalInfo").show();
                $("#shareConfirmationModalAction").text("re" + action);
            }
        });

        $("#shareConfirmation").on("click", function (e) {
            e.preventDefault();
            const albumId = $("#currentAlbumId").val();
            const baseUrl = $("#currentBaseUrl").val();

            const action = $("#shareConfirmationAction").val();
            if (action !== null && action.length > 0) {
                if (action === "clear") {
                    albumsModalSettings.updateShareLink(baseUrl, albumId, "clear");
                } else if (action === "generate") {
                    albumsModalSettings.updateShareLink(baseUrl, albumId, "generate");
                }
            }
        });

        $("#copyLink").on("click", function (e) {
            e.preventDefault();

            const shareLink = $("#copyLink").attr("data-clipboard-text");

            if (shareLink !== null && shareLink !== "") {
                Util.copyToClipboard(shareLink, function (successfullyCopied) {
                    if (successfullyCopied) {
                        $("#copyLinkIcon").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-x").addClass("bi-clipboard-check");
                        $('#copyLinkIcon').fadeOut(5000, function () {
                            $(this).removeClass("bi-clipboard-check").removeClass("bi-clipboard-x").addClass("bi-clipboard-plus");
                        }).fadeIn(400);
                    } else {
                        $("#copyLinkIcon").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-check").addClass("bi-clipboard-x");
                    }
                });
            } else {
                shashin.showToastMessage("Could not copy text", "Link must not be blank", {icon:"bi-exclamation-triangle", iconColor:"#FF0000", borderColor:"danger"});
                $("#copyLinkIcon").removeClass("bi-clipboard-plus").removeClass("bi-clipboard-check").addClass("bi-clipboard-x");
            }
        });

        $("#saveUserShare").on("click", async function (e) {
            e.preventDefault();
            await saveUserShare();
        });

        $(document).keypress(async function (e) {
            const key = e.which;
            if(key === 13) {
                await saveUserShare();
            }
        });

        async function saveUserShare() {
            const albumId = $("#currentAlbumId").val();

            $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('bi-x-circle').addClass('spinner-grow');
            $("#albumsModalStatus").visible();
            $("#albumsModalStatus").attr("title", "");
            $("#cancelUserShare").prop('disabled', true);
            $("#userShare"+albumId).css('display', 'none');

            let userShareMap = {};
            let albumShared = false;
            $('input[name^="userShare' + albumId + '').each(function () {
                let checkboxId = $(this).attr('id');
                let isChecked = $(this).prop("checked");
                if (true === isChecked) {
                    albumShared = true;
                }

                let checkboxIdArray = checkboxId.split("-");
                let userId = checkboxIdArray[1];
                userShareMap[userId] = isChecked;
            });
            if (true === albumShared) {
                $("#userShare"+albumId).css('display', 'inline-block');
            }

            const http = new Http("share album");
            let json = {albumId: albumId, userShareMap: JSON.stringify(userShareMap)};
            const data = await http.ajax("post", "/album/share/" + albumId, JSON.stringify(json), function () {
                $("#albumsModalStatus").removeClass('bi-check-circle').removeClass('spinner-grow').addClass('bi-x-circle');
                $("#albumsModalStatus").attr("title", shashin.modalStatusFailMessage());
                $("#cancelUserShare").prop('disabled', false);
            });

            if (data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
                if (data.status === shashin.apiResponse.SUCCESS) {
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
        }
    };

    albumsModalListeners.setCommentModalListeners = function () {

        $("#propcommentalbums").on('hide.bs.modal', async function () {
            $("#commentList").empty();
            $("#commentListContainer").css("display","none");
        });

        $("#propcommentalbums").on('shown.bs.modal', async function () {
            $("#updateCommentAlbum").hide();
            $("#cancelEditCommentAlbum").hide();
        });

        $("#cancelEditCommentAlbum").on("click", function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId").val();

            if (currentCommentId !== "") {
                $("#saveCommentAlbum").show();
                $("#dismissModalCommentAlbum").show();
                $("#updateCommentAlbum").hide();
                $("#cancelEditCommentAlbum").hide();

                $("#comment" + currentCommentId).siblings().find('a').show();

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
                    let json = {commentId: currentCommentId, comment: updatedComment};
                    const data = await http.ajax("put", "/comment/update", JSON.stringify(json));

                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                        let commentId = data.commentId;

                        // Update comment
                        $("#commentcontent" + commentId).html(updatedComment);

                        $("#saveCommentAlbum").show();
                        $("#dismissModalCommentAlbum").show();
                        $("#updateCommentAlbum").hide();
                        $("#cancelEditCommentAlbum").hide();

                        $("#comment" + commentId).siblings().find('a').show();

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

                $("#comment" + currentCommentId).siblings().find('a').show();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId").val("");
            }
        });

        $("#saveCommentAlbum").on("click", async function (e) {
            e.preventDefault();
            const username = $("#currentUser").val();
            const albumId = $("#currentAlbumId").val();

            let comment = $.trim($("#commentText").val());

            if (comment.length > 0) {
                const http = new Http("save comment");
                let json = {albumId: albumId, comment: comment};
                const data = await http.ajax("post", "/comment/album/save", JSON.stringify(json));

                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId") && data.hasOwnProperty("commentCount")) {
                    let commentId = data.commentId;
                    let commentCount = data.commentCount;
                    let userProfile = data.userProfile;
                    let createdAt = data.createdAt; //dateFormat(new Date(data.createdAt), "ddd, mmm d, yyyy 'at' h:mm TT");

                    if (data.status === shashin.apiResponse.SUCCESS) {
                        $("#commentcount" + albumId).text(commentCount);

                        // Insert comment at top of list
                        const commentItem = '<li class="list-group-item list-group-item-secondary" id="comment' + commentId + '">\n' +
                            '<span id="commentcontainer' + commentId + '">\n<p id="commentcontent' + commentId + '">' + comment + '</p>\n' +
                            '<small>'+(userProfile!=="null" && userProfile!==null && userProfile!==""?'<img src="'+userProfile+'?'+uuidv4()+'" class="me-1" style="display:inline-block;width:24px;height:24px;" />':'<span class="bi-person-circle me-1" style="font-size:1.0rem;"></span>')+'<strong>' + username + '</strong> on '+createdAt+'<span style="float: right"><a href="#" id="deletecomment' + commentId + '"><span class="bi-trash"></span></a>&nbsp;&nbsp;<a href="#" id="editcomment' + commentId + '"><span class="bi-pencil"></span></a></span></small></span>' +
                            '<span id="textareacontainer' + commentId + '"></span></li>';
                        $("#commentText").val("");
                        $("#commentList").prepend(commentItem);

                        $("#deletecomment" + commentId).on("click", function (e) {
                            e.preventDefault();
                            $("#albumsCommentAlbumId").val(albumId);
                            $("#albumsCommentCommentId").val(commentId);
                            $("#propalbumstrashcomment").modal('show');
                        });

                        $("#editcomment" + commentId).on("click", function (e) {
                            e.preventDefault();
                            albumsCommentsSettings.editComment(albumId, commentId);
                        });
                    }
                }

                $("#commentListContainer").css("display","block");
            }
        });
    };

    albumsModalListeners.setEditCommentModalListeners = function (commentId) {
        $("#propcommentalbums").on('hide.bs.modal', async function () {
            $("#commentList").empty();
            $("#commentListContainer").css("display","none");

            $("#deletecomment"+commentId).off();
            $("#editcomment"+commentId).off();
            $("#propcommentalbums").off();
        });

        $("#deletecomment"+commentId).on("click", function (e) {
            e.preventDefault();
            const albumId = $("#currentAlbumId").val();
            $("#albumsCommentAlbumId").val(albumId);
            $("#albumsCommentCommentId").val(commentId);
            $("#propalbumstrashcomment").modal('show');
        });

        $("#editcomment"+commentId).on("click", function (e) {
            e.preventDefault();
            const albumId = $("#currentAlbumId").val();
            $("#currentCommentId").val("");
            albumsCommentsSettings.editComment(albumId, commentId);
        });
    };
}(window.albumsModalListeners = window.albumsModalListeners || {}, jQuery));