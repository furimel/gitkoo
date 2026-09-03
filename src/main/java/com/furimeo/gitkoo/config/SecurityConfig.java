package com.furimeo.gitkoo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security wiring: session-based authentication with a custom login page.
 *
 * <p>Permits the first-run setup page, login, static assets, and the health endpoint
 * anonymously. Everything else requires authentication. CSRF is enabled (default) to
 * protect form POSTs. Git smart-HTTP endpoints are handled separately and disable CSRF
 * for those paths since Git clients are not browser sessions.
 *
 * @see DESIGN.md §43, §78
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                // Git smart-HTTP endpoints use HTTP auth / tokens, not browser forms.
                .ignoringRequestMatchers("/api/**", "/git/**", "/**.git/**")
            );
        return http.build();
    }
}
