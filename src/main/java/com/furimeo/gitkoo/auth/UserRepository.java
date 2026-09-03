package com.furimeo.gitkoo.auth;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link User}.
 *
 * @see DESIGN.md §87 (no generic BaseRepository, concrete per domain)
 */
public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Returns true when at least one admin user exists. Uses an explicit query
     * because the derived-method name "existsByIsAdminTrue" does not reliably
     * resolve the boolean "admin" property / "is_admin" column mapping.
     */
    @Query("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM users WHERE is_admin = 1")
    boolean existsAdmin();
}
