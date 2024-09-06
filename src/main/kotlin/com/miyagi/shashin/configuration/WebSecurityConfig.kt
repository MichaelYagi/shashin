package com.miyagi.shashin.configuration

import com.miyagi.shashin.component.*
import com.miyagi.shashin.configuration.MultiSecurityConfig.Companion.publicList
import com.miyagi.shashin.repository.UserRepository
import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall
import org.springframework.security.web.header.HeaderWriterFilter
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
import org.springframework.security.web.session.HttpSessionEventPublisher
import javax.sql.DataSource

// Used to validate URL paths for login redirect
class MultiSecurityConfig {
    companion object {
        val validWebPaths = arrayOf(
            "timeline",
            "timeline\\/video",
            "albums",
            "album\\/(\\d+)",
            "album\\/(\\d+)\\/video",
            "recent",
            "taken",
            "accessed",
            "modified",
            "folders",
            "folder\\/(.*)",
            "people",
            "person\\/compreface\\/(\\d+)",
            "person\\/(\\d+)",
            "matches\\/(\\d+)",
            "favorites",
            "map",
            "map\\?.*",
            "notifications",
            "dashboard",
            "archived",
            "settings",
            "settings\\/users",
            "settings\\/snapshot",
            "settings\\/logs",
            "settings\\/match",
            "settings\\/scan",
            "dashboard",
            "search",
            "search\\?term=.*",
            "users\\/profile",
            "users\\/update",
            "users\\/apikey",
            "rss",
            "atom",
            "test",
            "slideshow",
            "actuator"
        )

        var resourceList = arrayOf(
            "/",
            "/docs/**",
            "/articles/**",
            "/health",
            "/features",
            "/css/**",
            "/js/**",
            "/fonts/**",
            "/images/**",
            "/users/register"
        )

        var publicList = resourceList + arrayOf(
            "/websocket-endpoint",
            "/topic/messages",
            "/topic/matchmessages",
            "/topic/scrapermessages",
            "/settings/matchmessage",
            "/settings/scanmessage",
            "/dashboard/statmessages",
            "/dashboard/statmessage",
//            "/download/share/**/album/**",
            "/download/share/**",
            "/share/**",
            "/api/v1/thumbnails/**", "/api/v1/image/**", "/api/v1/video/**", "/api/v1/profile/**",
            "/image/**",
            "/video/**",
//            "/**/rss",
//            "/**/atom",
            "rss",
            "atom",
            "/users/login",
            "/users/logout"
        )

        var adminList = arrayOf(
            "timeline",
            "timeline/**",
            "actuator/**",
            "health",
            "complete/metadata/**",
            "albums/add",
            "rescan/metadata",
            "api/v1/update/**",
            "api/v1/folders",
            "api/v1/folders/**",
            "api/v1/folder",
            "api/v1/folder/**",
            "api/v1/recent",
            "api/v1/recent/**",
            "api/v1/taken",
            "api/v1/taken/**",
            "api/v1/accessed",
            "api/v1/accessed/**",
            "api/v1/modified",
            "api/v1/modified/**",
            "api/v1/share/album/save",
            "api/v1/exif/metadata/**",
            "api/v1/folders",
            "api/v1/all/album/delete",
            "api/v1/keywords",
            "api/v1/rescan/metadata"
        )

        val superList = adminList + arrayOf(
            "settings/**",
            "settings",
            "settings/users",
            "settings/scan",
            "users/delete",
            "api/v1/system/settings",
            "api/v1/users/info",
            "api/v1/user/info/**"
        )

        val allRoleList = arrayOf(
            "comments/**",
            "albums",
            "favorites",
            "slideshow",
            "map/**",
            "search/**",
            "articles/endpoints",
            "api/v1/endpoints",
            "api/v1/album/**",
            "api/v1/albums",
            "api/v1/albums/**",
            "api/v1/users/apikey/update",
            "api/v1/mapdata",
            "api/v1/placedata",
            "api/v1/profile/**",
            "api/v1/metadata/**",
            "api/v1/user/self",
            "api/v1/comment/**",
            "api/v1/favorites/**",
            "api/v1/complete/metadata/**",
            "api/v1/health",
            "api/v1/status"
        )
    }
}

@EnableMethodSecurity(securedEnabled = true)
@Configuration
@Order(1)
class ApiSecurityConfig {

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val apiAccessDeniedHandler: ApiAccessDeniedHandler? = null

    @Bean
    fun passwordApiEncoder(): PasswordEncoder? {
        return BCryptPasswordEncoder()
    }

    @Bean
    @Throws(Exception::class)
    fun configure(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf{ it.disable() }
            .sessionManagement{ it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(AuthenticationFilter(userRepository), UsernamePasswordAuthenticationFilter::class.java)
            .securityMatcher("/api/v1/**")
            .authorizeHttpRequests{ it.anyRequest().authenticated() }
            .exceptionHandling{ it.accessDeniedHandler(apiAccessDeniedHandler) }

        return http.build()
    }

    @Bean
    fun apiSecurityCustomizer(): WebSecurityCustomizer {
        return WebSecurityCustomizer { web: WebSecurity ->
            web.ignoring()
                .requestMatchers("/api/v1/thumbnails/**", "/api/v1/image/**", "/api/v1/video/**", "/api/v1/profile/**")
        }
    }
}

@EnableMethodSecurity(securedEnabled = true)
@Configuration
@Order(2)
class WebSecurityConfig {

    @Autowired
    private val environment: Environment? = null

    @Autowired
    private val dataSource: DataSource? = null

    @Autowired
    private var userDetailsService: UserDetailsService? = null

    @Autowired
    private val authFailureHandler: AuthFailureHandler? = null

    @Autowired
    private val authSuccessHandler: AuthSuccessHandler? = null

    @Value("\${app.role.super}")
    private var superRole: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    @Value("\${app.rememberme.expiration.seconds}")
    private var expirationSeconds: Int? = null

    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

    @Autowired
    @Throws(java.lang.Exception::class)
    fun configAuthentication(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService)
    }

    //        @Bean
    fun passEncoder(): PasswordEncoder? {
        return BCryptPasswordEncoder()
    }

    @Bean
    @Throws(java.lang.Exception::class)
    fun authenticationManager(http: HttpSecurity): AuthenticationManager {
        return http.getSharedObject(AuthenticationManagerBuilder::class.java)
            .build()
    }

    @Bean
    fun sessionRegistry(): SessionRegistry? {
        return SessionRegistryImpl()
    }

    @Bean
    fun httpSessionEventPublisher(): ServletListenerRegistrationBean<HttpSessionEventPublisher>? {
        return ServletListenerRegistrationBean(HttpSessionEventPublisher())
    }

    @Autowired
    @Throws(java.lang.Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth
            .jdbcAuthentication()
            .dataSource(dataSource)
            .passwordEncoder(passEncoder())
            .usersByUsernameQuery(
                "SELECT username, password, TRUE from user where lower(username) = lower(?)"
            )
            .authoritiesByUsernameQuery(
                "SELECT username, authority from user where lower(username) = lower(?)"
            )
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilter(http: HttpSecurity): SecurityFilterChain {
        var profile = ""
        if (environment != null && environment.activeProfiles.isNotEmpty()) {
            profile = environment.activeProfiles[0]
        }

        http
            .addFilterBefore(CSPNonceFilter(), HeaderWriterFilter::class.java)
            .sessionManagement{ it.sessionCreationPolicy(SessionCreationPolicy.ALWAYS) }
            .headers{
                it
                    .xssProtection{ it.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK) }
                    .contentSecurityPolicy{ it.policyDirectives("worker-src 'self' 'nonce-{nonce}' blob:") }
                    .frameOptions{ it.sameOrigin() }
            }
            .authorizeHttpRequests {
                it
                    .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                    .requestMatchers("/").permitAll()
                    .requestMatchers("/users/login").permitAll()
                    .requestMatchers("/users/register").permitAll()
                    .requestMatchers("/docs/**").permitAll()
                    .requestMatchers("/articles/**").permitAll()
                    .requestMatchers("/health").permitAll()
                    .requestMatchers("/features").permitAll()
                    .requestMatchers("/css/**").permitAll()
                    .requestMatchers("/js/**").permitAll()
                    .requestMatchers("/fonts/**").permitAll()
                    .requestMatchers("/images/**").permitAll()
                    .requestMatchers(publicList.joinToString(",")).permitAll()
                    .requestMatchers(MultiSecurityConfig.adminList.joinToString(","))
                    .hasRole(adminRole.toString().replace("ROLE_", ""))
                    .requestMatchers(MultiSecurityConfig.superList.joinToString(","))
                    .hasRole(superRole.toString().replace("ROLE_", ""))
                    .requestMatchers(MultiSecurityConfig.allRoleList.joinToString(","))
                    .hasAnyRole(
                        userRole.toString().replace("ROLE_", ""),
                        adminRole.toString().replace("ROLE_", ""),
                        superRole.toString().replace("ROLE_", "")
                    )
                    .anyRequest().authenticated()
            }
            .formLogin {
                it
                    .loginPage("/users/login").permitAll()
                    .successHandler(authSuccessHandler?.setProfile(profile)).permitAll()
                    .failureHandler(authFailureHandler).permitAll()
            }

        if (profile == "test") {
            http
                .rememberMe{ it.key(rememberMeKey).tokenValiditySeconds(3600) } // Use cookie based remember me for tests
        } else {
            http
                .rememberMe{ it.key(rememberMeKey).tokenValiditySeconds(expirationSeconds!!) }
        }

        http
            .logout{
                it
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
            }
            .csrf{
                it.disable()
            }.httpBasic(Customizer.withDefaults())

        http.exceptionHandling{ it.authenticationEntryPoint(AjaxAwareAuthenticationEntryPoint("/users/login")) }

        http.sessionManagement{
            it
                .maximumSessions(100)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/users/login")
                .sessionRegistry(sessionRegistry())
        }

        return http.build()
    }

    @Bean
    fun persistentTokenRepository(): PersistentTokenRepository? {
        val tokenRepo = JdbcTokenRepositoryImpl()
        tokenRepo.setDataSource(dataSource!!)
        return tokenRepo
    }

    @Bean
    fun allowUrlEncodedSlashHttpFirewall(): HttpFirewall {
        val firewall = StrictHttpFirewall()
        firewall.setAllowUrlEncodedPercent(true)
        firewall.setAllowSemicolon(true)
        firewall.setAllowUrlEncodedSlash(true)
        return firewall
    }

    @Bean
    fun webSecurityCustomizer(): WebSecurityCustomizer {
        return WebSecurityCustomizer { web: WebSecurity ->
            web
                .httpFirewall(allowUrlEncodedSlashHttpFirewall())
                .ignoring()
                .requestMatchers(
                    MultiSecurityConfig.resourceList.joinToString(",")
                )

        }
    }
}