# Workflow DSL

GitKoo uses a custom, line-oriented DSL (not YAML) for CI workflows.
Files live at `.gitkoo/workflows/*.koo` in the repository.

## Example

```
workflow build
    on push main

    env JAVA_HOME=/opt/java
    secret DEPLOY_TOKEN

    run ./gradlew test
    run ./gradlew build

    artifact build/libs/*.jar

    parallel
        run ./test-java
        run ./test-cpp
    end

    if branch == "release"
        run ./publish
    end
```

## Grammar

Line-oriented. Newline is a token. Indentation is cosmetic (no
indentation magic).

```
File        := Workflow*
Workflow    := "workflow" IDENT NL (Trigger|Env|Secret)* (Stmt)*
Trigger     := "on" ("push" IDENT? | "tag" | "pull_request" | "manual") NL
Stmt        := Run | Env | Secret | Artifact | Parallel | If | Timeout
Run         := "run" ("shell")? CmdRest NL
Env         := "env" IDENT "=" Value NL
Secret      := "secret" IDENT NL
Artifact    := "artifact" Glob NL
Parallel    := "parallel" NL (Stmt)* "end" NL
If          := "if" Expr NL (Stmt)* ("else" NL (Stmt)*)? "end" NL
Timeout     := "timeout" Duration NL
Expr        := Var ("=="|"!=") Literal
```

## Pipeline

```
Lexer -> Parser -> AST (sealed interface) -> Validator -> Executor
```

- Unknown keywords get "Did you mean" suggestions via Levenshtein distance.
- Validator checks: at least one trigger, at least one `run` statement,
  no duplicate triggers.

## Execution

- Async via `BlockingQueue` + worker pool (sized by `ci.workers`).
- `ProcessBuilder` with argument lists. No shell string concatenation
  unless `run shell` is explicit.
- Environment whitelist: only `GITKOO_*` vars + declared `env`/`secret`.
- Secrets injected as env vars, masked in log output (`***`).
- Logs written to `data/logs/{runId}.log`.
- Artifacts collected to `data/artifacts/{runId}/` with DB records.
- `if` expressions: `==` / `!=` on `branch`, `event`, `tag`, `ref`.

## Runtime context (injected env vars)

```
GITKOO_REPOSITORY
GITKOO_COMMIT
GITKOO_BRANCH
GITKOO_TAG
GITKOO_REF
GITKOO_RUN_ID
GITKOO_EVENT
```

## Triggers

Currently wired: `push` (with optional branch filter).
Parsed but not yet wired: `tag`, `pull_request`, `manual`.
