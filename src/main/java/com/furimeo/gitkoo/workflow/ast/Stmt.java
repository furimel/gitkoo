package com.furimeo.gitkoo.workflow.ast;

import java.util.List;

/**
 * A statement in a workflow body (DESIGN.md §117).
 *
 * <p>Sealed interface so the AST is exhaustive over valid statement types.
 */
public sealed interface Stmt
        permits Stmt.Run, Stmt.Env, Stmt.Secret, Stmt.Artifact, Stmt.Parallel, Stmt.If, Stmt.Timeout {

    /** {@code run <cmd>} — execute a command. {@code shell} enables shell mode. */
    record Run(String command, boolean shell) implements Stmt {}

    /** {@code env NAME=value} — set an environment variable. */
    record Env(String name, String value) implements Stmt {}

    /** {@code secret NAME} — reference a secret. */
    record Secret(String name) implements Stmt {}

    /** {@code artifact <glob>} — collect artifacts matching the glob. */
    record Artifact(String glob) implements Stmt {}

    /** {@code parallel ... end} — run children concurrently. */
    record Parallel(List<Stmt> body) implements Stmt {}

    /** {@code if <expr> ... [else ...] end} — conditional block. */
    record If(Expr expr, List<Stmt> thenBody, List<Stmt> elseBody) implements Stmt {}

    /** {@code timeout <duration>} — apply a timeout to the next run or block. */
    record Timeout(String duration) implements Stmt {}
}
