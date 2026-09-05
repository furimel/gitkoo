package com.furimeo.gitkoo.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Every persisted field must have a column to be persisted into.
 *
 * <p>{@code team_members} was created without the two timestamps its entity has
 * always declared, so every insert failed with "table team_members has no column
 * named CREATED_AT" - which means creating a team had never once worked, in any
 * version of this application. Nothing compared the entities against the schema, and
 * no test exercised the route.
 *
 * <p>Reading the migration files rather than a live database is deliberate: this
 * fails at the point the mismatch is introduced, not after someone runs the feature.
 */
class SchemaMatchesEntitiesTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/gitkoo/migrations");
    private static final Path SOURCES = Path.of("src/main/java");

    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE TABLE (\\w+)\\s*\\((.*?)\\n\\);", Pattern.DOTALL);
    private static final Pattern ADD_COLUMN = Pattern.compile("ALTER TABLE (\\w+) ADD COLUMN (\\w+)");
    private static final Pattern TABLE_ANNOTATION = Pattern.compile("@Table\\(\"(\\w+)\"\\)");
    /**
     * A persisted field, and the {@code @Column} name above it when one is given.
     *
     * <p>The line break between the two is matched explicitly rather than with a
     * general whitespace run. Consuming the field's indentation puts the match past
     * the start of the line, and a start-of-line anchor after it then fails - which
     * silently skips every field that has a {@code @Column} and reports it missing.
     */
    private static final Pattern FIELD = Pattern.compile(
            "(?:@Column\\(\"(\\w+)\"\\)[ \\t]*\\R)?[ \\t]*private [\\w.]+(?:<[^>]+>)?\\s+(\\w+);");

    @Test
    void everyEntityFieldHasAColumn() throws IOException {
        Map<String, Set<String>> schema = readSchema();
        List<String> problems = new ArrayList<>();

        try (Stream<Path> sources = Files.walk(SOURCES)) {
            for (Path java : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(java);
                Matcher table = TABLE_ANNOTATION.matcher(text);
                if (!table.find()) {
                    continue;
                }
                String name = table.group(1);
                Set<String> columns = schema.get(name);
                if (columns == null) {
                    problems.add(java.getFileName() + ": @Table(\"" + name
                            + "\") but no migration creates that table");
                    continue;
                }
                Matcher field = FIELD.matcher(text);
                while (field.find()) {
                    // An explicit @Column wins; otherwise Spring Data derives snake_case.
                    String column = field.group(1) != null ? field.group(1) : snake(field.group(2));
                    if (!columns.contains(column.toLowerCase(Locale.ROOT))) {
                        problems.add(java.getFileName() + ": " + name + " has no column '"
                                + column + "' for field '" + field.group(2) + "'");
                    }
                }
            }
        }

        assertThat(problems)
                .as("entity fields with nowhere to be stored:\n%s", String.join("\n", problems))
                .isEmpty();
    }

    /** Columns per table, as the migrations leave them. */
    private static Map<String, Set<String>> readSchema() throws IOException {
        Map<String, Set<String>> schema = new HashMap<>();
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".sql")).sorted().toList()) {
                String sql = Files.readString(file);

                Matcher created = CREATE_TABLE.matcher(sql);
                while (created.find()) {
                    Set<String> columns = new HashSet<>();
                    for (String line : created.group(2).split("\n")) {
                        String trimmed = line.strip().replaceAll(",$", "");
                        if (trimmed.isEmpty() || isConstraint(trimmed)) {
                            continue;
                        }
                        columns.add(trimmed.split("\\s+")[0].toLowerCase(Locale.ROOT));
                    }
                    schema.put(created.group(1), columns);
                }

                Matcher added = ADD_COLUMN.matcher(sql);
                while (added.find()) {
                    schema.computeIfAbsent(added.group(1), key -> new HashSet<>())
                            .add(added.group(2).toLowerCase(Locale.ROOT));
                }
            }
        }
        return schema;
    }

    private static boolean isConstraint(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        return upper.startsWith("UNIQUE") || upper.startsWith("PRIMARY")
                || upper.startsWith("FOREIGN") || upper.startsWith("CHECK")
                || upper.startsWith("--");
    }

    private static String snake(String field) {
        return field.replaceAll("(?<!^)(?=[A-Z])", "_").toLowerCase(Locale.ROOT);
    }
}
