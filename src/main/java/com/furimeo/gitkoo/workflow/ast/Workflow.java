package com.furimeo.gitkoo.workflow.ast;

import java.util.List;

/**
 * Root AST node for a parsed GitKoo workflow file (DESIGN.md §117).
 *
 * @param name      workflow name (after "workflow" keyword)
 * @param triggers  list of trigger declarations (push/tag/pull_request/manual)
 * @param envs      environment variables declared at the top level
 * @param secrets   secret references declared at the top level
 * @param body      list of statements forming the workflow body
 */
public record Workflow(
        String name,
        List<Trigger> triggers,
        List<EnvVar> envs,
        List<SecretRef> secrets,
        List<Stmt> body
) {

    /** A trigger declaration, e.g. {@code on push main}. */
    public record Trigger(String event, String filter) {
        public Trigger {
            if (event == null) throw new IllegalArgumentException("trigger event is null");
        }
    }

    /** An environment variable: {@code env NAME=value}. */
    public record EnvVar(String name, String value) {}

    /** A secret reference: {@code secret NAME}. */
    public record SecretRef(String name) {}
}
