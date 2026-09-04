package com.furimeo.gitkoo.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * Structural rules for the Thymeleaf templates.
 *
 * <p>These live in JUnit rather than in {@code tools/check-css-classes.sh} because CI
 * runs only {@code ./gradlew test} and {@code ./gradlew bootJar} - the shell script
 * never executed anywhere except a developer machine, so the guard the project
 * believed it had was not a guard at all.
 *
 * <p>Several rules are <em>ratchets</em>: the codebase currently violates them, and
 * the test pins the violation count so it can fall but never rise. Each phase of the
 * redesign lowers a budget to zero and the rule becomes absolute. A ratchet is
 * honest about existing debt while making new debt impossible.
 */
class TemplateConventionsTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path CSS = Path.of("src/main/resources/static/css");

    /** Attached by JavaScript or produced by Thymeleaf, so they never need a rule. */
    private static final Set<String> CLASS_ALLOWLIST = Set.of("theme-toggle");

    /**
     * The only literal style attribute allowed: the icon sprite must not render, and
     * hiding it via a stylesheet would leave a flash of 40 raw symbols before the CSS
     * loads.
     */
    private static final String SPRITE = "fragments/icons.html";

    // ── Rule 1: every class used is defined (absolute) ────────────────────

    @Test
    void everyClassUsedIsDefinedInAStylesheet() throws IOException {
        Set<String> defined = definedClasses();
        List<String> missing = new ArrayList<>();

        for (Path template : templates()) {
            String source = Files.readString(template);
            for (String cls : classesUsedIn(source)) {
                if (!defined.contains(cls) && !CLASS_ALLOWLIST.contains(cls)) {
                    missing.add(rel(template) + ": " + cls);
                }
            }
        }
        assertThat(missing)
                .as("classes used in a template but defined in no stylesheet")
                .isEmpty();
    }

    // ── Rule 2: no literal pixel widths in markup (ratchet) ───────────────

    /**
     * Hardcoded widths scattered through the markup were a direct cause of the
     * mismatched layout: twenty different magic numbers, none of them adjacent to
     * each other or to the stylesheet.
     *
     * <p>A {@code th:style} carrying {@code $&#123;...&#125;} is fine - a label colour or a
     * progress width genuinely comes from data.
     */
    @Test
    void literalStyleAttributesDoNotIncrease() throws IOException {
        List<String> offenders = new ArrayList<>();
        Pattern literalStyle = Pattern.compile("(?<!th:)\\bstyle\\s*=\\s*\"([^\"]*)\"");

        for (Path template : templates()) {
            if (rel(template).equals(SPRITE)) {
                continue;
            }
            Matcher m = literalStyle.matcher(Files.readString(template));
            while (m.find()) {
                offenders.add(rel(template) + ": style=\"" + m.group(1) + "\"");
            }
        }
        assertThat(offenders)
                .as("literal style attributes (budget only ever falls):\n%s",
                        String.join("\n", offenders))
                .hasSizeLessThanOrEqualTo(17);
    }

    // ── Rule 3: containers live in one place (ratchet) ────────────────────

    /**
     * {@code container-xl px-3 px-md-4 px-lg-5} is repeated in almost every page.
     * Once the layout fragment owns it, no page should name a container at all.
     */
    @Test
    void containerClassesDoNotIncrease() throws IOException {
        List<String> offenders = new ArrayList<>();
        Pattern container = Pattern.compile("\\bcontainer-(sm|md|lg|xl)\\b");

        for (Path template : templates()) {
            Matcher m = container.matcher(Files.readString(template));
            while (m.find()) {
                offenders.add(rel(template) + ": " + m.group());
            }
        }
        assertThat(offenders)
                .as("pages naming their own container (budget only ever falls)")
                .hasSizeLessThanOrEqualTo(30);
    }

    // ── Rule 4: no stacked cards (ratchet) ────────────────────────────────

    /**
     * Two sibling cards is a smell and three is a bug: the pull request page stacks
     * five, which reads as a ladder of identical rectangles rather than a page.
     */
    @Test
    void siblingBoxesDoNotIncrease() throws IOException {
        List<String> offenders = new ArrayList<>();

        for (Path template : templates()) {
            Document doc = Jsoup.parse(Files.readString(template));
            for (Element parent : doc.select(":has(> .Box)")) {
                int boxes = parent.children().stream()
                        .filter(child -> child.hasClass("Box"))
                        .toList().size();
                if (boxes > 1) {
                    offenders.add(rel(template) + ": " + boxes + " sibling Box elements under <"
                            + parent.tagName() + ">");
                }
            }
        }
        assertThat(offenders)
                .as("stacked sibling cards (budget only ever falls):\n%s",
                        String.join("\n", offenders))
                .hasSizeLessThanOrEqualTo(7);
    }

    // ── Rule 5: icons are hidden from assistive technology (absolute) ─────

    @Test
    void everyOcticonIsHiddenFromAssistiveTechnology() throws IOException {
        List<String> offenders = new ArrayList<>();

        for (Path template : templates()) {
            if (rel(template).equals(SPRITE)) {
                continue;
            }
            Document doc = Jsoup.parse(Files.readString(template));
            for (Element svg : doc.select("svg.octicon")) {
                if (!svg.hasAttr("aria-hidden")) {
                    offenders.add(rel(template) + ": " + svg.outerHtml().lines().findFirst().orElse(""));
                }
            }
        }
        assertThat(offenders)
                .as("decorative icons must carry aria-hidden")
                .isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private List<Path> templates() throws IOException {
        try (Stream<Path> walk = Files.walk(TEMPLATES)) {
            return walk.filter(p -> p.toString().endsWith(".html")).sorted().toList();
        }
    }

    private String rel(Path template) {
        return TEMPLATES.relativize(template).toString().replace('\\', '/');
    }

    /** Every class selector declared across the stylesheets. */
    private Set<String> definedClasses() throws IOException {
        Set<String> defined = new LinkedHashSet<>();
        Pattern selector = Pattern.compile("\\.([A-Za-z][A-Za-z0-9_-]*)");
        try (Stream<Path> walk = Files.walk(CSS)) {
            for (Path sheet : walk.filter(p -> p.toString().endsWith(".css")).toList()) {
                Matcher m = selector.matcher(Files.readString(sheet));
                while (m.find()) {
                    defined.add(m.group(1));
                }
            }
        }
        return defined;
    }

    /**
     * Classes named by a template: literal {@code class="..."} plus the branches of a
     * {@code th:classappend} ternary.
     *
     * <p>Every {@code $&#123;...&#125;} is stripped before the quoted branches are read.
     * Without that, a nested ternary such as
     * {@code $&#123;s == 'OPEN'&#125; ? 'a' : ($&#123;s == 'MERGED'&#125; ? 'b' : 'c')}
     * reports {@code MERGED} as a missing class, when it is a value being compared.
     * Stripping the expressions also makes nesting depth irrelevant.
     */
    private Set<String> classesUsedIn(String source) {
        Set<String> used = new LinkedHashSet<>();

        Matcher literal = Pattern.compile("(?<!th:)\\bclass\\s*=\\s*\"([^\"$]*)\"").matcher(source);
        while (literal.find()) {
            addTokens(used, literal.group(1));
        }

        // DOTALL: a classappend expression is routinely wrapped across lines.
        Matcher append = Pattern.compile("th:classappend\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL)
                .matcher(source);
        while (append.find()) {
            String branches = append.group(1).replaceAll("\\$\\{[^}]*\\}", "");
            Matcher quoted = Pattern.compile("'([^']*)'").matcher(branches);
            while (quoted.find()) {
                addTokens(used, quoted.group(1));
            }
        }
        return used;
    }

    private void addTokens(Set<String> into, String value) {
        for (String token : value.trim().split("\\s+")) {
            if (token.matches("[A-Za-z][A-Za-z0-9_-]*")) {
                into.add(token);
            }
        }
    }
}
