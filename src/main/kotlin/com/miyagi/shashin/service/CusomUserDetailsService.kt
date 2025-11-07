package com.miyagi.shashin.service

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service


@Service
class CustomUserDetailsService(private val userRepository: UserRepository? = null) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository!!.findByUsername(username) ?: throw UsernameNotFoundException(username)
        return CustomUserPrincipal(user)
    }
}


class CustomUserPrincipal(private val user: User) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()
        authorities.add(SimpleGrantedAuthority(user.getAuthority()))
        return authorities
    }

    override fun getPassword(): String {
        return user.getPassword()!!
    }

    override fun getUsername(): String {
        return user.getUsername()!!
    }

    override fun isAccountNonExpired(): Boolean {
        return user.getIsAuthorized()!!
    }

    override fun isAccountNonLocked(): Boolean {
        return user.getIsAuthorized()!!
    }

    override fun isCredentialsNonExpired(): Boolean {
        return user.getIsAuthorized()!!
    }

    override fun isEnabled(): Boolean {
        return user.getIsAuthorized()!!
    }
}