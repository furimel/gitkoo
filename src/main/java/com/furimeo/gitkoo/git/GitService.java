package com.furimeo.gitkoo.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.furimeo.gitkoo.config.GitKooProperties;

/**
 * Thin wrapper around the Git CLI via {@link ProcessBuilder} (DESIGN.md §7).
 *
 * <p>Every command uses an argument list \u2014 never a concatenated shell string \u2014 to avoid
 * command injection (DESIGN.md §79). Methods translate between Java types and Git CLI
 * output; they do not implement the Git protocol.
 *
 * @see DESIGN.md §5, §7, §79
 */
@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private final String gitBinary;

    public GitService(GitKooProperties properties) {
        this.gitBinary = properties.getGit().getBinary();
    }

    /** Result of a Git CLI invocation: stdout, stderr (if failed), and exit code. */
    public record GitResult(int exitCode, String stdout, String stderr) {
        public boolean success() {
            return exitCode == 0;
        }
    }

    /**
     * Initializes a bare Git repository at the given path and sets the default branch.
     *
     * <p>The target directory may not exist yet (git init creates it), so the working
     * directory for the process is the parent, and the target path is passed as an argument.
     *
     * @param storagePath absolute path to the {@code {id}.git} directory
     * @param defaultBranch initial branch name (e.g. {@code main})
     */
    public void initBare(Path storagePath, String defaultBranch) {
        Path parent = storagePath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create parent directory " + parent, e);
            }
        }
        run(parent, "init", "--bare", "--initial-branch=" + defaultBranch, storagePath.toString());
        log.info("Initialized bare repository at {}", storagePath);
    }

    // ── tree / file operations ──────────────────────────────────────────

    /**
     * Lists entries in a tree (directory) at a given ref + path.
     *
     * @return parsed tree entries; empty list if the path does not exist
     */
    public List<TreeEntry> listTree(Path storagePath, String ref, String path) {
        String treeish = (path == null || path.isBlank()) ? ref : ref + ":" + path;
        GitResult result = run(storagePath, "ls-tree", treeish);
        if (!result.success()) {
            return List.of();
        }
        List<TreeEntry> entries = new ArrayList<>();
        for (String line : result.stdout().lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            // format: <mode> <type> <sha>\t<name>
            String[] parts = line.split("\t", 2);
            if (parts.length != 2) {
                continue;
            }
            String[] meta = parts[0].split("\\s+", 3);
            if (meta.length != 3) {
                continue;
            }
            entries.add(new TreeEntry(meta[0], meta[1], meta[2], parts[1]));
        }
        return entries;
    }

    /**
     * Returns the raw content of a file at a given ref + path.
     *
     * @return file content, or empty string if not found
     */
    public String catFile(Path storagePath, String ref, String path) {
        GitResult result = run(storagePath, "show", ref + ":" + path);
        if (!result.success()) {
            return "";
        }
        return result.stdout();
    }

    // ── commit history ───────────────────────────────────────────────────

    /**
     * Returns recent commits on a ref, using a stable log format.
     *
     * <p>Format per line: {@code <sha>%<author>%<subject>} separated by a unit separator.
     */
    public List<CommitInfo> log(Path storagePath, String ref, int limit) {
        String sep = "\u001F"; // unit separator, unlikely in commit text
        String format = "%H" + sep + "%an" + sep + "%ae" + sep + "%s" + sep + "%cI";
        GitResult result = run(storagePath, "log", "-" + limit,
                "--pretty=format:" + format, ref);
        if (!result.success()) {
            return List.of();
        }
        List<CommitInfo> commits = new ArrayList<>();
        for (String line : result.stdout().lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(sep, -1);
            if (parts.length < 5) {
                continue;
            }
            commits.add(new CommitInfo(parts[0], parts[1], parts[2], parts[3], parts[4]));
        }
        return commits;
    }

    // ── commit detail & diff ────────────────────────────────────────────

    /**
     * Returns metadata for a single commit (DESIGN.md §13): SHA, author, author date,
     * full message, and parent SHA(s). Uses {@code git show -s} so the diff is suppressed.
     *
     * <p>For a merge commit {@code parent} contains all parent SHAs space-separated; callers
     * that need a single parent (e.g. {@link #diff(Path, String)}) take the first one.
     *
     * @return the commit detail, or {@code null} if the SHA does not resolve
     */
    public CommitDetail showCommit(Path storagePath, String sha) {
        String sep = "\u001F"; // unit separator, unlikely in commit text (see log())
        String format = "%H" + sep + "%an" + sep + "%ae" + sep + "%aI" + sep + "%P" + sep + "%B";
        GitResult result = run(storagePath, "show", "-s", "--format=" + format, sha);
        if (!result.success() || result.stdout().isBlank()) {
            return null;
        }
        // %B is multi-line, so split the whole output by the unit separator (not by line).
        String[] parts = result.stdout().split(sep, -1);
        if (parts.length < 6) {
            return null;
        }
        return new CommitDetail(parts[0], parts[1], parts[2], parts[3],
                parts[4].strip(), parts[5].stripTrailing());
    }

    /**
     * Returns the diff introduced by a single commit (DESIGN.md §13). For a normal commit
     * this is {@code git diff {parent}..{sha}}; for a root commit (no parent) it is
     * {@code git show {sha}} so the initial state is shown.
     */
    public String diff(Path storagePath, String sha) {
        String parent = firstParent(storagePath, sha);
        if (parent.isBlank()) {
            GitResult result = run(storagePath, "show", sha, "--no-color");
            return result.success() ? result.stdout() : "";
        }
        return diff(storagePath, parent, sha);
    }

    /**
     * Returns the diff between two refs (DESIGN.md §15), e.g. a PR's source and target
     * branches: {@code git diff {baseRef} {headRef}}.
     */
    public String diff(Path storagePath, String baseRef, String headRef) {
        GitResult result = run(storagePath, "diff", baseRef, headRef, "--no-color");
        return result.success() ? result.stdout() : "";
    }

    /** Resolves the first parent SHA of a commit, or empty for a root commit. */
    private String firstParent(Path storagePath, String sha) {
        GitResult result = run(storagePath, "show", "-s", "--format=%P", sha);
        if (!result.success()) {
            return "";
        }
        String parents = result.stdout().strip();
        int space = parents.indexOf(' ');
        return space > 0 ? parents.substring(0, space) : parents;
    }

    // ── branches ────────────────────────────────────────────────────────

    /** Lists branch names. */
    public List<String> branches(Path storagePath) {
        GitResult result = run(storagePath, "branch", "--list", "--format=%(refname:short)");
        if (!result.success()) {
            return List.of();
        }
        return result.stdout().lines().filter(s -> !s.isBlank()).toList();
    }

    // ── per-entry history ───────────────────────────────────────────────

    /**
     * The most recent commit touching each direct child of {@code dir}.
     *
     * <p>The obvious implementation is one {@code git log -1} per entry, which is a
     * subprocess per row and turns a 100-file directory into 100 process spawns. This
     * instead walks the history once, newest first, attributing each changed path to
     * the first commit that touched it and stopping as soon as every entry is
     * accounted for.
     *
     * @param names direct children of {@code dir} to resolve, as returned by listTree
     * @param maxCommits how far back to walk before giving up; entries not seen within
     *                   that window are simply absent from the result
     * @return entry name to its latest commit; missing keys mean "not found in range"
     */
    public Map<String, CommitInfo> lastCommits(Path storagePath, String ref, String dir,
                                               Collection<String> names, int maxCommits) {
        if (names == null || names.isEmpty()) {
            return Map.of();
        }
        Set<String> pending = new HashSet<>(names);
        Map<String, CommitInfo> found = new HashMap<>();

        List<String> args = new ArrayList<>(List.of(
                // Keep non-ASCII paths literal so they match the tree entries verbatim.
                "-c", "core.quotepath=false",
                "log",
                // %x01 marks a commit header, %x1f separates its fields. Both are
                // control characters, so neither can occur in a path or a subject.
                "--format=%x01%H%x1f%an%x1f%cI%x1f%s",
                "--name-only",
                // A rename would otherwise report only the new path and hide the entry.
                "--no-renames",
                "-n", String.valueOf(maxCommits),
                ref));
        if (!dir.isEmpty()) {
            args.add("--");
            args.add(dir);
        }

        GitResult result = run(storagePath, args.toArray(new String[0]));
        if (!result.success()) {
            return Map.of();
        }

        CommitInfo current = null;
        for (String line : result.stdout().split("\n")) {
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith(RECORD)) {
                current = parseLogRecord(line.substring(RECORD.length()));
                continue;
            }
            if (current == null) {
                continue;
            }
            String name = childOf(line, dir);
            // First commit to mention it wins, because the walk is newest first.
            if (name != null && pending.remove(name)) {
                found.put(name, current);
                if (pending.isEmpty()) {
                    break;
                }
            }
        }
        return found;
    }

    /** Marker prefixed to each commit header so it is distinguishable from a path. */
    private static final String RECORD = "\001";

    /** Separates the fields within one commit header. */
    private static final String FIELD = "\037";

    private CommitInfo parseLogRecord(String record) {
        String[] parts = record.split(FIELD, -1);
        if (parts.length < 4) {
            return null;
        }
        return new CommitInfo(parts[0], parts[1], "", parts[3], parts[2]);
    }

    /**
     * The direct child of {@code dir} that {@code path} sits under.
     *
     * <p>{@code childOf("src/main/App.java", "src")} is {@code "main"}, so a commit deep
     * inside a directory still dates the directory row.
     */
    private String childOf(String path, String dir) {
        String rest = path;
        if (!dir.isEmpty()) {
            String prefix = dir.endsWith("/") ? dir : dir + "/";
            if (!path.startsWith(prefix)) {
                return null;
            }
            rest = path.substring(prefix.length());
        }
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    // ── merge checks ────────────────────────────────────────────────────

    /**
     * Whether {@code head} merges into {@code base} without conflicts.
     *
     * <p>Uses {@code git merge-tree --write-tree}, which computes the merge in the
     * object database and exits non-zero on conflict, so nothing touches a work tree
     * and no branch is modified.
     *
     * @return true only when Git reports a clean merge; false on conflict, and also
     *         when either ref does not resolve
     */
    public boolean mergesCleanly(Path storagePath, String base, String head) {
        if (resolveRef(storagePath, base).isBlank() || resolveRef(storagePath, head).isBlank()) {
            return false;
        }
        return run(storagePath, "merge-tree", "--write-tree", base, head).success();
    }

    // ── counting ────────────────────────────────────────────────────────

    /** Number of commits reachable from {@code ref}, or 0 when the ref does not resolve. */
    public int commitCount(Path storagePath, String ref) {
        GitResult result = run(storagePath, "rev-list", "--count", ref);
        if (!result.success()) {
            return 0;
        }
        try {
            return Integer.parseInt(result.stdout().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ── tags ────────────────────────────────────────────────────────────

    /** Tags, newest first. */
    public List<TagInfo> tags(Path storagePath, int limit) {
        GitResult result = run(storagePath, "for-each-ref",
                "--sort=-creatordate",
                "--count=" + limit,
                // Same unit separator as log(): a control character cannot
                // appear in a ref name or a commit SHA.
                "--format=%(refname:short)%1f%(objectname)%1f%(creatordate:iso-strict)",
                "refs/tags");
        if (!result.success()) {
            return List.of();
        }
        List<TagInfo> tags = new ArrayList<>();
        for (String line : result.stdout().lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\u001F", -1);
            if (parts.length == 3) {
                tags.add(new TagInfo(parts[0], parts[1], parts[2]));
            }
        }
        return tags;
    }

    // ── contributors ────────────────────────────────────────────────────

    /**
     * Authors by commit count, most prolific first.
     *
     * <p>{@code git shortlog} reads from stdin when it is not given a ref, and this
     * process has no stdin to give it, so it would hang forever. The ref is therefore
     * mandatory, and an unresolvable one returns empty without running the command.
     */
    public List<Contributor> contributors(Path storagePath, String ref, int limit) {
        if (ref == null || ref.isBlank() || resolveRef(storagePath, ref).isEmpty()) {
            return List.of();
        }
        GitResult result = run(storagePath, "shortlog", "-sne", "--no-merges", ref);
        if (!result.success()) {
            return List.of();
        }
        List<Contributor> out = new ArrayList<>();
        for (String line : result.stdout().lines().toList()) {
            // "    42\tAda Lovelace <ada@example.com>"
            String trimmed = line.strip();
            int tab = trimmed.indexOf('\t');
            if (tab < 0) {
                continue;
            }
            int count;
            try {
                count = Integer.parseInt(trimmed.substring(0, tab).strip());
            } catch (NumberFormatException e) {
                continue;
            }
            String rest = trimmed.substring(tab + 1).strip();
            String name = rest;
            String email = "";
            int lt = rest.lastIndexOf('<');
            int gt = rest.lastIndexOf('>');
            if (lt > 0 && gt > lt) {
                name = rest.substring(0, lt).strip();
                email = rest.substring(lt + 1, gt);
            }
            out.add(new Contributor(name, email, count));
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    // ── size and language breakdown ─────────────────────────────────────

    /**
     * On-disk size of the object database, in bytes.
     *
     * <p>Uses {@code -v} rather than {@code -vH}: the human-readable form returns
     * strings like "1.20 MiB" that would only have to be parsed back into a number.
     */
    public long sizeBytes(Path storagePath) {
        GitResult result = run(storagePath, "count-objects", "-v");
        if (!result.success()) {
            return 0L;
        }
        long total = 0L;
        for (String line : result.stdout().lines().toList()) {
            // size and size-pack are both reported in KiB.
            if (line.startsWith("size:") || line.startsWith("size-pack:")) {
                try {
                    total += Long.parseLong(line.substring(line.indexOf(':') + 1).strip()) * 1024L;
                } catch (NumberFormatException e) {
                    // A malformed line is not worth failing the page over.
                }
            }
        }
        return total;
    }

    /**
     * Bytes of source per language at {@code ref}, largest first.
     *
     * <p>Files whose extension maps to no known language are left out entirely rather
     * than lumped into an "Other" bucket, which on most repositories would dwarf every
     * real language while saying nothing.
     *
     * <p>This walks the whole tree, so callers must cache the result against the head
     * SHA rather than calling it once per request.
     */
    public Map<String, Long> languageBytes(Path storagePath, String ref) {
        if (ref == null || ref.isBlank()) {
            return Map.of();
        }
        GitResult result = run(storagePath, "-c", "core.quotepath=false",
                "ls-tree", "-r", "-l", "--full-name", ref);
        if (!result.success()) {
            return Map.of();
        }
        Map<String, Long> bytes = new HashMap<>();
        for (String line : result.stdout().lines().toList()) {
            // "100644 blob <sha> <size>\t<path>"
            int tab = line.indexOf('\t');
            if (tab < 0) {
                continue;
            }
            String path = line.substring(tab + 1);
            String[] meta = line.substring(0, tab).strip().split("\\s+");
            if (meta.length < 4 || !"blob".equals(meta[1])) {
                continue;
            }
            long size;
            try {
                size = Long.parseLong(meta[3]);
            } catch (NumberFormatException e) {
                continue; // "-" is reported for submodules
            }
            String language = com.furimeo.gitkoo.web.Languages.displayName(path);
            if (language != null) {
                bytes.merge(language, size, Long::sum);
            }
        }
        return bytes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, java.util.LinkedHashMap::new));
    }

    // ── licence ─────────────────────────────────────────────────────────

    private static final List<String> LICENSE_FILES = List.of(
            "LICENSE", "LICENSE.md", "LICENSE.txt", "COPYING", "COPYING.md");

    /**
     * Best-effort licence name from the repository's LICENSE file.
     *
     * <p>Matches the title line of the handful of licences that cover almost every
     * repository. Anything else reports the generic "License": naming a licence
     * wrongly is worse than not naming it.
     *
     * @return the licence name, or null when the repository has no licence file
     */
    public String licenseName(Path storagePath, String ref) {
        for (String candidate : LICENSE_FILES) {
            String text = catFile(storagePath, ref, candidate);
            if (text == null || text.isBlank()) {
                continue;
            }
            String head = text.length() > 2000 ? text.substring(0, 2000) : text;
            String upper = head.toUpperCase(java.util.Locale.ROOT);
            if (upper.contains("MIT LICENSE")) {
                return "MIT";
            }
            if (upper.contains("APACHE LICENSE")) {
                return "Apache-2.0";
            }
            if (upper.contains("GNU AFFERO GENERAL PUBLIC LICENSE")) {
                return "AGPL-3.0";
            }
            if (upper.contains("GNU LESSER GENERAL PUBLIC LICENSE")) {
                return "LGPL-3.0";
            }
            if (upper.contains("GNU GENERAL PUBLIC LICENSE")) {
                return "GPL-3.0";
            }
            if (upper.contains("MOZILLA PUBLIC LICENSE")) {
                return "MPL-2.0";
            }
            if (upper.contains("BSD 3-CLAUSE") || upper.contains("BSD 2-CLAUSE")) {
                return "BSD";
            }
            if (upper.contains("THE UNLICENSE")) {
                return "Unlicense";
            }
            return "License";
        }
        return null;
    }

    // ── ref resolution ──────────────────────────────────────────────────

    /** Resolves a ref (branch/tag/HEAD) to a commit SHA, or empty if it does not resolve. */
    public String resolveRef(Path storagePath, String ref) {
        GitResult result = run(storagePath, "rev-parse", "--verify", ref);
        if (!result.success()) {
            return "";
        }
        return result.stdout().trim();
    }

    // ── low-level command runner ─────────────────────────────────────────

    /**
     * Runs a Git command with the given arguments, inside the repository directory.
     */
    public GitResult run(Path storagePath, String... args) {
        List<String> command = new ArrayList<>();
        command.add(gitBinary);
        command.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(storagePath.toFile())
                .redirectErrorStream(false);
        try {
            Process process = pb.start();
            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());
            int code = process.waitFor();
            if (code != 0) {
                log.debug("git {} exited {} in {}: {}", String.join(" ", args), code, storagePath,
                        stderr.trim());
            }
            return new GitResult(code, stdout, stderr);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to run git " + String.join(" ", args), e);
        }
    }

    /** A tag: its short name, the object it points at, and when it was created. */
    public record TagInfo(String name, String sha, String createdAtIso) {
    }

    /** An author and how many commits they landed. */
    public record Contributor(String name, String email, int commits) {
    }

    /** A parsed entry from {@code git ls-tree}. */
    public record TreeEntry(String mode, String type, String sha, String name) {
        public boolean isDirectory() {
            return "tree".equals(type);
        }

        public boolean isBlob() {
            return "blob".equals(type);
        }
    }

    /** A parsed commit from {@code git log}. */
    public record CommitInfo(String sha, String authorName, String authorEmail, String subject,
                              String committerDateIso) {
    }

    /** Full metadata for a single commit (DESIGN.md §13). {@code parent} is space-separated
     *  for merge commits (empty for a root commit). */
    public record CommitDetail(String sha, String authorName, String authorEmail,
                               String authorDateIso, String parent, String message) {
    }
}
