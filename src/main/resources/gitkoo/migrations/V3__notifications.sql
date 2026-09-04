-- GitKoo notifications (DESIGN.md §116).
-- SQLite-friendly: INTEGER ids, bool as 0/1, ISO-8601 TEXT timestamps.

CREATE TABLE notifications (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id     INTEGER NOT NULL,
  type        TEXT NOT NULL,
  message     TEXT NOT NULL,
  target_type TEXT,
  target_id   INTEGER,
  read        INTEGER NOT NULL DEFAULT 0,
  created_at  TEXT NOT NULL
);
