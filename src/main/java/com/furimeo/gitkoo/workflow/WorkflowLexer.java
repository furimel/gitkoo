package com.furimeo.gitkoo.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.furimeo.gitkoo.workflow.Token.TokenType;

/**
 * Tokenizes a GitKoo workflow (`.koo`) source file (DESIGN.md §117).
 *
 * <p>Line-oriented: newline is a token. Comments start with {@code #} and run to
 * end-of-line. Keywords are matched case-sensitively. Strings are double-quoted.
 * After the {@code run} keyword (optionally followed by {@code shell}), the rest of
 * the line is captured as a single {@link TokenType#REST} token.
 */
public class WorkflowLexer {

    private static final Set<String> KEYWORDS = Set.of(
            "workflow", "on", "run", "shell", "env", "secret", "artifact",
            "parallel", "if", "else", "end", "timeout",
            "push", "tag", "pull_request", "manual"
    );

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    public WorkflowLexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < source.length()) {
            char c = source.charAt(pos);

            // Skip whitespace (but not newlines).
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
                continue;
            }

            // Comment: skip to end of line.
            if (c == '#') {
                while (pos < source.length() && source.charAt(pos) != '\n') {
                    advance();
                }
                continue;
            }

            // Newline.
            if (c == '\n') {
                tokens.add(new Token(TokenType.NEWLINE, "\\n", line, col));
                advance();
                continue;
            }

            // String literal.
            if (c == '"') {
                tokens.add(readString());
                continue;
            }

            // Operators.
            if (c == '=' && peek(1) == '=') {
                tokens.add(new Token(TokenType.EQ, "==", line, col));
                advance();
                advance();
                continue;
            }
            if (c == '!' && peek(1) == '=') {
                tokens.add(new Token(TokenType.NEQ, "!=", line, col));
                advance();
                advance();
                continue;
            }
            if (c == '=') {
                tokens.add(new Token(TokenType.ASSIGN, "=", line, col));
                advance();
                continue;
            }

            // Identifier, keyword, or rest-of-line (after run).
            if (Character.isLetter(c) || c == '_' || c == '.' || c == '/' || c == '*' || c == '-') {
                // Check if this is after "run" - if so, capture the rest of line as REST.
                Token prev = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
                boolean afterRun = prev != null && (prev.type() == TokenType.RUN || prev.type() == TokenType.SHELL);
                if (afterRun) {
                    // Check if it's the "shell" keyword immediately after "run".
                    if (prev.type() == TokenType.RUN && lookAheadIsShell()) {
                        int shellLine = line, shellCol = col;
                        advance(); // s
                        advance(); // h
                        advance(); // e
                        advance(); // l
                        advance(); // l
                        tokens.add(new Token(TokenType.SHELL, "shell", shellLine, shellCol));
                        skipSpaces();
                        // Then capture rest as REST.
                        tokens.add(readRest());
                        continue;
                    }
                    tokens.add(readRest());
                    continue;
                }
                tokens.add(readIdentifierOrKeyword());
                continue;
            }

            // Numbers (for timeout durations like 30m) - treat as IDENT.
            if (Character.isDigit(c)) {
                tokens.add(readNumber());
                continue;
            }

            throw new WorkflowParseException(line, col, "Unexpected character: " + c);
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    private Token readString() {
        int startLine = line, startCol = col;
        advance(); // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && source.charAt(pos) != '"') {
            if (source.charAt(pos) == '\\' && pos + 1 < source.length()) {
                advance();
                char esc = source.charAt(pos);
                sb.append(switch (esc) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> esc;
                });
                advance();
            } else {
                sb.append(source.charAt(pos));
                advance();
            }
        }
        if (pos >= source.length()) {
            throw new WorkflowParseException(startLine, startCol, "Unterminated string");
        }
        advance(); // skip closing quote
        return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
    }

    private Token readIdentifierOrKeyword() {
        int startLine = line, startCol = col;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && isIdentChar(source.charAt(pos))) {
            sb.append(source.charAt(pos));
            advance();
        }
        String word = sb.toString();
        TokenType type = KEYWORDS.contains(word) ? keywordType(word) : TokenType.IDENT;
        return new Token(type, word, startLine, startCol);
    }

    private boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '/' || c == '*';
    }

    private Token readNumber() {
        int startLine = line, startCol = col;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == 'm' || source.charAt(pos) == 's' || source.charAt(pos) == 'h')) {
            sb.append(source.charAt(pos));
            advance();
        }
        return new Token(TokenType.IDENT, sb.toString(), startLine, startCol);
    }

    private Token readRest() {
        int startLine = line, startCol = col;
        int startPos = pos;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && source.charAt(pos) != '\n') {
            sb.append(source.charAt(pos));
            advance();
        }
        // Guard: if we didn't advance, force-read at least one char to avoid infinite loop.
        if (pos == startPos && pos < source.length()) {
            sb.append(source.charAt(pos));
            advance();
        }
        String value = sb.toString().trim();
        // If the rest is a single quoted string, unwrap the quotes.
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return new Token(TokenType.REST, value, startLine, startCol);
    }

    private boolean lookAheadIsShell() {
        return pos + 5 <= source.length()
                && source.startsWith("shell", pos)
                && (pos + 5 == source.length() || !isIdentChar(source.charAt(pos + 5)));
    }

    private void skipSpaces() {
        while (pos < source.length() && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            advance();
        }
    }

    private char peek(int offset) {
        return pos + offset < source.length() ? source.charAt(pos + offset) : '\0';
    }

    private void advance() {
        if (pos < source.length()) {
            if (source.charAt(pos) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
            pos++;
        }
    }

    private static TokenType keywordType(String word) {
        return switch (word) {
            case "workflow" -> TokenType.WORKFLOW;
            case "on" -> TokenType.ON;
            case "run" -> TokenType.RUN;
            case "shell" -> TokenType.SHELL;
            case "env" -> TokenType.ENV;
            case "secret" -> TokenType.SECRET;
            case "artifact" -> TokenType.ARTIFACT;
            case "parallel" -> TokenType.PARALLEL;
            case "if" -> TokenType.IF;
            case "else" -> TokenType.ELSE;
            case "end" -> TokenType.END;
            case "timeout" -> TokenType.TIMEOUT;
            case "push" -> TokenType.PUSH;
            case "tag" -> TokenType.TAG;
            case "pull_request" -> TokenType.PULL_REQUEST;
            case "manual" -> TokenType.MANUAL;
            default -> throw new IllegalArgumentException("Unknown keyword: " + word);
        };
    }
}
