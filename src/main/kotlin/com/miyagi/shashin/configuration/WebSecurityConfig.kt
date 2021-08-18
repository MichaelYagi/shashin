package com.miyagi.shashin.configuration

import com.miyagi.shashin.component.AuthFailureHandler
import com.miyagi.shashin.component.AuthSuccessHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
    private val dataSource: DataSource? = null

    @Autowired
    private val authFailureHandler: AuthFailureHandler? = null

    @Autowired
    private val authSuccessHandler: AuthSuccessHandler? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

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
                "SELECT username, password, TRUE from user where username = ?")
            .authoritiesByUsernameQuery(
                "SELECT username, authority from user where username = ?");
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
            .headers().frameOptions().sameOrigin()
                .and()
            .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/api/**", "/share", "/users/register", "/users/login", "/users/logout").permitAll()
                .antMatchers("settings/**", "timeline/**", "users/delete", "albums/add").hasRole(adminRole)
                .antMatchers("comments/**", "albums", "settings/users/edit").hasAnyRole(userRole, adminRole)
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/users/login")
                .successHandler(authSuccessHandler)
                .failureHandler(authFailureHandler)
                .permitAll()
                .and()
            .csrf().disable()
    }
}