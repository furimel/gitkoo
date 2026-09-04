# Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 LTS |
| Framework | Spring Boot 4.1.x, Spring MVC (no WebFlux) |
| Security | Spring Security |
| Persistence | Spring Data JDBC (no JPA / Hibernate) |
| Database | SQLite (default), PostgreSQL (optional, not yet wired) |
| Git | Native Git CLI via ProcessBuilder |
| SSH | Apache MINA SSHD |
| Frontend | Thymeleaf + HTML + CSS + HTMX + Vanilla JS (no React, no build) |
| CI | Custom GitKoo DSL (.koo, not YAML), Java ProcessBuilder |
| Build | Gradle Groovy DSL |
| Packaging | Executable JAR |

## What we do NOT use

JPA, Hibernate, WebFlux, GraphQL, Spring Cloud, microservices, Lombok,
MapStruct, Guava, Apache Commons, Tailwind, React, Vue, Next.js, Vite, npm,
webpack, Node.js, Flyway, Liquibase.

## Why these choices

- **Spring Data JDBC over JPA**: simpler, no lazy loading surprises, no
  entity state magic. RowMapper and repository interfaces are enough.
- **SQLite over PostgreSQL by default**: zero-config. One file, no server
  process. PostgreSQL is optional for larger deployments.
- **Git CLI over JGit**: Git is the source of truth. Calling the real binary
  avoids reimplementing Git internals. JGit can be added per-operation if
  performance needs it.
- **Thymeleaf + HTMX over React**: server-side rendering, no build step, no
  npm dependencies. HTMX adds interactivity without a frontend framework.
- **Custom DSL over YAML for workflows**: human-readable, small grammar,
  good error messages. YAML is fine for configuration but not for logic.
