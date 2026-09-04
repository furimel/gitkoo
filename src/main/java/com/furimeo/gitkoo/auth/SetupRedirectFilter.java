package com.furimeo.gitkoo.auth;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirects unauthenticated requests to {@code /setup} when no admin account exists yet
 * (DESIGN.md §68). Registered early in the Spring Security filter chain so it runs
 * before the authentication check redirects to /login.
 */
public class SetupRedirectFilter extends OncePerRequestFilter {

    private static final String SETUP_URL = "/setup";
    private static final String LOGIN_URL = "/login";

    private final UserService userService;

    public SetupRedirectFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (userService.needsSetup() && shouldRedirect(request)) {
            response.sendRedirect(SETUP_URL);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldRedirect(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals(SETUP_URL)
                && !path.equals(LOGIN_URL)
                && !path.startsWith("/assets/")
                && !path.equals("/health");
    }
}
