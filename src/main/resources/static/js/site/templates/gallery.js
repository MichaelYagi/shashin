const PhotoGalleryItem = ({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, renderTopRight, renderTopLeft, renderBottomLeft, renderCenter, renderBottomRight, overlayData}) => `
    ${(typeof dateHeadingObj === "undefined" || dateHeadingObj === null) ? '' : `<section class="dateSection" id="${dateHeadingObj.heading}"><p><strong>${dateHeadingObj.display}</strong></p></section>`}
    <div id="photoThumbnailContainer${metadata.id}" class="photo-thumbnail-container photo-thumbnail" style="width:${metadata.thumbnailSmallWidth}px;height:${metadata.thumbnailSmallHeight}px;padding-left:0;padding-right:0;">
        <a class="lightGalleryIndexAnchor" name="lightGalleryIndex${currentMediaLinkIndex}"></a>
        <img loading="lazy" src="${encodeURI(metadata.thumbnailUrlSmall)}" class="photo-thumbnail-image" id="image${metadata.id}" width="${metadata.thumbnailSmallWidth}" height="${metadata.thumbnailSmallHeight}" style="background-color:lightgray;" onError="Util.errorImg(this,\\'${metadata.title}\\',Util.thumbnailHeight())">
        <input type="hidden" name="filenamee${metadata.id}" id="filename${metadata.id}" value="${metadata.fileName}">
        <input type="hidden" name="thumbnailCentered${metadata.id}" id="thumbnailCentered${metadata.id}" value="${encodeURI(metadata.thumbnailUrlCentered)}">

        ${(typeof renderTopRight === "undefined" || renderTopRight === false) ? '' : getTopRightOverlay({id:metadata.id, overlays:overlayData.overlays, data:overlayData.data})}
        
        ${(typeof renderTopLeft === "undefined" || renderTopLeft === false) ? '' : getTopLeftOverlay({id:metadata.id, overlays:overlayData.overlays, data:overlayData.data})}
        
        ${(typeof renderBottomLeft === "undefined" || renderBottomLeft === false) ? '' : getBottomLeftOverlay({id:metadata.id, overlays:overlayData.overlays, data:overlayData.data})}
        
        ${(typeof renderBottomRight === "undefined" || renderBottomRight === false) ? '' : getBottomRightOverlay({id:metadata.id, overlays:overlayData.overlays, data:overlayData.data})}
        
        ${(typeof renderCenter === "undefined" || renderCenter === false) ? '' : getCenteredOverlay({id:metadata.id, overlays:overlayData.overlays, data:overlayData.data})}
        
    </div>
    ${(activePage === "album") ? `<span id="albummodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
    ${(activePage === "person") ? `<span id="personmodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
    <span class="${appendClass}" style="width:0;height:0;padding:0"></span>
    
    <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="'+shashin.nonce+'"' : '')}>
        shashin.setPhotoOverlays({id:"${metadata.id}",lat:"${(metadata.lat === null) ? '' : `${metadata.lat}`}", lng:"${(metadata.lng === null) ? '' : `${metadata.lng}`}", year:${metadata.year}, month:${metadata.month}, day:${metadata.day}, fileName:"${metadata.fileName}"}, "${activePage}");
        Util.activateMetadataListeners("${metadata.id}");
        $("#mediaLink${metadata.id}").attr("tag", "${metadata.id}");
        
        ${(typeof renderBottomLeft === "undefined" || renderBottomLeft === false || $.inArray("isInfo", overlayData.overlays) !== -1) ?
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
    </script>
`;

const getTopRightOverlay = ({id, overlays, data}) => `
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
    
    ${($.inArray("isTagged", overlays) !== -1) ?
        `
        <span class="bi-bookmark-fill overlayIconBackground" style="font-size: 1rem;color: lightsalmon;"></span>
        ` : ''
    }

    </div>
`

const getTopLeftOverlay = ({id, overlays, data}) => `
    <div class="thumbnail-tl" id="tntl${id}">
        <a href="#" id="select${id}">
            <span id="tlicon${id}" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>
        </a>
    </div>
`;

const getBottomRightOverlay = ({id, overlays, data}) => `
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
`

const getBottomLeftOverlay = ({id, overlays, data}) => `
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
        `: ''}
        
        ${($.inArray("isBlOnClickFunction", overlays) !== -1) ?
        `
        <br>
        <a href="#" id="${data.onClickIdPrefix}${id}">
            <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>
        </a>

        <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="'+shashin.nonce+'"' : '')}>
            $("#${data.onClickIdPrefix}${id}").on("click", function (e) {
                e.preventDefault();
                ${data.blOnClickFunction}(e,"${id}");
            });
        </script>
        `: ''}
        
        ${($.inArray("isOnClickIdPrefix", overlays) !== -1) ?
        `
        <br>
        <a href="#" data-bs-toggle="modal" data-bs-target="#${data.onClickIdPrefix}${id}">
            <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>
        </a>
        `: ''}
    </div>
`;

const getCenteredOverlay = ({id, overlays, data}) => `
    <div class="thumbnail-centered" id="tncentered${id}">

        ${($.inArray("isVideo", overlays) !== -1) ?
        `
        <a class="mediaLink" id="mediaLink${id}" data-download-url="${encodeURI(data.metadata.videoUrl)}/download" 
            ${data.metadata.description !== null ?? ` data-sub-html="${data.metadata.description}" `}
            data-video=\'{"source": [{"src":"${data.metadata.videoUrl}", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}\'>
            <span class="bi-play-btn" style="font-size: 4rem;color: lightgray;"></span>
        </a>
        `
        :
        `
        <a class="mediaLink" id="mediaLink${id}" data-src="${data.metadata.thumbnailUrlOriginal}" href="${data.metadata.thumbnailUrlOriginal}"
            data-download-url="${encodeURI(data.metadata.thumbnailUrlOriginal)}"
            ${data.metadata.description !== null ?? ` data-sub-html="${data.metadata.description}" `}>
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
`;

const TimelinePreLoadGalleryHeader = ({metadata}) => `
    ${(metadata["year"] === null || metadata["month"] === null || metadata["day"] === null) ?
    `
    <span class="dateContainer" id="container_undated">
    <br id="brundated">
    <section class="scrollspy" id="undated"><p><strong class="undatedTimelinePhotos p-1">Undated</strong></p></section>
    <div class="row image-group-padding" id="rowundated">
    `
    :
    `
    <span class="dateContainer" id="container_${metadata.year}-${metadata.month}-${metadata.day}">
    <br id="br${metadata.year}-${metadata.month}-${metadata.day}">
    <section class="scrollspy" id="${metadata.year}-${metadata.month}-${metadata.day}"><p><strong class="dateHeading p-1">${Util.getDateString(metadata.year, metadata.month, metadata.day)}</strong></p></section>
    <div class="row image-group-padding" id="row${metadata.year}-${metadata.month}-${metadata.day}">
    <span style="display: none;" class="yearTaken">${metadata.year}</span>
    <span style="display: none;" class="monthTaken">${metadata.month}</span>
    <span style="display: none;" class="dayTaken">${metadata.day}</span>                    
    `}
`

const TimelinePreLoadGalleryBody = ({metadata}) => `
    <div id="photoThumbnailContainer${metadata.id}" class="photo-thumbnail-container photo-thumbnail ${(metadata.type.includes('video') ? `is-video` : `is-not-video`)}" style="width:${metadata.thumbnailSmallWidth}px;height:${metadata.thumbnailSmallHeight}px;padding-left:0;padding-right:0;">
        <a class="lightGalleryIndexAnchor" id="lightGalleryIndex${metadata.id}"></a>
        <input type="hidden" name="filename${metadata.id}" id="filename${metadata.id}" value="${metadata.fileName}">
        <input type="hidden" name="thumbnailCentered${metadata.id}" id="thumbnailCentered${metadata.id}" value="${encodeURI(metadata.thumbnailUrlCentered)}">
        ${(metadata.year == null || metadata.month == null || metadata.day == null) ?
        `
        <input type="hidden" name="thumbnailUrl-undated[]" id="thumbnailUrl_${metadata.id}" value="${encodeURI(metadata.thumbnailUrlSmall)}">
        <img loading="lazy" class="photo-thumbnail-image thumbnailTag_undated" id="image${metadata.id}" width="${metadata.thumbnailSmallWidth}" height="${metadata.thumbnailSmallHeight}">
        ` :
        `
        <input type="hidden" name="thumbnailUrl-${metadata.year}-${metadata.month}-${metadata.day}[]" id="thumbnailUrl_${metadata.id}" value="${encodeURI(metadata.thumbnailUrlSmall)}">
        <img loading="lazy" class="photo-thumbnail-image thumbnailTag_${metadata.year}-${metadata.month}-${metadata.day}" id="image${metadata.id}" width="${metadata.thumbnailSmallWidth}" height="${metadata.thumbnailSmallHeight}">
        `}
        
        <div id="tntl${metadata.id}"></div>
        <div id="tnbr${metadata.id}"></div>
        <div id="tnbl${metadata.id}"></div>
        <div id="tntr${metadata.id}"></div>
        <div id="tncentered${metadata.id}"></div>

        <span id="timelinemodal${metadata.id}"></span>
    </div>
`

const TimelinePreLoadGalleryFooter = ({metadata, lastDate}) => `
    ${(metadata["year"] === null || metadata["month"] === null || metadata["day"] === null) ?
    `
            <span class="scrollspy metadataprocessed" id="tail_undated"></span>
        </div>
    </span>
    <span class="attachMetadataPhotos" id="amp_undated" style="visibility: hidden">EOL</span>
    `
    :
    `
        ${(lastDate === (metadata.year + '-' + metadata.month + '-' + metadata.day)) ? `
                <span class="scrollspy metadataprocessed" id="tail_${metadata.year}-${metadata.month}-${metadata.day}"></span>';
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
`

const TimelineGalleryBottomLeftOverlay = ({metadata, editIcon}) => `
    <a href="#" id="timelineModalEdit${metadata.id}" data-bs-target="#propTimelinModal">
        <span class="${editIcon}" style="font-size: 1rem;color: lightgray;"></span>
    </a>
`

const TimelineGalleryTopLeftOverlay = ({metadata}) => `
    <a href="#" id="select${metadata.id}">
        <span id="tlicon${metadata.id}" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>
    </a>
`

const TimelineGalleryTopRightOverlay = ({metadata}) => `
    ${metadata.type.indexOf("video") >= 0 ?
    `
    <span class="overlayIconBackground">${(metadata.hasOwnProperty("duration") && metadata.duration !== null && metadata.duration !== "") ? metadata.duration : "0:00"}&nbsp;<span id="video${metadata.id}" class="bi-camera-video overlayIcon"></span></span>
    `
    :
    `
        ${(metadata.originalImageWidth !== null && metadata.originalImageHeight !== null && metadata.originalImageWidth > metadata.originalImageHeight * 2) ?
        `
        <span id="panorama${metadata.id}" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>
        `
        : ''}
    `
    }
`

const TimelineGalleryBottomRightOverlay = ({metadata}) => `
    <a href="#" id="favorite${metadata.id}" class="text-decoration-none">
        <span class="overlayIconBackground">
            <span id="briconcount${metadata.id}"></span> <span id="bricon${metadata.id}" class="overlayIcon"></span>
        </span>
    </a>
`

const TimelineGalleryCenterOverlay = ({metadata,mediaContent}) => `
    <a class="mediaLink" 
        id="mediaLink${metadata.id}"
        data-download-url="${encodeURI(metadata.thumbnailUrlOriginal)}"
        data-metadataid="${metadata.id}"
        ${(metadata.type.indexOf("video") >= 0) ? `data-video="${Util.encodeHtml(mediaContent.video)}"` : `data-src="${encodeURI(metadata.thumbnailUrlOriginal)}"`}
        ${(metadata.description != null) ? `data-sub-html="${metadata.description}"` : ''}

        ${(metadata.originalImageWidth !== null && metadata.originalImageHeight !== null &&
        metadata.thumbnailSmallWidth !== null && metadata.thumbnailSmallHeight !== null) ?
        `
        data-lg-size="${metadata.thumbnailSmallWidth}-${metadata.thumbnailSmallHeight}-${metadata.thumbnailSmallWidth},${metadata.originalImageWidth}-${metadata.originalImageHeight}"
        data-responsive="${encodeURI(metadata.thumbnailUrlSmall)} ${metadata.thumbnailSmallWidth}"
        data-thumb="${encodeURI(metadata.thumbnailUrlSmall)}"
        data-width="${metadata.originalImageWidth}"
        ` : ''}
        >
        <span class="${(metadata.type.indexOf("video") >= 0) ? `bi-play-btn` : `bi-play-circle`}" style="font-size: 4rem;color: lightgray;"></span>
    </a>
`