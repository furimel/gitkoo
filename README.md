# GitKoo

A self-hosted Git forge: a place to store and work on your own code that runs
as soon as you launch it. Not a GitHub/GitLab clone — a Git forge plus teamwork
tools plus a lightweight CI, with extremely simple deployment.

## Quick start

```bash
java -jar gitkoo.jar
```

Open http://localhost:3000 and create your administrator account.

That's it. SQLite + filesystem storage + embedded web server. No PostgreSQL,
Redis, Nginx, or Docker required.

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

## Creating a repository

1. Click "New repository" on the dashboard.
2. Enter a name, description, and visibility.
3. Clone: `git clone http://localhost:3000/{username}/{name}.git`
4. Push your first commit.

## SSH access

Register an SSH key under Settings → SSH keys, then:

```bash
git clone git@localhost:{username}/{name}.git
```

SSH runs on port 2222 by default.

## Issues and pull requests

- Create issues with Markdown support (issue references like `#42` auto-link).
- Push a branch, then open a pull request.
- Submit reviews (Comment / Approve / Request changes).
- Merge creates a merge commit. Issues referenced by `fixes #NN` auto-close.

## Workflow DSL

Create `.gitkoo/workflows/build.koo` in your repository:

```
workflow build
    on push main

    env JAVA_HOME=/opt/java
    secret DEPLOY_TOKEN

    run ./gradlew test
    run ./gradlew build
    artifact build/libs/*.jar
```

Workflows trigger on push. Logs are viewable under Actions. Secrets are
masked in log output.

## Diagnostics

```bash
java -jar gitkoo.jar doctor
```

Checks Java, Git, database, data directory, and SSH.

## Stack

Java 21 · Spring Boot 4.1.x · Spring Data JDBC · SQLite · Thymeleaf · HTMX ·
Git CLI · Apache MINA SSHD · Custom workflow DSL
