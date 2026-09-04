package com.furimeo.gitkoo.git;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Turns unified {@code git diff} output into per-file, per-line structures the
 * templates can render as a table (docs/ui.md).
 *
 * <p>The raw patch was previously dumped into a {@code <pre>}, which gave no
 * line numbers, no add/remove colouring and no per-file headers. Parsing it here
 * keeps that presentation logic out of the view.
 *
 * <p>This handles the textual unified format only. Binary files are reported as a
 * file entry with no lines, which the view renders as "Binary file not shown".
 */
@Component
public class DiffParser {

    /** One line of a diff, tagged with the kind of change and its two line numbers. */
    public record Line(String type, Integer oldNumber, Integer newNumber, String content) {

        /** True for the {@code @@ ... @@} hunk headers. */
        public boolean isHunk() {
            return "hunk".equals(type);
        }

        public boolean isAddition() {
            return "add".equals(type);
        }

        public boolean isDeletion() {
            return "del".equals(type);
        }
    }

    /**
     * A single file's worth of diff, with counts for the "+N -M" summary.
     *
     * @param language highlight.js language id for this path, or null for plain text
     */
    public record FileDiff(String path, boolean binary, int additions, int deletions,
                           String language, List<Line> lines) {
    }

    /**
     * Parses a unified diff.
     *
     * @param diff raw output of {@code git diff}; may be null or blank
     * @return one entry per changed file, empty when there is nothing to show
     */
    public List<FileDiff> parse(String diff) {
        List<FileDiff> files = new ArrayList<>();
        if (diff == null || diff.isBlank()) {
            return files;
        }

        String path = null;
        boolean binary = false;
        List<Line> lines = new ArrayList<>();
        int oldNumber = 0;
        int newNumber = 0;

        for (String line : diff.split("\n", -1)) {
            if (line.startsWith("diff --git ")) {
                flush(files, path, binary, lines);
                path = pathFromHeader(line);
                binary = false;
                lines = new ArrayList<>();
                continue;
            }
            if (path == null) {
                // Preamble before the first file header; nothing to attribute it to.
                continue;
            }
            if (line.startsWith("Binary files ") || line.startsWith("GIT binary patch")) {
                binary = true;
                continue;
            }
            // Metadata lines carry no content worth showing.
            if (line.startsWith("index ") || line.startsWith("--- ") || line.startsWith("+++ ")
                    || line.startsWith("new file mode") || line.startsWith("deleted file mode")
                    || line.startsWith("old mode") || line.startsWith("new mode")
                    || line.startsWith("similarity index") || line.startsWith("rename ")) {
                continue;
            }
            if (line.startsWith("@@")) {
                int[] starts = hunkStarts(line);
                oldNumber = starts[0];
                newNumber = starts[1];
                lines.add(new Line("hunk", null, null, line));
                continue;
            }
            if (line.startsWith("+")) {
                lines.add(new Line("add", null, newNumber++, line.substring(1)));
            } else if (line.startsWith("-")) {
                lines.add(new Line("del", oldNumber++, null, line.substring(1)));
            } else if (line.startsWith(" ")) {
                lines.add(new Line("context", oldNumber++, newNumber++, line.substring(1)));
            } else if (line.startsWith("\\")) {
                // "\ No newline at end of file" - shown as context, belongs to neither side.
                lines.add(new Line("context", null, null, line));
            }
        }
        flush(files, path, binary, lines);
        return files;
    }

    /** Total additions and deletions across every file, for the PR summary line. */
    public int[] totals(List<FileDiff> files) {
        int additions = 0;
        int deletions = 0;
        for (FileDiff file : files) {
            additions += file.additions();
            deletions += file.deletions();
        }
        return new int[] {additions, deletions};
    }

    private void flush(List<FileDiff> files, String path, boolean binary, List<Line> lines) {
        if (path == null) {
            return;
        }
        int additions = 0;
        int deletions = 0;
        for (Line line : lines) {
            if (line.isAddition()) {
                additions++;
            } else if (line.isDeletion()) {
                deletions++;
            }
        }
        files.add(new FileDiff(path, binary, additions, deletions,
                com.furimeo.gitkoo.web.Languages.forPath(path), List.copyOf(lines)));
    }

    /** Pulls the b-side path out of {@code diff --git a/foo b/foo}. */
    private String pathFromHeader(String header) {
        int bIndex = header.lastIndexOf(" b/");
        if (bIndex >= 0) {
            return header.substring(bIndex + 3);
        }
        int aIndex = header.indexOf(" a/");
        return aIndex >= 0 ? header.substring(aIndex + 3) : header;
    }

    /** First old and new line numbers from a {@code @@ -a,b +c,d @@} header. */
    private int[] hunkStarts(String header) {
        int[] starts = {1, 1};
        try {
            int at = header.indexOf("@@", 2);
            String ranges = header.substring(2, at < 0 ? header.length() : at).trim();
            for (String part : ranges.split(" ")) {
                if (part.length() < 2) {
                    continue;
                }
                String digits = part.substring(1).split(",")[0];
                int value = Integer.parseInt(digits);
                if (part.charAt(0) == '-') {
                    starts[0] = value;
                } else if (part.charAt(0) == '+') {
                    starts[1] = value;
                }
            }
        } catch (RuntimeException e) {
            // A malformed header should not break the whole page; fall back to 1.
            return new int[] {1, 1};
        }
        return starts;
    }
}
