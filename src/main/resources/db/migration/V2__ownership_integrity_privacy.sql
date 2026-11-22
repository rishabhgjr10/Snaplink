-- ---------------------------------------------------------------------------
-- url_mapping: ownership, takedown flag, case-sensitive non-null short codes
-- ---------------------------------------------------------------------------

ALTER TABLE url_mapping
    ADD COLUMN owner_uid VARCHAR(128) NULL,
    ADD COLUMN disabled  BOOLEAN      NOT NULL DEFAULT FALSE;

-- Rows left without a code by the old insert-then-update flow get a stable placeholder
-- instead of being deleted.
UPDATE url_mapping SET short_code = CONCAT('legacy-', id) WHERE short_code IS NULL;
UPDATE url_mapping SET created_at = CURRENT_TIMESTAMP(6) WHERE created_at IS NULL;

-- Binary collation: "abc" and "ABC" are different codes (Base62 uses both cases).
ALTER TABLE url_mapping
    MODIFY short_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    MODIFY created_at DATETIME(6) NOT NULL;

CREATE INDEX idx_url_mapping_owner ON url_mapping (owner_uid, id);

-- ---------------------------------------------------------------------------
-- url_analytics: reference links by id instead of by (renameable) short code
-- ---------------------------------------------------------------------------

ALTER TABLE url_analytics ADD COLUMN url_id BIGINT NULL;

UPDATE url_analytics a
    JOIN url_mapping m ON BINARY a.short_code = BINARY m.short_code
SET a.url_id = m.id;

-- Clicks whose link no longer exists (deleted, or orphaned by an alias rename) are unreachable.
DELETE FROM url_analytics WHERE url_id IS NULL;

ALTER TABLE url_analytics MODIFY url_id BIGINT NOT NULL;
UPDATE url_analytics SET accessed_at = CURRENT_TIMESTAMP(6) WHERE accessed_at IS NULL;
ALTER TABLE url_analytics MODIFY accessed_at DATETIME(6) NOT NULL;

CREATE INDEX idx_analytics_url_time ON url_analytics (url_id, accessed_at);
CREATE INDEX idx_analytics_accessed_at ON url_analytics (accessed_at);

ALTER TABLE url_analytics
    ADD CONSTRAINT fk_analytics_url FOREIGN KEY (url_id) REFERENCES url_mapping (id) ON DELETE CASCADE;

-- The old index may be missing if Hibernate created the table before it was declared.
SET @drop_old_index = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'url_analytics'
       AND index_name = 'idx_analytics_short_code') > 0,
    'ALTER TABLE url_analytics DROP INDEX idx_analytics_short_code',
    'SELECT 1');
PREPARE drop_old_index_stmt FROM @drop_old_index;
EXECUTE drop_old_index_stmt;
DEALLOCATE PREPARE drop_old_index_stmt;

ALTER TABLE url_analytics DROP COLUMN short_code;

-- ---------------------------------------------------------------------------
-- Privacy: keep only truncated IPv4 addresses and the referrer's origin
-- ---------------------------------------------------------------------------

UPDATE url_analytics
SET ip_address = CONCAT(SUBSTRING_INDEX(ip_address, '.', 3), '.0')
WHERE ip_address REGEXP '^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$';

UPDATE url_analytics
SET ip_address = NULL
WHERE ip_address IS NOT NULL
  AND ip_address NOT REGEXP '^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.0$';

UPDATE url_analytics
SET referer = CASE
                  WHEN referer REGEXP '^https?://[^/]+' THEN SUBSTRING_INDEX(referer, '/', 3)
                  ELSE NULL
              END
WHERE referer IS NOT NULL;
