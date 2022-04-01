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