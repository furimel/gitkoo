package com.furimeo.gitkoo.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.furimeo.gitkoo.workflow.Token.TokenType;
import com.furimeo.gitkoo.workflow.ast.Expr;
import com.furimeo.gitkoo.workflow.ast.Stmt;
import com.furimeo.gitkoo.workflow.ast.Workflow;
import com.furimeo.gitkoo.workflow.ast.Workflow.EnvVar;
import com.furimeo.gitkoo.workflow.ast.Workflow.SecretRef;
import com.furimeo.gitkoo.workflow.ast.Workflow.Trigger;

/**
 * Parses a token stream into a {@link Workflow} AST (DESIGN.md §117).
 *
 * <p>Line-oriented, indentation-insensitive. Newlines separate statements; indentation
 * is cosmetic only (DESIGN.md §117 "no indentation magic").
 */
public class WorkflowParser {

    private final List<Token> tokens;
    private int pos = 0;

    /** Keywords for "Did you mean" suggestions. */
    private static final Set<String> KNOWN_COMMANDS = Set.of(
            "workflow", "on", "run", "shell", "env", "secret", "artifact",
            "parallel", "if", "else", "end", "timeout"
    );

    public WorkflowParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Workflow parse() {
        skipNewlines();
        expect(TokenType.WORKFLOW);
        Token name = expect(TokenType.IDENT);
        skipNewlines();

        List<Trigger> triggers = new ArrayList<>();
        List<EnvVar> envs = new ArrayList<>();
        List<SecretRef> secrets = new ArrayList<>();
        List<Stmt> body = new ArrayList<>();

        while (!check(TokenType.EOF)) {
            if (check(TokenType.ON)) {
                triggers.add(parseTrigger());
            } else if (check(TokenType.ENV)) {
                envs.add(parseEnv());
            } else if (check(TokenType.SECRET)) {
                secrets.add(parseSecret());
            } else if (check(TokenType.NEWLINE)) {
                advance();
            } else {
                body.add(parseStmt());
            }
            skipNewlines();
        }

        if (triggers.isEmpty()) {
            throw new WorkflowParseException(name.line(), name.col(),
                    "Workflow '" + name.value() + "' has no trigger (expected 'on ...')");
        }

        return new Workflow(name.value(), triggers, envs, secrets, body);
    }

    private Trigger parseTrigger() {
        expect(TokenType.ON);
        Token event = switch (peek().type()) {
            case PUSH -> advance();
            case TAG -> advance();
            case PULL_REQUEST -> advance();
            case MANUAL -> advance();
            default -> throw error(peek(), "Expected push/tag/pull_request/manual after 'on'");
        };
        String filter = null;
        if (check(TokenType.IDENT)) {
            filter = advance().value();
        }
        skipNewlines();
        return new Trigger(event.value(), filter);
    }

    private EnvVar parseEnv() {
        expect(TokenType.ENV);
        Token name = expect(TokenType.IDENT);
        expect(TokenType.ASSIGN);
        Token value = switch (peek().type()) {
            case STRING -> advance();
            case IDENT -> advance();
            default -> throw error(peek(), "Expected value after 'env NAME='");
        };
        skipNewlines();
        return new EnvVar(name.value(), value.value());
    }

    private SecretRef parseSecret() {
        expect(TokenType.SECRET);
        Token name = expect(TokenType.IDENT);
        skipNewlines();
        return new SecretRef(name.value());
    }

    private Stmt parseStmt() {
        return switch (peek().type()) {
            case RUN -> parseRun();
            case ENV -> envToStmt(parseEnv());
            case SECRET -> secretToStmt(parseSecret());
            case ARTIFACT -> parseArtifact();
            case PARALLEL -> parseParallel();
            case IF -> parseIf();
            case TIMEOUT -> parseTimeout();
            default -> throw unknownCommand(peek());
        };
    }

    private Stmt parseRun() {
        expect(TokenType.RUN);
        boolean shell = false;
        if (check(TokenType.SHELL)) {
            advance();
            shell = true;
        }
        Token cmd = expect(TokenType.REST);
        skipNewlines();
        return new Stmt.Run(cmd.value(), shell);
    }

    private Stmt parseArtifact() {
        expect(TokenType.ARTIFACT);
        Token glob = switch (peek().type()) {
            case IDENT -> advance();
            case STRING -> advance();
            default -> throw error(peek(), "Expected glob pattern after 'artifact'");
        };
        skipNewlines();
        return new Stmt.Artifact(glob.value());
    }

    private Stmt parseParallel() {
        expect(TokenType.PARALLEL);
        skipNewlines();
        List<Stmt> body = new ArrayList<>();
        while (!check(TokenType.END) && !check(TokenType.EOF)) {
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            body.add(parseStmt());
        }
        expect(TokenType.END);
        skipNewlines();
        return new Stmt.Parallel(body);
    }

    private Stmt parseIf() {
        expect(TokenType.IF);
        Expr expr = parseExpr();
        skipNewlines();
        List<Stmt> thenBody = new ArrayList<>();
        while (!check(TokenType.ELSE) && !check(TokenType.END) && !check(TokenType.EOF)) {
            if (check(TokenType.NEWLINE)) {
                advance();
                continue;
            }
            thenBody.add(parseStmt());
        }
        List<Stmt> elseBody = new ArrayList<>();
        if (check(TokenType.ELSE)) {
            advance();
            skipNewlines();
            while (!check(TokenType.END) && !check(TokenType.EOF)) {
                if (check(TokenType.NEWLINE)) {
                    advance();
                    continue;
                }
                elseBody.add(parseStmt());
            }
        }
        expect(TokenType.END);
        skipNewlines();
        return new Stmt.If(expr, thenBody, elseBody);
    }

    private Stmt parseTimeout() {
        expect(TokenType.TIMEOUT);
        Token duration = switch (peek().type()) {
            case IDENT -> advance();
            default -> throw error(peek(), "Expected duration after 'timeout'");
        };
        skipNewlines();
        return new Stmt.Timeout(duration.value());
    }

    private Expr parseExpr() {
        Token var = expect(TokenType.IDENT);
        Token op = switch (peek().type()) {
            case EQ -> advance();
            case NEQ -> advance();
            default -> throw error(peek(), "Expected == or != in if expression");
        };
        Token lit = switch (peek().type()) {
            case STRING -> advance();
            case IDENT -> advance();
            default -> throw error(peek(), "Expected literal after operator in if expression");
        };
        return new Expr(var.value(), op.value(), lit.value());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Stmt envToStmt(EnvVar e) {
        return new Stmt.Env(e.name(), e.value());
    }

    private Stmt secretToStmt(SecretRef s) {
        return new Stmt.Secret(s.name());
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private Token expect(TokenType type) {
        if (!check(type)) {
            throw error(peek(), "Expected " + type + " but got " + peek().type());
        }
        return advance();
    }

    private void skipNewlines() {
        while (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    private WorkflowParseException error(Token t, String msg) {
        return new WorkflowParseException(t.line(), t.col(), msg);
    }

    private WorkflowParseException unknownCommand(Token t) {
        String value = t.value();
        String suggestion = levenshteinSuggest(value, KNOWN_COMMANDS);
        String msg = "unknown command '" + value + "'";
        if (suggestion != null) {
            msg += "\nDid you mean: " + suggestion;
        }
        return new WorkflowParseException(t.line(), t.col(), msg);
    }

    /** Returns the closest known command name, or null if none is close enough. */
    static String levenshteinSuggest(String input, Set<String> known) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : known) {
            int dist = levenshtein(input, candidate);
            if (dist < bestDist && dist <= Math.max(1, candidate.length() / 3)) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[b.length()];
    }
}
