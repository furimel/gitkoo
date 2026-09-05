# Contributing to GitKoo

Thanks for your interest in contributing! GitKoo is a small, focused project
and we welcome improvements that fit the design philosophy.

## Before you start

Read these files first:

- **[README.md](../README.md)** - what GitKoo is and how to run it
- **[docs/](../docs/README.md)** - architecture, stack, storage, permissions, workflow DSL, etc.
- **[AGENTS.md](../AGENTS.md)** - locked stack and coding rules (applies to all
  contributors, not just AI)

The short version: GitKoo is intentionally minimal. Don't add abstractions,
dependencies, or features without a concrete use case.

## Getting started

```bash
git clone git@github.com:furimeo/gitkoo.git
cd gitkoo
./gradlew test
./gradlew bootRun
```

Open http://localhost:3000 and create an admin account.

## Stack (locked)

Java 21 · Spring Boot 4.1.x · Spring Data JDBC (no JPA) · SQLite · React 19 ·
TypeScript · Primer React · Inertia · Vite · Git CLI · Apache MINA SSHD ·
Gradle Groovy DSL

Building needs Node 24 as well as a JDK; `./gradlew bootJar` runs the client
build itself. The jar it produces needs only a JVM and `git`.

**No** React/Vue/Next, **no** Node/npm/build step, **no** Lombok, **no**
unnecessary abstractions. See AGENTS.md for the full rules.

## Project structure

Domain-oriented, not layer-oriented:

```
src/main/java/com/furimeo/gitkoo/
├── auth/          # users, sessions, access tokens, SSH keys
├── git/           # Git CLI wrapper, smart HTTP, SSH server
├── repository/    # repositories, permissions, protected branches
├── issue/         # issues, comments, labels, milestones
├── pullrequest/   # pull requests, reviews, merge
├── team/          # teams and membership
├── workflow/      # workflow DSL, parser, executor
├── activity/      # activity feed and audit log
├── notification/  # notification center
├── config/        # Spring configuration, properties, migrations
├── db/            # database migration runner
└── web/           # shared web controllers (health, search, admin, markdown)
```

## Making changes

1. Create a branch: `git checkout -b feat/your-feature`
2. Write code following the existing style (match the surrounding code).
3. Add tests. Run `./gradlew test` - all tests must pass.
4. Commit with clear messages: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`
5. Push and open a pull request.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add branch deletion UI
fix: merge into correct target branch
docs: update README with SSH setup
test: add E2E flow test
refactor: extract permission check helper
```

## Testing

- **Unit tests**: parser, permission logic, validators
- **Integration tests**: SQLite + Git binary, real HTTP, auth flow
- **E2E test**: admin → repo → issue → PR → review → activity

Run the full suite:

```bash
./gradlew test
```

## Pull requests

- Keep PRs small and focused.
- Each commit should compile and pass tests.
- Reference issues: `Fixes #42`, `Closes #13`.

## Reporting bugs

Use the issue templates on GitHub. Include:
- GitKoo version (`java -jar gitkoo.jar --version` or check admin/system)
- Java version, OS, Git version
- Steps to reproduce
- Expected vs actual behavior

## License

By contributing, you agree that your contributions are licensed under the
project's LICENSE file.
