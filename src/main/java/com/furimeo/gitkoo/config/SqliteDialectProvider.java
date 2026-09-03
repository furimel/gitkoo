package com.furimeo.gitkoo.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.data.jdbc.core.dialect.DialectResolver;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Registers a SQLite {@link Dialect} with Spring Data JDBC's {@link DialectResolver} SPI.
 *
 * <p>Spring Data JDBC ships dialects for H2, HSQLDB, MySQL, MariaDB, Postgres, DB2, Oracle,
 * and SQL Server, but not SQLite. This provider detects SQLite by the JDBC connection
 * database product name and returns {@link SqliteDialect}.
 *
 * <p>Registered via {@code META-INF/spring.factories}, matching the existing providers.
 *
 * @see DESIGN.md §3 (SQLite default database)
 */
public class SqliteDialectProvider implements DialectResolver.JdbcDialectProvider {

    @Override
    public Optional<Dialect> getDialect(JdbcOperations operations) {
        if (operations instanceof JdbcTemplate jdbcTemplate
                && jdbcTemplate.getDataSource() instanceof DataSource dataSource) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String productName = metaData.getDatabaseProductName();
                if (productName != null && productName.equalsIgnoreCase("SQLite")) {
                    return Optional.of(new SqliteDialect());
                }
            } catch (Exception ignored) {
                // fall through: let other providers try
            }
        }
        return Optional.empty();
    }
}
