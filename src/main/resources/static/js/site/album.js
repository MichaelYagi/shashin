(function( albumSettings, $, undefined ) {
    albumSettings.rendering = false;
    albumSettings.page = 1;
    albumSettings.http = null;
    albumSettings.eol = false;
    albumSettings.lastDate = "";
    albumSettings.thumbnailType = "225";
    albumSettings.thumbnailHeight = "225";
    if (Util.isMobile()) {
        albumSettings.thumbnailType = "centered";
        albumSettings.thumbnailHeight = "120";
    }

    albumSettings.init = async function (albumId, mediaTypeFilter, activePage, albumMetadataList, lastDate) {
        albumSettings.http = new Http(activePage);
        albumSettings.lastDate = lastDate;

        const lgConfig = {
            dynamic:true,
            plugins:[]
        };
        if (typeof lgMetadataDetail !== "undefined") {
            lgConfig.plugins.push(lgMetadataDetail);
            lgConfig.metadataDetail = true;
            lgConfig.metadataDetailFun = shashin.openEditMetadataModal;
        }
        if (typeof lgVideoThumbnail !== "undefined") {
            lgConfig.plugins.push(lgVideoThumbnail);
            lgConfig.videoThumbnail = true;
            lgConfig.videoThumbnailFun = shashin.processVideoThumbnail;
        }

        let mediaContentList = shashin.initLightGallery('infinite-scroll-gallery', lgConfig, '.mediaLink');
        $("#uploadToAlbumForm").attr("action", "/album/media/upload/batch/"+albumId);

        async function loadNextPage() {
            if (albumSettings.rendering === false) {
                // console.log(albumSettings.page)
                albumSettings.getPagedAlbum(albumId, mediaTypeFilter, albumSettings.page, activePage).then(function (additionalMediaContentList) {
                    // console.log(additionalMediaContentList)
                    albumSettings.page++;
                    mediaContentList = shashin.updateMediaContent(mediaContentList, additionalMediaContentList);

                    if (albumSettings.eol) {
                        setTimeout(() => {Util.reinitLightGalleryInstance({timeoutValue:0,mediaContentList:additionalMediaContentList,refreshContent:true});}, 0);
                    }
                });
            }

            return albumSettings.eol;
        }

        $("#deleteComment").on("click", function (e) {
            e.preventDefault();
            const commentId = $("#commentCommentId").val();
            const metadataId = $("#commentMetadataId").val();
            albumSettings.deleteComment(commentId, metadataId);
            $("#proptrashcomment").modal('hide');
        });

        setTimeout(async () => {
            shashin.pageLoader(await loadNextPage, ".appendAlbumPhotos", albumMetadataList, activePage);
        }, 0);
    };

    albumSettings.openAlbumModal = function (e,metadataId) {
        e.preventDefault();

        shashin.getMetadata(metadataId).then(function (metadata) {
            if (metadata !== null) {
                // Clear modal data
                $("#albumModalTitle").text(metadata.title);
                $('#propAlbumModal').find(':input').val('');
                // $("#removeFromAlbum")[0].checked = false;
                // $("#setCoverAlbum")[0].checked = true;
                $("#setCoverAlbum").val("yes");
                $("#propAlbumModalThumbnail").html("");

                $("#metadataId").val(metadata.id);
                if (metadata.thumbnailUrlCentered !== null) {
                    const version = Util.getMetadataLocalStorage();
                    $("#propAlbumModalThumbnail").html('<img loading="lazy" src="/api/v1/thumbnails/centered/' + metadata.id + (version === "" ? "" : "?v=" + version) + '" height="120" width="120" draggable="false">');
                }

                // Open modal window
                $("#propAlbumModal").modal('show');
            }
        });
    };

    albumSettings.getPagedAlbum = async function(albumId,mediaTypeFilter, nextPage,activePage) {
        albumSettings.rendering = true;

        let data = null;

        if (false === albumSettings.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get","/album/"+albumId+"/mediatype/"+mediaTypeFilter+"/page/"+nextPage);
        }

        const mediaContentList = [];

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("msg")) {
            let message = "Error";
            if (data.status === shashin.apiResponse.SUCCESS) {
                if (data.hasOwnProperty("album") &&
                    data.hasOwnProperty("albumMetadataList") &&
                    data.hasOwnProperty("albumPhotoCommentsMap") &&
                    data.hasOwnProperty("userMap")
                ) {
                    const albumData = data.album;
                    const albumMetadataList = data.albumMetadataList;
                    const albumPhotoCommentsMap = data.albumPhotoCommentsMap;
                    const userMap = data.userMap;
                    const favoritesMap = data.favorites;
                    const canEdit = data.canEdit;

                    shashin.printMessageToConsole("albumSettings.getPagedAlbum",{tag:"album"});
                    shashin.printMessageToConsole(albumData,{tag:"album"});
                    shashin.printMessageToConsole(albumMetadataList,{tag:"album"});
                    shashin.printMessageToConsole(albumPhotoCommentsMap,{tag:"album"});
                    shashin.printMessageToConsole(userMap,{tag:"album"});
                    shashin.printMessageToConsole(albumMetadataList.length,{tag:"album"});

                    if (albumMetadataList.length > 0) {
                        const mediaLinkLength = $(".mediaLink").length;
                        const appendClass = "appendAlbumPhotos";

                        for (let index in albumMetadataList) {
                            index = parseInt(index);
                            const currentMediaLinkIndex = (mediaLinkLength + index);
                            const metadata = albumMetadataList[index];
                            const currentDate = metadata.year + "-" + metadata.month + "-" + metadata.day;

                            if ($("#photoThumbnailContainer"+metadata.id).length === 0) {

                                let dateHeadingObj = null;
                                const overlayFlags = {};
                                overlayFlags.renderTopRight = true;
                                overlayFlags.renderTopLeft = true;
                                overlayFlags.renderBottomLeft = true;
                                overlayFlags.renderCenter = true;
                                overlayFlags.renderBottomRight = true;

                                const favoriteIcon = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].favorite === true ? 'bi-suit-heart-fill' : 'bi-suit-heart';
                                const favoriteCount = favoritesMap.hasOwnProperty(metadata.id) && favoritesMap[metadata.id].count > 0 ? favoritesMap[metadata.id].count : 0;

                                let lastDate = albumMetadataList.hasOwnProperty(index-1) ? albumMetadataList[index-1].year+ "-" + albumMetadataList[index-1].month + "-" + albumMetadataList[index-1].day : "";
                                if (albumSettings.lastDate !== "") {
                                    lastDate = albumSettings.lastDate;
                                    albumSettings.lastDate = "";
                                }
                                const nextDate = albumMetadataList.hasOwnProperty(index+1) ? albumMetadataList[index+1].year + "-" + albumMetadataList[index+1].month + "-" + albumMetadataList[index+1].day : "";
                                const displayCurrentDate = dateFormat(currentDate.replace(/-/g, "/"), "ddd, mmm d, yyyy");

                                if (lastDate !== currentDate || $("#"+currentDate).length === 0) {
                                    dateHeadingObj = {
                                        heading: currentDate,
                                        display: displayCurrentDate
                                    };
                                }

                                let overlayData;

                                if (userMap.showControls === true) {
                                    overlayData = shashin.getOverlayData(metadata, {
                                        blOnClickFunction: "albumSettings.openAlbumModal",
                                        cOnClickFunction: "shashin.openGallery",
                                        onClickIdPrefix: "albumModalEdit",
                                        galleryIndex: currentMediaLinkIndex,
                                        favoriteCount: favoriteCount,
                                        favoriteIcon: favoriteIcon,
                                        albumPhotoCommentsMap: albumPhotoCommentsMap,
                                        overlayFlags
                                    });
                                } else {
                                    overlayData = shashin.getOverlayData(metadata, {
                                        cOnClickFunction: "shashin.openGallery",
                                        galleryIndex: currentMediaLinkIndex,
                                        favoriteCount: favoriteCount,
                                        favoriteIcon: favoriteIcon,
                                        albumPhotoCommentsMap: albumPhotoCommentsMap,
                                        overlayFlags
                                    });
                                }

                                mediaContentList.push(shashin.getMediaContent(metadata));

                                // Append HTML
                                const uuid = uuidv4();

                                if ($("#"+currentDate).length === 0 && dateHeadingObj !== null) {
                                    const headerAndBody = '<section class="dateSection" id="'+dateHeadingObj.heading+'"><div class="dateHeader" style="margin-left: -17px; padding-left: 13px;margin-top: -25px; padding-top: 25px;margin-bottom: 0; padding-bottom: 15px;" id="dateHeader'+dateHeadingObj.heading+'"><span id="select'+metadata.year+'-'+metadata.month+'-'+metadata.day+'" class="bi-circle pe-2 day-select" style="font-size: 0.85rem;color: lightgray;display: none"></span><strong>'+dateHeadingObj.display+'</strong></div><div id="dateBody'+dateHeadingObj.heading+'" class="row" class="row" style="margin-left:-2px;"></div></section>';
                                    $(headerAndBody).insertBefore($("." + appendClass).last());
                                }

                                const html = $(GalleryTemplates.PhotoGalleryItem({
                                    activePage,
                                    metadata,
                                    overlayData,
                                    uuid,
                                    isMobile: Util.isMobile()
                                }));

                                if ($("#dateBody"+currentDate).length > 0) {
                                    $(html).appendTo($("#dateBody" + currentDate)).ready(function () {
                                        // Call JS and modal
                                        albumModal.renderAlbumCommentsModal(albumData, metadata, userMap, albumPhotoCommentsMap, canEdit);
                                        albumSettings.activateAlbumListeners(metadata, albumData);
                                    });
                                }

                                if ($("#"+currentDate).length > 0 && nextDate !== "" && currentDate !== nextDate) {
                                    $("<span class='"+appendClass+"' style='width:0;height:0;padding:0'></span>").insertAfter($("#"+currentDate));
                                }
                            }

                            shashin.dayHeadingListener(currentDate, activePage, mediaTypeFilter);
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
                message = '<div class="alert alert-danger" role="alert">' + data.msg + '</div>';
                $("#msgTimeline").html(message);
            }
        } else {
            albumSettings.eol = true;
            $("#spinner").css("display","none");
            $(".appendAlbumPhotos").last().text("EOL").css("display","none");
            albumSettings.rendering = false;
        }

        return mediaContentList;
    };

    albumSettings.activateAlbumListeners = function(metadata) {
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
            let currentCount = parseInt($("#brcommentcount" + metadata.id).text());
            if (currentCount > 0) {
                $("#commentListContainer"+metadata.id).css("display","block");
            } else {
                $("#commentListContainer"+metadata.id).css("display","none");
            }
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
    };

    albumSettings.deleteComment = async function (commentId, metadataId) {
        const http = new Http("delete comment");
        const json = {commentId: commentId};
        const data = await http.ajax("delete", "/comment/albumphoto/delete", JSON.stringify(json));

        if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
            let commentId = data.commentId;
            if (data.status === shashin.apiResponse.SUCCESS) {
                // Delete comment
                $("#comment" + commentId).remove();
                let currentCount = parseInt($("#brcommentcount" + metadataId).text());
                if (currentCount > 0) {
                    currentCount--;
                }

                if (currentCount > 0) {
                    $("#commentListContainer"+metadataId).css("display","block");
                } else {
                    $("#commentListContainer"+metadataId).css("display","none");
                }

                $("#brcommentcount" + metadataId).text(currentCount);
            }
        }

        $("#currentCommentId" + metadataId).val("");
    };

    albumSettings.editComment = function(commentId, metadataId) {
        if ($("#currentCommentId" + metadataId).val() === "") {
            $("#currentCommentId" + metadataId).val(commentId);

            $("#saveCommentMetadata" + metadataId).hide();
            $("#dismissModalCommentMetadata" + metadataId).hide();
            $("#updateCommentMetadata" + metadataId).show();
            $("#cancelEditCommentMetadata" + metadataId).show();

            // Hide other trash and pencil actions
            $("#comment" + commentId).siblings().find('a').hide();

            $("#commentcontainer" + commentId).hide();
            $("#textareacontainer" + commentId).show();
            const commentText = $("#commentcontent" + commentId).html();
            $("#textareacontainer" + commentId).html('<textarea class="form-control" id="commenttext' + commentId + '" rows="2" placeholder="Comment">' + commentText + '</textarea>');
        }
    };

    albumSettings.albumCommentsDeleteEditModalListener = function(commentId, metadataId) {
        $("#deletecomment" + commentId).on("click", function (e) {
            e.preventDefault();
            $("#commentMetadataId").val(metadataId);
            $("#commentCommentId").val(commentId);
            $("#proptrashcomment").modal('show');
            let currentCount = parseInt($("#brcommentcount" + metadataId).text());
            if (currentCount > 0) {
                $("#commentListContainer"+metadataId).css("display","block");
            } else {
                $("#commentListContainer"+metadataId).css("display","none");
            }
        });

        $("#editcomment" + commentId).on("click", function (e) {
            e.preventDefault();
            albumSettings.editComment(commentId, metadataId);
            let currentCount = parseInt($("#brcommentcount" + metadataId).text());
            if ((currentCount + 1) > 0) {
                $("#commentListContainer"+metadataId).css("display","block");
            } else {
                $("#commentListContainer"+metadataId).css("display","none");
            }
        });
    };

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

                $("#comment" + currentCommentId).siblings().find('a').show();

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
                    const json = {commentId: currentCommentId, comment: updatedComment};
                    const data = await http.ajax("put", "/comment/update", JSON.stringify(json));

                    if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                        let commentId = data.commentId;

                        // Update comment
                        $("#commentcontent" + commentId).html(updatedComment);

                        $("#saveCommentMetadata" + metadata.id).show();
                        $("#dismissModalCommentMetadata" + metadata.id).show();
                        $("#updateCommentMetadata" + metadata.id).hide();
                        $("#cancelEditCommentMetadata" + metadata.id).hide();

                        $("#comment" + commentId).siblings().find('a').show();

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

                $("#comment" + currentCommentId).siblings().find('a').show();

                $("#commentcontainer" + currentCommentId).show();
                $("#textareacontainer" + currentCommentId).html('');
                $("#textareacontainer" + currentCommentId).hide();

                $("#currentCommentId" + metadata.id).val("");

                let currentCount = parseInt($("#brcommentcount" + metadata.id).text());
                if (currentCount > 0) {
                    $("#commentListContainer"+metadata.id).css("display","block");
                } else {
                    $("#commentListContainer"+metadata.id).css("display","none");
                }
            }
        });

        $("#saveCommentMetadata"+metadata.id).on("click", async function (e) {
            e.preventDefault();

            let comment = $.trim($("#commentText" + metadata.id).val());

            if (comment.length > 0) {
                const http = new Http("saving album photo comment");
                const json = {metadataId: metadata.id, albumId: album.id, comment: comment};
                const data = await http.ajax("post", "/comment/albumphoto/save", JSON.stringify(json));

                if (data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("commentId")) {
                    let commentId = data.commentId;
                    let userProfile = data.userProfile;
                    let createdAt = data.createdAt;
                    let canEdit = data.createdAt;

                    // Insert comment at top of list
                    $("#commentText" + metadata.id).val("");
                    $("#commentList" + metadata.id).prepend(ModalTemplates.AlbumComment({
                        commentId: commentId,
                        commentText: comment,
                        userId: userMap.id,
                        commentUserId: userMap.id,
                        username: userMap.username,
                        userProfile: userProfile,
                        createdAt: createdAt,
                        canEdit: canEdit
                    }));

                    $("#deletecomment" + commentId).on("click", function (e) {
                        e.preventDefault();
                        $("#commentMetadataId").val(metadata.id);
                        $("#commentCommentId").val(commentId);
                        $("#proptrashcomment").modal('show');
                    });

                    $("#editcomment" + commentId).on("click", function (e) {
                        e.preventDefault();
                        albumSettings.editComment(commentId, metadata.id);
                    });

                    let currentCount = parseInt($("#brcommentcount" + metadata.id).text());
                    $("#brcommentcount" + metadata.id).text(currentCount + 1);
                    $("#commentListContainer"+metadata.id).css("display","block");
                }
            }
        });
    };
}( window.albumSettings = window.albumSettings || {}, jQuery ));

(function( albumModal, $, undefined ) {
    albumModal.renderAlbumCommentsModal = function (albumData,metadata,userMap,albumPhotoCommentsMap,canEdit) {
        let index;
        let html = ModalTemplates.AlbumCommentsModalHead({metadata:metadata});

        const commentIdArray = [];
        for (index in albumPhotoCommentsMap[metadata.id]) {
            const comments = albumPhotoCommentsMap[metadata.id][index];
            commentIdArray.push(comments.commentId);

            html += ModalTemplates.AlbumComment({commentId:comments.commentId,commentText:comments.comment,userId:userMap.id,commentUserId:comments.userId,username:comments.username,userProfile:comments.userProfile,createdAt:comments.createdAt,canEdit:canEdit});
        }

        if (commentIdArray.length > 0) {
            $("#commentListContainer"+metadata.id).css("display","block");
        }
        html += ModalTemplates.AlbumCommentsModalFooter({metadata:metadata});

        $("#albummodal"+metadata.id).after(html);
        for (index in commentIdArray) {
            const commentId = commentIdArray[index];
            albumSettings.albumCommentsDeleteEditModalListener(commentId,metadata.id);
        }

        albumSettings.albumCommentsUpdateSaveModalListener(metadata, albumData, userMap);
    };
}( window.albumModal = window.albumModal || {}, jQuery ));