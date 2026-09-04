-- GitKoo initial schema (DESIGN.md §116).
-- SQLite-friendly: INTEGER ids, TEXT timestamps (ISO-8601), bool as 0/1, enums as TEXT.

-- ── users & authentication ──────────────────────────────────────────────
CREATE TABLE users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  username      TEXT NOT NULL UNIQUE,
  display_name  TEXT,
  email         TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  avatar        TEXT,
  bio           TEXT,
  status        TEXT NOT NULL DEFAULT 'ACTIVE',
  is_admin      INTEGER NOT NULL DEFAULT 0,
  created_at    TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);

CREATE TABLE ssh_keys (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title       TEXT,
  fingerprint TEXT NOT NULL UNIQUE,
  key_type    TEXT,
  public_key  TEXT,
  content     TEXT NOT NULL,
  created_at  TEXT NOT NULL
);

CREATE TABLE access_tokens (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name         TEXT,
  token_hash   TEXT NOT NULL UNIQUE,
  scopes       TEXT,
  last_used_at TEXT,
  created_at   TEXT NOT NULL,
  expires_at   TEXT
);

-- ── teams ──────────────────────────────────────────────────────────────
CREATE TABLE teams (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  name         TEXT NOT NULL UNIQUE,
  display_name TEXT,
  description  TEXT,
  created_at   TEXT NOT NULL,
  updated_at   TEXT NOT NULL
);

CREATE TABLE team_members (
  id      INTEGER PRIMARY KEY AUTOINCREMENT,
  team_id INTEGER NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role    TEXT NOT NULL DEFAULT 'MEMBER',
  UNIQUE (team_id, user_id)
);

-- ── repositories ───────────────────────────────────────────────────────
CREATE TABLE repositories (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  owner_type     TEXT NOT NULL,
  owner_id       INTEGER NOT NULL,
  name           TEXT NOT NULL,
  description    TEXT,
  visibility     TEXT NOT NULL DEFAULT 'PUBLIC',
  default_branch TEXT NOT NULL DEFAULT 'main',
  storage_path   TEXT,
  archived       INTEGER NOT NULL DEFAULT 0,
  created_at     TEXT NOT NULL,
  updated_at     TEXT NOT NULL,
  UNIQUE (owner_type, owner_id, name)
);

CREATE TABLE repository_members (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  permission    TEXT NOT NULL DEFAULT 'READ',
  UNIQUE (repository_id, user_id)
);

-- ── issues & labels & milestones ───────────────────────────────────────
CREATE TABLE milestones (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  title         TEXT NOT NULL,
  description   TEXT,
  due_date      TEXT,
  status        TEXT NOT NULL DEFAULT 'OPEN',
  created_at    TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);

CREATE TABLE issues (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  number        INTEGER NOT NULL,
  title         TEXT NOT NULL,
  body          TEXT,
  author_id     INTEGER NOT NULL REFERENCES users(id),
  assignee_id   INTEGER REFERENCES users(id),
  milestone_id  INTEGER REFERENCES milestones(id),
  status        TEXT NOT NULL DEFAULT 'OPEN',
  created_at    TEXT NOT NULL,
  updated_at    TEXT NOT NULL,
  closed_at     TEXT,
  UNIQUE (repository_id, number)
);

CREATE TABLE issue_comments (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  issue_id   INTEGER NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
  author_id  INTEGER NOT NULL REFERENCES users(id),
  body       TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE labels (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  name          TEXT NOT NULL,
  color         TEXT,
  description   TEXT,
  UNIQUE (repository_id, name)
);

CREATE TABLE issue_labels (
  id       INTEGER PRIMARY KEY AUTOINCREMENT,
  issue_id INTEGER NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
  label_id INTEGER NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
  UNIQUE (issue_id, label_id)
);

-- ── pull requests & reviews ────────────────────────────────────────────
CREATE TABLE pull_requests (
  id                   INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id        INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  number               INTEGER NOT NULL,
  title                TEXT NOT NULL,
  body                 TEXT,
  author_id            INTEGER NOT NULL REFERENCES users(id),
  source_branch        TEXT NOT NULL,
  source_repository_id INTEGER REFERENCES repositories(id),
  target_branch        TEXT NOT NULL,
  status               TEXT NOT NULL DEFAULT 'OPEN',
  merge_commit_sha     TEXT,
  assignee_id          INTEGER REFERENCES users(id),
  milestone_id         INTEGER REFERENCES milestones(id),
  created_at           TEXT NOT NULL,
  updated_at           TEXT NOT NULL,
  merged_at            TEXT,
  closed_at            TEXT,
  UNIQUE (repository_id, number)
);

CREATE TABLE pull_request_reviewers (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  pull_request_id INTEGER NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
  user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE (pull_request_id, user_id)
);

CREATE TABLE pull_request_reviews (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  pull_request_id INTEGER NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
  reviewer_id     INTEGER NOT NULL REFERENCES users(id),
  state           TEXT NOT NULL,
  body            TEXT,
  created_at      TEXT NOT NULL
);

CREATE TABLE pull_request_comments (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  pull_request_id INTEGER NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
  author_id       INTEGER NOT NULL REFERENCES users(id),
  file_path       TEXT,
  line_number     INTEGER,
  commit_sha      TEXT,
  body            TEXT NOT NULL,
  created_at      TEXT NOT NULL
);

-- ── workflow / CI ──────────────────────────────────────────────────────
CREATE TABLE workflows (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  file_path     TEXT NOT NULL,
  name          TEXT NOT NULL,
  triggers      TEXT,
  created_at    TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);

CREATE TABLE workflow_runs (
  id                   INTEGER PRIMARY KEY AUTOINCREMENT,
  workflow_id          INTEGER NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
  repository_id        INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  commit_sha           TEXT NOT NULL,
  event                TEXT NOT NULL,
  ref                  TEXT,
  status               TEXT NOT NULL DEFAULT 'QUEUED',
  triggered_by_user_id INTEGER REFERENCES users(id),
  started_at           TEXT,
  finished_at          TEXT
);

CREATE TABLE workflow_jobs (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  run_id      INTEGER NOT NULL REFERENCES workflow_runs(id) ON DELETE CASCADE,
  name        TEXT NOT NULL,
  command     TEXT,
  status      TEXT NOT NULL DEFAULT 'QUEUED',
  exit_code   INTEGER,
  log_path    TEXT,
  started_at  TEXT,
  finished_at TEXT
);

CREATE TABLE workflow_artifacts (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  run_id     INTEGER NOT NULL REFERENCES workflow_runs(id) ON DELETE CASCADE,
  name       TEXT NOT NULL,
  file_path  TEXT NOT NULL,
  size       INTEGER,
  created_at TEXT NOT NULL,
  expires_at TEXT
);

CREATE TABLE workflow_secrets (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id   INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  name            TEXT NOT NULL,
  encrypted_value BLOB NOT NULL,
  created_at      TEXT NOT NULL,
  UNIQUE (repository_id, name)
);

-- ── activity & audit ───────────────────────────────────────────────────
CREATE TABLE activities (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER,                -- nullable (user/team activity); no FK - historical log
  actor_id      INTEGER,                -- no FK - activities persist after user deletion
  type          TEXT NOT NULL,
  message       TEXT NOT NULL,
  payload       TEXT,                   -- JSON
  created_at    TEXT NOT NULL
);

CREATE TABLE audit_events (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  actor_id    INTEGER,                -- no FK - audit persists after user deletion
  action      TEXT NOT NULL,
  target_type TEXT,
  target_id   INTEGER,
  ip          TEXT,
  created_at  TEXT NOT NULL
);

-- schema_version is created by the migration runner itself (not here), so it can
-- track applied versions before the first migration runs.
