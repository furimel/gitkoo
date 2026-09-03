package com.furimeo.gitkoo.config;

import java.sql.JDBCType;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.mapping.JdbcValue;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Registers custom JDBC converters for SQLite (which stores everything as TEXT/INTEGER).
 *
 * <p>Converters MUST be annotated {@code @ReadingConverter} or {@code @WritingConverter}.
 * An un-annotated {@code Converter<String, OffsetDateTime>} is ambiguous because both
 * {@code String} and {@code OffsetDateTime} are native JDBC types, so it gets registered
 * in both directions and misfires on unrelated TEXT columns (e.g. trying to parse a
 * username as an OffsetDateTime). Direction annotations scope a converter to only the
 * read path or the write path, applied to fields of the matching target type.
 *
 * <p>Writing converters target {@link JdbcValue} to pin the bind SQL type.
 *
 * @see DESIGN.md §3 (SQLite default database), §116 (schema uses ISO-8601 TEXT timestamps)
 */
@Configuration
public class JdbcConversionsConfig {

    @Bean
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(List.of(
                new StringToOffsetDateTime(),
                new OffsetDateTimeToString(),
                new IntegerToBoolean(),
                new BooleanToInteger()
        ));
    }

    /** Reads an ISO-8601 TEXT column into an OffsetDateTime field (read path only). */
    @ReadingConverter
    static class StringToOffsetDateTime implements Converter<String, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            return OffsetDateTime.parse(source);
        }
    }

    /** Writes an OffsetDateTime field as an ISO-8601 TEXT column (write path only). */
    @WritingConverter
    static class OffsetDateTimeToString implements Converter<OffsetDateTime, JdbcValue> {
        @Override
        public JdbcValue convert(OffsetDateTime source) {
            return JdbcValue.of(source == null ? null : source.toString(), JDBCType.VARCHAR);
        }
    }

    /** Reads an INTEGER column (0/1) into a boolean field (read path only). */
    @ReadingConverter
    static class IntegerToBoolean implements Converter<Integer, Boolean> {
        @Override
        public Boolean convert(Integer source) {
            if (source == null) {
                return null;
            }
            return source != 0;
        }
    }

    /** Writes a boolean field as an INTEGER column (0/1) (write path only). */
    @WritingConverter
    static class BooleanToInteger implements Converter<Boolean, JdbcValue> {
        @Override
        public JdbcValue convert(Boolean source) {
            return JdbcValue.of(source == null ? null : (source ? 1 : 0), JDBCType.INTEGER);
        }
    }
}
