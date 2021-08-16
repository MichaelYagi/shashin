package com.miyagi.shashin.configuration

import com.miyagi.shashin.model.User
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
//import org.springframework.security.config.annotation.web.builders.HttpSecurity
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
//import org.springframework.security.core.userdetails.UserDetails
//import org.springframework.security.provisioning.InMemoryUserDetailsManager
import java.util.*

//@Configuration
//@EnableWebSecurity
class WebSecurityConfig /*: WebSecurityConfigurerAdapter()*/ {

//    @Autowired
//    var userRepository: UserRepository? = null
//
//    @Throws(Exception::class)
//    override fun configure(http: HttpSecurity) {
//        http
//            .authorizeRequests()
//                .antMatchers("/css/**", "/js/**", "/", "/share", "/users/register", "/api/**").permitAll()
//                .antMatchers("/admin/**").hasAnyRole("ADMIN")
//                .antMatchers("/user/**").hasRole("USER")
//                .anyRequest().authenticated()
//                .and()
//            .formLogin()
//                .loginPage("/users/login")
//                .permitAll()
//                .and()
//            .logout()
//                .invalidateHttpSession(true)
//                .clearAuthentication(true)
//                .logoutSuccessUrl("/users/login?logout")
//                .permitAll()
//    }
//
//    fun loadUserById(id: Int): UserDetails? {
//        val user = userRepository?.findById(id)
//
//        if (user != null) {
//            return UserPrincipal(user.get())
//        }
//
//        return null
//    }
}