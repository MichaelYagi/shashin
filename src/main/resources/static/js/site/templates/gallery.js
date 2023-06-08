class GalleryTemplates {
    static PhotoGalleryItem({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, overlayData}) { return `
        ${(typeof dateHeadingObj === "undefined" || dateHeadingObj === null) ? '' : `<section class="dateSection" id="${dateHeadingObj.heading}"><p><strong>${dateHeadingObj.display}</strong></p></section>`}
        <div id="photoThumbnailContainer${metadata.id}" class="photo-thumbnail-container photo-thumbnail" style="width:${metadata.thumbnailSmallWidth}px;height:${metadata.thumbnailSmallHeight}px;padding-left:0;padding-right:0;">
            <a class="lightGalleryIndexAnchor" name="lightGalleryIndex${currentMediaLinkIndex}"></a>
            <img loading="lazy" src="${encodeURI(metadata.thumbnailUrlSmall)}" class="photo-thumbnail-image" id="image${metadata.id}" width="${metadata.thumbnailSmallWidth}" height="${metadata.thumbnailSmallHeight}" style="background-color:lightgray;">
            <input type="hidden" name="filenamee${metadata.id}" id="filename${metadata.id}" value="${metadata.fileName}">
            <input type="hidden" name="thumbnailCentered${metadata.id}" id="thumbnailCentered${metadata.id}" value="${encodeURI(metadata.thumbnailUrlCentered)}">
    
            ${(overlayData.hasOwnProperty("data") && overlayData["data"].hasOwnProperty("overlayFlags") && overlayData["data"]["overlayFlags"].hasOwnProperty("renderTopRight") && overlayData["data"]["overlayFlags"]["renderTopRight"] === true) ? GalleryTemplates.getTopRightOverlay({
            id: metadata.id,
            overlays: overlayData.overlays,
            data: overlayData.data
        }) : ''}
            
            ${(overlayData.hasOwnProperty("data") && overlayData["data"].hasOwnProperty("overlayFlags") && overlayData["data"]["overlayFlags"].hasOwnProperty("renderTopLeft") && overlayData["data"]["overlayFlags"]["renderTopLeft"] === true) ? GalleryTemplates.getTopLeftOverlay({
            id: metadata.id,
            overlays: overlayData.overlays,
            data: overlayData.data
        }) : ''}
            
            ${(overlayData.hasOwnProperty("data") && overlayData["data"].hasOwnProperty("overlayFlags") && overlayData["data"]["overlayFlags"].hasOwnProperty("renderBottomLeft") && overlayData["data"]["overlayFlags"]["renderBottomLeft"] === true) ? GalleryTemplates.getBottomLeftOverlay({
            id: metadata.id,
            overlays: overlayData.overlays,
            data: overlayData.data
        }) : ''}
            
            ${(overlayData.hasOwnProperty("data") && overlayData["data"].hasOwnProperty("overlayFlags") && overlayData["data"]["overlayFlags"].hasOwnProperty("renderBottomRight") && overlayData["data"]["overlayFlags"]["renderBottomRight"] === true) ? GalleryTemplates.getBottomRightOverlay({
            id: metadata.id,
            overlays: overlayData.overlays,
            data: overlayData.data
        }) : ''}
            
            ${(overlayData.hasOwnProperty("data") && overlayData["data"].hasOwnProperty("overlayFlags") && overlayData["data"]["overlayFlags"].hasOwnProperty("renderCenter") && overlayData["data"]["overlayFlags"]["renderCenter"] === true) ? GalleryTemplates.getCenteredOverlay({
            id: metadata.id,
            overlays: overlayData.overlays,
            data: overlayData.data
        }) : ''}
            
        </div>
        ${(activePage === "album") ? `<span id="albummodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
        ${(activePage === "person") ? `<span id="personmodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
        <span class="${appendClass}" style="width:0;height:0;padding:0"></span>
        
        <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="' + shashin.nonce + '"' : '')}>
            shashin.setPhotoOverlays({id:"${metadata.id}",lat:"${(metadata.lat === null) ? '' : `${metadata.lat}`}", lng:"${(metadata.lng === null) ? '' : `${metadata.lng}`}", year:${metadata.year}, month:${metadata.month}, day:${metadata.day}, fileName:"${metadata.fileName}"}, "${activePage}");
            Util.activateMetadataListeners("${metadata.id}");
            $("#mediaLink${metadata.id}").attr("tag", "${metadata.id}");
            
            ${(overlayData.hasOwnProperty("data") === false || overlayData["data"].hasOwnProperty("overlayFlags") === false || overlayData["data"]["overlayFlags"].hasOwnProperty("renderBottomLeft") === false || overlayData["data"]["overlayFlags"]["renderBottomLeft"] === false || $.inArray("isInfo", overlayData.overlays) !== -1) ?
            `
                $("#infoModalEdit${metadata.id}").on("click", function (e) {
                    e.preventDefault();
                    shashin.openInfoModal("${metadata.id}");
                })
                `
            :
            `
                $("#timelineModalEdit${metadata.id}").on("click", function (e) {
                    e.preventDefault();
                    shashin.openEditMetadataModal("${metadata.id}");
                })
                `
        }
    
            $("#image${metadata.id}").on('load', function () {
                $(this).css("background-color", "transparent");
            });
            
            Util.checkErrorImage();
        </script>
    `};

    static getTopRightOverlay({id, overlays, data}) { return `
        <div class="thumbnail-tr" id="tntr${id}">
    
        ${($.inArray("isVideo", overlays) !== -1) ?
            `
            <span class="overlayIconBackground">${data.duration}&nbsp;
                <span id="video${id}" class="bi-camera-video overlayIcon"></span>
            </span>
            <br>
            ` : ''
        } 
        
        ${($.inArray("isPan", overlays) !== -1) ?
            `
            <span id="panorama${id}" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>
            <br>
            ` : ''
        }
        
        ${($.inArray("isGif", overlays) !== -1) ?
        `
            <span id="gif${id}" class="bi-layers overlayIcon overlayIconBackground"></span>
            <br>
            ` : ''
    }
        
        ${($.inArray("isTagged", overlays) !== -1) ?
            `
            <span class="bi-bookmark-fill overlayIconBackground" style="font-size: 1rem;color: lightsalmon;"></span>
            ` : ''
        }
    
        </div>
    `};

    static getTopLeftOverlay({id, overlays, data}) { return `
        <div class="thumbnail-tl" id="tntl${id}">
            <a href="#" id="select${id}">
                <span id="tlicon${id}" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>
            </a>
        </div>
    `};

    static getBottomRightOverlay({id, overlays, data}) { return `
        <div class="thumbnail-br" id="tnbr${id}">
            ${($.inArray("isFavorites", overlays) !== -1) ?
            `
            <a href="#" id="favorite${id}" class="text-decoration-none">
                <span class="overlayIconBackground">
                    <span id="briconcount${id}">${data.favoriteCount}</span>&nbsp;<span class="${data.favoriteIcon} overlayIcon" id="brfavoriteicon${id}"></span>
                </span>
            </a>
            ` : ''}
            
            ${($.inArray("isComments", overlays) !== -1) ?
            `
            <br>
            <a href="#" data-bs-toggle="modal" data-bs-target="#propalbumphotocomment${id}" class="overlayCommentIconBackground overlayCommentText">
                <span id="brcommentcount${id}">${data.albumPhotoCommentsMap.hasOwnProperty(id) ? data.albumPhotoCommentsMap[id].length : `0`}</span> 
                <span id="bricon${id}" class="bi-chat-square position-relative overlayCommentIcon">
                    ${(data.notificationMap !== null && data.notificationMap[id] === true) ? `
                    <span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle">
                        <span class="visually-hidden">New alerts</span>
                    </span>
                    ` : ''}
                </span>
            </a>
            ` : ''}
        </div>
    `};

    static getBottomLeftOverlay({id, overlays, data}) { return `
        <div class="thumbnail-bl" id="tnbl${id}">
            ${($.inArray("isEditControls", overlays) !== -1) ?
            `
            <a href="#" id="timelineModalEdit${id}" data-bs-target="#propTimelinModal">
                <span class="${data.editIcon}" style="font-size: 1rem;color: lightgray;"></span>
            </a>
            ` : ''}
        
            ${($.inArray("isInfo", overlays) !== -1) ?
            `
            <a href="#" id="infoModalEdit${id}">
                <span class="bi-info-circle" style="font-size: 1rem;color: lightgray;"></span>
            </a>
            ` : ''}
            
            ${($.inArray("isBlOnClickFunction", overlays) !== -1) ?
            `
            <br>
            <a href="#" id="${data.onClickIdPrefix}${id}">
                <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>
            </a>
    
            <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="' + shashin.nonce + '"' : '')}>
                $("#${data.onClickIdPrefix}${id}").on("click", function (e) {
                    e.preventDefault();
                    ${data.blOnClickFunction}(e,"${id}");
                });
            </script>
            ` : ''}
            
            ${($.inArray("isOnClickIdPrefix", overlays) !== -1) ?
            `
            <br>
            <a href="#" data-bs-toggle="modal" data-bs-target="#${data.onClickIdPrefix}${id}">
                <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>
            </a>
            ` : ''}
        </div>
    `};

    static getCenteredOverlay({id, overlays, data}) { return `
        <div class="thumbnail-centered" id="tncentered${id}">
    
            ${($.inArray("isVideo", overlays) !== -1) ?
            `
            <a class="mediaLink" id="mediaLink${id}" data-download-url="${encodeURI(data.metadata.videoUrl)}/download" 
                ${(data.metadata.description !== null ? ` data-sub-html="${data.metadata.description}" ` : '')}
                data-video=\'{"source": [{"src":"${data.metadata.videoUrl}", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true, "autoplay": true}}\'>
                <span class="bi-play-btn" style="font-size: 4rem;color: lightgray;"></span>
            </a>
            `
            :
            `
            <a class="mediaLink" id="mediaLink${id}" data-src="${data.metadata.thumbnailUrlOriginal}" href="${data.metadata.thumbnailUrlOriginal}"
                data-download-url="${encodeURI(data.metadata.thumbnailUrlOriginal)}/download"
                ${(data.metadata.description !== null ? ` data-sub-html="${data.metadata.description}" ` : '')}>
                <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>
            </a>
            `
        }
    
            <script type="text/javascript"${(shashin.nonce.length > 0 ? ` nonce="${shashin.nonce}"` : '')}>
                $("#mediaLink${id}").on("click", function (e) {
                    e.preventDefault();
                    ${data.cOnClickFunction}(e,${data.galleryIndex});
                });
            </script>
    
        </div>
    `};

    static getFoldersCard({folder, thumbnailUrlCentered, count, appendClass}) { return `
        <div class="card" style="width:235px;padding-top:10px;">
            <a href="/folder/${encodeURIComponent(encodeURIComponent(folder))}" style="text-decoration: none !important;color: #777777;">
                <img loading="lazy" class="card-img-top" src="${thumbnailUrlCentered}" width="209" height="209" style="width: 209px;height: 209px;">
            </a>
            <div class="card-body">
                <p class="card-text"><strong>${folder}</strong></p>
                <p class="card-text"><small class="text-muted">${count} items</small></p>
            </div>
        </div>
        
        <span class="${appendClass}" style="width:0;height:0;padding:0"></span>
    `};
}