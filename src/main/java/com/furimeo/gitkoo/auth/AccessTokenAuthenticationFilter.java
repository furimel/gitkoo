package com.furimeo.gitkoo.auth;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates API requests carrying a Bearer personal access token (DESIGN.md §43).
 *
 * <p>Runs before the standard form-login filter for paths under {@code /api/}. When a valid
 * token is presented, the resolved user is placed in the security context so downstream
 * controllers see an authenticated principal. Sessions are not created for token requests.
 */
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenService accessTokenService;
    private final UserService userService;

    public AccessTokenAuthenticationFilter(AccessTokenService accessTokenService, UserService userService) {
        this.accessTokenService = accessTokenService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String rawToken = header.substring(BEARER_PREFIX.length()).trim();
            authenticate(rawToken, request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String rawToken, HttpServletRequest request) {
        Optional<AccessTokenService.ResolvedToken> resolved = accessTokenService.resolve(rawToken);
        if (resolved.isEmpty()) {
            return;
        }
        Optional<User> user = userService.findById(resolved.get().userId());
        if (user.isEmpty() || !"ACTIVE".equals(user.get().getStatus())) {
            return;
        }
        var authorities = user.get().isAdmin()
                ? java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var auth = new UsernamePasswordAuthenticationToken(user.get().getUsername(), null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
