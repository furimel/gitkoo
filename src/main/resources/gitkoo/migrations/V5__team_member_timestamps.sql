-- team_members was created without the timestamps its entity has always declared,
-- so every INSERT failed with "table team_members has no column named CREATED_AT"
-- and creating a team has never once worked. Nothing compared the two halves.
--
-- Existing rows cannot have a real creation time, and inventing one would be worse
-- than admitting it is unknown, so the columns are nullable rather than backfilled
-- with today's date.

ALTER TABLE team_members ADD COLUMN created_at TEXT;
ALTER TABLE team_members ADD COLUMN updated_at TEXT;
