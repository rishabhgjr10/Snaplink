package com.example.shortener;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * One MySQL 8 container shared by all integration tests (started once per JVM).
 * Using real MySQL means the Flyway scripts and JPQL queries are tested as they run in production.
 */
public abstract class MySqlContainerSupport {

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("snaplink")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
