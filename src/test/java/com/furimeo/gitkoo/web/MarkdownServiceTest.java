package com.furimeo.gitkoo.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownServiceTest {

    private final MarkdownService markdownService = new MarkdownService();

    @Test
    void rendersBasicMarkdown() {
        String html = markdownService.render("# Title\n\nSome **bold** text.");
        assertThat(html).contains("<h1>Title</h1>");
        assertThat(html).contains("<strong>bold</strong>");
    }

    @Test
    void stripsScriptTags() {
        String html = markdownService.render("<script>alert('xss')</script>");
        assertThat(html).doesNotContain("<script");
    }

    @Test
    void stripsEventHandlers() {
        String html = markdownService.render("<a href=\"#\" onclick=\"alert(1)\">link</a>");
        assertThat(html).doesNotContain("onclick");
        assertThat(html).contains("href=\"#\"");
    }

    @Test
    void preservesSafeLinks() {
        String html = markdownService.render("[GitKoo](https://example.com)");
        assertThat(html).contains("href=\"https://example.com\"");
    }

    @Test
    void rendersNullAsEmpty() {
        assertThat(markdownService.render(null)).isEmpty();
        assertThat(markdownService.render("")).isEmpty();
        assertThat(markdownService.render("   ")).isEmpty();
    }

    @Test
    void rendersCodeBlocks() {
        String html = markdownService.render("```java\nSystem.out.println();\n```");
        assertThat(html).contains("<pre><code");
    }

    @Test
    void sanitizesTableHtml() {
        // Standard CommonMark does not render pipe-tables (needs GFM extension).
        // Raw table HTML is sanitized (tags kept, scripts stripped).
        String html = markdownService.render("<table><tr><td>1</td></tr></table>");
        assertThat(html).contains("<table>");
        assertThat(html).doesNotContain("<script");
    }
}
