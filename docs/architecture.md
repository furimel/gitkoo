# Architecture

GitKoo is a monolithic Spring Boot application. One process serves HTTP,
runs SSH, and executes workflows.

```
Browser
   |
   v
Spring MVC (port 3000)
   |
   +-- Auth (session, access tokens, SSH keys)
   +-- Repository (metadata, code browser, settings)
   +-- Issue (issues, comments, labels, milestones)
   +-- PullRequest (reviews, merge, diff)
   +-- Workflow (DSL parser, async executor, logs)
   +-- Activity / Audit / Notifications
   |
   v
SQLite (metadata) + Filesystem (Git repos, artifacts, logs)
```

SSH (port 2222) runs in the same process via Apache MINA SSHD.

## Package layout

Domain-oriented, not layer-oriented:

```
src/main/java/com/furimeo/gitkoo/
  auth/           users, sessions, access tokens, SSH keys, register
  git/            Git CLI wrapper, smart HTTP, SSH server
  repository/     repositories, permissions, protected branches, commits
  issue/          issues, comments, labels, milestones
  pullrequest/    pull requests, reviews, merge
  team/           teams and membership
  workflow/       workflow DSL, parser, executor
  activity/       activity feed and audit log
  notification/   notification center
  config/         Spring config, properties, security, migrations
  db/             database migration runner
  web/            shared controllers (health, search, admin, markdown)
```

No `controller/`, `service/`, `repository/`, `dto/`, `config/` split across
the whole project. Each domain package contains its own controller, service,
repository, and entity classes.
