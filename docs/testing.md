# Testing

## Strategy

| Level | What | How |
|-------|------|-----|
| Unit | Parser, validators, permission logic, markdown, issue linker, shlex, secret masking | Pure Java, no Spring |
| Integration | SQLite + Git binary, user creation, repo creation (bare repo on disk), permission matrix, protected branches, notifications | `@SpringBootTest`, real DB, real Git |
| E2E | Admin -> repo -> issue -> PR -> review -> activity | `@SpringBootTest`, service-level flow |

75 tests, all passing.

## Running tests

```bash
./gradlew test
```

## Rules

- Git functionality is tested with the real Git binary, not mocked.
- `@Transactional` on integration tests for automatic rollback.
- No mocks for database or Git. Mock only external services if needed.
- Use `@TempDir` for temporary Git repositories in tests.
