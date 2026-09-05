# GitKoo

[![CI](https://github.com/furimeo/gitkoo/actions/workflows/ci.yml/badge.svg)](https://github.com/furimeo/gitkoo/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/furimeo/gitkoo)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green)](https://spring.io/projects/spring-boot)

A self-hosted Git forge: a place to store and work on your own code that runs
as soon as you launch it. Not a GitHub/GitLab clone - a Git forge plus teamwork
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
Redis, Nginx, or Docker required - and no Node: the React client is compiled into
the jar.

```
create repository -> clone -> push/pull -> issue -> pull request -> review -> CI -> merge
```

### Building from source

Needs a JDK 21 and Node 24. One command builds both halves:

```bash
./gradlew bootJar          # runs npm ci, tsc and vite build, then packages the jar
java -jar build/libs/gitkoo-*.jar
```

### Working on the interface

Two terminals. Vite gets hot module replacement, and Spring proxies to it, so
everything stays on one port:

```bash
cd frontend && npm install && npm run dev   # 1. the client, with hot reload
./gradlew bootRun                           # 2. GitKoo on http://localhost:3000
```

Open http://localhost:3000, not the Vite port. Editing a component updates the
running page without a reload. See [docs/ui.md](docs/ui.md) for how the two halves
find each other.

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
[docs/storage.md](docs/storage.md).

## Creating a repository

1. Click **New repository** on the dashboard.
2. Enter a name, description, and visibility.
3. Clone:

```bash
git clone http://localhost:3000/{username}/{name}.git
```

4. Push your first commit.

## SSH access

Register an SSH key under **Settings -> SSH keys**, then:

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

GitKoo uses a custom, human-readable DSL - not YAML. Create
`.gitkoo/workflows/build.koo` in your repository:

```
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

See [docs/workflow-dsl.md](docs/workflow-dsl.md) for the full grammar.

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
| Frontend | React 19 + TypeScript + Primer React, over Inertia |
| CI | Custom GitKoo DSL, Java ProcessBuilder |
| Build | Gradle Groovy DSL |
| Packaging | Executable JAR |

## Contributing

See [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md) for guidelines. Read
[AGENTS.md](AGENTS.md) for the locked stack and coding rules.

## Documentation

| File | Content |
|------|---------|
| [docs/](docs/README.md) | Architecture, stack, storage, auth, permissions, Git transport, workflow DSL, database schema, security, UI, testing |
| [.github/CHANGELOG.md](.github/CHANGELOG.md) | Version history |
| [.github/SECURITY.md](.github/SECURITY.md) | Reporting vulnerabilities |
| [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md) | How to contribute |
| [.github/CODE_OF_CONDUCT.md](.github/CODE_OF_CONDUCT.md) | Community standards |
| [AGENTS.md](AGENTS.md) | Locked stack and coding rules |

## License

See [LICENSE](LICENSE).
