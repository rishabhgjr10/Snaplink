-- Link lifetime controls: expiry time, start time and click limit.
-- One column per ALTER statement so the script runs on both MySQL and TiDB.

ALTER TABLE url_mapping ADD COLUMN expires_at DATETIME(6) NULL;
ALTER TABLE url_mapping ADD COLUMN starts_at DATETIME(6) NULL;
ALTER TABLE url_mapping ADD COLUMN max_clicks INT NULL;
-- Lifetime click total. Kept on the link so it survives the analytics retention purge
-- and so click limits can be checked without counting rows.
ALTER TABLE url_mapping ADD COLUMN click_count BIGINT NOT NULL DEFAULT 0;

UPDATE url_mapping m
SET click_count = (SELECT COUNT(*) FROM url_analytics a WHERE a.url_id = m.id);

CREATE INDEX idx_url_mapping_expires ON url_mapping (expires_at);
