package com.furimeo.gitkoo.issue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects issue references (#42) and auto-close keywords in text (DESIGN.md §20).
 *
 * <p>Scans commit messages, PR descriptions, and comments for patterns like
 * {@code #42}, {@code fixes #42}, {@code closes #42}, {@code resolves #42}
 * to link issues and optionally auto-close them.
 *
 * @see DESIGN.md §20
 */
public final class IssueLinker {

    /** Matches #NN references. */
    private static final Pattern REF_PATTERN = Pattern.compile("#(\\d+)");

    /** Matches auto-close keywords followed by #NN: fixes/closes/resolves #42. */
    private static final Pattern CLOSE_PATTERN = Pattern.compile(
            "(?i)\\b(fix(?:e[sd])?|close[sd]?|resolve[sd]?)\\s+#(\\d+)");

    private IssueLinker() {}

    /** Returns all issue numbers referenced by {@code #NN} in the text. */
    public static List<Integer> findReferences(String text) {
        List<Integer> numbers = new ArrayList<>();
        if (text == null) return numbers;
        Matcher m = REF_PATTERN.matcher(text);
        while (m.find()) {
            numbers.add(Integer.parseInt(m.group(1)));
        }
        return numbers;
    }

    /** Returns issue numbers that should be auto-closed (fixes/closes/resolves #NN). */
    public static List<Integer> findAutoClose(String text) {
        List<Integer> numbers = new ArrayList<>();
        if (text == null) return numbers;
        Matcher m = CLOSE_PATTERN.matcher(text);
        while (m.find()) {
            numbers.add(Integer.parseInt(m.group(2)));
        }
        return numbers;
    }
}
