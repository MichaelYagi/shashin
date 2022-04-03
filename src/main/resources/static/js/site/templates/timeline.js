const TimelineToc = ({index,mediaTypeFilter,metadataDates,year,month,day}) => `
    ${(index === 0 || (index > 0 && metadataDates[index-1].year !== year)) ? `
        <strong>${year}</strong>
        <div class='list-group'>
    ` : ''}

    ${(index > 0 && metadataDates[index-1].year === year && metadataDates[index-1].month === month) ?
    `
            <a style="display:none" id="offcanvas_${year}-${month}-${day}" class="list-group-item list-group-item-action${(index === 0) ? ` active` : ''}" href="#${year}-${month}-${day}"></a>
    `
    :
    `
            <a id="offcanvas_${year}-${month}-${day}" class="list-group-item list-group-item-action${(index === 0) ? ` active` : ''}" href="#${year}-${month}-${day}">${new Date(year, month-1, day).format("mmm yyyy")}</a>
    `}
    
            <script type="text/javascript"${(shashin.nonce.length > 0) ? ` nonce="${shashin.nonce}"` : ''}>
                $("#offcanvas_${year}-${month}-${day}").on("click", function (e) {
                    e.preventDefault();
                    timelineSettings.jumpFromTimelineToc(e,"${year}-${month}-${day}","${mediaTypeFilter}")
                });
            </script>

    ${(index > 0 && index < metadataDates.length-1 && metadataDates[index+1].year !== year) ? `
        </div>
        <br>
    ` : ''}
`

const MapLinks = ({metadata}) => `
    <a href="/map?lat=${metadata.lat}&lng=${metadata.lng}" target="_blank" class="bi-pin-fill" style="text-decoration: none;">&nbsp;${metadata.placeName}</a>
    <br>
    <a href="https://www.google.com/maps/search/?api=1&query=${metadata.lat}%2C${metadata.lng}" target="_blank" class="bi-google" style="text-decoration: none;">&nbsp;Google Maps link</a>
`

const HeaderThumbnail = ({metadata}) => `
    <img loading="lazy" src="${encodeURI(metadata.thumbnailUrlCentered)}" height="100" width="100" onError="Util.errorImg(this,\\'${metadata.title}\\',100)">
`

const BatchHeaderThumbnail = ({thumbnailImage,title}) => `
    <img loading="lazy" src="${thumbnailImage}" height="75" width="75" data-bs-toggle="tooltip" data-bs-placement="top" title="${title}" onError="Util.errorImg(this,\'${title}\',75)">
`

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