# GitKoo — AI Coding Rules

Read the project design doc before editing code. It is the source of truth
for the design. (The design doc is private and intentionally not committed.)

## Stack (locked)

- Java 21 LTS
- Spring Boot 4.1.x, Spring MVC (no WebFlux)
- Spring Security
- Spring Data JDBC (no JPA / Hibernate)
- SQLite (default), PostgreSQL optional
- Thymeleaf + HTML + CSS + HTMX + Vanilla JS (no React/Vue/npm/build)
- Git CLI via ProcessBuilder (JGit only when justified)
- Apache MINA SSHD (SSH git endpoint)
- Custom GitKoo workflow DSL (`.koo`, not YAML)
- Gradle Groovy DSL
- Packaging: executable JAR

## Rules

- **No JPA / Hibernate.** Use Spring Data JDBC (`RowMapper`, repository interfaces).
- **No React / Vue / Next / Vite / npm / webpack.** Frontend = Thymeleaf + HTMX + vanilla JS.
- **No Node.** No frontend build step. Static assets are committed.
- **No unnecessary abstractions.** No `BaseRepository<T>`, `CrudService<T>`,
  `AbstractGitProvider`, `GitProviderFactory` when there is only one implementation.
- **No microservices.** One process: web server + workflow scheduler + worker.
- **No dependency without justification.** Every new dependency needs a concrete reason.
- **No Lombok / MapStruct / Guava / Apache Commons** just to shorten code. Use the JDK API.
- **Use current official API.** Do not downgrade versions. Do not pull old tutorials into the codebase.
- **Keep domain-oriented structure.** Package by domain (`auth/`, `git/`, `repository/`,
  `issue/`, `pullrequest/`, `team/`, `workflow/`, `activity/`, `web/`), not by layer
  (`controller/`, `service/`, `repository/`, `dto/`, `config/`).
- **DIY migration** (no Flyway/Liquibase to start). Files in
  `src/main/resources/gitkoo/migrations/V{n}__name.sql`.
- **Git is the source of truth for Git state.** The database stores metadata only.
  Do not duplicate commit/branch/tree data into SQL.
- **Command execution**: `ProcessBuilder` with an argument list. Never concatenate a shell
  string unless the workflow DSL explicitly says `run shell`.
- **Security baseline is mandatory**: CSRF, XSS, SQL injection protection, password hashing
  (BCrypt for MVP), session security, path traversal protection, command injection protection.

## Package namespace

`com.furimeo.gitkoo` (not `dev.gitkoo`).

## Language

Code, commit messages, and in-repo documentation are written in English.
