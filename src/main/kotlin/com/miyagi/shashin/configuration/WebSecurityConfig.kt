package com.miyagi.shashin.configuration

import com.miyagi.shashin.component.*
import com.miyagi.shashin.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall
import org.springframework.security.web.header.HeaderWriterFilter
import org.springframework.security.web.session.HttpSessionEventPublisher
import javax.sql.DataSource

@EnableGlobalMethodSecurity(securedEnabled = true)
class MultiSecurityConfig: WebSecurityConfigurerAdapter() {
    // Used to validate URL paths for login redirect
    companion object {
        val validWebSubPaths = arrayOf(
            "timeline",
            "albums",
            "album",
            "recent",
            "modified",
            "folders",
            "folder",
            "people",
            "person",
            "matches",
            "favorites",
            "map",
            "notifications",
            "settings",
            "dashboard",
            "trash",
            "users",
            "dashboard",
            "search"
        )

        var publicList = arrayOf(
            "/",
            "/docs/**",
            "/articles/**",
            "/health",
            "/features",
            "/share/**",
            "/css/**",
            "/js/**",
            "/fonts/**",
            "/images/**",
            "/users/register",
            "/users/login",
            "/users/logout",
            "/websocket-endpoint",
            "/topic/messages",
            "/topic/matchmessages",
            "/settings/matchmessage",
            "/settings/scanmessage",
            "/dashboard/statmessages",
            "/dashboard/statmessage",
            "/api/v1/thumbnails/**"
        )

        var adminList = arrayOf(
            "settings/**",
            "settings",
            "settings/users",
            "settings/scan",
            "timeline",
            "timeline/**",
            "users/delete",
            "albums/add",
            "api/v1/folders",
            "api/v1/folders/**",
            "api/v1/folder",
            "api/v1/folder/**",
            "api/v1/recent",
            "api/v1/recent/**",
            "api/v1/modified",
            "api/v1/modified/**",
            "api/v1/share/album/save",
            "api/v1/exif/metadata/**",
            "api/v1/folders",
            "api/v1/all/album/delete"
        )

        val allRoleList = arrayOf(
            "comments/**",
            "albums",
            "favorites",
            "map/**",
            "search/**",
            "api/v1/album/**/page/**",
            "api/v1/album/**",
            "api/v1/users/apikey/update",
            "api/v1/mapdata",
            "api/v1/profile/**",
            "api/v1/metadata/**",
            "api/v1/users/self",
            "api/v1/comment/**",
            "api/v1/favorites/**",
            "api/v1/keywords"
        )
    }

    @Configuration
    @Order(1)
    class ApiSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        private val userRepository: UserRepository? = null

        @Autowired
        private val apiAccessDeniedHandler: ApiAccessDeniedHandler? = null

        @Bean
        fun passwordApiEncoder(): PasswordEncoder? {
            return BCryptPasswordEncoder()
        }

        @Throws(java.lang.Exception::class)
        override fun configure(auth: AuthenticationManagerBuilder) {
//        auth
//            .jdbcAuthentication()
//            .dataSource(dataSource)
//            .passwordEncoder(passwordApiEncoder())
//            .usersByUsernameQuery(
//                "SELECT username, password, TRUE from user where apikey = ?")
//            .authoritiesByUsernameQuery(
//                "SELECT username, authority from user where apikey = ?")
        }

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(AuthenticationFilter(userRepository), UsernamePasswordAuthenticationFilter::class.java)
                .antMatcher("/api/**")
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(apiAccessDeniedHandler)
        }

        override fun configure(web: WebSecurity) {
            web.ignoring().antMatchers("/api/v1/thumbnails/**","/api/v1/image/**","/api/v1/video/**", "/api/v1/profile/**")
        }
    }

    @Configuration
    @Order(2)
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {

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

        @Value("\${app.rememberme.key}")
        private var rememberMeKey: String? = null

        @Value("\${app.rememberme.expiration.seconds}")
        private var expirationSeconds: Int? = null

        @Value("\${app.api.version}")
        private lateinit var apiVersion: String

        @Bean
        fun passwordEncoder(): PasswordEncoder? {
            return BCryptPasswordEncoder()
        }

        @Bean("authenticationManager")
        @Throws(java.lang.Exception::class)
        override fun authenticationManagerBean(): AuthenticationManager? {
            return super.authenticationManagerBean()
        }

        @Bean
        fun sessionRegistry(): SessionRegistry? {
            return SessionRegistryImpl()
        }

        @Bean
        fun httpSessionEventPublisher(): ServletListenerRegistrationBean<HttpSessionEventPublisher>? {
            return ServletListenerRegistrationBean(HttpSessionEventPublisher())
        }

        @Throws(java.lang.Exception::class)
        override fun configure(auth: AuthenticationManagerBuilder) {
            auth
                .jdbcAuthentication()
                .dataSource(dataSource)
                .passwordEncoder(passwordEncoder())
                .usersByUsernameQuery(
                    "SELECT username, password, TRUE from user where username = ?"
                )
                .authoritiesByUsernameQuery(
                    "SELECT username, authority from user where username = ?"
                )
        }

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .addFilterBefore(CSPNonceFilter(), HeaderWriterFilter::class.java)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and()
                .headers()
                .xssProtection()
                .and()
                .contentSecurityPolicy("worker-src 'self' 'nonce-{nonce}' blob:")
                .and()
                .frameOptions()
                .sameOrigin()
                .and()
                .authorizeRequests()
                .antMatchers(*publicList).permitAll()
                .antMatchers(*adminList).hasRole(adminRole.toString().replace("ROLE_", ""))
                .antMatchers(*allRoleList)
                .hasAnyRole(userRole.toString().replace("ROLE_", ""), adminRole.toString().replace("ROLE_", ""))
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/users/login")
                .successHandler(authSuccessHandler)
                .failureHandler(authFailureHandler)
                .permitAll()
                .and()
                .rememberMe().key(rememberMeKey).tokenValiditySeconds(expirationSeconds!!)
                .and()
                .csrf().disable()
                .httpBasic()

            http.exceptionHandling()
                .authenticationEntryPoint(AjaxAwareAuthenticationEntryPoint("/users/login", apiVersion))

            http.sessionManagement()
                .maximumSessions(100)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/users/login")
                .sessionRegistry(sessionRegistry())
        }

        @Bean
        fun allowUrlEncodedSlashHttpFirewall(): HttpFirewall {
            val firewall = StrictHttpFirewall()
            firewall.setAllowUrlEncodedPercent(true)
            firewall.setAllowSemicolon(true)
            firewall.setAllowUrlEncodedSlash(true)
            return firewall
        }

        @Throws(Exception::class)
        override fun configure(web: WebSecurity) {
            super.configure(web)
            web.httpFirewall(allowUrlEncodedSlashHttpFirewall())
        }
    }
}