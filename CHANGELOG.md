# Changelog

All notable changes to GitKoo are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added — MVP (0.1.0)

#### Foundation
- Zero-config boot: `java -jar gitkoo.jar` → SQLite + filesystem + embedded web server
- `application.yaml` with sensible defaults (port 3000, SQLite, SSH 2222, CI 2 workers)
- `StorageInitializer` creates `data/{git/repositories,artifacts,attachments,logs}`
- `/health` endpoint (application + database status)
- `doctor` CLI command (Java, Git, DB, data dir, SSH checks)

#### Authentication & Users
- First-run setup page (`/setup`) — create administrator account
- Session-based login (Spring Security, BCrypt password hashing)
- Personal access tokens (Bearer auth for `/api/**`, `gitkoo_`-prefixed, hashed)
- SSH key management UI (add/list/delete keys with fingerprint)
- CSRF protection on all form POSTs

#### Repositories & Git
- Create repository → `git init --bare` at `data/git/repositories/{id}.git`
- ID-based storage path (rename-safe, DESIGN.md §70-71)
- Code browser: file tree (`git ls-tree`), file viewer (`git cat-file`)
- Smart HTTP protocol: `git-upload-pack` / `git-receive-pack` (stateless-RPC, streaming)
- SSH Git endpoint: Apache MINA SSHD, public key auth, `git@host:owner/repo.git`
- Clone box with HTTPS/SSH URL + copy button
- Repository settings page (description, visibility, default branch)

#### Issues & Collaboration
- Issues: create, list, view, close, reopen
- Issue comments with Markdown rendering (CommonMark + Jsoup sanitization)
- Issue linking: `#42` auto-references; `fixes #NN` / `closes #NN` auto-close on merge
- Labels: create, add/remove on issues (colored badges)
- Milestones: create, assign to issues

#### Pull Requests
- Create PR (source → target branch)
- PR view: metadata, description, reviews
- Reviews: COMMENT / APPROVE / REQUEST_CHANGES
- Merge via `git merge --no-ff` (temporary clone, honors target branch)
- PR diff view (HTMX-loaded `git diff` between branches)
- Protected branches: enforce require-PR on direct push

#### Teams & Permissions
- Teams: create, add members (OWNER / MAINTAINER / MEMBER)
- `RepositoryPermissionService`: owner→ADMIN, team roles→READ/WRITE/ADMIN,
  explicit grants, PUBLIC/PRIVATE/INTERNAL visibility
- Wired into all controllers + git transport (HTTP + SSH)

#### Workflow DSL & CI
- Custom `.koo` workflow DSL (not YAML) — line-oriented, no indentation magic
- Lexer → Parser → AST (sealed interface) → Validator → Executor
- Keywords: `workflow`, `on`, `run`, `shell`, `env`, `secret`, `artifact`,
  `parallel`, `if`/`else`/`end`, `timeout`
- Triggers: `push`, `tag`, `pull_request`, `manual` (with branch filter)
- "Did you mean" error suggestions via Levenshtein distance
- Async job queue with worker pool (`BlockingQueue` + `ExecutorService`)
- ProcessBuilder execution with whitelisted environment (`GITKOO_*` + declared)
- Secret injection + masking in log output
- Logs to filesystem (`data/logs/{runId}.log`) + log viewer UI
- Artifacts collected to `data/artifacts/{runId}/` + DB records
- Actions page with run history + status badges

#### Activity & Notifications
- Activity feed: push, issue open/close, PR open/merge/close (work-oriented, not social)
- Audit log: repo creation, issue creation, team creation, member addition
- Notification center: records on issue/PR events, HTMX polling (every 30s)

#### Admin & Search
- Admin dashboard: user count, system info, audit log
- Basic SQL search (repository names, no Elasticsearch)

#### Commit History
- Commit list on repo overview
- Commit detail page with diff (`git show`)

#### Frontend
- GitHub Primer-inspired design system (light + dark mode)
- CSS custom properties, hand-written utility classes, component classes
- Theme toggle (persisted to localStorage, respects `prefers-color-scheme`)
- Vanilla JS (no Node/npm/build) — theme toggle, clipboard copy
- Responsive layout

#### Infrastructure
- DIY migration runner (no Flyway/Liquibase) — `V{n}__name.sql`, `schema_version` table
- SQLite dialect for Spring Data JDBC (custom `JdbcDialectProvider` SPI)
- Directional JDBC converters (`@ReadingConverter`/`@WritingConverter`)

#### Documentation
- `README.md` — quick start, configuration, workflow DSL, doctor CLI
- `DESIGN.md` — public design reference
- `AGENTS.md` — locked stack and coding rules
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`

### Tests
- 75 tests (unit + integration + E2E), all passing
- Integration tests use real SQLite + Git binary (no mocks for Git)
- E2E flow test: admin → repo → issue → PR → review → activity

[Unreleased]: https://github.com/furimeo/gitkoo/commits/main
