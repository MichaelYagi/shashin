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