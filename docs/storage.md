# Storage

```
data/
  git/
    repositories/
      1.git/          # bare Git repos, ID-based (rename-safe)
      2.git/
  artifacts/
    {runId}/          # workflow artifacts
  attachments/
  logs/
    {runId}.log       # workflow run logs
  gitkoo.db           # SQLite database
  .ssh_host_key       # SSH server host key (auto-generated)
  .secret-key         # workflow secret encryption key
```

The database stores metadata only. Git state lives on the filesystem and
is the source of truth for commits, branches, and trees. Do not duplicate
Git data into SQL.

## Repository storage path

Storage paths are ID-based (`data/git/repositories/{id}.git`), not
name-based. This means renaming a repository only updates the database
row; the filesystem path never changes. This avoids moving gigabytes of
Git data on rename.

## SQLite configuration

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/gitkoo.db
    hikari:
      maximum-pool-size: 1
      connection-init-sql: PRAGMA foreign_keys = ON
```

Pool size is 1 because SQLite is single-writer. `PRAGMA foreign_keys = ON`
enables `ON DELETE CASCADE` so deleting a user or repository cascades to
child rows.

## Migrations

DIY migration runner (no Flyway/Liquibase). Files at
`src/main/resources/gitkoo/migrations/V{n}__name.sql`, applied in version
order at startup, each in its own transaction, tracked in a
`schema_version` table.

Current migrations:
- V1: initial schema (23 tables)
- V2: protected_branches
- V3: notifications
