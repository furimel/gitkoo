package com.furimeo.gitkoo.auth;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Loads a {@link User} from the database and maps it to Spring Security's
 * {@link UserDetails} for session-based authentication (DESIGN.md §43).
 */
@Component
public class GitKooUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public GitKooUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UsernameNotFoundException("User is not active: " + username);
        }

        var authorities = user.isAdmin()
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!"ACTIVE".equals(user.getStatus()))
                .build();
    }

    /**
     * Resolves a {@link User} from a Spring Security principal (the username string).
     */
    public User resolveUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
