package com.miyagi.shashin.repository

import com.miyagi.shashin.model.Authority
import org.springframework.data.repository.CrudRepository

interface AuthorityRepository : CrudRepository<Authority?, Int?> {
}