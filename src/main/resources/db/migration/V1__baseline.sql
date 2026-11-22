-- Baseline: the schema Hibernate (ddl-auto=update) created before Flyway was introduced.
-- Existing databases are baselined at version 1 (spring.flyway.baseline-on-migrate=true),
-- so this script only runs on brand-new, empty databases.

CREATE TABLE IF NOT EXISTS url_mapping (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    original_url VARCHAR(2048) NOT NULL,
    short_code   VARCHAR(64),
    created_at   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_url_mapping_short_code UNIQUE (short_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS url_analytics (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    short_code  VARCHAR(64)  NOT NULL,
    accessed_at DATETIME(6),
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(512),
    referer     VARCHAR(512),
    PRIMARY KEY (id),
    INDEX idx_analytics_short_code (short_code, accessed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
