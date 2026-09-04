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

    /**
     * highlight.js id to the name a person would recognise.
     *
     * <p>The two differ often enough to matter: highlight.js calls HTML "xml" and C#
     * "csharp", and a language bar reading "xml 62%" on an HTML project looks broken.
     */
    private static final Map<String, String> DISPLAY_NAMES = Map.ofEntries(
            Map.entry("java", "Java"),
            Map.entry("kotlin", "Kotlin"),
            Map.entry("javascript", "JavaScript"),
            Map.entry("typescript", "TypeScript"),
            Map.entry("python", "Python"),
            Map.entry("ruby", "Ruby"),
            Map.entry("go", "Go"),
            Map.entry("rust", "Rust"),
            Map.entry("php", "PHP"),
            Map.entry("csharp", "C#"),
            Map.entry("c", "C"),
            Map.entry("cpp", "C++"),
            Map.entry("objectivec", "Objective-C"),
            Map.entry("swift", "Swift"),
            Map.entry("lua", "Lua"),
            Map.entry("perl", "Perl"),
            Map.entry("r", "R"),
            Map.entry("sql", "SQL"),
            Map.entry("bash", "Shell"),
            Map.entry("xml", "HTML"),
            Map.entry("css", "CSS"),
            Map.entry("scss", "SCSS"),
            Map.entry("less", "Less"),
            Map.entry("json", "JSON"),
            Map.entry("yaml", "YAML"),
            Map.entry("ini", "INI"),
            Map.entry("markdown", "Markdown"),
            Map.entry("graphql", "GraphQL"),
            Map.entry("groovy", "Groovy"),
            Map.entry("makefile", "Makefile"),
            Map.entry("diff", "Diff"));

    /**
     * Display name to the dot colour GitHub uses, taken from Linguist.
     *
     * <p>An unlisted language falls back to a neutral grey rather than to a random
     * colour, so a language bar never claims a relationship that is not there.
     */
    private static final Map<String, String> COLORS = Map.ofEntries(
            Map.entry("Java", "#b07219"),
            Map.entry("Kotlin", "#A97BFF"),
            Map.entry("JavaScript", "#f1e05a"),
            Map.entry("TypeScript", "#3178c6"),
            Map.entry("Python", "#3572A5"),
            Map.entry("Ruby", "#701516"),
            Map.entry("Go", "#00ADD8"),
            Map.entry("Rust", "#dea584"),
            Map.entry("PHP", "#4F5D95"),
            Map.entry("C#", "#178600"),
            Map.entry("C", "#555555"),
            Map.entry("C++", "#f34b7d"),
            Map.entry("Objective-C", "#438eff"),
            Map.entry("Swift", "#F05138"),
            Map.entry("Lua", "#000080"),
            Map.entry("Perl", "#0298c3"),
            Map.entry("R", "#198CE7"),
            Map.entry("SQL", "#e38c00"),
            Map.entry("Shell", "#89e051"),
            Map.entry("HTML", "#e34c26"),
            Map.entry("CSS", "#563d7c"),
            Map.entry("SCSS", "#c6538c"),
            Map.entry("Less", "#1d365d"),
            Map.entry("JSON", "#292929"),
            Map.entry("YAML", "#cb171e"),
            Map.entry("INI", "#d1dbe0"),
            Map.entry("Markdown", "#083fa1"),
            Map.entry("GraphQL", "#e10098"),
            Map.entry("Groovy", "#4298b8"),
            Map.entry("Makefile", "#427819"),
            Map.entry("Diff", "#88dddd"));

    private static final String DEFAULT_COLOR = "#8b949e";

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

    /**
     * @param path file name or path
     * @return the human-readable language name, or null when the file counts towards
     *         no language at all
     */
    public static String displayName(String path) {
        String id = forPath(path);
        return id == null ? null : DISPLAY_NAMES.get(id);
    }

    /**
     * @param displayName a name returned by {@link #displayName(String)}
     * @return the hex colour for its dot; never null
     */
    public static String color(String displayName) {
        return COLORS.getOrDefault(displayName, DEFAULT_COLOR);
    }
}
