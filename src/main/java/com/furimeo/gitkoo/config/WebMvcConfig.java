package com.furimeo.gitkoo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers static assets under {@code /assets/**} to avoid conflicts with the
 * {@code /{username}/{name}} repository route pattern.
 *
 * <p>Without this, a request like {@code /css/gitkoo.css} would match the
 * {@code /{username}/{name}} controller mapping (username="css", name="gitkoo.css")
 * instead of being served as a static resource.
 *
 * @see DESIGN.md §4
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve everything under classpath:/static/ at the /assets/ URL prefix.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/");
    }
}
