package com.furimeo.gitkoo.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DatabaseMigrationRunnerTest {

    @Test
    void splitsMultipleStatements() {
        String sql = """
                CREATE TABLE a (id INTEGER PRIMARY KEY);
                CREATE TABLE b (id INTEGER PRIMARY KEY);
                """;
        List<String> statements = DatabaseMigrationRunner.splitStatements(sql);
        assertThat(statements).hasSize(2);
        assertThat(statements.get(0)).startsWith("CREATE TABLE a");
        assertThat(statements.get(1)).startsWith("CREATE TABLE b");
    }

    @Test
    void skipsCommentLines() {
        String sql = """
                -- this is a comment
                CREATE TABLE a (id INTEGER PRIMARY KEY);
                -- another comment
                CREATE TABLE b (id INTEGER PRIMARY KEY);
                """;
        List<String> statements = DatabaseMigrationRunner.splitStatements(sql);
        assertThat(statements).hasSize(2);
    }

    @Test
    void handlesMultilineStatement() {
        String sql = """
                CREATE TABLE a (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL
                );
                """;
        List<String> statements = DatabaseMigrationRunner.splitStatements(sql);
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0)).contains("id INTEGER PRIMARY KEY");
        assertThat(statements.get(0)).contains("name TEXT NOT NULL");
    }

    @Test
    void handlesEmptyAndWhitespaceOnly() {
        String sql = """
                CREATE TABLE a (id INTEGER PRIMARY KEY);



                """;
        List<String> statements = DatabaseMigrationRunner.splitStatements(sql);
        assertThat(statements).hasSize(1);
    }

    @Test
    void trailingStatementWithoutSemicicolonIsKept() {
        String sql = "CREATE TABLE a (id INTEGER PRIMARY KEY)";
        List<String> statements = DatabaseMigrationRunner.splitStatements(sql);
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0)).startsWith("CREATE TABLE a");
    }
}
