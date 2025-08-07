class Albums {

    constructor(albumsList, activePage, showControls, baseUrl, cspNonce, darkMode) {
        this.http = new Http(activePage);
        this.page = 1;
        this.rendering = false;
        this.albumsList = albumsList;
        this.eol = false;
        this.activePage = activePage;
        this.showControls = showControls;
        this.cspNonce = cspNonce;
        this.baseUrl = baseUrl;
        this.darkMode = darkMode;
    }

    async init() {
        setTimeout(async () => {
            shashin.pageLoader(await this.loadNextPage.bind(this), ".appendAlbumsPhotos", this.albumsList);
        }, 0);
        albumsModalListeners.setAlbumModalListeners();
        albumsModalListeners.setCommentModalListeners();

        $("#deleteAlbumsComment").on("click", function (e) {
            e.preventDefault();
            const commentId = $("#albumsCommentCommentId").val();
            const albumId = $("#albumsCommentAlbumId").val();
            albumsCommentsSettings.deleteComment(commentId, albumId);

            $("#propalbumstrashcomment").modal('hide');
        });
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateAlbums(this.page, this.activePage).then(function(data) {
                this.page++;
            }.bind(this));
        }

        return this.eol;
    }

    async updateAlbums(nextPage,activePage) {
        this.rendering = true;

        let data = null;
        let showControls = this.showControls;
        let cspNonce = this.cspNonce;
        let baseUrl = this.baseUrl;
        let darkMode = this.darkMode;
        const appendClass = "appendAlbumsPhotos";

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/" + activePage + "/" + nextPage);
        }

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("albumsList") && data.status === shashin.apiResponse.SUCCESS) {
            const albumsList = data.albumsList;
            const sharedAlbumMap = data.sharedAlbumsMap;

            if (albumsList !== null && albumsList.length > 0) {
                albumsList.forEach(function (album) {
                    if ($("#album"+album.id).length === 0) {
                        let html = '<div class="card" style="width:'+(Util.isMobile() ? '175' : '235')+'px;padding-top:'+(Util.isMobile() ? '14' : '10')+'px;margin-right:-1px;margin-bottom:-1px;">';
                        html += '<a href="/album/' + album.id + '" style="text-decoration: none !important;color: #777777;" id="album' + album.id + '">';
                        html += '<img loading="lazy" class="card-img-top" src="' + album.coverUrl + '" width="'+(Util.isMobile() ? '148' : '209')+'" height="'+(Util.isMobile() ? '148' : '209')+'" style="width: '+(Util.isMobile() ? '148' : '209')+'px;height: '+(Util.isMobile() ? '148' : '209')+'px;" draggable="false">';
                        html += '</a>';
                        html += '<div class="card-body" style="display: flex;flex-direction: column;">';
                        html += '<p class="card-text"><a href="/album/' + album.id + '" style="text-decoration: none !important;color: '+(darkMode ? '#FFFFFF;' : '#000000;')+';"><strong id="albumName' + album.id + '">' + album.name + '</strong></a></p>';
                        html += '<span style="margin-top: auto;"><a href="#" id="comment' + album.id + '" style="text-decoration: none;" title="Comments">';
                        html += '<span id="commentcount' + album.id + '">' + (album.id in data.albumsCommentsMap ? data.albumsCommentsMap[album.id].length : "0") + '</span>&nbsp;';
                        html += '<span class="bi-chat-square position-relative">';
                        html += '</span></a>';

                        if (album.albumPhotoCount > 0) {
                            html += '&nbsp;<form method="post" action="/album/download/' + album.id + '" style="display: inline-block;white-space: nowrap;">';
                            html += '<button class="bi-download' + (darkMode ? ' link-button-darkmode' : ' link-button-lightmode') + '" type="submit" id="download' + album.id + '" name="download" value="' + album.id + '" title="Download album photos"></button>';
                            html += '</form>&nbsp;';
                        } else {
                            html += '&nbsp;&nbsp;&nbsp;';
                        }

                        if (true === showControls) {
                            html += '<a href="#" id="edit' + album.id + '"><span class="bi-pencil" title="Edit album"></span></a>';
                            html += (Util.isMobile() ? '&nbsp;' : '&nbsp;&nbsp;&nbsp;') + '<a href="#" id="share' + album.id + '"><span class="' + (album.shareUrl != null && album.shareUrl !== '' ? 'bi-share-fill' : 'bi-share') + '" title="' + (album.shareUrl != null && album.shareUrl !== '' ? 'Shared' : 'Share') + ' with other people"></span></a>';
                            html += '<span id="userShare' + album.id + '" title="Shared with other users" style="' + (sharedAlbumMap.hasOwnProperty(album.id) && showControls === true ? "display: inline-block" : "display: none") + '">&nbsp;&nbsp;&nbsp;<span class="bi-person-up text-muted"></span></span>';
                            html += (Util.isMobile() ? '&nbsp;' : '&nbsp;&nbsp;&nbsp;') + '<a href="#" id="trash' + album.id + '" title="Delete album"><span class="bi-trash"></span></a>';
                        }

                        html += '<p class="card-text"><small class="text-muted">' + album.albumPhotoCount + (album.albumPhotoCount === 1 ? ' photo' : ' photos') + (Util.isMobile() ? '&nbsp;&nbsp;' : '&nbsp;&nbsp;&nbsp;&nbsp;') + album.albumVideoCount + (album.albumVideoCount === 1 ? ' video' : ' videos') + '</small></p></span>';
                        html += '</div></div>';

                        html += '<script type="text/javascript" nonce="' + cspNonce + '">Albums.setAlbumsEventListeners(' + album.id + ', "' + baseUrl + '", ' + showControls + ', "' + cspNonce + '");<\/script>';
                        html += '<span class="' + appendClass + '" style="width:0;height:0;padding:0"></span>';

                        $(html).insertBefore($("." + appendClass).last());
                    }
                });

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $("."+appendClass).last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $("."+appendClass).last().text("EOL").css("display","none");
            this.rendering = false;
            this.eol = true;
        }

        $("#spinner").css("display","none");

        return data;
    }

    static setAlbumsEventListeners(albumId, baseUrl, showControls, cspNonce) {

        if (true === showControls) {

            $("#share" + albumId).on("click", async function (e) {
                e.preventDefault();

                $("#currentAlbumId").val(albumId);

                let http = new Http("sharealbums");
                let data = await http.ajax("get", "/album/" + albumId + "/page/0");

                if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album")) {
                    if (data.status === shashin.apiResponse.SUCCESS) {
                        let album = data.album;

                        $("#albumName").text(album.name);
                        const version = Util.getMetadataLocalStorage();
                        $("#albumCoverThumb").attr("src", data.coverUrl+(version === "" ? "" : "?v=" + version));
                        $("#fullShareLinkContainer").css("display", "none");
                        $("#fullShareLink").text("");
                        $("#shareLink").val("");
                        $("#shareUserList").text("");
                        let shareUrl = album.shareUrl;

                        if (shareUrl !== null && "" !== shareUrl) {
                            const fullShareLink = baseUrl + "/share/" + shareUrl + "/album/" + albumId;
                            $("#shareLink").val(shareUrl);
                            $("#fullShareLinkContainer").css("display", "block");
                            $("#fullShareLink").html("<a target='_blank' href='" + fullShareLink + "'>" + fullShareLink + "</a>");
                            $("#copyLink").attr("data-clipboard-text", fullShareLink);
                            $("#clearLink").prop('disabled', false);
                            $("#copyLink").prop('disabled', false);
                        }

                        let sharedAlbumsList = await http.ajax("get", "/sharedalbums");

                        if (sharedAlbumsList != null && sharedAlbumsList.sharedAlbums.length > 0) {
                            let html = "<strong>"+shashin.getTranslatedValue("main.pages.albums.share")+"</strong><br>";
                            sharedAlbumsList.sharedAlbums.forEach(function (shareObj) {
                                if (albumId === shareObj.albumId) {
                                    html += '<div class="col-auto form-check">';
                                    let input = '<input type="checkbox" class="form-check-input" id="id-' + shareObj.userId + '-' + shareObj.albumId + '" name="userShare' + shareObj.albumId + '"';

                                    if (1 === shareObj.isShared) {
                                        html += input + ' checked>';
                                    } else {
                                        html += input + '>';
                                    }

                                    html += '<label class="form-check-label">' + shareObj.username + '</label></div>';
                                }
                            });

                            $("#shareUserList").html(html);
                        }

                        $("#propsharealbums").modal('show');
                    }
                }
            });

            $("#edit" + albumId).on("click", async function (e) {
                e.preventDefault();

                $("#currentAlbumId").val(albumId);

                $("#editAlbumNameStatus").addClass('spinner-grow').removeClass('bi-check-circle').removeClass('bi-x-circle');
                $("#editAlbumNameStatus").invisible();
                $("#albumEditName").val("");
                $("#originalAlbumName").val("");

                let http = new Http("editalbums");
                let data = await http.ajax("get", "/album/" + albumId + "/page/0");

                if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album") && data.status === shashin.apiResponse.SUCCESS) {
                    let album = data.album;

                    $("#albumNameEdit").text(album.name);
                    $("#albumEditName").val(album.name);
                    $("#originalAlbumName").val(album.name);
                    const version = Util.getMetadataLocalStorage();
                    $("#albumCoverEditThumb").attr("src", data.coverUrl+(version === "" ? "" : "?v=" + version));
                    $("#editAlbum").prop('disabled', true);

                    $("#propeditalbums").modal('show');

                    albumsModalListeners.setEditAlbumsListeners(albumId);
                }
            });

            $("#trash" + albumId).on("click", async function (e) {
                e.preventDefault();

                $("#currentAlbumId").val(albumId);

                let http = new Http("trashalbums");
                let data = await http.ajax("get", "/album/" + albumId + "/page/0");

                if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album")) {
                    if (data.status === shashin.apiResponse.SUCCESS) {
                        let album = data.album;

                        $("#albumNameTrash").text(album.name);
                        const version = Util.getMetadataLocalStorage();
                        $("#albumCoverTrashThumb").attr("src", data.coverUrl+(version === "" ? "" : "?v=" + version));

                        $("#proptrashalbums").modal('show');

                        albumsModalListeners.setDeleteAlbumsListeners(albumId);
                    }
                }
            });
        }

        $("#comment"+albumId).on("click", async function (e) {
            e.preventDefault();

            $("#currentAlbumId").val(albumId);

            let http = new Http("albumcomments");
            let data = await http.ajax("get", "/album/" + albumId + "/page/0");
            let currentUserId = $("#currentUserId").val();

            if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album") && data.status === shashin.apiResponse.SUCCESS) {
                let album = data.album;
                let canEdit = data.canEdit;

                $("#albumNameComments").text(album.name);
                const version = Util.getMetadataLocalStorage();
                $("#albumCoverCommentThumb").attr("src", data.coverUrl+(version === "" ? "" : "?v=" + version));

                let albumCommentsList = await http.ajax("get", "/albumcomments/"+albumId);

                if (albumCommentsList != null && albumCommentsList.albumCommentsList.length > 0) {
                    let html = "";
                    albumCommentsList = albumCommentsList.albumCommentsList;
                    albumCommentsList.forEach(function(comments) {
                        if (comments.albumId === albumId) {
                            html += '<li id="comment' + comments.commentId + '" class="list-group-item' + (comments.userId === currentUserId ? ' list-group-item-secondary' : '') + '">';
                            html += '<span id="commentcontainer' + comments.commentId + '">';
                            html += '<p id="commentcontent' + comments.commentId + '">' + comments.comment + '</p>';
                            html += '<small>';

                            if (comments.userProfile !== "" && comments.userProfile !== null && comments.userProfile !== "null") {
                                html += '<img src="'+comments.userProfile+'?'+uuidv4()+'" class="me-1" style="display:inline-block;width:24px;height:24px;" />';
                            } else {
                                html += '<span class="bi-person-circle me-1" style="font-size:1.0rem;"></span>';
                            }
                            html += '<strong>' + comments.username + '</strong> <span>- ' + comments.createdAt + '</span></small>';

                            if (parseInt(comments.userId, 10) === parseInt(currentUserId, 10)) {
                                html += '<small><span style="float: right">';
                                html += '<a href="#" id="deletecomment' + comments.commentId + '"><span class="bi-trash"></span></a>&nbsp;&nbsp';
                                html += '<a href="#" id="editcomment' + comments.commentId + '"><span class="bi-pencil"></span></a>';
                                html += '</span></small>';
                            } else if (canEdit) {
                                html += '<small><span style="float: right">';
                                html += '<a href="#" id="deletecomment' + comments.commentId + '"><span class="bi-trash"></span></a>&nbsp;&nbsp';
                                html += '</span></small>';
                            }
                            html += '</span><span id="textareacontainer' + comments.commentId + '"></span>';
                            html += '</li>';
                        }
                        html += '<script type="text/javascript" nonce="'+cspNonce+'">albumsModalListeners.setEditCommentModalListeners('+comments.commentId+');<\/script>';
                    });


                    $("#commentListContainer").css("display","block");
                    $("#commentList").html(html);
                }

                $("#propcommentalbums").modal('show');
            }
        });

        $("#download"+albumId).on("click", function() {
            $("#currentAlbumId").val(albumId);

            let downloadTimer;
            const tokenName = "ShashinAlbumName";
            const tokenSize = "ShashinAlbumSize";
            const configuredAttempts = 120;
            const downloadLocation = $("#download"+albumId).attr("href");
            const albumName = $("#albumName"+albumId).text();


            // $("#albumsMessage").html("<span class='spinner-grow spinner-grow-sm'></span> <strong>Exporting album \""+albumName+"\". Downloading photos only.</strong>").animate({opacity: 100}, 0);
            shashin.closeToastMessages({tag:"albumdownload"});
            shashin.showToastMessage(shashin.getTranslatedValue("main.pages.albums.downloading.title"), shashin.getTranslatedValue("main.pages.albums.downloading.body",albumName) + ".", {icon:"bi-info-circle", iconColor:"#777777", autohide:true, tag:"albumdownload"});
            setTimeout(function () { $("#download"+albumId).removeAttr("href"); }, 0);
                Util.setCookie(tokenName, "", "/");
                Util.setCookie(tokenSize, "", "/");

                let attempts = configuredAttempts;

                downloadTimer = window.setInterval( function() {
                const tokenCookieValue = Util.getCookie(tokenName);
                const tokenCookieSize = Util.getCookie(tokenSize);

                if ((tokenCookieValue !== "" && tokenCookieSize !== "") || attempts === 0) {
                    if (attempts === 0) {
                        // $("#albumsMessage").html("&nbsp;").animate({opacity: 0}, 5000);
                    } else {
                        shashin.showToastMessage(
                            shashin.getTranslatedValue("main.pages.albums.downloading.title"),
                            "<strong>"+shashin.getTranslatedValue("main.pages.albums.downloading.filename")+"</strong> " + tokenCookieValue +
                            " <strong>"+shashin.getTranslatedValue("main.pages.albums.downloading.filesize")+"</strong> " + Util.formatBytes(tokenCookieSize),{
                                icon:"bi-info-circle",
                                iconColor:"#777777"
                            }
                        );
                        // $("#albumsMessage").html("<strong>File name</strong> " + tokenCookieValue + " <strong>File size</strong> " + Util.formatBytes(tokenCookieSize)).animate({opacity: 0}, 10000);
                        $("#download" + albumId).attr("href", downloadLocation);
                        Util.deleteCookie(tokenName, "/");
                        Util.deleteCookie(tokenSize, "/");
                        window.clearInterval(downloadTimer);
                    }
                }

                attempts--;
            }, 1000);
        });
    }
}