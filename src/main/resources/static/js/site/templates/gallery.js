class GalleryTemplates {
    static PhotoGalleryItem({lastLgIndex, activePage, metadata, overlayData, uuid, isMobile}) { return `
        <div id="photoThumbnailContainer${metadata.id}" data-lg-index="${lastLgIndex}" class="photo-thumbnail-container photo-thumbnail" style="width:${isMobile?115:metadata.thumbnailSmallWidth}px;height:${isMobile?115:metadata.thumbnailSmallHeight}px;padding-left:0;padding-right:0;">
            <span class="lightGalleryIndexAnchor"></span>
            
            ${(overlayData.hasOwnProperty("data") && overlayData.data.hasOwnProperty("overlayFlags") && overlayData.data.overlayFlags.hasOwnProperty("renderCenter") && overlayData.data.overlayFlags.renderCenter === true) ? GalleryTemplates.getCenteredOverlay({
                id: metadata.id,
                overlays: overlayData.overlays,
                data: overlayData.data,
                uuid: uuid,
                isMobile: Util.isMobile()
            }) :''}
            
            <input type="hidden" name="filename${metadata.id}" id="filename${metadata.id}" value="${metadata.fileName}">
            <input type="hidden" name="thumbnailCentered${metadata.id}" id="thumbnailCentered${metadata.id}" value="${"/api/v1/thumbnails/centered/"+metadata.id}">
            <input type="hidden" name="dateTaken${metadata.id}" id="dateTaken${metadata.id}" value="${metadata.year}-${metadata.month}-${metadata.day}">
            
            ${(activePage === "share")?
            `<div class="thumbnail-bl" id="tnbl${metadata.id}">
                <a href="/api/v1${(metadata.type.includes("video") ? `/video/` : `/image/`)}${metadata.id}/download?v=${uuidv4()}" id="download${metadata.id}">
                    <span class="bi-download" style="font-size: 1rem;color: lightgray;"></span>
                </a>
            </div>`
            :''}
    
            ${(overlayData.hasOwnProperty("data") && overlayData.data.hasOwnProperty("overlayFlags") && overlayData.data.overlayFlags.hasOwnProperty("renderTopRight") && overlayData.data.overlayFlags.renderTopRight === true) ? GalleryTemplates.getTopRightOverlay({
                id: metadata.id,
                overlays: overlayData.overlays,
                data: overlayData.data
            }) : ''}
            
            ${(overlayData.hasOwnProperty("data") && overlayData.data.hasOwnProperty("overlayFlags") && overlayData.data.overlayFlags.hasOwnProperty("renderTopLeft") && overlayData.data.overlayFlags.renderTopLeft === true) ? GalleryTemplates.getTopLeftOverlay({
                id: metadata.id,
                overlays: overlayData.overlays,
                data: overlayData.data
            }) : ''}
            
            ${(overlayData.hasOwnProperty("data") && overlayData.data.hasOwnProperty("overlayFlags") && overlayData.data.overlayFlags.hasOwnProperty("renderBottomLeft") && overlayData.data.overlayFlags.renderBottomLeft === true) ? GalleryTemplates.getBottomLeftOverlay({
                metadata: metadata,
                overlays: overlayData.overlays,
                data: overlayData.data
            }) : ''}
            
            ${(overlayData.hasOwnProperty("data") && overlayData.data.hasOwnProperty("overlayFlags") && overlayData.data.overlayFlags.hasOwnProperty("renderBottomRight") && overlayData.data.overlayFlags.renderBottomRight === true) ? GalleryTemplates.getBottomRightOverlay({
                id: metadata.id,
                overlays: overlayData.overlays,
                data: overlayData.data
            }) : ''}
            
        </div>
        
        ${(activePage === "album") ? `<span id="albummodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
        ${(activePage === "person") ? `<span id="personmodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
        
        <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="' + shashin.nonce + '"' : '')}>
            $(document).ready(function () {
                shashin.setPhotoOverlays({id:"${metadata.id}",type:"${metadata.type}",lat:"${(metadata.lat === null) ? '' : `${metadata.lat}`}", lng:"${(metadata.lng === null) ? '' : `${metadata.lng}`}", year:${metadata.year}, month:${metadata.month}, day:${metadata.day}, fileName:"${metadata.fileName}", thumbnailUrlSmall:"${"/api/v1/thumbnails/"+(isMobile ? "centered" : "225")+"/"+metadata.id}"}, "${activePage}");
                Util.activateMetadataListeners("${metadata.id}");
                $("#mediaLink${metadata.id}").attr("tag", "${metadata.id}");
                
                ${(overlayData.hasOwnProperty("data") === false || overlayData.data.hasOwnProperty("overlayFlags") === false || overlayData.data.overlayFlags.hasOwnProperty("renderBottomLeft") === false || overlayData.data.overlayFlags.renderBottomLeft === false || $.inArray("isInfo", overlayData.overlays) !== -1) ?
            `
                    $("#infoModalEdit${metadata.id}").on("click", function (e) {
                        e.preventDefault();
                        shashin.openEditMetadataModal("${metadata.id}");
                    })
                    `
            :
            `
                    $("#metadataModalEdit${metadata.id}").on("click", function (e) {
                        e.preventDefault();
                        shashin.openEditMetadataModal("${metadata.id}");
                    })
                    `
            }
    
            $("#image${metadata.id}").on('load', function () {
                $(this).css("background-color", "transparent");
            });
            
            // Util.checkErrorImage();
            });
        </script>        
    `}

    static getTopRightOverlay({id, overlays, data}) {
        const icons = [];

        if ($.inArray("isVideo", overlays) !== -1) {
            icons.push(`
            <span class="overlayIconBackground"><span id="duration${id}">${data.duration}</span>
                <span id="video${id}" class="bi-camera-video overlayIcon" title="video"></span>
            </span>
        `);
        }

        if ($.inArray("isPan", overlays) !== -1) {
            icons.push(`
            <span id="panorama${id}" class="bi-arrows-expand-vertical overlayIcon overlayIconBackground" title="panorama"></span>
        `);
        }

        if ($.inArray("isGif", overlays) !== -1) {
            icons.push(`
            <span id="gif${id}" class="bi-layers overlayIcon overlayIconBackground" title="gif"></span>
        `);
        }

        if ($.inArray("isTagged", overlays) !== -1) {
            icons.push(`
            <span class="bi-bookmark-fill overlayIconBackground" style="font-size: 1rem;color: lightsalmon;"></span>
        `);
        }

        const iconHtml = icons.length > 0 ? `${icons.join('<br>\n')}<br>` : '';

        return `
        <div class="thumbnail-tr" id="tntr${id}">
            ${iconHtml}
        </div>
    `;
    }

    static getTopLeftOverlay({id, overlays, data}) { return `
        <div class="thumbnail-tl" id="tntl${id}">
            <a href="#" id="select${id}">
                <span id="tlicon${id}" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>
            </a>
        </div>
    `}

    static getBottomRightOverlay({id, overlays, data}) {
        const links = [];

        if ($.inArray("isComments", overlays) !== -1) {
            links.push(`
            <a href="#" data-bs-toggle="modal" data-bs-target="#propalbumphotocomment${id}" class="overlayCommentIconBackground overlayCommentText">
                <span id="brcommentcount${id}">${data.albumPhotoCommentsMap.hasOwnProperty(id) ? data.albumPhotoCommentsMap[id].length : `0`}</span> 
                <span id="bricon${id}" class="bi-chat-square position-relative overlayCommentIcon"></span>
            </a>
        `);
        }

        if ($.inArray("isFavorites", overlays) !== -1) {
            links.push(`
            <a href="#" id="favorite${id}" class="text-decoration-none">
                <span class="overlayIconBackground">
                    <span id="briconcount${id}">${data.favoriteCount}</span>&nbsp;<span class="${data.favoriteIcon} overlayIcon" id="brfavoriteicon${id}"></span>
                </span>
            </a>
        `);
        }

        const linkHtml = links.length > 0 ? `<br>${links.join('<br>\n')}` : '';

        return `
        <div class="thumbnail-br" id="tnbr${id}">
            ${linkHtml}
        </div>
    `;
    }

    static getBottomLeftOverlay({metadata, overlays, data}) {
        const links = [];

        if ($.inArray("isBlOnClickFunction", overlays) !== -1) {
            links.push(`
            <a href="#" id="${data.onClickIdPrefix}${metadata.id}">
                <span class="bi-journal-album" style="font-size: 1rem;color: lightgray;" title="${shashin.getTranslatedValue("main.gallery.album.cover")}"></span>
            </a>

            <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="' + shashin.nonce + '"' : '')}>
            $(document).ready(function () {
                $("#${data.onClickIdPrefix}${metadata.id}").on("click", function (e) {
                    e.preventDefault();
                    ${data.blOnClickFunction}(e,"${metadata.id}");
                });
            });
            </script>
        `);
        }

        if ($.inArray("isOnClickIdPrefix", overlays) !== -1) {
            links.push(`
            <a href="#" data-bs-toggle="modal" data-bs-target="#${data.onClickIdPrefix}${metadata.id}">
                <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>
            </a>
        `);
        }

        if ($.inArray("isInfo", overlays) !== -1) {
            links.push(`
            <a href="#" id="infoModalEdit${metadata.id}" tag="${metadata.id}">
                <span class="${(metadata.lat !== null && metadata.lng !== null) ? `bi-info-circle` : `bi-info-square`}" style="font-size: 1rem;color: lightgray;" title="${shashin.getTranslatedValue("main.gallery.album.cover")}"></span>
            </a>
        `);
        }

        if ($.inArray("isEditControls", overlays) !== -1) {
            links.push(`
            <a href="#" id="metadataModalEdit${metadata.id}" data-bs-target="#propTimelinModal" tag="${metadata.id}">
                <span class="${data.editIcon}" style="font-size: 1rem;color: lightgray;"></span>
            </a>
        `);
        }

        const linkHtml = links.length > 0 ? `<br>${links.join('<br>\n')}` : '';

        return `
        <div class="thumbnail-bl" id="tnbl${metadata.id}">
            ${linkHtml}
        </div>
    `;
    }

    static getCenteredOverlay({id, overlays, data, uuid, isMobile}) { return `
        ${($.inArray("isVideo", overlays) !== -1) ?
            `
            <a class="mediaLink" id="mediaLink${id}" data-download-url="${encodeURI(data.metadata.videoUrl).replace(";", "%3B")}/download?v=${uuidv4()}" 
                ${(data.metadata.description !== null ? ` data-sub-html="${data.metadata.description}" ` : '')}
                data-metadata-id="${data.metadata.id}"
                data-poster="${(data.metadata.thumbnailUrlOriginal === null || data.metadata.thumbnailUrlOriginal === "") ? "/api/v1/thumbnails/225/"+data.metadata.id : "/api/v1/thumbnails/original/"+data.metadata.id}?v=${uuid}"
                data-lg-size="${(data.metadata.originalImageWidth === null || data.metadata.originalImageWidth === "") ? `${data.metadata.thumbnailSmallWidth}-${data.metadata.thumbnailSmallHeight}` : `${data.metadata.originalImageWidth}-${data.metadata.originalImageHeight}`}"
                data-video=\'{"source": [{"src":"${data.metadata.videoUrl}", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true, "autoplay": true}}\'>
                <img loading="lazy" draggable="false" data-smallthumb="${"/api/v1/thumbnails/"+(isMobile ? "centered" : "225")+"/"+data.metadata.id}?v=${uuid}" src="${"/api/v1/thumbnails/"+(isMobile ? "centered" : "225")+"/"+data.metadata.id}?v=${uuid}" class="photo-thumbnail-image" id="image${data.metadata.id}" width="${isMobile?115:data.metadata.thumbnailSmallWidth}" height="${isMobile?115:data.metadata.thumbnailSmallHeight}" style="background-color:lightgray;">
            </a>
            `
            :
            `
            <a class="mediaLink" id="mediaLink${id}" data-src="${(data.metadata.thumbnailUrlOriginal === null || data.metadata.thumbnailUrlOriginal === "") ? "/api/v1/image/"+data.metadata.id+"?v="+uuid : "/api/v1/thumbnails/original/"+data.metadata.id+"?v="+uuid}" href="${(data.metadata.thumbnailUrlOriginal === null || data.metadata.thumbnailUrlOriginal === "") ? "/api/v1/image/"+data.metadata.id+"?v="+uuid : "/api/v1/thumbnails/original/"+data.metadata.id+"?v="+uuid}"
                data-download-url="${"/api/v1/image/"+data.metadata.id}/download?v=${uuidv4()}"
                data-metadata-id="${data.metadata.id}"
                ${(data.metadata.description !== null ? ` data-sub-html="${data.metadata.description}" ` : '')}>
                <img loading="lazy" draggable="false" data-smallthumb="${"/api/v1/thumbnails/"+(isMobile ? "centered" : "225")+"/"+data.metadata.id}?v=${uuid}" src="${"/api/v1/thumbnails/"+(isMobile ? "centered" : "225")+"/"+data.metadata.id}?v=${uuid}" class="photo-thumbnail-image" id="image${data.metadata.id}" width="${isMobile?115:data.metadata.thumbnailSmallWidth}" height="${isMobile?115:data.metadata.thumbnailSmallHeight}" style="background-color:lightgray;">
            </a>
            `
        }

        <script type="text/javascript"${(shashin.nonce.length > 0 ? ` nonce="${shashin.nonce}"` : '')}>
            $(document).ready(function () {
                $("#mediaLink${id}").on("click", function (e) {
                    e.preventDefault();
                    ${data.cOnClickFunction}(e,${data.galleryIndex});
                });
            });
        </script>
    `}

    static getFoldersCard({folder, thumbnailUrlCentered, count, appendClass, darkMode, isMobile}) { return `
        <div class="card" style="width:${isMobile ? '175' : '235'}px;padding-top:${isMobile ? '14' : '10'}px;margin-right:-1px;margin-top:-1px;">
            <a href="/folder/${encodeURIComponent(encodeURIComponent(folder)).replace(";", "%3B")}" style="text-decoration: none !important;color: #777777;">
                <img loading="lazy" draggable="false" class="card-img-top" src="${thumbnailUrlCentered}" width="${isMobile ? '148' : '209'}" height="${isMobile ? '148' : '209'}" style="width: ${isMobile ? '148' : '209'}px;height: ${isMobile ? '148' : '209'}px;">
            </a>
            <div class="card-body" style="display: flex;flex-direction: column;">
                <a href="/folder/${encodeURIComponent(encodeURIComponent(folder)).replace(";", "%3B")}" style="text-decoration: none !important;color: ${(darkMode ? '#FFFFFF;' : '#000000;')}">
                    <p class="card-text"><strong>${folder}</strong></p>
                </a>
                <span style="margin-top: auto;">
                    <p class="card-text"><small class="text-muted">${count} ${((count === 1) ? `item` : `items`)}</small></p>
                </span>
            </div>
        </div>
        
        <span class="${appendClass}" style="width:0;height:0;padding:0"></span>
    `}
}