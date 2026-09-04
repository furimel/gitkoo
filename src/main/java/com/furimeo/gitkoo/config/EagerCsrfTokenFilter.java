package com.furimeo.gitkoo.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resolves the CSRF token before the view renders.
 *
 * <p>Spring Security defers token generation until something reads it, and generating
 * one creates the HTTP session. On a page whose markup exceeds the response buffer
 * before its first {@code <form>} - the icon sprite alone is around 20 KB - the
 * response is already committed by then, and session creation fails with
 * "Cannot create a session after the response has been committed".
 *
 * <p>Touching the token here makes session creation independent of where forms happen
 * to sit in the markup, so page length can never break form rendering again.
 */
@Component
@Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
public class EagerCsrfTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            // Reading the value is what forces the token - and the session - into existence.
            token.getToken();
        }
        chain.doFilter(request, response);
    }
}
