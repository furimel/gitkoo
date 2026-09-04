package com.furimeo.gitkoo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * The packaged stylesheet must still contain every rule the source declares.
 *
 * <p>{@code minifyAssets} runs the YUI compressor, which is a regular expression and
 * not a parser. If it ever meets syntax it cannot follow, the failure mode is silent:
 * the source is right, the development server is right, and only the jar is wrong.
 * There is no symptom to notice, so it needs a test rather than vigilance.
 *
 * <p>Probed with CSS nesting, {@code @layer}, {@code color-mix()} and {@code calc()}
 * and found none of them mangled by version 2.4.8, so this is a guard against a
 * regression rather than a workaround for a known break.
 */
class MinifiedAssetsTest {

    private static final Path SOURCE = Path.of("src/main/resources/static/css/gitkoo.css");
    private static final Path PACKAGED = Path.of("build/resources/main/static/css/gitkoo.css");

    @Test
    void everySourceSelectorSurvivesMinification() throws IOException {
        // The packaged copy only exists after processResources; a bare `test` run in a
        // clean tree has nothing to compare against, and failing there would be noise.
        assumeTrue(Files.exists(PACKAGED), "run after processResources");

        String source = stripComments(Files.readString(SOURCE));
        String packaged = Files.readString(PACKAGED);

        assertThat(packaged.length())
                .as("the packaged copy should be smaller than the source, i.e. actually minified")
                .isLessThan(source.length());

        List<String> missing = new ArrayList<>();
        for (String selector : classSelectors(source)) {
            if (!packaged.contains(selector)) {
                missing.add(selector);
            }
        }
        assertThat(missing)
                .as("class selectors the minifier dropped:\n%s", String.join("\n", missing))
                .isEmpty();

        // A selector surviving is not the same as its rule surviving: a compressor
        // that loses a closing brace leaves the selector in place and swallows every
        // rule after it into that block. Unbalanced braces is the fingerprint.
        assertThat(braceBalance(packaged))
                .as("unbalanced braces in the packaged stylesheet: the compressor "
                        + "truncated a rule, so everything after it now sits inside "
                        + "that rule's block")
                .isZero();
    }

    private static int braceBalance(String css) {
        int depth = 0;
        for (int i = 0; i < css.length(); i++) {
            char c = css.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return depth;
    }

    /** Bare single-class selectors, which are the ones a dropped rule would take with it. */
    private static Set<String> classSelectors(String css) {
        Set<String> found = new LinkedHashSet<>();
        Matcher blocks = Pattern.compile("([^{}]+)\\{").matcher(css);
        while (blocks.find()) {
            for (String part : blocks.group(1).split(",")) {
                String selector = part.strip();
                if (selector.startsWith(".") && !selector.contains(" ") && !selector.contains(":")) {
                    found.add(selector);
                }
            }
        }
        return found;
    }

    private static String stripComments(String css) {
        return css.replaceAll("(?s)/\\*.*?\\*/", "");
    }
}
