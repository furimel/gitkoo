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

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    /** Safelist allowing common Markdown HTML but stripping scripts and event handlers. */
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            .addTags("del", "ins", "sub", "sup", "kbd")
            .addTags("table", "thead", "tbody", "tr", "th", "td")
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
