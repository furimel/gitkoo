package com.furimeo.gitkoo.web.inertia;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads Vite's build manifest so the shell can name the hashed asset files.
 *
 * <p>The alternative is a fixed filename with no content hash, which means either a
 * stale bundle after every deploy or no caching at all. The manifest is the only
 * thing that knows what {@code main.tsx} was actually compiled into.
 *
 * <p>The manifest hash also serves as Inertia's asset version: when the client sends
 * a version that no longer matches, the server answers 409 and the client does a
 * full page load, which is how a user with an old bundle open picks up a deploy.
 */
@Component
public class ViteManifest {

    private static final String MANIFEST = "static/app/.vite/manifest.json";
    private static final String ENTRY = "src/main.tsx";
    private static final String BASE = "/assets/app/";

    private final String scriptUrl;
    private final List<String> stylesheetUrls;
    private final String version;

    public ViteManifest(ObjectMapper objectMapper) {
        String script = null;
        List<String> styles = new ArrayList<>();
        String hash = "dev";

        ClassPathResource resource = new ClassPathResource(MANIFEST);
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                JsonNode entry = root.get(ENTRY);
                if (entry != null) {
                    script = BASE + entry.path("file").asText();
                    collectCss(root, entry, styles, new LinkedHashSet<>());
                    // The entry's own hashed filename changes whenever anything it
                    // imports changes, which is exactly the semantics Inertia wants.
                    hash = entry.path("file").asText();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unreadable Vite manifest at " + MANIFEST, e);
            }
        }

        this.scriptUrl = script;
        this.stylesheetUrls = List.copyOf(styles);
        this.version = hash;
    }

    /** Walks the entry's imports so a stylesheet pulled in by a lazy chunk is still linked. */
    private static void collectCss(JsonNode root, JsonNode node, List<String> out, Set<String> seen) {
        for (JsonNode css : node.path("css")) {
            String url = BASE + css.asText();
            if (!out.contains(url)) {
                out.add(url);
            }
        }
        for (JsonNode importName : node.path("imports")) {
            String name = importName.asText();
            if (seen.add(name)) {
                JsonNode imported = root.get(name);
                if (imported != null) {
                    collectCss(root, imported, out, seen);
                }
            }
        }
    }

    /**
     * @return true when a client bundle was built into this jar. False during a
     *         backend-only build, where the shell says so rather than rendering a
     *         blank page.
     */
    public boolean isBuilt() {
        return scriptUrl != null;
    }

    public String scriptUrl() {
        return scriptUrl;
    }

    public List<String> stylesheetUrls() {
        return stylesheetUrls;
    }

    public String version() {
        return version;
    }
}
