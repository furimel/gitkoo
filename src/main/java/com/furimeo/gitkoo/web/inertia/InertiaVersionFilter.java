package com.furimeo.gitkoo.web.inertia;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Forces a full page load when the client is running a bundle from a previous build.
 *
 * <p>Without this, someone who left a tab open across a deploy keeps making requests
 * with the old JavaScript against new props, and the failures look like random
 * rendering bugs. Inertia's answer is a 409 carrying the location to reload, which
 * the client turns into a hard navigation.
 *
 * <p>Only GET is checked. Answering 409 to a POST would silently discard whatever
 * the user just submitted; the redirect that follows the POST is where the version
 * gets noticed instead.
 */
@Component
@Order(1)
public class InertiaVersionFilter extends OncePerRequestFilter {

    private final ViteManifest manifest;

    public InertiaVersionFilter(ViteManifest manifest) {
        this.manifest = manifest;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        boolean inertia = "true".equals(request.getHeader(InertiaView.INERTIA_HEADER));
        String clientVersion = request.getHeader(InertiaView.VERSION_HEADER);

        if (inertia && "GET".equals(request.getMethod())
                && clientVersion != null && !clientVersion.equals(manifest.version())) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setHeader(InertiaView.LOCATION_HEADER, request.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }
}
