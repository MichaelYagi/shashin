const PhotoGalleryItem = ({activePage, appendClass, dateHeadingObj, metadata, currentMediaLinkIndex, renderTopRight, renderTopLeft, renderBottomLeft, renderCenter, renderBottomRight}) => `
    ${(typeof dateHeadingObj === "undefined" || dateHeadingObj === null) ? '' : `<section class="dateSection" id="${dateHeadingObj.heading}"><p><strong>${dateHeadingObj.display}</strong></p></section>`}
    <div id="photoThumbnailContainer${metadata.id}" class="photo-thumbnail-container photo-thumbnail" style="width:${metadata.thumbnailSmallWidth}px;height:${metadata.thumbnailSmallHeight}px;padding-left:0;padding-right:0;">
        <a class="lightGalleryIndexAnchor" name="lightGalleryIndex${currentMediaLinkIndex}"></a>
        <img loading="lazy" src="${encodeURI(metadata.thumbnailUrlSmall)}" class="photo-thumbnail-image" id="image${metadata.id}" width="${metadata.thumbnailSmallWidth}" height="${metadata.thumbnailSmallHeight}" style="background-color:lightgray;" onError="Util.errorImg(this,\\'${metadata.title}\\',Util.thumbnailHeight())">
        <input type="hidden" name="filenamee${metadata.id}" id="filename${metadata.id}" value="${metadata.fileName}">
        <input type="hidden" name="thumbnailCentered${metadata.id}" id="thumbnailCentered${metadata.id}" value="${encodeURI(metadata.thumbnailUrlCentered)}">

        ${(typeof renderTopRight === "undefined" || renderTopRight === null) ? '' : getTopRightOverlay(renderTopRight)}
        
        ${(typeof renderTopLeft === "undefined" || renderTopLeft === null) ? '' : getTopLeftOverlay(renderTopLeft)}
        
        ${(typeof renderBottomLeft === "undefined" || renderBottomLeft === null) ? '' : getBottomLeftOverlay(renderBottomLeft)}
        
        ${(typeof renderBottomRight === "undefined" || renderBottomRight === null) ? '' : getBottomRightOverlay(renderBottomRight)}
        
        ${(typeof renderCenter === "undefined" || renderCenter === null) ? '' : getCenteredOverlay(renderCenter)}
        
    </div>
    ${(activePage === "album") ? `<span id="albummodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
    ${(activePage === "person") ? `<span id="personmodal${metadata.id}" style="width:0;height:0;padding:0"></span>` : ''}
    <span class="${appendClass}" style="width:0;height:0;padding:0"></span>
    
    <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="'+shashin.nonce+'"' : '')}>
        shashin.setPhotoOverlays({id:"${metadata.id}",lat:"${(metadata.lat === null) ? '' : `${metadata.lat}`}", lng:"${(metadata.lng === null) ? '' : `${metadata.lng}`}", year:${metadata.year}, month:${metadata.month}, day:${metadata.day}, fileName:"${metadata.fileName}"}, "${activePage}");
        Util.activateMetadataListeners("${metadata.id}");
        $("#mediaLink${metadata.id}").attr("tag", "${metadata.id}");
        
        ${(typeof renderBottomLeft === "undefined" || renderBottomLeft === null || renderBottomLeft.editControls === false) ?
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

const getTopRightOverlay = ({type, id, content, width, height, isTagged}) => `
    <div class="thumbnail-tr" id="tntr${id}">

    ${(type.includes("video") === false) ? '' :
        `
        <span class="overlayIconBackground">${content}&nbsp;
            <span id="video${id}" class="bi-camera-video overlayIcon"></span>
        </span>
        <br>
        `
    } 
    
    ${(type.includes("video") === false && width !== null && height !== null && width > height*2) ?
        `
        <span id="panorama${id}" class="bi-aspect-ratio overlayIcon overlayIconBackground"></span>
        <br>
        ` : ''
    }
    
    ${(isTagged === true) ?
        `
        <span class="bi-bookmark-fill overlayIconBackground" style="font-size: 1rem;color: lightsalmon;"></span>
        ` : ''
    }

    </div>
`

const getTopLeftOverlay = ({id}) => `
    <div class="thumbnail-tl" id="tntl${id}">
        <a href="#" id="select${id}">
            <span id="tlicon${id}" class="bi-circle" style="font-size: 1rem;color: lightgray;"></span>
        </a>
    </div>
`;

const getBottomRightOverlay = ({id, favoriteCount, favoriteIcon, albumPhotoCommentsMap, notificationMap}) => `
    <div class="thumbnail-br" id="tnbr${id}">
        <a href="#" id="favorite${id}" class="text-decoration-none">
            <span class="overlayIconBackground">
                <span id="briconcount${id}">${favoriteCount}</span>&nbsp;<span class="${favoriteIcon} overlayIcon" id="brfavoriteicon${id}"></span>
            </span>
        </a>
        <br>
        <a href="#" data-bs-toggle="modal" data-bs-target="#propalbumphotocomment${id}" class="overlayCommentIconBackground overlayCommentText">
            <span id="brcommentcount${id}">${albumPhotoCommentsMap.hasOwnProperty(id) ? albumPhotoCommentsMap[id].length : `0`}</span> 
            <span id="bricon${id}" class="bi-chat-square position-relative overlayCommentIcon">
                ${(notificationMap !== null && notificationMap[id] === true) ? `
                <span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle">
                    <span class="visually-hidden">New alerts</span>
                </span>
                ` : ''}
            </span>
        </a>
    </div>
`

const getBottomLeftOverlay = ({id, targetPrefix, onclickIdPrefix, onclickFunctionCall, editControls, editIcon}) => `
    <div class="thumbnail-bl" id="tnbl${id}">
        ${(editControls === true) ?
        `
            <a href="#" id="timelineModalEdit${id}" data-bs-target="#propTimelinModal">
                <span class="${editIcon}" style="font-size: 1rem;color: lightgray;"></span>
            </a>
        ` 
        :
        `
            <a href="#" id="infoModalEdit${id}">
                <span class="bi-info-circle" style="font-size: 1rem;color: lightgray;"></span>
            </a>
        
            ${(onclickFunctionCall === null && targetPrefix === null) ? '' :
                `
                <br>
        
                    ${(onclickFunctionCall !== null) ?
                        `
                        <a href="#" id="${onclickIdPrefix}${id}">
                            <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>
                        </a>
            
                        <script type="text/javascript"${(shashin.nonce.length > 0 ? ' nonce="'+shashin.nonce+'"' : '')}>
                            $("#${onclickIdPrefix}${id}").on("click", function (e) {
                                e.preventDefault();
                                ${onclickFunctionCall}(e,"${id}");
                            });
                        </script>
                        ` : ''
                    }
                    
                    ${(onclickFunctionCall === null && targetPrefix !== null) ?
                        `
                        <a href="#" data-bs-toggle="modal" data-bs-target="#${targetPrefix}${id}">
                            <span class="bi-pencil" style="font-size: 1rem;color: lightgray;"></span>
                        </a>
                        ` : ''
                    }
                `
            }
        `}
    </div>
`;

const getCenteredOverlay = ({metadata,onclickFunctionCall,index}) => `
    <div class="thumbnail-centered" id="tncentered${metadata.id}">

        ${(metadata.type.includes("video") === true) ? 
            `
            <a class="mediaLink" id="mediaLink${metadata.id}" data-download-url="${encodeURI(metadata.videoUrl)}/download" 
                ${metadata.description !== null ?? ` data-sub-html="${metadata.description}" `}
                data-video=\'{"source": [{"src":"${metadata.videoUrl}", "type":"video/mp4"}], "attributes": {"preload": false, "controls": true}}\'>
                <span class="bi-play-btn" style="font-size: 4rem;color: lightgray;"></span>
            </a>
            `
        :
            `
            <a class="mediaLink" id="mediaLink${metadata.id}" data-src="${metadata.thumbnailUrlOriginal}" href="${metadata.thumbnailUrlOriginal}"
                data-download-url="${encodeURI(metadata.thumbnailUrlOriginal)}"
                ${metadata.description !== null ?? ` data-sub-html="${metadata.description}" `}>
                <span class="bi-play-circle" style="font-size: 4rem;color: lightgray;"></span>
            </a>
            `
        }

        <script type="text/javascript"${(shashin.nonce.length > 0 ? ` nonce="${shashin.nonce}"` : '')}>
            $("#mediaLink${metadata.id}").on("click", function (e) {
                e.preventDefault();
                ${onclickFunctionCall}(e,${index});
            });
        </script>

    </div>
`;