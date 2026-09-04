-- GitKoo protected branches (DESIGN.md §116).
-- SQLite-friendly: INTEGER ids, bool as 0/1.

CREATE TABLE protected_branches (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  branch_name   TEXT NOT NULL,
  require_pr    INTEGER NOT NULL DEFAULT 1,
  UNIQUE (repository_id, branch_name)
);
