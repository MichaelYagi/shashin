class ModalTemplates {
    static AlbumCommentsModalHead({metadata}) { return `
        <div class="modal fade" id="propalbumphotocomment${metadata.id}" tabindex="-1" role="dialog" aria-labelledby="label${metadata.id}">
            <div class="modal-dialog modal-lg modal-dialog-scrollable" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="commentModalLabel${metadata.id}">
                            <div id="thumbImage${metadata.id}">
                                <img loading="lazy" draggable="false" src="${"/api/v1/thumbnails/centered/"+metadata.id}" width="100" height="100">
                            </div>${shashin.getTranslatedValue("main.modal.control.comments")} ${metadata.fileName}
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body" id="commentListContainer${metadata.id}" style="display: none;">
                        <input type="hidden" id="currentCommentId${metadata.id}" name="currentCommentId${metadata.id}">
                        <ul class="list-group" id="commentList${metadata.id}">
    `};

    static AlbumComment({commentId, commentText, userId, commentUserId, username, userProfile, createdAt, canEdit}) { return `
        <li class="list-group-item${(commentUserId === userId) ? ` list-group-item-secondary` : ''}" id="comment${commentId}">
            <span id="commentcontainer${commentId}">
                <p id="commentcontent${commentId}">${commentText}</p>
                <small>${userProfile !== undefined ? (userProfile!=="null" && userProfile!==null && userProfile!==""?`<img src="${userProfile}?${uuidv4()}" class="me-1" style="display:inline-block;width:24px;height:24px;" />`:`<span class="bi-person-circle me-1" style="font-size:1.0rem;"></span>`):``} <strong>${username}</strong>${createdAt !== undefined? ` on ${createdAt}`:``}${(canEdit === true && commentUserId !== userId)?`<span style="float: right"><a href="#" id="deletecomment${commentId}"><span class="bi-trash"></span></a></span>`:``}${commentUserId === userId?`<span style="float: right"><a href="#" id="deletecomment${commentId}"><span class="bi-trash"></span></a>&nbsp;&nbsp;<a href="#" id="editcomment${commentId}"><span class="bi-pencil"></span></a></span>`:``}</small>
            </span>
            <span id="textareacontainer${commentId}"></span>
        </li>
    `};

    static AlbumCommentsModalFooter({metadata}) { return `
                        </ul>
                    </div>
                    <div class="modal-footer">
                        <textarea class="form-control" id="commentText${metadata.id}" rows="2" placeholder="Comment"></textarea>
                        <button type="button" class="btn btn-primary" id="saveCommentMetadata${metadata.id}">${shashin.getTranslatedValue("main.modal.save")}</button>
                        <button type="button" class="btn btn-primary" id="updateCommentMetadata${metadata.id}">${shashin.getTranslatedValue("main.modal.tab.update")}</button>
                        <button type="button" class="btn btn-secondary" id="dismissModalCommentMetadata${metadata.id}" data-bs-dismiss="modal">${shashin.getTranslatedValue("main.modal.close")}</button>
                        <button type="button" class="btn btn-secondary" id="cancelEditCommentMetadata${metadata.id}">${shashin.getTranslatedValue("main.modal.cancel")}</button>
                    </div>
                </div>
            </div>
        </div>
    `};

    static AlbumModalDropdownHeader({metadata}) { return `
        <div class="input-group-append dropdown" id="albumListInput">
            <button class="btn btn-secondary dropdown-toggle" id="albumdropdown${metadata.id}" type="button" aria-haspopup="true" aria-expanded="false">${shashin.getTranslatedValue("main.modal.control.albums")}</button>
            <div class="dropdown-menu" id="albumsList">
    `};

    static AlbumModalDropDown({metadata, album, checkedString}) { return `
                <button class="dropdown-item" type="button">
                    <input type="checkbox" class="album" value="${Util.escapeHtml(album.name)}" name="album${metadata.id}[]" id="album-${metadata.id}-${album.id}"${checkedString}>
                    <label for="album-${metadata.id}-${album.id}">${Util.escapeHtml(album.name)}</label>
                </button>
    `};

    static AlbumModalDropdownFooter() { return `
            </div>
        </div>
    `};

    static PersonModalHead({module, metadata, recognitionLabels, taggedPeopleList}) { return `
        <div class="modal fade" id="prop${module}${metadata.id}" tabindex="-1" role="dialog" aria-labelledby="label${metadata.id}">
            <div class="modal-dialog modal-dialog-scrollable modal-lg" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="propEditLabel">Edit "${metadata.title}"
                            <div id="prop${module}Thumbnail${metadata.id}">
                                <img loading="lazy" draggable="false" src="${"/api/v1/thumbnails/centered/"+metadata.id}" width="100" height="100">
                            </div>
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <form th:id="saveData${metadata.id}">
                            <input type="hidden" name="id" value="${metadata.id}">
                            <div class="form-group row">
                                <div class="col-sm">
                                    <label for="tagpeople${metadata.id}" class="col-form-label">Tag People (comma separated)</label>
                                    <div class="input-group" id="batchLabelIds${metadata.id}">
                                        <input type="text" class="form-control" aria-label="Tag People" id="tagpeople${metadata.id}" name="tagpeople" value="${taggedPeopleList}">
                                        ${(recognitionLabels.length > 0) ? `
                                        <div class="input-group-append">
                                            <button class="btn btn-secondary dropdown-toggle" id="tagpeopledropdown${metadata.id}" type="button" aria-haspopup="true" aria-expanded="false">${shashin.getTranslatedValue("main.modal.control.people")}</button>
                                            <div class="dropdown-menu ${module}Dropdown" id="recognitionLabelsList${metadata.id}">
                                        ` : ''}
    `};

    static PersonModalDropdownHead({metadata}) { return `
        <div class="input-group-append dropdown" id="recognitionLabelInput">
            <button class="btn btn-secondary dropdown-toggle" id="tagpeopledropdown${metadata.id}" type="button" aria-haspopup="true" aria-expanded="false">${shashin.getTranslatedValue("main.modal.control.people")}</button>
            <div class="dropdown-menu" id="recognitionLabelsList">
    `};

    static PersonModalDropDown({metadata, recognitionLabel, checkedString}) { return `
                                                ${(recognitionLabel.name === null || recognitionLabel.name === "null") ? ``: `
                                                <button class="dropdown-item" type="button">
                                                    <input type="checkbox" class="recognitionLabel" value="${Util.escapeHtml(recognitionLabel.name)}" name="recognitionLabel${metadata.id}[]" id="label-${metadata.id.length > 0 ? `${metadata.id}-` : ''}${recognitionLabel.id}"${(checkedString.length > 0) ? `${checkedString}` : ''}>
                                                    <label for="label-${metadata.id.length > 0 ? `${metadata.id}-` : ''}${recognitionLabel.id}">${Util.escapeHtml(recognitionLabel.name)}</label>
                                                </button>`}
    `};

    static PersonModalDropdownFooter() { return `
            </div>
        </div>
    `};

    static PersonModalFooter({module, metadata, recognitionLabels}) { return `
                                        ${(recognitionLabels.length > 0) ? `
                                            </div>
                                        </div>
                                        ` : ''}
                                    </div>
                                </div>
                            </div>
                            <div class="form-group row">
                                <div class="col-sm">
                                    <input type="checkbox" name="isobject${metadata.id}" id="isobject${metadata.id}">
                                    <label class="form-check-label" for="isobject${metadata.id}">${shashin.getTranslatedValue("main.modal.control.notpeople")}</label>
                                </div>
                            </div>
                        </form>
                        <div class="col-sm">
                            <span id="msg${metadata.id}"></span>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <div id="${module}ModalStatus${metadata.id}" class="spinner-grow me-auto" style="visibility: hidden;font-size: 2rem;" role="status" data-bs-toggle="tooltip" data-bs-placement="right" title=""></div>
                        <button type="button" class="btn btn-primary" id="saveMetadata${metadata.id}">${shashin.getTranslatedValue("main.modal.save")}</button>
                        <button id="${module}ModalCancel${metadata.id}" type="button" class="btn btn-secondary" data-bs-dismiss="modal">${shashin.getTranslatedValue("main.modal.cancel")}</button>
                    </div>
                </div>
            </div>
        </div>
    `};
}