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
        shashin.pageLoader(await this.loadNextPage.bind(this), ".appendAlbumsPhotos", this.albumsList);
    }

    async loadNextPage() {
        if (this.rendering === false) {
            // console.log(this.page)
            this.updateAlbums(this.page, this.activePage).then(function(data) {
                this.page++;
            }.bind(this));
        }
    }

    async updateAlbums(nextPage,activePage) {
        this.rendering = true;

        let data = null
        let showControls = this.showControls;
        let cspNonce = this.cspNonce;
        let baseUrl = this.baseUrl;
        let darkMode = this.darkMode;

        if (false === this.eol) {
            $("#spinner").css("display", "block");
            data = await this.http.ajax("get", "/api/v1/" + activePage + "/" + nextPage);
        }

        if (data !== null && data.hasOwnProperty("status") && data.hasOwnProperty("albumsList") && data["status"] === "success") {
            const albumsList = data["albumsList"];

            if (albumsList !== null && albumsList.length > 0) {

                albumsList.forEach(function(album) {
                    const appendClass = "appendAlbumsPhotos";

                    let html = '<div class="card" style="width:235px;padding-top:10px;">';
                    html += '<a href="album'+album.id+'" style="text-decoration: none !important;color: #777777;" id="album'+album.id+'">';
                    html += '<img loading="lazy" class="card-img-top" src="'+album.coverUrl+'" width="209" height="209" style="width: 209px;height: 209px;">';
                    html += '</a>';
                    html += '<div class="card-body">';
                    html += '<strong id="albumName'+album.id+'">'+album.name+'</strong><br>';
                    html += '<a href="#" id="comment'+album.id+'" style="text-decoration: none;" title="Comments">';
                    html += '<span id="commentcount'+album.id+'">'+(album.id in data["albumsCommentsMap"] ? data["albumsCommentsMap"][album.id].length : "0")+'</span>&nbsp;';
                    html += '<span class="bi-chat-square position-relative">';
                    // if (album.id in data["notificationMap"]) {
                    //     html += '<span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle">';
                    //     html += '<span class="visually-hidden">New alerts</span>';
                    //     html += '</span>';
                    // }
                    html += '</span>';

                    if (album.albumPhotoCount > 0) {
                        html += '&nbsp;<form method="post" action="/album/download/'+album.id+'" style="display: inline-block;white-space: nowrap;">';
                        html += '<button class="bi-download'+(darkMode ? ' link-button-darkmode' : ' link-button-lightmode')+'" type="submit" id="download'+album.id+'" name="download" value="'+album.id+'" title="Download album photos"></button>';
                        html += '</form>&nbsp;';
                    } else {
                        html += '&nbsp;&nbsp;&nbsp;';
                    }

                    if (album.albumPhotoCount === 0) {

                    }
                    if (true === showControls) {
                        html += '<a href="#" id="edit'+album.id+'"><span class="bi-pencil" title="Edit album"></span></a>';
                        html += '&nbsp;&nbsp;&nbsp;<a href="#" id="share'+album.id+'"><span class="'+(album.shareUrl != null && album.shareUrl !== '' ? 'bi-share-fill' : 'bi-share')+'" title="Share with other users"></span></a>';
                        html += '&nbsp;&nbsp;&nbsp;<a href="#" id="trash'+album.id+'" title="Delete album"><span class="bi-trash"></span></a>';
                    }
                    html += '<p class="card-text"><small class="text-muted">'+album.albumPhotoCount + (album.albumPhotoCount === 1 ? ' photo':' photos') + '&nbsp;&nbsp;&nbsp;&nbsp;' + album.albumVideoCount + (album.albumVideoCount === 1 ? ' video':' videos')+'</small></p>';
                    html += '</div></div>';

                    html += '<script type="text/javascript" nonce="'+cspNonce+'">Albums.setAlbumsEventListeners('+album.id+', "'+baseUrl+'", '+showControls+', "'+cspNonce+'");<\/script>';
                    html += '<span class="appendAlbumsPhotos" style="width:0;height:0;padding:0"></span>';

                    $(html).insertBefore($("."+appendClass).last());
                });

                this.rendering = false;
                $("#spinner").css("display", "none");
            } else {
                $(".appendAlbumsPhotos").last().text("EOL").css("display","none");
                this.rendering = false;
                this.eol = true;
            }
        } else {
            $(".appendAlbumsPhotos").last().text("EOL").css("display","none");
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

                let http = new Http("sharealbums");
                let data = await http.ajax("get", "/api/v1/album/" + albumId + "/page/0");

                if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album")) {
                    if (data["status"] === "success") {
                        let album = data["album"];

                        $("#albumName").text(album["name"]);
                        $("#albumCoverThumb").attr("src", album["coverUrl"]);
                        $("#fullShareLinkContainer").css("display", "none");
                        $("#fullShareLink").text("");
                        $("#shareLink").val("");
                        $("#shareUserList").text("");
                        let shareUrl = album["shareUrl"];

                        if (shareUrl !== null && "" !== shareUrl) {
                            const fullShareLink = baseUrl + "share/" + shareUrl + "/album/" + albumId;
                            $("#shareLink").val(shareUrl);
                            $("#fullShareLinkContainer").css("display", "block");
                            $("#fullShareLink").html("<a target='_blank' href='" + fullShareLink + "'>" + fullShareLink + "</a>");
                            $("#copyLink").attr("data-clipboard-text", fullShareLink);
                        }

                        let sharedAlbumsList = await http.ajax("get", "/api/v1/sharedalbums");

                        if (sharedAlbumsList != null && sharedAlbumsList["sharedAlbums"].length > 0) {
                            let html = "<strong>Share with other users</strong><br>";
                            sharedAlbumsList["sharedAlbums"].forEach(function (shareObj) {
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

                        albumsModalListeners.setAlbumModalListeners(albumId, /*[[${baseUrl}]]*/ '');

                        $("#propsharealbums").modal('show');
                    }
                }
            });

            $("#edit" + albumId).on("click", async function (e) {
                e.preventDefault();

                let http = new Http("sharealbums");
                let data = await http.ajax("get", "/api/v1/album/" + albumId + "/page/0");

                if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album")) {
                    if (data["status"] === "success") {
                        let album = data["album"];

                        $("#albumNameEdit").text(album["name"]);
                        $("#albumEditName").val(album["name"]);
                        $("#albumCoverEditThumb").attr("src", album["coverUrl"]);

                        $("#propeditalbums").modal('show');

                        albumsModalListeners.setEditAlbumsListeners(albumId);
                    }
                }


            });

            $("#trash" + albumId).on("click", async function (e) {
                e.preventDefault();

                let http = new Http("sharealbums");
                let data = await http.ajax("get", "/api/v1/album/" + albumId + "/page/0");

                if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album")) {
                    if (data["status"] === "success") {
                        let album = data["album"];

                        $("#albumNameTrash").text(album["name"]);
                        $("#albumCoverTrashThumb").attr("src", album["coverUrl"]);

                        $("#proptrashalbums").modal('show');

                        albumsModalListeners.setDeleteAlbumsListeners(albumId);
                    }
                }
            });
        }

        $("#comment"+albumId).on("click", async function (e) {
            e.preventDefault();

            let http = new Http("albumcomments");
            let data = await http.ajax("get", "/api/v1/album/" + albumId + "/page/0");
            let currentUserId = $("#currentUserId").val();

            if (data != null && data.hasOwnProperty("status") && data.hasOwnProperty("msg") && data.hasOwnProperty("album") && data["status"] === "success") {
                let album = data["album"];

                $("#albumNameComments").text(album["name"]);
                $("#albumCoverCommentThumb").attr("src", album["coverUrl"]);

                let albumCommentsList = await http.ajax("get", "/api/v1/albumcomments/"+albumId);

                if (albumCommentsList != null && albumCommentsList["albumCommentsList"].length > 0) {
                    let html = "";
                    albumCommentsList = albumCommentsList["albumCommentsList"];
                    albumCommentsList.forEach(function(comments) {
                        if (comments["albumId"] === albumId) {
                            html += '<li id="comment' + comments.commentId + '" class="list-group-item' + (comments.userId === currentUserId ? ' list-group-item-secondary' : '') + '">';
                            html += '<span id="commentcontainer' + comments.commentId + '">';
                            html += '<p id="commentcontent' + comments.commentId + '">' + comments.comment + '</p>';
                            html += '<small><strong>' + comments.username + '</strong> <span>on ' + comments.createdAt + '</span></small>';

                            if (parseInt(comments.userId, 10) === parseInt(currentUserId, 10)) {
                                html += '<small><span style="float: right">';
                                html += '<a href="#" id="deletecomment' + comments.commentId + '"><span class="bi-trash"></span></a>&nbsp;&nbsp';
                                html += '<a href="#" id="editcomment' + comments.commentId + '"><span class="bi-pencil"></span></a>';
                                html += '</span></small>';
                            }
                            html += '</span><span id="textareacontainer' + comments.commentId + '"></span>';
                            html += '</li>';
                        }
                        html += '<script type="text/javascript" nonce="'+cspNonce+'">albumsModalListeners.setEditCommentModalListeners('+comments.commentId+', '+albumId+');<\/script>';
                    });

                    $("#commentList").html(html);
                }

                $("#propcommentalbums").modal('show');

                albumsModalListeners.setCommentModalListeners(albumId, $("#currentUser").val());
            }
        });

        $("#download"+albumId).on("click", function() {
            let downloadTimer;
            const tokenName = "ShashinAlbumName";
            const tokenSize = "ShashinAlbumSize";
            const configuredAttempts = 120;
            const downloadLocation = $("#download"+albumId).attr("href");
            const albumName = $("#albumName"+albumId).text();

            $("#albumsMessage").html("<span class='spinner-grow spinner-grow-sm'></span> <strong>Exporting album \""+albumName+"\". Downloading photos only.</strong>").animate({opacity: 100}, 0);
            setTimeout(function () { $("#download"+albumId).removeAttr("href") }, 0);
                Util.setCookie(tokenName, "", "/");
                Util.setCookie(tokenSize, "", "/");

                let attempts = configuredAttempts;

                downloadTimer = window.setInterval( function() {
                const tokenCookieValue = Util.getCookie(tokenName);
                const tokenCookieSize = Util.getCookie(tokenSize);

                if ((tokenCookieValue !== "" && tokenCookieSize !== "") || attempts === 0) {
                    if (attempts === 0) {
                        $("#albumsMessage").html("&nbsp;").animate({opacity: 0}, 5000);
                    } else {
                        $("#albumsMessage").html("<strong>File name</strong> " + tokenCookieValue + " <strong>File size</strong> " + Util.formatBytes(tokenCookieSize)).animate({opacity: 0}, 10000);
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