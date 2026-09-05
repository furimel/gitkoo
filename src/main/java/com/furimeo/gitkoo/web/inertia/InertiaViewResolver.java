package com.furimeo.gitkoo.web.inertia;

import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import tools.jackson.databind.ObjectMapper;

/**
 * Turns every view name a controller returns into an Inertia page.
 *
 * <p>Resolving at the view layer rather than changing thirty controllers is the
 * point: {@code return "repository/code"} keeps working, and so does every
 * {@code redirect:}, every {@code RedirectAttributes} flash, and every permission
 * check that runs before the return statement. The React component name is the view
 * name with the slashes kept, so {@code repository/code} loads
 * {@code pages/repository/code.tsx}.
 */
@Component
public class InertiaViewResolver implements ViewResolver, Ordered {

    private final ObjectMapper objectMapper;
    private final ViteManifest manifest;
    private final SharedProps sharedProps;

    public InertiaViewResolver(ObjectMapper objectMapper, ViteManifest manifest,
                               SharedProps sharedProps) {
        this.objectMapper = objectMapper;
        this.manifest = manifest;
        this.sharedProps = sharedProps;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) {
        // redirect: and forward: are handled by Spring's own resolver, which is
        // ordered ahead of this one.
        if (viewName == null || viewName.startsWith("redirect:") || viewName.startsWith("forward:")) {
            return null;
        }
        return new InertiaView(viewName, objectMapper, manifest, sharedProps);
    }

    /**
     * Last. Spring's redirect and forward handling must get first refusal, and this
     * resolver answers for everything else rather than falling through.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
