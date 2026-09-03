package com.furimeo.gitkoo.workflow.ast;

/**
 * A comparison expression in an {@code if} block (DESIGN.md §117).
 *
 * <p>MVP supports only {@code ==} and {@code !=} on context variables
 * (branch, event, tag, ref).
 */
public record Expr(String variable, String operator, String literal) {}
