const AlbumCommentsModalHead = ({metadata}) => `
    <div class="modal fade" id="propalbumphotocomment${metadata.id}" tabindex="-1" role="dialog" aria-labelledby="label${metadata.id}" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-scrollable" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="commentModalLabel">
                        <div id="propalbumphotocomment${metadata.id}">
                            <img loading="lazy" src="${encodeURI(metadata.thumbnailUrlCentered)}" width="100" height="100" onError="Util.errorImg(this,\\'${metadata.title}\\',100)">
                        </div>Comments for ${metadata.fileName}
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="currentCommentId${metadata.id}" name="currentCommentId${metadata.id}">
                    <ul class="list-group" id="commentList${metadata.id}">
`

const AlbumComment = ({commentId, commentText, userId, commentUserId, username}) => `
    <li class="list-group-item${(commentUserId === userId) ? ` list-group-item-secondary` : ''}" id="comment${commentId}">
        <span id="commentcontainer${commentId}">
            <p id="commentcontent${commentId}">${commentText}</p>
            ${(commentUserId === userId) ? `<small>${username}<span style="float: right"><a href="#" id="deletecomment${commentId}"><span class="bi-trash"></span></a>&nbsp;&nbsp;<a href="#" id="editcomment${commentId}"><span class="bi-pencil"></span></a></span></small>` : ''}
        </span>
        <span id="textareacontainer${commentId}"></span>
    </li>
`

const AlbumCommentsModalFooter = ({metadata}) => `
                    </ul>
                </div>
                <div class="modal-footer">
                    <textarea class="form-control" id="commentText${metadata.id}" rows="2"></textarea>
                    <button type="button" class="btn btn-primary" id="saveCommentMetadata${metadata.id}">Save</button>
                    <button type="button" class="btn btn-primary" id="updateCommentMetadata${metadata.id}">Update</button>
                    <button type="button" class="btn btn-secondary" id="dismissModalCommentMetadata${metadata.id}" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-secondary" id="cancelEditCommentMetadata${metadata.id}" data-bs-dismiss="modal">Cancel</button>
                </div>
            </div>
        </div>
    </div>
`

const PersonModalHead = ({module,metadata,recognitionLabels,taggedPeopleList}) => `
    <div class="modal fade" id="prop${module}${metadata.id}" tabindex="-1" role="dialog" aria-labelledby="label${metadata.id}" aria-hidden="true">
        <div class="modal-dialog modal-dialog-scrollable modal-lg" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="exampleModalLabel">Edit "${metadata.title}"
                        <div id="prop${module}Thumbnail${metadata.id}">
                            <img loading="lazy" src="${encodeURI(metadata.thumbnailUrlCentered)}" width="100" height="100" onError="Util.errorImg(this,\'${metadata.title}\',100)">
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
                                        <button class="btn btn-outline-secondary dropdown-toggle" id="tagpeopledropdown${metadata.id}" type="button" aria-haspopup="true" aria-expanded="false">People</button>
                                        <div class="dropdown-menu ${module}Dropdown" id="recognitionLabelsList${metadata.id}">
                                    ` : ''}
`

const PersonModalDropDown = ({metadata,recognitionLabel,checkedString}) => `
                                            <button class="dropdown-item" type="button">
                                                <input type="checkbox" class="recognitionLabel" value="${recognitionLabel.name}" name="recognitionLabel${metadata.id}[]" id="${metadata.id.length > 0 ? `${metadata.id}-` : ''}${recognitionLabel.id}"${(checkedString.length > 0) ? `${checkedString}` : ''}>
                                                <label for="${metadata.id.length > 0 ? `${metadata.id}-` : ''}${recognitionLabel.id}" id="label${metadata.id.length > 0 ? `-${metadata.id}-` : ''}${recognitionLabel.id}">${recognitionLabel.name}</label>
                                            </button>
`

const PersonModalFooter = ({module,metadata,recognitionLabels}) => `
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
                                <label class="form-check-label" for="isobject${metadata.id}">This is not a person</label>
                            </div>
                        </div>
                    </form>
                    <div class="col-sm">
                        <span id="msg${metadata.id}"></span>
                    </div>
                </div>
                <div class="modal-footer">
                    <div id="${module}ModalStatus${metadata.id}" class="spinner-grow me-auto" style="visibility: hidden;font-size: 2rem;" role="status" aria-hidden="true" data-bs-toggle="tooltip" data-bs-placement="right" title=""></div>
                    <button type="button" class="btn btn-primary" id="saveMetadata${metadata.id}">Save</button>
                    <button id="${module}ModalCancel${metadata.id}" type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                </div>
            </div>
        </div>
    </div>
`