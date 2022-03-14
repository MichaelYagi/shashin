package com.miyagi.shashin.configuration

import com.miyagi.shashin.component.AjaxAwareAuthenticationEntryPoint
import com.miyagi.shashin.component.AuthFailureHandler
import com.miyagi.shashin.component.AuthSuccessHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall
import org.springframework.security.web.session.HttpSessionEventPublisher
import javax.sql.DataSource


@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true)
class WebSecurityConfig: WebSecurityConfigurerAdapter() {

    @Autowired
    private val dataSource: DataSource? = null

    @Autowired
    private val authFailureHandler: AuthFailureHandler? = null

    @Autowired
    private val authSuccessHandler: AuthSuccessHandler? = null

    @Value("\${app.api.version}")
    private val apiVersion: String? = null

    @Value("\${app.role.admin}")
    private var adminRole: String? = null

    @Value("\${app.role.user}")
    private var userRole: String? = null

    @Value("\${app.rememberme.key}")
    private var rememberMeKey: String? = null

    @Value("\${app.rememberme.expiration.seconds}")
    private var expirationSeconds: Int? = null

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
                "SELECT username, password, TRUE from user where username = ?")
            .authoritiesByUsernameQuery(
                "SELECT username, authority from user where username = ?");
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and()
            .headers()
                .xssProtection()
                .and()
                .frameOptions()
                .sameOrigin()
                .and()
            .authorizeRequests()
                .antMatchers("/", "/docs/**", "/features", "/api/**", "/share/**", "/css/**", "/js/**", "/fonts/**", "/images/**", "/users/register", "/users/login", "/users/logout", "/websocket-endpoint", "/topic/messages", "/topic/matchmessages", "/settings/matchmessage", "/settings/scanmessage","/dashboard/statmessages","/dashboard/statmessage").permitAll()
                .antMatchers("settings/**", "settings", "settings/users", "settings/scan", "favorites", "timeline", "users/delete", "albums/add").hasRole(adminRole.toString().replace("ROLE_", ""))
                .antMatchers("comments/**", "albums", "map/**", "search/**").hasAnyRole(userRole.toString().replace("ROLE_", ""), adminRole.toString().replace("ROLE_", ""))
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