-- Stars, watchers, topics, forks, and the profile fields that go with them.
-- SQLite-friendly: INTEGER ids, ISO-8601 TEXT timestamps, same as V1-V3.
--
-- Deliberately no star_count column on repositories: COUNT(*) over an indexed
-- column is free at this scale, and a denormalised counter is a drift bug waiting
-- to happen.

CREATE TABLE repository_stars (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at    TEXT NOT NULL,
  UNIQUE (repository_id, user_id)
);

CREATE TABLE repository_watchers (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at    TEXT NOT NULL,
  UNIQUE (repository_id, user_id)
);

CREATE TABLE repository_topics (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  repository_id INTEGER NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
  topic         TEXT NOT NULL,
  UNIQUE (repository_id, topic)
);

-- Listing "repositories this user starred" and "who starred this" are both common,
-- and only the second is served by the UNIQUE index above.
CREATE INDEX idx_repository_stars_user ON repository_stars(user_id);
CREATE INDEX idx_repository_watchers_user ON repository_watchers(user_id);

ALTER TABLE repositories ADD COLUMN fork_of_id INTEGER REFERENCES repositories(id);
ALTER TABLE repositories ADD COLUMN homepage TEXT;

CREATE INDEX idx_repositories_fork_of ON repositories(fork_of_id);

ALTER TABLE users ADD COLUMN company TEXT;
ALTER TABLE users ADD COLUMN location TEXT;
ALTER TABLE users ADD COLUMN website TEXT;
