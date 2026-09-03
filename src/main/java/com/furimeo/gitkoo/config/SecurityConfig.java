package com.furimeo.gitkoo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal Spring Security wiring for the foundation phase.
 *
 * <p>Foundation phase only needs {@code /health} to work, so everything is permitted
 * and CSRF is off for now. Full authentication (form login, sessions, CSRF policy,
 * access tokens, protected routes) lands in the auth phase. BCrypt is wired early so
 * user creation can use it from the start.
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
        // Foundation phase: only the health endpoint needs to work. Full authentication
        // (form login, sessions, CSRF policy, access tokens) lands in the auth phase.
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health").permitAll()
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
