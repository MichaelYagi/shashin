package com.miyagi.shashin.repository

import com.miyagi.shashin.model.AlbumComment
import org.springframework.data.repository.CrudRepository

interface AlbumCommentRepository : CrudRepository<AlbumComment?, Int?> {
}