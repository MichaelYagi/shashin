package com.miyagi.shashin.service

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service


@Service
class CustomUserDetailsService : UserDetailsService {
    @Autowired
    private val userRepository: UserRepository? = null
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository!!.findByUsername(username) ?: throw UsernameNotFoundException(username)
        return CustomUserPrincipal(user)
    }
}


class CustomUserPrincipal(user: User) : UserDetails {
    private val user: User

    init {
        this.user = user
    } //...

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        TODO("Not yet implemented")
    }

    override fun getPassword(): String {
        TODO("Not yet implemented")
    }

    override fun getUsername(): String {
        TODO("Not yet implemented")
    }

    override fun isAccountNonExpired(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isAccountNonLocked(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCredentialsNonExpired(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEnabled(): Boolean {
        TODO("Not yet implemented")
    }
}