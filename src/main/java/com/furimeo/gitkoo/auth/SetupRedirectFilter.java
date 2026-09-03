package com.furimeo.gitkoo.auth;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirects unauthenticated requests to {@code /setup} when no admin account exists yet.
 *
 * <p>This guards every protected page during the first-run experience (DESIGN.md §68):
 * the very first time GitKoo starts, there is no admin, so the user should be funneled to
 * the setup page instead of seeing a login form they cannot use.
 *
 * <p>Once an admin is created, {@link UserService#needsSetup()} returns false and this
 * filter becomes a no-op.
 */
@Component
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
        // Don't redirect away from the setup/login/health/assets paths themselves.
        return !path.equals(SETUP_URL)
                && !path.equals(LOGIN_URL)
                && !path.startsWith("/assets/")
                && !path.equals("/health");
    }
}
