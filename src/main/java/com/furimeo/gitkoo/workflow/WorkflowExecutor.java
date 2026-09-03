package com.furimeo.gitkoo.workflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
 * whitelisted (DESIGN.md §34) — only GITKOO_* vars plus declared env/secret values
 * are passed to child processes.
 */
@Service
public class WorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutor.class);

    private final GitKooProperties properties;
    private final GitService gitService;
    private final ExecutorService pool;

    public WorkflowExecutor(GitKooProperties properties, GitService gitService) {
        this.properties = properties;
        this.gitService = gitService;
        this.pool = Executors.newFixedThreadPool(properties.getCi().getWorkers());
    }

    /**
     * Executes the workflow body in the given workspace directory, with the given context.
     *
     * @param workflow   the parsed workflow
     * @param workspace  the checkout directory (where commands run)
     * @param context    runtime context (GITKOO_BRANCH, GITKOO_EVENT, etc.)
     * @param secrets    resolved secret values (name → value), to be injected as env vars
     * @return true if all statements succeeded, false if any failed
     */
    public boolean execute(Workflow workflow, Path workspace, Map<String, String> context,
                           Map<String, String> secrets) {
        // Build the base environment: whitelist GITKOO_* plus declared env/secret.
        Map<String, String> env = buildEnv(workflow, context, secrets);
        List<Stmt> body = filterByIf(workflow.body(), context);

        for (Stmt stmt : body) {
            boolean ok = executeStmt(stmt, workspace, env, context);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Shuts down the thread pool. */
    public void shutdown() {
        pool.shutdown();
    }

    // ── statement execution ───────────────────────────────────────────────

    private boolean executeStmt(Stmt stmt, Path workspace, Map<String, String> env,
                                Map<String, String> context) {
        return switch (stmt) {
            case Stmt.Run run -> executeRun(run, workspace, env);
            case Stmt.Parallel parallel -> executeParallel(parallel, workspace, env, context);
            case Stmt.If ifStmt -> {
                if (evalExpr(ifStmt.expr(), context)) {
                    yield executeList(ifStmt.thenBody(), workspace, env, context);
                } else {
                    yield executeList(ifStmt.elseBody(), workspace, env, context);
                }
            }
            case Stmt.Timeout timeout -> {
                // MVP: timeout applies to the next statement; we don't enforce it yet.
                log.debug("timeout {} (not enforced in MVP)", timeout.duration());
                yield true;
            }
            case Stmt.Artifact artifact -> collectArtifact(artifact, workspace);
            case Stmt.Env envVar -> { env.put(envVar.name(), envVar.value()); yield true; }
            case Stmt.Secret secret -> true; // resolved and injected already
        };
    }

    private boolean executeList(List<Stmt> stmts, Path workspace, Map<String, String> env,
                               Map<String, String> context) {
        for (Stmt s : stmts) {
            if (!executeStmt(s, workspace, env, context)) return false;
        }
        return true;
    }

    private boolean executeRun(Stmt.Run run, Path workspace, Map<String, String> env) {
        List<String> command;
        if (run.shell()) {
            command = List.of("/bin/sh", "-c", run.command());
        } else {
            command = shlexSplit(run.command());
        }
        if (command.isEmpty()) {
            log.warn("Empty run command");
            return false;
        }

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workspace.toFile())
                .redirectErrorStream(true);
        pb.environment().clear();
        pb.environment().putAll(env);

        try {
            Process process = pb.start();
            // Log output (MVP: write to stdout; later stream to filesystem log).
            String output = new String(process.getInputStream().readAllBytes());
            int code = process.waitFor();
            if (code != 0) {
                log.warn("Command '{}' exited with {}:\n{}", run.command(), code, output);
                return false;
            }
            log.debug("Command '{}' succeeded:\n{}", run.command(), output);
            return true;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to run command '{}'", run.command(), e);
            return false;
        }
    }

    private boolean executeParallel(Stmt.Parallel parallel, Path workspace,
                                    Map<String, String> env, Map<String, String> context) {
        var futures = parallel.body().stream()
                .map(stmt -> pool.submit(() -> executeStmt(stmt, workspace, new HashMap<>(env), context)))
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

    private boolean collectArtifact(Stmt.Artifact artifact, Path workspace) {
        // MVP: glob matching is simplified; collect matching files.
        log.info("Collecting artifacts matching '{}' from {}", artifact.glob(), workspace);
        // Full glob + storage implementation deferred.
        return true;
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
