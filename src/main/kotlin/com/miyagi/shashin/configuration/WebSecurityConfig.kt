package com.miyagi.shashin.configuration

import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import javax.sql.DataSource


@Configuration
@EnableWebSecurity
class WebSecurityConfig: WebSecurityConfigurerAdapter() {

    @Autowired
    var userRepository: UserRepository? = null

    @Autowired
    private val dataSource: DataSource? = null

    @Bean
    fun passwordEncoder(): PasswordEncoder? {
        return BCryptPasswordEncoder()
    }

    @Throws(java.lang.Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth
            .jdbcAuthentication()
            .dataSource(dataSource)
            .passwordEncoder(passwordEncoder())
            .usersByUsernameQuery(
                "SELECT username, password from users where username = ?")
            .authoritiesByUsernameQuery(
                "SELECT u.username, a.authority " +
                        "FROM authorities a, users u " +
                        "WHERE u.username = ? " +
                        "AND u.id = a.user_id"
            );
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/", "/share", "/users/register", "/api/**").permitAll()
                .antMatchers("/settings/**").hasAnyRole("ADMIN")
                .antMatchers("/comments/**", "album/**").hasRole("USER")
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/users/login")
                .permitAll()
                .and()
            .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessUrl("/users/login?logout")
                .permitAll()
    }
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