package com.furimeo.gitkoo.workflow;

/**
 * Thrown when a workflow (`.koo`) file has a syntax or semantic error (DESIGN.md §117).
 *
 * <p>Carries the source line and column so error messages point the user at the
 * right place:
 * <pre>
 * build.koo:7
 * unknown command 'artifcat'
 * Did you mean: artifact
 * </pre>
 */
public class WorkflowParseException extends RuntimeException {

    private final int line;
    private final int col;

    public WorkflowParseException(int line, int col, String message) {
        super("[" + line + ":" + col + "] " + message);
        this.line = line;
        this.col = col;
    }

    public WorkflowParseException(int line, int col, String message, Throwable cause) {
        super("[" + line + ":" + col + "] " + message, cause);
        this.line = line;
        this.col = col;
    }

    public int sourceLine() { return line; }
    public int sourceCol() { return col; }
}
