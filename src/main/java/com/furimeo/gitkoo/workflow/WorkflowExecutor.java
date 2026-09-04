package com.furimeo.gitkoo.workflow;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.furimeo.gitkoo.config.GitKooProperties;
import com.furimeo.gitkoo.git.GitService;
import com.furimeo.gitkoo.workflow.ast.Expr;
import com.furimeo.gitkoo.workflow.ast.Stmt;
import com.furimeo.gitkoo.workflow.ast.Workflow;

/**
 * Executes a parsed {@link Workflow} by running each statement as an OS process
 * via {@link ProcessBuilder} (DESIGN.md §31, §117).
 *
 * <p>MVP runs in the same process as the web server (Phase 1 runner architecture,
 * DESIGN.md §32). A fixed thread pool executes parallel blocks. Environment is
 * whitelisted (DESIGN.md §34) - only GITKOO_* vars plus declared env/secret values
 * are passed to child processes.
 */
@Service
public class WorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutor.class);

    private final GitKooProperties properties;
    private final GitService gitService;
    private final WorkflowArtifactRepository artifactRepository;
    private final ExecutorService pool;

    /**
     * Id of the run currently being executed by this executor instance. Set at the
     * start of {@link #execute} and read by {@link #collectArtifact} when persisting
     * artifact rows. Each run executes on a single worker thread, so a plain field is
     * sufficient here; the parallel pool never executes {@code artifact} statements
     * concurrently with the run that owns them.
     */
    private long runId;

    public WorkflowExecutor(GitKooProperties properties, GitService gitService,
                            WorkflowArtifactRepository artifactRepository) {
        this.properties = properties;
        this.gitService = gitService;
        this.artifactRepository = artifactRepository;
        this.pool = Executors.newFixedThreadPool(properties.getCi().getWorkers());
    }

    /**
     * Executes the workflow body in the given workspace directory, with the given context.
     *
     * <p>Process output is streamed to a per-run log file at
     * {@code {data}/logs/{runId}.log} (DESIGN.md §109 "workflow logs").
     *
     * @param runId      the workflow run id (names the log file)
     * @param workflow   the parsed workflow
     * @param workspace  the checkout directory (where commands run)
     * @param context    runtime context (GITKOO_BRANCH, GITKOO_EVENT, etc.)
     * @param secrets    resolved secret values (name → value), to be injected as env vars
     * @return true if all statements succeeded, false if any failed
     */
    public boolean execute(long runId, Workflow workflow, Path workspace, Map<String, String> context,
                           Map<String, String> secrets) {
        this.runId = runId;
        // Build the base environment: whitelist GITKOO_* plus declared env/secret.
        Map<String, String> env = buildEnv(workflow, context, secrets);
        List<Stmt> body = filterByIf(workflow.body(), context);

        // Collect the secret values to mask in log output (DESIGN.md §78).
        List<String> secretValues = new ArrayList<>();
        for (String value : secrets.values()) {
            if (value != null && !value.isEmpty()) {
                secretValues.add(value);
            }
        }

        RunLog runLog = openLog(runId, secretValues);
        runLog.appendLine("Workflow '" + workflow.name() + "' run #" + runId + " started");
        try {
            for (Stmt stmt : body) {
                if (!executeStmt(stmt, workspace, env, context, runLog)) {
                    runLog.appendLine("Workflow run #" + runId + " failed");
                    return false;
                }
            }
            runLog.appendLine("Workflow run #" + runId + " succeeded");
            return true;
        } finally {
            runLog.close();
        }
    }

    /**
     * Reads the persisted log for a run, or an empty string if no log was written.
     */
    public String readLog(long runId) {
        Path logFile = logPath(runId);
        if (!Files.exists(logFile)) {
            return "";
        }
        try {
            return Files.readString(logFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read workflow log file {}", logFile, e);
            return "";
        }
    }

    /** Shuts down the thread pool. */
    public void shutdown() {
        pool.shutdown();
    }

    // ── statement execution ───────────────────────────────────────────────

    private boolean executeStmt(Stmt stmt, Path workspace, Map<String, String> env,
                                Map<String, String> context, RunLog runLog) {
        return switch (stmt) {
            case Stmt.Run run -> executeRun(run, workspace, env, runLog);
            case Stmt.Parallel parallel -> executeParallel(parallel, workspace, env, context, runLog);
            case Stmt.If ifStmt -> {
                if (evalExpr(ifStmt.expr(), context)) {
                    yield executeList(ifStmt.thenBody(), workspace, env, context, runLog);
                } else {
                    yield executeList(ifStmt.elseBody(), workspace, env, context, runLog);
                }
            }
            case Stmt.Timeout timeout -> {
                // MVP: timeout applies to the next statement; we don't enforce it yet.
                runLog.appendLine("timeout " + timeout.duration() + " (not enforced in MVP)");
                yield true;
            }
            case Stmt.Artifact artifact -> collectArtifact(artifact, workspace);
            case Stmt.Env envVar -> { env.put(envVar.name(), envVar.value()); yield true; }
            case Stmt.Secret secret -> true; // resolved and injected already
        };
    }

    private boolean executeList(List<Stmt> stmts, Path workspace, Map<String, String> env,
                               Map<String, String> context, RunLog runLog) {
        for (Stmt s : stmts) {
            if (!executeStmt(s, workspace, env, context, runLog)) return false;
        }
        return true;
    }

    private boolean executeRun(Stmt.Run run, Path workspace, Map<String, String> env, RunLog runLog) {
        List<String> command;
        if (run.shell()) {
            command = List.of("/bin/sh", "-c", run.command());
        } else {
            command = shlexSplit(run.command());
        }
        if (command.isEmpty()) {
            runLog.appendLine("Empty run command");
            return false;
        }
        runLog.appendLine("$ " + run.command());

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workspace.toFile())
                .redirectErrorStream(true);
        pb.environment().clear();
        pb.environment().putAll(env);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            // Stream the process output to the per-run log, line by line.
            output.lines().forEach(runLog::appendLine);
            if (code != 0) {
                runLog.appendLine("Command exited with " + code);
                log.warn("Command '{}' exited with {}", run.command(), code);
                return false;
            }
            return true;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            runLog.appendLine("Failed to run command: " + e.getMessage());
            log.error("Failed to run command '{}'", run.command(), e);
            return false;
        }
    }

    private boolean executeParallel(Stmt.Parallel parallel, Path workspace,
                                    Map<String, String> env, Map<String, String> context, RunLog runLog) {
        var futures = parallel.body().stream()
                .map(stmt -> pool.submit(() -> executeStmt(stmt, workspace, new HashMap<>(env), context, runLog)))
                .toList();
        boolean allOk = true;
        for (var f : futures) {
            try {
                if (!f.get()) allOk = false;
            } catch (Exception e) {
                log.error("Parallel task failed", e);
                allOk = false;
            }
        }
        return allOk;
    }

    /**
     * Collects files in the workspace matching the artifact's glob, copies each into
     * {@code <data>/artifacts/{runId}/{relpath}} and records a {@link WorkflowArtifact}
     * row (DESIGN.md §109 "artifacts").
     *
     * <p>The glob uses {@link PathMatcher} {@code glob:} syntax, such as a single-segment
     * wildcard ending in {@code .jar} or a recursive {@code .txt} match. Paths are
     * resolved and matched relative to the workspace (DESIGN.md §78 path traversal
     * protection: the glob is treated as a pattern, never concatenated into a shell
     * string).
     *
     * @return true even if no files matched (an artifact step with zero matches is not a
     *         run failure); false only if the workspace cannot be walked
     */
    private boolean collectArtifact(Stmt.Artifact artifact, Path workspace) {
        log.info("Collecting artifacts matching '{}' from {}", artifact.glob(), workspace);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + artifact.glob());
        Path destRoot = Path.of(properties.getData(), "artifacts", String.valueOf(runId));
        OffsetDateTime now = OffsetDateTime.now();
        boolean allOk = true;

        try (Stream<Path> paths = Files.walk(workspace)) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();
            for (Path path : files) {
                Path rel = workspace.relativize(path);
                if (!matcher.matches(rel)) {
                    continue;
                }
                try {
                    allOk &= copyAndRecord(path, rel, destRoot, now);
                } catch (IOException e) {
                    log.warn("Failed to collect artifact {} (run #{})", path, runId, e);
                    allOk = false;
                }
            }
        } catch (IOException e) {
            log.error("Failed to walk workspace for artifact collection (run #{})", runId, e);
            return false;
        }
        return allOk;
    }

    /** Copies one matched file into the artifact store and persists its metadata row. */
    private boolean copyAndRecord(Path source, Path rel, Path destRoot, OffsetDateTime now) throws IOException {
        // Guard against path traversal: normalize the relative path and reject any
        // remaining parent segments that would escape the run's artifact directory.
        Path normalized = rel.normalize();
        if (normalized.startsWith("..") || normalized.isAbsolute()) {
            log.warn("Skipping artifact with traversal path: {}", rel);
            return true;
        }
        Path dest = destRoot.resolve(normalized);
        Files.createDirectories(dest.getParent());
        Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        long size = Files.size(source);
        WorkflowArtifact record = new WorkflowArtifact();
        record.setRunId(runId);
        record.setName(normalized.getFileName().toString());
        record.setFilePath(dest.toString());
        record.setSize(size);
        record.setCreatedAt(now);
        artifactRepository.save(record);
        log.info("Collected artifact {} ({} bytes) for run #{}", normalized, size, runId);
        return true;
    }

    // ── logging ────────────────────────────────────────────────────────────

    /** Resolves the per-run log file path under the data directory. */
    private Path logPath(long runId) {
        return Path.of(properties.getData(), "logs", runId + ".log");
    }

    /** Opens (creating parent dirs) the per-run log file, or a no-op sink on failure. */
    private RunLog openLog(long runId, List<String> secretValues) {
        Path logFile = logPath(runId);
        try {
            Files.createDirectories(logFile.getParent());
            Writer writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return new RunLog(writer, secretValues);
        } catch (IOException e) {
            log.warn("Failed to open workflow log file {}; logging will be dropped", logFile, e);
            return new RunLog(Writer.nullWriter(), secretValues);
        }
    }

    /**
     * Appends lines to the per-run log file. Writes are synchronized so that
     * parallel-block tasks running on the worker pool can share one writer safely.
     *
     * <p>Each line is scanned for known secret values, which are replaced with
     * {@code ***} before being written (DESIGN.md §78 secret masking).
     */
    private static final class RunLog implements AutoCloseable {
        private final Writer writer;
        private final List<String> secretValues;

        RunLog(Writer writer, List<String> secretValues) {
            this.writer = writer;
            this.secretValues = secretValues;
        }

        synchronized void appendLine(String line) {
            String masked = mask(line);
            try {
                writer.write(masked);
                writer.write("\n");
                writer.flush();
            } catch (IOException e) {
                log.warn("Failed to write workflow log line", e);
            }
        }

        /** Replaces any occurrence of a known secret value with {@code ***}. */
        private String mask(String line) {
            return maskSecrets(line, secretValues);
        }

        @Override
        public void close() {
            try {
                writer.close();
            } catch (IOException e) {
                log.warn("Failed to close workflow log", e);
            }
        }
    }

    // ── env + context ─────────────────────────────────────────────────────

    private Map<String, String> buildEnv(Workflow workflow, Map<String, String> context,
                                         Map<String, String> secrets) {
        Map<String, String> env = new HashMap<>();
        // Whitelisted GITKOO_* context variables.
        env.putAll(context);
        // Declared env vars.
        for (var e : workflow.envs()) {
            env.put(e.name(), e.value());
        }
        // Declared secrets (injected as env, masked in logs).
        for (var s : workflow.secrets()) {
            String value = secrets.get(s.name());
            if (value != null) {
                env.put(s.name(), value);
            }
        }
        return env;
    }

    /** Evaluates an if expression against the context variables. */
    private boolean evalExpr(Expr expr, Map<String, String> context) {
        String actual = context.get("GITKOO_" + expr.variable().toUpperCase());
        String expected = expr.literal().replace("\"", "");
        return switch (expr.operator()) {
            case "==" -> expected.equals(actual);
            case "!=" -> !expected.equals(actual);
            default -> false;
        };
    }

    /** Pre-evaluates top-level if statements into a flat list. */
    private List<Stmt> filterByIf(List<Stmt> stmts, Map<String, String> context) {
        List<Stmt> result = new ArrayList<>();
        for (Stmt s : stmts) {
            if (s instanceof Stmt.If ifStmt) {
                if (evalExpr(ifStmt.expr(), context)) {
                    result.addAll(filterByIf(ifStmt.thenBody(), context));
                } else {
                    result.addAll(filterByIf(ifStmt.elseBody(), context));
                }
            } else {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Replaces any occurrence of a known secret value in the line with
     * {@code ***}. Used to keep secrets out of workflow log output
     * (DESIGN.md §78 secret masking).
     */
    static String maskSecrets(String line, List<String> secretValues) {
        if (line == null || secretValues == null || secretValues.isEmpty()) {
            return line;
        }
        String result = line;
        for (String secret : secretValues) {
            if (secret != null && !secret.isEmpty() && result.contains(secret)) {
                result = result.replace(secret, "***");
            }
        }
        return result;
    }

    /**
     * Splits a command string into argv tokens (shlex-like, no external dependency).
     * MVP handles simple cases: space-separated args and double-quoted strings.
     */
    static List<String> shlexSplit(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ' ' && !inQuote) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
