package com.furimeo.gitkoo.workflow;

import java.util.Objects;

/**
 * A single token produced by {@link WorkflowLexer} (DESIGN.md §117).
 */
public record Token(TokenType type, String value, int line, int col) {

    public enum TokenType {
        // Keywords
        WORKFLOW, ON, RUN, SHELL, ENV, SECRET, ARTIFACT,
        PARALLEL, IF, ELSE, END, TIMEOUT,
        PUSH, TAG, PULL_REQUEST, MANUAL,

        // Operators / punctuation
        EQ,       // ==
        NEQ,      // !=
        ASSIGN,   // =
        IDENT,    // identifier
        STRING,   // quoted string "..."
        REST,     // rest-of-line (for run commands)
        NEWLINE,
        EOF
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token t)) return false;
        return type == t.type && line == t.line && col == t.col
                && Objects.equals(value, t.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, line, col);
    }

    @Override
    public String toString() {
        return type + "[" + line + ":" + col + "]" + (value != null && !value.isEmpty() ? "(" + value + ")" : "");
    }
}
