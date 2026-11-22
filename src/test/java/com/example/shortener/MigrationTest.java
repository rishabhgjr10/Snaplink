package com.example.shortener;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs V2+ against data shaped like the pre-Flyway production schema
 * (case-insensitive codes, null codes, analytics keyed by code, raw IPs).
 */
class MigrationTest {

    @Test
    void upgradesLegacyDataSafely() throws Exception {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")) {
            mysql.start();
            Flyway v1 = Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .target("1")
                    .load();
            v1.migrate();

            try (Connection c = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 Statement s = c.createStatement()) {
                s.execute("INSERT INTO url_mapping (id, original_url, short_code, created_at) VALUES "
                        + "(1, 'https://one.example', 'abc', NOW(6)),"
                        + "(2, 'https://two.example', NULL, NULL),"
                        + "(3, 'https://three.example', 'promo', NOW(6))");
                s.execute("INSERT INTO url_analytics (short_code, accessed_at, ip_address, user_agent, referer) VALUES "
                        + "('abc', NOW(6), '203.0.113.77', 'ua', 'https://mail.example.com/inbox/1?x=secret'),"
                        + "('abc', NOW(6), '2001:db8::1', 'ua', 'android-app://com.slack'),"
                        + "('promo', NOW(6), 'unknown', 'ua', NULL),"
                        + "('old-renamed', NOW(6), '198.51.100.1', 'ua', NULL)");
            }

            Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .load()
                    .migrate();

            try (Connection c = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                 Statement s = c.createStatement()) {
                assertThat(scalar(s, "SELECT short_code FROM url_mapping WHERE id = 2")).isEqualTo("legacy-2");
                assertThat(scalar(s, "SELECT COUNT(*) FROM url_analytics")).isEqualTo("3"); // orphan removed
                assertThat(scalar(s, "SELECT COUNT(*) FROM url_analytics WHERE url_id = 1")).isEqualTo("2");
                assertThat(scalar(s, "SELECT ip_address FROM url_analytics WHERE url_id = 1 AND ip_address IS NOT NULL"))
                        .isEqualTo("203.0.113.0");
                assertThat(scalar(s, "SELECT COUNT(*) FROM url_analytics WHERE ip_address IS NULL")).isEqualTo("2");
                assertThat(scalar(s, "SELECT referer FROM url_analytics WHERE referer LIKE 'https%'"))
                        .isEqualTo("https://mail.example.com");

                // Codes are now case-sensitive: "ABC" can coexist with "abc".
                s.execute("INSERT INTO url_mapping (original_url, short_code, created_at) VALUES ('https://x.example', 'ABC', NOW(6))");
                assertThat(scalar(s, "SELECT COUNT(*) FROM url_mapping WHERE short_code = 'abc'")).isEqualTo("1");

                // Deleting a link cascades to its clicks.
                s.execute("DELETE FROM url_mapping WHERE id = 1");
                assertThat(scalar(s, "SELECT COUNT(*) FROM url_analytics WHERE url_id = 1")).isEqualTo("0");

                assertThatThrownBy(() -> s.execute(
                        "INSERT INTO url_mapping (original_url, short_code, created_at) VALUES ('https://y.example', NULL, NOW(6))"))
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    private static String scalar(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
