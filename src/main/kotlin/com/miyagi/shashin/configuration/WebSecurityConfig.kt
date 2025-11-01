package com.miyagi.shashin.configuration

import com.miyagi.shashin.component.*
import com.miyagi.shashin.configuration.MultiSecurityConfig.Companion.publicApiList
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
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.sql.DataSource

// Used to validate URL paths for login redirect
class MultiSecurityConfig {
    companion object {
        // Pages valid for session pages
        val validWebPaths = arrayOf(
            "timeline",
            "timeline\\/.*",
            "albums",
            "album\\/(\\d+)",
            "album\\/(\\d+)\\/.*",
            "recent",
            "recent\\/(\\d+)\\/.*",
            "taken",
            "taken\\/(\\d+)\\/.*",
            "accessed",
            "accessed\\/(\\d+)\\/.*",
            "modified",
            "modified\\/(\\d+)\\/.*",
            "folders\\/(\\d+)",
            "folders",
            "folder\\/(.*)",
            "people",
            "person\\/compreface\\/(\\d+)",
            "person\\/(\\d+)",
            "matches\\/(\\d+)",
            "favorites",
            "favorites\\/(\\d+)\\/.*",
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
            "bcrypt",
            "bcrypt\\/(.*)",
            "search",
            "search\\?term=.*",
            "users\\/profile",
            "users\\/account",
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
            "/testgrounds",
            "/bcrypt/**",
            "/features",
            "/css/**",
            "/js/**",
            "/fonts/**",
            "/images/**",
            "/media/**",
            "/users/register"
        )

        var publicApiList = arrayOf(
            "/api/v1/thumbnails/**", "/api/v1/image/**", "/api/v1/video/**", "/api/v1/profile/**", "/api/v1/share/**", "/api/v1/tags", "/api/v1/metadata/*"
        )

        var publicList = publicApiList + resourceList + arrayOf(
            "/websocket-endpoint",
            "/topic/messages",
            "/topic/matchmessages",
            "/topic/scrapermessages",
            "/settings/matchmessage",
            "/settings/scanmessage",
            "/dashboard/statmessages",
            "/dashboard/statmessage",
            "/download/share/**",
            "/share/**",
            "/image/**",
            "/video/**",
            "/*/rss",
            "/*/atom",
            "/users/login",
            "/users/logout",
            "/metadata/*"
        )

        private val adminApiList = arrayOf(
            "/api/v1/timeline",
            "/api/v1/timeline/**",
            "/api/v1/update/**",
            "/api/v1/folders",
            "/api/v1/folders/**",
            "/api/v1/folder",
            "/api/v1/folder/**",
            "/api/v1/recent",
            "/api/v1/recent/**",
            "/api/v1/taken",
            "/api/v1/taken/**",
            "/api/v1/accessed",
            "/api/v1/accessed/**",
            "/api/v1/modified",
            "/api/v1/modified/**",
            "/api/v1/share/album/save",
            "/api/v1/exif/metadata/**",
            "/api/v1/folders",
            "/api/v1/all/album/delete",
            "/api/v1/keywords",
            "/api/v1/rescan/metadata",
            "/api/v1/dashboard/data"
        )

        var adminList = adminApiList + arrayOf(
            "/timeline",
            "/timeline/**",
            "/actuator/**",
            "/health",
            "/complete/metadata/**",
            "/albums/add",
            "/rescan/metadata"
        )

        private val superApiList = arrayOf(
            "/api/v1/system/settings",
            "/api/v1/users/info",
            "/api/v1/users/role/**",
            "/api/v1/users/authorized/**",
            "/api/v1/users/delete",
            "/api/v1/user/update/password",
            "/api/v1/user/info/**",
            "/api/v1/users/update/**"
        )

        val superList = adminList + superApiList + arrayOf(
            "/settings/**",
            "/settings",
            "/settings/users",
            "/settings/scan",
            "/users/delete",
            "/user/update/password",
            "/users/update/**"
        )

        private val allRoleApiList = arrayOf(
            "/api/v1/sharedalbums",
            "/api/v1/sharedalbums/**",
            "/api/v1/mapdata",
            "/api/v1/mapdata/**",
            "/api/v1/albumcomments",
            "/api/v1/albumcomments/**",
            "/api/v1/endpoints",
            "/api/v1/album/**",
            "/api/v1/albums",
            "/api/v1/albums/**",
            "/api/v1/users/update/apikey",
            "/api/v1/placedata",
            "/api/v1/profile/**",
            "/api/v1/metadata/**",
            "/api/v1/user/self",
            "/api/v1/comment/**",
            "/api/v1/favorites/**",
            "/api/v1/complete/metadata/**",
            "/api/v1/health",
            "/api/v1/status",
            "/api/v1/media/metadata/**",
            "/api/v1/random/**"
        )

        val allRoleList = allRoleApiList + arrayOf(
            "/comments/**",
            "/albums",
            "/favorites",
            "/random/**",
            "/media/metadata/**",
            "/map/**",
            "/search/**",
            "/articles/endpoints"
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

    // Permit all for resources
    @Bean
    @Throws(Exception::class)
    fun publicSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(*publicApiList)
            .authorizeHttpRequests { it.anyRequest().permitAll() }

        return http.build()
    }

    @Bean
    @Throws(Exception::class)
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf{ it.disable() }
            .cors{ httpSecurityCorsConfigurer ->
                val configuration = CorsConfiguration()
                configuration.allowedOrigins = listOf("*")
                configuration.allowedMethods = listOf("*")
                configuration.allowedHeaders = listOf("*")
                val source = UrlBasedCorsConfigurationSource()
                source.registerCorsConfiguration("/**", configuration)
                httpSecurityCorsConfigurer.configurationSource(source)
            }
            .sessionManagement{ it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(AuthenticationFilter(userRepository), UsernamePasswordAuthenticationFilter::class.java)
            .securityMatcher("/api/v1/**")
            .authorizeHttpRequests{ it.anyRequest().authenticated() }
            .exceptionHandling{ it.accessDeniedHandler(apiAccessDeniedHandler) }

        return http.build()
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
    private val authFailureHandler: AuthFailureHandler? = null

    @Autowired
    private val authSuccessHandler: AuthSuccessHandler? = null

    @Autowired
    private val customLogoutHandler: CustomLogoutHandler? = null

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

    @Bean
    fun sessionRegistry(): SessionRegistry? {
        return SessionRegistryImpl()
    }

    @Bean
    fun httpSessionEventPublisher(): ServletListenerRegistrationBean<HttpSessionEventPublisher>? {
        return ServletListenerRegistrationBean(HttpSessionEventPublisher())
    }

    // Permit all for resources
    @Bean
    @Throws(Exception::class)
    fun resourceSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(*MultiSecurityConfig.resourceList)
            .authorizeHttpRequests { it.anyRequest().permitAll() }

        return http.build()
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
                    .requestMatchers(*publicList).permitAll()
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
                    .logoutUrl("/users/logout")
                    .addLogoutHandler(customLogoutHandler)
                    .logoutSuccessUrl("/users/login")
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
}