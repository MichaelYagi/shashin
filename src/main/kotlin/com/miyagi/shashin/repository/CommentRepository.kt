package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Album
import com.miyagi.shashin.model.Comment
import org.springframework.data.repository.CrudRepository

interface CommentRepository : CrudRepository<Comment?, Int?> {
}