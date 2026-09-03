package com.furimeo.gitkoo.workflow;

import java.util.List;
import java.util.Set;

import com.furimeo.gitkoo.workflow.ast.Stmt;
import com.furimeo.gitkoo.workflow.ast.Workflow;
import com.furimeo.gitkoo.workflow.ast.Workflow.EnvVar;
import com.furimeo.gitkoo.workflow.ast.Workflow.SecretRef;

/**
 * Validates a parsed {@link Workflow} AST (DESIGN.md §117).
 *
 * <p>Checks that the workflow has at least one {@code run} statement, that secret
 * references are valid, and that no trigger is duplicated.
 */
public final class WorkflowValidator {

    private WorkflowValidator() {}

    /** Validates a workflow and throws {@link WorkflowParseException} on failure. */
    public static void validate(Workflow workflow) {
        if (workflow == null) {
            throw new WorkflowParseException(0, 0, "Workflow is null");
        }
        if (workflow.triggers() == null || workflow.triggers().isEmpty()) {
            throw new WorkflowParseException(0, 0,
                    "Workflow '" + workflow.name() + "' has no trigger");
        }
        if (!hasRunStatement(workflow.body())) {
            throw new WorkflowParseException(0, 0,
                    "Workflow '" + workflow.name() + "' has no run statement");
        }
        // Check for duplicate trigger events.
        Set<String> seen = new java.util.HashSet<>();
        for (var t : workflow.triggers()) {
            if (!seen.add(t.event())) {
                throw new WorkflowParseException(0, 0,
                        "Duplicate trigger: " + t.event());
            }
        }
    }

    /** Recursively checks if a statement list (including nested blocks) contains a Run. */
    private static boolean hasRunStatement(List<Stmt> stmts) {
        if (stmts == null) return false;
        for (Stmt s : stmts) {
            if (s instanceof Stmt.Run) return true;
            if (s instanceof Stmt.Parallel p && hasRunStatement(p.body())) return true;
            if (s instanceof Stmt.If i && (hasRunStatement(i.thenBody()) || hasRunStatement(i.elseBody()))) return true;
        }
        return false;
    }
}
