package gr.unipi.eshop.config;

import gr.unipi.eshop.auth.AppUserDetailsService;
import gr.unipi.eshop.shared.LogFields;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JsonAuthHandlers authHandlers,
                                           SecurityContextRepository securityContextRepository,
                                           ProblemDetailWriter writer) {
        // docs: servlet/authentication/persistence.html — SecurityContextRepository / explicit save
        // docs: servlet/authentication/session-management.html — session fixation / changeSessionId / maximumSessions
        // docs: servlet/exploits/csrf.html#csrf-integration-javascript-spa — .spa() = CookieCsrfTokenRepository + SpaCsrfTokenRequestHandler
        // docs: servlet/authentication/logout.html — LogoutFilter, REST logout, ClearSiteData
        return http
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .sessionManagement(session ->
                        // Allow only one active session per user — new login expires the oldest.
                        // Expired or missing sessions fall through to AuthenticationEntryPoint (401 JSON).
                        // docs: servlet/authentication/session-management.html#ns-concurrent-sessions
                        session.sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredSessionStrategy(event ->
                                        writer.write(event.getResponse(), HttpStatus.UNAUTHORIZED, "You have been logged in from another device.")))
                )
                .csrf(CsrfConfigurer::spa)
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/logout"))
                        .addLogoutHandler((request, response, auth) -> {
                            if (auth != null) {
                                log.atInfo()
                                        .addKeyValue(LogFields.Key.EVENT, LogFields.Event.LOGOUT)
                                        .addKeyValue(LogFields.Key.USER, auth.getName())
                                        .log("logout user={}", auth.getName());
                            }
                        })
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                        .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(Directive.COOKIES)))
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(cache ->
                        cache.requestCache(new NullRequestCache())
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authHandlers)
                        .accessDeniedHandler(authHandlers)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder,
                                                       AuthenticationEventPublisher eventPublisher) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        var manager = new ProviderManager(provider);
        manager.setAuthenticationEventPublisher(eventPublisher);

        return manager;
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        var argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        return new DelegatingPasswordEncoder(
                "argon2@SpringSecurity_v5_8",
                Map.of("argon2@SpringSecurity_v5_8", argon2)
        );
    }

    // Required for maximumSessions — publishes session lifecycle events to Spring Security's SessionRegistry
    // docs: servlet/authentication/session-management.html#ns-concurrent-sessions
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }


}
