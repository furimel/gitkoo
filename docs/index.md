---
layout: home

hero:
  name: GitKoo
  text: Self-hosted Git forge
  tagline: Simple. Fast. Yours. Run it and use it.
  actions:
    - theme: brand
      text: Quick start
      link: https://github.com/furimeo/gitkoo#quick-start
    - theme: alt
      text: GitHub
      link: https://github.com/furimeo/gitkoo

features:
  - title: Zero config
    details: java -jar gitkoo.jar. SQLite, filesystem storage, embedded web server. No PostgreSQL, Redis, or Docker required.
  - title: Git native
    details: Real Git repositories on disk. Clone, push, pull over HTTPS and SSH. Git is the source of truth.
  - title: Custom workflow DSL
    details: Human-readable .koo files, not YAML. Small grammar, good errors, no indentation magic.
  - title: Teamwork
    details: Issues, pull requests, reviews, merge, labels, milestones, teams, and permissions.
  - title: Lightweight CI
    details: ProcessBuilder execution, async job queue, log streaming, secret masking, artifacts.
  - title: No build step
    details: React + TypeScript on Primer, GitHub's own design system, served through Inertia so the Spring controllers keep owning routing and permissions.
---
