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

    /**
     * The Git smart-HTTP service endpoints, in both the bare and {@code .git} forms
     * that GitHttpController maps. Authorization for these lives in that controller,
     * which is the only place that can answer a Git client in a way it understands.
     */
    private static final String[] GIT_SERVICE_PATHS = {
        "/*/*/info/refs", "/*/*.git/info/refs",
        "/*/*/git-upload-pack", "/*/*.git/git-upload-pack",
        "/*/*/git-receive-pack", "/*/*.git/git-receive-pack"
    };

    private static String[] concat(String[] first, String[] second) {
        String[] all = java.util.Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }

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
                        "/avatars/**",
                        // Spring forwards here to render an error. Without it, an
                        // anonymous visitor who hits a bad URL is redirected to the
                        // login page instead of being told the page does not exist.
                        "/error"
                ).permitAll()
                /*
                 * Git smart HTTP authenticates itself. GitHttpController reads HTTP Basic
                 * or a token, checks READ for upload-pack and WRITE for receive-pack, and
                 * answers 401 with WWW-Authenticate so the client prompts for credentials.
                 *
                 * Spring must not get there first: its response to an unauthenticated
                 * request is a redirect to /login, which a Git client follows until it
                 * gives up ("Maximum (20) redirects followed") instead of asking for a
                 * password. Leaving only the GET half open was worse still - ref discovery
                 * succeeded and the pack transfer then failed, so an anonymous clone of a
                 * public repository broke halfway through.
                 */
                .requestMatchers(GIT_SERVICE_PATHS).permitAll()
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
                // The service paths are listed explicitly: "/**.git/**" misses the
                // suffix-less form, so a push to /owner/repo/git-receive-pack was
                // rejected as a CSRF failure.
                .ignoringRequestMatchers(
                        concat(new String[] {"/api/**", "/git/**", "/**.git/**"}, GIT_SERVICE_PATHS))
            );
        return http.build();
    }
}
