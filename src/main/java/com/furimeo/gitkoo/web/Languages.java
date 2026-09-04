package com.furimeo.gitkoo.web;

import java.util.Locale;
import java.util.Map;

/**
 * Maps a filename to a highlight.js language id.
 *
 * <p>Resolved on the server so the browser never has to guess: highlight.js
 * auto-detection is unreliable on short files and picks a different language for
 * the same file depending on which lines it sees. The id is handed to the view as
 * a {@code data-language} attribute.
 *
 * <p>Only languages present in the vendored highlight.js bundle are listed. An
 * unknown extension returns {@code null}, and the view then renders plain text.
 */
public final class Languages {

    /** Extension (lowercase, no dot) to highlight.js language id. */
    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("java", "java"),
            Map.entry("kt", "kotlin"),
            Map.entry("kts", "kotlin"),
            Map.entry("js", "javascript"),
            Map.entry("mjs", "javascript"),
            Map.entry("cjs", "javascript"),
            Map.entry("jsx", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("tsx", "typescript"),
            Map.entry("py", "python"),
            Map.entry("rb", "ruby"),
            Map.entry("go", "go"),
            Map.entry("rs", "rust"),
            Map.entry("php", "php"),
            Map.entry("cs", "csharp"),
            Map.entry("c", "c"),
            Map.entry("h", "c"),
            Map.entry("cpp", "cpp"),
            Map.entry("cc", "cpp"),
            Map.entry("cxx", "cpp"),
            Map.entry("hpp", "cpp"),
            Map.entry("m", "objectivec"),
            Map.entry("swift", "swift"),
            Map.entry("lua", "lua"),
            Map.entry("pl", "perl"),
            Map.entry("r", "r"),
            Map.entry("sql", "sql"),
            Map.entry("sh", "bash"),
            Map.entry("bash", "bash"),
            Map.entry("zsh", "bash"),
            Map.entry("html", "xml"),
            Map.entry("htm", "xml"),
            Map.entry("xml", "xml"),
            Map.entry("svg", "xml"),
            Map.entry("css", "css"),
            Map.entry("scss", "scss"),
            Map.entry("less", "less"),
            Map.entry("json", "json"),
            Map.entry("yml", "yaml"),
            Map.entry("yaml", "yaml"),
            Map.entry("toml", "ini"),
            Map.entry("ini", "ini"),
            Map.entry("cfg", "ini"),
            Map.entry("properties", "ini"),
            Map.entry("md", "markdown"),
            Map.entry("markdown", "markdown"),
            Map.entry("graphql", "graphql"),
            Map.entry("gradle", "groovy"),
            Map.entry("diff", "diff"),
            Map.entry("patch", "diff"));

    /** Files with no extension that still have a well-known language. */
    private static final Map<String, String> BY_NAME = Map.of(
            "dockerfile", "bash",
            "makefile", "makefile",
            "gnumakefile", "makefile",
            "gemfile", "ruby",
            "rakefile", "ruby",
            "vagrantfile", "ruby",
            ".gitignore", "bash",
            ".gitattributes", "bash",
            ".editorconfig", "ini",
            ".env", "bash");

    private Languages() {
    }

    /**
     * @param path file name or path
     * @return a highlight.js language id, or null when the file should render as plain text
     */
    public static String forPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);

        String byName = BY_NAME.get(name);
        if (byName != null) {
            return byName;
        }

        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        return BY_EXTENSION.get(name.substring(dot + 1));
    }
}
