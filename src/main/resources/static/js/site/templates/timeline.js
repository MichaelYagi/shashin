class TimelineTemplates {
    static TimelineToc({index, mediaTypeFilter, metadataDates, year, month, day}) { return `
        ${(index === 0 || (index > 0 && metadataDates[index - 1].year !== year)) ? `
            <strong>${year}</strong>
            <div class='list-group'>
        ` : ''}
    
        ${(index > 0 && metadataDates[index - 1].year === year && metadataDates[index - 1].month === month) ?
            `
                <a style="display:none" id="offcanvas_${year}-${month}-${day}" class="list-group-item list-group-item-action${(index === 0) ? ` active` : ''}" href="#${year}-${month}-${day}"></a>
        `
            :
            `
                <a id="offcanvas_${year}-${month}-${day}" class="list-group-item list-group-item-action${(index === 0) ? ` active` : ''}" href="#${year}-${month}-${day}">${new Date(year, month - 1, day).format("mmm yyyy")}</a>
        `}
        
                <script type="text/javascript"${(shashin.nonce.length > 0) ? ` nonce="${shashin.nonce}"` : ''}>
                $(document).ready(function () {
                    $("#offcanvas_${year}-${month}-${day}").off("click").on("click", function (e) {
                        e.preventDefault();
                        const jumpFromTimelineToc = timelineSettings.once(timelineSettings.jumpFromTimelineToc);
                        jumpFromTimelineToc(e,"${year}-${month}-${day}","${mediaTypeFilter}");
                    });
                });
                </script>
    
        ${(index > 0 && index < metadataDates.length - 1 && metadataDates[index + 1].year !== year) ? `
            </div>
            <br>
        ` : ''}
    `}

    static MapLinks({metadata, placeNameDisplayName, queryParamDates}) { return `
        <a href="/map?lat=${metadata.lat}&lng=${metadata.lng}${queryParamDates}" target="_blank" style="text-decoration: none;"><span class="bi-geo-alt-fill" style="font-size:1.0rem;"></span>${placeNameDisplayName}</a>&nbsp;<a href="https://www.google.com/maps/search/?api=1&query=${metadata.lat}%2C${metadata.lng}" target="_blank" class="bi-google" style="text-decoration: none;"></a>
    `}

    static HeaderThumbnail({metadata,version,showMap}) { return `
        <img loading="lazy" draggable="false" src="${"/api/v1/thumbnails/centered/"+metadata.id+(version === "" ? "" : "?v=" + version)}" height="100" width="100" class="mb-3">${(metadata.lat !== null && metadata.lng !== null && showMap === true) ? `<div id="headerMap" class="map ps-2" style="width: 108px; height: 100px; overflow: hidden;"></div>` : ''}
    `}

    static BatchHeaderThumbnail({thumbnailImage, title, version}) { return `
        <img class="me-1" loading="lazy" draggable="false" src="${thumbnailImage+(version === "" ? "" : "?v=" + version)}" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="${title}">
    `}

    static TimelinePreLoadGalleryHeader({metadata, placeNameHeaders, listHtml, isMobile, locale}) { return `
        ${(metadata.year === null || metadata.month === null || metadata.day === null) ?
            `
        <span class="dateContainer" id="container_undated">
        <br id="brundated">
        <section class="scrollspy" id="undated"><p><strong class="undatedTimelinePhotos p-1">Undated</strong></p></section>
        <div class="row${isMobile ? " ms-0" : " image-group-padding"}" id="rowundated">
        `
            :
            `
        <span class="dateContainer" id="container_${metadata.year}-${metadata.month}-${metadata.day}">
        <br id="br${metadata.year}-${metadata.month}-${metadata.day}">
        <section class="scrollspy" id="${metadata.year}-${metadata.month}-${metadata.day}"><div class="dateHeader"><span id="select${metadata.year}-${metadata.month}-${metadata.day}" class="bi-circle pe-2 day-select" style="font-size: 0.85rem;color: lightgray;display: none"></span><strong class="dateHeading pe-1">${Util.getDateString(metadata.year, metadata.month, metadata.day, locale)}</strong>
        ${(placeNameHeaders.length === 1) ? `<span class="text-muted"><a class="link-unstyled" href="/search?term=`+placeNameHeaders[0]+`" target="_blank" id="placeNameHeader`+metadata.year+'-'+metadata.month+'-'+metadata.day+`">`+placeNameHeaders[0]+`</a></span>` : (placeNameHeaders.length > 1) ? `
        <span class="text-muted"><div class="dropdown" style="display: inline-block;"><a class="dropdown-toggle link-unstyled" data-bs-toggle="dropdown" href="#">`+placeNameHeaders[0]+`</a>
        <ul class="dropdown-menu">`+listHtml+`</ul></div></span>` : ``}</div></section>
        <div class="row${isMobile ? " ms-0" : " image-group-padding"}" id="row${metadata.year}-${metadata.month}-${metadata.day}">
        <span style="display: none;" class="yearTaken">${metadata.year}</span>
        <span style="display: none;" class="monthTaken">${metadata.month}</span>
        <span style="display: none;" class="dayTaken">${metadata.day}</span>                    
        `}
    `}

    static TimelinePreLoadGalleryBody({metadata,videoData,uuid,isMobile,thumbnailType,thumbnailHeight,index}) { return `
        <div id="photoThumbnailContainer${metadata.id}" data-lg-index="${index}" class="photo-thumbnail-container photo-thumbnail ${(metadata.type.includes('video') ? `is-video` : `is-not-video`)}" style="width:${isMobile ? thumbnailHeight : metadata.thumbnailSmallWidth}px;height:${isMobile ? thumbnailHeight : metadata.thumbnailSmallHeight}px;padding-left:0;padding-right:0;">
            <a class="lightGalleryIndexAnchor" id="lightGalleryIndex${metadata.id}"></a>
            <input type="hidden" name="filename${metadata.id}" id="filename${metadata.id}" value="${metadata.fileName}">
            <input type="hidden" name="thumbnailCentered${metadata.id}" id="thumbnailCentered${metadata.id}" value="${"/api/v1/thumbnails/centered/"+metadata.id}">
            ${(metadata.year == null || metadata.month == null || metadata.day == null) ?
            `
            <input type="hidden" name="thumbnailUrl-undated[]" id="thumbnailUrl_${metadata.id}" value="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id }">
            <img loading="lazy" draggable="false" class="photo-thumbnail-image thumbnailTag_undated" id="image${metadata.id}" width="${isMobile ? thumbnailHeight : metadata.thumbnailSmallWidth}" height="${isMobile ? thumbnailHeight : metadata.thumbnailSmallHeight}" src="${Util.getPlaceholderBackground()}">
            ` :
            `
            <input type="hidden" name="thumbnailUrl-${metadata.year}-${metadata.month}-${metadata.day}[]" id="thumbnailUrl_${metadata.id}" value="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id}">
            <a class="mediaLink" 
            id="mediaLink${metadata.id}"
            data-download-url="${(metadata.type.indexOf("video") >= 0) ? encodeURI(metadata.videoUrl) : "/api/v1/image/"+metadata.id}/download?v=${uuid}"
            data-metadata-id="${metadata.id}"
            data-lg-size="${(metadata.originalImageWidth === null || metadata.originalImageWidth === "") ? `${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallHeight}` : `${metadata.originalImageWidth}-${metadata.originalImageHeight}`}"
            ${(metadata.type.indexOf("video") >= 0) ? `data-video="${Util.encodeHtml(videoData)}" data-poster="${(metadata.thumbnailUrlOriginal === null || metadata.thumbnailUrlOriginal === "") ? "/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id+"?v="+uuid : "/api/v1/thumbnails/original/"+metadata.id+"?v="+uuid}"` : `data-src="${(metadata.thumbnailUrlOriginal === null || metadata.thumbnailUrlOriginal === "") ? "/api/v1/image/"+metadata.id+"?v="+uuid : "/api/v1/thumbnails/original/"+metadata.id+"?v="+uuid}"`}
            ${(metadata.description != null) ? `data-sub-html="${Util.encodeHtml(metadata.description)}"` : ''}
    
            ${(metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
                metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) ?
                `
            data-lg-size="${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallHeight},${metadata.originalImageWidth}-${metadata.originalImageHeight}"
            data-responsive="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id} ${metadata.thumbnailSmallWidth}"
            data-thumb="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id}"
            data-width="${metadata.originalImageWidth}"
            ` : ''}
            >
                <img loading="lazy" draggable="false" class="photo-thumbnail-image thumbnailTag_${metadata.year}-${metadata.month}-${metadata.day}" id="image${metadata.id}" width="${isMobile ? thumbnailHeight : metadata.thumbnailSmallWidth}" height="${isMobile ? thumbnailHeight : metadata.thumbnailSmallHeight}" src="${Util.getPlaceholderBackground()}">
            </a>
            `}
            
            <div id="tntl${metadata.id}"></div>
            <div id="tnbr${metadata.id}"></div>
            <div id="tnbl${metadata.id}"></div>
            <div id="tntr${metadata.id}"></div>
    
            <span id="metadatamodal${metadata.id}"></span>
        </div>
    `}

    static TimelinePreLoadGalleryFooter({metadata, lastDate}) { return `
        ${(metadata.year === null || metadata.month === null || metadata.day === null) ?
            `
                <span class="scrollspy metadataprocessed" id="tail_undated"></span>
            </div>
        </span>
        <span class="attachMetadataPhotos" id="amp_undated" style="visibility: hidden">EOL</span>
        `
            :
            `
            ${(lastDate === (metadata.year + '-' + metadata.month + '-' + metadata.day)) ? `
                    <span class="scrollspy metadataprocessed" id="tail_${metadata.year}-${metadata.month}-${metadata.day}"></span>
                </div>
            </span>
            <span class="attachMetadataPhotos" id="amp_${metadata.year}-${metadata.month}-${metadata.day}" style="visibility: hidden">EOL</span>
            `
                :
                `
                    <span class="scrollspy metadataprocessed" id="tail_${metadata.year}-${metadata.month}-${metadata.day}"></span>
                </div>
            </span>
            <span class="attachMetadataPhotos" id="amp_${metadata.year}-${metadata.month}-${metadata.day}"></span>
            `}                       
        `}
    `}

    static TimelineGalleryBottomLeftOverlay({metadata, editIcon}) { return `
        <a href="#" id="metadataModalEdit${metadata.id}" data-bs-target="#propTimelinModal">
            <span class="${editIcon}" style="font-size: 1rem;color: lightgray;"></span>
        </a>
    `}

    static TimelineGalleryTopLeftOverlay({metadata}) { return `
        <a href="#" id="select${metadata.id}">
            <span id="tlicon${metadata.id}" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>
        </a>
    `}

    static TimelineGalleryTopRightOverlay({metadata}) { return `
        ${metadata.type.indexOf("video") >= 0 ?
            `
            <span class="overlayIconBackground"><span id="duration${metadata.id}">${(metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00"}</span>&nbsp;<span id="video${metadata.id}" class="bi-camera-video overlayIcon" title="video"></span></span>
            `
            :
            ` 
                ${(metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight * 2) ?
                    `
                        <span id="panorama${metadata.id}" class="bi-arrows-expand-vertical overlayIcon overlayIconBackground" title="panorama"></span>
                    `
                :
                `
                    ${metadata.expectedExtension === "gif" ?
                    `
                    <span id="gif${metadata.id}" class="bi-layers overlayIcon overlayIconBackground" title="gif"></span>
                    `
                    : ''
                }
                `
            }
            `
        }
    `}

    static TimelineGalleryBottomRightOverlay({metadata}) { return `
        <a href="#" id="favorite${metadata.id}" class="text-decoration-none">
            <span class="overlayIconBackground">
                <span id="briconcount${metadata.id}"></span> <span id="bricon${metadata.id}" class="overlayIcon"></span>
            </span>
        </a>
    `}

    static TimelineGalleryCenterOverlay({metadata, mediaContent, uuid, isMobile, thumbnailType, thumbnailHeight}) { return `
        <a class="mediaLink" 
            id="mediaLink${metadata.id}"
            data-download-url="${(metadata.type.indexOf("video") >= 0) ? encodeURI(metadata.videoUrl) : "/api/v1/image/"+metadata.id}/download?v=${uuidv4()}"
            data-metadata-id="${metadata.id}"
            data-lg-size="${(metadata.originalImageWidth === null || metadata.originalImageWidth === "") ? `${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallHeight}` : `${metadata.originalImageWidth}-${metadata.originalImageHeight}`}"
            ${(metadata.type.indexOf("video") >= 0) ? `data-video="${Util.encodeHtml(mediaContent.video)}" data-poster="${(metadata.thumbnailUrlOriginal === null || metadata.thumbnailUrlOriginal === "") ? "/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id : "/api/v1/thumbnails/original/"+metadata.id}?v=${uuid}"` : `data-src="${(metadata.thumbnailUrlOriginal === null || metadata.thumbnailUrlOriginal === "") ? "/api/v1/image/"+metadata.id+"?v="+uuid : "/api/v1/thumbnails/original/"+metadata.id+"?v="+uuid}"`}
            ${(metadata.description != null) ? `data-sub-html="${Util.encodeHtml(metadata.description)}"` : ''}
    
            ${(metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
            metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) ?
            `
            data-lg-size="${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallHeight},${metadata.originalImageWidth}-${metadata.originalImageHeight}"
            data-responsive="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id} ${metadata.thumbnailSmallWidth}"
            data-thumb="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id}"
            data-width="${metadata.originalImageWidth}"
            ` : ''}
            >
            <span class="${(metadata.type.indexOf("video") >= 0) ? `bi-play-circle` : `bi-play-btn`}" style="font-size: ${isMobile ? '1.5' : '4'}rem;color: lightgray;"></span>
        </a>
    `}

    static TimelineGalleryImageOverlay({metadata, mediaContent, uuid, isMobile, thumbnailType, thumbnailHeight}) { return `
        <a class="mediaLink" 
            id="mediaLink${metadata.id}"
            data-download-url="${(metadata.type.indexOf("video") >= 0) ? encodeURI(metadata.videoUrl) : "/api/v1/image/"+metadata.id}/download?v=${uuidv4()}"
            data-metadata-id="${metadata.id}"
            data-lg-size="${(metadata.originalImageWidth === null || metadata.originalImageWidth === "") ? `${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallHeight}` : `${metadata.originalImageWidth}-${metadata.originalImageHeight}`}"
            ${(metadata.type.indexOf("video") >= 0) ? `data-video="${Util.encodeHtml(mediaContent.video)}" data-poster="${(metadata.thumbnailUrlOriginal === null || metadata.thumbnailUrlOriginal === "") ? "/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id : "/api/v1/thumbnails/original/"+metadata.id}?v=${uuid}"` : `data-src="${(metadata.thumbnailUrlOriginal === null || metadata.thumbnailUrlOriginal === "") ? "/api/v1/image/"+metadata.id+"?v="+uuid : "/api/v1/thumbnails/original/"+metadata.id+"?v="+uuid}"`}
            ${(metadata.description != null) ? `data-sub-html="${Util.encodeHtml(metadata.description)}"` : ''}
    
            ${(metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
            metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) ?
            `
            data-lg-size="${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallHeight},${metadata.originalImageWidth}-${metadata.originalImageHeight}"
            data-responsive="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id} ${metadata.thumbnailSmallWidth}"
            data-thumb="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id}"
            data-width="${metadata.originalImageWidth}"
            ` : ''}
            >
            <img loading="lazy" draggable="false" class="${"photo-thumbnail-image thumbnailTag_"+metadata.year+'-'+metadata.month+'-'+metadata.day}" id="${"image"+metadata.id}" width="${isMobile ? thumbnailHeight : metadata.thumbnailSmallWidth}" height="${isMobile ? thumbnailHeight : metadata.thumbnailSmallHeight}" src="${"/api/v1/thumbnails/"+thumbnailType+"/"+metadata.id}">
        </a>
    `}
}