-- Abuse reports submitted by visitors; reviewed by admins who can disable a link.
CREATE TABLE link_reports (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    short_code   VARCHAR(64)   CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    reason       VARCHAR(32)   NOT NULL,
    details      VARCHAR(1000),
    reporter_uid VARCHAR(128),
    status       VARCHAR(16)   NOT NULL DEFAULT 'OPEN',
    created_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_reports_status (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
