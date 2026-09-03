package com.furimeo.gitkoo.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Applies SQL migration files at startup, before the application is ready.
 *
 * <p>Migration files live in {@code classpath:gitkoo/migrations/V{n}__name.sql}. The runner
 * creates the {@code schema_version} table itself (so it can track progress before the
 * first migration runs), reads the highest applied version, and applies every pending
 * file in version order. Each file runs in its own transaction.
 *
 * <p>No Flyway/Liquibase for now \u2014 this is deliberately small (DESIGN.md \u00a7116).
 *
 * @see DESIGN.md §116 (schema & migration)
 */
@Component
@Order(1)
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private static final Pattern VERSION_PATTERN = Pattern.compile("V(\\d+)__.*\\.sql$");
    private static final String MIGRATION_LOCATION = "classpath:gitkoo/migrations/V*__*.sql";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ResourcePatternResolver resourceResolver;

    public DatabaseMigrationRunner(DataSource dataSource, PlatformTransactionManager transactionManager,
                                   ResourcePatternResolver resourceResolver) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.tx = new TransactionTemplate(transactionManager);
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void run(String... args) {
        ensureSchemaVersionTable();
        int current = currentVersion();
        List<Migration> pending = pendingMigrations(current);

        if (pending.isEmpty()) {
            log.info("Database schema up to date (version {})", current);
            return;
        }

        for (Migration migration : pending) {
            log.info("Applying migration V{}__{}", migration.version(), migration.name());
            applyMigration(migration);
            log.info("Applied migration V{}__{}", migration.version(), migration.name());
        }
    }

    private void ensureSchemaVersionTable() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version    INTEGER PRIMARY KEY,
                    applied_at TEXT NOT NULL
                )
                """);
    }

    private int currentVersion() {
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(version), 0) FROM schema_version", Integer.class);
        return max != null ? max : 0;
    }

    private List<Migration> pendingMigrations(int currentVersion) {
        Resource[] resources;
        try {
            resources = resourceResolver.getResources(MIGRATION_LOCATION);
        } catch (IOException e) {
            throw new IllegalStateException("Could not resolve migration files from " + MIGRATION_LOCATION, e);
        }

        List<Migration> migrations = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            Matcher matcher = VERSION_PATTERN.matcher(filename);
            if (!matcher.matches()) {
                continue;
            }
            int version = Integer.parseInt(matcher.group(1));
            String name = filename.substring(filename.indexOf("__") + 2, filename.length() - 4);
            if (version > currentVersion) {
                migrations.add(new Migration(version, name, resource));
            }
        }
        migrations.sort(Comparator.comparingInt(Migration::version));
        return migrations;
    }

    private void applyMigration(Migration migration) {
        String sql;
        try {
            sql = migration.resource().getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read migration " + migration.filename(), e);
        }

        List<String> statements = splitStatements(sql);
        tx.executeWithoutResult(status -> {
            for (String statement : statements) {
                if (!statement.isBlank()) {
                    jdbc.execute(statement);
                }
            }
            jdbc.update("INSERT INTO schema_version (version, applied_at) VALUES (?, ?)",
                    migration.version(), nowIso());
        });
    }

    /**
     * Splits SQL into individual statements on {@code ;}. Strips {@code --} comment lines.
     * Simple by design: migration SQL in this project has no string literals containing
     * semicolons. If that changes, enhance this splitter.
     */
    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                statements.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (!current.isEmpty() && !current.toString().isBlank()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private static String nowIso() {
        return java.time.OffsetDateTime.now().toString();
    }

    private record Migration(int version, String name, Resource resource) {
        String filename() {
            return "V" + version + "__" + name + ".sql";
        }
    }
}
