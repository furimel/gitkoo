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
| Frontend | React 19 + TypeScript + Primer React, over Inertia |
| Client build | Vite, compiled into the jar by Gradle |
| CI | Custom GitKoo DSL (.koo, not YAML), Java ProcessBuilder |
| Build | Gradle Groovy DSL |
| Packaging | Executable JAR |

## What we do NOT use

JPA, Hibernate, WebFlux, GraphQL, Spring Cloud, microservices, Lombok,
MapStruct, Guava, Apache Commons, Tailwind, Vue, Next.js, Redux, React Router,
Flyway, Liquibase.

No separate REST API either. Every page is rendered by the Spring controller that
already owns its permission checks; see the Inertia note below.

## Why these choices

- **Spring Data JDBC over JPA**: simpler, no lazy loading surprises, no
  entity state magic. RowMapper and repository interfaces are enough.
- **SQLite over PostgreSQL by default**: zero-config. One file, no server
  process. PostgreSQL is optional for larger deployments.
- **Git CLI over JGit**: Git is the source of truth. Calling the real binary
  avoids reimplementing Git internals. JGit can be added per-operation if
  performance needs it.
- **React and Primer React over hand-written HTML**: Primer is GitHub's own design
  system and it ships as an npm package of React components. Reimplementing its
  markup by hand meant approximating it, and the approximation was where the bugs
  lived - a condition that could not guard the element it was written on, an empty
  state rendered next to a full list, a theme that resolved no tokens at all. Those
  are compile-time errors or impossible states in a typed component tree.

- **Inertia over a REST API**: the client renders, the server still routes. A Spring
  controller returns the same view name it always did and the props are its model, so
  every permission check, redirect and flash message keeps working untouched. A JSON
  API would have meant a second copy of every authorization decision living in the
  browser, and this codebase has already shipped three authorization holes. The
  protocol is about two hundred lines of Java (`web/inertia`), so there is no adapter
  dependency.

- **Node at build time, never at run time**: `./gradlew bootJar` runs Vite and copies
  the output into the jar. Deploying is still one file and one `java -jar`; nothing
  on the server needs Node. This replaces the previous "no npm at all" rule, which
  bought a simpler build at the cost of hand-maintaining a design system.
- **Custom DSL over YAML for workflows**: human-readable, small grammar,
  good error messages. YAML is fine for configuration but not for logic.
