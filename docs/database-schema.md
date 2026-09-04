# Database Schema

26 tables across these groups:

```
users, ssh_keys, access_tokens
teams, team_members
repositories, repository_members, protected_branches
milestones, issues, issue_comments, labels, issue_labels
pull_requests, pull_request_reviewers, pull_request_reviews, pull_request_comments
workflows, workflow_runs, workflow_jobs, workflow_artifacts, workflow_secrets
activities, audit_events, notifications
schema_version
```

## Key decisions

- Repository owner is polymorphic (`owner_type` = USER|TEAM, `owner_id`
  with no FK). App-enforced so a repository can belong to a user or a team.
- Issue and PR numbers are per-repository sequences
  (`UNIQUE (repository_id, number)`). Numbering via
  `COALESCE(MAX(number), 0) + 1` inside a transaction.
- Workflow secrets are encrypted at rest (AES-GCM). Key file at
  `data/.secret-key`.
- `activities` and `audit_events` have no FK on `actor_id` so historical
  logs persist after user deletion.
- `storage_path` on `repositories` is nullable on insert (set to the
  ID-based path after the first save returns the generated ID).
- Boolean columns stored as INTEGER (0/1). Timestamps as ISO-8601 TEXT.
  Enums as TEXT (app-enforced).

## Migrations

DIY runner. Files at `src/main/resources/gitkoo/migrations/V{n}__name.sql`.

| Version | File | Content |
|---------|------|---------|
| 1 | V1__init.sql | 23 tables (users through audit_events) |
| 2 | V2__protected_branches.sql | protected_branches table |
| 3 | V3__notifications.sql | notifications table |

To add a new migration: create `V{n}__name.sql` with the next version
number. The runner applies it automatically at startup.
