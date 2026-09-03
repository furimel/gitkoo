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
import com.furimeo.gitkoo.auth.UserService;

/**
 * Spring Security wiring: session-based authentication with a custom login page, plus a
 * Bearer token filter for the API (DESIGN.md §43, §78).
 *
 * <p>Permits the first-run setup page, login, static assets, and the health endpoint
 * anonymously. Everything else requires authentication. CSRF is enabled (default) to
 * protect form POSTs; it is disabled for {@code /api/**} and git transport paths since
 * those use tokens, not browser sessions.
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AccessTokenAuthenticationFilter accessTokenFilter) throws Exception {
        http
            .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/health",
                        "/setup",
                        "/login",
                        "/css/**",
                        "/js/**",
                        "/vendor/**"
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
