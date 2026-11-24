package com.example.shortener;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Production runs on TiDB: every Flyway migration must apply cleanly there too. */
class TiDbMigrationTest {

    @Test
    void allMigrationsApplyOnTiDb() throws Exception {
        try (GenericContainer<?> tidb = new GenericContainer<>("pingcap/tidb:v8.5.1")
                .withExposedPorts(4000)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(3))) {
            tidb.start();
            String root = "jdbc:mysql://" + tidb.getHost() + ":" + tidb.getMappedPort(4000) + "/";
            try (Connection c = DriverManager.getConnection(root + "?useSSL=false", "root", "");
                 Statement s = c.createStatement()) {
                s.execute("CREATE DATABASE snaplink");
            }
            String url = root + "snaplink?useSSL=false";

            Flyway.configure().dataSource(url, "root", "").target("3").load().migrate();
            try (Connection c = DriverManager.getConnection(url, "root", "");
                 Statement s = c.createStatement()) {
                s.execute("INSERT INTO url_mapping (original_url, short_code, created_at) VALUES ('https://x.example', 'abc', NOW(6))");
                s.execute("INSERT INTO url_analytics (url_id, accessed_at) SELECT id, NOW(6) FROM url_mapping WHERE short_code = 'abc'");
                s.execute("INSERT INTO url_analytics (url_id, accessed_at) SELECT id, NOW(6) FROM url_mapping WHERE short_code = 'abc'");
            }

            Flyway.configure().dataSource(url, "root", "").load().migrate();
            try (Connection c = DriverManager.getConnection(url, "root", "");
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT click_count, expires_at FROM url_mapping WHERE short_code = 'abc'")) {
                rs.next();
                assertThat(rs.getLong(1)).isEqualTo(2);  // backfilled from analytics
                assertThat(rs.getObject(2)).isNull();
            }
        }
    }
}
