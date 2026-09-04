package com.furimeo.gitkoo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.furimeo.gitkoo.auth.AccessTokenAuthenticationFilter;
import com.furimeo.gitkoo.auth.AccessTokenService;
import com.furimeo.gitkoo.auth.SetupRedirectFilter;
import com.furimeo.gitkoo.auth.UserService;

/**
 * Spring Security wiring: session-based authentication with a custom login page,
 * plus a Bearer token filter for the API and a setup redirect for first-run
 * (DESIGN.md §43, §68, §78).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessTokenAuthenticationFilter accessTokenAuthenticationFilter(
            AccessTokenService accessTokenService, UserService userService) {
        return new AccessTokenAuthenticationFilter(accessTokenService, userService);
    }

    @Bean
    public SetupRedirectFilter setupRedirectFilter(UserService userService) {
        return new SetupRedirectFilter(userService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AccessTokenAuthenticationFilter accessTokenFilter,
                                                   SetupRedirectFilter setupRedirectFilter,
                                                   EagerCsrfTokenFilter eagerCsrfTokenFilter) throws Exception {
        http
            // SetupRedirectFilter runs first so it redirects to /setup before
            // the authentication layer sends the user to /login (DESIGN.md §68).
            .addFilterBefore(setupRedirectFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
            // Runs last in the chain, so the CSRF token exists before any view renders.
            .addFilterAfter(eagerCsrfTokenFilter,
                    org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            /*
             * Order matters. A repository lives at /{username}/{name}, which also
             * matches two-segment application routes like /settings/keys, so every
             * such route has to be claimed before the repository pattern is reached.
             *
             * Anonymous readers are allowed as far as the repository pages; the
             * controllers then call requireRead, and RepositoryPermissionService
             * grants READ to anonymous callers only for PUBLIC repositories. The
             * pattern below is deliberately GET-only, so no anonymous writes.
             */
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/health",
                        "/healthz",
                        "/setup",
                        "/login",
                        "/register",
                        "/assets/**",
                        "/avatars/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(
                        "/",
                        "/new",
                        "/notifications/**",
                        "/settings/**",
                        "/teams/**"
                ).authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/@/*",
                        "/{username}/{name}",
                        "/{username}/{name}/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                // Git smart-HTTP and API endpoints use tokens, not browser forms.
                .ignoringRequestMatchers("/api/**", "/git/**", "/**.git/**")
            );
        return http.build();
    }
}
