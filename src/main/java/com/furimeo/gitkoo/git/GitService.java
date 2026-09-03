package com.furimeo.gitkoo.git;

import java.io.IOException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // ── branches ────────────────────────────────────────────────────────

    /** Lists branch names. */
    public List<String> branches(Path storagePath) {
        GitResult result = run(storagePath, "branch", "--list", "--format=%(refname:short)");
        if (!result.success()) {
            return List.of();
        }
        return result.stdout().lines().filter(s -> !s.isBlank()).toList();
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
    GitResult run(Path storagePath, String... args) {
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
}
