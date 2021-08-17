package com.miyagi.shashin.repository

import com.miyagi.shashin.model.User
import org.springframework.data.domain.Sort
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User?, Int?> {
    fun findAll(sort: Sort?): MutableIterable<User?>?
    fun findByUsername(username: String?): User?
}