package com.furimeo.gitkoo.web.inertia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Renders one page as an Inertia response.
 *
 * <p><a href="https://inertiajs.com/the-protocol">The protocol</a> is small enough
 * to implement directly, which is why there is no adapter dependency here: a first
 * visit gets an HTML shell carrying the page object in a JSON script tag, and every
 * navigation after that is the same page object as the whole response. Both come from the same
 * controller, the same model, and the same Spring Security rules - which is the
 * whole reason for choosing Inertia over a separate JSON API. An API would have
 * meant a second copy of every authorization decision on the client, and this
 * codebase has already shipped three authorization holes.
 */
class InertiaView implements View {

    /** Marks a request as coming from the Inertia client rather than the address bar. */
    static final String INERTIA_HEADER = "X-Inertia";
    static final String VERSION_HEADER = "X-Inertia-Version";
    static final String LOCATION_HEADER = "X-Inertia-Location";
    static final String PARTIAL_DATA_HEADER = "X-Inertia-Partial-Data";
    static final String PARTIAL_COMPONENT_HEADER = "X-Inertia-Partial-Component";

    private final String component;
    private final ObjectMapper objectMapper;
    private final ViteManifest manifest;
    private final ViteDevServer devServer;
    private final SharedProps sharedProps;

    InertiaView(String component, ObjectMapper objectMapper, ViteManifest manifest,
                ViteDevServer devServer, SharedProps sharedProps) {
        this.component = component;
        this.objectMapper = objectMapper;
        this.manifest = manifest;
        this.devServer = devServer;
        this.sharedProps = sharedProps;
    }

    @Override
    public String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

        Map<String, Object> props = new LinkedHashMap<>(sharedProps.forEveryPage());
        if (model != null) {
            model.forEach((key, value) -> {
                // Spring puts its own machinery in the model; none of it is page data.
                if (!key.startsWith("org.springframework") && !"view".equals(key)) {
                    props.put(key, value);
                }
            });
        }

        // A partial reload asks for a named subset, so the server can skip the
        // expensive props the page already has.
        String partialComponent = request.getHeader(PARTIAL_COMPONENT_HEADER);
        String only = request.getHeader(PARTIAL_DATA_HEADER);
        if (only != null && !only.isBlank() && component.equals(partialComponent)) {
            java.util.Set<String> keep = new java.util.HashSet<>(java.util.List.of(only.split(",")));
            props.keySet().removeIf(key -> !keep.contains(key));
        }

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("component", component);
        page.put("props", props);
        page.put("url", fullPath(request));
        page.put("version", manifest.version());

        response.setHeader(HttpHeaders.VARY, INERTIA_HEADER);

        if ("true".equals(request.getHeader(INERTIA_HEADER))) {
            response.setHeader(INERTIA_HEADER, "true");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(), page);
            return;
        }

        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(shell(objectMapper.writeValueAsString(page)));
    }

    /** The path the client should consider itself on, query string included. */
    private static String fullPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query == null ? uri : uri + "?" + query;
    }

    /**
     * The only HTML the server writes.
     *
     * <p>The theme attributes are set here rather than by the client so a reader on
     * a dark theme never sees a white flash before React mounts. Primer only defines
     * tokens for a light mode paired with a light theme and a dark mode paired with a
     * dark one; pairing across families matches no rule and leaves every token unset,
     * so the mode is derived from the theme's family.
     */
    private String shell(String pageJson) throws IOException {
        StringBuilder html = new StringBuilder(4096);
        html.append("<!DOCTYPE html>\n<html lang=\"en\" data-color-mode=\"auto\" ")
            .append("data-light-theme=\"light\" data-dark-theme=\"dark\">\n<head>\n")
            .append("<meta charset=\"utf-8\">\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
            .append("<link rel=\"icon\" href=\"/assets/img/favicon.svg\" type=\"image/svg+xml\">\n")
            .append("<title>GitKoo</title>\n")
            .append("<script>")
            .append(THEME_BOOTSTRAP)
            .append("</script>\n");

        if (devServer.isRunning()) {
            /*
             * Development. The URLs stay same-origin and ViteDevProxyFilter forwards
             * them, so the browser never learns the dev server exists. No stylesheet
             * links: in this mode Vite injects CSS through the module graph, which is
             * what makes an edit to a stylesheet apply without a reload.
             *
             * The preamble has to come first. Vite normally injects it while
             * transforming an HTML entry, and this application has no HTML entry to
             * transform - so without it every component fails to load with
             * "@vitejs/plugin-react can't detect preamble" and the page stays blank.
             */
            html.append(REACT_REFRESH_PREAMBLE)
                .append("<script type=\"module\" src=\"/assets/app/@vite/client\"></script>\n")
                .append("<script type=\"module\" src=\"/assets/app/src/main.tsx\"></script>\n");
        } else {
            for (String href : manifest.stylesheetUrls()) {
                html.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">\n");
            }
            if (manifest.isBuilt()) {
                html.append("<script type=\"module\" src=\"").append(manifest.scriptUrl())
                    .append("\" defer></script>\n");
            }
        }

        /*
         * Inertia 3 reads the page object from a JSON script tag keyed by the root
         * element's id, not from a data attribute on the element itself. Writing the
         * attribute form mounts nothing at all: a blank page and "cannot read
         * properties of null" in the console.
         */
        html.append("</head>\n<body>\n")
            .append("<script data-page=\"app\" type=\"application/json\">")
            .append(escapeForScript(pageJson))
            .append("</script>\n")
            .append("<div id=\"app\"></div>\n");

        if (!manifest.isBuilt() && !devServer.isRunning()) {
            html.append("<noscript>The client bundle is missing. Run `npm run dev` in "
                    + "frontend/, or build with `./gradlew bootJar`.</noscript>\n");
        }
        html.append("</body>\n</html>\n");
        return html.toString();
    }

    /**
     * Makes JSON safe inside a script element.
     *
     * <p>A script element's content is not HTML-escaped by the parser, so entity
     * escaping would corrupt the JSON. The only sequence that can end the element
     * early is a literal "<", and writing it as a JSON unicode escape leaves the
     * parsed value identical while making "</script>" - and the comment opener
     * "<!--" - impossible to write.
     *
     * <p>The replacement is written with a doubled backslash on purpose. Java expands
     * a unicode escape in the source before tokenising, even inside a string literal,
     * so the single-backslash form compiles to "<" and replaces a character with
     * itself.
     */
    private static String escapeForScript(String json) {
        return json.replace("<", "\\u003c");
    }

    /**
     * React Fast Refresh's bootstrap, copied from what @vitejs/plugin-react injects
     * into an HTML entry. Only meaningful while the dev server is running.
     */
    private static final String REACT_REFRESH_PREAMBLE = """
            <script type="module">
            import RefreshRuntime from "/assets/app/@react-refresh"
            RefreshRuntime.injectIntoGlobalHook(window)
            window.$RefreshReg$ = () => {}
            window.$RefreshSig$ = () => (type) => type
            window.__vite_plugin_react_preamble_installed__ = true
            </script>
            """;

    private static final String THEME_BOOTSTRAP = """
            (function(){var e=document.documentElement,p="auto";\
            try{p=localStorage.getItem("gitkoo-theme")||"auto"}catch(x){}\
            var d=p.indexOf("dark")===0;\
            e.setAttribute("data-color-mode",p==="auto"?"auto":(d?"dark":"light"));\
            e.setAttribute("data-light-theme",d?"light":(p==="auto"?"light":p));\
            e.setAttribute("data-dark-theme",p==="auto"?"dark":(d?p:"dark"));})();""";
}
