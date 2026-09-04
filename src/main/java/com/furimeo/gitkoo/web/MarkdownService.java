package com.furimeo.gitkoo.web;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Renders Markdown to sanitized HTML (DESIGN.md §49).
 *
 * <p>Uses CommonMark for parsing/rendering and Jsoup to strip dangerous HTML
 * (scripts, event handlers, etc.) to prevent XSS. Raw HTML in Markdown is
 * sanitized, not executed.
 *
 * @see DESIGN.md §49
 */
@Service
public class MarkdownService {

    /**
     * GitHub-flavoured Markdown extensions. Without these, tables render as raw
     * pipe characters and {@code - [ ]} renders as literal brackets rather than
     * checkboxes, which is what readers of a Git forge actually expect.
     */
    private static final java.util.List<org.commonmark.Extension> EXTENSIONS = java.util.List.of(
            org.commonmark.ext.gfm.tables.TablesExtension.create(),
            org.commonmark.ext.gfm.strikethrough.StrikethroughExtension.create(),
            org.commonmark.ext.task.list.items.TaskListItemsExtension.create(),
            org.commonmark.ext.autolink.AutolinkExtension.create());

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    /** Safelist allowing common Markdown HTML but stripping scripts and event handlers. */
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            .addTags("del", "ins", "sub", "sup", "kbd")
            .addTags("table", "thead", "tbody", "tr", "th", "td")
            // Task-list checkboxes, always rendered disabled so they are display-only.
            .addTags("input")
            .addAttributes("input", "type", "checked", "disabled")
            .addAttributes("li", "class")
            .addAttributes("ul", "class")
            .addAttributes("a", "href", "title", "rel")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addAttributes("th", "align")
            .addAttributes("td", "align")
            .addAttributes("code", "class")
            .addAttributes("pre", "class")
            .addProtocols("a", "href", "http", "https", "mailto", "#")
            .addProtocols("img", "src", "http", "https", "/")
            .preserveRelativeLinks(true);

    /** Renders Markdown to sanitized HTML. Returns empty string for null/blank input. */
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        return Jsoup.clean(html, SAFELIST);
    }
}
