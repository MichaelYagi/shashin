(function( albumSettings, $, undefined ) {
    albumSettings.rendering = false;
    albumSettings.page = 1;
    albumSettings.http = null;
    albumSettings.eol = false;

    albumSettings.init = async function (albumId, activePage, albumMetadataList) {
        albumSettings.http = new Http(activePage);

        let mediaContentList = shashin.initLightGallery('infinite-scroll-gallery', {
            dynamic: true,
            plugins: [lgMetadataDetail],
            metadataDetail: true
        }, '.mediaLink');

        shashin.setVideoWidth($("#infinite-scroll-gallery")[0]);

        async function loadNextPage() {
            if (albumSettings.rendering === false) {
                // console.log(albumSettings.page)
                albumSettings.updateAlbum(albumId, albumSettings.page, activePage).then(function (additionalMediaContentList) {
                    // console.log(additionalMediaContentList)
                    albumSettings.page++;
                    mediaContentList = shashin.updateMediaContent(mediaContentList, additionalMediaContentList);
                });

                shashin.setVideoWidth($("#infinite-scroll-gallery")[0]);
            }
        }

        shashin.pageLoader(await loadNextPage, ".appendAlbumPhotos", albumMetadataList);

        shashin.mouseMoveListener();
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
                    $("#propAlbumModalThumbnail").html('<img loading="lazy" src="' + encodeURI(metadata.thumbnailUrlCentered) + '" height="100" width="100">');
                }

                // Open modal window
                $("#propAlbumModal").modal('show');
            }
        });
    }

    albumSettings.updateAlbum = async function(albumId,nextPage,activePage) {
        albumSettings.rendering = true;

        let data = null

        if (false === albumSettings.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get","/album/"+albumId+"/page/"+nextPage);
        }

        const mediaContentList = [];

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
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

                    shashin.printMessageToConsole(albumData);
                    shashin.printMessageToConsole(albumMetadataList);
                    shashin.printMessageToConsole(albumPhotoCommentsMap);
                    shashin.printMessageToConsole(userMap);
                    shashin.printMessageToConsole(albumMetadataList.length);

                    if (albumMetadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;

                        for (const index in albumMetadataList) {
                            const currentMediaLinkIndex = (mediaLinkLength + parseInt(index));
                            const metadata = albumMetadataList[index];

                            let dateHeadingObj = null;
                            const overlayFlags = {};
                            overlayFlags.renderTopRight = true;
                            overlayFlags.renderTopLeft = true;
                            overlayFlags.renderBottomLeft = true;
                            overlayFlags.renderCenter = true;
                            overlayFlags.renderBottomRight = true;

                            const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["favorite"] === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                            const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id]["count"] > 0 ? favoritesMap[metadata.id]["count"] : 0;

                            const dateHeadingCount = $(".dateSection").length;
                            const lastDateHeading = $(".dateSection").get(dateHeadingCount - 1).id;
                            const currentDate = metadata["year"] +"-"+ metadata["month"] +"-"+ metadata["day"];
                            const displayCurrentDate = Util.getDateString(metadata["year"], metadata["month"], metadata["day"]);

                            if (lastDateHeading !== currentDate) {
                                dateHeadingObj = {heading: currentDate, display: displayCurrentDate};
                            }

                            let overlayData;

                            if (userMap.showControls === true) {
                                overlayData = shashin.getOverlayData(metadata, {blOnClickFunction:"albumSettings.openAlbumModal",cOnClickFunction:"shashin.openGallery",onClickIdPrefix:"albumModalEdit",galleryIndex:currentMediaLinkIndex,favoriteCount:favoriteCount,favoriteIcon:favoriteIcon,albumPhotoCommentsMap:albumPhotoCommentsMap,notificationMap:notificationMap,overlayFlags});
                            } else {
                                overlayData = shashin.getOverlayData(metadata, {cOnClickFunction:"shashin.openGallery",galleryIndex:currentMediaLinkIndex,favoriteCount:favoriteCount,favoriteIcon:favoriteIcon,albumPhotoCommentsMap:albumPhotoCommentsMap,notificationMap:notificationMap,overlayFlags});
                            }

                            mediaContentList.push(shashin.getMediaContent(metadata));

                            // Append HTML
                            const appendClass = "appendAlbumPhotos"; //"albummodal" + metadata.id;
                            $(GalleryTemplates.PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData})).insertBefore($("."+appendClass).last()).ready(function () {
                                // Call JS and modal
                                albumModal.renderAlbumCommentsModal(albumData, metadata, userMap, albumPhotoCommentsMap);
                                albumSettings.activateAlbumListeners(metadata, albumData);
                            });
                        }

                        $("#spinner").css("display","none");
                        albumSettings.rendering = false;
                    } else {
                        albumSettings.eol = true;
                        $("#spinner").css("display","none");
                        $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                        albumSettings.rendering = false;
                    }
                }
            } else {
                albumSettings.eol = true;
                $("#spinner").css("display","none");
                $(".appendAlbumPhotos").last().text("EOL").css("display","none");
                albumSettings.rendering = false;
                message = '<div class="alert alert-danger" role="alert">' + data["msg"] + '</div>';
                $("#msgTimeline").html(message);
            }
        } else {
            albumSettings.eol = true;
            $("#spinner").css("display","none");
            $(".appendAlbumPhotos").last().text("EOL").css("display","none");
            albumSettings.rendering = false;
        }

        return mediaContentList;
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

        $('#propalbumphotocomment'+metadata.id).on('show.bs.modal', async function () {
            const http = new Http("mark comment read");
            const data = await http.ajax("get", "/notifications/markread/metadata/" + metadata.id);
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

    albumSettings.deleteComment = async function (commentId, metadata) {
        const http = new Http("delete comment");
        const json = {commentId: commentId, metadataId: metadata.id};
        const data = await http.ajax("post", "/comment/albumphoto/delete/", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
            let commentId = data["commentId"];
            if (data["status"] === "success") {
                // Delete comment
                $("#comment" + commentId).remove();
                let currentCount = parseInt($("#brcommentcount" + metadata.id).text());
                if (currentCount > 0) {
                    currentCount--;
                }
                $("#brcommentcount" + metadata.id).text(currentCount);
            }
        }

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

        $("#updateCommentMetadata"+metadata.id).on("click", async function (e) {
            e.preventDefault();

            const currentCommentId = $("#currentCommentId" + metadata.id).val();

            if (currentCommentId !== "") {
                const updatedComment = $.trim($("#commenttext" + currentCommentId).val());

                if (updatedComment.length > 0) {
                    const http = new Http("updating album photo comment");
                    const json = {commentId: currentCommentId, comment: updatedComment}
                    const data = await http.ajax("post", "/comment/update", JSON.stringify(json));

                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                        let commentId = data["commentId"];

                        // Update comment
                        $("#commentcontent" + commentId).text(updatedComment);

                        $("#saveCommentMetadata" + metadata.id).show();
                        $("#dismissModalCommentMetadata" + metadata.id).show();
                        $("#updateCommentMetadata" + metadata.id).hide();
                        $("#cancelEditCommentMetadata" + metadata.id).hide();

                        $("#commentcontainer" + commentId).show();
                        $("#textareacontainer" + commentId).html('');
                        $("#textareacontainer" + commentId).hide();
                        $("#commenttext" + commentId).val("");
                    }
                }

                $("#saveCommentMetadata" + metadata.id).show();
                $("#dismissModalCommentMetadata" + metadata.id).show();
                $("#updateCommentMetadata" + metadata.id).hide();
                $("#cancelEditCommentMetadata" + metadata.id).hide();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId" + metadata.id).val("");
            }
        });

        $("#saveCommentMetadata"+metadata.id).on("click", async function (e) {
            e.preventDefault();

            let comment = $.trim($("#commentText" + metadata.id).val());

            if (comment.length > 0) {
                const http = new Http("saving album photo comment");
                const json = {metadataId: metadata.id, albumId: album.id, comment: comment};
                const data = await http.ajax("post", "/comment/albumphoto/save/", JSON.stringify(json));

                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                    let commentId = data["commentId"];

                    // Insert comment at top of list
                    $("#commentText" + metadata.id).val("")
                    $("#commentList" + metadata.id).prepend(ModalTemplates.AlbumComment({
                        commentId: commentId,
                        commentText: comment,
                        userId: userMap.id,
                        commentUserId: userMap.id,
                        username: userMap.username
                    }));

                    $("#deletecomment" + commentId).on("click", function (e) {
                        e.preventDefault();
                        albumSettings.deleteComment(commentId, metadata);
                    });

                    $("#editcomment" + commentId).on("click", function (e) {
                        e.preventDefault();
                        albumSettings.editComment(commentId, metadata);
                    });

                    let currentCount = parseInt($("#brcommentcount" + metadata.id).text());
                    $("#brcommentcount" + metadata.id).text(currentCount + 1)
                }
            }
        });
    }
}( window.albumSettings = window.albumSettings || {}, jQuery ));

(function( albumModal, $, undefined ) {
    albumModal.renderAlbumCommentsModal = function (albumData,metadata,userMap,albumPhotoCommentsMap) {
        let index;
        let html = ModalTemplates.AlbumCommentsModalHead({metadata:metadata});

        const commentIdArray = [];
        for (index in albumPhotoCommentsMap[metadata.id]) {
            const comments = albumPhotoCommentsMap[metadata.id][index];
            commentIdArray.push(comments["commentId"]);

            html += ModalTemplates.AlbumComment({commentId:comments["commentId"],commentText:comments["comment"],userId:userMap.id,commentUserId:comments['userId'],username:comments["username"]});
        }

        html += ModalTemplates.AlbumCommentsModalFooter({metadata:metadata});

        $("#albummodal"+metadata.id).after(html);
        for (index in commentIdArray) {
            const commentId = commentIdArray[index];
            albumSettings.albumCommentsDeleteEditModalListener(commentId, metadata);
        }

        albumSettings.albumCommentsUpdateSaveModalListener(metadata, albumData, userMap);
    }
}( window.albumModal = window.albumModal || {}, jQuery ));