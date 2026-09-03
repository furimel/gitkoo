package com.furimeo.gitkoo.config;

import org.springframework.data.jdbc.core.dialect.JdbcH2Dialect;

/**
 * SQLite dialect for Spring Data JDBC.
 *
 * <p>Spring Data JDBC has no built-in SQLite dialect. SQLite is ANSI-ish and uses the same
 * {@code LIMIT ? OFFSET ?} clause as H2, so we reuse {@link JdbcH2Dialect} and only fix the name.
 * This is a deliberate reuse of an existing implementation, not a new abstraction.
 *
 * @see DESIGN.md §3 (SQLite default database)
 */
public class SqliteDialect extends JdbcH2Dialect {

    @Override
    public String getName() {
        return "SQLite";
    }
}
