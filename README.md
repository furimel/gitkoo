# GitKoo

[![CI](https://github.com/furimeo/gitkoo/actions/workflows/ci.yml/badge.svg)](https://github.com/furimeo/gitkoo/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/furimeo/gitkoo)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green)](https://spring.io/projects/spring-boot)

A self-hosted Git forge: a place to store and work on your own code that runs
as soon as you launch it. Not a GitHub/GitLab clone — a Git forge plus teamwork
tools plus a lightweight CI, with extremely simple deployment.

> **Have a place to store and work on your own code. Run it and use it.**

## Table of contents

- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Creating a repository](#creating-a-repository)
- [SSH access](#ssh-access)
- [Issues and pull requests](#issues-and-pull-requests)
- [Workflow DSL](#workflow-dsl)
- [Diagnostics](#diagnostics)
- [Stack](#stack)
- [Contributing](#contributing)
- [Documentation](#documentation)

## Quick start

```bash
java -jar gitkoo.jar
```

Open http://localhost:3000 and create your administrator account.

That's it. SQLite + filesystem storage + embedded web server. No PostgreSQL,
Redis, Nginx, or Docker required.

```text
create repository → clone → push/pull → issue → pull request → review → CI → merge
```

## Configuration

Defaults (zero config needed):

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

Override any value in `application.yml` next to the JAR. Full reference in
[DESIGN.md](DESIGN.md) §37.

## Creating a repository

1. Click **New repository** on the dashboard.
2. Enter a name, description, and visibility.
3. Clone:

```bash
git clone http://localhost:3000/{username}/{name}.git
```

4. Push your first commit.

## SSH access

Register an SSH key under **Settings → SSH keys**, then:

```bash
git clone git@localhost:{username}/{name}.git
```

SSH runs on port 2222 by default.

## Issues and pull requests

- Create issues with Markdown support. Issue references like `#42` auto-link.
- Push a branch, then open a pull request.
- Submit reviews: **Comment**, **Approve**, or **Request changes**.
- Merge creates a merge commit. Issues referenced by `fixes #NN` or
  `closes #NN` auto-close.
- Protected branches can require a pull request instead of direct push.

## Workflow DSL

GitKoo uses a custom, human-readable DSL — not YAML. Create
`.gitkoo/workflows/build.koo` in your repository:

```text
workflow build
    on push main

    env JAVA_HOME=/opt/java
    secret DEPLOY_TOKEN

    run ./gradlew test
    run ./gradlew build

    artifact build/libs/*.jar
```

Workflows trigger on push. Logs are viewable under **Actions**. Secrets are
masked in log output. Runs execute asynchronously via a worker pool.

See [DESIGN.md](DESIGN.md) §117 for the full grammar.

## Diagnostics

```bash
java -jar gitkoo.jar doctor
```

Checks Java, Git, database, data directory, and SSH configuration.

## Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 LTS |
| Framework | Spring Boot 4.1.x, Spring MVC |
| Persistence | Spring Data JDBC (no JPA) |
| Database | SQLite (default), PostgreSQL (optional) |
| Git | Native Git CLI via ProcessBuilder |
| SSH | Apache MINA SSHD |
| Frontend | Thymeleaf + HTML + CSS + HTMX (no React, no build step) |
| CI | Custom GitKoo DSL, Java ProcessBuilder |
| Build | Gradle Groovy DSL |
| Packaging | Executable JAR |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines. Read
[AGENTS.md](AGENTS.md) for the locked stack and coding rules.

## Documentation

- [DESIGN.md](DESIGN.md) — design decisions and architecture
- [CHANGELOG.md](CHANGELOG.md) — version history
- [SECURITY.md](SECURITY.md) — reporting vulnerabilities
- [CONTRIBUTING.md](CONTRIBUTING.md) — how to contribute
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — community standards

## License

See [LICENSE](LICENSE).
