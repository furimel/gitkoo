# GitKoo — Design (public)

GitKoo is a self-hosted Git forge: a place to store and work on your own code that
runs as soon as you launch it. It is **not** a GitHub/GitLab clone, an enterprise DevOps
suite, or a social network for programmers. It is a Git forge plus teamwork tools plus a
lightweight CI, with extremely simple deployment.

```text
create repository → clone → push/pull → issue → pull request → review → CI → merge
```

The full design rationale lives in a private design doc kept out of the repository.
This file records the decisions that are referenced by the code (via `@see` tags), so
the references resolve to something contributors can actually read.

## §2 Principles

- Don't over-engineer. Every feature must answer "what problem does this solve?"
- One instance must be trivial to set up. Default is SQLite + filesystem storage + an
  embedded web server. `java -jar gitkoo.jar` is the only required command.
- Do not require PostgreSQL, Redis, Nginx, RabbitMQ, Node, Docker, or a separate runner
  unless the operator chooses to scale.

## §3 Runtime & stack

```text
Language      Java 21 LTS
Framework     Spring Boot 4.1.x, Spring MVC (no WebFlux)
Security      Spring Security
Persistence   Spring Data JDBC (no JPA / Hibernate)
Database      SQLite (default), PostgreSQL optional
Git           Native Git CLI via ProcessBuilder (JGit only when justified)
SSH           Apache MINA SSHD
Frontend      Thymeleaf + HTML + CSS + HTMX + Vanilla JS (no React/npm/build)
CI            Custom GitKoo DSL (.koo, not YAML), Java ProcessBuilder, built-in worker
Build         Gradle Groovy DSL
Packaging     Executable JAR
Storage       Filesystem
```

Java 21 LTS is the baseline (aligned with the JDK available in the dev environment;
Spring Boot 4.1.x is fully compatible). The toolchain is locked to 21 in `build.gradle`.

Not used in core: JPA/Hibernate, Spring WebFlux, GraphQL, Spring Cloud, microservices.

## §4 Frontend

Server-side rendered with Thymeleaf. HTMX adds interactivity without a frontend build
system (comments, issue updates, labels, reviews, merge, pagination, search, toggles).
Vanilla JS only where HTMX is a poor fit. No React/Vue/Next/Vite/npm/webpack. Static
assets are committed, not built.

## §5 Git storage

GitKoo does **not** invent its own repository format. Each repository is a real, standard
Git repository on disk:

```text
data/git/repositories/{id}.git/
├── HEAD
├── config
├── objects/
├── refs/
└── ...
```

The database only stores metadata (`repository.id`, `owner_id`, `name`, `storage_path`).
Git is the source of truth for Git state; the database is the source of truth for metadata
(see §89). Do not duplicate commit/branch/tree data into SQL.

## §7 Git command strategy

Git CLI is the source of truth for complex operations (`git-upload-pack`,
`git-receive-pack`, `git rev-parse`, `git cat-file`, `git diff`, etc.). GitKoo invokes it
via `ProcessBuilder`. JGit may be added later, only where a specific operation needs native
API performance.

## §37 Configuration

`application.yaml`, zero-config by default:

```yaml
server:
  port: 3000
gitkoo:
  data: ./data
  git:
    binary: git
  ssh:
    enabled: true
    port: 2222
  ci:
    enabled: true
    workers: 2
```

If the operator configures nothing, the defaults apply: port 3000, SQLite at
`./data/gitkoo.db`, storage under `./data`, SSH on 2222, CI enabled with 2 workers.

## §43 Authentication

MVP: username + password (BCrypt hashing), server-side sessions, SSH keys, personal
access tokens. No plaintext passwords. CSRF protection on.

## §58 Health check

`/health` returns a small JSON status document covering application, database, and
storage. Kept intentionally simple — no observability platform.

## §78 Security baseline

Mandatory: CSRF, XSS protection, SQL injection protection, password hashing, session
security, SSH key verification, permission checks, path traversal protection, command
injection protection, workflow isolation, secret masking, rate limiting.

Especially: Git repository paths, artifact paths, and workflow commands must never be
assembled from raw user input without sanitizing/validating. Command execution uses
`ProcessBuilder` with an argument list, never concatenated shell strings (unless the
workflow DSL explicitly requests `run shell`).

## §116 Schema & migration

The database stores metadata only (Git state lives on the filesystem). The full schema
has 23 tables across these groups:

```text
users, ssh_keys, access_tokens
teams, team_members
repositories, repository_members
milestones, issues, issue_comments, labels, issue_labels
pull_requests, pull_request_reviewers, pull_request_reviews, pull_request_comments
workflows, workflow_runs, workflow_jobs, workflow_artifacts, workflow_secrets
activities, audit_events
```

Key decisions:
- Repository owner is polymorphic (`owner_type` ∈ USER|TEAM, `owner_id` with no FK —
  app-enforced) so a repository can belong to a user or a team.
- Issue and PR numbers are per-repository sequences (`UNIQUE (repository_id, number)`).
- Workflow secrets are encrypted at rest (AES-GCM); the key lives outside the repo.
- No Flyway/Liquibase for now. A small `DatabaseMigrationRunner` applies
  `classpath:gitkoo/migrations/V{n}__name.sql` files at startup, each in its own
  transaction, tracking applied versions in a `schema_version` table.

SQLite `PRAGMA foreign_keys = ON` is set via the HikariCP connection-init-sql so
`ON DELETE CASCADE` works.

## §109 MVP scope

```text
authentication, users, repositories, Git clone/push/pull, SSH,
repository browser, branches, commits, issues, comments,
PR, review, merge, teams, basic permissions, activity,
basic CI, workflow DSL, workflow logs, artifacts, SQLite
```
